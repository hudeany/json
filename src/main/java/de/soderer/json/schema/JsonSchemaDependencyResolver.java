package de.soderer.json.schema;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.soderer.json.Json5Reader;
import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.path.JsonPathArrayElement;
import de.soderer.json.path.JsonPathElement;
import de.soderer.json.path.JsonPathPropertyElement;
import de.soderer.json.path.JsonPathRoot;
import de.soderer.json.schema.validator.BaseJsonSchemaValidator;
import de.soderer.json.utilities.Utilities;

/**
 * Resolves "$ref" references of a JSON schema and holds the settings used while creating and
 * applying its validators (JSON schema version, download permission).
 * <p>
 * References may point into the schema itself ("#/definitions/x"), into additional schemas given as
 * {@link JsonSchemaDependency} ("other.json#/definitions/x"), or, if allowed, into schemas
 * downloaded by http(s) URL. The resolver also caches the validators built per reference and
 * detects cyclic references.
 * </p>
 * <p>
 * References are resolved by URI according to the JSON schema standard: every schema object has
 * a base URI (changed by "$id", or "id" up to draft 4), and the reference is resolved against it.
 * Plain name anchors ("$anchor", "$dynamicAnchor", "$id": "#name") and JSON pointer fragments are
 * supported. The meta schemas of all supported JSON schema versions are available offline from
 * classpath resources. If a reference cannot be resolved this way, the former lookup rules of this
 * library are used as fallback (e.g. plain definition names like "$ref": "address").
 * </p>
 * <p>
 * Thread safety: one resolver may be used by several threads validating data concurrently. All
 * access to the schema documents and the resource index is synchronized on the resolver, the
 * runtime state of a validation is kept per thread. Downloads of external schemas also happen
 * while holding the lock, so other threads wait for them. Settings (e.g.
 * {@link #setFormatAssertion(Boolean)}, {@link #setRegexStepLimit(long)}) are configuration and
 * must not be changed while other threads are validating.
 * </p>
 */
public class JsonSchemaDependencyResolver {
	/**
	 * Classpath resources of the bundled meta schemas by their normalized URI ("http" scheme, no
	 * trailing "#"), so that references to meta schemas do not need any download.
	 */
	private static final Map<String, String> BUNDLED_META_SCHEMAS = new HashMap<>();
	static {
		BUNDLED_META_SCHEMAS.put("http://json-schema.org/draft-03/schema", JsonSchemaVersion.draftV3.getLocalFile());
		BUNDLED_META_SCHEMAS.put("http://json-schema.org/draft-04/schema", JsonSchemaVersion.draftV4.getLocalFile());
		BUNDLED_META_SCHEMAS.put("http://json-schema.org/draft-06/schema", JsonSchemaVersion.draftV6.getLocalFile());
		BUNDLED_META_SCHEMAS.put("http://json-schema.org/draft-07/schema", JsonSchemaVersion.draftV7.getLocalFile());
		BUNDLED_META_SCHEMAS.put("http://json-schema.org/draft/2019-09/schema", JsonSchemaVersion.draft2019_09.getLocalFile());
		for (final String vocabulary : new String[] { "core", "applicator", "validation", "meta-data", "format", "content" }) {
			BUNDLED_META_SCHEMAS.put("http://json-schema.org/draft/2019-09/meta/" + vocabulary, "json/JsonSchemaDescriptionDraft2019-09/meta/" + vocabulary + ".json");
		}
		BUNDLED_META_SCHEMAS.put("http://json-schema.org/draft/2020-12/schema", JsonSchemaVersion.draft2020_12.getLocalFile());
		for (final String vocabulary : new String[] { "core", "applicator", "unevaluated", "validation", "meta-data", "format-annotation", "format-assertion", "content" }) {
			BUNDLED_META_SCHEMAS.put("http://json-schema.org/draft/2020-12/meta/" + vocabulary, "json/JsonSchemaDescriptionDraft2020-12/meta/" + vocabulary + ".json");
		}
	}

	/** URI of the validation vocabulary of draft 2019-09. */
	private static final String VOCABULARY_2019_09_VALIDATION = "https://json-schema.org/draft/2019-09/vocab/validation";

	/** URI of the validation vocabulary of draft 2020-12. */
	private static final String VOCABULARY_2020_12_VALIDATION = "https://json-schema.org/draft/2020-12/vocab/validation";

	/** URI of the format assertion vocabulary of draft 2020-12. */
	private static final String VOCABULARY_2020_12_FORMAT_ASSERTION = "https://json-schema.org/draft/2020-12/vocab/format-assertion";

	/**
	 * Index of all known schema resources by URI, built on first use.
	 */
	private JsonSchemaResourceIndex resourceIndex = null;

	/**
	 * Initial base URI of the main schema document (before its own "$id").
	 */
	private String rootRetrievalUri = JsonSchemaUriUtilities.DEFAULT_ROOT_BASE_URI;

	/**
	 * Evaluation context without frames for JSON schema versions up to draft 7. No frames are ever
	 * entered in it, so all annotation calls are without effect.
	 */
	private static final JsonSchemaEvaluationContext INACTIVE_EVALUATION_CONTEXT = new JsonSchemaEvaluationContext();

	/**
	 * Runtime state of the current validation per thread (draft 2019-09 and newer).
	 */
	private final ThreadLocal<JsonSchemaEvaluationContext> evaluationContext = ThreadLocal.withInitial(JsonSchemaEvaluationContext::new);

	/**
	 * Whether "format" is validated: null to use the default of the JSON schema version (validated
	 * up to draft 7, annotation only since draft 2019-09).
	 */
	private Boolean formatAssertion = null;

	/**
	 * Whether the meta schema of the main schema declares the format assertion vocabulary.
	 */
	private boolean formatAssertionVocabulary = false;

	/**
	 * Whether the validation vocabulary is active (type, enum, minimum, required ...). It is only
	 * disabled by a custom meta schema whose "$vocabulary" does not contain it.
	 */
	private boolean validationVocabulary = true;
	/**
	 * The main schema document.
	 */
	private JsonObject schemaDocumentNode = null;
	/**
	 * Additional schema documents by their reference name.
	 */
	private final Map<String, JsonObject> additionalSchemaDocumentNodes = new HashMap<>();

	/**
	 * Cache of already built sub-validators per "$ref" reference key.<br />
	 * Without this cache {@link de.soderer.json.schema.validator.ReferenceValidator} would rebuild the whole
	 * sub-validator tree for a reference on every single data node it is applied to, which is both a severe
	 * performance problem and, in combination with recursive/self-referencing schemas, a stack-overflow risk.
	 */
	private final Map<String, List<BaseJsonSchemaValidator>> referenceValidatorCache = new ConcurrentHashMap<>();

	/**
	 * Tracks "$ref" resolutions that are currently in progress for the current data path (reference key + JSON data
	 * path, on the current thread), in order to detect genuinely cyclic schema references (a "$ref" chain that
	 * resolves back to itself without ever descending into a different part of the JSON data) without relying on
	 * catching {@link StackOverflowError}.
	 */
	private final ThreadLocal<Set<String>> activeReferenceResolutions = ThreadLocal.withInitial(HashSet::new);

	/**
	 * The resolver this one is a version view of, null for a main resolver. See
	 * {@link #getResolverForSchemaNode(JsonNode)}.
	 */
	private final JsonSchemaDependencyResolver mainResolver;

	/**
	 * Version views of this main resolver for schema documents of other JSON schema versions, created
	 * on first use.
	 */
	private final Map<JsonSchemaVersion, JsonSchemaDependencyResolver> versionViews = new ConcurrentHashMap<>();

	/**
	 * Returns the cached validators for a reference.
	 *
	 * @param referenceKey
	 *            the "$ref" value
	 * @return the validators, or null if not cached yet
	 */
	public List<BaseJsonSchemaValidator> getCachedReferenceValidators(final String referenceKey) {
		return referenceValidatorCache.get(referenceKey);
	}

	/**
	 * Caches the validators for a reference.
	 *
	 * @param referenceKey
	 *            the "$ref" value
	 * @param validators
	 *            the validators of the referenced schema
	 */
	public void putCachedReferenceValidators(final String referenceKey, final List<BaseJsonSchemaValidator> validators) {
		referenceValidatorCache.put(referenceKey, validators);
	}

	/**
	 * Marks the given reference as "currently being resolved" for the given data path.<br />
	 * Returns {@code true} if this exact combination was already active (i.e. a genuine cycle was
	 * detected), in which case the caller must NOT proceed with resolving/validating it again.
	 *
	 * @param referenceKey
	 *            the "$ref" value
	 * @param jsonPathString
	 *            the path of the JSON data node
	 * @return true, if a cycle was detected
	 */
	public boolean enterReferenceResolution(final String referenceKey, final String jsonPathString) {
		return !activeReferenceResolutions.get().add(referenceKey + " @ " + jsonPathString);
	}

	/**
	 * Marks the given reference as resolved for the given data path, counterpart of
	 * {@link #enterReferenceResolution(String, String)}.
	 *
	 * @param referenceKey
	 *            the "$ref" value
	 * @param jsonPathString
	 *            the path of the JSON data node
	 */
	public void exitReferenceResolution(final String referenceKey, final String jsonPathString) {
		final Set<String> active = activeReferenceResolutions.get();
		active.remove(referenceKey + " @ " + jsonPathString);
		if (active.isEmpty()) {
			// Avoid leaking ThreadLocal state on thread-pool threads once validation of a document is complete
			activeReferenceResolutions.remove();
		}
	}

	/**
	 * JSON schema version, null means simple mode.
	 * <p>
	 * The initial value draft v7 only applies to a resolver used on its own. {@link JsonSchema}
	 * always sets the version from its configuration or from "$schema", which is null and therefore
	 * simple mode if neither defines a version.
	 * </p>
	 */
	private JsonSchemaVersion jsonSchemaVersion = JsonSchemaVersion.draftV7;

	/**
	 * Whether external schemas referenced by URL may be downloaded.
	 */
	private boolean downloadReferencedSchemas = false;
	/**
	 * Whether a missing external schema is only reported when it is actually used.
	 */
	private boolean lazyFailOnMissingExternalSchemas = true;
	/**
	 * Base step limit for the evaluation of regular expressions against data.
	 */
	private long regexStepLimit = JsonSchemaRegexUtilities.DEFAULT_REGEX_STEP_LIMIT;

	/**
	 * Creates a resolver for a schema.
	 *
	 * @param schemaDocumentNode
	 *            the main schema document
	 * @param dependencies
	 *            additional schemas referenced by the main schema
	 * @throws Exception
	 *             if the schema is null or a dependency is invalid or added twice
	 */
	public JsonSchemaDependencyResolver(final JsonObject schemaDocumentNode, final JsonSchemaDependency... dependencies) throws Exception {
		if (schemaDocumentNode == null) {
			throw new JsonSchemaDefinitionError("Invalid data type 'null' for JsonSchemaDependencyResolver", new JsonSchemaPath());
		}
		this.schemaDocumentNode = schemaDocumentNode;
		mainResolver = null;
		for (final JsonSchemaDependency dependency : dependencies) {
			addJsonSchemaDefinition(dependency.getJsonSchemaReferenceName(), dependency.getJsonSchemaReferenceObject());
		}
	}

	/**
	 * Creates a version view of a main resolver, see {@link VersionView}.
	 *
	 * @param mainResolver
	 *            the main resolver
	 * @param jsonSchemaVersion
	 *            the JSON schema version of the view
	 */
	private JsonSchemaDependencyResolver(final JsonSchemaDependencyResolver mainResolver, final JsonSchemaVersion jsonSchemaVersion) {
		this.mainResolver = mainResolver;
		schemaDocumentNode = mainResolver.schemaDocumentNode;
		this.jsonSchemaVersion = jsonSchemaVersion;
	}

	/**
	 * Returns the resolver to build the validators of a schema object with. Schema objects of
	 * documents declaring another JSON schema version by "$schema" than the main schema (e.g. a
	 * referenced draft 7 schema within a draft 2020-12 schema) are evaluated with the version of
	 * their own document. For them a view of this resolver is returned, which shares all schema
	 * documents, caches, settings and the runtime state of the validation, but has the other JSON
	 * schema version.<br />
	 * <br />
	 * The main schema document is always evaluated with the configured version, and simple mode
	 * never switches to another version.
	 *
	 * @param schemaNode
	 *            a schema node of one of the known schema documents
	 * @return this resolver or a view of it for another JSON schema version
	 */
	public JsonSchemaDependencyResolver getResolverForSchemaNode(final JsonNode schemaNode) {
		final JsonSchemaDependencyResolver main = mainResolver != null ? mainResolver : this;
		if (schemaNode == null || main.isSimpleMode()) {
			return this;
		}

		final JsonSchemaVersion schemaVersion;
		synchronized (main) {
			schemaVersion = main.getResourceIndex().getSchemaVersion(schemaNode);
		}
		if (schemaVersion == null || schemaVersion == JsonSchemaVersion.simple || schemaVersion == jsonSchemaVersion) {
			return this;
		} else if (schemaVersion == main.jsonSchemaVersion) {
			return main;
		} else {
			return main.versionViews.computeIfAbsent(schemaVersion, version -> new VersionView(main, version));
		}
	}

	/**
	 * Returns the schema object a reference points to. A reference without "#" is looked up in
	 * "definitions" of the main schema.
	 *
	 * @param reference
	 *            the "$ref" value
	 * @param jsonSchemaPath
	 *            the path of the "$ref" keyword, used in error messages
	 * @return the referenced schema object
	 * @throws Exception
	 *             if the reference cannot be resolved, or an external schema is missing and may not be
	 *             downloaded
	 */
	public JsonObject getDependencyByReference(final String reference, final JsonSchemaPath jsonSchemaPath) throws Exception {
		if (reference != null) {
			// Standard resolution by URI relative to the main schema document
			final JsonNode resolvedNode = getSchemaNodeByUri(JsonSchemaUriUtilities.resolve(getBaseUri(schemaDocumentNode), reference));
			if (resolvedNode instanceof JsonObject) {
				return (JsonObject) resolvedNode;
			}
		}
		return getDependencyByLegacyReference(reference, jsonSchemaPath);
	}

	/**
	 * Returns the schema object a reference points to by the former lookup rules of this library:
	 * a reference without "#" is looked up in "definitions" of the main schema and the additional
	 * schemas, "#..." in the main schema, and "name#..." in the additional schema of that name.
	 *
	 * @param reference
	 *            the "$ref" value
	 * @param jsonSchemaPath
	 *            the path of the "$ref" keyword, used in error messages
	 * @return the referenced schema object
	 * @throws Exception
	 *             if the reference cannot be resolved, or an external schema is missing and may not be
	 *             downloaded
	 */
	public synchronized JsonObject getDependencyByLegacyReference(final String reference, final JsonSchemaPath jsonSchemaPath) throws Exception {
		if (reference != null) {
			if (!reference.contains("#")) {
				// Dereference simple reference without '#'
				if (schemaDocumentNode.get("definitions") != null && schemaDocumentNode.get("definitions") instanceof JsonObject && ((JsonObject) schemaDocumentNode.get("definitions")).containsKey(reference)) {
					final Object dereferencedValue = ((JsonObject) schemaDocumentNode.get("definitions")).get(reference);
					if (!(dereferencedValue instanceof JsonObject)) {
						throw new JsonSchemaDefinitionError("Invalid JSON schema reference data type for key '" + reference + "'. Expected 'object' but was '" + dereferencedValue.getClass().getSimpleName() + "'", jsonSchemaPath);
					} else {
						return (JsonObject) dereferencedValue;
					}
				} else {
					for (final JsonObject indirectJsonDefinitions : additionalSchemaDocumentNodes.values()) {
						if (indirectJsonDefinitions == null) {
							// Not yet resolved external schema (lazyFailOnMissingExternalSchemas placeholder), skip it
							continue;
						} else if (indirectJsonDefinitions.get("definitions") != null && indirectJsonDefinitions.get("definitions") instanceof JsonObject && ((JsonObject) indirectJsonDefinitions.get("definitions")).containsKey(reference)) {
							final Object dereferencedValue = ((JsonObject) indirectJsonDefinitions.get("definitions")).get(reference);
							if (!(dereferencedValue instanceof JsonObject)) {
								throw new JsonSchemaDefinitionError("Invalid JSON schema reference data type for key '" + reference + "'. Expected 'object' but was '" + dereferencedValue.getClass().getSimpleName() + "'", jsonSchemaPath);
							} else {
								return (JsonObject) dereferencedValue;
							}
						}
					}
					throw new Exception("Invalid JSON schema reference key '" + reference + "' or reference key not found. Use simple reference keys or this pattern for reference keys: '<referenced packagename or empty>#/definitions/<your reference key>'");
				}
			} else if (reference.startsWith("#")) {
				// Dereference local document reference
				final JsonPath jsonPath = new JsonPath(reference);
				Object referencedObject = schemaDocumentNode;
				for (final JsonPathElement jsonPathElement : jsonPath.getPathParts()) {
					if (jsonPathElement == null) {
						throw new JsonSchemaDefinitionError("Invalid JSON reference path contains null value", jsonSchemaPath);
					} else if (jsonPathElement instanceof JsonPathRoot) {
						// Skip
					} else if (referencedObject instanceof JsonObject) {
						if (!(jsonPathElement instanceof JsonPathPropertyElement)) {
							throw new JsonSchemaDefinitionError("Referenced JsonSchema does not contain the reference path '" + reference + "'", jsonSchemaPath);
						} else {
							final JsonObject referencedJsonObject = (JsonObject) referencedObject;
							if (!referencedJsonObject.containsKey(jsonPathElement.toString())) {
								throw new JsonSchemaDefinitionError("Referenced JsonSchema does not contain the reference path '" + reference + "'", jsonSchemaPath);
							} else if (referencedJsonObject.get(jsonPathElement.toString()) == null) {
								throw new JsonSchemaDefinitionError("Invalid data type 'null' for reference path '" + reference + "'", jsonSchemaPath);
							} else if ((referencedJsonObject.get(jsonPathElement.toString()) instanceof JsonObject)) {
								referencedObject = referencedJsonObject.get(jsonPathElement.toString());
							} else if ((referencedJsonObject.get(jsonPathElement.toString()) instanceof JsonArray)) {
								referencedObject = referencedJsonObject.get(jsonPathElement.toString());
							} else {
								throw new JsonSchemaDefinitionError("Invalid data type '" + referencedJsonObject.get(jsonPathElement.toString()).getClass().getSimpleName() + "' for reference path '" + reference + "'", jsonSchemaPath);
							}
						}
					} else if (referencedObject instanceof JsonArray) {
						final int referencedIndex;
						if (jsonPathElement instanceof JsonPathArrayElement) {
							referencedIndex = ((JsonPathArrayElement) jsonPathElement).getIndex();
						} else if (jsonPathElement instanceof JsonPathPropertyElement) {
							try {
								referencedIndex = Integer.parseInt(((JsonPathPropertyElement) jsonPathElement).toString());
							} catch (@SuppressWarnings("unused") final NumberFormatException e) {
								throw new JsonSchemaDefinitionError("Referenced JsonSchema does not contain the reference path '" + reference + "'", jsonSchemaPath);
							}
						} else {
							throw new JsonSchemaDefinitionError("Referenced JsonSchema does not contain the reference path '" + reference + "'", jsonSchemaPath);
						}

						final JsonArray referencedJsonArray = (JsonArray) referencedObject;
						if (referencedJsonArray.size() <= referencedIndex) {
							throw new JsonSchemaDefinitionError("Referenced JsonSchema does not contain the reference path '" + reference + "'", jsonSchemaPath);
						} else if (referencedJsonArray.get(referencedIndex) == null) {
							throw new JsonSchemaDefinitionError("Invalid data type 'null' for reference path '" + reference + "'", jsonSchemaPath);
						} else if ((referencedJsonArray.get(referencedIndex) instanceof JsonObject)) {
							referencedObject = referencedJsonArray.get(referencedIndex);
						} else if ((referencedJsonArray.get(referencedIndex) instanceof JsonArray)) {
							referencedObject = referencedJsonArray.get(referencedIndex);
						} else {
							throw new JsonSchemaDefinitionError("Invalid data type '" + referencedJsonArray.get(referencedIndex).getClass().getSimpleName() + "' for reference path '" + reference + "'", jsonSchemaPath);
						}
					}
				}
				return (JsonObject) referencedObject;
			} else {
				// Dereference other document reference
				String packageName = reference;
				if (packageName.contains("#")) {
					packageName = packageName.substring(0, packageName.lastIndexOf("#"));
				}

				if (!additionalSchemaDocumentNodes.containsKey(packageName) && packageName != null && packageName.toLowerCase().startsWith("http")) {
					if (downloadReferencedSchemas) {
						try {
							downloadSchemaData(packageName);
						} catch (final JsonSchemaDefinitionError e) {
							throw e;
						} catch (final Exception e) {
							throw new JsonSchemaDefinitionError("Cannot get content from '" + packageName + "'", jsonSchemaPath, e);
						}
					} else if (lazyFailOnMissingExternalSchemas){
						additionalSchemaDocumentNodes.put(packageName, null);
					} else {
						throw new Exception("Referenced JSON schema needs download: " + packageName);
					}
				}

				if (!additionalSchemaDocumentNodes.containsKey(packageName)) {
					throw new Exception("Unknown JSON schema reference package name '" + packageName + "'");
				} else if (additionalSchemaDocumentNodes.get(packageName) == null) {
					throw new Exception("Invalid empty JSON schema reference for package name '" + packageName + "'");
				} else {
					JsonObject referencedObject = additionalSchemaDocumentNodes.get(packageName);
					final JsonPath jsonPath = new JsonPath(reference.substring(reference.lastIndexOf("#")));
					for (final JsonPathElement jsonPathElement : jsonPath.getPathParts()) {
						if (jsonPathElement == null) {
							throw new JsonSchemaDefinitionError("Invalid JSON reference path contains null value", jsonSchemaPath);
						} else if (jsonPathElement instanceof JsonPathPropertyElement) {
							if (!referencedObject.containsKey(jsonPathElement.toString())) {
								throw new JsonSchemaDefinitionError("Referenced JsonSchema does not contain the reference path '" + reference + "'", jsonSchemaPath);
							} else if (referencedObject.get(jsonPathElement.toString()) == null) {
								throw new JsonSchemaDefinitionError("Invalid data type 'null' for reference path '" + reference + "'", jsonSchemaPath);
							} else if (!(referencedObject.get(jsonPathElement.toString()) instanceof JsonObject)) {
								throw new JsonSchemaDefinitionError("Invalid data type '" + referencedObject.get(jsonPathElement.toString()).getClass().getSimpleName() + "' for reference path '" + reference + "'", jsonSchemaPath);
							} else {
								referencedObject = (JsonObject) referencedObject.get(jsonPathElement.toString());
							}
						}
					}
					return referencedObject;
				}
			}
		} else {
			throw new Exception("Invalid JSON schema reference key 'null'");
		}
	}

	/**
	 * Maximum number of HTTP redirects followed when downloading a referenced schema.
	 */
	private static final int MAX_DOWNLOAD_REDIRECTS = 10;

	/**
	 * Connect/read timeout (in milliseconds) for downloading referenced schemas.<br />
	 * Prevents a hanging or slow remote server from blocking schema parsing indefinitely.
	 */
	private static final int DOWNLOAD_TIMEOUT_MILLISECONDS = 10_000;

	/**
	 * Maximum allowed size (in bytes) of a downloaded schema document.<br />
	 * Prevents memory exhaustion from a malicious or misbehaving remote server.
	 */
	private static final long MAX_DOWNLOAD_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

	private void downloadSchemaData(final String packageName) throws IOException, MalformedURLException, Exception, JsonSchemaDefinitionError {
		String downloadUrl = packageName;
		int redirectsFollowed = 0;
		while (redirectsFollowed <= MAX_DOWNLOAD_REDIRECTS) {
			validateDownloadUrl(downloadUrl, packageName);

			final HttpURLConnection urlConnection = (HttpURLConnection) URI.create(downloadUrl).toURL().openConnection();
			urlConnection.setConnectTimeout(DOWNLOAD_TIMEOUT_MILLISECONDS);
			urlConnection.setReadTimeout(DOWNLOAD_TIMEOUT_MILLISECONDS);
			urlConnection.setInstanceFollowRedirects(false);
			try {
				final int statusCode = urlConnection.getResponseCode();
				if (statusCode >= 300 && statusCode < 400) {
					final String location = urlConnection.getHeaderField("Location");
					if (Utilities.isBlank(location)) {
						throw new JsonSchemaDefinitionError("Cannot get content from '" + packageName + "'. Redirect response did not contain a 'Location' header", null);
					}
					// Resolve relative redirect targets against the current URL
					downloadUrl = URI.create(downloadUrl).resolve(location).toString();
					redirectsFollowed++;
				} else if (statusCode != HttpURLConnection.HTTP_OK) {
					throw new JsonSchemaDefinitionError("Cannot get content from '" + packageName + "'. Http-Code was " + statusCode, null);
				} else {
					try (InputStream jsonSchemaInputStream = new SizeLimitedInputStream(urlConnection.getInputStream(), MAX_DOWNLOAD_SIZE_BYTES, packageName)) {
						addJsonSchemaDefinition(packageName, jsonSchemaInputStream);
					}
					return;
				}
			} finally {
				urlConnection.disconnect();
			}
		}
		throw new JsonSchemaDefinitionError("Cannot get content from '" + packageName + "'. Too many redirects (max " + MAX_DOWNLOAD_REDIRECTS + ")", null);
	}

	/**
	 * Restricts schema downloads to http/https URLs.<br />
	 * This is a basic guard against SSRF via crafted or redirected "$ref" URLs (e.g. "file://", "jar://", ...).<br />
	 * It intentionally does NOT attempt to block internal/private IP ranges, since that requires environment-specific
	 * network policy that callers should enforce via {@link #setDownloadReferencedSchemas(boolean)} and/or a
	 * network-level egress restriction. Downloading of referenced schemas is disabled by default for this reason.
	 */
	private static void validateDownloadUrl(final String url, final String originalPackageName) throws JsonSchemaDefinitionError {
		final URI uri;
		try {
			uri = URI.create(url);
		} catch (final IllegalArgumentException e) {
			throw new JsonSchemaDefinitionError("Cannot get content from '" + originalPackageName + "'. Invalid redirect target URL: " + url, null, e);
		}
		final String scheme = uri.getScheme();
		if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
			throw new JsonSchemaDefinitionError("Cannot get content from '" + originalPackageName + "'. Only 'http' and 'https' URLs are allowed for JSON schema downloads, but was: " + url, null);
		}
	}

	/**
	 * Returns the name of the schema containing a reference without "#".
	 *
	 * @param reference
	 *            the reference, e.g. a definition name
	 * @param jsonSchemaPath
	 *            the path of the "$ref" keyword, used in error messages
	 * @return "#" for the main schema, or the name of an additional schema
	 * @throws JsonSchemaDefinitionError
	 *             if the reference is null or found in no schema
	 */
	public synchronized String getSchemaContainingReference(final String reference, final JsonSchemaPath jsonSchemaPath) throws JsonSchemaDefinitionError {
		if (reference != null) {
			if (!reference.contains("#")) {
				// Dereference simple reference without '#'
				if (schemaDocumentNode.get("definitions") != null && schemaDocumentNode.get("definitions") instanceof JsonObject && ((JsonObject) schemaDocumentNode.get("definitions")).containsKey(reference)) {
					final Object dereferencedValue = ((JsonObject) schemaDocumentNode.get("definitions")).get(reference);
					if (!(dereferencedValue instanceof JsonObject)) {
						throw new JsonSchemaDefinitionError("Invalid JSON schema reference data type for key '" + reference + "'. Expected 'object' but was '" + dereferencedValue.getClass().getSimpleName() + "'", jsonSchemaPath);
					} else {
						return "#";
					}
				} else {
					for (final Entry<String, JsonObject> additionalSchemaDocuments : additionalSchemaDocumentNodes.entrySet()) {
						final JsonObject indirectJsonDefinitions = additionalSchemaDocuments.getValue();
						if (indirectJsonDefinitions == null) {
							// Not yet resolved external schema (lazyFailOnMissingExternalSchemas placeholder), skip it
							continue;
						} else if (indirectJsonDefinitions.get("definitions") != null && indirectJsonDefinitions.get("definitions") instanceof JsonObject && ((JsonObject) indirectJsonDefinitions.get("definitions")).containsKey(reference)) {
							final Object dereferencedValue = ((JsonObject) indirectJsonDefinitions.get("definitions")).get(reference);
							if (!(dereferencedValue instanceof JsonObject)) {
								throw new JsonSchemaDefinitionError("Invalid JSON schema reference data type for key '" + reference + "'. Expected 'object' but was '" + dereferencedValue.getClass().getSimpleName() + "'", jsonSchemaPath);
							} else {
								return additionalSchemaDocuments.getKey();
							}
						}
					}
					throw new JsonSchemaDefinitionError("Invalid JSON schema reference key '" + reference + "' or reference key not found. Use simple reference keys or this pattern for reference keys: '<referenced packagename or empty>#/definitions/<your reference key>'", jsonSchemaPath);
				}
			} else if (reference.startsWith("#")) {
				// Dereference local document reference
				final JsonPath jsonPath = new JsonPath(reference);
				JsonObject referencedObject = schemaDocumentNode;
				for (final JsonPathElement jsonPathElement : jsonPath.getPathParts()) {
					if (jsonPathElement == null) {
						throw new JsonSchemaDefinitionError("Invalid JSON reference path contains null value", jsonSchemaPath);
					} else if (jsonPathElement instanceof JsonPathPropertyElement) {
						if (!referencedObject.containsKey(jsonPathElement.toString())) {
							throw new JsonSchemaDefinitionError("Referenced JsonSchema does not contain the reference path '" + reference + "'", jsonSchemaPath);
						} else if (referencedObject.get(jsonPathElement.toString()) == null) {
							throw new JsonSchemaDefinitionError("Invalid data type 'null' for reference path '" + reference + "'", jsonSchemaPath);
						} else if (!(referencedObject.get(jsonPathElement.toString()) instanceof JsonObject)) {
							throw new JsonSchemaDefinitionError("Invalid data type '" + referencedObject.get(jsonPathElement.toString()).getClass().getSimpleName() + "' for reference path '" + reference + "'", jsonSchemaPath);
						} else {
							referencedObject = (JsonObject) referencedObject.get(jsonPathElement.toString());
						}
					}
				}
				return "#";
			} else {
				// Dereference other document reference
				final String packageName = reference.substring(0, reference.lastIndexOf("#"));

				if (!additionalSchemaDocumentNodes.containsKey(packageName) && packageName != null && packageName.toLowerCase().startsWith("http")) {
					if (downloadReferencedSchemas) {
						try {
							downloadSchemaData(packageName);
						} catch (final JsonSchemaDefinitionError e) {
							throw e;
						} catch (final Exception e) {
							throw new JsonSchemaDefinitionError("Cannot get content from '" + packageName + "'", jsonSchemaPath, e);
						}
					} else if (lazyFailOnMissingExternalSchemas){
						additionalSchemaDocumentNodes.put(packageName, null);
					} else {
						throw new JsonSchemaDefinitionError("Referenced JSON schema needs download: " + packageName, jsonSchemaPath);
					}
				}

				if (!additionalSchemaDocumentNodes.containsKey(packageName)) {
					throw new JsonSchemaDefinitionError("Unknown JSON schema reference package name '" + packageName + "'", jsonSchemaPath);
				} else if (additionalSchemaDocumentNodes.get(packageName) == null) {
					throw new JsonSchemaDefinitionError("Invalid empty JSON schema reference for package name '" + packageName + "'", jsonSchemaPath);
				} else {
					final JsonPath jsonPath = new JsonPath(reference.substring(reference.lastIndexOf("#")));
					// Must walk the external referenced document, not the local root schema document
					JsonObject referencedObject = additionalSchemaDocumentNodes.get(packageName);
					for (final JsonPathElement jsonPathElement : jsonPath.getPathParts()) {
						if (jsonPathElement == null) {
							throw new JsonSchemaDefinitionError("Invalid JSON reference path contains null value", jsonSchemaPath);
						} else if (jsonPathElement instanceof JsonPathPropertyElement) {
							if (!referencedObject.containsKey(jsonPathElement.toString())) {
								throw new JsonSchemaDefinitionError("Referenced JsonSchema does not contain the reference path '" + reference + "'", jsonSchemaPath);
							} else if (referencedObject.get(jsonPathElement.toString()) == null) {
								throw new JsonSchemaDefinitionError("Invalid data type 'null' for reference path '" + reference + "'", jsonSchemaPath);
							} else if (!(referencedObject.get(jsonPathElement.toString()) instanceof JsonObject)) {
								throw new JsonSchemaDefinitionError("Invalid data type '" + referencedObject.get(jsonPathElement.toString()).getClass().getSimpleName() + "' for reference path '" + reference + "'", jsonSchemaPath);
							} else {
								referencedObject = (JsonObject) referencedObject.get(jsonPathElement.toString());
							}
						}
					}
					return packageName;
				}
			}
		} else {
			throw new JsonSchemaDefinitionError("Invalid JSON schema reference key 'null'", jsonSchemaPath);
		}
	}

	/**
	 * Adds an additional schema document to the resource index, if the index was already built.
	 * Internal references of the document ("#...") are resolved against its own base URI, which is
	 * its "$id" or otherwise its reference name.
	 *
	 * @param jsonSchemaReferenceName
	 *            name or URI the document is registered by
	 * @param jsonSchemaDocument
	 *            the schema document
	 */
	private synchronized void addToResourceIndex(final String jsonSchemaReferenceName, final JsonObject jsonSchemaDocument) {
		if (resourceIndex != null) {
			resourceIndex.addDocument(JsonSchemaUriUtilities.resolve(rootRetrievalUri, jsonSchemaReferenceName), jsonSchemaDocument, jsonSchemaVersion);
		}
	}

	/**
	 * Returns the resource index, building it on first use.
	 *
	 * @return the resource index
	 */
	private synchronized JsonSchemaResourceIndex getResourceIndex() {
		if (resourceIndex == null) {
			final JsonSchemaResourceIndex newResourceIndex = new JsonSchemaResourceIndex();
			// The main schema document is evaluated with the configured version, even if its "$schema" differs
			newResourceIndex.addDocument(rootRetrievalUri, schemaDocumentNode, jsonSchemaVersion, jsonSchemaVersion);
			for (final Entry<String, JsonObject> additionalSchemaDocument : additionalSchemaDocumentNodes.entrySet()) {
				if (additionalSchemaDocument.getValue() != null) {
					newResourceIndex.addDocument(JsonSchemaUriUtilities.resolve(rootRetrievalUri, additionalSchemaDocument.getKey()), additionalSchemaDocument.getValue(), jsonSchemaVersion);
				}
			}
			resourceIndex = newResourceIndex;
		}
		return resourceIndex;
	}

	/**
	 * Returns the base URI in effect for a schema object, against which its "$ref" values are
	 * resolved.
	 *
	 * @param schemaObject
	 *            a schema object of one of the known schema documents
	 * @return the base URI, or the base URI of the main schema document for unknown objects
	 */
	public synchronized String getBaseUri(final JsonNode schemaObject) {
		final String baseUri = getResourceIndex().getBaseUri(schemaObject);
		if (baseUri != null) {
			return baseUri;
		} else {
			final String rootBaseUri = getResourceIndex().getBaseUri(schemaDocumentNode);
			return rootBaseUri != null ? rootBaseUri : rootRetrievalUri;
		}
	}

	/**
	 * Returns the schema node identified by an absolute URI. Bundled meta schemas are loaded from
	 * the classpath and, if allowed, other http(s) schemas are downloaded on first use.
	 *
	 * @param absoluteUri
	 *            the absolute URI, optionally with a JSON pointer or anchor fragment
	 * @return the schema node (object, boolean or any other node a JSON pointer leads to), or null
	 *         if it cannot be resolved
	 * @throws JsonSchemaDefinitionError
	 *             if a download fails
	 */
	public JsonNode getSchemaNodeByUri(final String absoluteUri) throws JsonSchemaDefinitionError {
		return getSchemaNodeByUri(absoluteUri, true);
	}

	/**
	 * Returns the schema node identified by an absolute URI. Bundled meta schemas are loaded from
	 * the classpath on first use.
	 *
	 * @param absoluteUri
	 *            the absolute URI, optionally with a JSON pointer or anchor fragment
	 * @param allowDownload
	 *            true to download other http(s) schemas, if downloads are allowed by
	 *            {@link #setDownloadReferencedSchemas(boolean)}
	 * @return the schema node (object, boolean or any other node a JSON pointer leads to), or null
	 *         if it cannot be resolved
	 * @throws JsonSchemaDefinitionError
	 *             if a download fails
	 */
	public synchronized JsonNode getSchemaNodeByUri(final String absoluteUri, final boolean allowDownload) throws JsonSchemaDefinitionError {
		final JsonSchemaResourceIndex index = getResourceIndex();
		final JsonNode node = index.get(absoluteUri);
		if (node != null) {
			return node;
		}

		final String documentUri = JsonSchemaUriUtilities.removeFragment(JsonSchemaUriUtilities.normalize(absoluteUri));
		if (index.containsResource(documentUri) || additionalSchemaDocumentNodes.containsKey(documentUri)) {
			// Document is known, but the fragment does not exist
			return null;
		}

		if (loadBundledMetaSchema(documentUri)) {
			return index.get(absoluteUri);
		} else if (allowDownload && downloadReferencedSchemas && documentUri.toLowerCase().startsWith("http")) {
			try {
				downloadSchemaData(documentUri);
			} catch (final JsonSchemaDefinitionError e) {
				throw e;
			} catch (final Exception e) {
				throw new JsonSchemaDefinitionError("Cannot get content from '" + documentUri + "'", null, e);
			}
			return index.get(absoluteUri);
		} else {
			return null;
		}
	}

	/**
	 * Checks whether the schema document a URI points to is already known (main schema, additional
	 * schemas or already loaded schemas).
	 *
	 * @param uri
	 *            the URI, a fragment is ignored
	 * @return true, if the document is known
	 */
	public synchronized boolean isKnownSchemaDocument(final String uri) {
		return getResourceIndex().containsResource(uri);
	}

	/**
	 * Loads a bundled meta schema from the classpath, if the URI identifies one.
	 *
	 * @param documentUri
	 *            the document URI without fragment
	 * @return true, if a bundled meta schema was loaded
	 */
	private boolean loadBundledMetaSchema(final String documentUri) {
		String normalizedUri = documentUri.toLowerCase();
		if (normalizedUri.startsWith("https://")) {
			normalizedUri = "http://" + normalizedUri.substring(8);
		}
		final String resourcePath = BUNDLED_META_SCHEMAS.get(normalizedUri);
		if (resourcePath == null) {
			return false;
		}
		try (InputStream metaSchemaStream = JsonSchemaDependencyResolver.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (metaSchemaStream == null) {
				return false;
			}
			addJsonSchemaDefinition(documentUri, metaSchemaStream);
			return true;
		} catch (@SuppressWarnings("unused") final Exception e) {
			return false;
		}
	}

	/**
	 * Returns the runtime state of the current validation of this thread.
	 *
	 * @return the evaluation context
	 */
	public JsonSchemaEvaluationContext getEvaluationContext() {
		if (!isVersionAtLeast(JsonSchemaVersion.draft2019_09)) {
			// No annotations and dynamic scope up to draft 7: avoid creating thread bound state
			return INACTIVE_EVALUATION_CONTEXT;
		}
		return evaluationContext.get();
	}

	/**
	 * Removes the runtime state of the current thread after a validation run.
	 */
	public void clearEvaluationContext() {
		evaluationContext.remove();
	}

	/**
	 * Checks whether a schema resource defines a dynamic anchor (draft 2020-12).
	 *
	 * @param resourceUri
	 *            the URI of the schema resource
	 * @param anchorName
	 *            the anchor name
	 * @return true, if the resource contains "$dynamicAnchor" with this name
	 */
	public synchronized boolean hasDynamicAnchor(final String resourceUri, final String anchorName) {
		return getResourceIndex().hasDynamicAnchor(resourceUri, anchorName);
	}

	/**
	 * Checks whether the root schema of a resource has "$recursiveAnchor": true (draft 2019-09).
	 *
	 * @param resourceUri
	 *            the URI of the schema resource
	 * @return true, if the recursive anchor is set
	 */
	public synchronized boolean hasRecursiveAnchor(final String resourceUri) {
		return getResourceIndex().hasRecursiveAnchor(resourceUri);
	}

	/**
	 * Evaluates the meta schema given by "$schema" of the main schema document: detects the JSON
	 * schema version of a custom meta schema and the vocabularies it declares by "$vocabulary".
	 *
	 * @param metaSchemaUri
	 *            the "$schema" value
	 * @return the JSON schema version the meta schema is written for, or null if unknown
	 */
	public JsonSchemaVersion applyMetaSchema(final String metaSchemaUri) {
		JsonSchemaVersion version = JsonSchemaVersion.getJsonSchemaVersionByVersionUrl(metaSchemaUri);
		if (version == null) {
			try {
				final JsonNode metaSchema = getSchemaNodeByUri(JsonSchemaUriUtilities.resolve(rootRetrievalUri, metaSchemaUri));
				if (metaSchema instanceof JsonObject) {
					final JsonObject metaSchemaObject = (JsonObject) metaSchema;
					if (metaSchemaObject.get("$schema") instanceof JsonValueString) {
						version = JsonSchemaVersion.getJsonSchemaVersionByVersionUrl(((JsonValueString) metaSchemaObject.get("$schema")).getValue());
					}
					if (metaSchemaObject.get("$vocabulary") instanceof JsonObject) {
						final JsonObject vocabularies = (JsonObject) metaSchemaObject.get("$vocabulary");
						validationVocabulary = vocabularies.containsKey(VOCABULARY_2019_09_VALIDATION) || vocabularies.containsKey(VOCABULARY_2020_12_VALIDATION);
						formatAssertionVocabulary = vocabularies.containsKey(VOCABULARY_2020_12_FORMAT_ASSERTION);
					}
				}
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Unknown meta schema, keep defaults
			}
		}
		return version;
	}

	/**
	 * Checks whether the validation vocabulary (type, enum, minimum, required ...) is active.
	 *
	 * @return false only if a custom meta schema does not declare the validation vocabulary
	 */
	public boolean isValidationVocabularyActive() {
		return validationVocabulary;
	}

	/**
	 * Checks whether "format" is validated as assertion.
	 *
	 * @return true if set explicitly, if the format assertion vocabulary is declared, or for JSON
	 *         schema versions up to draft 7 and simple mode
	 */
	public boolean isFormatAssertion() {
		if (formatAssertion != null) {
			return formatAssertion;
		} else if (formatAssertionVocabulary) {
			return true;
		} else {
			return !isVersionAtLeast(JsonSchemaVersion.draft2019_09);
		}
	}

	/**
	 * Sets whether "format" is validated as assertion.
	 *
	 * @param formatAssertion
	 *            true to validate, false to regard it as annotation only, null for the default of
	 *            the JSON schema version
	 */
	public void setFormatAssertion(final Boolean formatAssertion) {
		this.formatAssertion = formatAssertion;
	}

	/**
	 * Sets whether "format" is validated as assertion.
	 *
	 * @param newFormatAssertion
	 *            true to validate, false to regard it as annotation only, null for the default of
	 *            the JSON schema version
	 * @return this resolver for chaining
	 */
	public JsonSchemaDependencyResolver withFormatAssertion(final Boolean newFormatAssertion) {
		setFormatAssertion(newFormatAssertion);
		return this;
	}

	/**
	 * Sets the URI the main schema document was retrieved from. It is the initial base URI for
	 * resolving references, if the document has no absolute "$id".
	 *
	 * @param rootRetrievalUri
	 *            an absolute URI
	 */
	public void setRootRetrievalUri(final String rootRetrievalUri) {
		this.rootRetrievalUri = rootRetrievalUri == null ? JsonSchemaUriUtilities.DEFAULT_ROOT_BASE_URI : JsonSchemaUriUtilities.normalize(rootRetrievalUri);
		resourceIndex = null;
		versionViews.clear();
	}

	/**
	 * Sets the URI the main schema document was retrieved from.
	 *
	 * @param newRootRetrievalUri
	 *            an absolute URI
	 * @return this resolver for chaining
	 */
	public JsonSchemaDependencyResolver withRootRetrievalUri(final String newRootRetrievalUri) {
		setRootRetrievalUri(newRootRetrievalUri);
		return this;
	}

	/**
	 * Adds an additional schema read from a stream (JSON5 syntax allowed).
	 *
	 * @param jsonSchemaReferenceName
	 *            name used to reference the schema
	 * @param jsonSchemaInputStream
	 *            the schema data
	 * @throws JsonSchemaDefinitionError
	 *             if the name is blank or already used, or the data is no JSON object
	 */
	public synchronized void addJsonSchemaDefinition(final String jsonSchemaReferenceName, final InputStream jsonSchemaInputStream) throws JsonSchemaDefinitionError {
		if (Utilities.isBlank(jsonSchemaReferenceName)) {
			throw new JsonSchemaDefinitionError("Invalid empty JSON schema definition package name", null);
		} else if (additionalSchemaDocumentNodes.containsKey(jsonSchemaReferenceName)) {
			throw new JsonSchemaDefinitionError("Additional JSON schema definition package '" + jsonSchemaReferenceName + "' was already added before", null);
		} else {
			try (Json5Reader reader = new Json5Reader(jsonSchemaInputStream)) {
				final JsonNode jsonNode = reader.read();
				if (!jsonNode.isJsonObject()) {
					throw new JsonSchemaDefinitionError("Additional JSON schema definition package '" + jsonSchemaReferenceName + "' does not contain JSON schema data of type 'object'", null);
				} else {
					final JsonObject jsonSchema = (JsonObject) jsonNode;
					additionalSchemaDocumentNodes.put(jsonSchemaReferenceName, jsonSchema);
					addToResourceIndex(jsonSchemaReferenceName, jsonSchema);
				}
			} catch (final JsonSchemaDefinitionError e) {
				throw e;
			} catch (final Exception e) {
				throw new JsonSchemaDefinitionError("Additional JSON schema definition is no valid JSON", null, e);
			}
		}
	}

	/**
	 * Adds an additional schema. Its internal references ("#...") are resolved against its own base
	 * URI, which is its "$id" or otherwise its reference name.
	 *
	 * @param jsonSchemaReferenceName
	 *            name used to reference the schema
	 * @param jsonSchemaReferenceObject
	 *            the schema data
	 * @throws Exception
	 *             if the name is blank or already used
	 */
	public synchronized void addJsonSchemaDefinition(final String jsonSchemaReferenceName, final JsonObject jsonSchemaReferenceObject) throws Exception {
		if (Utilities.isBlank(jsonSchemaReferenceName)) {
			throw new JsonSchemaDefinitionError("Invalid empty JSON schema definition package name", null);
		} else if (additionalSchemaDocumentNodes.containsKey(jsonSchemaReferenceName)) {
			throw new JsonSchemaDefinitionError("Additional JSON schema definition package '" + jsonSchemaReferenceName + "' was already added before", null);
		} else {
			additionalSchemaDocumentNodes.put(jsonSchemaReferenceName, jsonSchemaReferenceObject);
			addToResourceIndex(jsonSchemaReferenceName, jsonSchemaReferenceObject);
		}
	}

	/**
	 * Sets the JSON schema version.
	 *
	 * @param jsonSchemaVersion
	 *            the version, null means simple mode
	 */
	public void setJsonSchemaVersion(final JsonSchemaVersion jsonSchemaVersion) {
		if (this.jsonSchemaVersion != jsonSchemaVersion) {
			// Identifier keywords depend on the version, so the index has to be rebuilt
			resourceIndex = null;
			versionViews.clear();
		}
		this.jsonSchemaVersion = jsonSchemaVersion;
	}

	/**
	 * Returns the JSON schema version.
	 *
	 * @return the version, null means simple mode
	 */
	public JsonSchemaVersion getJsonSchemaVersion() {
		return jsonSchemaVersion;
	}

	/**
	 * Sets the JSON schema version.
	 *
	 * @param newJsonSchemaVersion
	 *            the version, null means simple mode
	 * @return this resolver for chaining
	 */
	public JsonSchemaDependencyResolver withJsonSchemaVersion(final JsonSchemaVersion newJsonSchemaVersion) {
		setJsonSchemaVersion(newJsonSchemaVersion);
		return this;
	}

	/**
	 * Checks for simple mode (strict validation, no version specific rules).
	 *
	 * @return true, if the version is simple or null
	 */
	public boolean isSimpleMode() {
		return jsonSchemaVersion == null || jsonSchemaVersion == JsonSchemaVersion.simple;
	}

	/**
	 * Checks for JSON schema draft v3.
	 *
	 * @return true, if the version is draft v3
	 */
	public boolean isDraftV3Mode() {
		return jsonSchemaVersion == JsonSchemaVersion.draftV3;
	}

	/**
	 * Checks for JSON schema draft v4.
	 *
	 * @return true, if the version is draft v4
	 */
	public boolean isDraftV4Mode() {
		return jsonSchemaVersion == JsonSchemaVersion.draftV4;
	}

	/**
	 * Checks for JSON schema draft v6.
	 *
	 * @return true, if the version is draft v6
	 */
	public boolean isDraftV6Mode() {
		return jsonSchemaVersion == JsonSchemaVersion.draftV6;
	}

	/**
	 * Checks for JSON schema draft v7.
	 *
	 * @return true, if the version is draft v7
	 */
	public boolean isDraftV7Mode() {
		return jsonSchemaVersion == JsonSchemaVersion.draftV7;
	}

	/**
	 * Checks for JSON schema draft 2019-09.
	 *
	 * @return true, if the version is draft 2019-09
	 */
	public boolean isDraft2019_09Mode() {
		return jsonSchemaVersion == JsonSchemaVersion.draft2019_09;
	}

	/**
	 * Checks for JSON schema draft 2020-12.
	 *
	 * @return true, if the version is draft 2020-12
	 */
	public boolean isDraft2020_12Mode() {
		return jsonSchemaVersion == JsonSchemaVersion.draft2020_12;
	}

	/**
	 * Checks whether the JSON schema version is the given version or a newer one.
	 *
	 * @param minimumVersion
	 *            the version to compare with
	 * @return true, if the version is equal or newer, false in simple mode
	 */
	public boolean isVersionAtLeast(final JsonSchemaVersion minimumVersion) {
		return jsonSchemaVersion != null && jsonSchemaVersion.isAtLeast(minimumVersion);
	}

	/**
	 * Sets whether external schemas referenced by http(s) URL may be downloaded.
	 *
	 * @param downloadReferencedSchemas
	 *            true to allow downloads
	 */
	public void setDownloadReferencedSchemas(final boolean downloadReferencedSchemas) {
		this.downloadReferencedSchemas = downloadReferencedSchemas;
	}

	/**
	 * Sets whether external schemas referenced by http(s) URL may be downloaded.
	 *
	 * @param newDownloadReferencedSchemas
	 *            true to allow downloads
	 * @return this resolver for chaining
	 */
	public JsonSchemaDependencyResolver withDownloadReferencedSchemas(final boolean newDownloadReferencedSchemas) {
		setDownloadReferencedSchemas(newDownloadReferencedSchemas);
		return this;
	}

	/**
	 * Sets whether a missing external schema is only reported when it is actually used for
	 * validation, instead of when the schema is read.
	 *
	 * @param lazyFailOnMissingExternalSchemas
	 *            true to report missing external schemas lazily
	 */
	public void setLazyFailOnMissingExternalSchemas(final boolean lazyFailOnMissingExternalSchemas) {
		this.lazyFailOnMissingExternalSchemas = lazyFailOnMissingExternalSchemas;
	}

	/**
	 * Sets whether a missing external schema is only reported when it is actually used.
	 *
	 * @param newLazyFailOnMissingExternalSchemas
	 *            true to report missing external schemas lazily
	 * @return this resolver for chaining
	 */
	public JsonSchemaDependencyResolver withLazyFailOnMissingExternalSchemas(final boolean newLazyFailOnMissingExternalSchemas) {
		setLazyFailOnMissingExternalSchemas(newLazyFailOnMissingExternalSchemas);
		return this;
	}

	/**
	 * Returns the base step limit for the evaluation of regular expressions against data, see
	 * {@link JsonSchemaConfiguration#setRegexStepLimit(long)}.
	 *
	 * @return the base step limit, 0 or less for no limit
	 */
	public long getRegexStepLimit() {
		return regexStepLimit;
	}

	/**
	 * Sets the base step limit for the evaluation of regular expressions against data, see
	 * {@link JsonSchemaConfiguration#setRegexStepLimit(long)}.
	 *
	 * @param regexStepLimit
	 *            the base step limit, 0 or less for no limit
	 */
	public void setRegexStepLimit(final long regexStepLimit) {
		this.regexStepLimit = regexStepLimit;
	}

	/**
	 * Sets the base step limit for the evaluation of regular expressions against data.
	 *
	 * @param newRegexStepLimit
	 *            the base step limit, 0 or less for no limit
	 * @return this resolver for chaining
	 */
	public JsonSchemaDependencyResolver withRegexStepLimit(final long newRegexStepLimit) {
		setRegexStepLimit(newRegexStepLimit);
		return this;
	}

	/**
	 * View of a main resolver with another JSON schema version, used for schema documents declaring
	 * another version by "$schema" than the main schema (see
	 * {@link JsonSchemaDependencyResolver#getResolverForSchemaNode(JsonNode)}).<br />
	 * <br />
	 * Only the version dependent queries are answered by the view itself. Everything else (schema
	 * documents, resource index, reference resolution, validator cache, cycle detection, settings
	 * and the evaluation context) is delegated to the main resolver, so that all validators of one
	 * validation run share the same state. Every new public method of the resolver has to be
	 * delegated here, which is checked by a unit test.
	 */
	private static final class VersionView extends JsonSchemaDependencyResolver {
		/** The main resolver all state is delegated to. */
		private final JsonSchemaDependencyResolver main;

		/**
		 * Creates a version view.
		 *
		 * @param main
		 *            the main resolver
		 * @param jsonSchemaVersion
		 *            the JSON schema version of the view
		 */
		private VersionView(final JsonSchemaDependencyResolver main, final JsonSchemaVersion jsonSchemaVersion) {
			super(main, jsonSchemaVersion);
			this.main = main;
		}

		@Override
		public List<BaseJsonSchemaValidator> getCachedReferenceValidators(final String referenceKey) {
			return main.getCachedReferenceValidators(referenceKey);
		}

		@Override
		public void putCachedReferenceValidators(final String referenceKey, final List<BaseJsonSchemaValidator> validators) {
			main.putCachedReferenceValidators(referenceKey, validators);
		}

		@Override
		public boolean enterReferenceResolution(final String referenceKey, final String jsonPathString) {
			return main.enterReferenceResolution(referenceKey, jsonPathString);
		}

		@Override
		public void exitReferenceResolution(final String referenceKey, final String jsonPathString) {
			main.exitReferenceResolution(referenceKey, jsonPathString);
		}

		@Override
		public JsonObject getDependencyByReference(final String reference, final JsonSchemaPath jsonSchemaPath) throws Exception {
			return main.getDependencyByReference(reference, jsonSchemaPath);
		}

		@Override
		public synchronized JsonObject getDependencyByLegacyReference(final String reference, final JsonSchemaPath jsonSchemaPath) throws Exception {
			return main.getDependencyByLegacyReference(reference, jsonSchemaPath);
		}

		@Override
		public synchronized String getSchemaContainingReference(final String reference, final JsonSchemaPath jsonSchemaPath) throws JsonSchemaDefinitionError {
			return main.getSchemaContainingReference(reference, jsonSchemaPath);
		}

		@Override
		public synchronized String getBaseUri(final JsonNode schemaObject) {
			return main.getBaseUri(schemaObject);
		}

		@Override
		public JsonNode getSchemaNodeByUri(final String absoluteUri) throws JsonSchemaDefinitionError {
			return main.getSchemaNodeByUri(absoluteUri);
		}

		@Override
		public synchronized JsonNode getSchemaNodeByUri(final String absoluteUri, final boolean allowDownload) throws JsonSchemaDefinitionError {
			return main.getSchemaNodeByUri(absoluteUri, allowDownload);
		}

		@Override
		public synchronized boolean isKnownSchemaDocument(final String uri) {
			return main.isKnownSchemaDocument(uri);
		}

		@Override
		public JsonSchemaEvaluationContext getEvaluationContext() {
			if (!isVersionAtLeast(JsonSchemaVersion.draft2019_09)) {
				// Same as the main resolver: no annotations and dynamic scope up to draft 7
				return INACTIVE_EVALUATION_CONTEXT;
			}
			// The runtime state must be shared with the main resolver, e.g. for "unevaluatedProperties"
			return main.evaluationContext.get();
		}

		@Override
		public void clearEvaluationContext() {
			main.clearEvaluationContext();
		}

		@Override
		public synchronized boolean hasDynamicAnchor(final String resourceUri, final String anchorName) {
			return main.hasDynamicAnchor(resourceUri, anchorName);
		}

		@Override
		public synchronized boolean hasRecursiveAnchor(final String resourceUri) {
			return main.hasRecursiveAnchor(resourceUri);
		}

		@Override
		public JsonSchemaVersion applyMetaSchema(final String metaSchemaUri) {
			return main.applyMetaSchema(metaSchemaUri);
		}

		@Override
		public boolean isFormatAssertion() {
			if (main.formatAssertion != null) {
				return main.formatAssertion;
			} else {
				// The vocabularies of a custom meta schema only apply to the main schema
				return !isVersionAtLeast(JsonSchemaVersion.draft2019_09);
			}
		}

		@Override
		public void setFormatAssertion(final Boolean formatAssertion) {
			main.setFormatAssertion(formatAssertion);
		}

		@Override
		public JsonSchemaDependencyResolver withFormatAssertion(final Boolean newFormatAssertion) {
			main.setFormatAssertion(newFormatAssertion);
			return this;
		}

		@Override
		public void setRootRetrievalUri(final String rootRetrievalUri) {
			main.setRootRetrievalUri(rootRetrievalUri);
		}

		@Override
		public JsonSchemaDependencyResolver withRootRetrievalUri(final String newRootRetrievalUri) {
			main.setRootRetrievalUri(newRootRetrievalUri);
			return this;
		}

		@Override
		public synchronized void addJsonSchemaDefinition(final String jsonSchemaReferenceName, final InputStream jsonSchemaInputStream) throws JsonSchemaDefinitionError {
			main.addJsonSchemaDefinition(jsonSchemaReferenceName, jsonSchemaInputStream);
		}

		@Override
		public synchronized void addJsonSchemaDefinition(final String jsonSchemaReferenceName, final JsonObject jsonSchemaReferenceObject) throws Exception {
			main.addJsonSchemaDefinition(jsonSchemaReferenceName, jsonSchemaReferenceObject);
		}

		@Override
		public void setJsonSchemaVersion(final JsonSchemaVersion jsonSchemaVersion) {
			throw new UnsupportedOperationException("The JSON schema version of a version view cannot be changed");
		}

		@Override
		public JsonSchemaDependencyResolver withJsonSchemaVersion(final JsonSchemaVersion newJsonSchemaVersion) {
			throw new UnsupportedOperationException("The JSON schema version of a version view cannot be changed");
		}

		@Override
		public void setDownloadReferencedSchemas(final boolean downloadReferencedSchemas) {
			main.setDownloadReferencedSchemas(downloadReferencedSchemas);
		}

		@Override
		public JsonSchemaDependencyResolver withDownloadReferencedSchemas(final boolean newDownloadReferencedSchemas) {
			main.setDownloadReferencedSchemas(newDownloadReferencedSchemas);
			return this;
		}

		@Override
		public void setLazyFailOnMissingExternalSchemas(final boolean lazyFailOnMissingExternalSchemas) {
			main.setLazyFailOnMissingExternalSchemas(lazyFailOnMissingExternalSchemas);
		}

		@Override
		public JsonSchemaDependencyResolver withLazyFailOnMissingExternalSchemas(final boolean newLazyFailOnMissingExternalSchemas) {
			main.setLazyFailOnMissingExternalSchemas(newLazyFailOnMissingExternalSchemas);
			return this;
		}

		@Override
		public long getRegexStepLimit() {
			return main.getRegexStepLimit();
		}

		@Override
		public void setRegexStepLimit(final long regexStepLimit) {
			main.setRegexStepLimit(regexStepLimit);
		}

		@Override
		public JsonSchemaDependencyResolver withRegexStepLimit(final long newRegexStepLimit) {
			main.setRegexStepLimit(newRegexStepLimit);
			return this;
		}
	}

	/**
	 * InputStream wrapper that aborts with an IOException once more than {@code maxBytes} have been read.<br />
	 * Used to protect against memory exhaustion when downloading referenced JSON schemas from untrusted URLs.
	 */
	private static final class SizeLimitedInputStream extends InputStream {
		private final InputStream delegate;
		private final long maxBytes;
		private final String sourceDescription;
		private long bytesRead = 0;

		private SizeLimitedInputStream(final InputStream delegate, final long maxBytes, final String sourceDescription) {
			this.delegate = delegate;
			this.maxBytes = maxBytes;
			this.sourceDescription = sourceDescription;
		}

		@Override
		public int read() throws IOException {
			final int nextByte = delegate.read();
			if (nextByte != -1) {
				checkLimit(1);
			}
			return nextByte;
		}

		@Override
		public int read(final byte[] buffer, final int offset, final int length) throws IOException {
			final int readCount = delegate.read(buffer, offset, length);
			if (readCount > 0) {
				checkLimit(readCount);
			}
			return readCount;
		}

		private void checkLimit(final int readCount) throws IOException {
			bytesRead += readCount;
			if (bytesRead > maxBytes) {
				throw new IOException("Downloaded JSON schema from '" + sourceDescription + "' exceeds the maximum allowed size of " + maxBytes + " bytes");
			}
		}

		@Override
		public void close() throws IOException {
			delegate.close();
		}
	}
}
