package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Validator for the "anyOf" keyword: its value is a non-empty array of schemas, at least one of
 * which must validate the JSON data node. Boolean schemas (true, false) are allowed as array items.
 */
public class AnyOfValidator extends BaseJsonSchemaValidator {
	/** Validators of each sub schema of the "anyOf" array. */
	private List<List<BaseJsonSchemaValidator>> subValidatorPackages = null;

	/**
	 * Creates a new "anyOf" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, an array of schemas
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a non-empty array of objects or booleans, or a sub schema is
	 *             invalid
	 * @throws DuplicateKeyException
	 *             if a sub schema contains duplicate keys
	 */
	public AnyOfValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("AnyOf array is 'null'", jsonSchemaPath);
		} else if (validatorData.isJsonArray()) {
			subValidatorPackages = new ArrayList<>();
			for (int i = 0; i < ((JsonArray) validatorData).size(); i++) {
				final JsonNode subValidationData = ((JsonArray) validatorData).get(i);
				final JsonSchemaPath itemSchemaPath = new JsonSchemaPath(jsonSchemaPath).addArrayIndex(i);
				if (subValidationData.isBoolean()) {
					final List<BaseJsonSchemaValidator> subValidators = new ArrayList<>();
					subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, itemSchemaPath, subValidationData));
					subValidatorPackages.add(subValidators);
				} else if (subValidationData.isJsonObject()) {
					subValidatorPackages.add(JsonSchema.createValidators((JsonObject) subValidationData, jsonSchemaDependencyResolver, itemSchemaPath));
				} else {
					throw new JsonSchemaDefinitionError("AnyOf array contains an item that is neither 'object' nor 'boolean'", itemSchemaPath);
				}
			}
			if (subValidatorPackages.isEmpty()) {
				throw new JsonSchemaDefinitionError("AnyOf array is empty", jsonSchemaPath);
			}
		} else {
			throw new JsonSchemaDefinitionError("AnyOf property does not have an array value", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		final List<JsonSchemaDataValidationError> subSchemaErrors = new ArrayList<>();
		for (final List<BaseJsonSchemaValidator> subValidatorPackage : subValidatorPackages) {
			final JsonSchemaDataValidationError subSchemaError = getSubSchemaValidationError(subValidatorPackage, jsonNode, jsonPath);
			if (subSchemaError == null) {
				return;
			} else {
				subSchemaErrors.add(subSchemaError);
			}
		}

		// Keep the errors of all sub schemas for diagnosis: the first one as cause, the others as suppressed
		final JsonSchemaDataValidationError error = new JsonSchemaDataValidationError("No option of 'anyOf' property did apply to JsonNode", jsonPath, subSchemaErrors.get(0));
		for (int i = 1; i < subSchemaErrors.size(); i++) {
			error.addSuppressed(subSchemaErrors.get(i));
		}
		throw error;
	}
}
