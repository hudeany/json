package de.soderer.json.utilities;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

/**
 * Helpers for checking, parsing, comparing and formatting numbers.
 */
public class NumberUtilities {
	/**
	 * Integer format with grouping separators of the default locale, like "1,234,567".
	 * Note: DecimalFormat is not thread-safe, so concurrent use of this shared instance must be synchronized by the caller.
	 */
	public static final DecimalFormat NUMBER_WITH_POINTS = new DecimalFormat("###,##0");

	/**
	 * Integer format with at least 2 digits (leading zeros), like "07".
	 * Note: DecimalFormat is not thread-safe, so concurrent use of this shared instance must be synchronized by the caller.
	 */
	public static final DecimalFormat NUMBER_WITH_MIN_2_DIGITS = new DecimalFormat("00");

	/**
	 * Integer format with at least 4 digits (leading zeros), like "0007".
	 * Note: DecimalFormat is not thread-safe, so concurrent use of this shared instance must be synchronized by the caller.
	 */
	public static final DecimalFormat NUMBER_WITH_MIN_4_DIGITS = new DecimalFormat("0000");

	/**
	 * Integer format with at least 6 digits (leading zeros), like "000007".
	 * Note: DecimalFormat is not thread-safe, so concurrent use of this shared instance must be synchronized by the caller.
	 */
	public static final DecimalFormat NUMBER_WITH_MIN_6_DIGITS = new DecimalFormat("000000");

	/**
	 * Integer format with at least 7 digits (leading zeros), like "0000007".
	 * Note: DecimalFormat is not thread-safe, so concurrent use of this shared instance must be synchronized by the caller.
	 */
	public static final DecimalFormat NUMBER_WITH_MIN_7_DIGITS = new DecimalFormat("0000000");

	/**
	 * Number in english notation without grouping: optional sign, digits with optional decimals after a dot, optional exponent.
	 */
	private static final Pattern NUMBER_PATTERN = Pattern.compile("[+-]?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([eE][+-]?[0-9]+)?");

	/**
	 * Number in english notation with optional sign, optional comma grouping of the integer part,
	 * optional decimals after a dot and optional exponent. At least one digit is required before the exponent.
	 */
	private static final Pattern NUMBER_WITH_OPTIONAL_GROUPING_PATTERN = Pattern.compile("[+-]?(?=\\.?\\d)(\\d{1,3}(,\\d{3})+|\\d+)?(\\.\\d*)?([eE][+-]?\\d+)?");

	/** Hexadecimal number with "0x" or "0X" prefix */
	private static final Pattern HEX_NUMBER_PATTERN = Pattern.compile("0[xX][0-9A-Fa-f]+");

	private static final BigDecimal INTEGER_MIN = BigDecimal.valueOf(Integer.MIN_VALUE);
	private static final BigDecimal INTEGER_MAX = BigDecimal.valueOf(Integer.MAX_VALUE);
	private static final BigDecimal LONG_MIN = BigDecimal.valueOf(Long.MIN_VALUE);
	private static final BigDecimal LONG_MAX = BigDecimal.valueOf(Long.MAX_VALUE);

	private NumberUtilities() {
		// Utility class
	}

	/**
	 * Check for String of digits. Like {@link #isDigit(char)}, only the characters '0' to '9' are
	 * digits, other Unicode digits are not accepted.
	 *
	 * @param digitString
	 *            string to check
	 * @return true if all characters are digits (also true for an empty string), false for null
	 */
	public static boolean isDigit(final String digitString) {
		if (digitString == null) {
			return false;
		}
		for (int i = 0; i < digitString.length(); i++) {
			if (!isDigit(digitString.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check for a single digit
	 *
	 * @param characterToCheck
	 *            character to check
	 * @return true for the characters '0' to '9'
	 */
	public static boolean isDigit(final char characterToCheck) {
		return characterToCheck >= '0' && characterToCheck <= '9';
	}

	/**
	 * Check for a integer value without decimals
	 *
	 * @param value
	 *            string to check
	 * @return true if the string can be parsed as int, false for null
	 */
	public static boolean isInteger(final String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (@SuppressWarnings("unused") final NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Check for a long integer value without decimals
	 *
	 * @param value
	 *            string to check
	 * @return true if the string can be parsed as long, false for null
	 */
	public static boolean isLong(final String value) {
		try {
			Long.parseLong(value);
			return true;
		} catch (@SuppressWarnings("unused") final NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Check for an integer value of any size without decimals, as accepted by
	 * {@link BigInteger#BigInteger(String)}: an optional sign followed by at least one digit.
	 * Like the parse methods of Integer and Long, this also accepts non-ASCII Unicode digits.
	 *
	 * @param value
	 *            string to check
	 * @return true if the string can be parsed as BigInteger, false for null
	 */
	public static boolean isBigInteger(final String value) {
		if (value == null || value.isEmpty()) {
			return false;
		}
		// Checked character by character instead of creating a BigInteger, because parsing very long strings takes a long time
		int index = 0;
		if (value.charAt(0) == '+' || value.charAt(0) == '-') {
			index = 1;
		}
		if (index >= value.length()) {
			return false;
		}
		for (; index < value.length(); index++) {
			if (Character.digit(value.charAt(index), 10) < 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check whether a string can be parsed by {@link Double#parseDouble(String)}. Like that method,
	 * this also accepts surrounding whitespace, "NaN", "Infinity", hexadecimal floating point
	 * notation and type suffixes like "1d" or "1f". Use {@link #isNumber(String)} for a strict check.
	 *
	 * @param value
	 *            string to check
	 * @return true if the string can be parsed as double, false for null
	 */
	public static boolean isDouble(final String value) {
		if (value == null) {
			// Double.parseDouble(null) throws a NullPointerException instead of a NumberFormatException
			return false;
		}
		try {
			Double.parseDouble(value);
			return true;
		} catch (@SuppressWarnings("unused") final NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Compare Number objects by their numeric value, regardless of their type. NaN is greater than
	 * all other values (including positive infinity) and equal to itself, like in
	 * {@link Double#compare(double, double)}.
	 *
	 * @param a
	 *            first number, not null
	 * @param b
	 *            second number, not null
	 * @return 1 if a &gt; b, 0 if a = b, -1 if a &lt; b
	 * @throws NullPointerException
	 *             if a or b is null
	 */
	public static int compare(final Number a, final Number b) {
		if (a == null || b == null) {
			throw new NullPointerException("Numbers to compare must not be null");
		}
		final boolean aNonFinite = isNonFinite(a);
		final boolean bNonFinite = isNonFinite(b);
		if (aNonFinite && bNonFinite) {
			return Integer.signum(Double.compare(a.doubleValue(), b.doubleValue()));
		} else if (aNonFinite) {
			// NaN and infinity have no BigDecimal representation. They are compared directly, because a finite BigDecimal beyond the
			// double range would become infinity by doubleValue() and wrongly compare as equal.
			return nonFiniteSign(a.doubleValue());
		} else if (bNonFinite) {
			return -nonFiniteSign(b.doubleValue());
		} else {
			return toBigDecimal(a).compareTo(toBigDecimal(b));
		}
	}

	private static boolean isNonFinite(final Number number) {
		return (number instanceof Double || number instanceof Float) && !Double.isFinite(number.doubleValue());
	}

	/**
	 * Comparison result of a NaN or infinite value against any finite value
	 */
	private static int nonFiniteSign(final double nonFiniteValue) {
		return nonFiniteValue == Double.NEGATIVE_INFINITY ? -1 : 1;
	}

	private static BigDecimal toBigDecimal(final Number number) {
		if (number instanceof BigDecimal) {
			return (BigDecimal) number;
		} else if (number instanceof BigInteger) {
			return new BigDecimal((BigInteger) number);
		} else {
			// toString keeps the decimal representation of Float and Double, e.g. 0.1f stays 0.1
			return new BigDecimal(number.toString());
		}
	}

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
	 * Check for a hexadecimal number with "0x" or "0X" prefix, like {@code 0x1F}.
	 *
	 * @param numberString
	 *            string to check
	 * @return true if the string is a prefixed hexadecimal number, false for null
	 */
	public static boolean isHexNumber(final String numberString) {
		return numberString != null && HEX_NUMBER_PATTERN.matcher(numberString).matches();
	}

	/**
	 * Parse a hexadecimal number with "0x" or "0X" prefix as non-negative value.
	 * The resulting type is the smallest of Integer, Long or BigInteger able to contain the value.
	 *
	 * @param hexNumberString
	 *            hexadecimal number like {@code 0xFFFFFFFF}
	 * @return parsed number, e.g. Long 4294967295 for {@code 0xFFFFFFFF}
	 * @throws NumberFormatException
	 *             if the string is not a prefixed hexadecimal number
	 */
	public static Number parseHexNumber(final String hexNumberString) throws NumberFormatException {
		if (!isHexNumber(hexNumberString)) {
			throw new NumberFormatException("Not a hex number: '" + hexNumberString + "'");
		} else {
			// Choose the type by the bit length of the value
			final BigInteger value = new BigInteger(hexNumberString.substring(2), 16);
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
	 * Format a number by its {@link Object#toString()} representation with another decimal separator.
	 * If the decimal separator is '.', the text is returned unchanged.
	 *
	 * @param number
	 *            number to format, may be null
	 * @param decimalSeparator
	 *            decimal separator replacing '.'
	 * @param groupingSeparator
	 *            grouping separator replacing ',' in the text, null to remove ',' characters
	 * @return formatted number, or null for null input
	 */
	public static String formatNumber(final Number number, final char decimalSeparator, final Character groupingSeparator) {
		if (number == null) {
			return null;
		}
		final String numberString = number.toString();
		if (decimalSeparator == '.') {
			return numberString;
		}

		// Single pass, so a grouping separator '.' is not replaced by the decimal separator afterwards
		final StringBuilder result = new StringBuilder(numberString.length());
		for (final char character : numberString.toCharArray()) {
			if (character == '.') {
				result.append(decimalSeparator);
			} else if (character == ',') {
				if (groupingSeparator != null) {
					result.append(groupingSeparator.charValue());
				}
			} else {
				result.append(character);
			}
		}
		return result.toString();
	}

	/**
	 * Format a number like {@link #formatNumber(Number, char, Character)} and right-align it by prepending blanks,
	 * so that the part before the decimal separator has at least the given number of characters.
	 *
	 * @param number
	 *            number to format, may be null
	 * @param minPreSeparatorCharacters
	 *            minimum number of characters before the decimal separator
	 * @param decimalSeparator
	 *            decimal separator replacing '.'
	 * @param groupingSeparator
	 *            grouping separator replacing ',' in the text, null to remove ',' characters
	 * @return formatted number, or null for null input
	 */
	public static String formatNumber(final Number number, final int minPreSeparatorCharacters, final char decimalSeparator, final Character groupingSeparator) {
		final String numberString = formatNumber(number, decimalSeparator, groupingSeparator);
		if (numberString == null) {
			return null;
		}

		int preSeparatorCharacters = numberString.indexOf(decimalSeparator);
		if (preSeparatorCharacters < 0) {
			preSeparatorCharacters = numberString.length();
		}

		if (preSeparatorCharacters < minPreSeparatorCharacters) {
			return " ".repeat(minPreSeparatorCharacters - preSeparatorCharacters) + numberString;
		} else {
			return numberString;
		}
	}

	/**
	 * Check whether a BigDecimal has an integer value (no non-zero decimals), like "5.000".
	 *
	 * @param bigDecimal
	 *            value to check
	 * @return true if the value has no fractional part, false for null
	 */
	public static boolean isInteger(final BigDecimal bigDecimal) {
		if (bigDecimal == null) {
			return false;
		} else if (bigDecimal.scale() <= 0 || bigDecimal.signum() == 0) {
			return true;
		} else if (bigDecimal.scale() >= bigDecimal.precision()) {
			// More decimal places than digits, so the absolute value is between 0 and 1 (exclusive)
			return false;
		} else {
			// Effort depends on the number of digits, not on the scale (BigInteger.TEN.pow(scale) took very long for large scales)
			return bigDecimal.stripTrailingZeros().scale() <= 0;
		}
	}
}
