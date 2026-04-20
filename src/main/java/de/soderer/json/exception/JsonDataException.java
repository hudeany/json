package de.soderer.json.exception;

public class JsonDataException extends Exception {
	private static final long serialVersionUID = 797537731999935894L;

	private final String erroneousData;
	private final long lineIndex;
	private final long columnIndex;
	private final long overallCharacterIndex;

	public JsonDataException(final char erroneousDataChar, final long lineIndex, final long columnIndex, final long overallCharacterIndex) {
		super("Invalid json data '" + Character.toString(erroneousDataChar) + "' in line " + (lineIndex + 1) + " at column " + columnIndex + " at overall index " + overallCharacterIndex);

		erroneousData = Character.toString(erroneousDataChar);
		this.lineIndex = lineIndex;
		this.columnIndex = columnIndex;
		this.overallCharacterIndex = overallCharacterIndex;
	}

	public JsonDataException(final String erroneousData, final long lineIndex, final long columnIndex, final long overallCharacterIndex) {
		super("Invalid json data '" + erroneousData + "' in line " + (lineIndex + 1) + " at column " + columnIndex + " at overall index " + overallCharacterIndex);

		this.erroneousData = erroneousData;
		this.lineIndex = lineIndex;
		this.columnIndex = columnIndex;
		this.overallCharacterIndex = overallCharacterIndex;
	}

	public String getErroneousData() {
		return erroneousData;
	}

	public long getLineIndex() {
		return lineIndex;
	}

	public long getColumnIndex() {
		return columnIndex;
	}

	public long getOverallCharacterIndex() {
		return overallCharacterIndex;
	}
}
