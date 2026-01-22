package de.soderer.yaml.data;

import de.soderer.json.utilities.Utilities;

public enum YamlMultilineScalarType {
	/**
	 * "|" or "|-" or "|+"<br />
	 * Keep the lines separated by a linebreak<br />
	 * Leading blanks after first line are kept after the indentation level of the first line<br />
	 */
	LITERAL,

	/**
	 * ">" or ">-" or ">+"<br />
	 * Fold all the lines to a single line, each separated by an additional single blank<br />
	 */
	FOLDED;

	public static YamlMultilineScalarType getYamlMultilineScalarType(final String multilineTypeSign) throws Exception {
		if (multilineTypeSign != null && multilineTypeSign.startsWith("|")) {
			return YamlMultilineScalarType.LITERAL;
		} else if (multilineTypeSign != null && multilineTypeSign.startsWith(">")) {
			return YamlMultilineScalarType.FOLDED;
		} else {
			throw new Exception("Invalid multiline scalar type: '" + multilineTypeSign + "'");
		}
	}

	public static int getYamlMultilineScalarIndentationIndicator(String multilineTypeSign) throws Exception {
		if (multilineTypeSign != null && (multilineTypeSign.startsWith("|") || multilineTypeSign.startsWith(">"))) {
			multilineTypeSign = multilineTypeSign.substring(1);
		} else {
			throw new Exception("Invalid multiline scalar type: '" + multilineTypeSign + "'");
		}

		if (multilineTypeSign.startsWith("-")) {
			multilineTypeSign = multilineTypeSign.substring(1);
		} else if ( multilineTypeSign.startsWith("+")) {
			multilineTypeSign = multilineTypeSign.substring(1);
		} else if (multilineTypeSign.startsWith(" ")) {
			multilineTypeSign = multilineTypeSign.substring(1);
		}

		if (Utilities.isBlank(multilineTypeSign)) {
			return 0;
		} else {
			int indentationIndicator;
			try {
				indentationIndicator = Integer.parseInt(multilineTypeSign.trim());
			} catch (@SuppressWarnings("unused") final NumberFormatException e) {
				throw new Exception("Invalid multiline scalar indentation indicator: '" + multilineTypeSign + "'");
			}
			if (indentationIndicator <= 0) {
				throw new Exception("Invalid multiline scalar indentation indicator: '" + multilineTypeSign + "'");
			} else {
				return indentationIndicator;
			}
		}
	}
}
