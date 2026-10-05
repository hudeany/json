package de.soderer.json.utilities;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * Helper methods for parsing and comparing numbers of any type.
 */
public class NumberUtilities {
	/** Hexadecimal number with prefix "0x" or "0X". */
	private static final Pattern HEX_NUMBER_PATTERN = Pattern.compile("0[xX][0-9A-Fa-f]+");

	/**
	 * Utility class, not to be instantiated.
	 */
	private NumberUtilities() {
	}

	/**
	 * Compares Number objects of any type numerically, e.g. Integer 1 equals Double 1.0
	 *
	 * @param a
	 *            first number
	 * @param b
	 *            second number
	 * @return 1 if a &gt; b, 0 if a = b, -1 if a &lt; b
	 */
	public static int compare(final Number a, final Number b) {
		if (isNonFinite(a) || isNonFinite(b)) {
			// NaN and infinite values cannot be converted to BigDecimal (JSON5 and YAML allow them)
			return Double.compare(a.doubleValue(), b.doubleValue());
		} else {
			return new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString()));
		}
	}

	/**
	 * Checks for NaN and infinite floating point values.
	 *
	 * @param number
	 *            the number
	 * @return true, if the number is a Double or Float with NaN or infinite value
	 */
	private static boolean isNonFinite(final Number number) {
		return (number instanceof Double || number instanceof Float) && !Double.isFinite(number.doubleValue());
	}

	/**
	 * Number in english notation without grouping: optional sign, digits with optional decimals after a dot, optional exponent.
	 */
	private static final Pattern NUMBER_PATTERN = Pattern.compile("[+-]?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([eE][+-]?[0-9]+)?");

	/**
	 * Check for a number in english notation without grouping separators, like {@code -12.5E-3}.
	 *
	 * @param numberString
	 *            string to check
	 * @return true if the string is a number, false for null
	 */
	public static boolean isNumber(final String numberString) {
		return numberString != null && NUMBER_PATTERN.matcher(numberString).matches();
	}

	/**
	 * Number in english notation with optional sign, optional comma grouping of the integer part,
	 * optional decimals after a dot and optional exponent. At least one digit is required before the exponent.
	 */
	private static final Pattern NUMBER_WITH_OPTIONAL_GROUPING_PATTERN = Pattern.compile("[+-]?(?=\\.?\\d)(\\d{1,3}(,\\d{3})+|\\d+)?(\\.\\d*)?([eE][+-]?\\d+)?");

	private static final BigDecimal INTEGER_MIN = BigDecimal.valueOf(Integer.MIN_VALUE);
	private static final BigDecimal INTEGER_MAX = BigDecimal.valueOf(Integer.MAX_VALUE);
	private static final BigDecimal LONG_MIN = BigDecimal.valueOf(Long.MIN_VALUE);
	private static final BigDecimal LONG_MAX = BigDecimal.valueOf(Long.MAX_VALUE);

	/**
	 * Parse a number of unknown type in english notation like {@code 1,234,567.90E-12}.
	 * Comma grouping of the integer part is optional, but if used it must be in groups of three digits.
	 *
	 * <p>The resulting type is the smallest type able to contain the given number without loss of accuracy:</p>
	 * <ul>
	 * <li>Numbers without decimal point and exponent become Integer, Long or BigDecimal.
	 * Byte and Short values are returned as Integer.</li>
	 * <li>Numbers with decimal point or exponent become Float or Double, if the value is exactly
	 * representable by their decimal representation, otherwise BigDecimal.</li>
	 * </ul>
	 *
	 * @param numberString
	 *            number to parse
	 * @return parsed number
	 * @throws NumberFormatException
	 *             if the string is not a number
	 */
	public static Number parseNumber(final String numberString) throws NumberFormatException {
		if (numberString == null || !NUMBER_WITH_OPTIONAL_GROUPING_PATTERN.matcher(numberString).matches()) {
			throw new NumberFormatException("Not a number: '" + numberString + "'");
		}

		final String plainNumberString = numberString.replace(",", "");
		final BigDecimal value = new BigDecimal(plainNumberString);

		final boolean integerNotation = plainNumberString.indexOf('.') < 0 && plainNumberString.indexOf('e') < 0 && plainNumberString.indexOf('E') < 0;
		if (integerNotation) {
			if (value.compareTo(INTEGER_MIN) >= 0 && value.compareTo(INTEGER_MAX) <= 0) {
				return Integer.valueOf(value.intValue());
			} else if (value.compareTo(LONG_MIN) >= 0 && value.compareTo(LONG_MAX) <= 0) {
				return Long.valueOf(value.longValue());
			} else {
				return value;
			}
		} else {
			// Float or Double only if converting back yields exactly the same decimal value
			final float floatValue = value.floatValue();
			if (Float.isFinite(floatValue) && new BigDecimal(Float.toString(floatValue)).compareTo(value) == 0) {
				return Float.valueOf(floatValue);
			}
			final double doubleValue = value.doubleValue();
			if (Double.isFinite(doubleValue) && new BigDecimal(Double.toString(doubleValue)).compareTo(value) == 0) {
				return Double.valueOf(doubleValue);
			}
			return value;
		}
	}

	/**
	 * Checks whether a string is a hexadecimal number with prefix "0x" or "0X", like "0x1F".
	 *
	 * @param numberString
	 *            the string to check
	 * @return true, if the string is a hexadecimal number, false for null
	 */
	public static boolean isHexNumber(final String numberString) {
		return numberString != null && HEX_NUMBER_PATTERN.matcher(numberString).matches();
	}

	/**
	 * Parses a hexadecimal number with prefix "0x" or "0X". The resulting type is the smallest of
	 * Integer, Long and BigInteger able to contain the value.
	 *
	 * @param hexNumberString
	 *            the hexadecimal number, like "0x1F"
	 * @return the parsed number
	 * @throws NumberFormatException
	 *             if the string is not a hexadecimal number
	 */
	public static Number parseHexNumber(final String hexNumberString) throws NumberFormatException {
		if (!isHexNumber(hexNumberString)) {
			throw new NumberFormatException("Not a hex number: '" + hexNumberString + "'");
		} else {
			final BigInteger value = new BigInteger(hexNumberString.substring(2), 16);
			// bitLength excludes the sign bit, so values up to Integer.MAX_VALUE have bitLength 31 at most
			if (value.bitLength() < Integer.SIZE) {
				return value.intValue();
			} else if (value.bitLength() < Long.SIZE) {
				return value.longValue();
			} else {
				return value;
			}
		}
	}

	/**
	 * Checks whether a BigDecimal has an integer value, e.g. 2.00 and 2E+3 are integers, 2.5 is not.
	 *
	 * @param bigDecimal
	 *            the value to check
	 * @return true, if the value is an integer, false for null
	 */
	public static boolean isInteger(final BigDecimal bigDecimal) {
		if (bigDecimal == null) {
			return false;
		}
		final int scale = bigDecimal.scale();
		if (scale <= 0) {
			return true;
		}

		final BigInteger unscaled = bigDecimal.unscaledValue();
		if (unscaled.signum() == 0) {
			return true;
		}

		// For small scales, check trailing zeros via string (avoids BigInteger.pow)
		if (scale <= 18) {
			final String unscaledStr = unscaled.toString();
			return unscaledStr.length() >= scale && unscaledStr.substring(unscaledStr.length() - scale).matches("0+");
		} else {
			// For large scales, use mod (fallback)
			final BigInteger tenToScale = BigInteger.TEN.pow(scale);
			return unscaled.mod(tenToScale).signum() == 0;
		}
	}
}
