package de.soderer.json.schema;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * URI helper methods for JSON schema identifiers ("$id", "$ref", "$anchor" ...) following RFC 3986
 * as far as needed for JSON schema. Also handles URNs and other non-hierarchical URIs, which
 * {@link URI#resolve(String)} does not resolve against.
 */
public final class JsonSchemaUriUtilities {
	/**
	 * Prefix of the default base URI of schema documents without own "$id" and without retrieval URI.
	 */
	public static final String DEFAULT_ROOT_BASE_URI_PREFIX = "https://json-schema.invalid/";

	/**
	 * Base URI used for a schema document without its own "$id" and without retrieval URI. The
	 * reserved top level domain ".invalid" guarantees that it is never downloaded.
	 */
	public static final String DEFAULT_ROOT_BASE_URI = DEFAULT_ROOT_BASE_URI_PREFIX + "root.json";

	private JsonSchemaUriUtilities() {
		// Utility class
	}

	/**
	 * Resolves a URI reference against a base URI.
	 *
	 * @param baseUri
	 *            absolute base URI, may contain a fragment, which is ignored
	 * @param reference
	 *            the URI reference, e.g. "other.json#/$defs/a", "#anchor" or an absolute URI
	 * @return the resolved, normalized URI (fragment kept, empty fragment removed)
	 */
	public static String resolve(final String baseUri, final String reference) {
		if (reference == null) {
			return normalize(baseUri);
		}
		final String trimmedReference = reference.trim();
		if (hasScheme(trimmedReference)) {
			return normalize(trimmedReference);
		} else if (baseUri == null) {
			return normalize(trimmedReference);
		} else if (trimmedReference.isEmpty()) {
			return removeFragment(baseUri);
		} else if (trimmedReference.startsWith("#")) {
			return normalize(removeFragment(baseUri) + trimmedReference);
		}

		final String baseWithoutFragment = removeFragment(baseUri);
		try {
			URI base = URI.create(baseWithoutFragment);
			if (base.isOpaque()) {
				// Relative references cannot be resolved against URNs and other opaque URIs
				return normalize(trimmedReference);
			}
			if (base.getRawAuthority() != null && (base.getRawPath() == null || base.getRawPath().isEmpty())) {
				// java.net.URI would join "http://host" and "a.json" to "http://hosta.json"
				base = URI.create(base.getScheme() + "://" + base.getRawAuthority() + "/" + (base.getRawQuery() == null ? "" : "?" + base.getRawQuery()));
			}
			final String fragment = getFragment(trimmedReference);
			final String referenceWithoutFragment = removeFragment(trimmedReference);
			String resolved;
			if (referenceWithoutFragment.isEmpty()) {
				resolved = baseWithoutFragment;
			} else {
				resolved = base.resolve(URI.create(encodeIllegalCharacters(referenceWithoutFragment))).toString();
			}
			if (fragment != null) {
				resolved = resolved + "#" + fragment;
			}
			return normalize(resolved);
		} catch (@SuppressWarnings("unused") final IllegalArgumentException e) {
			return normalize(trimmedReference);
		}
	}

	/**
	 * Normalizes a URI for use as lookup key: an empty fragment ("#" at the end) is removed.
	 *
	 * @param uri
	 *            the URI
	 * @return the normalized URI
	 */
	public static String normalize(final String uri) {
		if (uri == null) {
			return null;
		}
		String normalizedUri = uri.trim();
		if (normalizedUri.endsWith("#")) {
			normalizedUri = normalizedUri.substring(0, normalizedUri.length() - 1);
		}
		return normalizedUri;
	}

	/**
	 * Returns the URI without its fragment.
	 *
	 * @param uri
	 *            the URI
	 * @return the URI without "#" and fragment
	 */
	public static String removeFragment(final String uri) {
		if (uri == null) {
			return null;
		}
		final int fragmentStart = uri.indexOf('#');
		return fragmentStart < 0 ? uri : uri.substring(0, fragmentStart);
	}

	/**
	 * Returns the raw (still percent encoded) fragment of a URI.
	 *
	 * @param uri
	 *            the URI
	 * @return the fragment without "#", or null if there is no "#"
	 */
	public static String getFragment(final String uri) {
		if (uri == null) {
			return null;
		}
		final int fragmentStart = uri.indexOf('#');
		return fragmentStart < 0 ? null : uri.substring(fragmentStart + 1);
	}

	/**
	 * Checks whether a URI reference starts with a scheme (e.g. "http:", "urn:"), which makes it an
	 * absolute URI.
	 *
	 * @param reference
	 *            the URI reference
	 * @return true, if the reference has a scheme
	 */
	public static boolean hasScheme(final String reference) {
		if (reference == null || reference.isEmpty() || !Character.isLetter(reference.charAt(0))) {
			return false;
		}
		for (int i = 1; i < reference.length(); i++) {
			final char character = reference.charAt(i);
			if (character == ':') {
				return true;
			} else if (!(Character.isLetterOrDigit(character) || character == '+' || character == '-' || character == '.')) {
				return false;
			}
		}
		return false;
	}

	/**
	 * Decodes percent encoded characters (UTF-8) of a URI fragment.
	 *
	 * @param value
	 *            the encoded value
	 * @return the decoded value
	 */
	public static String percentDecode(final String value) {
		if (value == null || value.indexOf('%') < 0) {
			return value;
		}
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		for (int i = 0; i < value.length(); i++) {
			final char character = value.charAt(i);
			if (character == '%' && i + 2 < value.length() && isHexDigit(value.charAt(i + 1)) && isHexDigit(value.charAt(i + 2))) {
				buffer.write(Integer.parseInt(value.substring(i + 1, i + 3), 16));
				i += 2;
			} else {
				final byte[] bytes = String.valueOf(character).getBytes(StandardCharsets.UTF_8);
				buffer.write(bytes, 0, bytes.length);
			}
		}
		return buffer.toString(StandardCharsets.UTF_8);
	}

	/**
	 * Checks whether a fragment is a JSON pointer (empty or starting with "/") instead of a plain
	 * name anchor.
	 *
	 * @param fragment
	 *            the fragment without "#"
	 * @return true, if the fragment is a JSON pointer
	 */
	public static boolean isJsonPointerFragment(final String fragment) {
		return fragment == null || fragment.isEmpty() || fragment.startsWith("/");
	}

	/**
	 * Decodes one reference token of a JSON pointer ("~1" to "/", "~0" to "~").
	 *
	 * @param token
	 *            the token
	 * @return the decoded token
	 */
	public static String decodeJsonPointerToken(final String token) {
		return token.replace("~1", "/").replace("~0", "~");
	}

	/**
	 * Encodes one reference token for a JSON pointer ("~" to "~0", "/" to "~1").
	 *
	 * @param token
	 *            the token
	 * @return the encoded token
	 */
	public static String encodeJsonPointerToken(final String token) {
		return token.replace("~", "~0").replace("/", "~1");
	}

	/**
	 * Checks for a hexadecimal digit.
	 *
	 * @param character
	 *            the character
	 * @return true, if it is 0-9, a-f or A-F
	 */
	private static boolean isHexDigit(final char character) {
		return (character >= '0' && character <= '9') || (character >= 'a' && character <= 'f') || (character >= 'A' && character <= 'F');
	}

	/**
	 * Percent encodes characters which are not allowed in a URI, so {@link URI#create(String)}
	 * accepts the value.
	 *
	 * @param value
	 *            the value
	 * @return the encoded value
	 */
	private static String encodeIllegalCharacters(final String value) {
		final StringBuilder result = new StringBuilder();
		for (final byte characterByte : value.getBytes(StandardCharsets.UTF_8)) {
			final int character = characterByte & 0xFF;
			if (character > 0x20 && character < 0x7F && "\"<>\\^`{|}".indexOf(character) < 0) {
				result.append((char) character);
			} else {
				result.append('%').append(String.format("%02X", character));
			}
		}
		return result.toString();
	}
}
