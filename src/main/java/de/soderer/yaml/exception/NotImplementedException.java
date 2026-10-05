package de.soderer.yaml.exception;

/**
 * Thrown for features that are not implemented yet.
 */
public class NotImplementedException extends RuntimeException {
	private static final long serialVersionUID = -7823765940863346460L;

	/**
	 * Creates a new exception with the message "Not implemented".
	 */
	public NotImplementedException() {
		super("Not implemented");
	}

	/**
	 * Creates a new exception.
	 *
	 * @param message
	 *            the detail message
	 */
	public NotImplementedException(final String message) {
		super(message);
	}

	/**
	 * Creates a new exception with a cause.
	 *
	 * @param message
	 *            the detail message
	 * @param e
	 *            the cause
	 */
	public NotImplementedException(final String message, final Exception e) {
		super(message, e);
	}
}

