package de.soderer.yaml.data;

public enum YamlScalarType {
	STRING,
	NUMBER,
	BOOLEAN,
	NULL_VALUE,

	// Multiline strings
	MULTILINE_LITERAL,   // |
	MULTILINE_FOLDED     // >
}
