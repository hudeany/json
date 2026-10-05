package de.soderer.json.schema.validator;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.utilities.Utilities;

/**
 * Validator for the "contentMediaType" keyword (since JSON schema draft 7): defines the media type
 * of string data. Only "application/json" is checked, other media types are accepted without
 * checks. If the parent schema defines "contentEncoding" "base64", the data is decoded before the
 * check. Data that is not a string is ignored.
 */
public class ContentMediaTypeValidator extends ExtendedBaseJsonSchemaValidator {
	/**
	 * Creates a new "contentMediaType" validator.
	 *
	 * @param parentValidatorData
	 *            the parent schema containing this keyword, used to read its "contentEncoding"
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, the media type
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a non-blank string
	 */
	public ContentMediaTypeValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("ContentMediaType value is 'null'", jsonSchemaPath);
		} else if (!validatorData.isString()) {
			throw new JsonSchemaDefinitionError("ContentMediaType value is not a string", jsonSchemaPath);
		} else if (Utilities.isBlank(((JsonValueString) validatorData).getValue())) {
			throw new JsonSchemaDefinitionError("Invalid ContentMediaType '" + validatorData + "'", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!jsonNode.isString()) {
			// ContentMediaType only applies to string values
			return;
		}

		if ("application/json".equalsIgnoreCase(((JsonValueString) validatorData).getValue())) {
			String content = ((JsonValueString) jsonNode).getValue();

			final Object contentEncoding = parentValidatorData.containsKey("contentEncoding") ? parentValidatorData.getSimpleValue("contentEncoding") : null;
			if (contentEncoding instanceof String && "base64".equalsIgnoreCase((String) contentEncoding)) {
				try {
					content = new String(Base64.getDecoder().decode(content), StandardCharsets.UTF_8);
				} catch (final IllegalArgumentException e) {
					throw new JsonSchemaDataValidationError("Invalid base64 encoded data: " + e.getMessage(), jsonPath, e);
				}
			}

			try {
				JsonReader.readJsonItemString(content);
			} catch (final Exception e) {
				throw new JsonSchemaDataValidationError("Data is not valid JSON data: " + e.getMessage(), jsonPath, e);
			}
		}
	}
}
