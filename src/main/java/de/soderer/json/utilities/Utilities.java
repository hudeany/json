package de.soderer.json.utilities;

import java.io.Closeable;
import java.io.IOException;
import java.util.Base64;

/**
 * General helper methods for strings, arrays and collections.
 */
public class Utilities {
	/**
	 * Utility class, not to be instantiated.
	 */
	private Utilities() {
	}

	/**
	 * Encodes data as Base64 string (standard alphabet, with padding, without line breaks).
	 *
	 * @param clearData
	 *            data to encode
	 * @return Base64 string
	 */
	public static String encodeBase64(final byte[] clearData) {
		return Base64.getEncoder().encodeToString(clearData);
	}

	/**
	 * Checks whether a string is null, empty or contains only whitespace.
	 *
	 * @param value
	 *            the string to check
	 * @return true, if the string is blank
	 */
	public static boolean isBlank(final String value) {
		return value == null || value.trim().length() == 0;
	}

	/**
	 * Checks whether a string contains at least one non-whitespace character.
	 *
	 * @param value
	 *            the string to check
	 * @return true, if the string is not blank
	 */
	public static boolean isNotBlank(final String value) {
		return !isBlank(value);
	}

	/**
	 * Closes an item and ignores any IOException thrown by its close method.
	 *
	 * @param closeable
	 *            the item to close, may be null
	 */
	public static void closeQuietly(final Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (@SuppressWarnings("unused") final IOException e) {
				// Do nothing
			}
		}
	}

	/**
	 * Repeats a string.
	 *
	 * @param value
	 *            the string to repeat
	 * @param count
	 *            the number of repetitions
	 * @return the repeated string, an empty string for 0 repetitions, or null if the value is null
	 */
	public static String repeat(final String value, final int count) {
		return repeat(value, count, null);
	}

	/**
	 * Repeats a string with a separator between the repetitions.
	 *
	 * @param value
	 *            the string to repeat
	 * @param count
	 *            the number of repetitions
	 * @param separatorString
	 *            the separator between the repetitions, or null for none
	 * @return the repeated string, an empty string for 0 repetitions, or null if the value is null
	 */
	public static String repeat(final String value, final int count, final String separatorString) {
		if (value == null) {
			return null;
		} else if (value.length() == 0 || count <= 0) {
			return "";
		} else {
			final StringBuilder returnValue = new StringBuilder();
			for (int i = 0; i < count; i++) {
				if (separatorString != null && i > 0) {
					returnValue.append(separatorString);
				}
				returnValue.append(value);
			}
			return returnValue.toString();
		}
	}

	/**
	 * Joins the string representations of array items. Null items are joined as empty strings.
	 *
	 * @param array
	 *            the items to join
	 * @param glue
	 *            the separator between the items, or null for none
	 * @return the joined string, or null if the array is null
	 */
	public static String join(final Object[] array, final String glue) {
		if (array == null) {
			return null;
		} else {
			final StringBuilder returnValue = new StringBuilder();
			for (int i = 0; i < array.length; i++) {
				if (i > 0 && glue != null) {
					returnValue.append(glue);
				}
				if (array[i] != null) {
					returnValue.append(array[i].toString());
				}
			}
			return returnValue.toString();
		}
	}

	/**
	 * Joins characters.
	 *
	 * @param array
	 *            the characters to join
	 * @param glue
	 *            the separator between the characters, or null for none
	 * @return the joined string, or null if the array is null
	 */
	public static String join(final char[] array, final String glue) {
		if (array == null) {
			return null;
		} else {
			final StringBuilder returnValue = new StringBuilder();
			for (int i = 0; i < array.length; i++) {
				if (i > 0 && glue != null) {
					returnValue.append(glue);
				}
				returnValue.append(array[i]);
			}
			return returnValue.toString();
		}
	}

	/**
	 * Joins the string representations of items. Null items are joined as empty strings.
	 *
	 * @param iterableObject
	 *            the items to join
	 * @param glue
	 *            the separator between the items, or null for none
	 * @return the joined string, or null if the items are null
	 */
	public static String join(final Iterable<?> iterableObject, final String glue) {
		if (iterableObject == null) {
			return null;
		} else {
			final StringBuilder returnValue = new StringBuilder();
			boolean isFirst = true;
			for (final Object object : iterableObject) {
				if (!isFirst && glue != null) {
					returnValue.append(glue);
				}
				if (object != null) {
					returnValue.append(object.toString());
				}
				isFirst = false;
			}
			return returnValue.toString();
		}
	}

	/**
	 * Shortens a string to a maximum length by cutting off its start, which is replaced by "... ".
	 *
	 * @param value
	 *            the string, may be null
	 * @param maxLength
	 *            the maximum length of the result including the "... " prefix
	 * @return the shortened string, or the unchanged string if it is not longer than the maximum
	 *         length
	 */
	public static String shortenStringToMaxLengthCutLeft(final String value, final int maxLength) {
		final String cutPrefix = "... ";
		if (value == null || value.length() <= maxLength) {
			return value;
		} else if (maxLength <= cutPrefix.length()) {
			// No space left for the prefix
			return value.substring(value.length() - Math.max(0, maxLength));
		} else {
			return cutPrefix + value.substring(value.length() - maxLength + cutPrefix.length());
		}
	}

	/**
	 * Checks whether items contain a specific object. Items are compared by identity (==), not by
	 * equals().
	 *
	 * @param hayshack
	 *            items to search in
	 * @param needle
	 *            object to search for
	 * @return true, if the object is contained
	 */
	public static boolean containsObject(final Iterable<?> hayshack, final Object needle) {
		for (final Object item : hayshack) {
			if (item == needle) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Replaces a leading "~" and the placeholders "$HOME" and "${HOME}" in a file path by the
	 * home directory of the current user.
	 *
	 * @param filePath
	 *            the file path, may be null
	 * @return the file path with the home directory inserted, or null if the file path is null
	 */
	public static String replaceUsersHome(final String filePath) {
		if (filePath == null) {
			return null;
		}
		final String homePath = System.getProperty("user.home");
		String returnValue = filePath;
		if (returnValue.startsWith("~")) {
			// Only a leading "~" means the home directory, e.g. not in "file~1.txt"
			returnValue = homePath + returnValue.substring(1);
		}
		return returnValue.replace("${HOME}", homePath).replace("$HOME", homePath);
	}
}
