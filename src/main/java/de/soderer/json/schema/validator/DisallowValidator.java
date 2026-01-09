package de.soderer.json.schema.validator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonDataType;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueString;
import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * JSON subschema that matches a simple data value to a type definition
 */
public class DisallowValidator extends BaseJsonSchemaValidator {
	private final List<String> typeStrings = new ArrayList<>();
	private final List<List<BaseJsonSchemaValidator>> typeValidators = new ArrayList<>();

	public DisallowValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Type data is 'null'", jsonSchemaPath);
		} else if (!(validatorData.isString()) && !(validatorData.isJsonArray())) {
			throw new JsonSchemaDefinitionError("Type data is not a 'string' or 'array'", jsonSchemaPath);
		}

		if (validatorData.isString()) {
			try {
				JsonDataType.getFromString(((JsonValueString) validatorData).getValue());
			} catch (final Exception e) {
				throw new JsonSchemaDefinitionError("Invalid JSON data type '" + validatorData + "'", jsonSchemaPath, e);
			}
			typeStrings.add(((JsonValueString) validatorData).getValue());
		} else if (validatorData.isJsonArray()) {
			for (final JsonNode typeData : ((JsonArray) validatorData).items()) {
				if (typeData == null) {
					throw new JsonSchemaDefinitionError("Type data array contains a 'null' item", jsonSchemaPath);
				} else if (typeData.isString()) {
					try {
						JsonDataType.getFromString(((JsonValueString) typeData).getValue());
					} catch (final Exception e) {
						throw new JsonSchemaDefinitionError("Invalid JSON data type '" + validatorData + "'", jsonSchemaPath, e);
					}
					typeStrings.add(((JsonValueString) typeData).getValue());
				} else if (typeData.isJsonObject()) {
					typeValidators.add(JsonSchema.createValidators((JsonObject) typeData, jsonSchemaDependencyResolver, jsonSchemaPath));
				} else {
					throw new JsonSchemaDefinitionError("Type data array contains an item that is no 'string' and no 'object'", jsonSchemaPath);
				}
			}
		} else {
			throw new JsonSchemaDefinitionError("Invalid JSON data type definition item '" + validatorData + "'", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		for (final String typeString : typeStrings) {
			if ("any".equals(typeString)) {
				throw new JsonSchemaDataValidationError("Invalid data type '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			} else {
				JsonDataType jsonDataType;
				try {
					jsonDataType = JsonDataType.getFromString(typeString);
				} catch (final Exception e) {
					throw new JsonSchemaDataValidationError("Invalid JSON data type '" + typeString + "'", jsonPath, e);
				}

				if (checkJsonDataType(jsonNode, jsonDataType)) {
					throw new JsonSchemaDataValidationError("Invalid data type '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
				}
			}
		}
		for (final List<BaseJsonSchemaValidator> typeValidatorList : typeValidators) {
			if (validateSubSchema(typeValidatorList, jsonNode, jsonPath)) {
				throw new JsonSchemaDataValidationError("Invalid data type '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		}
	}

	private boolean checkJsonDataType(final JsonNode jsonNode, final JsonDataType jsonDataType) {
		if (jsonNode.getJsonDataType() == jsonDataType) {
			return true;
		} else if (jsonDataType == JsonDataType.NUMBER) {
			// Integer datatype is a sub type of number
			return jsonNode.getJsonDataType() == JsonDataType.INTEGER;
		} else if (jsonDataType == JsonDataType.INTEGER && jsonNode.getJsonDataType() == JsonDataType.NUMBER) {
			// In JSON schema draft v6+ a float value with zero fraction is also allowed as integer, although it is NOT recommended
			if (jsonSchemaDependencyResolver.isSimpleMode() || jsonSchemaDependencyResolver.isDraftV3Mode() || jsonSchemaDependencyResolver.isDraftV4Mode()) {
				return false;
			} else {
				String stringRepresentation = ((JsonValueNumber) jsonNode).getValue().toString();
				if (stringRepresentation.contains("E")) {
					final BigDecimal bigDecimal = new BigDecimal(((JsonValueInteger) jsonNode).getValue().toString());
					return bigDecimal.stripTrailingZeros().scale() <= 0;
				} else {
					while (stringRepresentation.contains(".0")) {
						stringRepresentation = stringRepresentation.replace(".0", ".");
					}
					return !stringRepresentation.contains(".") || stringRepresentation.indexOf(".") == stringRepresentation.length() - 1;
				}
			}
		} else {
			return false;
		}
	}
}
