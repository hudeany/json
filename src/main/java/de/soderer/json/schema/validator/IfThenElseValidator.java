package de.soderer.json.schema.validator;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.exception.JsonDuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Tripartite directive with mandatory "if", optional "then" and optional "else" part.
 * If the JSON subschema from the "if" part matches, then the JSON schema from the "then" part must match also, if available.
 * If the JSON subschema from the "if" part does not match, then the JSON schema from the "else" part must match also, if available.
 */
public class IfThenElseValidator extends BaseJsonSchemaValidator {
	private JsonSchema ifJsonSchema;
	private Boolean thenBoolean = null;
	private JsonSchema thenJsonSchema = null;
	private Boolean elseBoolean = null;
	private JsonSchema elseJsonSchema = null;

	public IfThenElseValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object ifJsonObject, final Object thenObject, final Object elseObject) throws JsonSchemaDefinitionError, JsonDuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, ifJsonObject);

		if (!jsonSchemaDependencyResolver.isDraftV7Mode()) {
			throw new JsonSchemaDefinitionError("Support for 'if' comes with draft version v7. Please configure used JSON schema version accordingly.", jsonSchemaPath);
		} else if (ifJsonObject == null) {
			throw new JsonSchemaDefinitionError("'if' value is 'null'", jsonSchemaPath);
		} else if (thenObject != null && !(thenObject instanceof Boolean) && !(thenObject instanceof JsonObject)) {
			throw new JsonSchemaDefinitionError("'then' branch is not 'boolean' or 'object'", jsonSchemaPath);
		} else if (elseObject != null && !(elseObject instanceof Boolean) && !(elseObject instanceof JsonObject)) {
			throw new JsonSchemaDefinitionError("'else' branch is not 'boolean' or 'object'", jsonSchemaPath);
		} else {
			// Parse "if" JSON sub schema
			if (ifJsonObject instanceof Boolean) {
				ifJsonSchema = new JsonSchema((Boolean) ifJsonObject, jsonSchemaDependencyResolver);
			} else if (ifJsonObject instanceof JsonObject) {
				ifJsonSchema = new JsonSchema((JsonObject) ifJsonObject, jsonSchemaDependencyResolver);
			}

			if (thenObject != null && thenObject instanceof Boolean) {
				thenBoolean = (Boolean) thenObject;
			} else if (thenObject != null && thenObject instanceof JsonObject) {
				thenJsonSchema = new JsonSchema((JsonObject) thenObject, jsonSchemaDependencyResolver);
			}

			if (elseObject != null && elseObject instanceof Boolean) {
				elseBoolean = (Boolean) elseObject;
			} else if (elseObject != null && elseObject instanceof JsonObject) {
				elseJsonSchema = new JsonSchema((JsonObject) elseObject, jsonSchemaDependencyResolver);
			}
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		try {
			ifJsonSchema.validate(jsonNode.getValue());
		} catch (@SuppressWarnings("unused") final JsonSchemaDataValidationError e) {
			if (elseBoolean != null && !elseBoolean) {
				throw new JsonSchemaDataValidationError("JSON object does not match 'else' branch", jsonPath);
			} else if (elseJsonSchema != null) {
				elseJsonSchema.validate(jsonNode.getValue());
			}
			return;
		}

		if (thenBoolean != null && !thenBoolean) {
			throw new JsonSchemaDataValidationError("JSON object does not match 'then' branch", jsonPath);
		} else if (thenJsonSchema != null) {
			thenJsonSchema.validate(jsonNode.getValue());
		}
	}
}
