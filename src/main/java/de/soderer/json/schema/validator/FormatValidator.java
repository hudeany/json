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
import de.soderer.json.schema.JsonSchemaFormats;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.schema.JsonSchemaRegexUtilities;
import de.soderer.json.schema.JsonSchemaVersion;
import de.soderer.json.utilities.DateUtilities;
import de.soderer.json.utilities.TextUtilities;

/**
 * Validator for the "format" keyword: checks string data against a named data format like "email",
 * "date-time" or "ipv4". Format names are case-insensitive. Data that is not a string is ignored.
 * The checks follow the RFCs referenced by the JSON schema standard, see {@link JsonSchemaFormats}.
 * <br />
 * <br />
 * In simple mode "date", "time" and "date-time" are checked leniently (optional and short time
 * zones like "+01" are accepted) and unknown format names are a definition error. In all JSON
 * schema drafts unknown format names are ignored.<br />
 * <br />
 * Security note: the "regex" format only checks that a data value compiles as a pattern, it is never executed. The
 * built-in format checks regard data exceeding the recursion depth of the Java regex engine as invalid.
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
			"uuid", "duration",
			"unknown"));

	/** Pattern of the "time" format of draft 3: "hh:mm:ss" without time offset. */
	private static final Pattern DRAFT_V3_TIME_PATTERN = Pattern.compile("(?:[01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]");

	/** Whether the format name is unknown, which is ignored since draft 2019-09. */
	private boolean unknownFormat = false;

	/**
	 * Whether "date", "time" and "date-time" are checked strictly after RFC 3339 (all JSON schema
	 * drafts) or leniently with optional and short time zones (simple mode).
	 */
	private boolean strictDateTimeFormats = false;

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
			if (jsonSchemaDependencyResolver != null && !jsonSchemaDependencyResolver.isSimpleMode()) {
				// The JSON schema drafts regard unknown formats as valid (custom formats are allowed), only simple mode rejects them
				unknownFormat = true;
			} else {
				throw new JsonSchemaDefinitionError("Unknown format name '" + validatorData + "'", jsonSchemaPath);
			}
		}
		strictDateTimeFormats = jsonSchemaDependencyResolver != null && !jsonSchemaDependencyResolver.isSimpleMode();
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!jsonNode.isString() || unknownFormat) {
			// String formats only apply to string values
			return;
		}

		final String validatorDataString = ((JsonValueString) validatorData).getValue();
		final String value = ((JsonValueString) jsonNode).getValue();
		final String invalidDataMessage = "Invalid data for format '" + validatorDataString + "' was '" + value + "'";

		final boolean valid;
		switch (validatorDataString.toLowerCase(Locale.ROOT)) {
			case "email":
				valid = JsonSchemaFormats.isValidEmail(value);
				break;
			case "idn-email":
				valid = JsonSchemaFormats.isValidIdnEmail(value);
				break;
			case "date":
				valid = strictDateTimeFormats ? JsonSchemaFormats.isValidDate(value) : isValidLenientDate(value);
				break;
			case "date-time":
				valid = strictDateTimeFormats ? JsonSchemaFormats.isValidDateTime(value) : isValidLenientDateTime(value);
				break;
			case "time":
				if (jsonSchemaDependencyResolver != null && jsonSchemaDependencyResolver.isDraftV3Mode()) {
					// Draft 3: "hh:mm:ss" without time offset
					valid = DRAFT_V3_TIME_PATTERN.matcher(value).matches();
				} else {
					valid = strictDateTimeFormats ? JsonSchemaFormats.isValidTime(value) : isValidLenientTime(value);
				}
				break;
			case "duration":
				valid = JsonSchemaFormats.isValidDuration(value);
				break;
			case "hostname":
			case "host-name":
				// Up to draft 6 "hostname" is defined by RFC 1123 only, without the reserved labels of RFC 5891
				valid = JsonSchemaFormats.isValidHostname(value, jsonSchemaDependencyResolver == null || jsonSchemaDependencyResolver.isSimpleMode() || jsonSchemaDependencyResolver.isVersionAtLeast(JsonSchemaVersion.draftV7));
				break;
			case "idn-hostname":
			case "idn-host-name":
				valid = JsonSchemaFormats.isValidIdnHostname(value);
				break;
			case "ipv4":
			case "ip-address":
				valid = JsonSchemaFormats.isValidIpv4(value);
				break;
			case "ipv6":
				valid = JsonSchemaFormats.isValidIpv6(value);
				break;
			case "uri":
				valid = JsonSchemaFormats.isValidUri(value);
				break;
			case "uri-reference":
				valid = JsonSchemaFormats.isValidUriReference(value);
				break;
			case "iri":
				valid = JsonSchemaFormats.isValidIri(value);
				break;
			case "iri-reference":
				valid = JsonSchemaFormats.isValidIriReference(value);
				break;
			case "uri-template":
				valid = JsonSchemaFormats.isValidUriTemplate(value);
				break;
			case "json-pointer":
				valid = JsonSchemaFormats.isValidJsonPointer(value);
				break;
			case "relative-json-pointer":
				valid = JsonSchemaFormats.isValidRelativeJsonPointer(value);
				break;
			case "uuid":
				valid = JsonSchemaFormats.isValidUuid(value);
				break;
			case "regex":
				valid = isValidRegex(value);
				break;
			case "base64":
				if (!TextUtilities.isValidBase64(value)) {
					throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + trimStringToMaximumLength(value, 20, " ...") + "'", jsonPath);
				}
				valid = true;
				break;
			case "color":
				valid = CSS_COLOR_NAMES.contains(value) || HEX_COLOR_PATTERN.matcher(value).matches();
				break;
			case "uri-pointer":
			case "unknown":
				// No special checks
				valid = true;
				break;
			default:
				throw new JsonSchemaDataValidationError("Unknown format name '" + validatorData + "'", jsonPath);
		}
		if (!valid) {
			throw new JsonSchemaDataValidationError(invalidDataMessage, jsonPath);
		}
	}

	/**
	 * Checks whether a value compiles as ECMA-262 regular expression.
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	private static boolean isValidRegex(final String value) {
		return JsonSchemaRegexUtilities.isValidEcmaScriptRegex(value);
	}

	/**
	 * Lenient check of the "date" format in simple mode: ISO 8601 date with optional time zone.
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	private static boolean isValidLenientDate(final String value) {
		try {
			DateUtilities.parseStrictLocalDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, value);
			return true;
		} catch (@SuppressWarnings("unused") final DateTimeParseException e1) {
			try {
				DateUtilities.parseStrictLocalDate(DateUtilities.ISO_8601_DATE_FORMAT, value);
				return true;
			} catch (@SuppressWarnings("unused") final DateTimeParseException e2) {
				return false;
			}
		}
	}

	/**
	 * Lenient check of the "date-time" format in simple mode: ISO 8601 date-time, time zone optional
	 * and also in short form like "+01".
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	private static boolean isValidLenientDateTime(final String value) {
		try {
			DateUtilities.parseIso8601DateTimeString(value);
			return true;
		} catch (@SuppressWarnings("unused") final DateTimeParseException e) {
			return false;
		}
	}

	/**
	 * Lenient check of the "time" format in simple mode: ISO 8601 time, time zone optional and also
	 * in short form like "+01".
	 *
	 * @param value
	 *            the value
	 * @return true, if the value matches one of the supported time patterns
	 */
	private static boolean isValidLenientTime(final String value) {
		for (final String timePattern : TIME_PATTERNS) {
			try {
				DateUtilities.parseLocalTime(timePattern, value);
				return true;
			} catch (@SuppressWarnings("unused") final DateTimeParseException e) {
				// Try next pattern
			}
		}
		return false;
	}

	/**
	 * Trim a string to a maximum number of characters by replacing its middle part by a cutoff sign.
	 * Surrogate pairs (like emojis) are never split, so the result may be shorter than maxLength by up to two characters.
	 *
	 * @param inputString text, may be null
	 * @param maxLength maximum length of the result; if it is less than the length of the cutoff sign, the end is cut off without cutoff sign
	 * @param cutoffSign text replacing the removed middle part, e.g. "...", null for none
	 * @return shortened text, unchanged if it is not longer than maxLength
	 */
	public static String trimStringToMaximumLength(final String inputString, final int maxLength, String cutoffSign) {
		if (inputString == null || inputString.length() <= maxLength) {
			return inputString;
		}
		if (cutoffSign == null) {
			cutoffSign = "";
		}
		if (maxLength < cutoffSign.length()) {
			// The cutoff sign does not fit, so the end is cut off without it (before, a StringIndexOutOfBoundsException was thrown)
			int endIndex = Math.max(0, maxLength);
			if (endIndex > 0 && Character.isHighSurrogate(inputString.charAt(endIndex - 1))) {
				endIndex--;
			}
			return inputString.substring(0, endIndex);
		}
		int takeFirstLength = (maxLength - cutoffSign.length()) / 2;
		final int takeLastLength = maxLength - cutoffSign.length() - takeFirstLength;
		int lastPartStartIndex = inputString.length() - takeLastLength;
		// Do not split a surrogate pair (like an emoji) at the cut positions
		if (takeFirstLength > 0 && Character.isHighSurrogate(inputString.charAt(takeFirstLength - 1))) {
			takeFirstLength--;
		}
		if (lastPartStartIndex < inputString.length() && Character.isLowSurrogate(inputString.charAt(lastPartStartIndex))) {
			lastPartStartIndex++;
		}
		return inputString.substring(0, takeFirstLength) + cutoffSign + inputString.substring(lastPartStartIndex);
	}
}
