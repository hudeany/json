package de.soderer.json.schema;

/**
 * Thrown when a JSON schema definition is invalid. The message contains the path within the
 * schema.
 */
public class JsonSchemaDefinitionError extends Exception {
	private static final long serialVersionUID = 571902904309032324L;

	/**
	 * Path of the error within the JSON schema.
	 */
	private final JsonSchemaPath jsonSchemaPath;

	/**
	 * Creates a new error.
	 *
	 * @param message
	 *            the detail message
	 * @param jsonSchemaPath
	 *            path of the error within the JSON schema, may be null
	 */
	public JsonSchemaDefinitionError(final String message, final JsonSchemaPath jsonSchemaPath) {
		super(message);

		this.jsonSchemaPath = jsonSchemaPath;
	}

	/**
	 * Creates a new error with a cause.
	 *
	 * @param message
	 *            the detail message
	 * @param jsonSchemaPath
	 *            path of the error within the JSON schema, may be null
	 * @param e
	 *            the cause
	 */
	public JsonSchemaDefinitionError(final String message, final JsonSchemaPath jsonSchemaPath, final Exception e) {
		super(message, e);

		this.jsonSchemaPath = jsonSchemaPath;
	}

	/**
	 * Returns the path of the error within the JSON schema.
	 *
	 * @return the path, may be null
	 */
	public JsonSchemaPath getJsonSchemaPath() {
		return jsonSchemaPath;
	}

	/**
	 * Returns the message prefixed by "Invalid JSON schema definition" and followed by the schema
	 * path.
	 */
	@Override
	public String getMessage() {
		return "Invalid JSON schema definition: " + super.getMessage() + (jsonSchemaPath == null ? "" : " at JSON schema path: " + jsonSchemaPath);
	}
}
