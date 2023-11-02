package de.soderer.utilities.json.path;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

import de.soderer.utilities.json.utilities.BasicReader;

public class JsonPath {
	private Stack<JsonPathElement> jsonPathElements = new Stack<>();

	public JsonPath() throws Exception {
		this("$");
	}

	/**
	 * Allowed syntax for JSON path:
	 *
	 * dot-notation:
	 * 	$.store.customer[5].item[2]
	 *
	 * bracket-notation:
	 * 	$['store']['customer'][5]['item'][2]
	 *
	 * schema-reference-notation:
	 * 	#/store/customer/item
	 *
	 * external schema-reference-notation:
	 * 	otherSchema.json#/store/customer/item
	 *
	 * @param jsonPathString
	 * @throws Exception
	 */
	public JsonPath(final String jsonPathString) throws Exception {
		try (JsonPathReader jsonPathReader = new JsonPathReader(jsonPathString)) {
			jsonPathElements = jsonPathReader.getReadJsonPathElements();
		}
	}

	public JsonPath(final JsonPath JsonPath) throws Exception {
		jsonPathElements = new Stack<>();
		jsonPathElements.addAll(JsonPath.getPathParts());
	}

	public String getDotFormattedPath() {
		final StringBuilder returnValue = new StringBuilder();
		for (final JsonPathElement jsonPathElement : jsonPathElements) {
			if (jsonPathElement instanceof JsonPathRoot) {
				returnValue.append(jsonPathElement.toString());
			} else if (jsonPathElement instanceof JsonPathPropertyElement) {
				returnValue.append(".").append(jsonPathElement.toString().replace(".", "\\."));
			} else if (jsonPathElement instanceof JsonPathArrayElement) {
				returnValue.append("[").append(jsonPathElement).append("]");
			}
		}
		return returnValue.toString();
	}

	public String getBracketFormattedPath() {
		final StringBuilder returnValue = new StringBuilder();
		for (final JsonPathElement jsonPathElement : jsonPathElements) {
			if (jsonPathElement instanceof JsonPathRoot) {
				returnValue.append(jsonPathElement.toString());
			} else if (jsonPathElement instanceof JsonPathPropertyElement) {
				returnValue.append("['").append(jsonPathElement.toString().replace("'", "\\'")).append("']");
			} else if (jsonPathElement instanceof JsonPathArrayElement) {
				returnValue.append("[").append(jsonPathElement).append("]");
			}
		}
		return returnValue.toString();
	}

	public String getReferenceFormattedPath() throws Exception {
		final StringBuilder returnValue = new StringBuilder();
		for (final JsonPathElement jsonPathElement : jsonPathElements) {
			if (jsonPathElement instanceof JsonPathRoot) {
				returnValue.append(jsonPathElement.toString());
			} else if (jsonPathElement instanceof JsonPathPropertyElement) {
				returnValue.append(jsonPathElement.toString().replace("/", "\\/"));
			} else if (jsonPathElement instanceof JsonPathArrayElement) {
				returnValue.append("[").append(jsonPathElement).append("]");
			}
		}
		return returnValue.toString();
	}

	public JsonPath add(final JsonPathElement jsonPathElement) {
		if (jsonPathElement == null) {
			throw new RuntimeException("Invalid null value for JsonPathElement");
		} else {
			jsonPathElements.push(jsonPathElement);
			return this;
		}
	}

	public JsonPath addArrayIndex(final int arrayIndex) {
		jsonPathElements.push(new JsonPathArrayElement(arrayIndex));
		return this;
	}

	public JsonPath addPropertyKey(final String propertyKey) {
		jsonPathElements.push(new JsonPathPropertyElement(propertyKey));
		return this;
	}

	private class JsonPathReader extends BasicReader {
		Stack<JsonPathElement> readJsonPathElements;

		public JsonPathReader(final String jsonPathString) throws Exception {
			super(new ByteArrayInputStream(jsonPathString.getBytes(StandardCharsets.UTF_8)));

			readJsonPathElements = new Stack<>();

			Character nextChar = readNextNonWhitespace();
			if (nextChar == null) {
				// Empty json path
				readJsonPathElements.push(new JsonPathRoot("$"));
				return;
			} else if (nextChar == '#' || nextChar == '$') {
				// Root element
				readJsonPathElements.push(new JsonPathRoot(nextChar.toString()));
				nextChar = readNextNonWhitespace();
			}

			while (nextChar != null) {
				String nextJsonPathPart;
				switch (nextChar) {
					case '.':
						nextJsonPathPart = readUpToNext(false, '\\', '.', '[');
						readJsonPathElements.push(parseJsonPathElement(nextJsonPathPart.substring(1).trim()));
						reuseCurrentChar();
						break;
					case '/':
						nextJsonPathPart = readUpToNext(false, '\\', '/', '[');
						readJsonPathElements.push(parseJsonPathElement(nextJsonPathPart.substring(1).trim()));
						reuseCurrentChar();
						break;
					case '[':
						nextJsonPathPart = readUpToNext(true, '\\', ']');
						nextJsonPathPart = nextJsonPathPart.substring(1, nextJsonPathPart.length() - 1);
						if (nextJsonPathPart.startsWith("'") && nextJsonPathPart.endsWith("'")) {
							readJsonPathElements.push(parseJsonPathElement(nextJsonPathPart.substring(1, nextJsonPathPart.length() - 1)));
						} else {
							readJsonPathElements.push(parseJsonPathElement(nextJsonPathPart));
						}
						break;
					default:
						throw new Exception("Invalid JSON path data at '" + nextChar + "'");
				}

				nextChar = readNextNonWhitespace();
			}
		}


		public Stack<JsonPathElement> getReadJsonPathElements() {
			return readJsonPathElements;
		}
	}

	private static JsonPathElement parseJsonPathElement(final String value) {
		final String valueRaw = value.replace("~0", "~").replace("~1", "/").replace("%25", "%");
		if (valueRaw.startsWith("['") && valueRaw.endsWith("']")) {
			return new JsonPathPropertyElement(valueRaw.substring(2, valueRaw.length() - 2));
		} else if (valueRaw.startsWith("[") && valueRaw.endsWith("]")) {
			return new JsonPathArrayElement(Integer.parseInt(valueRaw.substring(1, valueRaw.length() - 1)));
		} else  {
			return new JsonPathPropertyElement(valueRaw);
		}
	}

	public Stack<JsonPathElement> getPathParts() {
		return jsonPathElements;
	}

	public boolean endsWith(final String trailingPart) {
		return jsonPathElements != null && jsonPathElements.size() > 0 && jsonPathElements.get(jsonPathElements.size() - 1).toString().equals(trailingPart);
	}

	@Override
	public String toString() {
		return getDotFormattedPath();
	}

	@Override
	public boolean equals(final Object otherObject) {
		if (otherObject == null) {
			return false;
		} else if (!(otherObject instanceof JsonPath)) {
			return false;
		} else {
			return getDotFormattedPath().equals(((JsonPath) otherObject).getDotFormattedPath());
		}
	}

	@Override
	public int hashCode() {
		return getDotFormattedPath().hashCode();
	}
}