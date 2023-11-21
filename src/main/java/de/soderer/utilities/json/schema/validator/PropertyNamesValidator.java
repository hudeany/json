package de.soderer.utilities.json.schema.validator;

import java.util.List;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchema;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

public class PropertyNamesValidator extends BaseJsonSchemaValidator {
	private final List<BaseJsonSchemaValidator> subValidators;

	public PropertyNamesValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("PropertyNamesValidator data is 'null'", jsonSchemaPath);
		} else if (!(validatorData instanceof JsonObject)) {
			throw new JsonSchemaDefinitionError("PropertyNamesValidator data is not a JsonObject", jsonSchemaPath);
		}

		subValidators = JsonSchema.createValidators((JsonObject) validatorData, jsonSchemaDependencyResolver, jsonSchemaPath);
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
					throw new JsonSchemaDataValidationError("Invalid data type '" + ((JsonObject) jsonNode.getValue()).getClass().getSimpleName() + "'", jsonPath, e);
				}
				for (final BaseJsonSchemaValidator subValidator : subValidators) {
					subValidator.validate(newJsonNode, jsonPath);
				}
			}
		}
	}
}
