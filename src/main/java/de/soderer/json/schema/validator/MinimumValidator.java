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
 * Validator for the "minimum" keyword: the JSON data number must be at least the given value. Data
 * that is not a number is ignored, except in simple mode.<br />
 * <br />
 * Up to draft 4 a boolean "exclusiveMinimum" in the same schema object makes the minimum exclusive.
 * Since draft 6 "exclusiveMinimum" is a number of its own, handled by
 * {@link ExclusiveMinimumValidator}.
 */
public class MinimumValidator extends ExtendedBaseJsonSchemaValidator {
	/** The minimum value, parsed once from the schema. */
	private final Number minimumValue;

	/** Whether the minimum is exclusive (boolean "exclusiveMinimum" up to draft 4). */
	private boolean isExclusiveMinimum = false;

	/**
	 * Creates a new "minimum" validator.
	 *
	 * @param parentValidatorData
	 *            the parent schema containing this keyword, used to read its "exclusiveMinimum"
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, a number or a string containing a number
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a number, or "exclusiveMinimum" is neither boolean nor number
	 */
	public MinimumValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Data for minimum is null", jsonSchemaPath);
		} else if (validatorData.isString()) {
			try {
				minimumValue = NumberUtilities.parseNumber(((JsonValueString) validatorData).getValue());
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for minimum '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData.isInteger()) {
			minimumValue = ((JsonValueInteger) validatorData).getValue();
		} else if (validatorData.isNumber()) {
			minimumValue = ((JsonValueNumber) validatorData).getValue();
		} else {
			throw new JsonSchemaDefinitionError("Data for minimum '" + validatorData + "' is not a number", jsonSchemaPath);
		}

		if (parentValidatorData.containsKey("exclusiveMinimum")) {
			final JsonNode exclusiveMinimumRaw = parentValidatorData.get("exclusiveMinimum");
			if (exclusiveMinimumRaw == null) {
				throw new JsonSchemaDefinitionError("Property 'exclusiveMinimum' is 'null'", jsonSchemaPath);
			} else if (exclusiveMinimumRaw.isBoolean()) {
				isExclusiveMinimum = ((JsonValueBoolean) exclusiveMinimumRaw).getValue();
			} else if (exclusiveMinimumRaw.isInteger() || exclusiveMinimumRaw.isNumber()) {
				// Numeric "exclusiveMinimum" (since draft 6) is an independent keyword, handled by ExclusiveMinimumValidator
			} else {
				throw new JsonSchemaDefinitionError("ExclusiveMinimum data is not 'boolean'", jsonSchemaPath);
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

		final int comparison = NumberUtilities.compare(dataValue, minimumValue);
		if (comparison < 0) {
			throw new JsonSchemaDataValidationError("Minimum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
		} else if (isExclusiveMinimum && comparison == 0) {
			throw new JsonSchemaDataValidationError("Exclusive minimum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
		}
	}
}
