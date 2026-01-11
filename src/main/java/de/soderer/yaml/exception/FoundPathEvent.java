package de.soderer.yaml.exception;

public class FoundPathEvent extends RuntimeException {
	private static final long serialVersionUID = 4230530013541561131L;

	public FoundPathEvent(final String message) {
		super(message);
	}
}
