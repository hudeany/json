package de.soderer.json.path;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueNull;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPathFilterElement.FilterOperator;

/**
 * Logical expression of a JSON path filter, e.g. the "@.price &lt; 10 &amp;&amp; @.category == 'fiction'"
 * of "$.store.book[?(@.price &lt; 10 &amp;&amp; @.category == 'fiction')]".
 * <p>
 * Supported are comparisons ({@link Comparison}) of paths and literals with ==, !=, &lt;, &lt;=, &gt;
 * and &gt;=, existence tests of paths ({@link Existence}, e.g. "@.isbn"), the logical operators
 * &amp;&amp; ({@link And}), || ({@link Or}) and ! ({@link Not}) and parentheses. Paths start with "@"
 * (the current candidate) or "$" (the root node) and may use all path elements, e.g.
 * "@.address.city", "@.tags[0]" or "$.limits.maxPrice".
 * </p>
 * <p>
 * The functions of RFC 9535 are supported ({@link Function}): "length(@.name) &gt; 3",
 * "count(@.tags[*]) == 2" and "value(@..color) == 'red'" in comparisons ({@link FunctionCall}),
 * "match(@.version, '2[0-9]\\..*')" and "search(@.name, 'nex')" as tests ({@link FunctionTest}).
 * The regular expressions are I-Regexps (RFC 9485).
 * </p>
 * <p>
 * Comparisons follow RFC 9535: numbers are compared by value (1 == 1.0), strings by their code
 * points, objects and arrays structurally (only == and !=). A path which matches no node yields
 * "nothing", which only equals "nothing" ("@.missing == null" is false, "@.missing != 1" is true).
 * The ordering operators are only true for two numbers or two strings, or if == is true for
 * &lt;= and &gt;=. A path used in a comparison must match at most one node, otherwise it yields
 * "nothing".
 * </p>
 */
public abstract class JsonPathFilterExpression {
	/**
	 * Precedence of "||" for formatting.
	 */
	private static final int PRECEDENCE_OR = 1;
	/**
	 * Precedence of "&amp;&amp;" for formatting.
	 */
	private static final int PRECEDENCE_AND = 2;
	/**
	 * Precedence of "!" for formatting.
	 */
	private static final int PRECEDENCE_NOT = 3;
	/**
	 * Precedence of comparisons and existence tests for formatting.
	 */
	private static final int PRECEDENCE_TEST = 4;

	/**
	 * Marker for the result of a path, which matches no node.
	 */
	private static final Object NOTHING = new Object() {
		@Override
		public String toString() {
			return "<nothing>";
		}
	};

	/**
	 * Marker for the JSON null value.
	 */
	private static final Object JSON_NULL = new Object() {
		@Override
		public String toString() {
			return "null";
		}
	};

	/**
	 * Only the nested classes of this class are expressions.
	 */
	JsonPathFilterExpression() {
	}

	/**
	 * Evaluates this expression for a candidate node.
	 *
	 * @param currentNode
	 *            the candidate node, referenced by "@"
	 * @param rootNode
	 *            the root node, referenced by "$"
	 * @return true, if the candidate matches
	 */
	public abstract boolean test(JsonNode currentNode, JsonNode rootNode);

	/**
	 * Returns the precedence of this expression for formatting with the minimum of parentheses.
	 *
	 * @return the precedence, higher binds stronger
	 */
	abstract int getPrecedence();

	/**
	 * Formats this expression, putting it in parentheses if it binds weaker than the surrounding
	 * expression.
	 *
	 * @param surroundingPrecedence
	 *            precedence of the surrounding expression
	 * @return the formatted expression
	 */
	String format(final int surroundingPrecedence) {
		return getPrecedence() < surroundingPrecedence ? "(" + toString() + ")" : toString();
	}

	/**
	 * Two expressions are equal, if their formatted representations are equal.
	 */
	@Override
	public boolean equals(final Object otherObject) {
		return otherObject instanceof JsonPathFilterExpression && toString().equals(otherObject.toString());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Logical "or" ("||") of two expressions.
	 */
	public static final class Or extends JsonPathFilterExpression {
		/**
		 * The left operand.
		 */
		private final JsonPathFilterExpression left;
		/**
		 * The right operand.
		 */
		private final JsonPathFilterExpression right;

		/**
		 * Creates a logical "or".
		 *
		 * @param left
		 *            the left operand
		 * @param right
		 *            the right operand
		 */
		public Or(final JsonPathFilterExpression left, final JsonPathFilterExpression right) {
			this.left = requireOperand(left);
			this.right = requireOperand(right);
		}

		/**
		 * Returns the left operand.
		 *
		 * @return the left operand
		 */
		public JsonPathFilterExpression getLeft() {
			return left;
		}

		/**
		 * Returns the right operand.
		 *
		 * @return the right operand
		 */
		public JsonPathFilterExpression getRight() {
			return right;
		}

		@Override
		public boolean test(final JsonNode currentNode, final JsonNode rootNode) {
			return left.test(currentNode, rootNode) || right.test(currentNode, rootNode);
		}

		@Override
		int getPrecedence() {
			return PRECEDENCE_OR;
		}

		@Override
		public String toString() {
			return left.format(PRECEDENCE_OR) + " || " + right.format(PRECEDENCE_OR + 1);
		}
	}

	/**
	 * Logical "and" ("&amp;&amp;") of two expressions.
	 */
	public static final class And extends JsonPathFilterExpression {
		/**
		 * The left operand.
		 */
		private final JsonPathFilterExpression left;
		/**
		 * The right operand.
		 */
		private final JsonPathFilterExpression right;

		/**
		 * Creates a logical "and".
		 *
		 * @param left
		 *            the left operand
		 * @param right
		 *            the right operand
		 */
		public And(final JsonPathFilterExpression left, final JsonPathFilterExpression right) {
			this.left = requireOperand(left);
			this.right = requireOperand(right);
		}

		/**
		 * Returns the left operand.
		 *
		 * @return the left operand
		 */
		public JsonPathFilterExpression getLeft() {
			return left;
		}

		/**
		 * Returns the right operand.
		 *
		 * @return the right operand
		 */
		public JsonPathFilterExpression getRight() {
			return right;
		}

		@Override
		public boolean test(final JsonNode currentNode, final JsonNode rootNode) {
			return left.test(currentNode, rootNode) && right.test(currentNode, rootNode);
		}

		@Override
		int getPrecedence() {
			return PRECEDENCE_AND;
		}

		@Override
		public String toString() {
			return left.format(PRECEDENCE_AND) + " && " + right.format(PRECEDENCE_AND + 1);
		}
	}

	/**
	 * Logical "not" ("!") of an expression.
	 */
	public static final class Not extends JsonPathFilterExpression {
		/**
		 * The negated expression.
		 */
		private final JsonPathFilterExpression operand;

		/**
		 * Creates a logical "not".
		 *
		 * @param operand
		 *            the negated expression
		 */
		public Not(final JsonPathFilterExpression operand) {
			this.operand = requireOperand(operand);
		}

		/**
		 * Returns the negated expression.
		 *
		 * @return the negated expression
		 */
		public JsonPathFilterExpression getOperand() {
			return operand;
		}

		@Override
		public boolean test(final JsonNode currentNode, final JsonNode rootNode) {
			return !operand.test(currentNode, rootNode);
		}

		@Override
		int getPrecedence() {
			return PRECEDENCE_NOT;
		}

		@Override
		public String toString() {
			// A negated comparison needs parentheses: "!@.a == 1" would be ambiguous
			if (operand instanceof Comparison) {
				return "!(" + operand + ")";
			} else {
				return "!" + operand.format(PRECEDENCE_NOT);
			}
		}
	}

	/**
	 * Existence test of a path, e.g. "@.isbn": true, if the path matches at least one node (also
	 * a JSON null value).
	 */
	public static final class Existence extends JsonPathFilterExpression {
		/**
		 * The tested path.
		 */
		private final PathOperand path;

		/**
		 * Creates an existence test.
		 *
		 * @param path
		 *            the tested path
		 */
		public Existence(final PathOperand path) {
			if (path == null) {
				throw new IllegalArgumentException("JSON path filter existence test needs a path");
			}
			this.path = path;
		}

		/**
		 * Returns the tested path.
		 *
		 * @return the tested path
		 */
		public PathOperand getPath() {
			return path;
		}

		@Override
		public boolean test(final JsonNode currentNode, final JsonNode rootNode) {
			return !path.evaluate(currentNode, rootNode).isEmpty();
		}

		@Override
		int getPrecedence() {
			return PRECEDENCE_TEST;
		}

		@Override
		public String toString() {
			return path.toString();
		}
	}

	/**
	 * Comparison of two operands (paths or literals), e.g. "@.price &lt; 10".
	 */
	public static final class Comparison extends JsonPathFilterExpression {
		/**
		 * The left operand.
		 */
		private final Operand left;
		/**
		 * The comparison operator.
		 */
		private final FilterOperator operator;
		/**
		 * The right operand.
		 */
		private final Operand right;

		/**
		 * Creates a comparison.
		 *
		 * @param left
		 *            the left operand
		 * @param operator
		 *            the comparison operator
		 * @param right
		 *            the right operand
		 */
		public Comparison(final Operand left, final FilterOperator operator, final Operand right) {
			if (left == null || right == null) {
				throw new IllegalArgumentException("JSON path filter comparison needs two operands");
			} else if (operator == null) {
				throw new IllegalArgumentException("JSON path filter comparison needs an operator");
			}
			for (final Operand operand : new Operand[] { left, right }) {
				if (operand instanceof FunctionCall && ((FunctionCall) operand).getFunction().hasLogicalResult()) {
					throw new IllegalArgumentException("Logical result of JSON path filter function " + ((FunctionCall) operand).getFunction().getName() + "() cannot be compared");
				}
			}
			this.left = left;
			this.operator = operator;
			this.right = right;
		}

		/**
		 * Returns the left operand.
		 *
		 * @return the left operand
		 */
		public Operand getLeft() {
			return left;
		}

		/**
		 * Returns the comparison operator.
		 *
		 * @return the operator
		 */
		public FilterOperator getOperator() {
			return operator;
		}

		/**
		 * Returns the right operand.
		 *
		 * @return the right operand
		 */
		public Operand getRight() {
			return right;
		}

		@Override
		public boolean test(final JsonNode currentNode, final JsonNode rootNode) {
			final Object leftValue = left.getValue(currentNode, rootNode);
			final Object rightValue = right.getValue(currentNode, rootNode);
			switch (operator) {
				case EQUALS:
					return valuesEqual(leftValue, rightValue);
				case NOT_EQUALS:
					return !valuesEqual(leftValue, rightValue);
				case LESS_THAN:
					return lessThan(leftValue, rightValue);
				case LESS_EQUALS:
					return lessThan(leftValue, rightValue) || valuesEqual(leftValue, rightValue);
				case GREATER_THAN:
					return lessThan(rightValue, leftValue);
				case GREATER_EQUALS:
					return lessThan(rightValue, leftValue) || valuesEqual(leftValue, rightValue);
				default:
					return false;
			}
		}

		@Override
		int getPrecedence() {
			return PRECEDENCE_TEST;
		}

		@Override
		public String toString() {
			return left + " " + operator.getSymbol() + " " + right;
		}
	}

	/**
	 * Operand of a comparison: a path or a literal.
	 */
	public abstract static class Operand {
		/**
		 * Only the nested classes of {@link JsonPathFilterExpression} are operands.
		 */
		Operand() {
		}

		/**
		 * Returns the comparable value of this operand.
		 *
		 * @param currentNode
		 *            the candidate node, referenced by "@"
		 * @param rootNode
		 *            the root node, referenced by "$"
		 * @return a String, BigDecimal (or Double for non-finite numbers), Boolean, the JSON null
		 *         marker, a JsonObject or JsonArray, or the "nothing" marker
		 */
		abstract Object getValue(JsonNode currentNode, JsonNode rootNode);
	}

	/**
	 * Literal operand: a string, number, boolean or null.
	 */
	public static final class Literal extends Operand {
		/**
		 * The literal value: String, Number, Boolean or null.
		 */
		private final Object value;

		/**
		 * Creates a literal operand.
		 *
		 * @param value
		 *            the value: String, Number (e.g. Long or Double), Boolean or null for JSON null
		 * @throws IllegalArgumentException
		 *             if the value has another type
		 */
		public Literal(final Object value) {
			if (value != null && !(value instanceof String) && !(value instanceof Number) && !(value instanceof Boolean)) {
				throw new IllegalArgumentException("Invalid JSON path filter literal type: " + value.getClass().getName());
			}
			this.value = value;
		}

		/**
		 * Returns the literal value.
		 *
		 * @return String, Number, Boolean or null for JSON null
		 */
		public Object getLiteralValue() {
			return value;
		}

		@Override
		Object getValue(final JsonNode currentNode, final JsonNode rootNode) {
			return toComparableValue(value);
		}

		@Override
		public String toString() {
			if (value == null) {
				return "null";
			} else if (value instanceof String) {
				return "'" + JsonPath.escapeQuotedText((String) value, '\'') + "'";
			} else {
				return value.toString();
			}
		}
	}

	/**
	 * Path operand, relative to the candidate ("@...") or absolute ("$...").
	 */
	public static final class PathOperand extends Operand {
		/**
		 * True for a path starting at the root node ("$").
		 */
		private final boolean absolute;
		/**
		 * The path elements after "@" or "$".
		 */
		private final List<JsonPathElement> elements;

		/**
		 * Creates a path operand.
		 *
		 * @param absolute
		 *            true for a path starting at the root node ("$"), false for a path starting at
		 *            the candidate ("@")
		 * @param elements
		 *            the path elements after "@" or "$", may be empty (the candidate or root itself)
		 */
		public PathOperand(final boolean absolute, final List<JsonPathElement> elements) {
			this.absolute = absolute;
			final List<JsonPathElement> elementsCopy = new ArrayList<>();
			if (elements != null) {
				for (final JsonPathElement element : elements) {
					if (element == null || element instanceof JsonPathRoot) {
						throw new IllegalArgumentException("Invalid JSON path filter path element: " + element);
					}
					elementsCopy.add(element);
				}
			}
			this.elements = Collections.unmodifiableList(elementsCopy);
		}

		/**
		 * Creates a relative path operand for a single property, e.g. "@.price".
		 *
		 * @param propertyKey
		 *            the property key
		 * @return the path operand
		 */
		public static PathOperand ofProperty(final String propertyKey) {
			return new PathOperand(false, List.of(new JsonPathPropertyElement(propertyKey)));
		}

		/**
		 * Returns whether this path starts at the root node ("$").
		 *
		 * @return true for "$", false for "@"
		 */
		public boolean isAbsolute() {
			return absolute;
		}

		/**
		 * Returns the path elements after "@" or "$".
		 *
		 * @return the unmodifiable list of path elements
		 */
		public List<JsonPathElement> getElements() {
			return elements;
		}

		/**
		 * Evaluates this path.
		 *
		 * @param currentNode
		 *            the candidate node, referenced by "@"
		 * @param rootNode
		 *            the root node, referenced by "$"
		 * @return the matching nodes
		 */
		List<JsonNode> evaluate(final JsonNode currentNode, final JsonNode rootNode) {
			return JsonPathEvaluator.evaluateLenient(elements, absolute ? rootNode : currentNode, rootNode);
		}

		@Override
		Object getValue(final JsonNode currentNode, final JsonNode rootNode) {
			final List<JsonNode> nodes = evaluate(currentNode, rootNode);
			if (nodes.size() == 1) {
				return toComparableValue(nodes.get(0));
			} else {
				// No match, or several matches, which are not comparable
				return NOTHING;
			}
		}

		@Override
		public String toString() {
			return (absolute ? "$" : "@") + JsonPath.formatRelativePath(elements);
		}
	}

	/**
	 * The function extensions of RFC 9535.
	 */
	public enum Function {
		/**
		 * "length(value)": number of characters (Unicode code points) of a string, number of items
		 * of an array or number of members of an object; "nothing" for other values.
		 */
		LENGTH("length", false, ArgumentType.VALUE),
		/**
		 * "count(path)": number of nodes matched by a path.
		 */
		COUNT("count", false, ArgumentType.NODES),
		/**
		 * "value(path)": value of the single node matched by a path; "nothing" for no or several
		 * nodes.
		 */
		VALUE("value", false, ArgumentType.NODES),
		/**
		 * "match(string, regex)": true, if the whole string matches the I-Regexp (RFC 9485).
		 */
		MATCH("match", true, ArgumentType.VALUE, ArgumentType.VALUE),
		/**
		 * "search(string, regex)": true, if a part of the string matches the I-Regexp (RFC 9485).
		 */
		SEARCH("search", true, ArgumentType.VALUE, ArgumentType.VALUE);

		/**
		 * The function name.
		 */
		private final String name;
		/**
		 * True for functions with a logical result (usable as test, not in comparisons).
		 */
		private final boolean logicalResult;
		/**
		 * The expected types of the arguments.
		 */
		private final ArgumentType[] argumentTypes;

		Function(final String name, final boolean logicalResult, final ArgumentType... argumentTypes) {
			this.name = name;
			this.logicalResult = logicalResult;
			this.argumentTypes = argumentTypes;
		}

		/**
		 * Returns the function name as used in expressions.
		 *
		 * @return the name, e.g. "length"
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns whether the function has a logical result ("match", "search"), so it is used as a
		 * test, or a value result ("length", "count", "value"), so it is used in comparisons.
		 *
		 * @return true for a logical result
		 */
		public boolean hasLogicalResult() {
			return logicalResult;
		}

		/**
		 * Returns the number of arguments.
		 *
		 * @return the number of arguments
		 */
		public int getArgumentCount() {
			return argumentTypes.length;
		}

		/**
		 * Returns the function for a name.
		 *
		 * @param name
		 *            the function name
		 * @return the function, or null if the name is unknown
		 */
		public static Function getByName(final String name) {
			for (final Function function : values()) {
				if (function.name.equals(name)) {
					return function;
				}
			}
			return null;
		}
	}

	/**
	 * Argument types of functions (RFC 9535 type system).
	 */
	private enum ArgumentType {
		/**
		 * A value: literal, path matching at most one node, or function with a value result.
		 */
		VALUE,
		/**
		 * The nodes of a path.
		 */
		NODES
	}

	/**
	 * Call of a function with a value result ("length", "count", "value") as operand of a
	 * comparison, or of a function with a logical result ("match", "search") as argument of
	 * {@link FunctionTest}.
	 */
	public static final class FunctionCall extends Operand {
		/**
		 * The called function.
		 */
		private final Function function;
		/**
		 * The arguments.
		 */
		private final List<Operand> arguments;

		/**
		 * Creates a function call and checks the arguments.
		 *
		 * @param function
		 *            the function
		 * @param arguments
		 *            the arguments: paths, literals or calls of functions with a value result
		 * @throws IllegalArgumentException
		 *             if the number or the types of the arguments do not fit the function
		 */
		public FunctionCall(final Function function, final List<Operand> arguments) {
			if (function == null) {
				throw new IllegalArgumentException("JSON path filter function call needs a function");
			}
			final List<Operand> argumentsCopy = arguments == null ? List.of() : List.copyOf(arguments);
			if (argumentsCopy.size() != function.getArgumentCount()) {
				throw new IllegalArgumentException("JSON path filter function " + function.getName() + "() needs " + function.getArgumentCount() + " argument(s), but got " + argumentsCopy.size());
			}
			for (int i = 0; i < argumentsCopy.size(); i++) {
				final Operand argument = argumentsCopy.get(i);
				if (function.argumentTypes[i] == ArgumentType.NODES) {
					if (!(argument instanceof PathOperand)) {
						throw new IllegalArgumentException("Argument " + (i + 1) + " of JSON path filter function " + function.getName() + "() must be a path");
					}
				} else if (argument instanceof FunctionCall && ((FunctionCall) argument).function.hasLogicalResult()) {
					throw new IllegalArgumentException("Argument " + (i + 1) + " of JSON path filter function " + function.getName() + "() must be a value, not the logical result of " + ((FunctionCall) argument).function.getName() + "()");
				}
			}
			this.function = function;
			this.arguments = argumentsCopy;
		}

		/**
		 * Returns the called function.
		 *
		 * @return the function
		 */
		public Function getFunction() {
			return function;
		}

		/**
		 * Returns the arguments.
		 *
		 * @return the unmodifiable list of arguments
		 */
		public List<Operand> getArguments() {
			return arguments;
		}

		@Override
		Object getValue(final JsonNode currentNode, final JsonNode rootNode) {
			switch (function) {
				case LENGTH: {
					final Object value = arguments.get(0).getValue(currentNode, rootNode);
					if (value instanceof String) {
						return BigDecimal.valueOf(((String) value).codePointCount(0, ((String) value).length()));
					} else if (value instanceof JsonArray) {
						return BigDecimal.valueOf(((JsonArray) value).size());
					} else if (value instanceof JsonObject) {
						return BigDecimal.valueOf(((JsonObject) value).size());
					} else {
						return NOTHING;
					}
				}
				case COUNT:
					return BigDecimal.valueOf(((PathOperand) arguments.get(0)).evaluate(currentNode, rootNode).size());
				case VALUE: {
					final List<JsonNode> nodes = ((PathOperand) arguments.get(0)).evaluate(currentNode, rootNode);
					return nodes.size() == 1 ? toComparableValue(nodes.get(0)) : NOTHING;
				}
				case MATCH:
				case SEARCH: {
					final Object text = arguments.get(0).getValue(currentNode, rootNode);
					final Object regex = arguments.get(1).getValue(currentNode, rootNode);
					if (!(text instanceof String) || !(regex instanceof String)) {
						return Boolean.FALSE;
					} else if (function == Function.MATCH) {
						return JsonPathIRegexp.matches((String) text, (String) regex);
					} else {
						return JsonPathIRegexp.contains((String) text, (String) regex);
					}
				}
				default:
					return NOTHING;
			}
		}

		@Override
		public String toString() {
			final StringBuilder returnValue = new StringBuilder(function.getName()).append('(');
			for (int i = 0; i < arguments.size(); i++) {
				if (i > 0) {
					returnValue.append(", ");
				}
				returnValue.append(arguments.get(i));
			}
			return returnValue.append(')').toString();
		}
	}

	/**
	 * Test by a function with a logical result, e.g. "match(@.name, 'nex.*')".
	 */
	public static final class FunctionTest extends JsonPathFilterExpression {
		/**
		 * The function call with logical result.
		 */
		private final FunctionCall functionCall;

		/**
		 * Creates a test by a function with a logical result.
		 *
		 * @param functionCall
		 *            the call of "match" or "search"
		 * @throws IllegalArgumentException
		 *             if the function has no logical result
		 */
		public FunctionTest(final FunctionCall functionCall) {
			if (functionCall == null) {
				throw new IllegalArgumentException("JSON path filter function test needs a function call");
			} else if (!functionCall.getFunction().hasLogicalResult()) {
				throw new IllegalArgumentException("Result of JSON path filter function " + functionCall.getFunction().getName() + "() must be compared, it is no test");
			}
			this.functionCall = functionCall;
		}

		/**
		 * Returns the function call.
		 *
		 * @return the function call
		 */
		public FunctionCall getFunctionCall() {
			return functionCall;
		}

		@Override
		public boolean test(final JsonNode currentNode, final JsonNode rootNode) {
			return Boolean.TRUE.equals(functionCall.getValue(currentNode, rootNode));
		}

		@Override
		int getPrecedence() {
			return PRECEDENCE_TEST;
		}

		@Override
		public String toString() {
			return functionCall.toString();
		}
	}

	/**
	 * Requires a non-null operand of a logical expression.
	 *
	 * @param operand
	 *            the operand
	 * @return the operand
	 */
	private static JsonPathFilterExpression requireOperand(final JsonPathFilterExpression operand) {
		if (operand == null) {
			throw new IllegalArgumentException("JSON path filter expression needs an operand");
		}
		return operand;
	}

	/**
	 * Converts a JSON node or a literal into a comparable value.
	 *
	 * @param value
	 *            a JSON node or a literal value (String, Number, Boolean or null)
	 * @return a String, BigDecimal (or Double for non-finite numbers), Boolean, the JSON null
	 *         marker, or the JsonObject or JsonArray itself
	 */
	private static Object toComparableValue(final Object value) {
		if (value == null || value instanceof JsonValueNull) {
			return JSON_NULL;
		} else if (value instanceof JsonValueString) {
			return ((JsonValueString) value).getValue();
		} else if (value instanceof JsonValueInteger) {
			return toComparableNumber(((JsonValueInteger) value).getValue());
		} else if (value instanceof JsonValueNumber) {
			return toComparableNumber(((JsonValueNumber) value).getValue());
		} else if (value instanceof JsonValueBoolean) {
			return ((JsonValueBoolean) value).getValue();
		} else if (value instanceof Number) {
			return toComparableNumber((Number) value);
		} else {
			// String, Boolean, JsonObject or JsonArray
			return value;
		}
	}

	/**
	 * Converts a number into a BigDecimal for exact comparisons. Non-finite values (possible in
	 * JSON5 data) are returned as Double.
	 *
	 * @param number
	 *            the number
	 * @return the BigDecimal, or a Double for NaN and infinite values, or the JSON null marker for
	 *         a null number
	 */
	private static Object toComparableNumber(final Number number) {
		if (number == null) {
			return JSON_NULL;
		} else if (number instanceof BigDecimal) {
			return number;
		} else if (number instanceof BigInteger) {
			return new BigDecimal((BigInteger) number);
		} else if (number instanceof Double || number instanceof Float) {
			final double doubleValue = number.doubleValue();
			if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
				return Double.valueOf(doubleValue);
			} else {
				// Via the text representation, so 0.1 stays 0.1
				return new BigDecimal(number.toString());
			}
		} else {
			return BigDecimal.valueOf(number.longValue());
		}
	}

	/**
	 * Checks two comparable values for equality as defined by RFC 9535.
	 *
	 * @param left
	 *            the first comparable value
	 * @param right
	 *            the second comparable value
	 * @return true, if the values are equal
	 */
	private static boolean valuesEqual(final Object left, final Object right) {
		if (left == NOTHING || right == NOTHING) {
			return left == right;
		} else if (isNumber(left) && isNumber(right)) {
			return compareNumbers(left, right) == 0;
		} else if (left instanceof JsonObject && right instanceof JsonObject) {
			return objectsEqual((JsonObject) left, (JsonObject) right);
		} else if (left instanceof JsonArray && right instanceof JsonArray) {
			return arraysEqual((JsonArray) left, (JsonArray) right);
		} else {
			return left.equals(right);
		}
	}

	/**
	 * Checks whether the first value is less than the second: only for two numbers or two strings.
	 *
	 * @param left
	 *            the first comparable value
	 * @param right
	 *            the second comparable value
	 * @return true, if both are numbers or strings and the first is less than the second
	 */
	private static boolean lessThan(final Object left, final Object right) {
		if (isNumber(left) && isNumber(right)) {
			return compareNumbers(left, right) < 0;
		} else if (left instanceof String && right instanceof String) {
			return compareCodePoints((String) left, (String) right) < 0;
		} else {
			return false;
		}
	}

	private static boolean isNumber(final Object value) {
		return value instanceof BigDecimal || value instanceof Double;
	}

	/**
	 * Compares two comparable numbers. NaN is neither less, nor equal, nor greater than any number.
	 */
	private static int compareNumbers(final Object left, final Object right) {
		if (left instanceof BigDecimal && right instanceof BigDecimal) {
			return ((BigDecimal) left).compareTo((BigDecimal) right);
		} else {
			final double leftValue = ((Number) left).doubleValue();
			final double rightValue = ((Number) right).doubleValue();
			if (Double.isNaN(leftValue) || Double.isNaN(rightValue)) {
				// Not equal and not ordered; a value different from 0 and from "less" for both orders
				return Integer.MAX_VALUE;
			}
			return Double.compare(leftValue, rightValue);
		}
	}

	/**
	 * Compares two strings by their Unicode code points (not by UTF-16 code units), as defined by
	 * RFC 9535.
	 */
	private static int compareCodePoints(final String left, final String right) {
		int leftIndex = 0;
		int rightIndex = 0;
		while (leftIndex < left.length() && rightIndex < right.length()) {
			final int leftCodePoint = left.codePointAt(leftIndex);
			final int rightCodePoint = right.codePointAt(rightIndex);
			if (leftCodePoint != rightCodePoint) {
				return Integer.compare(leftCodePoint, rightCodePoint);
			}
			leftIndex += Character.charCount(leftCodePoint);
			rightIndex += Character.charCount(rightCodePoint);
		}
		return Integer.compare(left.length() - leftIndex, right.length() - rightIndex);
	}

	/**
	 * Structural equality of objects: same keys with equal values (numbers by value).
	 */
	private static boolean objectsEqual(final JsonObject left, final JsonObject right) {
		if (left.size() != right.size()) {
			return false;
		}
		for (final Entry<String, JsonNode> entry : left.entrySet()) {
			if (!right.containsKey(entry.getKey()) || !valuesEqual(toComparableValue(entry.getValue()), toComparableValue(right.get(entry.getKey())))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Structural equality of arrays: same size with equal items in the same order (numbers by
	 * value).
	 */
	private static boolean arraysEqual(final JsonArray left, final JsonArray right) {
		if (left.size() != right.size()) {
			return false;
		}
		final Iterator<JsonNode> leftItems = left.items().iterator();
		final Iterator<JsonNode> rightItems = right.items().iterator();
		while (leftItems.hasNext()) {
			if (!valuesEqual(toComparableValue(leftItems.next()), toComparableValue(rightItems.next()))) {
				return false;
			}
		}
		return true;
	}
}
