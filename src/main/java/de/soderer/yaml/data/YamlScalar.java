package de.soderer.yaml.data;

import java.util.Objects;

/**
 * Repräsentiert einen YAML-Skalarwert.
 * Unterstützt:
 *  - STRING
 *  - NUMBER
 *  - BOOLEAN
 *  - NULL_VALUE
 *  - MULTILINE_LITERAL (|)
 *  - MULTILINE_FOLDED  (>)
 */
public class YamlScalar extends YamlNode {

	private final String value;
	private final YamlScalarType type;

	public YamlScalar(String value, YamlScalarType type) {
		this.value = value;
		this.type = Objects.requireNonNull(type, "ScalarType must not be null");
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
