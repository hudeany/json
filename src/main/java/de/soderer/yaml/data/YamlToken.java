package de.soderer.yaml.data;

/**
 * Structure events while reading YAML data, used to track the current path.
 */
public enum YamlToken {
	/**
	 * Start of a mapping.
	 */
	YamlMapping_Start,
	/**
	 * Key of a mapping entry.
	 */
	YamlMapping_PropertyKey,
	/**
	 * End of a mapping.
	 */
	YamlMapping_End,
	/**
	 * Start of a sequence.
	 */
	YamlSequence_Start,
	/**
	 * End of a sequence.
	 */
	YamlSequence_End,
	/**
	 * A scalar value.
	 */
	YamlScalar
}
