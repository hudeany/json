package de.soderer.json.schema.validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
 * Validator for the "dependentSchemas" keyword (since JSON schema draft 2019-09): its value is an
 * object mapping property names to schemas. If the JSON data object contains such a property, the
 * whole data object must match the schema. Data that is not an object is ignored.
 */
public class DependentSchemasValidator extends BaseJsonSchemaValidator {
	/** Schema validators by the property name they depend on. */
	private final Map<String, List<BaseJsonSchemaValidator>> dependentValidators = new LinkedHashMap<>();

	/**
	 * Creates a new "dependentSchemas" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, an object of schemas
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not an object of schemas
	 * @throws DuplicateKeyException
	 *             if a schema contains duplicate keys
	 */
	public DependentSchemasValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (!validatorData.isJsonObject()) {
			throw new JsonSchemaDefinitionError("DependentSchemas value is not an 'object'", jsonSchemaPath);
		}
		for (final Entry<String, JsonNode> entry : ((JsonObject) validatorData).entrySet()) {
			dependentValidators.put(entry.getKey(), JsonSchema.createValidatorsForSchemaNode(entry.getValue(), jsonSchemaDependencyResolver, new JsonSchemaPath(jsonSchemaPath).addPropertyKey(entry.getKey())));
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (jsonNode.isJsonObject()) {
			for (final Entry<String, List<BaseJsonSchemaValidator>> dependentValidatorEntry : dependentValidators.entrySet()) {
				if (((JsonObject) jsonNode).containsKey(dependentValidatorEntry.getKey())) {
					final JsonSchemaDataValidationError subSchemaError = getSubSchemaValidationError(dependentValidatorEntry.getValue(), jsonNode, jsonPath);
					if (subSchemaError != null) {
						throw new JsonSchemaDataValidationError("Dependent schema for existing key '" + dependentValidatorEntry.getKey() + "' did not apply to JsonNode", jsonPath, subSchemaError);
					}
				}
			}
		}
	}
}
