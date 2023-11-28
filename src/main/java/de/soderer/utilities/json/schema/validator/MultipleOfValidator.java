package de.soderer.utilities.json.schema.validator;

import java.math.BigDecimal;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

/**
 * Numeric value is a multiple of some defined value
 */
public class MultipleOfValidator extends BaseJsonSchemaValidator {
	public MultipleOfValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Data for multipleOf is null", jsonSchemaPath);
		} else if (validatorData instanceof String) {
			try {
				this.validatorData = new BigDecimal((String) validatorData);
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for multipleOf '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (!(validatorData instanceof Number)) {
			throw new JsonSchemaDefinitionError("Data for multipleOf '" + validatorData + "' is not a number", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isNumber())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'number' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			if (new BigDecimal(jsonNode.getValue().toString()).remainder(new BigDecimal(validatorData.toString())).compareTo(BigDecimal.ZERO) != 0) {
				throw new JsonSchemaDataValidationError("Number must be multiple of '" + ((Number) validatorData).toString() + "' but value was '" + jsonNode.getValue() + "'", jsonPath);
			}
		}
	}
}
