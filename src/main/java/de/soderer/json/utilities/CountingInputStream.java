package de.soderer.json.utilities;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream wrapper counting the number of bytes read or skipped from the underlying stream.
 * <p>
 * The count follows {@link #mark(int)} and {@link #reset()}, so bytes read again after a reset are
 * not counted twice. All counter operations are thread safe.
 * </p>
 */
public class CountingInputStream extends FilterInputStream {
	/** Value returned by read operations at the end of data. */
	private static final int EOF = -1;

	/** Number of bytes read or skipped so far. */
	private long count = 0;

	/** Byte count at the time of the last call of {@link #mark(int)}. */
	private long markedCount = 0;

	/**
	 * Creates a new counting stream wrapping the given input stream.
	 *
	 * @param in
	 *            the underlying input stream
	 */
	public CountingInputStream(final InputStream in) {
		super(in);
	}

	/**
	 * Adds the number of bytes read by a read operation to the byte count. Subclasses may override
	 * this to get notified about read operations.
	 *
	 * @param n
	 *            the number of bytes read, or {@value #EOF} at the end of data, which is ignored
	 */
	protected synchronized void afterRead(final int n) {
		if (n != EOF) {
			count += n;
		}
	}

	/**
	 * Returns the number of bytes read or skipped so far as int.
	 *
	 * @return the byte count
	 * @throws ArithmeticException
	 *             if the byte count exceeds {@link Integer#MAX_VALUE}, use {@link #getByteCount()}
	 *             instead
	 */
	public int getCount() {
		return toIntCount(getByteCount());
	}

	/**
	 * Returns the number of bytes read or skipped so far as int and resets the byte count to 0.
	 *
	 * @return the byte count before the reset
	 * @throws ArithmeticException
	 *             if the byte count exceeds {@link Integer#MAX_VALUE}, use
	 *             {@link #resetByteCount()} instead. In this case the byte count is not reset.
	 */
	public synchronized int resetCount() {
		final int result = toIntCount(count);
		count = 0;
		markedCount = 0;
		return result;
	}

	/**
	 * Returns the number of bytes read or skipped so far.
	 *
	 * @return the byte count
	 */
	public synchronized long getByteCount() {
		return count;
	}

	/**
	 * Returns the number of bytes read or skipped so far and resets the byte count to 0.
	 *
	 * @return the byte count before the reset
	 */
	public synchronized long resetByteCount() {
		final long result = count;
		count = 0;
		markedCount = 0;
		return result;
	}

	@Override
	public int read() throws IOException {
		final int b = in.read();
		afterRead(b != EOF ? 1 : EOF);
		return b;
	}

	@Override
	public int read(final byte[] bts) throws IOException {
		final int n = in.read(bts);
		afterRead(n);
		return n;
	}

	@Override
	public int read(final byte[] bts, final int off, final int len) throws IOException {
		final int n = in.read(bts, off, len);
		afterRead(n);
		return n;
	}

	@Override
	public long skip(final long ln) throws IOException {
		final long skipped = in.skip(ln);
		if (skipped > 0) {
			synchronized (this) {
				count += skipped;
			}
		}
		return skipped;
	}

	@Override
	public synchronized void mark(final int readlimit) {
		in.mark(readlimit);
		markedCount = count;
	}

	@Override
	public synchronized void reset() throws IOException {
		in.reset();
		count = markedCount;
	}

	/**
	 * Converts a byte count to int.
	 *
	 * @param byteCount
	 *            the byte count
	 * @return the byte count as int
	 * @throws ArithmeticException
	 *             if the byte count exceeds {@link Integer#MAX_VALUE}
	 */
	private static int toIntCount(final long byteCount) {
		if (byteCount > Integer.MAX_VALUE) {
			throw new ArithmeticException("The byte count " + byteCount + " is too large to be converted to an int");
		}
		return (int) byteCount;
	}
}
