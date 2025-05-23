package de.soderer.json.schema.validator;

import de.soderer.json.JsonNode;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * The schema is a single boolean value that results in a match or not, no matter what the JSON data node is like
 */
public class BooleanValidator extends BaseJsonSchemaValidator {
	public BooleanValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("BooleanValidator data is 'null'", jsonSchemaPath);
		} else if (!(validatorData instanceof Boolean)) {
			throw new JsonSchemaDefinitionError("BooleanValidator data is not 'boolean'", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(Boolean) validatorData) {
			throw new JsonSchemaDataValidationError("No option of 'anyOf' property did apply to JsonNode", jsonPath);
		}
	}
}
