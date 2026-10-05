package de.soderer.json.exception;

/**
 * Thrown when JSON data contains invalid characters or values.
 */
public class JsonDataException extends Exception {
	private static final long serialVersionUID = 797537731999935894L;

	/**
	 * The invalid data.
	 */
	private final String erroneousData;
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
	 * Creates a new exception for an invalid character.
	 *
	 * @param erroneousDataChar
	 *            the invalid character
	 * @param lineIndex
	 *            line of the error, 0 based
	 * @param columnIndex
	 *            column of the error within its line
	 * @param overallCharacterIndex
	 *            index of the error within the whole data
	 */
	public JsonDataException(final char erroneousDataChar, final long lineIndex, final long columnIndex, final long overallCharacterIndex) {
		super("Invalid json data '" + Character.toString(erroneousDataChar) + "' in line " + (lineIndex + 1) + " at column " + columnIndex + " at overall index " + overallCharacterIndex);

		erroneousData = Character.toString(erroneousDataChar);
		this.lineIndex = lineIndex;
		this.columnIndex = columnIndex;
		this.overallCharacterIndex = overallCharacterIndex;
	}

	/**
	 * Creates a new exception for invalid data.
	 *
	 * @param erroneousData
	 *            the invalid data
	 * @param lineIndex
	 *            line of the error, 0 based
	 * @param columnIndex
	 *            column of the error within its line
	 * @param overallCharacterIndex
	 *            index of the error within the whole data
	 */
	public JsonDataException(final String erroneousData, final long lineIndex, final long columnIndex, final long overallCharacterIndex) {
		super("Invalid json data '" + erroneousData + "' in line " + (lineIndex + 1) + " at column " + columnIndex + " at overall index " + overallCharacterIndex);

		this.erroneousData = erroneousData;
		this.lineIndex = lineIndex;
		this.columnIndex = columnIndex;
		this.overallCharacterIndex = overallCharacterIndex;
	}

	/**
	 * Returns the invalid data.
	 *
	 * @return the invalid data
	 */
	public String getErroneousData() {
		return erroneousData;
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
