package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;
import de.soderer.utilities.json.utilities.NumberUtilities;

/**
 * before JSON schema draft v6:
 * Boolean value that defines whether the value of maximum is to be used as an exclusive maximum in class MaximumValidator
 *
 * with JSON schema draft v6:
 * Numeric value that defines a value to be used as an exclusive maximum
 */
public class ExclusiveMaximumValidator extends ExtendedBaseJsonSchemaValidator {
	public ExclusiveMaximumValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Data for exclusiveMaximum is null", jsonSchemaPath);
		} else if (validatorData instanceof Boolean) {
			throw new JsonSchemaDefinitionError("Data for exclusiveMaximum '" + validatorData + "' is not a number. Boolean data for exclusiveMinimum is only valid up to JSON schema version draft v4", jsonSchemaPath);
		} else if (validatorData instanceof String) {
			try {
				this.validatorData = NumberUtilities.parseNumber((String) validatorData);
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for exclusiveMaximum '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (!(validatorData instanceof Number)) {
			throw new JsonSchemaDefinitionError("Data for exclusiveMaximum '" + validatorData + "' is not a number", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isNumber())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'number' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			final Number dataValue = (Number) jsonNode.getValue();
			final Number exclusiveMaximumValue = (Number) validatorData;

			if (NumberUtilities.compare(dataValue, exclusiveMaximumValue) >= 0) {
				throw new JsonSchemaDataValidationError("ExclusiveMaximum number is '" + validatorData + "' but value was '" + jsonNode.getValue() + "'", jsonPath);
			}
		}
	}
}
