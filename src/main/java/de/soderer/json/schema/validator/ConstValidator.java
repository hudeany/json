package de.soderer.json.schema.validator;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.utilities.NumberUtilities;

/**
 * A list of values that are allowed
 */
public class ConstValidator extends BaseJsonSchemaValidator {
	private final JsonNode validatorDataInclusiveNull;

	public ConstValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, new JsonValueString(""));
		// BaseJsonSchemaValidator does not allow "null" values in validatorData
		validatorDataInclusiveNull = validatorData;
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (validatorDataInclusiveNull == null && jsonNode.isNull()) {
			return;
		} else if (jsonNode.isBoolean() && validatorDataInclusiveNull.isBoolean() && (((JsonValueBoolean) jsonNode).getValue()) == ((JsonValueBoolean) validatorDataInclusiveNull).getValue()) {
			return;
		} else if (jsonNode.isString() && validatorDataInclusiveNull.isString() && (((JsonValueString) jsonNode).getValue()).equals(((JsonValueString) validatorDataInclusiveNull).getValue())) {
			return;
		} else if (jsonNode.isInteger() && validatorDataInclusiveNull.isInteger() && NumberUtilities.compare((((JsonValueInteger) jsonNode).getValue()), (((JsonValueInteger) validatorDataInclusiveNull).getValue())) == 0) {
			return;
		} else if (jsonNode.isNumber() && validatorDataInclusiveNull.isNumber() && NumberUtilities.compare((((JsonValueNumber) jsonNode).getValue()), (((JsonValueNumber) validatorDataInclusiveNull).getValue())) == 0) {
			return;
		} else if (jsonNode.isJsonObject() && validatorDataInclusiveNull.isJsonObject() && ((JsonObject) jsonNode).equals(validatorDataInclusiveNull)) {
			return;
		} else if (jsonNode.isJsonArray() && validatorDataInclusiveNull.isJsonArray() && ((JsonArray) jsonNode).equals(validatorDataInclusiveNull)) {
			return;
		} else {
			throw new JsonSchemaDataValidationError("Const expected '" + (validatorDataInclusiveNull == null ? null : validatorDataInclusiveNull) + "' but was '" + (jsonNode.isNull() ? null : jsonNode) + "'", jsonPath);
		}
	}
}
