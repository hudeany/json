package de.soderer.json.schema;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Configuration for reading and applying a JSON schema.
 */
public class JsonSchemaConfiguration {
	/**
	 * Default maximum number of errors reported by {@link JsonSchema#validateAll(de.soderer.json.JsonNode)}.
	 */
	public static final int DEFAULT_MAXIMUM_VALIDATION_ERRORS = 100;

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
	 * Whether "format" is validated, null to use the default of the JSON schema version.
	 */
	private Boolean formatAssertion = null;
	/**
	 * Maximum number of errors reported by {@link JsonSchema#validateAll(de.soderer.json.JsonNode)}.
	 */
	private int maximumValidationErrors = DEFAULT_MAXIMUM_VALIDATION_ERRORS;
	/**
	 * Base step limit for the evaluation of regular expressions of the schema against data, see
	 * {@link #setRegexStepLimit(long)}.
	 */
	private long regexStepLimit = JsonSchemaRegexUtilities.DEFAULT_REGEX_STEP_LIMIT;

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

	/**
	 * Returns whether "format" is validated as assertion.
	 *
	 * @return true or false if set explicitly, null for the default of the JSON schema version:
	 *         validated up to draft 7, annotation only since draft 2019-09 (unless the meta schema
	 *         declares the format assertion vocabulary)
	 */
	public Boolean getFormatAssertion() {
		return formatAssertion;
	}

	/**
	 * Sets whether "format" is validated as assertion.
	 *
	 * @param formatAssertion
	 *            true to validate, false to regard it as annotation only, null for the default of
	 *            the JSON schema version
	 */
	public void setFormatAssertion(final Boolean formatAssertion) {
		this.formatAssertion = formatAssertion;
	}

	/**
	 * Sets whether "format" is validated as assertion.
	 *
	 * @param newFormatAssertion
	 *            true to validate, false to regard it as annotation only, null for the default of
	 *            the JSON schema version
	 * @return this configuration for chaining
	 */
	public JsonSchemaConfiguration withFormatAssertion(final Boolean newFormatAssertion) {
		setFormatAssertion(newFormatAssertion);
		return this;
	}

	/**
	 * Returns the maximum number of errors reported by
	 * {@link JsonSchema#validateAll(de.soderer.json.JsonNode)}.
	 *
	 * @return the maximum number of errors
	 */
	public int getMaximumValidationErrors() {
		return maximumValidationErrors;
	}

	/**
	 * Sets the maximum number of errors reported by
	 * {@link JsonSchema#validateAll(de.soderer.json.JsonNode)}. The validation stops after this
	 * number of errors, which protects against huge error lists for large invalid data. Default is
	 * {@link #DEFAULT_MAXIMUM_VALIDATION_ERRORS}.
	 *
	 * @param maximumValidationErrors
	 *            the maximum number of errors, at least 1
	 */
	public void setMaximumValidationErrors(final int maximumValidationErrors) {
		if (maximumValidationErrors < 1) {
			throw new IllegalArgumentException("Maximum number of validation errors must be at least 1");
		}
		this.maximumValidationErrors = maximumValidationErrors;
	}

	/**
	 * Sets the maximum number of errors reported by
	 * {@link JsonSchema#validateAll(de.soderer.json.JsonNode)}, see
	 * {@link #setMaximumValidationErrors(int)}.
	 *
	 * @param newMaximumValidationErrors
	 *            the maximum number of errors, at least 1
	 * @return this configuration for chaining
	 */
	public JsonSchemaConfiguration withMaximumValidationErrors(final int newMaximumValidationErrors) {
		setMaximumValidationErrors(newMaximumValidationErrors);
		return this;
	}

	/**
	 * Returns the base step limit for the evaluation of regular expressions ("pattern",
	 * "patternProperties") against data.
	 *
	 * @return the base step limit, 0 or less for no limit
	 */
	public long getRegexStepLimit() {
		return regexStepLimit;
	}

	/**
	 * Sets the base step limit for the evaluation of regular expressions ("pattern",
	 * "patternProperties") against data. It protects against patterns with catastrophic
	 * backtracking (ReDoS) in schemas from untrusted sources. A step is a character access of the
	 * regex engine; the effective limit is this value plus
	 * {@link JsonSchemaRegexUtilities#REGEX_STEPS_PER_INPUT_CHARACTER} per character of the data
	 * string. Data exceeding the limit is reported as invalid. Default is
	 * {@link JsonSchemaRegexUtilities#DEFAULT_REGEX_STEP_LIMIT}.<br />
	 * <br />
	 * Independent of this limit, input which exceeds the recursion depth of the Java regex engine is
	 * always reported as invalid instead of causing a StackOverflowError.
	 *
	 * @param regexStepLimit
	 *            the base step limit, 0 or less for no limit
	 */
	public void setRegexStepLimit(final long regexStepLimit) {
		this.regexStepLimit = regexStepLimit;
	}

	/**
	 * Sets the base step limit for the evaluation of regular expressions, see
	 * {@link #setRegexStepLimit(long)}.
	 *
	 * @param newRegexStepLimit
	 *            the base step limit, 0 or less for no limit
	 * @return this configuration for chaining
	 */
	public JsonSchemaConfiguration withRegexStepLimit(final long newRegexStepLimit) {
		setRegexStepLimit(newRegexStepLimit);
		return this;
	}
}
