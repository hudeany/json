package de.soderer.json.utilities;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

public class NumberUtilities {
	public static DecimalFormat NUMBER_WITH_POINTS = new DecimalFormat("###,##0");
	public static DecimalFormat NUMBER_WITH_MIN_2_DIGITS = new DecimalFormat("00");
	public static DecimalFormat NUMBER_WITH_MIN_4_DIGITS = new DecimalFormat("0000");
	public static DecimalFormat NUMBER_WITH_MIN_6_DIGITS = new DecimalFormat("000000");
	public static DecimalFormat NUMBER_WITH_MIN_7_DIGITS = new DecimalFormat("0000000");

	/**
	 * Check for String of digits
	 *
	 * @param digitString
	 *            string to check
	 * @return true if all characters are digits (also true for an empty string)
	 */
	public static boolean isDigit(final String digitString) {
		for (final char character : digitString.toCharArray()) {
			if (!Character.isDigit(character)) {
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
	 * @return true if the string can be parsed as int
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
	 * @return true if the string can be parsed as long
	 */
	public static boolean isBigInteger(final String value) {
		try {
			Long.parseLong(value);
			return true;
		} catch (@SuppressWarnings("unused") final NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Check for a double value with optional decimals after a dot(.) and exponent
	 *
	 * @param value
	 *            string to check
	 * @return true if the string can be parsed as double
	 */
	public static boolean isDouble(final String value) {
		try {
			Double.parseDouble(value);
			return true;
		} catch (@SuppressWarnings("unused") final NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Compare Number objects
	 *
	 * @param a
	 *            first number
	 * @param b
	 *            second number
	 * @return 1 if a &gt; b, 0 if a = b, -1 if a &lt; b
	 */
	public static int compare(final Number a, final Number b) {
		return new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString()));
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

	public static boolean isHexNumber(final String numberString) {
		return Pattern.matches("0(x|X)[0-9A-Fa-f]+", numberString);
	}

	public static Number parseHexNumber(final String hexNumberString) throws NumberFormatException {
		if (!isHexNumber(hexNumberString)) {
			throw new NumberFormatException("Not a hex number: '" + hexNumberString + "'");
		} else {
			if (hexNumberString.length() < 12) {
				return Integer.parseInt(hexNumberString.substring(2), 16);
			} else {
				final BigInteger value = new BigInteger(hexNumberString.substring(2), 16);
				final boolean isInteger = new BigInteger(Integer.toString(Integer.MIN_VALUE)).compareTo(value) == -1 && value.compareTo(new BigInteger(Integer.toString(Integer.MAX_VALUE))) == -1;
				if (isInteger) {
					return Integer.parseInt(hexNumberString.substring(2), 16);
				} else {
					final boolean isLong = new BigInteger(Long.toString(Long.MIN_VALUE)).compareTo(value) == -1 && value.compareTo(new BigInteger(Long.toString(Long.MAX_VALUE))) == -1;
					if (isLong) {
						return Long.parseLong(hexNumberString.substring(2), 16);
					} else {
						return value;
					}
				}
			}
		}
	}

	public static String formatNumber(final Number number, final char decimalSeparator, final Character groupingSeparator) {
		if (number == null) {
			return null;
		} else {
			String numberString = number.toString();
			if (decimalSeparator != '.') {
				if (groupingSeparator != null) {
					numberString = numberString.replace(',', groupingSeparator);
				} else {
					numberString = numberString.replace(",", "");
				}
				numberString = numberString.replace('.', decimalSeparator);
			}
			return numberString;
		}
	}

	public static String formatNumber(final Number number, final int minPreSeparatorCharacters, final char decimalSeparator, final Character groupingSeparator) {
		if (number == null) {
			return null;
		} else {
			String numberString = number.toString();
			if (decimalSeparator != '.') {
				if (groupingSeparator != null) {
					numberString = numberString.replace(',', groupingSeparator);
				} else {
					numberString = numberString.replace(",", "");
				}
				numberString = numberString.replace('.', decimalSeparator);
			}

			int preSeparatorCharacters = numberString.indexOf(decimalSeparator);
			if (preSeparatorCharacters < 0)
				preSeparatorCharacters = numberString.length();

			if (preSeparatorCharacters < minPreSeparatorCharacters) {
				numberString = Utilities.repeat(" ", minPreSeparatorCharacters - preSeparatorCharacters) + numberString;
			}

			return numberString;
		}
	}

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
