package de.soderer.json.schema.validator;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Validator for a boolean schema (since JSON schema draft 6): true accepts any JSON data node,
 * false rejects any JSON data node.
 */
public class BooleanValidator extends BaseJsonSchemaValidator {
	/**
	 * Creates a new boolean schema validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the boolean schema within the JSON schema
	 * @param validatorData
	 *            the boolean schema value
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a boolean
	 */
	public BooleanValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("BooleanValidator data is 'null'", jsonSchemaPath);
		} else if (!(validatorData.isBoolean())) {
			throw new JsonSchemaDefinitionError("BooleanValidator data is not 'boolean'", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!((JsonValueBoolean) validatorData).getValue()) {
			throw new JsonSchemaDataValidationError("JSON schema is boolean 'false' at '" + jsonSchemaPath + "', no data is valid against it", jsonPath);
		}
	}
}
