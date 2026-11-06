package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Validator for the "prefixItems" keyword (since JSON schema draft 2020-12): its value is a
 * non-empty array of schemas. Each item of the JSON data array must match the schema at the same
 * index position. Further items are validated by the sibling keyword "items". Data that is not an
 * array is ignored.
 */
public class PrefixItemsValidator extends BaseJsonSchemaValidator {
	/** Validators for the items at each index position. */
	private final List<List<BaseJsonSchemaValidator>> indexedValidatorPacks = new ArrayList<>();

	/**
	 * Creates a new "prefixItems" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, an array of schemas
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not an array of schemas
	 * @throws DuplicateKeyException
	 *             if a schema contains duplicate keys
	 */
	public PrefixItemsValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (!validatorData.isJsonArray()) {
			throw new JsonSchemaDefinitionError("PrefixItems data is not an 'array'", jsonSchemaPath);
		}
		final JsonArray schemaArray = (JsonArray) validatorData;
		for (int i = 0; i < schemaArray.size(); i++) {
			indexedValidatorPacks.add(JsonSchema.createValidatorsForSchemaNode(schemaArray.get(i), jsonSchemaDependencyResolver, new JsonSchemaPath(jsonSchemaPath).addArrayIndex(i)));
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (jsonNode.isJsonArray()) {
			final JsonArray jsonArray = (JsonArray) jsonNode;
			boolean valid = true;
			for (int i = 0; i < indexedValidatorPacks.size() && i < jsonArray.size(); i++) {
				valid = ItemsValidator.validateItem(jsonArray, i, indexedValidatorPacks.get(i), jsonPath) && valid;
				jsonSchemaDependencyResolver.getEvaluationContext().markItemEvaluated(i);
			}
			if (!valid) {
				throw JsonSchemaDataValidationError.createCollectedMarker(jsonPath);
			}
		}
	}
}
