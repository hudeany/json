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
 * JSON schema validator for external references in files and urls by code key name "$ref"
 */
public class ReferenceValidator extends BaseJsonSchemaValidator {
	public ReferenceValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		try {
			if (validatorData == null || validatorData.isNull()) {
				throw new JsonSchemaDefinitionError("Reference key is 'null'", jsonSchemaPath);
			} else if (!(validatorData.isString())) {
				throw new JsonSchemaDefinitionError("Reference key is not a 'string'", jsonSchemaPath);
			} else if (jsonSchemaDependencyResolver == null) {
				throw new JsonSchemaDefinitionError("JSON schema reference definitions is empty. Cannot dereference key '" + validatorData + "'", jsonSchemaPath);
			}
		} catch (final JsonSchemaDefinitionError e) {
			throw e;
		} catch (final Exception e) {
			throw new JsonSchemaDefinitionError("Error '" + e.getClass().getSimpleName() + "' while resolving JSON schema reference '" + validatorData + "': " + e.getMessage(), jsonSchemaPath);
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
			throw new JsonSchemaDataValidationError("JsonSchemaDefinitionError while using JSON schema reference: " + e.getMessage(), jsonPath, e);
		}

		jsonSchemaDependencyResolver.putCachedReferenceValidators(referenceKey, subValidators);
		return subValidators;
	}
}
