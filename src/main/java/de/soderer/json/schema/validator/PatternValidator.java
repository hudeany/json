package de.soderer.json.schema.validator;

import java.util.regex.Pattern;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * JSON subschema that matches a simple data value to a regex pattern<br />
 * <br />
 * Security note: The regex pattern is taken directly from the JSON schema and compiled/executed via
 * {@link java.util.regex.Pattern}, without any complexity or timeout guard. A schema from an untrusted source could
 * contain a pattern causing catastrophic backtracking (ReDoS) against crafted input data. Only use JSON schemas from
 * trusted sources, or validate/sanitize patterns before using this library with schemas of unknown origin.
 */
public class PatternValidator extends BaseJsonSchemaValidator {
	private final Pattern pattern;

	public PatternValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (!(validatorData.isString())) {
			throw new JsonSchemaDefinitionError("Pattern is no string", jsonSchemaPath);
		}

		try {
			pattern = Pattern.compile(((JsonValueString) validatorData).getValue());
		} catch (final Exception e) {
			throw new JsonSchemaDefinitionError("Pattern '" + ((JsonValueString) validatorData).getValue() + "' is not a valid RegEx pattern", jsonSchemaPath, e);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (jsonNode.isInteger()) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				if (!pattern.matcher(((JsonValueInteger) jsonNode).getValue().toString()).find()) {
					throw new JsonSchemaDataValidationError("RegEx pattern '" + ((JsonValueString) validatorData).getValue() + "' is not matched by data number '" + ((JsonValueInteger) jsonNode).getValue().toString() + "'", jsonPath);
				}
			}
		} else if (jsonNode.isNumber()) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				if (!pattern.matcher(((JsonValueNumber) jsonNode).getValue().toString()).find()) {
					throw new JsonSchemaDataValidationError("RegEx pattern '" + ((JsonValueString) validatorData).getValue() + "' is not matched by data number '" + ((JsonValueNumber) jsonNode).getValue().toString() + "'", jsonPath);
				}
			}
		} else if (jsonNode.isBoolean()) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				if (!pattern.matcher((((JsonValueBoolean) jsonNode).getValue()).toString()).find()) {
					throw new JsonSchemaDataValidationError("RegEx pattern '" + ((JsonValueString) validatorData).getValue() + "' is not matched by data boolean '" + ((JsonValueBoolean) jsonNode).getValue().toString() + "'", jsonPath);
				}
			}
		} else if (jsonNode.isString()) {
			if (!pattern.matcher(((JsonValueString) jsonNode).getValue()).find()) {
				throw new JsonSchemaDataValidationError("RegEx pattern '" + ((JsonValueString) validatorData).getValue() + "' is not matched by data string '" + ((JsonValueString) jsonNode).getValue() + "'", jsonPath);
			}
		} else {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'string' or 'number' or 'boolean' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		}
	}
}
