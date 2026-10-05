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
	/**
	 * Utility class, not to be instantiated.
	 */
	private IoUtilities() {
	}

	/**
	 * Reads a stream completely into a string. The stream is not closed.
	 *
	 * @param inputStream
	 *            the stream to read, may be null
	 * @param encoding
	 *            the encoding of the stream data
	 * @return the stream content, or null if the stream is null
	 * @throws IOException
	 *             if reading fails
	 */
	public static String toString(final InputStream inputStream, final Charset encoding) throws IOException {
		final byte[] data = toByteArray(inputStream);
		return data == null ? null : new String(data, encoding);
	}

	/**
	 * Reads a stream completely into a byte array. The stream is not closed.
	 *
	 * @param inputStream
	 *            the stream to read, may be null
	 * @return the stream content, or null if the stream is null
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
	 * Copies all remaining data of a stream to another stream and flushes the output stream. No
	 * stream is closed.
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
		while ((lengthRead = inputStream.read(buffer)) > -1) {
			outputStream.write(buffer, 0, lengthRead);
			bytesCopied += lengthRead;
		}
		outputStream.flush();
		return bytesCopied;
	}
}
