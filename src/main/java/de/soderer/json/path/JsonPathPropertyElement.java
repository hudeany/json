package de.soderer.json.path;

public class JsonPathPropertyElement implements JsonPathElement {
	private final String propertyKey;

	public JsonPathPropertyElement(final String propertyKey) {
		this.propertyKey = propertyKey;
	}

	@Override
	public String toString() {
		return propertyKey;
	}
}
