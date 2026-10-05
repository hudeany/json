package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.soderer.yaml.YamlReader;

/**
 * Parses a diff text in the format produced by {@link YamlComparator#renderAsText}
 * and applies it as a patch onto a YamlNode tree, e.g.:
 *   ~ rooting.abc: "123" -> "124"
 *   + servers[3].host: "10.0.0.5"
 *   - servers[4]: "oldHost"
 *
 * Line format:
 *   '+' path ':' newValue       -- add a new node, path must not exist yet
 *   '-' path ':' oldValue       -- remove an existing node, current value must match oldValue
 *   '~' path ':' oldValue '->' newValue -- change an existing node, current value must match oldValue
 *   '~' path ':' newValue       -- change an existing node unconditionally, regardless of its
 *                                   current value (no old value / no '->' present); the path
 *                                   must already exist, but its prior value is not checked
 *
 * Paths use dot notation for keys and bracket notation for indexes; keys with path syntax
 * characters are quoted like ["a.b"]. Number and boolean keys are found by their text.
 *
 * Values are rendered as either the literal "null", a double-quoted string with backslash
 * escapes, or yaml:"..." with the YAML text of a complex node (see YamlComparator#formatValue).
 * Quotes and escapes are resolved while parsing, complex nodes are parsed. Since
 * the diff text does not preserve the original scalar type, patched scalar
 * values are re-interpreted the same way YamlReader interprets unquoted
 * scalars (number, boolean, null, or string), to produce a reasonable type.
 */
public class YamlDiffPatcher {
	/**
	 * Utility class, not to be instantiated.
	 */
	private YamlDiffPatcher() {
	}

	/**
	 * Types of patch lines.
	 */
	public enum PatchLineType {
		/**
		 * Add a new node ("+" line).
		 */
		ADDED,
		/**
		 * Remove an existing node ("-" line).
		 */
		REMOVED,
		/**
		 * Change an existing node ("~" line).
		 */
		CHANGED;
	}

	/**
	 * Single parsed patch instruction, prior to being applied.
	 */
	public static class PatchEntry {
		/**
		 * Path of the node to patch.
		 */
		private final String path;
		/**
		 * Type of the patch.
		 */
		private final PatchLineType type;
		/**
		 * Expected current value as text, null if not applicable.
		 */
		private final String oldValueText;
		/**
		 * New value as text, null if not applicable.
		 */
		private final String newValueText;
		/**
		 * True, if the current value is not checked before a change.
		 */
		private final boolean ignoreOldValue;

		/**
		 * Creates a new patch entry which checks the current value before a change.
		 *
		 * @param path
		 *            path of the node to patch, e.g. "servers[2].name"
		 * @param type
		 *            type of the patch
		 * @param oldValueText
		 *            expected current value as text, null if not applicable
		 * @param newValueText
		 *            new value as text, null if not applicable
		 */
		public PatchEntry(final String path, final PatchLineType type, final String oldValueText, final String newValueText) {
			this(path, type, oldValueText, newValueText, false);
		}

		/**
		 * Creates a new patch entry.
		 *
		 * @param path
		 *            path of the node to patch, e.g. "servers[2].name"
		 * @param type
		 *            type of the patch
		 * @param oldValueText
		 *            expected current value as text, null if not applicable
		 * @param newValueText
		 *            new value as text, null if not applicable
		 * @param ignoreOldValue
		 *            if true, the current value at this path is not checked
		 *            before applying a CHANGED entry (line format '~ path: newValue' without
		 *            an old value / arrow). Only meaningful for {@link PatchLineType#CHANGED}.
		 */
		public PatchEntry(final String path, final PatchLineType type, final String oldValueText, final String newValueText, final boolean ignoreOldValue) {
			this.path = path;
			this.type = type;
			this.oldValueText = oldValueText;
			this.newValueText = newValueText;
			this.ignoreOldValue = ignoreOldValue;
		}

		/**
		 * Returns the path of the node to patch.
		 *
		 * @return the path
		 */
		public String getPath() {
			return path;
		}

		/**
		 * Returns the type of the patch.
		 *
		 * @return the type
		 */
		public PatchLineType getType() {
			return type;
		}

		/**
		 * Returns the expected current value.
		 *
		 * @return the value as text, null if not applicable
		 */
		public String getOldValueText() {
			return oldValueText;
		}

		/**
		 * Returns the new value.
		 *
		 * @return the value as text, null if not applicable
		 */
		public String getNewValueText() {
			return newValueText;
		}

		/**
		 * Returns whether the current value is not checked before a change.
		 *
		 * @return true, if the current value is not checked
		 */
		public boolean isIgnoreOldValue() {
			return ignoreOldValue;
		}
	}

	/**
	 * Thrown when a patch cannot be applied because the current value in the
	 * YAML tree does not match the expected "old value" from the diff, or when
	 * a structural precondition (node exists / does not exist) is violated.
	 */
	public static class PatchConflictException extends Exception {
		private static final long serialVersionUID = 1L;

		/**
		 * Creates a new exception.
		 *
		 * @param message
		 *            the detail message
		 */
		public PatchConflictException(final String message) {
			super(message);
		}
	}

	/** Quoted text with backslash escapes. */
	private static final String QUOTED = "\"(?:[^\"\\\\]|\\\\.)*\"";
	/** Path of key segments, quoted key segments and index segments. */
	private static final String PATH = "((?:\\[" + QUOTED + "\\]|\\[\\d+\\]|[^:\\[])+?)";
	/** Value: null, quoted text, quoted YAML text of a complex node, or unquoted text (hand written diffs). */
	private static final String VALUE = "(null|(?:yaml:)?" + QUOTED + "|.+?)";
	private static final Pattern ADDED_LINE_PATTERN = Pattern.compile("^\\+\\s+" + PATH + ":\\s+" + VALUE + "$");
	private static final Pattern REMOVED_LINE_PATTERN = Pattern.compile("^-\\s+" + PATH + ":\\s+" + VALUE + "$");
	private static final Pattern CHANGED_LINE_PATTERN = Pattern.compile("^~\\s+" + PATH + ":\\s+" + VALUE + "\\s+->\\s+" + VALUE + "$");
	private static final Pattern CHANGED_LINE_PATTERN_NO_OLD_VALUE = Pattern.compile("^~\\s+" + PATH + ":\\s+" + VALUE + "$");

	/**
	 * Parses a diff text (as produced by YamlComparator#renderAsText) into a
	 * list of patch entries. Blank lines and the "No differences found"
	 * placeholder text are ignored. Lines that match none of the known
	 * patterns cause a PatchConflictException, since silently skipping
	 * unparsable lines could lead to an incomplete patch.
	 *
	 * @param diffText
	 *            the diff text, may be null
	 * @return the patch entries, empty for null
	 * @throws PatchConflictException
	 *             if a line cannot be parsed
	 */
	public static List<PatchEntry> parseDiffText(final String diffText) throws PatchConflictException {
		final List<PatchEntry> patchEntries = new ArrayList<>();

		if (diffText == null) {
			return patchEntries;
		} else {
			// Continue parsing
		}

		final String[] lines = diffText.split("\\r?\\n");
		for (final String rawLine : lines) {
			final String line = rawLine.trim();
			if (line.isEmpty()) {
				// Skip blank lines
			} else if ("No differences found".equals(line)) {
				// Placeholder text from renderAsText, nothing to patch
			} else {
				patchEntries.add(parseLine(line));
			}
		}

		return patchEntries;
	}

	private static PatchEntry parseLine(final String line) throws PatchConflictException {
		final Matcher changedMatcher = CHANGED_LINE_PATTERN.matcher(line);
		final Matcher changedNoOldValueMatcher = CHANGED_LINE_PATTERN_NO_OLD_VALUE.matcher(line);
		final Matcher addedMatcher = ADDED_LINE_PATTERN.matcher(line);
		final Matcher removedMatcher = REMOVED_LINE_PATTERN.matcher(line);

		if (changedMatcher.matches()) {
			final String path = changedMatcher.group(1).trim();
			final String oldValueText = unquote(changedMatcher.group(2).trim());
			final String newValueText = unquote(changedMatcher.group(3).trim());
			return new PatchEntry(path, PatchLineType.CHANGED, oldValueText, newValueText);
		} else if (line.startsWith("~") && changedNoOldValueMatcher.matches()) {
			final String path = changedNoOldValueMatcher.group(1).trim();
			final String newValueText = unquote(changedNoOldValueMatcher.group(2).trim());
			return new PatchEntry(path, PatchLineType.CHANGED, null, newValueText, true);
		} else if (line.startsWith("+") && addedMatcher.matches()) {
			final String path = addedMatcher.group(1).trim();
			final String newValueText = unquote(addedMatcher.group(2).trim());
			return new PatchEntry(path, PatchLineType.ADDED, null, newValueText);
		} else if (line.startsWith("-") && removedMatcher.matches()) {
			final String path = removedMatcher.group(1).trim();
			final String oldValueText = unquote(removedMatcher.group(2).trim());
			return new PatchEntry(path, PatchLineType.REMOVED, oldValueText, null);
		} else {
			throw new PatchConflictException("Cannot parse diff line: '" + line + "'");
		}
	}

	/**
	 * Strips the surrounding double quotes added by YamlComparator#formatValue and resolves the
	 * escapes, or returns null for the literal "null" marker. Complex values keep a marker, so
	 * they can be told apart from strings.
	 */
	private static String unquote(final String valueText) {
		if ("null".equals(valueText)) {
			return null;
		} else if (valueText.startsWith("yaml:\"") && valueText.endsWith("\"")) {
			return STRUCTURED_VALUE_MARKER + YamlComparator.unquote(valueText.substring(5));
		} else if (valueText.length() >= 2 && valueText.startsWith("\"") && valueText.endsWith("\"")) {
			return YamlComparator.unquote(valueText);
		} else {
			// Not quoted (should not normally happen with formatValue output), use as-is
			return valueText;
		}
	}

	/**
	 * Internal marker prefix for complex values given as YAML text. A linebreak cannot be part of
	 * any other value read from a single diff line, unless it was escaped within quotes.
	 */
	private static final String STRUCTURED_VALUE_MARKER = "\u0000yaml\n";

	/**
	 * A single path segment, either a mapping key (by its simple string
	 * representation) or a sequence index.
	 */
	private static class PathSegment {
		private final String key;
		private final Integer index;

		private PathSegment(final String key, final Integer index) {
			this.key = key;
			this.index = index;
		}

		private static PathSegment forKey(final String key) {
			return new PathSegment(key, null);
		}

		private static PathSegment forIndex(final int index) {
			return new PathSegment(null, index);
		}

		private boolean isIndex() {
			return index != null;
		}
	}

	private static final Pattern PATH_SEGMENT_PATTERN = Pattern.compile("\\[(" + QUOTED + ")\\]|([^.\\[\\]]+)|\\[(\\d+)\\]");

	/**
	 * Splits a path like "servers[2].name" or "rooting.abc" into ordered
	 * path segments. A leading "root" segment (as produced by YamlComparator
	 * for top level keys) is treated like any other key segment, since the
	 * comparator only adds a literal "root." prefix for nested paths, not
	 * for the root document itself -- top level keys appear without that prefix.
	 */
	private static List<PathSegment> splitPath(final String path) {
		final List<PathSegment> pathSegments = new ArrayList<>();
		final Matcher matcher = PATH_SEGMENT_PATTERN.matcher(path);
		while (matcher.find()) {
			if (matcher.group(1) != null) {
				pathSegments.add(PathSegment.forKey(YamlComparator.unquote(matcher.group(1))));
			} else if (matcher.group(2) != null) {
				pathSegments.add(PathSegment.forKey(matcher.group(2)));
			} else {
				pathSegments.add(PathSegment.forIndex(Integer.parseInt(matcher.group(3))));
			}
		}
		return pathSegments;
	}

	/**
	 * Applies all given patch entries onto the given root node, in order.
	 * The root node is modified in place where possible; for mapping/sequence
	 * root replacements the original root instance is reused (its content is
	 * mutated), so the same reference passed in remains valid after patching.
	 *
	 * @param root
	 *            the YAML tree to patch
	 * @param patchEntries
	 *            the patch entries, e.g. from {@link #parseDiffText(String)}
	 * @throws PatchConflictException if any entry's expected old value does not
	 *         match the current value, or a structural precondition is violated
	 * @throws Exception propagated from underlying YamlMapping/YamlSequence operations
	 */
	public static void applyPatch(final YamlNode root, final List<PatchEntry> patchEntries) throws Exception {
		for (final PatchEntry patchEntry : patchEntries) {
			applyPatchEntry(root, patchEntry);
		}
	}

	private static void applyPatchEntry(final YamlNode root, final PatchEntry patchEntry) throws Exception {
		final List<PathSegment> pathSegments = splitPath(patchEntry.getPath());

		if (pathSegments.isEmpty()) {
			throw new PatchConflictException("Empty path in patch entry: '" + patchEntry.getPath() + "'");
		} else {
			// Continue
		}

		if (patchEntry.getType() == PatchLineType.ADDED) {
			applyAdded(root, pathSegments, patchEntry);
		} else if (patchEntry.getType() == PatchLineType.REMOVED) {
			applyRemoved(root, pathSegments, patchEntry);
		} else if (patchEntry.getType() == PatchLineType.CHANGED) {
			applyChanged(root, pathSegments, patchEntry);
		} else {
			throw new PatchConflictException("Unknown patch line type for path: '" + patchEntry.getPath() + "'");
		}
	}

	private static void applyAdded(final YamlNode root, final List<PathSegment> pathSegments, final PatchEntry patchEntry) throws Exception {
		final NavigationResult navigationResult = navigateToParent(root, pathSegments, patchEntry, true);
		final PathSegment lastSegment = pathSegments.get(pathSegments.size() - 1);

		if (lastSegment.isIndex()) {
			final YamlSequence parentSequence = asSequence(navigationResult.parent, patchEntry);
			if (lastSegment.index < parentSequence.size()) {
				throw new PatchConflictException("Cannot add, sequence index already exists at path '" + patchEntry.getPath() + "'");
			} else {
				parentSequence.add(createNodeFromText(patchEntry.getNewValueText()));
			}
		} else {
			final YamlMapping parentMapping = asMapping(navigationResult.parent, patchEntry);
			if (findKey(parentMapping, lastSegment.key) != null) {
				throw new PatchConflictException("Cannot add, key already exists at path '" + patchEntry.getPath() + "'");
			} else {
				parentMapping.add(lastSegment.key, createNodeFromText(patchEntry.getNewValueText()));
			}
		}
	}

	private static void applyRemoved(final YamlNode root, final List<PathSegment> pathSegments, final PatchEntry patchEntry) throws Exception {
		final NavigationResult navigationResult = navigateToParent(root, pathSegments, patchEntry, false);
		final PathSegment lastSegment = pathSegments.get(pathSegments.size() - 1);

		if (lastSegment.isIndex()) {
			final YamlSequence parentSequence = asSequence(navigationResult.parent, patchEntry);
			if (lastSegment.index >= parentSequence.size()) {
				throw new PatchConflictException("Cannot remove, sequence index missing at path '" + patchEntry.getPath() + "'");
			} else {
				final YamlNode currentChild = parentSequence.get(lastSegment.index);
				checkScalarValueMatches(currentChild, patchEntry.getOldValueText(), patchEntry);
				parentSequence.removeByIndex(lastSegment.index);
			}
		} else {
			final YamlMapping parentMapping = asMapping(navigationResult.parent, patchEntry);
			final YamlNode existingKey = findKey(parentMapping, lastSegment.key);
			if (existingKey == null) {
				throw new PatchConflictException("Cannot remove, key missing at path '" + patchEntry.getPath() + "'");
			} else {
				final YamlNode currentChild = parentMapping.get(existingKey);
				checkScalarValueMatches(currentChild, patchEntry.getOldValueText(), patchEntry);
				parentMapping.remove(existingKey);
			}
		}
	}

	private static void applyChanged(final YamlNode root, final List<PathSegment> pathSegments, final PatchEntry patchEntry) throws Exception {
		final NavigationResult navigationResult = navigateToParent(root, pathSegments, patchEntry, false);
		final PathSegment lastSegment = pathSegments.get(pathSegments.size() - 1);

		if (lastSegment.isIndex()) {
			final YamlSequence parentSequence = asSequence(navigationResult.parent, patchEntry);
			if (lastSegment.index >= parentSequence.size()) {
				throw new PatchConflictException("Cannot change, sequence index missing at path '" + patchEntry.getPath() + "'");
			} else {
				final YamlNode currentChild = parentSequence.get(lastSegment.index);
				if (!patchEntry.isIgnoreOldValue()) {
					checkScalarValueMatches(currentChild, patchEntry.getOldValueText(), patchEntry);
				} else {
					// Old value is irrelevant for this patch entry, skip the check
				}
				parentSequence.set(lastSegment.index, createNodeFromText(patchEntry.getNewValueText()));
			}
		} else {
			final YamlMapping parentMapping = asMapping(navigationResult.parent, patchEntry);
			final YamlNode existingKey = findKey(parentMapping, lastSegment.key);
			if (existingKey == null) {
				throw new PatchConflictException("Cannot change, key missing at path '" + patchEntry.getPath() + "'");
			} else {
				final YamlNode currentChild = parentMapping.get(existingKey);
				if (!patchEntry.isIgnoreOldValue()) {
					checkScalarValueMatches(currentChild, patchEntry.getOldValueText(), patchEntry);
				} else {
					// Old value is irrelevant for this patch entry, skip the check
				}
				parentMapping.replace(existingKey, createNodeFromText(patchEntry.getNewValueText()));
			}
		}
	}

	private static void checkScalarValueMatches(final YamlNode currentChild, final String expectedValueText, final PatchEntry patchEntry) throws Exception {
		final boolean matches;
		final String currentValueText;
		if (expectedValueText != null && expectedValueText.startsWith(STRUCTURED_VALUE_MARKER)) {
			// Complex value: compare the parsed nodes
			currentValueText = currentChild == null ? null : currentChild.toString();
			matches = createNodeFromText(expectedValueText).equals(currentChild);
		} else {
			currentValueText = scalarToComparableText(currentChild);
			if (currentValueText == null && expectedValueText == null) {
				matches = true;
			} else if (currentValueText == null || expectedValueText == null) {
				matches = false;
			} else {
				matches = currentValueText.equals(expectedValueText);
			}
		}
		if (!matches) {
			throw new PatchConflictException("Conflict at path '" + patchEntry.getPath() + "': expected old value '"
					+ expectedValueText + "' but found '" + currentValueText + "'");
		} else {
			// Value matches, continue
		}
	}

	private static String scalarToComparableText(final YamlNode node) throws PatchConflictException {
		if (node == null) {
			return null;
		} else if (node instanceof YamlScalar) {
			final Object value = ((YamlScalar) node).getValue();
			return value == null ? null : value.toString();
		} else {
			throw new PatchConflictException("Expected a scalar value but found a complex node");
		}
	}

	/**
	 * Finds the key of a mapping by its text as used in diff paths, see
	 * {@link YamlComparator#keyToText(YamlNode)}. This also finds number and boolean keys.
	 */
	private static YamlNode findKey(final YamlMapping mapping, final String keyText) {
		for (final YamlNode key : mapping.keySet()) {
			if (YamlComparator.keyToText(key).equals(keyText)) {
				return key;
			}
		}
		return null;
	}

	/**
	 * Result of navigating to the parent node of the final path segment.
	 */
	private static class NavigationResult {
		private final YamlNode parent;

		private NavigationResult(final YamlNode parent) {
			this.parent = parent;
		}
	}

	/**
	 * Walks all but the last path segment, descending into the tree. If
	 * createMissing is true, missing intermediate mappings are created on
	 * the fly (used for ADDED entries so that new nested keys can be added
	 * without requiring every ancestor to already exist); otherwise a
	 * missing intermediate node is a conflict.
	 */
	private static NavigationResult navigateToParent(final YamlNode root, final List<PathSegment> pathSegments, final PatchEntry patchEntry, final boolean createMissing) throws Exception {
		YamlNode current = root;

		for (int i = 0; i < pathSegments.size() - 1; i++) {
			final PathSegment segment = pathSegments.get(i);

			if (segment.isIndex()) {
				final YamlSequence currentSequence = asSequence(current, patchEntry);
				if (segment.index >= currentSequence.size()) {
					throw new PatchConflictException("Path segment [" + segment.index + "] missing at path '" + patchEntry.getPath() + "'");
				} else {
					current = currentSequence.get(segment.index);
				}
			} else {
				final YamlMapping currentMapping = asMapping(current, patchEntry);
				final YamlNode existingKey = findKey(currentMapping, segment.key);
				if (existingKey == null) {
					if (createMissing) {
						final YamlMapping newChildMapping = new YamlMapping();
						currentMapping.add(segment.key, newChildMapping);
						current = newChildMapping;
					} else {
						throw new PatchConflictException("Path segment '" + segment.key + "' missing at path '" + patchEntry.getPath() + "'");
					}
				} else {
					current = currentMapping.get(existingKey);
				}
			}
		}

		return new NavigationResult(current);
	}

	private static YamlMapping asMapping(final YamlNode node, final PatchEntry patchEntry) throws PatchConflictException {
		if (node instanceof YamlMapping) {
			return (YamlMapping) node;
		} else {
			throw new PatchConflictException("Expected a mapping at path '" + patchEntry.getPath() + "' but found a different node type");
		}
	}

	private static YamlSequence asSequence(final YamlNode node, final PatchEntry patchEntry) throws PatchConflictException {
		if (node instanceof YamlSequence) {
			return (YamlSequence) node;
		} else {
			throw new PatchConflictException("Expected a sequence at path '" + patchEntry.getPath() + "' but found a different node type");
		}
	}

	/**
	 * Re-interprets a plain text value the same way YamlReader interprets an
	 * unquoted scalar: boolean, number, null, or string (in that priority).
	 * The diff text format does not preserve the original scalar type, so
	 * this is a best-effort reconstruction. Complex values given as YAML text
	 * are parsed.
	 */
	private static YamlNode createNodeFromText(final String valueText) throws Exception {
		if (valueText == null) {
			return new YamlScalar(null);
		} else if (valueText.startsWith(STRUCTURED_VALUE_MARKER)) {
			final YamlDocument document = YamlReader.readDocument(valueText.substring(STRUCTURED_VALUE_MARKER.length()));
			return document == null || document.getRoot() == null ? new YamlScalar(null) : document.getRoot();
		} else if ("true".equals(valueText) || "false".equals(valueText)) {
			return new YamlScalar(valueText, YamlScalarType.BOOLEAN);
		} else {
			// Not numeric texts become a string scalar
			return new YamlScalar(valueText, YamlScalarType.NUMBER);
		}
	}
}
