package de.soderer.json;

import java.util.Objects;

public class JsonValueNumber extends JsonNode {
	private final Number value;

	public JsonValueNumber(final Number value) {
		super(JsonDataType.NUMBER);
		this.value = value;

		if (value == null) {
			throw new RuntimeException("Invalid 'null' value for " + getClass().getSimpleName() + ". Use JsonValueNull instead.");
		}
	}

	public Number getValue() {
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
		final JsonValueNumber other = (JsonValueNumber) obj;
		return Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		if (value == null) {
			return "null";
		} else {
			return value.toString();
		}
	}
}
