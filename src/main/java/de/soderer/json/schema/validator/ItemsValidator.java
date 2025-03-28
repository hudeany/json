package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Validates the items of a JSON array.
 * A single subschema defines a schema all items must match.
 * An array of subschemas define a schema for every indexed item of the JSON data matching the same index position.
 * Additional optional boolean attribute "additionalItems" defines whether or not there are more items allowed:
 * Additional optional subschema attribute "additionalItems" defines a schema all additional items must match.
 */
public class ItemsValidator extends ExtendedBaseJsonSchemaValidator {
	private List<BaseJsonSchemaValidator> singleValidatorPack = null;
	private List<List<BaseJsonSchemaValidator>> indexedValidatorPacks = null;
	private Boolean additionalItemsAllowed = null;
	private List<BaseJsonSchemaValidator> additionalItemsDefinitions = null;

	public ItemsValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Items data is 'null'", jsonSchemaPath);
		} else if (validatorData instanceof JsonObject) {
			if (((JsonObject) validatorData).size() == 0) {
				return;
			} else {
				singleValidatorPack = JsonSchema.createValidators((JsonObject) validatorData, jsonSchemaDependencyResolver, jsonSchemaPath);

				if (parentValidatorData.containsPropertyKey("additionalItems") && jsonSchemaDependencyResolver.isSimpleMode()) {
					throw new JsonSchemaDefinitionError("'additionalItems' is only allowed for 'items' with 'array' data value", jsonSchemaPath);
				}
			}
		} else if (validatorData instanceof JsonArray) {
			final JsonArray validatorDataArray = (JsonArray) validatorData;
			indexedValidatorPacks = new ArrayList<>();
			for (int i = 0; i < validatorDataArray.size(); i++) {
				final Object object = validatorDataArray.get(i);
				if (object instanceof JsonObject) {
					final JsonObject validatorObject = (JsonObject) object;

					final List<BaseJsonSchemaValidator> validators = JsonSchema.createValidators(validatorObject, jsonSchemaDependencyResolver, jsonSchemaPath);
					indexedValidatorPacks.add(validators);
				} else if (object instanceof Boolean) {
					final List<BaseJsonSchemaValidator> validators = new ArrayList<>();
					validators.add(new BooleanValidator(jsonSchemaDependencyResolver, jsonSchemaPath, object));
					indexedValidatorPacks.add(validators);
				} else {
					throw new JsonSchemaDefinitionError("Items data item is not an 'object'", jsonSchemaPath);
				}
			}

			if (parentValidatorData.containsPropertyKey("additionalItems")) {
				final Object additionalItemsRaw = parentValidatorData.get("additionalItems");
				if (additionalItemsRaw == null) {
					throw new JsonSchemaDefinitionError("Property 'additionalItems' is 'null'", jsonSchemaPath);
				} else if (additionalItemsRaw instanceof Boolean) {
					additionalItemsAllowed = (Boolean) additionalItemsRaw;
				} else if (additionalItemsRaw instanceof JsonObject) {
					additionalItemsDefinitions = JsonSchema.createValidators((JsonObject) additionalItemsRaw, jsonSchemaDependencyResolver, jsonSchemaPath);
				} else {
					throw new JsonSchemaDefinitionError("AdditionalItems data is not a 'boolean' or 'object'", jsonSchemaPath);
				}
			}
		} else {
			// Special boolean value for "items" in draft v7 which replaces "additionalItems".
			// It comes with "prefixItems" which is still part of the discussion.
			additionalItemsAllowed = (Boolean) validatorData;
			if (parentValidatorData.get("prefixItems") != null) {
				if (!(parentValidatorData.get("prefixItems") instanceof JsonArray)) {
					throw new JsonSchemaDefinitionError("'prefixItems' data is not a 'array'", jsonSchemaPath);
				} else {
					final JsonArray validatorDataArray = (JsonArray) parentValidatorData.get("prefixItems");
					indexedValidatorPacks = new ArrayList<>();
					for (int i = 0; i < validatorDataArray.size(); i++) {
						final Object object = validatorDataArray.get(i);
						if (!(object instanceof JsonObject)) {
							throw new JsonSchemaDefinitionError("Items data item is not an 'object'", jsonSchemaPath);
						}
						final JsonObject validatorObject = (JsonObject) object;

						final List<BaseJsonSchemaValidator> validators = JsonSchema.createValidators(validatorObject, jsonSchemaDependencyResolver, jsonSchemaPath);
						indexedValidatorPacks.add(validators);
					}
				}
			}
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonArray())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'array' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			if (singleValidatorPack != null) {
				for (int i = 0; i < ((JsonArray) jsonNode.getValue()).size(); i++) {
					JsonNode jsonNodeToCheck;
					try {
						jsonNodeToCheck = new JsonNode(((JsonArray) jsonNode.getValue()).get(i));
					} catch (final Exception e) {
						throw new JsonSchemaDataValidationError("Invalid data type '" + ((JsonArray) jsonNode.getValue()).get(i).getClass().getSimpleName() + "'", new JsonPath(jsonPath).addArrayIndex(i), e);
					}
					for (final BaseJsonSchemaValidator validator : singleValidatorPack) {
						validator.validate(jsonNodeToCheck, new JsonPath(jsonPath).addArrayIndex(i));
					}
				}
			} else if (indexedValidatorPacks != null) {
				for (int i = 0; i < indexedValidatorPacks.size() && i < ((JsonArray) jsonNode.getValue()).size(); i++) {
					JsonNode jsonNodeToCheck;
					try {
						jsonNodeToCheck = new JsonNode(((JsonArray) jsonNode.getValue()).get(i));
					} catch (final Exception e) {
						throw new JsonSchemaDataValidationError("Invalid data type '" + ((JsonArray) jsonNode.getValue()).get(i).getClass().getSimpleName() + "'", new JsonPath(jsonPath).addArrayIndex(i), e);
					}
					for (final BaseJsonSchemaValidator validator : indexedValidatorPacks.get(i)) {
						validator.validate(jsonNodeToCheck, new JsonPath(jsonPath).addArrayIndex(i));
					}
				}

				if (additionalItemsAllowed != null) {
					if (!additionalItemsAllowed) {
						if (((JsonArray) jsonNode.getValue()).size() > indexedValidatorPacks.size()) {
							throw new JsonSchemaDataValidationError("Maximum amount of array items is " + indexedValidatorPacks.size() + " but was " + ((JsonArray) jsonNode.getValue()).size(), jsonPath);
						}
					}
				} else if (additionalItemsDefinitions != null) {
					for (int i = indexedValidatorPacks.size(); i < ((JsonArray) jsonNode.getValue()).size(); i++) {
						JsonNode newJsonNode;
						try {
							newJsonNode = new JsonNode(((JsonArray) jsonNode.getValue()).get(i));
						} catch (final Exception e) {
							throw new JsonSchemaDataValidationError("Invalid data type '" + ((JsonArray) jsonNode.getValue()).get(i).getClass().getSimpleName() + "'", new JsonPath(jsonPath).addArrayIndex(i), e);
						}
						for (final BaseJsonSchemaValidator subValidator : additionalItemsDefinitions) {
							subValidator.validate(newJsonNode, new JsonPath(jsonPath).addArrayIndex(i));
						}
					}
				}
			} else {
				if (additionalItemsAllowed != null) {
					if (!additionalItemsAllowed && ((JsonArray) jsonNode.getValue()).size() > 0) {
						throw new JsonSchemaDataValidationError("Maximum amount of array items is 0 but was " + ((JsonArray) jsonNode.getValue()).size(), jsonPath);
					}
				} else if (additionalItemsDefinitions != null) {
					for (int i = indexedValidatorPacks.size(); i < ((JsonArray) jsonNode.getValue()).size(); i++) {
						JsonNode newJsonNode;
						try {
							newJsonNode = new JsonNode(((JsonArray) jsonNode.getValue()).get(i));
						} catch (final Exception e) {
							throw new JsonSchemaDataValidationError("Invalid data type '" + ((JsonArray) jsonNode.getValue()).get(i).getClass().getSimpleName() + "'", new JsonPath(jsonPath).addArrayIndex(i), e);
						}
						for (final BaseJsonSchemaValidator subValidator : additionalItemsDefinitions) {
							subValidator.validate(newJsonNode, new JsonPath(jsonPath).addArrayIndex(i));
						}
					}
				}
			}
		}
	}
}
