package de.soderer.json.schema.validator;

import java.util.List;

import de.soderer.json.JsonNode;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

public abstract class BaseJsonSchemaValidator {
	protected JsonSchemaDependencyResolver jsonSchemaDependencyResolver;
	protected JsonSchemaPath jsonSchemaPath;
	protected JsonNode validatorData;

	protected BaseJsonSchemaValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		if (validatorData == null || validatorData.isNull()) {
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
