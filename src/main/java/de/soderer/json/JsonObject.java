package de.soderer.json;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.soderer.json.exception.JsonDuplicateKeyException;
import de.soderer.json.utilities.DateUtilities;

public class JsonObject extends JsonNode implements Iterable<Map.Entry<String, JsonNode>> {
	private final LinkedHashMap<String, JsonNode> properties = new LinkedHashMap<>();

	public JsonObject() {
		super(JsonDataType.OBJECT);
	}

	public JsonObject addNull(final String key) throws JsonDuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new JsonDuplicateKeyException(key);
		} else {
			properties.put(key, new JsonValueNull());
			return this;
		}
	}

	public JsonObject add(final String key, final String value) throws JsonDuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new JsonDuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else {
				properties.put(key, new JsonValueString(value));
			}
			return this;
		}
	}

	public JsonObject add(final String key, final Integer value) throws JsonDuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new JsonDuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else {
				properties.put(key, new JsonValueInteger(value));
			}
			return this;
		}
	}

	public JsonObject add(final String key, final Long value) throws JsonDuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new JsonDuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else {
				properties.put(key, new JsonValueInteger(value));
			}
			return this;
		}
	}

	public JsonObject add(final String key, final Number value) throws JsonDuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new JsonDuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else if (value instanceof Integer) {
				properties.put(key, new JsonValueInteger((Integer) value));
			} else if (value instanceof Long) {
				properties.put(key, new JsonValueInteger((Long) value));
			} else {
				properties.put(key, new JsonValueNumber(value));
			}
			return this;
		}
	}

	public JsonObject add(final String key, final Boolean value) throws JsonDuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new JsonDuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else {
				properties.put(key, new JsonValueBoolean(value));
			}
			return this;
		}
	}

	public JsonObject add(final String key, final Date value) throws JsonDuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new JsonDuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else {
				properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, value)));
			}
			return this;
		}
	}

	public JsonObject add(final String key, final LocalDate value) throws JsonDuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new JsonDuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else {
				properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, value)));
			}
			return this;
		}
	}

	public JsonObject add(final String key, final LocalDateTime value) throws JsonDuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new JsonDuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else {
				if (value.getNano() > 0) {
					properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT_NO_TIMEZONE, value)));
				} else {
					properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, value)));
				}
			}
			return this;
		}
	}

	public JsonObject add(final String key, final ZonedDateTime value) throws JsonDuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new JsonDuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else {
				if (value.getNano() > 0) {
					properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT, value)));
				} else {
					properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, value)));
				}
			}
			return this;
		}
	}

	public JsonObject add(final String key, final JsonNode value) throws JsonDuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new JsonDuplicateKeyException(key);
		} else {
			properties.put(key, value);
			return this;
		}
	}

	public JsonNode remove(final String key) {
		return properties.remove(key);
	}

	public JsonNode get(final String key) {
		return properties.get(key);
	}

	public Object getSimpleValue(final String key) {
		final Object value = get(key);
		if (value == null || value instanceof JsonValueNull) {
			return null;
		} else if (value instanceof JsonValueString) {
			return ((JsonValueString) value).getValue();
		} else if (value instanceof JsonValueInteger) {
			return ((JsonValueInteger) value).getValue();
		} else if (value instanceof JsonValueNumber) {
			return ((JsonValueNumber) value).getValue();
		} else if (value instanceof JsonValueBoolean) {
			return ((JsonValueBoolean) value).getValue();
		} else {
			throw new RuntimeException("Selected value for property key '" + key + "' is not a simple value: '" + value.getClass().getSimpleName() + "'");
		}
	}

	public boolean containsKey(final String key) {
		return properties.containsKey(key);
	}

	public Set<String> keySet() {
		return Collections.unmodifiableSet(properties.keySet());
	}

	public Collection<Object> values () {
		return Collections.unmodifiableCollection(properties.values());
	}

	public Set<Entry<String, JsonNode>> entrySet() {
		return Collections.unmodifiableSet(properties.entrySet());
	}

	public int size() {
		return properties.size();
	}

	@Override
	public Iterator<Entry<String, JsonNode>> iterator() {
		return properties.entrySet().iterator();
	}

	@Override
	public String toString() {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream(); JsonWriter writer = new JsonWriter(output, StandardCharsets.UTF_8);) {
			writer.add(this);
			writer.flush();
			return new String(output.toByteArray(), StandardCharsets.UTF_8);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean equals(final Object other) {
		if (this == other) {
			return true;
		} else if (other != null && other instanceof JsonObject) {
			final JsonObject otherObject = (JsonObject) other;
			if (size() != otherObject.size()) {
				return false;
			} else {
				for (final Entry<String, JsonNode> propertyEntry : entrySet()) {
					final Object thisValue = propertyEntry.getValue();
					final Object otherValue = otherObject.get(propertyEntry.getKey());
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
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
		return result;
	}
}
