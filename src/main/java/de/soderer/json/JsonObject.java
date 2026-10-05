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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.exception.JsonInvalidValueTypeException;
import de.soderer.json.exception.MissingKeyException;
import de.soderer.json.utilities.DateUtilities;

/**
 * JSON object with properties in insertion order.
 * <p>
 * Values of Java types are converted: null to {@link JsonValueNull}, Integer and Long to
 * {@link JsonValueInteger}, other numbers to {@link JsonValueNumber}, and date and time values to
 * ISO 8601 strings. There are three ways to set a property: add (key must be new), replace (key
 * must exist, position is kept) and put (adds or replaces). Iterating returns the properties with
 * simple Java values (see {@link #simpleEntrySet()}).
 * </p>
 */
public class JsonObject extends JsonNode implements Iterable<Map.Entry<String, Object>> {
	/**
	 * The properties in insertion order, never containing Java null values.
	 */
	private final LinkedHashMap<String, JsonNode> properties = new LinkedHashMap<>();

	/**
	 * Creates a new empty JSON object.
	 */
	public JsonObject() {
		super(JsonDataType.OBJECT);
	}

	/**
	 * Adds a new property with JSON null value.
	 *
	 * @param key
	 *            the property key, must not exist yet
	 * @return this object for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject addNull(final String key) throws DuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new DuplicateKeyException(key);
		} else {
			properties.put(key, new JsonValueNull());
			return this;
		}
	}

	/**
	 * Adds a new property with a string.
	 *
	 * @param key
	 *            the property key, must not exist yet
	 * @param value
	 *            the value, null adds JSON null
	 * @return this object for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject add(final String key, final String value) throws DuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new DuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else {
				properties.put(key, new JsonValueString(value));
			}
			return this;
		}
	}

	/**
	 * Adds a new property with a integer.
	 *
	 * @param key
	 *            the property key, must not exist yet
	 * @param value
	 *            the value, null adds JSON null
	 * @return this object for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject add(final String key, final Integer value) throws DuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new DuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else {
				properties.put(key, new JsonValueInteger(value));
			}
			return this;
		}
	}

	/**
	 * Adds a new property with a integer.
	 *
	 * @param key
	 *            the property key, must not exist yet
	 * @param value
	 *            the value, null adds JSON null
	 * @return this object for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject add(final String key, final Long value) throws DuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new DuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else {
				properties.put(key, new JsonValueInteger(value));
			}
			return this;
		}
	}

	/**
	 * Adds a new property with a number (Integer and Long become integer values).
	 *
	 * @param key
	 *            the property key, must not exist yet
	 * @param value
	 *            the value, null adds JSON null
	 * @return this object for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject add(final String key, final Number value) throws DuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new DuplicateKeyException(key);
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

	/**
	 * Adds a new property with a boolean.
	 *
	 * @param key
	 *            the property key, must not exist yet
	 * @param value
	 *            the value, null adds JSON null
	 * @return this object for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject add(final String key, final Boolean value) throws DuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new DuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else {
				properties.put(key, new JsonValueBoolean(value));
			}
			return this;
		}
	}

	/**
	 * Adds a new property with a date time as ISO 8601 string with time zone.
	 *
	 * @param key
	 *            the property key, must not exist yet
	 * @param value
	 *            the value, null adds JSON null
	 * @return this object for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject add(final String key, final Date value) throws DuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new DuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else {
				properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, value)));
			}
			return this;
		}
	}

	/**
	 * Adds a new property with a date as ISO 8601 string.
	 *
	 * @param key
	 *            the property key, must not exist yet
	 * @param value
	 *            the value, null adds JSON null
	 * @return this object for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject add(final String key, final LocalDate value) throws DuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new DuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else {
				properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, value)));
			}
			return this;
		}
	}

	/**
	 * Adds a new property with a date time as ISO 8601 string without time zone, with fraction of second if not 0.
	 *
	 * @param key
	 *            the property key, must not exist yet
	 * @param value
	 *            the value, null adds JSON null
	 * @return this object for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject add(final String key, final LocalDateTime value) throws DuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new DuplicateKeyException(key);
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

	/**
	 * Adds a new property with a date time as ISO 8601 string with time zone, with fraction of second if not 0.
	 *
	 * @param key
	 *            the property key, must not exist yet
	 * @param value
	 *            the value, null adds JSON null
	 * @return this object for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject add(final String key, final ZonedDateTime value) throws DuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new DuplicateKeyException(key);
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

	/**
	 * Adds a new property with a JSON node.
	 *
	 * @param key
	 *            the property key, must not exist yet
	 * @param value
	 *            the value, null adds JSON null
	 * @return this object for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject add(final String key, final JsonNode value) throws DuplicateKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new DuplicateKeyException(key);
		} else {
			// A null node is stored as JSON null value, the property map must not contain Java null values
			properties.put(key, value == null ? new JsonValueNull() : value);
			return this;
		}
	}

	/**
	 * Adds a new property with a value of any supported type: String, Number, Boolean, Date, LocalDate, LocalDateTime, ZonedDateTime or JsonNode.
	 *
	 * @param key
	 *            the property key, must not exist yet
	 * @param value
	 *            the value, null adds JSON null
	 * @return this object for chaining
	 * @throws DuplicateKeyException
	 *             if the key already exists
	 * @throws JsonInvalidValueTypeException
	 *             if the value type is not supported
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject add(final String key, final Object value) throws DuplicateKeyException, JsonInvalidValueTypeException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			throw new DuplicateKeyException(key);
		} else {
			if (value == null) {
				addNull(key);
			} else if (value instanceof String) {
				add(key, (String) value);
			} else if (value instanceof Integer) {
				add(key, (Integer) value);
			} else if (value instanceof Long) {
				add(key, (Long) value);
			} else if (value instanceof Number) {
				add(key, (Number) value);
			} else if (value instanceof Boolean) {
				add(key, (Boolean) value);
			} else if (value instanceof Date) {
				add(key, (Date) value);
			} else if (value instanceof LocalDate) {
				add(key, (LocalDate) value);
			} else if (value instanceof LocalDateTime) {
				add(key, (LocalDateTime) value);
			} else if (value instanceof ZonedDateTime) {
				add(key, (ZonedDateTime) value);
			} else if (value instanceof JsonNode) {
				add(key, (JsonNode) value);
			} else {
				throw new JsonInvalidValueTypeException(value.getClass());
			}
			return this;
		}
	}

	/**
	 * Replaces the value of an existing property by JSON null, keeping the position of the property.
	 *
	 * @param key
	 *            the property key, must exist
	 * @return this object for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject replaceNull(final String key) throws MissingKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			properties.put(key, new JsonValueNull());
			return this;
		} else {
			throw new MissingKeyException(key);
		}
	}

	/**
	 * Replaces the value of an existing property by a string, keeping the position of the
	 * property.
	 *
	 * @param key
	 *            the property key, must exist
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject replace(final String key, final String value) throws MissingKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			if (value == null) {
				replaceNull(key);
			} else {
				properties.put(key, new JsonValueString(value));
			}
			return this;
		} else {
			throw new MissingKeyException(key);
		}
	}

	/**
	 * Replaces the value of an existing property by a integer, keeping the position of the
	 * property.
	 *
	 * @param key
	 *            the property key, must exist
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject replace(final String key, final Integer value) throws MissingKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			if (value == null) {
				replaceNull(key);
			} else {
				properties.put(key, new JsonValueInteger(value));
			}
			return this;
		} else {
			throw new MissingKeyException(key);
		}
	}

	/**
	 * Replaces the value of an existing property by a integer, keeping the position of the
	 * property.
	 *
	 * @param key
	 *            the property key, must exist
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject replace(final String key, final Long value) throws MissingKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			if (value == null) {
				replaceNull(key);
			} else {
				properties.put(key, new JsonValueInteger(value));
			}
			return this;
		} else {
			throw new MissingKeyException(key);
		}
	}

	/**
	 * Replaces the value of an existing property by a number (Integer and Long become integer values), keeping the position of the
	 * property.
	 *
	 * @param key
	 *            the property key, must exist
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject replace(final String key, final Number value) throws MissingKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			if (value == null) {
				replaceNull(key);
			} else if (value instanceof Integer) {
				properties.put(key, new JsonValueInteger((Integer) value));
			} else if (value instanceof Long) {
				properties.put(key, new JsonValueInteger((Long) value));
			} else {
				properties.put(key, new JsonValueNumber(value));
			}
			return this;
		} else {
			throw new MissingKeyException(key);
		}
	}

	/**
	 * Replaces the value of an existing property by a boolean, keeping the position of the
	 * property.
	 *
	 * @param key
	 *            the property key, must exist
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject replace(final String key, final Boolean value) throws MissingKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			if (value == null) {
				replaceNull(key);
			} else {
				properties.put(key, new JsonValueBoolean(value));
			}
			return this;
		} else {
			throw new MissingKeyException(key);
		}
	}

	/**
	 * Replaces the value of an existing property by a date time as ISO 8601 string with time zone, keeping the position of the
	 * property.
	 *
	 * @param key
	 *            the property key, must exist
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject replace(final String key, final Date value) throws MissingKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			if (value == null) {
				replaceNull(key);
			} else {
				properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, value)));
			}
			return this;
		} else {
			throw new MissingKeyException(key);
		}
	}

	/**
	 * Replaces the value of an existing property by a date as ISO 8601 string, keeping the position of the
	 * property.
	 *
	 * @param key
	 *            the property key, must exist
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject replace(final String key, final LocalDate value) throws MissingKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			if (value == null) {
				replaceNull(key);
			} else {
				properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, value)));
			}
			return this;
		} else {
			throw new MissingKeyException(key);
		}
	}

	/**
	 * Replaces the value of an existing property by a date time as ISO 8601 string without time zone, with fraction of second if not 0, keeping the position of the
	 * property.
	 *
	 * @param key
	 *            the property key, must exist
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject replace(final String key, final LocalDateTime value) throws MissingKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			if (value == null) {
				replaceNull(key);
			} else {
				if (value.getNano() > 0) {
					properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT_NO_TIMEZONE, value)));
				} else {
					properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, value)));
				}
			}
			return this;
		} else {
			throw new MissingKeyException(key);
		}
	}

	/**
	 * Replaces the value of an existing property by a date time as ISO 8601 string with time zone, with fraction of second if not 0, keeping the position of the
	 * property.
	 *
	 * @param key
	 *            the property key, must exist
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject replace(final String key, final ZonedDateTime value) throws MissingKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			if (value == null) {
				replaceNull(key);
			} else {
				if (value.getNano() > 0) {
					properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT, value)));
				} else {
					properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, value)));
				}
			}
			return this;
		} else {
			throw new MissingKeyException(key);
		}
	}

	/**
	 * Replaces the value of an existing property by a JSON node, keeping the position of the
	 * property.
	 *
	 * @param key
	 *            the property key, must exist
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject replace(final String key, final JsonNode value) throws MissingKeyException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			// A null node is stored as JSON null value, the property map must not contain Java null values
			properties.put(key, value == null ? new JsonValueNull() : value);
			return this;
		} else {
			throw new MissingKeyException(key);
		}
	}

	/**
	 * Replaces the value of an existing property by a value of any supported type: String, Number, Boolean, Date, LocalDate, LocalDateTime, ZonedDateTime or JsonNode, keeping the position of the
	 * property.
	 *
	 * @param key
	 *            the property key, must exist
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws MissingKeyException
	 *             if the key does not exist
	 * @throws JsonInvalidValueTypeException
	 *             if the value type is not supported
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject replace(final String key, final Object value) throws MissingKeyException, JsonInvalidValueTypeException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else if (properties.containsKey(key)) {
			if (value == null) {
				replaceNull(key);
			} else if (value instanceof String) {
				replace(key, (String) value);
			} else if (value instanceof Integer) {
				replace(key, (Integer) value);
			} else if (value instanceof Long) {
				replace(key, (Long) value);
			} else if (value instanceof Number) {
				replace(key, (Number) value);
			} else if (value instanceof Boolean) {
				replace(key, (Boolean) value);
			} else if (value instanceof Date) {
				replace(key, (Date) value);
			} else if (value instanceof LocalDate) {
				replace(key, (LocalDate) value);
			} else if (value instanceof LocalDateTime) {
				replace(key, (LocalDateTime) value);
			} else if (value instanceof ZonedDateTime) {
				replace(key, (ZonedDateTime) value);
			} else if (value instanceof JsonNode) {
				replace(key, (JsonNode) value);
			} else {
				throw new JsonInvalidValueTypeException(value.getClass());
			}
			return this;
		} else {
			throw new MissingKeyException(key);
		}
	}

	/**
	 * Sets a property to JSON null, adding it if it does not exist yet.
	 *
	 * @param key
	 *            the property key
	 * @return this object for chaining
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject putNull(final String key) {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else {
			properties.put(key, new JsonValueNull());
			return this;
		}
	}

	/**
	 * Sets a property to a string, adding it if it does not exist yet.
	 *
	 * @param key
	 *            the property key
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject put(final String key, final String value) {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else {
			if (value == null) {
				putNull(key);
			} else {
				properties.put(key, new JsonValueString(value));
			}
			return this;
		}
	}

	/**
	 * Sets a property to a integer, adding it if it does not exist yet.
	 *
	 * @param key
	 *            the property key
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject put(final String key, final Integer value) {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else {
			if (value == null) {
				putNull(key);
			} else {
				properties.put(key, new JsonValueInteger(value));
			}
			return this;
		}
	}

	/**
	 * Sets a property to a integer, adding it if it does not exist yet.
	 *
	 * @param key
	 *            the property key
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject put(final String key, final Long value) {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else {
			if (value == null) {
				putNull(key);
			} else {
				properties.put(key, new JsonValueInteger(value));
			}
			return this;
		}
	}

	/**
	 * Sets a property to a number (Integer and Long become integer values), adding it if it does not exist yet.
	 *
	 * @param key
	 *            the property key
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject put(final String key, final Number value) {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else {
			if (value == null) {
				putNull(key);
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

	/**
	 * Sets a property to a boolean, adding it if it does not exist yet.
	 *
	 * @param key
	 *            the property key
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject put(final String key, final Boolean value) {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else {
			if (value == null) {
				putNull(key);
			} else {
				properties.put(key, new JsonValueBoolean(value));
			}
			return this;
		}
	}

	/**
	 * Sets a property to a date time as ISO 8601 string with time zone, adding it if it does not exist yet.
	 *
	 * @param key
	 *            the property key
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject put(final String key, final Date value) {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else {
			if (value == null) {
				putNull(key);
			} else {
				properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, value)));
			}
			return this;
		}
	}

	/**
	 * Sets a property to a date as ISO 8601 string, adding it if it does not exist yet.
	 *
	 * @param key
	 *            the property key
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject put(final String key, final LocalDate value) {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else {
			if (value == null) {
				putNull(key);
			} else {
				properties.put(key, new JsonValueString(DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, value)));
			}
			return this;
		}
	}

	/**
	 * Sets a property to a date time as ISO 8601 string without time zone, with fraction of second if not 0, adding it if it does not exist yet.
	 *
	 * @param key
	 *            the property key
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject put(final String key, final LocalDateTime value) {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else {
			if (value == null) {
				putNull(key);
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

	/**
	 * Sets a property to a date time as ISO 8601 string with time zone, with fraction of second if not 0, adding it if it does not exist yet.
	 *
	 * @param key
	 *            the property key
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject put(final String key, final ZonedDateTime value) {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else {
			if (value == null) {
				putNull(key);
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

	/**
	 * Sets a property to a JSON node, adding it if it does not exist yet.
	 *
	 * @param key
	 *            the property key
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject put(final String key, final JsonNode value) {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else {
			// A null node is stored as JSON null value, the property map must not contain Java null values
			properties.put(key, value == null ? new JsonValueNull() : value);
			return this;
		}
	}

	/**
	 * Sets a property to a value of any supported type: String, Number, Boolean, Date, LocalDate, LocalDateTime, ZonedDateTime or JsonNode, adding it if it does not exist yet.
	 *
	 * @param key
	 *            the property key
	 * @param value
	 *            the value, null sets JSON null
	 * @return this object for chaining
	 * @throws JsonInvalidValueTypeException
	 *             if the value type is not supported
	 * @throws RuntimeException
	 *             if the key is null
	 */
	public JsonObject put(final String key, final Object value) throws JsonInvalidValueTypeException {
		if (key == null) {
			throw new RuntimeException("Invalid null value for JsonObject property key");
		} else {
			if (value == null) {
				putNull(key);
			} else if (value instanceof String) {
				put(key, (String) value);
			} else if (value instanceof Integer) {
				put(key, (Integer) value);
			} else if (value instanceof Long) {
				put(key, (Long) value);
			} else if (value instanceof Number) {
				put(key, (Number) value);
			} else if (value instanceof Boolean) {
				put(key, (Boolean) value);
			} else if (value instanceof Date) {
				put(key, (Date) value);
			} else if (value instanceof LocalDate) {
				put(key, (LocalDate) value);
			} else if (value instanceof LocalDateTime) {
				put(key, (LocalDateTime) value);
			} else if (value instanceof ZonedDateTime) {
				put(key, (ZonedDateTime) value);
			} else if (value instanceof JsonNode) {
				put(key, (JsonNode) value);
			} else {
				throw new JsonInvalidValueTypeException(value.getClass());
			}
			return this;
		}
	}

	/**
	 * Removes a property.
	 *
	 * @param key
	 *            the property key
	 * @return the removed value, or null if the property did not exist
	 */
	public JsonNode remove(final String key) {
		return properties.remove(key);
	}

	/**
	 * Returns the value of a property.
	 *
	 * @param key
	 *            the property key
	 * @return the value, a {@link JsonValueNull} for JSON null, or null if the property does not exist
	 */
	public JsonNode get(final String key) {
		return properties.get(key);
	}

	/**
	 * Returns the value of a property as simple Java value.
	 *
	 * @param key
	 *            the property key
	 * @return the String, Number or Boolean value, the JsonObject or JsonArray itself, or null for
	 *         JSON null and missing properties
	 */
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
			return value;
		}
	}

	/**
	 * Checks whether a property exists.
	 *
	 * @param key
	 *            the property key
	 * @return true, if the property exists, even with JSON null value
	 */
	public boolean containsKey(final String key) {
		return properties.containsKey(key);
	}

	/**
	 * Returns the property keys in insertion order.
	 *
	 * @return an unmodifiable view of the keys
	 */
	public Set<String> keySet() {
		return Collections.unmodifiableSet(properties.keySet());
	}

	/**
	 * Returns the property values in insertion order.
	 *
	 * @return an unmodifiable view of the values
	 */
	public Collection<JsonNode> values () {
		return Collections.unmodifiableCollection(properties.values());
	}

	/**
	 * Returns the property values as simple Java values, see {@link #getSimpleValue(String)}.
	 *
	 * @return an unmodifiable copy of the simple values
	 */
	public Collection<Object> simpleValues() {
		final List<Object> simpleValues = new ArrayList<>();
		for (final JsonNode value : properties.values()) {
			if (value.isNull()) {
				simpleValues.add(null);
			} else if (value.isString()) {
				simpleValues.add(((JsonValueString) value).getValue());
			} else if (value.isInteger()) {
				simpleValues.add(((JsonValueInteger) value).getValue());
			} else if (value.isNumber()) {
				simpleValues.add(((JsonValueNumber) value).getValue());
			} else if (value.isBoolean()) {
				simpleValues.add(((JsonValueBoolean) value).getValue());
			} else {
				simpleValues.add(value);
			}
		}
		return Collections.unmodifiableCollection(simpleValues);
	}

	/**
	 * Returns the properties in insertion order.
	 *
	 * @return an unmodifiable view of the properties
	 */
	public Set<Entry<String, JsonNode>> entrySet() {
		return Collections.unmodifiableSet(properties.entrySet());
	}

	/**
	 * Returns the properties with simple Java values, see {@link #getSimpleValue(String)}.
	 *
	 * @return an unmodifiable copy of the properties
	 */
	public Set<Entry<String, Object>> simpleEntrySet() {
		final LinkedHashMap<String, Object> simpleProperties = new LinkedHashMap<>();
		for (final Entry<String, JsonNode> entry : properties.entrySet()) {
			if (entry.getValue().isNull()) {
				simpleProperties.put(entry.getKey(), null);
			} else if (entry.getValue().isString()) {
				simpleProperties.put(entry.getKey(), ((JsonValueString) entry.getValue()).getValue());
			} else if (entry.getValue().isInteger()) {
				simpleProperties.put(entry.getKey(), ((JsonValueInteger) entry.getValue()).getValue());
			} else if (entry.getValue().isNumber()) {
				simpleProperties.put(entry.getKey(), ((JsonValueNumber) entry.getValue()).getValue());
			} else if (entry.getValue().isBoolean()) {
				simpleProperties.put(entry.getKey(), ((JsonValueBoolean) entry.getValue()).getValue());
			} else {
				simpleProperties.put(entry.getKey(), entry.getValue());
			}
		}
		return Collections.unmodifiableSet(simpleProperties.entrySet());
	}

	/**
	 * Returns the number of properties.
	 *
	 * @return the number of properties
	 */
	public int size() {
		return properties.size();
	}

	/**
	 * Iterates over the properties with simple Java values, see {@link #simpleEntrySet()}.
	 */
	@Override
	public Iterator<Entry<String, Object>> iterator() {
		return simpleEntrySet().iterator();
	}

	/**
	 * Returns this object as formatted JSON text.
	 */
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

	/**
	 * Two objects are equal, if they have the same keys with equal values, regardless of the order.
	 */
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

	/**
	 * Reorders the properties alphabetically by key.
	 *
	 * @param ascending
	 *            true for ascending, false for descending order
	 * @return this object for chaining
	 */
	public JsonObject sortKeys(final boolean ascending) {
		final List<String> sortedKeys = new ArrayList<>(properties.keySet());
		if (ascending) {
			Collections.sort(sortedKeys);
		} else {
			sortedKeys.sort(Collections.reverseOrder());
		}

		final List<Map.Entry<String, JsonNode>> sortedEntries = new ArrayList<>();
		for (final String key : sortedKeys) {
			sortedEntries.add(Map.entry(key, properties.get(key)));
		}

		properties.clear();
		for (final Map.Entry<String, JsonNode> entry : sortedEntries) {
			properties.put(entry.getKey(), entry.getValue());
		}

		return this;
	}

	/**
	 * Checks whether this object has no properties.
	 *
	 * @return true, if the object is empty
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Adds the properties of another object. Properties existing in both objects are handled by the
	 * strategy.
	 *
	 * @param other
	 *            the object to merge, may be null
	 * @param strategy
	 *            KEEP_EXISTING to keep the existing values, OVERWRITE to replace them
	 * @return this object for chaining
	 */
	public JsonObject merge(final JsonObject other, final JsonObjectMergeStrategy strategy) {
		if (other == null) {
			return this;
		} else {
			for (final Map.Entry<String, JsonNode> entry : other.entrySet()) {
				if (!properties.containsKey(entry.getKey())) {
					properties.put(entry.getKey(), entry.getValue());
				} else if (strategy == JsonObjectMergeStrategy.OVERWRITE) {
					properties.put(entry.getKey(), entry.getValue());
				} else {
					// KEEP_EXISTING
				}
			}
			return this;
		}
	}
}
