package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

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
 * A JSON schema object with regex patterns as keys and subschemas which must match if the key pattern matches with a JSON data object property name<br />
 * <br />
 * Security note: see {@link PatternValidator} regarding ReDoS risk from untrusted regex patterns in the schema.
 */
public class PatternPropertiesValidator extends BaseJsonSchemaValidator {
	/** Validators by the property name pattern they apply to. */
	private final Map<Pattern, List<BaseJsonSchemaValidator>> propertiesDefinitionsByPattern = new HashMap<>();

	/**
	 * Creates a new "patternProperties" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, an object with regex patterns as keys and schemas as
	 *            values
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not an object, a pattern is invalid, or a value is neither
	 *             object nor boolean
	 * @throws DuplicateKeyException
	 *             if a schema contains duplicate keys
	 */
	public PatternPropertiesValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("PatternProperties data is 'null'", jsonSchemaPath);
		} else if (!(validatorData.isJsonObject())) {
			throw new JsonSchemaDefinitionError("PatternProperties data is not a JsonObject", jsonSchemaPath);
		}

		for (final Entry<String, JsonNode> entry : ((JsonObject) validatorData).entrySet()) {
			final JsonSchemaPath entrySchemaPath = new JsonSchemaPath(jsonSchemaPath).addPropertyKey(entry.getKey());
			if (entry.getValue() == null) {
				throw new JsonSchemaDefinitionError("PatternProperties data is null", entrySchemaPath);
			} else if (!entry.getValue().isBoolean() && !entry.getValue().isJsonObject()) {
				throw new JsonSchemaDefinitionError("PatternProperties data is neither 'object' nor 'boolean'", entrySchemaPath);
			}

			final Pattern propertyKeyPattern;
			try {
				propertyKeyPattern = Pattern.compile(entry.getKey());
			} catch (final Exception e) {
				throw new JsonSchemaDefinitionError("PatternProperties data contains invalid RegEx pattern: " + entry.getKey(), entrySchemaPath, e);
			}

			final List<BaseJsonSchemaValidator> subValidators;
			if (entry.getValue().isBoolean()) {
				subValidators = new ArrayList<>();
				subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, entrySchemaPath, entry.getValue()));
			} else {
				subValidators = JsonSchema.createValidators((JsonObject) entry.getValue(), jsonSchemaDependencyResolver, entrySchemaPath);
			}
			propertiesDefinitionsByPattern.put(propertyKeyPattern, subValidators);
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
				for (final Entry<String, JsonNode> propertyEntry : ((JsonObject) jsonNode).entrySet()) {
					if (propertiesDefinitionsEntry.getKey().matcher(propertyEntry.getKey()).find()) {
						JsonNode newJsonNode;
						try {
							newJsonNode = propertyEntry.getValue().withRootNode(false);
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
