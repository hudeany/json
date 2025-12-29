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
 * Minimum length of string value
 */
public class MinLengthValidator extends BaseJsonSchemaValidator {
	public MinLengthValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Data for minLength is 'null'", jsonSchemaPath);
		} else if (validatorData.isString()) {
			try {
				Integer.parseInt(((JsonValueString) validatorData).getValue());
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for minLength '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData.isInteger()) {
			final int minimumLengthValue = ((JsonValueInteger) validatorData).getValue().intValue();
			if (minimumLengthValue < 0) {
				throw new JsonSchemaDefinitionError("Data for minLength is negative", jsonSchemaPath);
			}
		} else if (validatorData.isNumber()) {
			final int minimumLengthValue = ((JsonValueNumber) validatorData).getValue().intValue();
			if (minimumLengthValue < 0) {
				throw new JsonSchemaDefinitionError("Data for minLength is negative", jsonSchemaPath);
			}
		} else {
			throw new JsonSchemaDefinitionError("Data for minLength '" + validatorData + "' is not a number", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		final int minimumLengthValue;
		if (validatorData.isString()) {
			minimumLengthValue = Integer.parseInt(((JsonValueString) validatorData).getValue());
		} else if (validatorData.isInteger()) {
			minimumLengthValue = ((JsonValueInteger) validatorData).getValue().intValue();
		} else {
			minimumLengthValue = ((JsonValueNumber) validatorData).getValue().intValue();
		}

		if (!(jsonNode.isString())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'string' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			final int unicodeStringLength = TextUtilities.getUnicodeStringLength(((JsonValueString) jsonNode).getValue());
			if (unicodeStringLength < minimumLengthValue) {
				throw new JsonSchemaDataValidationError("String minLength is '" + validatorData + "' but was '" + unicodeStringLength + "'", jsonPath);
			}
		}
	}
}
