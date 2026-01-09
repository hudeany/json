package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import de.soderer.yaml.YamlWriter;

public class YamlSequence extends YamlNode implements Iterable<Object> {
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

	public void addNull() {
		add(new YamlScalar(null));
	}

	public void add(final String item) {
		if (item == null) {
			add(new YamlScalar(null));
		} else {
			add(new YamlScalar(item, YamlScalarType.STRING));
		}
	}

	public void add(final Number item) {
		if (item == null) {
			add(new YamlScalar(null));
		} else {
			add(new YamlScalar(item, YamlScalarType.NUMBER));
		}
	}

	public void add(final Boolean item) {
		if (item == null) {
			add(new YamlScalar(null));
		} else {
			add(new YamlScalar(item, YamlScalarType.BOOLEAN));
		}
	}

	public void add(final YamlNode item) {
		items.add(item);
	}

	public boolean remove(final String item) {
		if (item == null) {
			return remove(new YamlScalar(null));
		} else {
			return remove(new YamlScalar(item, YamlScalarType.STRING));
		}
	}

	public boolean remove(final Number item) {
		if (item == null) {
			return remove(new YamlScalar(null));
		} else {
			return remove(new YamlScalar(item, YamlScalarType.NUMBER));
		}
	}

	public boolean remove(final Boolean item) {
		if (item == null) {
			return remove(new YamlScalar(null));
		} else {
			return remove(new YamlScalar(item, YamlScalarType.BOOLEAN));
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

	public Collection<YamlNode> items() {
		return Collections.unmodifiableCollection(items);
	}

	public Collection<Object> simpleItems() {
		final List<Object> simpleItems = new ArrayList<>();
		for (final YamlNode item : items) {
			if (item instanceof YamlScalar) {
				if (((YamlScalar) item).getType() == YamlScalarType.NULL_VALUE) {
					simpleItems.add(null);
				} else {
					simpleItems.add(((YamlScalar) item).getValue());
				}
			} else {
				simpleItems.add(item);
			}
		}
		return Collections.unmodifiableCollection(simpleItems);
	}

	@Override
	public Iterator<Object> iterator() {
		return simpleItems().iterator();
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
