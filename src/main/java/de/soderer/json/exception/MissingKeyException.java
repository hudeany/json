package de.soderer.json.exception;

public class MissingKeyException extends Exception {
	private static final long serialVersionUID = -9038285559080759172L;

	private final String key;

	public MissingKeyException(final String key) {
		super("Expected key is missing: '" + key + "'");

		this.key = key;
	}

	public String getKey() {
		return key;
	}
}
