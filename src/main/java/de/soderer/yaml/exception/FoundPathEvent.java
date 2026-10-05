package de.soderer.yaml.exception;

/**
 * Internal event thrown by {@link de.soderer.yaml.YamlReader} to stop parsing when the searched
 * path is reached.
 */
public class FoundPathEvent extends RuntimeException {
	private static final long serialVersionUID = 4230530013541561131L;

	/**
	 * Creates a new event.
	 *
	 * @param message
	 *            the detail message
	 */
	public FoundPathEvent(final String message) {
		super(message);
	}
}
