package de.soderer.json.schema.validator;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueBoolean;
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

	public IfThenElseValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode ifJsonObject, final JsonNode thenObject, final JsonNode elseObject) throws JsonSchemaDefinitionError, JsonDuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, ifJsonObject);

		if (!jsonSchemaDependencyResolver.isDraftV7Mode()) {
			throw new JsonSchemaDefinitionError("Support for 'if' comes with draft version v7. Please configure used JSON schema version accordingly.", jsonSchemaPath);
		} else if (ifJsonObject == null) {
			throw new JsonSchemaDefinitionError("'if' value is 'null'", jsonSchemaPath);
		} else if (thenObject != null && !thenObject.isBoolean() && !thenObject.isJsonObject()) {
			throw new JsonSchemaDefinitionError("'then' branch is not 'boolean' or 'object'", jsonSchemaPath);
		} else if (elseObject != null && !elseObject.isBoolean() && !elseObject.isJsonObject()) {
			throw new JsonSchemaDefinitionError("'else' branch is not 'boolean' or 'object'", jsonSchemaPath);
		} else {
			// Parse "if" JSON sub schema
			if (ifJsonObject.isBoolean()) {
				ifJsonSchema = new JsonSchema(((JsonValueBoolean) ifJsonObject).getValue(), jsonSchemaDependencyResolver);
			} else if (ifJsonObject.isJsonObject()) {
				ifJsonSchema = new JsonSchema((JsonObject) ifJsonObject, jsonSchemaDependencyResolver);
			}

			if (thenObject != null && thenObject.isBoolean()) {
				thenBoolean = ((JsonValueBoolean) thenObject).getValue();
			} else if (thenObject != null && thenObject.isJsonObject()) {
				thenJsonSchema = new JsonSchema((JsonObject) thenObject, jsonSchemaDependencyResolver);
			}

			if (elseObject != null && elseObject.isBoolean()) {
				elseBoolean = ((JsonValueBoolean) elseObject).getValue();
			} else if (elseObject != null && elseObject.isJsonObject()) {
				elseJsonSchema = new JsonSchema((JsonObject) elseObject, jsonSchemaDependencyResolver);
			}
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		try {
			ifJsonSchema.validate(jsonNode);
		} catch (@SuppressWarnings("unused") final JsonSchemaDataValidationError e) {
			if (elseBoolean != null && !elseBoolean) {
				throw new JsonSchemaDataValidationError("JSON object does not match 'else' branch", jsonPath);
			} else if (elseJsonSchema != null) {
				elseJsonSchema.validate(jsonNode);
			}
			return;
		}

		if (thenBoolean != null && !thenBoolean) {
			throw new JsonSchemaDataValidationError("JSON object does not match 'then' branch", jsonPath);
		} else if (thenJsonSchema != null) {
			thenJsonSchema.validate(jsonNode);
		}
	}
}
