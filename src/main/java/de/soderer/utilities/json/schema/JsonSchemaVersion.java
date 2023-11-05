package de.soderer.utilities.json.schema;

public enum JsonSchemaVersion {
	draftV4("https://json-schema.org/draft-04/schema", "json/JsonSchemaDescriptionDraftV4.json"),

	/**
	 * draft v5 is an alias for draft v4 caused by version management at json-schema.org
	 */
	//draftV5("https://json-schema.org/draft-04/schema", "json/JsonSchemaDescriptionDraftV4.json"),

	/**
	 * See https://json-schema.org/draft-06/json-schema-release-notes
	 */
	draftV6("https://json-schema.org/draft-06/schema", "json/JsonSchemaDescriptionDraftV6.json"),

	/**
	 * See https://json-schema.org/draft-07/json-schema-release-notes
	 */
	draftV7("https://json-schema.org/draft-07/schema", "json/JsonSchemaDescriptionDraftV7.json");

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
}
