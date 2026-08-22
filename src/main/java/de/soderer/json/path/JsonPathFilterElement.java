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
	public enum FilterOperator {
		EQUALS("=="),
		NOT_EQUALS("!="),
		LESS_THAN("<"),
		LESS_EQUALS("<="),
		GREATER_THAN(">"),
		GREATER_EQUALS(">=");

		private final String symbol;

		FilterOperator(final String symbol) {
			this.symbol = symbol;
		}

		public String getSymbol() {
			return symbol;
		}

		public static FilterOperator getBySymbol(final String symbol) {
			for (final FilterOperator filterOperator : FilterOperator.values()) {
				if (filterOperator.symbol.equals(symbol)) {
					return filterOperator;
				}
			}
			throw new IllegalArgumentException("Unsupported JSON path filter operator: '" + symbol + "'");
		}
	}

	private final String propertyName;
	private final FilterOperator operator;
	private final Object literalValue;

	/**
	 * @param propertyName property name checked on each candidate (the "x" in "@.x")
	 * @param operator comparison operator
	 * @param literalValue value compared against, already parsed to its Java type
	 *        (String, Long, Double, Boolean, or null)
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

	public String getPropertyName() {
		return propertyName;
	}

	public FilterOperator getOperator() {
		return operator;
	}

	public Object getLiteralValue() {
		return literalValue;
	}

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
