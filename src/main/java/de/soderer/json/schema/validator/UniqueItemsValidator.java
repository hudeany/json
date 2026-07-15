package de.soderer.json.schema.validator;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * JSON schema validator to check all items of a JSON array to be unique by utilization of "equal" method
 */
public class UniqueItemsValidator extends BaseJsonSchemaValidator {
	private final boolean checkValue;

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
				for (int i = 0; i < jsonArray.size(); i++) {
					for (int j = i + 1; j < jsonArray.size(); j++) {
						if ((jsonArray.get(i) == jsonArray.get(j))
								|| (jsonArray.get(i) != null && jsonArray.get(i).equals(jsonArray.get(j)))) {
							throw new JsonSchemaDataValidationError("Item '" + jsonArray.get(i) + "' of array is not unique", jsonPath);
						}
					}
				}
			}
		}
	}
}
