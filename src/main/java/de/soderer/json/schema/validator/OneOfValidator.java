package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.exception.JsonDuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * An array of sub schemas of which exactly one must match
 */
public class OneOfValidator extends BaseJsonSchemaValidator {
	private List<List<BaseJsonSchemaValidator>> subValidatorPackages = null;

	public OneOfValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError, JsonDuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("OneOf array is 'null'", jsonSchemaPath);
		} else if (validatorData instanceof JsonArray) {
			subValidatorPackages = new ArrayList<>();
			for (final Object subValidationData : ((JsonArray) validatorData)) {
				if (subValidationData instanceof Boolean) {
					final List<BaseJsonSchemaValidator> subValidators = new ArrayList<>();
					subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, jsonSchemaPath, subValidationData));
					subValidatorPackages.add(subValidators);
				} else if (subValidationData instanceof JsonObject) {
					subValidatorPackages.add(JsonSchema.createValidators((JsonObject) subValidationData, jsonSchemaDependencyResolver, jsonSchemaPath));
				} else {
					throw new JsonSchemaDefinitionError("OneOf array contains a non-JsonObject", jsonSchemaPath);
				}
			}
			if (subValidatorPackages == null || subValidatorPackages.size() == 0) {
				throw new JsonSchemaDefinitionError("OneOf array is empty", jsonSchemaPath);
			}
		} else {
			throw new JsonSchemaDefinitionError("OneOf property does not have an array value", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		boolean foundMatch = false;

		for (final List<BaseJsonSchemaValidator> subValidatorPackage : subValidatorPackages) {
			if (validateSubSchema(subValidatorPackage, jsonNode, jsonPath)) {
				if (foundMatch) {
					throw new JsonSchemaDataValidationError("More than one option of 'oneOf' property did apply to JsonNode", jsonPath);
				} else {
					foundMatch = true;
				}
			}
		}

		if (!foundMatch) {
			throw new JsonSchemaDataValidationError("No option of 'oneOf' property did apply to JsonNode", jsonPath);
		}
	}
}
