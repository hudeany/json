package de.soderer.json.schema.validator;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Minimum number of JSON opbject properties
 */
public class MinPropertiesValidator extends BaseJsonSchemaValidator {
	public MinPropertiesValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Data for minimum property keys is 'null'", jsonSchemaPath);
		} else if (validatorData.isString()) {
			try {
				Integer.parseInt(((JsonValueString) validatorData).getValue());
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for minimum property keys '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData.isInteger()) {
			final int minimumPropertiesValue = ((JsonValueInteger) validatorData).getValue().intValue();
			if (minimumPropertiesValue < 0) {
				throw new JsonSchemaDefinitionError("Data for minimum property keys is negative", jsonSchemaPath);
			}
		} else if (validatorData.isNumber()) {
			final int minimumPropertiesValue = ((JsonValueNumber) validatorData).getValue().intValue();
			if (minimumPropertiesValue < 0) {
				throw new JsonSchemaDefinitionError("Data for minimum property keys is negative", jsonSchemaPath);
			}
		} else {
			throw new JsonSchemaDefinitionError("Data for minimum property keys '" + validatorData + "' is not a number", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		final int minimumPropertiesValue;
		if (validatorData.isString()) {
			minimumPropertiesValue = Integer.parseInt(((JsonValueString) validatorData).getValue());
		} else if (validatorData.isInteger()) {
			minimumPropertiesValue = ((JsonValueInteger) validatorData).getValue().intValue();
		} else {
			minimumPropertiesValue = ((JsonValueNumber) validatorData).getValue().intValue();
		}

		if (!(jsonNode.isJsonObject())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'object' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			if (((JsonObject) jsonNode).size() < minimumPropertiesValue) {
				throw new JsonSchemaDataValidationError("Required minimum number of properties is '" + validatorData + "' but was '" + ((JsonObject) jsonNode).size() + "'", jsonPath);
			}
		}
	}
}
