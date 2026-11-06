package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import de.soderer.yaml.data.YamlToken;
import de.soderer.yaml.data.YamlVersion;
import de.soderer.yaml.data.directive.YamlDirective;
import de.soderer.yaml.data.directive.YamlTagDirective;
import de.soderer.yaml.data.directive.YamlVersionDirective;
import de.soderer.yaml.exception.FoundPathEvent;
import de.soderer.yaml.exception.YamlParseException;

// TODO: Check alias references after document read
// TODO: Check cyclic dependencies in aliases
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
 */
public class YamlReader extends BasicReadAheadReader {
	private final Stack<Integer> indentations = new Stack<>();
	private final Map<String, YamlNode> anchorTable = new HashMap<>();
	private List<String> pendingLeadingComments = new ArrayList<>();
	private int pendingLeadingEmptyLines = 0;
	private int pendingTrailingEmptyLinesAfterComments = 0;
	private Boolean documentContentStarted = null;

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
	 * Blanks before the root node on its line, which are not part of the indentations yet.
	 */
	private int rootLineIndentation = 0;

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
		super(inputStream, encodingCharset);

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

	private YamlDocument parseDocument() throws Exception {
		try {
			final YamlDocument document = new YamlDocument();
			currentPath = new JsonPath();
			currentYamlVersion = defaultYamlVersion;
			if (!pendingLeadingComments.isEmpty()) {
				for (final String commentLine : pendingLeadingComments) {
					document.addLeadingComment(commentLine);
				}
				pendingLeadingComments = new ArrayList<>();
			}
			// Note: YamlDocument is not a YamlNode and has no leadingEmptyLinesCount of its own;
			// any pendingLeadingEmptyLines accumulated so far are intentionally left untouched
			// here so they naturally end up attributed to the document's root node instead.

			while (isNotEOF()) {
				// Blanks before a root node on the current line, which readUpToNextContent() skips
				// without adding them to the indentations
				final long lineBeforeContent = getCurrentLine();
				final int leadingBlanks = getCurrentColumn() == 1 ? skipAndCountBlanks() : 0;
				// An anchor of the root node is read by parseYamlNode()
				skipToNextContent();
				rootLineIndentation = getCurrentLine() == lineBeforeContent ? leadingBlanks : 0;
				if (peekCharMatch('%')) {
					final YamlDirective<?> directive = readDirective();
					if (directive == null) {
						// Unknown directive, which is ignored
						documentContentStarted = false;
						continue;
					} else if (directive instanceof final YamlVersionDirective versionDirective) {
						final YamlVersion directiveVersion = YamlVersion.fromDirectiveValue(versionDirective.getYamlVersion());
						if (directiveVersion != null) {
							currentYamlVersion = directiveVersion;
						}
					}
					document.addDirective(directive);
					documentContentStarted = false;
				} else if (peekCharMatch('-')) {
					if (searchPath != null && getCurrentPath().equals(searchPath)) {
						throw new FoundPathEvent("Path '" + searchPath + "' was found");
					}
					if (peekNextCharMatch(1, '-') && peekNextCharMatch(2, '-')) {
						readChar();
						readChar();
						readChar();

						skipBlanks();
						if (peekCharMatch('#')) {
							final String documentComment = readInlineComment();
							document.addLeadingComment(documentComment);
						}

						if (documentContentStarted != null && documentContentStarted) {
							// Start of next document
							break;
						} else if (documentContentStarted != null && !documentContentStarted) {
							documentContentStarted = true;
						} else {
							documentContentStarted = true;
							skipToNextContent();
							document.setRoot(parseYamlNode());
							return document;
						}
					} else if (peekNextCharMatch(1, ' ') || peekNextCharMatch(1, '\t') || peekNextCharMatch(1, '\n')) {
						if (documentContentStarted != null && !documentContentStarted) {
							throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
						} else {
							addRootIndentation();
							document.setRoot(parseBlockSequence());
							return document;
						}
					} else if (documentContentStarted != null && !documentContentStarted) {
						throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
					} else {
						document.setRoot(parseBlockMappingOrScalar(null));
						return document;
					}
				} else if (peekCharMatch('.') && peekNextCharMatch(1, '.') && peekNextCharMatch(2, '.')) {
					readChar();
					readChar();
					readChar();
					skipToNextContent();
					documentContentStarted = null;
					// End of document
					break;
				} else if (documentContentStarted != null && !documentContentStarted) {
					throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
				} else {
					documentContentStarted = true;
					if (searchPath != null && getCurrentPath().equals(searchPath)) {
						throw new FoundPathEvent("Path '" + searchPath + "' was found");
					}
					addRootIndentation();
					document.setRoot(parseYamlNode());
					return document;
				}
			}

			return null;
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
				final YamlNode nextSequenceItem = readNextSequenceItem();
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
		while (peekCharMatch(' ') || peekCharMatch('\t')) {
			readChar();
			count++;
		}
		return count;
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
		while (isNotEOF()) {
			if (peekCharMatch(' ') || peekCharMatch('\t')) {
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
			} else {
				break;
			}
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

	/**
	 * Measures the indentation (number of leading spaces/tabs) of the next line without
	 * touching the {@link #indentations} stack and without comparing it against any
	 * previously known indentation level.
	 * This is used while reading the raw content lines of a multiline block scalar,
	 * where each line's indentation must only be compared against the block scalar's own
	 * base indentation, not against the general block-structure indentation stack.
	 *
	 * @return
	 * 	the number of leading spaces/tabs of the next line<br />
	 * 	or -1 if the next line is empty (only a linebreak or EOF, no indentation consumed)
	 */
	private int peekNextLineIndentation() throws Exception {
		int nextIndentationSize = 0;
		while (isNotEOF()) {
			if (peekCharMatch(' ') || peekCharMatch('\t')) {
				readChar();
				nextIndentationSize++;
			} else if (peekCharMatch('\n')) {
				return -1;
			} else {
				break;
			}
		}
		if (isEOF()) {
			return -1;
		} else {
			return nextIndentationSize;
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
		if (peekCharMatch('!')) {
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
			// An anchor may also follow the tag, like "!!str &anchor value"
			final String anchorAfterTag = readUpToNextContent(null);
			if (anchorAfterTag != null) {
				if (anchor != null) {
					throw new YamlParseException("Assignment of multiple anchors single YAML node: '" + anchor + "'", getCurrentLine(), getCurrentColumn());
				}
				anchor = anchorAfterTag;
			}
			final boolean contentOnTagLine = !tagLineEnded && getCurrentLine() == tagLine;

			final YamlNode typedNode;
			if (tagLineEnded && parentIndentation != null && isEmptyTaggedValue(parentIndentation, allowSequenceAtParentIndentation)) {
				// Tag without value, like "key: !!null" followed by the next key
				typedNode = applyTag(tag, new YamlScalar("", YamlScalarType.NULL_VALUE));
			} else if (!contentOnTagLine) {
				// Properties on a line of their own belong to the following node, also to a block mapping
				typedNode = applyTag(tag, parseYamlNodeWithoutTag());
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
		} else {
			return parseYamlNodeWithoutTag();
		}
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
		} else if (allowSequenceAtParentIndentation && contentIndentation == parentIndentation && peekCharMatch('-') && peekNextCharMatchAny(1, " \t\n")) {
			return false;
		} else {
			return true;
		}
	}

	private YamlNode parseYamlNodeWithoutTag() throws Exception {
		if (peekCharMatch('-') && (peekNextCharMatch(1, ' ') || peekNextCharMatch(1, '\t') || peekNextCharMatch(1, '\n'))) {
			final YamlNode sequence = parseBlockSequence();
			return sequence;
		} else if (peekCharMatch('&')) {
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
			if (getCurrentLine() == anchorLine) {
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
		} else if (peekCharMatch('*')) {
			final PendingLeadingState pendingLeadingState = takePendingLeadingState();

			final YamlNode alias = parseAlias();

			applyPendingLeadingState(pendingLeadingState, alias);

			return alias;
		} else if (peekCharMatch('{')) {
			final int flowStartIndentations = getNumberOfIndentationChars();
			final PendingLeadingState pendingLeadingState = takePendingLeadingState();

			final PendingNodeProperties flowProperties = takePendingNodePropertiesAtCurrentPosition();
			YamlNode flowMapping = parseFlowMapping();
			flowMapping = applyPendingNodeProperties(flowProperties, flowMapping);

			applyPendingLeadingState(pendingLeadingState, flowMapping);

			readUpToNextContent(flowMapping);

			if (peekCharMatch(':') && flowStartIndentations + 1 < getCurrentColumn()) {
				return parseBlockMappingOrScalar(flowMapping);
			} else {
				return flowMapping;
			}
		} else if (peekCharMatch('[')) {
			final int flowStartIndentations = getNumberOfIndentationChars();
			final PendingLeadingState pendingLeadingState = takePendingLeadingState();

			final PendingNodeProperties flowProperties = takePendingNodePropertiesAtCurrentPosition();
			YamlNode flowSequence = parseFlowSequence();
			flowSequence = applyPendingNodeProperties(flowProperties, flowSequence);

			applyPendingLeadingState(pendingLeadingState, flowSequence);

			readUpToNextContent(flowSequence);

			if (peekCharMatch(':') && flowStartIndentations + 1 < getCurrentColumn()) {
				return parseBlockMappingOrScalar(flowSequence);
			} else {
				return flowSequence;
			}
		} else {
			return parseBlockMappingOrScalar(null);
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
		} else {
			throw new Exception("Unknown YAML datatype: '" + datatype + "'");
		}
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
	private static YamlNode applyTag(final String tag, final YamlNode yamlNode) throws Exception {
		if (tag.startsWith("!!")) {
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

	private YamlNode parseAlias() throws Exception {
		if (peekCharNotMatch('*')) {
			throw new YamlParseException("Expected alias start not found", getCurrentLine(), getCurrentColumn());
		}

		readChar();

		final String anchorName = readUpToNext(false, null, '\n', ' ', '\t');
		final YamlAlias alias = new YamlAlias(anchorName);

		skipBlanks();

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
		if (peekCharNotMatch('-') || (peekNextCharNotMatch(1, ' ') && peekNextCharNotMatch(1, '\t') && peekNextCharNotMatch(1, '\n'))) {
			throw new YamlParseException("Expected sequence start not found", getCurrentLine(), getCurrentColumn());
		}

		final int sequenceIndentation = (int) getCurrentColumn() - 1;

		final YamlSequence sequence = new YamlSequence();
		updatePath(YamlToken.YamlSequence_Start, null);

		while (isNotEOF() && peekCharMatch('-') && (int) getCurrentColumn() - 1 == sequenceIndentation) {
			if (peekCharMatch('-') && peekNextCharMatch(1, '-') && peekNextCharMatch(2, '-')) {
				readChar();
				readChar();
				readChar();
				documentContentStarted = true;
			} else if (peekCharMatch('.') && peekNextCharMatch(1, '.') && peekNextCharMatch(2, '.')) {
				readChar();
				readChar();
				readChar();
				documentContentStarted = null;
			} else {
				final YamlNode nextItemNode = readNextSequenceItem();
				if (nextItemNode != null) {
					sequence.add(nextItemNode);
				} else {
					break;
				}
			}
		}

		updatePath(YamlToken.YamlSequence_End, null);

		return sequence;
	}

	private YamlNode readNextSequenceItem() throws Exception {
		final PendingLeadingState pendingLeadingState = takePendingLeadingState();

		if (peekCharMatch('-') && (peekNextCharMatch(1, ' ') || peekNextCharMatch(1, '\t') || peekNextCharMatch(1, '\n'))) {
			final int dashIndentation = (int) getCurrentColumn() - 1;
			readChar();

			// The item content defines the indentation of its lines, e.g. "-   key: value" has 4 characters
			final int blanksAfterDash = skipBlanks();
			final boolean noContentInItemLine = isEOF() || peekCharMatch('\n') || peekCharMatch('#');
			if (noContentInItemLine) {
				indentationsAdd(2);

				// The item content may follow on the next lines, but only if they are indented deeper than the dash
				final Boolean indentationChange = skipEmptyLinesAndReadNextIndentationAndLeadingComments();
				if (isEOF() || Boolean.FALSE.equals(indentationChange)) {
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

	private YamlNode parseBlockMappingOrScalarInternal(final YamlNode firstKeyNode, final String firstKeyTag, final String firstKeyAnchor, final long firstKeyColumn, final boolean[] firstKeyPropertiesUsed) throws Exception {
		final int mappingIndentation = getNumberOfIndentationChars();

		YamlNode keyOrScalarNode;
		if (firstKeyNode != null) {
			keyOrScalarNode = firstKeyNode;
		} else {
			// Comments collected while parsing the node itself (e.g. by a multiline block
			// scalar's own lookahead past its content, which may run into a trailing comment
			// belonging to whatever follows) must not be mistaken for comments that preceded
			// this node; only comments already pending before parsing started belong to it.
			final int leadingCommentsCountBeforeNode = pendingLeadingComments.size();
			final int leadingEmptyLinesCountBeforeNode = pendingLeadingEmptyLines;
			final int postCommentEmptyLinesCountBeforeNode = pendingTrailingEmptyLinesAfterComments;

			if (peekCharMatch('\"')) {
				if (firstKeyColumn - 1 - getNumberOfIndentationChars() > 0) {
					indentationsAdd((int) firstKeyColumn - 1 - getNumberOfIndentationChars());
				}
				keyOrScalarNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING).withQuoteType(YamlStringQuoteType.DOUBLE);
				skipBlanks();
				if (peekCharMatch('#')) {
					keyOrScalarNode.setInlineComment(readInlineComment());
				}
			} else if (peekCharMatch('\'')) {
				if (firstKeyColumn - 1 - getNumberOfIndentationChars() > 0) {
					indentationsAdd((int) firstKeyColumn - 1 - getNumberOfIndentationChars());
				}
				keyOrScalarNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING).withQuoteType(YamlStringQuoteType.SINGLE);
				skipBlanks();
				if (peekCharMatch('#')) {
					keyOrScalarNode.setInlineComment(readInlineComment());
				}
			} else if (peekCharMatch('[')) {
				keyOrScalarNode = parseFlowSequence();
			} else if (peekCharMatch('{')) {
				keyOrScalarNode = parseFlowMapping();
			} else if (peekCharMatch('?') && (peekNextCharMatch(1, ' ') || peekNextCharMatch(1, '\t') || peekNextCharMatch(1, '\n'))) {
				readChar();
				skipBlanks();
				indentationsAdd(2);
				// Complex key: this node is a mapping
				checkNestingDepth();
				keyOrScalarNode = parseYamlNode();
			} else {
				if (firstKeyColumn - 1 - getNumberOfIndentationChars() > 0) {
					indentationsAdd((int) firstKeyColumn - 1 - getNumberOfIndentationChars());
				}
				keyOrScalarNode = readUnquotedScalarString(mappingIndentation);
			}

			if (leadingCommentsCountBeforeNode > 0) {
				for (final String commentLine1 : pendingLeadingComments.subList(0, leadingCommentsCountBeforeNode)) {
					keyOrScalarNode.addLeadingComment(commentLine1);
				}
				pendingLeadingComments = new ArrayList<>(
						pendingLeadingComments.subList(leadingCommentsCountBeforeNode, pendingLeadingComments.size()));
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
		}

		if (peekCharMatch(':')
				&& mappingIndentation < getCurrentColumn()
				&& (peekNextCharMatch(1, ' ') || peekNextCharMatch(1, '\t') || peekNextCharMatch(1, '\n'))) {
			readChar();

			String pendingAnchor = readUpToNextContent(keyOrScalarNode);

			checkNestingDepth();
			if (firstKeyTag != null) {
				keyOrScalarNode = applyTag(firstKeyTag, keyOrScalarNode);
			}
			if (firstKeyAnchor != null) {
				assignAnchor(keyOrScalarNode, firstKeyAnchor);
			}
			firstKeyPropertiesUsed[0] = true;
			final YamlMapping mapping = new YamlMapping();
			updatePath(YamlToken.YamlMapping_Start, null);
			updatePath(YamlToken.YamlMapping_PropertyKey, keyOrScalarNode);

			YamlNode valueNode;
			if (getCurrentColumn() - 1 > mappingIndentation
					|| (peekCharMatch('-') && peekNextCharMatchAny(1, " \t\n"))) {
				valueNode = parseYamlNode(mappingIndentation, true);
			} else {
				// Empty mapping property value
				valueNode = new YamlScalar("", YamlScalarType.NULL_VALUE);
			}

			if (pendingAnchor != null) {
				assignAnchor(valueNode, pendingAnchor);
				pendingAnchor = null;
			}

			mapping.add(keyOrScalarNode, valueNode);

			while (isNotEOF() && getNumberOfIndentationChars() == mappingIndentation) {
				if (peekCharMatch('.') && peekNextCharMatch(1, '.') && peekNextCharMatch(2, '.')) {
					readChar();
					readChar();
					readChar();
					documentContentStarted = null;
					pendingAnchor = readUpToNextContent(null);
					return mapping;
				} else if (peekCharMatch('-') && peekNextCharMatch(1, '-') && peekNextCharMatch(2, '-')) {
					readChar();
					readChar();
					readChar();
					documentContentStarted = true;
					pendingAnchor = readUpToNextContent(null);
					return mapping;
				}

				// Tag and anchor of the key in any order, like "&anchor !tag key: value"
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

				if (peekCharMatch('\"')) {
					if (keyColumn - 1 - getNumberOfIndentationChars() > 0) {
						indentationsAdd((int) keyColumn - 1 - getNumberOfIndentationChars());
					}
					keyOrScalarNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING).withQuoteType(YamlStringQuoteType.DOUBLE);
					skipBlanks();
					if (peekCharMatch('#')) {
						keyOrScalarNode.setInlineComment(readInlineComment());
					}
				} else if (peekCharMatch('\'')) {
					if (keyColumn - 1 - getNumberOfIndentationChars() > 0) {
						indentationsAdd((int) keyColumn - 1 - getNumberOfIndentationChars());
					}
					keyOrScalarNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING).withQuoteType(YamlStringQuoteType.SINGLE);
					skipBlanks();
					if (peekCharMatch('#')) {
						keyOrScalarNode.setInlineComment(readInlineComment());
					}
				} else if (peekCharMatch('[')) {
					keyOrScalarNode = parseFlowSequence();
				} else if (peekCharMatch('{')) {
					keyOrScalarNode = parseFlowMapping();
				} else if (peekCharMatch('?') && (peekNextCharMatch(1, ' ') || peekNextCharMatch(1, '\t') || peekNextCharMatch(1, '\n'))) {
					readChar();
					skipBlanks();
					indentationsAdd(2);
					keyOrScalarNode = parseYamlNode();
				} else {
					if (keyColumn - 1 - getNumberOfIndentationChars() > 0) {
						indentationsAdd((int) keyColumn - 1 - getNumberOfIndentationChars());
					}
					keyOrScalarNode = readUnquotedScalarString(mappingIndentation);
				}

				if (keyTag != null) {
					keyOrScalarNode = applyTag(keyTag, keyOrScalarNode);
				}
				if (keyAnchor != null) {
					assignAnchor(keyOrScalarNode, keyAnchor);
				}

				applyAndClearPendingLeadingState(keyOrScalarNode);

				skipBlanks();

				if (peekCharMatch(':')
						&& mappingIndentation < getCurrentColumn()
						&& (peekNextCharMatch(1, ' ') || peekNextCharMatch(1, '\t') || peekNextCharMatch(1, '\n'))) {
					readChar();

					pendingAnchor = readUpToNextContent(keyOrScalarNode);

					updatePath(YamlToken.YamlMapping_PropertyKey, keyOrScalarNode);

					if (getCurrentColumn() - 1 > mappingIndentation
							|| (peekCharMatch('-') && peekNextCharMatchAny(1, " \t\n"))) {
						valueNode = parseYamlNode(mappingIndentation, true);
					} else {
						// Empty mapping property value
						valueNode = new YamlScalar("", YamlScalarType.NULL_VALUE);
					}
					if (pendingAnchor != null) {
						assignAnchor(valueNode, pendingAnchor);
						pendingAnchor = null;
					}

					mapping.add(keyOrScalarNode, valueNode);
				} else {
					throw new YamlParseException("Invalid YAML data when expecting mapping key found", getCurrentLine(), getCurrentColumn());
				}
			}

			pendingAnchor = readUpToNextContent(null);

			updatePath(YamlToken.YamlMapping_End, null);

			if (pendingAnchor != null) {
				assignAnchor(mapping, pendingAnchor);
				pendingAnchor = null;
			}

			return mapping;
		} else {
			String pendingAnchor = readUpToNextContent(null);
			if (pendingAnchor != null) {
				assignAnchor(keyOrScalarNode, pendingAnchor);
				pendingAnchor = null;
			}

			updatePath(YamlToken.YamlScalar, null);
			return keyOrScalarNode;
		}
	}

	private YamlNode parseFlowMapping() throws Exception {
		enterNesting();
		flowLevel++;
		try {
			return parseFlowMappingInternal();
		} finally {
			flowLevel--;
			nestingDepth--;
		}
	}

	private YamlNode parseFlowMappingInternal() throws Exception {
		if (peekCharNotMatch('{')) {
			throw new YamlParseException("Expected flow YAML mapping start character not found", getCurrentLine(), getCurrentColumn());
		}

		readChar();

		final YamlMapping mapping = new YamlMapping(true);
		updatePath(YamlToken.YamlMapping_Start, null);

		// An anchor directly after '{' belongs to the first key, an anchor of the mapping itself precedes the '{'
		String pendingAnchor = readUpToNextContent(mapping);

		applyAndClearPendingLeadingState(mapping);

		if (peekCharMatch('}')) {
			readChar();
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

			final YamlNode keyNode = parseFlowNode();
			if (pendingAnchor != null) {
				assignAnchor(keyNode, pendingAnchor);
				pendingAnchor = null;
			}

			applyAndClearPendingLeadingState(keyNode);

			pendingAnchor = readUpToNextContent(null);
			if (pendingAnchor != null) {
				assignAnchor(keyNode, pendingAnchor);
				pendingAnchor = null;
			}

			skipBlanks();

			if (peekCharMatch('}')) {
				readChar();

				mapping.add(keyNode, new YamlScalar("null", YamlScalarType.NULL_VALUE));

				pendingAnchor = readUpToNextContent(mapping);
				if (pendingAnchor != null) {
					assignAnchor(keyNode, pendingAnchor);
					pendingAnchor = null;
				}

				updatePath(YamlToken.YamlMapping_End, null);
				return mapping;
			} else if (peekCharMatch(',')) {
				readChar();

				// An anchor after ',' belongs to the next key
				pendingAnchor = readUpToNextContent(keyNode);

				mapping.add(keyNode, new YamlScalar(null));
			} else if (peekCharMatch(':') && peekNextCharMatchAny(1, ", \t\n")) {
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

					updatePath(YamlToken.YamlMapping_PropertyKey, keyNode);

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

					final YamlNode valueNode = parseFlowNode();

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

							updatePath(YamlToken.YamlMapping_End, null);
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

		updatePath(YamlToken.YamlMapping_End, null);
		return mapping;
	}

	private YamlNode parseFlowSequence() throws Exception {
		enterNesting();
		flowLevel++;
		try {
			return parseFlowSequenceInternal();
		} finally {
			flowLevel--;
			nestingDepth--;
		}
	}

	private YamlNode parseFlowSequenceInternal() throws Exception {
		if (peekCharNotMatch('[')) {
			throw new YamlParseException("Expected flow YAML sequence start character not found", getCurrentLine(), getCurrentColumn());
		}

		readChar();

		final YamlSequence sequence = new YamlSequence(true);
		updatePath(YamlToken.YamlSequence_Start, null);

		// An anchor directly after '[' belongs to the first item, an anchor of the sequence itself precedes the '['
		String pendingAnchor = readUpToNextContent(sequence);

		applyAndClearPendingLeadingState(sequence);

		if (peekCharMatch(']')) {
			readChar();
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
			if (peekCharMatch(']')) {
				readChar();

				readUpToNextContent(sequence);

				updatePath(YamlToken.YamlSequence_End, null);
				return sequence;
			} else {
				itemNode = parseFlowNode();
			}

			if (pendingAnchor != null) {
				assignAnchor(itemNode, pendingAnchor);
				pendingAnchor = null;
			}

			applyAndClearPendingLeadingState(itemNode);

			pendingAnchor = readUpToNextContent(null);
			if (pendingAnchor != null) {
				assignAnchor(itemNode, pendingAnchor);
				pendingAnchor = null;
			}

			skipBlanks();

			if (peekCharMatch(']')) {
				sequence.add(itemNode);
				readChar();

				pendingAnchor = readUpToNextContent(sequence);
				if (pendingAnchor != null) {
					assignAnchor(sequence, pendingAnchor);
					pendingAnchor = null;
				}

				updatePath(YamlToken.YamlSequence_End, null);
				return sequence;
			} else if (peekCharMatch(',')) {
				sequence.add(itemNode);
				readChar();

				// An anchor after ',' belongs to the next item
				pendingAnchor = readUpToNextContent(itemNode);
			} else if (peekCharMatch(':')) {
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

					pendingAnchor = readUpToNextContent(sequence);
					if (pendingAnchor != null) {
						assignAnchor(sequence, pendingAnchor);
						pendingAnchor = null;
					}

					updatePath(YamlToken.YamlSequence_End, null);
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

		updatePath(YamlToken.YamlSequence_End, null);
		return sequence;
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
		String scalarString = "";
		final int scalarStartIndentations = (int) getCurrentColumn() - 1;
		String nextScalarStringLine = "";
		String inlineComment = null;
		boolean continuationLineRead = false;
		while (isNotEOF()) {
			if (peekCharMatch(':') && (peekNextCharMatch(1, ' ') || peekNextCharMatch(1, '\t') || peekNextCharMatch(1, '\n'))) {
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
				} else if (nextLineIndentation > mappingIndentation || nextLineIndentation >= scalarStartIndentations) {
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
				scalar = parseMultilineScalar(type, comping, indentationIndicator, !removeCallerColumnIndentationBeforeMultiline);
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
	private YamlNode parseFlowNode() throws Exception {
		String tag = null;
		if (peekCharMatch('!')) {
			tag = readUpToNext(false, null, ' ', '\t', '\n', ',', ']', '}');
			skipBlanks();
		}

		final YamlNode node;
		if (peekCharMatch('\"')) {
			node = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING).withQuoteType(YamlStringQuoteType.DOUBLE);
		} else if (peekCharMatch('\'')) {
			node = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING).withQuoteType(YamlStringQuoteType.SINGLE);
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
		readChar();
		final String anchorName = readUpToNext(false, null, ' ', '\t', '\n', ',', '[', ']', '{', '}');
		if (anchorName.isEmpty()) {
			throw new YamlParseException("Missing alias name", getCurrentLine(), getCurrentColumn());
		}
		skipBlanks();
		return new YamlAlias(anchorName);
	}

	private YamlNode readFlowScalarString() throws Exception {
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
	 * @param parentColumnIsValidContentColumn
	 * 	true if {@code parentIndentationLevel} (derived from the shared indentations stack) is
	 * 	itself already a valid column for the block scalar's content to start at, as is the case
	 * 	for a multiline scalar used directly as a sequence item's value (e.g. "- |-"), where the
	 * 	sequence item's dash already established the column content is expected to align with.
	 * 	false if content must be indented strictly more than {@code parentIndentationLevel}, as is
	 * 	the case for a multiline scalar used as a mapping value (e.g. "key: |-"), where the column
	 * 	the scalar indicator happens to start on carries no meaning for the content's indentation.
	 */
	private YamlScalar parseMultilineScalar(
			final YamlMultilineScalarType multilineType,
			final YamlMultilineScalarChompingType chompingType,
			final int indentationIndicator,
			final boolean parentColumnIsValidContentColumn) throws Exception {
		// Indentation level of the block-structure context the multiline scalar starts in
		// (e.g. the mapping or sequence it is a value of). Content lines of the block scalar
		// must be indented at least as much as this (exactly this, for a sequence item's direct
		// value; strictly more, for a mapping value). Each content line's *own* indentation may
		// vary (it may decrease compared to a previous content line, as long as it does not
		// fall below the base indentation established by the first content line), which is
		// unrelated to the block-structure indentations stack used elsewhere for mappings and
		// sequences, so that stack must not be used to validate content line indentation here.
		final int parentIndentationLevel = getNumberOfIndentationChars();

		final StringBuilder raw = new StringBuilder();

		// Base indentation column of the block scalar's content, determined either by an
		// explicit indentation indicator or automatically from the first non-empty content line
		int contentIndentation = indentationIndicator > 0 ? parentIndentationLevel + indentationIndicator : -1;

		int pendingEmptyLines = 0;

		// How much of pendingEmptyLines has already been reported to the leading/post-comment
		// empty lines metadata (see addPendingEmptyLines below). This is tracked separately so
		// that pendingEmptyLines itself keeps accumulating uninterrupted for the raw scalar
		// content flushing further down, exactly as before; only the metadata bucketing needs
		// to happen at the precise chronological point relative to any interleaved comments.
		int emptyLinesAlreadyReportedAsMetadata = 0;

		// Indentation (in spaces/tabs) already consumed while looking ahead at the line that
		// turned out to not belong to the scalar anymore. This must be restored to the shared
		// indentations stack afterwards, since it was consumed from the input but never validated
		// nor accounted for there.
		int trailingNonScalarLineIndentation = -1;

		while (isNotEOF()) {
			final int lineIndentation = peekNextLineIndentation();

			if (lineIndentation == -1) {
				// Empty line (or EOF right after a linebreak): belongs to the scalar regardless
				// of indentation, collect it and continue looking ahead
				if (isEOF()) {
					break;
				}
				readChar();
				pendingEmptyLines++;
				continue;
			}

			if (contentIndentation < 0) {
				// First non-empty line establishes the base indentation
				final boolean lineIsTooShallow = parentColumnIsValidContentColumn
						? lineIndentation < parentIndentationLevel
						: lineIndentation <= parentIndentationLevel;
				if (lineIsTooShallow) {
					if (peekCharMatch('#')) {
						// A comment line is not subject to indentation rules at all, so it must
						// not be mistaken for a non-scalar line ending lookahead, nor be allowed
						// to influence the (not yet established) content indentation or the
						// shared indentations stack. Skip over it, but keep its text as a leading
						// comment for whichever node follows the multiline scalar, the same way
						// such comments are collected outside of multiline scalar parsing.
						// Any blank lines collected so far occurred *before* this comment in the
						// source and must be flushed into the correct bucket now, before the
						// comment itself is added to the pending comments below - otherwise they
						// would later be seen as occurring after a comment that is already
						// pending and be misattributed to the wrong bucket.
						addPendingEmptyLines(pendingEmptyLines);
						pendingEmptyLines = 0;
						pendingLeadingComments.add(readInlineComment());
						if (isEOF()) {
							break;
						} else {
							readChar();
						}
						continue;
					}
					// Empty lines collected so far were only ever look-ahead within the block
					// scalar's own region (between the indicator line and this non-scalar line)
					// and never belonged to actual content; with no content line establishing
					// the scalar at all, they are leading blank lines outside the scalar's value,
					// i.e. genuinely blank source lines preceding whatever node follows.
					addPendingEmptyLines(pendingEmptyLines);
					pendingEmptyLines = 0;
					trailingNonScalarLineIndentation = lineIndentation;
					break;
				}
				contentIndentation = lineIndentation;
			} else if (lineIndentation < contentIndentation) {
				if (peekCharMatch('#')) {
					// Same as above: a less-indented comment line does not belong to the block
					// scalar's content and must not be treated as the line ending the scalar;
					// keep its text as a leading comment for the node that follows. Report any
					// blank lines collected so far as metadata now, before the comment itself is
					// added to the pending comments below (see the field comment on
					// emptyLinesAlreadyReportedAsMetadata for why pendingEmptyLines itself must
					// not be reset here).
					addPendingEmptyLines(pendingEmptyLines - emptyLinesAlreadyReportedAsMetadata);
					emptyLinesAlreadyReportedAsMetadata = pendingEmptyLines;
					pendingLeadingComments.add(readInlineComment());
					if (isEOF()) {
						break;
					} else {
						readChar();
					}
					continue;
				}
				// Indented less than the block scalar's own content: this line belongs to the
				// surrounding block structure again, the scalar has ended. Blank lines collected
				// before this point are still trailing newlines of the scalar's value (per the
				// chomping rules applied below) and must be flushed, not discarded.
				for (int i = 0; i < pendingEmptyLines; i++) {
					raw.append("\n");
				}
				if (chompingType != YamlMultilineScalarChompingType.KEEP) {
					// Unless the chomping indicator explicitly keeps trailing blank lines as
					// part of the scalar's own value, these were genuinely blank source lines
					// preceding whatever node follows the block scalar (chomping will strip them
					// back out of the scalar's text below, in applyChomping). Only the portion not
					// already reported at an earlier interleaved comment is reported here.
					addPendingEmptyLines(pendingEmptyLines - emptyLinesAlreadyReportedAsMetadata);
				}
				pendingEmptyLines = 0;
				trailingNonScalarLineIndentation = lineIndentation;
				break;
			}

			for (int i = 0; i < pendingEmptyLines; i++) {
				raw.append("\n");
			}
			pendingEmptyLines = 0;

			final String restOfLine = readUpToNext(false, null, '\n');
			if (lineIndentation > contentIndentation) {
				raw.append(Utilities.repeat(" ", lineIndentation - contentIndentation));
			}
			raw.append(restOfLine);
			raw.append("\n");

			if (isEOF()) {
				break;
			} else {
				readChar();
			}
		}

		if (trailingNonScalarLineIndentation < 0) {
			// End of data: empty lines collected last are trailing linebreaks of the scalar (kept by "+" chomping)
			for (int i = 0; i < pendingEmptyLines; i++) {
				raw.append("\n");
			}
		}

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

	@SuppressWarnings("static-method")
	private String applyChomping(final String text, final YamlMultilineScalarChompingType chompingType) {
		switch (chompingType) {
			case KEEP:
				return text;
			case STRIP:
				return text.replaceAll("\\n+$", "");
			case CLIP:
			default:
				return text.replaceAll("\\n+$", "") + "\n";
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

	private void updatePath(final YamlToken jsonToken, final YamlNode key) throws Exception {
		if (jsonToken == null) {
			throw new Exception("Invalid YamlToken: null");
		}

		switch (jsonToken) {
			case YamlSequence_Start:
				if (currentPath.size() > 0 && currentPath.getLastPathPart() instanceof JsonPathArrayElement) {
					riseArrayIndex();
				}
				currentPath.add(new JsonPathArrayElement());
				break;
			case YamlSequence_End:
				if (currentPath.size() > 0 && currentPath.getLastPathPart() instanceof JsonPathArrayElement) {
					currentPath.removeLastElement();
				}
				if (currentPath.size() > 0 && currentPath.getLastPathPart() instanceof JsonPathPropertyElement) {
					currentPath.removeLastElement();
				}
				break;
			case YamlMapping_Start:
				if (currentPath.size() > 0 && currentPath.getLastPathPart() instanceof JsonPathArrayElement) {
					riseArrayIndex();
				}
				break;
			case YamlMapping_PropertyKey:
				if (key instanceof YamlScalar) {
					currentPath.add(new JsonPathPropertyElement(((YamlScalar) key).getValueString()));
				} else {
					currentPath.add(new JsonPathPropertyElement("<ComplexYamlKey>"));
				}
				break;
			case YamlScalar:
				if (currentPath.size() > 0) {
					if (currentPath.getLastPathPart() instanceof JsonPathArrayElement) {
						riseArrayIndex();
					} else if (currentPath.getLastPathPart() instanceof JsonPathPropertyElement) {
						currentPath.removeLastElement();
					}
				}
				break;
			case YamlMapping_End:
				if (currentPath.size() > 0 && currentPath.getLastPathPart() instanceof JsonPathPropertyElement) {
					currentPath.removeLastElement();
				}
				break;
			default:
				throw new Exception("Invalid jsonToken");
		}

		if (searchPath != null && getCurrentPath().equals(searchPath)) {
			throw new FoundPathEvent("Path '" + searchPath + "' was found");
		}
	}

	private void riseArrayIndex() {
		final JsonPathArrayElement currentJsonPathArrayElement = (JsonPathArrayElement) currentPath.getLastPathPart();
		currentPath.removeLastElement();
		currentPath.add(new JsonPathArrayElement(currentJsonPathArrayElement.getIndex() + 1));
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