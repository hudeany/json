package de.soderer.json;

/**
 * Strategy for merging JSON arrays.
 */
public enum JsonArrayMergeStrategy {
	/**
	 * Append all items of the other array.
	 */
	APPEND_ALL,
	/**
	 * Append only items not already contained (compared by equals).
	 */
	SKIP_DUPLICATES
}
