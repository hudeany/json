package de.soderer.json.utilities;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BasicReadAheadReader implements Closeable {
	/** UTF-8 BOM (Byte Order Mark) character for readers. */
	public static final char BOM_UTF_8_CHAR = (char) 65279;

	/** UTF-8 BOM (Byte Order Mark) first character for wrong encoding ISO-8859. */
	public static final char BOM_UTF_8_CHAR_ISO_8859 = (char) 239;

	/** Default input encoding. */
	public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	/** Input encoding. */
	private final Charset encoding;

	/** Normalize linebreaks (\r\n, \n, \r) to unix style (\n) */
	public boolean normalizeLinebreaks = false;

	/** Input reader. */
	private Reader inputReader;

	private long charactersRead = 0;
	private CountingInputStream countingInputStream;

	private final Character[] nextChars;
	private long currentColumn = 1;
	private long currentLine = 1;

	public BasicReadAheadReader(final InputStream inputStream) throws Exception {
		this(inputStream, null, 3);
	}

	public BasicReadAheadReader(final InputStream inputStream, final int numberOfReadAheadCharacters) throws Exception {
		this(inputStream, null, numberOfReadAheadCharacters);
	}

	public BasicReadAheadReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		this(inputStream, encodingCharset, 3);
	}

	public BasicReadAheadReader(final InputStream inputStream, final Charset encodingCharset, final int numberOfReadAheadCharacters) throws Exception {
		if (numberOfReadAheadCharacters < 1) {
			throw new Exception("Invalid size of read ahead buffer. Minimum 1 was: " + numberOfReadAheadCharacters);
		}

		if (inputStream == null) {
			throw new Exception("Invalid empty inputStream");
		} else {
			countingInputStream = new CountingInputStream(inputStream);
			encoding = encodingCharset == null ? DEFAULT_ENCODING : encodingCharset;
			inputReader = new BufferedReader(new InputStreamReader(countingInputStream, encoding));
		}

		nextChars = new Character[numberOfReadAheadCharacters];

		initializeBuffers();
	}

	protected boolean isNormalizeLinebreaks() {
		return normalizeLinebreaks;
	}

	protected void setNormalizeLinebreaks(final boolean normalizeLinebreaks) throws IOException {
		this.normalizeLinebreaks = normalizeLinebreaks;
		normalizeLinebreaks();
	}

	private void normalizeLinebreaks() throws IOException {
		if (normalizeLinebreaks) {
			for (int i = 0; i < nextChars.length - 2; i++) {
				if (nextChars[i] != null && nextChars[i] == '\r') {
					if (nextChars[i + 1] != null && nextChars[i + 1] == '\n') {
						nextChars[i] = '\n';
						readNextChars(i + 1);
					} else {
						nextChars[i] = '\n';
					}
				}
			}
		}
	}

	protected long getCurrentColumn() {
		return currentColumn;
	}

	protected long getCurrentLine() {
		return currentLine;
	}

	/**
	 * Initially fill the read ahead buffers
	 * Check for UTF-8 BOM at data start
	 * @throws IOException
	 */
	private void initializeBuffers() throws IOException {
		int currentCharInt = inputReader.read();
		if (currentCharInt != -1) {
			if (currentCharInt == BOM_UTF_8_CHAR && encoding == StandardCharsets.UTF_8) {
				currentCharInt = inputReader.read();
				if (currentCharInt != -1) {
					nextChars[0] = (char) currentCharInt;
				}
			} else if (currentCharInt == BOM_UTF_8_CHAR_ISO_8859 && encoding.displayName().toUpperCase().startsWith("ISO-8859-")) {
				throw new IOException("Data encoding \"" + encoding + "\" is invalid: UTF-8 BOM detected");
			} else {
				nextChars[0] = (char) currentCharInt;
			}
		}

		for (int i = 1; i < nextChars.length; i++) {
			readNextChars(1);
		}
	}

	protected Character readChar() throws IOException {
		final Character returnChar = nextChars[0];
		readNextChars(0);

		if (returnChar != null) {
			if (returnChar == '\r' || returnChar == '\n') {
				currentColumn = 1;
				currentLine++;
			} else {
				currentColumn++;
			}
		}

		charactersRead++;
		return returnChar;
	}

	private void readNextChars(final int startIndex) throws IOException {
		for (int i = startIndex; i < nextChars.length - 1; i++) {
			nextChars[i] = nextChars[i + 1];
		}

		final int newNextCharInt = inputReader.read();
		if (newNextCharInt == -1) {
			nextChars[nextChars.length - 1] = null;
		} else {
			nextChars[nextChars.length - 1] = (char) newNextCharInt;
		}

		normalizeLinebreaks();
	}

	protected Character peekChar() {
		return nextChars[0];
	}

	protected boolean peekCharMatch(final char otherChar) {
		if (nextChars[0] == null) {
			return false;
		} else {
			return nextChars[0] == otherChar;
		}
	}

	protected boolean peekCharNotMatch(final char otherChar) {
		return !peekCharMatch(otherChar);
	}

	protected boolean peekCharMatchAny(final String charSequence) {
		if (nextChars[0] == null || charSequence == null) {
			return false;
		} else {
			return charSequence.contains(nextChars[0].toString());
		}
	}

	protected boolean peekCharNotMatchAny(final String charSequence) {
		return !peekCharMatchAny(charSequence);
	}

	protected Character peekNextChar(final int indexOfNextChar) {
		return nextChars[indexOfNextChar];
	}

	protected boolean peekNextCharMatch(final int indexOfNextChar, final char otherChar) {
		if (nextChars[indexOfNextChar] == null) {
			return false;
		} else if (otherChar == '\n') {
			return nextChars[indexOfNextChar] == '\n' || (normalizeLinebreaks && nextChars[indexOfNextChar] == '\r');
		} else {
			return nextChars[indexOfNextChar] == otherChar;
		}
	}

	protected boolean peekNextCharNotMatch(final int indexOfNextChar, final char otherChar) {
		return !peekNextCharMatch(indexOfNextChar, otherChar);
	}

	protected boolean peekNextCharMatchAny(final int indexOfNextChar, final String charSequence) {
		if (nextChars[indexOfNextChar] == null || charSequence == null) {
			return false;
		} else if (normalizeLinebreaks) {
			return charSequence.replace("\n",  "\n\r").contains(nextChars[indexOfNextChar].toString());
		} else {
			return charSequence.contains(nextChars[indexOfNextChar].toString());
		}
	}

	protected boolean peekNextCharNotMatchAny(final int indexOfNextChar, final String charSequence) {
		return !peekNextCharMatchAny(indexOfNextChar, charSequence);
	}

	protected boolean isEOF() {
		return nextChars[0] == null;
	}

	protected boolean isNotEOF() {
		return nextChars[0] != null;
	}

	protected void skipWhitespaces() throws Exception {
		while (nextChars[0] != null && Character.isWhitespace(nextChars[0])) {
			readChar();
		}
	}

	protected void skipWhitespacesInCurrentLine() throws Exception {
		while (nextChars[0] != null && nextChars[0] != '\r' && nextChars[0] != '\n' && Character.isWhitespace(nextChars[0])) {
			readChar();
		}
	}

	protected String readLine() throws Exception {
		if (isEOF()) {
			return null;
		} else {
			String nextLine = "";
			while (isNotEOF() && nextChars[0] != '\r' && nextChars[0] != '\n') {
				nextLine += readChar();
			}
			if (nextChars[0] == '\r' || nextChars[0] == '\n') {
				readChar();
			}
			return nextLine;
		}
	}

	protected String readUpToNext(final boolean includeEndChar, final Character escapeCharacter, final char... endChars) throws Exception {
		if (anyCharsAreEqual(endChars)) {
			throw new Exception("Invalid set of limit characters, includes duplicates: '" + Utilities.join(endChars, "', '") + "'");
		} else if (escapeCharacter != null && contains(endChars, escapeCharacter)) {
			throw new Exception("Invalid escape character is included in limit characters: '" + escapeCharacter + "'");
		}

		String returnValue = "";
		boolean unescapeNextCharacter = false;
		while (isNotEOF()) {
			if (unescapeNextCharacter) {
				unescapeNextCharacter = false;
				char unescapedChar;
				if (escapeCharacter == peekChar()) {
					unescapedChar = readChar();
				} else if ('"' == peekChar()) {
					unescapedChar = readChar();
				} else if ('\'' == peekChar()) {
					// Single quotes should not be escaped, but they are allowed here for user convenience
					unescapedChar = readChar();
				} else if ('/' == peekChar()) {
					unescapedChar = readChar();
				} else if ('a' == peekChar()) {
					readChar();
					unescapedChar = '\u0007'; // ASCII Alert/Bell
				} else if ('b' == peekChar()) {
					readChar();
					unescapedChar = '\b';
				} else if ('e' == peekChar()) {
					readChar();
					unescapedChar = '\u001B'; // ESC
				} else if ('f' == peekChar()) {
					readChar();
					unescapedChar = '\f';
				} else if ('n' == peekChar()) {
					readChar();
					unescapedChar = '\n';
				} else if ('r' == peekChar()) {
					readChar();
					unescapedChar = '\r';
				} else if ('t' == peekChar()) {
					readChar();
					unescapedChar = '\t';
				} else if ('v' == peekChar()) {
					readChar();
					unescapedChar = '\u2B7F'; // Vertical Tab
				} else if (' ' == peekChar()) {
					readChar();
					unescapedChar = ' ';
				} else if ('	' == peekChar()) {
					readChar();
					unescapedChar = '\t';
				} else if ('_' == peekChar()) {
					readChar();
					unescapedChar = '\u00A0'; // NBSP/non-breaking space
				} else if ('0' == peekChar()) {
					readChar();
					unescapedChar = '\0';
				} else if ('N' == peekChar()) {
					readChar();
					unescapedChar = '\u0085'; // Next Line
				} else if ('L' == peekChar()) {
					readChar();
					unescapedChar = '\u2028'; // Unicode LINE SEPARATOR (LS or LSEP)
				} else if ('P' == peekChar()) {
					readChar();
					unescapedChar = '\u2029'; // Unicode PARAGRAPH SEPARATOR (PS or PSEP)
				} else if ('x' == peekChar()) {
					readChar();
					// hexadecimal encoded character
					String hexcode = "";
					for (int i = 0; i < 2 && isNotEOF(); i++) {
						final Character hexDigit = readChar();
						hexcode += hexDigit;
						if (hexDigit == null || Character.digit(hexDigit, 16) == -1) {
							throw new Exception("Invalid unicode sequence at character index " + (getCurrentColumn() - hexcode.length()) + " in line " + getCurrentLine() + " ('" + hexcode + "')");
						}
					}
					if (hexcode.length() != 2) {
						throw new Exception("Invalid unicode sequence at character index " + (getCurrentColumn() - hexcode.length()) + " in line " + getCurrentLine() + " ('" + hexcode + "')");
					} else {
						unescapedChar = (char) Integer.parseInt(hexcode.toString(), 16);
					}
				} else if ('u' == peekChar()) {
					readChar();
					// Java encoded character
					String unicode = "";
					for (int i = 0; i < 4 && isNotEOF(); i++) {
						final Character hexDigit = readChar();
						unicode += hexDigit;
						if (hexDigit == null || Character.digit(hexDigit, 16) == -1) {
							throw new Exception("Invalid unicode sequence at character index " + (getCurrentColumn() - unicode.length()) + " in line " + getCurrentLine() + " ('" + unicode + "')");
						}
					}
					if (unicode.length() != 4) {
						throw new Exception("Invalid unicode sequence at character index " + (getCurrentColumn() - unicode.length()) + " in line " + getCurrentLine() + " ('" + unicode + "')");
					} else {
						unescapedChar = (char) Integer.parseInt(unicode.toString(), 16);
					}
				} else if ('U' == peekChar()) {
					readChar();
					// Unicode encoded character
					String unicode = "";
					for (int i = 0; i < 8 && isNotEOF(); i++) {
						final Character hexDigit = readChar();
						unicode += hexDigit;
						if (hexDigit == null || Character.digit(hexDigit, 16) == -1) {
							throw new Exception("Invalid unicode sequence at character index " + (getCurrentColumn() - unicode.length()) + " in line " + getCurrentLine() + " ('" + unicode + "')");
						}
					}
					if (unicode.length() != 8) {
						throw new Exception("Invalid unicode sequence at character index " + (getCurrentColumn() - unicode.length()) + " in line " + getCurrentLine() + " ('" + unicode + "')");
					} else {
						unescapedChar = (char) Integer.parseInt(unicode.toString(), 16);
					}
				} else {
					throw new Exception("Invalid escape sequence at character index " + getCurrentColumn() + " in line " + getCurrentLine() + " ('" + escapeCharacter + nextChars[0] + "')");
				}
				returnValue += unescapedChar;
			} else if (escapeCharacter != null && escapeCharacter == peekChar()) {
				readChar();
				unescapeNextCharacter = true;
			} else if (contains(endChars, peekChar())){
				if (includeEndChar) {
					returnValue += readChar();
				}
				return returnValue;
			} else {
				returnValue += readChar();
			}
		}
		return returnValue;
	}

	protected String readQuotedText(final Character escapeCharacter) throws Exception {
		final long columnStart = currentColumn;
		final long lineStart = currentLine;
		final boolean previousSettingNormalizeLinebreaks = normalizeLinebreaks;
		try {
			normalizeLinebreaks = false;
			final char quoteChar = readChar();
			if (escapeCharacter == quoteChar) {
				String returnValue = "";
				while (isNotEOF()) {
					if (peekCharNotMatch(quoteChar)) {
						returnValue += readChar();
					} else if (peekCharMatch(quoteChar) && peekNextCharMatch(1, quoteChar)) {
						returnValue += readChar();
						readChar();
					} else {
						returnValue += readChar();
						break;
					}
				}
				if (returnValue.charAt(returnValue.length() - 1) != quoteChar) {
					throw new Exception("Missing closing quote character for quoted string beginning at line " + lineStart + " column " + columnStart);
				} else {
					returnValue = returnValue.substring(0, returnValue.length() - 1);
					returnValue = returnValue.replace("" + quoteChar + quoteChar, "" + quoteChar);
					return returnValue;
				}
			} else {
				final String returnValue = readUpToNext(true, escapeCharacter, quoteChar);
				if (returnValue.charAt(returnValue.length() - 1) != quoteChar) {
					throw new Exception("Missing closing quote character for quoted string beginning at line " + lineStart + " column " + columnStart);
				} else {
					return returnValue.substring(0, returnValue.length() - 1);
				}
			}
		} finally {
			setNormalizeLinebreaks(previousSettingNormalizeLinebreaks);
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

		nextChars[0] = null;
		nextChars[1] = null;
		currentColumn = 1;
		currentLine = 1;
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
		} else {
			for (final char character : characterArray) {
				if (character == searchCharacter) {
					return true;
				}
			}
			return false;
		}
	}

	public long getReadDataSize() {
		if (countingInputStream != null) {
			return countingInputStream.getByteCount();
		} else {
			return 0;
		}
	}

	public long getCharactersRead() {
		return charactersRead;
	}
}
