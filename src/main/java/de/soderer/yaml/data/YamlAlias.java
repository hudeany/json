package de.soderer.yaml.data;

import java.util.Objects;

/**
 * Alias ("*name") referring to the node with the anchor of the same name. Aliases are kept as
 * they are when reading; {@link de.soderer.yaml.YamlToJsonConverter} resolves them.
 */
public class YamlAlias extends YamlNode {
	/**
	 * Name of the referenced anchor.
	 */
	private final String targetAnchorName;

	/**
	 * Creates an alias.
	 *
	 * @param targetAnchorName
	 *            name of the referenced anchor without "*"
	 * @throws IllegalArgumentException
	 *             if the name is null or empty
	 */
	public YamlAlias(final String targetAnchorName) {
		if (targetAnchorName == null || targetAnchorName.isEmpty()) {
			throw new IllegalArgumentException("Alias name must not be empty");
		}
		this.targetAnchorName = targetAnchorName;
	}

	/**
	 * Returns the name of the referenced anchor.
	 *
	 * @return the anchor name without "*"
	 */
	public String getTargetAnchorName() {
		return targetAnchorName;
	}

	/**
	 * Returns a debug representation like "YamlAlias{*name}".
	 */
	@Override
	public String toString() {
		return "YamlAlias{*" + targetAnchorName + "}";
	}

	@Override
	public int hashCode() {
		return Objects.hash(targetAnchorName);
	}

	/**
	 * Two aliases are equal, if they refer to the same anchor name.
	 */
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
