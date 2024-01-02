package de.soderer.utilities.json.path;

public class JsonPathArrayElement implements JsonPathElement {
	private final int arrayIndex;

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
