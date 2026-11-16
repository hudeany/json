package de.soderer.json.path;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import de.soderer.json.schema.JsonSchemaRegexUtilities;
import de.soderer.json.schema.RegexEvaluationLimitException;

/**
 * Interoperable regular expressions (I-Regexp, RFC 9485) as used by the JSON path functions
 * {@code match()} and {@code search()} (RFC 9535).
 * <p>
 * An I-Regexp is checked strictly and translated into a {@link java.util.regex.Pattern}:
 * "." matches any character except line feed and carriage return, "^" and "$" are literal
 * characters, groups are non-capturing, character class escapes are limited to the Unicode general
 * categories ("\p{L}", "\P{Nd}", ...). Matching is limited in effort to protect against
 * catastrophic backtracking (see {@link JsonSchemaRegexUtilities}).
 * </p>
 */
final class JsonPathIRegexp {
	/**
	 * Maximum number of translated patterns kept in the cache.
	 */
	private static final int CACHE_SIZE = 256;

	/**
	 * Cache of translated patterns; an empty optional marks an invalid I-Regexp.
	 */
	private static final Map<String, Optional<Pattern>> PATTERN_CACHE = new LinkedHashMap<>(16, 0.75f, true) {
		private static final long serialVersionUID = 5150254357702146428L;

		@Override
		protected boolean removeEldestEntry(final Map.Entry<String, Optional<Pattern>> eldest) {
			return size() > CACHE_SIZE;
		}
	};

	/**
	 * The I-Regexp text.
	 */
	private final String regex;
	/**
	 * The current position as index into the text.
	 */
	private int position = 0;
	/**
	 * The translated Java regular expression.
	 */
	private final StringBuilder javaRegex = new StringBuilder();

	/**
	 * Static entry points only.
	 */
	private JsonPathIRegexp(final String regex) {
		this.regex = regex;
	}

	/**
	 * Checks whether a whole text matches an I-Regexp (function "match").
	 *
	 * @param text
	 *            the text
	 * @param iRegexp
	 *            the I-Regexp
	 * @return true, if the whole text matches; false also for an invalid I-Regexp or if the
	 *         evaluation effort is too high
	 */
	static boolean matches(final String text, final String iRegexp) {
		return evaluate(text, iRegexp, true);
	}

	/**
	 * Checks whether a text contains a match of an I-Regexp (function "search").
	 *
	 * @param text
	 *            the text
	 * @param iRegexp
	 *            the I-Regexp
	 * @return true, if a part of the text matches; false also for an invalid I-Regexp or if the
	 *         evaluation effort is too high
	 */
	static boolean contains(final String text, final String iRegexp) {
		return evaluate(text, iRegexp, false);
	}

	/**
	 * Checks whether a text is a valid I-Regexp.
	 *
	 * @param iRegexp
	 *            the text
	 * @return true, if the text is a valid I-Regexp
	 */
	static boolean isValid(final String iRegexp) {
		return getPattern(iRegexp).isPresent();
	}

	/**
	 * Translates an I-Regexp into a Java regular expression.
	 *
	 * @param iRegexp
	 *            the I-Regexp
	 * @return the Java regular expression
	 * @throws IllegalArgumentException
	 *             if the text is no valid I-Regexp
	 */
	static String toJavaRegex(final String iRegexp) {
		final JsonPathIRegexp translator = new JsonPathIRegexp(iRegexp);
		translator.translateRegexp();
		if (translator.position < translator.regex.length()) {
			throw translator.error("Unexpected character");
		}
		return translator.javaRegex.toString();
	}

	private static boolean evaluate(final String text, final String iRegexp, final boolean wholeText) {
		final Optional<Pattern> pattern = getPattern(iRegexp);
		if (pattern.isEmpty()) {
			return false;
		}
		try {
			if (wholeText) {
				return JsonSchemaRegexUtilities.matches(pattern.get(), text, JsonSchemaRegexUtilities.DEFAULT_REGEX_STEP_LIMIT);
			} else {
				return JsonSchemaRegexUtilities.find(pattern.get(), text, JsonSchemaRegexUtilities.DEFAULT_REGEX_STEP_LIMIT);
			}
		} catch (@SuppressWarnings("unused") final RegexEvaluationLimitException e) {
			// Too expensive to evaluate: treated as no match
			return false;
		}
	}

	private static Optional<Pattern> getPattern(final String iRegexp) {
		if (iRegexp == null) {
			return Optional.empty();
		}
		synchronized (PATTERN_CACHE) {
			final Optional<Pattern> cachedPattern = PATTERN_CACHE.get(iRegexp);
			if (cachedPattern != null) {
				return cachedPattern;
			}
		}
		Optional<Pattern> pattern;
		try {
			pattern = Optional.of(Pattern.compile(toJavaRegex(iRegexp)));
		} catch (@SuppressWarnings("unused") final IllegalArgumentException e) {
			// Invalid I-Regexp (also PatternSyntaxException, a subclass)
			pattern = Optional.empty();
		}
		synchronized (PATTERN_CACHE) {
			PATTERN_CACHE.put(iRegexp, pattern);
		}
		return pattern;
	}

	/**
	 * i-regexp = branch *( "|" branch )
	 */
	private void translateRegexp() {
		translateBranch();
		while (position < regex.length() && regex.charAt(position) == '|') {
			position++;
			javaRegex.append('|');
			translateBranch();
		}
	}

	/**
	 * branch = *piece; piece = atom [ quantifier ]
	 */
	private void translateBranch() {
		while (position < regex.length() && regex.charAt(position) != '|' && regex.charAt(position) != ')') {
			translateAtom();
			translateOptionalQuantifier();
		}
	}

	/**
	 * atom = NormalChar / charClass / ( "(" i-regexp ")" )
	 */
	private void translateAtom() {
		final int codePoint = regex.codePointAt(position);
		if (codePoint == '(') {
			position++;
			javaRegex.append("(?:");
			translateRegexp();
			if (position >= regex.length() || regex.charAt(position) != ')') {
				throw error("Missing ')'");
			}
			position++;
			javaRegex.append(')');
		} else if (codePoint == '.') {
			position++;
			// Any character except line feed and carriage return (also outside the BMP)
			javaRegex.append("[^\\n\\r]");
		} else if (codePoint == '[') {
			translateCharClassExpression();
		} else if (codePoint == '\\') {
			translateEscape(false);
		} else if (isNormalChar(codePoint)) {
			position += Character.charCount(codePoint);
			appendLiteral(codePoint);
		} else {
			throw error("Unexpected character '" + new String(Character.toChars(codePoint)) + "'");
		}
	}

	/**
	 * quantifier = ( "*" / "+" / "?" ) / range-quantifier
	 */
	private void translateOptionalQuantifier() {
		if (position >= regex.length()) {
			return;
		}
		final char character = regex.charAt(position);
		if (character == '*' || character == '+' || character == '?') {
			position++;
			javaRegex.append(character);
		} else if (character == '{') {
			position++;
			final long minimum = readQuantifierNumber();
			long maximum = minimum;
			boolean unbounded = false;
			if (position < regex.length() && regex.charAt(position) == ',') {
				position++;
				if (position < regex.length() && Character.isDigit(regex.charAt(position))) {
					maximum = readQuantifierNumber();
				} else {
					// "{n,}": no upper limit
					unbounded = true;
				}
			}
			if (position >= regex.length() || regex.charAt(position) != '}') {
				throw error("Missing '}'");
			}
			position++;
			if (!unbounded && maximum < minimum) {
				throw error("Invalid range quantifier");
			}
			javaRegex.append('{').append(minimum);
			if (unbounded) {
				javaRegex.append(',');
			} else if (maximum != minimum) {
				javaRegex.append(',').append(maximum);
			}
			javaRegex.append('}');
		}
		// A quantifier must not be followed by another quantifier (no lazy or possessive forms)
		if (position < regex.length() && "*+?{".indexOf(regex.charAt(position)) >= 0) {
			throw error("Unexpected quantifier");
		}
	}

	private long readQuantifierNumber() {
		final int start = position;
		while (position < regex.length() && Character.isDigit(regex.charAt(position))) {
			position++;
		}
		if (start == position) {
			throw error("Missing number in range quantifier");
		} else if (position - start > 9) {
			throw error("Range quantifier too large");
		}
		return Long.parseLong(regex.substring(start, position));
	}

	/**
	 * charClassExpr = "[" [ "^" ] ( "-" / CCE1 ) *CCE1 [ "-" ] "]"
	 */
	private void translateCharClassExpression() {
		position++;
		javaRegex.append('[');
		if (position < regex.length() && regex.charAt(position) == '^') {
			position++;
			javaRegex.append('^');
		}
		boolean first = true;
		while (true) {
			if (position >= regex.length()) {
				throw error("Missing ']'");
			}
			final char character = regex.charAt(position);
			if (character == ']') {
				if (first) {
					throw error("Empty character class");
				}
				position++;
				break;
			} else if (character == '-') {
				// Only allowed as first or last character of the class
				if (first || (position + 1 < regex.length() && regex.charAt(position + 1) == ']')) {
					position++;
					appendLiteral('-');
				} else {
					throw error("Unexpected '-' in character class");
				}
			} else if (character == '\\' && position + 1 < regex.length() && (regex.charAt(position + 1) == 'p' || regex.charAt(position + 1) == 'P')) {
				translateEscape(true);
			} else {
				final int rangeStart = readClassChar();
				if (position + 1 < regex.length() && regex.charAt(position) == '-' && regex.charAt(position + 1) != ']') {
					position++;
					if (regex.charAt(position) == '\\' && position + 1 < regex.length() && (regex.charAt(position + 1) == 'p' || regex.charAt(position + 1) == 'P')) {
						throw error("Category escape as end of a range");
					}
					final int rangeEnd = readClassChar();
					if (rangeEnd < rangeStart) {
						throw error("Invalid character range");
					}
					appendLiteral(rangeStart);
					javaRegex.append('-');
					appendLiteral(rangeEnd);
				} else {
					appendLiteral(rangeStart);
				}
			}
			first = false;
		}
		javaRegex.append(']');
	}

	/**
	 * Reads a CCchar: a character of a class or a single character escape.
	 *
	 * @return the code point
	 */
	private int readClassChar() {
		final int codePoint = regex.codePointAt(position);
		if (codePoint == '\\') {
			position++;
			return readSingleCharEscape();
		} else if (codePoint == '[' || codePoint == ']' || codePoint == '-' || isSurrogate(codePoint)) {
			throw error("Unexpected character in character class");
		} else {
			position += Character.charCount(codePoint);
			return codePoint;
		}
	}

	/**
	 * Translates an escape sequence: single character escape or category escape.
	 *
	 * @param inCharClass
	 *            true within a character class (then only category escapes are expected here)
	 */
	private void translateEscape(final boolean inCharClass) {
		position++;
		if (position >= regex.length()) {
			throw error("Incomplete escape sequence");
		}
		final char escapedChar = regex.charAt(position);
		if (escapedChar == 'p' || escapedChar == 'P') {
			position++;
			if (position >= regex.length() || regex.charAt(position) != '{') {
				throw error("Missing '{' of category escape");
			}
			final int end = regex.indexOf('}', position);
			if (end < 0) {
				throw error("Missing '}' of category escape");
			}
			final String category = regex.substring(position + 1, end);
			if (!isCategory(category)) {
				throw error("Unsupported character category '" + category + "'");
			}
			position = end + 1;
			javaRegex.append('\\').append(escapedChar).append('{').append(category).append('}');
		} else if (inCharClass) {
			throw error("Unexpected escape sequence");
		} else {
			appendLiteral(readSingleCharEscape());
		}
	}

	/**
	 * SingleCharEsc = "\" ( "(" / ")" / "*" / "+" / "-" / "." / "?" / "[" / "\" / "]" / "^" / "n" /
	 * "r" / "t" / "{" / "|" / "}" ); the backslash was already read.
	 *
	 * @return the escaped code point
	 */
	private int readSingleCharEscape() {
		if (position >= regex.length()) {
			throw error("Incomplete escape sequence");
		}
		final char escapedChar = regex.charAt(position);
		position++;
		switch (escapedChar) {
			case 'n':
				return '\n';
			case 'r':
				return '\r';
			case 't':
				return '\t';
			case '(':
			case ')':
			case '*':
			case '+':
			case '-':
			case '.':
			case '?':
			case '[':
			case '\\':
			case ']':
			case '^':
			case '{':
			case '|':
			case '}':
				return escapedChar;
			default:
				throw error("Invalid escape sequence '\\" + escapedChar + "'");
		}
	}

	/**
	 * Checks whether a category name is a Unicode general category allowed by I-Regexp.
	 */
	private static boolean isCategory(final String category) {
		return category.matches("L[lmotu]?|M[cen]?|N[dlo]?|P[cdefios]?|Z[lps]?|S[ckmo]?|C[cfno]?");
	}

	/**
	 * NormalChar: every character except ". \ ? * + { } ( ) [ ] |" and surrogate code points.
	 */
	private static boolean isNormalChar(final int codePoint) {
		return ".\\?*+{}()[]|".indexOf(codePoint) < 0 && !isSurrogate(codePoint);
	}

	private static boolean isSurrogate(final int codePoint) {
		return codePoint >= 0xD800 && codePoint <= 0xDFFF;
	}

	/**
	 * Appends a literal code point to the Java regular expression, escaped if it is no letter or
	 * digit.
	 */
	private void appendLiteral(final int codePoint) {
		if ((codePoint >= 'a' && codePoint <= 'z') || (codePoint >= 'A' && codePoint <= 'Z') || (codePoint >= '0' && codePoint <= '9')) {
			javaRegex.appendCodePoint(codePoint);
		} else {
			javaRegex.append("\\x{").append(Integer.toHexString(codePoint)).append('}');
		}
	}

	private IllegalArgumentException error(final String message) {
		return new IllegalArgumentException(message + " at position " + (position + 1) + " of I-Regexp '" + regex + "'");
	}
}
