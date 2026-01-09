package de.soderer.json.exception;

public class DuplicateKeyException extends Exception {
	private static final long serialVersionUID = -9038285559080759172L;

	private final String key;

	public DuplicateKeyException(final String key) {
		super("Key already exists: '" + key + "'");

		this.key = key;
	}

	public String getKey() {
		return key;
	}
}
