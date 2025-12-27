package de.soderer.yaml.data;

import java.util.Map.Entry;

public class YamlKeyValue implements Entry<YamlNode, YamlNode> {
	private final YamlNode key;
	private YamlNode value;

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

	@Override
	public YamlNode getKey() {
		return key;
	}

	@Override
	public YamlNode getValue() {
		return value;
	}

	@Override
	public YamlNode setValue(final YamlNode value) {
		final YamlNode oldValue = this.value;
		this.value = value;
		return oldValue;
	}

	@Override
	public String toString() {
		return "YamlKeyValue{" +
				"key=" + key +
				", value=" + value +
				'}';
	}
}
