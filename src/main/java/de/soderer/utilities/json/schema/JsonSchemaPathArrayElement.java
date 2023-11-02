package de.soderer.utilities.json.schema;

public class JsonSchemaPathArrayElement implements JsonSchemaPathElement {
	private final int arrayIndex;

	public JsonSchemaPathArrayElement(final int arrayIndex) {
		this.arrayIndex = arrayIndex;
	}

	@Override
	public String toString() {
		return Integer.toString(arrayIndex);
	}
}
