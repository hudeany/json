package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.soderer.yaml.YamlWriter;

public class YamlMapping extends YamlNode {
	private final List<YamlKeyValue> entries = new ArrayList<>();

	private boolean flowStyle;

	public YamlMapping() {
		this(false);
	}

	public YamlMapping(final boolean flowStyle) {
		this.flowStyle = flowStyle;
	}

	public void addEntry(final YamlNode key, final YamlNode value) {
		entries.add(new YamlKeyValue(key, value));
	}

	public List<YamlKeyValue> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	public boolean isFlowStyle() {
		return flowStyle;
	}

	public void setFlowStyle(final boolean flowStyle) {
		this.flowStyle = flowStyle;
	}

	@Override
	public String toString() {
		try {
			return YamlWriter.toString(this);
		} catch (final Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
