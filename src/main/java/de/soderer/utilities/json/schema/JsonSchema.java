package de.soderer.utilities.json.schema;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import de.soderer.utilities.json.Json5Reader;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.JsonReader;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.validator.AdditionalPropertiesValidator;
import de.soderer.utilities.json.schema.validator.AllOfValidator;
import de.soderer.utilities.json.schema.validator.AnyOfValidator;
import de.soderer.utilities.json.schema.validator.BaseJsonSchemaValidator;
import de.soderer.utilities.json.schema.validator.DependenciesValidator;
import de.soderer.utilities.json.schema.validator.EnumValidator;
import de.soderer.utilities.json.schema.validator.ExclusiveMaximumValidator;
import de.soderer.utilities.json.schema.validator.ExclusiveMinimumValidator;
import de.soderer.utilities.json.schema.validator.FormatValidator;
import de.soderer.utilities.json.schema.validator.ItemsValidator;
import de.soderer.utilities.json.schema.validator.MaxItemsValidator;
import de.soderer.utilities.json.schema.validator.MaxLengthValidator;
import de.soderer.utilities.json.schema.validator.MaxPropertiesValidator;
import de.soderer.utilities.json.schema.validator.MaximumValidator;
import de.soderer.utilities.json.schema.validator.MinItemsValidator;
import de.soderer.utilities.json.schema.validator.MinLengthValidator;
import de.soderer.utilities.json.schema.validator.MinPropertiesValidator;
import de.soderer.utilities.json.schema.validator.MinimumValidator;
import de.soderer.utilities.json.schema.validator.MultipleOfValidator;
import de.soderer.utilities.json.schema.validator.NotValidator;
import de.soderer.utilities.json.schema.validator.OneOfValidator;
import de.soderer.utilities.json.schema.validator.PatternPropertiesValidator;
import de.soderer.utilities.json.schema.validator.PatternValidator;
import de.soderer.utilities.json.schema.validator.PropertiesValidator;
import de.soderer.utilities.json.schema.validator.PropertyNamesValidator;
import de.soderer.utilities.json.schema.validator.ReferenceValidator;
import de.soderer.utilities.json.schema.validator.RequiredValidator;
import de.soderer.utilities.json.schema.validator.TypeValidator;
import de.soderer.utilities.json.schema.validator.UniqueItemsValidator;

/**
 * JSON Schema Validator vor Draft Version v4 / v6 / v7<br />
 * https://json-schema.org/draft-04/schema#<br />
 * https://json-schema.org/draft-06/schema#<br />
 * https://json-schema.org/draft-07/schema#<br />
 * <br />
 * For Validation of JSON schemas you may use the included file resource "JsonSchemaDescriptionDraftVx.json":<br />
 * JsonSchema.class.getClassLoader().getResourceAsStream("json/JsonSchemaDescriptionDraftV4.json")<br />
 * JsonSchema.class.getClassLoader().getResourceAsStream("json/JsonSchemaDescriptionDraftV6.json")<br />
 * JsonSchema.class.getClassLoader().getResourceAsStream("json/JsonSchemaDescriptionDraftV7.json")<br />
 * <br />
 * For the JSON schema standard definition see:<br />
 * https://json-schema.org<br />
 * <br />
 * For examples and help on JSON schema creation see:<br />
 * https://spacetelescope.github.io<br />
 */
public class JsonSchema {
	/**
	 * Url describing the JSON schema standard and version a JSON schema was written for in compliance<br />
	 * Example: "https://json-schema.org/schema#"
	 */
	private String schemaVersionUrl = null;

	private String id = null;
	private String title;
	private String description;
	private JsonObject jsonSchemaDefinition;
	private JsonSchemaDependencyResolver jsonSchemaDependencyResolver;

	/**
	 * Draft V7 mode is the default mode<br />
	 */
	public JsonSchema(final InputStream jsonSchemaInputStream) throws Exception {
		this(jsonSchemaInputStream, StandardCharsets.UTF_8, JsonSchemaVersion.draftV7);
	}

	/**
	 * Draft V7 mode is the default mode<br />
	 */
	public JsonSchema(final InputStream jsonSchemaInputStream, final Charset encoding) throws Exception {
		this(jsonSchemaInputStream, encoding, JsonSchemaVersion.draftV7);
	}

	/**
	 * Draft V7 mode is the default mode<br />
	 */
	public JsonSchema(final InputStream jsonSchemaInputStream, final JsonSchemaVersion jsonSchemaVersion) throws Exception {
		this(jsonSchemaInputStream, StandardCharsets.UTF_8, jsonSchemaVersion);
	}

	/**
	 * Draft V7 mode is the default mode<br />
	 */
	public JsonSchema(final InputStream jsonSchemaInputStream, final Charset encoding, final JsonSchemaVersion jsonSchemaVersion) throws Exception {
		JsonNode jsonNode;
		try (JsonReader jsonReader = new Json5Reader(jsonSchemaInputStream, encoding)) {
			jsonNode = jsonReader.read();
		} catch (final Exception e) {
			throw new JsonSchemaDefinitionError("Cannot read JSON-Schema: " + e.getMessage(), null);
		}

		if (jsonNode == null) {
			throw new JsonSchemaDefinitionError("Contains null data", null);
		} else if (jsonNode.isJsonObject()) {
			readSchemaData((JsonObject) jsonNode.getValue());
			jsonSchemaDependencyResolver.setJsonSchemaVersion(jsonSchemaVersion);
		} else {
			throw new JsonSchemaDefinitionError("Does not contain JsonObject", new JsonSchemaPath());
		}
	}

	/**
	 * Draft V7 mode is the default mode<br />
	 */
	public JsonSchema(final JsonObject jsonSchemaDefinition) throws Exception {
		this(jsonSchemaDefinition, JsonSchemaVersion.draftV7);
	}

	/**
	 * Draft V7 mode is the default mode<br />
	 */
	public JsonSchema(final JsonObject jsonSchemaDefinition, final JsonSchemaVersion jsonSchemaVersion) throws Exception {
		readSchemaData(jsonSchemaDefinition);
		jsonSchemaDependencyResolver.setJsonSchemaVersion(jsonSchemaVersion);
	}

	private void readSchemaData(final JsonObject jsonSchemaDefinitionObject) throws Exception {
		if (jsonSchemaDefinitionObject == null) {
			throw new JsonSchemaDefinitionError("Contains null data", null);
		} else {
			jsonSchemaDefinition = jsonSchemaDefinitionObject;
		}

		if (jsonSchemaDefinitionObject.containsPropertyKey("id")) {
			if (jsonSchemaDefinitionObject.get("id") == null) {
				throw new JsonSchemaDefinitionError("Invalid data type 'null' for key 'id'", new JsonSchemaPath());
			} else if (!(jsonSchemaDefinitionObject.get("id") instanceof String)) {
				throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinitionObject.get("id").getClass().getSimpleName() + "' for key 'id'", new JsonSchemaPath());
			} else if (id != null) {
				throw new JsonSchemaDefinitionError("Invalid duplicate definition for JSON schema id by key 'id'", new JsonSchemaPath());
			} else {
				id = (String) jsonSchemaDefinitionObject.get("id");
			}
		}

		if (jsonSchemaDefinitionObject.containsPropertyKey("$id")) {
			if (jsonSchemaDefinitionObject.get("$id") == null) {
				throw new JsonSchemaDefinitionError("Invalid data type 'null' for key '$id'", new JsonSchemaPath());
			} else if (!(jsonSchemaDefinitionObject.get("$id") instanceof String)) {
				throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinitionObject.get("$id").getClass().getSimpleName() + "' for key '$id'", new JsonSchemaPath());
			} else if (id != null) {
				throw new JsonSchemaDefinitionError("Invalid duplicate definition for JSON schema id by key '$id'", new JsonSchemaPath());
			} else {
				id = (String) jsonSchemaDefinitionObject.get("$id");
			}
		}

		if (jsonSchemaDefinitionObject.containsPropertyKey("$schema")) {
			if (jsonSchemaDefinitionObject.get("$schema") == null) {
				throw new JsonSchemaDefinitionError("Invalid data type 'null' for key '$schema'", new JsonSchemaPath());
			} else if (!(jsonSchemaDefinitionObject.get("$schema") instanceof String)) {
				throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinitionObject.get("$schema").getClass().getSimpleName() + "' for key '$schema'", new JsonSchemaPath());
			} else if (schemaVersionUrl != null) {
				throw new JsonSchemaDefinitionError("Invalid duplicate definition for JSON schema version url by key '$schema'", new JsonSchemaPath());
			} else {
				schemaVersionUrl = (String) jsonSchemaDefinitionObject.get("$schema");
			}
		}

		if (jsonSchemaDefinitionObject.containsPropertyKey("title")) {
			if (jsonSchemaDefinitionObject.get("title") == null) {
				throw new JsonSchemaDefinitionError("Invalid data type 'null' for key 'title'", new JsonSchemaPath());
			} else if (!(jsonSchemaDefinitionObject.get("title") instanceof String)) {
				throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinitionObject.get("title").getClass().getSimpleName() + "' for key 'title'", new JsonSchemaPath());
			} else if (title != null) {
				throw new JsonSchemaDefinitionError("Invalid duplicate definition for JSON schema title url by key 'title'", new JsonSchemaPath());
			} else {
				title = (String) jsonSchemaDefinitionObject.get("title");
			}
		}

		if (jsonSchemaDefinitionObject.containsPropertyKey("description")) {
			if (jsonSchemaDefinitionObject.get("description") == null) {
				throw new JsonSchemaDefinitionError("Invalid data type 'null' for key 'description'", new JsonSchemaPath());
			} else if (!(jsonSchemaDefinitionObject.get("description") instanceof String)) {
				throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinitionObject.get("description").getClass().getSimpleName() + "' for key 'description'", new JsonSchemaPath());
			} else if (description != null) {
				throw new JsonSchemaDefinitionError("Invalid duplicate definition for JSON schema description by key 'description'", new JsonSchemaPath());
			} else {
				description = (String) jsonSchemaDefinitionObject.get("description");
			}
		}

		jsonSchemaDependencyResolver = new JsonSchemaDependencyResolver(jsonSchemaDefinitionObject);
	}

	/**
	 * Download of any additional data is prevented by default.<br />
	 * Especially because there is no check for internet connection in forehand.<br />
	 */
	public void setDownloadReferencedSchemas(final boolean downloadReferencedSchemas) {
		jsonSchemaDependencyResolver.setDownloadReferencedSchemas(downloadReferencedSchemas);
	}

	/**
	 * Add some other JSON schema for usage of its reference definitions
	 *
	 * @param definitionPackageName
	 * @param jsonSchemaInputStream
	 * @throws Exception
	 */
	public void addJsonSchemaDefinition(final String definitionPackageName, final InputStream jsonSchemaInputStream) throws Exception {
		jsonSchemaDependencyResolver.addJsonSchemaDefinition(definitionPackageName, jsonSchemaInputStream);
	}

	public String getId() {
		return id;
	}

	public String getSchemaVersionUrl() {
		return schemaVersionUrl;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public JsonNode validate(final InputStream jsonDataInputStream, final Charset encoding) throws Exception {
		JsonNode jsonDataNode;
		try (JsonReader jsonReader = new Json5Reader(jsonDataInputStream, encoding)) {
			jsonDataNode = jsonReader.read();
		} catch (final Exception e) {
			throw new JsonSchemaDataValidationError("Cannot read JSON data: " + e.getMessage(), new JsonPath());
		}

		final List<BaseJsonSchemaValidator> validators = createValidators(jsonSchemaDefinition, jsonSchemaDependencyResolver, new JsonSchemaPath(), jsonDataNode, new JsonPath());
		for (final BaseJsonSchemaValidator validator : validators) {
			validator.validate();
		}
		return jsonDataNode;
	}

	public JsonNode validate(final InputStream jsonDataInputStream) throws Exception {
		JsonNode jsonDataNode;
		try (JsonReader jsonReader = new Json5Reader(jsonDataInputStream)) {
			jsonDataNode = jsonReader.read();
		} catch (final Exception e) {
			throw new JsonSchemaDataValidationError("Cannot read JSON data: " + e.getMessage(), new JsonPath());
		}

		final List<BaseJsonSchemaValidator> validators = createValidators(jsonSchemaDefinition, jsonSchemaDependencyResolver, new JsonSchemaPath(), jsonDataNode, new JsonPath());
		for (final BaseJsonSchemaValidator validator : validators) {
			validator.validate();
		}
		return jsonDataNode;
	}

	public void validate(final Object jsonData) throws Exception {
		final JsonNode jsonDataNode = new JsonNode(jsonData);

		final List<BaseJsonSchemaValidator> validators = createValidators(jsonSchemaDefinition, jsonSchemaDependencyResolver, new JsonSchemaPath(), jsonDataNode, new JsonPath());
		for (final BaseJsonSchemaValidator validator : validators) {
			validator.validate();
		}
	}

	public static List<BaseJsonSchemaValidator> createValidators(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath currentJsonSchemaPath, final JsonNode jsonNode, final JsonPath currentJsonPath) throws Exception {
		final List<BaseJsonSchemaValidator> validators = new ArrayList<>();
		for (final Entry<String, Object> entry : jsonSchemaDefinitionObject.entrySet()) {
			switch (entry.getKey()) {
				case "type":
					validators.add(new TypeValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "properties":
					validators.add(new PropertiesValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "propertyNames":
					validators.add(new PropertyNamesValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "patternProperties":
					validators.add(new PatternPropertiesValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "additionalProperties":
					validators.add(new AdditionalPropertiesValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "required":
					validators.add(new RequiredValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "minProperties":
					validators.add(new MinPropertiesValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "maxProperties":
					validators.add(new MaxPropertiesValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "items":
					validators.add(new ItemsValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "minItems":
					validators.add(new MinItemsValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "maxItems":
					validators.add(new MaxItemsValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "minLength":
					validators.add(new MinLengthValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "maxLength":
					validators.add(new MaxLengthValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "uniqueItems":
					validators.add(new UniqueItemsValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "minimum":
					validators.add(new MinimumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "maximum":
					validators.add(new MaximumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "multipleOf":
					validators.add(new MultipleOfValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "anyOf":
					validators.add(new AnyOfValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "allOf":
					validators.add(new AllOfValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "oneOf":
					validators.add(new OneOfValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "not":
					validators.add(new NotValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "enum":
					validators.add(new EnumValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "format":
					validators.add(new FormatValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "pattern":
					validators.add(new PatternValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "dependencies":
					validators.add(new DependenciesValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					break;

				case "exclusiveMinimum":
					// Do nothing, because this is validated by MinimumValidator, too
					// Value must be of boolean type and "minimum" value item must exist in simpleMode and draft v4.
					if ((jsonSchemaDependencyResolver.isSimpleMode() || jsonSchemaDependencyResolver.isDraftV4Mode())
							&& !jsonSchemaDefinitionObject.containsPropertyKey("minimum")) {
						throw new JsonSchemaDefinitionError("Missing 'minimum' rule for 'exclusiveMinimum'", currentJsonSchemaPath);
					} else if (!jsonSchemaDependencyResolver.isSimpleMode() && !jsonSchemaDependencyResolver.isDraftV4Mode()) {
						validators.add(new ExclusiveMinimumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					}
					break;
				case "exclusiveMaximum":
					// Do nothing, because this is validated by MaximumValidator, too
					// Value must be of boolean type and "maximum" value item must exist in simpleMode and draft v4.
					if ((jsonSchemaDependencyResolver.isSimpleMode() || jsonSchemaDependencyResolver.isDraftV4Mode())
							&& !jsonSchemaDefinitionObject.containsPropertyKey("maximum")) {
						throw new JsonSchemaDefinitionError("Missing 'maximum' rule for 'exclusiveMaximum'", currentJsonSchemaPath);
					} else if (!jsonSchemaDependencyResolver.isSimpleMode() && !jsonSchemaDependencyResolver.isDraftV4Mode()) {
						validators.add(new ExclusiveMaximumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue(), jsonNode, currentJsonPath));
					}
					break;
				case "additionalItems":
					// Do nothing, because this is validated by ItemsValidator, too
					if (!jsonSchemaDefinitionObject.containsPropertyKey("items") && jsonSchemaDependencyResolver.isSimpleMode()) {
						throw new JsonSchemaDefinitionError("Missing 'items' rule for 'additionalItems'", currentJsonSchemaPath);
					}
					break;

				case "$ref":
					validators.add(new ReferenceValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(parseJsonSchemaReference(jsonSchemaDependencyResolver, (String) entry.getValue(), currentJsonSchemaPath).toString()), entry.getValue(), jsonNode, currentJsonPath));
					break;

				case "id":
					if (!jsonSchemaDependencyResolver.isSimpleMode() && !jsonSchemaDependencyResolver.isDraftV4Mode()) {
						throw new JsonSchemaDefinitionError("JSON schema 'id' on top level of JSON schema is only allowed for JSON schema versions draft v4 and lower", currentJsonSchemaPath);
					}
					// $id should be a descriptive url
					if (!currentJsonSchemaPath.isRoot()) {
						throw new JsonSchemaDefinitionError("JSON schema 'id' must be defined on top level of JSON schema", currentJsonSchemaPath);
					}
					break;
				case "$id":
					if (jsonSchemaDependencyResolver.isSimpleMode() || jsonSchemaDependencyResolver.isDraftV4Mode()) {
						throw new JsonSchemaDefinitionError("JSON schema '$id' on top level of JSON schema is only allowed for JSON schema versions draft v6 and higher", currentJsonSchemaPath);
					}
					// $id should be a descriptive url
					if (!currentJsonSchemaPath.isRoot()) {
						throw new JsonSchemaDefinitionError("JSON schema '$id' must be defined on top level of JSON schema", currentJsonSchemaPath);
					}
					break;
				case "$schema":
					// $schema should be a descriptive url
					if (!currentJsonSchemaPath.isRoot()) {
						throw new JsonSchemaDefinitionError("JSON schema '$schema' must be defined on top level of JSON schema", currentJsonSchemaPath);
					}
					break;
				case "definitions":
					// Defined JSON schema definitions
					if (!currentJsonSchemaPath.isRoot()) {
						throw new JsonSchemaDefinitionError("JSON schema 'definitions' must be defined on top level of JSON schema", currentJsonSchemaPath);
					}
					break;

				case "title":
					// Descriptive title
					if (!(entry.getValue() instanceof String)) {
						throw new JsonSchemaDefinitionError("Invalid data type '" + entry.getValue().getClass().getSimpleName() + "' for key 'title'", currentJsonSchemaPath);
					}
					break;
				case "description":
					// Descriptive comments
					if (!(entry.getValue() instanceof String)) {
						throw new JsonSchemaDefinitionError("Invalid data type '" + entry.getValue().getClass().getSimpleName() + "' for key 'description'", currentJsonSchemaPath);
					}
					break;
				case "default":
					// Default value for processing the given JSON data, which is irrelevant for validation
					break;
				default:
					if (jsonSchemaDependencyResolver.isSimpleMode()) {
						throw new JsonSchemaDefinitionError("Unexpected data key '" + entry.getKey() + "'", currentJsonSchemaPath);
					}
			}
		}
		return validators;
	}

	private static JsonSchemaPathElement parseJsonSchemaReference(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final String referenceValue, final JsonSchemaPath jsonSchemaPath) throws Exception {
		if (referenceValue.startsWith("#")) {
			return new JsonSchemaPathInternalReference(referenceValue);
		} else if (referenceValue.contains("#")) {
			final String schemaLocation = referenceValue.substring(0, referenceValue.indexOf("#"));
			final String referenceString = referenceValue.substring(referenceValue.indexOf("#"));
			return new JsonSchemaPathExternalReference(schemaLocation, referenceString);
		} else {
			if (jsonSchemaDependencyResolver != null) {
				final String schemaName = jsonSchemaDependencyResolver.getSchemaContainingReference(referenceValue, jsonSchemaPath);
				if (schemaName == null) {
					throw new JsonSchemaDefinitionError("Invalid schema reference: " + referenceValue, jsonSchemaPath);
				} else if ("#".equals(schemaName)) {
					return new JsonSchemaPathInternalReference(referenceValue);
				} else {
					return new JsonSchemaPathExternalReference(schemaName, referenceValue);
				}
			} else {
				throw new JsonSchemaDefinitionError("Invalid schema reference: " + referenceValue, jsonSchemaPath);
			}
		}
	}
}
