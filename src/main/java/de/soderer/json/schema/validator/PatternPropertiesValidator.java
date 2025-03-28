package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * A JSON schema object with regex patterns as keys and subschemas which must match if the key pattern matches with a JSON data object property name
 */
public class PatternPropertiesValidator extends BaseJsonSchemaValidator {
	private final Map <Pattern, List<BaseJsonSchemaValidator>> propertiesDefinitionsByPattern = new HashMap<>();

	public PatternPropertiesValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("PatternProperties data is 'null'", jsonSchemaPath);
		} else if (!(validatorData instanceof JsonObject)) {
			throw new JsonSchemaDefinitionError("PatternProperties data is not a JsonObject", jsonSchemaPath);
		}

		for (final Entry<String, Object> entry : ((JsonObject) validatorData).entrySet()) {
			if (entry.getValue() == null) {
				throw new JsonSchemaDefinitionError("PatternProperties data is null", jsonSchemaPath.addPropertyKey(entry.getKey()));
			} else if (entry.getValue() instanceof Boolean) {
				Pattern propertyKeyPattern;
				try {
					propertyKeyPattern = Pattern.compile(entry.getKey());
				} catch (final Exception e) {
					throw new JsonSchemaDefinitionError("PatternProperties data contains invalid RegEx pattern: " + entry.getKey(), jsonSchemaPath.addPropertyKey(entry.getKey()), e);
				}

				final List<BaseJsonSchemaValidator> subValidators = new ArrayList<>();
				subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, jsonSchemaPath, entry.getValue()));
				propertiesDefinitionsByPattern.put(propertyKeyPattern, subValidators);
			} else if (entry.getValue() instanceof JsonObject) {
				Pattern propertyKeyPattern;
				try {
					propertyKeyPattern = Pattern.compile(entry.getKey());
				} catch (final Exception e) {
					throw new JsonSchemaDefinitionError("PatternProperties data contains invalid RegEx pattern: " + entry.getKey(), jsonSchemaPath.addPropertyKey(entry.getKey()), e);
				}

				final List<BaseJsonSchemaValidator> subValidators = JsonSchema.createValidators((JsonObject) entry.getValue(), jsonSchemaDependencyResolver, new JsonSchemaPath(jsonSchemaPath).addPropertyKey(entry.getKey()));
				propertiesDefinitionsByPattern.put(propertyKeyPattern, subValidators);
			} else {
				throw new JsonSchemaDefinitionError("PatternProperties data is not a JsonObject", jsonSchemaPath.addPropertyKey(entry.getKey()));
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
			for (final Entry<Pattern, List<BaseJsonSchemaValidator>> propertiesDefinitionsEntry : propertiesDefinitionsByPattern.entrySet()) {
				for (final Entry<String, Object> propertyEntry : ((JsonObject) jsonNode.getValue()).entrySet()) {
					if (propertiesDefinitionsEntry.getKey().matcher(propertyEntry.getKey()).find()) {
						JsonNode newJsonNode;
						try {
							newJsonNode = new JsonNode(propertyEntry.getValue());
						} catch (final Exception e) {
							throw new JsonSchemaDataValidationError("Invalid data type '" + propertyEntry.getValue().getClass().getSimpleName() + "'", new JsonPath(jsonPath).addPropertyKey(propertyEntry.getKey()), e);
						}
						for (final BaseJsonSchemaValidator subValidator : propertiesDefinitionsEntry.getValue()) {
							subValidator.validate(newJsonNode, new JsonPath(jsonPath).addPropertyKey(propertyEntry.getKey()));
						}
					}
				}
			}
		}
	}
}
