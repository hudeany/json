package de.soderer.yaml.data;

/**
 * Strategy for merging YAML mappings with keys existing in both mappings.
 */
public enum YamlMappingMergeStrategy {
	/**
	 * Keep the existing value.
	 */
	KEEP_EXISTING,
	/**
	 * Overwrite the existing value with the other one.
	 */
	OVERWRITE
}
