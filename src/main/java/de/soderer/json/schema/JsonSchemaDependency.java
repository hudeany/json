package de.soderer.json.schema;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map.Entry;

import de.soderer.json.Json5Reader;
import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueString;
import de.soderer.json.utilities.Utilities;

/**
 * Additional JSON schema whose definitions are referenced by another schema, like
 * "other.json#/definitions/address". Internal references ("#/...") within it are redirected to its
 * name.
 */
public class JsonSchemaDependency {
	/**
	 * Name used to reference this schema.
	 */
	private String jsonSchemaReferenceName;
	/**
	 * The schema data.
	 */
	private JsonObject jsonSchemaReferenceObject;

	/**
	 * Reads a JSON schema for usage of its reference definitions.
	 *
	 * @param jsonSchemaReferenceName
	 *            name used to reference the schema, e.g. "other.json"
	 * @param jsonSchemaReferenceObjectInputStream
	 *            the schema data (JSON5 syntax allowed)
	 * @throws Exception
	 *             if the name is blank or the data is no JSON object
	 */
	public JsonSchemaDependency(final String jsonSchemaReferenceName, final InputStream jsonSchemaReferenceObjectInputStream) throws Exception {
		if (Utilities.isBlank(jsonSchemaReferenceName)) {
			throw new Exception("Invalid empty JSON schema reference name");
		} else {
			this.jsonSchemaReferenceName = jsonSchemaReferenceName;
			try (Json5Reader reader = new Json5Reader(jsonSchemaReferenceObjectInputStream)) {
				final JsonNode jsonNode = reader.read();
				if (!jsonNode.isJsonObject()) {
					throw new Exception("JSON schema reference '" + jsonSchemaReferenceName + "' does not contain JSON schema data of type 'object'");
				} else {
					final JsonObject jsonSchema = (JsonObject) jsonNode;
					jsonSchemaReferenceObject = jsonSchema;
					redirectReferences(jsonSchema, "#", jsonSchemaReferenceName + "#");
				}
			}
		}
	}

	/**
	 * Uses a JSON schema for usage of its reference definitions. Internal references of the object
	 * are changed in place.
	 *
	 * @param jsonSchemaReferenceName
	 *            name used to reference the schema, e.g. "other.json"
	 * @param jsonSchemaReferenceObject
	 *            the schema data
	 * @throws Exception
	 *             if the name is blank
	 */
	public JsonSchemaDependency(final String jsonSchemaReferenceName, final JsonObject jsonSchemaReferenceObject) throws Exception {
		if (Utilities.isBlank(jsonSchemaReferenceName)) {
			throw new Exception("Invalid empty JSON schema reference name");
		} else {
			this.jsonSchemaReferenceName = jsonSchemaReferenceName;
			this.jsonSchemaReferenceObject = jsonSchemaReferenceObject;
			redirectReferences(jsonSchemaReferenceObject, "#", jsonSchemaReferenceName + "#");
		}
	}

	/**
	 * Returns the name used to reference this schema.
	 *
	 * @return the name
	 */
	public String getJsonSchemaReferenceName() {
		return jsonSchemaReferenceName;
	}

	/**
	 * Returns the schema data.
	 *
	 * @return the schema data with redirected references
	 */
	public JsonObject getJsonSchemaReferenceObject() {
		return jsonSchemaReferenceObject;
	}

	private void redirectReferences(final JsonObject jsonObject, final String referenceDefinitionStart, final String referenceDefinitionReplacement) throws Exception {
		// Iterate over a snapshot of the entries: the loop body calls jsonObject.remove(...)/add(...) on "$ref",
		// which would otherwise modify the map while entrySet() is being iterated (ConcurrentModificationException).
		for (final Entry<String, JsonNode> entry : new ArrayList<>(jsonObject.entrySet())) {
			if ("$ref".equals(entry.getKey()) && entry.getValue() != null && entry.getValue() instanceof JsonValueString && ((JsonValueString) entry.getValue()).getValue().startsWith(referenceDefinitionStart)) {
				// Replace keeps the position of the property
				jsonObject.replace("$ref", referenceDefinitionReplacement + ((JsonValueString) entry.getValue()).getValue().substring(referenceDefinitionStart.length()));
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
}
