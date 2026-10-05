package de.soderer.json.schema.validator;

import java.math.BigDecimal;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Validator for the "minProperties" keyword: the JSON data object must contain at least the given
 * number of properties. Data that is not an object is ignored, except in simple mode.
 */
public class MinPropertiesValidator extends BaseJsonSchemaValidator {
	/** The limit, parsed once from the schema. */
	private final int minimumPropertiesValue;

	/**
	 * Creates a new "minProperties" validator.
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
	public MinPropertiesValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		final BigDecimal limit;
		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Data for minimum property keys is 'null'", jsonSchemaPath);
		} else if (validatorData.isString()) {
			try {
				limit = new BigDecimal(((JsonValueString) validatorData).getValue().trim());
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for minimum property keys '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData.isInteger()) {
			limit = new BigDecimal(((JsonValueInteger) validatorData).getValue().toString());
		} else if (validatorData.isNumber()) {
			limit = new BigDecimal(((JsonValueNumber) validatorData).getValue().toString());
		} else {
			throw new JsonSchemaDefinitionError("Data for minimum property keys '" + validatorData + "' is not a number", jsonSchemaPath);
		}

		if (limit.signum() < 0) {
			throw new JsonSchemaDefinitionError("Data for minimum property keys is negative", jsonSchemaPath);
		} else if (limit.stripTrailingZeros().scale() > 0) {
			// Numbers with zero fraction like 2.0 are allowed, 2.5 is not
			throw new JsonSchemaDefinitionError("Data for minimum property keys '" + validatorData + "' is not an integer", jsonSchemaPath);
		}
		// Limits beyond int range can never be exceeded resp. reached by Java collections and strings
		minimumPropertiesValue = limit.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0 ? Integer.MAX_VALUE : limit.intValueExact();
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonObject())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'object' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			if (((JsonObject) jsonNode).size() < minimumPropertiesValue) {
				throw new JsonSchemaDataValidationError("Required minimum number of properties is '" + validatorData + "' but was '" + ((JsonObject) jsonNode).size() + "'", jsonPath);
			}
		}
	}
}
