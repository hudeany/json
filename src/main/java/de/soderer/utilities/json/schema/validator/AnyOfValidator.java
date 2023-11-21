package de.soderer.utilities.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchema;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

public class AnyOfValidator extends BaseJsonSchemaValidator {
	private List<List<BaseJsonSchemaValidator>> subValidatorPackages = null;

	public AnyOfValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("AnyOf array is 'null'", jsonSchemaPath);
		} else if (validatorData instanceof JsonArray) {
			subValidatorPackages = new ArrayList<>();
			for (int i = 0; i < ((JsonArray) validatorData).size(); i++) {
				final Object subValidationData = ((JsonArray) validatorData).get(i);
				if (subValidationData instanceof JsonObject) {
					subValidatorPackages.add(JsonSchema.createValidators((JsonObject) subValidationData, jsonSchemaDependencyResolver, jsonSchemaPath));
				} else {
					throw new JsonSchemaDefinitionError("AnyOf array contains a non-JsonObject", jsonSchemaPath);
				}
			}
			if (subValidatorPackages == null || subValidatorPackages.size() == 0) {
				throw new JsonSchemaDefinitionError("AnyOf array is empty", jsonSchemaPath);
			}
		} else {
			throw new JsonSchemaDefinitionError("AnyOf property does not have an array value", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		for (final List<BaseJsonSchemaValidator> subValidatorPackage : subValidatorPackages) {
			try {
				for (final BaseJsonSchemaValidator subValidator : subValidatorPackage) {
					subValidator.validate(jsonNode, jsonPath);
				}
				return;
			} catch (@SuppressWarnings("unused") final JsonSchemaDataValidationError e) {
				// Do nothing, only one subvalidator must have successfully validated
			}
		}
		throw new JsonSchemaDataValidationError("No option of 'anyOf' property did apply to JsonNode", jsonPath);
	}
}
