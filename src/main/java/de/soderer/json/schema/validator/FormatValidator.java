package de.soderer.json.schema.validator;

import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.utilities.DateUtilities;
import de.soderer.json.utilities.NetworkUtilities;
import de.soderer.json.utilities.TextUtilities;

/**
 * Validator for the "format" keyword: checks string data against a named data format like "email",
 * "date-time" or "ipv4". Format names are case-insensitive. Some formats are accepted without
 * checks (e.g. "uri-reference", "json-pointer"). Data that is not a string is ignored.<br />
 * <br />
 * Security note: see {@link PatternValidator} regarding ReDoS risk — the "regex" format only checks that a data
 * value compiles as a pattern, but "pattern"/"patternProperties"/"propertyNames" elsewhere in a schema execute
 * regex patterns from the schema against data without any complexity or timeout guard.
 */
public class FormatValidator extends BaseJsonSchemaValidator {
	/** All supported format names in lower case. */
	private static final Set<String> KNOWN_FORMATS = new HashSet<>(Arrays.asList(
			"email", "idn-email",
			"date-time", "date", "time",
			"hostname", "host-name", "idn-hostname", "idn-host-name",
			"ipv4", "ip-address", "ipv6",
			"uri", "iri", "uri-reference", "uri-template", "uri-pointer", "iri-reference",
			"regex", "base64", "color",
			"json-pointer", "relative-json-pointer",
			"unknown"));

	/** CSS color names accepted by the "color" format. */
	private static final Set<String> CSS_COLOR_NAMES = new HashSet<>(Arrays.asList(
			"aqua", "black", "blue", "fuchsia", "gray", "green", "lime", "maroon", "navy", "olive", "purple", "red", "silver", "teal", "white", "yellow"));

	/** Hexadecimal color values accepted by the "color" format, e.g. #fff or #ffffff. */
	private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#[A-Fa-f0-9]{3}|#[A-Fa-f0-9]{6}");

	/** Time patterns accepted by the "time" format, tried in this order. */
	private static final List<String> TIME_PATTERNS = Arrays.asList(
			DateUtilities.ISO_8601_TIME_FORMAT_NO_TIMEZONE,
			DateUtilities.ISO_8601_TIME_FORMAT,
			DateUtilities.ISO_8601_TIME_WITH_NANOS_FORMAT_NO_TIMEZONE,
			DateUtilities.ISO_8601_TIME_WITH_NANOS_FORMAT,
			"HH:mm:ss.SSSSSSX",
			"HH:mm:ss.SSSSSS",
			"HH:mm:ss.SSX",
			"HH:mm:ss.SS");

	/**
	 * Creates a new "format" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, the format name
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a string or an unknown format name
	 */
	public FormatValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Format value is 'null'", jsonSchemaPath);
		} else if (!(validatorData.isString())) {
			throw new JsonSchemaDefinitionError("Format value is not a string", jsonSchemaPath);
		} else if (!KNOWN_FORMATS.contains(((JsonValueString) validatorData).getValue().toLowerCase(Locale.ROOT))) {
			throw new JsonSchemaDefinitionError("Unknown format name '" + validatorData + "'", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!jsonNode.isString()) {
			// String formats only apply to string values
			return;
		}

		final String validatorDataString = ((JsonValueString) validatorData).getValue();
		final String value = ((JsonValueString) jsonNode).getValue();
		final String invalidDataMessage = "Invalid data for format '" + validatorDataString + "' was '" + value + "'";

		switch (validatorDataString.toLowerCase(Locale.ROOT)) {
			case "email":
			case "idn-email":
				if (!NetworkUtilities.isValidEmail(value)) {
					throw new JsonSchemaDataValidationError(invalidDataMessage, jsonPath);
				}
				break;
			case "date":
				try {
					DateUtilities.parseStrictLocalDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, value);
				} catch (final DateTimeParseException e1) {
					try {
						DateUtilities.parseStrictLocalDate(DateUtilities.ISO_8601_DATE_FORMAT, value);
					} catch (@SuppressWarnings("unused") final DateTimeParseException e2) {
						throw new JsonSchemaDataValidationError(invalidDataMessage, jsonPath, e1);
					}
				}
				break;
			case "date-time":
				try {
					DateUtilities.parseIso8601DateTimeString(value);
				} catch (final DateTimeParseException e) {
					throw new JsonSchemaDataValidationError(invalidDataMessage, jsonPath, e);
				}
				break;
			case "time":
				validateTime(value, jsonPath, invalidDataMessage);
				break;
			case "hostname":
			case "host-name":
			case "idn-hostname":
			case "idn-host-name":
				if (!NetworkUtilities.isValidHostname(value)) {
					throw new JsonSchemaDataValidationError(invalidDataMessage, jsonPath);
				}
				break;
			case "ipv4":
			case "ip-address":
				if (!NetworkUtilities.isValidIpV4(value)) {
					throw new JsonSchemaDataValidationError(invalidDataMessage, jsonPath);
				}
				break;
			case "ipv6":
				if (!NetworkUtilities.isValidIpV6(value)) {
					throw new JsonSchemaDataValidationError(invalidDataMessage, jsonPath);
				}
				break;
			case "uri":
			case "iri":
				if (!NetworkUtilities.isValidUri(value)) {
					throw new JsonSchemaDataValidationError(invalidDataMessage, jsonPath);
				}
				break;
			case "regex":
				try {
					Pattern.compile(value);
				} catch (final Exception e) {
					throw new JsonSchemaDataValidationError(invalidDataMessage, jsonPath, e);
				}
				break;
			case "base64":
				if (!TextUtilities.isValidBase64(value)) {
					throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + TextUtilities.trimStringToMaximumLength(value, 20, " ...") + "'", jsonPath);
				}
				break;
			case "color":
				if (!CSS_COLOR_NAMES.contains(value) && !HEX_COLOR_PATTERN.matcher(value).matches()) {
					throw new JsonSchemaDataValidationError("Invalid value for format 'color': " + value, jsonPath);
				}
				break;
			case "uri-reference":
			case "uri-template":
			case "uri-pointer":
			case "iri-reference":
			case "json-pointer":
			case "relative-json-pointer":
			case "unknown":
				// No special checks
				break;
			default:
				throw new JsonSchemaDataValidationError("Unknown format name '" + validatorData + "'", jsonPath);
		}
	}

	/**
	 * Checks a value for the "time" format against all supported time patterns.
	 *
	 * @param value
	 *            the value to check
	 * @param jsonPath
	 *            the path of the JSON data node, used in error messages
	 * @param invalidDataMessage
	 *            the error message for an invalid value
	 * @throws JsonSchemaDataValidationError
	 *             if the value matches none of the time patterns
	 */
	private static void validateTime(final String value, final JsonPath jsonPath, final String invalidDataMessage) throws JsonSchemaDataValidationError {
		DateTimeParseException firstParseException = null;
		for (final String timePattern : TIME_PATTERNS) {
			try {
				DateUtilities.parseLocalTime(timePattern, value);
				return;
			} catch (final DateTimeParseException e) {
				if (firstParseException == null) {
					firstParseException = e;
				}
			}
		}
		throw new JsonSchemaDataValidationError(invalidDataMessage, jsonPath, firstParseException);
	}
}
