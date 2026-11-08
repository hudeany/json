package de.soderer.json.schema.validator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

/**
 * Validator for the "uniqueItems" keyword: if true, all items of the JSON data array must be
 * different, compared by their JSON data value (numbers by numeric value, objects regardless of the
 * order of their properties). The check takes linear time by grouping the items by hash code.
 * Data that is not an array is ignored, except in simple mode.
 */
public class UniqueItemsValidator extends BaseJsonSchemaValidator {
	/** Whether the items must be unique. */
	private final boolean checkValue;

	/**
	 * Creates a new "uniqueItems" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, a boolean or the string "true" or "false"
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a boolean
	 */
	public UniqueItemsValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Data for 'uniqueItems' items is 'null'", jsonSchemaPath);
		} else if (validatorData.isString()) {
			final String stringValue = ((JsonValueString) validatorData).getValue();
			// Boolean.parseBoolean never throws, it silently returns false for any non-"true" string,
			// so the allowed values must be checked explicitly here to reject garbage input.
			if ("true".equalsIgnoreCase(stringValue)) {
				checkValue = true;
			} else if ("false".equalsIgnoreCase(stringValue)) {
				checkValue = false;
			} else {
				throw new JsonSchemaDefinitionError("Data for 'uniqueItems' items is '" + validatorData + "' and not 'boolean'", jsonSchemaPath);
			}
		} else if (validatorData.isBoolean()) {
			checkValue = ((JsonValueBoolean) validatorData).getValue();
		} else {
			throw new JsonSchemaDefinitionError("Data for 'uniqueItems' is not 'boolean'", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonArray())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'array' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			if (checkValue) {
				final JsonArray jsonArray = (JsonArray) jsonNode;
				// Items are grouped by a hash code consistent with isJsonDataEqual, so only items with equal
				// hash codes have to be compared: O(n) instead of comparing all pairs in O(n^2)
				final Map<Integer, List<JsonNode>> itemsByHashCode = new HashMap<>();
				for (int i = 0; i < jsonArray.size(); i++) {
					final JsonNode item = jsonArray.get(i);
					final List<JsonNode> itemsWithSameHashCode = itemsByHashCode.computeIfAbsent(getJsonDataHashCode(item), key -> new ArrayList<>(1));
					for (final JsonNode otherItem : itemsWithSameHashCode) {
						if (otherItem == item || isJsonDataEqual(otherItem, item)) {
							throw new JsonSchemaDataValidationError("Item '" + otherItem + "' of array is not unique", jsonPath);
						}
					}
					itemsWithSameHashCode.add(item);
				}
			}
		}
	}

	/**
	 * Returns a hash code of JSON data, which is consistent with
	 * {@link BaseJsonSchemaValidator#isJsonDataEqual(JsonNode, JsonNode)}: numbers are compared by
	 * their numeric value (1 equals 1.0), objects regardless of the order of their properties.
	 *
	 * @param jsonNode
	 *            the JSON data node, may be null
	 * @return the hash code
	 */
	static int getJsonDataHashCode(final JsonNode jsonNode) {
		if (jsonNode == null || jsonNode.isNull()) {
			return 0;
		} else if (jsonNode.isInteger()) {
			return getNumericHashCode(((JsonValueInteger) jsonNode).getValue());
		} else if (jsonNode.isNumber()) {
			return getNumericHashCode(((JsonValueNumber) jsonNode).getValue());
		} else if (jsonNode.isBoolean()) {
			return Boolean.hashCode(((JsonValueBoolean) jsonNode).getValue());
		} else if (jsonNode.isString()) {
			return ((JsonValueString) jsonNode).getValue().hashCode();
		} else if (jsonNode.isJsonArray()) {
			int hashCode = 1;
			for (final JsonNode item : ((JsonArray) jsonNode).items()) {
				hashCode = 31 * hashCode + getJsonDataHashCode(item);
			}
			return hashCode;
		} else if (jsonNode.isJsonObject()) {
			// Sum of the entry hash codes, so that the order of the properties is irrelevant
			int hashCode = 7;
			for (final Entry<String, JsonNode> entry : ((JsonObject) jsonNode).entrySet()) {
				hashCode += entry.getKey().hashCode() ^ getJsonDataHashCode(entry.getValue());
			}
			return hashCode;
		} else {
			return jsonNode.getJsonDataType().hashCode();
		}
	}

	/**
	 * Returns a hash code of a number, which is equal for numerically equal numbers of any type.
	 * The number is reduced to the double nearest to its exact decimal value (Float and Double by
	 * their decimal representation, as {@link de.soderer.json.utilities.NumberUtilities#compare(Number, Number)}
	 * does). Numerically equal numbers have the same exact value and therefore the same nearest
	 * double. Different numbers may share a hash code, which is resolved by the exact comparison.
	 *
	 * @param number
	 *            the number
	 * @return the hash code
	 */
	private static int getNumericHashCode(final Number number) {
		final double doubleValue;
		if (number instanceof Double || number instanceof Float) {
			// Decimal representation like NumberUtilities.compare, so that 0.1f and 0.1 are equal
			doubleValue = Double.isFinite(number.doubleValue()) ? Double.parseDouble(number.toString()) : number.doubleValue();
		} else if (number instanceof BigDecimal || number instanceof BigInteger || number instanceof Long || number instanceof Integer || number instanceof Short || number instanceof Byte) {
			// Correctly rounded conversions of the exact value
			doubleValue = number.doubleValue();
		} else {
			doubleValue = new BigDecimal(number.toString()).doubleValue();
		}
		// Adding 0.0 turns -0.0 into 0.0, which are numerically equal
		return Double.hashCode(doubleValue + 0.0);
	}
}
