package de.soderer.yaml.data;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import de.soderer.yaml.YamlWriter;
import de.soderer.yaml.exception.YamlDuplicateKeyException;

public class YamlMapping extends YamlNode implements Iterable<Map.Entry<YamlNode, YamlNode>> {
	private final LinkedHashMap<YamlNode, YamlNode> entries = new LinkedHashMap<>();

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

	public void add(final String key, final Object value) throws YamlDuplicateKeyException {
		if (key == null) {
			if (value == null) {
				add(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(null, YamlScalarType.NULL_VALUE));
			} else if (value instanceof String) {
				add(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.STRING));
			} else if (value instanceof Number) {
				add(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.NUMBER));
			} else if (value instanceof Boolean) {
				add(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.BOOLEAN));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method add(YamlNode, YamlNode) instead");
			}
		} else {
			if (value == null) {
				add(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(null, YamlScalarType.NULL_VALUE));
			} else if (value instanceof String) {
				add(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(value, YamlScalarType.STRING));
			} else if (value instanceof Number) {
				add(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(value, YamlScalarType.NUMBER));
			} else if (value instanceof Boolean) {
				add(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(value, YamlScalarType.BOOLEAN));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method add(YamlNode, YamlNode) instead");
			}
		}
	}

	public void add(final Number key, final Object value) throws YamlDuplicateKeyException {
		if (key == null) {
			if (value == null) {
				add(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(null, YamlScalarType.NULL_VALUE));
			} else if (value instanceof String) {
				add(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.STRING));
			} else if (value instanceof Number) {
				add(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.NUMBER));
			} else if (value instanceof Boolean) {
				add(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.BOOLEAN));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method add(YamlNode, YamlNode) instead");
			}
		} else {
			if (value == null) {
				add(new YamlScalar(key.toString(), YamlScalarType.NUMBER), new YamlScalar(null, YamlScalarType.NULL_VALUE));
			} else if (value instanceof String) {
				add(new YamlScalar(key.toString(), YamlScalarType.NUMBER), new YamlScalar(value, YamlScalarType.STRING));
			} else if (value instanceof Number) {
				add(new YamlScalar(key.toString(), YamlScalarType.NUMBER), new YamlScalar(value, YamlScalarType.NUMBER));
			} else if (value instanceof Boolean) {
				add(new YamlScalar(key.toString(), YamlScalarType.NUMBER), new YamlScalar(value, YamlScalarType.BOOLEAN));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method add(YamlNode, YamlNode) instead");
			}
		}
	}

	public void add(final Boolean key, final Object value) throws YamlDuplicateKeyException {
		if (key == null) {
			if (value == null) {
				add(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(null, YamlScalarType.NULL_VALUE));
			} else if (value instanceof String) {
				add(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.STRING));
			} else if (value instanceof Number) {
				add(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.NUMBER));
			} else if (value instanceof Boolean) {
				add(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.BOOLEAN));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method add(YamlNode, YamlNode) instead");
			}
		} else {
			if (value == null) {
				add(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(null, YamlScalarType.NULL_VALUE));
			} else if (value instanceof String) {
				add(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(value, YamlScalarType.STRING));
			} else if (value instanceof Number) {
				add(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(value, YamlScalarType.NUMBER));
			} else if (value instanceof Boolean) {
				add(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(value, YamlScalarType.BOOLEAN));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method add(YamlNode, YamlNode) instead");
			}
		}
	}

	public void add(final YamlNode key, final YamlNode value) throws YamlDuplicateKeyException {
		if (containsKey(key)) {
			throw new YamlDuplicateKeyException(key);
		} else {
			entries.put(key, value);
		}
	}

	public void put(final String key, final Object value) {
		if (key == null) {
			if (value == null) {
				put(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(null, YamlScalarType.NULL_VALUE));
			} else if (value instanceof String) {
				put(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.STRING));
			} else if (value instanceof Number) {
				put(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.NUMBER));
			} else if (value instanceof Boolean) {
				put(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.BOOLEAN));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method put(YamlNode, YamlNode) instead");
			}
		} else {
			if (value == null) {
				put(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(null, YamlScalarType.NULL_VALUE));
			} else if (value instanceof String) {
				put(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(value, YamlScalarType.STRING));
			} else if (value instanceof Number) {
				put(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(value, YamlScalarType.NUMBER));
			} else if (value instanceof Boolean) {
				put(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(value, YamlScalarType.BOOLEAN));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method put(YamlNode, YamlNode) instead");
			}
		}
	}

	public void put(final Number key, final Object value) {
		if (key == null) {
			if (value == null) {
				put(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(null, YamlScalarType.NULL_VALUE));
			} else if (value instanceof String) {
				put(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.STRING));
			} else if (value instanceof Number) {
				put(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.NUMBER));
			} else if (value instanceof Boolean) {
				put(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.BOOLEAN));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method put(YamlNode, YamlNode) instead");
			}
		} else {
			if (value == null) {
				put(new YamlScalar(key.toString(), YamlScalarType.NUMBER), new YamlScalar(null, YamlScalarType.NULL_VALUE));
			} else if (value instanceof String) {
				put(new YamlScalar(key.toString(), YamlScalarType.NUMBER), new YamlScalar(value, YamlScalarType.STRING));
			} else if (value instanceof Number) {
				put(new YamlScalar(key.toString(), YamlScalarType.NUMBER), new YamlScalar(value, YamlScalarType.NUMBER));
			} else if (value instanceof Boolean) {
				put(new YamlScalar(key.toString(), YamlScalarType.NUMBER), new YamlScalar(value, YamlScalarType.BOOLEAN));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method put(YamlNode, YamlNode) instead");
			}
		}
	}

	public void put(final Boolean key, final Object value) {
		if (key == null) {
			if (value == null) {
				put(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(null, YamlScalarType.NULL_VALUE));
			} else if (value instanceof String) {
				put(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.STRING));
			} else if (value instanceof Number) {
				put(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.NUMBER));
			} else if (value instanceof Boolean) {
				put(new YamlScalar(null, YamlScalarType.NULL_VALUE), new YamlScalar(value, YamlScalarType.BOOLEAN));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method put(YamlNode, YamlNode) instead");
			}
		} else {
			if (value == null) {
				put(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(null, YamlScalarType.NULL_VALUE));
			} else if (value instanceof String) {
				put(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(value, YamlScalarType.STRING));
			} else if (value instanceof Number) {
				put(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(value, YamlScalarType.NUMBER));
			} else if (value instanceof Boolean) {
				put(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(value, YamlScalarType.BOOLEAN));
			} else {
				throw new RuntimeException("Unsupported type of value object: '" + value.getClass().getSimpleName() + "'. Use method put(YamlNode, YamlNode) instead");
			}
		}
	}

	public void put(final YamlNode key, final YamlNode value) {
		entries.put(key, value);
	}

	public YamlNode remove(final String key) {
		if (key == null) {
			return remove(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return remove(new YamlScalar(key, YamlScalarType.STRING));
		}
	}

	public YamlNode remove(final Number key) {
		if (key == null) {
			return remove(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return remove(new YamlScalar(key, YamlScalarType.NUMBER));
		}
	}

	public YamlNode remove(final Boolean key) {
		if (key == null) {
			return remove(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return remove(new YamlScalar(key, YamlScalarType.BOOLEAN));
		}
	}

	public YamlNode remove(final YamlNode key) {
		return entries.remove(key);
	}

	public YamlNode get(final Number key) {
		if (key == null) {
			return get(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return get(new YamlScalar(key, YamlScalarType.NUMBER));
		}
	}

	public YamlNode get(final String key) {
		if (key == null) {
			return get(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return get(new YamlScalar(key, YamlScalarType.BOOLEAN));
		}
	}

	public YamlNode get(final Boolean key) {
		if (key == null) {
			return get(new YamlScalar(null, YamlScalarType.NULL_VALUE));
		} else {
			return get(new YamlScalar(key, YamlScalarType.STRING));
		}
	}

	public YamlNode get(final YamlNode key) {
		return entries.get(key);
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
		return entries.containsKey(key);
	}

	public Set<YamlNode> keySet() {
		return Collections.unmodifiableSet(entries.keySet());
	}

	public Collection<Object> values () {
		return Collections.unmodifiableCollection(entries.values());
	}

	public Set<Entry<YamlNode, YamlNode>> entrySet() {
		return Collections.unmodifiableSet(entries.entrySet());
	}

	public int size() {
		return entries.size();
	}

	@Override
	public Iterator<Entry<YamlNode, YamlNode>> iterator() {
		return entrySet().iterator();
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
