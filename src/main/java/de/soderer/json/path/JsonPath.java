package de.soderer.json.path;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.soderer.json.path.JsonPathFilterElement.FilterOperator;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.utilities.BasicReader;

public class JsonPath {
	/**
	 * Matches a filter expression's bracket content, e.g. "[?(@.version=='26.1.72')]":
	 * group 1 = property name ("version"), group 2 = operator ("=="), group 3 = raw literal
	 * text ("'26.1.72'", still quoted if it is a string). Only a single property level after
	 * "@." is supported (no nested "@.a.b").
	 */
	private static final Pattern FILTER_PATTERN = Pattern.compile("^\\[\\?\\(\\s*@\\.([A-Za-z_][A-Za-z0-9_]*)\\s*(==|!=|<=|>=|<|>)\\s*(.+?)\\s*\\)\\]$");

	private Stack<JsonPathElement> jsonPathElements = new Stack<>();

	public JsonPath() {
		jsonPathElements.push(new JsonPathRoot("$"));
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
	 * wildcard (matches every property value of an object, or every item of an array; only
	 * usable with {@link de.soderer.json.JsonNode#getDataListByJsonPath}, which can return
	 * several matches):
	 * 	$.store.*
	 * 	$.store[*]
	 *
	 * filter expression (keeps only the candidates - every property value of an object, or
	 * every item of an array - whose own given property compares as specified; only a single
	 * property level after "@." is supported, and only usable with
	 * {@link de.soderer.json.JsonNode#getDataListByJsonPath}):
	 * 	$.store.item[?(@.price<10)]
	 * 	$.*[?(@.version=='26.1.72')]
	 *
	 * @param jsonPathString
	 * @throws JsonSchemaDefinitionError
	 * @throws Exception
	 */
	public JsonPath(final String jsonPathString) throws JsonSchemaDefinitionError {
		try (JsonPathReader jsonPathReader = new JsonPathReader(jsonPathString)) {
			jsonPathElements = jsonPathReader.getReadJsonPathElements();
		} catch (final Exception e) {
			throw new JsonSchemaDefinitionError(e.getMessage(), null, e);
		}
	}

	public JsonPath(final JsonPath jsonPath) {
		jsonPathElements = new Stack<>();
		jsonPathElements.addAll(jsonPath.getPathParts());
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
			} else if (jsonPathElement instanceof JsonPathWildcardElement) {
				returnValue.append(".*");
			} else if (jsonPathElement instanceof JsonPathFilterElement) {
				returnValue.append(jsonPathElement);
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
			} else if (jsonPathElement instanceof JsonPathWildcardElement) {
				returnValue.append("[*]");
			} else if (jsonPathElement instanceof JsonPathFilterElement) {
				returnValue.append(jsonPathElement);
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
				returnValue.append("/").append(jsonPathElement.toString().replace("/", "\\/"));
			} else if (jsonPathElement instanceof JsonPathArrayElement) {
				returnValue.append("[").append(jsonPathElement).append("]");
			} else if (jsonPathElement instanceof JsonPathWildcardElement) {
				returnValue.append("/*");
			} else if (jsonPathElement instanceof JsonPathFilterElement) {
				returnValue.append(jsonPathElement);
			}
		}
		return returnValue.toString();
	}

	public JsonPath add(final JsonPathElement jsonPathElement) {
		if (jsonPathElement == null) {
			throw new RuntimeException("Invalid null value for JsonPathElement");
		} else if (jsonPathElement instanceof JsonPathRoot) {
			throw new RuntimeException("Cannot add JsonPathRoot as element");
		} else {
			jsonPathElements.push(jsonPathElement);
			return this;
		}
	}

	public JsonPath removeLastElement() {
		jsonPathElements.pop();
		return this;
	}

	public JsonPath addArrayIndex(final int arrayIndex) {
		jsonPathElements.push(new JsonPathArrayElement(arrayIndex));
		return this;
	}

	public JsonPath addPropertyKey(final String propertyKey) {
		jsonPathElements.push(new JsonPathPropertyElement(propertyKey));
		return this;
	}

	public JsonPath addWildcard() {
		jsonPathElements.push(new JsonPathWildcardElement());
		return this;
	}

	public JsonPath addFilter(final String propertyName, final FilterOperator operator, final Object literalValue) {
		jsonPathElements.push(new JsonPathFilterElement(propertyName, operator, literalValue));
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
						break;
					case '/':
						nextJsonPathPart = readUpToNext(false, '\\', '/', '[');
						readJsonPathElements.push(parseJsonPathElement(nextJsonPathPart.substring(1).trim()));
						break;
					case '[':
						nextJsonPathPart = readUpToNext(true, '\\', ']');
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
		if ("*".equals(valueRaw) || "[*]".equals(valueRaw)) {
			return new JsonPathWildcardElement();
		} else if (valueRaw.startsWith("[?") && valueRaw.endsWith(")]")) {
			return parseFilterElement(valueRaw);
		} else if (valueRaw.startsWith("['") && valueRaw.endsWith("']")) {
			return new JsonPathPropertyElement(valueRaw.substring(2, valueRaw.length() - 2));
		} else if (valueRaw.startsWith("[") && valueRaw.endsWith("]")) {
			return new JsonPathArrayElement(Integer.parseInt(valueRaw.substring(1, valueRaw.length() - 1)));
		} else  {
			return new JsonPathPropertyElement(valueRaw);
		}
	}

	private static JsonPathFilterElement parseFilterElement(final String bracketContent) {
		final Matcher matcher = FILTER_PATTERN.matcher(bracketContent);
		if (!matcher.matches()) {
			throw new RuntimeException("Invalid JSON path filter expression: '" + bracketContent + "'");
		}
		final String propertyName = matcher.group(1);
		final FilterOperator operator = FilterOperator.getBySymbol(matcher.group(2));
		final Object literalValue = parseFilterLiteral(matcher.group(3));
		return new JsonPathFilterElement(propertyName, operator, literalValue);
	}

	/**
	 * Parses the right-hand side of a filter comparison into its Java value: a quoted string
	 * (single or double quotes) becomes a String, "true"/"false" a Boolean, "null" a null
	 * value, and everything else is parsed as a number (Long if it has no decimal point or
	 * exponent, Double otherwise).
	 */
	private static Object parseFilterLiteral(final String rawValue) {
		if (rawValue.length() >= 2
				&& ((rawValue.startsWith("'") && rawValue.endsWith("'"))
						|| (rawValue.startsWith("\"") && rawValue.endsWith("\"")))) {
			return rawValue.substring(1, rawValue.length() - 1);
		} else if ("true".equalsIgnoreCase(rawValue)) {
			return Boolean.TRUE;
		} else if ("false".equalsIgnoreCase(rawValue)) {
			return Boolean.FALSE;
		} else if ("null".equalsIgnoreCase(rawValue)) {
			return null;
		} else {
			try {
				if (rawValue.contains(".") || rawValue.toLowerCase(Locale.ROOT).contains("e")) {
					return Double.parseDouble(rawValue);
				} else {
					return Long.parseLong(rawValue);
				}
			} catch (@SuppressWarnings("unused") final NumberFormatException e) {
				throw new RuntimeException("Invalid JSON path filter literal value: '" + rawValue + "'");
			}
		}
	}

	public Stack<JsonPathElement> getPathParts() {
		return jsonPathElements;
	}

	public JsonPathElement getLastPathPart() {
		return jsonPathElements.peek();
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

	/**
	 * Size is the number of elements within the JsonPath without the first root element
	 * @return
	 */
	public int size() {
		return jsonPathElements.size() - 1;
	}
}
