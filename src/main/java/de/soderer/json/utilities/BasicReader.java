package de.soderer.json.utilities;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BasicReader implements Closeable {
	/** UTF-8 BOM (Byte Order Mark) character for readers. */
	public static final char BOM_UTF_8_CHAR = (char) 65279;

	/** UTF-8 BOM (Byte Order Mark) first character for wrong encoding ISO-8859. */
	public static final char BOM_UTF_8_CHAR_ISO_8859 = (char) 239;

	/** Default input encoding. */
	public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	/** Input encoding. */
	private final Charset encoding;

	/** Input reader. */
	private PushbackReader inputReader;

	private CountingInputStream countingInputStream;

	private Character currentChar;
	private long readCharacters;
	private long readCharactersInCurrentLine;
	private long readLines;

	public BasicReader(final InputStream inputStream) throws Exception {
		this(inputStream, null);
	}

	public BasicReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		if (inputStream == null) {
			throw new Exception("Invalid empty inputStream");
		} else {
			this.countingInputStream = new CountingInputStream(inputStream);
			encoding = encodingCharset == null ? DEFAULT_ENCODING : encodingCharset;
			inputReader = new PushbackReader(new BufferedReader(new InputStreamReader(countingInputStream, encoding)), 2);
			readCharacters = 0;
			readLines = 0;
		}
	}

	public long getReadCharacters() {
		return readCharacters;
	}

	public long getReadLines() {
		return readLines;
	}

	public long getReadCharactersInCurrentLine() {
		return readCharactersInCurrentLine;
	}

	public void reuse(char[] charactersToReuse) throws IOException {
		if (charactersToReuse != null) {
			inputReader.unread(charactersToReuse, 0, charactersToReuse.length);
			readCharacters -= charactersToReuse.length;
			readCharactersInCurrentLine -= charactersToReuse.length;
		}
	}

	public void reuse(Character characterToReuse) throws IOException {
		if (characterToReuse != null) {
			inputReader.unread(characterToReuse);
			readCharacters -= 1;
			readCharactersInCurrentLine -= 1;
		}
	}

	public void reuseCurrentChar() throws IOException {
		reuse(currentChar);
	}

	protected Character readNextCharacter() throws IOException {
		final int currentCharInt = inputReader.read();
		if (currentCharInt != -1) {
			// Check for UTF-8 BOM at data start
			if (readCharacters == 0 && currentCharInt == BOM_UTF_8_CHAR && encoding == StandardCharsets.UTF_8) {
				return readNextCharacter();
			} else if (readCharacters == 0 && currentCharInt == BOM_UTF_8_CHAR_ISO_8859 && encoding.displayName().toUpperCase().startsWith("ISO-8859-")) {
				throw new IOException("Data encoding \"" + encoding + "\" is invalid: UTF-8 BOM detected");
			} else {
				final char nextChar = (char) currentCharInt;
				readCharacters++;
				readCharactersInCurrentLine++;
				if (nextChar == '\r' || (nextChar == '\n' && currentCharInt != '\r')) {
					readLines++;
					readCharactersInCurrentLine = 0;
				}
				currentChar = nextChar;
			}
		} else {
			currentChar = null;
		}

		//TODO: Remove
		System.out.println(currentChar);
		
		return currentChar;
	}

	protected Character readNextNonWhitespace() throws Exception {
		readNextCharacter();
		while (currentChar != null && Character.isWhitespace(currentChar) ) {
			readNextCharacter();
		}
		return currentChar;
	}

	protected String readUpToNext(final boolean includeEndChar, final Character escapeCharacter, final char... endChars) throws Exception {
		if (anyCharsAreEqual(endChars)) {
			throw new Exception("Invalid limit characters");
		} else if (contains(endChars, escapeCharacter)) {
			throw new Exception("Invalid escape characters");
		}

		final StringBuilder returnValue = new StringBuilder();
		if (currentChar != null) {
			returnValue.append(currentChar);
		}
		boolean escapeNextCharacter = false;
		while (true) {
			readNextCharacter();
			if (currentChar == null) {
				return returnValue.toString();
			} else if (escapeNextCharacter) {
				escapeNextCharacter = false;
				if (equals(escapeCharacter, currentChar)) {
					currentChar = escapeCharacter;
				} else if (equals('"', currentChar)) {
					currentChar = '"';
				} else if (equals('\'', currentChar)) {
					// Single quotes should not be escaped, but we allow them here for user convenience
					currentChar = '\'';
				} else if (equals('/', currentChar)) {
					currentChar = '/';
				} else if (equals('b', currentChar)) {
					currentChar = '\b';
				} else if (equals('f', currentChar)) {
					currentChar = '\f';
				} else if (equals('n', currentChar)) {
					currentChar = '\n';
				} else if (equals('r', currentChar)) {
					currentChar = '\r';
				} else if (equals('t', currentChar)) {
					currentChar = '\t';
				} else if (equals('u', currentChar)) {
					// Java encoded character
					final StringBuilder unicode = new StringBuilder();
					for (int i = 0; i < 4; i++) {
						final Character hexDigit = readNextCharacter();
						if (hexDigit != null) {
							unicode.append(hexDigit);
						}
						if (hexDigit == null || Character.digit(hexDigit, 16) == -1) {
							throw new Exception("Invalid unicode sequence at character index " + (readCharactersInCurrentLine - unicode.length()) + " in line " + (readLines + 1) + " ('" + unicode + "')");
						}
					}
					final int value = Integer.parseInt(unicode.toString(), 16);
					currentChar = (char) value;
				} else {
					throw new Exception("Invalid escape sequence at character index " + (readCharactersInCurrentLine - 1) + " in line " + (readLines + 1) + " ('" + currentChar + "')");
				}
				returnValue.append(currentChar);
			} else if (escapeCharacter != null && equals(escapeCharacter, currentChar)) {
				escapeNextCharacter = true;
			} else {
				for (final char endChar : endChars) {
					if (equals(endChar, currentChar)) {
						if (includeEndChar) {
							returnValue.append(currentChar);
						} else {
							reuse(currentChar);
						}
						return returnValue.toString();
					}
				}
				returnValue.append(currentChar);
			}
		}
	}

	protected String readUpToNextString(final boolean includeEndString, final Character escapeCharacter, final String... endStrings) throws Exception {
		final StringBuilder returnValue = new StringBuilder();
		if (currentChar != null) {
			returnValue.append(currentChar);
		}
		boolean escapeNextCharacter = false;
		while (true) {
			readNextCharacter();
			if (currentChar == null) {
				return returnValue.toString();
			} else if (escapeNextCharacter) {
				escapeNextCharacter = false;
				if (equals(escapeCharacter, currentChar)) {
					currentChar = escapeCharacter;
				} else if (equals('"', currentChar)) {
					currentChar = '"';
				} else if (equals('\'', currentChar)) {
					// Single quotes should not be escaped, but we allow them here for user convenience
					currentChar = '\'';
				} else if (equals('/', currentChar)) {
					currentChar = '/';
				} else if (equals('b', currentChar)) {
					currentChar = '\b';
				} else if (equals('f', currentChar)) {
					currentChar = '\f';
				} else if (equals('n', currentChar)) {
					currentChar = '\n';
				} else if (equals('r', currentChar)) {
					currentChar = '\r';
				} else if (equals('t', currentChar)) {
					currentChar = '\t';
				} else if (equals('u', currentChar)) {
					// Java encoded character
					final StringBuilder unicode = new StringBuilder();
					for (int i = 0; i < 4; i++) {
						final Character hexDigit = readNextCharacter();
						if (hexDigit != null) {
							unicode.append(hexDigit);
						}
						if (hexDigit == null || Character.digit(hexDigit, 16) == -1) {
							throw new Exception("Invalid unicode sequence at character index " + (readCharactersInCurrentLine - unicode.length()) + " in line " + (readLines + 1) + " ('" + unicode + "')");
						}
					}
					final int value = Integer.parseInt(unicode.toString(), 16);
					currentChar = (char) value;
				} else {
					throw new Exception("Invalid escape sequence at character index " + (readCharactersInCurrentLine - 1) + " in line " + (readLines + 1) + " ('" + currentChar + "')");
				}
				returnValue.append(currentChar);
			} else if (escapeCharacter != null && equals(escapeCharacter, currentChar)) {
				escapeNextCharacter = true;
			} else {
				returnValue.append(currentChar);
				for (final String endString : endStrings) {
					if (returnValue.substring(returnValue.length() - endString.length()).equals(endString)) {
						if (!includeEndString) {
							reuse(endString.toCharArray());
							return returnValue.substring(0, returnValue.length() - endString.length()).toString();
						} else {
							return returnValue.toString();
						}
					}
				}
			}
		}
	}

	protected String readQuotedText(final Character escapeCharacter) throws Exception {
		char quoteChar = currentChar;
		if (escapeCharacter == null || escapeCharacter != quoteChar) {
			final String returnValue = readUpToNext(true, escapeCharacter, quoteChar);
			if (currentChar == null || currentChar != quoteChar) {
				throw new Exception("Missing closing quote character");
			} else {
				return returnValue.substring(1, returnValue.length() - 1);
			}
		} else {
			String returnValue = readUpToNext(true, null, quoteChar);
			while (true) {
				Character nextCharacter = readNextCharacter();
				if (nextCharacter == null) {
					break;
				} else if (nextCharacter == quoteChar) {
					returnValue = returnValue + readUpToNext(true, null, quoteChar);
					if (currentChar != quoteChar) {
						throw new Exception("Missing closing quote character");
					}
				} else {
					reuse(currentChar);
					break;
				}
			}
			returnValue = returnValue.replace("" + escapeCharacter + escapeCharacter, "" + quoteChar);
			return returnValue.substring(1, returnValue.length() - 1);
		}
	}

	/**
	 * Close this writer and its underlying stream.
	 */
	@Override
	public void close() {
		closeQuietly(inputReader);
		inputReader = null;
		closeQuietly(countingInputStream);
		countingInputStream = null;
	}

	/**
	 * Check if String value is null or contains only whitespace characters.
	 *
	 * @param value
	 *            the value
	 * @return true, if is blank
	 */
	protected static boolean isBlank(final String value) {
		return value == null || value.trim().length() == 0;
	}

	/**
	 * Check if String value is not null and has a length greater than 0.
	 *
	 * @param value
	 *            the value
	 * @return true, if is not empty
	 */
	protected static boolean isNotEmpty(final String value) {
		return value != null && value.length() > 0;
	}

	/**
	 * Close a Closable item and ignore any Exception thrown by its close method.
	 *
	 * @param closeableItem
	 *            the closeable item
	 */
	private static void closeQuietly(final Closeable closeableItem) {
		if (closeableItem != null) {
			try {
				closeableItem.close();
			} catch (@SuppressWarnings("unused") final IOException e) {
				// Do nothing
			}
		}
	}

	/**
	 * Check if any characters in a list are equal
	 *
	 * @param values
	 * @return
	 */
	private static boolean anyCharsAreEqual(final char... values) {
		for (int i = 0; i < values.length; i++) {
			for (int j = i + 1; j < values.length; j++) {
				if (values[i] == values[j]) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check if character array contains specific character
	 * @param characterArray
	 * @param searchCharacter
	 * @return
	 */
	private static boolean contains(final char[] characterArray, final Character searchCharacter) {
		if (characterArray == null || searchCharacter == null) {
			return false;
		}

		for (final char character : characterArray) {
			if (character == searchCharacter) {
				return true;
			}
		}

		return false;
	}

	private static boolean equals(final char char1, final Character character2) {
		if (character2 == null) {
			return false;
		} else {
			return character2.charValue() == char1;
		}
	}

	private static boolean equals(final Character character1, final Character character2) {
		if (character1 == character2) {
			return true;
		} else {
			return character1 != null && character1.equals(character2);
		}
	}

	public long getReadDataSize() {
		if (countingInputStream != null) {
			return countingInputStream.getByteCount();
		} else {
			return 0;
		}
	}
}
