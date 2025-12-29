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
 * JSON subschema that matches a simple data value to a type definition
 */
public class ContentEncodingValidator extends BaseJsonSchemaValidator {
	public ContentEncodingValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("ContentEncoding value is 'null'", jsonSchemaPath);
		} else if (!(validatorData instanceof JsonValueString)) {
			throw new JsonSchemaDefinitionError("ContentEncoding value is not a string", jsonSchemaPath);
		} else if (Utilities.isBlank(((JsonValueString) validatorData).getValue())) {
			throw new JsonSchemaDefinitionError("Invalid ContentEncoding '" + validatorData + "'", jsonSchemaPath);
		} else {
			this.validatorData = validatorData;
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (jsonNode.isNull()) {
			// ContentEncoding ignore null values
		} else if (jsonNode.isInteger() || jsonNode.isFloat()) {
			// ContentEncoding ignore numeric values
		} else if (jsonNode.isJsonObject()) {
			// ContentEncoding ignore JsonObject values
		} else if (jsonNode.isJsonArray()) {
			// ContentEncoding ignore JsonArray values
		} else if (jsonNode.isBoolean()) {
			// ContentEncoding ignore Boolean values
		} else if (!jsonNode.isString()) {
			throw new JsonSchemaDataValidationError("Expected a 'string' value for ContentEncoding but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
		} else {
			if ("base64".equalsIgnoreCase(((JsonValueString) validatorData).getValue())) {
				try {
					Base64.getDecoder().decode(((JsonValueString) validatorData).getValue());
				} catch (final Exception e) {
					throw new JsonSchemaDataValidationError("Invalid base64 encoded data: " + e.getMessage(), jsonPath);
				}
			} else {
				// Do nothing
			}
		}
	}
}
