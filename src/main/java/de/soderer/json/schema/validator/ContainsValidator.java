package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonArray;
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
 * Validator for the "contains" keyword (since JSON schema draft 6): its value is a schema, which
 * must validate at least one item of the JSON data array. Data that is not an array is ignored,
 * except in simple mode.
 */
public class ContainsValidator extends ExtendedBaseJsonSchemaValidator {
	/** Validators of the schema at least one array item must match. */
	private final List<BaseJsonSchemaValidator> subValidators;

	/**
	 * Creates a new "contains" validator.
	 *
	 * @param parentValidatorData
	 *            the parent schema containing this keyword
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
	public ContainsValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData.isBoolean()) {
			subValidators = new ArrayList<>();
			subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData));
		} else if (validatorData.isJsonObject()) {
			try {
				subValidators = JsonSchema.createValidators((JsonObject) validatorData, jsonSchemaDependencyResolver, jsonSchemaPath);
			} catch (final JsonSchemaDefinitionError e) {
				throw new JsonSchemaDefinitionError("Contains data JSON schema is invalid: " + e.getMessage(), jsonSchemaPath, e);
			}
		} else {
			throw new JsonSchemaDefinitionError("Contains data is neither 'object' nor 'boolean'", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonArray())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'array' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			int itemIndex = 0;
			for (final JsonNode itemObject : ((JsonArray) jsonNode).items()) {
				final JsonPath itemJsonPath = new JsonPath(jsonPath).addArrayIndex(itemIndex);
				JsonNode newJsonNode;
				try {
					newJsonNode = itemObject.withRootNode(false);
				} catch (final Exception e) {
					throw new JsonSchemaDataValidationError("Invalid data type '" + itemObject.getClass().getSimpleName() + "'", itemJsonPath, e);
				}
				if (validateSubSchema(subValidators, newJsonNode, itemJsonPath)) {
					return;
				}
				itemIndex++;
			}

			throw new JsonSchemaDataValidationError("Array does not contain expected item", jsonPath);
		}
	}
}
