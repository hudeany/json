package de.soderer.yaml.data;

import java.util.Objects;

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

	@Override
	public int hashCode() {
		return Objects.hash(targetAnchorName);
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
			final YamlAlias other = (YamlAlias) otherObject;
			return Objects.equals(targetAnchorName, other.targetAnchorName);
		}
	}
}
