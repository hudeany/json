package de.soderer.yaml.data;


/**
 * Style of a block scalar: literal ("|") or folded ("&gt;").
 */
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

	/**
	 * Returns the style of a block scalar header like "|-" or "&gt;+2".
	 *
	 * @param multilineTypeSign
	 *            the block scalar header
	 * @return the style
	 * @throws Exception
	 *             if the header starts with neither "|" nor "&gt;"
	 */
	public static YamlMultilineScalarType getYamlMultilineScalarType(final String multilineTypeSign) throws Exception {
		if (multilineTypeSign != null && multilineTypeSign.startsWith("|")) {
			return YamlMultilineScalarType.LITERAL;
		} else if (multilineTypeSign != null && multilineTypeSign.startsWith(">")) {
			return YamlMultilineScalarType.FOLDED;
		} else {
			throw new Exception("Invalid multiline scalar type: '" + multilineTypeSign + "'");
		}
	}

	/**
	 * Returns the indentation indicator of a block scalar header like "|2" or "&gt;-4". The
	 * indicators may be given in any order.
	 *
	 * @param multilineTypeSign
	 *            the block scalar header
	 * @return the indentation indicator 1 to 9, or 0 if none is given
	 * @throws Exception
	 *             if the header is invalid
	 */
	public static int getYamlMultilineScalarIndentationIndicator(final String multilineTypeSign) throws Exception {
		final String indicators = getHeaderIndicators(multilineTypeSign);
		for (final char indicatorChar : indicators.toCharArray()) {
			if (indicatorChar >= '1' && indicatorChar <= '9') {
				return indicatorChar - '0';
			}
		}
		return 0;
	}

	/**
	 * Returns the indicators of a block scalar header after the "|" or "&gt;" and checks them: at
	 * most one chomping indicator ("-" or "+") and one indentation indicator ("1" to "9").
	 *
	 * @param multilineTypeSign
	 *            the block scalar header
	 * @return the indicators
	 * @throws Exception
	 *             if the header is invalid
	 */
	static String getHeaderIndicators(final String multilineTypeSign) throws Exception {
		if (multilineTypeSign == null || !(multilineTypeSign.startsWith("|") || multilineTypeSign.startsWith(">"))) {
			throw new Exception("Invalid multiline scalar type: '" + multilineTypeSign + "'");
		}
		// Up to two indicators in any order: one chomping indicator ('-' or '+') and one indentation indicator ('1' to '9')
		final String indicators = multilineTypeSign.substring(1).trim();
		boolean chompingFound = false;
		boolean indentationFound = false;
		for (final char indicatorChar : indicators.toCharArray()) {
			if ((indicatorChar == '-' || indicatorChar == '+') && !chompingFound) {
				chompingFound = true;
			} else if (indicatorChar >= '1' && indicatorChar <= '9' && !indentationFound) {
				indentationFound = true;
			} else {
				throw new Exception("Invalid multiline scalar header: '" + multilineTypeSign + "'");
			}
		}
		return indicators;
	}
}
