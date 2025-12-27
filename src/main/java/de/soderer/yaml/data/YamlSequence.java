package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.soderer.yaml.YamlWriter;

public class YamlSequence extends YamlNode {
	private final List<YamlNode> items = new ArrayList<>();

	private boolean flowStyle;

	public YamlSequence() {
		this(false);
	}

	public YamlSequence(final boolean flowStyle) {
		this.flowStyle = flowStyle;
	}

	public void addItem(final YamlNode node) {
		items.add(node);
	}

	public List<YamlNode> getItems() {
		return Collections.unmodifiableList(items);
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
