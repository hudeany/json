package de.soderer.utilities.json;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class JsonObject implements Iterable<Map.Entry<String, Object>> {
	private final Map<String, Object> properties = new LinkedHashMap<>();

	/**
	 * When using the same key multiple times only the last value will be stored
	 *
	 * @param key
	 * @param object
	 */
	public void add(final String key, final Object object) {
		properties.put(key, object);
	}

	public Object remove(final String key) {
		return properties.remove(key);
	}

	public Object get(final String key) {
		return properties.get(key);
	}

	public boolean containsPropertyKey(final String propertyKey) {
		return properties.containsKey(propertyKey);
	}

	public Set<String> keySet() {
		return properties.keySet();
	}

	public Set<Entry<String, Object>> entrySet() {
		return properties.entrySet();
	}

	public int size() {
		return properties.size();
	}

	@Override
	public Iterator<Entry<String, Object>> iterator() {
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
				for (final Entry<String, Object> propertyEntry : entrySet()) {
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
