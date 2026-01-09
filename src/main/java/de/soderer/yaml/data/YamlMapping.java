package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.yaml.YamlWriter;

public class YamlMapping extends YamlNode implements Iterable<Map.Entry<String, Object>> {
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

	public void add(final String key, final Object value) throws DuplicateKeyException {
		if (key == null) {
			add(new YamlScalar(null), new YamlScalar(value));
		} else {
			add(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(value));
		}
	}

	public void add(final Number key, final Object value) throws DuplicateKeyException {
		if (key == null) {
			add(new YamlScalar(null), new YamlScalar(value));
		} else {
			add(new YamlScalar(key, YamlScalarType.NUMBER), new YamlScalar(value));
		}
	}

	public void add(final Boolean key, final Object value) throws DuplicateKeyException {
		if (key == null) {
			add(new YamlScalar(null), new YamlScalar(value));
		} else {
			add(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(value));
		}
	}

	public void add(final YamlNode key, final YamlNode value) throws DuplicateKeyException {
		if (containsKey(key)) {
			throw new DuplicateKeyException(key.toString());
		} else {
			entries.put(key, value);
		}
	}

	public void put(final String key, final Object value) {
		if (key == null) {
			put(new YamlScalar(null), new YamlScalar(value));
		} else {
			put(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(value));
		}
	}

	public void put(final Number key, final Object value) {
		if (key == null) {
			put(new YamlScalar(null), new YamlScalar(value));
		} else {
			put(new YamlScalar(key, YamlScalarType.NUMBER), new YamlScalar(value));
		}
	}

	public void put(final Boolean key, final Object value) {
		if (key == null) {
			put(new YamlScalar(null), new YamlScalar(value));
		} else {
			put(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(value));
		}
	}

	public void put(final YamlNode key, final YamlNode value) {
		entries.put(key, value);
	}

	public YamlNode remove(final String key) {
		if (key == null) {
			return remove(new YamlScalar(null));
		} else {
			return remove(new YamlScalar(key, YamlScalarType.STRING));
		}
	}

	public YamlNode remove(final Number key) {
		if (key == null) {
			return remove(new YamlScalar(null));
		} else {
			return remove(new YamlScalar(key, YamlScalarType.NUMBER));
		}
	}

	public YamlNode remove(final Boolean key) {
		if (key == null) {
			return remove(new YamlScalar(null));
		} else {
			return remove(new YamlScalar(key, YamlScalarType.BOOLEAN));
		}
	}

	public YamlNode remove(final YamlNode key) {
		return entries.remove(key);
	}

	public YamlNode get(final Number key) {
		if (key == null) {
			return get(new YamlScalar(null));
		} else {
			return get(new YamlScalar(key, YamlScalarType.NUMBER));
		}
	}

	public YamlNode get(final String key) {
		if (key == null) {
			return get(new YamlScalar(null));
		} else {
			return get(new YamlScalar(key, YamlScalarType.STRING));
		}
	}

	public YamlNode get(final Boolean key) {
		if (key == null) {
			return get(new YamlScalar(null));
		} else {
			return get(new YamlScalar(key, YamlScalarType.BOOLEAN));
		}
	}

	public YamlNode get(final YamlNode key) {
		return entries.get(key);
	}

	public boolean containsKey(final String key) {
		if (key == null) {
			return containsKey(new YamlScalar(null));
		} else {
			return containsKey(new YamlScalar(key, YamlScalarType.STRING));
		}
	}

	public boolean containsKey(final Number key) {
		if (key == null) {
			return containsKey(new YamlScalar(null));
		} else {
			return containsKey(new YamlScalar(key, YamlScalarType.NUMBER));
		}
	}

	public boolean containsKey(final Boolean key) {
		if (key == null) {
			return containsKey(new YamlScalar(null));
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

	public Collection<Object> values() {
		return Collections.unmodifiableCollection(entries.values());
	}

	public Collection<Object> simpleValues() {
		final List<Object> simpleValues = new ArrayList<>();
		for (final YamlNode value : entries.values()) {
			if (value instanceof YamlScalar) {
				if (((YamlScalar) value).getType() == YamlScalarType.NULL_VALUE) {
					simpleValues.add(null);
				} else {
					simpleValues.add(((YamlScalar) value).getValue());
				}
			} else {
				simpleValues.add(value);
			}
		}
		return Collections.unmodifiableCollection(simpleValues);
	}

	public Set<Entry<YamlNode, YamlNode>> entrySet() {
		return Collections.unmodifiableSet(entries.entrySet());
	}

	public Set<Entry<String, Object>> simpleEntrySet() {
		final LinkedHashMap<String, Object> simpleProperties = new LinkedHashMap<>();
		for (final Entry<YamlNode, YamlNode> entry : entries.entrySet()) {
			if (entry.getKey() instanceof YamlScalar) {
				final YamlScalar keyScalar = (YamlScalar) entry.getKey();
				if (keyScalar.getType() == YamlScalarType.STRING
						|| keyScalar.getType() == YamlScalarType.NUMBER
						|| keyScalar.getType() == YamlScalarType.BOOLEAN) {
					if (entry.getValue() instanceof YamlScalar) {
						if (((YamlScalar) entry.getValue()).getType() == YamlScalarType.NULL_VALUE) {
							simpleProperties.put(keyScalar.getValueString(), null);
						} else {
							simpleProperties.put(keyScalar.getValueString(), ((YamlScalar) entry.getValue()).getValue());
						}
					} else {
						simpleProperties.put(keyScalar.getValueString(), entry.getValue());
					}
				} else if (keyScalar.getType() == YamlScalarType.NULL_VALUE) {
					if (entry.getValue() instanceof YamlScalar) {
						if (((YamlScalar) entry.getValue()).getType() == YamlScalarType.NULL_VALUE) {
							simpleProperties.put(null, null);
						} else {
							simpleProperties.put(null, ((YamlScalar) entry.getValue()).getValue());
						}
					} else {
						simpleProperties.put(null, entry.getValue());
					}
				} else {
					throw new RuntimeException("Cannot create simpleEntrySet, because YamlMapping contains unkown key type: '" + keyScalar.getType() + "'");
				}
			} else {
				throw new RuntimeException("Cannot create simpleEntrySet, because YamlMapping contains complex keys");
			}
		}
		return Collections.unmodifiableSet(simpleProperties.entrySet());
	}

	public int size() {
		return entries.size();
	}

	@Override
	public Iterator<Entry<String, Object>> iterator() {
		return simpleEntrySet().iterator();
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
