package de.soderer.json.exception;

/**
 * Thrown when a property value of a JSON object is replaced, but the key does not exist.
 */
public class MissingKeyException extends Exception {
	private static final long serialVersionUID = -9038285559080759172L;

	/**
	 * The missing key.
	 */
	private final String key;

	/**
	 * Creates a new exception.
	 *
	 * @param key
	 *            the missing key
	 */
	public MissingKeyException(final String key) {
		super("Expected key is missing: '" + key + "'");

		this.key = key;
	}

	/**
	 * Returns the missing key.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key;
	}
}
