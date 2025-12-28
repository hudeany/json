package de.soderer.json.schema;

import java.io.InputStream;
import java.util.Map.Entry;

import de.soderer.json.Json5Reader;
import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.utilities.Utilities;

public class JsonSchemaDependency {
	private String jsonSchemaReferenceName;
	private JsonObject jsonSchemaReferenceObject;

	/**
	 * JSON schema for usage of its reference definitions
	 *
	 * @param jsonSchemaReferenceName
	 * @param jsonSchemaReferenceObjectInputStream
	 * @throws Exception
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
					final JsonObject jsonSchema = (JsonObject) jsonNode.getValue();
					jsonSchemaReferenceObject = jsonSchema;
					redirectReferences(jsonSchema, "#", jsonSchemaReferenceName + "#");
				}
			}
		}
	}

	/**
	 * JSON schema for usage of its reference definitions
	 *
	 * @param jsonSchemaReferenceName
	 * @param jsonSchemaReferenceObject
	 * @throws Exception
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

	public String getJsonSchemaReferenceName() {
		return jsonSchemaReferenceName;
	}

	public JsonObject getJsonSchemaReferenceObject() {
		return jsonSchemaReferenceObject;
	}

	private void redirectReferences(final JsonObject jsonObject, final String referenceDefinitionStart, final String referenceDefinitionReplacement) throws Exception {
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

	private void redirectReferences(final JsonArray jsonArray, final String referenceDefinitionStart, final String referenceDefinitionReplacement) throws Exception {
		for (final Object item : jsonArray) {
			if (item instanceof JsonObject) {
				redirectReferences((JsonObject) item, referenceDefinitionStart, referenceDefinitionReplacement);
			} else if (item instanceof JsonArray) {
				redirectReferences((JsonArray) item, referenceDefinitionStart, referenceDefinitionReplacement);
			}
		}
	}
}
