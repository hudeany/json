package de.soderer.json.schema;

/**
 * Property key element of a JSON schema path.
 */
public class JsonSchemaPathPropertyElement implements JsonSchemaPathElement {
	/**
	 * The property key.
	 */
	private final String propertyKey;

	/**
	 * Creates a property key element.
	 *
	 * @param propertyKey
	 *            the property key
	 */
	public JsonSchemaPathPropertyElement(final String propertyKey) {
		this.propertyKey = propertyKey;
	}

	@Override
	public String toString() {
		return propertyKey;
	}
}
