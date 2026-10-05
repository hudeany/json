package de.soderer.json.schema.validator;

import java.math.BigDecimal;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.utilities.TextUtilities;

/**
 * Validator for the "maxLength" keyword: the JSON data string may contain at most the given number
 * of characters, counted as unicode code points. Data that is not a string is ignored, except in
 * simple mode.
 */
public class MaxLengthValidator extends BaseJsonSchemaValidator {
	/** The limit, parsed once from the schema. */
	private final int maximumLengthValue;

	/**
	 * Creates a new "maxLength" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, a non-negative integer or a string containing one
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a non-negative integer
	 */
	public MaxLengthValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		final BigDecimal limit;
		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Data for maxLength is 'null'", jsonSchemaPath);
		} else if (validatorData.isString()) {
			try {
				limit = new BigDecimal(((JsonValueString) validatorData).getValue().trim());
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for maxLength '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData.isInteger()) {
			limit = new BigDecimal(((JsonValueInteger) validatorData).getValue().toString());
		} else if (validatorData.isNumber()) {
			limit = new BigDecimal(((JsonValueNumber) validatorData).getValue().toString());
		} else {
			throw new JsonSchemaDefinitionError("Data for maxLength '" + validatorData + "' is not a number", jsonSchemaPath);
		}

		if (limit.signum() < 0) {
			throw new JsonSchemaDefinitionError("Data for maxLength is negative", jsonSchemaPath);
		} else if (limit.stripTrailingZeros().scale() > 0) {
			// Numbers with zero fraction like 2.0 are allowed, 2.5 is not
			throw new JsonSchemaDefinitionError("Data for maxLength '" + validatorData + "' is not an integer", jsonSchemaPath);
		}
		// Limits beyond int range can never be exceeded resp. reached by Java collections and strings
		maximumLengthValue = limit.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0 ? Integer.MAX_VALUE : limit.intValueExact();
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isString())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'string' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			final int unicodeStringLength = TextUtilities.getUnicodeStringLength(((JsonValueString) jsonNode).getValue());
			if (unicodeStringLength > maximumLengthValue) {
				throw new JsonSchemaDataValidationError("String maxLength is '" + validatorData + "' but was '" + unicodeStringLength + "'", jsonPath);
			}
		}
	}
}
