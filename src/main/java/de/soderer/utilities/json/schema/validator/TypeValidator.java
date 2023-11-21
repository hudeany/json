package de.soderer.utilities.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonDataType;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchema;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

public class TypeValidator extends BaseJsonSchemaValidator {
	private final List<String> typeStrings = new ArrayList<>();
	private final List<List<BaseJsonSchemaValidator>> typeValidators = new ArrayList<>();

	public TypeValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Type data is 'null'", jsonSchemaPath);
		} else if (!(validatorData instanceof String) && !(validatorData instanceof JsonArray)) {
			throw new JsonSchemaDefinitionError("Type data is not a 'string' or 'array'", jsonSchemaPath);
		}

		if (validatorData instanceof String) {
			try {
				JsonDataType.getFromString((String) validatorData);
			} catch (final Exception e) {
				throw new JsonSchemaDefinitionError("Invalid JSON data type '" + validatorData + "'", jsonSchemaPath, e);
			}
			typeStrings.add((String) validatorData);
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
			try {
				for (final BaseJsonSchemaValidator subValidator : typeValidatorList) {
					subValidator.validate(jsonNode, jsonPath);
				}
				return;
			} catch (@SuppressWarnings("unused") final JsonSchemaDataValidationError e) {
				// Do nothing, just check the next array item
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
			if (jsonSchemaDependencyResolver.isSimpleMode() || jsonSchemaDependencyResolver.isDraftV4Mode()) {
				return false;
			} else {
				String stringRepresentation =((Number) jsonNode.getValue()).toString();
				while (stringRepresentation.contains(".0")) {
					stringRepresentation = stringRepresentation.replace(".0", ".");
				}
				return !stringRepresentation.contains(".") || stringRepresentation.indexOf(".") == stringRepresentation.length() - 1;
			}
		} else {
			return false;
		}
	}
}
