package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;
import de.soderer.utilities.json.utilities.NumberUtilities;

/**
 * A list of values that are allowed
 */
public class ConstValidator extends BaseJsonSchemaValidator {
	private final Object validatorDataInclusiveNull;
	public ConstValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, "");
		// BaseJsonSchemaValidator does not allow "null" values in validatorData
		validatorDataInclusiveNull = validatorData;
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (validatorDataInclusiveNull == null && jsonNode.isNull()) {
			return;
		} else if (jsonNode.isBoolean() && validatorDataInclusiveNull instanceof Boolean && ((Boolean) jsonNode.getValue()) == ((Boolean) validatorDataInclusiveNull)) {
			return;
		} else if (jsonNode.isString() && validatorDataInclusiveNull instanceof String && ((String) jsonNode.getValue()).equals(validatorDataInclusiveNull)) {
			return;
		} else if (jsonNode.isInteger() && validatorDataInclusiveNull instanceof Integer && ((Integer) jsonNode.getValue()) == ((Integer) validatorDataInclusiveNull)) {
			return;
		} else if (jsonNode.isInteger() && validatorDataInclusiveNull instanceof Long && ((Long) jsonNode.getValue()) == ((Long) validatorDataInclusiveNull)) {
			return;
		} else if (jsonNode.isNumber() && validatorDataInclusiveNull instanceof Number && NumberUtilities.compare(((Number) jsonNode.getValue()), ((Number) validatorDataInclusiveNull)) == 0) {
			return;
		} else if (jsonNode.isJsonObject() && validatorDataInclusiveNull instanceof JsonObject && ((JsonObject) jsonNode.getValue()).equals(validatorDataInclusiveNull)) {
			return;
		} else if (jsonNode.isJsonArray() && validatorDataInclusiveNull instanceof JsonArray && ((JsonArray) jsonNode.getValue()).equals(validatorDataInclusiveNull)) {
			return;
		} else {
			throw new JsonSchemaDataValidationError("Const expected '" + (validatorDataInclusiveNull == null ? null : validatorDataInclusiveNull) + "' but was '" + (jsonNode.getValue() == null ? null : jsonNode.getValue()) + "'", jsonPath);
		}
	}
}
