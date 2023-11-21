package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

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
