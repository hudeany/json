package de.soderer.json.schema.validator;

import java.math.BigDecimal;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Validator for the "multipleOf" keyword (named "divisibleBy" in draft 3): the JSON data number must
 * be a multiple of the given positive number. The check uses exact decimal arithmetic, so 0.3 is a
 * multiple of 0.1. Data that is not a number is ignored, except in simple mode.
 */
public class MultipleOfValidator extends BaseJsonSchemaValidator {
	/** The divisor, parsed once from the schema. */
	private final BigDecimal checkNumber;

	/**
	 * Creates a new "multipleOf" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, a positive number or a string containing one
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a number greater than 0
	 */
	public MultipleOfValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Data for multipleOf is null", jsonSchemaPath);
		} else if (validatorData.isString()) {
			try {
				checkNumber = new BigDecimal(((JsonValueString) validatorData).getValue());
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for multipleOf '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData.isInteger()) {
			checkNumber = new BigDecimal(((JsonValueInteger) validatorData).getValue().toString());
		} else if (validatorData.isNumber()) {
			checkNumber = new BigDecimal(((JsonValueNumber) validatorData).getValue().toString());
		} else {
			throw new JsonSchemaDefinitionError("Data for multipleOf '" + validatorData + "' is not a number", jsonSchemaPath);
		}

		if (checkNumber.compareTo(BigDecimal.ZERO) <= 0) {
			throw new JsonSchemaDefinitionError("Data for multipleOf '" + validatorData + "' must be a positive number greater than 0", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (jsonNode.isInteger()) {
			if (new BigDecimal(((JsonValueInteger) jsonNode).getValue().toString()).remainder(checkNumber).compareTo(BigDecimal.ZERO) != 0) {
				throw new JsonSchemaDataValidationError("Number must be multiple of '" + checkNumber.toString() + "' but value was '" + ((JsonValueInteger) jsonNode).getValue().toString() + "'", jsonPath);
			}
		} else if (jsonNode.isNumber()) {
			if (new BigDecimal(((JsonValueNumber) jsonNode).getValue().toString()).remainder(checkNumber).compareTo(BigDecimal.ZERO) != 0) {
				throw new JsonSchemaDataValidationError("Number must be multiple of '" + checkNumber.toString() + "' but value was '" + ((JsonValueNumber) jsonNode).getValue().toString() + "'", jsonPath);
			}
		} else if (jsonSchemaDependencyResolver.isSimpleMode()) {
			throw new JsonSchemaDataValidationError("Expected data type 'number' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
		}
	}
}
