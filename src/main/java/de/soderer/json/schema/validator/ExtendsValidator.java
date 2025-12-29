package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * JSON schema validator for external references in files and urls by code key name "$ref"
 */
public class ExtendsValidator extends BaseJsonSchemaValidator {
	List<JsonSchema> extendedSchemas = new ArrayList<>();

	public ExtendsValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		try {
			if (validatorData == null || validatorData.isNull()) {
				throw new JsonSchemaDefinitionError("Extended JSON schema value is 'null'", jsonSchemaPath);
			} else if (validatorData.isJsonArray()) {
				for (final JsonNode subSchemaObject : ((JsonArray) validatorData).items()) {
					if (subSchemaObject == null) {
						throw new JsonSchemaDefinitionError("Extended JSON schema value is 'null'", jsonSchemaPath);
					} else if (subSchemaObject.isJsonObject()) {
						extendedSchemas.add(new JsonSchema((JsonObject) subSchemaObject, jsonSchemaDependencyResolver));
					} else {
						throw new JsonSchemaDefinitionError("Extended JSON schema value is not 'JsonObject'", jsonSchemaPath);
					}
				}
			} else if (validatorData.isJsonObject()) {
				extendedSchemas.add(new JsonSchema((JsonObject) validatorData, jsonSchemaDependencyResolver));
			} else {
				throw new JsonSchemaDefinitionError("Extended JSON schema value is not a 'JsonArray' or 'JsonObject'", jsonSchemaPath);
			}
		} catch (final JsonSchemaDefinitionError e) {
			throw e;
		} catch (final Exception e) {
			throw new JsonSchemaDefinitionError("Error '" + e.getClass().getSimpleName() + "' while resolving JSON schema reference '" + validatorData + "': " + e.getMessage(), jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		for (final JsonSchema jsonSchema : extendedSchemas) {
			try {
				jsonSchema.validate(jsonNode);
				return;
			} catch (@SuppressWarnings("unused") final JsonSchemaDataValidationError e) {
				// do nothing
			}
		}
		throw new JsonSchemaDataValidationError("Extended JSON schemas do not match", jsonPath);
	}
}
