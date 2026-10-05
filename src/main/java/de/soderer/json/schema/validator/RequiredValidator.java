package de.soderer.json.schema.validator;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Validator for the "required" keyword (since draft 4): its value is an array of property names the
 * JSON data object must contain. Data that is not an object is ignored, except in simple mode.<br />
 * <br />
 * In draft 3 "required" is a boolean inside a property schema, which is handled by
 * {@link PropertiesValidator}.
 */
public class RequiredValidator extends BaseJsonSchemaValidator {
	/**
	 * Creates a new "required" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, an array of property names (a boolean in draft 3)
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not an array of strings
	 */
	public RequiredValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (jsonSchemaDependencyResolver.isDraftV3Mode() && validatorData.isBoolean()) {
			// Handled in PropertiesValidator
		} else if (!(validatorData.isJsonArray())) {
			throw new JsonSchemaDefinitionError("Data for required property keys is not a JsonArray", jsonSchemaPath);
		} else {
			// Check the property names once here, so a schema error is reported as such and not as data error
			int index = 0;
			for (final JsonNode propertyKey : ((JsonArray) validatorData).items()) {
				if (propertyKey == null || !propertyKey.isString()) {
					throw new JsonSchemaDefinitionError("Data entry for required property key name must be 'string' but was '" + (propertyKey == null ? "null" : propertyKey.getJsonDataType().getName()) + "'", new JsonSchemaPath(jsonSchemaPath).addArrayIndex(index));
				}
				index++;
			}
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (jsonSchemaDependencyResolver.isDraftV3Mode() && validatorData.isBoolean()) {
			// Handled in PropertiesValidator, nothing to do here (validatorData is not a JsonArray in this case)
			return;
		}

		if (!(jsonNode.isJsonObject())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'object' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			for (final JsonNode propertyKey : ((JsonArray) validatorData).items()) {
				// All items are strings, checked in the constructor
				if (!((JsonObject) jsonNode).containsKey(((JsonValueString) propertyKey).getValue())) {
					throw new JsonSchemaDataValidationError("Invalid property key. Missing required property '" + propertyKey + "'", jsonPath);
				}
			}
		}
	}
}
