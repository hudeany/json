package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.path.JsonPathArrayElement;
import de.soderer.json.path.JsonPathPropertyElement;
import de.soderer.json.utilities.BasicReadAheadReader;
import de.soderer.json.utilities.Utilities;
import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlMultilineScalarChompingType;
import de.soderer.yaml.data.YamlMultilineScalarType;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.data.YamlStringQuoteType;
import de.soderer.yaml.data.YamlVersion;
import de.soderer.yaml.data.directive.YamlDirective;
import de.soderer.yaml.data.directive.YamlTagDirective;
import de.soderer.yaml.data.directive.YamlVersionDirective;
import de.soderer.yaml.exception.FoundPathEvent;
import de.soderer.yaml.exception.YamlParseException;

/**
 * Reader for YAML data from an input stream.
 * <p>
 * A document is read completely by {@link #readDocument()}. Large data can be read item by item:
 * {@link #readUpToPath(String)} positions the reader at a sequence, then
 * {@link #readNextYamlNode()} returns its items one after another.
 * </p>
 * <p>
 * Comments, anchors, quote styles, block scalar styles and empty lines are kept in the nodes, so
 * that {@link YamlWriter} can write the data in a similar form again. Aliases are kept as
 * {@link de.soderer.yaml.data.YamlAlias} nodes. Linebreaks are normalized to LF.
 * </p>
 * <p>
 * After a document (or an item read by {@link #readNextYamlNode()}) is read, all its aliases are
 * checked: each alias must refer to an anchor defined before it, and it must not refer to a node
 * containing the alias itself (cyclic reference).
 * </p>
 * <p>
 * Comments after the last document, also in data without any document, are available by
 * {@link #getTrailingComments()} once the end of the data was reached.
 * </p>
 */
public class YamlReader extends BasicReadAheadReader {
	private final Stack<Integer> indentations = new Stack<>();
	private final Map<String, YamlNode> anchorTable = new HashMap<>();
	private List<String> pendingLeadingComments = new ArrayList<>();
	private int pendingLeadingEmptyLines = 0;
	private int pendingTrailingEmptyLinesAfterComments = 0;
	/**
	 * Comment lines after the last document, which belong to no document.
	 */
	private final List<String> trailingComments = new ArrayList<>();
	/**
	 * Positions (line, column) of the aliases read for the current document or item, used for
	 * error messages of the alias check.
	 */
	private final Map<YamlAlias, long[]> aliasPositions = new IdentityHashMap<>();
	/**
	 * True if a document was read, which was not ended by a document end marker "..." yet. Only a
	 * document start marker "---" may follow then.
	 */
	private boolean documentOpen = false;

	/**
	 * Named tag handles like "!e!" declared by "%TAG" directives of the current document.
	 */
	private final Set<String> currentTagHandles = new HashSet<>();

	/**
	 * True if a "%TAG !! ..." directive of the current document assigns the secondary tag handle
	 * "!!" to other tags than the YAML core schema, so "!!int" is no datatype then.
	 */
	private boolean secondaryTagHandleRedefined = false;

	/**
	 * Line and column of content preceded by a tab in the whitespace before it. A block collection
	 * must not start there, because tabs must not be used for indentation. -1 if there is none.
	 */
	private long tabSeparatedContentLine = -1;

	/**
	 * Column belonging to {@link #tabSeparatedContentLine}.
	 */
	private long tabSeparatedContentColumn = -1;

	/**
	 * Line on which no block collection may start, like the line of a document start marker or the
	 * rest of the line of a block mapping key. -1 if there is none.
	 */
	private long blockCollectionForbiddenLine = -1;

	/**
	 * Number of characters the reader looks ahead.
	 */
	private static final int READ_AHEAD_CHARACTERS = 4;

	/**
	 * Default maximum nesting depth of mappings and sequences.
	 */
	public static final int DEFAULT_MAXIMUM_NESTING_DEPTH = 500;

	/**
	 * Maximum allowed nesting depth of mappings and sequences.
	 */
	private int maximumNestingDepth = DEFAULT_MAXIMUM_NESTING_DEPTH;

	/**
	 * Tag read before content on the same line, like "!tag key: value". It belongs to the first key,
	 * if the content is a block mapping, otherwise to the node. Null if there is none.
	 */
	private String pendingNodeTag = null;

	/**
	 * Line of the content following {@link #pendingNodeTag}.
	 */
	private long pendingNodeTagLine = -1;

	/**
	 * Column of the content following {@link #pendingNodeTag}.
	 */
	private long pendingNodeTagColumn = -1;

	/**
	 * Column of {@link #pendingNodeTag} itself, which is the column of a key it belongs to.
	 */
	private long pendingNodeTagStartColumn = -1;

	/**
	 * Anchor read before content on the same line, like "&amp;anchor key: value". Like
	 * {@link #pendingNodeTag} it belongs to the first key, if the content is a block mapping,
	 * otherwise to the node. It shares the position of {@link #pendingNodeTag}.
	 */
	private String pendingNodeAnchor = null;

	/**
	 * Nesting level of flow collections, in which linebreaks have no meaning.
	 */
	private int flowLevel = 0;

	/**
	 * Indentations of the block structure before each flow collection currently read, which are
	 * restored after its closing bracket.
	 */
	private final Deque<List<Integer>> flowIndentationSnapshots = new ArrayDeque<>();

	/**
	 * Blanks before the root node on its line, which are not part of the indentations yet.
	 */
	private int rootLineIndentation = 0;

	/**
	 * Indentation of the block collection the node currently read belongs to: the column (0-based)
	 * of the mapping keys for a mapping value, the column of the dash for a sequence item, -1 for the
	 * root node of a document. Continuation lines of multi-line flow scalars and the content of
	 * block scalars must be indented deeper than this.
	 */
	private int blockParentIndentation = -1;

	/**
	 * Current nesting depth of mappings and sequences while parsing.
	 */
	private int nestingDepth = 0;

	/**
	 * YAML version for documents without "%YAML" directive.
	 */
	private YamlVersion defaultYamlVersion = YamlVersion.V1_2;

	/**
	 * YAML version of the document currently read.
	 */
	private YamlVersion currentYamlVersion = YamlVersion.V1_2;

	/**
	 * Path of the current read position.
	 */
	protected JsonPath currentPath = new JsonPath();
	/**
	 * Path searched by {@link #readUpToPath(String)}, null if no search is active.
	 */
	protected JsonPath searchPath = null;
	/**
	 * Indentation of the found search path, -1 if no path was found.
	 */
	protected int searchPathWasFoundAtLevel = -1;

	/**
	 * Creates a new YAML reader using UTF-8 encoding.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @throws Exception
	 *             if the input stream is null
	 */
	public YamlReader(final InputStream inputStream) throws Exception {
		this(inputStream, null);
	}

	/**
	 * Creates a new YAML reader.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @param encodingCharset
	 *            the encoding of the data, or null for UTF-8
	 * @throws Exception
	 *             if the input stream is null
	 */
	public YamlReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		// Four characters read ahead: a document marker "---" or "..." is only one, if a blank or linebreak follows
		super(inputStream, encodingCharset, READ_AHEAD_CHARACTERS);

		setNormalizeLinebreaks(true);

		indentationsAdd(0);
	}

	/**
	 * Returns the maximum allowed nesting depth of mappings and sequences.
	 *
	 * @return the maximum nesting depth
	 */
	public int getMaximumNestingDepth() {
		return maximumNestingDepth;
	}

	/**
	 * Sets the maximum allowed nesting depth of mappings and sequences. Deeper nested data is
	 * rejected by a {@link YamlParseException} instead of risking a {@link StackOverflowError}.
	 * Default is {@link #DEFAULT_MAXIMUM_NESTING_DEPTH}.
	 *
	 * @param maximumNestingDepth
	 *            the maximum nesting depth, must be at least 1
	 */
	public void setMaximumNestingDepth(final int maximumNestingDepth) {
		if (maximumNestingDepth < 1) {
			throw new IllegalArgumentException("Maximum nesting depth must be at least 1");
		}
		this.maximumNestingDepth = maximumNestingDepth;
	}

	/**
	 * Sets the maximum allowed nesting depth of mappings and sequences, see
	 * {@link #setMaximumNestingDepth(int)}.
	 *
	 * @param newMaximumNestingDepth
	 *            the maximum nesting depth, must be at least 1
	 * @return this reader
	 */
	public YamlReader withMaximumNestingDepth(final int newMaximumNestingDepth) {
		setMaximumNestingDepth(newMaximumNestingDepth);
		return this;
	}

	/**
	 * Returns the YAML version for documents without "%YAML" directive.
	 *
	 * @return the default YAML version
	 */
	public YamlVersion getDefaultYamlVersion() {
		return defaultYamlVersion;
	}

	/**
	 * Sets the YAML version for documents without "%YAML" directive. It decides how plain scalars are
	 * resolved, see {@link YamlVersion}. A "%YAML" directive of a document takes precedence. Default
	 * is {@link YamlVersion#V1_2}.
	 *
	 * @param defaultYamlVersion
	 *            the default YAML version, not null
	 */
	public void setDefaultYamlVersion(final YamlVersion defaultYamlVersion) {
		if (defaultYamlVersion == null) {
			throw new IllegalArgumentException("Default YAML version must not be null");
		}
		this.defaultYamlVersion = defaultYamlVersion;
		currentYamlVersion = defaultYamlVersion;
	}

	/**
	 * Sets the YAML version for documents without "%YAML" directive, see
	 * {@link #setDefaultYamlVersion(YamlVersion)}.
	 *
	 * @param newDefaultYamlVersion
	 *            the default YAML version, not null
	 * @return this reader
	 */
	public YamlReader withDefaultYamlVersion(final YamlVersion newDefaultYamlVersion) {
		setDefaultYamlVersion(newDefaultYamlVersion);
		return this;
	}

	/**
	 * Enters the next nesting level of a mapping or sequence. The caller must decrement
	 * {@link #nestingDepth} in a finally block.
	 *
	 * @throws YamlParseException
	 *             if the maximum nesting depth is exceeded
	 */
	private void enterNesting() throws YamlParseException {
		nestingDepth++;
		if (nestingDepth > maximumNestingDepth) {
			nestingDepth--;
			throw new YamlParseException("Maximum nesting depth of " + maximumNestingDepth + " exceeded", getCurrentLine(), getCurrentColumn());
		}
	}

	/**
	 * Checks the current nesting depth for a level that was entered without check.
	 *
	 * @throws YamlParseException
	 *             if the maximum nesting depth is exceeded
	 */
	private void checkNestingDepth() throws YamlParseException {
		if (nestingDepth > maximumNestingDepth) {
			throw new YamlParseException("Maximum nesting depth of " + maximumNestingDepth + " exceeded", getCurrentLine(), getCurrentColumn());
		}
	}

	/**
	 * Reads the next YAML document. Multiple documents separated by "---" are read by repeated
	 * calls.
	 *
	 * @return the document, or null if there is no further document
	 * @throws Exception
	 *             if a path search was started before, or the YAML data is invalid
	 */
	public YamlDocument readDocument() throws Exception {
		if (searchPath != null || searchPathWasFoundAtLevel >= 0) {
			throw new Exception("Search path was already started before by method 'readUpToPath'");
		} else {
			return parseDocument();
		}
	}

	/**
	 * Returns the node with an anchor read so far.
	 *
	 * @param anchorName
	 *            the anchor name without "&amp;"
	 * @return the node
	 * @throws Exception
	 *             if no anchor with this name was read
	 */
	public YamlNode getYamlNodeByAnchor(final String anchorName) throws Exception {
		if (!anchorTable.containsKey(anchorName)) {
			throw new Exception("There is no anchor defined with name '" + anchorName + "'");
		}
		return anchorTable.get(anchorName);
	}

	/**
	 * Returns the comment lines after the last document, which belong to no document. This also
	 * covers data consisting of comments only, for which {@link #readDocument()} returns null.
	 * <p>
	 * The list is complete once the end of the data was reached, which is sure after
	 * {@link #readDocument()} returned null. Before that, comments which may still belong to a
	 * following document are not part of it.
	 * </p>
	 *
	 * @return the comment lines without "#", empty list if there are none
	 * @throws Exception
	 *             if reading fails
	 */
	public List<String> getTrailingComments() throws Exception {
		if (searchPath == null && searchPathWasFoundAtLevel < 0 && isEOF() && !pendingLeadingComments.isEmpty()) {
			// The data ended directly after the last document, so the pending comments follow no document
			trailingComments.addAll(pendingLeadingComments);
			pendingLeadingComments = new ArrayList<>();
		}
		return new ArrayList<>(trailingComments);
	}

	private YamlDocument parseDocument() throws Exception {
		try {
			final YamlDocument document = new YamlDocument();
			currentPath = new JsonPath();
			currentYamlVersion = defaultYamlVersion;
			blockParentIndentation = -1;
			if (!pendingLeadingComments.isEmpty()) {
				for (final String commentLine : pendingLeadingComments) {
					document.addLeadingComment(commentLine);
				}
				pendingLeadingComments = new ArrayList<>();
			}
			// Note: YamlDocument is not a YamlNode and has no leadingEmptyLinesCount of its own;
			// any pendingLeadingEmptyLines accumulated so far are intentionally left untouched
			// here so they naturally end up attributed to the document's root node instead.

			boolean directivesRead = false;
			boolean yamlDirectiveRead = false;
			secondaryTagHandleRedefined = false;
			currentTagHandles.clear();
			while (true) {
				// Each document starts at the beginning of the block structure again
				indentations.clear();
				indentationsAdd(0);

				// Blanks before a root node on the current line, which readUpToNextContent() skips
				// without adding them to the indentations
				final long lineBeforeContent = getCurrentLine();
				final int leadingBlanks = getCurrentColumn() == 1 ? skipAndCountBlanks() : 0;
				// An anchor of the root node is read by parseYamlNode()
				skipToNextContent();
				rootLineIndentation = getCurrentLine() == lineBeforeContent ? leadingBlanks : 0;

				if (isEOF()) {
					if (directivesRead) {
						throw new YamlParseException("Directives without following document", getCurrentLine(), getCurrentColumn());
					}
					// No further document: data of only blanks and comments contains no document at all.
					// Comments read so far follow the last document and belong to no document.
					if (document.getLeadingComments() != null) {
						trailingComments.addAll(document.getLeadingComments());
					}
					trailingComments.addAll(pendingLeadingComments);
					pendingLeadingComments = new ArrayList<>();
					documentOpen = false;
					return null;
				} else if (peekCharMatch('%') && getCurrentColumn() == 1) {
					if (documentOpen) {
						throw new YamlParseException("Directive after a document, which was not ended by \"...\"", getCurrentLine(), getCurrentColumn());
					}
					final YamlDirective<?> directive = readDirective();
					directivesRead = true;
					if (directive instanceof final YamlVersionDirective versionDirective) {
						if (yamlDirectiveRead) {
							throw new YamlParseException("Duplicate %YAML directive", getCurrentLine(), getCurrentColumn());
						}
						yamlDirectiveRead = true;
						final YamlVersion directiveVersion = YamlVersion.fromDirectiveValue(versionDirective.getYamlVersion());
						if (directiveVersion != null) {
							currentYamlVersion = directiveVersion;
						}
					}
					if (directive instanceof final YamlTagDirective tagDirective) {
						currentTagHandles.add(tagDirective.getTag());
					}
					if (directive instanceof final YamlTagDirective tagDirective && "!!".equals(tagDirective.getTag())) {
						// "!!" refers to other tags than the YAML core schema datatypes
						secondaryTagHandleRedefined = !"tag:yaml.org,2002:".equals(tagDirective.getReplacement());
					}
					if (directive != null) {
						document.addDirective(directive);
					}
				} else if (isDocumentMarkerAtLineStart() && peekCharMatch('.')) {
					// Document end marker
					if (directivesRead) {
						throw new YamlParseException("Directives without following document", getCurrentLine(), getCurrentColumn());
					}
					readChar();
					readChar();
					readChar();
					skipBlanks();
					if (peekCharMatch('#')) {
						pendingLeadingComments.add(readInlineComment());
					} else if (isNotEOF() && peekCharNotMatch('\n')) {
						throw new YamlParseException("Unexpected content after document end marker", getCurrentLine(), getCurrentColumn());
					}
					documentOpen = false;
				} else if (isDocumentMarkerAtLineStart()) {
					// Document start marker
					if (searchPath != null && getCurrentPath().equals(searchPath)) {
						throw new FoundPathEvent("Path '" + searchPath + "' was found");
					}
					readChar();
					readChar();
					readChar();
					documentOpen = true;
					final long markerLine = getCurrentLine();
					skipBlanks();
					if (peekCharMatch('#')) {
						document.addLeadingComment(readInlineComment());
					}
					skipToNextContent();
					if (isEOF() || isDocumentMarkerAtLineStart()) {
						// Empty document
						return document;
					}
					if (getCurrentLine() == markerLine) {
						// A block collection must not start on the line of the document start marker
						blockCollectionForbiddenLine = markerLine;
					}
					document.setRoot(parseYamlNode());
					checkAliases(document.getRoot(), new HashMap<>());
					return document;
				} else {
					// Bare document without document start marker
					if (documentOpen) {
						throw new YamlParseException("Unexpected content after the end of the document", getCurrentLine(), getCurrentColumn());
					} else if (directivesRead) {
						throw new YamlParseException("Directives must be followed by a document start marker \"---\"", getCurrentLine(), getCurrentColumn());
					}
					documentOpen = true;
					if (searchPath != null && getCurrentPath().equals(searchPath)) {
						throw new FoundPathEvent("Path '" + searchPath + "' was found");
					}
					addRootIndentation();
					document.setRoot(parseYamlNode());
					checkAliases(document.getRoot(), new HashMap<>());
					return document;
				}
			}
		} catch (final DuplicateKeyException e) {
			throw new YamlParseException("Duplicate key found: " + e.getMessage(), getCurrentLine(), getCurrentColumn());
		}
	}

	/**
	 * Reads up to a path, so the following {@link #readNextYamlNode()} calls return the items of the
	 * sequence at this path.
	 *
	 * @param yamlPathString
	 *            the path in JSON path syntax, e.g. "$.list.customers"
	 * @throws Exception
	 *             if the path is not part of the YAML data or the data is invalid
	 */
	public void readUpToPath(final String yamlPathString) throws Exception {
		searchPath = new JsonPath(yamlPathString);

		try {
			parseDocument();
		} catch (@SuppressWarnings("unused") final FoundPathEvent e) {
			// Expected FoundPathEvent
		}

		if (!getCurrentPath().equals(searchPath)) {
			throw new Exception("Path '" + yamlPathString + "' is not part of the YAML data");
		} else {
			searchPathWasFoundAtLevel = getNumberOfIndentationChars();
			searchPath = null;
		}
	}

	/**
	 *
	 * JsonPath syntax:<br />
	 *	$ : root<br />
	 *	. : child separator<br />
	 *	[n] : array operator<br />
	 *<br />
	 * JsonPath example:<br />
	 * 	"$.list.customer[0].name"<br />
	 */
	private JsonPath getCurrentPath() {
		return currentPath;
	}

	/**
	 * Immutable snapshot of the pending leading comments and pending leading/post-comment empty
	 * lines counts at a certain point during parsing, taken before recursively parsing a nested
	 * node whose own parsing may collect further pending leading state (which then belongs to
	 * whatever follows that nested node, not to the nested node itself).
	 */
	private static final class PendingLeadingState {
		private final List<String> comments;
		private final int emptyLinesCount;
		private final int postCommentEmptyLinesCount;

		private PendingLeadingState(final List<String> comments, final int emptyLinesCount, final int postCommentEmptyLinesCount) {
			this.comments = comments;
			this.emptyLinesCount = emptyLinesCount;
			this.postCommentEmptyLinesCount = postCommentEmptyLinesCount;
		}
	}

	/**
	 * Takes away the currently pending leading comments and pending empty lines counts,
	 * resetting all of them to their empty/zero state, so that further parsing does not
	 * attribute them to the wrong node.
	 */
	private PendingLeadingState takePendingLeadingState() {
		final PendingLeadingState pendingLeadingState = new PendingLeadingState(
				pendingLeadingComments, pendingLeadingEmptyLines, pendingTrailingEmptyLinesAfterComments);
		pendingLeadingComments = new ArrayList<>();
		pendingLeadingEmptyLines = 0;
		pendingTrailingEmptyLinesAfterComments = 0;
		return pendingLeadingState;
	}

	/**
	 * Applies a previously taken {@link PendingLeadingState} onto the given node.
	 */
	private static void applyPendingLeadingState(final PendingLeadingState pendingLeadingState, final YamlNode node) {
		if (!pendingLeadingState.comments.isEmpty()) {
			for (final String commentLine : pendingLeadingState.comments) {
				node.addLeadingComment(commentLine);
			}
		}
		if (pendingLeadingState.emptyLinesCount > 0) {
			node.setLeadingEmptyLinesCount(pendingLeadingState.emptyLinesCount);
		}
		if (pendingLeadingState.postCommentEmptyLinesCount > 0) {
			node.setPostCommentEmptyLinesCount(pendingLeadingState.postCommentEmptyLinesCount);
		}
	}

	/**
	 * Transfers all currently pending leading comments and the currently pending empty lines
	 * counts directly onto the given node and clears all pending states.
	 */
	private void applyAndClearPendingLeadingState(final YamlNode node) {
		if (!pendingLeadingComments.isEmpty()) {
			for (final String commentLine : pendingLeadingComments) {
				node.addLeadingComment(commentLine);
			}
			pendingLeadingComments = new ArrayList<>();
		}
		if (pendingLeadingEmptyLines > 0) {
			node.setLeadingEmptyLinesCount(pendingLeadingEmptyLines);
			pendingLeadingEmptyLines = 0;
		}
		if (pendingTrailingEmptyLinesAfterComments > 0) {
			node.setPostCommentEmptyLinesCount(pendingTrailingEmptyLinesAfterComments);
			pendingTrailingEmptyLinesAfterComments = 0;
		}
	}

	/**
	 * Adds newly found blank source lines to the correct pending bucket: lines found before any
	 * comment is currently pending are counted towards the leading empty lines (written before
	 * the comments), while lines found once at least one comment is already pending are counted
	 * towards the post-comment empty lines (written after the comments, still before the node).
	 */
	private void addPendingEmptyLines(final int count) {
		if (count <= 0) {
			return;
		}
		if (pendingLeadingComments.isEmpty()) {
			pendingLeadingEmptyLines += count;
		} else {
			pendingTrailingEmptyLinesAfterComments += count;
		}
	}

	/**
	 * Reads the next item of the sequence found by {@link #readUpToPath(String)}.
	 *
	 * @return the next item, or null if the sequence has no further items
	 * @throws Exception
	 *             if the YAML data is invalid
	 */
	public YamlNode readNextYamlNode() throws Exception {
		try {
			if (isEOF()) {
				return null;
			} else if (searchPathWasFoundAtLevel >= 0 && getNumberOfIndentationChars() == searchPathWasFoundAtLevel) {
				// Aliases of the item may refer to anchors read before it
				final Map<String, YamlNode> anchorsReadBefore = new HashMap<>(anchorTable);
				final YamlNode nextSequenceItem = readNextSequenceItem();
				checkAliases(nextSequenceItem, anchorsReadBefore);
				return nextSequenceItem;
			} else {
				return null;
			}
		} catch (@SuppressWarnings("unused") final FoundPathEvent e) {
			throw new Exception("Cannot read items of path");
		}
	}

	/**
	 * Reads a YAML document from a string.
	 *
	 * @param yamlDocumentString
	 *            the YAML text
	 * @return the first document, or null for empty data
	 * @throws Exception
	 *             if the YAML data is invalid
	 */
	public static YamlDocument readDocument(final String yamlDocumentString) throws Exception {
		try (final YamlReader yamlReader = new YamlReader(new ByteArrayInputStream(yamlDocumentString.getBytes(StandardCharsets.UTF_8)))) {
			return yamlReader.readDocument();
		}
	}

	private int skipBlanks() throws Exception {
		int count = 0;
		boolean tabRead = false;
		while (peekCharMatch(' ') || peekCharMatch('\t')) {
			tabRead |= peekCharMatch('\t');
			readChar();
			count++;
		}
		if (tabRead && isNotEOF()) {
			// Remember the position, a block collection must not start after a tab
			tabSeparatedContentLine = getCurrentLine();
			tabSeparatedContentColumn = getCurrentColumn();
		}
		return count;
	}

	/**
	 * Checks that a block collection starting at the given position is not preceded by a tab.
	 *
	 * @throws YamlParseException
	 *             if a tab precedes the position on its line
	 */
	private void checkNoTabBeforeBlockCollection(final long line, final long column) throws YamlParseException {
		if (line == tabSeparatedContentLine && column == tabSeparatedContentColumn) {
			throw new YamlParseException("Tabs must not be used for indentation of block collections", line, column);
		}
	}

	/**
	 * @return
	 * 	null: same unchanged level of indentation<br />
	 * 	true: new added level of indentation<br />
	 * 	false: one or more levels if indentation reduced<br />
	 *
	 * @throws Exception
	 */
	private Boolean skipEmptyLinesAndReadNextIndentationAndLeadingComments() throws Exception {
		if (peekCharNotMatch(' ') && peekCharNotMatch('\t') && peekCharNotMatch('\n') && peekCharNotMatch('#')) {
			// Prevent multiple skip of indentation without intention
			return null;
		}

		int nextIndentationSize = 0;
		// If the reader is currently positioned mid-line (i.e. after some content, such as a
		// scalar value or an already consumed inline comment, was read by the caller), that
		// line already has content and its upcoming linebreak must not be counted as an empty
		// line. Only lines consisting purely of whitespace (or nothing) between two linebreaks
		// are counted as empty lines.
		boolean currentLineHasContent = getCurrentColumn() > 1;
		boolean linebreakRead = false;
		boolean tabInLinePrefix = false;
		boolean lineStartsWithTab = false;
		// Spaces at the beginning of the current line before any tab, which are its indentation
		int lineIndentationSpaces = 0;
		while (isNotEOF()) {
			if (peekCharMatch(' ') || peekCharMatch('\t')) {
				if (peekCharMatch('\t')) {
					tabInLinePrefix = true;
					lineStartsWithTab |= getCurrentColumn() == 1;
				} else if (!tabInLinePrefix && !currentLineHasContent) {
					lineIndentationSpaces++;
				}
				readChar();
				nextIndentationSize++;
			} else if (peekCharMatch('#')) {
				readChar();
				final String commentText = readUpToNext(false, null, '\n');
				pendingLeadingComments.add(commentText);
				nextIndentationSize = getNumberOfIndentationChars();
				currentLineHasContent = true;
			} else if (peekCharMatch('\n')) {
				readChar();
				if (!currentLineHasContent) {
					addPendingEmptyLines(1);
				}
				currentLineHasContent = false;
				nextIndentationSize = 0;
				linebreakRead = true;
				tabInLinePrefix = false;
				lineStartsWithTab = false;
				lineIndentationSpaces = 0;
			} else {
				break;
			}
		}

		if (tabInLinePrefix && isNotEOF()) {
			// Remember the position, a block collection must not start after a tab
			tabSeparatedContentLine = getCurrentLine();
			tabSeparatedContentColumn = getCurrentColumn();
		}

		if (flowLevel > 0 && linebreakRead && isNotEOF()) {
			// Lines of flow collections must be indented deeper than the surrounding block collection
			if (lineIndentationSpaces <= blockParentIndentation && peekCharNotMatch('#')) {
				throw new YamlParseException("Line of flow collection is not indented enough", getCurrentLine(), getCurrentColumn());
			} else if (isDocumentMarkerAtLineStart()) {
				throw new YamlParseException("Document marker within flow collection", getCurrentLine(), getCurrentColumn());
			} else if (lineStartsWithTab && blockParentIndentation >= 0 && peekCharNotMatch('#')) {
				throw new YamlParseException("Tabs must not be used for indentation", getCurrentLine(), getCurrentColumn());
			}
		}

		if (flowLevel > 0) {
			// Lines within flow collections do not change the block indentation
			return null;
		}

		for (final int indentationSize : indentations) {
			nextIndentationSize -= indentationSize;
		}

		if (nextIndentationSize == 0) {
			return null;
		} else if (nextIndentationSize > 0) {
			indentationsAdd(nextIndentationSize);
			return true;
		} else {
			// First entry is always [0]
			while (nextIndentationSize < 0 && indentations.size() > 1) {
				nextIndentationSize += indentations.pop();
			}
			if (nextIndentationSize == 0) {
				return false;
			} else {
				throw new YamlParseException("Unexpected indentation level: " + nextIndentationSize + " indentations: " + indentations, getCurrentLine(), getCurrentColumn());
			}
		}
	}

	/**
	 * Reads the leading blanks of the current line without changing the {@link #indentations} stack.
	 *
	 * @return the number of leading spaces and tabs read
	 */
	private int readIndentationOfCurrentLine() throws Exception {
		int indentationSize = 0;
		while (peekCharMatch(' ') || peekCharMatch('\t')) {
			readChar();
			indentationSize++;
		}
		return indentationSize;
	}

	/**
	 * Updates the {@link #indentations} stack for a line with the given indentation.
	 *
	 * @param indentationOfLine
	 *            the number of leading spaces and tabs of the line
	 * @return
	 * 	null: same unchanged level of indentation<br />
	 * 	true: new added level of indentation<br />
	 * 	false: one or more levels if indentation reduced<br />
	 * @throws YamlParseException
	 *             if the indentation does not match any known level
	 */
	private Boolean applyIndentationLevel(final int indentationOfLine) throws YamlParseException {
		int nextIndentationSize = indentationOfLine;
		for (final int indentationSize : indentations) {
			nextIndentationSize -= indentationSize;
		}

		if (nextIndentationSize == 0) {
			return null;
		} else if (nextIndentationSize > 0) {
			indentationsAdd(nextIndentationSize);
			return true;
		} else {
			// First entry is always [0]
			while (nextIndentationSize < 0 && indentations.size() > 1) {
				nextIndentationSize += indentations.pop();
			}
			if (nextIndentationSize == 0) {
				return false;
			} else {
				throw new YamlParseException("Unexpected indentation level: " + nextIndentationSize + " indentations: " + indentations, getCurrentLine(), getCurrentColumn());
			}
		}
	}

	private String readInlineComment() throws Exception {
		if (peekCharNotMatch('#')) {
			throw new YamlParseException("Expected comment not found", getCurrentLine(), getCurrentColumn());
		}

		readChar();

		final String commentText = readUpToNext(false, null, '\n');
		return commentText;
	}

	/**
	 * Reads a "%YAML" or "%TAG" directive. Other directives are reserved for future use and ignored,
	 * as the YAML specification requires.
	 *
	 * @return the directive, or null for an unknown directive
	 * @throws Exception
	 *             if the directive data is invalid
	 */
	private YamlDirective<?> readDirective() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		if (peekCharNotMatch('%')) {
			throw new YamlParseException("Expected directive not found", startLine, startColumn);
		}

		final String directiveText = readUpToNext(false, null, '#', '\n');
		final YamlDirective<?> directive;
		try {
			if (directiveText.toUpperCase().startsWith("%YAML ")) {
				directive = new YamlVersionDirective(directiveText.substring(6));
			} else if (directiveText.toUpperCase().startsWith("%TAG ")) {
				directive = new YamlTagDirective(directiveText.substring(5));
			} else {
				// Reserved for future use: the YAML specification (chapter 6.8) requires to ignore unknown directives
				if (peekCharMatch('#')) {
					readInlineComment();
				}
				return null;
			}
		} catch (final Exception e) {
			throw new YamlParseException("Invalid YAML directive data '" + directiveText + "': " + e.getMessage(), startLine, startColumn, e);
		}

		checkCommentSeparation();
		if (peekCharMatch('#')) {
			directive.setInlineComment(readInlineComment());
		}

		return directive;
	}

	private YamlNode parseYamlNode() throws Exception {
		return parseYamlNode(null, false);
	}

	/**
	 * Assigns an anchor to a node and registers it. An anchor defined again replaces the former
	 * definition for the following aliases.
	 *
	 * @param node
	 *            the node
	 * @param anchor
	 *            the anchor name
	 * @throws YamlParseException
	 *             if the node already has another anchor
	 */
	private void assignAnchor(final YamlNode node, final String anchor) throws YamlParseException {
		if (node instanceof YamlAlias) {
			throw new YamlParseException("An alias must not have an anchor: '" + anchor + "'", getCurrentLine(), getCurrentColumn());
		}
		if (node.getAnchorName() != null && !node.getAnchorName().equals(anchor)) {
			throw new YamlParseException("Assignment of multiple anchors single YAML node: '" + node.getAnchorName() + "' and '" + anchor + "'", getCurrentLine(), getCurrentColumn());
		}
		node.setAnchorName(anchor);
		anchorTable.put(anchor, node);
	}

	/**
	 * Tag and anchor waiting for the first key of a block mapping, see {@link #pendingNodeTag}.
	 */
	private static final class PendingNodeProperties {
		/** The tag, or null. */
		private final String tag;
		/** The anchor name, or null. */
		private final String anchor;
		/** Line of the content following the properties. */
		private final long line;
		/** Column of the content following the properties. */
		private final long column;
		/** Column of the first property, which is the column of a key they belong to. */
		private final long startColumn;

		/**
		 * Creates pending properties.
		 *
		 * @param tag
		 *            the tag, or null
		 * @param anchor
		 *            the anchor name, or null
		 * @param line
		 *            line of the content following the properties
		 * @param column
		 *            column of the content following the properties
		 * @param startColumn
		 *            column of the first property
		 */
		private PendingNodeProperties(final String tag, final String anchor, final long line, final long column, final long startColumn) {
			this.tag = tag;
			this.anchor = anchor;
			this.line = line;
			this.column = column;
			this.startColumn = startColumn;
		}
	}

	/**
	 * Returns the current pending tag and anchor, to restore them after a nested node.
	 *
	 * @return the pending properties
	 */
	private PendingNodeProperties savePendingNodeProperties() {
		return new PendingNodeProperties(pendingNodeTag, pendingNodeAnchor, pendingNodeTagLine, pendingNodeTagColumn, pendingNodeTagStartColumn);
	}

	/**
	 * Restores pending tag and anchor saved by {@link #savePendingNodeProperties()}.
	 *
	 * @param properties
	 *            the saved properties
	 */
	private void restorePendingNodeProperties(final PendingNodeProperties properties) {
		pendingNodeTag = properties.tag;
		pendingNodeAnchor = properties.anchor;
		pendingNodeTagLine = properties.line;
		pendingNodeTagColumn = properties.column;
		pendingNodeTagStartColumn = properties.startColumn;
	}

	/**
	 * Takes the pending tag and anchor, if they precede the current position. Used for flow
	 * collections, to which properties directly before them always belong, also if the collection is
	 * a mapping key.
	 *
	 * @return the properties taken, or null if there are none for the current position
	 */
	private PendingNodeProperties takePendingNodePropertiesAtCurrentPosition() {
		if ((pendingNodeTag != null || pendingNodeAnchor != null) && pendingNodeTagLine == getCurrentLine() && pendingNodeTagColumn == getCurrentColumn()) {
			final PendingNodeProperties properties = savePendingNodeProperties();
			pendingNodeTag = null;
			pendingNodeAnchor = null;
			return properties;
		} else {
			return null;
		}
	}

	/**
	 * Applies properties taken by {@link #takePendingNodePropertiesAtCurrentPosition()} to a node.
	 *
	 * @param properties
	 *            the properties, may be null
	 * @param node
	 *            the node
	 * @return the node, which is a different node if a datatype tag changed it
	 * @throws Exception
	 *             if the node does not fit a datatype tag or has another anchor
	 */
	private YamlNode applyPendingNodeProperties(final PendingNodeProperties properties, final YamlNode node) throws Exception {
		YamlNode result = node;
		if (properties != null) {
			if (properties.tag != null) {
				result = applyTag(properties.tag, result);
			}
			if (properties.anchor != null) {
				assignAnchor(result, properties.anchor);
			}
		}
		return result;
	}

	/**
	 * Sets tag and anchor for the content at the current position, which belong to its first key if
	 * the content is a block mapping.
	 *
	 * @param tag
	 *            the tag, or null
	 * @param anchor
	 *            the anchor name, or null
	 * @param startColumn
	 *            the column of the first property, which is the column of the key
	 */
	private void setPendingNodeProperties(final String tag, final String anchor, final long startColumn) {
		pendingNodeTag = tag;
		pendingNodeAnchor = anchor;
		pendingNodeTagLine = getCurrentLine();
		pendingNodeTagColumn = getCurrentColumn();
		pendingNodeTagStartColumn = startColumn;
	}

	/**
	 * Reads a node, optionally preceded by a tag like "!!str".
	 *
	 * @param parentIndentation
	 *            the indentation of the parent (mapping key or sequence dash), or null if unknown. A tag
	 *            at the end of a line is only applied to content on the following lines indented deeper
	 *            than the parent, otherwise it tags an empty value.
	 * @param allowSequenceAtParentIndentation
	 *            true if a block sequence at the parent indentation is the content, as for mapping values
	 */
	private YamlNode parseYamlNode(final Integer parentIndentation, final boolean allowSequenceAtParentIndentation) throws Exception {
		// Kept small, because it is part of the recursion per nesting level
		final int outerBlockParentIndentation = blockParentIndentation;
		if (parentIndentation != null) {
			blockParentIndentation = parentIndentation;
		}
		try {
			if (peekCharMatch('!')) {
				return parseTaggedYamlNode(parentIndentation, allowSequenceAtParentIndentation);
			} else {
				return parseYamlNodeWithoutTag();
			}
		} finally {
			blockParentIndentation = outerBlockParentIndentation;
		}
	}

	/**
	 * Reads a node preceded by a tag like "!!str", see {@link #parseYamlNode(Integer, boolean)}.
	 */
	private YamlNode parseTaggedYamlNode(final Integer parentIndentation, final boolean allowSequenceAtParentIndentation) throws Exception {
		final long tagLine = getCurrentLine();
		final long tagColumn = getCurrentColumn();
		// An anchor directly before the tag on the same line, like "&anchor !tag key: value"
		String anchor = null;
		long propertiesStartColumn = tagColumn;
		if (pendingNodeAnchor != null && pendingNodeTagLine == tagLine && pendingNodeTagColumn == tagColumn) {
			anchor = pendingNodeAnchor;
			propertiesStartColumn = pendingNodeTagStartColumn;
			pendingNodeAnchor = null;
		}

		final String tag = readUpToNext(false, null, ' ', '\t', '\n');
		skipBlanks();
		final boolean tagLineEnded = isEOF() || peekCharMatch('\n') || peekCharMatch('#');
		if (tagLineEnded && flowLevel == 0 && propertiesStartColumn == tagColumn && indentations.size() > 1 && getNumberOfIndentationChars() == tagColumn - 1) {
			// The tag is alone on its line, so the indentation of its line is no level of the block
			// structure: the content on the following lines may be indented differently
			indentations.pop();
		}
		// An anchor may also follow the tag, like "!!str &anchor value"
		final String anchorAfterTag = readUpToNextContent(null);
		if (anchorAfterTag != null) {
			if (anchor != null) {
				throw new YamlParseException("Assignment of multiple anchors single YAML node: '" + anchor + "'", getCurrentLine(), getCurrentColumn());
			}
			anchor = anchorAfterTag;
		}
		final boolean contentOnTagLine = !tagLineEnded && getCurrentLine() == tagLine;
		if (contentOnTagLine && isIndicatorFollowedBySeparator('-')) {
			throw new YamlParseException("Block sequence must not start on the line of its tag", getCurrentLine(), getCurrentColumn());
		}

		final YamlNode typedNode;
		if (tagLineEnded && parentIndentation != null && isEmptyTaggedValue(parentIndentation, allowSequenceAtParentIndentation)) {
			// Tag without value, like "key: !!null" followed by the next key
			typedNode = applyTag(tag, new YamlScalar("", YamlScalarType.NULL_VALUE));
		} else if (!contentOnTagLine) {
			// Properties on a line of their own belong to the following node, also to a block mapping,
			// whose first key may have properties of its own
			typedNode = applyTag(tag, peekCharMatch('!') ? parseYamlNode() : parseYamlNodeWithoutTag());
		} else {
			// The block mapping parser takes the properties for its first key, if the content turns
			// out to be a block mapping starting at this position, like "!tag key: value"
			final PendingNodeProperties outerPendingProperties = savePendingNodeProperties();
			setPendingNodeProperties(tag, anchor, propertiesStartColumn);
			final YamlNode node;
			final String remainingTag;
			try {
				node = parseYamlNodeWithoutTag();
			} finally {
				remainingTag = pendingNodeTag;
				anchor = pendingNodeAnchor;
				restorePendingNodeProperties(outerPendingProperties);
			}
			typedNode = remainingTag == null ? node : applyTag(remainingTag, node);
		}

		if (anchor != null) {
			assignAnchor(typedNode, anchor);
		}
		return typedNode;
	}

	/**
	 * Checks whether the content following a tag at the end of a line belongs to a sibling or parent node.
	 */
	private boolean isEmptyTaggedValue(final int parentIndentation, final boolean allowSequenceAtParentIndentation) throws Exception {
		if (isEOF()) {
			return true;
		}
		final int contentIndentation = (int) getCurrentColumn() - 1;
		if (contentIndentation > parentIndentation) {
			return false;
		} else if (allowSequenceAtParentIndentation && contentIndentation == parentIndentation && isIndicatorFollowedBySeparator('-')) {
			return false;
		} else {
			return true;
		}
	}

	private YamlNode parseYamlNodeWithoutTag() throws Exception {
		// Kept small, because it is part of the recursion per nesting level
		if (isIndicatorFollowedBySeparator('-')) {
			return parseBlockSequence();
		} else if (peekCharMatch('&')) {
			return parseAnchoredYamlNode();
		} else if (peekCharMatch('*')) {
			return parseAliasOrAliasKeyMapping();
		} else if (peekCharMatch('{') || peekCharMatch('[')) {
			return parseFlowCollectionOrFlowKeyMapping();
		} else {
			return parseBlockMappingOrScalar(null);
		}
	}

	/**
	 * Reads a node preceded by an anchor like "&amp;anchor".
	 */
	private YamlNode parseAnchoredYamlNode() throws Exception {
		final long anchorLine = getCurrentLine();
		final long anchorColumn = getCurrentColumn();
		readChar();
		final String anchor = readUpToNext(false, null, '\n', ' ', '\t');
		if (anchor == null || anchor.isEmpty()) {
			throw new YamlParseException("Missing anchor name", anchorLine, anchorColumn);
		}
		skipToNextContent();

		final YamlNode yamlNode;
		String remainingAnchor = anchor;
		if (getCurrentLine() == anchorLine && isIndicatorFollowedBySeparator('-')) {
			throw new YamlParseException("Block sequence must not start on the line of its anchor", getCurrentLine(), getCurrentColumn());
		} else if (getCurrentLine() == anchorLine) {
			// Content on the line of the anchor: the block mapping parser takes the anchor for its
			// first key, if the content turns out to be a block mapping, like "&anchor key: value"
			final PendingNodeProperties outerPendingProperties = savePendingNodeProperties();
			setPendingNodeProperties(null, anchor, anchorColumn);
			try {
				yamlNode = parseYamlNode();
			} finally {
				remainingAnchor = pendingNodeAnchor;
				restorePendingNodeProperties(outerPendingProperties);
			}
		} else {
			// An anchor on a line of its own belongs to the following node, also to a block mapping
			yamlNode = parseYamlNode();
		}

		if (remainingAnchor != null) {
			assignAnchor(yamlNode, remainingAnchor);
		}
		return yamlNode;
	}

	/**
	 * Reads an alias, or a block mapping with an alias as first key like "*anchor : value".
	 */
	private YamlNode parseAliasOrAliasKeyMapping() throws Exception {
		final PendingLeadingState pendingLeadingState = takePendingLeadingState();

		final YamlNode alias = parseAlias();

		applyPendingLeadingState(pendingLeadingState, alias);

		if (isIndicatorFollowedBySeparator(':')) {
			// Alias as key of a block mapping like "*anchor : value"
			return parseBlockMappingOrScalar(alias);
		}
		return alias;
	}

	/**
	 * Reads a flow collection in block context, or a block mapping with a flow collection as first
	 * key like "[a, b]: value".
	 */
	private YamlNode parseFlowCollectionOrFlowKeyMapping() throws Exception {
		final int flowStartIndentations = getNumberOfIndentationChars();
		final long flowStartLine = getCurrentLine();
		final PendingLeadingState pendingLeadingState = takePendingLeadingState();

		final PendingNodeProperties flowProperties = takePendingNodePropertiesAtCurrentPosition();
		YamlNode flowCollection = peekCharMatch('{') ? parseFlowMapping() : parseFlowSequence();
		flowCollection = applyPendingNodeProperties(flowProperties, flowCollection);

		applyPendingLeadingState(pendingLeadingState, flowCollection);

		readUpToNextContent(flowCollection);

		if (peekCharMatch(':') && flowStartIndentations + 1 < getCurrentColumn()) {
			if (getCurrentLine() != flowStartLine) {
				throw new YamlParseException("Implicit mapping key must be on a single line", getCurrentLine(), getCurrentColumn());
			}
			return parseBlockMappingOrScalar(flowCollection);
		} else {
			return flowCollection;
		}
	}


	/**
	 * Applies a YAML core schema tag like "!!str" to a node: scalars are converted to the tagged type,
	 * collections are checked to match the tag.
	 *
	 * @param datatype
	 *            the tag name without "!!"
	 * @param yamlNode
	 *            the node read
	 * @return the node of the tagged type
	 * @throws Exception
	 *             if the tag is unknown or the node does not fit the tag
	 */
	private static YamlNode applyDatatype(final String datatype, final YamlNode yamlNode) throws Exception {
		if ("str".equals(datatype)) {
			final YamlScalar scalar = requireScalar(datatype, yamlNode);
			if (scalar.getType() != YamlScalarType.STRING && scalar.getType() != YamlScalarType.MULTILINE) {
				return copyNodeMetadata(scalar, new YamlScalar(scalar.getValueString(), YamlScalarType.STRING));
			} else {
				return yamlNode;
			}
		} else if ("float".equals(datatype) || "int".equals(datatype)) {
			final YamlScalar scalar = requireScalar(datatype, yamlNode);
			final YamlScalar numberScalar = scalar.getType() == YamlScalarType.NUMBER ? scalar : new YamlScalar(scalar.getValueString(), YamlScalarType.NUMBER);
			// The NUMBER constructor falls back to a string scalar for texts that are no number
			if (numberScalar.getType() != YamlScalarType.NUMBER) {
				throw new Exception("Invalid value '" + scalar.getValueString() + "' for YAML datatype: '" + datatype + "'");
			} else if ("int".equals(datatype) && !isIntegerNumber(numberScalar)) {
				throw new Exception("Invalid value '" + scalar.getValueString() + "' for YAML datatype: '" + datatype + "'");
			}
			return numberScalar == scalar ? scalar : copyNodeMetadata(scalar, numberScalar);
		} else if ("bool".equals(datatype)) {
			final YamlScalar scalar = requireScalar(datatype, yamlNode);
			if (scalar.getType() == YamlScalarType.BOOLEAN) {
				return scalar;
			}
			try {
				return copyNodeMetadata(scalar, new YamlScalar(scalar.getValueString(), YamlScalarType.BOOLEAN));
			} catch (final RuntimeException e) {
				throw new Exception("Invalid value '" + scalar.getValueString() + "' for YAML datatype: '" + datatype + "'", e);
			}
		} else if ("null".equals(datatype)) {
			final YamlScalar scalar = requireScalar(datatype, yamlNode);
			if (scalar.getType() == YamlScalarType.NULL_VALUE) {
				return scalar;
			} else if ("".equals(scalar.getValueString()) || "null".equalsIgnoreCase(scalar.getValueString()) || "~".equals(scalar.getValueString())) {
				return copyNodeMetadata(scalar, new YamlScalar(scalar.getValueString(), YamlScalarType.NULL_VALUE));
			} else {
				throw new Exception("Invalid value '" + scalar.getValueString() + "' for YAML datatype: '" + datatype + "'");
			}
		} else if ("binary".equals(datatype)) {
			// The value stays the base64 text, the tag marks it as binary data (see YamlScalar.getBinaryValue())
			final YamlScalar scalar = requireScalar(datatype, yamlNode);
			if (scalar.getType() != YamlScalarType.STRING && scalar.getType() != YamlScalarType.MULTILINE) {
				throw new Exception("Invalid value '" + scalar.getValueString() + "' for YAML datatype: '" + datatype + "'");
			}
			// The base64 text is checked when it is decoded, so documents with placeholders can still be read
			scalar.setTag(YamlScalar.BINARY_TAG);
			return scalar;
		} else if ("map".equals(datatype)) {
			if (yamlNode instanceof YamlMapping) {
				return yamlNode;
			} else {
				throw new Exception("Invalid data '" + yamlNode.getClass().getSimpleName() + "' for YAML datatype: '" + datatype + "'");
			}
		} else if ("seq".equals(datatype)) {
			if (yamlNode instanceof YamlSequence) {
				return yamlNode;
			} else {
				throw new Exception("Invalid data '" + yamlNode.getClass().getSimpleName() + "' for YAML datatype: '" + datatype + "'");
			}
		} else if ("set".equals(datatype)) {
			// YAML 1.1 type: a mapping with null values only
			if (yamlNode instanceof final YamlMapping mapping && mapping.values().stream().allMatch(YamlReader::isNullValue)) {
				return yamlNode;
			} else {
				throw new Exception("Invalid data for YAML datatype: '" + datatype + "', expected a mapping with null values");
			}
		} else if ("omap".equals(datatype) || "pairs".equals(datatype)) {
			// YAML 1.1 types: a sequence of mappings with a single entry each
			if (yamlNode instanceof final YamlSequence sequence && sequence.items().stream().allMatch(item -> item instanceof final YamlMapping pair && pair.size() == 1)) {
				return yamlNode;
			} else {
				throw new Exception("Invalid data for YAML datatype: '" + datatype + "', expected a sequence of single entry mappings");
			}
		} else {
			throw new Exception("Unknown YAML datatype: '" + datatype + "'");
		}
	}

	/**
	 * Checks whether a node is a null scalar.
	 */
	private static boolean isNullValue(final YamlNode node) {
		return node instanceof final YamlScalar scalar && scalar.getType() == YamlScalarType.NULL_VALUE;
	}

	/**
	 * Applies a tag to a node. Secondary tags of known datatypes like "!!str" or "!!int" change the
	 * type of the node, all other tags are stored in the node, see {@link YamlNode#getTag()}.
	 *
	 * @param tag
	 *            the tag as written, starting with "!"
	 * @param yamlNode
	 *            the node
	 * @return the node of the applied type
	 * @throws Exception
	 *             if the node does not fit the datatype of the tag
	 */
	private YamlNode applyTag(final String tag, final YamlNode yamlNode) throws Exception {
		checkTag(tag);
		if (tag.startsWith("!!") && !secondaryTagHandleRedefined) {
			return applyDatatype(tag.substring(2), yamlNode);
		} else {
			if (yamlNode instanceof final YamlScalar scalar && "!".equals(tag) && scalar.getType() != YamlScalarType.STRING && scalar.getType() != YamlScalarType.MULTILINE) {
				// The non-specific tag "!" forces a plain scalar to be a string
				return copyNodeMetadata(scalar, new YamlScalar(scalar.getValueString(), YamlScalarType.STRING)).withTag(tag);
			}
			yamlNode.setTag(tag);
			return yamlNode;
		}
	}

	/**
	 * Checks the syntax of a tag and that a named tag handle is declared.
	 *
	 * @param tag
	 *            the tag as written, starting with "!"
	 * @throws YamlParseException
	 *             if the tag is invalid
	 */
	private void checkTag(final String tag) throws YamlParseException {
		if (tag.startsWith("!<")) {
			// Verbatim tag
			return;
		}
		for (final char character : tag.toCharArray()) {
			if ("{}[],".indexOf(character) >= 0) {
				throw new YamlParseException("Invalid character '" + character + "' in tag '" + tag + "'", getCurrentLine(), getCurrentColumn());
			}
		}
		// A named tag handle like "!e!" must be declared by a "%TAG" directive of the document
		final int handleEnd = tag.indexOf('!', 1);
		if (handleEnd > 1 && !currentTagHandles.contains(tag.substring(0, handleEnd + 1))) {
			throw new YamlParseException("Tag handle '" + tag.substring(0, handleEnd + 1) + "' is not declared by a %TAG directive", getCurrentLine(), getCurrentColumn());
		}
	}

	/**
	 * Checks whether a number scalar holds an integer written without fraction or exponent. Very large
	 * integers are read as BigDecimal.
	 */
	private static boolean isIntegerNumber(final YamlScalar numberScalar) {
		final Object value = numberScalar.getValue();
		if (value instanceof Integer || value instanceof Long || value instanceof BigInteger) {
			return true;
		} else if (value instanceof BigDecimal) {
			final String valueString = numberScalar.getValueString();
			return valueString.indexOf('.') < 0 && valueString.indexOf('e') < 0 && valueString.indexOf('E') < 0;
		} else {
			return false;
		}
	}

	private static YamlScalar requireScalar(final String datatype, final YamlNode yamlNode) throws Exception {
		if (yamlNode instanceof final YamlScalar scalar) {
			return scalar;
		} else {
			throw new Exception("Invalid data '" + yamlNode.getClass().getSimpleName() + "' for YAML datatype: '" + datatype + "'");
		}
	}

	/**
	 * Copies comments and empty line counts of a node to its replacement.
	 *
	 * @param source
	 *            the node read
	 * @param target
	 *            the replacing node
	 * @return the replacing node
	 */
	private static YamlScalar copyNodeMetadata(final YamlNode source, final YamlScalar target) {
		target.setInlineComment(source.getInlineComment());
		if (source.getLeadingComments() != null) {
			for (final String leadingComment : source.getLeadingComments()) {
				target.addLeadingComment(leadingComment);
			}
		}
		target.setLeadingEmptyLinesCount(source.getLeadingEmptyLinesCount());
		target.setPostCommentEmptyLinesCount(source.getPostCommentEmptyLinesCount());
		return target;
	}

	/**
	 * True if the last quoted scalar read by {@link #readQuotedScalar(char)} spanned several lines.
	 */
	private boolean lastQuotedScalarWasMultiline = false;

	/**
	 * The last quoted scalar node, which spanned several lines and therefore must not be used as an
	 * implicit mapping key. Null if there is none.
	 */
	private YamlNode lastMultilineQuotedScalarNode = null;

	/**
	 * Reads a quoted scalar node, see {@link #readQuotedScalar(char)}.
	 *
	 * @param quoteChar
	 *            '"' or '\''
	 * @return the scalar node
	 */
	private YamlScalar readQuotedScalarNode(final char quoteChar) throws Exception {
		final YamlScalar scalar = new YamlScalar(readQuotedScalar(quoteChar), YamlScalarType.STRING).withQuoteType(quoteChar == '"' ? YamlStringQuoteType.DOUBLE : YamlStringQuoteType.SINGLE);
		lastMultilineQuotedScalarNode = lastQuotedScalarWasMultiline ? scalar : null;
		return scalar;
	}

	/**
	 * Checks that a node about to be used as implicit block mapping key is not a quoted scalar spanning several lines.
	 */
	private void checkImplicitKey(final YamlNode keyNode) throws YamlParseException {
		if (keyNode != null && keyNode == lastMultilineQuotedScalarNode) {
			throw new YamlParseException("Implicit mapping key must not span several lines", getCurrentLine(), getCurrentColumn());
		}
	}

	/**
	 * Reads a single or double quoted scalar including its quotes, following the YAML rules for
	 * escape sequences (double quoted only) and line folding: a single linebreak becomes a blank, n
	 * linebreaks in a row become n-1 linebreaks, and blanks around linebreaks are removed. An escaped
	 * linebreak in a double quoted scalar joins the lines without a blank and keeps the blanks before
	 * it.
	 *
	 * @param quoteChar
	 *            '"' or '\''
	 * @return the text of the scalar
	 * @throws YamlParseException
	 *             if the scalar is not closed, contains an invalid escape sequence or a document
	 *             marker, or a continuation line is not indented deep enough
	 */
	private String readQuotedScalar(final char quoteChar) throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();
		if (peekCharNotMatch(quoteChar)) {
			throw new YamlParseException("Expected quote character not found", startLine, startColumn);
		}
		readChar();
		lastQuotedScalarWasMultiline = false;

		final StringBuilder text = new StringBuilder();
		// Text up to this length must not be trimmed at a linebreak, because it was written escaped
		int protectedLength = 0;
		while (true) {
			if (isEOF()) {
				throw new YamlParseException("Missing closing quote character for quoted string", startLine, startColumn);
			}
			final char nextChar = peekChar();
			if (nextChar == quoteChar) {
				readChar();
				if (quoteChar == '\'' && peekCharMatch('\'')) {
					// Doubled single quote represents a single quote
					readChar();
					text.append('\'');
					protectedLength = text.length();
				} else {
					return text.toString();
				}
			} else if (nextChar == '\\' && quoteChar == '"') {
				readChar();
				if (isEOF()) {
					throw new YamlParseException("Missing closing quote character for quoted string", startLine, startColumn);
				} else if (peekCharMatch('\n')) {
					// Escaped linebreak: the lines are joined without blank, blanks before the escape are kept
					readChar();
					protectedLength = text.length();
					final int emptyLines = skipQuotedScalarLinePrefixes(startLine, startColumn);
					text.append("\n".repeat(emptyLines));
					protectedLength = text.length();
				} else {
					appendEscapedCharacter(text);
					protectedLength = text.length();
				}
			} else if (nextChar == '\n') {
				readChar();
				// Blanks before a linebreak are no content
				int textEnd = text.length();
				while (textEnd > protectedLength && (text.charAt(textEnd - 1) == ' ' || text.charAt(textEnd - 1) == '\t')) {
					textEnd--;
				}
				text.setLength(textEnd);
				final int emptyLines = skipQuotedScalarLinePrefixes(startLine, startColumn);
				if (emptyLines == 0) {
					text.append(' ');
				} else {
					text.append("\n".repeat(emptyLines));
				}
				protectedLength = text.length();
			} else {
				text.append(readChar());
			}
		}
	}

	/**
	 * Skips the leading blanks of the continuation lines of a quoted scalar after a linebreak and the
	 * empty lines in between.
	 *
	 * @return the number of empty lines skipped
	 * @throws YamlParseException
	 *             if a line starts with a document marker or is not indented deep enough
	 */
	private int skipQuotedScalarLinePrefixes(final long startLine, final long startColumn) throws Exception {
		lastQuotedScalarWasMultiline = true;
		int emptyLines = 0;
		while (true) {
			if (isEOF()) {
				throw new YamlParseException("Missing closing quote character for quoted string", startLine, startColumn);
			} else if (isDocumentMarkerAtLineStart()) {
				throw new YamlParseException("Document marker within quoted string", getCurrentLine(), getCurrentColumn());
			}
			int leadingSpaces = 0;
			while (peekCharMatch(' ')) {
				readChar();
				leadingSpaces++;
			}
			skipBlanks();
			if (peekCharMatch('\n')) {
				readChar();
				emptyLines++;
			} else {
				if (flowLevel == 0 && leadingSpaces <= blockParentIndentation && isNotEOF()) {
					throw new YamlParseException("Continuation line of quoted string is not indented enough", getCurrentLine(), getCurrentColumn());
				}
				return emptyLines;
			}
		}
	}

	/**
	 * The character read last, null at the beginning of the data.
	 */
	private Character lastReadCharacter = null;

	@Override
	protected Character readChar() throws IOException {
		final Character character = super.readChar();
		lastReadCharacter = character;
		return character;
	}

	/**
	 * Checks that a comment at the read position is separated from preceding content by a blank.
	 *
	 * @throws YamlParseException
	 *             if a '#' directly follows content
	 */
	private void checkCommentSeparation() throws YamlParseException {
		if (peekCharMatch('#') && getCurrentColumn() > 1 && lastReadCharacter != null && lastReadCharacter != ' ' && lastReadCharacter != '\t' && lastReadCharacter != '\n') {
			throw new YamlParseException("Comment must be separated from content by a blank", getCurrentLine(), getCurrentColumn());
		}
	}

	/**
	 * Checks the rest of the line after a quoted scalar or a flow collection in block context: only
	 * blanks, a comment or the ':' of a mapping key may follow.
	 *
	 * @throws YamlParseException
	 *             if other content follows or a comment is not separated by a blank
	 */
	private void checkEndOfBlockValueLine() throws Exception {
		checkCommentSeparation();
		skipBlanks();
		if (isNotEOF() && peekCharNotMatch('\n') && peekCharNotMatch('#') && peekCharNotMatch(':')) {
			throw new YamlParseException("Unexpected content after value", getCurrentLine(), getCurrentColumn());
		}
	}

	/**
	 * Leaves a flow collection after its closing bracket was read. The rest of the line after the
	 * outermost flow collection is checked, see {@link #checkEndOfBlockValueLine()}.
	 */
	private void closeFlowCollection() throws Exception {
		flowLevel--;
		final List<Integer> indentationsBeforeFlowCollection = flowIndentationSnapshots.pop();
		indentations.clear();
		indentations.addAll(indentationsBeforeFlowCollection);
		if (flowLevel == 0) {
			checkEndOfBlockValueLine();
		}
	}

	/**
	 * Checks whether the next character is the given indicator followed by a blank, a linebreak or
	 * the end of the data, like "- " of a sequence item.
	 */
	private boolean isIndicatorFollowedBySeparator(final char indicator) {
		return peekCharMatch(indicator) && (peekNextChar(1) == null || peekNextCharMatchAny(1, " \t\n"));
	}

	/**
	 * Checks whether the read position is at the beginning of a line with a document marker "---" or
	 * "..." followed by a blank, a linebreak or the end of the data.
	 */
	private boolean isDocumentMarkerAtLineStart() {
		if (getCurrentColumn() != 1) {
			return false;
		}
		final boolean dashes = peekCharMatch('-') && peekNextCharMatch(1, '-') && peekNextCharMatch(2, '-');
		final boolean dots = peekCharMatch('.') && peekNextCharMatch(1, '.') && peekNextCharMatch(2, '.');
		return (dashes || dots) && (peekNextChar(3) == null || peekNextCharMatchAny(3, " \t\n"));
	}

	/**
	 * Reads the character(s) of an escape sequence in a double quoted scalar after the backslash and
	 * appends the character it represents.
	 */
	private void appendEscapedCharacter(final StringBuilder text) throws Exception {
		final long line = getCurrentLine();
		final long column = getCurrentColumn();
		final char escapedChar = readChar();
		switch (escapedChar) {
			case '0':
				text.append('\0');
				break;
			case 'a':
				text.append('\u0007');
				break;
			case 'b':
				text.append('\b');
				break;
			case 't':
			case '\t':
				text.append('\t');
				break;
			case 'n':
				text.append('\n');
				break;
			case 'v':
				text.append('\u000B');
				break;
			case 'f':
				text.append('\f');
				break;
			case 'r':
				text.append('\r');
				break;
			case 'e':
				text.append('\u001B');
				break;
			case ' ':
			case '"':
			case '/':
			case '\\':
				text.append(escapedChar);
				break;
			case 'N':
				text.append('\u0085');
				break;
			case '_':
				text.append(' ');
				break;
			case 'L':
				text.append(' ');
				break;
			case 'P':
				text.append(' ');
				break;
			case 'x':
				text.appendCodePoint(readEscapedHexDigits(2, line, column));
				break;
			case 'u':
				text.append((char) readEscapedHexDigits(4, line, column));
				break;
			case 'U':
				final int codePoint = readEscapedHexDigits(8, line, column);
				if (!Character.isValidCodePoint(codePoint) || (codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE)) {
					throw new YamlParseException("Invalid unicode escape sequence", line, column);
				}
				text.appendCodePoint(codePoint);
				break;
			default:
				throw new YamlParseException("Invalid escape sequence '\\" + escapedChar + "'", line, column);
		}
	}

	private int readEscapedHexDigits(final int digitCount, final long line, final long column) throws Exception {
		int value = 0;
		for (int i = 0; i < digitCount; i++) {
			final int digitValue = isEOF() ? -1 : Character.digit(peekChar(), 16);
			if (digitValue < 0 || peekChar() > 'f') {
				throw new YamlParseException("Invalid hexadecimal escape sequence", line, column);
			}
			readChar();
			value = (value << 4) | digitValue;
		}
		return value;
	}

	/**
	 * Reads an alias like "*anchor" and the blanks after it.
	 */
	private YamlAlias readAliasNode() throws Exception {
		if (peekCharNotMatch('*')) {
			throw new YamlParseException("Expected alias start not found", getCurrentLine(), getCurrentColumn());
		}

		final long aliasLine = getCurrentLine();
		final long aliasColumn = getCurrentColumn();
		readChar();

		final String anchorName = readUpToNext(false, null, '\n', ' ', '\t');
		if (anchorName.isEmpty()) {
			throw new YamlParseException("Missing alias name", getCurrentLine(), getCurrentColumn());
		}
		final YamlAlias alias = new YamlAlias(anchorName);
		aliasPositions.put(alias, new long[] { aliasLine, aliasColumn });

		skipBlanks();
		return alias;
	}

	private YamlNode parseAlias() throws Exception {
		final YamlAlias alias = readAliasNode();
		if (isIndicatorFollowedBySeparator(':')) {
			// Alias as implicit mapping key, the caller reads the mapping
			return alias;
		}

		if (peekCharMatch('#')) {
			alias.setInlineComment(readInlineComment());
		}

		skipEmptyLinesAndReadNextIndentationAndLeadingComments();

		return alias;
	}

	private YamlNode parseBlockSequence() throws Exception {
		enterNesting();
		try {
			return parseBlockSequenceInternal();
		} finally {
			nestingDepth--;
		}
	}

	private YamlNode parseBlockSequenceInternal() throws Exception {
		if (!isIndicatorFollowedBySeparator('-')) {
			throw new YamlParseException("Expected sequence start not found", getCurrentLine(), getCurrentColumn());
		}

		if (getCurrentLine() == blockCollectionForbiddenLine) {
			throw new YamlParseException("Block sequence must not start on this line", getCurrentLine(), getCurrentColumn());
		}
		checkNoTabBeforeBlockCollection(getCurrentLine(), getCurrentColumn());
		final int sequenceIndentation = (int) getCurrentColumn() - 1;

		final YamlSequence sequence = new YamlSequence();
		final int sequencePathSize = currentPath.size();
		currentPath.add(new JsonPathArrayElement());

		int itemIndex = 0;
		while (isNotEOF() && peekCharMatch('-') && (int) getCurrentColumn() - 1 == sequenceIndentation && !isDocumentMarkerAtLineStart()) {
			setPathArrayIndex(sequencePathSize, itemIndex++);
			final YamlNode nextItemNode = readNextSequenceItem();
			if (nextItemNode != null) {
				sequence.add(nextItemNode);
			} else {
				break;
			}
		}

		truncatePath(sequencePathSize);

		return sequence;
	}

	private YamlNode readNextSequenceItem() throws Exception {
		final PendingLeadingState pendingLeadingState = takePendingLeadingState();

		if (isIndicatorFollowedBySeparator('-')) {
			final int dashIndentation = (int) getCurrentColumn() - 1;
			readChar();

			// The item content defines the indentation of its lines, e.g. "-   key: value" has 4 characters
			final int blanksAfterDash = skipBlanks();
			final boolean noContentInItemLine = isEOF() || peekCharMatch('\n') || peekCharMatch('#');
			if (noContentInItemLine) {
				// The item content may follow on the next lines, but only if they are indented deeper than the dash
				final Boolean indentationChange = skipEmptyLinesAndReadNextIndentationAndLeadingComments();
				if (isEOF() || !Boolean.TRUE.equals(indentationChange) || (int) getCurrentColumn() - 1 <= dashIndentation) {
					// Empty item like "-" followed by the next item or a less indented line: the value is null
					final YamlScalar nullItemNode = new YamlScalar(null);
					applyPendingLeadingState(pendingLeadingState, nullItemNode);
					return nullItemNode;
				}
			} else {
				indentationsAdd(1 + blanksAfterDash);
			}

			// An anchor of the item is read by parseYamlNode(), which decides whether it belongs to
			// the item or to the first key of a compact mapping like "- &anchor key: value"
			skipToNextContent();

			final YamlNode nextItemNode = parseYamlNode(dashIndentation, false);

			applyPendingLeadingState(pendingLeadingState, nextItemNode);

			return nextItemNode;
		} else {
			return null;
		}
	}

	private YamlNode parseBlockMappingOrScalar(final YamlNode firstKeyNode) throws Exception {
		// Not checked yet, because the node may turn out to be a simple scalar, which is no nesting
		// level of its own. The check follows as soon as the node is known to be a mapping.
		nestingDepth++;
		// A tag directly before this position belongs to the first key, if a block mapping starts here
		String firstKeyTag = null;
		String firstKeyAnchor = null;
		// Column of the first key including its tag and anchor, which decides the indentation
		long firstKeyColumn = getCurrentColumn();
		if (firstKeyNode == null && (pendingNodeTag != null || pendingNodeAnchor != null) && pendingNodeTagLine == getCurrentLine() && pendingNodeTagColumn == getCurrentColumn()) {
			firstKeyTag = pendingNodeTag;
			firstKeyAnchor = pendingNodeAnchor;
			firstKeyColumn = pendingNodeTagStartColumn;
			pendingNodeTag = null;
			pendingNodeAnchor = null;
		}
		final boolean[] firstKeyPropertiesUsed = { false };
		try {
			return parseBlockMappingOrScalarInternal(firstKeyNode, firstKeyTag, firstKeyAnchor, firstKeyColumn, firstKeyPropertiesUsed);
		} finally {
			nestingDepth--;
			if ((firstKeyTag != null || firstKeyAnchor != null) && !firstKeyPropertiesUsed[0]) {
				// No block mapping: the properties belong to the node
				pendingNodeTag = firstKeyTag;
				pendingNodeAnchor = firstKeyAnchor;
			}
		}
	}

	/**
	 * True if the key read last by {@link #readBlockMappingKey(int, long)} was an explicit key "? key".
	 */
	private boolean lastBlockMappingKeyWasExplicit = false;

	/**
	 * Reads the first key of a possible block mapping, which may also turn out to be a simple
	 * scalar, and assigns the comments and empty lines pending before it.
	 * <p>
	 * Kept separate from {@link #parseBlockMappingOrScalarInternal}, so the stack frame of the
	 * recursion per nesting level stays small.
	 * </p>
	 */
	private YamlNode readFirstBlockMappingKeyOrScalar(final int mappingIndentation, final long firstKeyColumn) throws Exception {
		// Comments collected while parsing the node itself (e.g. by a multiline block
		// scalar's own lookahead past its content, which may run into a trailing comment
		// belonging to whatever follows) must not be mistaken for comments that preceded
		// this node; only comments already pending before parsing started belong to it.
		final int leadingCommentsCountBeforeNode = pendingLeadingComments.size();
		final int leadingEmptyLinesCountBeforeNode = pendingLeadingEmptyLines;
		final int postCommentEmptyLinesCountBeforeNode = pendingTrailingEmptyLinesAfterComments;

		final YamlNode keyOrScalarNode = readBlockMappingKey(mappingIndentation, firstKeyColumn);

		// A nested node (like an explicit key) may already have taken the pending comments
		final int leadingCommentsOfNode = Math.min(leadingCommentsCountBeforeNode, pendingLeadingComments.size());
		if (leadingCommentsOfNode > 0) {
			for (final String commentLine1 : pendingLeadingComments.subList(0, leadingCommentsOfNode)) {
				keyOrScalarNode.addLeadingComment(commentLine1);
			}
			pendingLeadingComments = new ArrayList<>(
					pendingLeadingComments.subList(leadingCommentsOfNode, pendingLeadingComments.size()));
		}

		if (leadingEmptyLinesCountBeforeNode > 0) {
			keyOrScalarNode.setLeadingEmptyLinesCount(leadingEmptyLinesCountBeforeNode);
			pendingLeadingEmptyLines -= leadingEmptyLinesCountBeforeNode;
		}

		if (postCommentEmptyLinesCountBeforeNode > 0) {
			keyOrScalarNode.setPostCommentEmptyLinesCount(postCommentEmptyLinesCountBeforeNode);
			pendingTrailingEmptyLinesAfterComments -= postCommentEmptyLinesCountBeforeNode;
		}

		skipBlanks();
		return keyOrScalarNode;
	}

	/**
	 * Reads a further key of a block mapping including its tag and anchor in any order, like
	 * "&amp;anchor !tag key: value", and assigns the comments and empty lines pending before it.
	 */
	private YamlNode readFurtherBlockMappingKey(final int mappingIndentation) throws Exception {
		String keyTag = null;
		String keyAnchor = null;
		// Column of the key including its properties, which decides the indentation
		final long keyColumn = getCurrentColumn();
		while ((peekCharMatch('!') && keyTag == null) || (peekCharMatch('&') && keyAnchor == null)) {
			if (peekCharMatch('!')) {
				keyTag = readUpToNext(false, null, ' ', '\t', '\n');
			} else {
				readChar();
				keyAnchor = readUpToNext(false, null, ' ', '\t', '\n');
			}
			skipBlanks();
		}

		YamlNode keyNode = readBlockMappingKey(mappingIndentation, keyColumn);

		if (keyTag != null) {
			keyNode = applyTag(keyTag, keyNode);
		}
		if (keyAnchor != null) {
			assignAnchor(keyNode, keyAnchor);
		}

		applyAndClearPendingLeadingState(keyNode);

		skipBlanks();
		return keyNode;
	}

	/**
	 * Reads a key of a block mapping, which may also turn out to be a simple scalar: a quoted or
	 * plain scalar, a flow collection, an alias or an explicit key "? key". Sets
	 * {@link #lastBlockMappingKeyWasExplicit}.
	 *
	 * @param mappingIndentation
	 *            the indentation of the mapping
	 * @param keyColumn
	 *            the column of the key including its properties
	 */
	private YamlNode readBlockMappingKey(final int mappingIndentation, final long keyColumn) throws Exception {
		lastBlockMappingKeyWasExplicit = false;
		final YamlNode keyNode;
		if (peekCharMatch('\"') || peekCharMatch('\'')) {
			if (keyColumn - 1 - getNumberOfIndentationChars() > 0) {
				indentationsAdd((int) keyColumn - 1 - getNumberOfIndentationChars());
			}
			keyNode = readQuotedScalarNode(peekChar());
			checkEndOfBlockValueLine();
			skipBlanks();
			if (peekCharMatch('#')) {
				keyNode.setInlineComment(readInlineComment());
			}
		} else if (peekCharMatch('[')) {
			keyNode = parseFlowSequence();
		} else if (peekCharMatch('{')) {
			keyNode = parseFlowMapping();
		} else if (peekCharMatch('*')) {
			keyNode = readAliasNode();
		} else if (isIndicatorFollowedBySeparator('?')) {
			readChar();
			lastBlockMappingKeyWasExplicit = true;
			skipBlanks();
			// Complex key: this node is a mapping
			checkNestingDepth();
			if (isEOF() || peekCharMatch('\n') || peekCharMatch('#')) {
				// The explicit key starts on the next line, it may be a block sequence at the indentation of the mapping
				skipToNextContent();
				if (isNotEOF() && (getCurrentColumn() - 1 > mappingIndentation || (getCurrentColumn() - 1 == mappingIndentation && isIndicatorFollowedBySeparator('-')))) {
					keyNode = parseYamlNode(mappingIndentation, true);
				} else {
					// Empty explicit key
					keyNode = new YamlScalar(null);
				}
			} else {
				indentationsAdd(2);
				// The explicit key belongs to this mapping
				final int outerBlockParentIndentation = blockParentIndentation;
				blockParentIndentation = mappingIndentation;
				try {
					keyNode = parseYamlNode();
				} finally {
					blockParentIndentation = outerBlockParentIndentation;
				}
			}
			// A nested parse may have read further keys, the flag belongs to this key
			lastBlockMappingKeyWasExplicit = true;
		} else {
			if (keyColumn - 1 - getNumberOfIndentationChars() > 0) {
				indentationsAdd((int) keyColumn - 1 - getNumberOfIndentationChars());
			}
			keyNode = readUnquotedScalarString(mappingIndentation);
		}
		return keyNode;
	}

	/**
	 * Reads the ':' after a block mapping key and the blanks, comment and anchor up to the value.
	 * The rest of the line of an implicit key may not contain a block collection.
	 *
	 * @return the anchor of the value, or null
	 */
	private String readBlockMappingValueIndicator(final YamlNode keyNode, final boolean explicitKey) throws Exception {
		readChar();
		final long colonLine = getCurrentLine();
		final String valueAnchor = readUpToNextContent(keyNode);
		if (!explicitKey && getCurrentLine() == colonLine) {
			// No block collection may start in the rest of the line of an implicit key, like "key: - item"
			blockCollectionForbiddenLine = colonLine;
		}
		return valueAnchor;
	}

	/**
	 * Reads the value of a block mapping entry after its ':', or returns an empty value if the value
	 * is missing.
	 */
	private YamlNode readBlockMappingValue(final int mappingIndentation, final boolean valueIndicatorRead, final String valueAnchor) throws Exception {
		final YamlNode valueNode;
		if (valueIndicatorRead && (getCurrentColumn() - 1 > mappingIndentation || isIndicatorFollowedBySeparator('-'))) {
			valueNode = parseYamlNode(mappingIndentation, true);
		} else {
			// Empty mapping property value
			valueNode = new YamlScalar("", YamlScalarType.NULL_VALUE);
		}
		if (valueAnchor != null) {
			assignAnchor(valueNode, valueAnchor);
		}
		return valueNode;
	}

	private YamlNode parseBlockMappingOrScalarInternal(final YamlNode firstKeyNode, final String firstKeyTag, final String firstKeyAnchor, final long firstKeyColumn, final boolean[] firstKeyPropertiesUsed) throws Exception {
		final int mappingIndentation = getNumberOfIndentationChars();
		final long firstKeyLine = getCurrentLine();

		YamlNode keyOrScalarNode;
		final boolean explicitKey;
		if (firstKeyNode != null) {
			keyOrScalarNode = firstKeyNode;
			explicitKey = false;
		} else {
			keyOrScalarNode = readFirstBlockMappingKeyOrScalar(mappingIndentation, firstKeyColumn);
			explicitKey = lastBlockMappingKeyWasExplicit;
		}

		// The ':' of an implicit key is on the line of the key
		final boolean colonFollows = peekCharMatch(':')
				&& mappingIndentation < getCurrentColumn()
				&& (peekNextChar(1) == null || peekNextCharMatchAny(1, " \t\n"))
				&& (explicitKey || firstKeyNode != null || getCurrentLine() == firstKeyLine);
		// An explicit key "? key" is a mapping entry, also without value
		if (colonFollows || (explicitKey && getNumberOfIndentationChars() == mappingIndentation)) {
			if (!explicitKey) {
				checkImplicitKey(keyOrScalarNode);
				if (firstKeyLine == blockCollectionForbiddenLine) {
					throw new YamlParseException("Block mapping must not start on this line", getCurrentLine(), getCurrentColumn());
				}
				checkNoTabBeforeBlockCollection(firstKeyLine, firstKeyColumn);
			}
			final String valueAnchor = colonFollows ? readBlockMappingValueIndicator(keyOrScalarNode, explicitKey) : null;

			checkNestingDepth();
			if (firstKeyTag != null) {
				keyOrScalarNode = applyTag(firstKeyTag, keyOrScalarNode);
			}
			if (firstKeyAnchor != null) {
				assignAnchor(keyOrScalarNode, firstKeyAnchor);
			}
			firstKeyPropertiesUsed[0] = true;
			final YamlMapping mapping = new YamlMapping();
			// Each entry restores the path after its value, whatever the value is (also an empty value)
			final int mappingPathSize = currentPath.size();
			addPathKey(keyOrScalarNode);
			mapping.add(keyOrScalarNode, readBlockMappingValue(mappingIndentation, colonFollows, valueAnchor));
			truncatePath(mappingPathSize);

			while (isNotEOF() && getNumberOfIndentationChars() == mappingIndentation) {
				if (isDocumentMarkerAtLineStart()) {
					// End of the document, the marker is read by parseDocument()
					return mapping;
				}

				keyOrScalarNode = readFurtherBlockMappingKey(mappingIndentation);
				final boolean keyIsExplicit = lastBlockMappingKeyWasExplicit;

				if (peekCharMatch(':')
						&& mappingIndentation < getCurrentColumn()
						&& (peekNextChar(1) == null || peekNextCharMatchAny(1, " \t\n"))) {
					if (!keyIsExplicit) {
						checkImplicitKey(keyOrScalarNode);
					}
					final String entryValueAnchor = readBlockMappingValueIndicator(keyOrScalarNode, keyIsExplicit);
					addPathKey(keyOrScalarNode);
					mapping.add(keyOrScalarNode, readBlockMappingValue(mappingIndentation, true, entryValueAnchor));
					truncatePath(mappingPathSize);
				} else if (keyIsExplicit && getNumberOfIndentationChars() == mappingIndentation) {
					// Explicit key without value
					mapping.add(keyOrScalarNode, new YamlScalar("", YamlScalarType.NULL_VALUE));
				} else {
					throw new YamlParseException("Invalid YAML data when expecting mapping key found", getCurrentLine(), getCurrentColumn());
				}
			}

			final String mappingAnchor = readUpToNextContent(null);


			if (mappingAnchor != null) {
				assignAnchor(mapping, mappingAnchor);
			}

			return mapping;
		} else {
			final String pendingAnchor = readUpToNextContent(null);
			if (pendingAnchor != null) {
				assignAnchor(keyOrScalarNode, pendingAnchor);
			}

			return keyOrScalarNode;
		}
	}

	private YamlNode parseFlowMapping() throws Exception {
		enterNesting();
		final int outerFlowLevel = flowLevel;
		flowLevel++;
		// Lines within flow collections do not change the block indentation, see closeFlowCollection()
		flowIndentationSnapshots.push(new ArrayList<>(indentations));
		final int snapshotCount = flowIndentationSnapshots.size();
		final int pathSize = currentPath.size();
		try {
			final YamlNode collection = parseFlowMappingInternal();
			truncatePath(pathSize);
			return collection;
		} finally {
			// The closing bracket may already have decreased the flow level and restored the indentations
			flowLevel = outerFlowLevel;
			while (flowIndentationSnapshots.size() >= snapshotCount) {
				flowIndentationSnapshots.pop();
			}
			nestingDepth--;
		}
	}

	private YamlNode parseFlowMappingInternal() throws Exception {
		if (peekCharNotMatch('{')) {
			throw new YamlParseException("Expected flow YAML mapping start character not found", getCurrentLine(), getCurrentColumn());
		}

		readChar();

		final YamlMapping mapping = new YamlMapping(true);

		// An anchor directly after '{' belongs to the first key, an anchor of the mapping itself precedes the '{'
		String pendingAnchor = readUpToNextContent(mapping);

		applyAndClearPendingLeadingState(mapping);

		if (peekCharMatch('}')) {
			readChar();
			closeFlowCollection();
			return mapping;
		}

		while (isNotEOF()) {
			final String keyAnchor = readUpToNextContent(null);
			if (keyAnchor != null) {
				if (pendingAnchor != null) {
					throw new YamlParseException("Assignment of multiple anchors single YAML node: '" + pendingAnchor + "'", getCurrentLine(), getCurrentColumn());
				}
				pendingAnchor = keyAnchor;
			}

			if (peekCharMatch(',')) {
				throw new YamlParseException("Empty flow mapping entry", getCurrentLine(), getCurrentColumn());
			} else if (peekCharMatch('}') && pendingAnchor == null) {
				// Closing bracket after a trailing ','
				readChar();
				closeFlowCollection();
				readUpToNextContent(mapping);
				return mapping;
			}
			final YamlNode keyNode = parseFlowNode();
			if (pendingAnchor != null) {
				assignAnchor(keyNode, pendingAnchor);
				pendingAnchor = null;
			}

			applyAndClearPendingLeadingState(keyNode);

			// A comment on the line of the key is its inline comment, also after a quoted key
			pendingAnchor = readUpToNextContent(keyNode);
			if (pendingAnchor != null) {
				assignAnchor(keyNode, pendingAnchor);
				pendingAnchor = null;
			}

			skipBlanks();

			if (peekCharMatch('}')) {
				readChar();
				closeFlowCollection();

				mapping.add(keyNode, new YamlScalar("null", YamlScalarType.NULL_VALUE));

				pendingAnchor = readUpToNextContent(mapping);
				if (pendingAnchor != null) {
					assignAnchor(keyNode, pendingAnchor);
					pendingAnchor = null;
				}

				return mapping;
			} else if (peekCharMatch(',')) {
				readChar();

				// An anchor after ',' belongs to the next key
				pendingAnchor = readUpToNextContent(keyNode);

				mapping.add(keyNode, new YamlScalar(null));
			} else if (peekCharMatch(':') && (peekNextChar(1) == null || peekNextCharMatchAny(1, ", \t\n]}") || isJsonLikeNode(keyNode))) {
				readChar();

				indentationsAdd(1);

				// An anchor after ':' belongs to the value, like in "{key: &anchor value}"
				final String valueAnchor = readUpToNextContent(keyNode);

				if (peekCharMatch(',')) {
					readChar();

					final YamlScalar valueNode = new YamlScalar(null);
					if (valueAnchor != null) {
						assignAnchor(valueNode, valueAnchor);
					}

					// An anchor after ',' belongs to the next key
					pendingAnchor = readUpToNextContent(keyNode);


					mapping.add(keyNode, valueNode);
					continue;
				} else {
					pendingAnchor = readUpToNextContent(keyNode);
					if (valueAnchor != null) {
						if (pendingAnchor != null) {
							throw new YamlParseException("Assignment of multiple anchors single YAML node: '" + valueAnchor + "'", getCurrentLine(), getCurrentColumn());
						}
						pendingAnchor = valueAnchor;
					}

					final int entryPathSize = currentPath.size();
					addPathKey(keyNode);
					final YamlNode valueNode = parseFlowNode();
					truncatePath(entryPathSize);

					if (pendingAnchor != null) {
						assignAnchor(valueNode, pendingAnchor);
						pendingAnchor = null;
					}

					applyAndClearPendingLeadingState(valueNode);

					pendingAnchor = readUpToNextContent(valueNode);

					if (pendingAnchor != null) {
						assignAnchor(valueNode, pendingAnchor);
						pendingAnchor = null;
					}

					boolean mapIsClosed = false;
					if (peekCharMatch('}') || peekCharMatch(',')) {
						mapIsClosed = peekCharMatch('}');
						readChar();
						if (mapIsClosed) {
							closeFlowCollection();
						}

						// An anchor after ',' belongs to the next key
						pendingAnchor = readUpToNextContent(valueNode);
						if (pendingAnchor != null && mapIsClosed) {
							assignAnchor(valueNode, pendingAnchor);
							pendingAnchor = null;
						}

						mapping.add(keyNode, valueNode);

						if (mapIsClosed) {
							pendingAnchor = readUpToNextContent(mapping);
							if (pendingAnchor != null) {
								assignAnchor(valueNode, pendingAnchor);
								pendingAnchor = null;
							}

							return mapping;
						}
					} else {
						throw new YamlParseException("Invalid flow YAML mapping syntax found", getCurrentLine(), getCurrentColumn());
					}
				}
			} else {
				throw new YamlParseException("Invalid flow YAML mapping syntax found", getCurrentLine(), getCurrentColumn());
			}
		}

		throw new YamlParseException("Missing closing bracket '}' of flow mapping", getCurrentLine(), getCurrentColumn());
	}

	private YamlNode parseFlowSequence() throws Exception {
		enterNesting();
		final int outerFlowLevel = flowLevel;
		flowLevel++;
		// Lines within flow collections do not change the block indentation, see closeFlowCollection()
		flowIndentationSnapshots.push(new ArrayList<>(indentations));
		final int snapshotCount = flowIndentationSnapshots.size();
		final int pathSize = currentPath.size();
		try {
			final YamlNode collection = parseFlowSequenceInternal();
			truncatePath(pathSize);
			return collection;
		} finally {
			// The closing bracket may already have decreased the flow level and restored the indentations
			flowLevel = outerFlowLevel;
			while (flowIndentationSnapshots.size() >= snapshotCount) {
				flowIndentationSnapshots.pop();
			}
			nestingDepth--;
		}
	}

	private YamlNode parseFlowSequenceInternal() throws Exception {
		if (peekCharNotMatch('[')) {
			throw new YamlParseException("Expected flow YAML sequence start character not found", getCurrentLine(), getCurrentColumn());
		}

		readChar();

		final YamlSequence sequence = new YamlSequence(true);
		final int sequencePathSize = currentPath.size();
		currentPath.add(new JsonPathArrayElement());
		int itemIndex = 0;

		// An anchor directly after '[' belongs to the first item, an anchor of the sequence itself precedes the '['
		String pendingAnchor = readUpToNextContent(sequence);

		applyAndClearPendingLeadingState(sequence);

		if (peekCharMatch(']')) {
			readChar();
			closeFlowCollection();
			return sequence;
		}

		while (isNotEOF()) {
			final String itemAnchor = readUpToNextContent(sequence);
			if (itemAnchor != null) {
				if (pendingAnchor != null) {
					throw new YamlParseException("Assignment of multiple anchors single YAML node: '" + pendingAnchor + "'", getCurrentLine(), getCurrentColumn());
				}
				pendingAnchor = itemAnchor;
			}

			final YamlNode itemNode;
			final long itemLine = getCurrentLine();
			if (peekCharMatch(',')) {
				throw new YamlParseException("Empty flow sequence entry", getCurrentLine(), getCurrentColumn());
			} else if (peekCharMatch(']')) {
				readChar();
				closeFlowCollection();

				readUpToNextContent(sequence);

				return sequence;
			} else {
				setPathArrayIndex(sequencePathSize, itemIndex++);
				itemNode = parseFlowNode();
			}
			final boolean itemIsExplicitKey = lastFlowNodeWasExplicitKey;

			if (pendingAnchor != null) {
				assignAnchor(itemNode, pendingAnchor);
				pendingAnchor = null;
			}

			applyAndClearPendingLeadingState(itemNode);

			// A comment on the line of the item is its inline comment, also after a quoted item
			pendingAnchor = readUpToNextContent(itemNode);
			if (pendingAnchor != null) {
				assignAnchor(itemNode, pendingAnchor);
				pendingAnchor = null;
			}

			skipBlanks();

			if (peekCharMatch(']')) {
				sequence.add(itemNode);
				readChar();
				closeFlowCollection();

				pendingAnchor = readUpToNextContent(sequence);
				if (pendingAnchor != null) {
					assignAnchor(sequence, pendingAnchor);
					pendingAnchor = null;
				}

				return sequence;
			} else if (peekCharMatch(',')) {
				sequence.add(itemNode);
				readChar();

				// An anchor after ',' belongs to the next item
				pendingAnchor = readUpToNextContent(itemNode);
			} else if (peekCharMatch(':')) {
				if (getCurrentLine() != itemLine && !itemIsExplicitKey) {
					throw new YamlParseException("Implicit key of a flow sequence entry must be on a single line", getCurrentLine(), getCurrentColumn());
				}
				readChar();

				indentationsAdd(1);

				final YamlMapping mapping = new YamlMapping();

				final String valueAnchor = readUpToNextContent(null);
				final YamlNode valueNode = parseFlowNode();
				if (valueAnchor != null) {
					assignAnchor(valueNode, valueAnchor);
				}

				applyAndClearPendingLeadingState(valueNode);

				pendingAnchor = readUpToNextContent(valueNode);
				if (pendingAnchor != null) {
					assignAnchor(valueNode, pendingAnchor);
					pendingAnchor = null;
				}

				mapping.add(itemNode, valueNode);

				sequence.add(mapping);

				if (peekCharMatch(']')) {
					readChar();
					closeFlowCollection();

					pendingAnchor = readUpToNextContent(sequence);
					if (pendingAnchor != null) {
						assignAnchor(sequence, pendingAnchor);
						pendingAnchor = null;
					}

					return sequence;
				} else if (peekCharMatch(',')) {
					readChar();

					// An anchor after ',' belongs to the next item
					pendingAnchor = readUpToNextContent(valueNode);
				}
			} else {
				throw new YamlParseException("Invalid flow YAML sequence syntax found", getCurrentLine(), getCurrentColumn());
			}
		}

		throw new YamlParseException("Missing closing bracket ']' of flow sequence", getCurrentLine(), getCurrentColumn());
	}

	/**
	 * Skips the blanks (spaces and tabs) at the current read position and counts them.
	 *
	 * @return the number of blanks skipped
	 * @throws Exception
	 *             if reading fails
	 */
	private int skipAndCountBlanks() throws Exception {
		int blanks = 0;
		while (isNotEOF() && (peekCharMatch(' ') || peekCharMatch('\t'))) {
			readChar();
			blanks++;
		}
		return blanks;
	}

	/**
	 * Adds the indentation of a root node, which starts indented at the beginning of a line, like
	 * " - item". The leading blanks were already read, but the following lines are indented
	 * relative to them.
	 */
	private void addRootIndentation() {
		int rootIndentation = rootLineIndentation;
		for (final int indentationSize : indentations) {
			rootIndentation -= indentationSize;
		}
		if (rootIndentation > 0) {
			indentationsAdd(rootIndentation);
		}
	}

	private void indentationsAdd(final int indentationSize) {
		indentations.add(indentationSize);
	}

	/**
	 * @param mappingIndentation
	 * 	the indentation level of the enclosing mapping/sequence *before* the caller possibly
	 * 	added a temporary indentation level reflecting the column this scalar starts on. Used to
	 * 	determine whether such a temporary level was actually added and therefore needs to be
	 * 	removed again before parsing a multiline block scalar's own, independent indentation.
	 */
	private YamlNode readUnquotedScalarString(final int mappingIndentation) throws Exception {
		if (peekCharMatchAny("%@`,]}")) {
			throw new YamlParseException("Plain scalar must not start with the indicator character '" + peekChar() + "'", getCurrentLine(), getCurrentColumn());
		}
		String scalarString = "";
		final int scalarStartIndentations = (int) getCurrentColumn() - 1;
		String nextScalarStringLine = "";
		String inlineComment = null;
		boolean continuationLineRead = false;
		while (isNotEOF()) {
			if (isIndicatorFollowedBySeparator(':')) {
				if (continuationLineRead) {
					throw new YamlParseException("Mapping values are not allowed within a multi-line plain scalar", getCurrentLine(), getCurrentColumn());
				}
				// Mapping separator ': ' ends Scalar
				break;
			} else if ((peekCharMatch(' ') || peekCharMatch('\t') || peekCharMatch('\n')) && peekNextCharMatch(1, '#')) {
				// Comment ' #' ends Scalar
				if (peekCharMatch(' ') || peekCharMatch('\t')) {
					readChar();
					inlineComment = readInlineComment();
					final String headerCandidate = nextScalarStringLine.trim();
					if (scalarString.isEmpty() && (headerCandidate.startsWith("|") || headerCandidate.startsWith(">")) && peekCharMatch('\n')) {
						// Block scalar header with comment, like "| # comment": its content starts on the next line
						readChar();
					}
				}
				break;
			} else if (peekCharMatch('\n')) {
				readChar();
				nextScalarStringLine = nextScalarStringLine.trim();
				if (nextScalarStringLine.startsWith("|") || nextScalarStringLine.startsWith(">")) {
					break;
				}
				if (nextScalarStringLine.length() > 0) {
					if (scalarString.length() > 0 && !scalarString.endsWith("\n")) {
						scalarString += " ";
					}
					scalarString += nextScalarStringLine;
					nextScalarStringLine = "";
				} else {
					scalarString += "\n";
				}
				final int nextLineIndentation = readIndentationOfCurrentLine();
				if (isEOF()) {
					applyIndentationLevel(nextLineIndentation);
					break;
				} else if (peekCharMatch('\n')) {
					// Empty line, part of the scalar if further content follows
				} else if (peekCharMatch('#')) {
					// A comment line ends the scalar, its indentation is irrelevant for the block structure
					break;
				} else if (nextLineIndentation == 0 && isDocumentMarkerAtLineStart()) {
					// A document marker ends the scalar and the document
					applyIndentationLevel(0);
					break;
				} else if (nextLineIndentation > mappingIndentation || nextLineIndentation >= scalarStartIndentations || (flowLevel == 0 && nextLineIndentation > blockParentIndentation && blockParentIndentation >= 0)) {
					// Continuation line of a multi-line plain scalar: indented deeper than its parent
					// (e.g. the mapping key) or at least as deep as the scalar itself (e.g. a sequence item)
					continuationLineRead = true;
				} else {
					applyIndentationLevel(nextLineIndentation);
					break;
				}
			} else {
				nextScalarStringLine += readChar();
			}
		}

		if (nextScalarStringLine.length() > 0) {
			if (scalarString.length() > 0 && !scalarString.endsWith("\n")) {
				scalarString += " ";
			}
			scalarString += nextScalarStringLine.trim();
		}

		// While looking ahead for a possible continuation of this plain scalar onto further
		// lines, any blank lines found were speculatively appended above as literal "\n"
		// characters (matching plain-scalar folding rules, in case real content followed them
		// on a later line). If parsing then stopped right here instead (this scalar has ended),
		// those trailing "\n" characters are about to be discarded by trim() below and never
		// end up part of this scalar's value; they were genuinely blank lines in the source
		// between this scalar and whatever node follows, so they must be counted as such here.
		int trailingBlankLineCount = 0;
		while (trailingBlankLineCount < scalarString.length()
				&& scalarString.charAt(scalarString.length() - 1 - trailingBlankLineCount) == '\n') {
			trailingBlankLineCount++;
		}
		addPendingEmptyLines(trailingBlankLineCount);

		scalarString = scalarString.trim();

		// A temporary indentation level reflecting the scalar's start column was only actually
		// added by the caller if that column was indented further than the enclosing
		// mapping/sequence; this must be removed again before parsing a multiline block scalar.
		final boolean removeCallerColumnIndentationBeforeMultiline = scalarStartIndentations > mappingIndentation;

		final YamlScalar scalar = interpretScalarValue(scalarString, inlineComment, removeCallerColumnIndentationBeforeMultiline);

		return scalar;
	}

	private YamlScalar interpretScalarValue(final String scalarString, final String inlineComment) throws Exception {
		return interpretScalarValue(scalarString, inlineComment, false);
	}

	/**
	 * @param removeCallerColumnIndentationBeforeMultiline
	 * 	true if the caller (readUnquotedScalarString, via parseBlockMappingOrScalar) has added a
	 * 	temporary indentation level reflecting the column the scalar started on, which must be
	 * 	removed before parsing a multiline block scalar's own, independent content indentation.
	 * 	false for callers (e.g. readFlowScalarString) that never add such a level, so nothing
	 * 	must be popped off the shared indentations stack here.
	 */
	private YamlScalar interpretScalarValue(final String scalarString, final String inlineComment, final boolean removeCallerColumnIndentationBeforeMultiline) throws Exception {
		if (scalarString.length() == 0) {
			return new YamlScalar(null);		} else if (scalarString.startsWith(">") || scalarString.startsWith("|")) {
			final YamlMultilineScalarType type = YamlMultilineScalarType.getYamlMultilineScalarType(scalarString);
			final YamlMultilineScalarChompingType comping = YamlMultilineScalarChompingType.getYamlMultilineScalarChompingType(scalarString);
			final int indentationIndicator = YamlMultilineScalarType.getYamlMultilineScalarIndentationIndicator(scalarString);

			YamlScalar scalar;
			try {
				if (removeCallerColumnIndentationBeforeMultiline) {
					indentations.pop();
				}
				scalar = parseMultilineScalar(type, comping, indentationIndicator);
			} catch (final Exception e) {
				throw new YamlParseException("Invalid YAML mulitline scalar: " + e.getMessage(), getCurrentLine(), getCurrentColumn());
			}
			if (inlineComment != null) {
				scalar.setInlineComment(inlineComment);
			}
			return scalar;
		} else {
			final YamlScalar scalar = YamlScalar.fromPlainScalarString(scalarString, currentYamlVersion);
			if (inlineComment != null) {
				scalar.setInlineComment(inlineComment);
			}
			return scalar;
		}
	}

	/**
	 * Reads a key, value or item within a flow mapping or flow sequence: a quoted or plain scalar, a
	 * nested flow collection or an alias, optionally preceded by a tag like "!!str".
	 *
	 * @return the node read
	 * @throws Exception
	 *             if the data is invalid or the node does not fit its tag
	 */
	/**
	 * True if the node read last by {@link #parseFlowNode()} was an explicit key "? key".
	 */
	private boolean lastFlowNodeWasExplicitKey = false;

	private YamlNode parseFlowNode() throws Exception {
		String tag = null;
		if (peekCharMatch('!')) {
			tag = readUpToNext(false, null, ' ', '\t', '\n', ',', ']', '}');
			// The tagged node may follow on the next line
			skipToNextContent();
		}

		lastFlowNodeWasExplicitKey = false;
		if (tag == null && isIndicatorFollowedBySeparator('?')) {
			// Explicit key like "[? key : value]", which may span several lines
			readChar();
			skipToNextContent();
			final YamlNode keyNode = parseFlowNode();
			lastFlowNodeWasExplicitKey = true;
			return keyNode;
		}

		final YamlNode node;
		if (tag != null && (isEOF() || peekCharMatchAny(",]}") || isIndicatorFollowedBySeparator(':'))) {
			// Tag without value, like "!!str" in "{key: !!str, other: 1}"
			node = new YamlScalar("", YamlScalarType.NULL_VALUE);
		} else if (peekCharMatch('\"')) {
			node = readQuotedScalarNode('"');
		} else if (peekCharMatch('\'')) {
			node = readQuotedScalarNode('\'');
		} else if (peekCharMatch('{')) {
			node = parseFlowMapping();
		} else if (peekCharMatch('[')) {
			node = parseFlowSequence();
		} else if (peekCharMatch('*')) {
			node = readFlowAlias();
		} else {
			node = readFlowScalarString();
		}

		if (tag == null) {
			return node;
		} else {
			return applyTag(tag, node);
		}
	}

	/**
	 * Reads an alias like "*anchor" within a flow collection. The alias name ends at whitespace or a
	 * flow indicator character.
	 *
	 * @return the alias
	 * @throws Exception
	 *             if reading fails
	 */
	private YamlNode readFlowAlias() throws Exception {
		final long aliasLine = getCurrentLine();
		final long aliasColumn = getCurrentColumn();
		readChar();
		final String anchorName = readUpToNext(false, null, ' ', '\t', '\n', ',', '[', ']', '{', '}');
		if (anchorName.isEmpty()) {
			throw new YamlParseException("Missing alias name", getCurrentLine(), getCurrentColumn());
		}
		skipBlanks();
		final YamlAlias alias = new YamlAlias(anchorName);
		aliasPositions.put(alias, new long[] { aliasLine, aliasColumn });
		return alias;
	}

	/**
	 * Checks the aliases of a node read completely: each alias must refer to an anchor defined
	 * before it in document order, and it must not refer to a node containing the alias (cyclic
	 * reference). An anchor defined again replaces the former definition for the following aliases.
	 *
	 * @param node
	 *            the node read, may be null
	 * @param anchors
	 *            anchors defined before the node, extended by the anchors of the node
	 * @throws YamlParseException
	 *             if an alias refers to an unknown anchor or creates a cyclic reference
	 */
	private void checkAliases(final YamlNode node, final Map<String, YamlNode> anchors) throws YamlParseException {
		try {
			checkAliases(node, anchors, Collections.newSetFromMap(new IdentityHashMap<>()));
		} finally {
			aliasPositions.clear();
		}
	}

	private void checkAliases(final YamlNode node, final Map<String, YamlNode> anchors, final Set<YamlNode> enclosingNodes) throws YamlParseException {
		if (node == null) {
			return;
		} else if (node instanceof final YamlAlias alias) {
			final YamlNode target = anchors.get(alias.getTargetAnchorName());
			final long[] position = aliasPositions.getOrDefault(alias, new long[] { getCurrentLine(), getCurrentColumn() });
			if (target == null) {
				throw new YamlParseException("Alias refers to unknown anchor: '*" + alias.getTargetAnchorName() + "'", position[0], position[1]);
			} else if (enclosingNodes.contains(target)) {
				throw new YamlParseException("Cyclic alias reference: '*" + alias.getTargetAnchorName() + "' is part of the node it refers to", position[0], position[1]);
			}
			return;
		}

		// The anchor precedes the content of its node, so it is defined for the aliases within
		if (node.getAnchorName() != null) {
			anchors.put(node.getAnchorName(), node);
		}
		if (node instanceof final YamlMapping mapping) {
			enclosingNodes.add(mapping);
			for (final YamlNode key : mapping.keySet()) {
				checkAliases(key, anchors, enclosingNodes);
				checkAliases(mapping.get(key), anchors, enclosingNodes);
			}
			enclosingNodes.remove(mapping);
		} else if (node instanceof final YamlSequence sequence) {
			enclosingNodes.add(sequence);
			for (final YamlNode item : sequence.items()) {
				checkAliases(item, anchors, enclosingNodes);
			}
			enclosingNodes.remove(sequence);
		}
	}

	/**
	 * Checks whether a node is a quoted scalar or a flow collection. After such a "JSON like" key in a
	 * flow collection the ':' may be directly followed by the value, like in {"key":value}.
	 */
	private static boolean isJsonLikeNode(final YamlNode node) {
		return (node instanceof final YamlScalar scalar && scalar.getQuoteType() != null) || node instanceof YamlMapping || node instanceof YamlSequence;
	}

	private YamlNode readFlowScalarString() throws Exception {
		if (peekCharMatch('-') && (peekNextChar(1) == null || peekNextCharMatchAny(1, " \t\n,[]{}"))) {
			throw new YamlParseException("Plain scalar must not consist of the indicator '-' only", getCurrentLine(), getCurrentColumn());
		}
		String scalarString = "";
		while(true) {
			scalarString += readUpToNext(false, null, ",:#{}[]\n".toCharArray());

			// Allow sexagesimal numbers like "190:20:30.15" or text with ':' without following whitespace
			if (peekCharMatch(':') && (peekNextChar(1) == null || peekNextCharNotMatchAny(1, ",{}[] \t\n"))) {
				readChar();
				scalarString += ":";
			} else if (peekCharMatch('#') && scalarString.length() > 0 && !Character.isWhitespace(scalarString.charAt(scalarString.length() - 1))) {
				// '#' only starts a comment after whitespace, otherwise it is part of the text like "a#b"
				readChar();
				scalarString += "#";
			} else if (peekCharMatch('\n') && !scalarString.isBlank()) {
				// A plain scalar in a flow collection may continue on the next lines, linebreaks are folded
				int linebreaks = 0;
				while (isNotEOF() && (peekCharMatch(' ') || peekCharMatch('\t') || peekCharMatch('\n'))) {
					if (readChar() == '\n') {
						linebreaks++;
					}
				}
				if (isDocumentMarkerAtLineStart()) {
					throw new YamlParseException("Document marker within flow collection", getCurrentLine(), getCurrentColumn());
				} else if (isEOF() || peekCharMatchAny(",[]{}#") || (peekCharMatch(':') && (peekNextChar(1) == null || peekNextCharMatchAny(1, ",{}[] \t\n")))) {
					break;
				}
				scalarString = scalarString.stripTrailing() + (linebreaks == 1 ? " " : "\n".repeat(linebreaks - 1));
			} else {
				break;
			}
		}

		scalarString = scalarString.trim();

		String inlineComment = null;
		if (peekCharMatch('#')) {
			inlineComment = readInlineComment();
		}

		skipBlanks();

		final YamlScalar scalar = interpretScalarValue(scalarString, inlineComment);

		return scalar;
	}

	/**
	 * Reads the content lines of a block scalar after its header line.
	 * <p>
	 * The content indentation is given by the indentation indicator relative to
	 * {@link #blockParentIndentation}, or detected from the first non-empty line, which must be
	 * indented deeper than the parent. Only spaces count as indentation, tabs after the indentation
	 * are content. Lines consisting of spaces only are empty lines, unless they have more spaces than
	 * the content indentation. A less indented comment line ends the content: it and all following
	 * comment and empty lines are trailing comments, which belong to the node that follows.
	 * </p>
	 */
	private YamlScalar parseMultilineScalar(
			final YamlMultilineScalarType multilineType,
			final YamlMultilineScalarChompingType chompingType,
			final int indentationIndicator) throws Exception {
		final int parentIndentation = blockParentIndentation;
		final int minimumContentIndentation = parentIndentation + 1;

		final StringBuilder raw = new StringBuilder();

		// Base indentation of the content, given by the indentation indicator or detected from the first non-empty line
		int contentIndentation = -1;
		if (indentationIndicator > 0) {
			contentIndentation = parentIndentation < 0 ? indentationIndicator : parentIndentation + indentationIndicator;
		}

		// Empty lines since the last content line
		int pendingEmptyLines = 0;

		// Most spaces of an empty line before the first content line, which must not be more than the content indentation
		int leadingEmptyLinesMaximumSpaces = 0;

		// Empty lines after the last content line and before trailing comments, which are linebreaks of the scalar (kept by "+" chomping)
		int trailingEmptyLines = -1;

		// Indentation of the line following the scalar, which belongs to the surrounding block structure again
		int trailingNonScalarLineIndentation = -1;

		while (isNotEOF()) {
			if (isDocumentMarkerAtLineStart()) {
				// A document marker ends the scalar and the document
				if (trailingEmptyLines < 0) {
					trailingEmptyLines = pendingEmptyLines;
					pendingEmptyLines = 0;
				}
				trailingNonScalarLineIndentation = 0;
				break;
			}
			int lineIndentation = 0;
			while (peekCharMatch(' ')) {
				readChar();
				lineIndentation++;
			}

			if (isEOF() || peekCharMatch('\n')) {
				// Empty line, or a content line of spaces only if it has more spaces than the content indentation
				final boolean lineHadLinebreak = isNotEOF();
				if (lineHadLinebreak) {
					readChar();
				} else if (lineIndentation == 0) {
					break;
				}
				if (trailingEmptyLines >= 0) {
					// Empty line between trailing comments
					addPendingEmptyLines(1);
				} else if (contentIndentation >= 0 && lineIndentation > contentIndentation) {
					appendEmptyLines(raw, pendingEmptyLines);
					pendingEmptyLines = 0;
					raw.append(Utilities.repeat(" ", lineIndentation - contentIndentation)).append('\n');
				} else {
					if (contentIndentation < 0) {
						leadingEmptyLinesMaximumSpaces = Math.max(leadingEmptyLinesMaximumSpaces, lineIndentation);
					}
					pendingEmptyLines++;
				}
				continue;
			}

			final boolean belowContentIndentation = contentIndentation >= 0 ? lineIndentation < contentIndentation : lineIndentation < minimumContentIndentation;
			if (belowContentIndentation && trailingEmptyLines < 0 && peekCharMatch('\t')) {
				throw new YamlParseException("Tabs must not be used for indentation", getCurrentLine(), getCurrentColumn());
			}
			if (trailingEmptyLines >= 0 || belowContentIndentation) {
				if (peekCharMatch('#')) {
					if (trailingEmptyLines < 0) {
						// First trailing comment line: the content of the scalar has ended
						trailingEmptyLines = pendingEmptyLines;
						pendingEmptyLines = 0;
						if (chompingType != YamlMultilineScalarChompingType.KEEP) {
							// Empty lines not kept as linebreaks of the scalar precede the comment
							addPendingEmptyLines(trailingEmptyLines);
						}
					}
					pendingLeadingComments.add(readInlineComment());
					if (isNotEOF()) {
						readChar();
					}
					continue;
				} else if (trailingEmptyLines >= 0 && !belowContentIndentation) {
					throw new YamlParseException("Block scalar content after trailing comment lines", getCurrentLine(), getCurrentColumn());
				} else {
					// This line belongs to the surrounding block structure again
					trailingNonScalarLineIndentation = lineIndentation;
					break;
				}
			}

			if (contentIndentation < 0) {
				if (leadingEmptyLinesMaximumSpaces > lineIndentation) {
					throw new YamlParseException("Leading empty line of block scalar has more spaces than the first content line", getCurrentLine(), getCurrentColumn());
				}
				contentIndentation = lineIndentation;
			}

			appendEmptyLines(raw, pendingEmptyLines);
			pendingEmptyLines = 0;
			raw.append(Utilities.repeat(" ", lineIndentation - contentIndentation));
			raw.append(readUpToNext(false, null, '\n'));
			raw.append('\n');
			if (isNotEOF()) {
				readChar();
			}
		}

		if (trailingEmptyLines < 0) {
			trailingEmptyLines = pendingEmptyLines;
			if (trailingNonScalarLineIndentation >= 0 && chompingType != YamlMultilineScalarChompingType.KEEP) {
				// Empty lines not kept as linebreaks of the scalar precede the following node
				addPendingEmptyLines(trailingEmptyLines);
			}
		}
		appendEmptyLines(raw, trailingEmptyLines);

		if (trailingNonScalarLineIndentation >= 0) {
			// Restore the indentation of the trailing, non-scalar line to the shared
			// indentations stack so the caller continuing to parse the surrounding block
			// structure (mapping/sequence) sees the correct current indentation level again.
			int remaining = trailingNonScalarLineIndentation;
			for (final int existingIndentationSize : indentations) {
				remaining -= existingIndentationSize;
			}
			if (remaining > 0) {
				indentationsAdd(remaining);
			} else {
				while (remaining < 0 && indentations.size() > 1) {
					remaining += indentations.pop();
				}
				if (remaining != 0) {
					throw new YamlParseException("Unexpected indentation level: " + remaining + " indentations: " + indentations, getCurrentLine(), getCurrentColumn());
				}
			}
		}

		String text = raw.toString();
		text = applyChomping(text, chompingType);
		if (multilineType == YamlMultilineScalarType.FOLDED) {
			text = foldLines(text);
		}

		final YamlScalar scalar = new YamlScalar(text, YamlScalarType.MULTILINE);
		scalar.setMultilineType(multilineType);
		scalar.setMultilineChompingType(chompingType);
		scalar.setIndentationIndicator(indentationIndicator);

		return scalar;
	}

	private static void appendEmptyLines(final StringBuilder text, final int count) {
		for (int i = 0; i < count; i++) {
			text.append('\n');
		}
	}

	@SuppressWarnings("static-method")
	private String applyChomping(final String text, final YamlMultilineScalarChompingType chompingType) {
		switch (chompingType) {
			case KEEP:
				return text;
			case STRIP:
				return text.replaceAll("\\n+$", "");
			case CLIP:
			default:
				final String content = text.replaceAll("\\n+$", "");
				return content.isEmpty() ? "" : content + "\n";
		}
	}

	/**
	 * Folds the lines of a folded block scalar: a linebreak between two lines that are not more
	 * indented becomes a blank, each following empty line becomes a linebreak. Linebreaks next to
	 * more indented lines and trailing linebreaks (after chomping) are kept.
	 */
	@SuppressWarnings("static-method")
	private String foldLines(final String text) {
		int contentEnd = text.length();
		while (contentEnd > 0 && text.charAt(contentEnd - 1) == '\n') {
			contentEnd--;
		}
		final String[] lines = text.substring(0, contentEnd).split("\n", -1);
		final StringBuilder foldedText = new StringBuilder();
		int index = 0;
		while (index < lines.length - 1 && lines[index].isEmpty()) {
			// Leading empty lines are kept as linebreaks
			foldedText.append("\n");
			index++;
		}
		foldedText.append(lines[index]);
		String previousLine = lines[index];
		index++;
		while (index < lines.length) {
			int emptyLines = 0;
			while (lines[index].isEmpty()) {
				emptyLines++;
				index++;
			}
			final String nextLine = lines[index];
			final boolean moreIndented = previousLine.startsWith(" ") || previousLine.startsWith("\t") || nextLine.startsWith(" ") || nextLine.startsWith("\t");
			if (moreIndented) {
				foldedText.append("\n".repeat(emptyLines + 1));
			} else if (emptyLines == 0) {
				foldedText.append(' ');
			} else {
				foldedText.append("\n".repeat(emptyLines));
			}
			foldedText.append(nextLine);
			previousLine = nextLine;
			index++;
		}
		return foldedText.append(text.substring(contentEnd)).toString();
	}

	private int getNumberOfIndentationChars() {
		return indentations.stream().reduce(0, Integer::sum);
	}

	/**
	 * Adds a mapping key to the current path and checks whether the searched path was found.
	 * Collection keys are represented by "&lt;ComplexYamlKey&gt;".
	 *
	 * @throws FoundPathEvent
	 *             if the path searched by {@link #readUpToPath(String)} was reached
	 */
	private void addPathKey(final YamlNode key) throws FoundPathEvent {
		if (key instanceof final YamlScalar scalarKey) {
			currentPath.add(new JsonPathPropertyElement(scalarKey.getValueString()));
		} else {
			currentPath.add(new JsonPathPropertyElement("<ComplexYamlKey>"));
		}
		checkSearchPathFound();
	}

	/**
	 * Sets the index of the sequence item read next. The array element of the sequence is the
	 * element after the given path size.
	 *
	 * @throws FoundPathEvent
	 *             if the path searched by {@link #readUpToPath(String)} was reached
	 */
	private void setPathArrayIndex(final int sequencePathSize, final int index) throws FoundPathEvent {
		truncatePath(sequencePathSize);
		currentPath.add(new JsonPathArrayElement(index));
		checkSearchPathFound();
	}

	/**
	 * Removes path elements added by nested nodes, so the path has the given size again.
	 */
	private void truncatePath(final int size) {
		while (currentPath.size() > size) {
			currentPath.removeLastElement();
		}
	}

	private void checkSearchPathFound() throws FoundPathEvent {
		if (searchPath != null && currentPath.equals(searchPath)) {
			throw new FoundPathEvent("Path '" + searchPath + "' was found");
		}
	}

	/**
	 * Skips blanks, comments and empty lines up to the next content, reading an anchor on the way.
	 * <p>
	 * In block context only an anchor on the current line is read, like the anchor of a value in
	 * "key: &amp;anchor value". An anchor at the beginning of a following line belongs to the node
	 * starting there and is left for {@link #parseYamlNode()}, e.g. the anchor of a key in
	 * "&amp;anchor key: value". In flow collections linebreaks have no meaning, so an anchor on a
	 * following line is read, too.
	 * </p>
	 *
	 * @param yamlNodeForInlineComment
	 *            the node to set a comment on the current line at, or null
	 * @return the anchor name, or null if there is none
	 * @throws Exception
	 *             if reading fails or a node has two anchors
	 */
	private String readUpToNextContent(final YamlNode yamlNodeForInlineComment) throws Exception {
		// At the beginning of a line an anchor belongs to the node starting there, not to the node
		// read before, e.g. after the value "1" of "x: 1" followed by the line "&anchor key: 2"
		final boolean anchorAllowed = flowLevel > 0 || getCurrentColumn() > 1;
		skipBlanks();
		checkCommentSeparation();

		final long startLine = getCurrentLine();
		String pendingAnchor = null;
		if (anchorAllowed && peekCharMatch('&')) {
			readChar();
			// Anchors may be redefined: aliases refer to the latest definition before them
			pendingAnchor = readUpToNext(false, null, '\n', ' ', '\t');
			skipBlanks();
		}

		if (peekCharMatch('#') && yamlNodeForInlineComment != null) {
			yamlNodeForInlineComment.setInlineComment(readInlineComment());
		}

		skipEmptyLinesAndReadNextIndentationAndLeadingComments();

		if (anchorAllowed && peekCharMatch('&') && (flowLevel > 0 || getCurrentLine() == startLine)) {
			if (pendingAnchor != null) {
				throw new YamlParseException("Assignment of multiple anchors single YAML node: '" + pendingAnchor + "'", getCurrentLine(), getCurrentColumn());
			}

			readChar();
			pendingAnchor = readUpToNext(false, null, '\n', ' ', '\t');

			skipEmptyLinesAndReadNextIndentationAndLeadingComments();
		}

		return pendingAnchor;
	}

	/**
	 * Skips blanks, comments and empty lines up to the next content without reading an anchor. An
	 * anchor there is read by {@link #parseYamlNode()}, which decides whether it belongs to the node
	 * or to the first key of a block mapping.
	 *
	 * @throws Exception
	 *             if reading fails
	 */
	private void skipToNextContent() throws Exception {
		skipBlanks();
		skipEmptyLinesAndReadNextIndentationAndLeadingComments();
	}

}