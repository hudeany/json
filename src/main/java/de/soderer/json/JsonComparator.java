package de.soderer.json;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Compares two JsonNode trees and produces either a textual diff listing
 * or a new JsonNode tree containing only the differing parts.
 *
 * This is the JSON counterpart of {@code YamlComparator}, mirroring its
 * behaviour and diff text format so that diffs of YAML and JSON documents
 * look and parse the same way.
 */
public class JsonComparator {
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
	 * The path uses dot notation for objects and bracket notation for array indexes,
	 * e.g. "servers[2].name".
	 */
	public List<DiffEntry> compare(final JsonNode left, final JsonNode right) {
		final List<DiffEntry> diffEntries = new ArrayList<>();
		compareNodes("root", left, right, diffEntries);
		return diffEntries;
	}

	private void compareNodes(final String path, final JsonNode left, final JsonNode right, final List<DiffEntry> diffEntries) {
		if (left == null && right == null) {
			// Nothing to compare
		} else if (left == null) {
			diffEntries.add(new DiffEntry(path, DiffType.ADDED, null, toSimpleValue(right)));
		} else if (right == null) {
			diffEntries.add(new DiffEntry(path, DiffType.REMOVED, toSimpleValue(left), null));
		} else if (left instanceof JsonObject && right instanceof JsonObject) {
			compareObjects(path, (JsonObject) left, (JsonObject) right, diffEntries);
		} else if (left instanceof JsonArray && right instanceof JsonArray) {
			compareArrays(path, (JsonArray) left, (JsonArray) right, diffEntries);
		} else if (isScalarNode(left) && isScalarNode(right)) {
			compareScalars(path, left, right, diffEntries);
		} else {
			// Different node types at the same path (e.g. object replaced by scalar)
			diffEntries.add(new DiffEntry(path, DiffType.CHANGED, toSimpleValue(left), toSimpleValue(right)));
		}
	}

	private void compareObjects(final String path, final JsonObject left, final JsonObject right, final List<DiffEntry> diffEntries) {
		final Set<String> allKeys = new LinkedHashSet<>();
		allKeys.addAll(left.keySet());
		allKeys.addAll(right.keySet());

		for (final String key : allKeys) {
			final String childPath = "root".equals(path) ? key : path + "." + key;
			final JsonNode leftChild = left.containsKey(key) ? left.get(key) : null;
			final JsonNode rightChild = right.containsKey(key) ? right.get(key) : null;
			compareNodes(childPath, leftChild, rightChild, diffEntries);
		}
	}

	private void compareArrays(final String path, final JsonArray left, final JsonArray right, final List<DiffEntry> diffEntries) {
		final int maxSize = Math.max(left.size(), right.size());
		for (int i = 0; i < maxSize; i++) {
			final String childPath = path + "[" + i + "]";
			final JsonNode leftChild = i < left.size() ? left.get(i) : null;
			final JsonNode rightChild = i < right.size() ? right.get(i) : null;
			compareNodes(childPath, leftChild, rightChild, diffEntries);
		}
	}

	private static void compareScalars(final String path, final JsonNode left, final JsonNode right, final List<DiffEntry> diffEntries) {
		final Object leftValue = toSimpleValue(left);
		final Object rightValue = toSimpleValue(right);

		if (leftValue == null && rightValue == null) {
			// Equal
		} else if (leftValue == null || rightValue == null || !leftValue.equals(rightValue)) {
			diffEntries.add(new DiffEntry(path, DiffType.CHANGED, leftValue, rightValue));
		} else {
			// Equal
		}
	}

	private static boolean isScalarNode(final JsonNode node) {
		return node instanceof JsonValueNull
				|| node instanceof JsonValueString
				|| node instanceof JsonValueInteger
				|| node instanceof JsonValueNumber
				|| node instanceof JsonValueBoolean;
	}

	private static Object toSimpleValue(final JsonNode node) {
		if (node == null || node instanceof JsonValueNull) {
			return null;
		} else if (node instanceof JsonValueString) {
			return ((JsonValueString) node).getValue();
		} else if (node instanceof JsonValueInteger) {
			return ((JsonValueInteger) node).getValue();
		} else if (node instanceof JsonValueNumber) {
			return ((JsonValueNumber) node).getValue();
		} else if (node instanceof JsonValueBoolean) {
			return ((JsonValueBoolean) node).getValue();
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
	 *
	 * The format is identical to {@code YamlComparator#renderAsText}, so the
	 * same {@code JsonDiffPatcher} parsing rules apply.
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
		} else if (value instanceof JsonNode) {
			return value.toString();
		} else {
			return "\"" + value + "\"";
		}
	}

	/**
	 * Builds a new JsonObject/JsonArray/scalar tree from "right" that
	 * contains only the parts which differ from "left" (added or changed values,
	 * including the changed value's full subtree). Removed nodes (present in left,
	 * missing in right) are represented as a JsonValueNull so the caller can
	 * see that the key existed but vanished.
	 *
	 * Returns null if there is no difference at all at this level.
	 * @throws Exception
	 */
	public JsonNode buildDifferenceOnly(final JsonNode left, final JsonNode right) throws Exception {
		if (left == null && right == null) {
			return null;
		} else if (left == null) {
			return right;
		} else if (right == null) {
			// Removed entirely -- represented as explicit null scalar so the
			// caller can see the key existed but vanished.
			return new JsonValueNull();
		} else if (left instanceof JsonObject && right instanceof JsonObject) {
			return buildObjectDifference((JsonObject) left, (JsonObject) right);
		} else if (left instanceof JsonArray && right instanceof JsonArray) {
			return buildArrayDifference((JsonArray) left, (JsonArray) right);
		} else if (isScalarNode(left) && isScalarNode(right)) {
			final Object leftValue = toSimpleValue(left);
			final Object rightValue = toSimpleValue(right);
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

	private JsonNode buildObjectDifference(final JsonObject left, final JsonObject right) throws Exception {
		final JsonObject diffObject = new JsonObject();
		final Set<String> allKeys = new LinkedHashSet<>();
		allKeys.addAll(left.keySet());
		allKeys.addAll(right.keySet());

		for (final String key : allKeys) {
			final JsonNode leftChild = left.containsKey(key) ? left.get(key) : null;
			final JsonNode rightChild = right.containsKey(key) ? right.get(key) : null;
			final JsonNode childDifference = buildDifferenceOnly(leftChild, rightChild);
			if (childDifference != null) {
				diffObject.put(key, childDifference);
			} else {
				// No difference for this key, skip
			}
		}

		if (diffObject.isEmpty()) {
			return null;
		} else {
			return diffObject;
		}
	}

	private JsonNode buildArrayDifference(final JsonArray left, final JsonArray right) throws Exception {
		// Arrays are compared by index. Any structural change (insert/remove
		// in the middle) will show up as differences from that index onward.
		final int maxSize = Math.max(left.size(), right.size());
		boolean anyDifference = false;

		for (int i = 0; i < maxSize; i++) {
			final JsonNode leftChild = i < left.size() ? left.get(i) : null;
			final JsonNode rightChild = i < right.size() ? right.get(i) : null;
			final JsonNode childDifference = buildDifferenceOnly(leftChild, rightChild);
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
			final JsonArray diffArray = new JsonArray();
			for (int i = 0; i < right.size(); i++) {
				diffArray.add(right.get(i));
			}
			return diffArray;
		}
	}

	/**
	 * Builds a new JsonNode tree containing only the parts that are identical
	 * (equal value) in both "left" and "right". This is the counterpart of
	 * {@link #buildDifferenceOnly(JsonNode, JsonNode)}.
	 *
	 * Returns null if there is nothing in common at this level.
	 * @throws Exception
	 */
	public JsonNode buildIntersectionOnly(final JsonNode left, final JsonNode right) throws Exception {
		if (left == null || right == null) {
			return null;
		} else if (left instanceof JsonObject && right instanceof JsonObject) {
			return buildObjectIntersection((JsonObject) left, (JsonObject) right);
		} else if (left instanceof JsonArray && right instanceof JsonArray) {
			return buildArrayIntersection((JsonArray) left, (JsonArray) right);
		} else if (isScalarNode(left) && isScalarNode(right)) {
			final Object leftValue = toSimpleValue(left);
			final Object rightValue = toSimpleValue(right);
			if (leftValue == null && rightValue == null) {
				return new JsonValueNull();
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

	private JsonNode buildObjectIntersection(final JsonObject left, final JsonObject right) throws Exception {
		final JsonObject intersectionObject = new JsonObject();

		for (final String key : left.keySet()) {
			if (right.containsKey(key)) {
				final JsonNode commonChild = buildIntersectionOnly(left.get(key), right.get(key));
				if (commonChild != null) {
					intersectionObject.put(key, commonChild);
				} else {
					// Key present in both but value differs, skip
				}
			} else {
				// Key only in left, skip
			}
		}

		if (intersectionObject.isEmpty()) {
			return null;
		} else {
			return intersectionObject;
		}
	}

	private JsonNode buildArrayIntersection(final JsonArray left, final JsonArray right) throws Exception {
		// Arrays are compared by index. Only positions present in both
		// arrays with equal values are considered common.
		final int minSize = Math.min(left.size(), right.size());
		final JsonArray intersectionArray = new JsonArray();

		for (int i = 0; i < minSize; i++) {
			final JsonNode commonChild = buildIntersectionOnly(left.get(i), right.get(i));
			if (commonChild != null) {
				intersectionArray.add(commonChild);
			} else {
				// Differs at this index, skip
			}
		}

		if (intersectionArray.isEmpty()) {
			return null;
		} else {
			return intersectionArray;
		}
	}
}
