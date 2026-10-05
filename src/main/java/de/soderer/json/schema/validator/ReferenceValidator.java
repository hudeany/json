package de.soderer.json.schema.validator;

import java.util.List;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Validator for the "$ref" keyword: the JSON data node must match the referenced schema, which may
 * be part of the same schema (e.g. "#/definitions/address") or an external file or URL resolved by
 * the {@link JsonSchemaDependencyResolver}.<br />
 * <br />
 * The referenced schema is resolved lazily on first use and its validators are cached on the
 * resolver. A reference resolved again for the same JSON data path is reported as cyclic.
 */
public class ReferenceValidator extends BaseJsonSchemaValidator {
	/**
	 * Creates a new "$ref" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, the reference string
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a string or there is no dependency resolver
	 */
	public ReferenceValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Reference key is 'null'", jsonSchemaPath);
		} else if (!(validatorData.isString())) {
			throw new JsonSchemaDefinitionError("Reference key is not a 'string'", jsonSchemaPath);
		} else if (jsonSchemaDependencyResolver == null) {
			throw new JsonSchemaDefinitionError("JSON schema reference definitions is empty. Cannot dereference key '" + validatorData + "'", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		final String referenceKey = ((JsonValueString) validatorData).getValue();
		final String jsonPathString = jsonPath == null ? "" : jsonPath.toString();

		// Detect a genuine cycle: the same "$ref" being resolved again for the exact same JSON data path means the
		// schema keeps referring back to itself without ever descending into a different part of the data.
		if (jsonSchemaDependencyResolver.enterReferenceResolution(referenceKey, jsonPathString)) {
			throw new JsonSchemaDataValidationError("Cyclic reference detected for '" + referenceKey + "'", jsonPath);
		}
		try {
			final List<BaseJsonSchemaValidator> subValidators = getOrBuildSubValidators(referenceKey, jsonPath);
			for (final BaseJsonSchemaValidator subValidator : subValidators) {
				subValidator.validate(jsonNode, jsonPath);
			}
		} finally {
			jsonSchemaDependencyResolver.exitReferenceResolution(referenceKey, jsonPathString);
		}
	}

	/**
	 * Builds the sub-validators for this reference once and caches them on the shared
	 * {@link JsonSchemaDependencyResolver}, instead of rebuilding the whole sub-validator tree on every single data
	 * node that this "$ref" is applied to.
	 *
	 * @param referenceKey
	 *            the reference string
	 * @param jsonPath
	 *            the path of the JSON data node, used in error messages
	 * @return the validators of the referenced schema
	 * @throws JsonSchemaDataValidationError
	 *             if the reference cannot be resolved or the referenced schema is invalid
	 */
	private List<BaseJsonSchemaValidator> getOrBuildSubValidators(final String referenceKey, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		List<BaseJsonSchemaValidator> subValidators = jsonSchemaDependencyResolver.getCachedReferenceValidators(referenceKey);
		if (subValidators != null) {
			return subValidators;
		}

		try {
			final JsonObject dereferencedValue = jsonSchemaDependencyResolver.getDependencyByReference(referenceKey, jsonSchemaPath);
			if (dereferencedValue == null) {
				throw new JsonSchemaDefinitionError("Invalid JSON schema reference data type for key '" + validatorData + "'. Expected 'object' but was 'null'", jsonSchemaPath);
			} else {
				final JsonSchemaPath referenceSchemaPath = new JsonSchemaPath(referenceKey);
				subValidators = JsonSchema.createValidators(dereferencedValue, jsonSchemaDependencyResolver, referenceSchemaPath);
			}
		} catch (final JsonSchemaDefinitionError e) {
			throw new JsonSchemaDataValidationError("JsonSchemaDefinitionError while using JSON schema reference: " + e.getMessage(), jsonPath, e);
		} catch (final Exception e) {
			throw new JsonSchemaDataValidationError("Error '" + e.getClass().getSimpleName() + "' while using JSON schema reference: " + e.getMessage(), jsonPath, e);
		}

		jsonSchemaDependencyResolver.putCachedReferenceValidators(referenceKey, subValidators);
		return subValidators;
	}
}
