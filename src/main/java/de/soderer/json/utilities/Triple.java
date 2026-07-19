package de.soderer.json.utilities;

public class Triple<T1, T2, T3> {
	private T1 value1;
	private T2 value2;
	private T3 value3;

	public Triple() {
		this.value1 = null;
		this.value2 = null;
		this.value3 = null;
	}

	public Triple(T1 value1, T2 value2, T3 value3) {
		this.value1 = value1;
		this.value2 = value2;
		this.value3 = value3;
	}

	public T1 getFirst() {
		return value1;
	}

	public T2 getSecond() {
		return value2;
	}

	public T3 getThird() {
		return value3;
	}

	public void setFirst(T1 value1) {
		this.value1 = value1;
	}

	public Triple<T1, T2, T3> withFirst(final T1 newValue1) {
		setFirst(newValue1);
		return this;
	}

	public void setSecond(T2 value2) {
		this.value2 = value2;
	}

	public Triple<T1, T2, T3> withSecond(final T2 newValue2) {
		setSecond(newValue2);
		return this;
	}

	public void setThird(T3 value3) {
		this.value3 = value3;
	}

	public Triple<T1, T2, T3> withThird(final T3 newValue3) {
		setThird(newValue3);
		return this;
	}

	@Override
	public String toString() {
		StringBuilder returnString = new StringBuilder("<");

		if (value1 != null) {
			returnString.append(value1.toString());
		} else {
			returnString.append("<null>");
		}

		returnString.append(", ");

		if (value2 != null) {
			returnString.append(value2.toString());
		} else {
			returnString.append("<null>");
		}

		returnString.append(", ");

		if (value3 != null) {
			returnString.append(value3.toString());
		} else {
			returnString.append("<null>");
		}

		returnString.append(">");
		return returnString.toString();
	}
}
