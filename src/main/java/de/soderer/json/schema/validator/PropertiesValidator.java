package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Validator for the "properties" keyword: its value is an object mapping property names to schemas.
 * Each property of the JSON data object that is listed there must match its schema. Properties not
 * listed are not checked here (see {@link AdditionalPropertiesValidator}).<br />
 * <br />
 * In draft 3 a property schema may contain the boolean "required", which makes the property
 * mandatory.
 */
public class PropertiesValidator extends BaseJsonSchemaValidator {
	/** Mandatory property names defined by draft 3 "required": true, or null if none. */
	private List<String> requiredKeysV3 = null;

	/** Validators by property name. */
	private final Map<String, List<BaseJsonSchemaValidator>> propertiesDefinitions = new HashMap<>();

	/**
	 * Creates a new "properties" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, an object with property names as keys and schemas as
	 *            values
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not an object or a property schema is neither object nor
	 *             boolean
	 * @throws DuplicateKeyException
	 *             if a schema contains duplicate keys
	 */
	public PropertiesValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (!(validatorData.isJsonObject())) {
			throw new JsonSchemaDefinitionError("Properties data is not a JsonObject", jsonSchemaPath);
		}

		for (final Entry<String, JsonNode> entry : ((JsonObject) validatorData).entrySet()) {
			if (entry.getValue() == null) {
				throw new JsonSchemaDefinitionError("Properties data is 'null'", new JsonSchemaPath(jsonSchemaPath).addPropertyKey(entry.getKey()));
			} else if ("default".equals(entry.getKey()) && !entry.getValue().isBoolean() && !entry.getValue().isJsonObject()) {
				// Tolerate a misplaced "default" annotation value. A property named "default" with a schema is validated normally.
			} else if (entry.getValue().isBoolean()) {
				final List<BaseJsonSchemaValidator> subValidators = new ArrayList<>();
				subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(jsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
				propertiesDefinitions.put(entry.getKey(), subValidators);
			} else if (!(entry.getValue().isJsonObject())) {
				throw new JsonSchemaDefinitionError("Properties data is not a JsonObject", new JsonSchemaPath(jsonSchemaPath).addPropertyKey(entry.getKey()));
			} else {
				final List<BaseJsonSchemaValidator> subValidators = JsonSchema.createValidators((JsonObject) entry.getValue(), jsonSchemaDependencyResolver, new JsonSchemaPath(jsonSchemaPath).addPropertyKey(entry.getKey()));
				propertiesDefinitions.put(entry.getKey(), subValidators);

				if (jsonSchemaDependencyResolver.isDraftV3Mode() && ((JsonObject) entry.getValue()).containsKey("required") && ((JsonObject) entry.getValue()).get("required").isBoolean() && ((JsonValueBoolean) ((JsonObject) entry.getValue()).get("required")).getValue()) {
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
			for (final Entry<String, JsonNode> propertyEntry : ((JsonObject) jsonNode).entrySet()) {
				if (propertiesDefinitions.containsKey(propertyEntry.getKey())) {
					JsonNode newJsonNode;
					try {
						newJsonNode = propertyEntry.getValue().withRootNode(false);
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
					if (!((JsonObject) jsonNode).containsKey(propertyKey)) {
						throw new JsonSchemaDataValidationError("Invalid property key. Missing required property '" + propertyKey + "'", jsonPath);
					}
				}
			}
		}
	}
}
