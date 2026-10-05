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
import de.soderer.json.exception.MissingKeyException;
import de.soderer.yaml.YamlWriter;

/**
 * YAML mapping of key nodes to value nodes in insertion order.
 * <p>
 * Keys are usually scalars, but may be any node ("complex keys"). Java keys are converted:
 * String to a string scalar, Number to a number scalar, Boolean to a boolean scalar, null to the
 * null scalar. A number key 1 and a string key "1" are different keys. There are three ways to
 * set a value: add (key must be new), replace (key must exist, position is kept) and put (adds
 * or replaces). Iterating returns the entries with simple Java keys and values, see
 * {@link #simpleEntrySet()}.
 * </p>
 */
public class YamlMapping extends YamlNode implements Iterable<Map.Entry<String, Object>> {
	/**
	 * The entries in insertion order.
	 */
	private final LinkedHashMap<YamlNode, YamlNode> entries = new LinkedHashMap<>();

	/**
	 * True to write this mapping in flow style ("{a: 1}").
	 */
	private boolean flowStyle;

	/**
	 * Creates an empty mapping in block style.
	 */
	public YamlMapping() {
		this(false);
	}

	/**
	 * Creates an empty mapping.
	 *
	 * @param flowStyle
	 *            true for flow style ("{a: 1}"), false for block style
	 */
	public YamlMapping(final boolean flowStyle) {
		this.flowStyle = flowStyle;
	}

	/**
	 * Returns whether this mapping is written in flow style.
	 *
	 * @return true for flow style
	 */
	public boolean isFlowStyle() {
		return flowStyle;
	}

	/**
	 * Sets whether this mapping is written in flow style.
	 *
	 * @param flowStyle
	 *            true for flow style, false for block style
	 */
	public void setFlowStyle(final boolean flowStyle) {
		this.flowStyle = flowStyle;
	}

	/**
	 * Sets whether this mapping is written in flow style.
	 *
	 * @param newFlowStyle
	 *            true for flow style, false for block style
	 * @return this mapping for chaining
	 */
	public YamlMapping withFlowStyle(final boolean newFlowStyle) {
		setFlowStyle(newFlowStyle);
		return this;
	}

	/**
	 * Adds a new entry with a string key.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @param value
	 *            the value: a YamlNode, or a String, Number, Boolean or null converted to a scalar
	 * @return this mapping for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws RuntimeException
	 *             if the value type is not supported
	 */
	public YamlMapping add(final String key, final Object value) throws DuplicateKeyException {
		if (key == null) {
			add(new YamlScalar(null), toYamlNode(value));
		} else if (value instanceof YamlNode) {
			add(new YamlScalar(key), (YamlNode) value);
		} else {
			add(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(value));
		}
		return this;
	}

	/**
	 * Adds a new entry with a number key.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @param value
	 *            the value: a YamlNode, or a String, Number, Boolean or null converted to a scalar
	 * @return this mapping for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws RuntimeException
	 *             if the value type is not supported
	 */
	public YamlMapping add(final Number key, final Object value) throws DuplicateKeyException {
		if (key == null) {
			add(new YamlScalar(null), toYamlNode(value));
		} else {
			add(new YamlScalar(key, YamlScalarType.NUMBER), toYamlNode(value));
		}
		return this;
	}

	/**
	 * Adds a new entry with a boolean key.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @param value
	 *            the value: a YamlNode, or a String, Number, Boolean or null converted to a scalar
	 * @return this mapping for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws RuntimeException
	 *             if the value type is not supported
	 */
	public YamlMapping add(final Boolean key, final Object value) throws DuplicateKeyException {
		if (key == null) {
			add(new YamlScalar(null), toYamlNode(value));
		} else {
			add(new YamlScalar(key, YamlScalarType.BOOLEAN), toYamlNode(value));
		}
		return this;
	}

	/**
	 * Adds a new entry.
	 *
	 * @param key
	 *            the key node
	 * @param value
	 *            the value node
	 * @return this mapping for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 */
	public YamlMapping add(final YamlNode key, final YamlNode value) throws DuplicateKeyException {
		if (containsKey(key)) {
			throw new DuplicateKeyException(key.toString());
		} else {
			entries.put(key, value);
		}
		return this;
	}

	/**
	 * Replacing the property value for the given property key without changing the order of the properties within the YamlMapping
	 *
	 * @param key
	 *            the string key, null for the null key
	 * @param value
	 *            the value: a YamlNode, or a String, Number, Boolean or null converted to a scalar
	 * @return this mapping for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws RuntimeException
	 *             if the value type is not supported
	 */
	public YamlMapping replace(final String key, final Object value) throws MissingKeyException {
		if (key == null) {
			replace(new YamlScalar(null), toYamlNode(value));
		} else if (value instanceof YamlNode) {
			replace(new YamlScalar(key), (YamlNode) value);
		} else {
			replace(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(value));
		}
		return this;
	}

	/**
	 * Replacing the property value for the given property key without changing the order of the properties within the YamlMapping
	 *
	 * @param key
	 *            the number key, null for the null key
	 * @param value
	 *            the value: a YamlNode, or a String, Number, Boolean or null converted to a scalar
	 * @return this mapping for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws RuntimeException
	 *             if the value type is not supported
	 */
	public YamlMapping replace(final Number key, final Object value) throws MissingKeyException {
		if (key == null) {
			replace(new YamlScalar(null), toYamlNode(value));
		} else {
			replace(new YamlScalar(key, YamlScalarType.NUMBER), toYamlNode(value));
		}
		return this;
	}

	/**
	 * Replacing the property value for the given property key without changing the order of the properties within the YamlMapping
	 *
	 * @param key
	 *            the boolean key, null for the null key
	 * @param value
	 *            the value: a YamlNode, or a String, Number, Boolean or null converted to a scalar
	 * @return this mapping for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws RuntimeException
	 *             if the value type is not supported
	 */
	public YamlMapping replace(final Boolean key, final Object value) throws MissingKeyException {
		if (key == null) {
			replace(new YamlScalar(null), toYamlNode(value));
		} else {
			replace(new YamlScalar(key, YamlScalarType.BOOLEAN), toYamlNode(value));
		}
		return this;
	}

	/**
	 * Replacing the property value for the given property key without changing the order of the properties within the YamlMapping
	 *
	 * @param key
	 *            the key node
	 * @param value
	 *            the value node
	 * @return this mapping for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 */
	public YamlMapping replace(final YamlNode key, final YamlNode value) throws MissingKeyException {
		if (containsKey(key)) {
			entries.put(key, value);
		} else {
			throw new MissingKeyException(key.toString());
		}
		return this;
	}

	/**
	 * Sets the value of a string key, adding the entry if the key does not exist yet.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @param value
	 *            the value: a YamlNode, or a String, Number, Boolean or null converted to a scalar
	 * @return this mapping for chaining
	 * @throws RuntimeException
	 *             if the value type is not supported
	 */
	public YamlMapping put(final String key, final Object value) {
		if (key == null) {
			put(new YamlScalar(null), toYamlNode(value));
		} else if (value instanceof YamlNode) {
			put(new YamlScalar(key, YamlScalarType.STRING), (YamlNode) value);
		} else {
			put(new YamlScalar(key, YamlScalarType.STRING), new YamlScalar(value));
		}
		return this;
	}

	/**
	 * Sets the value of a number key, adding the entry if the key does not exist yet.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @param value
	 *            the value: a YamlNode, or a String, Number, Boolean or null converted to a scalar
	 * @return this mapping for chaining
	 * @throws RuntimeException
	 *             if the value type is not supported
	 */
	public YamlMapping put(final Number key, final Object value) {
		if (key == null) {
			put(new YamlScalar(null), toYamlNode(value));
		} else if (value instanceof YamlNode) {
			put(new YamlScalar(key, YamlScalarType.NUMBER), (YamlNode) value);
		} else {
			put(new YamlScalar(key, YamlScalarType.NUMBER), new YamlScalar(value));
		}
		return this;
	}

	/**
	 * Sets the value of a boolean key, adding the entry if the key does not exist yet.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @param value
	 *            the value: a YamlNode, or a String, Number, Boolean or null converted to a scalar
	 * @return this mapping for chaining
	 * @throws RuntimeException
	 *             if the value type is not supported
	 */
	public YamlMapping put(final Boolean key, final Object value) {
		if (key == null) {
			put(new YamlScalar(null), toYamlNode(value));
		} else if (value instanceof YamlNode) {
			put(new YamlScalar(key, YamlScalarType.BOOLEAN), (YamlNode) value);
		} else {
			put(new YamlScalar(key, YamlScalarType.BOOLEAN), new YamlScalar(value));
		}
		return this;
	}

	/**
	 * Converts a value to a node.
	 *
	 * @param value
	 *            a YamlNode, or a String, Number, Boolean or null
	 * @return the node
	 */
	private static YamlNode toYamlNode(final Object value) {
		return value instanceof YamlNode ? (YamlNode) value : new YamlScalar(value);
	}

	/**
	 * Sets the value of a key, adding the entry if the key does not exist yet.
	 *
	 * @param key
	 *            the key node
	 * @param value
	 *            the value node
	 * @return this mapping for chaining
	 */
	public YamlMapping put(final YamlNode key, final YamlNode value) {
		entries.put(key, value);
		return this;
	}

	/**
	 * Removes the entry of a string key.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @return the removed value, or null if the key did not exist
	 */
	public YamlNode remove(final String key) {
		if (key == null) {
			return remove(new YamlScalar(null));
		} else {
			return remove(new YamlScalar(key, YamlScalarType.STRING));
		}
	}

	/**
	 * Removes the entry of a number key.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @return the removed value, or null if the key did not exist
	 */
	public YamlNode remove(final Number key) {
		if (key == null) {
			return remove(new YamlScalar(null));
		} else {
			return remove(new YamlScalar(key, YamlScalarType.NUMBER));
		}
	}

	/**
	 * Removes the entry of a boolean key.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @return the removed value, or null if the key did not exist
	 */
	public YamlNode remove(final Boolean key) {
		if (key == null) {
			return remove(new YamlScalar(null));
		} else {
			return remove(new YamlScalar(key, YamlScalarType.BOOLEAN));
		}
	}

	/**
	 * Removes the entry of a key.
	 *
	 * @param key
	 *            the key node
	 * @return the removed value, or null if the key did not exist
	 */
	public YamlNode remove(final YamlNode key) {
		return entries.remove(key);
	}

	/**
	 * Returns the value of a number key.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @return the value node, or null if the key does not exist
	 */
	public YamlNode get(final Number key) {
		if (key == null) {
			return get(new YamlScalar(null));
		} else {
			return get(new YamlScalar(key, YamlScalarType.NUMBER));
		}
	}

	/**
	 * Returns the value of a string key.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @return the value node, or null if the key does not exist
	 */
	public YamlNode get(final String key) {
		if (key == null) {
			return get(new YamlScalar(null));
		} else {
			return get(new YamlScalar(key, YamlScalarType.STRING));
		}
	}

	/**
	 * Returns the value of a boolean key.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @return the value node, or null if the key does not exist
	 */
	public YamlNode get(final Boolean key) {
		if (key == null) {
			return get(new YamlScalar(null));
		} else {
			return get(new YamlScalar(key, YamlScalarType.BOOLEAN));
		}
	}

	/**
	 * Returns the value of a key.
	 *
	 * @param key
	 *            the key node
	 * @return the value node, or null if the key does not exist
	 */
	public YamlNode get(final YamlNode key) {
		return entries.get(key);
	}

	/**
	 * Returns the value of a string key as simple Java value.
	 *
	 * @param key
	 *            the key
	 * @return the String, Number or Boolean value of a scalar, the node itself for other nodes, or
	 *         null for the null value and missing keys
	 */
	public Object getSimpleValue(final String key) {
		final Object value = get(key);
		if (value == null) {
			return null;
		} else if (value instanceof YamlScalar) {
			return ((YamlScalar) value).getValue();
		} else {
			return value;
		}
	}

	/**
	 * Checks whether a string key exists.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @return true, if the key exists
	 */
	public boolean containsKey(final String key) {
		if (key == null) {
			return containsKey(new YamlScalar(null));
		} else {
			return containsKey(new YamlScalar(key, YamlScalarType.STRING));
		}
	}

	/**
	 * Checks whether a number key exists.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @return true, if the key exists
	 */
	public boolean containsKey(final Number key) {
		if (key == null) {
			return containsKey(new YamlScalar(null));
		} else {
			return containsKey(new YamlScalar(key, YamlScalarType.NUMBER));
		}
	}

	/**
	 * Checks whether a boolean key exists.
	 *
	 * @param key
	 *            the key, null for the null key
	 * @return true, if the key exists
	 */
	public boolean containsKey(final Boolean key) {
		if (key == null) {
			return containsKey(new YamlScalar(null));
		} else {
			return containsKey(new YamlScalar(key, YamlScalarType.BOOLEAN));
		}
	}

	/**
	 * Checks whether a key exists.
	 *
	 * @param key
	 *            the key node
	 * @return true, if the key exists
	 */
	public boolean containsKey(final YamlNode key) {
		return entries.containsKey(key);
	}

	/**
	 * Returns the keys in insertion order.
	 *
	 * @return an unmodifiable view of the keys
	 */
	public Set<YamlNode> keySet() {
		return Collections.unmodifiableSet(entries.keySet());
	}

	/**
	 * Returns the values in insertion order.
	 *
	 * @return an unmodifiable view of the values
	 */
	public Collection<YamlNode> values() {
		return Collections.unmodifiableCollection(entries.values());
	}

	/**
	 * Returns the values as simple Java values: String, Number or Boolean for scalars, null for the
	 * null value, and the node itself for other nodes.
	 *
	 * @return an unmodifiable copy of the simple values
	 */
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

	/**
	 * Returns the entries in insertion order.
	 *
	 * @return an unmodifiable view of the entries
	 */
	public Set<Entry<YamlNode, YamlNode>> entrySet() {
		return Collections.unmodifiableSet(entries.entrySet());
	}

	/**
	 * Returns the entries with the key text and the simple Java value, see {@link #simpleValues()}.
	 *
	 * @return an unmodifiable copy of the entries
	 * @throws RuntimeException
	 *             if the mapping contains complex keys
	 */
	public Set<Entry<String, Object>> simpleEntrySet() {
		final LinkedHashMap<String, Object> simpleProperties = new LinkedHashMap<>();
		for (final Entry<YamlNode, YamlNode> entry : entries.entrySet()) {
			if (entry.getKey() instanceof YamlScalar) {
				final YamlScalar keyScalar = (YamlScalar) entry.getKey();
				if (keyScalar.getType() == YamlScalarType.STRING
						|| keyScalar.getType() == YamlScalarType.MULTILINE
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

	/**
	 * Returns the number of entries.
	 *
	 * @return the number of entries
	 */
	public int size() {
		return entries.size();
	}

	/**
	 * Iterates over the entries with the key text and the simple Java value, see
	 * {@link #simpleEntrySet()}.
	 */
	@Override
	public Iterator<Entry<String, Object>> iterator() {
		return simpleEntrySet().iterator();
	}

	/**
	 * Returns this mapping as YAML text in default format.
	 */
	@Override
	public String toString() {
		try {
			return YamlWriter.toString(this);
		} catch (final Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Hash code consistent with {@link #equals(Object)}.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(entries);
	}

	/**
	 * Two mappings are equal, if they have equal keys with equal values, regardless of the order.
	 */
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
			return Objects.equals(entries, other.entries);
		}
	}

	/**
	 * Reorders the entries by their scalar keys: null keys first, then booleans, numbers and other
	 * values in ascending order. Complex keys are sorted like null keys.
	 *
	 * @param ascending
	 *            true for ascending, false for descending order
	 * @return this mapping for chaining
	 */
	public YamlMapping sortKeys(final boolean ascending) {
		final List<Map.Entry<YamlNode, YamlNode>> sortedEntries = new ArrayList<>(entries.entrySet());

		sortedEntries.sort((a, b) -> {
			final Object aKey = getScalarValue(a.getKey());
			final Object bKey = getScalarValue(b.getKey());

			if (aKey == null && bKey == null) {
				return 0;
			} else if (aKey == null) {
				return ascending ? -1 : 1;
			} else if (bKey == null) {
				return ascending ? 1 : -1;
			} else {
				return compareKeys(aKey, bKey, ascending);
			}
		});

		entries.clear();
		for (final Map.Entry<YamlNode, YamlNode> entry : sortedEntries) {
			entries.put(entry.getKey(), entry.getValue());
		}

		return this;
	}

	/**
	 * Returns the simple value of a scalar key.
	 *
	 * @param node
	 *            the key
	 * @return the value, or null for the null scalar and complex keys
	 */
	private static Object getScalarValue(final YamlNode node) {
		if (!(node instanceof YamlScalar)) {
			return null;
		} else {
			final YamlScalar scalar = (YamlScalar) node;
			if (scalar.getType() == YamlScalarType.NULL_VALUE) {
				return null;
			} else {
				return scalar.getValue();
			}
		}
	}

	/**
	 * Compares two simple key values.
	 *
	 * @param a
	 *            the first key value
	 * @param b
	 *            the second key value
	 * @param ascending
	 *            true for ascending, false for descending order
	 * @return the comparison result
	 */
	private static int compareKeys(final Object a, final Object b, final boolean ascending) {
		final int result = YamlSequence.compareSimpleValues(a, b);
		return ascending ? result : -result;
	}

	/**
	 * Checks whether this mapping has no entries.
	 *
	 * @return true, if the mapping is empty
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Adds the entries of another mapping (no deep merge). Keys existing in both mappings are
	 * handled by the strategy.
	 *
	 * @param other
	 *            the mapping to merge, may be null
	 * @param strategy
	 *            KEEP_EXISTING to keep the existing values, OVERWRITE to replace them
	 * @return this mapping for chaining
	 */
	public YamlMapping merge(final YamlMapping other, final YamlMappingMergeStrategy strategy) {
		if (other == null) {
			return this;
		} else {
			for (final Map.Entry<YamlNode, YamlNode> entry : new ArrayList<>(other.entrySet())) {
				if (!entries.containsKey(entry.getKey())) {
					entries.put(entry.getKey(), entry.getValue());
				} else if (strategy == YamlMappingMergeStrategy.OVERWRITE) {
					entries.put(entry.getKey(), entry.getValue());
				} else {
					// KEEP_EXISTING
				}
			}
			return this;
		}
	}
}
