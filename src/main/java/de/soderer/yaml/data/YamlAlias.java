package de.soderer.yaml.data;

public class YamlAlias extends YamlNode {
	private final String targetAnchorName;

	public YamlAlias(final String targetAnchorName) {
		if (targetAnchorName == null || targetAnchorName.isEmpty()) {
			throw new IllegalArgumentException("Alias name must not be empty");
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
