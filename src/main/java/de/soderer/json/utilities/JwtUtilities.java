package de.soderer.json.utilities;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

import de.soderer.json.Json5Reader;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;

/**
 * Helpers for JSON Web Tokens (JWT).
 * The token signature is not verified.
 */
public class JwtUtilities {
	private JwtUtilities() {
		// Utility class
	}

	/**
	 * Read the expiration time ("exp" claim) from the payload of a JWT token.
	 * The signature of the token is NOT verified, so the result must not be used for security decisions.
	 *
	 * @param jwtToken
	 *            JWT token in compact serialization ("header.payload.signature", Base64url encoded parts)
	 * @return expiration time in UTC, or null if the token has no (numeric or numeric string) "exp" claim
	 * @throws Exception
	 *             if the token is null, has no payload part or the payload is no valid Base64url encoded JSON object
	 */
	public static ZonedDateTime getJwtTokenValidity(final String jwtToken) throws Exception {
		try {
			final String[] jwtParts = jwtToken.split("\\.");

			if (jwtParts.length < 2) {
				throw new Exception("Missing JSON data part in JWT token");
			} else {
				final String jwtPart = jwtParts[1];
				String data;
				try {
					// JWT parts are Base64url encoded ('-' and '_' instead of '+' and '/') without padding (RFC 7515)
					data = new String(Base64.getUrlDecoder().decode(jwtPart.trim()), StandardCharsets.UTF_8);
				} catch (@SuppressWarnings("unused") final Exception e) {
					data = null;
				}

				if (data == null) {
					throw new Exception("Invalid JSON data in JWT token");
				} else {
					try {
						final JsonNode jsonItem = Json5Reader.readJsonItemString(data);
						if (jsonItem == null || !(jsonItem instanceof JsonObject)) {
							throw new Exception("Invalid JSON data in JWT token");
						} else {
							final JsonObject dataJsonObject = (JsonObject) jsonItem;
							final Object expireDateObject = dataJsonObject.getSimpleValue("exp");
							// exp = Seconds since 1970-01-01T00:00:00Z (Not milliseconds !)
							if (expireDateObject == null) {
								return null;
							} else if (expireDateObject instanceof Long) {
								return ZonedDateTime.ofInstant(Instant.ofEpochSecond((Long) expireDateObject), ZoneId.of("UTC"));
							} else if (expireDateObject instanceof Integer) {
								return ZonedDateTime.ofInstant(Instant.ofEpochSecond((Integer) expireDateObject), ZoneId.of("UTC"));
							} else if (expireDateObject instanceof Number) {
								// RFC 7519 NumericDate may contain fractional seconds, which are cut off here
								final long epochSecondsExpireDate = new BigDecimal(expireDateObject.toString()).longValue();
								return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecondsExpireDate), ZoneId.of("UTC"));
							} else if (expireDateObject instanceof String) {
								final long epochSecondsExpireDate = Long.parseLong((String) expireDateObject);
								return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecondsExpireDate), ZoneId.of("UTC"));
							}
						}
					} catch (final Exception e) {
						throw new Exception("Invalid JSON data in JWT token: " + e.getMessage(), e);
					}
				}

				return null;
			}
		} catch (final Exception e) {
			throw new Exception("Invalid JWT token: " + e.getMessage(), e);
		}
	}
}
