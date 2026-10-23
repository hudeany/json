package de.soderer.json.utilities;

/**
 * Linebreak type of a text.
 * <p>
 * Same content as de.soderer.utilities.LineBreak in the separate library, so both libraries behave identically without depending on each other.
 * </p>
 */
public enum LineBreak {
	/**
	 * No linebreak or not known
	 */
	Unknown(null),

	/**
	 * Multiple linebreak types
	 */
	Mixed(null),

	/**
	 * Unix/Linux linebreak ("\n")
	 */
	Unix("\n"),

	/**
	 * Mac/Apple linebreak ("\r")
	 */
	Mac("\r"),

	/**
	 * Windows linebreak ("\r\n")
	 */
	Windows("\r\n");

	/** The linebreak characters, or null for Unknown and Mixed. */
	private final String representationString;

	/**
	 * Creates a linebreak type.
	 *
	 * @param representationString
	 *            the linebreak characters, or null for Unknown and Mixed
	 */
	LineBreak(final String representationString) {
		this.representationString = representationString;
	}

	/**
	 * Returns the linebreak characters.
	 *
	 * @return the linebreak characters, or null for Unknown and Mixed
	 */
	public String getRepresentationString() {
		return representationString;
	}

	/**
	 * Returns the linebreak characters, see {@link #getRepresentationString()}.
	 *
	 * @return the linebreak characters, or null for Unknown and Mixed
	 */
	@Override
	public String toString() {
		return representationString;
	}

	/**
	 * Returns the linebreak type with the given name, ignoring case and surrounding whitespace. Only
	 * the concrete linebreak types Windows, Unix and Mac can be selected by name, because Unknown and
	 * Mixed have no linebreak characters to use.
	 *
	 * @param lineBreakTypeName
	 *            the name "Windows", "Unix" or "Mac" in any case, may be null
	 * @return the matching linebreak type, or {@link #Unknown} for null or any other name
	 */
	public static LineBreak getLineBreakTypeByName(final String lineBreakTypeName) {
		if (lineBreakTypeName != null) {
			final String trimmedName = lineBreakTypeName.trim();
			for (final LineBreak lineBreakType : values()) {
				if (lineBreakType.representationString != null && lineBreakType.name().equalsIgnoreCase(trimmedName)) {
					return lineBreakType;
				}
			}
		}
		return Unknown;
	}

	/**
	 * Returns the linebreak type for the given linebreak characters.
	 *
	 * @param representationString
	 *            the linebreak characters, e.g. "\r\n", may be null
	 * @return the matching linebreak type, or {@link #Unknown} for null or any other characters
	 */
	public static LineBreak getLineBreakTypeByString(final String representationString) {
		if (representationString != null) {
			for (final LineBreak lineBreakType : values()) {
				// Unknown and Mixed have no representation string and never match
				if (representationString.equals(lineBreakType.representationString)) {
					return lineBreakType;
				}
			}
		}
		return Unknown;
	}
}
