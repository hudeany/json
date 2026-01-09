package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * The allOf value is an array of schemas, that all need to validate against the JSON data node
 */
public class AllOfValidator extends BaseJsonSchemaValidator {
	private List<List<BaseJsonSchemaValidator>> subValidatorPackages = null;

	public AllOfValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("AllOf array is 'null'", jsonSchemaPath);
		} else if (validatorData.isJsonArray()) {
			subValidatorPackages = new ArrayList<>();
			for (int i = 0; i < ((JsonArray) validatorData).size(); i++) {
				final JsonNode subValidationData = ((JsonArray) validatorData).get(i);
				if (subValidationData.isBoolean()) {
					final List<BaseJsonSchemaValidator> subValidators = new ArrayList<>();
					subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, jsonSchemaPath, subValidationData));
					subValidatorPackages.add(subValidators);
				} else if (subValidationData.isJsonObject()) {
					subValidatorPackages.add(JsonSchema.createValidators((JsonObject) subValidationData, jsonSchemaDependencyResolver, jsonSchemaPath));
				} else {
					throw new JsonSchemaDefinitionError("AllOf array contains a non-JsonObject", jsonSchemaPath);
				}
			}
			if (subValidatorPackages == null || subValidatorPackages.size() == 0) {
				throw new JsonSchemaDefinitionError("AllOf array is empty", jsonSchemaPath);
			}
		} else {
			throw new JsonSchemaDefinitionError("AllOf property does not have an array value", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		for (final List<BaseJsonSchemaValidator> subValidatorPackage : subValidatorPackages) {
			if (!validateSubSchema(subValidatorPackage, jsonNode, jsonPath)) {
				throw new JsonSchemaDataValidationError("Some option of 'allOf' property did not apply to JsonNode", jsonPath);
			}
		}
	}
}
