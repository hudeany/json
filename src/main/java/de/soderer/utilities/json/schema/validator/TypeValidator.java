package de.soderer.utilities.json.schema.validator;

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
	public TypeValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData, final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData, jsonNode, jsonPath);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Type data is 'null'", jsonSchemaPath);
		} else if (!(validatorData instanceof String) && !(validatorData instanceof JsonArray)) {
			throw new JsonSchemaDefinitionError("Type data is not a 'string' or 'array'", jsonSchemaPath);
		}
	}

	@Override
	public void validate() throws Exception {
		if (validatorData instanceof JsonArray) {
			for (final Object typeData : ((JsonArray) validatorData)) {
				if (typeData == null) {
					throw new JsonSchemaDefinitionError("Type data array contains a 'null' item", jsonSchemaPath);
				} else if (typeData instanceof String) {
					if ("any".equals(typeData)) {
						return;
					} else {
						JsonDataType jsonDataType;
						try {
							jsonDataType = JsonDataType.getFromString((String) typeData);
						} catch (final Exception e) {
							throw new JsonSchemaDefinitionError("Invalid JSON data type '" + validatorData + "'", jsonSchemaPath, e);
						}

						if (jsonNode.getJsonDataType() == jsonDataType || (jsonDataType == JsonDataType.NUMBER && jsonNode.getJsonDataType() == JsonDataType.INTEGER)) {
							return;
						}
					}
				} else if (typeData instanceof JsonObject) {
					final List<BaseJsonSchemaValidator> subValidators = JsonSchema.createValidators((JsonObject) typeData, jsonSchemaDependencyResolver, jsonSchemaPath, jsonNode, jsonPath);
					try {
						for (final BaseJsonSchemaValidator subValidator : subValidators) {
							subValidator.validate();
						}
						return;
					} catch (@SuppressWarnings("unused") final JsonSchemaDataValidationError e) {
						// Do nothing, just check the next array item
					}
				} else {
					throw new JsonSchemaDefinitionError("Type data array contains a item that is no 'string' and no 'object'", jsonSchemaPath);
				}
			}
			throw new JsonSchemaDataValidationError("Invalid data type '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
		} else {
			if ("any".equals(validatorData)) {
				return;
			} else {
				JsonDataType jsonDataType;
				try {
					jsonDataType = JsonDataType.getFromString((String) validatorData);
				} catch (final Exception e) {
					throw new JsonSchemaDefinitionError("Invalid JSON data type '" + validatorData + "'", jsonSchemaPath, e);
				}

				if (jsonNode.getJsonDataType() != jsonDataType) {
					if (jsonDataType == JsonDataType.NUMBER) {
						// Integer datatype is a sub type of number
						if (jsonNode.getJsonDataType() != JsonDataType.INTEGER) {
							throw new JsonSchemaDataValidationError("Expected data type is '" + (String) validatorData + "' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
						}
					} else if (jsonDataType == JsonDataType.INTEGER && jsonNode.getJsonDataType() == JsonDataType.NUMBER) {
						// In JSON schema draft v6+ a float value with zero fraction is also allowed as integer, although it is NOT recommended
						if (jsonSchemaDependencyResolver.isSimpleMode() || jsonSchemaDependencyResolver.isDraftV4Mode()) {
							throw new JsonSchemaDataValidationError("Expected data type is '" + (String) validatorData + "' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
						} else {
							String stringRepresentation =((Number) jsonNode.getValue()).toString();
							while (stringRepresentation.contains(".0")) {
								stringRepresentation = stringRepresentation.replace(".0", ".");
							}
							if (stringRepresentation.contains(".") && stringRepresentation.indexOf(".") != stringRepresentation.length() - 1) {
								throw new JsonSchemaDataValidationError("Expected data type is '" + (String) validatorData + "' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
							}
						}
					} else {
						throw new JsonSchemaDataValidationError("Expected data type is '" + (String) validatorData + "' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
					}
				}
			}
		}
	}
}
