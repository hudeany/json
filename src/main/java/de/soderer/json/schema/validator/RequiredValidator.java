package de.soderer.json.schema.validator;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * JSON schema validator for mandatory JSON object property key names in JSON data objects
 */
public class RequiredValidator extends BaseJsonSchemaValidator {
	public RequiredValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (jsonSchemaDependencyResolver.isDraftV3Mode() && validatorData instanceof Boolean) {
			// Handled in PropertiesValidator
		} else if (!(validatorData instanceof JsonArray)) {
			throw new JsonSchemaDefinitionError("Data for required property keys is not a JsonArray", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonObject())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'object' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			for (final Object propertyKey : (JsonArray) validatorData) {
				if (propertyKey == null) {
					throw new JsonSchemaDataValidationError("Data entry for required property key name must be 'string' but was 'null'", jsonPath);
				} else if (!(propertyKey instanceof String)) {
					throw new JsonSchemaDataValidationError("Data entry for required property key name must be 'string' but was '" + propertyKey.getClass().getSimpleName() + "'", jsonPath);
				} else if (!((JsonObject) jsonNode.getValue()).containsPropertyKey((String) propertyKey)) {
					throw new JsonSchemaDataValidationError("Invalid property key. Missing required property '" + propertyKey + "'", jsonPath);
				}
			}
		}
	}
}
