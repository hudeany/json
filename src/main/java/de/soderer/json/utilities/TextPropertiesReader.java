package de.soderer.json.utilities;

import java.util.HashMap;
import java.util.Map;

/**
 * Scanner class for properties describing a text
 */
public class TextPropertiesReader {
	protected String dataString = null;

	private int characters = -1;
	private Map<Character, Integer> foundCharacters = null;
	private int nonWhitespaceCharacters = -1;
	private int lines = -1;
	private int words = -1;
	private Linebreak linebreaks = Linebreak.Unknown;

	/**
	 * Internal constructor for derived classes
	 */
	protected TextPropertiesReader() {
	}

	/**
	 * Open a new TextPropertiesReader with the text to scan lateron
	 *
	 * @param dataString
	 */
	public TextPropertiesReader(final String dataString) {
		this.dataString = dataString;
	}

	/**
	 * Scan for the text properties of the given text
	 */
	public void readProperties() {
		foundCharacters = new HashMap<>();
		int lineCount = 0;
		int nonWhitespaceCharactersCount = 0;
		int wordCount = 0;
		boolean foundLinebreakUnix = false;
		boolean foundLinebreakMac = false;
		boolean foundLinebreakWindows = false;
		boolean isWithinWord = false;

		for (int i = 0; i < dataString.length(); i++) {
			final char character = dataString.charAt(i);

			if (foundCharacters.containsKey(character)) {
				foundCharacters.put(character, foundCharacters.get(character) + 1);
			} else {
				foundCharacters.put(character, 1);
			}

			if (character == '\r') {
				if (dataString.charAt(i + 1) == '\n') {
					i++;
					foundLinebreakWindows = true;
				} else {
					foundLinebreakMac = true;
				}

				if (isWithinWord) {
					wordCount++;
				}
				isWithinWord = false;

				lineCount++;
			} else if (character == '\n') {
				foundLinebreakUnix = true;

				if (isWithinWord) {
					wordCount++;
				}
				isWithinWord = false;

				lineCount++;
			} else if (!Character.isWhitespace(character)) {
				nonWhitespaceCharactersCount++;

				isWithinWord = true;

				if (i == dataString.length() - 1) {
					lineCount++;
				}
			} else {
				if (isWithinWord) {
					wordCount++;
				}
				isWithinWord = false;

				if (i == dataString.length() - 1) {
					lineCount++;
				}
			}
		}

		if (isWithinWord) {
			wordCount++;
		}

		if ((foundLinebreakUnix && foundLinebreakMac) || (foundLinebreakWindows && foundLinebreakMac) || (foundLinebreakUnix && foundLinebreakWindows)) {
			linebreaks = Linebreak.Mixed;
		} else if (foundLinebreakUnix) {
			linebreaks = Linebreak.Unix;
		} else if (foundLinebreakMac) {
			linebreaks = Linebreak.Mac;
		} else if (foundLinebreakWindows) {
			linebreaks = Linebreak.Windows;
		} else {
			linebreaks = Linebreak.Unknown;
		}

		lines = lineCount;
		characters = dataString.length();
		nonWhitespaceCharacters = nonWhitespaceCharactersCount;
		words = wordCount;
	}

	/**
	 * Get the number of characters of the text
	 *
	 * @return
	 */
	public int getCharactersCount() {
		return characters;
	}

	/**
	 * Get the characters used in the text and their numbers of occurences
	 *
	 * @return
	 */
	public Map<Character, Integer> getFoundCharacters() {
		return foundCharacters;
	}

	/**
	 * Get the number of non whitespace characters of the text
	 *
	 * @return
	 */
	public int getNonWhitespaceCharactersCount() {
		return nonWhitespaceCharacters;
	}

	/**
	 * Get the number of lines of the text
	 *
	 * @return
	 */
	public int getLinesCount() {
		return lines;
	}

	/**
	 * Get the number of words separated by whitespaces of the text
	 *
	 * @return
	 */
	public int getWordsCount() {
		return words;
	}

	/**
	 * Get the Linebreak type of the text
	 *
	 * @return
	 */
	public Linebreak getLinebreakType() {
		return linebreaks;
	}
}
