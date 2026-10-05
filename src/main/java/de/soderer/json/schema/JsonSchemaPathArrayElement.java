package de.soderer.json.schema;

/**
 * Array index element of a JSON schema path.
 */
public class JsonSchemaPathArrayElement implements JsonSchemaPathElement {
	/**
	 * The array index.
	 */
	private final int arrayIndex;

	/**
	 * Creates an array index element.
	 *
	 * @param arrayIndex
	 *            the array index, 0 based
	 */
	public JsonSchemaPathArrayElement(final int arrayIndex) {
		this.arrayIndex = arrayIndex;
	}

	@Override
	public String toString() {
		return Integer.toString(arrayIndex);
	}
}
