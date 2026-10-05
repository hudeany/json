package de.soderer.json.path;

/**
 * Array index element of a JSON path.
 */
public class JsonPathArrayElement implements JsonPathElement {
	/**
	 * The array index, -1 for the start of an array.
	 */
	private final int arrayIndex;

	/**
	 * Creates an element for the start of an array, before its first item (index -1).
	 */
	public JsonPathArrayElement() {
		arrayIndex = -1;
	}

	/**
	 * Creates an array index element.
	 *
	 * @param arrayIndex
	 *            the array index, 0 based
	 * @throws IllegalArgumentException
	 *             if the index is negative
	 */
	public JsonPathArrayElement(final int arrayIndex) {
		if (arrayIndex < 0) {
			throw new IllegalArgumentException("Array index less than zero: " + arrayIndex);
		}
		this.arrayIndex = arrayIndex;
	}

	/**
	 * Returns the array index.
	 *
	 * @return the array index, -1 for the start of an array
	 */
	public int getIndex() {
		return arrayIndex;
	}

	@Override
	public String toString() {
		return Integer.toString(arrayIndex);
	}
}
