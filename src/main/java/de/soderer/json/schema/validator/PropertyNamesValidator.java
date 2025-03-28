package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * JSON schema validator for property key names of JSON objects
 */
public class PropertyNamesValidator extends BaseJsonSchemaValidator {
	private final List<BaseJsonSchemaValidator> subValidators;

	public PropertyNamesValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("PropertyNames validator data is 'null'", jsonSchemaPath);
		} else if (validatorData instanceof JsonObject) {
			subValidators = JsonSchema.createValidators((JsonObject) validatorData, jsonSchemaDependencyResolver, jsonSchemaPath);
		} else if (validatorData instanceof Boolean) {
			subValidators = new ArrayList<>();
			subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData));
		} else {
			throw new JsonSchemaDefinitionError("PropertyNames validator data is not a JsonObject or Boolean", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonObject())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'object' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			for (final String propertyName : ((JsonObject) jsonNode.getValue()).keySet()) {
				JsonNode newJsonNode;
				try {
					newJsonNode = new JsonNode(propertyName);
				} catch (final Exception e) {
					throw new JsonSchemaDataValidationError("Invalid data type '" + jsonNode.getValue().getClass().getSimpleName() + "'", jsonPath, e);
				}
				for (final BaseJsonSchemaValidator subValidator : subValidators) {
					subValidator.validate(newJsonNode, jsonPath);
				}
			}
		}
	}
}
