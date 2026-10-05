package de.soderer.yaml.data;

/**
 * Types of YAML scalars.
 */
public enum YamlScalarType {
	/**
	 * Text in plain or quoted style.
	 */
	STRING,
	/**
	 * Text in block style ("|" literal or "&gt;" folded).
	 */
	MULTILINE,
	/**
	 * Integer or floating point number, including hexadecimal, octal and binary notation and .inf/.nan.
	 */
	NUMBER,
	/**
	 * Boolean value: true/false, yes/no, on/off, y/n (case insensitive).
	 */
	BOOLEAN,
	/**
	 * The null value: null, ~ or an empty value.
	 */
	NULL_VALUE,
}
