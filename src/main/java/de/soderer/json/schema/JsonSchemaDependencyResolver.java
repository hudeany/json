package de.soderer.json.schema;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
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

public class JsonSchemaDependencyResolver {
	private JsonObject schemaDocumentNode = null;
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

	public List<BaseJsonSchemaValidator> getCachedReferenceValidators(final String referenceKey) {
		return referenceValidatorCache.get(referenceKey);
	}

	public void putCachedReferenceValidators(final String referenceKey, final List<BaseJsonSchemaValidator> validators) {
		referenceValidatorCache.put(referenceKey, validators);
	}

	/**
	 * Marks the given reference as "currently being resolved" for the given data path.<br />
	 * Returns {@code true} if this exact combination was already active (i.e. a genuine cycle was detected), in
	 * which case the caller must NOT proceed with resolving/validating it again.
	 */
	public boolean enterReferenceResolution(final String referenceKey, final String jsonPathString) {
		return !activeReferenceResolutions.get().add(referenceKey + " @ " + jsonPathString);
	}

	public void exitReferenceResolution(final String referenceKey, final String jsonPathString) {
		final Set<String> active = activeReferenceResolutions.get();
		active.remove(referenceKey + " @ " + jsonPathString);
		if (active.isEmpty()) {
			// Avoid leaking ThreadLocal state on thread-pool threads once validation of a document is complete
			activeReferenceResolutions.remove();
		}
	}

	/**
	 * Draft V7 mode is the default mode<br />
	 */
	private JsonSchemaVersion jsonSchemaVersion = JsonSchemaVersion.draftV7;

	private boolean downloadReferencedSchemas = false;
	private boolean lazyFailOnMissingExternalSchemas = true;

	public JsonSchemaDependencyResolver(final JsonObject schemaDocumentNode, final JsonSchemaDependency... dependencies) throws Exception {
		if (schemaDocumentNode == null) {
			throw new JsonSchemaDefinitionError("Invalid data type 'null' for JsonSchemaDependencyResolver", new JsonSchemaPath());
		}
		this.schemaDocumentNode = schemaDocumentNode;
		for (final JsonSchemaDependency dependency : dependencies) {
			addJsonSchemaDefinition(dependency.getJsonSchemaReferenceName(), dependency.getJsonSchemaReferenceObject());
		}
	}

	public JsonObject getDependencyByReference(final String reference, final JsonSchemaPath jsonSchemaPath) throws Exception {
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
	private void validateDownloadUrl(final String url, final String originalPackageName) throws JsonSchemaDefinitionError {
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

	public String getSchemaContainingReference(final String reference, final JsonSchemaPath jsonSchemaPath) throws JsonSchemaDefinitionError {
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

	public void addJsonSchemaDefinition(final String jsonSchemaReferenceName, final InputStream jsonSchemaInputStream) throws JsonSchemaDefinitionError {
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
					redirectReferences(jsonSchema, "#", jsonSchemaReferenceName + "#");
					additionalSchemaDocumentNodes.put(jsonSchemaReferenceName, jsonSchema);
				}
			} catch (final JsonSchemaDefinitionError e) {
				throw e;
			} catch (final Exception e) {
				throw new JsonSchemaDefinitionError("Additional JSON schema definition is no valid JSON", null, e);
			}
		}
	}

	public void addJsonSchemaDefinition(final String jsonSchemaReferenceName, final JsonObject jsonSchemaReferenceObject) throws Exception {
		if (Utilities.isBlank(jsonSchemaReferenceName)) {
			throw new JsonSchemaDefinitionError("Invalid empty JSON schema definition package name", null);
		} else if (additionalSchemaDocumentNodes.containsKey(jsonSchemaReferenceName)) {
			throw new JsonSchemaDefinitionError("Additional JSON schema definition package '" + jsonSchemaReferenceName + "' was already added before", null);
		} else {
			redirectReferences(jsonSchemaReferenceObject, "#", jsonSchemaReferenceName + "#");
			additionalSchemaDocumentNodes.put(jsonSchemaReferenceName, jsonSchemaReferenceObject);
		}
	}

	private void redirectReferences(final JsonObject jsonObject, final String referenceDefinitionStart, final String referenceDefinitionReplacement) throws Exception {
		// Iterate over a snapshot of the entries: the loop body calls jsonObject.remove(...)/add(...) on "$ref",
		// which would otherwise modify the map while entrySet() is being iterated (ConcurrentModificationException).
		for (final Entry<String, JsonNode> entry : new ArrayList<>(jsonObject.entrySet())) {
			if ("$ref".equals(entry.getKey()) && entry.getValue() != null && entry.getValue() instanceof JsonValueString && ((JsonValueString) entry.getValue()).getValue().startsWith(referenceDefinitionStart)) {
				jsonObject.remove("$ref");
				jsonObject.add("$ref", referenceDefinitionReplacement + ((JsonValueString) entry.getValue()).getValue().substring(referenceDefinitionStart.length()));
			} else if (entry.getValue() instanceof JsonObject) {
				redirectReferences((JsonObject) entry.getValue(), referenceDefinitionStart, referenceDefinitionReplacement);
			} else if (entry.getValue() instanceof JsonArray) {
				redirectReferences((JsonArray) entry.getValue(), referenceDefinitionStart, referenceDefinitionReplacement);
			}
		}
	}

	private void redirectReferences(final JsonArray jsonArray, final String referenceDefinitionStart, final String referenceDefinitionReplacement) throws Exception {
		for (final JsonNode item : jsonArray.items()) {
			if (item instanceof JsonObject) {
				redirectReferences((JsonObject) item, referenceDefinitionStart, referenceDefinitionReplacement);
			} else if (item instanceof JsonArray) {
				redirectReferences((JsonArray) item, referenceDefinitionStart, referenceDefinitionReplacement);
			}
		}
	}

	public void setJsonSchemaVersion(final JsonSchemaVersion jsonSchemaVersion) {
		this.jsonSchemaVersion = jsonSchemaVersion;
	}

	public boolean isSimpleMode() {
		return jsonSchemaVersion == null || jsonSchemaVersion == JsonSchemaVersion.simple;
	}

	public boolean isDraftV3Mode() {
		return jsonSchemaVersion == JsonSchemaVersion.draftV3;
	}

	public boolean isDraftV4Mode() {
		return jsonSchemaVersion == JsonSchemaVersion.draftV4;
	}

	public boolean isDraftV6Mode() {
		return jsonSchemaVersion == JsonSchemaVersion.draftV6;
	}

	public boolean isDraftV7Mode() {
		return jsonSchemaVersion == JsonSchemaVersion.draftV7;
	}

	public void setDownloadReferencedSchemas(final boolean downloadReferencedSchemas) {
		this.downloadReferencedSchemas = downloadReferencedSchemas;
	}

	public void setLazyFailOnMissingExternalSchemas(final boolean lazyFailOnMissingExternalSchemas) {
		this.lazyFailOnMissingExternalSchemas = lazyFailOnMissingExternalSchemas;
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
