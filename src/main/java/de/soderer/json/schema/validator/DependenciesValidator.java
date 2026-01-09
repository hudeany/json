package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueString;
import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * JSON Object that defines properties that must have a subschema as JSON object value which must be matched by the JAON data object
 */
public class DependenciesValidator extends BaseJsonSchemaValidator {
	private final Map<String, List<BaseJsonSchemaValidator>> validators = new HashMap<>();
	private final Map<String, List<String>> mandatoryProperties = new HashMap<>();

	public DependenciesValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Dependencies value is 'null'", jsonSchemaPath);
		} else if (!(validatorData.isJsonObject())) {
			throw new JsonSchemaDefinitionError("Dependencies value is not an 'object'", jsonSchemaPath);
		}

		for (final Entry<String, JsonNode> entry : ((JsonObject) validatorData).entrySet()) {
			if (entry.getValue() == null) {
				throw new JsonSchemaDefinitionError("Dependencies value is 'null'", jsonSchemaPath);
			} else if (entry.getValue().isJsonObject()) {
				final List<BaseJsonSchemaValidator> subValidators = JsonSchema.createValidators((JsonObject) entry.getValue(), jsonSchemaDependencyResolver, jsonSchemaPath);
				validators.put(entry.getKey(), subValidators);
			} else if (entry.getValue().isJsonArray()) {
				final List<String> propertiesList = new ArrayList<>();
				for (final JsonNode item : ((JsonArray) entry.getValue()).items()) {
					if (item == null || !(item.isString())) {
						throw new JsonSchemaDefinitionError("Dependencies value for key '" + entry.getKey() + "' contains invalid data that is not 'string'", jsonSchemaPath);
					} else {
						propertiesList.add(((JsonValueString) item).getValue());
					}
				}
				mandatoryProperties.put(entry.getKey(), propertiesList);
			} else if (entry.getValue().isString()) {
				final List<String> propertiesList = new ArrayList<>();
				propertiesList.add(((JsonValueString) entry.getValue()).getValue());
				mandatoryProperties.put(entry.getKey(), propertiesList);
			} else if (entry.getValue().isBoolean()) {
				final List<BaseJsonSchemaValidator> subValidators = new ArrayList<>();
				subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, jsonSchemaPath, entry.getValue()));
				validators.put(entry.getKey(), subValidators);
			} else {
				throw new JsonSchemaDefinitionError("Dependencies value for key '" + entry.getKey() + "' is not an 'object' or 'array' or 'string'", jsonSchemaPath);
			}
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!jsonNode.isJsonObject()) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected a 'object' value for dependency but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			for (final Entry<String, List<BaseJsonSchemaValidator>> validatorEntry : validators.entrySet()) {
				if (((JsonObject) jsonNode).containsKey(validatorEntry.getKey())) {
					final List<BaseJsonSchemaValidator> subValidators = validatorEntry.getValue();
					for (final BaseJsonSchemaValidator validator : subValidators) {
						validator.validate(jsonNode, jsonPath);
					}
				}
			}

			for (final Entry<String, List<String>> mandatoryPropertyEntry : mandatoryProperties.entrySet()) {
				if (((JsonObject) jsonNode).containsKey(mandatoryPropertyEntry.getKey())) {
					for (final String item : mandatoryPropertyEntry.getValue()) {
						if (!((JsonObject) jsonNode).containsKey(item)) {
							throw new JsonSchemaDataValidationError("Dependent property key '" + item + "' for existing parent key '" + mandatoryPropertyEntry.getKey() + "' is missing", jsonPath);
						}
					}
				}
			}
		}
	}
}
