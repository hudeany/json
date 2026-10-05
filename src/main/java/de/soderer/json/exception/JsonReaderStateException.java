package de.soderer.json.exception;

/**
 * Thrown when a JsonReader method is called in a state that does not allow it, e.g.
 * reading all data after reading tokens.
 */
public class JsonReaderStateException extends Exception {
	private static final long serialVersionUID = -8358642025701366115L;

	/**
	 * Creates a new exception.
	 *
	 * @param errormessage
	 *            the detail message
	 */
	public JsonReaderStateException(final String errormessage) {
		super(errormessage);
	}
}
