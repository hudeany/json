package de.soderer.yaml.exception;

public class YamlParseException extends Exception {
	private static final long serialVersionUID = -5557689187489246140L;

	private final long line;
	private final long column;

	public YamlParseException(final String message, final long line, final long column) {
		super(message + " (at " + line + ":" + column + ")");

		this.line = line;
		this.column = column;
	}

	public YamlParseException(final String message, final long line, final long column, final Exception e) {
		super(message + " (at " + line + ":" + column + ")", e);

		this.line = line;
		this.column = column;
	}

	public long getLine() {
		return line;
	}

	public long getColumn() {
		return column;
	}
}

