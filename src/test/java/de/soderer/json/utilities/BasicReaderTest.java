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
 * Covers the protected read helpers of {@link BasicReader}: BOM handling, line break
 * normalization (also of unread characters), unreading, peeking and the text reading methods.
 */
@SuppressWarnings({ "static-method", "resource" })
class BasicReaderTest {
	private static BasicReader reader(final String text) throws Exception {
		return new BasicReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
	}

	private static BasicReader reader(final byte[] data, final Charset charset) throws Exception {
		return new BasicReader(new ByteArrayInputStream(data), charset);
	}

	private static String readAll(final BasicReader reader) throws Exception {
		final StringBuilder result = new StringBuilder();
		Character next;
		while ((next = reader.readNextCharacter()) != null) {
			result.append(next);
		}
		return result.toString();
	}

	@Test
	void constructorAndClose() throws Exception {
		assertThrows(Exception.class, () -> new BasicReader(null));
		final BasicReader reader = reader("abc");
		assertFalse(reader.readWasInitialized());
		assertNull(reader.getCurrentChar());
		assertEquals('a', reader.readNextCharacter());
		assertTrue(reader.readWasInitialized());
		assertEquals('a', reader.getCurrentChar());
		assertTrue(reader.getReadDataSize() > 0);
		reader.close();
		assertEquals(0, reader.getReadDataSize());
		assertThrows(IllegalStateException.class, reader::readNextCharacter);
		assertTrue(BasicReader.isBlank(" "));
		assertFalse(BasicReader.isBlank("x"));
		assertTrue(BasicReader.isNotEmpty(" "));
		assertFalse(BasicReader.isNotEmpty(""));
	}

	@Test
	void byteOrderMarks() throws Exception {
		assertEquals("a", readAll(reader(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a' }, StandardCharsets.UTF_8)));
		assertThrows(IOException.class, () -> readAll(reader(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a' }, StandardCharsets.ISO_8859_1)));
		assertEquals("ïxy", readAll(reader(new byte[] { (byte) 0xEF, 'x', 'y' }, StandardCharsets.ISO_8859_1)));
		assertEquals("ïx", readAll(reader(new byte[] { (byte) 0xEF, 'x' }, Charset.forName("windows-1252"))));
		assertEquals("ï", readAll(reader(new byte[] { (byte) 0xEF }, StandardCharsets.ISO_8859_1)));
		assertEquals("", readAll(reader(new byte[0], StandardCharsets.UTF_8)));
	}

	@Test
	void linebreakNormalization() throws Exception {
		final BasicReader reader = reader("a\r\nb\rc\nd\r");
		assertFalse(reader.isNormalizeLinebreaks());
		reader.setNormalizeLinebreaks(true);
		assertTrue(reader.isNormalizeLinebreaks());
		assertEquals("a\nb\nc\nd\n", readAll(reader));
		assertEquals(4, reader.getReadLines());
		assertEquals(9, reader.getReadCharacters());

		final BasicReader rawReader = reader("a\r\nb");
		assertEquals("a\r\nb", readAll(rawReader));
		assertEquals(1, rawReader.getReadLines());
	}

	@Test
	void normalizeUnreadCharacters() throws Exception {
		final BasicReader reader = reader("x\r\ny\rz\r");
		assertEquals("x\r\ny\rz\r", readAll(reader));
		reader.unread(7);
		reader.setNormalizeLinebreaks(true);
		// The trailing CR is normalized later together with its (missing) successor
		assertEquals("x\ny\nz\n", readAll(reader));

		// No raw CR among the unread characters
		final BasicReader noCrReader = reader("abc");
		noCrReader.readNextCharacter();
		noCrReader.readNextCharacter();
		noCrReader.unread(2);
		noCrReader.setNormalizeLinebreaks(true);
		assertEquals("abc", readAll(noCrReader));

		// Nothing unread at all
		final BasicReader emptyPushbackReader = reader("q");
		emptyPushbackReader.setNormalizeLinebreaks(true);
		assertEquals("q", readAll(emptyPushbackReader));
	}

	@Test
	void denormalizeUnreadCharacters() throws Exception {
		final BasicReader crlfReader = reader("\r\nq");
		crlfReader.setNormalizeLinebreaks(true);
		assertEquals("\nq", readAll(crlfReader));
		crlfReader.unread(2);
		crlfReader.setNormalizeLinebreaks(false);
		assertEquals("\r\nq", readAll(crlfReader));

		final BasicReader crReader = reader("\rq");
		crReader.setNormalizeLinebreaks(true);
		assertEquals("\nq", readAll(crReader));
		crReader.unread(2);
		crReader.setNormalizeLinebreaks(false);
		assertEquals("\rq", readAll(crReader));

		// No normalized characters among the unread characters
		final BasicReader plainReader = reader("ab");
		plainReader.readNextCharacter();
		plainReader.unread(1);
		plainReader.setNormalizeLinebreaks(false);
		assertEquals("ab", readAll(plainReader));
	}

	@Test
	void peekAndEof() throws Exception {
		final BasicReader reader = reader("ab");
		assertEquals('a', reader.peek());
		assertNull(reader.getCurrentChar());
		assertEquals('b', reader.peekAhead());
		assertEquals('a', reader.readNextCharacter());
		assertNull(reader.peekAhead());
		assertEquals('b', reader.readNextCharacter());
		assertNull(reader.peek());
		assertTrue(reader.isEOF());
		assertNull(reader.peekAhead());
	}

	@Test
	void whitespaceAndLines() throws Exception {
		assertEquals('x', reader("  \t x").readNextNonWhitespaceWithinLine());
		assertEquals('\n', reader(" \ny").readNextNonWhitespaceWithinLine());
		assertNull(reader("   ").readNextNonWhitespaceWithinLine());
		assertEquals('y', reader(" \n y").readNextNonWhitespace());

		final BasicReader lineReader = reader("l1\r\nl2\rl3\nl4");
		assertEquals("l1", lineReader.readNextLine());
		assertEquals("l2", lineReader.readNextLine());
		assertEquals("l3", lineReader.readNextLine());
		assertEquals("l4", lineReader.readNextLine());
		assertNull(lineReader.readNextLine());

		final BasicReader crOnlyReader = reader("\r");
		assertEquals("", crOnlyReader.readNextLine());
		assertNull(crOnlyReader.readNextLine());
	}

	@Test
	void indentationAndPosition() throws Exception {
		final BasicReader reader = reader("  \tx\n y");
		for (int i = 0; i < 4; i++) {
			reader.readNextCharacter();
		}
		assertEquals(3, reader.getCurrentIndentationLevel());
		assertEquals(4, reader.getReadCharactersInCurrentLine());
		reader.readNextCharacter();
		assertEquals(0, reader.getCurrentIndentationLevel());
		reader.readNextCharacter();
		reader.readNextCharacter();
		assertEquals(1, reader.getCurrentIndentationLevel());
		assertTrue(reader.getCurrentPositionText().contains("in line 2"));
	}

	@Test
	void reuseAndUnread() throws Exception {
		final BasicReader reader = reader("abc");
		reader.reuseCurrentChar();
		reader.readNextCharacter();
		reader.readNextCharacter();
		reader.reuse(new char[] { 'a', 'b' });
		assertEquals('a', reader.readNextCharacter());
		assertThrows(IllegalArgumentException.class, () -> reader.reuse(Character.valueOf('x')));
		assertThrows(IllegalStateException.class, () -> reader.reuse(new char[] { 'a', 'b', 'c' }));
		reader.reuse((Character) null);
		reader.reuse((char[]) null);
		reader.reuse(new char[0]);
		reader.reuse(Character.valueOf('a'));
		assertEquals('a', reader.readNextCharacter());
		reader.reuseCurrentChar();
		// Already unread, so a second call does nothing
		reader.reuseCurrentChar();
		assertEquals("abc", readAll(reader));
		assertThrows(IllegalStateException.class, () -> reader.unread(-1));
		assertThrows(IllegalStateException.class, () -> reader.unread(100));

		// The history keeps the most recent characters when it wraps around
		final StringBuilder longText = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			longText.append((char) ('a' + i % 26));
		}
		final BasicReader longReader = reader(longText.toString());
		assertEquals(longText.toString(), readAll(longReader));
		longReader.unread(BasicReader.MAX_UNREAD_CHARACTERS);
		assertEquals(longText.substring(100 - BasicReader.MAX_UNREAD_CHARACTERS), readAll(longReader));
	}

	@Test
	void readUpToNext() throws Exception {
		assertThrows(Exception.class, () -> reader("x").readUpToNext(false, null, ',', ','));
		assertThrows(Exception.class, () -> reader("x").readUpToNext(false, '\\', ',', '\\'));

		// An escaped limit character does not end the text
		final BasicReader reader = reader("ab\\/c/d");
		assertEquals("ab/c", reader.readUpToNext(false, '\\', '/'));
		assertEquals('/', reader.readNextCharacter());
		assertEquals("/d", reader.readUpToNext(true, null, '/'));

		assertThrows(Exception.class, () -> reader("ab\\").readUpToNext(false, '\\', ','));

		final BasicReader pendingReader = reader("xy,");
		pendingReader.readNextCharacter();
		pendingReader.reuseCurrentChar();
		assertEquals("xy,", pendingReader.readUpToNext(true, null, ','));
	}

	@Test
	void escapeSequences() throws Exception {
		assertEquals("\\\"/'\b\f\n\r\tää", reader("\\\\\\\"\\/\\'\\b\\f\\n\\r\\t\\u00E4\\u00e4,").readUpToNext(false, '\\', ','));
		assertThrows(Exception.class, () -> reader("\\u00g0").readUpToNext(false, '\\', ','));
		assertThrows(Exception.class, () -> reader("\\u00").readUpToNext(false, '\\', ','));
		assertThrows(Exception.class, () -> reader("\\x").readUpToNext(false, '\\', ','));
		// Another escape character than backslash escapes itself
		assertEquals("a^b", reader("a^^b").readUpToNext(false, '^', ','));
	}

	@Test
	void readUpToNextString() throws Exception {
		assertThrows(Exception.class, () -> reader("x").readUpToNextString(false, null, (String[]) null));
		assertThrows(Exception.class, () -> reader("x").readUpToNextString(false, null));
		assertThrows(Exception.class, () -> reader("x").readUpToNextString(false, null, ""));
		assertThrows(Exception.class, () -> reader("x").readUpToNextString(false, null, (String) null));
		assertThrows(Exception.class, () -> reader("x").readUpToNextString(false, null, "x".repeat(BasicReader.MAX_UNREAD_CHARACTERS + 1)));
		assertThrows(Exception.class, () -> reader("x").readUpToNextString(false, '\\', "a\\b"));
		assertThrows(Exception.class, () -> reader("x").readUpToNextString(false, null, "ab", "ab"));

		final BasicReader excludingReader = reader("abc-->def");
		assertEquals("abc", excludingReader.readUpToNextString(false, null, "-->", "!!"));
		assertEquals('-', excludingReader.readNextCharacter());

		assertEquals("abc-->", reader("abc-->def").readUpToNextString(true, null, "-->"));
		assertEquals("abc", reader("abc").readUpToNextString(true, null, "-->"));
		// Escaped characters never form part of a limit string
		assertEquals("a/xb", reader("a\\/xb/x").readUpToNextString(false, '\\', "/x"));
		assertThrows(Exception.class, () -> reader("a\\").readUpToNextString(false, '\\', "/x"));
	}

	@Test
	void readQuotedText() throws Exception {
		assertThrows(Exception.class, () -> reader("\"x\"").readQuotedText('\\'));

		final BasicReader wrongStart = reader("x");
		wrongStart.readNextCharacter();
		assertThrows(Exception.class, () -> wrongStart.readQuotedText('"', '\\'));

		final BasicReader escaped = reader("\"a\\\"b\\u00e4\\n\" rest");
		escaped.readNextCharacter();
		assertEquals("a\"bä\n", escaped.readQuotedText('\\'));
		assertEquals('"', escaped.getCurrentChar());

		final BasicReader pending = reader("\"q\"");
		pending.readNextCharacter();
		pending.reuseCurrentChar();
		assertEquals("q", pending.readQuotedText('"', '\\'));

		final BasicReader missingClose = reader("\"abc");
		missingClose.readNextCharacter();
		assertThrows(Exception.class, () -> missingClose.readQuotedText('\\'));

		final BasicReader doubled = reader("'it''s' x");
		doubled.readNextCharacter();
		assertEquals("it's", doubled.readQuotedText('\''));

		final BasicReader doubledMissingClose = reader("'abc");
		doubledMissingClose.readNextCharacter();
		assertThrows(Exception.class, () -> doubledMissingClose.readQuotedText('\''));

		// Line breaks within quotes are kept, normalization applies again afterwards
		final BasicReader normalized = reader("\"a\r\nb\"\r\n");
		normalized.setNormalizeLinebreaks(true);
		normalized.readNextCharacter();
		assertEquals("a\r\nb", normalized.readQuotedText('\\'));
		assertEquals('\n', normalized.readNextCharacter());
		assertNull(normalized.readNextCharacter());
	}
}
