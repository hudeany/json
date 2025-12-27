package de.soderer.json.schema.validator;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.utilities.Utilities;

/**
 * JSON subschema that matches a simple data value to a type definition
 */
public class ContentMediaTypeValidator extends ExtendedBaseJsonSchemaValidator {
	public ContentMediaTypeValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("ContentMediaType value is 'null'", jsonSchemaPath);
		} else if (!(validatorData instanceof String)) {
			throw new JsonSchemaDefinitionError("ContentMediaType value is not a string", jsonSchemaPath);
		} else if (Utilities.isBlank((String) validatorData)) {
			throw new JsonSchemaDefinitionError("Invalid ContentMediaType '" + validatorData + "'", jsonSchemaPath);
		} else {
			this.validatorData = validatorData;
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (jsonNode.isNull()) {
			// ContentMediaType ignore null values
		} else if (jsonNode.isInteger() || jsonNode.isNumber()) {
			// ContentMediaType ignore numeric values
		} else if (jsonNode.isJsonObject()) {
			// ContentMediaType ignore JsonObject values
		} else if (jsonNode.isJsonArray()) {
			// ContentMediaType ignore JsonArray values
		} else if (jsonNode.isBoolean()) {
			// ContentMediaType ignore Boolean values
		} else if (!jsonNode.isString()) {
			throw new JsonSchemaDataValidationError("Expected a 'string' value for ContentMediaType but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
		} else {
			Object value = jsonNode.getValue();

			if (parentValidatorData.containsKey("contentEncoding")) {
				if ("base64".equalsIgnoreCase((String) parentValidatorData.get("contentEncoding"))) {
					try {
						value = Base64.getDecoder().decode((String) jsonNode.getValue());
					} catch (final Exception e) {
						throw new JsonSchemaDataValidationError("Invalid base64 encoded data: " + e.getMessage(), jsonPath);
					}
				} else {
					// Do nothing
				}
			}

			if ("application/json".equalsIgnoreCase((String) validatorData)) {
				if (value instanceof String) {
					try {
						JsonReader.readJsonItemString((String) value);
					} catch (final Exception e) {
						throw new JsonSchemaDataValidationError("Data is not valid JSON data: " + e.getMessage(), jsonPath);
					}
				} else if (value instanceof byte[]) {
					try {
						JsonReader.readJsonItemString(new String((byte[]) value, StandardCharsets.UTF_8));
					} catch (final Exception e) {
						throw new JsonSchemaDataValidationError("Data is not valid JSON data: " + e.getMessage(), jsonPath);
					}
				} else {
					throw new JsonSchemaDataValidationError("Data is not a String", jsonPath);
				}
			} else {
				// Do nothing
			}
		}
	}
}
