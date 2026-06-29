package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 *
 * Values are rendered as either the literal "null" or a double-quoted string
 * (see YamlComparator#formatValue). Quotes are stripped while parsing. Since
 * the diff text does not preserve the original scalar type, patched scalar
 * values are re-interpreted the same way YamlReader interprets unquoted
 * scalars (number, boolean, null, or string), to produce a reasonable type.
 */
public class YamlDiffPatcher {
	public enum PatchLineType {
		ADDED,
		REMOVED,
		CHANGED;
	}

	/**
	 * Single parsed patch instruction, prior to being applied.
	 */
	public static class PatchEntry {
		private final String path;
		private final PatchLineType type;
		private final String oldValueText;
		private final String newValueText;

		public PatchEntry(final String path, final PatchLineType type, final String oldValueText, final String newValueText) {
			this.path = path;
			this.type = type;
			this.oldValueText = oldValueText;
			this.newValueText = newValueText;
		}

		public String getPath() {
			return path;
		}

		public PatchLineType getType() {
			return type;
		}

		public String getOldValueText() {
			return oldValueText;
		}

		public String getNewValueText() {
			return newValueText;
		}
	}

	/**
	 * Thrown when a patch cannot be applied because the current value in the
	 * YAML tree does not match the expected "old value" from the diff, or when
	 * a structural precondition (node exists / does not exist) is violated.
	 */
	public static class PatchConflictException extends Exception {
		private static final long serialVersionUID = 1L;

		public PatchConflictException(final String message) {
			super(message);
		}
	}

	private static final Pattern ADDED_LINE_PATTERN = Pattern.compile("^\\+\\s+(.+?):\\s+(.+)$");
	private static final Pattern REMOVED_LINE_PATTERN = Pattern.compile("^-\\s+(.+?):\\s+(.+)$");
	private static final Pattern CHANGED_LINE_PATTERN = Pattern.compile("^~\\s+(.+?):\\s+(.+?)\\s+->\\s+(.+)$");

	/**
	 * Parses a diff text (as produced by YamlComparator#renderAsText) into a
	 * list of patch entries. Blank lines and the "No differences found"
	 * placeholder text are ignored. Lines that match none of the known
	 * patterns cause a PatchConflictException, since silently skipping
	 * unparsable lines could lead to an incomplete patch.
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
		final Matcher addedMatcher = ADDED_LINE_PATTERN.matcher(line);
		final Matcher removedMatcher = REMOVED_LINE_PATTERN.matcher(line);

		if (changedMatcher.matches()) {
			final String path = changedMatcher.group(1).trim();
			final String oldValueText = unquote(changedMatcher.group(2).trim());
			final String newValueText = unquote(changedMatcher.group(3).trim());
			return new PatchEntry(path, PatchLineType.CHANGED, oldValueText, newValueText);
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
	 * Strips the surrounding double quotes added by YamlComparator#formatValue,
	 * or returns null for the literal "null" marker.
	 */
	private static String unquote(final String valueText) {
		if ("null".equals(valueText)) {
			return null;
		} else if (valueText.length() >= 2 && valueText.startsWith("\"") && valueText.endsWith("\"")) {
			return valueText.substring(1, valueText.length() - 1);
		} else {
			// Not quoted (should not normally happen with formatValue output), use as-is
			return valueText;
		}
	}

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

	private static final Pattern PATH_SEGMENT_PATTERN = Pattern.compile("([^.\\[\\]]+)|\\[(\\d+)\\]");

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
				pathSegments.add(PathSegment.forKey(matcher.group(1)));
			} else {
				pathSegments.add(PathSegment.forIndex(Integer.parseInt(matcher.group(2))));
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
				parentSequence.add(createScalarFromText(patchEntry.getNewValueText()));
			}
		} else {
			final YamlMapping parentMapping = asMapping(navigationResult.parent, patchEntry);
			if (parentMapping.containsKey(lastSegment.key)) {
				throw new PatchConflictException("Cannot add, key already exists at path '" + patchEntry.getPath() + "'");
			} else {
				parentMapping.add(lastSegment.key, createScalarFromText(patchEntry.getNewValueText()));
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
			if (!parentMapping.containsKey(lastSegment.key)) {
				throw new PatchConflictException("Cannot remove, key missing at path '" + patchEntry.getPath() + "'");
			} else {
				final YamlNode currentChild = parentMapping.get(lastSegment.key);
				checkScalarValueMatches(currentChild, patchEntry.getOldValueText(), patchEntry);
				parentMapping.remove(lastSegment.key);
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
				checkScalarValueMatches(currentChild, patchEntry.getOldValueText(), patchEntry);
				parentSequence.set(lastSegment.index, createScalarFromText(patchEntry.getNewValueText()));
			}
		} else {
			final YamlMapping parentMapping = asMapping(navigationResult.parent, patchEntry);
			if (!parentMapping.containsKey(lastSegment.key)) {
				throw new PatchConflictException("Cannot change, key missing at path '" + patchEntry.getPath() + "'");
			} else {
				final YamlNode currentChild = parentMapping.get(lastSegment.key);
				checkScalarValueMatches(currentChild, patchEntry.getOldValueText(), patchEntry);
				parentMapping.replace(lastSegment.key, createScalarFromText(patchEntry.getNewValueText()));
			}
		}
	}

	private static void checkScalarValueMatches(final YamlNode currentChild, final String expectedValueText, final PatchEntry patchEntry) throws PatchConflictException {
		final String currentValueText = scalarToComparableText(currentChild);
		final boolean matches;
		if (currentValueText == null && expectedValueText == null) {
			matches = true;
		} else if (currentValueText == null || expectedValueText == null) {
			matches = false;
		} else {
			matches = currentValueText.equals(expectedValueText);
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
				if (!currentMapping.containsKey(segment.key)) {
					if (createMissing) {
						final YamlMapping newChildMapping = new YamlMapping();
						currentMapping.add(segment.key, newChildMapping);
						current = newChildMapping;
					} else {
						throw new PatchConflictException("Path segment '" + segment.key + "' missing at path '" + patchEntry.getPath() + "'");
					}
				} else {
					current = currentMapping.get(segment.key);
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
	 * this is a best-effort reconstruction.
	 */
	private static YamlScalar createScalarFromText(final String valueText) {
		if (valueText == null) {
			return new YamlScalar(null);
		} else if ("true".equals(valueText) || "false".equals(valueText)) {
			return new YamlScalar(valueText, YamlScalarType.BOOLEAN);
		} else {
			try {
				return new YamlScalar(valueText, YamlScalarType.NUMBER);
			} catch (@SuppressWarnings("unused") final NumberFormatException e) {
				return new YamlScalar(valueText, YamlScalarType.STRING);
			}
		}
	}
}