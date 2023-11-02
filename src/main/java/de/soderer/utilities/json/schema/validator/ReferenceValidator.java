package de.soderer.utilities.json.schema.validator;

import java.util.List;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchema;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

public class ReferenceValidator extends BaseJsonSchemaValidator {
	private JsonObject dereferencedSchemaObject;

	public ReferenceValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData, final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData, jsonNode, jsonPath);

		try {
			if (validatorData == null) {
				throw new JsonSchemaDefinitionError("Reference key is 'null'", jsonSchemaPath);
			} else if (!(validatorData instanceof String)) {
				throw new JsonSchemaDefinitionError("Reference key is not a 'string'", jsonSchemaPath);
			} else if (jsonSchemaDependencyResolver == null) {
				throw new JsonSchemaDefinitionError("JSON schema reference definitions is empty. Cannot dereference key '" + validatorData + "'", jsonSchemaPath);
			} else {
				jsonSchemaDependencyResolver.checkCyclicDependency(jsonPath, (String) validatorData, jsonSchemaPath);

				final JsonObject dereferencedValue = jsonSchemaDependencyResolver.getDependencyByReference((String) validatorData, jsonSchemaPath);
				if (dereferencedValue == null) {
					throw new JsonSchemaDefinitionError("Invalid JSON schema reference data type for key '" + validatorData + "'. Expected 'object' but was 'null'", jsonSchemaPath);
				} else {
					dereferencedSchemaObject = dereferencedValue;
					this.jsonSchemaPath = new JsonSchemaPath((String) validatorData);
				}
			}
		} catch (final JsonSchemaDefinitionError e) {
			throw e;
		} catch (final Exception e) {
			throw new JsonSchemaDefinitionError("Error '" + e.getClass().getSimpleName() + "' while resolving JSON schema reference '" + validatorData + "': " + e.getMessage(), jsonSchemaPath);
		}
	}

	@Override
	public void validate() throws Exception {
		if (dereferencedSchemaObject == null) {
			throw new JsonSchemaDefinitionError("Invalid JSON schema reference data type for key '" + validatorData + "'. Expected 'object' but was 'null'", jsonSchemaPath);
		}

		final List<BaseJsonSchemaValidator> subValidators = JsonSchema.createValidators(dereferencedSchemaObject, jsonSchemaDependencyResolver, jsonSchemaPath, jsonNode, jsonPath);
		for (final BaseJsonSchemaValidator subValidator : subValidators) {
			subValidator.validate();
		}
	}
}
