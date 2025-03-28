package de.soderer.json.schema;

public enum JsonSchemaVersion {
	simple(null, null),

	draftV3("http://json-schema.org/draft-03/schema", "json/JsonSchemaDescriptionDraftV3.json"),

	draftV4("http://json-schema.org/draft-04/schema", "json/JsonSchemaDescriptionDraftV4.json"),

	/**
	 * draft v5 is an alias for draft v4 caused by version management at json-schema.org
	 */
	//draftV5("http://json-schema.org/draft-04/schema", "json/JsonSchemaDescriptionDraftV4.json"),

	/**
	 * See https://json-schema.org/draft-06/json-schema-release-notes
	 */
	draftV6("http://json-schema.org/draft-06/schema", "json/JsonSchemaDescriptionDraftV6.json"),

	/**
	 * See https://json-schema.org/draft-07/json-schema-release-notes
	 */
	draftV7("http://json-schema.org/draft-07/schema", "json/JsonSchemaDescriptionDraftV7.json"),

	/**
	 * See https://json-schema.org/draft/2019-09/release-notes
	 * Not supported yet
	 */
	//v2019_09("https://json-schema.org/draft/2019-09/schema", "json/2019-09/schema"),

	/**
	 * See https://json-schema.org/draft/2020-12/release-notes
	 * Not supported yet
	 */
	//v2020_12("https://json-schema.org/draft/2020-12/schema", "json/2020-12/schema"),
	;

	private final String downloadUrl;

	private final String localFile;

	JsonSchemaVersion(final String downloadUrl, final String localFile) {
		this.downloadUrl = downloadUrl;
		this.localFile = localFile;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public String getLocalFile() {
		return localFile;
	}

	public static JsonSchemaVersion getJsonSchemaVersionByVersionUrl(final String jsonSchemaVersionUrl) {
		String jsonSchemaVersionUrlNormalized = jsonSchemaVersionUrl.toLowerCase();
		if (jsonSchemaVersionUrlNormalized.startsWith("https://")) {
			jsonSchemaVersionUrlNormalized = "http://" + jsonSchemaVersionUrlNormalized.substring(8);
		}
		if (jsonSchemaVersionUrlNormalized.endsWith("#")) {
			jsonSchemaVersionUrlNormalized = jsonSchemaVersionUrlNormalized.substring(0, jsonSchemaVersionUrlNormalized.length() - 1);
		}
		for (final JsonSchemaVersion jsonSchemaVersion : JsonSchemaVersion.values()) {
			if (jsonSchemaVersion.getDownloadUrl() != null && jsonSchemaVersion.getDownloadUrl().equalsIgnoreCase(jsonSchemaVersionUrlNormalized)) {
				return jsonSchemaVersion;
			}
		}
		return null;
	}
}
