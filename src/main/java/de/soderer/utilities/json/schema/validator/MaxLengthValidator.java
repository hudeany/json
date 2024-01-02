package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;
import de.soderer.utilities.json.utilities.TextUtilities;

/**
 * Maximum length of string value
 */
public class MaxLengthValidator extends BaseJsonSchemaValidator {
	public MaxLengthValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Data for maxLength is 'null'", jsonSchemaPath);
		} else if (validatorData instanceof String) {
			try {
				this.validatorData = Integer.parseInt((String) validatorData);
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for maxLength '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData instanceof Number) {
			final int maximumLengthValue = ((Number) validatorData).intValue();
			if (maximumLengthValue < 0) {
				throw new JsonSchemaDefinitionError("Data for maxLength is negative", jsonSchemaPath);
			} else {
				this.validatorData = maximumLengthValue;
			}
		} else {
			throw new JsonSchemaDefinitionError("Data for maxLength '" + validatorData + "' is not a number", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isString())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'string' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			if (TextUtilities.getUnicodeStringLength((String) jsonNode.getValue()) > ((Integer) validatorData)) {
				throw new JsonSchemaDataValidationError("String maxLength is '" + validatorData + "' but was '" + ((String) jsonNode.getValue()).length() + "'", jsonPath);
			}
		}
	}
}
