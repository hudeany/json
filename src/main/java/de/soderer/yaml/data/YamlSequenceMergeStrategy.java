package de.soderer.yaml.data;

/**
 * Strategy for merging YAML sequences.
 */
public enum YamlSequenceMergeStrategy {
	/**
	 * Append all items of the other sequence.
	 */
	APPEND_ALL,
	/**
	 * Append only items not already contained (compared by equals).
	 */
	SKIP_DUPLICATES
}
