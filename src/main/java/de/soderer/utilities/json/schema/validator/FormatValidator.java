package de.soderer.utilities.json.schema.validator;

import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;
import de.soderer.utilities.json.utilities.DateUtilities;
import de.soderer.utilities.json.utilities.NetworkUtilities;
import de.soderer.utilities.json.utilities.TextUtilities;

/**
 * Validates string values against special data formats
 */
public class FormatValidator extends BaseJsonSchemaValidator {
	public FormatValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Format value is 'null'", jsonSchemaPath);
		} else if (!(validatorData instanceof String)) {
			throw new JsonSchemaDefinitionError("Format value is not a string", jsonSchemaPath);
		}

		if (!"email".equalsIgnoreCase((String) validatorData)
				&& !"idn-email".equalsIgnoreCase((String) validatorData)
				&& !"date-time".equalsIgnoreCase((String) validatorData)
				&& !"date".equalsIgnoreCase((String) validatorData)
				&& !"time".equalsIgnoreCase((String) validatorData)
				&& !"hostname".equalsIgnoreCase((String) validatorData)
				&& !"idn-hostname".equalsIgnoreCase((String) validatorData)
				&& !"ipv4".equalsIgnoreCase((String) validatorData)
				&& !"ipv6".equalsIgnoreCase((String) validatorData)
				&& !"uri".equalsIgnoreCase((String) validatorData)
				&& !"iri".equalsIgnoreCase((String) validatorData)
				&& !"uri-reference".equalsIgnoreCase((String) validatorData)
				&& !"uri-template".equalsIgnoreCase((String) validatorData)
				&& !"uri-pointer".equalsIgnoreCase((String) validatorData)
				&& !"iri-reference".equalsIgnoreCase((String) validatorData)
				&& !"regex".equalsIgnoreCase((String) validatorData)
				&& !"base64".equalsIgnoreCase((String) validatorData)) {
			throw new JsonSchemaDefinitionError("Unknown format name '" + validatorData + "'", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!jsonNode.isString()) {
			throw new JsonSchemaDataValidationError("Expected a 'string' value for formatcheck but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
		} else if ("email".equalsIgnoreCase((String) validatorData)) {
			if (!NetworkUtilities.isValidEmail((String) jsonNode.getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath);
			}
		} else if ("idn-email".equalsIgnoreCase((String) validatorData)) {
			if (!NetworkUtilities.isValidEmail((String) jsonNode.getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath);
			}
		} else if ("date".equalsIgnoreCase((String) validatorData)) {
			try {
				DateUtilities.parseLocalDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, (String) jsonNode.getValue());
			} catch (final DateTimeParseException e1) {
				try {
					DateUtilities.parseLocalDate(DateUtilities.ISO_8601_DATE_FORMAT, (String) jsonNode.getValue());
				} catch (@SuppressWarnings("unused") final DateTimeParseException e2) {
					throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath, e1);
				}
			}
		} else if ("date-time".equalsIgnoreCase((String) validatorData)) {
			try {
				DateUtilities.parseLocalDateTime(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, (String) jsonNode.getValue());
			} catch (final DateTimeParseException e1) {
				try {
					DateUtilities.parseLocalDateTime(DateUtilities.ISO_8601_DATETIME_FORMAT, (String) jsonNode.getValue());
				} catch (@SuppressWarnings("unused") final DateTimeParseException e2) {
					throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath, e1);
				}
			}
		} else if ("time".equalsIgnoreCase((String) validatorData)) {
			try {
				DateUtilities.parseLocalTime(DateUtilities.ISO_8601_TIME_FORMAT_NO_TIMEZONE, (String) jsonNode.getValue());
			} catch (final DateTimeParseException e1) {
				try {
					DateUtilities.parseLocalTime(DateUtilities.ISO_8601_TIME_FORMAT, (String) jsonNode.getValue());
				} catch (@SuppressWarnings("unused") final DateTimeParseException e2) {
					throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath, e1);
				}
			}
		} else if ("hostname".equalsIgnoreCase((String) validatorData)) {
			if (!NetworkUtilities.isValidHostname((String) jsonNode.getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath);
			}
		} else if ("idn-hostname".equalsIgnoreCase((String) validatorData)) {
			if (!NetworkUtilities.isValidHostname((String) jsonNode.getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath);
			}
		} else if ("ipv4".equalsIgnoreCase((String) validatorData)) {
			if (!NetworkUtilities.isValidIpV4((String) jsonNode.getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath);
			}
		} else if ("ipv6".equalsIgnoreCase((String) validatorData)) {
			if (!NetworkUtilities.isValidIpV6((String) jsonNode.getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath);
			}
		} else if ("uri".equalsIgnoreCase((String) validatorData)) {
			if (!NetworkUtilities.isValidUri((String) jsonNode.getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath);
			}
		} else if ("iri".equalsIgnoreCase((String) validatorData)) {
			if (!NetworkUtilities.isValidUri((String) jsonNode.getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath);
			}
		} else if ("uri-reference".equalsIgnoreCase((String) validatorData)) {
			// no special checks by now
		} else if ("uri-template".equalsIgnoreCase((String) validatorData)) {
			// no special checks by now
		} else if ("uri-pointer".equalsIgnoreCase((String) validatorData)) {
			// no special checks by now
		} else if ("iri-reference".equalsIgnoreCase((String) validatorData)) {
			// no special checks
		} else if ("regex".equalsIgnoreCase((String) validatorData)) {
			try {
				Pattern.compile((String) jsonNode.getValue());
			} catch (final Exception e) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath, e);
			}
		} else if ("base64".equalsIgnoreCase((String) validatorData)) {
			if (!TextUtilities.isValidBase64((String) jsonNode.getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + TextUtilities.trimStringToMaximumLength((String) jsonNode.getValue(), 20, " ...") + "'", jsonPath);
			}
		} else {
			throw new JsonSchemaDataValidationError("Unknown format name '" + validatorData + "'", jsonPath);
		}
	}
}
