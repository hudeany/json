package de.soderer.json.schema.validator;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueFloat;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.utilities.NumberUtilities;
import de.soderer.json.utilities.Utilities;

/**
 * A list of values that are allowed
 */
public class EnumValidator extends BaseJsonSchemaValidator {
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
		for (final JsonNode enumObject : ((JsonArray) validatorData)) {
			if (enumObject == null && jsonNode.isNull()) {
				return;
			} else if (enumObject != null && jsonNode != null) {
				if (enumObject.isBoolean()) {
					if ((jsonNode.isBoolean() && enumObject.isBoolean() && ((JsonValueBoolean) jsonNode).getValue() == ((JsonValueBoolean) enumObject).getValue())) {
						return;
					}
				} else if (enumObject instanceof JsonValueString) {
					if (jsonNode.isString() && enumObject.isString() && ((JsonValueString) jsonNode).getValue().equals(((JsonValueString) enumObject).getValue())) {
						return;
					}
				} else if (enumObject.isInteger() && jsonNode.isInteger()) {
					if (NumberUtilities.compare(((JsonValueInteger) enumObject).getValue(), ((JsonValueInteger) jsonNode).getValue()) == 0) {
						return;
					}
				} else if (enumObject.isFloat() && jsonNode.isFloat()) {
					if (NumberUtilities.compare(((JsonValueFloat) enumObject).getValue(), ((JsonValueFloat) jsonNode).getValue()) == 0) {
						return;
					}
				} else if (enumObject.isFloat() && jsonNode.isInteger()) {
					if (NumberUtilities.compare(((JsonValueFloat) enumObject).getValue(), ((JsonValueInteger) jsonNode).getValue()) == 0) {
						return;
					}
				} else if (enumObject.isInteger() && jsonNode.isFloat()) {
					if (NumberUtilities.compare(((JsonValueInteger) enumObject).getValue(), ((JsonValueFloat) jsonNode).getValue()) == 0) {
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
		throw new JsonSchemaDataValidationError("Enumeration expected one of '" + Utilities.join((JsonArray) validatorData, "', '") + "' but was '" + getJsonNodeDisplayString(jsonNode, jsonPath) + "'", jsonPath);
	}

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
			case FLOAT:
				return ((JsonValueFloat) jsonNode).getValue().toString();
			case BOOLEAN:
				return ((JsonValueBoolean) jsonNode).getValue().toString();
			default:
				throw new JsonSchemaDataValidationError("Unkown JsonDataType: '" + jsonNode.getJsonDataType().name() + "'", jsonPath);

		}
	}
}
