package de.soderer.json.utilities;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Character based base reader with a read ahead buffer for text formats.
 * <p>
 * Reads characters from an {@link InputStream} using a configurable encoding and skips a leading
 * UTF-8 BOM. The next characters can be inspected without consuming them via the peek methods, up
 * to the configured number of read ahead characters. The current line and column are tracked for
 * error messages; CR, LF and CRLF each count as one line break.
 * </p>
 * <p>
 * Optionally line breaks are normalized to LF (see {@link #setNormalizeLinebreaks(boolean)}). The
 * normalization applies to the characters in the read ahead buffer, so switching it off does not
 * restore line breaks already normalized in the buffer.
 * </p>
 */
public class BasicReadAheadReader implements Closeable {
	/** UTF-8 BOM (Byte Order Mark) character for readers. */
	public static final char BOM_UTF_8_CHAR = (char) 65279;

	/** UTF-8 BOM (Byte Order Mark) first character for wrong encoding ISO-8859. */
	public static final char BOM_UTF_8_CHAR_ISO_8859 = (char) 239;

	/** Default input encoding. */
	public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	/** Default number of read ahead characters. */
	private static final int DEFAULT_READ_AHEAD_CHARACTERS = 3;

	/** Input encoding. */
	private final Charset encoding;

	/** Normalize linebreaks (\r\n, \n, \r) to unix style (\n) */
	private boolean normalizeLinebreaks = false;

	/** Input reader, null after this reader was closed. */
	private Reader inputReader;

	/** Number of characters read so far. */
	private long charactersRead = 0;

	/** Stream wrapper counting the bytes read from the input stream. */
	private CountingInputStream countingInputStream;

	/** Read ahead buffer, index 0 is the next character to be read, null marks the end of data. */
	private final Character[] nextChars;

	/** Column of the next character, 1 based. */
	private long currentColumn = 1;

	/** Line of the next character, 1 based. */
	private long currentLine = 1;

	/** Character read most recently, used to count CRLF as a single line break. */
	private Character lastReadChar = null;

	/**
	 * Creates a new reader using the default encoding and 3 read ahead characters.
	 *
	 * @param inputStream
	 *            the stream to read from, must not be null
	 * @throws Exception
	 *             if the input stream is null or reading the first characters fails
	 */
	public BasicReadAheadReader(final InputStream inputStream) throws Exception {
		this(inputStream, null, DEFAULT_READ_AHEAD_CHARACTERS);
	}

	/**
	 * Creates a new reader using the default encoding.
	 *
	 * @param inputStream
	 *            the stream to read from, must not be null
	 * @param numberOfReadAheadCharacters
	 *            the number of characters that can be inspected ahead, at least 1
	 * @throws Exception
	 *             if the input stream is null, the number of read ahead characters is less than 1
	 *             or reading the first characters fails
	 */
	public BasicReadAheadReader(final InputStream inputStream, final int numberOfReadAheadCharacters) throws Exception {
		this(inputStream, null, numberOfReadAheadCharacters);
	}

	/**
	 * Creates a new reader using 3 read ahead characters.
	 *
	 * @param inputStream
	 *            the stream to read from, must not be null
	 * @param encodingCharset
	 *            the encoding of the input data, or null to use {@link #DEFAULT_ENCODING}
	 * @throws Exception
	 *             if the input stream is null or reading the first characters fails
	 */
	public BasicReadAheadReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		this(inputStream, encodingCharset, DEFAULT_READ_AHEAD_CHARACTERS);
	}

	/**
	 * Creates a new reader.
	 *
	 * @param inputStream
	 *            the stream to read from, must not be null
	 * @param encodingCharset
	 *            the encoding of the input data, or null to use {@link #DEFAULT_ENCODING}
	 * @param numberOfReadAheadCharacters
	 *            the number of characters that can be inspected ahead, at least 1
	 * @throws Exception
	 *             if the input stream is null, the number of read ahead characters is less than 1
	 *             or reading the first characters fails
	 */
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

		// At least 2 characters are needed internally to detect CRLF and doubled quote characters
		nextChars = new Character[Math.max(2, numberOfReadAheadCharacters)];

		initializeBuffers();
	}

	/**
	 * Returns whether line breaks (CRLF, LF, CR) are normalized to unix style (LF).
	 *
	 * @return true, if line breaks are normalized
	 */
	protected boolean isNormalizeLinebreaks() {
		return normalizeLinebreaks;
	}

	/**
	 * Sets whether line breaks (CRLF, LF, CR) are normalized to unix style (LF). Switching it on also
	 * normalizes the characters already in the read ahead buffer.
	 *
	 * @param normalizeLinebreaks
	 *            true to normalize line breaks
	 * @throws IOException
	 *             if reading further characters into the read ahead buffer fails
	 */
	protected void setNormalizeLinebreaks(final boolean normalizeLinebreaks) throws IOException {
		this.normalizeLinebreaks = normalizeLinebreaks;
		normalizeLinebreaks();
	}

	/**
	 * Normalizes the line breaks in the read ahead buffer, if normalization is switched on.
	 *
	 * @throws IOException
	 *             if reading further characters into the read ahead buffer fails
	 */
	private void normalizeLinebreaks() throws IOException {
		if (normalizeLinebreaks) {
			// A CR at the last buffer position is normalized later, when its successor is known
			for (int i = 0; i < nextChars.length - 1; i++) {
				if (nextChars[i] != null && nextChars[i] == '\r') {
					nextChars[i] = '\n';
					if (nextChars[i + 1] != null && nextChars[i + 1] == '\n') {
						// Remove the LF of a CRLF
						readNextChars(i + 1);
					}
				}
			}
		}
	}

	/**
	 * Returns the column of the next character to be read.
	 *
	 * @return the column, 1 based
	 */
	protected long getCurrentColumn() {
		return currentColumn;
	}

	/**
	 * Returns the line of the next character to be read.
	 *
	 * @return the line, 1 based
	 */
	protected long getCurrentLine() {
		return currentLine;
	}

	/**
	 * Initially fills the read ahead buffer and skips a UTF-8 BOM at data start.
	 *
	 * @throws IOException
	 *             if reading fails or a UTF-8 BOM is detected while reading with an ISO-8859
	 *             encoding
	 */
	private void initializeBuffers() throws IOException {
		int currentCharInt = inputReader.read();
		if (currentCharInt != -1) {
			if (currentCharInt == BOM_UTF_8_CHAR && StandardCharsets.UTF_8.equals(encoding)) {
				currentCharInt = inputReader.read();
				if (currentCharInt != -1) {
					nextChars[0] = (char) currentCharInt;
				}
			} else if (currentCharInt == BOM_UTF_8_CHAR_ISO_8859 && encoding.name().toUpperCase().startsWith("ISO-8859-")) {
				throw new IOException("Data encoding \"" + encoding + "\" is invalid: UTF-8 BOM detected");
			} else {
				nextChars[0] = (char) currentCharInt;
			}
		}

		for (int i = 1; i < nextChars.length; i++) {
			readNextChars(1);
		}
	}

	/**
	 * Reads the next character.
	 *
	 * @return the next character, or null if the end of data has been reached
	 * @throws IOException
	 *             if reading further characters into the read ahead buffer fails
	 */
	protected Character readChar() throws IOException {
		final Character returnChar = nextChars[0];
		readNextChars(0);

		if (returnChar != null) {
			if (returnChar == '\n' && lastReadChar != null && lastReadChar == '\r') {
				// LF of a CRLF, the line break was already counted with the CR
			} else if (returnChar == '\r' || returnChar == '\n') {
				currentColumn = 1;
				currentLine++;
			} else {
				currentColumn++;
			}
			charactersRead++;
		}

		lastReadChar = returnChar;
		return returnChar;
	}

	/**
	 * Removes the buffer entry at the given index by shifting the following entries and reads a new
	 * character into the last buffer position.
	 *
	 * @param startIndex
	 *            the index of the buffer entry to remove
	 * @throws IOException
	 *             if reading fails
	 */
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

	/**
	 * Returns the next character without consuming it.
	 *
	 * @return the next character, or null at the end of data
	 */
	protected Character peekChar() {
		return nextChars[0];
	}

	/**
	 * Checks whether the next character is the given character.
	 *
	 * @param otherChar
	 *            the character to compare with
	 * @return true, if the next character equals the given character
	 */
	protected boolean peekCharMatch(final char otherChar) {
		if (nextChars[0] == null) {
			return false;
		} else {
			return nextChars[0] == otherChar;
		}
	}

	/**
	 * Checks whether the next character is not the given character.
	 *
	 * @param otherChar
	 *            the character to compare with
	 * @return true, if the next character differs from the given character or the end of data has
	 *         been reached
	 */
	protected boolean peekCharNotMatch(final char otherChar) {
		return !peekCharMatch(otherChar);
	}

	/**
	 * Checks whether the next character is one of the given characters.
	 *
	 * @param charSequence
	 *            the characters to compare with
	 * @return true, if the next character is contained in the given characters
	 */
	protected boolean peekCharMatchAny(final String charSequence) {
		if (nextChars[0] == null || charSequence == null) {
			return false;
		} else {
			return charSequence.indexOf(nextChars[0]) >= 0;
		}
	}

	/**
	 * Checks whether the next character is none of the given characters.
	 *
	 * @param charSequence
	 *            the characters to compare with
	 * @return true, if the next character is not contained in the given characters or the end of
	 *         data has been reached
	 */
	protected boolean peekCharNotMatchAny(final String charSequence) {
		return !peekCharMatchAny(charSequence);
	}

	/**
	 * Returns a character ahead without consuming it.
	 *
	 * @param indexOfNextChar
	 *            the index of the character ahead, 0 for the next character, less than the number
	 *            of read ahead characters
	 * @return the character, or null if the end of data is reached before
	 */
	protected Character peekNextChar(final int indexOfNextChar) {
		return nextChars[indexOfNextChar];
	}

	/**
	 * Checks whether a character ahead is the given character. If line breaks are normalized, LF
	 * also matches CR.
	 *
	 * @param indexOfNextChar
	 *            the index of the character ahead, 0 for the next character
	 * @param otherChar
	 *            the character to compare with
	 * @return true, if the character ahead equals the given character
	 */
	protected boolean peekNextCharMatch(final int indexOfNextChar, final char otherChar) {
		if (nextChars[indexOfNextChar] == null) {
			return false;
		} else if (otherChar == '\n') {
			return nextChars[indexOfNextChar] == '\n' || (normalizeLinebreaks && nextChars[indexOfNextChar] == '\r');
		} else {
			return nextChars[indexOfNextChar] == otherChar;
		}
	}

	/**
	 * Checks whether a character ahead is not the given character.
	 *
	 * @param indexOfNextChar
	 *            the index of the character ahead, 0 for the next character
	 * @param otherChar
	 *            the character to compare with
	 * @return true, if the character ahead differs from the given character or the end of data is
	 *         reached before
	 */
	protected boolean peekNextCharNotMatch(final int indexOfNextChar, final char otherChar) {
		return !peekNextCharMatch(indexOfNextChar, otherChar);
	}

	/**
	 * Checks whether a character ahead is one of the given characters. If line breaks are
	 * normalized, LF also matches CR.
	 *
	 * @param indexOfNextChar
	 *            the index of the character ahead, 0 for the next character
	 * @param charSequence
	 *            the characters to compare with
	 * @return true, if the character ahead is contained in the given characters
	 */
	protected boolean peekNextCharMatchAny(final int indexOfNextChar, final String charSequence) {
		if (nextChars[indexOfNextChar] == null || charSequence == null) {
			return false;
		} else if (normalizeLinebreaks) {
			return charSequence.replace("\n",  "\n\r").indexOf(nextChars[indexOfNextChar]) >= 0;
		} else {
			return charSequence.indexOf(nextChars[indexOfNextChar]) >= 0;
		}
	}

	/**
	 * Checks whether a character ahead is none of the given characters.
	 *
	 * @param indexOfNextChar
	 *            the index of the character ahead, 0 for the next character
	 * @param charSequence
	 *            the characters to compare with
	 * @return true, if the character ahead is not contained in the given characters or the end of
	 *         data is reached before
	 */
	protected boolean peekNextCharNotMatchAny(final int indexOfNextChar, final String charSequence) {
		return !peekNextCharMatchAny(indexOfNextChar, charSequence);
	}

	/**
	 * Checks whether the end of data has been reached.
	 *
	 * @return true, if there are no more characters to read
	 */
	protected boolean isEOF() {
		return nextChars[0] == null;
	}

	/**
	 * Checks whether there are more characters to read.
	 *
	 * @return true, if the end of data has not been reached yet
	 */
	protected boolean isNotEOF() {
		return nextChars[0] != null;
	}

	/**
	 * Skips all whitespace characters, including line breaks.
	 *
	 * @throws Exception
	 *             if reading fails
	 */
	protected void skipWhitespaces() throws Exception {
		while (nextChars[0] != null && Character.isWhitespace(nextChars[0])) {
			readChar();
		}
	}

	/**
	 * Skips all whitespace characters up to the next line break, which is not consumed.
	 *
	 * @throws Exception
	 *             if reading fails
	 */
	protected void skipWhitespacesInCurrentLine() throws Exception {
		while (nextChars[0] != null && nextChars[0] != '\r' && nextChars[0] != '\n' && Character.isWhitespace(nextChars[0])) {
			readChar();
		}
	}

	/**
	 * Reads the rest of the current line. The terminating line break (CR, LF or CRLF) is consumed
	 * but not included in the result.
	 *
	 * @return the line read, or null if the end of data has been reached before
	 * @throws Exception
	 *             if reading fails
	 */
	protected String readLine() throws Exception {
		if (isEOF()) {
			return null;
		} else {
			final StringBuilder nextLine = new StringBuilder();
			while (isNotEOF() && nextChars[0] != '\r' && nextChars[0] != '\n') {
				nextLine.append(readChar());
			}
			if (isNotEOF()) {
				final char lineBreak = readChar();
				if (lineBreak == '\r' && peekCharMatch('\n')) {
					readChar();
				}
			}
			return nextLine.toString();
		}
	}

	/**
	 * Reads text up to the next occurrence of one of the given limit characters.
	 * <p>
	 * Escape sequences introduced by the escape character are resolved: the escape character itself,
	 * double and single quote, slash, a (bell), b, e (escape), f, n, r, t, v (vertical tab), escaped
	 * space and tab, _ (non-breaking space), 0, N (next line), L (line separator), P (paragraph
	 * separator), and hexadecimal sequences x41, u0041 and U0001F600.
	 * </p>
	 *
	 * @param includeEndChar
	 *            true to consume the found limit character and append it to the result, false to
	 *            leave it unread
	 * @param escapeCharacter
	 *            the escape character, or null if no escaping is used
	 * @param endChars
	 *            the limit characters ending the text
	 * @return the text read, or the text up to the end of data if no limit character was found
	 * @throws Exception
	 *             if the limit characters are not distinct, the escape character is one of the
	 *             limit characters, an invalid escape sequence is found, or reading fails
	 */
	protected String readUpToNext(final boolean includeEndChar, final Character escapeCharacter, final char... endChars) throws Exception {
		if (anyCharsAreEqual(endChars)) {
			throw new Exception("Invalid set of limit characters, includes duplicates: '" + Utilities.join(endChars, "', '") + "'");
		} else if (escapeCharacter != null && contains(endChars, escapeCharacter)) {
			throw new Exception("Invalid escape character is included in limit characters: '" + escapeCharacter + "'");
		}

		final StringBuilder returnValue = new StringBuilder();
		while (isNotEOF()) {
			if (escapeCharacter != null && escapeCharacter.equals(peekChar())) {
				readChar();
				if (isNotEOF()) {
					appendUnescapedCharacter(returnValue, escapeCharacter);
				}
			} else if (contains(endChars, peekChar())) {
				if (includeEndChar) {
					returnValue.append(readChar());
				}
				return returnValue.toString();
			} else {
				returnValue.append(readChar());
			}
		}
		return returnValue.toString();
	}

	/**
	 * Reads the character(s) following an escape character and appends the character they represent.
	 *
	 * @param returnValue
	 *            the text to append to
	 * @param escapeCharacter
	 *            the escape character in use
	 * @throws Exception
	 *             if the escape sequence is invalid or reading fails
	 */
	private void appendUnescapedCharacter(final StringBuilder returnValue, final Character escapeCharacter) throws Exception {
		final char escapedChar = peekChar();
		if (escapedChar == escapeCharacter || escapedChar == '"' || escapedChar == '/') {
			returnValue.append(readChar());
		} else if (escapedChar == '\'') {
			// Single quotes should not be escaped, but they are allowed here for user convenience
			returnValue.append(readChar());
		} else if (escapedChar == 'x') {
			// Hexadecimal encoded character
			readChar();
			returnValue.append((char) readHexDigits(2));
		} else if (escapedChar == 'u') {
			// Java encoded character
			readChar();
			returnValue.append((char) readHexDigits(4));
		} else if (escapedChar == 'U') {
			// Unicode encoded character, may be outside the basic multilingual plane
			readChar();
			final long columnBefore = getCurrentColumn();
			final int codePoint = readHexDigits(8);
			if (!Character.isValidCodePoint(codePoint)) {
				throw new Exception("Invalid unicode sequence at character index " + columnBefore + " in line " + getCurrentLine() + " ('" + Integer.toHexString(codePoint) + "')");
			}
			returnValue.appendCodePoint(codePoint);
		} else {
			final Character unescapedChar = getSimpleUnescapedCharacter(escapedChar);
			if (unescapedChar == null) {
				throw new Exception("Invalid escape sequence at character index " + getCurrentColumn() + " in line " + getCurrentLine() + " ('" + escapeCharacter + escapedChar + "')");
			}
			readChar();
			returnValue.append(unescapedChar.charValue());
		}
	}

	/**
	 * Returns the character represented by a single character escape sequence like n for LF.
	 *
	 * @param escapedChar
	 *            the character following the escape character
	 * @return the represented character, or null if it is no single character escape sequence
	 */
	private static Character getSimpleUnescapedCharacter(final char escapedChar) {
		switch (escapedChar) {
			case 'a':
				// ASCII Alert/Bell
				return '\u0007';
			case 'b':
				return '\b';
			case 'e':
				// ESC
				return '\u001B';
			case 'f':
				return '\f';
			case 'n':
				return '\n';
			case 'r':
				return '\r';
			case 't':
			case '\t':
				return '\t';
			case 'v':
				// Vertical tab
				return '\u000B';
			case ' ':
				return ' ';
			case '_':
				// NBSP/non-breaking space
				return '\u00A0';
			case '0':
				return '\0';
			case 'N':
				// Next Line
				return '\u0085';
			case 'L':
				// Unicode LINE SEPARATOR (LS or LSEP)
				return '\u2028';
			case 'P':
				// Unicode PARAGRAPH SEPARATOR (PS or PSEP)
				return '\u2029';
			default:
				return null;
		}
	}

	/**
	 * Reads a fixed number of ASCII hexadecimal digits.
	 *
	 * @param digitCount
	 *            the number of digits to read
	 * @return the value of the digits
	 * @throws Exception
	 *             if the data ends before or a character is no ASCII hexadecimal digit
	 */
	private int readHexDigits(final int digitCount) throws Exception {
		final StringBuilder hexCode = new StringBuilder();
		int value = 0;
		for (int i = 0; i < digitCount && isNotEOF(); i++) {
			final char hexDigit = readChar();
			hexCode.append(hexDigit);
			final int digitValue = hexDigitValue(hexDigit);
			if (digitValue == -1) {
				throw new Exception("Invalid unicode sequence at character index " + (getCurrentColumn() - hexCode.length()) + " in line " + getCurrentLine() + " ('" + hexCode + "')");
			}
			value = (value << 4) | digitValue;
		}
		if (hexCode.length() != digitCount) {
			throw new Exception("Invalid unicode sequence at character index " + (getCurrentColumn() - hexCode.length()) + " in line " + getCurrentLine() + " ('" + hexCode + "')");
		}
		return value;
	}

	/**
	 * Returns the value of an ASCII hexadecimal digit (0-9, a-f, A-F). Unlike
	 * {@link Character#digit(char, int)} this does not accept non ASCII digits.
	 *
	 * @param character
	 *            the character to convert
	 * @return the value 0 to 15, or -1 if the character is no ASCII hexadecimal digit
	 */
	private static int hexDigitValue(final char character) {
		if (character >= '0' && character <= '9') {
			return character - '0';
		} else if (character >= 'a' && character <= 'f') {
			return character - 'a' + 10;
		} else if (character >= 'A' && character <= 'F') {
			return character - 'A' + 10;
		} else {
			return -1;
		}
	}

	/**
	 * Reads a quoted text. The next character is the opening quote character, which is also used
	 * as closing quote character. Line breaks are not normalized within the quoted text.
	 *
	 * @param escapeCharacter
	 *            the escape character, or null if no escaping is used. If it equals the quote
	 *            character, a doubled quote character represents a single one and no other escape
	 *            sequences exist.
	 * @return the unquoted text with resolved escape sequences
	 * @throws Exception
	 *             if the data ends before the closing quote, an invalid escape sequence is found,
	 *             or reading fails
	 */
	protected String readQuotedText(final Character escapeCharacter) throws Exception {
		final long columnStart = currentColumn;
		final long lineStart = currentLine;
		if (isEOF()) {
			throw new Exception("Missing quote character for quoted string at line " + lineStart + " column " + columnStart);
		}

		final boolean previousSettingNormalizeLinebreaks = normalizeLinebreaks;
		try {
			normalizeLinebreaks = false;
			final char quoteChar = readChar();
			if (escapeCharacter != null && escapeCharacter == quoteChar) {
				final StringBuilder returnValue = new StringBuilder();
				while (isNotEOF()) {
					if (peekCharNotMatch(quoteChar)) {
						returnValue.append(readChar());
					} else if (peekNextCharMatch(1, quoteChar)) {
						// Doubled quote character represents a single one
						returnValue.append(readChar());
						readChar();
					} else {
						// Closing quote character
						readChar();
						return returnValue.toString();
					}
				}
				throw new Exception("Missing closing quote character for quoted string beginning at line " + lineStart + " column " + columnStart);
			} else {
				final String returnValue = readUpToNext(false, escapeCharacter, quoteChar);
				if (isEOF()) {
					throw new Exception("Missing closing quote character for quoted string beginning at line " + lineStart + " column " + columnStart);
				}
				// Closing quote character
				readChar();
				return returnValue;
			}
		} finally {
			setNormalizeLinebreaks(previousSettingNormalizeLinebreaks);
		}
	}

	/**
	 * Closes this reader and its underlying stream.
	 */
	@Override
	public void close() {
		closeQuietly(inputReader);
		inputReader = null;
		closeQuietly(countingInputStream);
		countingInputStream = null;

		Arrays.fill(nextChars, null);
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
	 * Checks if any characters in a list are equal.
	 *
	 * @param values
	 *            the characters to check
	 * @return true, if at least two characters are equal
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
	 * Checks if a character array contains a specific character.
	 *
	 * @param characterArray
	 *            the characters to search in, may be null
	 * @param searchCharacter
	 *            the character to search for, may be null
	 * @return true, if the character is contained in the array
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

	/**
	 * Returns the number of bytes read from the underlying input stream so far. Because of internal
	 * buffering this may be ahead of the characters actually processed.
	 *
	 * @return the number of bytes read, or 0 if this reader was closed
	 */
	public long getReadDataSize() {
		if (countingInputStream != null) {
			return countingInputStream.getByteCount();
		} else {
			return 0;
		}
	}

	/**
	 * Returns the number of characters read so far.
	 *
	 * @return the number of characters read
	 */
	public long getCharactersRead() {
		return charactersRead;
	}

	/**
	 * Skips all whitespace characters and reads the next character.
	 *
	 * @return the next non-whitespace character, or null if the end of data has been reached
	 * @throws Exception
	 *             if reading fails
	 */
	protected Character readNextNonWhitespace() throws Exception {
		skipWhitespaces();
		return readChar();
	}
}
