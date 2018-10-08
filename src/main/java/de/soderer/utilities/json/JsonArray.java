package de.soderer.utilities.json;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonArray implements Iterable<Object> {
	private List<Object> items = new ArrayList<Object>();

	public void add(Object value) {
		items.add(value);
	}

	public Object remove(Object value) {
		return items.remove(value);
	}

	public Object get(int index) {
		return items.get(index);
	}

	public int size() {
		return items.size();
	}

	@Override
	public Iterator<Object> iterator() {
		return items.iterator();
	}

	@Override
	public String toString() {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
			JsonWriter writer = new JsonWriter(output, "UTF-8");) {
			writer.add(this);
			writer.flush();
			return new String(output.toByteArray(), "UTF-8");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean equals(Object otherObject) {
		if (this == otherObject) {
			return true;
		} else if (otherObject != null && otherObject instanceof JsonArray) {
			JsonArray otherArray = (JsonArray) otherObject;
			if (this.size() != otherArray.size()) {
				return false;
			} else {
				for (int i = 0; i < this.size(); i++) {
					Object thisValue = this.get(i);
					Object otherValue = otherArray.get(i);
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
}
