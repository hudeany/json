package de.soderer.json.schema;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Configuration for reading and applying a JSON schema.
 */
public class JsonSchemaConfiguration {
	/**
	 * Encoding of the JSON schema and data streams.
	 */
	private Charset encoding;
	/**
	 * JSON schema version, null to detect it from "$schema".
	 */
	private JsonSchemaVersion jsonSchemaVersion;
	/**
	 * Whether external schemas referenced by URL may be downloaded.
	 */
	private boolean downloadReferencedSchemas;

	/**
	 * Creates a configuration with UTF-8 encoding, version detection from "$schema" and without
	 * downloads. Download of any additional data is prevented by default, especially because there is
	 * no check for an internet connection beforehand.
	 */
	public JsonSchemaConfiguration() {
		encoding = StandardCharsets.UTF_8;
		jsonSchemaVersion = null;
		downloadReferencedSchemas = false;
	}

	/**
	 * Creates a configuration.
	 *
	 * @param encoding
	 *            encoding of the JSON schema and data streams
	 * @param jsonSchemaVersion
	 *            JSON schema version, null to detect it from "$schema"
	 * @param downloadReferencedSchemas
	 *            true to allow downloading external schemas referenced by URL
	 */
	public JsonSchemaConfiguration(final Charset encoding, final JsonSchemaVersion jsonSchemaVersion, final boolean downloadReferencedSchemas) {
		this.encoding = encoding;
		this.jsonSchemaVersion = jsonSchemaVersion;
		this.downloadReferencedSchemas = downloadReferencedSchemas;
	}

	/**
	 * Returns the encoding of the JSON schema and data streams.
	 *
	 * @return the encoding
	 */
	public Charset getEncoding() {
		return encoding;
	}

	/**
	 * Sets the encoding of the JSON schema and data streams.
	 *
	 * @param encoding
	 *            the encoding
	 */
	public void setEncoding(final Charset encoding) {
		this.encoding = encoding;
	}

	/**
	 * Sets the encoding of the JSON schema and data streams.
	 *
	 * @param newEncoding
	 *            the encoding
	 * @return this configuration for chaining
	 */
	public JsonSchemaConfiguration withEncoding(final Charset newEncoding) {
		setEncoding(newEncoding);
		return this;
	}

	/**
	 * Returns the JSON schema version.
	 *
	 * @return the version, null to detect it from "$schema"
	 */
	public JsonSchemaVersion getJsonSchemaVersion() {
		return jsonSchemaVersion;
	}

	/**
	 * Sets the JSON schema version.
	 *
	 * @param jsonSchemaVersion
	 *            the version, null to detect it from "$schema"
	 */
	public void setJsonSchemaVersion(final JsonSchemaVersion jsonSchemaVersion) {
		this.jsonSchemaVersion = jsonSchemaVersion;
	}

	/**
	 * Sets the JSON schema version.
	 *
	 * @param newJsonSchemaVersion
	 *            the version, null to detect it from "$schema"
	 * @return this configuration for chaining
	 */
	public JsonSchemaConfiguration withJsonSchemaVersion(final JsonSchemaVersion newJsonSchemaVersion) {
		setJsonSchemaVersion(newJsonSchemaVersion);
		return this;
	}

	/**
	 * Returns whether external schemas referenced by URL may be downloaded. Download of any
	 * additional data is prevented by default, especially because there is no check for an internet
	 * connection beforehand.
	 *
	 * @return true, if downloads are allowed
	 */
	public boolean isDownloadReferencedSchemas() {
		return downloadReferencedSchemas;
	}

	/**
	 * Sets whether external schemas referenced by URL may be downloaded.
	 *
	 * @param downloadReferencedSchemas
	 *            true to allow downloads
	 */
	public void setDownloadReferencedSchemas(final boolean downloadReferencedSchemas) {
		this.downloadReferencedSchemas = downloadReferencedSchemas;
	}

	/**
	 * Sets whether external schemas referenced by URL may be downloaded.
	 *
	 * @param newDownloadReferencedSchemas
	 *            true to allow downloads
	 * @return this configuration for chaining
	 */
	public JsonSchemaConfiguration withDownloadReferencedSchemas(final boolean newDownloadReferencedSchemas) {
		setDownloadReferencedSchemas(newDownloadReferencedSchemas);
		return this;
	}
}
