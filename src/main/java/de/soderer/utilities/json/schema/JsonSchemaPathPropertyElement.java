package de.soderer.utilities.json.schema;

public class JsonSchemaPathPropertyElement implements JsonSchemaPathElement {
	private final String propertyKey;

	public JsonSchemaPathPropertyElement(final String propertyKey) {
		this.propertyKey = propertyKey;
	}

	@Override
	public String toString() {
		return propertyKey;
	}
}
