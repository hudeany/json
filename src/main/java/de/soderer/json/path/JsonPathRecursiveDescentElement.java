package de.soderer.json.path;

/**
 * Recursive descent ("..") of a JSON path, e.g. "$..version" or "$..book[?(@.price&lt;10)]".
 * <p>
 * It is always followed by another path element, which is applied to the current node and to all
 * its descendants (objects and arrays at any depth), as defined by RFC 9535. Only meaningful with
 * {@link de.soderer.json.JsonNode#getDataListByJsonPath}, which can return several matches.
 * </p>
 */
public class JsonPathRecursiveDescentElement implements JsonPathElement {
	/**
	 * Creates a recursive descent element.
	 */
	public JsonPathRecursiveDescentElement() {
		// No state
	}

	@Override
	public String toString() {
		return "..";
	}

	@Override
	public boolean equals(final Object otherObject) {
		return otherObject instanceof JsonPathRecursiveDescentElement;
	}

	@Override
	public int hashCode() {
		return JsonPathRecursiveDescentElement.class.hashCode();
	}
}
