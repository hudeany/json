package de.soderer.yaml.data;

public class YamlKeyValue {

	private final YamlNode key;
	private final YamlNode value;

	public YamlKeyValue(final YamlNode key, final YamlNode value) {
		if (key == null) {
			throw new IllegalArgumentException("Key must not be null");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value must not be null");
		}
		this.key = key;
		this.value = value;
	}

	public YamlNode getKey() {
		return key;
	}

	public YamlNode getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "YamlKeyValue{" +
				"key=" + key +
				", value=" + value +
				'}';
	}
}
