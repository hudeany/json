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
import de.soderer.json.utilities.Utilities;

/**
 * Validator for the "enum" keyword: its value is a non-empty array of allowed values, one of which
 * the JSON data node must equal. Integer and number values are compared numerically, so 1 equals
 * 1.0. The array may contain null.
 */
public class EnumValidator extends BaseJsonSchemaValidator {
	/**
	 * Creates a new "enum" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, an array of allowed values
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a non-empty array
	 */
	public EnumValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Enum data is 'null'", jsonSchemaPath);
		} else if (!(validatorData.isJsonArray())) {
			throw new JsonSchemaDefinitionError("Enum contains a non-JsonArray", jsonSchemaPath);
		} else if (((JsonArray) validatorData).size() == 0) {
			throw new JsonSchemaDefinitionError("Enum contains an empty JsonArray", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		for (final JsonNode enumObject : ((JsonArray) validatorData).items()) {
			// An allowed null value may be given as Java null or as JSON null value
			if ((enumObject == null || enumObject.isNull()) && jsonNode.isNull()) {
				return;
			} else if (enumObject != null && jsonNode != null) {
				if (enumObject.isBoolean()) {
					if (jsonNode.isBoolean() && (boolean) ((JsonValueBoolean) jsonNode).getValue() == (boolean) ((JsonValueBoolean) enumObject).getValue()) {
						return;
					}
				} else if (enumObject.isString()) {
					if (jsonNode.isString() && enumObject.isString() && ((JsonValueString) jsonNode).getValue().equals(((JsonValueString) enumObject).getValue())) {
						return;
					}
				} else if (enumObject.isInteger() && jsonNode.isInteger()) {
					if (NumberUtilities.compare(((JsonValueInteger) enumObject).getValue(), ((JsonValueInteger) jsonNode).getValue()) == 0) {
						return;
					}
				} else if (enumObject.isNumber() && jsonNode.isNumber()) {
					if (NumberUtilities.compare(((JsonValueNumber) enumObject).getValue(), ((JsonValueNumber) jsonNode).getValue()) == 0) {
						return;
					}
				} else if (enumObject.isNumber() && jsonNode.isInteger()) {
					if (NumberUtilities.compare(((JsonValueNumber) enumObject).getValue(), ((JsonValueInteger) jsonNode).getValue()) == 0) {
						return;
					}
				} else if (enumObject.isInteger() && jsonNode.isNumber()) {
					if (NumberUtilities.compare(((JsonValueInteger) enumObject).getValue(), ((JsonValueNumber) jsonNode).getValue()) == 0) {
						return;
					}
				} else if (enumObject.isJsonObject()) {
					if (jsonNode.isJsonObject() && enumObject.isJsonObject() && ((JsonObject) enumObject).equals(jsonNode)) {
						return;
					}
				} else if (enumObject.isJsonArray()) {
					if (jsonNode.isJsonArray() && enumObject.isJsonArray() && ((JsonArray) enumObject).equals(jsonNode)) {
						return;
					}
				}
			}
		}
		throw new JsonSchemaDataValidationError("Enumeration expected one of '" + Utilities.join(((JsonArray) validatorData).items(), "', '") + "' but was '" + getJsonNodeDisplayString(jsonNode, jsonPath) + "'", jsonPath);
	}

	/**
	 * Returns a short display text of a JSON data node for error messages.
	 *
	 * @param jsonNode
	 *            the JSON data node
	 * @param jsonPath
	 *            the path of the JSON data node, used in error messages
	 * @return the display text
	 * @throws JsonSchemaDataValidationError
	 *             if the data type is unknown
	 */
	private static String getJsonNodeDisplayString(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		switch(jsonNode.getJsonDataType()) {
			case OBJECT:
				return jsonNode.getJsonDataType().getName();
			case ARRAY:
				return jsonNode.getJsonDataType().getName();
			case NULL:
				return "null";
			case STRING:
				return ((JsonValueString) jsonNode).getValue();
			case INTEGER:
				return ((JsonValueInteger) jsonNode).getValue().toString();
			case NUMBER:
				return ((JsonValueNumber) jsonNode).getValue().toString();
			case BOOLEAN:
				return ((JsonValueBoolean) jsonNode).getValue().toString();
			default:
				throw new JsonSchemaDataValidationError("Unknown JsonDataType: '" + jsonNode.getJsonDataType().name() + "'", jsonPath);

		}
	}
}
