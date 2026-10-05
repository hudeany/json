package de.soderer.json.path;

/**
 * Thrown when a JSON path does not exist in JSON data or cannot be evaluated.
 */
public class JsonPathException extends Exception {
	private static final long serialVersionUID = -3686896720369394469L;

	/**
	 * The path that caused the error.
	 */
	private final JsonPath jsonPath;

	/**
	 * Creates a new exception.
	 *
	 * @param message
	 *            the detail message
	 * @param jsonPath
	 *            the path that caused the error, may be null
	 */
	public JsonPathException(final String message, final JsonPath jsonPath) {
		super(message);
		this.jsonPath = jsonPath;
	}

	/**
	 * Returns the path that caused the error.
	 *
	 * @return the path, may be null
	 */
	public JsonPath getJsonPath() {
		return jsonPath;
	}
}
