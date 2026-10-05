package de.soderer.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import de.soderer.json.utilities.NumberUtilities;

/**
 * JSON number value without fraction. The value is stored as Integer, Long, BigInteger or
 * BigDecimal with integer value.
 */
public class JsonValueInteger extends JsonNode {
	private final Number value;

	/**
	 * Creates a JSON integer value.
	 *
	 * @param value
	 *            the value, must not be null (use {@link JsonValueNull} instead)
	 * @throws RuntimeException
	 *             if the value is null
	 */
	public JsonValueInteger(final Integer value) {
		super(JsonDataType.INTEGER);

		if (value == null) {
			throw new RuntimeException("Invalid 'null' value for " + getClass().getSimpleName() + ". Use JsonValueNull instead.");
		} else {
			this.value = value;
		}
	}

	/**
	 * Creates a JSON integer value.
	 *
	 * @param value
	 *            the value, must not be null (use {@link JsonValueNull} instead)
	 * @throws RuntimeException
	 *             if the value is null
	 */
	public JsonValueInteger(final Long value) {
		super(JsonDataType.INTEGER);

		if (value == null) {
			throw new RuntimeException("Invalid 'null' value for " + getClass().getSimpleName() + ". Use JsonValueNull instead.");
		} else {
			this.value = value;
		}
	}

	/**
	 * Creates a JSON integer value.
	 *
	 * @param value
	 *            the value, must not be null (use {@link JsonValueNull} instead)
	 * @throws RuntimeException
	 *             if the value is null
	 */
	public JsonValueInteger(final BigInteger value) {
		super(JsonDataType.INTEGER);

		if (value == null) {
			throw new RuntimeException("Invalid 'null' value for " + getClass().getSimpleName() + ". Use JsonValueNull instead.");
		} else {
			this.value = value;
		}
	}

	/**
	 * Creates a JSON integer value from a BigDecimal with integer value, e.g. 2 or 2.00.
	 *
	 * @param value
	 *            the value, must not be null (use {@link JsonValueNull} instead)
	 * @throws RuntimeException
	 *             if the value is null or has a fraction
	 */
	public JsonValueInteger(final BigDecimal value) {
		super(JsonDataType.INTEGER);

		if (value == null) {
			throw new RuntimeException("Invalid 'null' value for " + getClass().getSimpleName() + ". Use JsonValueNull instead.");
		} else if (!NumberUtilities.isInteger(value)) {
			throw new RuntimeException("Invalid non integer value '" + value.toString() + "' for " + getClass().getSimpleName() + ". Use JsonValueNumber instead.");
		} else {
			this.value = value;
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
		final JsonValueInteger other = (JsonValueInteger) obj;
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
