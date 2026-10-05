package de.soderer.json.schema.validator;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Base class of JSON schema validators whose keyword depends on other keywords of the same schema
 * object, e.g. "additionalProperties" on "properties" and "patternProperties".
 */
public abstract class ExtendedBaseJsonSchemaValidator extends BaseJsonSchemaValidator {
	/** The schema object containing this validator's keyword and its sibling keywords. */
	protected JsonObject parentValidatorData;

	/**
	 * Creates a new validator with access to its parent schema object.
	 *
	 * @param parentValidatorData
	 *            the schema object containing this keyword
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword in the JSON schema definition
	 * @throws JsonSchemaDefinitionError
	 *             if the parent schema object or the validator data is null
	 */
	protected ExtendedBaseJsonSchemaValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (parentValidatorData == null) {
			throw new JsonSchemaDefinitionError("ParentValidatorData is 'null'", jsonSchemaPath);
		} else {
			this.parentValidatorData = parentValidatorData;
		}
	}
}
