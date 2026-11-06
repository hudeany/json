package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaEvaluationContext;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.schema.JsonSchemaVersion;

/**
 * Validator for the "items" keyword, validating the items of a JSON data array.<br />
 * <br />
 * Up to draft 2019-09:
 * <ul>
 * <li>A single schema (object or boolean) defines a schema all items must match.</li>
 * <li>An array of schemas defines a schema for each item at the same index position. The optional
 * sibling keyword "additionalItems" then defines whether more items are allowed (boolean) or a
 * schema all further items must match (object).</li>
 * </ul>
 * Since draft 2020-12 the value is a single schema (object or boolean), which all items after the
 * ones covered by the sibling keyword "prefixItems" must match. "additionalItems" is no keyword
 * anymore.<br />
 * <br />
 * Data that is not an array is ignored, except in simple mode.
 */
public class ItemsValidator extends ExtendedBaseJsonSchemaValidator {
	/** Validators all items must match, or null if not defined. */
	private List<BaseJsonSchemaValidator> singleValidatorPack = null;

	/** Validators for the items at each index position, or null if not defined. */
	private List<List<BaseJsonSchemaValidator>> indexedValidatorPacks = null;

	/** Whether items beyond the indexed schemas are allowed, or null if not defined. */
	private Boolean additionalItemsAllowed = null;

	/** Validators all items beyond the indexed schemas must match, or null if not defined. */
	private List<BaseJsonSchemaValidator> additionalItemsDefinitions = null;

	/** Whether "items" is a single schema, so it applies to all (remaining) items. */
	private boolean singleSchema = false;

	/** Number of leading items covered by "prefixItems" (draft 2020-12 only). */
	private int prefixItemsCount = 0;

	/**
	 * Creates a new "items" validator.
	 *
	 * @param parentValidatorData
	 *            the parent schema containing this keyword, used to read its "additionalItems"
	 *            and "prefixItems"
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, a schema object, an array of schemas or a boolean
	 * @throws JsonSchemaDefinitionError
	 *             if the value or the related keywords "additionalItems" and "prefixItems" are
	 *             invalid
	 * @throws DuplicateKeyException
	 *             if a schema contains duplicate keys
	 */
	public ItemsValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Items data is 'null'", jsonSchemaPath);
		} else if (jsonSchemaDependencyResolver.isVersionAtLeast(JsonSchemaVersion.draft2020_12)) {
			// Draft 2020-12: single schema for all items after "prefixItems"
			if (!validatorData.isJsonObject() && !validatorData.isBoolean()) {
				throw new JsonSchemaDefinitionError("Items data is neither 'object' nor 'boolean'", jsonSchemaPath);
			}
			singleSchema = true;
			singleValidatorPack = JsonSchema.createValidatorsForSchemaNode(validatorData, jsonSchemaDependencyResolver, jsonSchemaPath);
			final JsonNode prefixItems = parentValidatorData.get("prefixItems");
			if (prefixItems != null && prefixItems.isJsonArray()) {
				prefixItemsCount = ((JsonArray) prefixItems).size();
			}
		} else if (validatorData.isJsonObject()) {
			singleSchema = true;
			if (((JsonObject) validatorData).size() > 0) {
				singleValidatorPack = JsonSchema.createValidators((JsonObject) validatorData, jsonSchemaDependencyResolver, jsonSchemaPath);

				if (parentValidatorData.containsKey("additionalItems") && jsonSchemaDependencyResolver.isSimpleMode()) {
					throw new JsonSchemaDefinitionError("'additionalItems' is only allowed for 'items' with 'array' data value", jsonSchemaPath);
				}
			}
		} else if (validatorData.isJsonArray()) {
			indexedValidatorPacks = createIndexedValidatorPacks((JsonArray) validatorData, jsonSchemaPath);

			if (parentValidatorData.containsKey("additionalItems")) {
				final JsonNode additionalItemsRaw = parentValidatorData.get("additionalItems");
				if (additionalItemsRaw == null) {
					throw new JsonSchemaDefinitionError("Property 'additionalItems' is 'null'", jsonSchemaPath);
				} else if (additionalItemsRaw.isBoolean()) {
					additionalItemsAllowed = ((JsonValueBoolean) additionalItemsRaw).getValue();
				} else if (additionalItemsRaw.isJsonObject()) {
					additionalItemsDefinitions = JsonSchema.createValidators((JsonObject) additionalItemsRaw, jsonSchemaDependencyResolver, jsonSchemaPath);
				} else {
					throw new JsonSchemaDefinitionError("AdditionalItems data is not a 'boolean' or 'object'", jsonSchemaPath);
				}
			}
		} else if (validatorData.isBoolean()) {
			// Special boolean value for "items" in newer drafts which replaces "additionalItems".
			// It comes with "prefixItems", which define the schemas of the leading items.
			additionalItemsAllowed = ((JsonValueBoolean) validatorData).getValue();
			final JsonNode prefixItems = parentValidatorData.get("prefixItems");
			if (prefixItems != null) {
				if (!(prefixItems.isJsonArray())) {
					throw new JsonSchemaDefinitionError("'prefixItems' data is not an 'array'", jsonSchemaPath);
				} else {
					indexedValidatorPacks = createIndexedValidatorPacks((JsonArray) prefixItems, jsonSchemaPath);
				}
			}
		} else {
			throw new JsonSchemaDefinitionError("Items data is not an 'object', 'array' or 'boolean'", jsonSchemaPath);
		}
	}

	/**
	 * Creates the validators for each index position of an array of schemas.
	 *
	 * @param schemaArray
	 *            the array of schema objects or booleans
	 * @param arraySchemaPath
	 *            the path of the array within the JSON schema
	 * @return the validators for each index position
	 * @throws JsonSchemaDefinitionError
	 *             if an item is neither object nor boolean, or a schema is invalid
	 * @throws DuplicateKeyException
	 *             if a schema contains duplicate keys
	 */
	private List<List<BaseJsonSchemaValidator>> createIndexedValidatorPacks(final JsonArray schemaArray, final JsonSchemaPath arraySchemaPath) throws JsonSchemaDefinitionError, DuplicateKeyException {
		final List<List<BaseJsonSchemaValidator>> validatorPacks = new ArrayList<>();
		for (int i = 0; i < schemaArray.size(); i++) {
			final JsonNode itemSchema = schemaArray.get(i);
			final JsonSchemaPath itemSchemaPath = new JsonSchemaPath(arraySchemaPath).addArrayIndex(i);
			if (itemSchema.isJsonObject()) {
				validatorPacks.add(JsonSchema.createValidators((JsonObject) itemSchema, jsonSchemaDependencyResolver, itemSchemaPath));
			} else if (itemSchema.isBoolean()) {
				final List<BaseJsonSchemaValidator> validators = new ArrayList<>();
				validators.add(new BooleanValidator(jsonSchemaDependencyResolver, itemSchemaPath, itemSchema));
				validatorPacks.add(validators);
			} else {
				throw new JsonSchemaDefinitionError("Items data item is neither 'object' nor 'boolean'", itemSchemaPath);
			}
		}
		return validatorPacks;
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonArray())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'array' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
			return;
		}

		final JsonArray jsonArray = (JsonArray) jsonNode;
		final JsonSchemaEvaluationContext context = jsonSchemaDependencyResolver.getEvaluationContext();
		boolean valid = true;
		if (singleSchema) {
			if (singleValidatorPack != null) {
				for (int i = prefixItemsCount; i < jsonArray.size(); i++) {
					valid = validateItem(jsonArray, i, singleValidatorPack, jsonPath) && valid;
				}
			}
			// Annotation for "unevaluatedItems" (draft 2019-09 and newer)
			context.markAllItemsEvaluated();
		} else {
			// Without indexed schemas all items are additional items
			final int indexedItemsCount = indexedValidatorPacks == null ? 0 : indexedValidatorPacks.size();
			for (int i = 0; i < indexedItemsCount && i < jsonArray.size(); i++) {
				valid = validateItem(jsonArray, i, indexedValidatorPacks.get(i), jsonPath) && valid;
				context.markItemEvaluated(i);
			}

			if (additionalItemsAllowed != null) {
				if (!additionalItemsAllowed && jsonArray.size() > indexedItemsCount) {
					throw new JsonSchemaDataValidationError("Maximum amount of array items is " + indexedItemsCount + " but was " + jsonArray.size(), jsonPath);
				}
				context.markAllItemsEvaluated();
			} else if (additionalItemsDefinitions != null) {
				for (int i = indexedItemsCount; i < jsonArray.size(); i++) {
					valid = validateItem(jsonArray, i, additionalItemsDefinitions, jsonPath) && valid;
				}
				context.markAllItemsEvaluated();
			}
		}
		if (!valid) {
			throw JsonSchemaDataValidationError.createCollectedMarker(jsonPath);
		}
	}

	/**
	 * Validates one item of a JSON data array.
	 *
	 * @param jsonArray
	 *            the JSON data array
	 * @param index
	 *            the index of the item
	 * @param validators
	 *            the validators the item must match
	 * @param jsonPath
	 *            the path of the JSON data array, used in error messages
	 * @return true if the item is valid, false if its errors were collected (see
	 *         {@link BaseJsonSchemaValidator#applyValidators(List, JsonNode, JsonPath)})
	 * @throws JsonSchemaDataValidationError
	 *             if the item is not valid and errors are not collected
	 */
	static boolean validateItem(final JsonArray jsonArray, final int index, final List<BaseJsonSchemaValidator> validators, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		final JsonNode jsonNodeToCheck;
		try {
			jsonNodeToCheck = jsonArray.get(index).withRootNode(false);
		} catch (final Exception e) {
			throw new JsonSchemaDataValidationError("Invalid data type '" + jsonArray.get(index).getClass().getSimpleName() + "'", new JsonPath(jsonPath).addArrayIndex(index), e);
		}
		return applyValidators(validators, jsonNodeToCheck, new JsonPath(jsonPath).addArrayIndex(index));
	}
}
