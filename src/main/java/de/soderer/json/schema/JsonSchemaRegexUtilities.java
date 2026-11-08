package de.soderer.json.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Compiles regular expressions of JSON schemas ("pattern", "patternProperties", "regex" format),
 * which are written in ECMA-262 syntax, into Java patterns.<br />
 * <br />
 * The expression is translated where ECMA-262 and Java differ:
 * <ul>
 * <li>"\s" and "\S" use the ECMA-262 white space set (incl. NO-BREAK SPACE, Unicode space
 * separators, LINE/PARAGRAPH SEPARATOR, BOM)</li>
 * <li>"[]" matches nothing and "[^]" matches any character</li>
 * <li>"[" and "&amp;" within a character class are literal characters (Java: nested class and
 * intersection)</li>
 * <li>"\v" is the vertical tab, "\0" the NUL character, "\cx" a control character (also with lower
 * case letter), "&#92;u{...}" a code point</li>
 * <li>Unicode property escapes with long general category names, e.g. "\p{Letter}" or
 * "\p{General_Category=Uppercase_Letter}"; "Script_Extensions=" is approximated by "Script="</li>
 * </ul>
 * {@link #isValidEcmaScriptRegex(String)} additionally rejects constructs which are valid in Java,
 * but not in ECMA-262 (e.g. "\a", "\Q", global inline flags "(?i)", possessive quantifiers, atomic
 * groups).<br />
 * <br />
 * Regular expressions from JSON schemas are untrusted code: a pattern with catastrophic
 * backtracking (ReDoS) may take practically forever on crafted data. {@link #find(Pattern, String, long)}
 * and {@link #matches(Pattern, String, long)} therefore limit the number of character accesses of
 * the regex engine and abort deep recursion of the engine on long input (StackOverflowError).
 */
public final class JsonSchemaRegexUtilities {
	/**
	 * Default base step limit of a regex evaluation, see {@link #getStepLimit(long, int)}. A step is a
	 * character access of the regex engine. 10 million steps take about 30 to 100 milliseconds.
	 */
	public static final long DEFAULT_REGEX_STEP_LIMIT = 10_000_000L;

	/**
	 * Additional steps allowed per character of the input, so that long input data with ordinary
	 * patterns is never affected by the limit.
	 */
	public static final long REGEX_STEPS_PER_INPUT_CHARACTER = 1_000L;

	/** ECMA-262 white space and line terminator characters for use within a character class. */
	private static final String ECMA_WHITE_SPACE = "\\t\\n\\x0B\\f\\r\\x20\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF";

	/** Characters allowed after a backslash in ECMA-262 (unicode mode) besides the special escapes. */
	private static final String ECMA_SYNTAX_CHARACTERS = "^$\\.*+?()[]{}|/-";

	/** Short names of Unicode general categories by their long names (lower case, without "_"). */
	private static final Map<String, String> GENERAL_CATEGORY_SHORT_NAMES = new HashMap<>();
	static {
		final String[][] generalCategories = {
				{ "Letter", "L" }, { "Cased_Letter", "LC" }, { "Uppercase_Letter", "Lu" }, { "Lowercase_Letter", "Ll" }, { "Titlecase_Letter", "Lt" }, { "Modifier_Letter", "Lm" }, { "Other_Letter", "Lo" },
				{ "Mark", "M" }, { "Combining_Mark", "M" }, { "Nonspacing_Mark", "Mn" }, { "Spacing_Mark", "Mc" }, { "Enclosing_Mark", "Me" },
				{ "Number", "N" }, { "Decimal_Number", "Nd" }, { "Digit", "Nd" }, { "Letter_Number", "Nl" }, { "Other_Number", "No" },
				{ "Punctuation", "P" }, { "Punct", "P" }, { "Connector_Punctuation", "Pc" }, { "Dash_Punctuation", "Pd" }, { "Open_Punctuation", "Ps" }, { "Close_Punctuation", "Pe" }, { "Initial_Punctuation", "Pi" }, { "Final_Punctuation", "Pf" }, { "Other_Punctuation", "Po" },
				{ "Symbol", "S" }, { "Math_Symbol", "Sm" }, { "Currency_Symbol", "Sc" }, { "Modifier_Symbol", "Sk" }, { "Other_Symbol", "So" },
				{ "Separator", "Z" }, { "Space_Separator", "Zs" }, { "Line_Separator", "Zl" }, { "Paragraph_Separator", "Zp" },
				{ "Other", "C" }, { "Control", "Cc" }, { "Cntrl", "Cc" }, { "Format", "Cf" }, { "Surrogate", "Cs" }, { "Private_Use", "Co" }, { "Unassigned", "Cn" }
		};
		for (final String[] generalCategory : generalCategories) {
			GENERAL_CATEGORY_SHORT_NAMES.put(normalizePropertyName(generalCategory[0]), generalCategory[1]);
		}
	}

	private JsonSchemaRegexUtilities() {
		// Utility class
	}

	/**
	 * Compiles a regular expression of a JSON schema. Constructs only known by Java regex are still
	 * accepted.
	 *
	 * @param ecmaScriptRegex
	 *            the regular expression in ECMA-262 syntax
	 * @return the Java pattern
	 * @throws PatternSyntaxException
	 *             if the expression is invalid
	 */
	public static Pattern compile(final String ecmaScriptRegex) throws PatternSyntaxException {
		final String javaRegex = translate(ecmaScriptRegex, false);
		try {
			return Pattern.compile(javaRegex);
		} catch (@SuppressWarnings("unused") final StackOverflowError e) {
			// Deeply nested groups exceed the recursion depth of the Java regex compiler
			throw new PatternSyntaxException("Regular expression is nested too deeply", ecmaScriptRegex, -1);
		}
	}

	/**
	 * Searches for a match of a pattern within the input (like {@link Matcher#find()}) with limited
	 * evaluation effort.
	 *
	 * @param pattern
	 *            the pattern
	 * @param input
	 *            the input text
	 * @param stepLimit
	 *            base step limit (see {@link #getStepLimit(long, int)}), 0 or less for no limit
	 * @return true, if the input contains a match
	 * @throws RegexEvaluationLimitException
	 *             if the step limit or the recursion depth of the regex engine is exceeded
	 */
	public static boolean find(final Pattern pattern, final String input, final long stepLimit) throws RegexEvaluationLimitException {
		return evaluate(pattern, input, stepLimit, false);
	}

	/**
	 * Checks whether the whole input matches a pattern (like {@link Matcher#matches()}) with limited
	 * evaluation effort.
	 *
	 * @param pattern
	 *            the pattern
	 * @param input
	 *            the input text
	 * @param stepLimit
	 *            base step limit (see {@link #getStepLimit(long, int)}), 0 or less for no limit
	 * @return true, if the whole input matches
	 * @throws RegexEvaluationLimitException
	 *             if the step limit or the recursion depth of the regex engine is exceeded
	 */
	public static boolean matches(final Pattern pattern, final String input, final long stepLimit) throws RegexEvaluationLimitException {
		return evaluate(pattern, input, stepLimit, true);
	}

	/**
	 * Returns the effective step limit for an input: the base limit plus
	 * {@link #REGEX_STEPS_PER_INPUT_CHARACTER} per input character.
	 *
	 * @param stepLimit
	 *            the base step limit, 0 or less for no limit
	 * @param inputLength
	 *            the length of the input
	 * @return the effective step limit, or {@link Long#MAX_VALUE} for no limit
	 */
	public static long getStepLimit(final long stepLimit, final int inputLength) {
		if (stepLimit <= 0) {
			return Long.MAX_VALUE;
		}
		final long inputSteps = REGEX_STEPS_PER_INPUT_CHARACTER * inputLength;
		return stepLimit > Long.MAX_VALUE - inputSteps ? Long.MAX_VALUE : stepLimit + inputSteps;
	}

	/**
	 * Evaluates a pattern with limited effort.
	 *
	 * @param pattern
	 *            the pattern
	 * @param input
	 *            the input text
	 * @param stepLimit
	 *            base step limit, 0 or less for no limit
	 * @param wholeInput
	 *            true for {@link Matcher#matches()}, false for {@link Matcher#find()}
	 * @return the result of the evaluation
	 * @throws RegexEvaluationLimitException
	 *             if the step limit or the recursion depth of the regex engine is exceeded
	 */
	private static boolean evaluate(final Pattern pattern, final String input, final long stepLimit, final boolean wholeInput) throws RegexEvaluationLimitException {
		final long effectiveStepLimit = getStepLimit(stepLimit, input.length());
		final CharSequence limitedInput = effectiveStepLimit == Long.MAX_VALUE ? input : new StepLimitedCharSequence(input, effectiveStepLimit);
		try {
			final Matcher matcher = pattern.matcher(limitedInput);
			return wholeInput ? matcher.matches() : matcher.find();
		} catch (@SuppressWarnings("unused") final StepLimitExceededException e) {
			throw new RegexEvaluationLimitException("Evaluation exceeded the limit of " + effectiveStepLimit + " steps for input of length " + input.length());
		} catch (@SuppressWarnings("unused") final StackOverflowError e) {
			// The Java regex engine recurses for each repetition of a group, which overflows the stack for long input
			throw new RegexEvaluationLimitException("Evaluation exceeded the recursion depth of the regex engine for input of length " + input.length());
		}
	}

	/**
	 * Input wrapper counting the character accesses of the regex engine. Backtracking is only
	 * possible by reading characters again, so the count bounds the evaluation effort.
	 */
	private static final class StepLimitedCharSequence implements CharSequence {
		/** The wrapped input. */
		private final String input;

		/** Maximum number of character accesses. */
		private final long stepLimit;

		/** Number of character accesses so far. */
		private long steps = 0;

		/**
		 * Creates the wrapper.
		 *
		 * @param input
		 *            the input text
		 * @param stepLimit
		 *            maximum number of character accesses
		 */
		private StepLimitedCharSequence(final String input, final long stepLimit) {
			this.input = input;
			this.stepLimit = stepLimit;
		}

		@Override
		public char charAt(final int index) {
			if (++steps > stepLimit) {
				throw StepLimitExceededException.INSTANCE;
			}
			return input.charAt(index);
		}

		@Override
		public int length() {
			return input.length();
		}

		@Override
		public CharSequence subSequence(final int start, final int end) {
			// Only used for group values, which are not evaluated by the limited methods
			return input.subSequence(start, end);
		}

		@Override
		public String toString() {
			return input;
		}
	}

	/**
	 * Internal signal of an exceeded step limit. It is thrown from within the regex engine, so it
	 * must be unchecked. A shared instance without stack trace keeps aborting cheap.
	 */
	private static final class StepLimitExceededException extends RuntimeException {
		private static final long serialVersionUID = -2416069733154049071L;

		/** Shared instance. */
		private static final StepLimitExceededException INSTANCE = new StepLimitExceededException();

		/**
		 * Creates the shared instance without stack trace.
		 */
		private StepLimitExceededException() {
			super("Regex step limit exceeded", null, false, false);
		}
	}

	/**
	 * Checks whether a text is a valid ECMA-262 regular expression (used by the "regex" format).
	 *
	 * @param ecmaScriptRegex
	 *            the regular expression
	 * @return true, if valid
	 */
	public static boolean isValidEcmaScriptRegex(final String ecmaScriptRegex) {
		try {
			final String javaRegex = translate(ecmaScriptRegex, true);
			if (javaRegex == null) {
				return false;
			}
			Pattern.compile(javaRegex);
			return true;
		} catch (@SuppressWarnings("unused") final PatternSyntaxException e) {
			return false;
		} catch (@SuppressWarnings("unused") final StackOverflowError e) {
			// Deeply nested groups exceed the recursion depth of the Java regex compiler
			return false;
		}
	}

	/**
	 * Translates an ECMA-262 regular expression into Java regex syntax.
	 *
	 * @param regex
	 *            the ECMA-262 regular expression
	 * @param strict
	 *            true to return null for constructs which are not valid in ECMA-262
	 * @return the Java regular expression, or null in strict mode for invalid ECMA-262 expressions
	 */
	private static String translate(final String regex, final boolean strict) {
		final StringBuilder result = new StringBuilder(regex.length() + 16);
		boolean inCharacterClass = false;
		int i = 0;
		while (i < regex.length()) {
			final char character = regex.charAt(i);
			if (character == '\\') {
				if (i + 1 >= regex.length()) {
					if (strict) {
						return null;
					}
					result.append(character);
					i++;
					continue;
				}
				final char escaped = regex.charAt(i + 1);
				switch (escaped) {
					case 's':
						result.append(inCharacterClass ? ECMA_WHITE_SPACE : "[" + ECMA_WHITE_SPACE + "]");
						i += 2;
						break;
					case 'S':
						result.append("[^" + ECMA_WHITE_SPACE + "]");
						i += 2;
						break;
					case 'v':
						result.append("\\x0B");
						i += 2;
						break;
					case '0':
						if (i + 2 < regex.length() && Character.isDigit(regex.charAt(i + 2))) {
							if (strict) {
								return null;
							}
							result.append("\\0");
						} else {
							result.append("\\x00");
						}
						i += 2;
						break;
					case 'c':
						if (i + 2 < regex.length() && isAsciiLetter(regex.charAt(i + 2))) {
							result.append(String.format("\\x%02X", regex.charAt(i + 2) % 32));
							i += 3;
						} else if (strict) {
							return null;
						} else {
							result.append("\\\\c");
							i += 2;
						}
						break;
					case 'u':
						if (i + 2 < regex.length() && regex.charAt(i + 2) == '{') {
							final int closeIndex = regex.indexOf('}', i + 3);
							if (closeIndex < 0 || closeIndex == i + 3 || !isHexText(regex.substring(i + 3, closeIndex))) {
								if (strict) {
									return null;
								}
								result.append("\\u");
								i += 2;
							} else {
								result.append("\\x{").append(regex, i + 3, closeIndex).append('}');
								i = closeIndex + 1;
							}
						} else {
							if (strict && (i + 6 > regex.length() || !isHexText(regex.substring(i + 2, i + 6)))) {
								return null;
							}
							result.append("\\u");
							i += 2;
						}
						break;
					case 'p':
					case 'P':
						if (i + 2 < regex.length() && regex.charAt(i + 2) == '{') {
							final int closeIndex = regex.indexOf('}', i + 3);
							if (closeIndex < 0) {
								if (strict) {
									return null;
								}
								result.append('\\').append(escaped);
								i += 2;
							} else {
								result.append('\\').append(escaped).append('{').append(translateUnicodeProperty(regex.substring(i + 3, closeIndex))).append('}');
								i = closeIndex + 1;
							}
						} else if (strict) {
							return null;
						} else {
							result.append('\\').append(escaped);
							i += 2;
						}
						break;
					case 'd':
					case 'D':
					case 'w':
					case 'W':
					case 'b':
					case 'B':
					case 'f':
					case 'n':
					case 'r':
					case 't':
					case 'x':
					case 'k':
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
					case '8':
					case '9':
						result.append('\\').append(escaped);
						i += 2;
						break;
					default:
						if (ECMA_SYNTAX_CHARACTERS.indexOf(escaped) < 0 && strict) {
							// Identity escapes of other characters (e.g. "\a", "\Q", "\z") are not allowed in ECMA-262 unicode mode
							return null;
						}
						result.append('\\').append(escaped);
						i += 2;
						break;
				}
			} else if (inCharacterClass) {
				if (character == ']') {
					inCharacterClass = false;
					result.append(character);
				} else if (character == '[' || character == '&') {
					// Literal in ECMA-262, nested class or intersection in Java
					result.append('\\').append(character);
				} else {
					result.append(character);
				}
				i++;
			} else if (character == '[') {
				if (regex.startsWith("[]", i)) {
					// Empty class matches nothing
					result.append("(?!)");
					i += 2;
				} else if (regex.startsWith("[^]", i)) {
					// Negated empty class matches any character
					result.append("[\\s\\S]");
					i += 3;
				} else {
					inCharacterClass = true;
					result.append(character);
					i++;
					if (i < regex.length() && regex.charAt(i) == '^') {
						result.append('^');
						i++;
					}
					if (i < regex.length() && regex.charAt(i) == ']') {
						// A leading "]" is literal in Java, but closes the class in ECMA-262 (handled above)
						result.append("\\]");
						i++;
					}
				}
			} else {
				if (strict && character == '(' && regex.startsWith("(?", i) && !isValidEcmaScriptGroupStart(regex, i)) {
					return null;
				}
				if (strict && (character == '+' || character == '*' || character == '?' || character == '}') && i + 1 < regex.length() && regex.charAt(i + 1) == '+' && !isEscapedOrLiteralBrace(regex, i)) {
					// Possessive quantifiers are not known in ECMA-262
					return null;
				}
				result.append(character);
				i++;
			}
		}
		if (inCharacterClass && strict) {
			return null;
		}
		return result.toString();
	}

	/**
	 * Checks whether a group start "(?" is valid in ECMA-262: non-capturing group, lookahead,
	 * lookbehind, named group or modifiers group with ":" ("(?i:...)"). Global inline flags
	 * "(?i)" and atomic groups "(?&gt;" are not.
	 *
	 * @param regex
	 *            the regular expression
	 * @param index
	 *            the index of "("
	 * @return true, if valid
	 */
	private static boolean isValidEcmaScriptGroupStart(final String regex, final int index) {
		if (regex.startsWith("(?:", index) || regex.startsWith("(?=", index) || regex.startsWith("(?!", index) || regex.startsWith("(?<=", index) || regex.startsWith("(?<!", index)) {
			return true;
		} else if (regex.startsWith("(?<", index)) {
			return true;
		}
		int position = index + 2;
		while (position < regex.length() && (isAsciiLetter(regex.charAt(position)) || regex.charAt(position) == '-')) {
			position++;
		}
		// Modifiers are only allowed with a colon, global inline flags like "(?i)" are not
		return position > index + 2 && position < regex.length() && regex.charAt(position) == ':';
	}

	/**
	 * Checks whether a "}" is not the end of a quantifier ("a{2}"), so a following "+" is no
	 * possessive quantifier.
	 *
	 * @param regex
	 *            the regular expression
	 * @param index
	 *            the index of the character before "+"
	 * @return true, if the character is no quantifier end
	 */
	private static boolean isEscapedOrLiteralBrace(final String regex, final int index) {
		if (regex.charAt(index) != '}') {
			return false;
		}
		final int openIndex = regex.lastIndexOf('{', index);
		return openIndex < 0 || !regex.substring(openIndex + 1, index).matches("[0-9]+(?:,[0-9]*)?");
	}

	/**
	 * Translates the content of a Unicode property escape into Java syntax.
	 *
	 * @param property
	 *            the content between "{" and "}", e.g. "Letter" or "Script=Greek"
	 * @return the translated content
	 */
	private static String translateUnicodeProperty(final String property) {
		final int equalsIndex = property.indexOf('=');
		if (equalsIndex < 0) {
			final String shortName = GENERAL_CATEGORY_SHORT_NAMES.get(normalizePropertyName(property));
			return shortName != null ? shortName : property;
		}
		final String propertyName = normalizePropertyName(property.substring(0, equalsIndex));
		final String propertyValue = property.substring(equalsIndex + 1);
		if ("generalcategory".equals(propertyName) || "gc".equals(propertyName)) {
			final String shortName = GENERAL_CATEGORY_SHORT_NAMES.get(normalizePropertyName(propertyValue));
			return "gc=" + (shortName != null ? shortName : propertyValue);
		} else if ("scriptextensions".equals(propertyName) || "scx".equals(propertyName)) {
			return "sc=" + propertyValue;
		} else {
			return property;
		}
	}

	/**
	 * Checks for an ASCII letter.
	 *
	 * @param character
	 *            the character
	 * @return true, if a-z or A-Z
	 */
	private static boolean isAsciiLetter(final char character) {
		return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z');
	}

	/**
	 * Checks whether a text consists of hexadecimal digits only.
	 *
	 * @param text
	 *            the text
	 * @return true, if not empty and all characters are hexadecimal digits
	 */
	private static boolean isHexText(final String text) {
		if (text.isEmpty()) {
			return false;
		}
		for (int i = 0; i < text.length(); i++) {
			final char character = text.charAt(i);
			if (!((character >= '0' && character <= '9') || (character >= 'a' && character <= 'f') || (character >= 'A' && character <= 'F'))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Normalizes a Unicode property name for lookup: lower case without "_", "-" and spaces.
	 *
	 * @param propertyName
	 *            the property name
	 * @return the normalized name
	 */
	private static String normalizePropertyName(final String propertyName) {
		return propertyName.replace("_", "").replace("-", "").replace(" ", "").toLowerCase();
	}
}
