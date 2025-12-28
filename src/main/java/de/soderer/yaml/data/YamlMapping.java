package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import de.soderer.yaml.YamlWriter;

public class YamlMapping extends YamlNode implements Iterable<Map.Entry<YamlNode, YamlNode>> {
	private final List<YamlKeyValue> entries = new ArrayList<>();

	private boolean flowStyle;

	public YamlMapping() {
		this(false);
	}

	public YamlMapping(final boolean flowStyle) {
		this.flowStyle = flowStyle;
	}

	public boolean isFlowStyle() {
		return flowStyle;
	}

	public void setFlowStyle(final boolean flowStyle) {
		this.flowStyle = flowStyle;
	}

	public void add(final String key, final Object value) {
		if (key == null) {
			if (value == null) {
				entries.add(new YamlKeyValue(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(null, YamlScalarType.NULL_VALUE)));
			} else if (value instanceof String) {
				entries.add(new YamlKeyValue(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.STRING)));
			} else if (value instanceof Number) {
				entries.add(new YamlKeyValue(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.NUMBER)));
			} else if (value instanceof Boolean) {
				entries.add(new YamlKeyValue(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.BOOLEAN)));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method add(YamlNode, YamlNode) instead");
			}
		} else {
			if (value == null) {
				entries.add(new YamlKeyValue(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(null, YamlScalarType.NULL_VALUE)));
			} else if (value instanceof String) {
				entries.add(new YamlKeyValue(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(value, YamlScalarType.STRING)));
			} else if (value instanceof Number) {
				entries.add(new YamlKeyValue(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(value, YamlScalarType.NUMBER)));
			} else if (value instanceof Boolean) {
				entries.add(new YamlKeyValue(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(value, YamlScalarType.BOOLEAN)));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method add(YamlNode, YamlNode) instead");
			}
		}
	}

	public void add(final Number key, final Object value) {
		if (key == null) {
			if (value == null) {
				entries.add(new YamlKeyValue(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(null, YamlScalarType.NULL_VALUE)));
			} else if (value instanceof String) {
				entries.add(new YamlKeyValue(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.STRING)));
			} else if (value instanceof Number) {
				entries.add(new YamlKeyValue(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.NUMBER)));
			} else if (value instanceof Boolean) {
				entries.add(new YamlKeyValue(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.BOOLEAN)));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method add(YamlNode, YamlNode) instead");
			}
		} else {
			if (value == null) {
				entries.add(new YamlKeyValue(new YamlScalar(key.toString(), YamlScalarType.NUMBER), new YamlScalar(null, YamlScalarType.NULL_VALUE)));
			} else if (value instanceof String) {
				entries.add(new YamlKeyValue(new YamlScalar(key.toString(), YamlScalarType.NUMBER), new YamlScalar(value, YamlScalarType.STRING)));
			} else if (value instanceof Number) {
				entries.add(new YamlKeyValue(new YamlScalar(key.toString(), YamlScalarType.NUMBER), new YamlScalar(value, YamlScalarType.NUMBER)));
			} else if (value instanceof Boolean) {
				entries.add(new YamlKeyValue(new YamlScalar(key.toString(), YamlScalarType.NUMBER), new YamlScalar(value, YamlScalarType.BOOLEAN)));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method add(YamlNode, YamlNode) instead");
			}
		}
	}

	public void add(final Boolean key, final Object value) {
		if (key == null) {
			if (value == null) {
				entries.add(new YamlKeyValue(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(null, YamlScalarType.NULL_VALUE)));
			} else if (value instanceof String) {
				entries.add(new YamlKeyValue(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.STRING)));
			} else if (value instanceof Number) {
				entries.add(new YamlKeyValue(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.NUMBER)));
			} else if (value instanceof Boolean) {
				entries.add(new YamlKeyValue(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.BOOLEAN)));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method add(YamlNode, YamlNode) instead");
			}
		} else {
			if (value == null) {
				entries.add(new YamlKeyValue(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(null, YamlScalarType.NULL_VALUE)));
			} else if (value instanceof String) {
				entries.add(new YamlKeyValue(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(value, YamlScalarType.STRING)));
			} else if (value instanceof Number) {
				entries.add(new YamlKeyValue(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(value, YamlScalarType.NUMBER)));
			} else if (value instanceof Boolean) {
				entries.add(new YamlKeyValue(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(value, YamlScalarType.BOOLEAN)));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method add(YamlNode, YamlNode) instead");
			}
		}
	}

	public void add(final YamlNode key, final YamlNode value) {
		entries.add(new YamlKeyValue(key, value));
	}

	public List<Object> remove(final String key) {
		if (key == null) {
			return remove(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return remove(new YamlScalar(key, YamlScalarType.STRING));
		}
	}

	public List<Object> remove(final Number key) {
		if (key == null) {
			return remove(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return remove(new YamlScalar(key, YamlScalarType.NUMBER));
		}
	}

	public List<Object> remove(final Boolean key) {
		if (key == null) {
			return remove(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return remove(new YamlScalar(key, YamlScalarType.BOOLEAN));
		}
	}

	public List<Object> remove(final YamlNode key) {
		final List<YamlKeyValue> entriesToDelete = entries.stream().filter(x -> x.getKey().equals(key)).collect(Collectors.toList());
		if (entriesToDelete.size() == 0) {
			return null;
		} else {
			final List<Object> returnList = new ArrayList<>();
			for (final YamlKeyValue entryToDelete : entriesToDelete) {
				returnList.add(entryToDelete.getValue());
				entries.remove(entryToDelete);
			}
			return returnList;
		}
	}

	public List<Object> get(final Number key) {
		if (key == null) {
			return get(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return get(new YamlScalar(key, YamlScalarType.NUMBER));
		}
	}

	public List<Object> get(final String key) {
		if (key == null) {
			return get(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return get(new YamlScalar(key, YamlScalarType.BOOLEAN));
		}
	}

	public List<Object> get(final Boolean key) {
		if (key == null) {
			return get(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return get(new YamlScalar(key, YamlScalarType.STRING));
		}
	}

	public List<Object> get(final YamlNode key) {
		final List<YamlKeyValue> entriesFound = entries.stream().filter(x -> x.getKey().equals(key)).collect(Collectors.toList());
		if (entriesFound.size() == 0) {
			return null;
		} else {
			final List<Object> returnList = new ArrayList<>();
			for (final YamlKeyValue entryFound : entriesFound) {
				returnList.add(entryFound.getValue());
			}
			return returnList;
		}
	}

	public boolean containsKey(final String key) {
		if (key == null) {
			return containsKey(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return containsKey(new YamlScalar(key, YamlScalarType.STRING));
		}
	}

	public boolean containsKey(final Number key) {
		if (key == null) {
			return containsKey(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return containsKey(new YamlScalar(key, YamlScalarType.NUMBER));
		}
	}

	public boolean containsKey(final Boolean key) {
		if (key == null) {
			return containsKey(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return containsKey(new YamlScalar(key, YamlScalarType.BOOLEAN));
		}
	}

	public boolean containsKey(final YamlNode key) {
		for (final YamlKeyValue entry : entries) {
			if (entry.getKey().equals(key)) {
				return true;
			}
		}
		return false;
	}

	public Collection<YamlNode> keyList() {
		return entries.stream().map(x -> x.getKey()).collect(Collectors.toList());
	}

	public Collection<YamlNode> values() {
		return entries.stream().map(x -> x.getValue()).collect(Collectors.toList());
	}

	public Collection<Entry<YamlNode, YamlNode>> entryList() {
		return Collections.unmodifiableCollection(entries);
	}

	public List<YamlKeyValue> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	public int size() {
		return entries.size();
	}

	@Override
	public Iterator<Entry<YamlNode, YamlNode>> iterator() {
		return entryList().iterator();
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
		return Objects.hash(entries, flowStyle);
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
			final YamlMapping other = (YamlMapping) otherObject;
			return Objects.equals(entries, other.entries) && flowStyle == other.flowStyle;
		}
	}
}
