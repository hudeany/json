package de.soderer.json.schema.validator;

import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaEvaluationContext;
import de.soderer.json.schema.JsonSchemaValidationCollector;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Validator for the "unevaluatedItems" keyword (since JSON schema draft 2019-09): its value is a
 * schema, which every item of the JSON data array must match, that was not evaluated successfully
 * by any other keyword of the same schema object or its in-place sub schemas ("prefixItems",
 * "items", "additionalItems", "contains" (since 2020-12), "allOf", "$ref" ...). Data that is not an
 * array is ignored.<br />
 * <br />
 * It must be applied after all other keywords of its schema object.
 */
public class UnevaluatedItemsValidator extends BaseJsonSchemaValidator {
	/** Validators of the schema unevaluated items must match. */
	private final List<BaseJsonSchemaValidator> subValidators;

	/**
	 * Creates a new "unevaluatedItems" validator.
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
	public UnevaluatedItemsValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		subValidators = JsonSchema.createValidatorsForSchemaNode(validatorData, jsonSchemaDependencyResolver, jsonSchemaPath);
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (jsonNode.isJsonArray()) {
			final JsonSchemaEvaluationContext context = jsonSchemaDependencyResolver.getEvaluationContext();
			final JsonArray jsonArray = (JsonArray) jsonNode;
			final boolean collecting = isCollectingErrors();
			boolean valid = true;
			for (int i = 0; i < jsonArray.size(); i++) {
				if (!context.isItemEvaluated(i)) {
					final JsonPath itemPath = new JsonPath(jsonPath).addArrayIndex(i);
					final JsonSchemaDataValidationError subSchemaError = getSubSchemaValidationError(subValidators, jsonArray.get(i), itemPath);
					if (subSchemaError != null) {
						final JsonSchemaDataValidationError error = new JsonSchemaDataValidationError("Unevaluated array item at index " + i + " is not valid", itemPath, subSchemaError);
						if (!collecting) {
							throw error;
						}
						error.setJsonSchemaPathIfMissing(jsonSchemaPath);
						JsonSchemaValidationCollector.getActive().addError(error);
						valid = false;
					}
				}
			}
			context.markAllItemsEvaluated();
			if (!valid) {
				throw JsonSchemaDataValidationError.createCollectedMarker(jsonPath);
			}
		}
	}
}
