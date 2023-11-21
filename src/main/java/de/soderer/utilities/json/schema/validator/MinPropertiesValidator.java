package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

public class MinPropertiesValidator extends BaseJsonSchemaValidator {
	public MinPropertiesValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (!(validatorData instanceof Integer)) {
			throw new JsonSchemaDefinitionError("Data for minimum property keys amount is not an integer", jsonSchemaPath);
		} else if (validatorData instanceof String) {
			try {
				this.validatorData = Integer.parseInt((String) validatorData);
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for minimum property keys amount '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (((Integer) validatorData) < 0) {
			throw new JsonSchemaDefinitionError("Data for minimum property keys amount is negative", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonObject())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'object' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			if (((JsonObject) jsonNode.getValue()).keySet().size() < ((Integer) validatorData)) {
				throw new JsonSchemaDataValidationError("Required minimum number of properties is '" + validatorData + "' but was '" + ((JsonObject) jsonNode.getValue()).keySet().size() + "'", jsonPath);
			}
		}
	}
}
