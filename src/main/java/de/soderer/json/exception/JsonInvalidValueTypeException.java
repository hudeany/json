package de.soderer.json.exception;

/**
 * Thrown when a Java value of an unsupported type is added to JSON data.
 */
public class JsonInvalidValueTypeException extends Exception {
	private static final long serialVersionUID = 3608014202525240836L;

	/**
	 * The unsupported type.
	 */
	private final Class<?> clazz;

	/**
	 * Creates a new exception.
	 *
	 * @param clazz
	 *            the unsupported type
	 */
	public JsonInvalidValueTypeException(final Class<?> clazz) {
		super("Unsupported value type found: '" + clazz + "'");

		this.clazz = clazz;
	}

	/**
	 * Returns the unsupported type.
	 *
	 * @return the type
	 */
	public Class<?> getUnsupportedClass() {
		return clazz;
	}
}
