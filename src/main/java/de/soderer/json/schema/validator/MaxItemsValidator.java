package de.soderer.json.schema.validator;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonValueFloat;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Maximum number of items in an array
 */
public class MaxItemsValidator extends BaseJsonSchemaValidator {
	public MaxItemsValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Data for maximum items is 'null'", jsonSchemaPath);
		} else if (validatorData.isString()) {
			try {
				Integer.parseInt(((JsonValueString) validatorData).getValue());
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for maximum items '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData.isInteger()) {
			final int maximumItemsValue = ((JsonValueInteger) validatorData).getValue().intValue();
			if (maximumItemsValue < 0) {
				throw new JsonSchemaDefinitionError("Data for maximum items amount is negative", jsonSchemaPath);
			}
		} else if (validatorData.isFloat()) {
			final int maximumItemsValue = ((JsonValueFloat) validatorData).getValue().intValue();
			if (maximumItemsValue < 0) {
				throw new JsonSchemaDefinitionError("Data for maximum items amount is negative", jsonSchemaPath);
			}
		} else {
			throw new JsonSchemaDefinitionError("Data for maximum items '" + validatorData + "' is not a number", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		final int maximumItemsValue;
		if (validatorData.isString()) {
			maximumItemsValue = Integer.parseInt(((JsonValueString) validatorData).getValue());
		} else if (validatorData.isInteger()) {
			maximumItemsValue = ((JsonValueInteger) validatorData).getValue().intValue();
		} else {
			maximumItemsValue = ((JsonValueFloat) validatorData).getValue().intValue();
		}

		if (!(jsonNode.isJsonArray())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'array' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			if (((JsonArray) jsonNode).size() > maximumItemsValue) {
				throw new JsonSchemaDataValidationError("Required maximum number of items is '" + validatorData + "' but was '" + ((JsonArray) jsonNode).size() + "'", jsonPath);
			}
		}
	}
}
