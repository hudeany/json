package de.soderer.yaml.exception;

public class NotImplementedException extends RuntimeException {
	private static final long serialVersionUID = -7823765940863346460L;

	public NotImplementedException() {
		super("Not implemented");
	}

	public NotImplementedException(final String message) {
		super(message);
	}

	public NotImplementedException(final String message, final Exception e) {
		super(message, e);
	}
}

