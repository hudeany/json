package de.soderer.yaml.data;

/**
 * Repräsentiert einen YAML-Alias (*id).
 * Ein Alias verweist auf einen zuvor definierten Anchor (&id).
 */
public class YamlAlias extends YamlNode {

	private final String targetAnchorName;

	public YamlAlias(final String targetAnchorName) {
		if (targetAnchorName == null || targetAnchorName.isEmpty()) {
			throw new IllegalArgumentException("Alias-Name darf nicht leer sein");
		}
		this.targetAnchorName = targetAnchorName;
	}

	public String getTargetAnchorName() {
		return targetAnchorName;
	}

	@Override
	public String toString() {
		return "YamlAlias{*" + targetAnchorName + "}";
	}
}
