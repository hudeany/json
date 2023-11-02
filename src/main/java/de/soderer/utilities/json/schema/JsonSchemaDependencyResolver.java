package de.soderer.utilities.json.schema;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
	 * Draft V4 mode is NOT default mode<br />
	 * <br />
	 * The default mode uses a slightly more strict JSON schema definition.<br />
	 * This is useful in detection of schema definition errors.<br />
	 * Nontheless you can switch to the Draf V4 standard behaviour<br />
	 */
	private boolean useDraftV4Mode = false;

	private boolean downloadReferencedSchemas = false;

	private JsonPath latestJsonPath = null;
	private Set<String> latestDependencies;

	public JsonSchemaDependencyResolver(final JsonObject schemaDocumentNode) throws Exception {
		if (schemaDocumentNode == null) {
			throw new JsonSchemaDefinitionError("Invalid data type 'null' for JsonSchemaDependencyResolver", new JsonSchemaPath());
		}
		this.schemaDocumentNode = schemaDocumentNode;
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
				final String packageName = reference.substring(0, reference.lastIndexOf("#"));

				if (!additionalSchemaDocumentNodes.containsKey(packageName) && packageName != null && packageName.toLowerCase().startsWith("http") && downloadReferencedSchemas) {
					final URLConnection urlConnection = new URL(packageName).openConnection();
					final int statusCode = ((HttpURLConnection) urlConnection).getResponseCode();
					if (statusCode != HttpURLConnection.HTTP_OK) {
						throw new Exception("Cannot get content from '" + packageName + "'. Http-Code was " + statusCode);
					}
					try (InputStream jsonSchemaInputStream = urlConnection.getInputStream()) {
						addJsonSchemaDefinition(packageName, jsonSchemaInputStream);
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

	public String getSchemaContainingReference(final String reference, final JsonSchemaPath jsonSchemaPath) throws Exception {
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
				return "#";
			} else {
				// Dereference other document reference
				final String packageName = reference.substring(0, reference.lastIndexOf("#"));

				if (!additionalSchemaDocumentNodes.containsKey(packageName) && packageName != null && packageName.toLowerCase().startsWith("http") && downloadReferencedSchemas) {
					final URLConnection urlConnection = new URL(packageName).openConnection();
					final int statusCode = ((HttpURLConnection) urlConnection).getResponseCode();
					if (statusCode != HttpURLConnection.HTTP_OK) {
						throw new Exception("Cannot get content from '" + packageName + "'. Http-Code was " + statusCode);
					}
					try (InputStream jsonSchemaInputStream = urlConnection.getInputStream()) {
						addJsonSchemaDefinition(packageName, jsonSchemaInputStream);
					}
				}

				if (!additionalSchemaDocumentNodes.containsKey(packageName)) {
					throw new Exception("Unknown JSON schema reference package name '" + packageName + "'");
				} else if (additionalSchemaDocumentNodes.get(packageName) == null) {
					throw new Exception("Invalid empty JSON schema reference for package name '" + packageName + "'");
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
			throw new Exception("Invalid JSON schema reference key 'null'");
		}
	}

	public void addJsonSchemaDefinition(final String definitionPackageName, final InputStream jsonSchemaInputStream) throws Exception {
		if (Utilities.isBlank(definitionPackageName)) {
			throw new Exception("Invalid empty JSON schema definition package name");
		} else if (additionalSchemaDocumentNodes.containsKey(definitionPackageName)) {
			throw new Exception("Additional JSON schema definition package '" + definitionPackageName + "' was already added before");
		} else {
			try (Json5Reader reader = new Json5Reader(jsonSchemaInputStream)) {
				final JsonNode jsonNode = reader.read();
				if (!jsonNode.isJsonObject()) {
					throw new Exception("Additional JSON schema definition package '" + definitionPackageName + "' does not contain JSON schema data of type 'object'");
				} else {
					final JsonObject jsonSchema = (JsonObject) jsonNode.getValue();
					redirectReferences(jsonSchema, "#", definitionPackageName + "#");
					additionalSchemaDocumentNodes.put(definitionPackageName, jsonSchema);
				}
			}
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

	public void checkCyclicDependency(final JsonPath jsonPath, final String validatorData, final JsonSchemaPath jsonSchemaPath) throws JsonSchemaDefinitionError {
		if (latestJsonPath == null || !latestJsonPath.equals(jsonPath)) {
			latestJsonPath = jsonPath;
			latestDependencies = new HashSet<>();
		}
		if (latestDependencies.contains(validatorData)) {
			throw new JsonSchemaDefinitionError("Cyclic dependency detected: '" + Utilities.join(latestDependencies, "', ") + "'", jsonSchemaPath);
		} else {
			latestDependencies.add(validatorData);
		}
	}

	public void setUseDraftV4Mode(final boolean useDraftV4Mode) {
		this.useDraftV4Mode = useDraftV4Mode;
	}

	public boolean isUseDraftV4Mode() {
		return useDraftV4Mode;
	}

	public void setDownloadReferencedSchemas(final boolean downloadReferencedSchemas) {
		this.downloadReferencedSchemas = downloadReferencedSchemas;
	}
}
