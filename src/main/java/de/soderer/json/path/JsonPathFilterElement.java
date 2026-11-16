package de.soderer.json.path;

import java.util.List;

import de.soderer.json.JsonNode;
import de.soderer.json.path.JsonPathFilterExpression.Comparison;
import de.soderer.json.path.JsonPathFilterExpression.Literal;
import de.soderer.json.path.JsonPathFilterExpression.PathOperand;

/**
 * A JSON path filter such as {@code [?(@.version=='26.1.72')]} or
 * {@code [?(@.price < 10 && @.category == 'fiction')]}, which keeps the candidates matching its
 * {@link JsonPathFilterExpression}.
 * <p>
 * The candidates are the property values of an object or the items of an array at this position.
 * After a wildcard or another filter, an object candidate is tested itself, while an array is
 * replaced by its items (see {@link de.soderer.json.JsonNode#getDataListByJsonPath}). Only
 * meaningful with {@link de.soderer.json.JsonNode#getDataListByJsonPath}, which can return several
 * matches - {@link de.soderer.json.JsonNode#getDataByJsonPath} (single result) rejects any path
 * containing a filter.
 * </p>
 */
public class JsonPathFilterElement implements JsonPathElement {
	/**
	 * Comparison operators of filter expressions. The ordering operators apply to two numbers or two
	 * strings, see {@link JsonPathFilterExpression}.
	 */
	public enum FilterOperator {
		/**
		 * Equal ("==").
		 */
		EQUALS("=="),
		/**
		 * Not equal ("!=").
		 */
		NOT_EQUALS("!="),
		/**
		 * Less than ("&lt;").
		 */
		LESS_THAN("<"),
		/**
		 * Less than or equal ("&lt;=").
		 */
		LESS_EQUALS("<="),
		/**
		 * Greater than ("&gt;").
		 */
		GREATER_THAN(">"),
		/**
		 * Greater than or equal ("&gt;=").
		 */
		GREATER_EQUALS(">=");

		/**
		 * The operator symbol.
		 */
		private final String symbol;

		/**
		 * Creates an operator.
		 *
		 * @param symbol
		 *            the operator symbol
		 */
		FilterOperator(final String symbol) {
			this.symbol = symbol;
		}

		/**
		 * Returns the operator symbol.
		 *
		 * @return the symbol, e.g. "=="
		 */
		public String getSymbol() {
			return symbol;
		}

		/**
		 * Returns the operator for a symbol.
		 *
		 * @param symbol
		 *            the symbol, e.g. "=="
		 * @return the operator
		 * @throws IllegalArgumentException
		 *             if the symbol is unknown
		 */
		public static FilterOperator getBySymbol(final String symbol) {
			for (final FilterOperator filterOperator : FilterOperator.values()) {
				if (filterOperator.symbol.equals(symbol)) {
					return filterOperator;
				}
			}
			throw new IllegalArgumentException("Unsupported JSON path filter operator: '" + symbol + "'");
		}
	}
	/**
	 * The filter expression.
	 */
	private final JsonPathFilterExpression expression;

	/**
	 * Creates a simple filter comparing a property of each candidate with a literal, e.g.
	 * {@code [?(@.price < 10)]}.
	 *
	 * @param propertyName
	 *            the property checked on each candidate
	 * @param operator
	 *            the comparison operator
	 * @param literalValue
	 *            the value to compare with: String, Number (e.g. Long or Double), Boolean or null
	 * @throws IllegalArgumentException
	 *             if the property name is empty, the operator is null or the literal has an
	 *             unsupported type
	 */
	public JsonPathFilterElement(final String propertyName, final FilterOperator operator, final Object literalValue) {
		if (propertyName == null || propertyName.isEmpty()) {
			throw new IllegalArgumentException("JSON path filter needs a non-empty property name");
		}
		if (operator == null) {
			throw new IllegalArgumentException("JSON path filter needs an operator");
		}
		expression = new Comparison(PathOperand.ofProperty(propertyName), operator, new Literal(literalValue));
	}

	/**
	 * Creates a filter with any filter expression.
	 *
	 * @param expression
	 *            the filter expression
	 * @throws IllegalArgumentException
	 *             if the expression is null
	 */
	public JsonPathFilterElement(final JsonPathFilterExpression expression) {
		if (expression == null) {
			throw new IllegalArgumentException("JSON path filter needs an expression");
		}
		this.expression = expression;
	}

	/**
	 * Returns the filter expression.
	 *
	 * @return the filter expression
	 */
	public JsonPathFilterExpression getExpression() {
		return expression;
	}

	/**
	 * Checks whether this filter is a simple comparison of a single property of the candidate with
	 * a literal, like {@code [?(@.price < 10)]}, so {@link #getPropertyName()},
	 * {@link #getOperator()} and {@link #getLiteralValue()} describe it completely.
	 *
	 * @return true for a simple property comparison
	 */
	public boolean isSimpleComparison() {
		if (expression instanceof Comparison) {
			final Comparison comparison = (Comparison) expression;
			if (comparison.getLeft() instanceof PathOperand && comparison.getRight() instanceof Literal) {
				final PathOperand path = (PathOperand) comparison.getLeft();
				return !path.isAbsolute() && path.getElements().size() == 1 && path.getElements().get(0) instanceof JsonPathPropertyElement;
			}
		}
		return false;
	}

	/**
	 * Returns the compared property of a simple comparison (see {@link #isSimpleComparison()}).
	 *
	 * @return the property name, or null if this filter is no simple comparison
	 */
	public String getPropertyName() {
		if (isSimpleComparison()) {
			final List<JsonPathElement> elements = ((PathOperand) ((Comparison) expression).getLeft()).getElements();
			return ((JsonPathPropertyElement) elements.get(0)).getPropertyKey();
		} else {
			return null;
		}
	}

	/**
	 * Returns the operator of a simple comparison (see {@link #isSimpleComparison()}).
	 *
	 * @return the operator, or null if this filter is no simple comparison
	 */
	public FilterOperator getOperator() {
		return isSimpleComparison() ? ((Comparison) expression).getOperator() : null;
	}

	/**
	 * Returns the literal of a simple comparison (see {@link #isSimpleComparison()}).
	 *
	 * @return the literal value (String, Number, Boolean or null), or null if this filter is no
	 *         simple comparison
	 */
	public Object getLiteralValue() {
		return isSimpleComparison() ? ((Literal) ((Comparison) expression).getRight()).getLiteralValue() : null;
	}

	/**
	 * Checks whether a candidate matches this filter.
	 *
	 * @param candidate
	 *            the node to test, referenced by "@"
	 * @param rootNode
	 *            the root node, referenced by "$"
	 * @return true, if the candidate matches
	 */
	public boolean matches(final JsonNode candidate, final JsonNode rootNode) {
		return expression.test(candidate, rootNode);
	}

	@Override
	public String toString() {
		return "[?(" + expression + ")]";
	}

	@Override
	public boolean equals(final Object otherObject) {
		return otherObject instanceof JsonPathFilterElement && toString().equals(otherObject.toString());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}
}
