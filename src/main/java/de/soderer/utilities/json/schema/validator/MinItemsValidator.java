package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

public class MinItemsValidator extends BaseJsonSchemaValidator {
	public MinItemsValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Data for minimum items is 'null'", jsonSchemaPath);
		} else if (validatorData instanceof Integer) {
			if (((Integer) validatorData) < 0) {
				throw new JsonSchemaDefinitionError("Data for minimum items amount is negative", jsonSchemaPath);
			}
		} else if (validatorData instanceof String) {
			try {
				this.validatorData = Integer.parseInt((String) validatorData);
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for minimum items '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
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
