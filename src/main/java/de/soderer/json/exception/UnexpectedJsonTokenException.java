package de.soderer.json.exception;

import de.soderer.json.JsonReader.JsonToken;

public class UnexpectedJsonTokenException extends Exception {
	private static final long serialVersionUID = -6849224600974085174L;

	private final JsonToken erroneousToken;
	private final long lineIndex;
	private final long columnIndex;
	private final long overallCharacterIndex;

	public UnexpectedJsonTokenException(final JsonToken erroneousToken, final long lineIndex, final long columnIndex, final long overallCharacterIndex) {
		super("Unexpected Json token '" + (erroneousToken == null ? "NULL" : erroneousToken.name()) + "' in line " + (lineIndex + 1) + " at column " + columnIndex + " at overall index " + overallCharacterIndex);

		this.erroneousToken = erroneousToken;
		this.lineIndex = lineIndex;
		this.columnIndex = columnIndex;
		this.overallCharacterIndex = overallCharacterIndex;
	}

	public JsonToken getErroneousToken() {
		return erroneousToken;
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
