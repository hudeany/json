package de.soderer.json.path;

/**
 * Matches every property value of a {@link de.soderer.json.JsonObject} or every item of a
 * {@link de.soderer.json.JsonArray} at this position in the path (dot-notation "*" or
 * bracket-notation "[*]"). Only meaningful with {@link de.soderer.json.JsonNode#getDataListByJsonPath},
 * which can return several matches - {@link de.soderer.json.JsonNode#getDataByJsonPath} (single
 * result) rejects any path containing a wildcard.
 */
public class JsonPathWildcardElement implements JsonPathElement {
	@Override
	public String toString() {
		return "*";
	}

	@Override
	public boolean equals(final Object otherObject) {
		return otherObject instanceof JsonPathWildcardElement;
	}

	@Override
	public int hashCode() {
		return JsonPathWildcardElement.class.hashCode();
	}
}
