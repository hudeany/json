package de.soderer.json;

import java.util.Objects;

public class JsonValueNull extends JsonNode {
	public JsonValueNull() {
		super(JsonDataType.NULL);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(null);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (getClass() == obj.getClass()) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return "null";
	}
}
