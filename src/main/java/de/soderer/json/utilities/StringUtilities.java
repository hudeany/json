package de.soderer.json.utilities;

/**
 * Null-safe basic operations on single strings: checks for empty and blank values, comparisons and repetition.
 */
public class StringUtilities {
	private StringUtilities() {
		// Utility class
	}

	/**
	 * Trim a string to a maximum number of characters by replacing its middle part by a cutoff sign.
	 * Surrogate pairs (like emojis) are never split, so the result may be shorter than maxLength by up to two characters.
	 *
	 * @param inputString text, may be null
	 * @param maxLength maximum length of the result; if it is less than the length of the cutoff sign, the end is cut off without cutoff sign
	 * @param cutoffSign text replacing the removed middle part, e.g. "...", null for none
	 * @return shortened text, unchanged if it is not longer than maxLength
	 */
	public static String trimStringToMaximumLength(final String inputString, final int maxLength, String cutoffSign) {
		if (inputString == null || inputString.length() <= maxLength) {
			return inputString;
		}
		if (cutoffSign == null) {
			cutoffSign = "";
		}
		if (maxLength < cutoffSign.length()) {
			// The cutoff sign does not fit, so the end is cut off without it (before, a StringIndexOutOfBoundsException was thrown)
			int endIndex = Math.max(0, maxLength);
			if (endIndex > 0 && Character.isHighSurrogate(inputString.charAt(endIndex - 1))) {
				endIndex--;
			}
			return inputString.substring(0, endIndex);
		}
		int takeFirstLength = (maxLength - cutoffSign.length()) / 2;
		final int takeLastLength = maxLength - cutoffSign.length() - takeFirstLength;
		int lastPartStartIndex = inputString.length() - takeLastLength;
		// Do not split a surrogate pair (like an emoji) at the cut positions
		if (takeFirstLength > 0 && Character.isHighSurrogate(inputString.charAt(takeFirstLength - 1))) {
			takeFirstLength--;
		}
		if (lastPartStartIndex < inputString.length() && Character.isLowSurrogate(inputString.charAt(lastPartStartIndex))) {
			lastPartStartIndex++;
		}
		return inputString.substring(0, takeFirstLength) + cutoffSign + inputString.substring(lastPartStartIndex);
	}
}
