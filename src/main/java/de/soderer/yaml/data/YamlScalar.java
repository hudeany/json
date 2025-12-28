package de.soderer.yaml.data;

import java.util.Objects;

public class YamlScalar extends YamlNode {
	private final Object value;
	private final YamlScalarType type;

	public YamlScalar(final Object value, final YamlScalarType type) {
		if (type == null) {
			throw new IllegalArgumentException("ScalarType must not be null");
		}

		this.value = value;
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public YamlScalarType getType() {
		return type;
	}

	@Override
	public String toString() {
		if (value == null) {
			return "null";
		} else {
			return value.toString();
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, value);
	}

	@Override
	public boolean equals(final Object otherObject) {
		if (this == otherObject) {
			return true;
		} else if (otherObject == null) {
			return false;
		} else if (getClass() != otherObject.getClass()) {
			return false;
		} else {
			final YamlScalar other = (YamlScalar) otherObject;
			return type == other.type && Objects.equals(value, other.value);
		}
	}
}
