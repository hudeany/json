package de.soderer.yaml;

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
		}
		this.anchor = anchor;
		return this;
	}

	public YamlStyle getStyle() {
		return style;
	}

	public YamlNode setStyle(final YamlStyle style) {
		this.style = style;
		return this;
	}

	public abstract Set<String> getAllAvailableAnchorIds();

	public abstract Set<String> getAllReferencedAnchorIds();
}
