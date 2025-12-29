package de.soderer.json.schema.validator;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.utilities.NumberUtilities;

/**
 * Minimum value for numeric JSON data
 * Optional with boolean "exclusiveMinimum" which defines whether it is an exclusive minimum
 */
public class MinimumValidator extends ExtendedBaseJsonSchemaValidator {
	boolean isExclusiveMinimum = false;

	public MinimumValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Data for minimum is null", jsonSchemaPath);
		} else if (validatorData.isString()) {
			try {
				NumberUtilities.parseNumber(((JsonValueString) validatorData).getValue());
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for minimum '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (!validatorData.isInteger() && !validatorData.isNumber()) {
			throw new JsonSchemaDefinitionError("Data for minimum '" + validatorData + "' is not a number", jsonSchemaPath);
		}

		if (parentValidatorData.containsKey("exclusiveMinimum")) {
			final JsonNode exclusiveMinimumRaw = parentValidatorData.get("exclusiveMinimum");
			if (exclusiveMinimumRaw == null) {
				throw new JsonSchemaDefinitionError("Property 'exclusiveMinimum' is 'null'", jsonSchemaPath);
			} else if (exclusiveMinimumRaw.isBoolean()) {
				isExclusiveMinimum = ((JsonValueBoolean) exclusiveMinimumRaw).getValue();
			} else {
				throw new JsonSchemaDefinitionError("ExclusiveMinimum data is not 'boolean'", jsonSchemaPath);
			}
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (jsonNode.isInteger()) {
			final Number dataValue = ((JsonValueInteger) jsonNode).getValue().doubleValue();
			final Number minimumValue;
			if (validatorData.isString()) {
				minimumValue = NumberUtilities.parseNumber(((JsonValueString) validatorData).getValue()).doubleValue();
			} else if (validatorData.isInteger()) {
				minimumValue = ((JsonValueInteger) validatorData).getValue().doubleValue();
			} else {
				minimumValue = ((JsonValueNumber) validatorData).getValue().doubleValue();
			}

			if (NumberUtilities.compare(dataValue, minimumValue) < 0) {
				throw new JsonSchemaDataValidationError("Minimum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
			} else if (isExclusiveMinimum && NumberUtilities.compare(dataValue, minimumValue) == 0) {
				throw new JsonSchemaDataValidationError("Exclusive minimum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
			}
		} else if (jsonNode.isNumber()) {
			final Number dataValue = ((JsonValueNumber) jsonNode).getValue().doubleValue();
			final Number minimumValue;
			if (validatorData.isString()) {
				minimumValue = NumberUtilities.parseNumber(((JsonValueString) validatorData).getValue()).doubleValue();
			} else if (validatorData.isInteger()) {
				minimumValue = ((JsonValueInteger) validatorData).getValue().doubleValue();
			} else {
				minimumValue = ((JsonValueNumber) validatorData).getValue().doubleValue();
			}

			if (NumberUtilities.compare(dataValue, minimumValue) < 0) {
				throw new JsonSchemaDataValidationError("Minimum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
			} else if (isExclusiveMinimum && NumberUtilities.compare(dataValue, minimumValue) == 0) {
				throw new JsonSchemaDataValidationError("Exclusive minimum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
			}
		} else {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'number' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		}
	}
}
