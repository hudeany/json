package de.soderer.json.exception;

/**
 * Thrown by the JSON readers if objects and arrays are nested deeper than the configured maximum.
 * <p>
 * The limit protects against stack overflows caused by maliciously deep nested data, which could
 * otherwise terminate the whole application with a {@link StackOverflowError}.
 * </p>
 */
public class MaximumNestingDepthExceededException extends JsonDataException {
	private static final long serialVersionUID = 4021734459380614732L;

	/**
	 * The configured maximum nesting depth that was exceeded.
	 */
	private final int maximumNestingDepth;

	/**
	 * Creates a new exception.
	 *
	 * @param maximumNestingDepth
	 *            the configured maximum nesting depth that was exceeded
	 * @param erroneousDataChar
	 *            the opening character exceeding the limit
	 * @param lineIndex
	 *            the zero based line index of the error position
	 * @param columnIndex
	 *            the column index of the error position
	 * @param overallCharacterIndex
	 *            the overall character index of the error position
	 */
	public MaximumNestingDepthExceededException(final int maximumNestingDepth, final String erroneousDataChar, final long lineIndex, final long columnIndex, final long overallCharacterIndex) {
		super("Maximum nesting depth of " + maximumNestingDepth + " exceeded", erroneousDataChar, lineIndex, columnIndex, overallCharacterIndex);

		this.maximumNestingDepth = maximumNestingDepth;
	}

	/**
	 * Returns the configured maximum nesting depth that was exceeded.
	 *
	 * @return the maximum nesting depth
	 */
	public int getMaximumNestingDepth() {
		return maximumNestingDepth;
	}
}
