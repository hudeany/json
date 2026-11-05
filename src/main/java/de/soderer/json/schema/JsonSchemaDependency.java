package de.soderer.json.schema;

import java.io.InputStream;

import de.soderer.json.Json5Reader;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.utilities.Utilities;

/**
 * Additional JSON schema whose definitions are referenced by another schema, like
 * "other.json#/definitions/address". The name is its URI (relative names are resolved against the
 * base URI of the main schema). Internal references ("#/...") within it are resolved against its
 * own base URI, which is its "$id" or otherwise its name. The schema data is not changed.
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
	 * @return the schema data, unchanged
	 */
	public JsonObject getJsonSchemaReferenceObject() {
		return jsonSchemaReferenceObject;
	}
}
