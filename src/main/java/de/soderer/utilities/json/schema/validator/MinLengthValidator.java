package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;
import de.soderer.utilities.json.utilities.TextUtilities;

/**
 * Minimum length of string value
 */
public class MinLengthValidator extends BaseJsonSchemaValidator {
	public MinLengthValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Data for minLength is 'null'", jsonSchemaPath);
		} else if (validatorData instanceof String) {
			try {
				this.validatorData = Integer.parseInt((String) validatorData);
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for minLength '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData instanceof Number) {
			final int minimumLengthValue = ((Number) validatorData).intValue();
			if (minimumLengthValue < 0) {
				throw new JsonSchemaDefinitionError("Data for minLength is negative", jsonSchemaPath);
			} else {
				this.validatorData = minimumLengthValue;
			}
		} else {
			throw new JsonSchemaDefinitionError("Data for minLength '" + validatorData + "' is not a number", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isString())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'string' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			if (TextUtilities.getUnicodeStringLength((String) jsonNode.getValue()) < ((Integer) validatorData)) {
				throw new JsonSchemaDataValidationError("String minLength is '" + validatorData + "' but was '" + ((String) jsonNode.getValue()).length() + "'", jsonPath);
			}
		}
	}
}
