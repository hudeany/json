package de.soderer.json.schema.validator;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * JSON schema validator to check all items of a JSON array to be unique by utilization of "equal" method
 */
public class UniqueItemsValidator extends BaseJsonSchemaValidator {
	public UniqueItemsValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Data for 'uniqueItems' items is 'null'", jsonSchemaPath);
		} else if (validatorData instanceof Boolean) {
			this.validatorData = validatorData;
		} else if (validatorData instanceof String) {
			try {
				this.validatorData = Boolean.parseBoolean((String) validatorData);
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for 'uniqueItems' items is '" + validatorData + "' and not 'boolean'", jsonSchemaPath, e);
			}
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
			if ((Boolean) validatorData) {
				final JsonArray jsonArray = (JsonArray) jsonNode.getValue();
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
