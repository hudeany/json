package de.soderer.yaml.data;

/**
 * Typen von YAML-Skalaren.
 * Wird von YamlScalar verwendet, um die Art des Wertes zu bestimmen.
 */
public enum YamlScalarType {

	// Einfache Werte
	STRING,
	NUMBER,
	BOOLEAN,
	NULL_VALUE,

	// Multiline-Strings
	MULTILINE_LITERAL,   // |
	MULTILINE_FOLDED     // >
}
