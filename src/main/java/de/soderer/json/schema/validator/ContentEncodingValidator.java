package de.soderer.json.schema.validator;

import java.util.Base64;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.utilities.Utilities;

/**
 * Validator for the "contentEncoding" keyword (since JSON schema draft 7): defines the encoding of
 * string data. Only "base64" is checked, other encodings are accepted without checks. Data that is
 * not a string is ignored.
 */
public class ContentEncodingValidator extends BaseJsonSchemaValidator {
	/**
	 * Creates a new "contentEncoding" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, the name of the encoding
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a non-blank string
	 */
	public ContentEncodingValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("ContentEncoding value is 'null'", jsonSchemaPath);
		} else if (!validatorData.isString()) {
			throw new JsonSchemaDefinitionError("ContentEncoding value is not a string", jsonSchemaPath);
		} else if (Utilities.isBlank(((JsonValueString) validatorData).getValue())) {
			throw new JsonSchemaDefinitionError("Invalid ContentEncoding '" + validatorData + "'", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!jsonNode.isString()) {
			// ContentEncoding only applies to string values
			return;
		}

		if ("base64".equalsIgnoreCase(((JsonValueString) validatorData).getValue())) {
			try {
				Base64.getDecoder().decode(((JsonValueString) jsonNode).getValue());
			} catch (final IllegalArgumentException e) {
				throw new JsonSchemaDataValidationError("Invalid base64 encoded data: " + e.getMessage(), jsonPath, e);
			}
		}
	}
}
