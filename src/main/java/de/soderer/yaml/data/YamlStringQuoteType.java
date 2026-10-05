package de.soderer.yaml.data;

/**
 * Quote style of a YAML string scalar.
 */
public enum YamlStringQuoteType {
	/**
	 * Plain style without quotes.
	 */
	NONE,
	/**
	 * Double quoted style with backslash escapes.
	 */
	DOUBLE,
	/**
	 * Single quoted style, a single quote is written as two single quotes.
	 */
	SINGLE
}
