package de.soderer.json.exception;

/**
 * Thrown when JSON data ends while objects or arrays are still open.
 */
public class UnexpectedEndOfJsonDataException extends Exception {
	private static final long serialVersionUID = -6849224600974085174L;

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
	 * @param lineIndex
	 *            line of the error, 0 based
	 * @param columnIndex
	 *            column of the error within its line
	 * @param overallCharacterIndex
	 *            index of the error within the whole data
	 */
	public UnexpectedEndOfJsonDataException(final long lineIndex, final long columnIndex, final long overallCharacterIndex) {
		super("Unexpected end of json data in line " + (lineIndex + 1) + " at column " + columnIndex + " at overall index " + overallCharacterIndex);

		this.lineIndex = lineIndex;
		this.columnIndex = columnIndex;
		this.overallCharacterIndex = overallCharacterIndex;
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
