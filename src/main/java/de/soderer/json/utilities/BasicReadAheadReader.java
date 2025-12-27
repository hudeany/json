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

	private CountingInputStream countingInputStream;

	private Character currentChar = null;
	private Character nextChar = null;
	private long currentColumn = 1;
	private long currentLine = 1;

	public BasicReadAheadReader(final InputStream inputStream) throws Exception {
		this(inputStream, null);
	}

	public BasicReadAheadReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		if (inputStream == null) {
			throw new Exception("Invalid empty inputStream");
		} else {
			countingInputStream = new CountingInputStream(inputStream);
			encoding = encodingCharset == null ? DEFAULT_ENCODING : encodingCharset;
			inputReader = new BufferedReader(new InputStreamReader(countingInputStream, encoding));
		}

		initializeBuffers();
	}

	protected boolean isNormalizeLinebreaks() {
		return normalizeLinebreaks;
	}

	protected void setNormalizeLinebreaks(final boolean normalizeLinebreaks) throws IOException {
		this.normalizeLinebreaks = normalizeLinebreaks;

		if (normalizeLinebreaks && currentChar != null && nextChar != null) {
			if (currentChar == '\r' && nextChar == '\n') {
				currentChar = '\n';
				readNextChar();
			}
		} else if (normalizeLinebreaks && currentChar != null && currentChar == '\r') {
			currentChar = '\n';
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
					currentChar = (char) currentCharInt;
					readNextChar();
				}
			} else if (currentCharInt == BOM_UTF_8_CHAR_ISO_8859 && encoding.displayName().toUpperCase().startsWith("ISO-8859-")) {
				throw new IOException("Data encoding \"" + encoding + "\" is invalid: UTF-8 BOM detected");
			} else {
				currentChar = (char) currentCharInt;
				readNextChar();
			}
		}

		if (normalizeLinebreaks && currentChar != null && nextChar != null) {
			if (currentChar == '\r' && nextChar == '\n') {
				currentChar = '\n';
				readNextChar();
			}
		} else if (normalizeLinebreaks && currentChar != null && currentChar == '\r') {
			currentChar = '\n';
		}
	}

	protected Character readChar() throws IOException {
		final Character returnChar = currentChar;
		currentChar = nextChar;
		if (currentChar != null) {
			readNextChar();
		}

		if (normalizeLinebreaks && currentChar != null && nextChar != null) {
			if (currentChar == '\r' && nextChar == '\n') {
				currentChar = '\n';
				readNextChar();
			}
		} else if (normalizeLinebreaks && currentChar != null && currentChar == '\r') {
			currentChar = '\n';
		}

		if (returnChar != null) {
			if (returnChar == '\r' || returnChar == '\n') {
				currentColumn = 1;
				currentLine++;
			} else {
				currentColumn++;
			}
		}

		return returnChar;
	}

	private void readNextChar() throws IOException {
		final int newNextCharInt = inputReader.read();
		if (newNextCharInt == -1) {
			nextChar = null;
		} else {
			nextChar = (char) newNextCharInt;
		}
	}

	protected Character peekChar() {
		return currentChar;
	}

	protected Character peekNextChar() {
		return nextChar;
	}

	protected boolean isEOF() {
		return currentChar == null;
	}

	protected void skipWhitespaces() throws Exception {
		while (currentChar != null && Character.isWhitespace(currentChar)) {
			readChar();
		}
	}

	protected void skipWhitespacesInCurrentLine() throws Exception {
		while (currentChar != null && currentChar != '\r' && currentChar != '\n' && Character.isWhitespace(currentChar)) {
			readChar();
		}
	}

	protected String readLine() throws Exception {
		if (isEOF()) {
			return null;
		} else {
			String nextLine = "";
			while (!isEOF() && currentChar != '\r' && currentChar != '\n') {
				nextLine += readChar();
			}
			if (currentChar == '\r' || currentChar == '\n') {
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
		while (!isEOF()) {
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
				} else if ('b' == peekChar()) {
					readChar();
					unescapedChar = '\b';
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
				} else if ('u' == peekChar()) {
					readChar();
					// Java encoded character
					String unicode = "";
					for (int i = 0; i < 4 && !isEOF(); i++) {
						final Character hexDigit = readChar();
						unicode += hexDigit;
						if (hexDigit == null || Character.digit(hexDigit, 16) == -1) {
							throw new Exception("Invalid unicode sequence at character index " + (getCurrentColumn() - unicode.length()) + " in line " + getCurrentLine() + " ('" + unicode + "')");
						}
					}
					if (unicode.length() != 4) {
						throw new Exception("Invalid unicode sequence at character index " + (getCurrentColumn() - unicode.length()) + " in line " + getCurrentLine() + " ('" + unicode + "')");
					} else {
						unescapedChar = (char)Integer.parseInt(unicode.toString(), 16);
					}
				} else {
					throw new Exception("Invalid escape sequence at character index " + getCurrentColumn() + " in line " + getCurrentLine() + " ('" + currentChar + "')");
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
				while (!isEOF()) {
					if (currentChar == quoteChar) {
						returnValue += readChar();
						break;
					} else {
						returnValue += readChar();
					}
				}
				if (returnValue.charAt(returnValue.length()) != quoteChar) {
					throw new Exception("Missing closing quote character for quoted string beginning at line " + lineStart + " column " + columnStart);
				} else {
					returnValue = returnValue.substring(0, returnValue.length() - 1);
					returnValue = returnValue.replace("" + quoteChar + quoteChar, "" + quoteChar);
					return returnValue;
				}
			} else {
				final String returnValue = readUpToNext(true, escapeCharacter, quoteChar);
				if (returnValue.charAt(returnValue.length()) != quoteChar) {
					throw new Exception("Missing closing quote character for quoted string beginning at line " + lineStart + " column " + columnStart);
				} else {
					return returnValue.substring(0, returnValue.length() - 1);
				}
			}
		} finally {
			normalizeLinebreaks = previousSettingNormalizeLinebreaks;
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

		currentChar = null;
		nextChar = null;
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
}
