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

	private final String representationString;

	@Override
	public String toString() {
		return representationString;
	}

	Linebreak(final String representationString) {
		this.representationString = representationString;
	}

	public static Linebreak getLineBreakTypeByName(final String lineBreakTypeName) {
		if (lineBreakTypeName == null) {
			return Unix;
		}
		for (Linebreak linebreakType : Linebreak.values()) {
			if (linebreakType.name().equalsIgnoreCase(lineBreakTypeName)) {
				return linebreakType;
			}
		}
		throw new RuntimeException("Unknown lineBreakType name: " + lineBreakTypeName);
	}

	public static Linebreak getLineBreakTypeByString(final String representationString) {
		for (Linebreak linebreakType : Linebreak.values()) {
			if (linebreakType.toString().equals(representationString)) {
				return linebreakType;
			}
		}
		throw new RuntimeException("Unknown lineBreakType string");
	}
}
