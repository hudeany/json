package de.soderer.json.utilities;

/**
 * Mutable container for three values of any type.
 *
 * @param <T1>
 *            type of the first value
 * @param <T2>
 *            type of the second value
 * @param <T3>
 *            type of the third value
 */
public class Triple<T1, T2, T3> {
	/** The first value. */
	private T1 value1;

	/** The second value. */
	private T2 value2;

	/** The third value. */
	private T3 value3;

	/**
	 * Creates a new triple with all values null.
	 */
	public Triple() {
		this(null, null, null);
	}

	/**
	 * Creates a new triple.
	 *
	 * @param value1
	 *            the first value
	 * @param value2
	 *            the second value
	 * @param value3
	 *            the third value
	 */
	public Triple(final T1 value1, final T2 value2, final T3 value3) {
		this.value1 = value1;
		this.value2 = value2;
		this.value3 = value3;
	}

	/**
	 * Returns the first value.
	 *
	 * @return the first value
	 */
	public T1 getFirst() {
		return value1;
	}

	/**
	 * Returns the second value.
	 *
	 * @return the second value
	 */
	public T2 getSecond() {
		return value2;
	}

	/**
	 * Returns the third value.
	 *
	 * @return the third value
	 */
	public T3 getThird() {
		return value3;
	}

	/**
	 * Sets the first value.
	 *
	 * @param value1
	 *            the first value
	 */
	public void setFirst(final T1 value1) {
		this.value1 = value1;
	}

	/**
	 * Sets the first value.
	 *
	 * @param newValue1
	 *            the first value
	 * @return this triple for chaining
	 */
	public Triple<T1, T2, T3> withFirst(final T1 newValue1) {
		setFirst(newValue1);
		return this;
	}

	/**
	 * Sets the second value.
	 *
	 * @param value2
	 *            the second value
	 */
	public void setSecond(final T2 value2) {
		this.value2 = value2;
	}

	/**
	 * Sets the second value.
	 *
	 * @param newValue2
	 *            the second value
	 * @return this triple for chaining
	 */
	public Triple<T1, T2, T3> withSecond(final T2 newValue2) {
		setSecond(newValue2);
		return this;
	}

	/**
	 * Sets the third value.
	 *
	 * @param value3
	 *            the third value
	 */
	public void setThird(final T3 value3) {
		this.value3 = value3;
	}

	/**
	 * Sets the third value.
	 *
	 * @param newValue3
	 *            the third value
	 * @return this triple for chaining
	 */
	public Triple<T1, T2, T3> withThird(final T3 newValue3) {
		setThird(newValue3);
		return this;
	}

	/**
	 * Returns the values like "&lt;value1, value2, value3&gt;", null values as "&lt;null&gt;".
	 */
	@Override
	public String toString() {
		return "<" + (value1 == null ? "<null>" : value1.toString())
				+ ", " + (value2 == null ? "<null>" : value2.toString())
				+ ", " + (value3 == null ? "<null>" : value3.toString())
				+ ">";
	}
}
