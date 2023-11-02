package de.soderer.utilities.json.path;

public class JsonPathRoot implements JsonPathElement {
	private final String rootElementString;

	public JsonPathRoot(final String rootElementString) {
		if ("#".equals(rootElementString) || "$".equals(rootElementString)) {
			this.rootElementString = rootElementString;
		} else {
			throw new RuntimeException("Invalid JSON path root element: " + rootElementString);
		}
	}

	@Override
	public String toString() {
		return rootElementString;
	}
}
