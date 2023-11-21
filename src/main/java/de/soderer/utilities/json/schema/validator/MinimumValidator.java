package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;
import de.soderer.utilities.json.utilities.NumberUtilities;

public class MinimumValidator extends ExtendedBaseJsonSchemaValidator {
	boolean isExclusiveMinimum = false;

	public MinimumValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Data for minimum is null", jsonSchemaPath);
		} else if (validatorData instanceof String) {
			try {
				this.validatorData = NumberUtilities.parseNumber((String) validatorData);
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for minimum '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (!(validatorData instanceof Number)) {
			throw new JsonSchemaDefinitionError("Data for minimum '" + validatorData + "' is not a number", jsonSchemaPath);
		}

		if (parentValidatorData.containsPropertyKey("exclusiveMinimum")) {
			final Object exclusiveMinimumRaw = parentValidatorData.get("exclusiveMinimum");
			if (exclusiveMinimumRaw == null) {
				throw new JsonSchemaDefinitionError("Property 'exclusiveMinimum' is 'null'", jsonSchemaPath);
			} else if (exclusiveMinimumRaw instanceof Boolean) {
				isExclusiveMinimum = (Boolean) exclusiveMinimumRaw;
			} else {
				throw new JsonSchemaDefinitionError("ExclusiveMinimum data is not 'boolean'", jsonSchemaPath);
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
			final Number minimumValue = ((Number) validatorData).doubleValue();

			if (NumberUtilities.compare(dataValue, minimumValue) < 0) {
				throw new JsonSchemaDataValidationError("Minimum number is '" + validatorData + "' but value was '" + jsonNode.getValue() + "'", jsonPath);
			} else if (isExclusiveMinimum && NumberUtilities.compare(dataValue, minimumValue) == 0) {
				throw new JsonSchemaDataValidationError("Exclusive minimum number is '" + validatorData + "' but value was '" + jsonNode.getValue() + "'", jsonPath);
			}
		}
	}
}
