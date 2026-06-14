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

public class JsonArray extends JsonNode implements Iterable<Object> {
	private final List<JsonNode> items = new ArrayList<>();

	public JsonArray() {
		super(JsonDataType.ARRAY);
	}

	public JsonArray addNull() {
		items.add(new JsonValueNull());
		return this;
	}

	public JsonArray add(final String value) {
		if (value == null) {
			addNull();
		} else {
			add(new JsonValueString(value));
		}
		return this;
	}

	public JsonArray add(final Integer value) {
		if (value == null) {
			addNull();
		} else {
			add(new JsonValueInteger(value));
		}
		return this;
	}

	public JsonArray add(final Long value) {
		if (value == null) {
			addNull();
		} else {
			add(new JsonValueInteger(value));
		}
		return this;
	}

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

	public JsonArray add(final Boolean value) {
		if (value == null) {
			addNull();
		} else {
			add(new JsonValueBoolean(value));
		}
		return this;
	}

	public JsonArray add(final Date value) {
		if (value == null) {
			addNull();
		} else {
			add(new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, value)));
		}
		return this;
	}

	public JsonArray add(final LocalDate value) {
		if (value == null) {
			addNull();
		} else {
			add(new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, value)));
		}
		return this;
	}

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

	public JsonArray add(final JsonNode value) {
		if (value == null) {
			addNull();
		} else {
			items.add(value);
		}
		return this;
	}

	public JsonArray insertNull(final int index) {
		items.add(index, new JsonValueNull());
		return this;
	}

	public JsonArray insert(final int index, final String value) {
		if (value == null) {
			insertNull(index);
		} else {
			insert(index, new JsonValueString(value));
		}
		return this;
	}

	public JsonArray insert(final int index, final Integer value) {
		if (value == null) {
			insertNull(index);
		} else {
			insert(index, new JsonValueInteger(value));
		}
		return this;
	}

	public JsonArray insert(final int index, final Long value) {
		if (value == null) {
			insertNull(index);
		} else {
			insert(index, new JsonValueInteger(value));
		}
		return this;
	}

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

	public JsonArray insert(final int index, final Boolean value) {
		if (value == null) {
			insertNull(index);
		} else {
			insert(index, new JsonValueBoolean(value));
		}
		return this;
	}

	public JsonArray insert(final int index, final Date value) {
		if (value == null) {
			insertNull(index);
		} else {
			insert(index, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, value)));
		}
		return this;
	}

	public JsonArray insert(final int index, final LocalDate value) {
		if (value == null) {
			insertNull(index);
		} else {
			insert(index, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, value)));
		}
		return this;
	}

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

	public JsonArray insert(final int index, final JsonNode value) {
		if (value == null) {
			insertNull(index);
		} else {
			items.add(index, value);
		}
		return this;
	}

	public boolean removeNull() {
		return items.remove(new JsonValueNull());
	}

	public boolean remove(final String value) {
		if (value == null) {
			return removeNull();
		} else {
			return items.remove(new JsonValueString(value));
		}
	}

	public boolean remove(final Integer value) {
		if (value == null) {
			return removeNull();
		} else {
			return items.remove(new JsonValueInteger(value));
		}
	}

	public boolean remove(final Long value) {
		if (value == null) {
			return removeNull();
		} else {
			return items.remove(new JsonValueInteger(value));
		}
	}

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

	public boolean remove(final Boolean value) {
		if (value == null) {
			return removeNull();
		} else {
			return items.remove(new JsonValueBoolean(value));
		}
	}

	public boolean remove(final JsonNode value) {
		if (value == null) {
			return removeNull();
		} else {
			return items.remove(value);
		}
	}

	public JsonNode removeByIndex(final int index) {
		return items.remove(index);
	}

	public boolean containsNull() {
		return items.contains(new JsonValueNull());
	}

	public boolean contains(final String value) {
		if (value == null) {
			return containsNull();
		} else {
			return items.contains(new JsonValueString(value));
		}
	}

	public boolean contains(final Integer value) {
		if (value == null) {
			return containsNull();
		} else {
			return items.contains(new JsonValueInteger(value));
		}
	}

	public boolean contains(final Long value) {
		if (value == null) {
			return containsNull();
		} else {
			return items.contains(new JsonValueInteger(value));
		}
	}

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

	public boolean contains(final Boolean value) {
		if (value == null) {
			return containsNull();
		} else {
			return items.contains(new JsonValueBoolean(value));
		}
	}

	public boolean contains(final JsonNode value) {
		if (value == null) {
			return containsNull();
		} else {
			return items.contains(value);
		}
	}

	public JsonNode get(final int index) {
		return items.get(index);
	}

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

	public int size() {
		return items.size();
	}

	public Collection<JsonNode> items() {
		return Collections.unmodifiableCollection(items);
	}

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

	@Override
	public Iterator<Object> iterator() {
		return simpleItems().iterator();
	}

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

	public JsonArray sortByAttribute(final String attributeName, final boolean ascending) {
		items.sort((a, b) -> {
			if (!(a instanceof JsonObject) || !(b instanceof JsonObject)) {
				return 0;
			} else {
				final JsonNode aNode = ((JsonObject) a).get(attributeName);
				final JsonNode bNode = ((JsonObject) b).get(attributeName);

				if (aNode == null || aNode instanceof JsonValueNull) {
					return ascending ? -1 : 1;
				}
				if (bNode == null || bNode instanceof JsonValueNull) {
					return ascending ? 1 : -1;
				}

				return compareValues(getSimpleValue(aNode), getSimpleValue(bNode), ascending);
			}
		});
		return this;
	}

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

	private static int compareValues(final Object a, final Object b, final boolean ascending) {
		if (a == null && b == null) {
			return 0;
		} else if (a == null) {
			return ascending ? -1 : 1;
		} else if (b == null) {
			return ascending ? 1 : -1;
		} else {
			final int result;
			if (a instanceof String && b instanceof String) {
				result = ((String) a).compareTo((String) b);
			} else if (a instanceof Number && b instanceof Number) {
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
