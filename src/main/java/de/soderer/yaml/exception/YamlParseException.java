package de.soderer.yaml.exception;

/**
 * Thrown when YAML data is invalid. The message contains the position of the error.
 */
public class YamlParseException extends Exception {
	private static final long serialVersionUID = -5557689187489246140L;

	/**
	 * Line of the error, 1 based.
	 */
	private final long line;
	/**
	 * Column of the error, 1 based.
	 */
	private final long column;

	/**
	 * Creates a new exception.
	 *
	 * @param message
	 *            the detail message
	 * @param line
	 *            line of the error, 1 based
	 * @param column
	 *            column of the error, 1 based
	 */
	public YamlParseException(final String message, final long line, final long column) {
		super(message + " (at " + line + ":" + column + ")");

		this.line = line;
		this.column = column;
	}

	/**
	 * Creates a new exception with a cause.
	 *
	 * @param message
	 *            the detail message
	 * @param line
	 *            line of the error, 1 based
	 * @param column
	 *            column of the error, 1 based
	 * @param e
	 *            the cause
	 */
	public YamlParseException(final String message, final long line, final long column, final Exception e) {
		super(message + " (at " + line + ":" + column + ")", e);

		this.line = line;
		this.column = column;
	}

	/**
	 * Returns the line of the error.
	 *
	 * @return the line, 1 based
	 */
	public long getLine() {
		return line;
	}

	/**
	 * Returns the column of the error.
	 *
	 * @return the column, 1 based
	 */
	public long getColumn() {
		return column;
	}
}

