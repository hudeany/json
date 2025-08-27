package de.soderer.yaml;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class YamlSimpleValue extends YamlNode {
	private YamlDataType explicitDataType;

	@Override
	public Set<String> getAllAvailableAnchorIds() {
		final Set<String> anchorIds = new HashSet<>();
		if (getAnchor() != null) {
			anchorIds.add(getAnchor());
		}
		return anchorIds;
	}

	@Override
	public YamlSimpleValue setValue(final Object value) {
		super.setValue(value);
		return this;
	}

	@Override
	public Set<String> getAllReferencedAnchorIds() {
		return new HashSet<>();
	}

	@Override
	public YamlSimpleValue setComment(final String comment) {
		super.setComment(comment);
		return this;
	}

	@Override
	public YamlSimpleValue setInlineComment(final String inlineComment) {
		super.setInlineComment(inlineComment);
		return this;
	}

	@Override
	public YamlSimpleValue setAnchor(final String anchor) throws Exception {
		// TODO
		if ("id005".equals(anchor)) {
			System.out.println("Anchor found");
		}

		super.setAnchor(anchor);
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(anchor, style, value, comment, inlineComment);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (getClass() != obj.getClass()) {
			return false;
		} else {
			final YamlSimpleValue other = (YamlSimpleValue) obj;
			return Objects.equals(anchor, other.anchor)
					&& Objects.equals(style, other.style)
					&& Objects.equals(value, other.value)
					&& Objects.equals(comment, other.comment)
					&& Objects.equals(inlineComment, other.inlineComment);
		}
	}

	public YamlSimpleValue setExplicitDataType(final YamlDataType explicitDataType) {
		this.explicitDataType = explicitDataType;
		return this;
	}

	// TODO: use this information in YamlWriter
	public YamlDataType getExplicitDataType() {
		return explicitDataType;
	}
}
