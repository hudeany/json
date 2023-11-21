package de.soderer.utilities.json.schema.validator;

import java.util.List;

import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchema;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

public class ContainsValidator extends ExtendedBaseJsonSchemaValidator {
	final List<BaseJsonSchemaValidator> subValidators;

	public ContainsValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (!(validatorData instanceof JsonObject)) {
			throw new JsonSchemaDefinitionError("Contains data is not an 'object'", jsonSchemaPath);
		} else {
			try {
				subValidators = JsonSchema.createValidators((JsonObject) validatorData, jsonSchemaDependencyResolver, jsonSchemaPath);
			} catch (final JsonSchemaDefinitionError e) {
				throw new JsonSchemaDefinitionError("Contains data JSON schema is invalid: " + e.getMessage(), jsonSchemaPath, e);
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
			boolean foundMatchingItem = false;

			for (final Object itemObject : (JsonArray) jsonNode.getValue()) {
				JsonNode newJsonNode;
				try {
					newJsonNode = new JsonNode(itemObject);
				} catch (final Exception e) {
					throw new JsonSchemaDataValidationError("Invalid data type '" + itemObject.getClass().getSimpleName() + "'", jsonPath, e);
				}
				try {
					for (final BaseJsonSchemaValidator subValidator : subValidators) {
						subValidator.validate(newJsonNode, jsonPath);
					}
					foundMatchingItem = true;
					break;
				} catch (@SuppressWarnings("unused") final JsonSchemaDataValidationError e) {
					// Current item does not match the schema
				}
			}

			if (!foundMatchingItem) {
				throw new JsonSchemaDataValidationError("Array does not contain expected item", jsonPath);
			}
		}
	}
}
