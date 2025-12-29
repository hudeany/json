package de.soderer.json.schema.validator;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueFloat;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.utilities.NumberUtilities;

/**
 * before JSON schema draft v6:
 * Boolean value that defines whether the value of maximum is to be used as an exclusive maximum in class MaximumValidator
 *
 * with JSON schema draft v6:
 * Numeric value that defines a value to be used as an exclusive maximum
 */
public class ExclusiveMaximumValidator extends ExtendedBaseJsonSchemaValidator {
	public ExclusiveMaximumValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Data for exclusiveMaximum is null", jsonSchemaPath);
		} else if (validatorData.isBoolean()) {
			throw new JsonSchemaDefinitionError("Data for exclusiveMaximum '" + validatorData + "' is not a number. Boolean data for exclusiveMinimum is only valid up to JSON schema version draft v4", jsonSchemaPath);
		} else if (validatorData.isString()) {
			try {
				NumberUtilities.parseNumber(((JsonValueString) validatorData).getValue());
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for exclusiveMaximum '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (!validatorData.isInteger() && !validatorData.isFloat()) {
			throw new JsonSchemaDefinitionError("Data for exclusiveMaximum '" + validatorData + "' is not a number", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (jsonNode.isInteger()) {
			final Number dataValue = ((JsonValueInteger) jsonNode).getValue();
			final Number exclusiveMaximumValue;
			if (validatorData.isString()) {
				exclusiveMaximumValue = NumberUtilities.parseNumber(((JsonValueString) validatorData).getValue());
			} else if (validatorData.isInteger()) {
				exclusiveMaximumValue = ((JsonValueInteger) validatorData).getValue();
			} else if (validatorData.isFloat()) {
				exclusiveMaximumValue = ((JsonValueFloat) validatorData).getValue();
			} else {
				throw new JsonSchemaDataValidationError("ExclusiveMaximum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
			}

			if (NumberUtilities.compare(dataValue, exclusiveMaximumValue) >= 0) {
				throw new JsonSchemaDataValidationError("ExclusiveMaximum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
			}
		} else if (jsonNode.isFloat()) {
			final Number dataValue = ((JsonValueFloat) jsonNode).getValue();
			final Number exclusiveMaximumValue;
			if (validatorData.isString()) {
				exclusiveMaximumValue = NumberUtilities.parseNumber(((JsonValueString) validatorData).getValue());
			} else if (validatorData.isInteger()) {
				exclusiveMaximumValue = ((JsonValueInteger) validatorData).getValue();
			} else if (validatorData.isFloat()) {
				exclusiveMaximumValue = ((JsonValueFloat) validatorData).getValue();
			} else {
				throw new JsonSchemaDataValidationError("ExclusiveMaximum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
			}

			if (NumberUtilities.compare(dataValue, exclusiveMaximumValue) >= 0) {
				throw new JsonSchemaDataValidationError("ExclusiveMaximum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
			}
		} else {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'number' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		}
	}
}
