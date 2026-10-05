package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueString;
import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Validator for the "propertyNames" keyword (since draft 6): every property name of the JSON data
 * object, as a string, must match the given schema. Data that is not an object is ignored, except
 * in simple mode.
 */
public class PropertyNamesValidator extends BaseJsonSchemaValidator {
	/** Validators of the schema all property names must match. */
	private final List<BaseJsonSchemaValidator> subValidators;

	/**
	 * Creates a new "propertyNames" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, a schema object or a boolean
	 * @throws JsonSchemaDefinitionError
	 *             if the value is neither object nor boolean, or the schema is invalid
	 * @throws DuplicateKeyException
	 *             if a schema contains duplicate keys
	 */
	public PropertyNamesValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("PropertyNames validator data is 'null'", jsonSchemaPath);
		} else if (validatorData.isJsonObject()) {
			subValidators = JsonSchema.createValidators((JsonObject) validatorData, jsonSchemaDependencyResolver, jsonSchemaPath);
		} else if (validatorData.isBoolean()) {
			subValidators = new ArrayList<>();
			subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData));
		} else {
			throw new JsonSchemaDefinitionError("PropertyNames validator data is not a JsonObject or Boolean", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonObject())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'object' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			for (final String propertyName : ((JsonObject) jsonNode).keySet()) {
				final JsonNode newJsonNode = new JsonValueString(propertyName).withRootNode(false);
				for (final BaseJsonSchemaValidator subValidator : subValidators) {
					// The path of the property shows which property name is invalid
					subValidator.validate(newJsonNode, new JsonPath(jsonPath).addPropertyKey(propertyName));
				}
			}
		}
	}
}
