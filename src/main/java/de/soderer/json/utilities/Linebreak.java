package de.soderer.json.utilities;

/**
 * Enum to represent a linebreak type of an text
 */
public enum Linebreak {
	/**
	 * No linebreak
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

	/** The line break characters, or null for Unknown and Mixed. */
	private final String representationString;

	/**
	 * Returns the line break characters.
	 *
	 * @return the line break characters, or null for Unknown and Mixed
	 */
	@Override
	public String toString() {
		return representationString;
	}

	/**
	 * Creates a line break type.
	 *
	 * @param representationString
	 *            the line break characters
	 */
	Linebreak(final String representationString) {
		this.representationString = representationString;
	}

	/**
	 * Returns the line break type with the given name, ignoring case.
	 *
	 * @param lineBreakTypeName
	 *            the name, e.g. "unix" or "Windows"
	 * @return the line break type, Unix if the name is null
	 * @throws IllegalArgumentException
	 *             if the name is unknown
	 */
	public static Linebreak getLineBreakTypeByName(final String lineBreakTypeName) {
		if (lineBreakTypeName == null) {
			return Unix;
		}
		for (final Linebreak linebreakType : Linebreak.values()) {
			if (linebreakType.name().equalsIgnoreCase(lineBreakTypeName)) {
				return linebreakType;
			}
		}
		throw new IllegalArgumentException("Unknown lineBreakType name: " + lineBreakTypeName);
	}

	/**
	 * Returns the line break type for the given line break characters.
	 *
	 * @param representationString
	 *            the line break characters, e.g. "\r\n"
	 * @return the line break type
	 * @throws IllegalArgumentException
	 *             if the characters are no known line break
	 */
	public static Linebreak getLineBreakTypeByString(final String representationString) {
		for (final Linebreak linebreakType : Linebreak.values()) {
			if (linebreakType.toString().equals(representationString)) {
				return linebreakType;
			}
		}
		throw new IllegalArgumentException("Unknown lineBreakType string");
	}
}
