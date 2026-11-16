package de.soderer.json.path;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.soderer.json.path.JsonPathFilterElement.FilterOperator;
import de.soderer.json.path.JsonPathFilterExpression.And;
import de.soderer.json.path.JsonPathFilterExpression.Comparison;
import de.soderer.json.path.JsonPathFilterExpression.Existence;
import de.soderer.json.path.JsonPathFilterExpression.Function;
import de.soderer.json.path.JsonPathFilterExpression.FunctionCall;
import de.soderer.json.path.JsonPathFilterExpression.FunctionTest;
import de.soderer.json.path.JsonPathFilterExpression.Literal;
import de.soderer.json.path.JsonPathFilterExpression.Not;
import de.soderer.json.path.JsonPathFilterExpression.Operand;
import de.soderer.json.path.JsonPathFilterExpression.Or;
import de.soderer.json.path.JsonPathFilterExpression.PathOperand;
import de.soderer.json.path.JsonPathUnionElement.IndexSelector;
import de.soderer.json.path.JsonPathUnionElement.NameSelector;
import de.soderer.json.path.JsonPathUnionElement.Selector;
import de.soderer.json.path.JsonPathUnionElement.SliceSelector;
import de.soderer.json.schema.JsonSchemaDefinitionError;

/**
 * Path to a position within JSON data, like "$.store.customer[5].name".
 * <p>
 * A path consists of a root ("$" or "#"), property keys, array indexes and optionally wildcards,
 * recursive descents, slices, unions and filter expressions. It can be formatted in dot notation
 * ({@link #getDotFormattedPath()}), bracket notation ({@link #getBracketFormattedPath()}) or
 * reference notation ({@link #getReferenceFormattedPath()}). The add methods modify this path and
 * return it for chaining, so copy a shared path with {@link #JsonPath(JsonPath)} before extending
 * it.
 * </p>
 */
public class JsonPath {
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
	 * <p>Allowed syntax for JSON path (based on RFC 9535):</p>
	 * <pre>
	 * dot-notation:                        $.store.customer[5].item[2]
	 * bracket-notation:                    $['store']['customer'][5]['item'][2]   (also with double quotes)
	 * schema-reference-notation:           #/store/customer/item
	 * wildcard:                            $.store.*   or   $.store[*]
	 * recursive descent:                   $..price   $..book[0]   $..*
	 * negative index (from the end):       $.store.book[-1]
	 * slice (start:end:step):              $.store.book[0:2]   $.store.book[::2]   $.store.book[-2:]
	 * union:                               $.store.book[0,2]   $.store['book','bicycle']
	 * filter expression:                   $.store.book[?(@.price&lt;10)]
	 *                                      $.store.book[?@.price&lt;10]   (without parentheses, as in RFC 9535)
	 *                                      $.*[?(@.version=='26.1.72')]
	 *                                      $..book[?(@.author.name == 'Tolkien' &amp;&amp; (@.price &lt; 10 || !@.isbn))]
	 *                                      $.store.book[?(@.price &lt; $.limits.maxPrice)]
	 * filter functions (RFC 9535):         $.store.book[?(length(@.title) &gt; 10)]
	 *                                      $.store.book[?(count(@.tags[*]) == 2)]
	 *                                      $.store.book[?(value(@..color) == 'red')]
	 *                                      $.versions[?(match(@.version, '2[0-9]\\..*'))]
	 *                                      $.users[?(search(@.name, '[Mm]eier'))]
	 * </pre>
	 *
	 * <p>A wildcard matches every property value of an object, or every item of an array. A
	 * recursive descent applies the following element to a node and all its descendants. A filter
	 * keeps the candidates matching its expression; expressions support comparisons (==, !=, &lt;,
	 * &lt;=, &gt;, &gt;=) of paths ("@..." relative to the candidate, "$..." from the root) and
	 * literals (strings in single or double quotes, numbers, true, false, null), existence tests
	 * ("@.isbn"), &amp;&amp;, ||, ! and parentheses, and the functions length(), count(), value(),
	 * match() and search() with regular expressions as I-Regexp of RFC 9485 (see
	 * {@link JsonPathFilterExpression}).</p>
	 *
	 * <p>Within property keys of dot and reference notation a backslash escapes the next character
	 * (e.g. "\." for a dot within a key), and "~1", "~0" and "%25" are resolved to "/", "~" and "%"
	 * as in JSON pointers. Paths with wildcards, recursive descents, filters, slices or unions can
	 * match several nodes and are only usable with
	 * {@link de.soderer.json.JsonNode#getDataListByJsonPath}.</p>
	 *
	 * @param jsonPathString
	 *            JSON path string
	 * @throws JsonSchemaDefinitionError
	 *             if the JSON path string is invalid
	 */
	public JsonPath(final String jsonPathString) throws JsonSchemaDefinitionError {
		try {
			jsonPathElements = new Parser(jsonPathString).parsePath();
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
	 * Formatting styles of a path.
	 */
	private enum FormatStyle {
		/** Dot notation "$.a.b[2]" */
		DOT,
		/** Bracket notation "$['a']['b'][2]" */
		BRACKET,
		/** Reference notation "$/a/b[2]" */
		REFERENCE
	}

	/**
	 * Returns this path in dot notation, e.g. "$.store.customer[5].name". Dots, opening brackets
	 * and backslashes within property keys are escaped by a backslash.
	 *
	 * @return the formatted path
	 */
	public String getDotFormattedPath() {
		return format(jsonPathElements, FormatStyle.DOT);
	}

	/**
	 * Returns this path in bracket notation, e.g. "$['store']['customer'][5]['name']". Single
	 * quotes and backslashes within property keys are escaped by a backslash.
	 *
	 * @return the formatted path
	 */
	public String getBracketFormattedPath() {
		return format(jsonPathElements, FormatStyle.BRACKET);
	}

	/**
	 * Returns this path in reference notation, e.g. "$/store/customer[5]/name". Slashes, opening
	 * brackets and backslashes within property keys are escaped by a backslash. A recursive descent
	 * is shown as "/..", which cannot be parsed again.
	 *
	 * @return the formatted path
	 */
	public String getReferenceFormattedPath() {
		return format(jsonPathElements, FormatStyle.REFERENCE);
	}

	/**
	 * Formats path elements.
	 *
	 * @param elements
	 *            the elements
	 * @param style
	 *            the formatting style
	 * @return the formatted elements
	 */
	private static String format(final List<JsonPathElement> elements, final FormatStyle style) {
		final StringBuilder returnValue = new StringBuilder();
		for (int i = 0; i < elements.size(); i++) {
			final JsonPathElement jsonPathElement = elements.get(i);
			if (jsonPathElement instanceof JsonPathRoot) {
				returnValue.append(jsonPathElement.toString());
			} else if (jsonPathElement instanceof JsonPathPropertyElement) {
				final String propertyKey = ((JsonPathPropertyElement) jsonPathElement).getPropertyKey();
				switch (style) {
					case BRACKET:
						returnValue.append("['").append(escapeQuotedText(propertyKey, '\'')).append("']");
						break;
					case REFERENCE:
						returnValue.append("/").append(escapeName(propertyKey, '/'));
						break;
					case DOT:
					default:
						if (needsBracketsInDotNotation(propertyKey)) {
							// Keys like "", "*x" or " x" would be read differently in dot notation
							returnValue.append("['").append(escapeQuotedText(propertyKey, '\'')).append("']");
						} else {
							returnValue.append(".").append(escapeName(propertyKey, '.'));
						}
						break;
				}
			} else if (jsonPathElement instanceof JsonPathArrayElement) {
				returnValue.append("[").append(jsonPathElement).append("]");
			} else if (jsonPathElement instanceof JsonPathWildcardElement) {
				switch (style) {
					case BRACKET:
						returnValue.append("[*]");
						break;
					case REFERENCE:
						returnValue.append("/*");
						break;
					case DOT:
					default:
						returnValue.append(".*");
						break;
				}
			} else if (jsonPathElement instanceof JsonPathRecursiveDescentElement) {
				final JsonPathElement nextElement = i + 1 < elements.size() ? elements.get(i + 1) : null;
				if (style == FormatStyle.REFERENCE) {
					returnValue.append("/..");
				} else if (style == FormatStyle.DOT && ((nextElement instanceof JsonPathPropertyElement && !needsBracketsInDotNotation(((JsonPathPropertyElement) nextElement).getPropertyKey())) || nextElement instanceof JsonPathWildcardElement)) {
					// The following ".name" or ".*" completes the ".."
					returnValue.append(".");
				} else {
					returnValue.append("..");
				}
			} else if (jsonPathElement instanceof JsonPathFilterElement || jsonPathElement instanceof JsonPathUnionElement) {
				returnValue.append(jsonPathElement);
			}
		}
		return returnValue.toString();
	}

	/**
	 * Formats the elements of a path within a filter expression (after "@" or "$"): dot notation for
	 * simple property keys, bracket notation for all others.
	 *
	 * @param elements
	 *            the path elements without root
	 * @return the formatted elements
	 */
	static String formatRelativePath(final List<JsonPathElement> elements) {
		final StringBuilder returnValue = new StringBuilder();
		for (int i = 0; i < elements.size(); i++) {
			final JsonPathElement jsonPathElement = elements.get(i);
			if (jsonPathElement instanceof JsonPathPropertyElement) {
				final String propertyKey = ((JsonPathPropertyElement) jsonPathElement).getPropertyKey();
				final boolean afterRecursiveDescent = i > 0 && elements.get(i - 1) instanceof JsonPathRecursiveDescentElement;
				if (isFilterName(propertyKey)) {
					returnValue.append(afterRecursiveDescent ? "" : ".").append(propertyKey);
				} else {
					returnValue.append("['").append(escapeQuotedText(propertyKey, '\'')).append("']");
				}
			} else if (jsonPathElement instanceof JsonPathWildcardElement) {
				final boolean afterRecursiveDescent = i > 0 && elements.get(i - 1) instanceof JsonPathRecursiveDescentElement;
				returnValue.append(afterRecursiveDescent ? "*" : ".*");
			} else if (jsonPathElement instanceof JsonPathRecursiveDescentElement) {
				returnValue.append("..");
			} else {
				returnValue.append(format(List.of(jsonPathElement), FormatStyle.DOT));
			}
		}
		return returnValue.toString();
	}

	/**
	 * Escapes the backslash and the given quote character by a backslash, e.g. for property keys
	 * and string literals in quotes.
	 *
	 * @param text
	 *            the text
	 * @param quoteChar
	 *            the quote character
	 * @return the escaped text
	 */
	static String escapeQuotedText(final String text, final char quoteChar) {
		final StringBuilder returnValue = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			final char character = text.charAt(i);
			if (character == '\\' || character == quoteChar) {
				returnValue.append('\\');
			}
			returnValue.append(character);
		}
		return returnValue.toString();
	}

	/**
	 * Escapes the backslash, the opening bracket and the given separator character within a
	 * property key of dot or reference notation.
	 */
	private static String escapeName(final String propertyKey, final char separatorChar) {
		final StringBuilder returnValue = new StringBuilder();
		for (int i = 0; i < propertyKey.length(); i++) {
			final char character = propertyKey.charAt(i);
			if (character == '\\' || character == '[' || character == separatorChar) {
				returnValue.append('\\');
			}
			returnValue.append(character);
		}
		return returnValue.toString();
	}

	/**
	 * Checks whether a property key must be written in bracket notation within a dot formatted
	 * path, because it would be read differently in dot notation: an empty key, a key starting
	 * with "*" (wildcard) or with surrounding whitespace (trimmed when read).
	 */
	private static boolean needsBracketsInDotNotation(final String propertyKey) {
		return propertyKey.isEmpty()
				|| propertyKey.charAt(0) == '*'
				|| Character.isWhitespace(propertyKey.charAt(0))
				|| Character.isWhitespace(propertyKey.charAt(propertyKey.length() - 1));
	}

	/**
	 * Checks whether a property key can be written in dot notation within a filter expression.
	 */
	private static boolean isFilterName(final String propertyKey) {
		if (propertyKey.isEmpty() || "true".equals(propertyKey) || "false".equals(propertyKey) || "null".equals(propertyKey) || Character.isDigit(propertyKey.charAt(0)) || propertyKey.charAt(0) == '-') {
			return false;
		}
		for (int i = 0; i < propertyKey.length(); i++) {
			if (!Parser.isFilterNameChar(propertyKey.charAt(i))) {
				return false;
			}
		}
		return true;
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
	 *            the array index, 0 based; a negative index counts from the end ("-1" is the last
	 *            item)
	 * @return this path for chaining
	 */
	public JsonPath addArrayIndex(final int arrayIndex) {
		if (arrayIndex >= 0) {
			jsonPathElements.push(new JsonPathArrayElement(arrayIndex));
		} else {
			jsonPathElements.push(new JsonPathUnionElement(new IndexSelector(arrayIndex)));
		}
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
	 * Appends a recursive descent (".."), which applies the next appended element to the current
	 * node and all its descendants.
	 *
	 * @return this path for chaining
	 */
	public JsonPath addRecursiveDescent() {
		jsonPathElements.push(new JsonPathRecursiveDescentElement());
		return this;
	}

	/**
	 * Appends an array slice "start:end:step", e.g. "[1:3]".
	 *
	 * @param start
	 *            the first index, or null for the default
	 * @param end
	 *            the index after the last selected item, or null for the default
	 * @param step
	 *            the step width, or null for 1
	 * @return this path for chaining
	 */
	public JsonPath addSlice(final Integer start, final Integer end, final Integer step) {
		jsonPathElements.push(new JsonPathUnionElement(new SliceSelector(start, end, step)));
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
	 *            the value to compare with: String, Number (e.g. Long or Double), Boolean or null
	 * @return this path for chaining
	 */
	public JsonPath addFilter(final String propertyName, final FilterOperator operator, final Object literalValue) {
		jsonPathElements.push(new JsonPathFilterElement(propertyName, operator, literalValue));
		return this;
	}

	/**
	 * Appends a filter with an expression given as text, e.g. "@.price &lt; 10 &amp;&amp; @.isbn" for
	 * "[?(@.price &lt; 10 &amp;&amp; @.isbn)]".
	 *
	 * @param filterExpression
	 *            the filter expression without the surrounding "[?(" and ")]"
	 * @return this path for chaining
	 * @throws JsonSchemaDefinitionError
	 *             if the filter expression is invalid
	 */
	public JsonPath addFilter(final String filterExpression) throws JsonSchemaDefinitionError {
		try {
			final Parser parser = new Parser(filterExpression);
			final JsonPathFilterExpression expression = parser.parseOrExpression();
			parser.skipWhitespace();
			if (!parser.isAtEnd()) {
				throw parser.error("Unexpected character '" + parser.peek() + "' in filter expression");
			}
			jsonPathElements.push(new JsonPathFilterElement(expression));
			return this;
		} catch (final Exception e) {
			throw new JsonSchemaDefinitionError(e.getMessage(), null, e);
		}
	}

	/**
	 * Parser for path strings and filter expressions.
	 */
	private static final class Parser {
		/**
		 * The parsed text.
		 */
		private final String text;
		/**
		 * The current position within the text.
		 */
		private int position = 0;

		/**
		 * Creates a parser.
		 *
		 * @param text
		 *            the text to parse
		 */
		Parser(final String text) {
			this.text = text == null ? "" : text;
		}

		/**
		 * Parses a complete path.
		 *
		 * @return the path elements, the root element first (if any)
		 * @throws Exception
		 *             if the path is invalid
		 */
		Stack<JsonPathElement> parsePath() throws Exception {
			final Stack<JsonPathElement> elements = new Stack<>();
			skipWhitespace();
			if (isAtEnd()) {
				// Empty json path
				elements.push(new JsonPathRoot("$"));
				return elements;
			} else if (peek() == '#' || peek() == '$') {
				// Root element
				elements.push(new JsonPathRoot(Character.toString(peek())));
				position++;
			}
			parseSegments(elements, false);
			skipWhitespace();
			if (!isAtEnd()) {
				throw error("Invalid JSON path data at '" + peek() + "'");
			}
			return elements;
		}

		/**
		 * Parses path segments up to the end of the path or, within a filter, up to the first
		 * character which is no part of a path.
		 *
		 * @param elements
		 *            the list to add the elements to
		 * @param inFilter
		 *            true for a path within a filter expression
		 */
		void parseSegments(final List<JsonPathElement> elements, final boolean inFilter) throws Exception {
			while (true) {
				if (!inFilter) {
					skipWhitespace();
				}
				if (isAtEnd()) {
					return;
				}
				final char character = peek();
				if (character == '.' && peekAt(1) == '.') {
					position += 2;
					elements.add(new JsonPathRecursiveDescentElement());
					if (!isAtEnd() && peek() == '[') {
						elements.add(parseBracket());
					} else if (!isAtEnd() && peek() == '*') {
						position++;
						elements.add(new JsonPathWildcardElement());
					} else {
						final String name = inFilter ? readFilterName() : readName('.');
						if (name.isEmpty()) {
							throw error("Missing selector after '..'");
						}
						elements.add(new JsonPathPropertyElement(name));
					}
				} else if (character == '.') {
					position++;
					if (isWildcardAhead(inFilter, '.')) {
						position++;
						elements.add(new JsonPathWildcardElement());
					} else {
						final String name = inFilter ? readFilterName() : readName('.');
						if (inFilter && name.isEmpty()) {
							throw error("Missing property name after '.'");
						}
						elements.add(new JsonPathPropertyElement(name));
					}
				} else if (character == '/' && !inFilter) {
					position++;
					if (isWildcardAhead(false, '/')) {
						position++;
						elements.add(new JsonPathWildcardElement());
					} else {
						elements.add(new JsonPathPropertyElement(readName('/')));
					}
				} else if (character == '[') {
					elements.add(parseBracket());
				} else if (inFilter) {
					return;
				} else {
					throw error("Invalid JSON path data at '" + character + "'");
				}
			}
		}

		/**
		 * Checks whether the next character is a wildcard "*" followed by the end of the segment.
		 */
		private boolean isWildcardAhead(final boolean inFilter, final char separatorChar) {
			if (isAtEnd() || peek() != '*') {
				return false;
			}
			final int afterPosition = position + 1;
			if (afterPosition >= text.length()) {
				return true;
			}
			final char afterChar = text.charAt(afterPosition);
			return afterChar == separatorChar || afterChar == '[' || afterChar == '.' || (inFilter && !isFilterNameChar(afterChar)) || (!inFilter && Character.isWhitespace(afterChar));
		}

		/**
		 * Reads a property key of dot or reference notation up to the next unescaped separator or
		 * opening bracket. Escape sequences and JSON pointer escapes are resolved, surrounding
		 * whitespace is removed.
		 */
		private String readName(final char separatorChar) throws Exception {
			final StringBuilder name = new StringBuilder();
			while (!isAtEnd()) {
				final char character = peek();
				if (character == '\\') {
					position++;
					name.append(readEscapedCharacter());
				} else if (character == separatorChar || character == '[') {
					break;
				} else {
					name.append(character);
					position++;
				}
			}
			return resolveJsonPointerEscapes(name.toString().trim());
		}

		/**
		 * Reads a property key in dot notation within a filter expression: letters, digits, "_",
		 * "-", "$" and non-ASCII characters.
		 */
		private String readFilterName() {
			final int start = position;
			while (!isAtEnd() && isFilterNameChar(peek())) {
				position++;
			}
			return text.substring(start, position);
		}

		/**
		 * Checks whether a character may be part of a property key in dot notation within a
		 * filter expression.
		 *
		 * @param character
		 *            the character
		 * @return true, if the character is allowed
		 */
		static boolean isFilterNameChar(final char character) {
			return Character.isLetterOrDigit(character) || character == '_' || character == '-' || character == '$' || character > 127;
		}

		/**
		 * Parses a bracket element: "[?...]", "[*]", "['name']", "[2]", "[-1]", "[1:3]" or a union
		 * like "[0,2]".
		 */
		private JsonPathElement parseBracket() throws Exception {
			expect('[');
			skipWhitespace();
			if (!isAtEnd() && peek() == '?') {
				position++;
				final JsonPathFilterExpression expression = parseOrExpression();
				skipWhitespace();
				expect(']');
				return new JsonPathFilterElement(expression);
			} else if (!isAtEnd() && peek() == '*') {
				position++;
				skipWhitespace();
				expect(']');
				return new JsonPathWildcardElement();
			}

			final List<Selector> selectors = new ArrayList<>();
			while (true) {
				skipWhitespace();
				selectors.add(parseSelector());
				skipWhitespace();
				if (!isAtEnd() && peek() == ',') {
					position++;
				} else {
					expect(']');
					break;
				}
			}

			if (selectors.size() == 1 && selectors.get(0) instanceof NameSelector) {
				return new JsonPathPropertyElement(((NameSelector) selectors.get(0)).propertyKey());
			} else if (selectors.size() == 1 && selectors.get(0) instanceof IndexSelector && ((IndexSelector) selectors.get(0)).index() >= 0) {
				return new JsonPathArrayElement(((IndexSelector) selectors.get(0)).index());
			} else {
				return new JsonPathUnionElement(selectors);
			}
		}

		/**
		 * Parses a selector within brackets: a quoted property key, an index or a slice.
		 */
		private Selector parseSelector() throws Exception {
			if (isAtEnd()) {
				throw error("Unexpected end of JSON path within brackets");
			}
			final char character = peek();
			if (character == '\'' || character == '"') {
				return new NameSelector(resolveJsonPointerEscapes(readQuotedText()));
			} else if (character == '-' || character == ':' || Character.isDigit(character)) {
				final Integer start = readOptionalInteger();
				skipWhitespace();
				if (isAtEnd() || peek() != ':') {
					if (start == null) {
						throw error("Invalid array index");
					}
					return new IndexSelector(start);
				}
				position++;
				skipWhitespace();
				final Integer end = readOptionalInteger();
				skipWhitespace();
				Integer step = null;
				if (!isAtEnd() && peek() == ':') {
					position++;
					skipWhitespace();
					step = readOptionalInteger();
				}
				return new SliceSelector(start, end, step);
			} else {
				throw error("Invalid selector within brackets at '" + character + "'");
			}
		}

		/**
		 * Reads an optional integer (with optional minus sign).
		 *
		 * @return the integer, or null if there is no integer at the current position
		 */
		private Integer readOptionalInteger() throws Exception {
			final int start = position;
			if (!isAtEnd() && peek() == '-') {
				position++;
			}
			while (!isAtEnd() && Character.isDigit(peek())) {
				position++;
			}
			final String number = text.substring(start, position);
			if (number.isEmpty()) {
				return null;
			} else if ("-".equals(number)) {
				throw error("Invalid number '-'");
			}
			try {
				return Integer.valueOf(number);
			} catch (@SuppressWarnings("unused") final NumberFormatException e) {
				throw error("Invalid array index '" + number + "'");
			}
		}

		/**
		 * Parses a filter expression: "||" of "&amp;&amp;" expressions.
		 *
		 * @return the expression
		 */
		JsonPathFilterExpression parseOrExpression() throws Exception {
			JsonPathFilterExpression expression = parseAndExpression();
			while (true) {
				skipWhitespace();
				if (text.startsWith("||", position)) {
					position += 2;
					expression = new Or(expression, parseAndExpression());
				} else {
					return expression;
				}
			}
		}

		/**
		 * Parses "&amp;&amp;" of negations, comparisons and existence tests.
		 */
		private JsonPathFilterExpression parseAndExpression() throws Exception {
			JsonPathFilterExpression expression = parseNotExpression();
			while (true) {
				skipWhitespace();
				if (text.startsWith("&&", position)) {
					position += 2;
					expression = new And(expression, parseNotExpression());
				} else {
					return expression;
				}
			}
		}

		/**
		 * Parses an optional negation "!" of a comparison, existence test or expression in
		 * parentheses.
		 */
		private JsonPathFilterExpression parseNotExpression() throws Exception {
			skipWhitespace();
			if (!isAtEnd() && peek() == '!' && peekAt(1) != '=') {
				position++;
				return new Not(parseNotExpression());
			} else {
				return parseComparison();
			}
		}

		/**
		 * Parses an expression in parentheses, a comparison or an existence test.
		 */
		private JsonPathFilterExpression parseComparison() throws Exception {
			skipWhitespace();
			if (isAtEnd()) {
				throw error("Unexpected end of filter expression");
			} else if (peek() == '(') {
				position++;
				final JsonPathFilterExpression expression = parseOrExpression();
				skipWhitespace();
				expect(')');
				return expression;
			}

			final int operandStart = position;
			final Operand left = parseOperand();
			skipWhitespace();
			final FilterOperator operator = readOperator();
			if (operator == null) {
				if (left instanceof PathOperand) {
					return new Existence((PathOperand) left);
				} else if (left instanceof FunctionCall && ((FunctionCall) left).getFunction().hasLogicalResult()) {
					return new FunctionTest((FunctionCall) left);
				} else if (left instanceof FunctionCall) {
					position = operandStart;
					throw error("Result of function " + ((FunctionCall) left).getFunction().getName() + "() must be compared, it is no test");
				} else {
					throw error("Literal without comparison in filter expression");
				}
			}
			skipWhitespace();
			final Operand right = parseOperand();
			try {
				return new Comparison(left, operator, right);
			} catch (final IllegalArgumentException e) {
				position = operandStart;
				throw error(e.getMessage());
			}
		}

		/**
		 * Reads a comparison operator.
		 *
		 * @return the operator, or null if there is no operator at the current position
		 */
		private FilterOperator readOperator() {
			for (final String symbol : new String[] { "==", "!=", "<=", ">=", "<", ">" }) {
				if (text.startsWith(symbol, position)) {
					position += symbol.length();
					return FilterOperator.getBySymbol(symbol);
				}
			}
			return null;
		}

		/**
		 * Parses an operand of a comparison: a path ("@..." or "$...") or a literal.
		 */
		private Operand parseOperand() throws Exception {
			skipWhitespace();
			if (isAtEnd()) {
				throw error("Unexpected end of filter expression");
			}
			final char character = peek();
			if (character == '@' || character == '$') {
				position++;
				final List<JsonPathElement> elements = new ArrayList<>();
				parseSegments(elements, true);
				return new PathOperand(character == '$', elements);
			} else if (character == '\'' || character == '"') {
				return new Literal(readQuotedText());
			} else if (character == '-' || Character.isDigit(character)) {
				return new Literal(readNumberLiteral());
			} else if (isFunctionCallAhead()) {
				return parseFunctionCall();
			} else if (isKeywordAhead("true")) {
				position += 4;
				return new Literal(Boolean.TRUE);
			} else if (isKeywordAhead("false")) {
				position += 5;
				return new Literal(Boolean.FALSE);
			} else if (isKeywordAhead("null")) {
				position += 4;
				return new Literal(null);
			} else {
				throw error("Unexpected character '" + character + "' in filter expression");
			}
		}

		/**
		 * Checks whether a function call follows: a lower case name directly followed by "(".
		 */
		private boolean isFunctionCallAhead() {
			int index = position;
			if (index >= text.length() || text.charAt(index) < 'a' || text.charAt(index) > 'z') {
				return false;
			}
			while (index < text.length() && ((text.charAt(index) >= 'a' && text.charAt(index) <= 'z') || Character.isDigit(text.charAt(index)) || text.charAt(index) == '_')) {
				index++;
			}
			return index < text.length() && text.charAt(index) == '(';
		}

		/**
		 * Parses a function call like "length(@.name)" or "match(@.version, '2.*')".
		 */
		private FunctionCall parseFunctionCall() throws Exception {
			final int start = position;
			while (peek() != '(') {
				position++;
			}
			final String name = text.substring(start, position);
			final Function function = Function.getByName(name);
			if (function == null) {
				position = start;
				throw error("Unknown function '" + name + "' (supported: length, count, value, match, search)");
			}
			position++;
			final List<Operand> arguments = new ArrayList<>();
			skipWhitespace();
			if (!isAtEnd() && peek() == ')') {
				position++;
			} else {
				while (true) {
					arguments.add(parseOperand());
					skipWhitespace();
					if (!isAtEnd() && peek() == ',') {
						position++;
					} else {
						expect(')');
						break;
					}
				}
			}
			try {
				return new FunctionCall(function, arguments);
			} catch (final IllegalArgumentException e) {
				position = start;
				throw error(e.getMessage());
			}
		}

		/**
		 * Checks whether a keyword like "true" follows, which is not the beginning of a longer name.
		 */
		private boolean isKeywordAhead(final String keyword) {
			return text.startsWith(keyword, position)
					&& (position + keyword.length() >= text.length() || !isFilterNameChar(text.charAt(position + keyword.length())));
		}

		/**
		 * Reads a number literal: a Long for integers (BigDecimal if too large), a Double for
		 * numbers with fraction or exponent.
		 */
		private Number readNumberLiteral() throws Exception {
			final int start = position;
			if (peek() == '-') {
				position++;
			}
			while (!isAtEnd() && (Character.isDigit(peek()) || peek() == '.' || peek() == 'e' || peek() == 'E'
					|| ((peek() == '+' || peek() == '-') && (text.charAt(position - 1) == 'e' || text.charAt(position - 1) == 'E')))) {
				position++;
			}
			final String number = text.substring(start, position);
			try {
				if (number.contains(".") || number.contains("e") || number.contains("E")) {
					return Double.valueOf(number);
				} else {
					try {
						return Long.valueOf(number);
					} catch (@SuppressWarnings("unused") final NumberFormatException e) {
						return new BigDecimal(number);
					}
				}
			} catch (@SuppressWarnings("unused") final NumberFormatException e) {
				throw error("Invalid number literal '" + number + "'");
			}
		}

		/**
		 * Reads a text in single or double quotes, resolving escape sequences.
		 */
		private String readQuotedText() throws Exception {
			final char quoteChar = peek();
			position++;
			final StringBuilder value = new StringBuilder();
			while (true) {
				if (isAtEnd()) {
					throw error("Missing closing quote " + quoteChar);
				}
				final char character = peek();
				position++;
				if (character == quoteChar) {
					return value.toString();
				} else if (character == '\\') {
					value.append(readEscapedCharacter());
				} else {
					value.append(character);
				}
			}
		}

		/**
		 * Reads the character after a backslash: b, f, n, r, t and uXXXX are resolved as in JSON,
		 * every other character stands for itself.
		 */
		private char readEscapedCharacter() throws Exception {
			if (isAtEnd()) {
				throw error("Incomplete escape sequence at end of JSON path");
			}
			final char character = peek();
			position++;
			switch (character) {
				case 'b':
					return '\b';
				case 'f':
					return '\f';
				case 'n':
					return '\n';
				case 'r':
					return '\r';
				case 't':
					return '\t';
				case 'u':
					if (position + 4 > text.length()) {
						throw error("Incomplete unicode escape sequence");
					}
					try {
						final char unicodeChar = (char) Integer.parseInt(text.substring(position, position + 4), 16);
						position += 4;
						return unicodeChar;
					} catch (@SuppressWarnings("unused") final NumberFormatException e) {
						throw error("Invalid unicode escape sequence");
					}
				default:
					return character;
			}
		}

		/**
		 * Resolves JSON pointer escapes (RFC 6901: "~1" for "/", "~0" for "~") and "%25" for "%".
		 */
		private static String resolveJsonPointerEscapes(final String value) {
			// "~1" must be resolved before "~0", otherwise "~01" would wrongly become "/"
			return value.replace("~1", "/").replace("~0", "~").replace("%25", "%");
		}

		/**
		 * Skips whitespace characters.
		 */
		void skipWhitespace() {
			while (!isAtEnd() && Character.isWhitespace(peek())) {
				position++;
			}
		}

		/**
		 * Requires the given character at the current position and skips it.
		 */
		private void expect(final char character) throws Exception {
			if (isAtEnd()) {
				throw error("Expected '" + character + "', but the JSON path ended");
			} else if (peek() != character) {
				throw error("Expected '" + character + "', but found '" + peek() + "'");
			}
			position++;
		}

		/**
		 * Checks whether all characters were read.
		 *
		 * @return true at the end of the text
		 */
		boolean isAtEnd() {
			return position >= text.length();
		}

		/**
		 * Returns the character at the current position.
		 *
		 * @return the current character
		 */
		char peek() {
			return text.charAt(position);
		}

		/**
		 * Returns the character at an offset from the current position.
		 *
		 * @return the character, or 0 behind the end of the text
		 */
		private char peekAt(final int offset) {
			return position + offset < text.length() ? text.charAt(position + offset) : 0;
		}

		/**
		 * Creates an exception with the position within the parsed text.
		 *
		 * @param message
		 *            the error message
		 * @return the exception
		 */
		Exception error(final String message) {
			return new Exception(message + " (at position " + (position + 1) + " of JSON path '" + text + "')");
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

	/**
	 * Checks whether this path can match several nodes, because it contains a wildcard, recursive
	 * descent, filter, slice or union. Such paths are only usable with
	 * {@link de.soderer.json.JsonNode#getDataListByJsonPath}.
	 *
	 * @return true, if this path can match several nodes
	 */
	public boolean canMatchSeveralNodes() {
		for (final JsonPathElement jsonPathElement : jsonPathElements) {
			if (jsonPathElement instanceof JsonPathWildcardElement || jsonPathElement instanceof JsonPathRecursiveDescentElement || jsonPathElement instanceof JsonPathFilterElement
					|| (jsonPathElement instanceof JsonPathUnionElement && !((JsonPathUnionElement) jsonPathElement).isSingular())) {
				return true;
			}
		}
		return false;
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
