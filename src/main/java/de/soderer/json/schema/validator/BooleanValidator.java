package de.soderer.json.schema.validator;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * The schema is a single boolean value that results in a match or not, no matter what the JSON data node is like
 */
public class BooleanValidator extends BaseJsonSchemaValidator {
	public BooleanValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("BooleanValidator data is 'null'", jsonSchemaPath);
		} else if (!(validatorData.isBoolean())) {
			throw new JsonSchemaDefinitionError("BooleanValidator data is not 'boolean'", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!((JsonValueBoolean) validatorData).getValue()) {
			throw new JsonSchemaDataValidationError("No option of 'anyOf' property did apply to JsonNode", jsonPath);
		}
	}
}
