package de.soderer.utilities.json.schema.validator;

import java.util.List;
import java.util.Map.Entry;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchema;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

public class PropertiesValidator extends BaseJsonSchemaValidator {
	public PropertiesValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData, final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData, jsonNode, jsonPath);

		if (!(validatorData instanceof JsonObject)) {
			throw new JsonSchemaDefinitionError("Properties data is not a JsonObject", jsonSchemaPath);
		}
	}

	@Override
	public void validate() throws Exception {
		if (!(jsonNode.isJsonObject())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'object' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			for (final Entry<String, Object> entry : ((JsonObject) validatorData).entrySet()) {
				if (!(entry.getValue() instanceof JsonObject)) {
					throw new JsonSchemaDefinitionError("Properties data is not a JsonObject", jsonSchemaPath);
				} else {
					if (((JsonObject) jsonNode.getValue()).containsPropertyKey(entry.getKey())) {
						JsonNode newJsonNode;
						try {
							newJsonNode = new JsonNode(((JsonObject) jsonNode.getValue()).get(entry.getKey()));
						} catch (final Exception e) {
							throw new JsonSchemaDataValidationError("Invalid data type '" + ((JsonObject) jsonNode.getValue()).get(entry.getKey()).getClass().getSimpleName() + "'", new JsonPath(jsonPath).addPropertyKey(entry.getKey()), e);
						}
						final List<BaseJsonSchemaValidator> subValidators = JsonSchema.createValidators((JsonObject) entry.getValue(), jsonSchemaDependencyResolver, new JsonSchemaPath(jsonSchemaPath).addPropertyKey(entry.getKey()), newJsonNode, new JsonPath(jsonPath).addPropertyKey(entry.getKey()));
						for (final BaseJsonSchemaValidator subValidator : subValidators) {
							subValidator.validate();
						}
					}
				}
			}
		}
	}
}
