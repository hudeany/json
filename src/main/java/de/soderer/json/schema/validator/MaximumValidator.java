package de.soderer.json.schema.validator;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.utilities.NumberUtilities;

/**
 * Validator for the "maximum" keyword: the JSON data number must be at most the given value. Data
 * that is not a number is ignored, except in simple mode.<br />
 * <br />
 * Up to draft 4 a boolean "exclusiveMaximum" in the same schema object makes the maximum exclusive.
 * Since draft 6 "exclusiveMaximum" is a number of its own, handled by
 * {@link ExclusiveMaximumValidator}.
 */
public class MaximumValidator extends ExtendedBaseJsonSchemaValidator {
	/** The maximum value, parsed once from the schema. */
	private final Number maximumValue;

	/** Whether the maximum is exclusive (boolean "exclusiveMaximum" up to draft 4). */
	private boolean isExclusiveMaximum = false;

	/**
	 * Creates a new "maximum" validator.
	 *
	 * @param parentValidatorData
	 *            the parent schema containing this keyword, used to read its "exclusiveMaximum"
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, a number or a string containing a number
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a number, or "exclusiveMaximum" is neither boolean nor number
	 */
	public MaximumValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Data for maximum is null", jsonSchemaPath);
		} else if (validatorData.isString()) {
			try {
				maximumValue = NumberUtilities.parseNumber(((JsonValueString) validatorData).getValue());
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for maximum '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData.isInteger()) {
			maximumValue = ((JsonValueInteger) validatorData).getValue();
		} else if (validatorData.isNumber()) {
			maximumValue = ((JsonValueNumber) validatorData).getValue();
		} else {
			throw new JsonSchemaDefinitionError("Data for maximum '" + validatorData + "' is not a number", jsonSchemaPath);
		}

		if (parentValidatorData.containsKey("exclusiveMaximum")) {
			final JsonNode exclusiveMaximumRaw = parentValidatorData.get("exclusiveMaximum");
			if (exclusiveMaximumRaw == null) {
				throw new JsonSchemaDefinitionError("Property 'exclusiveMaximum' is 'null'", jsonSchemaPath);
			} else if (exclusiveMaximumRaw.isBoolean()) {
				isExclusiveMaximum = ((JsonValueBoolean) exclusiveMaximumRaw).getValue();
			} else if (exclusiveMaximumRaw.isInteger() || exclusiveMaximumRaw.isNumber()) {
				// Numeric "exclusiveMaximum" (since draft 6) is an independent keyword, handled by ExclusiveMaximumValidator
			} else {
				throw new JsonSchemaDefinitionError("ExclusiveMaximum data is not 'boolean'", jsonSchemaPath);
			}
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		final Number dataValue;
		if (jsonNode.isInteger()) {
			dataValue = ((JsonValueInteger) jsonNode).getValue();
		} else if (jsonNode.isNumber()) {
			dataValue = ((JsonValueNumber) jsonNode).getValue();
		} else {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'number' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
			return;
		}

		final int comparison = NumberUtilities.compare(dataValue, maximumValue);
		if (comparison > 0) {
			throw new JsonSchemaDataValidationError("Maximum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
		} else if (isExclusiveMaximum && comparison == 0) {
			throw new JsonSchemaDataValidationError("Exclusive maximum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
		}
	}
}
