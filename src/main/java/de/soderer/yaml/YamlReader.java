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

import de.soderer.json.utilities.BasicReadAheadReader;
import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.data.directive.YamlDirective;
import de.soderer.yaml.data.directive.YamlTagDirective;
import de.soderer.yaml.data.directive.YamlVersionDirective;
import de.soderer.yaml.exception.YamlParseException;

/**
 * TODO:
 * Read datatype definitions like "!!str"
 * Read complex mapping keys (Fix test file yaml/reference_1_1/input.yaml)
 * Improve multiline scalars folded and literal
 * Enable SAX-like reading of path items with methods "readUpToPath" and "readNextYamlNode"
 * Check alias references after document read
 * Check cyclic dependencies in aliases
 */
public class YamlReader extends BasicReadAheadReader {
	private final Stack<Integer> indentations = new Stack<>();
	private final Map<String, YamlNode> anchorTable = new HashMap<>();
	private List<String> pendingLeadingComments = new ArrayList<>();
	private Boolean yamlDocumentContentStarted = null;

	public YamlReader(final InputStream inputStream) throws Exception {
		this(inputStream, null);
	}

	public YamlReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		super(inputStream, encodingCharset);

		setNormalizeLinebreaks(true);

		indentations.add(0);
	}

	public YamlDocument readDocument() throws Exception {
		final YamlDocument document = new YamlDocument();
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
				yamlDocumentContentStarted = false;
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

						if (yamlDocumentContentStarted != null && yamlDocumentContentStarted) {
							// Start of next document
							break;
						} else if (yamlDocumentContentStarted != null && !yamlDocumentContentStarted) {
							yamlDocumentContentStarted = true;
						} else {
							yamlDocumentContentStarted = true;
							skipEmptyLinesAndReadNextIndentationAndLeadingComments();
							document.setRoot(parseYamlNode());
							return document;
						}
					} else if (yamlDocumentContentStarted != null && !yamlDocumentContentStarted) {
						throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn() - 1);
					} else {
						yamlDocumentContentStarted = true;
						document.setRoot(parseBlockMappingOrScalar('-'));
						return document;
					}
				} else if (peekNextCharMatch(' ') || peekNextCharMatch('\t') || peekNextCharMatch('\n')) {
					if (yamlDocumentContentStarted != null && !yamlDocumentContentStarted) {
						throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
					} else {
						document.setRoot(parseBlockSequence());
						return document;
					}
				} else if (yamlDocumentContentStarted != null && !yamlDocumentContentStarted) {
					throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
				} else {
					document.setRoot(parseBlockMappingOrScalar(null));
					return document;
				}
			} else if (peekCharMatch('.')) {
				if (peekNextCharMatch('.')) {
					readChar();
					if (peekNextCharMatch('.')) {
						readChar();
						readChar();
						skipEmptyLinesAndReadNextIndentationAndLeadingComments();
						yamlDocumentContentStarted = null;
						// End of document
						break;
					} else if (yamlDocumentContentStarted != null && !yamlDocumentContentStarted) {
						throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
					} else {
						yamlDocumentContentStarted = true;
						document.setRoot(parseBlockMappingOrScalar('.'));
						return document;
					}
				} else if (yamlDocumentContentStarted != null && !yamlDocumentContentStarted) {
					throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
				} else {
					yamlDocumentContentStarted = true;
					document.setRoot(parseBlockMappingOrScalar(null));
					return document;
				}
			} else if (yamlDocumentContentStarted != null && !yamlDocumentContentStarted) {
				throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
			} else {
				yamlDocumentContentStarted = true;
				document.setRoot(parseYamlNode());
				return document;
			}
		}

		return null;
	}

	public YamlNode readUpToPath(final String yamlPath) {
		// TODO
		return null;
	}

	public YamlNode readNextYamlNode() throws Exception {
		return parseYamlNode();
	}

	public static YamlDocument readYamlDocument(final String yamlDocumentString) throws Exception {
		try (final YamlReader yamlReader = new YamlReader(new ByteArrayInputStream(yamlDocumentString.getBytes(StandardCharsets.UTF_8)))) {
			return yamlReader.readDocument();
		}
	}

	private void skipBlanks() throws Exception {
		while (peekCharMatch(' ') || peekCharMatch('\t')) {
			readChar();
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
			List<String> interimPendingLeadingComments = pendingLeadingComments;
			pendingLeadingComments = new ArrayList<>();

			final YamlNode flowMapping = parseFlowMapping();

			if (!interimPendingLeadingComments.isEmpty()) {
				for (final String commentLine : interimPendingLeadingComments) {
					flowMapping.addLeadingComment(commentLine);
				}
				interimPendingLeadingComments = new ArrayList<>();
			}

			return flowMapping;
		} else if (peekCharMatch('[')) {
			List<String> interimPendingLeadingComments = pendingLeadingComments;
			pendingLeadingComments = new ArrayList<>();

			final YamlNode flowSequence = parseFlowSequence();

			if (!interimPendingLeadingComments.isEmpty()) {
				for (final String commentLine : interimPendingLeadingComments) {
					flowSequence.addLeadingComment(commentLine);
				}
				interimPendingLeadingComments = new ArrayList<>();
			}

			return flowSequence;
		} else {
			final YamlNode mappingOrScalar = parseBlockMappingOrScalar(null);
			return mappingOrScalar;
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
					yamlDocumentContentStarted = true;
					return sequence;
				}
			} else if (peekCharMatch('.') && (peekNextCharMatch('.'))) {
				readChar();
				if (peekNextCharMatch('.')) {
					readChar();
					readChar();
					yamlDocumentContentStarted = null;
					return sequence;
				}
			} else {
				throw new YamlParseException("Expected sequence start not found", getCurrentLine(), getCurrentColumn());
			}
		}

		return sequence;
	}

	private YamlNode parseBlockMappingOrScalar(final Character additionalLeadingChar) throws Exception {
		final int mappingIndentation = getNumberOfIndentationChars();

		YamlNode keyOrScalarNode;
		if (peekCharMatch('\"')) {
			keyOrScalarNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING);
		} else if (peekCharMatch('\'')) {
			keyOrScalarNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING);
		} else {
			keyOrScalarNode = readScalarString(additionalLeadingChar);
		}

		if (!pendingLeadingComments.isEmpty()) {
			for (final String commentLine1 : pendingLeadingComments) {
				keyOrScalarNode.addLeadingComment(commentLine1);
			}
			pendingLeadingComments = new ArrayList<>();
		}

		skipBlanks();

		if (peekCharMatch(':') && (peekNextCharMatch(' ') || peekNextCharMatch('\t'))) {
			readChar();
			readChar();
			skipBlanks();

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

			YamlNode valueNode = parseYamlNode();
			if (pendingAnchor != null) {
				valueNode.setAnchorName(pendingAnchor);
				pendingAnchor = null;
			}

			mapping.add(keyOrScalarNode, valueNode);

			while (isNotEOF() && getNumberOfIndentationChars() == mappingIndentation) {
				if (peekCharMatch('\"')) {
					keyOrScalarNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING);
				} else if (peekCharMatch('\'')) {
					keyOrScalarNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING);
				} else {
					keyOrScalarNode = readScalarString(null);

					if ("...".equals(((YamlScalar) keyOrScalarNode).getValueString())) {
						yamlDocumentContentStarted = null;
						return mapping;
					} else if ("---".equals(((YamlScalar) keyOrScalarNode).getValueString())) {
						if (keyOrScalarNode.getInlineComment() != null) {
							pendingLeadingComments.add(keyOrScalarNode.getInlineComment());
						}
						yamlDocumentContentStarted = true;
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

					if (peekCharMatch('&')) {
						pendingAnchor = parseAnchor();
					}

					if (peekCharMatch('#')) {
						keyOrScalarNode.setInlineComment(readInlineComment());
					}

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					valueNode = parseYamlNode();
					if (pendingAnchor != null) {
						valueNode.setAnchorName(pendingAnchor);
						pendingAnchor = null;
					}

					mapping.add(keyOrScalarNode, valueNode);
				} else if (peekCharMatch(':') && (peekNextCharMatch('\n'))) {
					readChar();
					skipBlanks();

					if (peekCharMatch('&')) {
						pendingAnchor = parseAnchor();
					}

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

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

			return mapping;
		} else if (peekCharMatch(':') && peekNextCharMatch('\n')) {
			readChar();
			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			String pendingAnchor = null;
			if (peekCharMatch('&')) {
				pendingAnchor = parseAnchor();
			}

			final YamlMapping mapping = new YamlMapping();

			YamlNode valueNode = parseYamlNode();
			if (pendingAnchor != null) {
				valueNode.setAnchorName(pendingAnchor);
				pendingAnchor = null;
			}

			mapping.add(keyOrScalarNode, valueNode);

			while (isNotEOF() && getNumberOfIndentationChars() == mappingIndentation) {
				if (peekCharMatch('\"')) {
					keyOrScalarNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING);
				} else if (peekCharMatch('\'')) {
					keyOrScalarNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING);
				} else {
					keyOrScalarNode = readScalarString(null);

					if ("...".equals(((YamlScalar) keyOrScalarNode).getValueString())) {
						yamlDocumentContentStarted = null;
						return mapping;
					} else if ("---".equals(((YamlScalar) keyOrScalarNode).getValueString())) {
						if (keyOrScalarNode.getInlineComment() != null) {
							pendingLeadingComments.add(keyOrScalarNode.getInlineComment());
						}
						yamlDocumentContentStarted = true;
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

					if (peekCharMatch('&')) {
						pendingAnchor = parseAnchor();
					}

					if (peekCharMatch('#')) {
						keyOrScalarNode.setInlineComment(readInlineComment());
					}

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					valueNode = parseYamlNode();
					if (pendingAnchor != null) {
						valueNode.setAnchorName(pendingAnchor);
						pendingAnchor = null;
					}

					mapping.add(keyOrScalarNode, valueNode);
				} else if (peekCharMatch(':') && (peekNextCharMatch('\n'))) {
					readChar();
					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					if (peekCharMatch('&')) {
						pendingAnchor = parseAnchor();
					}

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

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

			return mapping;
		} else {
			String pendingAnchor = null;
			if (peekCharMatch('&')) {
				pendingAnchor = parseAnchor();
			}

			if (peekCharMatch('#')) {
				keyOrScalarNode.setInlineComment(readInlineComment());
			}

			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			if (pendingAnchor != null) {
				keyOrScalarNode.setAnchorName(pendingAnchor);
				pendingAnchor = null;
			}

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
						return mapping;
					}
				} else {
					throw new YamlParseException("Invalid flow YAML mapping syntax found", getCurrentLine(), getCurrentColumn());
				}
			} else {
				throw new YamlParseException("Invalid flow YAML mapping syntax found", getCurrentLine(), getCurrentColumn());
			}
		}

		return mapping;
	}

	private YamlNode parseFlowSequence() throws Exception {
		if (peekCharNotMatch('[')) {
			throw new YamlParseException("Expected flow YAML sequence start character not found", getCurrentLine(), getCurrentColumn());
		}

		readChar();

		skipBlanks();

		final YamlSequence sequence = new YamlSequence(true);

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

		return sequence;
	}

	private YamlNode readScalarString(Character additionalLeadingChar) throws Exception {
		String scalarString = "";
		while(true) {
			scalarString += readUpToNext(false, null, ":#\n".toCharArray());
			if (additionalLeadingChar != null) {
				scalarString = additionalLeadingChar + scalarString;
				additionalLeadingChar = null;
			}

			// Allow sexagesimal numbers like "190:20:30.15" or text with ':' without following whitespace
			if (peekCharMatch(':') && (peekNextChar() == null || (peekNextCharNotMatch(' ') && peekNextCharNotMatch('\t') && peekNextCharNotMatch('\n')))) {
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

		if (scalarString.length() == 0) {
			throw new YamlParseException("Unexpected empty scalar String", getCurrentLine(), getCurrentColumn());
		} else if ("true".equalsIgnoreCase(scalarString)
				|| "yes".equalsIgnoreCase(scalarString)
				|| "on".equalsIgnoreCase(scalarString)
				|| "false".equalsIgnoreCase(scalarString)
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

		if (scalarString.length() == 0) {
			throw new YamlParseException("Unexpected empty scalar String", getCurrentLine(), getCurrentColumn());
		} else if ("true".equalsIgnoreCase(scalarString)
				|| "yes".equalsIgnoreCase(scalarString)
				|| "on".equalsIgnoreCase(scalarString)
				|| "false".equalsIgnoreCase(scalarString)
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
			final YamlScalar scalar = new YamlScalar(scalarString, YamlScalarType.NUMBER);
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
}
