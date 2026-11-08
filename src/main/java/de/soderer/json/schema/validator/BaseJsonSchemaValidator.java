package de.soderer.json.schema.validator;

import java.util.List;
import java.util.regex.Pattern;

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
import de.soderer.json.schema.JsonSchemaRegexUtilities;
import de.soderer.json.schema.JsonSchemaValidationCollector;
import de.soderer.json.schema.RegexEvaluationLimitException;
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
		// The failure of a sub schema checked here may be expected (e.g. options of "anyOf"), so its
		// errors are not collected
		JsonSchemaValidationCollector.suspend();
		try {
			for (final BaseJsonSchemaValidator validator : subSchemaValidators) {
				try {
					validator.validate(jsonNode, jsonPath);
				} catch (final JsonSchemaDataValidationError e) {
					e.setJsonSchemaPathIfMissing(validator.jsonSchemaPath);
					return e;
				}
			}
			return null;
		} finally {
			JsonSchemaValidationCollector.resume();
		}
	}

	/**
	 * Searches for a match of a regex pattern of the schema within a data text, with the step limit
	 * of the configuration to protect against catastrophic backtracking (ReDoS).
	 *
	 * @param pattern
	 *            the compiled pattern
	 * @param patternText
	 *            the pattern as written in the schema, used in error messages
	 * @param input
	 *            the data text
	 * @param jsonPath
	 *            the path of the JSON data node, used in error messages
	 * @return true, if the text contains a match
	 * @throws JsonSchemaDataValidationError
	 *             if the evaluation exceeds its limits; the data is then regarded as invalid
	 */
	protected boolean findRegex(final Pattern pattern, final String patternText, final String input, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		try {
			return JsonSchemaRegexUtilities.find(pattern, input, jsonSchemaDependencyResolver == null ? JsonSchemaRegexUtilities.DEFAULT_REGEX_STEP_LIMIT : jsonSchemaDependencyResolver.getRegexStepLimit());
		} catch (final RegexEvaluationLimitException e) {
			throw new JsonSchemaDataValidationError("RegEx pattern '" + patternText + "' could not be evaluated for the data: " + e.getMessage(), jsonPath, e);
		}
	}

	/**
	 * Checks whether errors are collected at the current validation position, see
	 * {@link de.soderer.json.schema.JsonSchema#validateAll(JsonNode)}.
	 *
	 * @return true if errors are collected instead of thrown
	 */
	protected static boolean isCollectingErrors() {
		return JsonSchemaValidationCollector.getActive() != null;
	}

	/**
	 * Validates a JSON data node against the validators of a schema.
	 * <p>
	 * Without collecting the first error is thrown. While errors are collected (see
	 * {@link de.soderer.json.schema.JsonSchema#validateAll(JsonNode)}), all validators are applied
	 * and their errors are recorded. A failing "type" stops the other validators then, because their
	 * errors would only follow from the wrong data type.
	 * </p>
	 *
	 * @param validators
	 *            the validators of the schema
	 * @param jsonNode
	 *            the JSON data node to validate
	 * @param jsonPath
	 *            the path of the JSON data node
	 * @return true if the data node is valid, false if errors were collected
	 * @throws JsonSchemaDataValidationError
	 *             the first error, if errors are not collected
	 */
	public static boolean applyValidators(final List<BaseJsonSchemaValidator> validators, final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		final JsonSchemaValidationCollector collector = JsonSchemaValidationCollector.getActive();
		if (collector == null) {
			for (final BaseJsonSchemaValidator validator : validators) {
				try {
					// Separate path object per validator, as validators may modify it
					validator.validate(jsonNode, jsonPath == null ? null : new JsonPath(jsonPath));
				} catch (final JsonSchemaDataValidationError e) {
					e.setJsonSchemaPathIfMissing(validator.jsonSchemaPath);
					throw e;
				}
			}
			return true;
		} else if (collector.isFull()) {
			// No further errors are reported, so the remaining validation is skipped
			return false;
		} else {
			for (final BaseJsonSchemaValidator validator : validators) {
				if (validator instanceof TypeValidator && !applyValidator(collector, validator, jsonNode, jsonPath)) {
					return false;
				}
			}
			boolean valid = true;
			for (final BaseJsonSchemaValidator validator : validators) {
				if (!(validator instanceof TypeValidator)) {
					valid = applyValidator(collector, validator, jsonNode, jsonPath) && valid;
				}
			}
			return valid;
		}
	}

	/**
	 * Validates a JSON data node against the validators of a schema, see
	 * {@link #applyValidators(List, JsonNode, JsonPath)}, and throws an error if it is not valid.
	 *
	 * @param validators
	 *            the validators of the schema
	 * @param jsonNode
	 *            the JSON data node to validate
	 * @param jsonPath
	 *            the path of the JSON data node
	 * @throws JsonSchemaDataValidationError
	 *             the first error, or a marker of already collected errors
	 */
	public static void applyValidatorsOrThrow(final List<BaseJsonSchemaValidator> validators, final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!applyValidators(validators, jsonNode, jsonPath)) {
			throw JsonSchemaDataValidationError.createCollectedMarker(jsonPath);
		}
	}

	/**
	 * Applies a single validator while collecting errors.
	 *
	 * @param collector
	 *            the active collector
	 * @param validator
	 *            the validator
	 * @param jsonNode
	 *            the JSON data node to validate
	 * @param jsonPath
	 *            the path of the JSON data node
	 * @return true if the data node is valid for the validator
	 */
	private static boolean applyValidator(final JsonSchemaValidationCollector collector, final BaseJsonSchemaValidator validator, final JsonNode jsonNode, final JsonPath jsonPath) {
		if (collector.isFull()) {
			return false;
		}
		try {
			validator.validate(jsonNode, jsonPath == null ? null : new JsonPath(jsonPath));
			return true;
		} catch (final JsonSchemaDataValidationError e) {
			if (!e.isCollectedMarker()) {
				e.setJsonSchemaPathIfMissing(validator.jsonSchemaPath);
				collector.addError(e);
			}
			return false;
		}
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
