package de.soderer.json.schema;

/**
 * Root element of a JSON schema path: "$" or "#".
 */
public class JsonSchemaPathRoot implements JsonSchemaPathElement {
	/**
	 * The root symbol.
	 */
	private final String rootElementString;

	/**
	 * Creates a root element.
	 *
	 * @param rootElementString
	 *            "$" or "#"
	 * @throws RuntimeException
	 *             if the symbol is neither "$" nor "#"
	 */
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
