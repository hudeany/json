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
 * JSON schema validator for the draft v3/v4 "extends" keyword.<br />
 * The data must be valid against EVERY schema listed in "extends" (same semantics as "allOf" in later drafts), not
 * just against at least one of them.<br />
 * <br />
 * Note: like {@link IfThenElseValidator}, this builds sub-validators via {@link JsonSchema#createValidators} and
 * invokes them directly with the actual JSON data path, instead of wrapping each extended schema into a separate
 * {@link JsonSchema} instance and calling its single-argument {@code validate(JsonNode)} (which mutates the given
 * data node's root flag in place and discards the real JSON data path).
 */
public class ExtendsValidator extends BaseJsonSchemaValidator {
	private final List<List<BaseJsonSchemaValidator>> extendedSchemaValidators = new ArrayList<>();

	public ExtendsValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		try {
			if (validatorData == null || validatorData.isNull()) {
				throw new JsonSchemaDefinitionError("Extended JSON schema value is 'null'", jsonSchemaPath);
			} else if (validatorData.isJsonArray()) {
				int index = 0;
				for (final JsonNode subSchemaObject : ((JsonArray) validatorData).items()) {
					if (subSchemaObject == null) {
						throw new JsonSchemaDefinitionError("Extended JSON schema value is 'null'", jsonSchemaPath);
					} else if (subSchemaObject.isJsonObject()) {
						extendedSchemaValidators.add(JsonSchema.createValidators((JsonObject) subSchemaObject, jsonSchemaDependencyResolver, new JsonSchemaPath(jsonSchemaPath).addArrayIndex(index)));
					} else {
						throw new JsonSchemaDefinitionError("Extended JSON schema value is not 'JsonObject'", jsonSchemaPath);
					}
					index++;
				}
			} else if (validatorData.isJsonObject()) {
				extendedSchemaValidators.add(JsonSchema.createValidators((JsonObject) validatorData, jsonSchemaDependencyResolver, new JsonSchemaPath(jsonSchemaPath)));
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
		for (final List<BaseJsonSchemaValidator> subValidators : extendedSchemaValidators) {
			for (final BaseJsonSchemaValidator subValidator : subValidators) {
				subValidator.validate(jsonNode, jsonPath);
			}
		}
	}
}
