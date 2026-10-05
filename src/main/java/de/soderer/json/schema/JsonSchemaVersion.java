package de.soderer.json.schema;

/**
 * Supported JSON schema versions with their meta schema URL and the bundled meta schema file.
 */
public enum JsonSchemaVersion {
	/** Simple mode: strict validation without version specific rules, unknown keywords are errors. */
	simple(null, null),

	/** JSON schema draft v3. */
	draftV3("http://json-schema.org/draft-03/schema", "json/JsonSchemaDescriptionDraftV3.json"),

	/** JSON schema draft v4 (draft v5 is an alias for draft v4 caused by version management at json-schema.org). */
	draftV4("http://json-schema.org/draft-04/schema", "json/JsonSchemaDescriptionDraftV4.json"),

	/** JSON schema draft v6, see https://json-schema.org/draft-06/json-schema-release-notes */
	draftV6("http://json-schema.org/draft-06/schema", "json/JsonSchemaDescriptionDraftV6.json"),

	/** JSON schema draft v7, see https://json-schema.org/draft-07/json-schema-release-notes */
	draftV7("http://json-schema.org/draft-07/schema", "json/JsonSchemaDescriptionDraftV7.json"),

	// Not supported yet:
	// v2019_09("https://json-schema.org/draft/2019-09/schema", "json/2019-09/schema"), see https://json-schema.org/draft/2019-09/release-notes
	// v2020_12("https://json-schema.org/draft/2020-12/schema", "json/2020-12/schema"), see https://json-schema.org/draft/2020-12/release-notes
	;

	/**
	 * URL of the meta schema, also used as "$schema" value.
	 */
	private final String downloadUrl;

	/**
	 * Classpath resource of the bundled meta schema.
	 */
	private final String localFile;

	/**
	 * Creates a version.
	 *
	 * @param downloadUrl
	 *            URL of the meta schema
	 * @param localFile
	 *            classpath resource of the bundled meta schema
	 */
	JsonSchemaVersion(final String downloadUrl, final String localFile) {
		this.downloadUrl = downloadUrl;
		this.localFile = localFile;
	}

	/**
	 * Returns the URL of the meta schema.
	 *
	 * @return the URL, null for simple mode
	 */
	public String getDownloadUrl() {
		return downloadUrl;
	}

	/**
	 * Returns the classpath resource of the bundled meta schema.
	 *
	 * @return the resource path, null for simple mode
	 */
	public String getLocalFile() {
		return localFile;
	}

	/**
	 * Returns the version for a "$schema" URL. "https" and a trailing "#" are accepted.
	 *
	 * @param jsonSchemaVersionUrl
	 *            the "$schema" URL, may be null
	 * @return the version, or null if the URL is null or unknown
	 */
	public static JsonSchemaVersion getJsonSchemaVersionByVersionUrl(final String jsonSchemaVersionUrl) {
		if (jsonSchemaVersionUrl == null) {
			return null;
		}
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
