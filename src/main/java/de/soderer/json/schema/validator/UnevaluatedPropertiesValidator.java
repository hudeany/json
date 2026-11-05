package de.soderer.json.schema.validator;

import java.util.List;
import java.util.Map.Entry;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaEvaluationContext;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Validator for the "unevaluatedProperties" keyword (since JSON schema draft 2019-09): its value is
 * a schema, which every property of the JSON data object must match, that was not evaluated
 * successfully by any other keyword of the same schema object or its in-place sub schemas
 * ("properties", "patternProperties", "additionalProperties", "allOf", "$ref" ...). Data that is not
 * an object is ignored.<br />
 * <br />
 * It must be applied after all other keywords of its schema object.
 */
public class UnevaluatedPropertiesValidator extends BaseJsonSchemaValidator {
	/** Validators of the schema unevaluated properties must match. */
	private final List<BaseJsonSchemaValidator> subValidators;

	/**
	 * Creates a new "unevaluatedProperties" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, a schema object or a boolean
	 * @throws JsonSchemaDefinitionError
	 *             if the value is neither object nor boolean, or the schema is invalid
	 * @throws DuplicateKeyException
	 *             if the schema contains duplicate keys
	 */
	public UnevaluatedPropertiesValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		subValidators = JsonSchema.createValidatorsForSchemaNode(validatorData, jsonSchemaDependencyResolver, jsonSchemaPath);
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (jsonNode.isJsonObject()) {
			final JsonSchemaEvaluationContext context = jsonSchemaDependencyResolver.getEvaluationContext();
			for (final Entry<String, JsonNode> propertyEntry : ((JsonObject) jsonNode).entrySet()) {
				if (!context.isPropertyEvaluated(propertyEntry.getKey())) {
					final JsonPath propertyPath = new JsonPath(jsonPath).addPropertyKey(propertyEntry.getKey());
					final JsonSchemaDataValidationError subSchemaError = getSubSchemaValidationError(subValidators, propertyEntry.getValue(), propertyPath);
					if (subSchemaError != null) {
						throw new JsonSchemaDataValidationError("Unevaluated property '" + propertyEntry.getKey() + "' is not valid", propertyPath, subSchemaError);
					}
				}
			}
			for (final String propertyName : ((JsonObject) jsonNode).keySet()) {
				context.markPropertyEvaluated(propertyName);
			}
		}
	}
}
