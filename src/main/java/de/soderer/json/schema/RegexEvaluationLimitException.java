package de.soderer.json.schema;

/**
 * Thrown if the evaluation of a regular expression against some input exceeds its step limit
 * (catastrophic backtracking, ReDoS) or the recursion depth of the Java regex engine.
 */
public class RegexEvaluationLimitException extends Exception {
	private static final long serialVersionUID = 4730513290284417384L;

	/**
	 * Creates a new exception.
	 *
	 * @param message
	 *            the reason
	 */
	public RegexEvaluationLimitException(final String message) {
		super(message);
	}
}
