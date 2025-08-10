package de.soderer.json.schema;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import de.soderer.json.Json5Reader;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.validator.AdditionalPropertiesValidator;
import de.soderer.json.schema.validator.AllOfValidator;
import de.soderer.json.schema.validator.AnyOfValidator;
import de.soderer.json.schema.validator.BaseJsonSchemaValidator;
import de.soderer.json.schema.validator.BooleanValidator;
import de.soderer.json.schema.validator.ConstValidator;
import de.soderer.json.schema.validator.ContainsValidator;
import de.soderer.json.schema.validator.ContentEncodingValidator;
import de.soderer.json.schema.validator.ContentMediaTypeValidator;
import de.soderer.json.schema.validator.DependenciesValidator;
import de.soderer.json.schema.validator.DisallowValidator;
import de.soderer.json.schema.validator.EnumValidator;
import de.soderer.json.schema.validator.ExclusiveMaximumValidator;
import de.soderer.json.schema.validator.ExclusiveMinimumValidator;
import de.soderer.json.schema.validator.ExtendsValidator;
import de.soderer.json.schema.validator.FormatValidator;
import de.soderer.json.schema.validator.IfThenElseValidator;
import de.soderer.json.schema.validator.ItemsValidator;
import de.soderer.json.schema.validator.MaxItemsValidator;
import de.soderer.json.schema.validator.MaxLengthValidator;
import de.soderer.json.schema.validator.MaxPropertiesValidator;
import de.soderer.json.schema.validator.MaximumValidator;
import de.soderer.json.schema.validator.MinItemsValidator;
import de.soderer.json.schema.validator.MinLengthValidator;
import de.soderer.json.schema.validator.MinPropertiesValidator;
import de.soderer.json.schema.validator.MinimumValidator;
import de.soderer.json.schema.validator.MultipleOfValidator;
import de.soderer.json.schema.validator.NotValidator;
import de.soderer.json.schema.validator.OneOfValidator;
import de.soderer.json.schema.validator.PatternPropertiesValidator;
import de.soderer.json.schema.validator.PatternValidator;
import de.soderer.json.schema.validator.PropertiesValidator;
import de.soderer.json.schema.validator.PropertyNamesValidator;
import de.soderer.json.schema.validator.ReferenceValidator;
import de.soderer.json.schema.validator.RequiredValidator;
import de.soderer.json.schema.validator.TypeValidator;
import de.soderer.json.schema.validator.UniqueItemsValidator;

/**
 * JSON Schema Validator for Draft Version v4 / v6 / v7<br />
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
	private String comment = null;
	private String id = null;
	private String title;
	private String description;
	private JsonSchemaDependencyResolver jsonSchemaDependencyResolver;
	private List<BaseJsonSchemaValidator> validators;

	public JsonSchema(final InputStream jsonSchemaInputStream, final JsonSchemaDependency... dependencies) throws Exception {
		this(jsonSchemaInputStream, new JsonSchemaConfiguration(), dependencies);
	}

	public JsonSchema(final Boolean jsonSchemaDefinitionObject, final JsonSchemaDependency... dependencies) throws JsonSchemaDefinitionError {
		this(jsonSchemaDefinitionObject == null ? (JsonObject) null : (jsonSchemaDefinitionObject ? new JsonObject() : new JsonObject().add("not", new JsonObject())), new JsonSchemaConfiguration(), dependencies);
	}

	public JsonSchema(final Boolean jsonSchemaDefinitionObject, final JsonSchemaConfiguration jsonSchemaConfiguration, final JsonSchemaDependency... dependencies) throws JsonSchemaDefinitionError {
		this(jsonSchemaDefinitionObject == null ? (JsonObject) null : (jsonSchemaDefinitionObject ? new JsonObject() : new JsonObject().add("not", new JsonObject())), jsonSchemaConfiguration, dependencies);
	}

	public JsonSchema(final Boolean jsonSchemaDefinitionObject, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver) throws JsonSchemaDefinitionError {
		this(jsonSchemaDefinitionObject == null ? (JsonObject) null : (jsonSchemaDefinitionObject ? new JsonObject() : new JsonObject().add("not", new JsonObject())), jsonSchemaDependencyResolver);
	}

	public JsonSchema(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaDependency... dependencies) throws JsonSchemaDefinitionError {
		this(jsonSchemaDefinitionObject, new JsonSchemaConfiguration(), dependencies);
	}

	public JsonSchema(final InputStream jsonSchemaInputStream, final JsonSchemaConfiguration jsonSchemaConfiguration, final JsonSchemaDependency... dependencies) throws Exception {
		JsonNode jsonNode;
		try (JsonReader jsonReader = new Json5Reader(jsonSchemaInputStream, jsonSchemaConfiguration.getEncoding())) {
			jsonNode = jsonReader.read();
		} catch (final Exception e) {
			throw new JsonSchemaDefinitionError("Cannot read JSON-Schema: " + e.getMessage(), null);
		}

		if (jsonNode == null) {
			throw new JsonSchemaDefinitionError("Contains null data", null);
		} else if (jsonNode.isBoolean()) {
			validators = new ArrayList<>();
			validators.add(new BooleanValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(), jsonNode.getValue()));
		} else if (jsonNode.isJsonObject()) {
			readSchemaData((JsonObject) jsonNode.getValue(), jsonSchemaConfiguration, dependencies);
			jsonSchemaDependencyResolver.setJsonSchemaVersion(jsonSchemaConfiguration.getJsonSchemaVersion());
			jsonSchemaDependencyResolver.setDownloadReferencedSchemas(jsonSchemaConfiguration.isDownloadReferencedSchemas());
			validators = createValidators((JsonObject) jsonNode.getValue(), jsonSchemaDependencyResolver, new JsonSchemaPath());
		} else {
			throw new JsonSchemaDefinitionError("Does not contain JsonObject", new JsonSchemaPath());
		}
	}

	public JsonSchema(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaConfiguration jsonSchemaConfiguration, final JsonSchemaDependency... dependencies) throws JsonSchemaDefinitionError {
		if (jsonSchemaDefinitionObject == null) {
			throw new JsonSchemaDefinitionError("Contains null data", null);
		} else {
			readSchemaData(jsonSchemaDefinitionObject, jsonSchemaConfiguration, dependencies);
			jsonSchemaDependencyResolver.setJsonSchemaVersion(jsonSchemaConfiguration.getJsonSchemaVersion());
			jsonSchemaDependencyResolver.setDownloadReferencedSchemas(jsonSchemaConfiguration.isDownloadReferencedSchemas());
			validators = createValidators(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath());
		}
	}

	public JsonSchema(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver) throws JsonSchemaDefinitionError {
		if (jsonSchemaDefinitionObject == null) {
			throw new JsonSchemaDefinitionError("Contains null data", null);
		} else {
			this.jsonSchemaDependencyResolver = jsonSchemaDependencyResolver;
			validators = createValidators(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath());
		}
	}

	private void readSchemaData(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaConfiguration jsonSchemaConfiguration, final JsonSchemaDependency... dependencies) throws JsonSchemaDefinitionError {
		if (jsonSchemaDefinitionObject == null) {
			throw new JsonSchemaDefinitionError("Contains null data", null);
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
				if (jsonSchemaConfiguration.getJsonSchemaVersion() == null) {
					jsonSchemaConfiguration.setJsonSchemaVersion(JsonSchemaVersion.getJsonSchemaVersionByVersionUrl(schemaVersionUrl));
				}
			}
		}

		if (jsonSchemaDefinitionObject.containsPropertyKey("$comment")) {
			if (jsonSchemaDefinitionObject.get("$comment") == null) {
				throw new JsonSchemaDefinitionError("Invalid data type 'null' for key '$comment'", new JsonSchemaPath());
			} else if (!(jsonSchemaDefinitionObject.get("$comment") instanceof String)) {
				throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinitionObject.get("$comment").getClass().getSimpleName() + "' for key '$comment'", new JsonSchemaPath());
			} else if (comment != null) {
				throw new JsonSchemaDefinitionError("Invalid duplicate definition for JSON schema comment by key '$comment'", new JsonSchemaPath());
			} else {
				comment = (String) jsonSchemaDefinitionObject.get("$comment");
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

		jsonSchemaDependencyResolver = new JsonSchemaDependencyResolver(jsonSchemaDefinitionObject, dependencies);
	}

	public String getId() {
		return id;
	}

	public String getSchemaVersionUrl() {
		return schemaVersionUrl;
	}

	public String getComment() {
		return comment;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public JsonNode validate(final InputStream jsonDataInputStream, final Charset encoding) throws JsonSchemaDataValidationError {
		JsonNode jsonDataNode;
		try (JsonReader jsonReader = new Json5Reader(jsonDataInputStream, encoding)) {
			jsonDataNode = jsonReader.read();
		} catch (final Exception e) {
			throw new JsonSchemaDataValidationError("Cannot read JSON data: " + e.getMessage(), new JsonPath());
		}

		for (final BaseJsonSchemaValidator validator : validators) {
			validator.validate(jsonDataNode, new JsonPath());
		}
		return jsonDataNode;
	}

	public JsonNode validate(final InputStream jsonDataInputStream) throws JsonSchemaDataValidationError {
		JsonNode jsonDataNode;
		try (JsonReader jsonReader = new Json5Reader(jsonDataInputStream)) {
			jsonDataNode = jsonReader.read();
		} catch (final Exception e) {
			throw new JsonSchemaDataValidationError("Cannot read JSON data: " + e.getMessage(), new JsonPath());
		}

		for (final BaseJsonSchemaValidator validator : validators) {
			validator.validate(jsonDataNode, new JsonPath());
		}
		return jsonDataNode;
	}

	public void validate(final Object jsonData) throws JsonSchemaDataValidationError {
		JsonNode jsonDataNode;
		try {
			jsonDataNode = new JsonNode(true, jsonData);
		} catch (final Exception e) {
			throw new JsonSchemaDataValidationError(e.getMessage(), new JsonPath(), e);
		}

		for (final BaseJsonSchemaValidator validator : validators) {
			validator.validate(jsonDataNode, new JsonPath());
		}
	}

	public static List<BaseJsonSchemaValidator> createValidators(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath currentJsonSchemaPath) throws JsonSchemaDefinitionError {
		final List<BaseJsonSchemaValidator> validators = new ArrayList<>();

		Object ifJsonObject = null;
		Object thenObject = null;
		Object elseObject = null;

		for (final Entry<String, Object> entry : jsonSchemaDefinitionObject.entrySet()) {
			switch (entry.getKey()) {
				case "type":
					validators.add(new TypeValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "contentMediaType":
					validators.add(new ContentMediaTypeValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "contentEncoding":
					validators.add(new ContentEncodingValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "disallow":
					validators.add(new DisallowValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "properties":
					validators.add(new PropertiesValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "propertyNames":
					validators.add(new PropertyNamesValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "patternProperties":
					validators.add(new PatternPropertiesValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "additionalProperties":
					validators.add(new AdditionalPropertiesValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "required":
					validators.add(new RequiredValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "minProperties":
					validators.add(new MinPropertiesValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "maxProperties":
					validators.add(new MaxPropertiesValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "items":
					validators.add(new ItemsValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "contains":
					validators.add(new ContainsValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "minItems":
					validators.add(new MinItemsValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "maxItems":
					validators.add(new MaxItemsValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "minLength":
					validators.add(new MinLengthValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "maxLength":
					validators.add(new MaxLengthValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "uniqueItems":
					validators.add(new UniqueItemsValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "minimum":
					validators.add(new MinimumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "maximum":
					validators.add(new MaximumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "divisibleBy":
				case "multipleOf":
					validators.add(new MultipleOfValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "anyOf":
					validators.add(new AnyOfValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "allOf":
					validators.add(new AllOfValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "oneOf":
					validators.add(new OneOfValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "not":
					validators.add(new NotValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "enum":
					validators.add(new EnumValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "const":
					validators.add(new ConstValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "format":
					validators.add(new FormatValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "pattern":
					validators.add(new PatternValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "dependencies":
					validators.add(new DependenciesValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;
				case "examples":
					// Ignore any example data
					break;

				case "exclusiveMinimum":
					// Do nothing, because this is validated by MinimumValidator, too
					// Value must be of boolean type and "minimum" value item must exist in simpleMode and draft v3/v4.
					if ((jsonSchemaDependencyResolver.isSimpleMode() || jsonSchemaDependencyResolver.isDraftV3Mode() || jsonSchemaDependencyResolver.isDraftV4Mode())
							&& !jsonSchemaDefinitionObject.containsPropertyKey("minimum")) {
						throw new JsonSchemaDefinitionError("Missing 'minimum' rule for 'exclusiveMinimum'", currentJsonSchemaPath);
					} else if (!jsonSchemaDependencyResolver.isSimpleMode() && !jsonSchemaDependencyResolver.isDraftV3Mode() && !jsonSchemaDependencyResolver.isDraftV4Mode()) {
						validators.add(new ExclusiveMinimumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					}
					break;
				case "exclusiveMaximum":
					// Do nothing, because this is validated by MaximumValidator, too
					// Value must be of boolean type and "maximum" value item must exist in simpleMode and draft v3/v4.
					if ((jsonSchemaDependencyResolver.isSimpleMode() || jsonSchemaDependencyResolver.isDraftV3Mode() || jsonSchemaDependencyResolver.isDraftV4Mode())
							&& !jsonSchemaDefinitionObject.containsPropertyKey("maximum")) {
						throw new JsonSchemaDefinitionError("Missing 'maximum' rule for 'exclusiveMaximum'", currentJsonSchemaPath);
					} else if (!jsonSchemaDependencyResolver.isSimpleMode() && !jsonSchemaDependencyResolver.isDraftV3Mode() && !jsonSchemaDependencyResolver.isDraftV4Mode()) {
						validators.add(new ExclusiveMaximumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					}
					break;
				case "additionalItems":
					// Do nothing, because this is validated by ItemsValidator, if there is any
					break;

				case "extends":
					validators.add(new ExtendsValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					break;

				case "$ref":
					validators.add(new ReferenceValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(parseJsonSchemaReference(jsonSchemaDependencyResolver, (String) entry.getValue(), currentJsonSchemaPath).toString()), entry.getValue()));
					break;

				case "id":
					if (!jsonSchemaDependencyResolver.isSimpleMode() && !jsonSchemaDependencyResolver.isDraftV3Mode() && !jsonSchemaDependencyResolver.isDraftV4Mode()) {
						throw new JsonSchemaDefinitionError("JSON schema 'id' on top level of JSON schema is only allowed for JSON schema versions draft v4 and lower", currentJsonSchemaPath);
					}
					// $id should be a descriptive url
					if (!currentJsonSchemaPath.isRoot()) {
						throw new JsonSchemaDefinitionError("JSON schema 'id' must be defined on top level of JSON schema", currentJsonSchemaPath);
					}
					break;
				case "$id":
					if (jsonSchemaDependencyResolver.isSimpleMode() || jsonSchemaDependencyResolver.isDraftV3Mode() || jsonSchemaDependencyResolver.isDraftV4Mode()) {
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
				case "if":
					// Main part of "if-then-else" construct
					if (entry.getValue() == null) {
						throw new JsonSchemaDefinitionError("Invalid data type 'null' for key 'if'. JsonObject or Boolean expected", currentJsonSchemaPath);
					} else if (entry.getValue() instanceof JsonObject) {
						ifJsonObject = entry.getValue();
					} else if (entry.getValue() instanceof Boolean) {
						ifJsonObject = entry.getValue();
					} else {
						throw new JsonSchemaDefinitionError("Invalid data type '" + entry.getValue().getClass().getSimpleName() + "' for key 'if'. JsonObject or Boolean expected", currentJsonSchemaPath);
					}
					break;
				case "then":
					// Supplemental part of "if-then-else" construct
					thenObject = entry.getValue();
					break;
				case "else":
					// Supplemental part of "if-then-else" construct
					elseObject = entry.getValue();
					break;
				default:
					if (jsonSchemaDependencyResolver.isSimpleMode()) {
						throw new JsonSchemaDefinitionError("Unexpected data key '" + entry.getKey() + "'", currentJsonSchemaPath);
					}
			}
		}

		if (ifJsonObject != null) {
			validators.add(new IfThenElseValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath, ifJsonObject, thenObject, elseObject));
		}

		return validators;
	}

	private static JsonSchemaPathElement parseJsonSchemaReference(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final String referenceValue, final JsonSchemaPath jsonSchemaPath) throws JsonSchemaDefinitionError {
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
