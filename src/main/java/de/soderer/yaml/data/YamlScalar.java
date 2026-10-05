package de.soderer.yaml.data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.regex.Pattern;

import de.soderer.json.utilities.NumberUtilities;

/**
 * YAML scalar: a string, number, boolean or null value.
 * <p>
 * Besides the value the scalar keeps its text as written ({@link #getValueString()}), e.g. "0x1F"
 * for the number 31, and its presentation: quote style for strings, and style, chomping and
 * indentation indicator for block scalars. Numbers are equal if they are numerically equal,
 * regardless of their Java type.
 * </p>
 */
public class YamlScalar extends YamlNode {
	/**
	 * Hexadecimal integer like "0x1F".
	 */
	private static final Pattern HEX_PATTERN = Pattern.compile("0x[0-9A-Fa-f_]+");
	/**
	 * Octal integer of YAML 1.2 like "0o17".
	 */
	private static final Pattern OCTAL_YAML12_PATTERN = Pattern.compile("0o[0-7_]+");
	/**
	 * Binary integer of YAML 1.1 like "0b101".
	 */
	private static final Pattern BINARY_PATTERN = Pattern.compile("0b[01_]+");
	/**
	 * Octal integer of YAML 1.1 like "017".
	 */
	private static final Pattern OCTAL_YAML11_PATTERN = Pattern.compile("0[0-7_]+");
	/**
	 * Number with leading zero, which is no valid octal number like "08".
	 */
	private static final Pattern LEADING_ZERO_PATTERN = Pattern.compile("0[0-9_]+");

	/**
	 * The value as written.
	 */
	private final String valueString;
	/**
	 * The value: String, Number, Boolean or null.
	 */
	private final Object value;
	/**
	 * The scalar type.
	 */
	private final YamlScalarType type;
	/**
	 * Quote style of a string, null for default.
	 */
	private YamlStringQuoteType quoteType = null;
	/**
	 * Style of a block scalar.
	 */
	private YamlMultilineScalarType multilineType = YamlMultilineScalarType.LITERAL;
	/**
	 * Preferred chomping of a block scalar. The writer uses it only where it fits the text.
	 */
	private YamlMultilineScalarChompingType multilineChompingType = YamlMultilineScalarChompingType.KEEP;
	/**
	 * Indentation indicator of a block scalar, 0 for automatic detection.
	 */
	private int indentationIndicator = 0;

	/**
	 * Creates a scalar from a Java value. The type is derived from the value type.
	 *
	 * @param value
	 *            a String, Number or Boolean, or null for the null value
	 * @throws RuntimeException
	 *             if the value type is not supported
	 */
	public YamlScalar(final Object value) throws RuntimeException {
		if (value == null) {
			type = YamlScalarType.NULL_VALUE;
			valueString = "null";
			this.value = null;
		} else if (value instanceof String) {
			type = YamlScalarType.STRING;
			valueString = value.toString();
			this.value = value;
		} else if (value instanceof Number) {
			type = YamlScalarType.NUMBER;
			valueString = formatNumber((Number) value);
			this.value = value;
		} else if (value instanceof Boolean) {
			type = YamlScalarType.BOOLEAN;
			valueString = value.toString();
			this.value = value;
		} else {
			throw new RuntimeException("Unsupported type of value object for Yaml scalar: '" + value.getClass().getSimpleName() + "'");
		}
	}

	/**
	 * Creates a scalar from its text as written in YAML.
	 * <p>
	 * For type NUMBER the text is parsed like an unquoted YAML value: decimal and floating point
	 * numbers with optional "_" separators, "0x" hexadecimal, "0o" and leading "0" octal, "0b" binary,
	 * ".inf", "-.inf" and ".nan". A text that is no number (e.g. "12:30" or "1,234") results in a
	 * scalar of type STRING. For type BOOLEAN the texts true/false, yes/no, on/off and y/n are
	 * accepted, ignoring case.
	 * </p>
	 *
	 * @param valueString
	 *            the text as written
	 * @param type
	 *            the scalar type
	 * @throws IllegalArgumentException
	 *             if the type is null
	 * @throws RuntimeException
	 *             if the text is null or no valid boolean for type BOOLEAN
	 */
	public YamlScalar(final String valueString, final YamlScalarType type) throws RuntimeException {
		if (type == null) {
			throw new IllegalArgumentException("ScalarType must not be null");
		}

		if (valueString == null) {
			throw new RuntimeException("Invalid null String value for YAML scalar type: '" + type + "'");
		} else if (type == YamlScalarType.BOOLEAN) {
			this.valueString = valueString;
			if ("true".equalsIgnoreCase(valueString)
					|| "y".equalsIgnoreCase(valueString)
					|| "yes".equalsIgnoreCase(valueString)
					|| "on".equalsIgnoreCase(valueString)) {
				value = true;
				this.type = type;
			} else if ("false".equalsIgnoreCase(valueString)
					|| "n".equalsIgnoreCase(valueString)
					|| "no".equalsIgnoreCase(valueString)
					|| "off".equalsIgnoreCase(valueString)) {
				value = false;
				this.type = type;
			} else {
				throw new RuntimeException("Invalid boolean string repesentation: '" + valueString + "'");
			}
		} else if (type == YamlScalarType.NUMBER) {
			this.valueString = valueString;
			final Number numberValue = parseYamlNumber(valueString);
			if (numberValue != null) {
				value = numberValue;
				this.type = type;
			} else {
				// Not a number, e.g. a sexagesimal value like "12:30" or a text like "1,234"
				value = valueString;
				this.type = YamlScalarType.STRING;
			}
		} else if (type == YamlScalarType.NULL_VALUE) {
			this.valueString = valueString;
			value = null;
			this.type = type;
		} else {
			this.valueString = valueString;
			value = valueString;
			this.type = type;
		}
	}

	/**
	 * Creates a number scalar.
	 *
	 * @param value
	 *            the number, NaN and infinite values are written as .nan and .inf
	 * @param type
	 *            must be NUMBER
	 * @throws IllegalArgumentException
	 *             if the type is null
	 * @throws RuntimeException
	 *             if the value is null or the type is not NUMBER
	 */
	public YamlScalar(final Number value, final YamlScalarType type) throws RuntimeException {
		if (type == null) {
			throw new IllegalArgumentException("ScalarType must not be null");
		}

		if (value == null) {
			throw new RuntimeException("Invalid null value for YAML scalar type for number");
		} else if (type == YamlScalarType.NUMBER) {
			valueString = formatNumber(value);
			this.value = value;
		} else {
			throw new RuntimeException("Invalid YAML scalar type for number: '" + type + "'");
		}

		this.type = type;
	}

	/**
	 * Creates a boolean scalar.
	 *
	 * @param value
	 *            the boolean value
	 * @param type
	 *            must be BOOLEAN
	 * @throws IllegalArgumentException
	 *             if the type is null
	 * @throws RuntimeException
	 *             if the value is null or the type is not BOOLEAN
	 */
	public YamlScalar(final Boolean value, final YamlScalarType type) throws RuntimeException {
		if (type == null) {
			throw new IllegalArgumentException("ScalarType must not be null");
		}

		if (value == null) {
			throw new RuntimeException("Invalid null value for YAML scalar type for boolean");
		} else if (type == YamlScalarType.BOOLEAN) {
			valueString = value.toString();
			this.value = value;
		} else {
			throw new RuntimeException("Invalid YAML scalar type for boolean: '" + type + "'");
		}

		this.type = type;
	}

	/**
	 * Returns the value.
	 *
	 * @return the String, Number or Boolean value, or null for the null value
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Returns the value as written, e.g. "0x1F" for the number 31.
	 *
	 * @return the text, never null
	 */
	public String getValueString() {
		return valueString;
	}

	/**
	 * Returns the scalar type.
	 *
	 * @return the type
	 */
	public YamlScalarType getType() {
		return type;
	}

	/**
	 * Returns the style of a block scalar.
	 *
	 * @return the style, LITERAL by default
	 */
	public YamlMultilineScalarType getMultilineType() {
		return multilineType;
	}

	/**
	 * Returns the quote style of a string.
	 *
	 * @return the quote style, or null to use the writer's default
	 */
	public YamlStringQuoteType getQuoteType() {
		return quoteType;
	}

	/**
	 * Sets the quote style of a string. The writer quotes nevertheless where needed.
	 *
	 * @param quoteType
	 *            the quote style, or null to use the writer's default
	 */
	public void setQuoteType(final YamlStringQuoteType quoteType) {
		this.quoteType = quoteType;
	}

	/**
	 * Sets the quote style of a string.
	 *
	 * @param newQuoteType
	 *            the quote style, or null to use the writer's default
	 * @return this scalar for chaining
	 */
	public YamlScalar withQuoteType(final YamlStringQuoteType newQuoteType) {
		setQuoteType(newQuoteType);
		return this;
	}

	/**
	 * Sets the style of a block scalar.
	 *
	 * @param multilineType
	 *            the style
	 * @throws Exception
	 *             if the style is null
	 */
	public void setMultilineType(final YamlMultilineScalarType multilineType) throws Exception {
		if (multilineType == null) {
			throw new Exception("Invalid empty multilineType");
		}
		this.multilineType = multilineType;
	}

	/**
	 * Sets the style of a block scalar.
	 *
	 * @param newMultilineType
	 *            the style
	 * @return this scalar for chaining
	 * @throws Exception
	 *             if the style is null
	 */
	public YamlScalar withMultilineType(final YamlMultilineScalarType newMultilineType) throws Exception {
		setMultilineType(newMultilineType);
		return this;
	}

	/**
	 * Returns the preferred chomping of a block scalar.
	 *
	 * @return the chomping type, KEEP by default
	 */
	public YamlMultilineScalarChompingType getMultilineChompingType() {
		return multilineChompingType;
	}

	/**
	 * Sets the preferred chomping of a block scalar. The writer derives the chomping indicator
	 * from the trailing linebreaks of the text, so the text is never changed by writing.
	 *
	 * @param multilineChompingType
	 *            the chomping type
	 * @throws Exception
	 *             if the chomping type is null
	 */
	public void setMultilineChompingType(final YamlMultilineScalarChompingType multilineChompingType) throws Exception {
		if (multilineChompingType == null) {
			throw new Exception("Invalid empty multilineChompingType");
		}
		this.multilineChompingType = multilineChompingType;
	}

	/**
	 * Sets the preferred chomping of a block scalar.
	 *
	 * @param newMultilineChompingType
	 *            the chomping type
	 * @return this scalar for chaining
	 * @throws Exception
	 *             if the chomping type is null
	 */
	public YamlScalar withMultilineChompingType(final YamlMultilineScalarChompingType newMultilineChompingType) throws Exception {
		setMultilineChompingType(newMultilineChompingType);
		return this;
	}

	/**
	 * Returns the indentation indicator of a block scalar.
	 *
	 * @return 1 to 9, or 0 for automatic detection
	 */
	public int getIndentationIndicator() {
		return indentationIndicator;
	}

	/**
	 * Sets the indentation indicator of a block scalar.
	 *
	 * @param indentationIndicator
	 *            1 to 9, or 0 for automatic detection
	 * @throws Exception
	 *             if the value is not within 0 to 9
	 */
	public void setIndentationIndicator(final int indentationIndicator) throws Exception {
		if (indentationIndicator < 0 || indentationIndicator > 9) {
			throw new Exception("Invalid multiline scalar indentation indicator: " + indentationIndicator);
		}
		this.indentationIndicator = indentationIndicator;
	}

	/**
	 * Sets the indentation indicator of a block scalar.
	 *
	 * @param newIndentationIndicator
	 *            1 to 9, or 0 for automatic detection
	 * @return this scalar for chaining
	 * @throws Exception
	 *             if the value is not within 0 to 9
	 */
	public YamlScalar withIndentationIndicator(final int newIndentationIndicator) throws Exception {
		setIndentationIndicator(newIndentationIndicator);
		return this;
	}

	/**
	 * Returns the value as text, "null" for the null value.
	 */
	@Override
	public String toString() {
		if (value == null) {
			return "null";
		} else {
			return value.toString();
		}
	}

	/**
	 * Hash code consistent with {@link #equals(Object)}: numerically equal numbers have the same hash
	 * code.
	 */
	@Override
	public int hashCode() {
		if (value instanceof Number) {
			final BigDecimal decimalValue = toBigDecimal((Number) value);
			return Objects.hash(type, decimalValue == null ? Double.valueOf(((Number) value).doubleValue()) : decimalValue.stripTrailingZeros());
		} else {
			return Objects.hash(type, value);
		}
	}

	/**
	 * Two scalars are equal, if they have the same type and value. Numbers are compared numerically,
	 * e.g. Integer 1 and Long 1 are equal.
	 */
	@Override
	public boolean equals(final Object otherObject) {
		if (this == otherObject) {
			return true;
		} else if (otherObject == null) {
			return false;
		} else if (getClass() != otherObject.getClass()) {
			return false;
		} else {
			final YamlScalar other = (YamlScalar) otherObject;
			return type == other.type && valuesEqual(value, other.value);
		}
	}

	/**
	 * Compares two scalar values, numbers numerically.
	 *
	 * @param value
	 *            the first value
	 * @param otherValue
	 *            the second value
	 * @return true, if the values are equal
	 */
	static boolean valuesEqual(final Object value, final Object otherValue) {
		if (value instanceof Number && otherValue instanceof Number) {
			// Compare numerically, because equal values may be stored in different Java number types (e.g. Integer and Long)
			final BigDecimal decimalValue = toBigDecimal((Number) value);
			final BigDecimal otherDecimalValue = toBigDecimal((Number) otherValue);
			if (decimalValue != null && otherDecimalValue != null) {
				return decimalValue.compareTo(otherDecimalValue) == 0;
			} else {
				return Double.compare(((Number) value).doubleValue(), ((Number) otherValue).doubleValue()) == 0;
			}
		} else {
			return Objects.equals(value, otherValue);
		}
	}

	/**
	 * Converts a number for numeric comparison.
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

	/**
	 * Returns a number as YAML text, NaN and infinite values as .nan, .inf and -.inf.
	 *
	 * @param number
	 *            the number
	 * @return the text
	 */
	static String formatNumber(final Number number) {
		if ((number instanceof Double || number instanceof Float) && !Double.isFinite(number.doubleValue())) {
			if (Double.isNaN(number.doubleValue())) {
				return ".nan";
			} else if (number.doubleValue() > 0) {
				return ".inf";
			} else {
				return "-.inf";
			}
		} else {
			return number.toString();
		}
	}

	/**
	 * Parses a text like an unquoted YAML number, see {@link #YamlScalar(String, YamlScalarType)}.
	 *
	 * @param numberString
	 *            the text
	 * @return the number (Integer, Long, BigInteger, Float, Double or BigDecimal), or null if the
	 *         text is no number
	 */
	public static Number parseYamlNumber(final String numberString) {
		try {
			return parseYamlNumberOrFail(numberString);
		} catch (@SuppressWarnings("unused") final NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Parses a text like an unquoted YAML number.
	 *
	 * @param numberString
	 *            the text
	 * @return the number, or null if the text is no number
	 * @throws NumberFormatException
	 *             if the digits are invalid
	 */
	private static Number parseYamlNumberOrFail(final String numberString) throws NumberFormatException {
		final boolean negative = numberString.startsWith("-");
		final String unsignedString = numberString.startsWith("-") || numberString.startsWith("+") ? numberString.substring(1) : numberString;
		if (HEX_PATTERN.matcher(unsignedString).matches()) {
			return parseInteger(unsignedString.substring(2), 16, negative);
		} else if (OCTAL_YAML12_PATTERN.matcher(unsignedString).matches()) {
			return parseInteger(unsignedString.substring(2), 8, negative);
		} else if (BINARY_PATTERN.matcher(unsignedString).matches()) {
			return parseInteger(unsignedString.substring(2), 2, negative);
		} else if (OCTAL_YAML11_PATTERN.matcher(unsignedString).matches()) {
			return parseInteger(unsignedString.substring(1), 8, negative);
		} else if (LEADING_ZERO_PATTERN.matcher(unsignedString).matches()) {
			// Leading zero, but no valid octal number like "08"
			return null;
		} else if (unsignedString.contains(":") || unsignedString.contains(",")) {
			// Sexagesimal number or text with commas, kept as string
			return null;
		} else if (".inf".equalsIgnoreCase(unsignedString)) {
			return negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
		} else if (".nan".equalsIgnoreCase(unsignedString)) {
			return negative ? null : Double.NaN;
		} else {
			// Remove optional thousands separator '_'
			return NumberUtilities.parseNumber(numberString.replace("_", ""));
		}
	}

	/**
	 * Parses integer digits of a radix to the smallest fitting type.
	 *
	 * @param digits
	 *            the digits, may contain "_" separators
	 * @param radix
	 *            the radix
	 * @param negative
	 *            true for a negative number
	 * @return the Integer, Long or BigInteger value
	 * @throws NumberFormatException
	 *             if the digits are invalid
	 */
	private static Number parseInteger(final String digits, final int radix, final boolean negative) throws NumberFormatException {
		BigInteger bigIntegerValue = new BigInteger(digits.replace("_", ""), radix);
		if (negative) {
			bigIntegerValue = bigIntegerValue.negate();
		}
		if (bigIntegerValue.bitLength() < 32) {
			return bigIntegerValue.intValue();
		} else if (bigIntegerValue.bitLength() < 64) {
			return bigIntegerValue.longValue();
		} else {
			return bigIntegerValue;
		}
	}
}
