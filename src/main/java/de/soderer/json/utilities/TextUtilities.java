package de.soderer.json.utilities;

import java.util.regex.Pattern;

/**
 * Text helper methods.
 */
public class TextUtilities {
	/**
	 * A simple string for testing, which includes all german characters
	 */
	public static final String GERMAN_TEST_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"´`^°¹²³*#.,;:=+-~_|½¼¬";

	/**
	 * A simple string for testing, which includes all special characters
	 */
	public static final String SPECIAL_TEST_STRING = "\n\r\t\b\f\u00c4\u00e4\u00d6\u00f6\u00dc\u00fc\u00df";

	/** Pattern for valid Base64 data without line breaks, including correct padding. */
	private static final Pattern BASE64_PATTERN = Pattern.compile("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$");

	/**
	 * Utility class, not to be instantiated.
	 */
	private TextUtilities() {
	}

	/**
	 * Checks whether a string is valid Base64 data (standard alphabet, correct padding, no line
	 * breaks).
	 *
	 * @param value
	 *            the value to check
	 * @return true, if the value is valid Base64 data
	 */
	public static boolean isValidBase64(final String value) {
		return BASE64_PATTERN.matcher(value).matches();
	}

	/**
	 * Replaces the last occurrence of a text. The replacement is literal, "$" and "\" have no
	 * special meaning.
	 *
	 * @param text
	 *            the text to search in
	 * @param searchText
	 *            the text to replace
	 * @param replacement
	 *            the replacement
	 * @return the text with the last occurrence replaced, or the unchanged text if the search text
	 *         was not found
	 */
	public static String replaceLast(final String text, final String searchText, final String replacement) {
		final int lastIndex = text.lastIndexOf(searchText);
		if (lastIndex < 0) {
			return text;
		} else {
			return text.substring(0, lastIndex) + replacement + text.substring(lastIndex + searchText.length());
		}
	}

	/**
	 * Returns the number of unicode code points of a string. Characters outside the basic
	 * multilingual plane (like emojis) count as one, unlike in {@link String#length()}.
	 *
	 * @param input
	 *            the string
	 * @return the number of code points
	 */
	public static int getUnicodeStringLength(final String input) {
		return input.codePointCount(0, input.length());
	}
}
