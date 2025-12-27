package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

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
			Assert.assertEquals(1, getCurrentLine());
			Assert.assertEquals(1, getCurrentColumn());

			final String testText1 = readUpToNext(false, null, "\r\n".toCharArray());
			Assert.assertEquals("Test line 1", testText1);

			Assert.assertEquals(1, getCurrentLine());
			Assert.assertEquals(12, getCurrentColumn());

			final char nextChar1 = readChar();
			Assert.assertEquals('\n', nextChar1);

			Assert.assertEquals(2, getCurrentLine());
			Assert.assertEquals(1, getCurrentColumn());

			final String testText2 = readUpToNext(false, null, "\r\n".toCharArray());
			Assert.assertEquals("Test line _ 2", testText2);

			Assert.assertEquals(2, getCurrentLine());
			Assert.assertEquals(14, getCurrentColumn());

			final char nextChar2 = readChar();
			Assert.assertEquals('\n', nextChar2);

			Assert.assertEquals(3, getCurrentLine());
			Assert.assertEquals(1, getCurrentColumn());

			final String testText3 = readUpToNext(false, null, "\r\n".toCharArray());
			Assert.assertEquals("Testline3", testText3);

			Assert.assertEquals(3, getCurrentLine());
			Assert.assertEquals(10, getCurrentColumn());

			final char nextChar3 = readChar();
			Assert.assertEquals('\n', nextChar3);

			Assert.assertEquals(4, getCurrentLine());
			Assert.assertEquals(1, getCurrentColumn());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
