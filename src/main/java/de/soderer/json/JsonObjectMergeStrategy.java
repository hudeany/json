package de.soderer.json;

/**
 * Strategy for merging JSON objects with properties existing in both objects.
 */
public enum JsonObjectMergeStrategy {
	/**
	 * Keep the existing property value.
	 */
	KEEP_EXISTING,
	/**
	 * Overwrite the existing property value with the other one.
	 */
	OVERWRITE
}
