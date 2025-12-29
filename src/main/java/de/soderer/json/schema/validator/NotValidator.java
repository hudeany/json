package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

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
 * Logic inverter for JSON schema tests
 */
public class NotValidator extends BaseJsonSchemaValidator {
	private List<BaseJsonSchemaValidator> subValidators = null;

	public NotValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, JsonDuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Not-validation data is 'null'", jsonSchemaPath);
		} else if (validatorData.isBoolean()) {
			subValidators = new ArrayList<>();
			subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData));
		} else if (validatorData.isJsonObject()) {
			subValidators = JsonSchema.createValidators((JsonObject) validatorData, jsonSchemaDependencyResolver, jsonSchemaPath);
			if (subValidators == null) {
				throw new JsonSchemaDefinitionError("Not-validation JsonObject is empty", jsonSchemaPath);
			}
		} else {
			throw new JsonSchemaDefinitionError("Not-validation property does not have an JsonObject value", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		boolean didNotApply = false;
		if (!validateSubSchema(subValidators, jsonNode, jsonPath))  {
			didNotApply = true;
		}
		if (!didNotApply) {
			throw new JsonSchemaDataValidationError("The 'not' property did apply to JsonNode", jsonPath);
		}
	}
}
