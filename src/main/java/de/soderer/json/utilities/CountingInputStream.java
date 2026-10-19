package de.soderer.json.utilities;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream wrapper counting the number of bytes read or skipped from the underlying stream.
 * <p>
 * The byte count is the position within the underlying stream relative to the position at
 * creation or at the last reset of the count ({@link #resetByteCount()}, {@link #resetCount()}).
 * It follows {@link #mark(int)}, {@link #reset()} and backward skips (like
 * {@link java.io.FileInputStream#skip(long)} with a negative value), so bytes read again are not
 * counted twice. After moving back before the start of counting, the count is negative until that
 * position is reached again.
 * </p>
 * <p>
 * All counter operations are thread safe.
 * </p>
 */
public class CountingInputStream extends FilterInputStream {
	/** Value returned by read operations at the end of data. */
	private static final int EOF = -1;

	/** Number of bytes read or skipped so far, relative to the start of counting. */
	private long count = 0;

	/** Byte count at the time of the last call of {@link #mark(int)}, relative to the start of counting. */
	private long markedCount = 0;

	/** True after {@link #mark(int)} was called. */
	private boolean markSet = false;

	/**
	 * Creates a new counting stream wrapping the given input stream.
	 *
	 * @param in
	 *            the underlying input stream, must not be null
	 * @throws IllegalArgumentException
	 *             if the input stream is null
	 */
	public CountingInputStream(final InputStream in) {
		super(requireInputStream(in));
	}

	/**
	 * Checks the input stream before it is passed to the super constructor.
	 *
	 * @param inputStream
	 *            the stream to check
	 * @return the stream
	 * @throws IllegalArgumentException
	 *             if the stream is null
	 */
	private static InputStream requireInputStream(final InputStream inputStream) {
		if (inputStream == null) {
			throw new IllegalArgumentException("Invalid empty inputStream");
		}
		return inputStream;
	}

	/**
	 * Adds the number of bytes read by a read operation to the byte count. Subclasses may override
	 * this to get notified about read operations, but must call this implementation.
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
	 * Adds the number of bytes skipped by a skip operation to the byte count. Subclasses may
	 * override this to get notified about skip operations, but must call this implementation.
	 *
	 * @param n
	 *            the number of bytes skipped, negative for a backward skip
	 */
	protected synchronized void afterSkip(final long n) {
		count += n;
	}

	/**
	 * Returns the number of bytes read or skipped so far as int.
	 *
	 * @return the byte count
	 * @throws ArithmeticException
	 *             if the byte count does not fit into an int, use {@link #getByteCount()} instead
	 */
	public int getCount() {
		return toIntCount(getByteCount());
	}

	/**
	 * Returns the number of bytes read or skipped so far as int and resets the byte count to 0.
	 *
	 * @return the byte count before the reset
	 * @throws ArithmeticException
	 *             if the byte count does not fit into an int, use {@link #resetByteCount()}
	 *             instead. In this case the byte count is not reset.
	 */
	public synchronized int resetCount() {
		final int result = toIntCount(count);
		resetByteCount();
		return result;
	}

	/**
	 * Returns the number of bytes read or skipped so far.
	 *
	 * @return the byte count, negative only after moving back before the start of counting
	 */
	public synchronized long getByteCount() {
		return count;
	}

	/**
	 * Returns the number of bytes read or skipped so far and resets the byte count to 0. A marked
	 * position stays valid, a later {@link #reset()} to it is counted relative to the new start.
	 *
	 * @return the byte count before the reset
	 */
	public synchronized long resetByteCount() {
		final long result = count;
		// Keep the mark at the same stream position, now relative to the new start of counting
		markedCount -= result;
		count = 0;
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

	/**
	 * Skips bytes of the underlying stream and counts them. A negative return value of the
	 * underlying stream (backward skip) decreases the byte count.
	 */
	@Override
	public long skip(final long ln) throws IOException {
		final long skipped = in.skip(ln);
		if (skipped != 0) {
			afterSkip(skipped);
		}
		return skipped;
	}

	@Override
	public synchronized void mark(final int readlimit) {
		in.mark(readlimit);
		markedCount = count;
		markSet = true;
	}

	/**
	 * Resets the underlying stream to its mark and restores the byte count of that position. If no
	 * mark was set and the underlying stream resets anyway (like {@link java.io.ByteArrayInputStream}
	 * to its start), the byte count is set to 0, assuming the underlying stream was at that position
	 * when counting started.
	 */
	@Override
	public synchronized void reset() throws IOException {
		in.reset();
		count = markSet ? markedCount : 0;
	}

	/**
	 * Converts a byte count to int.
	 *
	 * @param byteCount
	 *            the byte count
	 * @return the byte count as int
	 * @throws ArithmeticException
	 *             if the byte count does not fit into an int
	 */
	private static int toIntCount(final long byteCount) {
		if (byteCount > Integer.MAX_VALUE || byteCount < Integer.MIN_VALUE) {
			throw new ArithmeticException("The byte count " + byteCount + " is too large to be converted to an int");
		}
		return (int) byteCount;
	}
}
