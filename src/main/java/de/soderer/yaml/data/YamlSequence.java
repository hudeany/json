package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import de.soderer.yaml.YamlWriter;

public class YamlSequence extends YamlNode implements Iterable<YamlNode> {
	private final List<YamlNode> items = new ArrayList<>();

	private boolean flowStyle;

	public YamlSequence() {
		this(false);
	}

	public YamlSequence(final boolean flowStyle) {
		this.flowStyle = flowStyle;
	}

	public boolean isFlowStyle() {
		return flowStyle;
	}

	public void setFlowStyle(final boolean flowStyle) {
		this.flowStyle = flowStyle;
	}

	public void add(final YamlNode node) {
		items.add(node);
	}

	public Object remove(final Object item) {
		if (item == null) {
			return remove(new YamlScalar("null", YamlScalarType.NULL_VALUE));
		} else if (item instanceof String) {
			return remove(new YamlScalar(item.toString(), YamlScalarType.STRING));
		} else if (item instanceof Number) {
			return remove(new YamlScalar(item.toString(), YamlScalarType.NUMBER));
		} else if (item instanceof Boolean) {
			return remove(new YamlScalar(item.toString(), YamlScalarType.BOOLEAN));
		} else {
			throw new RuntimeException("Unsupported type of value object: '" + item.getClass().getSimpleName() + "'. Use method remove(YamlNode) instead");
		}
	}

	public boolean remove(final YamlNode item) {
		return items.remove(item);
	}

	public YamlNode get(final int index) {
		return items.get(index);
	}

	public int size() {
		return items.size();
	}

	@Override
	public Iterator<YamlNode> iterator() {
		return items.iterator();
	}

	public Stream<YamlNode> stream() {
		return items.stream();
	}

	@Override
	public String toString() {
		try {
			return YamlWriter.toString(this);
		} catch (final Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(flowStyle, items);
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
			final YamlSequence other = (YamlSequence) otherObject;
			return flowStyle == other.flowStyle && Objects.equals(items, other.items);
		}
	}
}
