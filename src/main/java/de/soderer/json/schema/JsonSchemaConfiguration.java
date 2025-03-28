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

	public JsonSchemaConfiguration setEncoding(final Charset encoding) {
		this.encoding = encoding;
		return this;
	}

	public JsonSchemaVersion getJsonSchemaVersion() {
		return jsonSchemaVersion;
	}

	public JsonSchemaConfiguration setJsonSchemaVersion(final JsonSchemaVersion jsonSchemaVersion) {
		this.jsonSchemaVersion = jsonSchemaVersion;
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
	public JsonSchemaConfiguration setDownloadReferencedSchemas(final boolean downloadReferencedSchemas) {
		this.downloadReferencedSchemas = downloadReferencedSchemas;
		return this;
	}
}
