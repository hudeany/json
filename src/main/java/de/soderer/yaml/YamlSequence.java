package de.soderer.yaml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class YamlSequence extends YamlObject<YamlSequence> implements Iterable<YamlObject<?>> {
	private final List<YamlObject<?>> items = new ArrayList<>();

	public YamlSequence add(final YamlObject<?> value) {
		items.add(value);
		return this;
	}

	public Object remove(final Object value) {
		for (final YamlObject<?> item : items) {
			if (value != null && value.equals(item.getValue())) {
				items.remove(item);
				return item;
			}
		}
		return null;
	}

	public Object get(final int index) {
		return items.get(index);
	}

	public int size() {
		return items.size();
	}

	@Override
	public Iterator<YamlObject<?>> iterator() {
		return items.iterator();
	}

	public Stream<YamlObject<?>> stream() {
		return items.stream();
	}

	@Override
	public boolean equals(final Object otherObject) {
		if (this == otherObject) {
			return true;
		} else if (otherObject != null && otherObject instanceof YamlSequence) {
			final YamlSequence otherArray = (YamlSequence) otherObject;
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
