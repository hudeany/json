package de.soderer.utilities.json.schema.validator;

import java.util.regex.Pattern;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

public class PatternValidator extends BaseJsonSchemaValidator {
	public PatternValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData, final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData, jsonNode, jsonPath);

		if (!(validatorData instanceof String)) {
			throw new JsonSchemaDefinitionError("Pattern is no string", jsonSchemaPath);
		}
	}

	@Override
	public void validate() throws JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		final Pattern pattern = Pattern.compile((String) validatorData);
		if (jsonNode.isNumber()) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				if (!pattern.matcher(((Number) jsonNode.getValue()).toString()).find()) {
					throw new JsonSchemaDataValidationError("RegEx pattern '" + (String) validatorData + "' is not matched by data number '" + jsonNode.getValue() + "'", jsonPath);
				}
			}
		} else if (jsonNode.isBoolean()) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				if (!pattern.matcher(((Boolean) jsonNode.getValue()).toString()).find()) {
					throw new JsonSchemaDataValidationError("RegEx pattern '" + (String) validatorData + "' is not matched by data boolean '" + jsonNode.getValue() + "'", jsonPath);
				}
			}
		} else if (jsonNode.isString()) {
			if (!pattern.matcher((String) jsonNode.getValue()).find()) {
				throw new JsonSchemaDataValidationError("RegEx pattern '" + (String) validatorData + "' is not matched by data string '" + (String) jsonNode.getValue() + "'", jsonPath);
			}
		} else {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'string' or 'number' or 'boolean' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		}
	}
}
