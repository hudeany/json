package de.soderer.json.path;

/**
 * Property key element of a JSON path.
 */
public class JsonPathPropertyElement implements JsonPathElement {
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
	public JsonPathPropertyElement(final String propertyKey) {
		this.propertyKey = propertyKey;
	}

	/**
	 * Returns the property key.
	 *
	 * @return the property key
	 */
	public String getPropertyKey() {
		return propertyKey;
	}

	@Override
	public String toString() {
		return propertyKey;
	}
}
