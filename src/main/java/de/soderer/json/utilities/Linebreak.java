package de.soderer.json.utilities;

/**
 * Linebreak type of a text.
 * <p>
 * Same content as de.soderer.utilities.LineBreak in the separate library, so both libraries behave identically without depending on each other.
 * </p>
 */
public enum Linebreak {
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
	Linebreak(final String representationString) {
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
	 * Returns the linebreak type with the given name, ignoring case and surrounding whitespace.
	 *
	 * @param lineBreakTypeName
	 *            the name, e.g. "Windows", "unix" or "MAC", may be null
	 * @return the matching linebreak type, or {@link #Unknown} for null or any other name
	 */
	public static Linebreak getLineBreakTypeByName(final String lineBreakTypeName) {
		if (lineBreakTypeName != null) {
			final String trimmedName = lineBreakTypeName.trim();
			for (final Linebreak lineBreakType : values()) {
				if (lineBreakType.name().equalsIgnoreCase(trimmedName)) {
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
	public static Linebreak getLineBreakTypeByString(final String representationString) {
		if (representationString != null) {
			for (final Linebreak lineBreakType : values()) {
				// Unknown and Mixed have no representation string and never match
				if (representationString.equals(lineBreakType.representationString)) {
					return lineBreakType;
				}
			}
		}
		return Unknown;
	}
}
