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

/**
 * Path to a position within JSON data, like "$.store.customer[5].name".
 * <p>
 * A path consists of a root ("$" or "#"), property keys, array indexes and optionally wildcards
 * and filter expressions. It can be formatted in dot notation ({@link #getDotFormattedPath()}),
 * bracket notation ({@link #getBracketFormattedPath()}) or reference notation
 * ({@link #getReferenceFormattedPath()}). The add methods modify this path and return it for
 * chaining, so copy a shared path with {@link #JsonPath(JsonPath)} before extending it.
 * </p>
 */
public class JsonPath {
	/**
	 * Matches a filter expression's bracket content, e.g. "[?(@.version=='26.1.72')]":
	 * group 1 = property name ("version"), group 2 = operator ("=="), group 3 = raw literal
	 * text ("'26.1.72'", still quoted if it is a string). Only a single property level after
	 * "@." is supported (no nested "@.a.b").
	 */
	private static final Pattern FILTER_PATTERN = Pattern.compile("^\\[\\?\\(\\s*@\\.([A-Za-z_][A-Za-z0-9_]*)\\s*(==|!=|<=|>=|<|>)\\s*(.+?)\\s*\\)\\]$");

	/**
	 * The path elements, the root element first.
	 */
	private Stack<JsonPathElement> jsonPathElements = new Stack<>();

	/**
	 * Creates a new path pointing to the root ("$").
	 */
	public JsonPath() {
		jsonPathElements.push(new JsonPathRoot("$"));
	}

	/**
	 * Parse a JSON path string.
	 *
	 * <p>Allowed syntax for JSON path:</p>
	 * <pre>
	 * dot-notation:                        $.store.customer[5].item[2]
	 * bracket-notation:                    $['store']['customer'][5]['item'][2]
	 * schema-reference-notation:           #/store/customer/item
	 * external schema-reference-notation:  otherSchema.json#/store/customer/item
	 * wildcard:                            $.store.*   or   $.store[*]
	 * filter expression:                   $.store.item[?(@.price&lt;10)]
	 *                                      $.*[?(@.version=='26.1.72')]
	 * </pre>
	 *
	 * <p>A wildcard matches every property value of an object, or every item of an array.
	 * A filter expression keeps only the candidates (every property value of an object, or
	 * every item of an array) whose own given property compares as specified. Only a single
	 * property level after "@." is supported. Paths with wildcards or filters can match several
	 * nodes and are only usable with {@link de.soderer.json.JsonNode#getDataListByJsonPath}.</p>
	 *
	 * @param jsonPathString
	 *            JSON path string
	 * @throws JsonSchemaDefinitionError
	 *             if the JSON path string is invalid
	 */
	public JsonPath(final String jsonPathString) throws JsonSchemaDefinitionError {
		try (JsonPathReader jsonPathReader = new JsonPathReader(jsonPathString)) {
			jsonPathElements = jsonPathReader.getReadJsonPathElements();
		} catch (final Exception e) {
			throw new JsonSchemaDefinitionError(e.getMessage(), null, e);
		}
	}

	/**
	 * Creates a copy of a path. Extending the copy does not change the original.
	 *
	 * @param jsonPath
	 *            the path to copy
	 */
	public JsonPath(final JsonPath jsonPath) {
		jsonPathElements = new Stack<>();
		jsonPathElements.addAll(jsonPath.getPathParts());
	}

	/**
	 * Returns this path in dot notation, e.g. "$.store.customer[5].name". Dots within property
	 * keys are escaped by a backslash.
	 *
	 * @return the formatted path
	 */
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

	/**
	 * Returns this path in bracket notation, e.g. "$['store']['customer'][5]['name']". Single
	 * quotes within property keys are escaped by a backslash.
	 *
	 * @return the formatted path
	 */
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

	/**
	 * Returns this path in reference notation, e.g. "$/store/customer[5]/name". Slashes within
	 * property keys are escaped by a backslash.
	 *
	 * @return the formatted path
	 */
	public String getReferenceFormattedPath() {
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

	/**
	 * Appends a path element.
	 *
	 * @param jsonPathElement
	 *            the element to append, must be no root element
	 * @return this path for chaining
	 * @throws IllegalArgumentException
	 *             if the element is null or a root element
	 */
	public JsonPath add(final JsonPathElement jsonPathElement) {
		if (jsonPathElement == null) {
			throw new IllegalArgumentException("Invalid null value for JsonPathElement");
		} else if (jsonPathElement instanceof JsonPathRoot) {
			throw new IllegalArgumentException("Cannot add JsonPathRoot as element");
		} else {
			jsonPathElements.push(jsonPathElement);
			return this;
		}
	}

	/**
	 * Removes the last path element.
	 *
	 * @return this path for chaining
	 * @throws java.util.EmptyStackException
	 *             if the path is empty
	 */
	public JsonPath removeLastElement() {
		jsonPathElements.pop();
		return this;
	}

	/**
	 * Appends an array index.
	 *
	 * @param arrayIndex
	 *            the array index, 0 based
	 * @return this path for chaining
	 */
	public JsonPath addArrayIndex(final int arrayIndex) {
		jsonPathElements.push(new JsonPathArrayElement(arrayIndex));
		return this;
	}

	/**
	 * Appends a property key.
	 *
	 * @param propertyKey
	 *            the property key
	 * @return this path for chaining
	 */
	public JsonPath addPropertyKey(final String propertyKey) {
		jsonPathElements.push(new JsonPathPropertyElement(propertyKey));
		return this;
	}

	/**
	 * Appends a wildcard matching every property value or array item.
	 *
	 * @return this path for chaining
	 */
	public JsonPath addWildcard() {
		jsonPathElements.push(new JsonPathWildcardElement());
		return this;
	}

	/**
	 * Appends a filter expression like "[?(@.price &lt; 10)]".
	 *
	 * @param propertyName
	 *            the property checked on each candidate
	 * @param operator
	 *            the comparison operator
	 * @param literalValue
	 *            the value to compare with: String, Long, Double, Boolean or null
	 * @return this path for chaining
	 */
	public JsonPath addFilter(final String propertyName, final FilterOperator operator, final Object literalValue) {
		jsonPathElements.push(new JsonPathFilterElement(propertyName, operator, literalValue));
		return this;
	}

	/**
	 * Parser for path strings.
	 */
	private static class JsonPathReader extends BasicReader {
		/** The path elements read. */
		private final Stack<JsonPathElement> readJsonPathElements;

		/**
		 * Parses a path string.
		 *
		 * @param jsonPathString
		 *            the path string
		 * @throws Exception
		 *             if the path string is invalid
		 */
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

		/**
		 * Returns the path elements read.
		 *
		 * @return the path elements, the root element first
		 */
		public Stack<JsonPathElement> getReadJsonPathElements() {
			return readJsonPathElements;
		}
	}

	/**
	 * Parses a single path part, resolving JSON pointer escapes ("~1" for "/", "~0" for "~").
	 *
	 * @param value
	 *            the path part, e.g. "name", "['name']", "[2]", "*" or a filter expression
	 * @return the path element
	 */
	private static JsonPathElement parseJsonPathElement(final String value) {
		// RFC 6901: "~1" must be resolved before "~0", otherwise "~01" would wrongly become "/"
		final String valueRaw = value.replace("~1", "/").replace("~0", "~").replace("%25", "%");
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

	/**
	 * Parses a filter expression like "[?(@.version=='1.0')]".
	 *
	 * @param bracketContent
	 *            the filter expression including brackets
	 * @return the filter element
	 * @throws RuntimeException
	 *             if the expression is invalid
	 */
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
	 *
	 * @param rawValue
	 *            literal text of the filter expression
	 * @return parsed value
	 * @throws RuntimeException
	 *             if the value is no valid literal
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

	/**
	 * Returns the elements of this path. The returned stack is the internal one, so changes affect
	 * this path.
	 *
	 * @return the path elements, the root element first
	 */
	public Stack<JsonPathElement> getPathParts() {
		return jsonPathElements;
	}

	/**
	 * Returns the last element of this path.
	 *
	 * @return the last element, the root element for an empty path
	 */
	public JsonPathElement getLastPathPart() {
		return jsonPathElements.peek();
	}

	/**
	 * Checks whether the last element of this path has the given text representation.
	 *
	 * @param trailingPart
	 *            the text, e.g. a property key
	 * @return true, if the last element matches
	 */
	public boolean endsWith(final String trailingPart) {
		return jsonPathElements != null && jsonPathElements.size() > 0 && jsonPathElements.get(jsonPathElements.size() - 1).toString().equals(trailingPart);
	}

	@Override
	public String toString() {
		return getDotFormattedPath();
	}

	/**
	 * Two paths are equal, if their dot formatted representations are equal.
	 */
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
	 *
	 * @return number of path elements without the root element
	 */
	public int size() {
		return jsonPathElements.size() - 1;
	}
}
