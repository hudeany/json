package de.soderer.json.schema;

public class JsonSchemaPathRoot implements JsonSchemaPathElement {
	private final String rootElementString;

	public JsonSchemaPathRoot(final String rootElementString) {
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
