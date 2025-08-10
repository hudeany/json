package de.soderer.json.path;

public class JsonPathArrayElement implements JsonPathElement {
	private final int arrayIndex;

	/**
	 * Empty JSON array or start of JSON array
	 */
	public JsonPathArrayElement() {
		arrayIndex = -1;
	}

	public JsonPathArrayElement(final int arrayIndex) {
		if (arrayIndex < 0) {
			throw new IllegalArgumentException("Array index less than zero: " + arrayIndex);
		}
		this.arrayIndex = arrayIndex;
	}

	public int getIndex() {
		return arrayIndex;
	}

	@Override
	public String toString() {
		return Integer.toString(arrayIndex);
	}
}
