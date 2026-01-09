package de.soderer.json.exception;

public class JsonInvalidValueTypeException extends Exception {
	private static final long serialVersionUID = 3608014202525240836L;

	private final Class<?> clazz;

	public JsonInvalidValueTypeException(final Class<?> clazz) {
		super("Unsupported value type found: '" + clazz + "'");

		this.clazz = clazz;
	}

	public Class<?> getUnsupportedClass() {
		return clazz;
	}
}
