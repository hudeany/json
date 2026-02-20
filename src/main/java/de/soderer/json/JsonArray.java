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

/**
 * TODO: add "insert", "removeByIndex"
 */
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
}
