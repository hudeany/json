package de.soderer.yaml.data;

public class YamlToken {
	private final YamlTokenType type;
	private final String value;
	private final long line;
	private final long column;

	public YamlToken(final YamlTokenType type, final String value, final long line, final long column) {
		this.type = type;
		this.value = value;
		this.line = line;
		this.column = column;
	}

	public YamlTokenType getType() {
		return type;
	}

	public String getValue() {
		return value;
	}

	public long getLine() {
		return line;
	}

	public long getColumn() {
		return column;
	}

	@Override
	public String toString() {
		return type + (value != null ? "(" + value + ")" : "") + " at " + line + ":" + column;
	}
}
