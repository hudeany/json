package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.soderer.json.utilities.BasicReadAheadReader;

public class BasicReadAheadReaderTest extends BasicReadAheadReader {
	public BasicReadAheadReaderTest() throws Exception {
		super(getTestInputStream(), StandardCharsets.UTF_8);
		setNormalizeLinebreaks(true);
	}

	private static InputStream getTestInputStream() {
		final String dataString = "Test line 1\r\nTest line _ 2\nTestline3\r";
		return new ByteArrayInputStream(dataString.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void test() {
		try {
			Assertions.assertEquals(1, getCurrentLine());
			Assertions.assertEquals(1, getCurrentColumn());
			Assertions.assertTrue(peekCharMatch('T'));
			Assertions.assertTrue(peekNextCharMatch(1, 'e'));
			Assertions.assertTrue(peekNextCharMatch(2, 's'));

			final String testText1 = readUpToNext(false, null, "\r\n".toCharArray());
			Assertions.assertEquals("Test line 1", testText1);

			Assertions.assertEquals(1, getCurrentLine());
			Assertions.assertEquals(12, getCurrentColumn());

			final char nextChar1 = readChar();
			Assertions.assertEquals('\n', nextChar1);

			Assertions.assertEquals(2, getCurrentLine());
			Assertions.assertEquals(1, getCurrentColumn());

			final String testText2 = readUpToNext(false, null, "\r\n".toCharArray());
			Assertions.assertEquals("Test line _ 2", testText2);

			Assertions.assertEquals(2, getCurrentLine());
			Assertions.assertEquals(14, getCurrentColumn());

			final char nextChar2 = readChar();
			Assertions.assertEquals('\n', nextChar2);
			Assertions.assertTrue(peekCharMatch('T'));
			Assertions.assertTrue(peekNextCharMatch(1, 'e'));
			Assertions.assertTrue(peekNextCharMatch(2, 's'));

			Assertions.assertEquals(3, getCurrentLine());
			Assertions.assertEquals(1, getCurrentColumn());

			final String testText3 = readUpToNext(false, null, "\r\n".toCharArray());
			Assertions.assertEquals("Testline3", testText3);
			Assertions.assertTrue(peekCharMatch('\n'));
			Assertions.assertNull(peekNextChar(1));
			Assertions.assertNull(peekNextChar(2));

			Assertions.assertEquals(3, getCurrentLine());
			Assertions.assertEquals(10, getCurrentColumn());

			final char nextChar3 = readChar();
			Assertions.assertEquals('\n', nextChar3);

			Assertions.assertEquals(4, getCurrentLine());
			Assertions.assertEquals(1, getCurrentColumn());
		} catch (final Exception e) {
			Assertions.fail(e.getMessage());
		}
	}
}
