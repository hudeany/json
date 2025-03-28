package de.soderer.json.compare;

public class JsonCompareSettings {
	/**
	 * - allow different order of items in arrays
	 */
	private boolean allowArrayMixedOrder = false;

	private boolean compareArraysOfDifferentLength = false;

	public boolean isAllowArrayMixedOrder() {
		return allowArrayMixedOrder;
	}

	public JsonCompareSettings setAllowArrayMixedOrder(final boolean allowArrayMixedOrder) {
		this.allowArrayMixedOrder = allowArrayMixedOrder;
		return this;
	}

	public boolean isCompareArraysOfDifferentLength() {
		return compareArraysOfDifferentLength;
	}

	public JsonCompareSettings setCompareArraysOfDifferentLength(final boolean compareArraysOfDifferentLength) {
		this.compareArraysOfDifferentLength = compareArraysOfDifferentLength;
		return this;
	}
}
