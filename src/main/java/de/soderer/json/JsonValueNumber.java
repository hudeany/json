package de.soderer.json;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * JSON number value with fraction or exponent (for numbers without fraction see
 * {@link JsonValueInteger}). NaN and infinite values are allowed for JSON5.
 */
public class JsonValueNumber extends JsonNode {
	private final Number value;

	/**
	 * Creates a JSON number value.
	 *
	 * @param value
	 *            the value, must not be null (use {@link JsonValueNull} instead)
	 * @throws RuntimeException
	 *             if the value is null
	 */
	public JsonValueNumber(final Number value) {
		super(JsonDataType.NUMBER);
		this.value = value;

		if (value == null) {
			throw new RuntimeException("Invalid 'null' value for " + getClass().getSimpleName() + ". Use JsonValueNull instead.");
		}
	}

	/**
	 * Returns the value.
	 *
	 * @return the value, never null
	 */
	public Number getValue() {
		return value;
	}

	/**
	 * Hash code consistent with {@link #equals(Object)}: numerically equal values have the same
	 * hash code, regardless of their Java number type.
	 */
	@Override
	public int hashCode() {
		final BigDecimal decimalValue = toBigDecimal(value);
		return decimalValue == null ? Objects.hashCode(value) : decimalValue.stripTrailingZeros().hashCode();
	}

	/**
	 * Two values are equal, if they are numerically equal, e.g. Integer 1 and Long 1.
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final JsonValueNumber other = (JsonValueNumber) obj;
		// Compare numerically, because equal values may be stored in different Java number types (e.g. Integer and Long)
		final BigDecimal decimalValue = toBigDecimal(value);
		final BigDecimal otherDecimalValue = toBigDecimal(other.value);
		if (decimalValue != null && otherDecimalValue != null) {
			return decimalValue.compareTo(otherDecimalValue) == 0;
		} else {
			return Objects.equals(value, other.value);
		}
	}

	/**
	 * Converts a number to BigDecimal for numeric comparison.
	 *
	 * @param number
	 *            the number
	 * @return the number as BigDecimal, or null for NaN and infinite values
	 */
	private static BigDecimal toBigDecimal(final Number number) {
		if (number instanceof BigDecimal) {
			return (BigDecimal) number;
		} else if ((number instanceof Double || number instanceof Float) && !Double.isFinite(number.doubleValue())) {
			return null;
		} else {
			return new BigDecimal(number.toString());
		}
	}

	@Override
	public String toString() {
		if (value == null) {
			return "null";
		} else {
			return value.toString();
		}
	}
}
