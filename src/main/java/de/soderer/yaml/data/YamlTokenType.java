package de.soderer.yaml.data;

public enum YamlTokenType {
	// Structure
	INDENT,
	DEDENT,
	NEWLINE,
	EOF,

	// Block Structure
	DASH,           // -
	COLON,          // :
	QUESTION,       // ?

	// Flow Structure
	FLOW_MAP_START, // {
	FLOW_MAP_END,   // }
	FLOW_SEQ_START, // [
	FLOW_SEQ_END,   // ]
	COMMA,          // ,

	// Scalars
	STRING,
	NUMBER,
	BOOLEAN,
	NULL_VALUE,

	// Anchors & Aliases
	ANCHOR,         // &id
	ALIAS,          // *id

	// Comments
	COMMENT
}
