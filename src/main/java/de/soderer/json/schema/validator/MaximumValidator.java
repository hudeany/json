package de.soderer.json.schema.validator;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
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

	public MaximumValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Data for maximum is null", jsonSchemaPath);
		} else if (validatorData instanceof String) {
			try {
				this.validatorData = NumberUtilities.parseNumber((String) validatorData);
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for maximum '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (!(validatorData instanceof Number)) {
			throw new JsonSchemaDefinitionError("Data for maximum '" + validatorData + "' is not a number", jsonSchemaPath);
		}

		if (parentValidatorData.containsKey("exclusiveMaximum")) {
			final Object exclusiveMaximumRaw = parentValidatorData.get("exclusiveMaximum");
			if (exclusiveMaximumRaw == null) {
				throw new JsonSchemaDefinitionError("Property 'exclusiveMaximum' is 'null'", jsonSchemaPath);
			} else if (exclusiveMaximumRaw instanceof Boolean) {
				isExclusiveMaximum = (Boolean) exclusiveMaximumRaw;
			} else {
				throw new JsonSchemaDefinitionError("ExclusiveMaximum data is not 'boolean'", jsonSchemaPath);
			}
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isNumber())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'number' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			final Number dataValue = ((Number) jsonNode.getValue()).doubleValue();
			final Number maximumValue = ((Number) validatorData).doubleValue();

			if (NumberUtilities.compare(dataValue, maximumValue) > 0) {
				throw new JsonSchemaDataValidationError("Maximum number is '" + validatorData + "' but value was '" + jsonNode.getValue() + "'", jsonPath);
			} else if (isExclusiveMaximum && NumberUtilities.compare(dataValue, maximumValue) == 0) {
				throw new JsonSchemaDataValidationError("Exclusive maximum number is '" + validatorData + "' but value was '" + jsonNode.getValue() + "'", jsonPath);
			}
		}
	}
}
