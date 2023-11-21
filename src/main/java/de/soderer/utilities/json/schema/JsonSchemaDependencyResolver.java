package de.soderer.utilities.json.schema;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.soderer.utilities.json.Json5Reader;
import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.path.JsonPathElement;
import de.soderer.utilities.json.path.JsonPathPropertyElement;
import de.soderer.utilities.json.utilities.Utilities;

public class JsonSchemaDependencyResolver {
	private JsonObject schemaDocumentNode = null;
	private final Map<String, JsonObject> additionalSchemaDocumentNodes = new HashMap<>();

	/**
	 * Draft V7 mode is the default mode<br />
	 */
	private JsonSchemaVersion jsonSchemaVersion = JsonSchemaVersion.draftV7;

	private boolean downloadReferencedSchemas = false;

	public JsonSchemaDependencyResolver(final JsonObject schemaDocumentNode, final JsonSchemaDependency... dependencies) throws JsonSchemaDefinitionError {
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
				if (schemaDocumentNode.get("definitions") != null && schemaDocumentNode.get("definitions") instanceof JsonObject && ((JsonObject) schemaDocumentNode.get("definitions")).containsPropertyKey(reference)) {
					final Object dereferencedValue = ((JsonObject) schemaDocumentNode.get("definitions")).get(reference);
					if (!(dereferencedValue instanceof JsonObject)) {
						throw new JsonSchemaDefinitionError("Invalid JSON schema reference data type for key '" + reference + "'. Expected 'object' but was '" + dereferencedValue.getClass().getSimpleName() + "'", jsonSchemaPath);
					} else {
						return (JsonObject) dereferencedValue;
					}
				} else {
					for (final JsonObject indirectJsonDefinitions : additionalSchemaDocumentNodes.values()) {
						if (indirectJsonDefinitions.get("definitions") != null && indirectJsonDefinitions.get("definitions") instanceof JsonObject && ((JsonObject) indirectJsonDefinitions.get("definitions")).containsPropertyKey(reference)) {
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
				JsonObject referencedObject = schemaDocumentNode;
				for (final JsonPathElement jsonPathElement : jsonPath.getPathParts()) {
					if (jsonPathElement == null) {
						throw new JsonSchemaDefinitionError("Invalid JSON reference path contains null value", jsonSchemaPath);
					} else if (jsonPathElement instanceof JsonPathPropertyElement) {
						if (!referencedObject.containsPropertyKey(jsonPathElement.toString())) {
							throw new JsonSchemaDefinitionError("Referenced JsonSchema does not contain the reference path '" + reference + "'", jsonSchemaPath);
						} else if (referencedObject.get(jsonPathElement.toString()) == null) {
							throw new JsonSchemaDefinitionError("Invalid data type 'null' for reference path '" + reference + "'", jsonSchemaPath);
						} else if (!(referencedObject.get(jsonPathElement.toString()) instanceof JsonObject)) {
							throw new JsonSchemaDefinitionError("Invalid data type '" + schemaDocumentNode.get("definitions").getClass().getSimpleName() + "' for reference path '" + reference + "'", jsonSchemaPath);
						} else {
							referencedObject = (JsonObject) referencedObject.get(jsonPathElement.toString());
						}
					}
				}
				return referencedObject;
			} else {
				// Dereference other document reference
				String packageName = reference;
				if (packageName.contains("#")) {
					packageName = packageName.substring(0, packageName.lastIndexOf("#"));
				}

				if (!additionalSchemaDocumentNodes.containsKey(packageName) && packageName != null && packageName.toLowerCase().startsWith("http")) {
					if (downloadReferencedSchemas) {
						final URLConnection urlConnection = new URL(packageName).openConnection();
						final int statusCode = ((HttpURLConnection) urlConnection).getResponseCode();
						if (statusCode != HttpURLConnection.HTTP_OK) {
							throw new Exception("Cannot get content from '" + packageName + "'. Http-Code was " + statusCode);
						}
						try (InputStream jsonSchemaInputStream = urlConnection.getInputStream()) {
							addJsonSchemaDefinition(packageName, jsonSchemaInputStream);
						}
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
							if (!referencedObject.containsPropertyKey(jsonPathElement.toString())) {
								throw new JsonSchemaDefinitionError("Referenced JsonSchema does not contain the reference path '" + reference + "'", jsonSchemaPath);
							} else if (referencedObject.get(jsonPathElement.toString()) == null) {
								throw new JsonSchemaDefinitionError("Invalid data type 'null' for reference path '" + reference + "'", jsonSchemaPath);
							} else if (!(referencedObject.get(jsonPathElement.toString()) instanceof JsonObject)) {
								throw new JsonSchemaDefinitionError("Invalid data type '" + schemaDocumentNode.get("definitions").getClass().getSimpleName() + "' for reference path '" + reference + "'", jsonSchemaPath);
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

	public String getSchemaContainingReference(final String reference, final JsonSchemaPath jsonSchemaPath) throws JsonSchemaDefinitionError {
		if (reference != null) {
			if (!reference.contains("#")) {
				// Dereference simple reference without '#'
				if (schemaDocumentNode.get("definitions") != null && schemaDocumentNode.get("definitions") instanceof JsonObject && ((JsonObject) schemaDocumentNode.get("definitions")).containsPropertyKey(reference)) {
					final Object dereferencedValue = ((JsonObject) schemaDocumentNode.get("definitions")).get(reference);
					if (!(dereferencedValue instanceof JsonObject)) {
						throw new JsonSchemaDefinitionError("Invalid JSON schema reference data type for key '" + reference + "'. Expected 'object' but was '" + dereferencedValue.getClass().getSimpleName() + "'", jsonSchemaPath);
					} else {
						return "#";
					}
				} else {
					for (final Entry<String, JsonObject> additionalSchemaDocuments : additionalSchemaDocumentNodes.entrySet()) {
						final JsonObject indirectJsonDefinitions = additionalSchemaDocuments.getValue();
						if (indirectJsonDefinitions.get("definitions") != null && indirectJsonDefinitions.get("definitions") instanceof JsonObject && ((JsonObject) indirectJsonDefinitions.get("definitions")).containsPropertyKey(reference)) {
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
						if (!referencedObject.containsPropertyKey(jsonPathElement.toString())) {
							throw new JsonSchemaDefinitionError("Referenced JsonSchema does not contain the reference path '" + reference + "'", jsonSchemaPath);
						} else if (referencedObject.get(jsonPathElement.toString()) == null) {
							throw new JsonSchemaDefinitionError("Invalid data type 'null' for reference path '" + reference + "'", jsonSchemaPath);
						} else if (!(referencedObject.get(jsonPathElement.toString()) instanceof JsonObject)) {
							throw new JsonSchemaDefinitionError("Invalid data type '" + schemaDocumentNode.get("definitions").getClass().getSimpleName() + "' for reference path '" + reference + "'", jsonSchemaPath);
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
							final URLConnection urlConnection = new URL(packageName).openConnection();
							final int statusCode = ((HttpURLConnection) urlConnection).getResponseCode();
							if (statusCode != HttpURLConnection.HTTP_OK) {
								throw new JsonSchemaDefinitionError("Cannot get content from '" + packageName + "'. Http-Code was " + statusCode, jsonSchemaPath);
							}
							try (InputStream jsonSchemaInputStream = urlConnection.getInputStream()) {
								addJsonSchemaDefinition(packageName, jsonSchemaInputStream);
							}
						} catch (final JsonSchemaDefinitionError e) {
							throw e;
						} catch (final Exception e) {
							throw new JsonSchemaDefinitionError("Cannot get content from '" + packageName + "'", jsonSchemaPath, e);
						}
					} else {
						throw new JsonSchemaDefinitionError("Referenced JSON schema needs download: " + packageName, jsonSchemaPath);
					}
				}

				if (!additionalSchemaDocumentNodes.containsKey(packageName)) {
					throw new JsonSchemaDefinitionError("Unknown JSON schema reference package name '" + packageName + "'", jsonSchemaPath);
				} else if (additionalSchemaDocumentNodes.get(packageName) == null) {
					throw new JsonSchemaDefinitionError("Invalid empty JSON schema reference for package name '" + packageName + "'", jsonSchemaPath);
				} else {
					final JsonPath jsonPath = new JsonPath(reference);
					JsonObject referencedObject = schemaDocumentNode;
					for (final JsonPathElement jsonPathElement : jsonPath.getPathParts()) {
						if (jsonPathElement == null) {
							throw new JsonSchemaDefinitionError("Invalid JSON reference path contains null value", jsonSchemaPath);
						} else if (jsonPathElement instanceof JsonPathPropertyElement) {
							if (!referencedObject.containsPropertyKey(jsonPathElement.toString())) {
								throw new JsonSchemaDefinitionError("Referenced JsonSchema does not contain the reference path '" + reference + "'", jsonSchemaPath);
							} else if (referencedObject.get(jsonPathElement.toString()) == null) {
								throw new JsonSchemaDefinitionError("Invalid data type 'null' for reference path '" + reference + "'", jsonSchemaPath);
							} else if (!(referencedObject.get(jsonPathElement.toString()) instanceof JsonObject)) {
								throw new JsonSchemaDefinitionError("Invalid data type '" + schemaDocumentNode.get("definitions").getClass().getSimpleName() + "' for reference path '" + reference + "'", jsonSchemaPath);
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
					final JsonObject jsonSchema = (JsonObject) jsonNode.getValue();
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

	public void addJsonSchemaDefinition(final String jsonSchemaReferenceName, final JsonObject jsonSchemaReferenceObject) throws JsonSchemaDefinitionError {
		if (Utilities.isBlank(jsonSchemaReferenceName)) {
			throw new JsonSchemaDefinitionError("Invalid empty JSON schema definition package name", null);
		} else if (additionalSchemaDocumentNodes.containsKey(jsonSchemaReferenceName)) {
			throw new JsonSchemaDefinitionError("Additional JSON schema definition package '" + jsonSchemaReferenceName + "' was already added before", null);
		} else {
			redirectReferences(jsonSchemaReferenceObject, "#", jsonSchemaReferenceName + "#");
			additionalSchemaDocumentNodes.put(jsonSchemaReferenceName, jsonSchemaReferenceObject);
		}
	}

	private void redirectReferences(final JsonObject jsonObject, final String referenceDefinitionStart, final String referenceDefinitionReplacement) {
		for (final Entry<String, Object> entry : jsonObject.entrySet()) {
			if ("$ref".equals(entry.getKey()) && entry.getValue() != null && entry.getValue() instanceof String && ((String) entry.getValue()).startsWith(referenceDefinitionStart)) {
				jsonObject.add("$ref", referenceDefinitionReplacement + ((String) entry.getValue()).substring(referenceDefinitionStart.length()));
			} else if (entry.getValue() instanceof JsonObject) {
				redirectReferences((JsonObject) entry.getValue(), referenceDefinitionStart, referenceDefinitionReplacement);
			} else if (entry.getValue() instanceof JsonArray) {
				redirectReferences((JsonArray) entry.getValue(), referenceDefinitionStart, referenceDefinitionReplacement);
			}
		}
	}

	private void redirectReferences(final JsonArray jsonArray, final String referenceDefinitionStart, final String referenceDefinitionReplacement) {
		for (final Object item : jsonArray) {
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
}
