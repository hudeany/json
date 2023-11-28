package de.soderer.utilities.json.schema.validator;

import java.util.List;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchema;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

/**
 * JSON schema validator for external references in files and urls by code key name "$ref"
 */
public class ReferenceValidator extends BaseJsonSchemaValidator {
	public ReferenceValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		try {
			if (validatorData == null) {
				throw new JsonSchemaDefinitionError("Reference key is 'null'", jsonSchemaPath);
			} else if (!(validatorData instanceof String)) {
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
		List<BaseJsonSchemaValidator> subValidators;
		try {
			final JsonObject dereferencedValue = jsonSchemaDependencyResolver.getDependencyByReference((String) validatorData, jsonSchemaPath);
			if (dereferencedValue == null) {
				throw new JsonSchemaDefinitionError("Invalid JSON schema reference data type for key '" + validatorData + "'. Expected 'object' but was 'null'", jsonSchemaPath);
			} else {
				jsonSchemaPath = new JsonSchemaPath((String) validatorData);
				subValidators = JsonSchema.createValidators(dereferencedValue, jsonSchemaDependencyResolver, jsonSchemaPath);
			}
		} catch (final JsonSchemaDefinitionError e) {
			throw new JsonSchemaDataValidationError("JsonSchemaDefinitionError while using JSON schema reference: " + e.getMessage(), jsonPath, e);
		} catch (final Exception e) {
			throw new JsonSchemaDataValidationError("JsonSchemaDefinitionError while using JSON schema reference: " + e.getMessage(), jsonPath, e);
		}

		try {
			for (final BaseJsonSchemaValidator subValidator : subValidators) {
				subValidator.validate(jsonNode, jsonPath);
			}
		} catch (@SuppressWarnings("unused") final StackOverflowError e) {
			throw new JsonSchemaDataValidationError("Cyclic reference detected", jsonPath);
		}
	}
}
