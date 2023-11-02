package de.soderer.utilities.json.path;

public class JsonPathArrayElement implements JsonPathElement {
	private final int arrayIndex;

	public JsonPathArrayElement(final int arrayIndex) {
		this.arrayIndex = arrayIndex;
	}

	@Override
	public String toString() {
		return Integer.toString(arrayIndex);
	}
}
