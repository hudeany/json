package de.soderer.json.schema.validator;

import java.util.List;

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
 * Base class of all JSON schema validators.
 * <p>
 * Each validator implements one keyword of a JSON schema (e.g. "allOf" or "additionalProperties").
 * It is created with the keyword's value from the schema definition and validates JSON data nodes
 * against it.
 * </p>
 */
public abstract class BaseJsonSchemaValidator {
	/** Resolver for references and settings of the JSON schema this validator belongs to. */
	protected JsonSchemaDependencyResolver jsonSchemaDependencyResolver;

	/** Path of this validator's keyword within the JSON schema, used in error messages. */
	protected JsonSchemaPath jsonSchemaPath;

	/** Value of this validator's keyword in the JSON schema definition. */
	protected JsonNode validatorData;

	/**
	 * Creates a new validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword in the JSON schema definition
	 * @throws JsonSchemaDefinitionError
	 *             if the validator data is null or a JSON null value
	 */
	protected BaseJsonSchemaValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("ValidatorData is 'null'", jsonSchemaPath);
		}

		this.jsonSchemaDependencyResolver = jsonSchemaDependencyResolver;
		this.jsonSchemaPath = jsonSchemaPath;
		this.validatorData = validatorData;
	}

	/**
	 * Checks whether a JSON data node is valid for all validators of a sub schema.
	 *
	 * @param subSchemaValidators
	 *            the validators of the sub schema
	 * @param jsonNode
	 *            the JSON data node to validate
	 * @param jsonPath
	 *            the path of the JSON data node, used in error messages
	 * @return true, if all validators accept the data node
	 */
	protected boolean validateSubSchema(final List<BaseJsonSchemaValidator> subSchemaValidators, final JsonNode jsonNode, final JsonPath jsonPath) {
		return getSubSchemaValidationError(subSchemaValidators, jsonNode, jsonPath) == null;
	}

	/**
	 * Validates a JSON data node against all validators of a sub schema and returns the first
	 * validation error instead of throwing it. Useful to keep the original error as cause of a
	 * more general error.
	 *
	 * @param subSchemaValidators
	 *            the validators of the sub schema
	 * @param jsonNode
	 *            the JSON data node to validate
	 * @param jsonPath
	 *            the path of the JSON data node, used in error messages
	 * @return the first validation error, or null if all validators accept the data node
	 */
	@SuppressWarnings("static-method")
	protected JsonSchemaDataValidationError getSubSchemaValidationError(final List<BaseJsonSchemaValidator> subSchemaValidators, final JsonNode jsonNode, final JsonPath jsonPath) {
		for (final BaseJsonSchemaValidator validator : subSchemaValidators) {
			try {
				validator.validate(jsonNode, jsonPath);
			} catch (final JsonSchemaDataValidationError e) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Checks two JSON data nodes for equality as defined by JSON schema: numbers are equal by
	 * mathematical value (1 equals 1.0), objects are equal if they have the same property names
	 * with equal values in any order, arrays are equal if they have equal items in the same order.
	 *
	 * @param first
	 *            the first node, Java null is regarded as JSON null
	 * @param second
	 *            the second node, Java null is regarded as JSON null
	 * @return true, if both nodes are equal
	 */
	public static boolean isJsonDataEqual(final JsonNode first, final JsonNode second) {
		// Null checks directly in the conditions, so the compiler's null analysis knows both nodes are non-null below
		if (first == null || first.isNull()) {
			return second == null || second.isNull();
		} else if (second == null || second.isNull()) {
			return false;
		} else if ((first.isInteger() || first.isNumber()) && (second.isInteger() || second.isNumber())) {
			return NumberUtilities.compare(getNumericValue(first), getNumericValue(second)) == 0;
		} else if (first.isBoolean() && second.isBoolean()) {
			return ((JsonValueBoolean) first).getValue().booleanValue() == ((JsonValueBoolean) second).getValue().booleanValue();
		} else if (first.isString() && second.isString()) {
			return ((JsonValueString) first).getValue().equals(((JsonValueString) second).getValue());
		} else if (first.isJsonArray() && second.isJsonArray()) {
			final JsonArray firstArray = (JsonArray) first;
			final JsonArray secondArray = (JsonArray) second;
			if (firstArray.size() != secondArray.size()) {
				return false;
			}
			for (int i = 0; i < firstArray.size(); i++) {
				if (!isJsonDataEqual(firstArray.get(i), secondArray.get(i))) {
					return false;
				}
			}
			return true;
		} else if (first.isJsonObject() && second.isJsonObject()) {
			final JsonObject firstObject = (JsonObject) first;
			final JsonObject secondObject = (JsonObject) second;
			if (firstObject.size() != secondObject.size()) {
				return false;
			}
			for (final String key : firstObject.keySet()) {
				if (!secondObject.containsKey(key) || !isJsonDataEqual(firstObject.get(key), secondObject.get(key))) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns the numeric value of an integer or number JSON node.
	 *
	 * @param numericJsonNode
	 *            the integer or number JSON node
	 * @return the numeric value
	 */
	private static Number getNumericValue(final JsonNode numericJsonNode) {
		if (numericJsonNode.isInteger()) {
			return ((JsonValueInteger) numericJsonNode).getValue();
		} else {
			return ((JsonValueNumber) numericJsonNode).getValue();
		}
	}

	/**
	 * Validates a JSON data node against this validator's keyword.
	 *
	 * @param jsonNode
	 *            the JSON data node to validate
	 * @param jsonPath
	 *            the path of the JSON data node, used in error messages
	 * @throws JsonSchemaDataValidationError
	 *             if the data node is not valid
	 */
	public abstract void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError;
}
