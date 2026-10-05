package de.soderer.json.exception;

/**
 * Thrown when a JsonWriter method does not fit the current write position, e.g. closing an
 * array while an object is open.
 */
public class JsonWriterStateException extends Exception {
	private static final long serialVersionUID = 664685088425241771L;

	/**
	 * Creates a new exception.
	 *
	 * @param errormessage
	 *            the detail message
	 */
	public JsonWriterStateException(final String errormessage) {
		super(errormessage);
	}
}
