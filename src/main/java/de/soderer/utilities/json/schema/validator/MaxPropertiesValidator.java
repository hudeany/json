package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

/**
 * Maximum number of JSON opbject properties
 */
public class MaxPropertiesValidator extends BaseJsonSchemaValidator {
	public MaxPropertiesValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Data for maximum property keys is 'null'", jsonSchemaPath);
		} else if (validatorData instanceof String) {
			try {
				this.validatorData = Integer.parseInt((String) validatorData);
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for maximum property keys '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData instanceof Number) {
			final int maximumPropertiesValue = ((Number) validatorData).intValue();
			if (maximumPropertiesValue < 0) {
				throw new JsonSchemaDefinitionError("Data for maximum property keys is negative", jsonSchemaPath);
			} else {
				this.validatorData = maximumPropertiesValue;
			}
		} else {
			throw new JsonSchemaDefinitionError("Data for maximum property keys '" + validatorData + "' is not a number", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonObject())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'object' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			if (((JsonObject) jsonNode.getValue()).keySet().size() > ((Integer) validatorData)) {
				throw new JsonSchemaDataValidationError("Required maximum number of properties is '" + validatorData + "' but was '" + ((JsonObject) jsonNode.getValue()).keySet().size() + "'", jsonPath);
			}
		}
	}
}
