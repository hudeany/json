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

	public YamlSequence setFlowStyle(final boolean flowStyle) {
		this.flowStyle = flowStyle;
		return this;
	}

	public YamlSequence addNull() {
		add(new YamlScalar(null));
		return this;
	}

	public YamlSequence add(final String item) {
		if (item == null) {
			add(new YamlScalar(null));
		} else {
			add(new YamlScalar(item, YamlScalarType.STRING));
		}
		return this;
	}

	public YamlSequence add(final Number item) {
		if (item == null) {
			add(new YamlScalar(null));
		} else {
			add(new YamlScalar(item, YamlScalarType.NUMBER));
		}
		return this;
	}

	public YamlSequence add(final Boolean item) {
		if (item == null) {
			add(new YamlScalar(null));
		} else {
			add(new YamlScalar(item, YamlScalarType.BOOLEAN));
		}
		return this;
	}

	public YamlSequence add(final YamlNode item) {
		items.add(item);
		return this;
	}

	public YamlSequence insertNull(final int index) {
		insert(index, new YamlScalar(null));
		return this;
	}

	public YamlSequence insert(final int index, final String item) {
		if (item == null) {
			insert(index, new YamlScalar(null));
		} else {
			insert(index, new YamlScalar(item, YamlScalarType.STRING));
		}
		return this;
	}

	public YamlSequence insert(final int index, final Number item) {
		if (item == null) {
			insert(index, new YamlScalar(null));
		} else {
			insert(index, new YamlScalar(item, YamlScalarType.NUMBER));
		}
		return this;
	}

	public YamlSequence insert(final int index, final Boolean item) {
		if (item == null) {
			insert(index, new YamlScalar(null));
		} else {
			insert(index, new YamlScalar(item, YamlScalarType.BOOLEAN));
		}
		return this;
	}

	public YamlSequence insert(final int index, final YamlNode item) {
		items.add(index, item);
		return this;
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

	public YamlNode removeByIndex(final int index) {
		return items.remove(index);
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
		return Objects.hash(items);
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
			return Objects.equals(items, other.items);
		}
	}

	public YamlSequence sort(final boolean ascending) {
		items.sort((a, b) -> {
			final boolean aIsNull = isNullScalar(a);
			final boolean bIsNull = isNullScalar(b);

			if (aIsNull && bIsNull) {
				return 0;
			} else if (aIsNull) {
				return ascending ? -1 : 1;
			} else if (bIsNull) {
				return ascending ? 1 : -1;
			} else {
				return compareValues(getSimpleValue(a), getSimpleValue(b), ascending);
			}
		});
		return this;
	}

	public YamlSequence sortByAttribute(final String attributeName, final boolean ascending) {
		items.sort((a, b) -> {
			if (!(a instanceof YamlMapping) || !(b instanceof YamlMapping)) {
				return 0;
			} else {
				final YamlNode aNode = ((YamlMapping) a).get(attributeName);
				final YamlNode bNode = ((YamlMapping) b).get(attributeName);

				final boolean aIsNull = aNode == null || isNullScalar(aNode);
				final boolean bIsNull = bNode == null || isNullScalar(bNode);

				if (aIsNull && bIsNull) {
					return 0;
				} else if (aIsNull) {
					return ascending ? -1 : 1;
				} else if (bIsNull) {
					return ascending ? 1 : -1;
				} else {
					return compareValues(getSimpleValue(aNode), getSimpleValue(bNode), ascending);
				}
			}
		});
		return this;
	}

	private static boolean isNullScalar(final YamlNode node) {
		return node instanceof YamlScalar && ((YamlScalar) node).getType() == YamlScalarType.NULL_VALUE;
	}

	private static Object getSimpleValue(final YamlNode node) {
		if (node instanceof YamlScalar) {
			return ((YamlScalar) node).getValue();
		} else {
			return null;
		}
	}

	private static int compareValues(final Object a, final Object b, final boolean ascending) {
		if (a == null && b == null) {
			return 0;
		} else if (a == null) {
			return ascending ? -1 : 1;
		} else if (b == null) {
			return ascending ? 1 : -1;
		} else {
			final int result;
			if (a instanceof Number && b instanceof Number) {
				result = Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
			} else if (a instanceof Boolean && b instanceof Boolean) {
				result = Boolean.compare((Boolean) a, (Boolean) b);
			} else {
				result = a.toString().compareTo(b.toString());
			}
			return ascending ? result : -result;
		}
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public YamlSequence merge(final YamlSequence other, final YamlSequenceMergeStrategy strategy) {
		if (other == null) {
			return this;
		} else {
			for (final YamlNode item : other.items()) {
				if (strategy == YamlSequenceMergeStrategy.APPEND_ALL) {
					items.add(item);
				} else if (!items.contains(item)) {
					items.add(item);
				} else {
					// SKIP_DUPLICATES
				}
			}
			return this;
		}
	}
}
