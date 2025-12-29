package de.soderer.json;

import java.math.BigDecimal;
import java.util.Objects;

import de.soderer.json.utilities.NumberUtilities;

public class JsonValueInteger extends JsonNode {
	private final Number value;

	public JsonValueInteger(final Integer value) {
		super(JsonDataType.INTEGER);

		if (value == null) {
			throw new RuntimeException("Invalid 'null' value for " + getClass().getSimpleName() + ". Use JsonValueNull instead.");
		} else {
			this.value = value;
		}
	}

	public JsonValueInteger(final Long value) {
		super(JsonDataType.INTEGER);

		if (value == null) {
			throw new RuntimeException("Invalid 'null' value for " + getClass().getSimpleName() + ". Use JsonValueNull instead.");
		} else {
			this.value = value;
		}
	}

	public JsonValueInteger(final BigDecimal value) {
		super(JsonDataType.INTEGER);

		if (value == null) {
			throw new RuntimeException("Invalid 'null' value for " + getClass().getSimpleName() + ". Use JsonValueNull instead.");
		} else if (!NumberUtilities.isInteger(value)) {
			throw new RuntimeException("Invalid non integer value '" + value.toString() + "' for " + getClass().getSimpleName() + ". Use JsonValueFloat instead.");
		} else {
			this.value = value;
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
		final JsonValueInteger other = (JsonValueInteger) obj;
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
