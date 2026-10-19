package de.soderer.json.utilities;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Character based base reader for text formats like CSV, JSON or YAML.
 * <p>
 * Reads characters from an {@link InputStream} using a configurable encoding and skips a leading
 * byte order mark (BOM) of a UTF encoding. It keeps track of the number of characters, lines and
 * bytes read, of the character position within the current line and of the indentation level of
 * the current line.
 * </p>
 * <p>
 * Subclasses can use the helper methods to read quoted text or text up to given limit characters
 * or strings including escape sequence handling, and to look ahead or unread up to
 * {@link #MAX_UNREAD_CHARACTERS} of the most recently read characters. Unreading restores all
 * position counters, so line and character positions stay correct.
 * </p>
 * <p>
 * Line breaks may be CR, LF or CRLF, each of them counts as a single line break. Optionally all
 * line breaks can be normalized to LF (see {@link #setNormalizeLinebreaks(boolean)}). Switching the
 * normalization also applies to characters already unread or peeked, so no line break form leaks
 * from one setting into the other.
 * </p>
 * <p>
 * The supported escape sequences are the JSON ones (see
 * {@link #readUpToNextString(boolean, Character, String...)}). They intentionally differ from the
 * extended set of BasicReadAheadReader.
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

	/** BOM (Byte Order Mark) character U+FEFF, as decoded by readers for any UTF encoding. */
	public static final char BOM_UTF_8_CHAR = (char) 65279;

	/** First character of a UTF-8 BOM (bytes EF BB BF) misread with a single byte Latin encoding like ISO-8859-1. */
	public static final char BOM_UTF_8_CHAR_ISO_8859 = (char) 239;

	/** Default input encoding. */
	public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	/** Maximum number of most recently read characters which can be unread. */
	public static final int MAX_UNREAD_CHARACTERS = 64;

	/**
	 * Internal history capacity. Two more than {@link #MAX_UNREAD_CHARACTERS}, so peek operations
	 * never reduce the number of characters that can be unread.
	 */
	private static final int HISTORY_CAPACITY = MAX_UNREAD_CHARACTERS + 2;

	/**
	 * Internal pushback capacity. Restoring the original form of normalized CRLF line breaks may
	 * split each pushed back character into two.
	 */
	private static final int PUSHBACK_CAPACITY = 2 * HISTORY_CAPACITY;

	/** Capacity of the raw lookahead stack (BOM check and CR handling). */
	private static final int RAW_LOOKAHEAD_CAPACITY = 4;

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

	/** Stack of raw characters already read from the input reader but not yet processed, the top is read first. */
	private final int[] rawLookahead = new int[RAW_LOOKAHEAD_CAPACITY];

	/** Number of entries on the raw lookahead stack. */
	private int rawLookaheadSize = 0;

	/** Character read most recently (possibly replaced by escape sequence handling), or null at end of data. */
	private Character currentChar = null;

	/** True, if the current character was unread and will be delivered again by the next read operations. */
	private boolean currentCharPending = false;

	/** Character consumed most recently, never changed by escape sequence handling, or EOF. Used to detect CRLF. */
	private int lastConsumedCharacter = EOF;

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
	private final char[] historyCharacter = new char[HISTORY_CAPACITY];

	/** History: number of raw input characters each consumed character represents. */
	private final int[] historyRawCount = new int[HISTORY_CAPACITY];

	/** History: true, if the consumed character is an LF normalized from CR or CRLF. */
	private final boolean[] historyNormalized = new boolean[HISTORY_CAPACITY];

	/** History: read characters counter before consumption. */
	private final long[] historyReadCharacters = new long[HISTORY_CAPACITY];

	/** History: characters in current line counter before consumption. */
	private final long[] historyReadCharactersInCurrentLine = new long[HISTORY_CAPACITY];

	/** History: read lines counter before consumption. */
	private final long[] historyReadLines = new long[HISTORY_CAPACITY];

	/** History: indentation level before consumption. */
	private final int[] historyIndentationLevel = new int[HISTORY_CAPACITY];

	/** History: indentation counting flag before consumption. */
	private final boolean[] historyCountForIndentationLevel = new boolean[HISTORY_CAPACITY];

	/** History: last consumed character before consumption. */
	private final int[] historyLastConsumedCharacter = new int[HISTORY_CAPACITY];

	/** Ring buffer index of the oldest history entry. */
	private int historyStart = 0;

	/** Number of history entries, which is the number of characters that can be unread. */
	private int historySize = 0;

	/** Stack of unread characters to be delivered again, the top of the stack is delivered first. */
	private final char[] pushbackCharacter = new char[PUSHBACK_CAPACITY];

	/** Stack of the raw character counts of the unread characters. */
	private final int[] pushbackRawCount = new int[PUSHBACK_CAPACITY];

	/** Stack of the normalization flags of the unread characters. */
	private final boolean[] pushbackNormalized = new boolean[PUSHBACK_CAPACITY];

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
	 * The setting also applies to characters already unread or peeked: switching it on normalizes
	 * their line breaks, switching it off restores their original line breaks.
	 * </p>
	 *
	 * @param normalizeLinebreaks
	 *            true to normalize line breaks
	 */
	protected void setNormalizeLinebreaks(final boolean normalizeLinebreaks) {
		this.normalizeLinebreaks = normalizeLinebreaks;
		if (normalizeLinebreaks) {
			normalizePushback();
		} else {
			denormalizePushback();
		}
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
	 * Returns the number of raw input characters read so far. A normalized CRLF line break counts as
	 * two characters, a skipped BOM is not counted.
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
	 * nothing at the end of data or if the current character was already unread.
	 * <p>
	 * The position counters are restored, the value of {@link #getCurrentChar()} stays unchanged.
	 * </p>
	 */
	public void reuseCurrentChar() {
		if (currentChar != null && !currentCharPending && historySize > 0) {
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
				final int index = (historyStart + historySize - charactersToReuse.length + i) % HISTORY_CAPACITY;
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
	 * {@link #getCurrentChar()} stays unchanged, but it is marked as pending, so the read helper
	 * methods do not use it a second time.
	 *
	 * @param count
	 *            the number of characters to unread, at least {@link #MAX_UNREAD_CHARACTERS} are
	 *            available after reading enough characters
	 * @throws IllegalStateException
	 *             if the count is negative or more characters are requested than available
	 */
	protected void unread(final int count) {
		if (count < 0 || count > historySize) {
			throw new IllegalStateException("Cannot unread " + count + " characters, only " + historySize + " available");
		} else if (pushbackSize + count > PUSHBACK_CAPACITY) {
			throw new IllegalStateException("Cannot unread " + count + " characters, pushback buffer is full");
		}
		for (int i = 0; i < count; i++) {
			historySize--;
			final int index = (historyStart + historySize) % HISTORY_CAPACITY;
			readCharacters = historyReadCharacters[index];
			readCharactersInCurrentLine = historyReadCharactersInCurrentLine[index];
			readLines = historyReadLines[index];
			currentIndentationLevel = historyIndentationLevel[index];
			countForIndentationLevel = historyCountForIndentationLevel[index];
			lastConsumedCharacter = historyLastConsumedCharacter[index];
			pushbackCharacter[pushbackSize] = historyCharacter[index];
			pushbackRawCount[pushbackSize] = historyRawCount[index];
			pushbackNormalized[pushbackSize] = historyNormalized[index];
			pushbackSize++;
		}
		if (count > 0) {
			currentCharPending = true;
		}
	}

	/**
	 * Reads the next character from the input.
	 * <p>
	 * A UTF BOM at the start of the data is skipped. Unread characters are delivered first.
	 * </p>
	 *
	 * @return the next character, or null if the end of data has been reached
	 * @throws IOException
	 *             if reading fails or a UTF-8 BOM is detected while reading with a single byte Latin
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
			return consume(pushbackCharacter[pushbackSize], pushbackRawCount[pushbackSize], pushbackNormalized[pushbackSize]);
		}

		int nextCharInt = readRawCharacter();
		if (!bomChecked && nextCharInt != EOF) {
			bomChecked = true;
			// The BOM character U+FEFF also remains with UTF-16LE/BE and UTF-32 decoders, only "UTF-16" removes it itself
			if (nextCharInt == BOM_UTF_8_CHAR && encoding.name().toUpperCase(Locale.ROOT).startsWith("UTF-")) {
				nextCharInt = readRawCharacter();
			} else if (nextCharInt == BOM_UTF_8_CHAR_ISO_8859 && isSingleByteLatinEncoding(encoding)) {
				// Only the complete sequence EF BB BF is a misread UTF-8 BOM, a single 'ï' is valid text
				final int secondCharInt = readRawCharacter();
				final int thirdCharInt = readRawCharacter();
				if (secondCharInt == 0xBB && thirdCharInt == 0xBF) {
					throw new IOException("Data encoding \"" + encoding + "\" is invalid: UTF-8 BOM detected");
				}
				// Push back in reverse order, so the characters are read again in their original order
				if (thirdCharInt != EOF) {
					pushRawLookahead(thirdCharInt);
				}
				if (secondCharInt != EOF) {
					pushRawLookahead(secondCharInt);
				}
			}
		}

		if (nextCharInt == EOF) {
			currentChar = null;
			currentCharPending = false;
			return null;
		}

		char nextChar = (char) nextCharInt;
		int rawCount = 1;
		boolean normalized = false;
		if (nextChar == '\r' && normalizeLinebreaks) {
			final int followingCharInt = readRawCharacter();
			if (followingCharInt == '\n') {
				rawCount = 2;
			} else if (followingCharInt != EOF) {
				pushRawLookahead(followingCharInt);
			}
			nextChar = '\n';
			normalized = true;
		}
		return consume(nextChar, rawCount, normalized);
	}

	/**
	 * Checks whether the encoding is a single byte Latin encoding, which shows a UTF-8 BOM as the
	 * characters "ï»¿".
	 *
	 * @param charset
	 *            the encoding to check
	 * @return true, if the encoding is ISO-8859-* or windows-1252
	 */
	private static boolean isSingleByteLatinEncoding(final Charset charset) {
		final String name = charset.name().toUpperCase(Locale.ROOT);
		return name.startsWith("ISO-8859-") || "WINDOWS-1252".equals(name);
	}

	/**
	 * Reads the next raw character from the input reader, preferring pending lookahead characters.
	 *
	 * @return the raw character, or {@link #EOF} at the end of data
	 * @throws IOException
	 *             if reading fails
	 */
	private int readRawCharacter() throws IOException {
		if (rawLookaheadSize > 0) {
			rawLookaheadSize--;
			return rawLookahead[rawLookaheadSize];
		} else {
			return inputReader.read();
		}
	}

	/**
	 * Pushes a raw character back, so it is read again before any further input data.
	 *
	 * @param rawCharacter
	 *            the raw character
	 * @throws IllegalStateException
	 *             if the raw lookahead stack is full, which indicates an internal error
	 */
	private void pushRawLookahead(final int rawCharacter) {
		if (rawLookaheadSize >= RAW_LOOKAHEAD_CAPACITY) {
			throw new IllegalStateException("Raw lookahead buffer overflow");
		}
		rawLookahead[rawLookaheadSize] = rawCharacter;
		rawLookaheadSize++;
	}

	/**
	 * Makes the given character the current character, updates all position counters and records
	 * the previous state for unreading.
	 *
	 * @param nextChar
	 *            the character to consume
	 * @param rawCount
	 *            the number of raw input characters it represents (2 for a normalized CRLF)
	 * @param normalized
	 *            true, if the character is an LF normalized from CR or CRLF
	 * @return the consumed character
	 */
	private Character consume(final char nextChar, final int rawCount, final boolean normalized) {
		final int index;
		if (historySize == HISTORY_CAPACITY) {
			index = historyStart;
			historyStart = (historyStart + 1) % HISTORY_CAPACITY;
		} else {
			index = (historyStart + historySize) % HISTORY_CAPACITY;
			historySize++;
		}
		historyCharacter[index] = nextChar;
		historyRawCount[index] = rawCount;
		historyNormalized[index] = normalized;
		historyReadCharacters[index] = readCharacters;
		historyReadCharactersInCurrentLine[index] = readCharactersInCurrentLine;
		historyReadLines[index] = readLines;
		historyIndentationLevel[index] = currentIndentationLevel;
		historyCountForIndentationLevel[index] = countForIndentationLevel;
		historyLastConsumedCharacter[index] = lastConsumedCharacter;

		readCharacters += rawCount;
		// A normalized LF is a complete line break of its own, even if a raw CR was consumed before
		if (nextChar == '\r' || (nextChar == '\n' && (normalized || lastConsumedCharacter != '\r'))) {
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
		currentCharPending = false;
		return currentChar;
	}

	/**
	 * Normalizes the line breaks of the unread characters. A raw CR as last unread character is
	 * moved to the raw lookahead, so it is normalized together with its yet unknown successor.
	 */
	private void normalizePushback() {
		if (pushbackSize == 0) {
			return;
		}

		// Entries in delivery order, index 0 is delivered first
		final int count = pushbackSize;
		final char[] characters = new char[count];
		final int[] rawCounts = new int[count];
		final boolean[] normalizedFlags = new boolean[count];
		boolean anyRawCr = false;
		for (int i = 0; i < count; i++) {
			final int stackIndex = count - 1 - i;
			characters[i] = pushbackCharacter[stackIndex];
			rawCounts[i] = pushbackRawCount[stackIndex];
			normalizedFlags[i] = pushbackNormalized[stackIndex];
			anyRawCr |= characters[i] == '\r';
		}
		if (!anyRawCr) {
			return;
		}

		final char[] newCharacters = new char[count];
		final int[] newRawCounts = new int[count];
		final boolean[] newNormalizedFlags = new boolean[count];
		int newCount = 0;
		boolean moveLastCrToLookahead = false;
		for (int i = 0; i < count; i++) {
			if (characters[i] == '\r') {
				if (i == count - 1) {
					moveLastCrToLookahead = true;
				} else if (characters[i + 1] == '\n' && !normalizedFlags[i + 1]) {
					newCharacters[newCount] = '\n';
					newRawCounts[newCount] = rawCounts[i] + rawCounts[i + 1];
					newNormalizedFlags[newCount] = true;
					newCount++;
					i++;
				} else {
					newCharacters[newCount] = '\n';
					newRawCounts[newCount] = rawCounts[i];
					newNormalizedFlags[newCount] = true;
					newCount++;
				}
			} else {
				newCharacters[newCount] = characters[i];
				newRawCounts[newCount] = rawCounts[i];
				newNormalizedFlags[newCount] = normalizedFlags[i];
				newCount++;
			}
		}

		setPushback(newCharacters, newRawCounts, newNormalizedFlags, newCount);
		if (moveLastCrToLookahead) {
			// The raw lookahead is read after all unread characters, so the order is kept
			pushRawLookahead('\r');
		}
	}

	/**
	 * Restores the original line breaks of normalized unread characters.
	 */
	private void denormalizePushback() {
		boolean anyNormalized = false;
		for (int i = 0; i < pushbackSize; i++) {
			if (pushbackNormalized[i]) {
				anyNormalized = true;
				break;
			}
		}
		if (!anyNormalized) {
			return;
		}

		final int count = pushbackSize;
		final char[] newCharacters = new char[count * 2];
		final int[] newRawCounts = new int[count * 2];
		final boolean[] newNormalizedFlags = new boolean[count * 2];
		int newCount = 0;
		// Entries in delivery order, the top of the stack first
		for (int stackIndex = count - 1; stackIndex >= 0; stackIndex--) {
			if (pushbackNormalized[stackIndex]) {
				newCharacters[newCount] = '\r';
				newRawCounts[newCount] = 1;
				newCount++;
				if (pushbackRawCount[stackIndex] == 2) {
					newCharacters[newCount] = '\n';
					newRawCounts[newCount] = 1;
					newCount++;
				}
			} else {
				newCharacters[newCount] = pushbackCharacter[stackIndex];
				newRawCounts[newCount] = pushbackRawCount[stackIndex];
				newCount++;
			}
		}
		setPushback(newCharacters, newRawCounts, newNormalizedFlags, newCount);
	}

	/**
	 * Replaces the unread characters.
	 *
	 * @param characters
	 *            the characters in delivery order
	 * @param rawCounts
	 *            the raw character counts in delivery order
	 * @param normalizedFlags
	 *            the normalization flags in delivery order
	 * @param count
	 *            the number of entries to use
	 * @throws IllegalStateException
	 *             if the pushback buffer is too small, which indicates an internal error
	 */
	private void setPushback(final char[] characters, final int[] rawCounts, final boolean[] normalizedFlags, final int count) {
		if (count > PUSHBACK_CAPACITY) {
			throw new IllegalStateException("Pushback buffer overflow");
		}
		for (int i = 0; i < count; i++) {
			final int stackIndex = count - 1 - i;
			pushbackCharacter[stackIndex] = characters[i];
			pushbackRawCount[stackIndex] = rawCounts[i];
			pushbackNormalized[stackIndex] = normalizedFlags[i];
		}
		pushbackSize = count;
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
		final boolean previousCurrentCharPending = currentCharPending;
		final Character nextChar = readNextCharacter();
		if (nextChar != null) {
			unread(1);
		}
		currentChar = previousCurrentChar;
		currentCharPending = previousCurrentCharPending;
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
		final boolean previousCurrentCharPending = currentCharPending;
		Character nextChar2 = null;
		final Character nextChar1 = readNextCharacter();
		if (nextChar1 != null) {
			nextChar2 = readNextCharacter();
			unread(nextChar2 != null ? 2 : 1);
		}
		currentChar = previousCurrentChar;
		currentCharPending = previousCurrentCharPending;
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
	 * limit characters. A current character that was unread is not used, because it is delivered
	 * again by the next read operation.
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
	 *             limit characters, an invalid or incomplete escape sequence is found, or reading
	 *             fails
	 */
	protected String readUpToNext(final boolean includeLimitChars, final Character escapeCharacter, final char... endChars) throws Exception {
		if (anyCharsAreEqual(endChars)) {
			throw new Exception("Invalid set of limit characters, includes duplicates: '" + new String(endChars) + "'");
		} else if (contains(endChars, escapeCharacter)) {
			throw new Exception("Invalid escape character is included in limit characters: '" + escapeCharacter + "'");
		}

		final StringBuilder returnValue = new StringBuilder();
		if (currentChar != null && !currentCharPending) {
			returnValue.append(currentChar);
		}
		boolean escapeNextCharacter = false;
		String escapePositionText = null;
		while (true) {
			final Character nextChar = readNextCharacter();
			if (nextChar == null) {
				if (escapeNextCharacter) {
					throw new Exception("Incomplete escape sequence at end of data" + escapePositionText);
				}
				return returnValue.toString();
			} else if (escapeNextCharacter) {
				escapeNextCharacter = false;
				currentChar = resolveEscapedCharacter(escapeCharacter, nextChar);
				returnValue.append(currentChar);
			} else if (escapeCharacter != null && nextChar.charValue() == escapeCharacter.charValue()) {
				escapeNextCharacter = true;
				escapePositionText = getCurrentPositionText();
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
	 * limit strings. A current character that was unread is not used, because it is delivered again
	 * by the next read operation.
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
	 *            the distinct limit strings ending the text, each with 1 to
	 *            {@link #MAX_UNREAD_CHARACTERS} characters and not containing the escape character
	 * @return the text read, or the text up to the end of data if no limit string was found
	 * @throws Exception
	 *             if a limit string is empty, too long, duplicate or contains the escape character,
	 *             an invalid or incomplete escape sequence is found, or reading fails
	 */
	protected String readUpToNextString(final boolean includeEndString, final Character escapeCharacter, final String... endStrings) throws Exception {
		if (endStrings == null || endStrings.length == 0) {
			throw new Exception("Missing limit strings");
		}
		for (int i = 0; i < endStrings.length; i++) {
			final String endString = endStrings[i];
			if (endString == null || endString.isEmpty() || endString.length() > MAX_UNREAD_CHARACTERS) {
				throw new Exception("Invalid limit string: " + endString);
			} else if (escapeCharacter != null && endString.indexOf(escapeCharacter.charValue()) >= 0) {
				// Escape characters are never part of the result, so such a limit string could never match
				throw new Exception("Invalid limit string, contains the escape character '" + escapeCharacter + "': " + endString);
			}
			for (int j = i + 1; j < endStrings.length; j++) {
				if (endString.equals(endStrings[j])) {
					throw new Exception("Invalid set of limit strings, includes duplicates: " + endString);
				}
			}
		}

		final StringBuilder returnValue = new StringBuilder();
		if (currentChar != null && !currentCharPending) {
			returnValue.append(currentChar);
		}
		// Limit strings may only match characters after this index, which were not escaped
		int matchableStartIndex = 0;
		boolean escapeNextCharacter = false;
		String escapePositionText = null;
		while (true) {
			final Character nextChar = readNextCharacter();
			if (nextChar == null) {
				if (escapeNextCharacter) {
					throw new Exception("Incomplete escape sequence at end of data" + escapePositionText);
				}
				return returnValue.toString();
			} else if (escapeNextCharacter) {
				escapeNextCharacter = false;
				currentChar = resolveEscapedCharacter(escapeCharacter, nextChar);
				returnValue.append(currentChar);
				matchableStartIndex = returnValue.length();
			} else if (escapeCharacter != null && nextChar.charValue() == escapeCharacter.charValue()) {
				escapeNextCharacter = true;
				escapePositionText = getCurrentPositionText();
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
	 * Reads a quoted text. The current character must be the given quote character. If it was
	 * unread, it is read again first.
	 * <p>
	 * If the escape character differs from the quote character, escape sequences are resolved as
	 * described in {@link #readUpToNextString(boolean, Character, String...)}. If it equals the
	 * quote character, a doubled quote character represents a single one and no other escape
	 * sequences exist. Line breaks within the quoted text are always delivered as in the input data.
	 * After reading, the current character is the closing quote character.
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
		} else if (currentCharPending) {
			// The opening quote was unread, so it must be the next character delivered
			final Character openingQuote = readNextCharacter();
			if (openingQuote == null || openingQuote.charValue() != quoteChar) {
				throw new Exception("Invalid start of quoted text" + getCurrentPositionText());
			}
		}

		final boolean previousNormalizeLinebreaks = normalizeLinebreaks;
		// Also restores the original line breaks of characters already unread or peeked
		setNormalizeLinebreaks(false);
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
			// Also normalizes a line break peeked raw behind the closing quote
			setNormalizeLinebreaks(previousNormalizeLinebreaks);
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
		return getPositionText(readCharactersInCurrentLine - 1, readLines + 1);
	}

	/**
	 * Returns a text describing a position for error messages, like " at character index 7 in line
	 * 3".
	 *
	 * @param characterIndex
	 *            the zero based character index within the line
	 * @param lineNumber
	 *            the one based line number
	 * @return the position text, starting with a space
	 */
	private static String getPositionText(final long characterIndex, final long lineNumber) {
		return " at character index " + Math.max(0, characterIndex) + " in line " + lineNumber;
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
				// Java encoded character, the position of the first hex digit is kept for error messages, because line breaks reset the column
				final long startCharacterIndex = readCharactersInCurrentLine;
				final long startLineNumber = readLines + 1;
				final StringBuilder unicode = new StringBuilder();
				int value = 0;
				for (int i = 0; i < 4; i++) {
					final Character hexDigit = readNextCharacter();
					if (hexDigit != null) {
						unicode.append(hexDigit);
					}
					final int digitValue = hexDigit == null ? -1 : hexDigitValue(hexDigit);
					if (digitValue == -1) {
						throw new Exception("Invalid unicode sequence" + getPositionText(startCharacterIndex, startLineNumber) + " ('" + unicode + "')");
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
