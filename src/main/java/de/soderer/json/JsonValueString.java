package de.soderer.json;

import java.util.Objects;

/**
 * JSON string value.
 */
public class JsonValueString extends JsonNode {
	private final String value;

	/**
	 * Creates a JSON string value.
	 *
	 * @param value
	 *            the value, must not be null (use {@link JsonValueNull} instead)
	 * @throws RuntimeException
	 *             if the value is null
	 */
	public JsonValueString(final String value) {
		super(JsonDataType.STRING);
		this.value = value;

		if (value == null) {
			throw new RuntimeException("Invalid 'null' value for " + getClass().getSimpleName() + ". Use JsonValueNull instead.");
		}
	}

	/**
	 * Returns the value.
	 *
	 * @return the value, never null
	 */
	public String getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final JsonValueString other = (JsonValueString) obj;
		return Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		if (value == null) {
			return "null";
		} else {
			return value;
		}
	}
}
