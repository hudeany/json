package de.soderer.json.schema;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class JsonSchemaConfiguration {
	private Charset encoding;
	private JsonSchemaVersion jsonSchemaVersion;
	private boolean downloadReferencedSchemas;

	/**
	 * Download of any additional data is prevented by default.<br />
	 * Especially because there is no check for internet connection in forehand.<br />
	 */
	public JsonSchemaConfiguration() {
		encoding = StandardCharsets.UTF_8;
		jsonSchemaVersion = null;
		downloadReferencedSchemas = false;
	}

	public JsonSchemaConfiguration(final Charset encoding, final JsonSchemaVersion jsonSchemaVersion, final boolean downloadReferencedSchemas) {
		this.encoding = encoding;
		this.jsonSchemaVersion = jsonSchemaVersion;
		this.downloadReferencedSchemas = downloadReferencedSchemas;
	}

	public Charset getEncoding() {
		return encoding;
	}

	public void setEncoding(final Charset encoding) {
		this.encoding = encoding;
	}

	public JsonSchemaConfiguration withEncoding(final Charset newEncoding) {
		setEncoding(newEncoding);
		return this;
	}

	public JsonSchemaVersion getJsonSchemaVersion() {
		return jsonSchemaVersion;
	}

	public void setJsonSchemaVersion(final JsonSchemaVersion jsonSchemaVersion) {
		this.jsonSchemaVersion = jsonSchemaVersion;
	}

	public JsonSchemaConfiguration withJsonSchemaVersion(final JsonSchemaVersion newJsonSchemaVersion) {
		setJsonSchemaVersion(newJsonSchemaVersion);
		return this;
	}

	/**
	 * Download of any additional data is prevented by default.<br />
	 * Especially because there is no check for internet connection in forehand.<br />
	 */
	public boolean isDownloadReferencedSchemas() {
		return downloadReferencedSchemas;
	}

	/**
	 * Download of any additional data is prevented by default.<br />
	 * Especially because there is no check for internet connection in forehand.<br />
	 */
	public void setDownloadReferencedSchemas(final boolean downloadReferencedSchemas) {
		this.downloadReferencedSchemas = downloadReferencedSchemas;
	}

	public JsonSchemaConfiguration withDownloadReferencedSchemas(final boolean newDownloadReferencedSchemas) {
		setDownloadReferencedSchemas(newDownloadReferencedSchemas);
		return this;
	}
}
