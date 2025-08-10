package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

public class PropertiesValidator extends BaseJsonSchemaValidator {
	private List<String> requiredKeysV3 = null;
	private final Map <String, List<BaseJsonSchemaValidator>> propertiesDefinitions = new HashMap<>();

	public PropertiesValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (!(validatorData instanceof JsonObject)) {
			throw new JsonSchemaDefinitionError("Properties data is not a JsonObject", jsonSchemaPath);
		}

		for (final Entry<String, Object> entry : ((JsonObject) validatorData).entrySet()) {
			if ("default".equals(entry.getKey())) {
				// Ignore default value, as it is only a descriptive annotation
			} else if (entry.getValue() instanceof Boolean) {
				final List<BaseJsonSchemaValidator> subValidators = new ArrayList<>();
				subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, jsonSchemaPath, entry.getValue()));
				propertiesDefinitions.put(entry.getKey(), subValidators);
			} else if (!(entry.getValue() instanceof JsonObject)) {
				throw new JsonSchemaDefinitionError("Properties data is not a JsonObject", jsonSchemaPath.addPropertyKey(entry.getKey()));
			} else {
				final List<BaseJsonSchemaValidator> subValidators = JsonSchema.createValidators((JsonObject) entry.getValue(), jsonSchemaDependencyResolver, new JsonSchemaPath(jsonSchemaPath).addPropertyKey(entry.getKey()));
				propertiesDefinitions.put(entry.getKey(), subValidators);

				if (jsonSchemaDependencyResolver.isDraftV3Mode() && ((JsonObject) entry.getValue()).containsPropertyKey("required") && ((JsonObject) entry.getValue()).get("required") instanceof Boolean && (Boolean) ((JsonObject) entry.getValue()).get("required")) {
					if (requiredKeysV3 == null) {
						requiredKeysV3 = new ArrayList<>();
					}
					requiredKeysV3.add(entry.getKey());
				}
			}
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonObject())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'object' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			for (final Entry<String, Object> propertyEntry : ((JsonObject) jsonNode.getValue()).entrySet()) {
				if (propertiesDefinitions.containsKey(propertyEntry.getKey())) {
					JsonNode newJsonNode;
					try {
						newJsonNode = new JsonNode(false, propertyEntry.getValue());
					} catch (final Exception e) {
						throw new JsonSchemaDataValidationError("Invalid data type '" + propertyEntry.getValue().getClass().getSimpleName() + "'", new JsonPath(jsonPath).addPropertyKey(propertyEntry.getKey()), e);
					}
					for (final BaseJsonSchemaValidator subValidator : propertiesDefinitions.get(propertyEntry.getKey())) {
						subValidator.validate(newJsonNode, new JsonPath(jsonPath).addPropertyKey(propertyEntry.getKey()));
					}
				}
			}

			if (requiredKeysV3 != null) {
				for (final String propertyKey : requiredKeysV3) {
					if (!((JsonObject) jsonNode.getValue()).containsPropertyKey(propertyKey)) {
						throw new JsonSchemaDataValidationError("Invalid property key. Missing required property '" + propertyKey + "'", jsonPath);
					}
				}
			}
		}
	}
}
