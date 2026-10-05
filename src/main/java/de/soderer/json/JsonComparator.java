package de.soderer.json;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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
	/**
	 * Creates a new comparator.
	 */
	public JsonComparator() {
		// Stateless, all settings are method parameters
	}

	/**
	 * Types of differences.
	 */
	public enum DiffType {
		/**
		 * Value exists only in the right tree.
		 */
		ADDED,
		/**
		 * Value exists only in the left tree.
		 */
		REMOVED,
		/**
		 * Value differs between the trees.
		 */
		CHANGED;
	}

	/**
	 * Single difference entry, identified by its path within the document.
	 */
	public static class DiffEntry {
		/**
		 * Path of the difference, e.g. "servers[2].name".
		 */
		private final String path;
		/**
		 * Type of the difference.
		 */
		private final DiffType type;
		/**
		 * Value in the left tree, null if added.
		 */
		private final Object oldValue;
		/**
		 * Value in the right tree, null if removed.
		 */
		private final Object newValue;

		/**
		 * Creates a new difference entry.
		 *
		 * @param path
		 *            path of the difference
		 * @param type
		 *            type of the difference
		 * @param oldValue
		 *            value in the left tree, a simple Java value or JsonNode
		 * @param newValue
		 *            value in the right tree, a simple Java value or JsonNode
		 */
		public DiffEntry(final String path, final DiffType type, final Object oldValue, final Object newValue) {
			this.path = path;
			this.type = type;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		/**
		 * Returns the path of the difference.
		 *
		 * @return the path, e.g. "servers[2].name"
		 */
		public String getPath() {
			return path;
		}

		/**
		 * Returns the type of the difference.
		 *
		 * @return the type
		 */
		public DiffType getType() {
			return type;
		}

		/**
		 * Returns the value in the left tree.
		 *
		 * @return the simple Java value or JsonNode, null if added or JSON null
		 */
		public Object getOldValue() {
			return oldValue;
		}

		/**
		 * Returns the value in the right tree.
		 *
		 * @return the simple Java value or JsonNode, null if removed or JSON null
		 */
		public Object getNewValue() {
			return newValue;
		}
	}

	/**
	 * Recursively compares "left" and "right" and returns a flat list of differences.
	 * The path uses dot notation for objects and bracket notation for array indexes,
	 * e.g. "servers[2].name", starting with "root".
	 *
	 * @param left
	 *            the left (old) tree, may be null
	 * @param right
	 *            the right (new) tree, may be null
	 * @return the differences, empty if the trees are equal
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
			final String childPath = appendKeyToPath(path, key);
			final JsonNode leftChild = left.containsKey(key) ? left.get(key) : null;
			final JsonNode rightChild = right.containsKey(key) ? right.get(key) : null;
			compareNodes(childPath, leftChild, rightChild, diffEntries);
		}
	}

	/**
	 * Appends an object key to a diff path. Keys with path syntax characters are written as
	 * ["key"], so the path stays unambiguous.
	 *
	 * @param path
	 *            the path so far, "root" for the root node
	 * @param keyText
	 *            the key
	 * @return the extended path
	 */
	static String appendKeyToPath(final String path, final String keyText) {
		if (keyNeedsQuoting(keyText)) {
			// Keys with path syntax characters are written as ["key"], so the path stays unambiguous
			return ("root".equals(path) ? "" : path) + "[" + quote(keyText) + "]";
		} else {
			return "root".equals(path) ? keyText : path + "." + keyText;
		}
	}

	/**
	 * Appends an array index to a diff path.
	 *
	 * @param path
	 *            the path so far, "root" for the root node
	 * @param index
	 *            the array index
	 * @return the extended path
	 */
	static String appendIndexToPath(final String path, final int index) {
		return ("root".equals(path) ? "" : path) + "[" + index + "]";
	}

	/**
	 * Checks whether a key must be quoted within a diff path.
	 *
	 * @param keyText
	 *            the key
	 * @return true, if the key is empty, has leading or trailing whitespace, or contains control
	 *         characters or path syntax characters
	 */
	private static boolean keyNeedsQuoting(final String keyText) {
		if (keyText.isEmpty() || Character.isWhitespace(keyText.charAt(0)) || Character.isWhitespace(keyText.charAt(keyText.length() - 1))) {
			return true;
		}
		for (final char keyChar : keyText.toCharArray()) {
			if (keyChar < ' ' || ".[]\":\\".indexOf(keyChar) >= 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Quotes a text with double quotes and escapes backslash, quote, linebreaks and tabs.
	 *
	 * @param text
	 *            the text
	 * @return the quoted text
	 */
	static String quote(final String text) {
		final StringBuilder quotedText = new StringBuilder("\"");
		for (final char nextChar : text.toCharArray()) {
			switch (nextChar) {
				case '\\':
					quotedText.append("\\\\");
					break;
				case '"':
					quotedText.append("\\\"");
					break;
				case '\n':
					quotedText.append("\\n");
					break;
				case '\r':
					quotedText.append("\\r");
					break;
				case '\t':
					quotedText.append("\\t");
					break;
				default:
					quotedText.append(nextChar);
			}
		}
		return quotedText.append("\"").toString();
	}

	/**
	 * Removes the quotes of a text quoted by {@link #quote(String)} and resolves the escapes.
	 *
	 * @param quotedText
	 *            the quoted text
	 * @return the text
	 */
	static String unquote(final String quotedText) {
		final StringBuilder text = new StringBuilder();
		for (int i = 1; i < quotedText.length() - 1; i++) {
			final char nextChar = quotedText.charAt(i);
			if (nextChar == '\\' && i + 1 < quotedText.length() - 1) {
				final char escapedChar = quotedText.charAt(++i);
				switch (escapedChar) {
					case 'n':
						text.append('\n');
						break;
					case 'r':
						text.append('\r');
						break;
					case 't':
						text.append('\t');
						break;
					default:
						text.append(escapedChar);
				}
			} else {
				text.append(nextChar);
			}
		}
		return text.toString();
	}

	private void compareArrays(final String path, final JsonArray left, final JsonArray right, final List<DiffEntry> diffEntries) {
		final int maxSize = Math.max(left.size(), right.size());
		for (int i = 0; i < maxSize; i++) {
			final String childPath = appendIndexToPath(path, i);
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
		} else if (leftValue == null || rightValue == null || !left.equals(right)) {
			// Compare the nodes, which compare numbers numerically (e.g. Integer 1 and Long 1 are equal)
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
	 * <pre>
	 *   ~ servers[2].name: "old" -&gt; "new"
	 *   + servers[3].host: "10.0.0.5"
	 *   - servers[4]: "oldHost"
	 * </pre>
	 * The format is identical to {@code YamlComparator#renderAsText} and can be parsed by
	 * {@link JsonDiffPatcher#parseDiffText(String)}.
	 *
	 * @param diffEntries
	 *            the differences
	 * @return the diff text, or "No differences found"
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
			// Complex values as compact JSON on one line, so the diff stays line based and can be patched
			return "json:" + quote(toCompactJson((JsonNode) value));
		} else {
			return quote(value.toString());
		}
	}

	/**
	 * Writes a JSON node as compact JSON text without linebreaks.
	 *
	 * @param jsonNode
	 *            the JSON node
	 * @return the JSON text
	 */
	private static String toCompactJson(final JsonNode jsonNode) {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (JsonWriter jsonWriter = new JsonWriter(outputStream)) {
			jsonWriter.setUglify(true);
			jsonWriter.add(jsonNode);
		} catch (final Exception e) {
			throw new RuntimeException("Cannot write JSON value: " + e.getMessage(), e);
		}
		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * Builds a new JsonObject/JsonArray/scalar tree from "right" that
	 * contains only the parts which differ from "left" (added or changed values,
	 * including the changed value's full subtree). Removed nodes (present in left,
	 * missing in right) are represented as a JsonValueNull so the caller can
	 * see that the key existed but vanished.
	 *
	 * @param left
	 *            the left (old) tree, may be null
	 * @param right
	 *            the right (new) tree, may be null
	 * @return the differing parts, or null if there is no difference at all at this level
	 * @throws Exception
	 *             if building the result tree fails
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
	 * @param left
	 *            the left tree, may be null
	 * @param right
	 *            the right tree, may be null
	 * @return the common parts, or null if there is nothing in common at this level
	 * @throws Exception
	 *             if building the result tree fails
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
