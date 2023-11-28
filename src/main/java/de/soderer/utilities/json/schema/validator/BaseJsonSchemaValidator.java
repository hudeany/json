package de.soderer.utilities.json.schema.validator;

import java.util.List;

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

	@SuppressWarnings("static-method")
	protected boolean validateSubSchema(final List<BaseJsonSchemaValidator> subSchemaValidators, final JsonNode jsonNode, final JsonPath jsonPath) {
		for (final BaseJsonSchemaValidator validator : subSchemaValidators) {
			try {
				validator.validate(jsonNode, jsonPath);
			} catch (@SuppressWarnings("unused") final JsonSchemaDataValidationError e) {
				return false;
			}
		}
		return true;
	}

	public abstract void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError;
}
