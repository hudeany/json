package de.soderer.yaml.exception;

import de.soderer.yaml.data.YamlNode;

public class YamlDuplicateKeyException extends Exception {
	private static final long serialVersionUID = -9038285559080759172L;

	private final YamlNode key;

	public YamlDuplicateKeyException(final YamlNode key) {
		super("Key already exists: '" + key.toString() + "'");

		this.key = key;
	}

	public YamlNode getKey() {
		return key;
	}
}
