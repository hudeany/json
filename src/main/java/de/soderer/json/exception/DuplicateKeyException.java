package de.soderer.json.exception;

/**
 * Thrown when a key is added to a JSON object or YAML mapping that already contains it.
 */
public class DuplicateKeyException extends Exception {
	private static final long serialVersionUID = -9038285559080759172L;

	/**
	 * The duplicate key.
	 */
	private final String key;

	/**
	 * Creates a new exception.
	 *
	 * @param key
	 *            the duplicate key
	 */
	public DuplicateKeyException(final String key) {
		super("Key already exists: '" + key + "'");

		this.key = key;
	}

	/**
	 * Returns the duplicate key.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key;
	}
}
