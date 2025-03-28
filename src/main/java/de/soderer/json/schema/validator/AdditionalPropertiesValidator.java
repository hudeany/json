package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.List;
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
import de.soderer.json.utilities.Utilities;

/**
 * Basically the value defines whether or not additional properties are allowed that are not included by the other definitions of properties like "properties" and "patternProperties".
 * Alternatively it is allowed to define a value that consists of a subschema that defines the structure of any additional property that is not included by the other definitions.
 */
public class AdditionalPropertiesValidator extends ExtendedBaseJsonSchemaValidator {
	private final List<String> parentPropertyItemNames = new ArrayList<>();
	private final List<Pattern> parentPropertyItemPatterns = new ArrayList<>();
	private Boolean allowAdditionalPropertyNames = null;
	private List<BaseJsonSchemaValidator> subValidators = null;

	public AdditionalPropertiesValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("AdditionalProperties data is 'null'", jsonSchemaPath);
		} else if (validatorData instanceof Boolean) {
			allowAdditionalPropertyNames = (Boolean) validatorData;
		} else if (validatorData instanceof JsonObject) {
			subValidators = JsonSchema.createValidators((JsonObject) validatorData, jsonSchemaDependencyResolver, jsonSchemaPath);
		} else {
			throw new JsonSchemaDefinitionError("AdditionalProperties data is not a 'boolean' or 'object'", jsonSchemaPath);
		}

		if (parentValidatorData.containsPropertyKey("properties")) {
			if (parentValidatorData.get("properties") == null) {
				throw new JsonSchemaDefinitionError("Properties data is 'null'", jsonSchemaPath);
			} else if (!(parentValidatorData.get("properties") instanceof JsonObject)) {
				throw new JsonSchemaDefinitionError("Properties data is not a JsonObject", jsonSchemaPath);
			} else {
				parentPropertyItemNames.addAll(((JsonObject) parentValidatorData.get("properties")).keySet());
			}
		}

		if (parentValidatorData.containsPropertyKey("patternProperties")) {
			if (parentValidatorData.get("patternProperties") == null) {
				throw new JsonSchemaDefinitionError("PatternProperties data is 'null'", jsonSchemaPath);
			} else if (!(parentValidatorData.get("patternProperties") instanceof JsonObject)) {
				throw new JsonSchemaDefinitionError("PatternProperties data is not a JsonObject", jsonSchemaPath);
			} else {
				for (final Entry<String, Object> entry : ((JsonObject) parentValidatorData.get("patternProperties")).entrySet()) {
					if (entry.getValue() == null || !(entry.getValue() instanceof JsonObject)) {
						throw new JsonSchemaDefinitionError("PatternProperties data contains a non-JsonObject", jsonSchemaPath);
					} else {
						Pattern propertyKeyPattern;
						try {
							propertyKeyPattern = Pattern.compile(entry.getKey());
						} catch (final Exception e) {
							throw new JsonSchemaDefinitionError("PatternProperties data contains invalid RegEx pattern: " + entry.getKey(), jsonSchemaPath, e);
						}

						parentPropertyItemPatterns.add(propertyKeyPattern);
					}
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
			final List<String> additionalPropertyNames = new ArrayList<>();

			for (final String checkPropertyKey : ((JsonObject) jsonNode.getValue()).keySet()) {
				if (!parentPropertyItemNames.contains(checkPropertyKey)) {
					boolean isAdditionalPropertyKey = true;
					for (final Pattern parentPropertyItemPattern : parentPropertyItemPatterns) {
						if (parentPropertyItemPattern.matcher(checkPropertyKey).find()) {
							isAdditionalPropertyKey = false;
							break;
						}
					}
					if (isAdditionalPropertyKey) {
						additionalPropertyNames.add(checkPropertyKey);
					}
				}
			}

			if (additionalPropertyNames.size() > 0) {
				if (allowAdditionalPropertyNames != null) {
					if (!allowAdditionalPropertyNames) {
						throw new JsonSchemaDataValidationError("Unexpected property keys found '" + Utilities.join(additionalPropertyNames, "', '") + "'", jsonPath);
					}
				} else {
					for (final String propertyKey : additionalPropertyNames) {
						JsonNode newJsonNode;
						try {
							newJsonNode = new JsonNode(((JsonObject) jsonNode.getValue()).get(propertyKey));
						} catch (final Exception e) {
							throw new JsonSchemaDataValidationError("Invalid data type '" + ((JsonObject) jsonNode.getValue()).get(propertyKey).getClass().getSimpleName() + "'", new JsonPath(jsonPath).addPropertyKey(propertyKey), e);
						}
						for (final BaseJsonSchemaValidator subValidator : subValidators) {
							subValidator.validate(newJsonNode, new JsonPath(jsonPath).addPropertyKey(propertyKey));
						}
					}
				}
			}
		}
	}
}
