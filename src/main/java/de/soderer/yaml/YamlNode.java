package de.soderer.yaml;

import java.util.Map.Entry;
import java.util.Set;

public abstract class YamlNode extends YamlValue {
	protected String anchor;
	protected YamlStyle style = null;

	public String getAnchor() {
		return anchor;
	}

	public YamlNode setAnchor(final String anchor) throws Exception {
		if (anchor.matches("\\s+")) {
			throw new Exception("Yaml anchor name contains invalid whitespace");
		} else {
			this.anchor = anchor;
			return this;
		}
	}

	public YamlStyle getStyle() {
		return style;
	}

	public YamlNode setStyle(final YamlStyle style) {
		this.style = style;
		return this;
	}

	public boolean hasComments() {
		if (getComment() != null) {
			return true;
		} else if (getInlineComment() != null) {
			return true;
		} else {
			return hasChildComments();
		}
	}

	public boolean hasChildComments() {
		if (this instanceof YamlMapping) {
			for (final Entry<YamlNode, YamlNode> entry : ((YamlMapping) this).entrySet()) {
				if (entry.getKey().hasComments()) {
					return true;
				} else if (entry.getValue().hasComments()) {
					return true;
				}
			}
			return false;
		} else if (this instanceof YamlSequence) {
			for (final YamlNode item : (YamlSequence) this) {
				if (item.hasComments()) {
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	public abstract Set<String> getAllAvailableAnchorIds();

	public abstract Set<String> getAllReferencedAnchorIds();
}
