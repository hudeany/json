package de.soderer.yaml.data;

public enum YamlMultilineScalarChompingType {
	/**
	 * ">" or "|"<br />
	 * Add a single trailing blank line<br />
	 */
	CLIP,

	/**
	 * ">-" or "|-"<br />
	 * Remove all the trailing blank lines<br />
	 */
	STRIP,

	/**
	 * ">+" or "|+"<br />
	 * Keep all the trailing blank lines<br />
	 */
	KEEP;

	public static YamlMultilineScalarChompingType getYamlMultilineScalarChompingType(String multilineTypeSign) throws Exception {
		if (multilineTypeSign != null && (multilineTypeSign.startsWith("|") || multilineTypeSign.startsWith(">"))) {
			multilineTypeSign = multilineTypeSign.substring(1);
		} else {
			throw new Exception("Invalid multiline scalar type: '" + multilineTypeSign + "'");
		}

		if (multilineTypeSign.startsWith("-")) {
			return YamlMultilineScalarChompingType.STRIP;
		} else if (multilineTypeSign.startsWith("+")) {
			return YamlMultilineScalarChompingType.KEEP;
		} else {
			return YamlMultilineScalarChompingType.CLIP;
		}
	}
}
