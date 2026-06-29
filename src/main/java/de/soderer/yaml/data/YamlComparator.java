package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Compares two YamlNode trees and produces either a textual diff listing
 * or a new YamlNode tree containing only the differing parts.
 */
public class YamlComparator {
	public enum DiffType {
		ADDED,
		REMOVED,
		CHANGED;
	}

	/**
	 * Single difference entry, identified by its path within the document.
	 */
	public static class DiffEntry {
		private final String path;
		private final DiffType type;
		private final Object oldValue;
		private final Object newValue;

		public DiffEntry(final String path, final DiffType type, final Object oldValue, final Object newValue) {
			this.path = path;
			this.type = type;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		public String getPath() {
			return path;
		}

		public DiffType getType() {
			return type;
		}

		public Object getOldValue() {
			return oldValue;
		}

		public Object getNewValue() {
			return newValue;
		}
	}

	/**
	 * Recursively compares "left" and "right" and returns a flat list of differences.
	 * The path uses dot notation for mappings and bracket notation for sequence indexes,
	 * e.g. "servers[2].name".
	 */
	public List<DiffEntry> compare(final YamlNode left, final YamlNode right) {
		final List<DiffEntry> diffEntries = new ArrayList<>();
		compareNodes("root", left, right, diffEntries);
		return diffEntries;
	}

	private void compareNodes(final String path, final YamlNode left, final YamlNode right, final List<DiffEntry> diffEntries) {
		if (left == null && right == null) {
			// Nothing to compare
		} else if (left == null) {
			diffEntries.add(new DiffEntry(path, DiffType.ADDED, null, toSimpleValue(right)));
		} else if (right == null) {
			diffEntries.add(new DiffEntry(path, DiffType.REMOVED, toSimpleValue(left), null));
		} else if (left instanceof YamlMapping && right instanceof YamlMapping) {
			compareMappings(path, (YamlMapping) left, (YamlMapping) right, diffEntries);
		} else if (left instanceof YamlSequence && right instanceof YamlSequence) {
			compareSequences(path, (YamlSequence) left, (YamlSequence) right, diffEntries);
		} else if (left instanceof YamlScalar && right instanceof YamlScalar) {
			compareScalars(path, (YamlScalar) left, (YamlScalar) right, diffEntries);
		} else {
			// Different node types at the same path (e.g. mapping replaced by scalar)
			diffEntries.add(new DiffEntry(path, DiffType.CHANGED, toSimpleValue(left), toSimpleValue(right)));
		}
	}

	private void compareMappings(final String path, final YamlMapping left, final YamlMapping right, final List<DiffEntry> diffEntries) {
		final Set<YamlNode> allKeys = new LinkedHashSet<>();
		allKeys.addAll(left.keySet());
		allKeys.addAll(right.keySet());

		for (final YamlNode key : allKeys) {
			final String childPath = "root".equals(path) ? keyToPathSegment(key) : path + "." + keyToPathSegment(key);
			final YamlNode leftChild = left.containsKey(key) ? left.get(key) : null;
			final YamlNode rightChild = right.containsKey(key) ? right.get(key) : null;
			compareNodes(childPath, leftChild, rightChild, diffEntries);
		}
	}

	/**
	 * Renders a YamlNode mapping key as a readable path segment.
	 * Plain scalar keys (the common case) are rendered as their simple value;
	 * complex (non-scalar) keys fall back to their YAML representation.
	 */
	private static String keyToPathSegment(final YamlNode key) {
		if (key instanceof YamlScalar) {
			final Object scalarValue = ((YamlScalar) key).getValue();
			return scalarValue == null ? "null" : scalarValue.toString();
		} else {
			return "[" + key.toString().trim() + "]";
		}
	}

	private void compareSequences(final String path, final YamlSequence left, final YamlSequence right, final List<DiffEntry> diffEntries) {
		final int maxSize = Math.max(left.size(), right.size());
		for (int i = 0; i < maxSize; i++) {
			final String childPath = path + "[" + i + "]";
			final YamlNode leftChild = i < left.size() ? left.get(i) : null;
			final YamlNode rightChild = i < right.size() ? right.get(i) : null;
			compareNodes(childPath, leftChild, rightChild, diffEntries);
		}
	}

	private static void compareScalars(final String path, final YamlScalar left, final YamlScalar right, final List<DiffEntry> diffEntries) {
		final Object leftValue = left.getValue();
		final Object rightValue = right.getValue();

		if (leftValue == null && rightValue == null) {
			// Equal
		} else if (leftValue == null || rightValue == null || !leftValue.equals(rightValue)) {
			diffEntries.add(new DiffEntry(path, DiffType.CHANGED, leftValue, rightValue));
		} else {
			// Equal
		}
	}

	private static Object toSimpleValue(final YamlNode node) {
		if (node instanceof YamlScalar) {
			return ((YamlScalar) node).getValue();
		} else {
			return node;
		}
	}

	/**
	 * Renders a flat diff list as a human readable text block,
	 * one line per difference, e.g.:
	 *   ~ servers[2].name: "old" -> "new"
	 *   + servers[3].host: "10.0.0.5"
	 *   - servers[4]
	 */
	public static String renderAsText(final List<DiffEntry> diffEntries) {
		final StringBuilder resultBuilder = new StringBuilder();
		for (final DiffEntry diffEntry : diffEntries) {
			if (diffEntry.getType() == DiffType.ADDED) {
				resultBuilder.append("+ ").append(diffEntry.getPath()).append(": ").append(formatValue(diffEntry.getNewValue())).append("\n");
			} else if (diffEntry.getType() == DiffType.REMOVED) {
				resultBuilder.append("- ").append(diffEntry.getPath()).append(": ").append(formatValue(diffEntry.getOldValue())).append("\n");
			} else if (diffEntry.getType() == DiffType.CHANGED) {
				resultBuilder.append("~ ").append(diffEntry.getPath()).append(": ").append(formatValue(diffEntry.getOldValue())).append(" -> ").append(formatValue(diffEntry.getNewValue())).append("\n");
			} else {
				// Unknown type, skip
			}
		}

		if (resultBuilder.length() == 0) {
			return "No differences found";
		} else {
			return resultBuilder.toString();
		}
	}

	private static String formatValue(final Object value) {
		if (value == null) {
			return "null";
		} else if (value instanceof YamlNode) {
			return value.toString();
		} else {
			return "\"" + value + "\"";
		}
	}

	/**
	 * Builds a new YamlMapping/YamlSequence/YamlScalar tree from "right" that
	 * contains only the parts which differ from "left" (added or changed values,
	 * including the changed value's full subtree). Removed nodes (present in left,
	 * missing in right) are represented as YamlScalar with value null, prefixed marker
	 * is left to the caller since YamlNode itself carries no comments.
	 *
	 * Returns null if there is no difference at all at this level.
	 * @throws Exception
	 */
	public YamlNode buildDifferenceOnly(final YamlNode left, final YamlNode right) throws Exception {
		if (left == null && right == null) {
			return null;
		} else if (left == null) {
			return right;
		} else if (right == null) {
			// Removed entirely -- represented as explicit null scalar so the
			// caller can see the key existed but vanished.
			return new YamlScalar(null);
		} else if (left instanceof YamlMapping && right instanceof YamlMapping) {
			return buildMappingDifference((YamlMapping) left, (YamlMapping) right);
		} else if (left instanceof YamlSequence && right instanceof YamlSequence) {
			return buildSequenceDifference((YamlSequence) left, (YamlSequence) right);
		} else if (left instanceof YamlScalar && right instanceof YamlScalar) {
			final Object leftValue = ((YamlScalar) left).getValue();
			final Object rightValue = ((YamlScalar) right).getValue();
			if (leftValue == null && rightValue == null) {
				return null;
			} else if (leftValue == null || !leftValue.equals(rightValue)) {
				return right;
			} else {
				return null;
			}
		} else {
			// Type changed completely, take right as the new value
			return right;
		}
	}

	private YamlNode buildMappingDifference(final YamlMapping left, final YamlMapping right) throws Exception {
		final YamlMapping diffMapping = new YamlMapping();
		final Set<YamlNode> allKeys = new LinkedHashSet<>();
		allKeys.addAll(left.keySet());
		allKeys.addAll(right.keySet());

		for (final YamlNode key : allKeys) {
			final YamlNode leftChild = left.containsKey(key) ? left.get(key) : null;
			final YamlNode rightChild = right.containsKey(key) ? right.get(key) : null;
			final YamlNode childDifference = buildDifferenceOnly(leftChild, rightChild);
			if (childDifference != null) {
				diffMapping.add(key, childDifference);
			} else {
				// No difference for this key, skip
			}
		}

		if (diffMapping.isEmpty()) {
			return null;
		} else {
			return diffMapping;
		}
	}

	private YamlNode buildSequenceDifference(final YamlSequence left, final YamlSequence right) throws Exception {
		// Sequences are compared by index. Any structural change (insert/remove
		// in the middle) will show up as differences from that index onward.
		final int maxSize = Math.max(left.size(), right.size());
		boolean anyDifference = false;
		final YamlSequence diffSequence = new YamlSequence();

		for (int i = 0; i < maxSize; i++) {
			final YamlNode leftChild = i < left.size() ? left.get(i) : null;
			final YamlNode rightChild = i < right.size() ? right.get(i) : null;
			final YamlNode childDifference = buildDifferenceOnly(leftChild, rightChild);
			if (childDifference != null) {
				anyDifference = true;
			} else {
				// Equal at this index
			}
		}

		if (!anyDifference) {
			return null;
		} else {
			// Rebuild using right's items so the result is a valid standalone
			// document reflecting the new state (not a sparse/holey structure).
			for (int i = 0; i < right.size(); i++) {
				diffSequence.add(right.get(i));
			}
			return diffSequence;
		}
	}

	/**
	 * Builds a new YamlNode tree containing only the parts that are identical
	 * (equal value) in both "left" and "right". This is the counterpart of
	 * {@link #buildDifferenceOnly(YamlNode, YamlNode)}.
	 *
	 * Returns null if there is nothing in common at this level.
	 * @throws Exception
	 */
	public YamlNode buildIntersectionOnly(final YamlNode left, final YamlNode right) throws Exception {
		if (left == null || right == null) {
			return null;
		} else if (left instanceof YamlMapping && right instanceof YamlMapping) {
			return buildMappingIntersection((YamlMapping) left, (YamlMapping) right);
		} else if (left instanceof YamlSequence && right instanceof YamlSequence) {
			return buildSequenceIntersection((YamlSequence) left, (YamlSequence) right);
		} else if (left instanceof YamlScalar && right instanceof YamlScalar) {
			final Object leftValue = ((YamlScalar) left).getValue();
			final Object rightValue = ((YamlScalar) right).getValue();
			if (leftValue == null && rightValue == null) {
				return new YamlScalar(null);
			} else if (leftValue != null && leftValue.equals(rightValue)) {
				return right;
			} else {
				return null;
			}
		} else {
			// Different node types at the same path, no common value
			return null;
		}
	}

	private YamlNode buildMappingIntersection(final YamlMapping left, final YamlMapping right) throws Exception {
		final YamlMapping intersectionMapping = new YamlMapping();

		for (final YamlNode key : left.keySet()) {
			if (right.containsKey(key)) {
				final YamlNode commonChild = buildIntersectionOnly(left.get(key), right.get(key));
				if (commonChild != null) {
					intersectionMapping.add(key, commonChild);
				} else {
					// Key present in both but value differs, skip
				}
			} else {
				// Key only in left, skip
			}
		}

		if (intersectionMapping.isEmpty()) {
			return null;
		} else {
			return intersectionMapping;
		}
	}

	private YamlNode buildSequenceIntersection(final YamlSequence left, final YamlSequence right) throws Exception {
		// Sequences are compared by index. Only positions present in both
		// sequences with equal values are considered common.
		final int minSize = Math.min(left.size(), right.size());
		final YamlSequence intersectionSequence = new YamlSequence();

		for (int i = 0; i < minSize; i++) {
			final YamlNode commonChild = buildIntersectionOnly(left.get(i), right.get(i));
			if (commonChild != null) {
				intersectionSequence.add(commonChild);
			} else {
				// Differs at this index, skip
			}
		}

		if (intersectionSequence.isEmpty()) {
			return null;
		} else {
			return intersectionSequence;
		}
	}
}