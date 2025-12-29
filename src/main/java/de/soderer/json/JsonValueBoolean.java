package de.soderer.json;

import java.util.Objects;

public class JsonValueBoolean extends JsonNode {
	private final Boolean value;

	public JsonValueBoolean(final Boolean value) {
		super(JsonDataType.BOOLEAN);
		this.value = value;

		if (value == null) {
			throw new RuntimeException("Invalid 'null' value for " + getClass().getSimpleName() + ". Use JsonValueNull instead.");
		}
	}

	public Boolean getValue() {
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
		final JsonValueBoolean other = (JsonValueBoolean) obj;
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
