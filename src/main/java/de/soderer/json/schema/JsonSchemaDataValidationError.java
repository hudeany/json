package de.soderer.json.schema;

import java.util.ArrayList;
import java.util.List;

import de.soderer.json.path.JsonPath;

/**
 * Thrown when JSON data is not valid against a JSON schema. The message contains the path within
 * the data.
 */
public class JsonSchemaDataValidationError extends Exception {
	private static final long serialVersionUID = -4849599671599546633L;

	/**
	 * Path of the invalid data.
	 */
	private final JsonPath jsonDataPath;

	/**
	 * Path of the schema keyword that failed, null if unknown.
	 */
	private JsonSchemaPath jsonSchemaPath = null;

	/**
	 * Whether this error only signals, that the errors of a node were already collected.
	 */
	private boolean collectedMarker = false;

	/**
	 * Creates a new error.
	 *
	 * @param message
	 *            the detail message
	 * @param jsonDataPath
	 *            path of the invalid data, may be null
	 */
	public JsonSchemaDataValidationError(final String message, final JsonPath jsonDataPath) {
		super(message);

		this.jsonDataPath = jsonDataPath;
	}

	/**
	 * Creates a new error with a cause, e.g. the error of a failing sub schema.
	 *
	 * @param message
	 *            the detail message
	 * @param jsonDataPath
	 *            path of the invalid data, may be null
	 * @param e
	 *            the cause
	 */
	public JsonSchemaDataValidationError(final String message, final JsonPath jsonDataPath, final Exception e) {
		super(message, e);

		this.jsonDataPath = jsonDataPath;
	}

	/**
	 * Creates an error that signals invalid data, whose errors were already collected by
	 * {@link JsonSchemaValidationCollector}. It is not reported itself.
	 *
	 * @param jsonDataPath
	 *            path of the invalid data
	 * @return the error
	 */
	public static JsonSchemaDataValidationError createCollectedMarker(final JsonPath jsonDataPath) {
		final JsonSchemaDataValidationError error = new JsonSchemaDataValidationError("Errors were collected", jsonDataPath);
		error.collectedMarker = true;
		return error;
	}

	/**
	 * Checks whether this error only signals, that the errors of a node were already collected.
	 *
	 * @return true for a marker created by {@link #createCollectedMarker(JsonPath)}
	 */
	public boolean isCollectedMarker() {
		return collectedMarker;
	}

	/**
	 * Returns the path of the invalid data.
	 *
	 * @return the path, may be null
	 */
	public JsonPath getJsonDataPath() {
		return jsonDataPath;
	}

	/**
	 * Returns the path of the schema keyword that failed, e.g. ".properties.name.type". For a
	 * keyword within a referenced schema the path continues after the "$ref".
	 *
	 * @return the path, may be null
	 */
	public JsonSchemaPath getJsonSchemaPath() {
		return jsonSchemaPath;
	}

	/**
	 * Sets the path of the schema keyword that failed, if it is not set yet. The innermost keyword
	 * sets it first, so outer keywords do not overwrite it.
	 *
	 * @param newJsonSchemaPath
	 *            the path of the schema keyword
	 */
	public void setJsonSchemaPathIfMissing(final JsonSchemaPath newJsonSchemaPath) {
		if (jsonSchemaPath == null) {
			jsonSchemaPath = newJsonSchemaPath;
		}
	}

	/**
	 * Returns the errors of the sub schemas that caused this error, e.g. the errors of all options
	 * of a failing "anyOf".
	 *
	 * @return the errors of the sub schemas, empty if there are none
	 */
	public List<JsonSchemaDataValidationError> getSubSchemaErrors() {
		final List<JsonSchemaDataValidationError> subSchemaErrors = new ArrayList<>();
		if (getCause() instanceof final JsonSchemaDataValidationError causeError) {
			subSchemaErrors.add(causeError);
		}
		for (final Throwable suppressed : getSuppressed()) {
			if (suppressed instanceof final JsonSchemaDataValidationError suppressedError) {
				subSchemaErrors.add(suppressedError);
			}
		}
		return subSchemaErrors;
	}

	/**
	 * Returns the message prefixed by "Invalid JSON data" and followed by the data path.
	 */
	@Override
	public String getMessage() {
		return "Invalid JSON data: " + super.getMessage() + (jsonDataPath == null ? "" : " at JSON path: " + jsonDataPath);
	}
}
