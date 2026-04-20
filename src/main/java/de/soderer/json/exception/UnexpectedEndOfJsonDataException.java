package de.soderer.json.exception;

public class UnexpectedEndOfJsonDataException extends Exception {
	private static final long serialVersionUID = -6849224600974085174L;

	private final long lineIndex;
	private final long columnIndex;
	private final long overallCharacterIndex;

	public UnexpectedEndOfJsonDataException(final long lineIndex, final long columnIndex, final long overallCharacterIndex) {
		super("Unexpected end of json data in line " + (lineIndex + 1) + " at column " + columnIndex + " at overall index " + overallCharacterIndex);

		this.lineIndex = lineIndex;
		this.columnIndex = columnIndex;
		this.overallCharacterIndex = overallCharacterIndex;
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
