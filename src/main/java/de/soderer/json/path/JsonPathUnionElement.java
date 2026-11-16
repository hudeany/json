package de.soderer.json.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Bracket selection of a JSON path, which is not a single property key, a single non-negative
 * array index or a wildcard: negative array indexes counted from the end ("[-1]"), array slices
 * ("[1:3]", "[::2]", "[-2:]") and unions of several property keys, indexes and slices
 * ("[0,2]", "['name','version']").
 * <p>
 * The selectors are applied in the given order, so a union may select a node several times. Only
 * meaningful with {@link de.soderer.json.JsonNode#getDataListByJsonPath}, except a single index,
 * which is also supported by {@link de.soderer.json.JsonNode#getDataByJsonPath}.
 * </p>
 */
public class JsonPathUnionElement implements JsonPathElement {
	/**
	 * One selector of a union: a property key, an array index or an array slice.
	 */
	public interface Selector {
		// Marker interface
	}

	/**
	 * Selects the value of a property key of an object.
	 *
	 * @param propertyKey
	 *            the property key
	 */
	public record NameSelector(String propertyKey) implements Selector {
		/**
		 * Creates a property key selector.
		 *
		 * @param propertyKey
		 *            the property key, must not be null
		 */
		public NameSelector {
			Objects.requireNonNull(propertyKey, "propertyKey");
		}

		@Override
		public String toString() {
			return "'" + JsonPath.escapeQuotedText(propertyKey, '\'') + "'";
		}
	}

	/**
	 * Selects an array item by index; a negative index counts from the end ("-1" is the last item).
	 *
	 * @param index
	 *            the array index
	 */
	public record IndexSelector(int index) implements Selector {
		@Override
		public String toString() {
			return Integer.toString(index);
		}
	}

	/**
	 * Selects array items by a slice "start:end:step" as defined by RFC 9535: start is included,
	 * end is excluded, negative values count from the end, a negative step selects in reverse
	 * order.
	 *
	 * @param start
	 *            the first index, or null for the default (0, or the last item for a negative step)
	 * @param end
	 *            the index after the last selected item, or null for the default (behind the last
	 *            item, or before the first item for a negative step)
	 * @param step
	 *            the step width, or null for 1; a step of 0 selects nothing
	 */
	public record SliceSelector(Integer start, Integer end, Integer step) implements Selector {
		/**
		 * Returns the indexes selected by this slice for an array of the given size.
		 *
		 * @param arraySize
		 *            the size of the array
		 * @return the selected indexes in selection order
		 */
		public List<Integer> getIndexes(final int arraySize) {
			final int stepValue = step == null ? 1 : step;
			if (stepValue == 0) {
				return Collections.emptyList();
			}
			final List<Integer> indexes = new ArrayList<>();
			if (stepValue > 0) {
				final int lower = Math.min(Math.max(normalize(start == null ? 0 : start, arraySize), 0), arraySize);
				final int upper = Math.min(Math.max(normalize(end == null ? arraySize : end, arraySize), 0), arraySize);
				for (int i = lower; i < upper; i += stepValue) {
					indexes.add(i);
				}
			} else {
				final int upper = Math.min(Math.max(normalize(start == null ? arraySize - 1 : start, arraySize), -1), arraySize - 1);
				final int lower = end == null ? -1 : Math.min(Math.max(normalize(end, arraySize), -1), arraySize - 1);
				for (int i = upper; i > lower; i += stepValue) {
					indexes.add(i);
				}
			}
			return indexes;
		}

		private static int normalize(final int index, final int arraySize) {
			return index >= 0 ? index : arraySize + index;
		}

		@Override
		public String toString() {
			return (start == null ? "" : start.toString()) + ":" + (end == null ? "" : end.toString()) + (step == null ? "" : ":" + step);
		}
	}

	/**
	 * The selectors in selection order.
	 */
	private final List<Selector> selectors;

	/**
	 * Creates a union of selectors.
	 *
	 * @param selectors
	 *            the selectors in selection order, at least one
	 * @throws IllegalArgumentException
	 *             if no selector or a null selector is given
	 */
	public JsonPathUnionElement(final List<Selector> selectors) {
		if (selectors == null || selectors.isEmpty()) {
			throw new IllegalArgumentException("JSON path union needs at least one selector");
		}
		for (final Selector selector : selectors) {
			if (selector == null) {
				throw new IllegalArgumentException("JSON path union contains a null selector");
			}
		}
		this.selectors = Collections.unmodifiableList(new ArrayList<>(selectors));
	}

	/**
	 * Creates a union of selectors.
	 *
	 * @param selectors
	 *            the selectors in selection order, at least one
	 */
	public JsonPathUnionElement(final Selector... selectors) {
		this(selectors == null ? null : List.of(selectors));
	}

	/**
	 * Returns the selectors.
	 *
	 * @return the unmodifiable list of selectors in selection order
	 */
	public List<Selector> getSelectors() {
		return selectors;
	}

	/**
	 * Checks whether this union selects at most one node, i.e. it consists of a single property key
	 * or a single index.
	 *
	 * @return true, if this union selects at most one node
	 */
	public boolean isSingular() {
		return selectors.size() == 1 && (selectors.get(0) instanceof IndexSelector || selectors.get(0) instanceof NameSelector);
	}

	@Override
	public String toString() {
		final StringBuilder returnValue = new StringBuilder("[");
		for (int i = 0; i < selectors.size(); i++) {
			if (i > 0) {
				returnValue.append(",");
			}
			returnValue.append(selectors.get(i));
		}
		return returnValue.append("]").toString();
	}

	@Override
	public boolean equals(final Object otherObject) {
		return otherObject instanceof JsonPathUnionElement && selectors.equals(((JsonPathUnionElement) otherObject).selectors);
	}

	@Override
	public int hashCode() {
		return selectors.hashCode();
	}
}
