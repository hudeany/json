package de.soderer.json.schema.validator;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Maximum number of items in an array
 */
public class MaxItemsValidator extends BaseJsonSchemaValidator {
	public MaxItemsValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Data for maximum items is 'null'", jsonSchemaPath);
		} else if (validatorData instanceof String) {
			try {
				this.validatorData = Integer.parseInt((String) validatorData);
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for maximum items '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData instanceof Number) {
			final int maximumItemsValue = ((Number) validatorData).intValue();
			if (maximumItemsValue < 0) {
				throw new JsonSchemaDefinitionError("Data for maximum items amount is negative", jsonSchemaPath);
			} else {
				this.validatorData = maximumItemsValue;
			}
		} else {
			throw new JsonSchemaDefinitionError("Data for maximum items '" + validatorData + "' is not a number", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonArray())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'array' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			if (!(validatorData instanceof Integer)) {
				System.out.println(); // TODO
			}

			if (((JsonArray) jsonNode.getValue()).size() > ((Integer) validatorData)) {
				throw new JsonSchemaDataValidationError("Required maximum number of items is '" + validatorData + "' but was '" + ((JsonArray) jsonNode.getValue()).size() + "'", jsonPath);
			}
		}
	}
}
