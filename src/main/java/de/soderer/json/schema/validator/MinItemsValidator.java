package de.soderer.json.schema.validator;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Minimum number of items in an array
 */
public class MinItemsValidator extends BaseJsonSchemaValidator {
	public MinItemsValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Data for minimum items is 'null'", jsonSchemaPath);
		} else if (validatorData instanceof String) {
			try {
				this.validatorData = Integer.parseInt((String) validatorData);
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for minimum items '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData instanceof Number) {
			final int minimumItemsValue = ((Number) validatorData).intValue();
			if (minimumItemsValue < 0) {
				throw new JsonSchemaDefinitionError("Data for minimum items amount is negative", jsonSchemaPath);
			} else {
				this.validatorData = minimumItemsValue;
			}
		} else {
			throw new JsonSchemaDefinitionError("Data for minimum items '" + validatorData + "' is not a number", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonArray())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'array' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			if (((JsonArray) jsonNode.getValue()).size() < ((Integer) validatorData)) {
				throw new JsonSchemaDataValidationError("Required minimum number of items is '" + validatorData + "' but was '" + ((JsonArray) jsonNode.getValue()).size() + "'", jsonPath);
			}
		}
	}
}
