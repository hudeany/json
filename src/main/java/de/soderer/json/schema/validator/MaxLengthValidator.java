package de.soderer.json.schema.validator;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.utilities.TextUtilities;

/**
 * Maximum length of string value
 */
public class MaxLengthValidator extends BaseJsonSchemaValidator {
	public MaxLengthValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Data for maxLength is 'null'", jsonSchemaPath);
		} else if (validatorData.isString()) {
			try {
				Integer.parseInt(((JsonValueString) validatorData).getValue());
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for maxLength '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData.isInteger()) {
			final int maximumLengthValue = ((JsonValueInteger) validatorData).getValue().intValue();
			if (maximumLengthValue < 0) {
				throw new JsonSchemaDefinitionError("Data for maxLength is negative", jsonSchemaPath);
			}
		} else if (validatorData.isNumber()) {
			final int maximumLengthValue = ((JsonValueNumber) validatorData).getValue().intValue();
			if (maximumLengthValue < 0) {
				throw new JsonSchemaDefinitionError("Data for maxLength is negative", jsonSchemaPath);
			}
		} else {
			throw new JsonSchemaDefinitionError("Data for maxLength '" + validatorData + "' is not a number", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		final int maximumLengthValue;
		if (validatorData.isString()) {
			maximumLengthValue = Integer.parseInt(((JsonValueString) validatorData).getValue());
		} else if (validatorData.isInteger()) {
			maximumLengthValue = ((JsonValueInteger) validatorData).getValue().intValue();
		} else {
			maximumLengthValue = ((JsonValueNumber) validatorData).getValue().intValue();
		}

		if (!(jsonNode.isString())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'string' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			final int unicodeStringLength = TextUtilities.getUnicodeStringLength(((JsonValueString) jsonNode).getValue());
			if (unicodeStringLength > maximumLengthValue) {
				throw new JsonSchemaDataValidationError("String maxLength is '" + validatorData + "' but was '" + unicodeStringLength + "'", jsonPath);
			}
		}
	}
}
