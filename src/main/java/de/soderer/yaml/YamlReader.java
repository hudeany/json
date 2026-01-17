package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.data.YamlToken;
import de.soderer.yaml.data.directive.YamlDirective;
import de.soderer.yaml.data.directive.YamlTagDirective;
import de.soderer.yaml.data.directive.YamlVersionDirective;
import de.soderer.yaml.exception.FoundPathEvent;
import de.soderer.yaml.exception.YamlParseException;

/**
 * TODO:
 * Read datatype definitions like "!!str"
 * Read complex mapping keys for maps as keys (Fix test file yaml/reference_1_1/input.yaml)
 * Improve multiline scalars folded and literal
 * Check alias references after document read
 * Check cyclic dependencies in aliases
 *
 */
public class YamlReader extends BasicReadAheadReader {
	private final Stack<Integer> indentations = new Stack<>();
	private final Map<String, YamlNode> anchorTable = new HashMap<>();
	private List<String> pendingLeadingComments = new ArrayList<>();
	private Boolean documentContentStarted = null;
	protected JsonPath currentPath = new JsonPath();
	protected JsonPath searchPath = null;

	public YamlReader(final InputStream inputStream) throws Exception {
		this(inputStream, null);
	}

	public YamlReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		super(inputStream, encodingCharset);

		setNormalizeLinebreaks(true);

		indentations.add(0);
	}

	public YamlDocument readDocument() throws Exception {
		if (searchPath != null) {
			throw new Exception("Search path was already started before by method 'readUpToPath'");
		} else {
			return parseDocument();
		}
	}

	private YamlDocument parseDocument() throws Exception {
		try {
			final YamlDocument document = new YamlDocument();
			currentPath = new JsonPath();
			if (!pendingLeadingComments.isEmpty()) {
				for (final String commentLine : pendingLeadingComments) {
					document.addLeadingComment(commentLine);
				}
				pendingLeadingComments = new ArrayList<>();
			}

			while (isNotEOF()) {
				skipEmptyLinesAndReadNextIndentationAndLeadingComments();
				if (peekCharMatch('#')) {
					readLeadingComment();
				} else if (peekCharMatch('%')) {
					document.addDirective(readDirective());
					documentContentStarted = false;
				} else if (peekCharMatch('-')) {
					if (peekNextCharMatch('-')) {
						readChar();
						if (peekNextCharMatch('-')) {
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
								skipEmptyLinesAndReadNextIndentationAndLeadingComments();
								document.setRoot(parseYamlNode());
								return document;
							}
						} else if (documentContentStarted != null && !documentContentStarted) {
							throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn() - 1);
						} else {
							documentContentStarted = true;
							document.setRoot(parseBlockMappingOrScalar('-', null));
							return document;
						}
					} else if (peekNextCharMatch(' ') || peekNextCharMatch('\t') || peekNextCharMatch('\n')) {
						if (documentContentStarted != null && !documentContentStarted) {
							throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
						} else {
							document.setRoot(parseBlockSequence());
							return document;
						}
					} else if (documentContentStarted != null && !documentContentStarted) {
						throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
					} else {
						document.setRoot(parseBlockMappingOrScalar(null, null));
						return document;
					}
				} else if (peekCharMatch('.')) {
					if (peekNextCharMatch('.')) {
						readChar();
						if (peekNextCharMatch('.')) {
							readChar();
							readChar();
							skipEmptyLinesAndReadNextIndentationAndLeadingComments();
							documentContentStarted = null;
							// End of document
							break;
						} else if (documentContentStarted != null && !documentContentStarted) {
							throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
						} else {
							documentContentStarted = true;
							document.setRoot(parseBlockMappingOrScalar('.', null));
							return document;
						}
					} else if (documentContentStarted != null && !documentContentStarted) {
						throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
					} else {
						documentContentStarted = true;
						document.setRoot(parseBlockMappingOrScalar(null, null));
						return document;
					}
				} else if (documentContentStarted != null && !documentContentStarted) {
					throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
				} else {
					documentContentStarted = true;
					document.setRoot(parseYamlNode());
					return document;
				}
			}

			return null;
		} catch (final DuplicateKeyException e) {
			throw new YamlParseException("Duplicate key found: " + e.getMessage(), getCurrentLine(), getCurrentColumn());
		}
	}

	public void readUpToPath(final String yamlPathString) throws Exception {
		searchPath = new JsonPath(yamlPathString);

		try {
			parseDocument();
		} catch (@SuppressWarnings("unused") final FoundPathEvent e) {
			// Expected FoundPathEvent
		}

		if (!getCurrentPath().equals(searchPath)) {
			throw new Exception("Path '" + yamlPathString + "' is not part of the YAML data");
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

	public YamlNode readNextYamlNode() throws Exception {
		try {
			if (getCurrentPath().equals(searchPath)) {
				final YamlSequence sequence = (YamlSequence) parseYamlNode();
				return sequence.get(0);
			} else {
				return null;
			}
		} catch (@SuppressWarnings("unused") final FoundPathEvent e) {
			throw new Exception("Cannot read items of path");
		}
	}

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
		while (isNotEOF()) {
			if (peekCharMatch(' ') || peekCharMatch('\t')) {
				readChar();
				nextIndentationSize++;
			} else if (peekCharMatch('#')) {
				readLeadingComment();
				nextIndentationSize = getNumberOfIndentationChars();
			} else if (peekCharMatch('\n')) {
				readChar();
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
			indentations.add(nextIndentationSize);
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

	private Boolean readNextIndentation() throws Exception {
		int nextIndentationSize = 0;
		while (isNotEOF()) {
			if (peekCharMatch(' ') || peekCharMatch('\t')) {
				readChar();
				nextIndentationSize++;
			} else if (peekCharMatch('\n')) {
				return null;
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
			indentations.add(nextIndentationSize);
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

	private void readLeadingComment() throws Exception {
		if (peekCharNotMatch('#')) {
			throw new YamlParseException("Expected comment not found", getCurrentLine(), getCurrentColumn());
		}

		while (peekCharMatch('#')) {
			readChar();
			final String commentText = readUpToNext(false, null, '\n').trim();
			pendingLeadingComments.add(commentText);
			skipEmptyLinesAndReadNextIndentationAndLeadingComments();
		}
	}

	private String readInlineComment() throws Exception {
		if (peekCharNotMatch('#')) {
			throw new YamlParseException("Expected comment not found", getCurrentLine(), getCurrentColumn());
		}

		readChar();

		final String commentText = readUpToNext(false, null, '\n').trim();
		return commentText;
	}

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
				throw new YamlParseException("Unknown yaml directive: " + directiveText, startLine, startColumn);
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
		String datatype = null;
		if (peekCharMatch('!') && peekNextCharMatch('!')) {
			readChar();
			readChar();
			datatype = readUpToNext(false, null, ' ', '\t', '\n');
			skipBlanks();
			skipEmptyLinesAndReadNextIndentationAndLeadingComments();
		}

		if (peekCharMatch('-') && (peekNextCharMatch(' ') || peekNextCharMatch('\t') || peekNextCharMatch('\n'))) {
			final YamlNode sequence = parseBlockSequence();
			return sequence;
		} else if (peekCharMatch('&')) {
			parseAnchor();
			return parseYamlNode();
		} else if (peekCharMatch('*')) {
			List<String> interimPendingLeadingComments = pendingLeadingComments;
			pendingLeadingComments = new ArrayList<>();

			final YamlNode alias = parseAlias();

			if (!interimPendingLeadingComments.isEmpty()) {
				for (final String commentLine : interimPendingLeadingComments) {
					alias.addLeadingComment(commentLine);
				}
				interimPendingLeadingComments = new ArrayList<>();
			}

			return alias;
		} else if (peekCharMatch('{')) {
			final int flowStartIndentations = getNumberOfIndentationChars();
			List<String> interimPendingLeadingComments = pendingLeadingComments;
			pendingLeadingComments = new ArrayList<>();

			final YamlNode flowMapping = parseFlowMapping();

			if (!interimPendingLeadingComments.isEmpty()) {
				for (final String commentLine : interimPendingLeadingComments) {
					flowMapping.addLeadingComment(commentLine);
				}
				interimPendingLeadingComments = new ArrayList<>();
			}

			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			if (peekCharMatch(':') && flowStartIndentations + 1 < getCurrentColumn()) {
				return parseBlockMappingOrScalar(null, flowMapping);
			} else {
				return flowMapping;
			}
		} else if (peekCharMatch('[')) {
			final int flowStartIndentations = getNumberOfIndentationChars();
			List<String> interimPendingLeadingComments = pendingLeadingComments;
			pendingLeadingComments = new ArrayList<>();

			final YamlNode flowSequence = parseFlowSequence();

			if (!interimPendingLeadingComments.isEmpty()) {
				for (final String commentLine : interimPendingLeadingComments) {
					flowSequence.addLeadingComment(commentLine);
				}
				interimPendingLeadingComments = new ArrayList<>();
			}

			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			if (peekCharMatch(':') && flowStartIndentations + 1 < getCurrentColumn()) {
				return parseBlockMappingOrScalar(null, flowSequence);
			} else {
				return flowSequence;
			}
		} else {
			final YamlNode mappingOrScalar = parseBlockMappingOrScalar(null, null);
			if (datatype == null) {
				return mappingOrScalar;
			} else {
				return applyDatatypeToScalar(datatype, mappingOrScalar);
			}
		}
	}

	private static YamlNode applyDatatypeToScalar(final String datatype, final YamlNode yamlNode) throws Exception {
		if ("str".equals(datatype)) {
			if (yamlNode instanceof final YamlScalar scalar) {
				if (scalar.getType() != YamlScalarType.STRING) {
					return new YamlScalar(scalar.getValueString(), YamlScalarType.STRING).setInlineComment(scalar.getInlineComment());
				} else {
					return yamlNode;
				}
			} else {
				throw new Exception("Invalid data '" + yamlNode.getClass().getSimpleName() + "' for YAML datatype: '" + datatype + "'");
			}
		} else if ("float".equals(datatype)) {
			if (yamlNode instanceof final YamlScalar scalar) {
				if (scalar.getType() != YamlScalarType.NUMBER) {
					return new YamlScalar(scalar.getValueString(), YamlScalarType.NUMBER).setInlineComment(scalar.getInlineComment());
				} else {
					return yamlNode;
				}
			} else {
				throw new Exception("Invalid data '" + yamlNode.getClass().getSimpleName() + "' for YAML datatype: '" + datatype + "'");
			}
		} else if ("binary".equals(datatype)) {
			// TODO
			return yamlNode;
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

	private String parseAnchor() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		if (peekCharNotMatch('&')) {
			throw new YamlParseException("Expected anchor start not found", getCurrentLine(), getCurrentColumn());
		}

		readChar();

		final String anchorName = readUpToNext(false, null, '\n', ' ', '\t');
		if (anchorTable.containsKey(anchorName)) {
			throw new YamlParseException("Multiple definition of anchor name found: '" + anchorName + "'", startLine, startColumn);
		}

		skipBlanks();

		return anchorName;
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
		if (peekCharNotMatch('-') || (peekNextCharNotMatch(' ') && peekNextCharNotMatch('\t') && peekNextCharNotMatch('\n'))) {
			throw new YamlParseException("Expected sequence start not found", getCurrentLine(), getCurrentColumn());
		}

		final int sequenceIndentation = getNumberOfIndentationChars();

		final YamlSequence sequence = new YamlSequence();
		updateJsonPath(YamlToken.YamlSequence_Start, null);

		while (isNotEOF() && getNumberOfIndentationChars() == sequenceIndentation) {
			List<String> interimPendingLeadingComments = pendingLeadingComments;
			pendingLeadingComments = new ArrayList<>();

			if (peekCharMatch('-') && (peekNextCharMatch(' ') || peekNextCharMatch('\t'))) {
				readChar();
				readChar();
				skipBlanks();

				String pendingAnchor = null;
				if (peekCharMatch('&')) {
					pendingAnchor = parseAnchor();
				}

				String inlineComment = null;
				if (peekCharMatch('#')) {
					inlineComment = readInlineComment();
				}

				indentations.add(2);
				skipEmptyLinesAndReadNextIndentationAndLeadingComments();

				final YamlNode nextItemNode = parseYamlNode();
				nextItemNode.setInlineComment(inlineComment);
				if (pendingAnchor != null) {
					nextItemNode.setAnchorName(pendingAnchor);
					pendingAnchor = null;
				}

				if (!interimPendingLeadingComments.isEmpty()) {
					for (final String commentLine : interimPendingLeadingComments) {
						nextItemNode.addLeadingComment(commentLine);
					}
					interimPendingLeadingComments = new ArrayList<>();
				}

				sequence.add(nextItemNode);
			} else if (peekCharMatch('-') && (peekNextCharMatch('\n'))) {
				readChar();
				skipEmptyLinesAndReadNextIndentationAndLeadingComments();

				final YamlNode nextItemNode = parseYamlNode();

				if (!interimPendingLeadingComments.isEmpty()) {
					for (final String commentLine : interimPendingLeadingComments) {
						nextItemNode.addLeadingComment(commentLine);
					}
					interimPendingLeadingComments = new ArrayList<>();
				}

				sequence.add(nextItemNode);
			} else if (peekCharMatch('-') && (peekNextCharMatch('-'))) {
				readChar();
				if (peekNextCharMatch('-')) {
					readChar();
					readChar();
					documentContentStarted = true;
					return sequence;
				}
			} else if (peekCharMatch('.') && (peekNextCharMatch('.'))) {
				readChar();
				if (peekNextCharMatch('.')) {
					readChar();
					readChar();
					documentContentStarted = null;
					return sequence;
				}
			} else {
				return sequence;
			}
		}

		updateJsonPath(YamlToken.YamlSequence_End, null);
		return sequence;
	}

	private YamlNode parseBlockMappingOrScalar(final Character additionalLeadingChar, final YamlNode firstKeyNode) throws Exception {
		final int mappingIndentation = getNumberOfIndentationChars();

		YamlNode keyOrScalarNode;
		if (firstKeyNode != null) {
			keyOrScalarNode = firstKeyNode;
		} else {
			if (peekCharMatch('\"')) {
				keyOrScalarNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING);
				skipBlanks();
				if (peekCharMatch('#')) {
					keyOrScalarNode.setInlineComment(readInlineComment());
				}
			} else if (peekCharMatch('\'')) {
				keyOrScalarNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING);
				skipBlanks();
				if (peekCharMatch('#')) {
					keyOrScalarNode.setInlineComment(readInlineComment());
				}
			} else if (peekCharMatch('[')) {
				keyOrScalarNode = parseFlowSequence();
			} else if (peekCharMatch('{')) {
				keyOrScalarNode = parseFlowMapping();
			} else if (peekCharMatch('?') && (peekNextCharMatch(' ') || peekNextCharMatch('\t') || peekNextCharMatch('\n'))) {
				readChar();
				skipBlanks();
				keyOrScalarNode = parseYamlNode();
			} else {
				keyOrScalarNode = readUnquotedScalarString(additionalLeadingChar);
			}

			if (!pendingLeadingComments.isEmpty()) {
				for (final String commentLine1 : pendingLeadingComments) {
					keyOrScalarNode.addLeadingComment(commentLine1);
				}
				pendingLeadingComments = new ArrayList<>();
			}

			skipBlanks();
		}

		if (peekCharMatch(':') && (peekNextCharMatch(' ') || peekNextCharMatch('\t'))) {
			readChar();
			readChar();
			skipBlanks();

			indentations.add(1);

			String pendingAnchor = null;
			if (peekCharMatch('&')) {
				pendingAnchor = parseAnchor();
			}

			if (peekCharMatch('#')) {
				keyOrScalarNode.setInlineComment(readInlineComment());
			}

			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			if (peekCharMatch('&')) {
				if (pendingAnchor != null) {
					throw new YamlParseException("Assignment of multiple anchors single YAML node: '" + pendingAnchor + "'", getCurrentLine(), getCurrentColumn());
				}
				pendingAnchor = parseAnchor();
			}

			final YamlMapping mapping = new YamlMapping();
			updateJsonPath(YamlToken.YamlMapping_Start, null);
			updateJsonPath(YamlToken.YamlMapping_PropertyKey, keyOrScalarNode);

			YamlNode valueNode = parseYamlNode();
			if (pendingAnchor != null) {
				valueNode.setAnchorName(pendingAnchor);
				pendingAnchor = null;
			}

			mapping.add(keyOrScalarNode, valueNode);

			while (isNotEOF() && getNumberOfIndentationChars() == mappingIndentation) {
				if (peekCharMatch('.') && (peekNextCharMatch('.'))) {
					readChar();
					if (peekNextCharMatch('.')) {
						readChar();
						readChar();
						documentContentStarted = null;
						skipEmptyLinesAndReadNextIndentationAndLeadingComments();
						return mapping;
					}
				} else if (peekCharMatch('-') && (peekNextCharMatch('-'))) {
					readChar();
					if (peekNextCharMatch('-')) {
						readChar();
						readChar();
						documentContentStarted = true;
						skipEmptyLinesAndReadNextIndentationAndLeadingComments();
						return mapping;
					}
				} else if (peekCharMatch('\"')) {
					keyOrScalarNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING);
					skipBlanks();
					if (peekCharMatch('#')) {
						keyOrScalarNode.setInlineComment(readInlineComment());
					}
				} else if (peekCharMatch('\'')) {
					keyOrScalarNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING);
					skipBlanks();
					if (peekCharMatch('#')) {
						keyOrScalarNode.setInlineComment(readInlineComment());
					}
				} else if (peekCharMatch('?') && (peekNextCharMatch(' ') || peekNextCharMatch('\t') || peekNextCharMatch('\n'))) {
					readChar();
					skipBlanks();
					keyOrScalarNode = parseYamlNode();
				} else {
					keyOrScalarNode = readUnquotedScalarString(null);

					if ("...".equals(((YamlScalar) keyOrScalarNode).getValueString())) {
						documentContentStarted = null;
						return mapping;
					} else if ("---".equals(((YamlScalar) keyOrScalarNode).getValueString())) {
						if (keyOrScalarNode.getInlineComment() != null) {
							pendingLeadingComments.add(keyOrScalarNode.getInlineComment());
						}
						documentContentStarted = true;
						return mapping;
					}
				}

				if (!pendingLeadingComments.isEmpty()) {
					for (final String commentLine2 : pendingLeadingComments) {
						keyOrScalarNode.addLeadingComment(commentLine2);
					}
					pendingLeadingComments = new ArrayList<>();
				}

				skipBlanks();

				if (peekCharMatch(':') && (peekNextCharMatch(' ') || peekNextCharMatch('\t'))) {
					readChar();
					readChar();
					skipBlanks();

					indentations.add(1);

					if (peekCharMatch('&')) {
						pendingAnchor = parseAnchor();
					}

					if (peekCharMatch('#')) {
						keyOrScalarNode.setInlineComment(readInlineComment());
					}

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					updateJsonPath(YamlToken.YamlMapping_PropertyKey, keyOrScalarNode);

					valueNode = parseYamlNode();
					if (pendingAnchor != null) {
						valueNode.setAnchorName(pendingAnchor);
						pendingAnchor = null;
					}

					mapping.add(keyOrScalarNode, valueNode);
				} else if (peekCharMatch(':') && (peekNextCharMatch('\n'))) {
					readChar();
					skipBlanks();

					indentations.add(1);

					if (peekCharMatch('&')) {
						pendingAnchor = parseAnchor();
					}

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					updateJsonPath(YamlToken.YamlMapping_PropertyKey, keyOrScalarNode);

					valueNode = parseYamlNode();
					if (pendingAnchor != null) {
						valueNode.setAnchorName(pendingAnchor);
						pendingAnchor = null;
					}

					mapping.add(keyOrScalarNode, valueNode);
				} else {
					throw new YamlParseException("Invalid YAML data when expecting mapping key found", getCurrentLine(), getCurrentColumn());
				}
			}

			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			updateJsonPath(YamlToken.YamlMapping_End, null);
			return mapping;
		} else if (peekCharMatch(':') && peekNextCharMatch('\n')) {
			readChar();

			indentations.add(1);

			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			String pendingAnchor = null;
			if (peekCharMatch('&')) {
				pendingAnchor = parseAnchor();
			}

			final YamlMapping mapping = new YamlMapping();
			updateJsonPath(YamlToken.YamlMapping_Start, null);
			updateJsonPath(YamlToken.YamlMapping_PropertyKey, keyOrScalarNode);

			YamlNode valueNode = parseYamlNode();
			if (pendingAnchor != null) {
				valueNode.setAnchorName(pendingAnchor);
				pendingAnchor = null;
			}

			mapping.add(keyOrScalarNode, valueNode);

			while (isNotEOF() && getNumberOfIndentationChars() == mappingIndentation) {
				if (peekCharMatch('.') && (peekNextCharMatch('.'))) {
					readChar();
					if (peekNextCharMatch('.')) {
						readChar();
						readChar();
						documentContentStarted = null;
						skipEmptyLinesAndReadNextIndentationAndLeadingComments();
						return mapping;
					}
				} else if (peekCharMatch('-') && (peekNextCharMatch('-'))) {
					readChar();
					if (peekNextCharMatch('-')) {
						readChar();
						readChar();
						documentContentStarted = true;
						skipEmptyLinesAndReadNextIndentationAndLeadingComments();
						return mapping;
					}
				} else if (peekCharMatch('\"')) {
					keyOrScalarNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING);
					skipBlanks();
					if (peekCharMatch('#')) {
						keyOrScalarNode.setInlineComment(readInlineComment());
					}
				} else if (peekCharMatch('\'')) {
					keyOrScalarNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING);
					skipBlanks();
					if (peekCharMatch('#')) {
						keyOrScalarNode.setInlineComment(readInlineComment());
					}
				} else if (peekCharMatch('?') && (peekNextCharMatch(' ') || peekNextCharMatch('\t') || peekNextCharMatch('\n'))) {
					readChar();
					skipBlanks();
					keyOrScalarNode = parseYamlNode();
				} else {
					keyOrScalarNode = readUnquotedScalarString(null);

					if ("...".equals(((YamlScalar) keyOrScalarNode).getValueString())) {
						documentContentStarted = null;
						return mapping;
					} else if ("---".equals(((YamlScalar) keyOrScalarNode).getValueString())) {
						if (keyOrScalarNode.getInlineComment() != null) {
							pendingLeadingComments.add(keyOrScalarNode.getInlineComment());
						}
						documentContentStarted = true;
						return mapping;
					}
				}
				final YamlNode keyOrScalarNode2 = keyOrScalarNode;

				if (!pendingLeadingComments.isEmpty()) {
					for (final String commentLine2 : pendingLeadingComments) {
						keyOrScalarNode2.addLeadingComment(commentLine2);
					}
					pendingLeadingComments = new ArrayList<>();
				}

				skipBlanks();

				if (peekCharMatch(':') && (peekNextCharMatch(' ') || peekNextCharMatch('\t'))) {
					readChar();
					readChar();
					skipBlanks();

					indentations.add(1);

					if (peekCharMatch('&')) {
						pendingAnchor = parseAnchor();
					}

					if (peekCharMatch('#')) {
						keyOrScalarNode.setInlineComment(readInlineComment());
					}

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					updateJsonPath(YamlToken.YamlMapping_PropertyKey, keyOrScalarNode);

					valueNode = parseYamlNode();
					if (pendingAnchor != null) {
						valueNode.setAnchorName(pendingAnchor);
						pendingAnchor = null;
					}

					mapping.add(keyOrScalarNode, valueNode);
				} else if (peekCharMatch(':') && (peekNextCharMatch('\n'))) {
					readChar();

					indentations.add(1);

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					if (peekCharMatch('&')) {
						pendingAnchor = parseAnchor();
					}

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					updateJsonPath(YamlToken.YamlMapping_PropertyKey, keyOrScalarNode);

					valueNode = parseYamlNode();
					if (pendingAnchor != null) {
						valueNode.setAnchorName(pendingAnchor);
						pendingAnchor = null;
					}

					mapping.add(keyOrScalarNode, valueNode);
				} else {
					throw new YamlParseException("Invalid YAML data when expecting mapping key found", getCurrentLine(), getCurrentColumn());
				}
			}

			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			updateJsonPath(YamlToken.YamlMapping_End, null);
			return mapping;
		} else {
			String pendingAnchor = null;
			if (peekCharMatch('&')) {
				pendingAnchor = parseAnchor();
			}

			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			if (pendingAnchor != null) {
				keyOrScalarNode.setAnchorName(pendingAnchor);
				pendingAnchor = null;
			}

			updateJsonPath(YamlToken.YamlScalar, null);
			return keyOrScalarNode;
		}
	}

	private YamlNode parseFlowMapping() throws Exception {
		if (peekCharNotMatch('{')) {
			throw new YamlParseException("Expected flow YAML mapping start character not found", getCurrentLine(), getCurrentColumn());
		}

		readChar();

		skipBlanks();

		final YamlMapping mapping = new YamlMapping(true);
		updateJsonPath(YamlToken.YamlMapping_Start, null);

		if (!pendingLeadingComments.isEmpty()) {
			for (final String commentLine : pendingLeadingComments) {
				mapping.addLeadingComment(commentLine);
			}
			pendingLeadingComments = new ArrayList<>();
		}

		while (isNotEOF()) {
			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			YamlNode keyNode;
			if (peekCharMatch('\"')) {
				keyNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING);
			} else if (peekCharMatch('\'')) {
				keyNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING);
			} else {
				keyNode = readFlowScalarString();
			}

			if (!pendingLeadingComments.isEmpty()) {
				for (final String commentLine : pendingLeadingComments) {
					keyNode.addLeadingComment(commentLine);
				}
				pendingLeadingComments = new ArrayList<>();
			}

			skipBlanks();

			String pendingAnchor = null;
			if (peekCharMatch('&')) {
				pendingAnchor = parseAnchor();
			}

			if (peekCharMatch('#')) {
				keyNode.setInlineComment(readInlineComment());
			}

			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			if (peekCharMatch('&')) {
				pendingAnchor = parseAnchor();
			}

			if (pendingAnchor != null) {
				keyNode.setAnchorName(pendingAnchor);
				pendingAnchor = null;
			}

			skipBlanks();

			if (peekCharMatch('}')) {
				readChar();
				skipEmptyLinesAndReadNextIndentationAndLeadingComments();

				updateJsonPath(YamlToken.YamlMapping_End, null);
				return mapping;
			} else if (peekCharMatch(',')) {
				readChar();
				skipBlanks();

				if (peekCharMatch('&')) {
					pendingAnchor = parseAnchor();
				}

				if (peekCharMatch('#')) {
					keyNode.setInlineComment(readInlineComment());
				}

				if (pendingAnchor != null) {
					keyNode.setAnchorName(pendingAnchor);
					pendingAnchor = null;
				}

				mapping.add(keyNode, new YamlScalar(null));
			} else if (peekCharMatch(':') && peekNextCharMatchAny(", \t\n")) {
				indentations.add(1);

				if (peekNextCharMatch(',')) {
					readChar();
					readChar();
					skipBlanks();

					if (peekCharMatch('&')) {
						pendingAnchor = parseAnchor();
					}

					if (peekCharMatch('#')) {
						keyNode.setInlineComment(readInlineComment());
					}

					updateJsonPath(YamlToken.YamlMapping_PropertyKey, keyNode);

					final YamlScalar valueNode = new YamlScalar(null);

					if (pendingAnchor != null) {
						valueNode.setAnchorName(pendingAnchor);
						pendingAnchor = null;
					}

					mapping.add(keyNode, valueNode);
					continue;
				} else if (peekNextCharMatch('\n')) {
					readChar();
					skipEmptyLinesAndReadNextIndentationAndLeadingComments();
				} else {
					readChar();
					readChar();
					skipBlanks();
				}

				if (peekCharMatch('&')) {
					pendingAnchor = parseAnchor();
				}

				if (peekCharMatch('#')) {
					keyNode.setInlineComment(readInlineComment());
				}

				skipEmptyLinesAndReadNextIndentationAndLeadingComments();

				if (peekCharMatch('&')) {
					pendingAnchor = parseAnchor();
				}

				YamlNode valueNode;
				if (peekCharMatch('\"')) {
					valueNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING);
				} else if (peekCharMatch('\'')) {
					valueNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING);
				} else if (peekCharMatch('{')) {
					valueNode = parseFlowMapping();
				} else if (peekCharMatch('[')) {
					valueNode = parseFlowSequence();
				} else {
					valueNode = readFlowScalarString();
				}

				if (pendingAnchor != null) {
					valueNode.setAnchorName(pendingAnchor);
					pendingAnchor = null;
				}

				if (!pendingLeadingComments.isEmpty()) {
					for (final String commentLine : pendingLeadingComments) {
						valueNode.addLeadingComment(commentLine);
					}
					pendingLeadingComments = new ArrayList<>();
				}

				skipBlanks();

				if (peekCharMatch('&')) {
					pendingAnchor = parseAnchor();
				}

				if (peekCharMatch('#')) {
					valueNode.setInlineComment(readInlineComment());
				}

				skipEmptyLinesAndReadNextIndentationAndLeadingComments();

				if (peekCharMatch('&')) {
					pendingAnchor = parseAnchor();
				}

				if (pendingAnchor != null) {
					valueNode.setAnchorName(pendingAnchor);
					pendingAnchor = null;
				}

				boolean mapIsClosed = false;
				if (peekCharMatch('}') || peekCharMatch(',')) {
					mapIsClosed = peekCharMatch('}');
					readChar();
					skipBlanks();

					if (peekCharMatch('&')) {
						pendingAnchor = parseAnchor();
					}

					if (peekCharMatch('#')) {
						valueNode.setInlineComment(readInlineComment());
					}

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					if (peekCharMatch('&')) {
						pendingAnchor = parseAnchor();
					}

					mapping.add(keyNode, valueNode);

					if (mapIsClosed) {
						skipEmptyLinesAndReadNextIndentationAndLeadingComments();

						updateJsonPath(YamlToken.YamlMapping_End, null);
						return mapping;
					}
				} else {
					throw new YamlParseException("Invalid flow YAML mapping syntax found", getCurrentLine(), getCurrentColumn());
				}
			} else {
				throw new YamlParseException("Invalid flow YAML mapping syntax found", getCurrentLine(), getCurrentColumn());
			}
		}

		updateJsonPath(YamlToken.YamlMapping_End, null);
		return mapping;
	}

	private YamlNode parseFlowSequence() throws Exception {
		if (peekCharNotMatch('[')) {
			throw new YamlParseException("Expected flow YAML sequence start character not found", getCurrentLine(), getCurrentColumn());
		}

		readChar();

		skipBlanks();

		final YamlSequence sequence = new YamlSequence(true);
		updateJsonPath(YamlToken.YamlSequence_Start, null);

		if (!pendingLeadingComments.isEmpty()) {
			for (final String commentLine : pendingLeadingComments) {
				sequence.addLeadingComment(commentLine);
			}
			pendingLeadingComments = new ArrayList<>();
		}

		while (isNotEOF()) {
			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			YamlNode itemNode;
			if (peekCharMatch('\"')) {
				itemNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING);
			} else if (peekCharMatch('\'')) {
				itemNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING);
			} else if (peekCharMatch('{')) {
				itemNode = parseFlowMapping();
			} else if (peekCharMatch('[')) {
				itemNode = parseFlowSequence();
			} else if (peekCharMatch(']')) {
				readChar();
				skipEmptyLinesAndReadNextIndentationAndLeadingComments();
				updateJsonPath(YamlToken.YamlSequence_End, null);
				return sequence;
			} else {
				itemNode = readFlowScalarString();
			}

			if (!pendingLeadingComments.isEmpty()) {
				for (final String commentLine : pendingLeadingComments) {
					itemNode.addLeadingComment(commentLine);
				}
				pendingLeadingComments = new ArrayList<>();
			}

			skipBlanks();

			String pendingAnchor = null;
			if (peekCharMatch('&')) {
				pendingAnchor = parseAnchor();
			}

			if (peekCharMatch('#')) {
				itemNode.setInlineComment(readInlineComment());
			}

			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			if (peekCharMatch('&')) {
				pendingAnchor = parseAnchor();
			}

			if (pendingAnchor != null) {
				itemNode.setAnchorName(pendingAnchor);
				pendingAnchor = null;
			}

			skipBlanks();

			if (peekCharMatch(']')) {
				sequence.add(itemNode);
				readChar();

				skipBlanks();

				if (peekCharMatch('#')) {
					final String inlineComment = readInlineComment();
					sequence.setInlineComment(inlineComment);
				}

				skipEmptyLinesAndReadNextIndentationAndLeadingComments();
				return sequence;
			} else if (peekCharMatch(',')) {
				sequence.add(itemNode);
				readChar();
				skipBlanks();

				if (peekCharMatch('&')) {
					pendingAnchor = parseAnchor();
				}

				if (peekCharMatch('#')) {
					itemNode.setInlineComment(readInlineComment());
				}

				skipEmptyLinesAndReadNextIndentationAndLeadingComments();

				if (peekCharMatch('&')) {
					pendingAnchor = parseAnchor();
				}
			} else if (peekCharMatch(':')) {
				readChar();

				indentations.add(1);

				final YamlMapping mapping = new YamlMapping();

				YamlNode valueNode;
				if (peekCharMatch('\"')) {
					valueNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING);
				} else if (peekCharMatch('\'')) {
					valueNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING);
				} else if (peekCharMatch('{')) {
					valueNode = parseFlowMapping();
				} else if (peekCharMatch('[')) {
					valueNode = parseFlowSequence();
				} else {
					valueNode = readFlowScalarString();
				}

				if (!pendingLeadingComments.isEmpty()) {
					for (final String commentLine : pendingLeadingComments) {
						valueNode.addLeadingComment(commentLine);
					}
					pendingLeadingComments = new ArrayList<>();
				}

				skipBlanks();

				if (peekCharMatch('&')) {
					pendingAnchor = parseAnchor();
				}

				if (peekCharMatch('#')) {
					valueNode.setInlineComment(readInlineComment());
				}

				skipEmptyLinesAndReadNextIndentationAndLeadingComments();

				if (peekCharMatch('&')) {
					pendingAnchor = parseAnchor();
				}

				if (pendingAnchor != null) {
					valueNode.setAnchorName(pendingAnchor);
					pendingAnchor = null;
				}

				mapping.add(itemNode, valueNode);

				sequence.add(mapping);

				if (peekCharMatch(']')) {
					readChar();
					skipEmptyLinesAndReadNextIndentationAndLeadingComments();
					return sequence;
				} else if (peekCharMatch(',')) {
					readChar();
					skipBlanks();

					if (peekCharMatch('&')) {
						pendingAnchor = parseAnchor();
					}

					if (peekCharMatch('#')) {
						valueNode.setInlineComment(readInlineComment());
					}

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					if (peekCharMatch('&')) {
						pendingAnchor = parseAnchor();
					}
				}
			} else {
				throw new YamlParseException("Invalid flow YAML sequence syntax found", getCurrentLine(), getCurrentColumn());
			}
		}

		updateJsonPath(YamlToken.YamlSequence_End, null);
		return sequence;
	}

	private YamlNode readUnquotedScalarString(final Character additionalLeadingChar) throws Exception {
		String scalarString = "";
		final int scalarStartIndentations = getNumberOfIndentationChars();
		String nextScalarStringLine = "";
		String inlineComment = null;
		if (additionalLeadingChar != null) {
			nextScalarStringLine += additionalLeadingChar;
		}
		while (isNotEOF()) {
			if (peekCharMatch(':') && (peekNextCharMatch(' ') || peekNextCharMatch('\t') || peekNextCharMatch('\n'))) {
				// Mapping separator ': ' ends Scalar
				break;
			} else if ((peekCharMatch(' ') || peekCharMatch('\t') || peekCharMatch('\n')) && peekNextCharMatch('#')) {
				// Comment ' #' ends Scalar
				if (peekCharMatch(' ') || peekCharMatch('\t')) {
					readChar();
					inlineComment = readInlineComment();
				} else {
					readChar();
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
				readNextIndentation();
				if (peekCharNotMatch('\n') && getNumberOfIndentationChars() < scalarStartIndentations) {
					break;
				} else if (peekCharMatch('#')) {
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

		scalarString = scalarString.trim();

		if (scalarString.length() == 0) {
			throw new YamlParseException("Unexpected empty scalar String", getCurrentLine(), getCurrentColumn());
		} else if ("true".equalsIgnoreCase(scalarString)
				|| "y".equalsIgnoreCase(scalarString)
				|| "yes".equalsIgnoreCase(scalarString)
				|| "on".equalsIgnoreCase(scalarString)
				|| "false".equalsIgnoreCase(scalarString)
				|| "n".equalsIgnoreCase(scalarString)
				|| "no".equalsIgnoreCase(scalarString)
				|| "off".equalsIgnoreCase(scalarString)) {
			final YamlScalar scalar = new YamlScalar(scalarString, YamlScalarType.BOOLEAN);
			scalar.setInlineComment(inlineComment);
			return scalar;
		} else if ("null".equalsIgnoreCase(scalarString)
				|| "~".equalsIgnoreCase(scalarString)) {
			final YamlScalar scalar = new YamlScalar(scalarString, YamlScalarType.NULL_VALUE);
			scalar.setInlineComment(inlineComment);
			return scalar;
		} else if (scalarString.startsWith("-") || scalarString.startsWith(".") || Character.isDigit(scalarString.charAt(0))) {
			YamlScalar scalar;
			try {
				scalar = new YamlScalar(scalarString, YamlScalarType.NUMBER);
			} catch (@SuppressWarnings("unused") final NumberFormatException e) {
				scalar = new YamlScalar(scalarString, YamlScalarType.STRING);
			}
			scalar.setInlineComment(inlineComment);
			return scalar;
		} else if (scalarString.startsWith(">")) {
			final char chomping = scalarString.contains("+") ? '+' : scalarString.contains("-") ? '-' : ' ';
			final YamlScalar scalar = parseMultilineScalar(true, chomping);
			scalar.setInlineComment(inlineComment);
			return scalar;
		} else if (scalarString.startsWith("|")) {
			final char chomping = scalarString.contains("+") ? '+' : scalarString.contains("-") ? '-' : ' ';
			final YamlScalar scalar = parseMultilineScalar(false, chomping);
			scalar.setInlineComment(inlineComment);
			return scalar;
		} else {
			final YamlScalar scalar = new YamlScalar(scalarString, YamlScalarType.STRING);
			scalar.setInlineComment(inlineComment);
			return scalar;
		}
	}

	private YamlNode readFlowScalarString() throws Exception {
		String scalarString = "";
		while(true) {
			scalarString += readUpToNext(false, null, ",:#{}[]\n".toCharArray());

			// Allow sexagesimal numbers like "190:20:30.15" or text with ':' without following whitespace
			if (peekCharMatch(':') && (peekNextChar() == null || peekNextCharNotMatchAny(",{}[] \t\n"))) {
				readChar();
				scalarString += ":";
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

		YamlScalar scalar;
		if (scalarString.length() == 0) {
			throw new YamlParseException("Unexpected empty scalar String", getCurrentLine(), getCurrentColumn());
		} else if ("true".equalsIgnoreCase(scalarString)
				|| "yes".equalsIgnoreCase(scalarString)
				|| "on".equalsIgnoreCase(scalarString)
				|| "false".equalsIgnoreCase(scalarString)
				|| "no".equalsIgnoreCase(scalarString)
				|| "off".equalsIgnoreCase(scalarString)) {
			scalar = new YamlScalar(scalarString, YamlScalarType.BOOLEAN);
			scalar.setInlineComment(inlineComment);
		} else if ("null".equalsIgnoreCase(scalarString)
				|| "~".equalsIgnoreCase(scalarString)) {
			scalar = new YamlScalar(scalarString, YamlScalarType.NULL_VALUE);
			scalar.setInlineComment(inlineComment);
		} else if (scalarString.startsWith("-") || scalarString.startsWith(".") || Character.isDigit(scalarString.charAt(0))) {
			scalar = new YamlScalar(scalarString, YamlScalarType.NUMBER);
			scalar.setInlineComment(inlineComment);
		} else if (scalarString.startsWith(">")) {
			final char chomping = scalarString.contains("+") ? '+' : scalarString.contains("-") ? '-' : ' ';
			scalar = parseMultilineScalar(true, chomping);
			scalar.setInlineComment(inlineComment);
		} else if (scalarString.startsWith("|")) {
			final char chomping = scalarString.contains("+") ? '+' : scalarString.contains("-") ? '-' : ' ';
			scalar = parseMultilineScalar(false, chomping);
			scalar.setInlineComment(inlineComment);
		} else {
			scalar = new YamlScalar(scalarString, YamlScalarType.STRING);
			scalar.setInlineComment(inlineComment);
		}

		updateJsonPath(YamlToken.YamlScalar, null);
		return scalar;
	}

	private YamlScalar parseMultilineScalar(final boolean folded, final char chomping) throws Exception {
		skipEmptyLinesAndReadNextIndentationAndLeadingComments();

		final int multilineIndentationLevel = getNumberOfIndentationChars();

		final StringBuilder raw = new StringBuilder();

		while (isNotEOF() && multilineIndentationLevel <= getNumberOfIndentationChars()) {
			final String nextLine = readUpToNext(false, null, '\n');
			raw.append(nextLine);
			raw.append("\n");
			readChar();
			readNextIndentation();
		}

		if (peekCharMatch('#')) {
			readLeadingComment();
		}

		String text = raw.toString();
		text = applyChomping(text, chomping);
		if (folded) {
			text = foldLines(text);
		}

		final YamlScalar scalar = new YamlScalar(text, folded ? YamlScalarType.MULTILINE_FOLDED : YamlScalarType.MULTILINE_LITERAL);

		return scalar;
	}

	@SuppressWarnings("static-method")
	private String applyChomping(final String text, final char chomping) {
		switch (chomping) {
			case '+':
				return text;
			case '-':
				return text.replaceAll("\\n+$", "");
			default:
				return text.replaceAll("\\n+$", "") + "\n";
		}
	}

	@SuppressWarnings("static-method")
	private String foldLines(final String text) {
		final String[] lines = text.split("\\n", -1);
		final StringBuilder out = new StringBuilder();

		for (int i = 0; i < lines.length; i++) {
			final String line = lines[i];

			if (line.isEmpty()) {
				out.append("\n");
			} else {
				out.append(line);
				if (i < lines.length - 1) {
					out.append(" ");
				}
			}
		}

		return out.toString().trim() + "\n";
	}

	private int getNumberOfIndentationChars() {
		return indentations.stream().reduce(0, Integer::sum);
	}

	private void updateJsonPath(final YamlToken jsonToken, final YamlNode key) throws Exception {
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
}
