package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import de.soderer.json.utilities.NumberUtilities;
import de.soderer.yaml.YamlWriter;

/**
 * YAML sequence of nodes.
 * <p>
 * Java values are converted to scalars on adding, null to the null scalar. Iterating returns the
 * simple Java values of the items, see {@link #simpleItems()}.
 * </p>
 */
public class YamlSequence extends YamlNode implements Iterable<Object> {
	/**
	 * The items.
	 */
	private final List<YamlNode> items = new ArrayList<>();

	/**
	 * True to write this sequence in flow style ("[1, 2]").
	 */
	private boolean flowStyle;

	/**
	 * Creates an empty sequence in block style.
	 */
	public YamlSequence() {
		this(false);
	}

	/**
	 * Creates an empty sequence.
	 *
	 * @param flowStyle
	 *            true for flow style ("[1, 2]"), false for block style
	 */
	public YamlSequence(final boolean flowStyle) {
		this.flowStyle = flowStyle;
	}

	/**
	 * Returns whether this sequence is written in flow style.
	 *
	 * @return true for flow style
	 */
	public boolean isFlowStyle() {
		return flowStyle;
	}

	/**
	 * Sets whether this sequence is written in flow style.
	 *
	 * @param flowStyle
	 *            true for flow style, false for block style
	 */
	public void setFlowStyle(final boolean flowStyle) {
		this.flowStyle = flowStyle;
	}

	/**
	 * Sets whether this sequence is written in flow style.
	 *
	 * @param newFlowStyle
	 *            true for flow style, false for block style
	 * @return this sequence for chaining
	 */
	public YamlSequence withFlowStyle(final boolean newFlowStyle) {
		setFlowStyle(newFlowStyle);
		return this;
	}

	/**
	 * Appends the null value.
	 *
	 * @return this sequence for chaining
	 */
	public YamlSequence addNull() {
		add(new YamlScalar(null));
		return this;
	}

	/**
	 * Appends a string.
	 *
	 * @param item
	 *            the item, null for the null value
	 * @return this sequence for chaining
	 */
	public YamlSequence add(final String item) {
		if (item == null) {
			add(new YamlScalar(null));
		} else {
			add(new YamlScalar(item, YamlScalarType.STRING));
		}
		return this;
	}

	/**
	 * Appends a number.
	 *
	 * @param item
	 *            the item, null for the null value
	 * @return this sequence for chaining
	 */
	public YamlSequence add(final Number item) {
		if (item == null) {
			add(new YamlScalar(null));
		} else {
			add(new YamlScalar(item, YamlScalarType.NUMBER));
		}
		return this;
	}

	/**
	 * Appends a boolean.
	 *
	 * @param item
	 *            the item, null for the null value
	 * @return this sequence for chaining
	 */
	public YamlSequence add(final Boolean item) {
		if (item == null) {
			add(new YamlScalar(null));
		} else {
			add(new YamlScalar(item, YamlScalarType.BOOLEAN));
		}
		return this;
	}

	/**
	 * Appends a node.
	 *
	 * @param item
	 *            the item
	 * @return this sequence for chaining
	 */
	public YamlSequence add(final YamlNode item) {
		items.add(item);
		return this;
	}

	/**
	 * Inserts the null value.
	 *
	 * @param index
	 *            the position
	 * @return this sequence for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public YamlSequence insertNull(final int index) {
		insert(index, new YamlScalar(null));
		return this;
	}

	/**
	 * Inserts a string.
	 *
	 * @param index
	 *            the position
	 * @param item
	 *            the item, null for the null value
	 * @return this sequence for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public YamlSequence insert(final int index, final String item) {
		if (item == null) {
			insert(index, new YamlScalar(null));
		} else {
			insert(index, new YamlScalar(item, YamlScalarType.STRING));
		}
		return this;
	}

	/**
	 * Inserts a number.
	 *
	 * @param index
	 *            the position
	 * @param item
	 *            the item, null for the null value
	 * @return this sequence for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public YamlSequence insert(final int index, final Number item) {
		if (item == null) {
			insert(index, new YamlScalar(null));
		} else {
			insert(index, new YamlScalar(item, YamlScalarType.NUMBER));
		}
		return this;
	}

	/**
	 * Inserts a boolean.
	 *
	 * @param index
	 *            the position
	 * @param item
	 *            the item, null for the null value
	 * @return this sequence for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public YamlSequence insert(final int index, final Boolean item) {
		if (item == null) {
			insert(index, new YamlScalar(null));
		} else {
			insert(index, new YamlScalar(item, YamlScalarType.BOOLEAN));
		}
		return this;
	}

	/**
	 * Inserts a node.
	 *
	 * @param index
	 *            the position
	 * @param item
	 *            the item
	 * @return this sequence for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public YamlSequence insert(final int index, final YamlNode item) {
		items.add(index, item);
		return this;
	}

	/**
	 * Replaces the item at a position by the null value.
	 *
	 * @param index
	 *            the position
	 * @return this sequence for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public YamlSequence setNull(final int index) {
		set(index, new YamlScalar(null));
		return this;
	}

	/**
	 * Replaces the item at a position by a string.
	 *
	 * @param index
	 *            the position
	 * @param item
	 *            the item, null for the null value
	 * @return this sequence for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public YamlSequence set(final int index, final String item) {
		if (item == null) {
			set(index, new YamlScalar(null));
		} else {
			set(index, new YamlScalar(item, YamlScalarType.STRING));
		}
		return this;
	}

	/**
	 * Replaces the item at a position by a number.
	 *
	 * @param index
	 *            the position
	 * @param item
	 *            the item, null for the null value
	 * @return this sequence for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public YamlSequence set(final int index, final Number item) {
		if (item == null) {
			set(index, new YamlScalar(null));
		} else {
			set(index, new YamlScalar(item, YamlScalarType.NUMBER));
		}
		return this;
	}

	/**
	 * Replaces the item at a position by a boolean.
	 *
	 * @param index
	 *            the position
	 * @param item
	 *            the item, null for the null value
	 * @return this sequence for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public YamlSequence set(final int index, final Boolean item) {
		if (item == null) {
			set(index, new YamlScalar(null));
		} else {
			set(index, new YamlScalar(item, YamlScalarType.BOOLEAN));
		}
		return this;
	}

	/**
	 * Replaces the item at a position by a node.
	 *
	 * @param index
	 *            the position
	 * @param item
	 *            the item
	 * @return this sequence for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public YamlSequence set(final int index, final YamlNode item) {
		items.set(index, item);
		return this;
	}

	/**
	 * Removes the first item equal to the given string.
	 *
	 * @param item
	 *            the item, null for the null value
	 * @return true, if an item was removed
	 */
	public boolean remove(final String item) {
		if (item == null) {
			return remove(new YamlScalar(null));
		} else {
			return remove(new YamlScalar(item, YamlScalarType.STRING));
		}
	}

	/**
	 * Removes the first item equal to the given number.
	 *
	 * @param item
	 *            the item, null for the null value
	 * @return true, if an item was removed
	 */
	public boolean remove(final Number item) {
		if (item == null) {
			return remove(new YamlScalar(null));
		} else {
			return remove(new YamlScalar(item, YamlScalarType.NUMBER));
		}
	}

	/**
	 * Removes the first item equal to the given boolean.
	 *
	 * @param item
	 *            the item, null for the null value
	 * @return true, if an item was removed
	 */
	public boolean remove(final Boolean item) {
		if (item == null) {
			return remove(new YamlScalar(null));
		} else {
			return remove(new YamlScalar(item, YamlScalarType.BOOLEAN));
		}
	}

	/**
	 * Removes the first item equal to the given node.
	 *
	 * @param item
	 *            the item
	 * @return true, if an item was removed
	 */
	public boolean remove(final YamlNode item) {
		return items.remove(item);
	}

	/**
	 * Removes the item at a position.
	 *
	 * @param index
	 *            the position
	 * @return the removed item
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public YamlNode removeByIndex(final int index) {
		return items.remove(index);
	}

	/**
	 * Returns the item at a position.
	 *
	 * @param index
	 *            the position
	 * @return the item
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public YamlNode get(final int index) {
		return items.get(index);
	}

	/**
	 * Returns the number of items.
	 *
	 * @return the number of items
	 */
	public int size() {
		return items.size();
	}

	/**
	 * Returns the items.
	 *
	 * @return an unmodifiable view of the items
	 */
	public Collection<YamlNode> items() {
		return Collections.unmodifiableCollection(items);
	}

	/**
	 * Returns the items as simple Java values: String, Number or Boolean for scalars, null for the
	 * null value, and the node itself for other nodes.
	 *
	 * @return an unmodifiable copy of the simple values
	 */
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

	/**
	 * Iterates over the simple Java values of the items, see {@link #simpleItems()}.
	 */
	@Override
	public Iterator<Object> iterator() {
		return simpleItems().iterator();
	}

	/**
	 * Returns this sequence as YAML text in default format.
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
		return Objects.hash(items);
	}

	/**
	 * Two sequences are equal, if they contain equal items in the same order.
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
			final YamlSequence other = (YamlSequence) otherObject;
			return Objects.equals(items, other.items);
		}
	}

	/**
	 * Sorts the items by their simple values: null first, then booleans, numbers and other values in
	 * ascending order. Mappings and sequences are sorted like null values.
	 *
	 * @param ascending
	 *            true for ascending, false for descending order
	 * @return this sequence for chaining
	 */
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

	/**
	 * Sorts mapping items by the value of a string key, see {@link #sort(boolean)}. Items that are no
	 * mappings or miss the key are sorted like null values.
	 *
	 * @param attributeName
	 *            the key
	 * @param ascending
	 *            true for ascending, false for descending order
	 * @return this sequence for chaining
	 */
	public YamlSequence sortByAttribute(final String attributeName, final boolean ascending) {
		items.sort((a, b) -> {
			// Missing values and non-mapping items are treated as null consistently, as required by the Comparator contract
			final YamlNode aNode = a instanceof YamlMapping ? ((YamlMapping) a).get(attributeName) : null;
			final YamlNode bNode = b instanceof YamlMapping ? ((YamlMapping) b).get(attributeName) : null;
			return compareValues(getSimpleValue(aNode), getSimpleValue(bNode), ascending);
		});
		return this;
	}

	/**
	 * Checks for the null scalar.
	 *
	 * @param node
	 *            the node
	 * @return true, if the node is a scalar of type NULL_VALUE
	 */
	private static boolean isNullScalar(final YamlNode node) {
		return node instanceof YamlScalar && ((YamlScalar) node).getType() == YamlScalarType.NULL_VALUE;
	}

	/**
	 * Returns the simple Java value of a node for sorting.
	 *
	 * @param node
	 *            the node, may be null
	 * @return the scalar value, or null for null, the null scalar and other nodes
	 */
	private static Object getSimpleValue(final YamlNode node) {
		if (isNullScalar(node)) {
			return null;
		} else if (node instanceof YamlScalar) {
			return ((YamlScalar) node).getValue();
		} else {
			return null;
		}
	}

	/**
	 * Compares two simple values for sorting.
	 *
	 * @param a
	 *            the first value
	 * @param b
	 *            the second value
	 * @param ascending
	 *            true for ascending, false for descending order
	 * @return the comparison result
	 */
	private static int compareValues(final Object a, final Object b, final boolean ascending) {
		final int result = compareSimpleValues(a, b);
		return ascending ? result : -result;
	}

	/**
	 * Compares two simple values in ascending order: by type first (null, boolean, number, other),
	 * then by value. This order is transitive also for mixed types, as required by sorting.
	 *
	 * @param a
	 *            the first value
	 * @param b
	 *            the second value
	 * @return the comparison result
	 */
	static int compareSimpleValues(final Object a, final Object b) {
		// Order by type first (null, boolean, number, other), so that mixed types are ordered transitively
		final int typeRankComparison = Integer.compare(getTypeRank(a), getTypeRank(b));
		if (typeRankComparison != 0) {
			return typeRankComparison;
		} else if (a == null) {
			return 0;
		} else if (a instanceof Boolean) {
			return Boolean.compare((Boolean) a, (Boolean) b);
		} else if (a instanceof Number) {
			return NumberUtilities.compare((Number) a, (Number) b);
		} else {
			return a.toString().compareTo(b.toString());
		}
	}

	/**
	 * Returns the sort rank of the type of a simple value.
	 *
	 * @param value
	 *            the simple value
	 * @return 0 for null, 1 for Boolean, 2 for Number, 3 for other values
	 */
	private static int getTypeRank(final Object value) {
		if (value == null) {
			return 0;
		} else if (value instanceof Boolean) {
			return 1;
		} else if (value instanceof Number) {
			return 2;
		} else {
			return 3;
		}
	}

	/**
	 * Checks whether this sequence has no items.
	 *
	 * @return true, if the sequence is empty
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Appends the items of another sequence.
	 *
	 * @param other
	 *            the sequence to merge, may be null
	 * @param strategy
	 *            APPEND_ALL to append all items, SKIP_DUPLICATES to append only items not contained yet
	 * @return this sequence for chaining
	 */
	public YamlSequence merge(final YamlSequence other, final YamlSequenceMergeStrategy strategy) {
		if (other == null) {
			return this;
		} else {
			for (final YamlNode item : new ArrayList<>(other.items())) {
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
