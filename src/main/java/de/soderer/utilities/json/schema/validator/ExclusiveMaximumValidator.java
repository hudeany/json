package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;
import de.soderer.utilities.json.utilities.NumberUtilities;

public class ExclusiveMaximumValidator extends ExtendedBaseJsonSchemaValidator {
	public ExclusiveMaximumValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData, final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData, jsonNode, jsonPath);

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
	public void validate() throws JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		if (!(jsonNode.isNumber())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'number' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			final Number dataValue = ((Number) jsonNode.getValue()).doubleValue();
			final Number exclusiveMaximumValue = ((Number) validatorData).doubleValue();

			if (NumberUtilities.compare(dataValue, exclusiveMaximumValue) >= 0) {
				throw new JsonSchemaDataValidationError("ExclusiveMaximum number is '" + validatorData + "' but value was '" + jsonNode.getValue() + "'", jsonPath);
			}
		}
	}
}