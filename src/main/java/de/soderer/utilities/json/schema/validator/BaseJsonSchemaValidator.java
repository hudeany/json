package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

public abstract class BaseJsonSchemaValidator {
	protected JsonSchemaDependencyResolver jsonSchemaDependencyResolver;
	protected JsonSchemaPath jsonSchemaPath;
	protected Object validatorData;
	protected JsonNode jsonNode;
	protected JsonPath jsonPath;

	protected BaseJsonSchemaValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData, final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDefinitionError {
		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("ValidatorData is 'null'", jsonSchemaPath);
		}

		this.jsonSchemaDependencyResolver = jsonSchemaDependencyResolver;
		this.jsonSchemaPath = jsonSchemaPath;
		this.validatorData = validatorData;
		this.jsonNode = jsonNode;
		this.jsonPath = jsonPath;
	}

	public abstract void validate() throws Exception;
}
