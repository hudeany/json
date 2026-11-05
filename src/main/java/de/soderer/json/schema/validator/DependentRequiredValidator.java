package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Validator for the "dependentRequired" keyword (since JSON schema draft 2019-09): its value is an
 * object mapping property names to arrays of property names. If the JSON data object contains such
 * a property, it must also contain all listed properties. Data that is not an object is ignored.
 */
public class DependentRequiredValidator extends BaseJsonSchemaValidator {
	/** Mandatory property names by the property name they depend on. */
	private final Map<String, List<String>> mandatoryProperties = new LinkedHashMap<>();

	/**
	 * Creates a new "dependentRequired" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, an object of string arrays
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not an object of string arrays
	 */
	public DependentRequiredValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (!validatorData.isJsonObject()) {
			throw new JsonSchemaDefinitionError("DependentRequired value is not an 'object'", jsonSchemaPath);
		}
		for (final Entry<String, JsonNode> entry : ((JsonObject) validatorData).entrySet()) {
			if (entry.getValue() == null || !entry.getValue().isJsonArray()) {
				throw new JsonSchemaDefinitionError("DependentRequired value for key '" + entry.getKey() + "' is not an 'array'", new JsonSchemaPath(jsonSchemaPath).addPropertyKey(entry.getKey()));
			}
			final List<String> propertyNames = new ArrayList<>();
			for (final JsonNode item : ((JsonArray) entry.getValue()).items()) {
				if (item == null || !item.isString()) {
					throw new JsonSchemaDefinitionError("DependentRequired value for key '" + entry.getKey() + "' contains an item that is not a 'string'", new JsonSchemaPath(jsonSchemaPath).addPropertyKey(entry.getKey()));
				}
				propertyNames.add(((JsonValueString) item).getValue());
			}
			mandatoryProperties.put(entry.getKey(), propertyNames);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (jsonNode.isJsonObject()) {
			final JsonObject jsonObject = (JsonObject) jsonNode;
			for (final Entry<String, List<String>> mandatoryPropertyEntry : mandatoryProperties.entrySet()) {
				if (jsonObject.containsKey(mandatoryPropertyEntry.getKey())) {
					for (final String propertyName : mandatoryPropertyEntry.getValue()) {
						if (!jsonObject.containsKey(propertyName)) {
							throw new JsonSchemaDataValidationError("Dependent property key '" + propertyName + "' for existing parent key '" + mandatoryPropertyEntry.getKey() + "' is missing", jsonPath);
						}
					}
				}
			}
		}
	}
}
