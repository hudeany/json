package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

public abstract class BaseJsonSchemaValidator {
	protected JsonSchemaDependencyResolver jsonSchemaDependencyResolver;
	protected JsonSchemaPath jsonSchemaPath;
	protected Object validatorData;

	protected BaseJsonSchemaValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("ValidatorData is 'null'", jsonSchemaPath);
		}

		this.jsonSchemaDependencyResolver = jsonSchemaDependencyResolver;
		this.jsonSchemaPath = jsonSchemaPath;
		this.validatorData = validatorData;
	}

	public abstract void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError;
}
