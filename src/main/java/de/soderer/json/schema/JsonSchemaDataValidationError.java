package de.soderer.json.schema;

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
	 * Returns the path of the invalid data.
	 *
	 * @return the path, may be null
	 */
	public JsonPath getJsonDataPath() {
		return jsonDataPath;
	}

	/**
	 * Returns the message prefixed by "Invalid JSON data" and followed by the data path.
	 */
	@Override
	public String getMessage() {
		return "Invalid JSON data: " + super.getMessage() + (jsonDataPath == null ? "" : " at JSON path: " + jsonDataPath);
	}
}
