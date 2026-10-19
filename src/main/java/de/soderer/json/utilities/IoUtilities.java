package de.soderer.json.utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Helper methods for streams.
 */
public class IoUtilities {
	/** Value returned by read operations at the end of data. */
	public static final int EOF = -1;

	/**
	 * Utility class
	 */
	private IoUtilities() {
		// Utility class
	}

	/**
	 * Reads all remaining data of a stream and decodes it as text.
	 *
	 * @param inputStream
	 *            the stream to read, may be null
	 * @param encoding
	 *            the encoding of the data
	 * @return the decoded text, or null if the stream is null (like {@link #toByteArray(InputStream)})
	 * @throws IOException
	 *             if reading fails
	 */
	public static String toString(final InputStream inputStream, final Charset encoding) throws IOException {
		final byte[] data = toByteArray(inputStream);
		return data == null ? null : new String(data, encoding);
	}

	/**
	 * Reads all remaining data of a stream.
	 *
	 * @param inputStream
	 *            the stream to read, may be null
	 * @return the data read, or null if the stream is null
	 * @throws IOException
	 *             if reading fails
	 */
	public static byte[] toByteArray(final InputStream inputStream) throws IOException {
		if (inputStream == null) {
			return null;
		} else {
			try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
				copy(inputStream, byteArrayOutputStream);
				return byteArrayOutputStream.toByteArray();
			}
		}
	}

	/**
	 * Copies all remaining data of a stream to another stream and flushes the output stream.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @param outputStream
	 *            the stream to write to
	 * @return the number of bytes copied
	 * @throws IOException
	 *             if reading or writing fails
	 */
	public static long copy(final InputStream inputStream, final OutputStream outputStream) throws IOException {
		final byte[] buffer = new byte[4096];
		int lengthRead;
		long bytesCopied = 0;
		while ((lengthRead = inputStream.read(buffer)) != EOF) {
			outputStream.write(buffer, 0, lengthRead);
			bytesCopied += lengthRead;
		}
		outputStream.flush();
		return bytesCopied;
	}
}
