package de.soderer.json.path;

import java.util.Objects;

/**
 * A JSONPath filter expression such as {@code [?(@.version=='26.1.72')]}, applied to every
 * candidate produced by the previous path step (every value of a {@link de.soderer.json.JsonObject},
 * or every item of a {@link de.soderer.json.JsonArray}): a candidate matches if it is itself a
 * JSON object, has the given property, and that property's simple value compares as specified
 * against the literal.
 *
 * Only the single-level form "@.propertyName" is supported (no nested "@.a.b" paths). Only
 * meaningful with {@link de.soderer.json.JsonNode#getDataListByJsonPath}, which can return several
 * matches - {@link de.soderer.json.JsonNode#getDataByJsonPath} (single result) rejects any path
 * containing a filter.
 */
public class JsonPathFilterElement implements JsonPathElement {
	/**
	 * Comparison operators of filter expressions. Only EQUALS and NOT_EQUALS apply to
	 * non-numeric values.
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
	 * Property checked on each candidate.
	 */
	private final String propertyName;
	/**
	 * The comparison operator.
	 */
	private final FilterOperator operator;
	/**
	 * The value compared against.
	 */
	private final Object literalValue;

	/**
	 * Creates a filter element.
	 *
	 * @param propertyName
	 *            property name checked on each candidate (the "x" in "@.x")
	 * @param operator
	 *            comparison operator
	 * @param literalValue
	 *            value compared against, already parsed to its Java type (String, Long, Double,
	 *            Boolean, or null)
	 * @throws IllegalArgumentException
	 *             if the property name is empty or the operator is null
	 */
	public JsonPathFilterElement(final String propertyName, final FilterOperator operator, final Object literalValue) {
		if (propertyName == null || propertyName.isEmpty()) {
			throw new IllegalArgumentException("JSON path filter needs a non-empty property name");
		}
		if (operator == null) {
			throw new IllegalArgumentException("JSON path filter needs an operator");
		}
		this.propertyName = propertyName;
		this.operator = operator;
		this.literalValue = literalValue;
	}

	/**
	 * Returns the property checked on each candidate.
	 *
	 * @return the property name
	 */
	public String getPropertyName() {
		return propertyName;
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
	 * Returns the value compared against.
	 *
	 * @return the value: String, Long, Double, Boolean or null
	 */
	public Object getLiteralValue() {
		return literalValue;
	}

	/**
	 * Returns the literal in filter syntax, strings single quoted.
	 *
	 * @return the literal text
	 */
	private String literalValueToString() {
		if (literalValue == null) {
			return "null";
		} else if (literalValue instanceof String) {
			return "'" + ((String) literalValue).replace("'", "\\'") + "'";
		} else {
			return literalValue.toString();
		}
	}

	@Override
	public String toString() {
		return "[?(@." + propertyName + " " + operator.getSymbol() + " " + literalValueToString() + ")]";
	}

	@Override
	public boolean equals(final Object otherObject) {
		if (this == otherObject) {
			return true;
		} else if (!(otherObject instanceof JsonPathFilterElement)) {
			return false;
		} else {
			final JsonPathFilterElement other = (JsonPathFilterElement) otherObject;
			return propertyName.equals(other.propertyName) && operator == other.operator && Objects.equals(literalValue, other.literalValue);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(propertyName, operator, literalValue);
	}
}
