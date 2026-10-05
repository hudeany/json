package de.soderer.json;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import de.soderer.json.utilities.DateUtilities;
import de.soderer.json.utilities.NumberUtilities;

/**
 * JSON array of JSON nodes.
 * <p>
 * Values of Java types are converted on adding: null to {@link JsonValueNull}, Integer and Long to
 * {@link JsonValueInteger}, other numbers to {@link JsonValueNumber}, and date and time values to
 * ISO 8601 strings. Iterating returns the simple Java values of the items (see
 * {@link #simpleItems()}).
 * </p>
 */
public class JsonArray extends JsonNode implements Iterable<Object> {
	/**
	 * The items of this array.
	 */
	private final List<JsonNode> items = new ArrayList<>();

	/**
	 * Creates a new empty JSON array.
	 */
	public JsonArray() {
		super(JsonDataType.ARRAY);
	}

	/**
	 * Appends a JSON null value.
	 *
	 * @return this array for chaining
	 */
	public JsonArray addNull() {
		items.add(new JsonValueNull());
		return this;
	}

	/**
	 * Appends a string.
	 *
	 * @param value
	 *            the value, null appends a JSON null value
	 * @return this array for chaining
	 */
	public JsonArray add(final String value) {
		if (value == null) {
			addNull();
		} else {
			add(new JsonValueString(value));
		}
		return this;
	}

	/**
	 * Appends a integer.
	 *
	 * @param value
	 *            the value, null appends a JSON null value
	 * @return this array for chaining
	 */
	public JsonArray add(final Integer value) {
		if (value == null) {
			addNull();
		} else {
			add(new JsonValueInteger(value));
		}
		return this;
	}

	/**
	 * Appends a integer.
	 *
	 * @param value
	 *            the value, null appends a JSON null value
	 * @return this array for chaining
	 */
	public JsonArray add(final Long value) {
		if (value == null) {
			addNull();
		} else {
			add(new JsonValueInteger(value));
		}
		return this;
	}

	/**
	 * Appends a number (Integer and Long become integer values).
	 *
	 * @param value
	 *            the value, null appends a JSON null value
	 * @return this array for chaining
	 */
	public JsonArray add(final Number value) {
		if (value == null) {
			addNull();
		} else if (value instanceof Integer) {
			add(new JsonValueInteger((Integer) value));
		} else if (value instanceof Long) {
			add(new JsonValueInteger((Long) value));
		} else {
			add(new JsonValueNumber(value));
		}
		return this;
	}

	/**
	 * Appends a boolean.
	 *
	 * @param value
	 *            the value, null appends a JSON null value
	 * @return this array for chaining
	 */
	public JsonArray add(final Boolean value) {
		if (value == null) {
			addNull();
		} else {
			add(new JsonValueBoolean(value));
		}
		return this;
	}

	/**
	 * Appends a date time as ISO 8601 string with time zone.
	 *
	 * @param value
	 *            the value, null appends a JSON null value
	 * @return this array for chaining
	 */
	public JsonArray add(final Date value) {
		if (value == null) {
			addNull();
		} else {
			add(new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, value)));
		}
		return this;
	}

	/**
	 * Appends a date as ISO 8601 string.
	 *
	 * @param value
	 *            the value, null appends a JSON null value
	 * @return this array for chaining
	 */
	public JsonArray add(final LocalDate value) {
		if (value == null) {
			addNull();
		} else {
			add(new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, value)));
		}
		return this;
	}

	/**
	 * Appends a date time as ISO 8601 string without time zone, with fraction of second if not 0.
	 *
	 * @param value
	 *            the value, null appends a JSON null value
	 * @return this array for chaining
	 */
	public JsonArray add(final LocalDateTime value) {
		if (value == null) {
			addNull();
		} else {
			if (value.getNano() > 0) {
				add(new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT_NO_TIMEZONE, value)));
			} else {
				add(new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, value)));
			}
		}
		return this;
	}

	/**
	 * Appends a date time as ISO 8601 string with time zone, with fraction of second if not 0.
	 *
	 * @param value
	 *            the value, null appends a JSON null value
	 * @return this array for chaining
	 */
	public JsonArray add(final ZonedDateTime value) {
		if (value == null) {
			addNull();
		} else {
			if (value.getNano() > 0) {
				add(new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT, value)));
			} else {
				add(new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, value)));
			}
		}
		return this;
	}

	/**
	 * Appends a JSON node.
	 *
	 * @param value
	 *            the value, null appends a JSON null value
	 * @return this array for chaining
	 */
	public JsonArray add(final JsonNode value) {
		if (value == null) {
			addNull();
		} else {
			items.add(value);
		}
		return this;
	}

	/**
	 * Inserts a JSON null value.
	 *
	 * @param index
	 *            the position to insert at
	 * @return this array for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public JsonArray insertNull(final int index) {
		items.add(index, new JsonValueNull());
		return this;
	}

	/**
	 * Inserts a string.
	 *
	 * @param index
	 *            the position to insert at
	 * @param value
	 *            the value, null inserts a JSON null value
	 * @return this array for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public JsonArray insert(final int index, final String value) {
		if (value == null) {
			insertNull(index);
		} else {
			insert(index, new JsonValueString(value));
		}
		return this;
	}

	/**
	 * Inserts a integer.
	 *
	 * @param index
	 *            the position to insert at
	 * @param value
	 *            the value, null inserts a JSON null value
	 * @return this array for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public JsonArray insert(final int index, final Integer value) {
		if (value == null) {
			insertNull(index);
		} else {
			insert(index, new JsonValueInteger(value));
		}
		return this;
	}

	/**
	 * Inserts a integer.
	 *
	 * @param index
	 *            the position to insert at
	 * @param value
	 *            the value, null inserts a JSON null value
	 * @return this array for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public JsonArray insert(final int index, final Long value) {
		if (value == null) {
			insertNull(index);
		} else {
			insert(index, new JsonValueInteger(value));
		}
		return this;
	}

	/**
	 * Inserts a number (Integer and Long become integer values).
	 *
	 * @param index
	 *            the position to insert at
	 * @param value
	 *            the value, null inserts a JSON null value
	 * @return this array for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public JsonArray insert(final int index, final Number value) {
		if (value == null) {
			insertNull(index);
		} else if (value instanceof Integer) {
			insert(index, new JsonValueInteger((Integer) value));
		} else if (value instanceof Long) {
			insert(index, new JsonValueInteger((Long) value));
		} else {
			insert(index, new JsonValueNumber(value));
		}
		return this;
	}

	/**
	 * Inserts a boolean.
	 *
	 * @param index
	 *            the position to insert at
	 * @param value
	 *            the value, null inserts a JSON null value
	 * @return this array for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public JsonArray insert(final int index, final Boolean value) {
		if (value == null) {
			insertNull(index);
		} else {
			insert(index, new JsonValueBoolean(value));
		}
		return this;
	}

	/**
	 * Inserts a date time as ISO 8601 string with time zone.
	 *
	 * @param index
	 *            the position to insert at
	 * @param value
	 *            the value, null inserts a JSON null value
	 * @return this array for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public JsonArray insert(final int index, final Date value) {
		if (value == null) {
			insertNull(index);
		} else {
			insert(index, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, value)));
		}
		return this;
	}

	/**
	 * Inserts a date as ISO 8601 string.
	 *
	 * @param index
	 *            the position to insert at
	 * @param value
	 *            the value, null inserts a JSON null value
	 * @return this array for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public JsonArray insert(final int index, final LocalDate value) {
		if (value == null) {
			insertNull(index);
		} else {
			insert(index, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, value)));
		}
		return this;
	}

	/**
	 * Inserts a date time as ISO 8601 string without time zone, with fraction of second if not 0.
	 *
	 * @param index
	 *            the position to insert at
	 * @param value
	 *            the value, null inserts a JSON null value
	 * @return this array for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public JsonArray insert(final int index, final LocalDateTime value) {
		if (value == null) {
			insertNull(index);
		} else {
			if (value.getNano() > 0) {
				insert(index, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT_NO_TIMEZONE, value)));
			} else {
				insert(index, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, value)));
			}
		}
		return this;
	}

	/**
	 * Inserts a date time as ISO 8601 string with time zone, with fraction of second if not 0.
	 *
	 * @param index
	 *            the position to insert at
	 * @param value
	 *            the value, null inserts a JSON null value
	 * @return this array for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public JsonArray insert(final int index, final ZonedDateTime value) {
		if (value == null) {
			insertNull(index);
		} else {
			if (value.getNano() > 0) {
				insert(index, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT, value)));
			} else {
				insert(index, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, value)));
			}
		}
		return this;
	}

	/**
	 * Inserts a JSON node.
	 *
	 * @param index
	 *            the position to insert at
	 * @param value
	 *            the value, null inserts a JSON null value
	 * @return this array for chaining
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public JsonArray insert(final int index, final JsonNode value) {
		if (value == null) {
			insertNull(index);
		} else {
			items.add(index, value);
		}
		return this;
	}

	/**
	 * Removes the first JSON null value.
	 *
	 * @return true, if a value was removed
	 */
	public boolean removeNull() {
		return items.remove(new JsonValueNull());
	}

	/**
	 * Removes the first item equal to the given value. Numbers are compared numerically within
	 * integer and within non-integer values.
	 *
	 * @param value
	 *            the value, null removes a JSON null value
	 * @return true, if an item was removed
	 */
	public boolean remove(final String value) {
		if (value == null) {
			return removeNull();
		} else {
			return items.remove(new JsonValueString(value));
		}
	}

	/**
	 * Removes the first item equal to the given value. Numbers are compared numerically within
	 * integer and within non-integer values.
	 *
	 * @param value
	 *            the value, null removes a JSON null value
	 * @return true, if an item was removed
	 */
	public boolean remove(final Integer value) {
		if (value == null) {
			return removeNull();
		} else {
			return items.remove(new JsonValueInteger(value));
		}
	}

	/**
	 * Removes the first item equal to the given value. Numbers are compared numerically within
	 * integer and within non-integer values.
	 *
	 * @param value
	 *            the value, null removes a JSON null value
	 * @return true, if an item was removed
	 */
	public boolean remove(final Long value) {
		if (value == null) {
			return removeNull();
		} else {
			return items.remove(new JsonValueInteger(value));
		}
	}

	/**
	 * Removes the first item equal to the given value. Numbers are compared numerically within
	 * integer and within non-integer values.
	 *
	 * @param value
	 *            the value, null removes a JSON null value
	 * @return true, if an item was removed
	 */
	public boolean remove(final Number value) {
		if (value == null) {
			return removeNull();
		} else if (value instanceof Integer) {
			return items.remove(new JsonValueInteger((Integer) value));
		} else if (value instanceof Long) {
			return items.remove(new JsonValueInteger((Long) value));
		} else {
			return items.remove(new JsonValueNumber(value));
		}
	}

	/**
	 * Removes the first item equal to the given value. Numbers are compared numerically within
	 * integer and within non-integer values.
	 *
	 * @param value
	 *            the value, null removes a JSON null value
	 * @return true, if an item was removed
	 */
	public boolean remove(final Boolean value) {
		if (value == null) {
			return removeNull();
		} else {
			return items.remove(new JsonValueBoolean(value));
		}
	}

	/**
	 * Removes the first item equal to the given value. Numbers are compared numerically within
	 * integer and within non-integer values.
	 *
	 * @param value
	 *            the value, null removes a JSON null value
	 * @return true, if an item was removed
	 */
	public boolean remove(final JsonNode value) {
		if (value == null) {
			return removeNull();
		} else {
			return items.remove(value);
		}
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
	public JsonNode removeByIndex(final int index) {
		return items.remove(index);
	}

	/**
	 * Checks whether this array contains a JSON null value.
	 *
	 * @return true, if a JSON null value is contained
	 */
	public boolean containsNull() {
		return items.contains(new JsonValueNull());
	}

	/**
	 * Checks whether this array contains an item equal to the given value. Numbers are compared
	 * numerically within integer and within non-integer values.
	 *
	 * @param value
	 *            the value, null checks for a JSON null value
	 * @return true, if such an item is contained
	 */
	public boolean contains(final String value) {
		if (value == null) {
			return containsNull();
		} else {
			return items.contains(new JsonValueString(value));
		}
	}

	/**
	 * Checks whether this array contains an item equal to the given value. Numbers are compared
	 * numerically within integer and within non-integer values.
	 *
	 * @param value
	 *            the value, null checks for a JSON null value
	 * @return true, if such an item is contained
	 */
	public boolean contains(final Integer value) {
		if (value == null) {
			return containsNull();
		} else {
			return items.contains(new JsonValueInteger(value));
		}
	}

	/**
	 * Checks whether this array contains an item equal to the given value. Numbers are compared
	 * numerically within integer and within non-integer values.
	 *
	 * @param value
	 *            the value, null checks for a JSON null value
	 * @return true, if such an item is contained
	 */
	public boolean contains(final Long value) {
		if (value == null) {
			return containsNull();
		} else {
			return items.contains(new JsonValueInteger(value));
		}
	}

	/**
	 * Checks whether this array contains an item equal to the given value. Numbers are compared
	 * numerically within integer and within non-integer values.
	 *
	 * @param value
	 *            the value, null checks for a JSON null value
	 * @return true, if such an item is contained
	 */
	public boolean contains(final Number value) {
		if (value == null) {
			return containsNull();
		} else if (value instanceof Integer) {
			return items.contains(new JsonValueInteger((Integer) value));
		} else if (value instanceof Long) {
			return items.contains(new JsonValueInteger((Long) value));
		} else {
			return items.contains(new JsonValueNumber(value));
		}
	}

	/**
	 * Checks whether this array contains an item equal to the given value. Numbers are compared
	 * numerically within integer and within non-integer values.
	 *
	 * @param value
	 *            the value, null checks for a JSON null value
	 * @return true, if such an item is contained
	 */
	public boolean contains(final Boolean value) {
		if (value == null) {
			return containsNull();
		} else {
			return items.contains(new JsonValueBoolean(value));
		}
	}

	/**
	 * Checks whether this array contains an item equal to the given value. Numbers are compared
	 * numerically within integer and within non-integer values.
	 *
	 * @param value
	 *            the value, null checks for a JSON null value
	 * @return true, if such an item is contained
	 */
	public boolean contains(final JsonNode value) {
		if (value == null) {
			return containsNull();
		} else {
			return items.contains(value);
		}
	}

	/**
	 * Returns the item at a position.
	 *
	 * @param index
	 *            the position
	 * @return the item, a {@link JsonValueNull} for null values
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public JsonNode get(final int index) {
		return items.get(index);
	}

	/**
	 * Returns the item at a position as simple Java value.
	 *
	 * @param index
	 *            the position
	 * @return the String, Number or Boolean value, null for JSON null, or the JsonObject or
	 *         JsonArray itself
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public Object getSimpleItem(final int index) {
		final Object item = get(index);
		if (item == null || item instanceof JsonValueNull) {
			return null;
		} else if (item instanceof JsonValueString) {
			return ((JsonValueString) item).getValue();
		} else if (item instanceof JsonValueInteger) {
			return ((JsonValueInteger) item).getValue();
		} else if (item instanceof JsonValueNumber) {
			return ((JsonValueNumber) item).getValue();
		} else if (item instanceof JsonValueBoolean) {
			return ((JsonValueBoolean) item).getValue();
		} else {
			return item;
		}
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
	public Collection<JsonNode> items() {
		return Collections.unmodifiableCollection(items);
	}

	/**
	 * Returns the items as simple Java values: String, Number or Boolean, null for JSON null, and
	 * JsonObject or JsonArray for nested items.
	 *
	 * @return an unmodifiable copy of the simple values
	 */
	public Collection<Object> simpleItems() {
		final List<Object> simpleItems = new ArrayList<>();
		for (final JsonNode item : items) {
			if (item.isNull()) {
				simpleItems.add(null);
			} else if (item.isString()) {
				simpleItems.add(((JsonValueString) item).getValue());
			} else if (item.isInteger()) {
				simpleItems.add(((JsonValueInteger) item).getValue());
			} else if (item.isNumber()) {
				simpleItems.add(((JsonValueNumber) item).getValue());
			} else if (item.isBoolean()) {
				simpleItems.add(((JsonValueBoolean) item).getValue());
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
	 * Returns this array as formatted JSON text.
	 */
	@Override
	public String toString() {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
				JsonWriter writer = new JsonWriter(output, StandardCharsets.UTF_8);) {
			writer.add(this);
			writer.flush();
			return new String(output.toByteArray(), StandardCharsets.UTF_8);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Two arrays are equal, if they contain equal items in the same order.
	 */
	@Override
	public boolean equals(final Object otherObject) {
		if (this == otherObject) {
			return true;
		} else if (otherObject != null && otherObject instanceof JsonArray) {
			final JsonArray otherArray = (JsonArray) otherObject;
			if (size() != otherArray.size()) {
				return false;
			} else {
				for (int i = 0; i < size(); i++) {
					final Object thisValue = get(i);
					final Object otherValue = otherArray.get(i);
					if ((thisValue != otherValue)
							&& (thisValue != null && !thisValue.equals(otherValue))) {
						return false;
					}
				}
				return true;
			}
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((items == null) ? 0 : items.hashCode());
		return result;
	}

	/**
	 * Sorts the items by their simple values: strings alphabetically, numbers numerically, false
	 * before true, and nulls first in ascending order.
	 *
	 * @param ascending
	 *            true for ascending, false for descending order
	 * @return this array for chaining
	 */
	public JsonArray sort(final boolean ascending) {
		items.sort((a, b) -> {
			if (a instanceof JsonValueNull && b instanceof JsonValueNull) {
				return 0;
			} else if (a instanceof JsonValueNull) {
				return ascending ? -1 : 1;
			} else if (b instanceof JsonValueNull) {
				return ascending ? 1 : -1;
			} else {
				return compareValues(getSimpleValue(a), getSimpleValue(b), ascending);
			}
		});
		return this;
	}

	/**
	 * Sorts object items by the value of a property, see {@link #sort(boolean)}. Items that are no
	 * objects or miss the property are sorted like null values.
	 *
	 * @param attributeName
	 *            the property name
	 * @param ascending
	 *            true for ascending, false for descending order
	 * @return this array for chaining
	 */
	public JsonArray sortByAttribute(final String attributeName, final boolean ascending) {
		items.sort((a, b) -> {
			// Missing values and non-object items are treated as null consistently, as required by the Comparator contract
			final JsonNode aNode = a instanceof JsonObject ? ((JsonObject) a).get(attributeName) : null;
			final JsonNode bNode = b instanceof JsonObject ? ((JsonObject) b).get(attributeName) : null;
			return compareValues(getSimpleValue(aNode), getSimpleValue(bNode), ascending);
		});
		return this;
	}

	/**
	 * Returns the simple Java value of a node.
	 *
	 * @param node
	 *            the node, may be null
	 * @return the String, Number or Boolean value, or null for null, JSON null, objects and arrays
	 */
	private static Object getSimpleValue(final JsonNode node) {
		if (node instanceof JsonValueString) {
			return ((JsonValueString) node).getValue();
		} else if (node instanceof JsonValueInteger) {
			return ((JsonValueInteger) node).getValue();
		} else if (node instanceof JsonValueNumber) {
			return ((JsonValueNumber) node).getValue();
		} else if (node instanceof JsonValueBoolean) {
			return ((JsonValueBoolean) node).getValue();
		} else {
			return null;
		}
	}

	/**
	 * Compares two simple values for sorting, nulls first in ascending order.
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
		// Order by type first (null, boolean, number, string), so that mixed types are ordered transitively
		final int typeRankComparison = Integer.compare(getTypeRank(a), getTypeRank(b));
		final int result;
		if (typeRankComparison != 0) {
			result = typeRankComparison;
		} else if (a == null) {
			result = 0;
		} else if (a instanceof Boolean) {
			result = Boolean.compare((Boolean) a, (Boolean) b);
		} else if (a instanceof Number) {
			result = NumberUtilities.compare((Number) a, (Number) b);
		} else {
			result = a.toString().compareTo(b.toString());
		}
		return ascending ? result : -result;
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
	 * Checks whether this array has no items.
	 *
	 * @return true, if the array is empty
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Appends the items of another array.
	 *
	 * @param other
	 *            the array to merge, may be null
	 * @param strategy
	 *            APPEND_ALL to append all items, SKIP_DUPLICATES to append only items not contained yet
	 * @return this array for chaining
	 */
	public JsonArray merge(final JsonArray other, final JsonArrayMergeStrategy strategy) {
		if (other == null) {
			return this;
		} else {
			for (final JsonNode item : other.items()) {
				if (strategy == JsonArrayMergeStrategy.APPEND_ALL) {
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
