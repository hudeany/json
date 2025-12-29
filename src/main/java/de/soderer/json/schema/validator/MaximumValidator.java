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
 * Maximum value for numeric JSON data
 * Optional with boolean "exclusiveMaximum" which defines whether it is an exclusive maximum
 */
public class MaximumValidator extends ExtendedBaseJsonSchemaValidator {
	boolean isExclusiveMaximum = false;

	public MaximumValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Data for maximum is null", jsonSchemaPath);
		} else if (validatorData.isString()) {
			try {
				NumberUtilities.parseNumber(((JsonValueString) validatorData).getValue());
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for maximum '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (!validatorData.isInteger() && !validatorData.isNumber()) {
			throw new JsonSchemaDefinitionError("Data for maximum '" + validatorData + "' is not a number", jsonSchemaPath);
		}

		if (parentValidatorData.containsKey("exclusiveMaximum")) {
			final JsonNode exclusiveMaximumRaw = parentValidatorData.get("exclusiveMaximum");
			if (exclusiveMaximumRaw == null) {
				throw new JsonSchemaDefinitionError("Property 'exclusiveMaximum' is 'null'", jsonSchemaPath);
			} else if (exclusiveMaximumRaw.isBoolean()) {
				isExclusiveMaximum = ((JsonValueBoolean) exclusiveMaximumRaw).getValue();
			} else {
				throw new JsonSchemaDefinitionError("ExclusiveMaximum data is not 'boolean'", jsonSchemaPath);
			}
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (jsonNode.isInteger()) {
			final Number dataValue = ((JsonValueInteger) jsonNode).getValue().doubleValue();
			final Number maximumValue;
			if (validatorData.isString()) {
				maximumValue = NumberUtilities.parseNumber(((JsonValueString) validatorData).getValue()).doubleValue();
			} else if (validatorData.isInteger()) {
				maximumValue = ((JsonValueInteger) validatorData).getValue().doubleValue();
			} else {
				maximumValue = ((JsonValueNumber) validatorData).getValue().doubleValue();
			}

			if (NumberUtilities.compare(dataValue, maximumValue) > 0) {
				throw new JsonSchemaDataValidationError("Maximum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
			} else if (isExclusiveMaximum && NumberUtilities.compare(dataValue, maximumValue) == 0) {
				throw new JsonSchemaDataValidationError("Exclusive maximum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
			}
		} else if (jsonNode.isNumber()) {
			final Number dataValue = ((JsonValueNumber) jsonNode).getValue().doubleValue();
			final Number maximumValue;
			if (validatorData.isString()) {
				maximumValue = NumberUtilities.parseNumber(((JsonValueString) validatorData).getValue()).doubleValue();
			} else if (validatorData.isInteger()) {
				maximumValue = ((JsonValueInteger) validatorData).getValue().doubleValue();
			} else {
				maximumValue = ((JsonValueNumber) validatorData).getValue().doubleValue();
			}

			if (NumberUtilities.compare(dataValue, maximumValue) > 0) {
				throw new JsonSchemaDataValidationError("Maximum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
			} else if (isExclusiveMaximum && NumberUtilities.compare(dataValue, maximumValue) == 0) {
				throw new JsonSchemaDataValidationError("Exclusive maximum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
			}
		} else {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'number' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		}
	}
}
