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
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.data.directive.YamlDirective;
import de.soderer.yaml.data.directive.YamlTagDirective;
import de.soderer.yaml.data.directive.YamlVersionDirective;
import de.soderer.yaml.exception.NotImplementedException;
import de.soderer.yaml.exception.YamlParseException;

/**
 * TODO:
 * Read anchor
 * Read alias
 * Read complex mapping keys
 * Read inline comments
 */
public class YamlReader extends BasicReadAheadReader {
	private final Stack<Integer> indentations = new Stack<>();
	private final Map<String, YamlNode> anchorTable = new HashMap<>();
	private List<String> pendingLeadingComments = new ArrayList<>();
	private Boolean yamlDocumentContentStarted = null;
	private YamlNode latestYamlNode = null;

	public YamlReader(final InputStream inputStream) throws Exception {
		this(inputStream, null);
	}

	public YamlReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		super(inputStream, encodingCharset);

		setNormalizeLinebreaks(true);

		indentations.add(0);
	}

	public YamlDocument readYamlDocument() throws Exception {
		final YamlDocument nextDocument = new YamlDocument();

		while (!isEOF()) {
			skipEmptyLinesAndReadNextIndentationAndLeadingComments();
			if (peekCharMatch('#')) {
				readLeadingComment();
			} else if (peekCharMatch('%')) {
				nextDocument.addDirective(readDirective());
				yamlDocumentContentStarted = false;
			} else if (peekCharMatch('-')) {
				if (peekNextCharMatch('-')) {
					readChar();
					if (peekNextCharMatch('-')) {
						readChar();
						readChar();
						if (yamlDocumentContentStarted != null && yamlDocumentContentStarted) {
							// Start of next document
							break;
						} else if (yamlDocumentContentStarted != null && !yamlDocumentContentStarted) {
							throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn() - 3);
						} else {
							yamlDocumentContentStarted = true;
							skipEmptyLinesAndReadNextIndentationAndLeadingComments();
							nextDocument.setRoot(parseNextYamlNode());
							return nextDocument;
						}
					} else if (yamlDocumentContentStarted != null && !yamlDocumentContentStarted) {
						throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn() - 1);
					} else {
						yamlDocumentContentStarted = true;
						nextDocument.setRoot(parseYamlMappingOrScalar('-'));
						return nextDocument;
					}
				} else if (peekNextCharMatch(' ') || peekNextCharMatch('\t') || peekNextCharMatch('\n')) {
					if (yamlDocumentContentStarted != null && !yamlDocumentContentStarted) {
						throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
					} else {
						nextDocument.setRoot(parseYamlSequence());
						return nextDocument;
					}
				} else if (yamlDocumentContentStarted != null && !yamlDocumentContentStarted) {
					throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
				} else {
					nextDocument.setRoot(parseYamlMappingOrScalar(null));
					return nextDocument;
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
						nextDocument.setRoot(parseYamlMappingOrScalar('.'));
						return nextDocument;
					}
				} else if (yamlDocumentContentStarted != null && !yamlDocumentContentStarted) {
					throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
				} else {
					yamlDocumentContentStarted = true;
					nextDocument.setRoot(parseYamlMappingOrScalar(null));
					return nextDocument;
				}
			} else if (yamlDocumentContentStarted != null && !yamlDocumentContentStarted) {
				throw new YamlParseException("Unexpected content found within YAML document directives section", getCurrentLine(), getCurrentColumn());
			} else {
				yamlDocumentContentStarted = true;
				nextDocument.setRoot(parseNextYamlNode());
				return nextDocument;
			}
		}

		return null;
	}

	public YamlNode readUpToPath(final String yamlPath) {
		// TODO
		return null;
	}

	public YamlNode readNextYamlNode() throws Exception {
		return parseNextYamlNode();
	}

	public static YamlDocument readYamlDocument(final String yamlDocumentString) throws Exception {
		try (final YamlReader yamlReader = new YamlReader(new ByteArrayInputStream(yamlDocumentString.getBytes(StandardCharsets.UTF_8)))) {
			return yamlReader.readYamlDocument();
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
		while (!isEOF()) {
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
		while (!isEOF()) {
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
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		if (peekCharNotMatch('#')) {
			throw new YamlParseException("Expected comment not found", startLine, startColumn);
		}

		while (peekCharMatch('#')) {
			readChar();
			final String commentText = readUpToNext(false, null, '\n').trim();
			pendingLeadingComments.add(commentText);
			skipEmptyLinesAndReadNextIndentationAndLeadingComments();
		}
	}

	private String readInlineComment() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		if (peekCharNotMatch('#')) {
			throw new YamlParseException("Expected comment not found", startLine, startColumn);
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

	private YamlNode parseNextYamlNode() throws Exception {
		if (peekCharMatch('-') && (peekNextCharMatch(' ') || peekNextCharMatch('\t') || peekNextCharMatch('\n'))) {
			return parseYamlSequence();
		} else if (peekNextCharMatch('&')) {
			//parseYamlAnchor();
			// TODO
			throw new NotImplementedException();
		} else if (peekNextCharMatch('*')) {
			//parseYamlAlias();
			// TODO
			throw new NotImplementedException();
		} else {
			return parseYamlMappingOrScalar(null);
		}
	}

	private YamlNode parseYamlSequence() throws Exception {
		if (peekCharNotMatch('-') || (peekNextCharNotMatch(' ') && peekNextCharNotMatch('\t') && peekNextCharNotMatch('\n'))) {
			throw new YamlParseException("Expected sequence start not found", getCurrentLine(), getCurrentColumn());
		}

		final int sequenceIndentation = getNumberOfIndentationChars();

		final YamlSequence sequence = new YamlSequence();

		while (!isEOF() && getNumberOfIndentationChars() == sequenceIndentation) {
			List<String> interimPendingLeadingComments = pendingLeadingComments;
			pendingLeadingComments = new ArrayList<>();

			if (peekCharMatch('-') && (peekNextCharMatch(' ') || peekNextCharMatch('\t'))) {
				readChar();
				readChar();
				skipBlanks();

				if (peekCharMatch('&')) {
					// parseYamlAnchor();
					// TODO
					throw new NotImplementedException();
				}

				String inlineComment = null;
				if (peekCharMatch('#')) {
					inlineComment = readInlineComment();
				}

				indentations.add(2);
				skipEmptyLinesAndReadNextIndentationAndLeadingComments();

				final YamlNode nextItemNode = parseNextYamlNode();
				nextItemNode.setInlineComment(inlineComment);

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

				final YamlNode nextItemNode = parseNextYamlNode();

				if (!interimPendingLeadingComments.isEmpty()) {
					for (final String commentLine : interimPendingLeadingComments) {
						nextItemNode.addLeadingComment(commentLine);
					}
					interimPendingLeadingComments = new ArrayList<>();
				}

				sequence.add(nextItemNode);
			} else {
				throw new YamlParseException("Expected sequence start not found", getCurrentLine(), getCurrentColumn());
			}
		}

		latestYamlNode = sequence;
		return latestYamlNode;
	}

	private YamlNode parseYamlMappingOrScalar(final Character additionalLeadingChar) throws Exception {
		final int mappingIndentation = getNumberOfIndentationChars();

		YamlNode keyOrScalarNode;
		if (peekCharMatch('\"')) {
			keyOrScalarNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING);
		} else if (peekCharMatch('\'')) {
			keyOrScalarNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING);
		} else {
			keyOrScalarNode = readScalarString(additionalLeadingChar);
		}
		final YamlNode keyOrScalarNode1 = keyOrScalarNode;

		if (!pendingLeadingComments.isEmpty()) {
			for (final String commentLine1 : pendingLeadingComments) {
				keyOrScalarNode1.addLeadingComment(commentLine1);
			}
			pendingLeadingComments = new ArrayList<>();
		}

		skipBlanks();

		if (peekCharMatch(':') && (peekNextCharMatch(' ') || peekNextCharMatch('\t'))) {
			readChar();
			readChar();
			skipBlanks();

			if (peekCharMatch('&')) {
				// parseYamlAnchor();
				// TODO
				throw new NotImplementedException();
			}

			if (peekCharMatch('#')) {
				keyOrScalarNode.setInlineComment(readInlineComment());
			}

			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			if (peekCharMatch('&')) {
				// parseYamlAnchor();
				// TODO
				throw new NotImplementedException();
			}

			final YamlMapping mapping = new YamlMapping();

			List<String> interimPendingLeadingComments = pendingLeadingComments;
			pendingLeadingComments = new ArrayList<>();

			YamlNode valueNode = parseNextYamlNode();

			if (!interimPendingLeadingComments.isEmpty()) {
				for (final String commentLine : interimPendingLeadingComments) {
					valueNode.addLeadingComment(commentLine);
				}
				interimPendingLeadingComments = null;
			}

			mapping.add(keyOrScalarNode, valueNode);

			while (!isEOF() && getNumberOfIndentationChars() == mappingIndentation) {
				if (peekCharMatch('\"')) {
					keyOrScalarNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING);
				} else if (peekCharMatch('\'')) {
					keyOrScalarNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING);
				} else {
					keyOrScalarNode = readScalarString(null);
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
						// parseYamlAnchor();
						// TODO
						throw new NotImplementedException();
					}

					if (peekCharMatch('#')) {
						keyOrScalarNode.setInlineComment(readInlineComment());
					}

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					interimPendingLeadingComments = pendingLeadingComments;
					pendingLeadingComments = new ArrayList<>();

					valueNode = parseNextYamlNode();

					if (!interimPendingLeadingComments.isEmpty()) {
						for (final String commentLine : interimPendingLeadingComments) {
							valueNode.addLeadingComment(commentLine);
						}
						interimPendingLeadingComments = null;
					}

					mapping.add(keyOrScalarNode, valueNode);
				} else if (peekCharMatch(':') && (peekNextCharMatch('\n'))) {
					readChar();
					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					if (peekCharMatch('&')) {
						// parseYamlAnchor();
						// TODO
						throw new NotImplementedException();
					}

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					interimPendingLeadingComments = pendingLeadingComments;
					pendingLeadingComments = new ArrayList<>();

					valueNode = parseNextYamlNode();

					if (!interimPendingLeadingComments.isEmpty()) {
						for (final String commentLine : interimPendingLeadingComments) {
							valueNode.addLeadingComment(commentLine);
						}
						interimPendingLeadingComments = null;
					}

					mapping.add(keyOrScalarNode, valueNode);
				} else {
					throw new YamlParseException("Invalid YAML data when expecting mapping key found", getCurrentLine(), getCurrentColumn());
				}
			}

			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			latestYamlNode = mapping;
			return latestYamlNode;
		} else if (peekCharMatch(':') && peekNextCharMatch('\n')) {
			readChar();
			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			if (peekCharMatch('&')) {
				// parseYamlAnchor();
				// TODO
				throw new NotImplementedException();
			}

			final YamlMapping mapping = new YamlMapping();

			List<String> interimPendingLeadingComments = pendingLeadingComments;
			pendingLeadingComments = new ArrayList<>();

			YamlNode valueNode = parseNextYamlNode();

			if (!interimPendingLeadingComments.isEmpty()) {
				for (final String commentLine : interimPendingLeadingComments) {
					valueNode.addLeadingComment(commentLine);
				}
				interimPendingLeadingComments = null;
			}

			mapping.add(keyOrScalarNode, valueNode);

			while (!isEOF() && getNumberOfIndentationChars() == mappingIndentation) {
				if (peekCharMatch('\"')) {
					keyOrScalarNode = new YamlScalar(readQuotedText('\\'), YamlScalarType.STRING);
				} else if (peekCharMatch('\'')) {
					keyOrScalarNode = new YamlScalar(readQuotedText('\''), YamlScalarType.STRING);
				} else {
					keyOrScalarNode = readScalarString(null);
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
						// parseYamlAnchor();
						// TODO
						throw new NotImplementedException();
					}

					if (peekCharMatch('#')) {
						keyOrScalarNode.setInlineComment(readInlineComment());
					}

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					interimPendingLeadingComments = pendingLeadingComments;
					pendingLeadingComments = new ArrayList<>();

					valueNode = parseNextYamlNode();

					if (!interimPendingLeadingComments.isEmpty()) {
						for (final String commentLine : interimPendingLeadingComments) {
							valueNode.addLeadingComment(commentLine);
						}
						interimPendingLeadingComments = null;
					}

					mapping.add(keyOrScalarNode, valueNode);
				} else if (peekCharMatch(':') && (peekNextCharMatch('\n'))) {
					readChar();
					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					if (peekCharMatch('&')) {
						// parseYamlAnchor();
						// TODO
						throw new NotImplementedException();
					}

					skipEmptyLinesAndReadNextIndentationAndLeadingComments();

					interimPendingLeadingComments = pendingLeadingComments;
					pendingLeadingComments = new ArrayList<>();

					valueNode = parseNextYamlNode();

					if (!interimPendingLeadingComments.isEmpty()) {
						for (final String commentLine : interimPendingLeadingComments) {
							valueNode.addLeadingComment(commentLine);
						}
						interimPendingLeadingComments = null;
					}

					mapping.add(keyOrScalarNode, valueNode);
				} else {
					throw new YamlParseException("Invalid YAML data when expecting mapping key found", getCurrentLine(), getCurrentColumn());
				}
			}

			skipEmptyLinesAndReadNextIndentationAndLeadingComments();

			latestYamlNode = mapping;
			return latestYamlNode;
		} else {
			skipEmptyLinesAndReadNextIndentationAndLeadingComments();
			latestYamlNode = keyOrScalarNode;
			return latestYamlNode;
		}
	}

	private YamlNode readScalarString(Character additionalLeadingChar) throws Exception {
		String scalarString = "";
		while(true) {
			scalarString += readUpToNext(false, null, ",:#{}[]\n".toCharArray());
			if (additionalLeadingChar != null) {
				scalarString = additionalLeadingChar + scalarString;
				additionalLeadingChar = null;
			}

			// Read sexagesimal numbers like "190:20:30.15"
			if (peekCharMatch(':') && peekNextChar() != null && Character.isDigit(peekNextChar())) {
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
			latestYamlNode = new YamlScalar(scalarString, YamlScalarType.BOOLEAN);
			latestYamlNode.setInlineComment(inlineComment);
			return latestYamlNode;
		} else if ("null".equalsIgnoreCase(scalarString)
				|| "~".equalsIgnoreCase(scalarString)) {
			latestYamlNode = new YamlScalar(scalarString, YamlScalarType.NULL_VALUE);
			latestYamlNode.setInlineComment(inlineComment);
			return latestYamlNode;
		} else if (scalarString.startsWith("-") || scalarString.startsWith(".") || Character.isDigit(scalarString.charAt(0))) {
			latestYamlNode = new YamlScalar(scalarString, YamlScalarType.NUMBER);
			latestYamlNode.setInlineComment(inlineComment);
			return latestYamlNode;
		} else if (scalarString.startsWith(">")) {
			final char chomping = scalarString.contains("+") ? '+' : scalarString.contains("-") ? '-' : ' ';
			latestYamlNode = parseMultilineScalar(true, chomping);
			latestYamlNode.setInlineComment(inlineComment);
			return latestYamlNode;
		} else if (scalarString.startsWith("|")) {
			final char chomping = scalarString.contains("+") ? '+' : scalarString.contains("-") ? '-' : ' ';
			latestYamlNode = parseMultilineScalar(false, chomping);
			latestYamlNode.setInlineComment(inlineComment);
			return latestYamlNode;
		} else {
			latestYamlNode = new YamlScalar(scalarString, YamlScalarType.STRING);
			latestYamlNode.setInlineComment(inlineComment);
			return latestYamlNode;
		}
	}

	private YamlScalar parseMultilineScalar(final boolean folded, final char chomping) throws Exception {
		skipEmptyLinesAndReadNextIndentationAndLeadingComments();

		final int multilineIndentationLevel = getNumberOfIndentationChars();

		final StringBuilder raw = new StringBuilder();

		while (!isEOF() && multilineIndentationLevel <= getNumberOfIndentationChars()) {
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
