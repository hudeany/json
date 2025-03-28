package de.soderer.yaml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YamlMapping extends YamlObject<YamlMapping> implements Iterable<YamlMappingEntry> {
	private final List<YamlMappingEntry> mappingEntries = new ArrayList<>();

	public YamlMapping add(YamlMappingEntry yamlMappingEntry) throws Exception {
		if (keySet().contains(yamlMappingEntry.getKey())) {
			throw new Exception("Duplicate mapping key: " + yamlMappingEntry.getKey());
		} else {
			mappingEntries.add(yamlMappingEntry);
			return this;
		}
	}
	
	public YamlMapping add(final String mappingKey, final YamlObject<?> object) throws Exception {
		if (keySet().contains(mappingKey)) {
			throw new Exception("Duplicate mapping key: " + mappingKey);
		} else {
			mappingEntries.add(new YamlMappingEntry().setKey(mappingKey).setValue(object));
			return this;
		}
	}

	public Object remove(final String mappingKey) {
		for (final YamlMappingEntry entry : mappingEntries) {
			if (mappingKey != null && mappingKey.equals(entry.getKey())) {
				mappingEntries.remove(entry);
				return entry;
			}
		}
		return null;
	}

	public Object get(final String mappingKey) {
		for (final YamlMappingEntry entry : mappingEntries) {
			if (mappingKey != null && mappingKey.equals(entry.getKey())) {
				return entry;
			}
		}
		return null;
	}

	public boolean containsMappingKey(final String mappingKey) {
		for (final YamlMappingEntry entry : mappingEntries) {
			if (mappingKey != null && mappingKey.equals(entry.getKey())) {
				return true;
			}
		}
		return false;
	}

	public Set<String> keySet() {
		return mappingEntries.stream().map(x -> (String) x.getKey()).collect(Collectors.toSet());
	}

	public Set<Object> values() {
		return mappingEntries.stream().map(x -> x.getKey()).collect(Collectors.toSet());
	}

	public Set<YamlMappingEntry> entrySet() {
		return new LinkedHashSet<>(mappingEntries);
	}

	public int size() {
		return mappingEntries.size();
	}

	@Override
	public Iterator<YamlMappingEntry> iterator() {
		return entrySet().iterator();
	}

	public Stream<YamlMappingEntry> entriesStream () {
		return entrySet().stream();
	}

	public Stream<String> keysStream () {
		return keySet().stream();
	}

	public Stream<Object> valuesStream () {
		return values().stream();
	}

	@Override
	public boolean equals(final Object other) {
		if (this == other) {
			return true;
		} else if (other != null && other instanceof YamlMapping) {
			final YamlMapping otherObject = (YamlMapping) other;
			if (size() != otherObject.size()) {
				return false;
			} else {
				for (final YamlMappingEntry mappingEntry : entrySet()) {
					final Object thisValue = mappingEntry.getValue();
					final Object otherValue = otherObject.get((String) mappingEntry.getKey());
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
		result = prime * result + ((mappingEntries == null) ? 0 : mappingEntries.hashCode());
		return result;
	}
}
