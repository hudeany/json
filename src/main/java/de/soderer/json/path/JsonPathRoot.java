package de.soderer.json.path;

/**
 * Root element of a JSON path: "$" for the data root or "#" for a schema reference root.
 */
public class JsonPathRoot implements JsonPathElement {
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
