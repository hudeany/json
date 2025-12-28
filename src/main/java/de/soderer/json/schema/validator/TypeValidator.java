package de.soderer.json.schema.validator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonDataType;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.exception.JsonDuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * JSON subschema that matches a simple data value to a type definition
 */
public class TypeValidator extends BaseJsonSchemaValidator {
	private final List<String> typeStrings = new ArrayList<>();
	private final List<List<BaseJsonSchemaValidator>> typeValidators = new ArrayList<>();

	public TypeValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError, JsonDuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Type data is 'null'", jsonSchemaPath);
		} else if (!(validatorData instanceof String) && !(validatorData instanceof JsonArray)) {
			throw new JsonSchemaDefinitionError("Type data is not a 'string' or 'array'", jsonSchemaPath);
		}

		if (validatorData instanceof String) {
			if ("any".equals(validatorData)) {
				typeStrings.add((String) validatorData);
			} else {
				try {
					JsonDataType.getFromString((String) validatorData);
				} catch (final Exception e) {
					throw new JsonSchemaDefinitionError("Invalid JSON data type '" + validatorData + "'", jsonSchemaPath, e);
				}
				typeStrings.add((String) validatorData);
			}
		} else if (validatorData instanceof JsonArray) {
			for (final Object typeData : ((JsonArray) validatorData)) {
				if (typeData == null) {
					throw new JsonSchemaDefinitionError("Type data array contains a 'null' item", jsonSchemaPath);
				} else if (typeData instanceof String) {
					try {
						JsonDataType.getFromString((String) typeData);
					} catch (final Exception e) {
						throw new JsonSchemaDefinitionError("Invalid JSON data type '" + validatorData + "'", jsonSchemaPath, e);
					}
					typeStrings.add((String) typeData);
				} else if (typeData instanceof JsonObject) {
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
				return;
			} else {
				JsonDataType jsonDataType;
				try {
					jsonDataType = JsonDataType.getFromString(typeString);
				} catch (final Exception e) {
					throw new JsonSchemaDataValidationError("Invalid JSON data type '" + typeString + "'", jsonPath, e);
				}

				if (checkJsonDataType(jsonNode, jsonDataType)) {
					return;
				}
			}
		}
		for (final List<BaseJsonSchemaValidator> typeValidatorList : typeValidators) {
			if (validateSubSchema(typeValidatorList, jsonNode, jsonPath)) {
				return;
			}
		}
		throw new JsonSchemaDataValidationError("Invalid data type '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
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
				String stringRepresentation = jsonNode.getValue().toString();
				if (stringRepresentation.contains("E")) {
					final BigDecimal bigDecimal = new BigDecimal(jsonNode.getValue().toString());
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
