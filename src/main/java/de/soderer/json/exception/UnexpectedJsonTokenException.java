package de.soderer.json.exception;

import de.soderer.json.JsonReader.JsonToken;

/**
 * Thrown when JSON data contains a token at a position where it is not allowed.
 */
public class UnexpectedJsonTokenException extends Exception {
	private static final long serialVersionUID = -6849224600974085174L;

	/**
	 * The unexpected token.
	 */
	private final JsonToken erroneousToken;
	/**
	 * Line of the error, 0 based.
	 */
	private final long lineIndex;
	/**
	 * Column of the error within its line.
	 */
	private final long columnIndex;
	/**
	 * Index of the error within the whole data.
	 */
	private final long overallCharacterIndex;

	/**
	 * Creates a new exception.
	 *
	 * @param erroneousToken
	 *            the unexpected token, may be null
	 * @param lineIndex
	 *            line of the error, 0 based
	 * @param columnIndex
	 *            column of the error within its line
	 * @param overallCharacterIndex
	 *            index of the error within the whole data
	 */
	public UnexpectedJsonTokenException(final JsonToken erroneousToken, final long lineIndex, final long columnIndex, final long overallCharacterIndex) {
		super("Unexpected Json token '" + (erroneousToken == null ? "NULL" : erroneousToken.name()) + "' in line " + (lineIndex + 1) + " at column " + columnIndex + " at overall index " + overallCharacterIndex);

		this.erroneousToken = erroneousToken;
		this.lineIndex = lineIndex;
		this.columnIndex = columnIndex;
		this.overallCharacterIndex = overallCharacterIndex;
	}

	/**
	 * Returns the unexpected token.
	 *
	 * @return the token, may be null
	 */
	public JsonToken getErroneousToken() {
		return erroneousToken;
	}

	/**
	 * Returns the line of the error.
	 *
	 * @return the line, 0 based (messages show it 1 based)
	 */
	public long getLineIndex() {
		return lineIndex;
	}

	/**
	 * Returns the column of the error within its line.
	 *
	 * @return the column
	 */
	public long getColumnIndex() {
		return columnIndex;
	}

	/**
	 * Returns the index of the error within the whole data.
	 *
	 * @return the character index
	 */
	public long getOverallCharacterIndex() {
		return overallCharacterIndex;
	}
}
