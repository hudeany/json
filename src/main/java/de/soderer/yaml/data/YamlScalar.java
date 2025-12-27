package de.soderer.yaml.data;

public class YamlScalar extends YamlNode {
	private final String value;
	private final YamlScalarType type;

	public YamlScalar(final String value, final YamlScalarType type) {
		if (type == null) {
			throw new IllegalArgumentException("ScalarType must not be null");
		}

		this.value = value;
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public YamlScalarType getType() {
		return type;
	}

	@Override
	public String toString() {
		return "YamlScalar{" +
				"type=" + type +
				", value='" + value + '\'' +
				'}';
	}
}
