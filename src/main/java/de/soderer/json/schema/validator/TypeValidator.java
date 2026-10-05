package de.soderer.json.schema.validator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonDataType;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
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
 * Validator for the "type" keyword: the JSON data node must be of the given type. The value is a type
 * name ("string", "number", "integer", "boolean", "object", "array", "null", or "any" in draft 3) or
 * an array of type names; in draft 3 the array may also contain schemas. The data must match at
 * least one of them. Integers are also numbers; since draft 6 numbers with zero fraction (like 1.0)
 * are also integers.
 */
public class TypeValidator extends BaseJsonSchemaValidator {
	/** Names of the allowed types. */
	private final List<String> typeStrings = new ArrayList<>();

	/** Validators of the allowed schemas (draft 3). */
	private final List<List<BaseJsonSchemaValidator>> typeValidators = new ArrayList<>();

	/**
	 * Creates a new "type" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, a type name or an array of type names and schemas
	 * @throws JsonSchemaDefinitionError
	 *             if the value is neither string nor array, contains an unknown type name or an
	 *             invalid schema
	 * @throws DuplicateKeyException
	 *             if a schema contains duplicate keys
	 */
	public TypeValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Type data is 'null'", jsonSchemaPath);
		} else if (!(validatorData.isString()) && !(validatorData.isJsonArray())) {
			throw new JsonSchemaDefinitionError("Type data is not a 'string' or 'array'", jsonSchemaPath);
		}

		if (validatorData.isString()) {
			if ("any".equals(((JsonValueString) validatorData).getValue())) {
				typeStrings.add(((JsonValueString) validatorData).getValue());
			} else {
				try {
					JsonDataType.getFromString(((JsonValueString) validatorData).getValue());
				} catch (final Exception e) {
					throw new JsonSchemaDefinitionError("Invalid JSON data type '" + validatorData + "'", jsonSchemaPath, e);
				}
				typeStrings.add(((JsonValueString) validatorData).getValue());
			}
		} else if (validatorData.isJsonArray()) {
			int index = 0;
			for (final JsonNode typeData : ((JsonArray) validatorData).items()) {
				if (typeData == null) {
					throw new JsonSchemaDefinitionError("Type data array contains a 'null' item", jsonSchemaPath);
				} else if (typeData.isString()) {
					try {
						JsonDataType.getFromString(((JsonValueString) typeData).getValue());
					} catch (final Exception e) {
						throw new JsonSchemaDefinitionError("Invalid JSON data type '" + typeData + "'", jsonSchemaPath, e);
					}
					typeStrings.add(((JsonValueString) typeData).getValue());
				} else if (typeData.isJsonObject()) {
					typeValidators.add(JsonSchema.createValidators((JsonObject) typeData, jsonSchemaDependencyResolver, new JsonSchemaPath(jsonSchemaPath).addArrayIndex(index)));
				} else {
					throw new JsonSchemaDefinitionError("Type data array contains an item that is no 'string' and no 'object'", jsonSchemaPath);
				}
				index++;
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

	/**
	 * Checks whether a JSON data node is of the given type. Integer values are also numbers; since
	 * draft 6 numbers with zero fraction (like 1.0) are also integers.
	 *
	 * @param jsonNode
	 *            the JSON data node
	 * @param jsonDataType
	 *            the type to check
	 * @return true, if the data node is of the given type
	 */
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
				final Number numberValue = ((JsonValueNumber) jsonNode).getValue();
				if ((numberValue instanceof Double || numberValue instanceof Float) && !Double.isFinite(numberValue.doubleValue())) {
					// NaN and infinite values (JSON5) are no integers
					return false;
				}
				// BigDecimal handles plain and exponential notation, e.g. "1.0", "1.50" or "1.0E5"
				final BigDecimal bigDecimal = new BigDecimal(((JsonValueNumber) jsonNode).getValue().toString());
				return bigDecimal.stripTrailingZeros().scale() <= 0;
			}
		} else {
			return false;
		}
	}
}
