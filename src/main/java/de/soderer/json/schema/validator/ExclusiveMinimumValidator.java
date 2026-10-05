package de.soderer.json.schema.validator;

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
import de.soderer.json.utilities.NumberUtilities;

/**
 * Validator for the "exclusiveMinimum" keyword since JSON schema draft 6: a numeric value the JSON
 * data number must be strictly greater than. Data that is not a number is ignored, except in simple
 * mode.<br />
 * <br />
 * Up to draft 4 "exclusiveMinimum" was a boolean modifier of "minimum", which is handled by
 * {@link MinimumValidator}.
 */
public class ExclusiveMinimumValidator extends ExtendedBaseJsonSchemaValidator {
	/** The exclusive minimum value, parsed once from the schema. */
	private final Number exclusiveMinimumValue;

	/**
	 * Creates a new "exclusiveMinimum" validator.
	 *
	 * @param parentValidatorData
	 *            the parent schema containing this keyword
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, a number or a string containing a number
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a number (a boolean value is only valid up to draft 4)
	 */
	public ExclusiveMinimumValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Data for exclusiveMinimum is null", jsonSchemaPath);
		} else if (validatorData.isBoolean()) {
			throw new JsonSchemaDefinitionError("Data for exclusiveMinimum '" + validatorData + "' is not a number. Boolean data for exclusiveMinimum is only valid up to JSON schema version draft v4", jsonSchemaPath);
		} else if (validatorData.isString()) {
			try {
				exclusiveMinimumValue = NumberUtilities.parseNumber(((JsonValueString) validatorData).getValue());
			} catch (final NumberFormatException e) {
				throw new JsonSchemaDefinitionError("Data for exclusiveMinimum '" + validatorData + "' is not a number", jsonSchemaPath, e);
			}
		} else if (validatorData.isInteger()) {
			exclusiveMinimumValue = ((JsonValueInteger) validatorData).getValue();
		} else if (validatorData.isNumber()) {
			exclusiveMinimumValue = ((JsonValueNumber) validatorData).getValue();
		} else {
			throw new JsonSchemaDefinitionError("Data for exclusiveMinimum '" + validatorData + "' is not a number", jsonSchemaPath);
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

		if (NumberUtilities.compare(dataValue, exclusiveMinimumValue) <= 0) {
			throw new JsonSchemaDataValidationError("ExclusiveMinimum number is '" + validatorData + "' but value was '" + dataValue.toString() + "'", jsonPath);
		}
	}
}
