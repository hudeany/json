package de.soderer.json.schema.validator;

import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
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
 * Validates string values against special data formats
 */
public class FormatValidator extends BaseJsonSchemaValidator {
	public FormatValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Format value is 'null'", jsonSchemaPath);
		} else if (!(validatorData.isString())) {
			throw new JsonSchemaDefinitionError("Format value is not a string", jsonSchemaPath);
		}

		final String validatorDataString = ((JsonValueString) validatorData).getValue();

		if (!"email".equalsIgnoreCase(validatorDataString)
				&& !"idn-email".equalsIgnoreCase(validatorDataString)
				&& !"date-time".equalsIgnoreCase(validatorDataString)
				&& !"date".equalsIgnoreCase(validatorDataString)
				&& !"time".equalsIgnoreCase(validatorDataString)
				&& !"hostname".equalsIgnoreCase(validatorDataString)
				&& !"host-name".equalsIgnoreCase(validatorDataString)
				&& !"idn-hostname".equalsIgnoreCase(validatorDataString)
				&& !"idn-host-name".equalsIgnoreCase(validatorDataString)
				&& !"ipv4".equalsIgnoreCase(validatorDataString)
				&& !"ip-address".equalsIgnoreCase(validatorDataString)
				&& !"ipv6".equalsIgnoreCase(validatorDataString)
				&& !"uri".equalsIgnoreCase(validatorDataString)
				&& !"iri".equalsIgnoreCase(validatorDataString)
				&& !"uri-reference".equalsIgnoreCase(validatorDataString)
				&& !"uri-template".equalsIgnoreCase(validatorDataString)
				&& !"uri-pointer".equalsIgnoreCase(validatorDataString)
				&& !"iri-reference".equalsIgnoreCase(validatorDataString)
				&& !"regex".equalsIgnoreCase(validatorDataString)
				&& !"base64".equalsIgnoreCase(validatorDataString)
				&& !"color".equalsIgnoreCase(validatorDataString)
				&& !"json-pointer".equalsIgnoreCase(validatorDataString)
				&& !"relative-json-pointer".equalsIgnoreCase(validatorDataString)
				&& !"unknown".equalsIgnoreCase(validatorDataString)) {
			throw new JsonSchemaDefinitionError("Unknown format name '" + validatorData + "'", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		final String validatorDataString = ((JsonValueString) validatorData).getValue();

		if (jsonNode.isNull()) {
			// String formats ignore null values
		} else if (jsonNode.isInteger() || jsonNode.isFloat()) {
			// String formats ignore numeric values
		} else if (jsonNode.isJsonObject()) {
			// String formats ignore JsonObject values
		} else if (jsonNode.isJsonArray()) {
			// String formats ignore JsonArray values
		} else if (jsonNode.isBoolean()) {
			// String formats ignore Boolean values
		} else if (!jsonNode.isString()) {
			throw new JsonSchemaDataValidationError("Expected a 'string' value for formatcheck but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
		} else if ("email".equalsIgnoreCase(validatorDataString)) {
			if (!NetworkUtilities.isValidEmail(((JsonValueString) jsonNode).getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath);
			}
		} else if ("idn-email".equalsIgnoreCase(validatorDataString)) {
			if (!NetworkUtilities.isValidEmail(((JsonValueString) jsonNode).getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath);
			}
		} else if ("date".equalsIgnoreCase(validatorDataString)) {
			try {
				DateUtilities.parseStrictLocalDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, ((JsonValueString) jsonNode).getValue());
			} catch (final DateTimeParseException e1) {
				try {
					DateUtilities.parseStrictLocalDate(DateUtilities.ISO_8601_DATE_FORMAT, ((JsonValueString) jsonNode).getValue());
				} catch (@SuppressWarnings("unused") final DateTimeParseException e2) {
					throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath, e1);
				}
			}
		} else if ("date-time".equalsIgnoreCase(validatorDataString)) {
			try {
				DateUtilities.parseIso8601DateTimeString(((JsonValueString) jsonNode).getValue());
			} catch (final DateTimeParseException e) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath, e);
			}
		} else if ("time".equalsIgnoreCase(validatorDataString)) {
			try {
				DateUtilities.parseLocalTime(DateUtilities.ISO_8601_TIME_FORMAT_NO_TIMEZONE, ((JsonValueString) jsonNode).getValue());
			} catch (final DateTimeParseException e1) {
				try {
					DateUtilities.parseLocalTime(DateUtilities.ISO_8601_TIME_FORMAT, ((JsonValueString) jsonNode).getValue());
				} catch (@SuppressWarnings("unused") final DateTimeParseException e2) {
					try {
						DateUtilities.parseLocalTime(DateUtilities.ISO_8601_TIME_WITH_NANOS_FORMAT_NO_TIMEZONE, ((JsonValueString) jsonNode).getValue());
					} catch (@SuppressWarnings("unused") final DateTimeParseException e3) {
						try {
							DateUtilities.parseLocalTime(DateUtilities.ISO_8601_TIME_WITH_NANOS_FORMAT, ((JsonValueString) jsonNode).getValue());
						} catch (@SuppressWarnings("unused") final DateTimeParseException e4) {
							try {
								DateUtilities.parseLocalTime("HH:mm:ss.SSSSSSX", ((JsonValueString) jsonNode).getValue());
							} catch (@SuppressWarnings("unused") final DateTimeParseException e5) {
								try {
									DateUtilities.parseLocalTime("HH:mm:ss.SSSSSS", ((JsonValueString) jsonNode).getValue());
								} catch (@SuppressWarnings("unused") final DateTimeParseException e6) {
									try {
										DateUtilities.parseLocalTime("HH:mm:ss.SSX", ((JsonValueString) jsonNode).getValue());
									} catch (@SuppressWarnings("unused") final DateTimeParseException e7) {
										try {
											DateUtilities.parseLocalTime("HH:mm:ss.SS", ((JsonValueString) jsonNode).getValue());
										} catch (@SuppressWarnings("unused") final DateTimeParseException e8) {
											throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath, e1);
										}
									}
								}
							}
						}
					}
				}
			}
		} else if ("hostname".equalsIgnoreCase(validatorDataString)) {
			if (!NetworkUtilities.isValidHostname(((JsonValueString) jsonNode).getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath);
			}
		} else if ("host-name".equalsIgnoreCase(validatorDataString)) {
			if (!NetworkUtilities.isValidHostname(((JsonValueString) jsonNode).getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath);
			}
		} else if ("idn-hostname".equalsIgnoreCase(validatorDataString)) {
			if (!NetworkUtilities.isValidHostname(((JsonValueString) jsonNode).getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath);
			}
		} else if ("idn-host-name".equalsIgnoreCase(validatorDataString)) {
			if (!NetworkUtilities.isValidHostname(((JsonValueString) jsonNode).getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath);
			}
		} else if ("ipv4".equalsIgnoreCase(validatorDataString)) {
			if (!NetworkUtilities.isValidIpV4(((JsonValueString) jsonNode).getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath);
			}
		} else if ("ip-address".equalsIgnoreCase(validatorDataString)) {
			if (!NetworkUtilities.isValidIpV4(((JsonValueString) jsonNode).getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath);
			}
		} else if ("ipv6".equalsIgnoreCase(validatorDataString)) {
			if (!NetworkUtilities.isValidIpV6(((JsonValueString) jsonNode).getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath);
			}
		} else if ("uri".equalsIgnoreCase(validatorDataString)) {
			if (!NetworkUtilities.isValidUri(((JsonValueString) jsonNode).getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath);
			}
		} else if ("iri".equalsIgnoreCase(validatorDataString)) {
			if (!NetworkUtilities.isValidUri(((JsonValueString) jsonNode).getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath);
			}
		} else if ("uri-reference".equalsIgnoreCase(validatorDataString)) {
			// no special checks by now
		} else if ("uri-template".equalsIgnoreCase(validatorDataString)) {
			// no special checks by now
		} else if ("uri-pointer".equalsIgnoreCase(validatorDataString)) {
			// no special checks by now
		} else if ("iri-reference".equalsIgnoreCase(validatorDataString)) {
			// no special checks
		} else if ("regex".equalsIgnoreCase(validatorDataString)) {
			try {
				Pattern.compile(((JsonValueString) jsonNode).getValue());
			} catch (final Exception e) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath, e);
			}
		} else if ("base64".equalsIgnoreCase(validatorDataString)) {
			if (!TextUtilities.isValidBase64(((JsonValueString) jsonNode).getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + validatorDataString + "' was '" + TextUtilities.trimStringToMaximumLength(((JsonValueString) jsonNode).getValue(), 20, " ...") + "'", jsonPath);
			}
		} else if ("color".equalsIgnoreCase(validatorDataString)) {
			final String colorValue = ((JsonValueString) jsonNode).getValue();
			final List<String> cssColorList = Arrays.asList(new String[] {"aqua", "black", "blue", "fuchsia", "gray", "green", "lime", "maroon", "navy", "olive", "purple", "red", "silver", "teal", "white", "yellow"});
			if (!cssColorList.contains(colorValue) && !Pattern.matches("#[A-Fa-f0-9]{3}|#[A-Fa-f0-9]{6}", ((JsonValueString) jsonNode).getValue())) {
				throw new JsonSchemaDataValidationError("Invalid value for format 'color': " + ((JsonValueString) jsonNode).getValue(), jsonPath);
			}
		} else if ("json-pointer".equalsIgnoreCase(validatorDataString)) {
			// no special checks
		} else if ("relative-json-pointer".equalsIgnoreCase(validatorDataString)) {
			// no special checks
		} else if ("unknown".equalsIgnoreCase(validatorDataString)) {
			// no special checks
		} else {
			throw new JsonSchemaDataValidationError("Unknown format name '" + validatorData + "'", jsonPath);
		}
	}
}
