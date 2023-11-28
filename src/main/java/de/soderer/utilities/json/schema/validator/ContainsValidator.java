package de.soderer.utilities.json.schema.validator;

import java.util.ArrayList;
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

/**
 * The contains value is a schema, that needs to validate against one or more items in the JSON data array
 */
public class ContainsValidator extends ExtendedBaseJsonSchemaValidator {
	final List<BaseJsonSchemaValidator> subValidators;

	public ContainsValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData instanceof Boolean) {
			subValidators = new ArrayList<>();
			subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData));
		} else if (validatorData instanceof JsonObject) {
			try {
				subValidators = JsonSchema.createValidators((JsonObject) validatorData, jsonSchemaDependencyResolver, jsonSchemaPath);
			} catch (final JsonSchemaDefinitionError e) {
				throw new JsonSchemaDefinitionError("Contains data JSON schema is invalid: " + e.getMessage(), jsonSchemaPath, e);
			}
		} else {
			throw new JsonSchemaDefinitionError("Contains data is not an 'object'", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonArray())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'array' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			for (final Object itemObject : (JsonArray) jsonNode.getValue()) {
				JsonNode newJsonNode;
				try {
					newJsonNode = new JsonNode(itemObject);
				} catch (final Exception e) {
					throw new JsonSchemaDataValidationError("Invalid data type '" + itemObject.getClass().getSimpleName() + "'", jsonPath, e);
				}
				if (validateSubSchema(subValidators, newJsonNode, jsonPath)) {
					return;
				}
			}

			throw new JsonSchemaDataValidationError("Array does not contain expected item", jsonPath);
		}
	}
}
