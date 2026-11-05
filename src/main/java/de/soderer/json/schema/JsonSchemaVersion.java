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

	/**
	 * JSON schema draft 2019-09, see https://json-schema.org/draft/2019-09/release-notes<br />
	 * Main changes: "$defs", "$anchor", "$recursiveRef"/"$recursiveAnchor", "unevaluatedProperties",
	 * "unevaluatedItems", "dependentRequired", "dependentSchemas", "minContains"/"maxContains", "$ref"
	 * applies together with its sibling keywords, "format" and "content*" are annotations only.
	 */
	draft2019_09("https://json-schema.org/draft/2019-09/schema", "json/JsonSchemaDescriptionDraft2019-09/schema.json"),

	/**
	 * JSON schema draft 2020-12, see https://json-schema.org/draft/2020-12/release-notes<br />
	 * Main changes compared to 2019-09: "prefixItems" replaces the array form of "items", "items"
	 * replaces "additionalItems", "$dynamicRef"/"$dynamicAnchor" replace
	 * "$recursiveRef"/"$recursiveAnchor", "contains" marks matched items as evaluated.
	 */
	draft2020_12("https://json-schema.org/draft/2020-12/schema", "json/JsonSchemaDescriptionDraft2020-12/schema.json");

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
	 * Checks whether this version is the given version or a newer one. Simple mode is regarded as
	 * older than any draft.
	 *
	 * @param otherVersion
	 *            the version to compare with
	 * @return true, if this version is equal to or newer than the other version
	 */
	public boolean isAtLeast(final JsonSchemaVersion otherVersion) {
		return otherVersion != null && ordinal() >= otherVersion.ordinal();
	}

	/**
	 * Returns the version for a "$schema" URL. "http" and "https" are regarded as equal and a
	 * trailing "#" is accepted.
	 *
	 * @param jsonSchemaVersionUrl
	 *            the "$schema" URL, may be null
	 * @return the version, or null if the URL is null or unknown
	 */
	public static JsonSchemaVersion getJsonSchemaVersionByVersionUrl(final String jsonSchemaVersionUrl) {
		if (jsonSchemaVersionUrl == null) {
			return null;
		}
		final String jsonSchemaVersionUrlNormalized = normalizeVersionUrl(jsonSchemaVersionUrl);
		for (final JsonSchemaVersion jsonSchemaVersion : JsonSchemaVersion.values()) {
			if (jsonSchemaVersion.getDownloadUrl() != null && normalizeVersionUrl(jsonSchemaVersion.getDownloadUrl()).equals(jsonSchemaVersionUrlNormalized)) {
				return jsonSchemaVersion;
			}
		}
		return null;
	}

	/**
	 * Normalizes a meta schema URL for comparison: lower case, "http" scheme and no trailing "#".
	 *
	 * @param url
	 *            the URL
	 * @return the normalized URL
	 */
	private static String normalizeVersionUrl(final String url) {
		String normalizedUrl = url.trim().toLowerCase();
		if (normalizedUrl.startsWith("https://")) {
			normalizedUrl = "http://" + normalizedUrl.substring(8);
		}
		if (normalizedUrl.endsWith("#")) {
			normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
		}
		return normalizedUrl;
	}
}
