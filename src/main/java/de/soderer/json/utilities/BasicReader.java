package de.soderer.json.utilities;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Character based base reader for text formats like CSV, JSON or YAML.
 * <p>
 * Reads characters from an {@link InputStream} using a configurable encoding and skips a leading
 * UTF-8 BOM. It keeps track of the number of characters, lines and bytes read, of the character
 * position within the current line and of the indentation level of the current line.
 * </p>
 * <p>
 * Subclasses can use the helper methods to read quoted text or text up to given limit characters
 * or strings including escape sequence handling, and to look ahead or unread up to
 * {@link #MAX_UNREAD_CHARACTERS} of the most recently read characters. Unreading restores all
 * position counters, so line and character positions stay correct.
 * </p>
 * <p>
 * Line breaks may be CR, LF or CRLF, each of them counts as a single line break. Optionally all
 * line breaks can be normalized to LF (see {@link #setNormalizeLinebreaks(boolean)}).
 * </p>
 * <p>
 * This class exists as identical copies in several de.soderer libraries, differing only in their
 * package declaration, to keep each library free of external dependencies. Keep all copies in
 * sync when changing this class.
 * </p>
 */
public class BasicReader implements Closeable {
	/** Value returned by raw read operations at the end of data. */
	public static final int EOF = -1;

	/** UTF-8 BOM (Byte Order Mark) character for readers. */
	public static final char BOM_UTF_8_CHAR = (char) 65279;

	/** UTF-8 BOM (Byte Order Mark) first character for wrong encoding ISO-8859. */
	public static final char BOM_UTF_8_CHAR_ISO_8859 = (char) 239;

	/** Default input encoding. */
	public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	/** Maximum number of most recently read characters which can be unread. */
	public static final int MAX_UNREAD_CHARACTERS = 64;

	/** Marker for "no character" in int based character fields. */
	private static final int NO_CHARACTER = EOF;

	/** Input encoding. */
	private final Charset encoding;

	/** Stream wrapper counting the bytes read from the input stream. */
	private CountingInputStream countingInputStream;

	/** Input reader, null after this reader was closed. */
	private BufferedReader inputReader;

	/** Normalize line breaks (CRLF, LF, CR) to unix style (LF). */
	private boolean normalizeLinebreaks = false;

	/** True after the first read operation was started. */
	private boolean readInitialized = false;

	/** True after the check for a leading BOM was done. */
	private boolean bomChecked = false;

	/** Raw character already read from the input reader but not yet processed, or NO_CHARACTER. */
	private int rawLookaheadCharacter = NO_CHARACTER;

	/** Character read most recently (possibly replaced by escape sequence handling), or null at end of data. */
	private Character currentChar = null;

	/** Character consumed most recently, never changed by escape sequence handling, or NO_CHARACTER. Used to detect CRLF. */
	private int lastConsumedCharacter = NO_CHARACTER;

	/** Number of characters read from the input so far. */
	private long readCharacters = 0;

	/** Number of characters read in the current line so far. */
	private long readCharactersInCurrentLine = 0;

	/** Number of line breaks read so far. */
	private long readLines = 0;

	/** Number of leading spaces and tabs in the current line. */
	private int currentIndentationLevel = 0;

	/** True as long as only spaces and tabs were read in the current line. */
	private boolean countForIndentationLevel = true;

	/** History of the most recently consumed characters (ring buffer). */
	private final char[] historyCharacter = new char[MAX_UNREAD_CHARACTERS];

	/** History: number of raw input characters each consumed character represents. */
	private final int[] historyRawCount = new int[MAX_UNREAD_CHARACTERS];

	/** History: read characters counter before consumption. */
	private final long[] historyReadCharacters = new long[MAX_UNREAD_CHARACTERS];

	/** History: characters in current line counter before consumption. */
	private final long[] historyReadCharactersInCurrentLine = new long[MAX_UNREAD_CHARACTERS];

	/** History: read lines counter before consumption. */
	private final long[] historyReadLines = new long[MAX_UNREAD_CHARACTERS];

	/** History: indentation level before consumption. */
	private final int[] historyIndentationLevel = new int[MAX_UNREAD_CHARACTERS];

	/** History: indentation counting flag before consumption. */
	private final boolean[] historyCountForIndentationLevel = new boolean[MAX_UNREAD_CHARACTERS];

	/** History: last consumed character before consumption. */
	private final int[] historyLastConsumedCharacter = new int[MAX_UNREAD_CHARACTERS];

	/** Ring buffer index of the oldest history entry. */
	private int historyStart = 0;

	/** Number of history entries, which is the number of characters that can be unread. */
	private int historySize = 0;

	/** Stack of unread characters to be delivered again, the top of the stack is delivered first. */
	private final char[] pushbackCharacter = new char[MAX_UNREAD_CHARACTERS];

	/** Stack of the raw character counts of the unread characters. */
	private final int[] pushbackRawCount = new int[MAX_UNREAD_CHARACTERS];

	/** Number of unread characters waiting to be delivered again. */
	private int pushbackSize = 0;

	/**
	 * Creates a new reader using the default encoding ({@link #DEFAULT_ENCODING}).
	 *
	 * @param inputStream
	 *            the stream to read from, must not be null
	 * @throws Exception
	 *             if the input stream is null
	 */
	public BasicReader(final InputStream inputStream) throws Exception {
		this(inputStream, null);
	}

	/**
	 * Creates a new reader using the given encoding.
	 *
	 * @param inputStream
	 *            the stream to read from, must not be null
	 * @param encodingCharset
	 *            the encoding of the input data, or null to use {@link #DEFAULT_ENCODING}
	 * @throws Exception
	 *             if the input stream is null
	 */
	public BasicReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		if (inputStream == null) {
			throw new Exception("Invalid empty inputStream");
		}
		encoding = encodingCharset == null ? DEFAULT_ENCODING : encodingCharset;
		countingInputStream = new CountingInputStream(inputStream);
		inputReader = new BufferedReader(new InputStreamReader(countingInputStream, encoding));
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
	 * Sets whether line breaks (CRLF, LF, CR) are normalized to unix style (LF).
	 * <p>
	 * Characters already unread or peeked keep the line break form they were read with.
	 * </p>
	 *
	 * @param normalizeLinebreaks
	 *            true to normalize line breaks
	 */
	protected void setNormalizeLinebreaks(final boolean normalizeLinebreaks) {
		this.normalizeLinebreaks = normalizeLinebreaks;
	}

	/**
	 * Returns the character read most recently. Escape sequence handling may have replaced it by
	 * the character it represents.
	 *
	 * @return the current character, or null if nothing was read yet or the end of data was reached
	 */
	public Character getCurrentChar() {
		return currentChar;
	}

	/**
	 * Returns the number of characters read so far. A normalized CRLF line break counts as two
	 * characters.
	 *
	 * @return the number of characters read
	 */
	public long getReadCharacters() {
		return readCharacters;
	}

	/**
	 * Returns whether any read operation was started yet.
	 *
	 * @return true, if reading was initialized
	 */
	public boolean readWasInitialized() {
		return readInitialized;
	}

	/**
	 * Returns the number of line breaks read so far. CR, LF and CRLF each count as one line break.
	 *
	 * @return the number of lines read
	 */
	public long getReadLines() {
		return readLines;
	}

	/**
	 * Returns the number of characters read in the current line so far, excluding line breaks.
	 *
	 * @return the number of characters read in the current line
	 */
	public long getReadCharactersInCurrentLine() {
		return readCharactersInCurrentLine;
	}

	/**
	 * Returns the number of leading spaces and tabs read in the current line.
	 *
	 * @return the indentation level of the current line
	 */
	public int getCurrentIndentationLevel() {
		return currentIndentationLevel;
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
	 * Unreads the current character, so it is delivered again by the next read operation. Does
	 * nothing at the end of data.
	 * <p>
	 * The position counters are restored, the value of {@link #getCurrentChar()} stays unchanged.
	 * </p>
	 */
	public void reuseCurrentChar() {
		if (currentChar != null && historySize > 0) {
			unread(1);
		}
	}

	/**
	 * Unreads the most recently read character, which must equal the given character.
	 *
	 * @param characterToReuse
	 *            the most recently read character, or null to do nothing
	 * @throws IllegalArgumentException
	 *             if the given character is not the most recently read character
	 * @throws IllegalStateException
	 *             if there is no character left to be unread
	 */
	protected void reuse(final Character characterToReuse) {
		if (characterToReuse != null) {
			reuse(new char[] { characterToReuse });
		}
	}

	/**
	 * Unreads the most recently read characters, which must equal the given characters in reading
	 * order.
	 *
	 * @param charactersToReuse
	 *            the most recently read characters, or null to do nothing
	 * @throws IllegalArgumentException
	 *             if the given characters are not the most recently read characters
	 * @throws IllegalStateException
	 *             if there are not enough characters left to be unread
	 */
	protected void reuse(final char[] charactersToReuse) {
		if (charactersToReuse != null && charactersToReuse.length > 0) {
			if (charactersToReuse.length > historySize) {
				throw new IllegalStateException("Cannot unread " + charactersToReuse.length + " characters, only " + historySize + " available");
			}
			for (int i = 0; i < charactersToReuse.length; i++) {
				final int index = (historyStart + historySize - charactersToReuse.length + i) % MAX_UNREAD_CHARACTERS;
				if (historyCharacter[index] != charactersToReuse[i]) {
					throw new IllegalArgumentException("Characters to reuse do not match the most recently read characters");
				}
			}
			unread(charactersToReuse.length);
		}
	}

	/**
	 * Unreads the given number of most recently read characters, so they are delivered again by
	 * the next read operations. All position counters are restored, the value of
	 * {@link #getCurrentChar()} stays unchanged.
	 *
	 * @param count
	 *            the number of characters to unread, at most {@link #MAX_UNREAD_CHARACTERS}
	 * @throws IllegalStateException
	 *             if the count is negative or more characters are requested than available
	 */
	protected void unread(final int count) {
		if (count < 0 || count > historySize) {
			throw new IllegalStateException("Cannot unread " + count + " characters, only " + historySize + " available");
		}
		for (int i = 0; i < count; i++) {
			historySize--;
			final int index = (historyStart + historySize) % MAX_UNREAD_CHARACTERS;
			readCharacters = historyReadCharacters[index];
			readCharactersInCurrentLine = historyReadCharactersInCurrentLine[index];
			readLines = historyReadLines[index];
			currentIndentationLevel = historyIndentationLevel[index];
			countForIndentationLevel = historyCountForIndentationLevel[index];
			lastConsumedCharacter = historyLastConsumedCharacter[index];
			pushbackCharacter[pushbackSize] = historyCharacter[index];
			pushbackRawCount[pushbackSize] = historyRawCount[index];
			pushbackSize++;
		}
	}

	/**
	 * Reads the next character from the input.
	 * <p>
	 * A UTF-8 BOM at the start of the data is skipped. Unread characters are delivered first.
	 * </p>
	 *
	 * @return the next character, or null if the end of data has been reached
	 * @throws IOException
	 *             if reading fails or a UTF-8 BOM is detected while reading with an ISO-8859
	 *             encoding
	 * @throws IllegalStateException
	 *             if this reader is already closed
	 */
	protected Character readNextCharacter() throws IOException {
		if (inputReader == null) {
			throw new IllegalStateException("Reader is already closed");
		}
		readInitialized = true;

		if (pushbackSize > 0) {
			pushbackSize--;
			return consume(pushbackCharacter[pushbackSize], pushbackRawCount[pushbackSize]);
		}

		int nextCharInt = readRawCharacter();
		if (!bomChecked && nextCharInt != EOF) {
			bomChecked = true;
			if (nextCharInt == BOM_UTF_8_CHAR && StandardCharsets.UTF_8.equals(encoding)) {
				nextCharInt = readRawCharacter();
			} else if (nextCharInt == BOM_UTF_8_CHAR_ISO_8859 && encoding.name().toUpperCase().startsWith("ISO-8859-")) {
				throw new IOException("Data encoding \"" + encoding + "\" is invalid: UTF-8 BOM detected");
			}
		}

		if (nextCharInt == EOF) {
			currentChar = null;
			return null;
		}

		char nextChar = (char) nextCharInt;
		int rawCount = 1;
		if (nextChar == '\r' && normalizeLinebreaks) {
			final int followingCharInt = readRawCharacter();
			if (followingCharInt == '\n') {
				rawCount = 2;
			} else if (followingCharInt != EOF) {
				rawLookaheadCharacter = followingCharInt;
			}
			nextChar = '\n';
		}
		return consume(nextChar, rawCount);
	}

	/**
	 * Reads the next raw character from the input reader, preferring a pending lookahead character.
	 *
	 * @return the raw character, or {@link #EOF} at the end of data
	 * @throws IOException
	 *             if reading fails
	 */
	private int readRawCharacter() throws IOException {
		if (rawLookaheadCharacter != NO_CHARACTER) {
			final int character = rawLookaheadCharacter;
			rawLookaheadCharacter = NO_CHARACTER;
			return character;
		} else {
			return inputReader.read();
		}
	}

	/**
	 * Makes the given character the current character, updates all position counters and records
	 * the previous state for unreading.
	 *
	 * @param nextChar
	 *            the character to consume
	 * @param rawCount
	 *            the number of raw input characters it represents (2 for a normalized CRLF)
	 * @return the consumed character
	 */
	private Character consume(final char nextChar, final int rawCount) {
		final int index;
		if (historySize == MAX_UNREAD_CHARACTERS) {
			index = historyStart;
			historyStart = (historyStart + 1) % MAX_UNREAD_CHARACTERS;
		} else {
			index = (historyStart + historySize) % MAX_UNREAD_CHARACTERS;
			historySize++;
		}
		historyCharacter[index] = nextChar;
		historyRawCount[index] = rawCount;
		historyReadCharacters[index] = readCharacters;
		historyReadCharactersInCurrentLine[index] = readCharactersInCurrentLine;
		historyReadLines[index] = readLines;
		historyIndentationLevel[index] = currentIndentationLevel;
		historyCountForIndentationLevel[index] = countForIndentationLevel;
		historyLastConsumedCharacter[index] = lastConsumedCharacter;

		readCharacters += rawCount;
		if (nextChar == '\r' || (nextChar == '\n' && lastConsumedCharacter != '\r')) {
			readLines++;
			readCharactersInCurrentLine = 0;
			currentIndentationLevel = 0;
			countForIndentationLevel = true;
		} else if (nextChar != '\n') {
			// A LF directly after CR is part of an already counted CRLF line break
			readCharactersInCurrentLine++;
			if (countForIndentationLevel) {
				if (nextChar == ' ' || nextChar == '\t') {
					currentIndentationLevel++;
				} else {
					countForIndentationLevel = false;
				}
			}
		}
		lastConsumedCharacter = nextChar;
		currentChar = nextChar;
		return currentChar;
	}

	/**
	 * Returns the next character without consuming it. The current character and all position
	 * counters stay unchanged.
	 *
	 * @return the next character, or null if the end of data has been reached
	 * @throws IOException
	 *             if reading fails
	 */
	protected Character peek() throws IOException {
		final Character previousCurrentChar = currentChar;
		final Character nextChar = readNextCharacter();
		if (nextChar != null) {
			unread(1);
		}
		currentChar = previousCurrentChar;
		return nextChar;
	}

	/**
	 * Returns the character after the next character without consuming any of them. The current
	 * character and all position counters stay unchanged.
	 *
	 * @return the character after the next character, or null if the end of data is reached before
	 * @throws IOException
	 *             if reading fails
	 */
	protected Character peekAhead() throws IOException {
		final Character previousCurrentChar = currentChar;
		Character nextChar2 = null;
		final Character nextChar1 = readNextCharacter();
		if (nextChar1 != null) {
			nextChar2 = readNextCharacter();
			unread(nextChar2 != null ? 2 : 1);
		}
		currentChar = previousCurrentChar;
		return nextChar2;
	}

	/**
	 * Checks whether the end of data has been reached, without consuming any character.
	 *
	 * @return true, if there are no more characters to read
	 * @throws IOException
	 *             if reading fails
	 */
	protected boolean isEOF() throws IOException {
		return peek() == null;
	}

	/**
	 * Reads characters until the first non-whitespace character.
	 *
	 * @return the first non-whitespace character, or null if the end of data has been reached
	 * @throws Exception
	 *             if reading from the input fails
	 */
	protected Character readNextNonWhitespace() throws Exception {
		readNextCharacter();
		while (currentChar != null && Character.isWhitespace(currentChar)) {
			readNextCharacter();
		}
		return currentChar;
	}

	/**
	 * Reads characters until the first non-whitespace character or line break.
	 *
	 * @return the first non-whitespace character or line break, or null if the end of data has
	 *         been reached
	 * @throws Exception
	 *             if reading from the input fails
	 */
	protected Character readNextNonWhitespaceWithinLine() throws Exception {
		readNextCharacter();
		while (currentChar != null && currentChar != '\r' && currentChar != '\n' && Character.isWhitespace(currentChar)) {
			readNextCharacter();
		}
		return currentChar;
	}

	/**
	 * Reads the rest of the current line. The terminating line break (CR, LF or CRLF) is consumed
	 * but not included in the result.
	 *
	 * @return the line read, or null if the end of data has been reached before
	 * @throws Exception
	 *             if reading from the input fails
	 */
	protected String readNextLine() throws Exception {
		Character nextChar = readNextCharacter();
		if (nextChar == null) {
			return null;
		}

		final StringBuilder nextLine = new StringBuilder();
		while (nextChar != null && nextChar != '\r' && nextChar != '\n') {
			nextLine.append(nextChar);
			nextChar = readNextCharacter();
		}
		if (nextChar != null && nextChar == '\r') {
			final Character followingChar = peek();
			if (followingChar != null && followingChar == '\n') {
				readNextCharacter();
			}
		}
		return nextLine.toString();
	}

	/**
	 * Reads text starting with the current character up to the next occurrence of one of the given
	 * limit characters.
	 * <p>
	 * Escape sequences introduced by the escape character are resolved (see
	 * {@link #readUpToNextString(boolean, Character, String...)}). An escaped limit character does
	 * not end the text.
	 * </p>
	 *
	 * @param includeLimitChars
	 *            true to append the found limit character to the result, false to unread it so it
	 *            is delivered by the next read operation
	 * @param escapeCharacter
	 *            the escape character, or null if no escaping is used
	 * @param endChars
	 *            the limit characters ending the text
	 * @return the text read, or the text up to the end of data if no limit character was found
	 * @throws Exception
	 *             if the limit characters are not distinct, the escape character is one of the
	 *             limit characters, an invalid escape sequence is found, or reading fails
	 */
	protected String readUpToNext(final boolean includeLimitChars, final Character escapeCharacter, final char... endChars) throws Exception {
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
			final Character nextChar = readNextCharacter();
			if (nextChar == null) {
				return returnValue.toString();
			} else if (escapeNextCharacter) {
				escapeNextCharacter = false;
				currentChar = resolveEscapedCharacter(escapeCharacter, nextChar);
				returnValue.append(currentChar);
			} else if (escapeCharacter != null && nextChar.charValue() == escapeCharacter.charValue()) {
				escapeNextCharacter = true;
			} else if (contains(endChars, nextChar)) {
				if (includeLimitChars) {
					returnValue.append(nextChar);
				} else {
					unread(1);
				}
				return returnValue.toString();
			} else {
				returnValue.append(nextChar);
			}
		}
	}

	/**
	 * Reads text starting with the current character up to the next occurrence of one of the given
	 * limit strings.
	 * <p>
	 * Escape sequences introduced by the escape character are resolved: the escape character
	 * itself, double and single quote, slash, b, f, n, r, t and unicode sequences like u00e4.
	 * Escaped characters never form part of a limit string.
	 * </p>
	 *
	 * @param includeEndString
	 *            true to append the found limit string to the result, false to unread it so it is
	 *            delivered by the next read operations
	 * @param escapeCharacter
	 *            the escape character, or null if no escaping is used
	 * @param endStrings
	 *            the limit strings ending the text, each with 1 to {@link #MAX_UNREAD_CHARACTERS}
	 *            characters
	 * @return the text read, or the text up to the end of data if no limit string was found
	 * @throws Exception
	 *             if a limit string is empty or too long, an invalid escape sequence is found, or
	 *             reading fails
	 */
	protected String readUpToNextString(final boolean includeEndString, final Character escapeCharacter, final String... endStrings) throws Exception {
		if (endStrings == null || endStrings.length == 0) {
			throw new Exception("Missing limit strings");
		}
		for (final String endString : endStrings) {
			if (endString == null || endString.isEmpty() || endString.length() > MAX_UNREAD_CHARACTERS) {
				throw new Exception("Invalid limit string: " + endString);
			}
		}

		final StringBuilder returnValue = new StringBuilder();
		if (currentChar != null) {
			returnValue.append(currentChar);
		}
		// Limit strings may only match characters after this index, which were not escaped
		int matchableStartIndex = 0;
		boolean escapeNextCharacter = false;
		while (true) {
			final Character nextChar = readNextCharacter();
			if (nextChar == null) {
				return returnValue.toString();
			} else if (escapeNextCharacter) {
				escapeNextCharacter = false;
				currentChar = resolveEscapedCharacter(escapeCharacter, nextChar);
				returnValue.append(currentChar);
				matchableStartIndex = returnValue.length();
			} else if (escapeCharacter != null && nextChar.charValue() == escapeCharacter.charValue()) {
				escapeNextCharacter = true;
			} else {
				returnValue.append(nextChar);
				for (final String endString : endStrings) {
					final int endStringStartIndex = returnValue.length() - endString.length();
					if (endStringStartIndex >= matchableStartIndex && returnValue.indexOf(endString, endStringStartIndex) == endStringStartIndex) {
						if (includeEndString) {
							return returnValue.toString();
						} else {
							unread(endString.length());
							return returnValue.substring(0, endStringStartIndex);
						}
					}
				}
			}
		}
	}

	/**
	 * Reads a quoted text. The current character must be the opening quote character, which is
	 * also used as closing quote character.
	 *
	 * @param escapeCharacter
	 *            the escape character, or null if no escaping is used. If it equals the quote
	 *            character, a doubled quote character represents a single one.
	 * @return the unquoted text with resolved escape sequences
	 * @throws Exception
	 *             if there is no current character, the closing quote is missing, an invalid escape
	 *             sequence is found, or reading fails
	 */
	protected String readQuotedText(final Character escapeCharacter) throws Exception {
		if (currentChar == null) {
			throw new Exception("Invalid start of quoted text" + getCurrentPositionText() + ": end of data");
		}
		return readQuotedText(currentChar, escapeCharacter);
	}

	/**
	 * Reads a quoted text. The current character must be the given quote character.
	 * <p>
	 * If the escape character differs from the quote character, escape sequences are resolved as
	 * described in {@link #readUpToNextString(boolean, Character, String...)}. If it equals the
	 * quote character, a doubled quote character represents a single one and no other escape
	 * sequences exist. Line breaks are never normalized within quoted text. After reading, the
	 * current character is the closing quote character.
	 * </p>
	 *
	 * @param quoteChar
	 *            the quote character enclosing the text
	 * @param escapeCharacter
	 *            the escape character, or null if no escaping is used
	 * @return the unquoted text with resolved escape sequences
	 * @throws Exception
	 *             if the current character is not the quote character, the closing quote is
	 *             missing, an invalid escape sequence is found, or reading fails
	 */
	protected String readQuotedText(final char quoteChar, final Character escapeCharacter) throws Exception {
		if (currentChar == null || currentChar.charValue() != quoteChar) {
			throw new Exception("Invalid start of quoted text" + getCurrentPositionText());
		}

		final boolean previousNormalizeLinebreaks = normalizeLinebreaks;
		normalizeLinebreaks = false;
		try {
			if (escapeCharacter == null || escapeCharacter.charValue() != quoteChar) {
				final String returnValue = readUpToNext(true, escapeCharacter, quoteChar);
				if (currentChar == null) {
					throw new Exception("Missing closing quote character" + getCurrentPositionText());
				}
				return returnValue.substring(1, returnValue.length() - 1);
			} else {
				final StringBuilder returnValue = new StringBuilder();
				while (true) {
					final Character nextChar = readNextCharacter();
					if (nextChar == null) {
						throw new Exception("Missing closing quote character" + getCurrentPositionText());
					} else if (nextChar.charValue() == quoteChar) {
						final Character followingChar = peek();
						if (followingChar != null && followingChar.charValue() == quoteChar) {
							// Doubled quote character represents a single one
							readNextCharacter();
							returnValue.append(quoteChar);
						} else {
							return returnValue.toString();
						}
					} else {
						returnValue.append(nextChar);
					}
				}
			}
		} finally {
			normalizeLinebreaks = previousNormalizeLinebreaks;
		}
	}

	/**
	 * Returns a text describing the current reading position for error messages, like
	 * " at character index 7 in line 3". The character index is zero based, the line number one
	 * based.
	 *
	 * @return the position text, starting with a space
	 */
	protected String getCurrentPositionText() {
		return getPositionText(readCharactersInCurrentLine - 1);
	}

	/**
	 * Returns a text describing a position within the current line for error messages, like
	 * " at character index 7 in line 3".
	 *
	 * @param characterIndex
	 *            the zero based character index within the current line
	 * @return the position text, starting with a space
	 */
	private String getPositionText(final long characterIndex) {
		return " at character index " + Math.max(0, characterIndex) + " in line " + (readLines + 1);
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
	}

	/**
	 * Resolves the character following an escape character. For unicode sequences the four hex
	 * digits are read from the input.
	 *
	 * @param escapeCharacter
	 *            the escape character in use
	 * @param escapedChar
	 *            the character following the escape character
	 * @return the character represented by the escape sequence
	 * @throws Exception
	 *             if the escape sequence is invalid or reading fails
	 */
	private char resolveEscapedCharacter(final Character escapeCharacter, final char escapedChar) throws Exception {
		if (escapeCharacter != null && escapedChar == escapeCharacter.charValue()) {
			return escapedChar;
		}
		switch (escapedChar) {
			case '"':
			case '/':
				return escapedChar;
			case '\'':
				// Single quotes should not be escaped, but we allow them here for user convenience
				return escapedChar;
			case 'b':
				return '\b';
			case 'f':
				return '\f';
			case 'n':
				return '\n';
			case 'r':
				return '\r';
			case 't':
				return '\t';
			case 'u': {
				// Java encoded character
				final StringBuilder unicode = new StringBuilder();
				int value = 0;
				for (int i = 0; i < 4; i++) {
					final Character hexDigit = readNextCharacter();
					if (hexDigit != null) {
						unicode.append(hexDigit);
					}
					final int digitValue = hexDigit == null ? -1 : hexDigitValue(hexDigit);
					if (digitValue == -1) {
						// Position of the first hex digit of the sequence
						throw new Exception("Invalid unicode sequence" + getPositionText(readCharactersInCurrentLine - unicode.length()) + " ('" + unicode + "')");
					}
					value = (value << 4) | digitValue;
				}
				return (char) value;
			}
			default:
				throw new Exception("Invalid escape sequence" + getCurrentPositionText() + " ('" + escapedChar + "')");
		}
	}

	/**
	 * Returns the value of an ASCII hexadecimal digit (0-9, a-f, A-F). Unlike
	 * {@link Character#digit(char, int)} this does not accept non ASCII digits like fullwidth or
	 * Arabic-Indic digits.
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
	 * Checks if a String value is null or contains only whitespace characters.
	 *
	 * @param value
	 *            the value
	 * @return true, if is blank
	 */
	protected static boolean isBlank(final String value) {
		return value == null || value.trim().length() == 0;
	}

	/**
	 * Checks if a String value is not null and has a length greater than 0.
	 *
	 * @param value
	 *            the value
	 * @return true, if is not empty
	 */
	protected static boolean isNotEmpty(final String value) {
		return value != null && value.length() > 0;
	}

	/**
	 * Closes a Closeable item and ignores any Exception thrown by its close method.
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
		}

		for (final char character : characterArray) {
			if (character == searchCharacter.charValue()) {
				return true;
			}
		}

		return false;
	}
}
