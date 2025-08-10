package de.soderer.yaml;

import java.util.Set;

public abstract class YamlNode extends YamlValue {
	protected String anchor;
	protected YamlStyle style = null;

	public String getAnchor() {
		return anchor;
	}

	public void setAnchor(final String anchor) {
		this.anchor = anchor;
	}

	public YamlStyle getStyle() {
		return style;
	}

	public void setStyle(YamlStyle style) {
		this.style = style;
	}
	
	public abstract Set<String> getAllAvailableAnchorIds();
	
	public abstract Set<String> getAllReferencedAnchorIds();
}
