package de.soderer.yaml.data;

/**
 * Ein einzelnes Key-Value-Paar in einem YAML-Mapping.
 * Key und Value sind jeweils vollständige YamlNode-Objekte,
 * sodass auch komplexe Keys unterstützt werden.
 */
public class YamlKeyValue {

	private final YamlNode key;
	private final YamlNode value;

	public YamlKeyValue(final YamlNode key, final YamlNode value) {
		if (key == null) {
			throw new IllegalArgumentException("Key darf nicht null sein");
		}
		if (value == null) {
			throw new IllegalArgumentException("Value darf nicht null sein");
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
