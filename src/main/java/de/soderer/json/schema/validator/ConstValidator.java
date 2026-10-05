package de.soderer.json.schema.validator;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.utilities.NumberUtilities;

/**
 * Validator for the "const" keyword (since JSON schema draft 6): the JSON data node must equal the
 * given value. Integer and number values are compared numerically, so 1 equals 1.0. The value may
 * also be null.
 */
public class ConstValidator extends BaseJsonSchemaValidator {
	/** The expected value, which may be null or a JSON null value (unlike the validator data). */
	private final JsonNode validatorDataInclusiveNull;

	/**
	 * Creates a new "const" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the expected value, may be null or a JSON null value
	 * @throws JsonSchemaDefinitionError
	 *             never thrown for this keyword, declared by the base class
	 */
	public ConstValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, new JsonValueString(""));
		// BaseJsonSchemaValidator does not allow "null" values in validatorData
		validatorDataInclusiveNull = validatorData;
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		// The expected null value may be given as Java null or as JSON null value
		final boolean constIsNull = validatorDataInclusiveNull == null || validatorDataInclusiveNull.isNull();
		if (constIsNull) {
			if (jsonNode.isNull()) {
				return;
			}
		} else if (jsonNode.isBoolean() && validatorDataInclusiveNull.isBoolean() && (boolean) ((JsonValueBoolean) jsonNode).getValue() == (boolean) ((JsonValueBoolean) validatorDataInclusiveNull).getValue()) {
			return;
		} else if (jsonNode.isString() && validatorDataInclusiveNull.isString() && (((JsonValueString) jsonNode).getValue()).equals(((JsonValueString) validatorDataInclusiveNull).getValue())) {
			return;
		} else if ((jsonNode.isInteger() || jsonNode.isNumber()) && (validatorDataInclusiveNull.isInteger() || validatorDataInclusiveNull.isNumber()) && NumberUtilities.compare(getNumberValue(jsonNode), getNumberValue(validatorDataInclusiveNull)) == 0) {
			return;
		} else if (jsonNode.isJsonObject() && validatorDataInclusiveNull.isJsonObject() && ((JsonObject) jsonNode).equals(validatorDataInclusiveNull)) {
			return;
		} else if (jsonNode.isJsonArray() && validatorDataInclusiveNull.isJsonArray() && ((JsonArray) jsonNode).equals(validatorDataInclusiveNull)) {
			return;
		}
		throw new JsonSchemaDataValidationError("Const expected '" + (constIsNull ? null : validatorDataInclusiveNull) + "' but was '" + (jsonNode.isNull() ? null : jsonNode) + "'", jsonPath);
	}

	/**
	 * Returns the numeric value of an integer or number JSON node.
	 *
	 * @param numericJsonNode
	 *            the integer or number JSON node
	 * @return the numeric value
	 */
	private static Number getNumberValue(final JsonNode numericJsonNode) {
		if (numericJsonNode.isInteger()) {
			return ((JsonValueInteger) numericJsonNode).getValue();
		} else {
			return ((JsonValueNumber) numericJsonNode).getValue();
		}
	}
}
