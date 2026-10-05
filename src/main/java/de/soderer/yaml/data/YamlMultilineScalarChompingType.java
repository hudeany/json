package de.soderer.yaml.data;

/**
 * Chomping of the trailing linebreaks of a block scalar (the "-" or "+" indicator).
 */
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

	/**
	 * Returns the chomping type of a block scalar header like "|-" or "&gt;+2".
	 *
	 * @param multilineTypeSign
	 *            the block scalar header
	 * @return the chomping type, CLIP if no chomping indicator is given
	 * @throws Exception
	 *             if the header is invalid
	 */
	public static YamlMultilineScalarChompingType getYamlMultilineScalarChompingType(final String multilineTypeSign) throws Exception {
		final String indicators = YamlMultilineScalarType.getHeaderIndicators(multilineTypeSign);
		if (indicators.contains("-")) {
			return YamlMultilineScalarChompingType.STRIP;
		} else if (indicators.contains("+")) {
			return YamlMultilineScalarChompingType.KEEP;
		} else {
			return YamlMultilineScalarChompingType.CLIP;
		}
	}
}
