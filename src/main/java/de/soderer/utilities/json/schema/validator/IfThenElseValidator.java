package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchema;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

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

	public IfThenElseValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonObject ifJsonObject, final Object thenObject, final Object elseObject) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, ifJsonObject);

		if (!jsonSchemaDependencyResolver.isDraftV7Mode()) {
			throw new JsonSchemaDefinitionError("Support for 'if' comes with draft version v7. Please configure used JSON schema version accordingly.", jsonSchemaPath);
		} else if (ifJsonObject == null) {
			throw new JsonSchemaDefinitionError("'if' value is 'null'", jsonSchemaPath);
		} else if (thenObject == null && elseObject == null) {
			throw new JsonSchemaDefinitionError("Both of 'then' and 'else' value of 'if-then-else' construct are missing or 'null'", jsonSchemaPath);
		} else if (thenObject != null && !(thenObject instanceof Boolean) && !(thenObject instanceof JsonObject)) {
			throw new JsonSchemaDefinitionError("'then' branch is not 'boolean' or 'object'", jsonSchemaPath);
		} else if (elseObject != null && !(elseObject instanceof Boolean) && !(elseObject instanceof JsonObject)) {
			throw new JsonSchemaDefinitionError("'else' branch is not 'boolean' or 'object'", jsonSchemaPath);
		} else {
			// Parse "if" JSON sub schema
			ifJsonSchema = new JsonSchema(ifJsonObject);

			if (thenObject != null && thenObject instanceof Boolean) {
				thenBoolean = (Boolean) thenObject;
			} else if (thenObject != null && thenObject instanceof JsonObject) {
				thenJsonSchema = new JsonSchema((JsonObject) thenObject);
			}

			if (elseObject != null && elseObject instanceof Boolean) {
				elseBoolean = (Boolean) elseObject;
			} else if (elseObject != null && elseObject instanceof JsonObject) {
				elseJsonSchema = new JsonSchema((JsonObject) elseObject);
			}
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		try {
			ifJsonSchema.validate(jsonNode.getValue());
			if (thenBoolean != null && !thenBoolean) {
				throw new JsonSchemaDataValidationError("JSON object does not match 'then' branch", jsonPath);
			} else if (thenJsonSchema != null) {
				thenJsonSchema.validate(jsonNode.getValue());
			}
		} catch (@SuppressWarnings("unused") final JsonSchemaDataValidationError e) {
			if (elseBoolean != null && !elseBoolean) {
				throw new JsonSchemaDataValidationError("JSON object does not match 'else' branch", jsonPath);
			} else if (elseJsonSchema != null) {
				elseJsonSchema.validate(jsonNode.getValue());
			}
		}
	}
}
