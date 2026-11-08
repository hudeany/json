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
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueString;
import de.soderer.json.exception.DuplicateKeyException;
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
import de.soderer.json.schema.validator.DependentRequiredValidator;
import de.soderer.json.schema.validator.DependentSchemasValidator;
import de.soderer.json.schema.validator.DisallowValidator;
import de.soderer.json.schema.validator.DynamicReferenceValidator;
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
import de.soderer.json.schema.validator.PrefixItemsValidator;
import de.soderer.json.schema.validator.PropertiesValidator;
import de.soderer.json.schema.validator.PropertyNamesValidator;
import de.soderer.json.schema.validator.ReferenceValidator;
import de.soderer.json.schema.validator.RequiredValidator;
import de.soderer.json.schema.validator.SchemaObjectValidator;
import de.soderer.json.schema.validator.TypeValidator;
import de.soderer.json.schema.validator.UnevaluatedItemsValidator;
import de.soderer.json.schema.validator.UnevaluatedPropertiesValidator;
import de.soderer.json.schema.validator.UniqueItemsValidator;

/**
 * JSON Schema Validator for Draft Version v3 / v4 / v6 / v7 / 2019-09 / 2020-12<br />
 * https://json-schema.org/draft-04/schema#<br />
 * https://json-schema.org/draft-06/schema#<br />
 * https://json-schema.org/draft-07/schema#<br />
 * https://json-schema.org/draft/2019-09/schema<br />
 * https://json-schema.org/draft/2020-12/schema<br />
 * <br />
 * The JSON schema version is taken from {@link JsonSchemaConfiguration} or detected from "$schema".
 * Without both, simple mode (strict validation without version specific rules) is used.<br />
 * <br />
 * For Validation of JSON schemas you may use the included meta schema file resources, see
 * {@link JsonSchemaVersion#getLocalFile()}:<br />
 * JsonSchema.class.getClassLoader().getResourceAsStream("json/JsonSchemaDescriptionDraftV4.json")<br />
 * JsonSchema.class.getClassLoader().getResourceAsStream("json/JsonSchemaDescriptionDraftV6.json")<br />
 * JsonSchema.class.getClassLoader().getResourceAsStream("json/JsonSchemaDescriptionDraftV7.json")<br />
 * JsonSchema.class.getClassLoader().getResourceAsStream("json/JsonSchemaDescriptionDraft2019-09/schema.json")<br />
 * JsonSchema.class.getClassLoader().getResourceAsStream("json/JsonSchemaDescriptionDraft2020-12/schema.json")<br />
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

	/** Value of "$comment". */
	private String comment = null;

	/** Value of "id" (up to draft 4) or "$id" (since draft 6). */
	private String id = null;

	/** Value of "title". */
	private String title;

	/** Value of "description". */
	private String description;

	/** Resolver for references and settings of this schema. */
	private JsonSchemaDependencyResolver jsonSchemaDependencyResolver;

	/** Validators of the top level keywords of this schema. */
	private List<BaseJsonSchemaValidator> validators;

	/**
	 * Maximum number of errors reported by {@link #validateAll(JsonNode)}.
	 */
	private int maximumValidationErrors = JsonSchemaConfiguration.DEFAULT_MAXIMUM_VALIDATION_ERRORS;

	/**
	 * Reads a JSON schema with default configuration. The schema may be written in JSON5 syntax.
	 *
	 * @param jsonSchemaInputStream
	 *            the stream to read the schema from
	 * @param dependencies
	 *            additional JSON schemas referenced by this schema via "$ref"
	 * @throws Exception
	 *             if the schema cannot be read or is invalid
	 */
	public JsonSchema(final InputStream jsonSchemaInputStream, final JsonSchemaDependency... dependencies) throws Exception {
		this(jsonSchemaInputStream, new JsonSchemaConfiguration(), dependencies);
	}

	/**
	 * Creates a boolean JSON schema with default configuration: true accepts any data, false rejects
	 * any data.
	 *
	 * @param jsonSchemaDefinitionObject
	 *            the boolean schema value
	 * @param dependencies
	 *            additional JSON schemas referenced by this schema via "$ref"
	 * @throws DuplicateKeyException
	 *             never thrown for boolean schemas, declared by the delegate constructor
	 * @throws Exception
	 *             if the value is null
	 */
	public JsonSchema(final Boolean jsonSchemaDefinitionObject, final JsonSchemaDependency... dependencies) throws DuplicateKeyException, Exception {
		this(jsonSchemaDefinitionObject == null ? (JsonObject) null : (jsonSchemaDefinitionObject ? new JsonObject() : new JsonObject().add("not", new JsonObject())), new JsonSchemaConfiguration(), dependencies);
	}

	/**
	 * Creates a boolean JSON schema: true accepts any data, false rejects any data.
	 *
	 * @param jsonSchemaDefinitionObject
	 *            the boolean schema value
	 * @param jsonSchemaConfiguration
	 *            the configuration (JSON schema version, encoding etc.)
	 * @param dependencies
	 *            additional JSON schemas referenced by this schema via "$ref"
	 * @throws DuplicateKeyException
	 *             never thrown for boolean schemas, declared by the delegate constructor
	 * @throws Exception
	 *             if the value is null
	 */
	public JsonSchema(final Boolean jsonSchemaDefinitionObject, final JsonSchemaConfiguration jsonSchemaConfiguration, final JsonSchemaDependency... dependencies) throws DuplicateKeyException, Exception {
		this(jsonSchemaDefinitionObject == null ? (JsonObject) null : (jsonSchemaDefinitionObject ? new JsonObject() : new JsonObject().add("not", new JsonObject())), jsonSchemaConfiguration, dependencies);
	}

	/**
	 * Creates a boolean JSON schema using an existing dependency resolver: true accepts any data,
	 * false rejects any data.
	 *
	 * @param jsonSchemaDefinitionObject
	 *            the boolean schema value
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings
	 * @throws JsonSchemaDefinitionError
	 *             if the value is null
	 * @throws DuplicateKeyException
	 *             never thrown for boolean schemas, declared by the delegate constructor
	 */
	public JsonSchema(final Boolean jsonSchemaDefinitionObject, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver) throws JsonSchemaDefinitionError, DuplicateKeyException {
		this(jsonSchemaDefinitionObject == null ? (JsonObject) null : (jsonSchemaDefinitionObject ? new JsonObject() : new JsonObject().add("not", new JsonObject())), jsonSchemaDependencyResolver);
	}

	/**
	 * Creates a JSON schema from an already parsed schema object with default configuration.
	 *
	 * @param jsonSchemaDefinitionObject
	 *            the schema object
	 * @param dependencies
	 *            additional JSON schemas referenced by this schema via "$ref"
	 * @throws Exception
	 *             if the schema is null or invalid
	 */
	public JsonSchema(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaDependency... dependencies) throws Exception {
		this(jsonSchemaDefinitionObject, new JsonSchemaConfiguration(), dependencies);
	}

	/**
	 * Reads a JSON schema. The schema may be written in JSON5 syntax and may be a boolean schema.
	 *
	 * @param jsonSchemaInputStream
	 *            the stream to read the schema from
	 * @param jsonSchemaConfiguration
	 *            the configuration (JSON schema version, encoding etc.)
	 * @param dependencies
	 *            additional JSON schemas referenced by this schema via "$ref"
	 * @throws JsonSchemaDefinitionError
	 *             if the schema cannot be read or is invalid
	 */
	public JsonSchema(final InputStream jsonSchemaInputStream, final JsonSchemaConfiguration jsonSchemaConfiguration, final JsonSchemaDependency... dependencies) throws JsonSchemaDefinitionError {
		JsonNode jsonNode;
		try (JsonReader jsonReader = new Json5Reader(jsonSchemaInputStream, jsonSchemaConfiguration.getEncoding())) {
			jsonNode = jsonReader.read();
		} catch (final Exception e) {
			throw new JsonSchemaDefinitionError("Cannot read JSON-Schema: " + e.getMessage(), null, e);
		}

		if (jsonNode == null) {
			throw new JsonSchemaDefinitionError("Contains null data", null);
		} else if (jsonNode.isBoolean()) {
			validators = new ArrayList<>();
			// A top-level boolean schema has no "$ref"/"definitions" of its own, so no dependency resolver is needed
			// or available yet at this point (readSchemaData(), which creates it, is only called in the JsonObject
			// branch below).
			validators.add(new BooleanValidator(null, new JsonSchemaPath(), jsonNode));
		} else if (jsonNode.isJsonObject()) {
			try {
				readSchemaData((JsonObject) jsonNode, jsonSchemaConfiguration, dependencies);
				jsonSchemaDependencyResolver.setJsonSchemaVersion(jsonSchemaConfiguration.getJsonSchemaVersion());
				jsonSchemaDependencyResolver.setDownloadReferencedSchemas(jsonSchemaConfiguration.isDownloadReferencedSchemas());
				validators = createValidators((JsonObject) jsonNode, jsonSchemaDependencyResolver, new JsonSchemaPath());
			} catch (final JsonSchemaDefinitionError e) {
				throw e;
			} catch (final Exception e) {
				throw new JsonSchemaDefinitionError("Invalid JSON schema data: " + e.getMessage(), null, e);
			}
		} else {
			throw new JsonSchemaDefinitionError("Does not contain JsonObject", new JsonSchemaPath());
		}
	}

	/**
	 * Creates a JSON schema from an already parsed schema object.
	 *
	 * @param jsonSchemaDefinitionObject
	 *            the schema object
	 * @param jsonSchemaConfiguration
	 *            the configuration (JSON schema version, encoding etc.)
	 * @param dependencies
	 *            additional JSON schemas referenced by this schema via "$ref"
	 * @throws Exception
	 *             if the schema is null or invalid
	 */
	public JsonSchema(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaConfiguration jsonSchemaConfiguration, final JsonSchemaDependency... dependencies) throws Exception {
		if (jsonSchemaDefinitionObject == null) {
			throw new JsonSchemaDefinitionError("Contains null data", null);
		} else {
			readSchemaData(jsonSchemaDefinitionObject, jsonSchemaConfiguration, dependencies);
			jsonSchemaDependencyResolver.setJsonSchemaVersion(jsonSchemaConfiguration.getJsonSchemaVersion());
			jsonSchemaDependencyResolver.setDownloadReferencedSchemas(jsonSchemaConfiguration.isDownloadReferencedSchemas());
			validators = createValidators(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath());
		}
	}

	/**
	 * Creates a JSON schema from an already parsed schema object using an existing dependency
	 * resolver, e.g. for a referenced sub schema.
	 *
	 * @param jsonSchemaDefinitionObject
	 *            the schema object
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings
	 * @throws JsonSchemaDefinitionError
	 *             if the schema is null or invalid
	 * @throws DuplicateKeyException
	 *             if the schema contains duplicate keys
	 */
	public JsonSchema(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver) throws JsonSchemaDefinitionError, DuplicateKeyException {
		if (jsonSchemaDefinitionObject == null) {
			throw new JsonSchemaDefinitionError("Contains null data", null);
		} else {
			this.jsonSchemaDependencyResolver = jsonSchemaDependencyResolver;
			validators = createValidators(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath());
		}
	}

	/**
	 * Reads the descriptive top level keywords ("id", "$id", "$schema", "$comment", "title",
	 * "description") and creates the dependency resolver.
	 *
	 * @param jsonSchemaDefinitionObject
	 *            the schema object
	 * @param jsonSchemaConfiguration
	 *            the configuration, its JSON schema version is set from "$schema" if not defined yet
	 * @param dependencies
	 *            additional JSON schemas referenced by this schema via "$ref"
	 * @throws IllegalArgumentException
	 *             if the configuration is null
	 * @throws Exception
	 *             if a keyword has an invalid value or is defined twice
	 */
	private void readSchemaData(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaConfiguration jsonSchemaConfiguration, final JsonSchemaDependency... dependencies) throws Exception {
		if (jsonSchemaDefinitionObject == null) {
			throw new JsonSchemaDefinitionError("Contains null data", null);
		}

		if (jsonSchemaConfiguration == null) {
			throw new IllegalArgumentException("JSON schema configuration must not be null");
		}
		maximumValidationErrors = jsonSchemaConfiguration.getMaximumValidationErrors();

		if (jsonSchemaDefinitionObject.containsKey("id")) {
			if (jsonSchemaDefinitionObject.getSimpleValue("id") == null) {
				throw new JsonSchemaDefinitionError("Invalid data type 'null' for key 'id'", new JsonSchemaPath());
			} else if (!(jsonSchemaDefinitionObject.getSimpleValue("id") instanceof String)) {
				throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinitionObject.getSimpleValue("id").getClass().getSimpleName() + "' for key 'id'", new JsonSchemaPath());
			} else if (id != null) {
				throw new JsonSchemaDefinitionError("Invalid duplicate definition for JSON schema id by key 'id'", new JsonSchemaPath());
			} else {
				id = (String) jsonSchemaDefinitionObject.getSimpleValue("id");
			}
		}

		if (jsonSchemaDefinitionObject.containsKey("$id")) {
			if (jsonSchemaDefinitionObject.getSimpleValue("$id") == null) {
				throw new JsonSchemaDefinitionError("Invalid data type 'null' for key '$id'", new JsonSchemaPath());
			} else if (!(jsonSchemaDefinitionObject.getSimpleValue("$id") instanceof String)) {
				throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinitionObject.getSimpleValue("$id").getClass().getSimpleName() + "' for key '$id'", new JsonSchemaPath());
			} else if (id != null) {
				throw new JsonSchemaDefinitionError("Invalid duplicate definition for JSON schema id by key '$id'", new JsonSchemaPath());
			} else {
				id = (String) jsonSchemaDefinitionObject.getSimpleValue("$id");
			}
		}

		if (jsonSchemaDefinitionObject.containsKey("$schema")) {
			if (jsonSchemaDefinitionObject.getSimpleValue("$schema") == null) {
				throw new JsonSchemaDefinitionError("Invalid data type 'null' for key '$schema'", new JsonSchemaPath());
			} else if (!(jsonSchemaDefinitionObject.getSimpleValue("$schema") instanceof String)) {
				throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinitionObject.getSimpleValue("$schema").getClass().getSimpleName() + "' for key '$schema'", new JsonSchemaPath());
			} else if (schemaVersionUrl != null) {
				throw new JsonSchemaDefinitionError("Invalid duplicate definition for JSON schema version url by key '$schema'", new JsonSchemaPath());
			} else {
				schemaVersionUrl = (String) jsonSchemaDefinitionObject.getSimpleValue("$schema");
				if (jsonSchemaConfiguration.getJsonSchemaVersion() == null) {
					jsonSchemaConfiguration.setJsonSchemaVersion(JsonSchemaVersion.getJsonSchemaVersionByVersionUrl(schemaVersionUrl));
				}
			}
		}

		if (jsonSchemaDefinitionObject.containsKey("$comment")) {
			if (jsonSchemaDefinitionObject.getSimpleValue("$comment") == null) {
				throw new JsonSchemaDefinitionError("Invalid data type 'null' for key '$comment'", new JsonSchemaPath());
			} else if (!(jsonSchemaDefinitionObject.getSimpleValue("$comment") instanceof String)) {
				throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinitionObject.getSimpleValue("$comment").getClass().getSimpleName() + "' for key '$comment'", new JsonSchemaPath());
			} else if (comment != null) {
				throw new JsonSchemaDefinitionError("Invalid duplicate definition for JSON schema comment by key '$comment'", new JsonSchemaPath());
			} else {
				comment = (String) jsonSchemaDefinitionObject.getSimpleValue("$comment");
			}
		}

		if (jsonSchemaDefinitionObject.containsKey("title")) {
			if (jsonSchemaDefinitionObject.getSimpleValue("title") == null) {
				throw new JsonSchemaDefinitionError("Invalid data type 'null' for key 'title'", new JsonSchemaPath());
			} else if (!(jsonSchemaDefinitionObject.getSimpleValue("title") instanceof String)) {
				throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinitionObject.getSimpleValue("title").getClass().getSimpleName() + "' for key 'title'", new JsonSchemaPath());
			} else if (title != null) {
				throw new JsonSchemaDefinitionError("Invalid duplicate definition for JSON schema title url by key 'title'", new JsonSchemaPath());
			} else {
				title = (String) jsonSchemaDefinitionObject.getSimpleValue("title");
			}
		}

		if (jsonSchemaDefinitionObject.containsKey("description")) {
			if (jsonSchemaDefinitionObject.getSimpleValue("description") == null) {
				throw new JsonSchemaDefinitionError("Invalid data type 'null' for key 'description'", new JsonSchemaPath());
			} else if (!(jsonSchemaDefinitionObject.getSimpleValue("description") instanceof String)) {
				throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinitionObject.getSimpleValue("description").getClass().getSimpleName() + "' for key 'description'", new JsonSchemaPath());
			} else if (description != null) {
				throw new JsonSchemaDefinitionError("Invalid duplicate definition for JSON schema description by key 'description'", new JsonSchemaPath());
			} else {
				description = (String) jsonSchemaDefinitionObject.getSimpleValue("description");
			}
		}

		jsonSchemaDependencyResolver = new JsonSchemaDependencyResolver(jsonSchemaDefinitionObject, dependencies);
		jsonSchemaDependencyResolver.setFormatAssertion(jsonSchemaConfiguration.getFormatAssertion());
		if (schemaVersionUrl != null) {
			// Detects the version of custom meta schemas and the vocabularies they declare
			final JsonSchemaVersion metaSchemaVersion = jsonSchemaDependencyResolver.applyMetaSchema(schemaVersionUrl);
			if (jsonSchemaConfiguration.getJsonSchemaVersion() == null) {
				jsonSchemaConfiguration.setJsonSchemaVersion(metaSchemaVersion);
			}
		}
	}

	/**
	 * Returns the "id" (up to draft 4) or "$id" (since draft 6) of this schema.
	 *
	 * @return the value, or null if not defined
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the "$schema" URL describing the JSON schema version this schema was written for.
	 *
	 * @return the value, or null if not defined
	 */
	public String getSchemaVersionUrl() {
		return schemaVersionUrl;
	}

	/**
	 * Returns the "$comment" of this schema.
	 *
	 * @return the value, or null if not defined
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Returns the "title" of this schema.
	 *
	 * @return the value, or null if not defined
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Returns the "description" of this schema.
	 *
	 * @return the value, or null if not defined
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Reads JSON data (JSON5 syntax allowed) and validates it against this schema.
	 *
	 * @param jsonDataInputStream
	 *            the stream to read the JSON data from
	 * @param encoding
	 *            the encoding of the JSON data
	 * @return the JSON data read
	 * @throws JsonSchemaDataValidationError
	 *             if the data cannot be read or is not valid against this schema
	 */
	public JsonNode validate(final InputStream jsonDataInputStream, final Charset encoding) throws JsonSchemaDataValidationError {
		JsonNode jsonDataNode;
		try (JsonReader jsonReader = new Json5Reader(jsonDataInputStream, encoding)) {
			jsonDataNode = jsonReader.read();
		} catch (final Exception e) {
			throw new JsonSchemaDataValidationError("Cannot read JSON data: " + e.getMessage(), new JsonPath(), e);
		}

		BaseJsonSchemaValidator.applyValidators(validators, jsonDataNode, new JsonPath());
		return jsonDataNode;
	}

	/**
	 * Reads JSON data (JSON5 syntax allowed) in UTF-8 encoding and validates it against this schema.
	 *
	 * @param jsonDataInputStream
	 *            the stream to read the JSON data from
	 * @return the JSON data read
	 * @throws JsonSchemaDataValidationError
	 *             if the data cannot be read or is not valid against this schema
	 */
	public JsonNode validate(final InputStream jsonDataInputStream) throws JsonSchemaDataValidationError {
		JsonNode jsonDataNode;
		try (JsonReader jsonReader = new Json5Reader(jsonDataInputStream)) {
			jsonDataNode = jsonReader.read();
		} catch (final Exception e) {
			throw new JsonSchemaDataValidationError("Cannot read JSON data: " + e.getMessage(), new JsonPath(), e);
		}

		BaseJsonSchemaValidator.applyValidators(validators, jsonDataNode, new JsonPath());
		return jsonDataNode;
	}

	/**
	 * Validates already parsed JSON data against this schema. The data node is marked as root node.
	 *
	 * @param jsonData
	 *            the JSON data
	 * @throws JsonSchemaDataValidationError
	 *             if the data is not valid against this schema
	 */
	public void validate(final JsonNode jsonData) throws JsonSchemaDataValidationError {
		JsonNode jsonDataNode;
		try {
			jsonDataNode = jsonData.withRootNode(true);
		} catch (final Exception e) {
			throw new JsonSchemaDataValidationError(e.getMessage(), new JsonPath(), e);
		}

		BaseJsonSchemaValidator.applyValidators(validators, jsonDataNode, new JsonPath());
	}

	/**
	 * Validates already parsed JSON data against this schema and collects all errors instead of
	 * stopping at the first one, up to the maximum configured by
	 * {@link JsonSchemaConfiguration#setMaximumValidationErrors(int)}.
	 * <p>
	 * The failing options of "anyOf" and "oneOf" are reported as one error, whose causes are the
	 * errors of the options. Errors that only follow from a wrong data type of a node (e.g. missing
	 * properties of a string) are not reported. Use {@link #validate(JsonNode)} for a quick check
	 * whether the data is valid.
	 * </p>
	 *
	 * @param jsonData
	 *            the JSON data
	 * @return the result with all errors found
	 */
	public JsonSchemaValidationResult validateAll(final JsonNode jsonData) {
		final JsonSchemaValidationCollector collector = JsonSchemaValidationCollector.start(maximumValidationErrors);
		try {
			JsonNode jsonDataNode;
			try {
				jsonDataNode = jsonData.withRootNode(true);
			} catch (final Exception e) {
				collector.addError(new JsonSchemaDataValidationError(e.getMessage(), new JsonPath(), e));
				return collector.getResult();
			}
			try {
				BaseJsonSchemaValidator.applyValidators(validators, jsonDataNode, new JsonPath());
			} catch (final JsonSchemaDataValidationError e) {
				// Only thrown for errors outside of collecting, e.g. by a validator of an unexpected structure
				if (!e.isCollectedMarker()) {
					collector.addError(e);
				}
			}
			return collector.getResult();
		} finally {
			JsonSchemaValidationCollector.stop();
		}
	}

	/**
	 * Reads JSON data (JSON5 syntax allowed) and validates it against this schema, collecting all
	 * errors, see {@link #validateAll(JsonNode)}.
	 *
	 * @param jsonDataInputStream
	 *            the stream to read the JSON data from
	 * @param encoding
	 *            the encoding of the JSON data
	 * @return the result with all errors found
	 * @throws JsonSchemaDataValidationError
	 *             if the data cannot be read
	 */
	public JsonSchemaValidationResult validateAll(final InputStream jsonDataInputStream, final Charset encoding) throws JsonSchemaDataValidationError {
		JsonNode jsonDataNode;
		try (JsonReader jsonReader = new Json5Reader(jsonDataInputStream, encoding)) {
			jsonDataNode = jsonReader.read();
		} catch (final Exception e) {
			throw new JsonSchemaDataValidationError("Cannot read JSON data: " + e.getMessage(), new JsonPath(), e);
		}
		return validateAll(jsonDataNode);
	}

	/**
	 * Returns the maximum number of errors reported by {@link #validateAll(JsonNode)}.
	 *
	 * @return the maximum number of errors
	 */
	public int getMaximumValidationErrors() {
		return maximumValidationErrors;
	}

	/**
	 * Sets the maximum number of errors reported by {@link #validateAll(JsonNode)}. The validation
	 * stops after this number of errors. Default is
	 * {@link JsonSchemaConfiguration#DEFAULT_MAXIMUM_VALIDATION_ERRORS} or the value of the
	 * configuration used to create this schema.
	 *
	 * @param maximumValidationErrors
	 *            the maximum number of errors, at least 1
	 */
	public void setMaximumValidationErrors(final int maximumValidationErrors) {
		if (maximumValidationErrors < 1) {
			throw new IllegalArgumentException("Maximum number of validation errors must be at least 1");
		}
		this.maximumValidationErrors = maximumValidationErrors;
	}

	/**
	 * Sets the maximum number of errors reported by {@link #validateAll(JsonNode)}, see
	 * {@link #setMaximumValidationErrors(int)}.
	 *
	 * @param newMaximumValidationErrors
	 *            the maximum number of errors, at least 1
	 * @return this schema for chaining
	 */
	public JsonSchema withMaximumValidationErrors(final int newMaximumValidationErrors) {
		setMaximumValidationErrors(newMaximumValidationErrors);
		return this;
	}

	/**
	 * Creates the validators for all keywords of a schema object. Descriptive keywords like "title"
	 * are only checked; unknown keywords are ignored, except in simple mode.<br />
	 * <br />
	 * For JSON schema draft 2019-09 and newer the returned list contains a single
	 * {@link SchemaObjectValidator}, which applies the keyword validators and collects their
	 * annotations.
	 *
	 * @param jsonSchemaDefinitionObject
	 *            the schema object
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings
	 * @param currentJsonSchemaPath
	 *            the path of the schema object within the JSON schema
	 * @return the validators
	 * @throws JsonSchemaDefinitionError
	 *             if a keyword has an invalid value
	 * @throws DuplicateKeyException
	 *             if a sub schema contains duplicate keys
	 */
	public static List<BaseJsonSchemaValidator> createValidators(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath currentJsonSchemaPath) throws JsonSchemaDefinitionError, DuplicateKeyException {
		if (jsonSchemaDependencyResolver != null && jsonSchemaDependencyResolver.isVersionAtLeast(JsonSchemaVersion.draft2019_09)) {
			final List<BaseJsonSchemaValidator> keywordValidators = createValidatorsDraft2019_09AndNewer(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, currentJsonSchemaPath);
			final List<BaseJsonSchemaValidator> validators = new ArrayList<>();
			validators.add(new SchemaObjectValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath, jsonSchemaDefinitionObject, keywordValidators, jsonSchemaDependencyResolver.getBaseUri(jsonSchemaDefinitionObject)));
			return validators;
		} else if (jsonSchemaDependencyResolver != null && jsonSchemaDefinitionObject.containsKey("$ref") && !jsonSchemaDependencyResolver.isSimpleMode()) {
			// Up to draft 7 "$ref" overrides all sibling keywords, they are ignored
			final List<BaseJsonSchemaValidator> validators = new ArrayList<>();
			validators.add(createReferenceValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, currentJsonSchemaPath, jsonSchemaDefinitionObject.get("$ref")));
			return validators;
		} else {
			return createValidatorsUpToDraftV7(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, currentJsonSchemaPath);
		}
	}

	/**
	 * Creates the validators for a schema node, which may be a schema object or a boolean schema.
	 *
	 * @param jsonSchemaNode
	 *            the schema object or boolean
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings
	 * @param currentJsonSchemaPath
	 *            the path of the schema within the JSON schema
	 * @return the validators
	 * @throws JsonSchemaDefinitionError
	 *             if the node is neither object nor boolean, or a keyword has an invalid value
	 * @throws DuplicateKeyException
	 *             if a sub schema contains duplicate keys
	 */
	public static List<BaseJsonSchemaValidator> createValidatorsForSchemaNode(final JsonNode jsonSchemaNode, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath currentJsonSchemaPath) throws JsonSchemaDefinitionError, DuplicateKeyException {
		if (jsonSchemaNode == null || jsonSchemaNode.isNull()) {
			throw new JsonSchemaDefinitionError("JSON schema is 'null'", currentJsonSchemaPath);
		} else if (jsonSchemaNode.isBoolean()) {
			final List<BaseJsonSchemaValidator> validators = new ArrayList<>();
			validators.add(new BooleanValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath, jsonSchemaNode));
			return validators;
		} else if (jsonSchemaNode.isJsonObject()) {
			return createValidators((JsonObject) jsonSchemaNode, jsonSchemaDependencyResolver, currentJsonSchemaPath);
		} else {
			throw new JsonSchemaDefinitionError("JSON schema is neither 'object' nor 'boolean' but '" + jsonSchemaNode.getJsonDataType().getName() + "'", currentJsonSchemaPath);
		}
	}

	/**
	 * Creates the validator for a "$ref" keyword. The reference is resolved against the base URI
	 * of the schema object containing it.
	 *
	 * @param jsonSchemaDefinitionObject
	 *            the schema object containing "$ref"
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings
	 * @param currentJsonSchemaPath
	 *            the path of the schema object within the JSON schema
	 * @param referenceNode
	 *            the value of "$ref"
	 * @return the validator
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a string or cannot be resolved
	 */
	private static ReferenceValidator createReferenceValidator(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath currentJsonSchemaPath, final JsonNode referenceNode) throws JsonSchemaDefinitionError {
		if (referenceNode == null || !referenceNode.isString()) {
			throw new JsonSchemaDefinitionError("Invalid data type for key '$ref'. String expected", currentJsonSchemaPath);
		}
		return new ReferenceValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey("$ref"), referenceNode, jsonSchemaDependencyResolver.getBaseUri(jsonSchemaDefinitionObject));
	}

	/**
	 * Creates the keyword validators of a schema object for JSON schema draft 2019-09 and newer.
	 * Unknown keywords are ignored. "unevaluatedProperties" and "unevaluatedItems" are placed last,
	 * because they depend on the annotations of all other keywords.
	 *
	 * @param jsonSchemaDefinitionObject
	 *            the schema object
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings
	 * @param currentJsonSchemaPath
	 *            the path of the schema object within the JSON schema
	 * @return the keyword validators
	 * @throws JsonSchemaDefinitionError
	 *             if a keyword has an invalid value
	 * @throws DuplicateKeyException
	 *             if a sub schema contains duplicate keys
	 */
	private static List<BaseJsonSchemaValidator> createValidatorsDraft2019_09AndNewer(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath currentJsonSchemaPath) throws JsonSchemaDefinitionError, DuplicateKeyException {
		final boolean draft2020_12 = jsonSchemaDependencyResolver.isVersionAtLeast(JsonSchemaVersion.draft2020_12);
		final boolean validationVocabulary = jsonSchemaDependencyResolver.isValidationVocabularyActive();
		final List<BaseJsonSchemaValidator> validators = new ArrayList<>();
		final List<BaseJsonSchemaValidator> unevaluatedValidators = new ArrayList<>();

		for (final Entry<String, JsonNode> entry : jsonSchemaDefinitionObject.entrySet()) {
			final JsonSchemaPath keywordPath = new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey());
			switch (entry.getKey()) {
				// Core vocabulary
				case "$ref":
					validators.add(createReferenceValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, currentJsonSchemaPath, entry.getValue()));
					break;
				case "$dynamicRef":
					if (draft2020_12) {
						validators.add(createDynamicReferenceValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, keywordPath, entry.getValue(), false));
					}
					break;
				case "$recursiveRef":
					if (!draft2020_12) {
						validators.add(createDynamicReferenceValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, keywordPath, entry.getValue(), true));
					}
					break;
				case "$id":
				case "$schema":
				case "$anchor":
				case "$dynamicAnchor":
				case "$recursiveAnchor":
				case "$vocabulary":
				case "$comment":
				case "$defs":
				case "definitions":
					// Identifiers and definitions are handled by the resource index of the dependency resolver
					break;

				// Applicator vocabulary
				case "allOf":
					validators.add(new AllOfValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					break;
				case "anyOf":
					validators.add(new AnyOfValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					break;
				case "oneOf":
					validators.add(new OneOfValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					break;
				case "not":
					validators.add(new NotValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					break;
				case "if":
					validators.add(new IfThenElseValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath, entry.getValue(), jsonSchemaDefinitionObject.get("then"), jsonSchemaDefinitionObject.get("else")));
					break;
				case "then":
				case "else":
					// Only relevant together with "if"
					break;
				case "dependentSchemas":
					validators.add(new DependentSchemasValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					break;
				case "dependencies":
					// Removed in draft 2019-09 ("dependentSchemas" and "dependentRequired"), still applied for compatibility
					validators.add(new DependenciesValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					break;
				case "prefixItems":
					if (draft2020_12) {
						validators.add(new PrefixItemsValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "items":
					validators.add(new ItemsValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					break;
				case "additionalItems":
					// Draft 2019-09: validated by ItemsValidator, if "items" is an array. Draft 2020-12: no keyword anymore
					break;
				case "contains":
					validators.add(new ContainsValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					break;
				case "minContains":
				case "maxContains":
					// Validated by ContainsValidator, no effect without "contains"
					break;
				case "properties":
					validators.add(new PropertiesValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					break;
				case "patternProperties":
					validators.add(new PatternPropertiesValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					break;
				case "additionalProperties":
					validators.add(new AdditionalPropertiesValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					break;
				case "propertyNames":
					validators.add(new PropertyNamesValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					break;

				// Unevaluated vocabulary
				case "unevaluatedProperties":
					unevaluatedValidators.add(new UnevaluatedPropertiesValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					break;
				case "unevaluatedItems":
					unevaluatedValidators.add(new UnevaluatedItemsValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					break;

				// Validation vocabulary
				case "type":
					if (validationVocabulary) {
						validators.add(new TypeValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "enum":
					if (validationVocabulary) {
						validators.add(new EnumValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "const":
					if (validationVocabulary) {
						validators.add(new ConstValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "multipleOf":
					if (validationVocabulary) {
						validators.add(new MultipleOfValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "maximum":
					if (validationVocabulary) {
						validators.add(new MaximumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "exclusiveMaximum":
					if (validationVocabulary) {
						validators.add(new ExclusiveMaximumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "minimum":
					if (validationVocabulary) {
						validators.add(new MinimumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "exclusiveMinimum":
					if (validationVocabulary) {
						validators.add(new ExclusiveMinimumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "maxLength":
					if (validationVocabulary) {
						validators.add(new MaxLengthValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "minLength":
					if (validationVocabulary) {
						validators.add(new MinLengthValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "pattern":
					if (validationVocabulary) {
						validators.add(new PatternValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "maxItems":
					if (validationVocabulary) {
						validators.add(new MaxItemsValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "minItems":
					if (validationVocabulary) {
						validators.add(new MinItemsValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "uniqueItems":
					if (validationVocabulary) {
						validators.add(new UniqueItemsValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "maxProperties":
					if (validationVocabulary) {
						validators.add(new MaxPropertiesValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "minProperties":
					if (validationVocabulary) {
						validators.add(new MinPropertiesValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "required":
					if (validationVocabulary) {
						validators.add(new RequiredValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;
				case "dependentRequired":
					if (validationVocabulary) {
						validators.add(new DependentRequiredValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;

				// Format vocabulary: annotation only, unless format assertion is configured or declared by the meta schema
				case "format":
					if (jsonSchemaDependencyResolver.isFormatAssertion()) {
						validators.add(new FormatValidator(jsonSchemaDependencyResolver, keywordPath, entry.getValue()));
					}
					break;

				// Content and meta-data vocabularies: annotations only
				case "contentMediaType":
				case "contentEncoding":
				case "contentSchema":
				case "title":
				case "description":
				case "default":
				case "deprecated":
				case "readOnly":
				case "writeOnly":
				case "examples":
					break;

				default:
					// Unknown keywords are ignored
					break;
			}
		}

		validators.addAll(unevaluatedValidators);
		return validators;
	}

	/**
	 * Creates the validator for a "$dynamicRef" or "$recursiveRef" keyword.
	 *
	 * @param jsonSchemaDefinitionObject
	 *            the schema object containing the keyword
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings
	 * @param keywordPath
	 *            the path of the keyword within the JSON schema
	 * @param referenceNode
	 *            the value of the keyword
	 * @param recursiveReference
	 *            true for "$recursiveRef", false for "$dynamicRef"
	 * @return the validator
	 * @throws JsonSchemaDefinitionError
	 *             if the value is invalid
	 */
	private static DynamicReferenceValidator createDynamicReferenceValidator(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath keywordPath, final JsonNode referenceNode, final boolean recursiveReference) throws JsonSchemaDefinitionError {
		if (referenceNode == null || !referenceNode.isString()) {
			throw new JsonSchemaDefinitionError("Invalid data type for dynamic reference. String expected", keywordPath);
		}
		return new DynamicReferenceValidator(jsonSchemaDependencyResolver, keywordPath, referenceNode, jsonSchemaDependencyResolver.getBaseUri(jsonSchemaDefinitionObject), recursiveReference);
	}

	/**
	 * Creates the validators for all keywords of a schema object for JSON schema versions up to
	 * draft 7 and simple mode.
	 *
	 * @param jsonSchemaDefinitionObject
	 *            the schema object
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings
	 * @param currentJsonSchemaPath
	 *            the path of the schema object within the JSON schema
	 * @return the validators
	 * @throws JsonSchemaDefinitionError
	 *             if a keyword has an invalid value
	 * @throws DuplicateKeyException
	 *             if a sub schema contains duplicate keys
	 */
	private static List<BaseJsonSchemaValidator> createValidatorsUpToDraftV7(final JsonObject jsonSchemaDefinitionObject, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath currentJsonSchemaPath) throws JsonSchemaDefinitionError, DuplicateKeyException {
		final List<BaseJsonSchemaValidator> validators = new ArrayList<>();

		JsonNode ifJsonObject = null;
		JsonNode thenObject = null;
		JsonNode elseObject = null;

		for (final Entry<String, JsonNode> entry : jsonSchemaDefinitionObject.entrySet()) {
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
					// Validated by default up to draft 7, unless switched off by the configuration
					if (jsonSchemaDependencyResolver.isFormatAssertion()) {
						validators.add(new FormatValidator(jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					}
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
					if (!jsonSchemaDependencyResolver.isVersionAtLeast(JsonSchemaVersion.draftV6)
							&& !jsonSchemaDefinitionObject.containsKey("minimum")) {
						throw new JsonSchemaDefinitionError("Missing 'minimum' rule for 'exclusiveMinimum'", currentJsonSchemaPath);
					} else if (jsonSchemaDependencyResolver.isVersionAtLeast(JsonSchemaVersion.draftV6)) {
						validators.add(new ExclusiveMinimumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, new JsonSchemaPath(currentJsonSchemaPath).addPropertyKey(entry.getKey()), entry.getValue()));
					}
					break;
				case "exclusiveMaximum":
					// Do nothing, because this is validated by MaximumValidator, too
					// Value must be of boolean type and "maximum" value item must exist in simpleMode and draft v3/v4.
					if (!jsonSchemaDependencyResolver.isVersionAtLeast(JsonSchemaVersion.draftV6)
							&& !jsonSchemaDefinitionObject.containsKey("maximum")) {
						throw new JsonSchemaDefinitionError("Missing 'maximum' rule for 'exclusiveMaximum'", currentJsonSchemaPath);
					} else if (jsonSchemaDependencyResolver.isVersionAtLeast(JsonSchemaVersion.draftV6)) {
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
					validators.add(createReferenceValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, currentJsonSchemaPath, entry.getValue()));
					break;

				case "id":
					if (jsonSchemaDependencyResolver.isVersionAtLeast(JsonSchemaVersion.draftV6)) {
						throw new JsonSchemaDefinitionError("JSON schema 'id' on top level of JSON schema is only allowed for JSON schema versions draft v4 and lower", currentJsonSchemaPath);
					}
					// "id" may change the base URI of sub schemas, only simple mode restricts it to the top level
					if (jsonSchemaDependencyResolver.isSimpleMode() && !currentJsonSchemaPath.isRoot()) {
						throw new JsonSchemaDefinitionError("JSON schema 'id' must be defined on top level of JSON schema", currentJsonSchemaPath);
					}
					break;
				case "$id":
					if (!jsonSchemaDependencyResolver.isVersionAtLeast(JsonSchemaVersion.draftV6)) {
						throw new JsonSchemaDefinitionError("JSON schema '$id' on top level of JSON schema is only allowed for JSON schema versions draft v6 and higher", currentJsonSchemaPath);
					}
					// "$id" may change the base URI of sub schemas, only simple mode restricts it to the top level
					if (jsonSchemaDependencyResolver.isSimpleMode() && !currentJsonSchemaPath.isRoot()) {
						throw new JsonSchemaDefinitionError("JSON schema '$id' must be defined on top level of JSON schema", currentJsonSchemaPath);
					}
					break;
				case "$schema":
					// $schema should be a descriptive url
					if (jsonSchemaDependencyResolver.isSimpleMode() && !currentJsonSchemaPath.isRoot()) {
						throw new JsonSchemaDefinitionError("JSON schema '$schema' must be defined on top level of JSON schema", currentJsonSchemaPath);
					}
					break;
				case "definitions":
					// Defined JSON schema definitions
					if (jsonSchemaDependencyResolver.isSimpleMode() && !currentJsonSchemaPath.isRoot()) {
						throw new JsonSchemaDefinitionError("JSON schema 'definitions' must be defined on top level of JSON schema", currentJsonSchemaPath);
					}
					break;

				case "title":
					// Descriptive title
					if (!(entry.getValue() instanceof JsonValueString)) {
						throw new JsonSchemaDefinitionError("Invalid data type '" + entry.getValue().getClass().getSimpleName() + "' for key 'title'", currentJsonSchemaPath);
					}
					break;
				case "description":
					// Descriptive comments
					if (!(entry.getValue() instanceof JsonValueString)) {
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
					} else if (entry.getValue() instanceof JsonValueBoolean) {
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
}