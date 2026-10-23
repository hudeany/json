package de.soderer.json.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Covers the protected read helpers of {@link BasicReadAheadReader}.
 */
@SuppressWarnings({ "static-method", "resource" })
class BasicReadAheadReaderTest {
	private static BasicReadAheadReader reader(final String text) throws Exception {
		return new BasicReadAheadReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
	}

	private static BasicReadAheadReader reader(final String text, final int readAhead) throws Exception {
		return new BasicReadAheadReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), readAhead);
	}

	private static BasicReadAheadReader reader(final byte[] data, final Charset charset) throws Exception {
		return new BasicReadAheadReader(new ByteArrayInputStream(data), charset);
	}

	private static String readAll(final BasicReadAheadReader reader) throws Exception {
		final StringBuilder result = new StringBuilder();
		Character next;
		while ((next = reader.readChar()) != null) {
			result.append(next);
		}
		return result.toString();
	}

	private static String unescape(final String escapedText) throws Exception {
		return reader(escapedText + ",").readUpToNext(false, '\\', ',');
	}

	@Test
	void constructorsAndClose() throws Exception {
		assertThrows(Exception.class, () -> new BasicReadAheadReader(new ByteArrayInputStream(new byte[0]), 0));
		assertThrows(Exception.class, () -> new BasicReadAheadReader(null));
		final BasicReadAheadReader reader = new BasicReadAheadReader(new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)), (Charset) null);
		assertTrue(reader.getReadDataSize() > 0);
		assertEquals('a', reader.readChar());
		assertEquals(1, reader.getCharactersRead());
		reader.close();
		assertEquals(0, reader.getReadDataSize());
		assertThrows(IllegalStateException.class, reader::readChar);
		assertTrue(BasicReadAheadReader.isBlank(null));
		assertFalse(BasicReadAheadReader.isBlank("x"));
		assertTrue(BasicReadAheadReader.isNotEmpty("x"));
		assertFalse(BasicReadAheadReader.isNotEmpty(null));
	}

	@Test
	void byteOrderMarks() throws Exception {
		assertEquals("a", readAll(reader(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a' }, StandardCharsets.UTF_8)));
		assertEquals("", readAll(reader(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }, StandardCharsets.UTF_8)));
		assertThrows(IOException.class, () -> reader(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a' }, StandardCharsets.ISO_8859_1));
		assertEquals("ïxy", readAll(reader(new byte[] { (byte) 0xEF, 'x', 'y' }, StandardCharsets.ISO_8859_1)));
		assertEquals("ï", readAll(reader(new byte[] { (byte) 0xEF }, Charset.forName("windows-1252"))));
	}

	@Test
	void linebreakNormalization() throws Exception {
		final BasicReadAheadReader reader = reader("a\r\nb\rc\nd");
		assertFalse(reader.isNormalizeLinebreaks());
		reader.setNormalizeLinebreaks(true);
		assertTrue(reader.isNormalizeLinebreaks());
		assertEquals("a\nb\nc\nd", readAll(reader));
		assertEquals(4, reader.getCurrentLine());
		assertEquals(2, reader.getCurrentColumn());

		final BasicReadAheadReader rawReader = reader("a\r\nb");
		assertEquals("a\r\nb", readAll(rawReader));
		assertEquals(2, rawReader.getCurrentLine());
	}

	@Test
	void restoreLinebreaksWhenSwitchingOff() throws Exception {
		final BasicReadAheadReader reader = reader("\r\n\r\n\rxyz", 3);
		reader.setNormalizeLinebreaks(true);
		assertEquals('\n', reader.peekChar());
		reader.setNormalizeLinebreaks(false);
		assertEquals("\r\n\r\n\rxyz", readAll(reader));

		// Switching off without any normalized line break in the buffer
		final BasicReadAheadReader plainReader = reader("abc");
		plainReader.setNormalizeLinebreaks(false);
		assertEquals("abc", readAll(plainReader));
	}

	@Test
	void peekMethods() throws Exception {
		final BasicReadAheadReader reader = reader("a\rbc", 3);
		assertEquals('a', reader.peekChar());
		assertTrue(reader.peekCharMatch('a'));
		assertTrue(reader.peekCharNotMatch('b'));
		assertTrue(reader.peekCharMatchAny("xa"));
		assertFalse(reader.peekCharMatchAny(null));
		assertTrue(reader.peekCharNotMatchAny("xyz"));
		assertEquals('\r', reader.peekNextChar(1));
		assertTrue(reader.peekNextCharMatch(2, 'b'));
		assertTrue(reader.peekNextCharNotMatch(2, 'c'));
		assertTrue(reader.peekNextCharMatchAny(1, "\r\n"));
		assertTrue(reader.peekNextCharNotMatchAny(2, "xyz"));
		assertThrows(IllegalArgumentException.class, () -> reader.peekNextChar(3));
		assertThrows(IllegalArgumentException.class, () -> reader.peekNextChar(-1));

		// While normalizing, a CR at the end of the buffer is already delivered as LF and CR matches LF
		reader.setNormalizeLinebreaks(true);
		assertEquals('\n', reader.peekNextChar(1));
		assertTrue(reader.peekNextCharMatch(1, '\r'));
		assertTrue(reader.peekNextCharMatchAny(1, "\r"));

		final BasicReadAheadReader empty = reader("");
		assertTrue(empty.isEOF());
		assertFalse(empty.isNotEOF());
		assertNull(empty.peekChar());
		assertFalse(empty.peekCharMatch('a'));
		assertFalse(empty.peekCharMatchAny("a"));
	}

	@Test
	void whitespaceAndLines() throws Exception {
		final BasicReadAheadReader reader = reader("  \t x\n \n y");
		reader.skipWhitespacesInCurrentLine();
		assertEquals('x', reader.readChar());
		reader.skipWhitespacesInCurrentLine();
		assertEquals('\n', reader.peekChar());
		reader.skipWhitespaces();
		assertEquals('y', reader.peekChar());
		assertEquals('z', reader(" \n\tz").readNextNonWhitespace());

		final BasicReadAheadReader lineReader = reader("l1\r\nl2\rl3\nl4");
		assertEquals("l1", lineReader.readLine());
		assertEquals("l2", lineReader.readLine());
		assertEquals("l3", lineReader.readLine());
		assertEquals("l4", lineReader.readLine());
		assertNull(lineReader.readLine());
	}

	@Test
	void readUpToNext() throws Exception {
		assertThrows(Exception.class, () -> reader("x").readUpToNext(false, null, ',', ','));
		assertThrows(Exception.class, () -> reader("x").readUpToNext(false, '\\', ',', '\\'));

		final BasicReadAheadReader reader = reader("ab,cd;e");
		assertEquals("ab", reader.readUpToNext(false, null, ',', ';'));
		assertEquals(",", reader.readUpToNext(true, null, ','));
		assertEquals("cd;", reader.readUpToNext(true, null, ';'));
		assertEquals("e", reader.readUpToNext(true, null, ';'));

		assertThrows(Exception.class, () -> reader("ab\\").readUpToNext(false, '\\', ','));

		// CR as limit character also matches a normalized line break
		final BasicReadAheadReader normalized = reader("ab\r\ncd");
		normalized.setNormalizeLinebreaks(true);
		assertEquals("ab", normalized.readUpToNext(false, null, '\r'));
	}

	@Test
	void escapeSequences() throws Exception {
		assertEquals("\\\"/'", unescape("\\\\\\\"\\/\\'"));
		assertEquals("Aä😀", unescape("\\x41\\u00E4\\U0001F600"));
		assertEquals("\u0007\b\u001B\f\n\r\t\t\u000B  \0\u0085  ", unescape("\\a\\b\\e\\f\\n\\r\\t\\\t\\v\\ \\_\\0\\N\\L\\P"));

		assertThrows(Exception.class, () -> unescape("\\q"));
		assertThrows(Exception.class, () -> unescape("\\xZZ"));
		assertThrows(Exception.class, () -> reader("\\x4").readUpToNext(false, '\\', ','));
		assertThrows(Exception.class, () -> unescape("\\U00110000"));
		assertThrows(Exception.class, () -> unescape("\\U0000D800"));
	}

	@Test
	void readQuotedText() throws Exception {
		assertThrows(Exception.class, () -> reader("").readQuotedText('\\'));

		assertEquals("a\"b", reader("\"a\\\"b\" rest").readQuotedText('\\'));
		assertEquals("it's", reader("'it''s' rest").readQuotedText('\''));
		assertThrows(Exception.class, () -> reader("'abc").readQuotedText('\''));
		assertThrows(Exception.class, () -> reader("\"abc").readQuotedText('\\'));

		// Line breaks within quotes are kept, the normalization setting is restored afterwards
		final BasicReadAheadReader normalized = reader("\"a\r\nb\"\r\nc");
		normalized.setNormalizeLinebreaks(true);
		assertEquals("a\r\nb", normalized.readQuotedText('\\'));
		assertTrue(normalized.isNormalizeLinebreaks());
		assertEquals('\n', normalized.readChar());
		assertEquals('c', normalized.readChar());

		// The setting is also restored when the quoted text is invalid
		final BasicReadAheadReader invalid = reader("\"a\\q\"\r\n");
		invalid.setNormalizeLinebreaks(true);
		assertThrows(Exception.class, () -> invalid.readQuotedText('\\'));
		assertTrue(invalid.isNormalizeLinebreaks());
	}
}
