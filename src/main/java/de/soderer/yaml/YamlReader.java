package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.soderer.json.utilities.BasicReadAheadReader;
import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.data.YamlToken;
import de.soderer.yaml.data.YamlTokenType;
import de.soderer.yaml.data.directive.YamlDirective;
import de.soderer.yaml.data.directive.YamlTagDirective;
import de.soderer.yaml.data.directive.YamlVersionDirective;

/**
 * TODOs:
 * - Read complex mapping keys
 */
public class YamlReader extends BasicReadAheadReader {
	private final List<YamlToken> yamlTokens = new ArrayList<>();
	private int yamlTokenIndex = 0;
	private final List<Integer> indentStack = new ArrayList<>();

	private final Map<String, YamlNode> anchorTable = new HashMap<>();

	private final List<String> pendingLeadingComments = new ArrayList<>();

	private Boolean yamlDocumentContentStarted = null;
	private List<YamlDocument> yamlDocumentList = null;

	private YamlNode latestYamlDataNode = null;

	public YamlReader(final InputStream inputStream) throws Exception {
		this(inputStream, null);
	}

	public YamlReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		super(inputStream, encodingCharset);

		setNormalizeLinebreaks(true);

		indentStack.add(0);
	}

	public YamlDocument readYamlDocument() throws Exception {
		yamlDocumentList = readYamlDocumentList();

		if (yamlDocumentList.size() == 1) {
			return yamlDocumentList.get(0);
		} else {
			throw new Exception("Data includes " + yamlDocumentList.size() + " YAML documents. Therefore use method 'readYamlDocumentList' to read all of them now. No need for reparsing data.");
		}
	}


	public List<YamlDocument> readYamlDocumentList() throws Exception {
		if (yamlDocumentList == null) {
			tokenize();

			yamlDocumentList = new ArrayList<>();

			while (!isAtDataEnd()) {
				final YamlDocument doc = new YamlDocument();

				consumeOptionalNewlines();

				while (match(YamlTokenType.DIRECTIVE)) {
					doc.addDirective(parseDirective(previousYamlToken().getValue()));
					consumeOptionalNewlines();
				}

				if (match(YamlTokenType.DOCUMENT_START)) {
					System.out.println();
				}

				while (match(YamlTokenType.COMMENT) || match(YamlTokenType.COMMENT_INLINE)) {
					doc.addLeadingComment(previousYamlToken().getValue());
					consumeOptionalNewlines();
				}

				consumeOptionalNewlines();

				final YamlNode root = parseNode(false);
				doc.setRoot(root);

				yamlDocumentList.add(doc);

				match(YamlTokenType.DOCUMENT_END);
			}
		}

		return yamlDocumentList;
	}

	private static YamlDirective<?> parseDirective(final String value) throws Exception {
		try {
			if (value.toUpperCase().startsWith("%YAML ")) {
				return new YamlVersionDirective(value.substring(6));
			} else if (value.toUpperCase().startsWith("%TAG ")) {
				return new YamlTagDirective(value.substring(5));
			} else {
				throw new Exception("Unknown yaml directive: " + value);
			}
		} catch (final Exception e) {
			throw new Exception("Invalid YAML directive data '" + value + "': " + e.getMessage());
		}
	}

	private void tokenize() throws Exception {
		boolean readNextIndentation = true;
		boolean multilineScalar = false;
		while (!isEOF()) {
			if (peekChar() == '\n') {
				readNextIndentation = true;
				readNewline();
				continue;
			}

			if (readNextIndentation) {
				readNextIndentation = false;
				readIndentation();
				if (yamlTokens.size() > 0 && yamlTokens.get(yamlTokens.size() - 1).getType() == YamlTokenType.DEDENT) {
					multilineScalar = false;
				}
				continue;
			}

			while (!isEOF() && isBlankOrTab(peekChar())) {
				// Skip blanks and tabs
				readChar();
			}

			switch (peekChar()) {
				case '%':
					checkOutsideDocumentContent();
					readDirective();
					continue;
				case '#':
					final YamlToken latestYamlToken = yamlTokens.size() > 0 ? yamlTokens.get(yamlTokens.size() - 1) : null;
					if (latestYamlToken == null
							|| latestYamlToken.getType() == YamlTokenType.INDENT
							|| latestYamlToken.getType() == YamlTokenType.DEDENT
							|| latestYamlToken.getType() == YamlTokenType.NEWLINE) {
						readComment();
					} else {
						readInlineComment();
					}
					continue;
				case '-':
					if (peekNextChar() == '-') {
						readChar();
						if (peekNextChar() == '-') {
							readChar();
							readChar();
							yamlTokens.add(new YamlToken(YamlTokenType.DOCUMENT_START, null, getCurrentLine(), getCurrentColumn() - 3));
							yamlDocumentContentStarted = true;
						} else {
							throw new Exception("Unexpected double hyphen '--' found at " + getCurrentLine() + ":" + (getCurrentColumn() - 1));
						}
					} else {
						yamlTokens.add(new YamlToken(YamlTokenType.DASH, String.valueOf(readChar()), getCurrentLine(), getCurrentColumn() - 1));
					}
					continue;
				case ':':
					checkInsideDocumentContent();
					yamlTokens.add(new YamlToken(YamlTokenType.COLON, String.valueOf(readChar()), getCurrentLine(), getCurrentColumn() - 1));
					continue;
				case '?':
					checkInsideDocumentContent();
					yamlTokens.add(new YamlToken(YamlTokenType.QUESTION, String.valueOf(readChar()), getCurrentLine(), getCurrentColumn() - 1));
					continue;
				case '{':
					checkInsideDocumentContent();
					yamlTokens.add(new YamlToken(YamlTokenType.FLOW_MAP_START, String.valueOf(readChar()), getCurrentLine(), getCurrentColumn() - 1));
					continue;
				case '}':
					checkInsideDocumentContent();
					yamlTokens.add(new YamlToken(YamlTokenType.FLOW_MAP_END, String.valueOf(readChar()), getCurrentLine(), getCurrentColumn() - 1));
					continue;
				case '[':
					checkInsideDocumentContent();
					yamlTokens.add(new YamlToken(YamlTokenType.FLOW_SEQ_START, String.valueOf(readChar()), getCurrentLine(), getCurrentColumn() - 1));
					continue;
				case ']':
					checkInsideDocumentContent();
					yamlTokens.add(new YamlToken(YamlTokenType.FLOW_SEQ_END, String.valueOf(readChar()), getCurrentLine(), getCurrentColumn() - 1));
					continue;
				case ',':
					checkInsideDocumentContent();
					yamlTokens.add(new YamlToken(YamlTokenType.COMMA, String.valueOf(readChar()), getCurrentLine(), getCurrentColumn() - 1));
					continue;
				case '.':
					if (peekNextChar() == '.') {
						readChar();
						if (peekNextChar() == '.') {
							readChar();
							readChar();
							yamlTokens.add(new YamlToken(YamlTokenType.DOCUMENT_END, null, getCurrentLine(), getCurrentColumn() - 3));
							yamlDocumentContentStarted = null;
						} else {
							throw new Exception("Unexpected double dot '..' found at " + getCurrentLine() + ":" + (getCurrentColumn() - 1));
						}
					} else {
						if (Character.isDigit(peekChar()) || (peekChar() == '-' && Character.isDigit(peekNextChar()))) {
							readNumber();
						} else if (multilineScalar) {
							readMultilineScalarString();
						} else {
							multilineScalar = readScalarString();
						}
					}
					continue;
				case '&':
					checkInsideDocumentContent();
					readAnchor();
					continue;
				case '*':
					checkInsideDocumentContent();
					readAlias();
					continue;
				case '"':
					checkInsideDocumentContent();
					readDoubleQuotedString();
					continue;
				case '\'':
					checkInsideDocumentContent();
					readSingleQuotedString();
					continue;
				default:
					checkInsideDocumentContent();
					if (Character.isDigit(peekChar()) || (peekChar() == '-' && Character.isDigit(peekNextChar()))) {
						readNumber();
					} else if (multilineScalar) {
						readMultilineScalarString();
					} else {
						multilineScalar = readScalarString();
					}
			}
		}

		while (indentStack.size() > 1) {
			indentStack.remove(indentStack.size() - 1);
			yamlTokens.add(new YamlToken(YamlTokenType.DEDENT, null, getCurrentLine(), getCurrentColumn()));
		}

		yamlTokens.add(new YamlToken(YamlTokenType.EOF, null, getCurrentLine(), getCurrentColumn()));
	}

	private void readDirective() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		final StringBuilder text = new StringBuilder();
		if (peekChar() != '%') {
			throw new Exception("Expected directive not found");
		}
		while (!isEOF()) {
			if (peekChar() == '\n') {
				break;
			} else {
				text.append(readChar());
			}
		}

		yamlTokens.add(new YamlToken(YamlTokenType.DIRECTIVE, text.toString().trim(), startLine, startColumn));
	}

	private void checkOutsideDocumentContent() throws Exception {
		if (yamlDocumentContentStarted != null && yamlDocumentContentStarted) {
			final long startLine = getCurrentLine();
			final long startColumn = getCurrentColumn();
			throw new Exception("Invalid yaml data starting YAML directives with '%' in line " + startLine +" at column " + startColumn);
		}
		yamlDocumentContentStarted = false;
	}

	private void checkInsideDocumentContent() throws Exception {
		if (yamlDocumentContentStarted != null && !yamlDocumentContentStarted) {
			final long startLine = getCurrentLine();
			final long startColumn = getCurrentColumn();
			throw new Exception("Invalid yaml content data starting before YAML directives end '---' in line " + startLine +" at column " + startColumn);
		}
		yamlDocumentContentStarted = true;
	}

	private void readNewline() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		if (!isEOF()) {
			readChar();
			yamlTokens.add(new YamlToken(YamlTokenType.NEWLINE, null, startLine, startColumn));
		}
	}

	private void readIndentation() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();
		int count = 0;

		while (!isEOF() && peekChar() == ' ') {
			readChar();
			count++;
		}

		if (peekChar() == '\n' || peekChar() == '#') {
			return;
		}

		int lastIndent = indentStack.get(indentStack.size() - 1);

		if (count > lastIndent) {
			indentStack.add(count);
			yamlTokens.add(new YamlToken(YamlTokenType.INDENT, null, startLine, startColumn));
		} else {
			while (count < lastIndent) {
				indentStack.remove(indentStack.size() - 1);
				lastIndent = indentStack.get(indentStack.size() - 1);
				yamlTokens.add(new YamlToken(YamlTokenType.DEDENT, null, startLine, startColumn));
			}
		}
	}

	private void readComment() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		final StringBuilder text = new StringBuilder();
		final Character nextChar = readChar();
		if (nextChar != '#') {
			throw new Exception("Expected comment start '#' not found");
		}
		while (!isEOF()) {
			if (peekChar() == '\n') {
				break;
			} else {
				text.append(readChar());
			}
		}
		yamlTokens.add(new YamlToken(YamlTokenType.COMMENT, text.toString().trim(), startLine, startColumn));
	}

	private void readInlineComment() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		final StringBuilder text = new StringBuilder();
		final Character nextChar = readChar();
		if (nextChar != '#') {
			throw new Exception("Expected inline comment start '#' not found");
		}
		while (!isEOF()) {
			if (peekChar() == '\n') {
				break;
			} else {
				text.append(readChar());
			}
		}
		yamlTokens.add(new YamlToken(YamlTokenType.COMMENT_INLINE, text.toString().trim(), startLine, startColumn));
	}

	private void readAnchor() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		readChar(); // &
		final String name = readIdentifier();
		yamlTokens.add(new YamlToken(YamlTokenType.ANCHOR, name, startLine, startColumn));
	}

	private void readAlias() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		readChar(); // *
		final String name = readIdentifier();
		yamlTokens.add(new YamlToken(YamlTokenType.ALIAS, name, startLine, startColumn));
	}

	private void readDoubleQuotedString() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		readChar(); // "
		final String quotedString = readUpToNext(false, '\\', '\"');
		readChar(); // closing "
		yamlTokens.add(new YamlToken(YamlTokenType.STRING, quotedString, startLine, startColumn));
	}

	private void readSingleQuotedString() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		readChar(); // '
		final String quotedString = readUpToNext(false, null, '\'');
		readChar(); // closing '
		yamlTokens.add(new YamlToken(YamlTokenType.STRING, quotedString, startLine, startColumn));
	}

	private void readNumber() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		final StringBuilder text = new StringBuilder();
		final Character nextChar = readChar();
		if (nextChar != '-' && !Character.isDigit(nextChar)) {
			throw new Exception("Expected number not found");
		} else {
			text.append(nextChar);
		}
		while (!isEOF()) {
			if (Character.isDigit(peekChar()) || peekChar() == '.' || peekChar() == '-') {
				text.append(readChar());
			} else {
				break;
			}
		}
		yamlTokens.add(new YamlToken(YamlTokenType.NUMBER, text.toString(), startLine, startColumn));
	}

	/**
	 * Return value 'true' indicates, that next lines are part of a multiline string scalar
	 * @return
	 * @throws Exception
	 */
	private boolean readScalarString() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		final StringBuilder text = new StringBuilder();
		while (!isEOF()) {
			if ("{}[],:#\n".indexOf(peekChar()) > -1) {
				break;
			} else {
				text.append(readChar());
			}
		}

		final String scalarString = text.toString().trim();

		if ("true".equalsIgnoreCase(scalarString)
				|| "yes".equalsIgnoreCase(scalarString)
				|| "on".equalsIgnoreCase(scalarString)
				|| "false".equalsIgnoreCase(scalarString)
				|| "no".equalsIgnoreCase(scalarString)
				|| "off".equalsIgnoreCase(scalarString)) {
			yamlTokens.add(new YamlToken(YamlTokenType.BOOLEAN, scalarString, startLine, startColumn));
			return false;
		} else if ("null".equalsIgnoreCase(scalarString)
				|| "~".equalsIgnoreCase(scalarString)) {
			yamlTokens.add(new YamlToken(YamlTokenType.NULL_VALUE, scalarString, startLine, startColumn));
			return false;
		} else {
			yamlTokens.add(new YamlToken(YamlTokenType.STRING, scalarString, startLine, startColumn));
			return scalarString.startsWith(">") || scalarString.startsWith("|");
		}
	}

	private void readMultilineScalarString() throws Exception {
		final long startLine = getCurrentLine();
		final long startColumn = getCurrentColumn();

		final StringBuilder text = new StringBuilder();
		while (!isEOF()) {
			if ("\n".indexOf(peekChar()) > -1) {
				break;
			} else {
				text.append(readChar());
			}
		}

		final String scalarString = text.toString().trim();

		yamlTokens.add(new YamlToken(YamlTokenType.STRING, scalarString, startLine, startColumn));
	}

	private String readIdentifier() throws Exception {
		final StringBuilder text = new StringBuilder();
		while (!isEOF()) {
			if ((Character.isLetterOrDigit(peekChar()) || peekChar() == '_' || peekChar() == '-')) {
				text.append(readChar());
			} else {
				break;
			}
		}

		return text.toString();
	}

	@SuppressWarnings("static-method")
	private boolean isBlankOrTab(final char characterToCheck) {
		return characterToCheck == ' ' || characterToCheck == '\t';
	}

	private YamlNode parseNode(final boolean inFlow) {
		consumeOptionalNewlinesAndComments();

		String anchorName = null;
		if (match(YamlTokenType.ANCHOR)) {
			anchorName = previousYamlToken().getValue();
			consumeOptionalNewlinesAndComments();
		}

		if (match(YamlTokenType.INDENT)) {
			final YamlNode valueNode = parseNode(false);
			valueNode.setAnchorName(anchorName);

			if (check(YamlTokenType.DEDENT)) {
				advance();
			}

			return valueNode;
		}

		if (check(YamlTokenType.FLOW_MAP_START)) {
			latestYamlDataNode = parseFlowMapping();
			return latestYamlDataNode;
		}
		if (check(YamlTokenType.FLOW_SEQ_START)) {
			latestYamlDataNode = parseFlowSequence();
			return latestYamlDataNode;
		}
		if (!inFlow && check(YamlTokenType.DASH)) {
			latestYamlDataNode = parseBlockSequence();
			return latestYamlDataNode;
		}
		if (!inFlow && isScalarStart()) {
			latestYamlDataNode = parseBlockPossiblyMappingOrScalar();
			return latestYamlDataNode;
		}

		latestYamlDataNode = parseScalarWithAnchorOrAlias();
		return latestYamlDataNode;
	}

	private boolean isScalarStart() {
		return check(YamlTokenType.STRING)
				|| check(YamlTokenType.NUMBER)
				|| check(YamlTokenType.BOOLEAN)
				|| check(YamlTokenType.NULL_VALUE)
				|| check(YamlTokenType.ANCHOR)
				|| check(YamlTokenType.ALIAS);
	}

	private YamlNode parseBlockPossiblyMappingOrScalar() {
		final int startIndex = yamlTokenIndex;
		final List<String> savedComments = new ArrayList<>(pendingLeadingComments);

		final YamlNode keyNode = parseScalarWithAnchorOrAlias();

		if (match(YamlTokenType.COLON)) {
			// Mapping
			final YamlMapping map = new YamlMapping(false);

			YamlNode valueNode;

			if (!check(YamlTokenType.NEWLINE) && !check(YamlTokenType.EOF) && !check(YamlTokenType.DEDENT)) {
				valueNode = parseNode(false);
			} else {
				consumeOptionalNewlinesAndComments();
				if (match(YamlTokenType.INDENT)) {
					valueNode = parseNode(false);
					if (check(YamlTokenType.DEDENT)) {
						advance();
					}
				} else {
					valueNode = new YamlScalar("null", YamlScalarType.NULL_VALUE);
				}
			}

			attachPendingComments(keyNode);
			map.addEntry(keyNode, valueNode);

			consumeOptionalNewlinesAndComments();

			while (isScalarStart()) {
				final int tempIndex = yamlTokenIndex;
				final YamlNode possibleKey = parseScalarWithAnchorOrAlias();

				if (!match(YamlTokenType.COLON)) {
					yamlTokenIndex = tempIndex;
					break;
				}

				YamlNode val;
				if (!check(YamlTokenType.NEWLINE) && !check(YamlTokenType.EOF) && !check(YamlTokenType.DEDENT)) {
					val = parseNode(false);
				} else {
					consumeOptionalNewlinesAndComments();
					if (match(YamlTokenType.INDENT)) {
						val = parseNode(false);
						if (check(YamlTokenType.DEDENT)) {
							advance();
						}
					} else {
						val = new YamlScalar("null", YamlScalarType.NULL_VALUE);
					}
				}

				attachPendingComments(possibleKey);
				map.addEntry(possibleKey, val);

				consumeOptionalNewlinesAndComments();
			}

			return map;
		}

		// Rollback map pre-readings and start reading a scalar
		yamlTokenIndex = startIndex;
		pendingLeadingComments.clear();
		pendingLeadingComments.addAll(savedComments);

		return parseScalarWithAnchorOrAlias();
	}

	private YamlSequence parseBlockSequence() {
		final YamlSequence seq = new YamlSequence(false);

		while (match(YamlTokenType.DASH)) {

			if (!check(YamlTokenType.NEWLINE) && !check(YamlTokenType.EOF)) {
				final YamlNode item = parseNode(false);
				attachPendingComments(item);
				seq.addItem(item);
			} else {
				consumeOptionalNewlinesAndComments();
				if (match(YamlTokenType.INDENT)) {
					final YamlNode item = parseNode(false);
					attachPendingComments(item);
					seq.addItem(item);

					if (check(YamlTokenType.DEDENT)) {
						advance();
					}
				} else {
					final YamlScalar empty = new YamlScalar("null", YamlScalarType.NULL_VALUE);
					attachPendingComments(empty);
					seq.addItem(empty);
				}
			}

			consumeOptionalNewlinesAndComments();

			if (!check(YamlTokenType.DASH)) break;
		}

		return seq;
	}

	private YamlMapping parseFlowMapping() {
		consume(YamlTokenType.FLOW_MAP_START, "{ expected");
		final YamlMapping map = new YamlMapping(true);

		consumeOptionalNewlinesAndComments();

		if (!check(YamlTokenType.FLOW_MAP_END)) {
			do {
				consumeOptionalNewlinesAndComments();
				final YamlNode key = parseNode(true);
				consumeOptionalNewlinesAndComments();
				consume(YamlTokenType.COLON, ": expected");
				consumeOptionalNewlinesAndComments();
				final YamlNode value = parseNode(true);

				attachPendingComments(key);
				map.addEntry(key, value);

				consumeOptionalNewlinesAndComments();
			} while (match(YamlTokenType.COMMA));
		}

		consumeNewLineAndIndentsAndDedents();
		consume(YamlTokenType.FLOW_MAP_END, "} expected");
		return map;
	}

	private YamlSequence parseFlowSequence() {
		consume(YamlTokenType.FLOW_SEQ_START, "[ expected");
		final YamlSequence seq = new YamlSequence(true);

		consumeOptionalNewlinesAndComments();

		if (!check(YamlTokenType.FLOW_SEQ_END)) {
			do {
				consumeOptionalNewlinesAndComments();
				final YamlNode item = parseNode(true);
				attachPendingComments(item);
				seq.addItem(item);
				consumeOptionalNewlinesAndComments();
			} while (match(YamlTokenType.COMMA));
		}

		consumeNewLineAndIndentsAndDedents();
		consume(YamlTokenType.FLOW_SEQ_END, "] expected");
		return seq;
	}

	private YamlNode parseScalarWithAnchorOrAlias() {
		consumeOptionalNewlinesAndComments();

		if (match(YamlTokenType.ALIAS)) {
			final YamlAlias alias = new YamlAlias(previousYamlToken().getValue());
			attachPendingComments(alias);
			return alias;
		}

		String anchorName = null;
		if (match(YamlTokenType.ANCHOR)) {
			anchorName = previousYamlToken().getValue();
			consumeOptionalNewlinesAndComments();
		}

		if (match(YamlTokenType.STRING)) {
			final String head = previousYamlToken().getValue();

			if (head.startsWith("|") || head.startsWith(">")) {
				final boolean folded = head.startsWith(">");
				final char chomping = extractChompingIndicator(head);

				final YamlScalar scalar = parseMultilineScalar(folded, chomping);
				if (anchorName != null) {
					scalar.setAnchorName(anchorName);
					anchorTable.put(anchorName, scalar);
				}
				match(YamlTokenType.DEDENT);
				return scalar;
			}

			final YamlScalar scalar = new YamlScalar(head, YamlScalarType.STRING);
			if (anchorName != null) {
				scalar.setAnchorName(anchorName);
				anchorTable.put(anchorName, scalar);
			}
			attachPendingComments(scalar);

			latestYamlDataNode = scalar;
			return scalar;
		}

		if (match(YamlTokenType.BOOLEAN)) {
			final YamlScalar scalar = new YamlScalar(previousYamlToken().getValue(), YamlScalarType.BOOLEAN);
			if (anchorName != null) {
				scalar.setAnchorName(anchorName);
				anchorTable.put(anchorName, scalar);
			}
			attachPendingComments(scalar);

			latestYamlDataNode = scalar;
			return scalar;
		}

		if (match(YamlTokenType.NUMBER)) {
			final YamlScalar scalar = new YamlScalar(previousYamlToken().getValue(), YamlScalarType.NUMBER);
			if (anchorName != null) {
				scalar.setAnchorName(anchorName);
				anchorTable.put(anchorName, scalar);
			}
			attachPendingComments(scalar);

			latestYamlDataNode = scalar;
			return scalar;
		}

		if (match(YamlTokenType.NULL_VALUE)) {
			final YamlScalar scalar = new YamlScalar(previousYamlToken().getValue(), YamlScalarType.NULL_VALUE);
			if (anchorName != null) {
				scalar.setAnchorName(anchorName);
				anchorTable.put(anchorName, scalar);
			}
			attachPendingComments(scalar);

			latestYamlDataNode = scalar;
			return scalar;
		}

		throw error(peekYamlToken(), "Unexpected token in scalar context: " + peekYamlToken().getType());
	}

	@SuppressWarnings("static-method")
	private char extractChompingIndicator(final String head) {
		if (head.contains("+")) {
			return '+';
		}
		if (head.contains("-")) {
			return '-';
		}
		return ' ';
	}

	private YamlScalar parseMultilineScalar(final boolean folded, final char chomping) {
		consumeOptionalNewlinesAndComments();

		final long indent = detectMultilineIndent();

		final StringBuilder raw = new StringBuilder();

		while (!isAtDocumentEnd()) {
			final YamlToken token = peekYamlToken();

			if (token.getType() == YamlTokenType.DEDENT || token.getType() == YamlTokenType.EOF) {
				break;
			}

			if (token.getType() == YamlTokenType.NEWLINE) {
				advance();
				continue;
			}

			if (token.getColumn() <= indent) {
				break;
			}

			raw.append(readMultilineLine(indent));
		}

		String text = raw.toString();
		text = applyChomping(text, chomping);
		if (folded) {
			text = foldLines(text);
		}

		final YamlScalarType type = folded ? YamlScalarType.MULTILINE_FOLDED : YamlScalarType.MULTILINE_LITERAL;
		final YamlScalar scalar = new YamlScalar(text, type);
		attachPendingComments(scalar);
		return scalar;
	}

	private long detectMultilineIndent() {
		int i = yamlTokenIndex;
		while (i < yamlTokens.size()) {
			final YamlToken token = yamlTokens.get(i);
			if (token.getType() == YamlTokenType.NEWLINE || token.getType() == YamlTokenType.COMMENT || token.getType() == YamlTokenType.COMMENT_INLINE) {
				i++;
				continue;
			}
			return token.getColumn() - 1;
		}
		return 0;
	}

	private String readMultilineLine(final long indent) {
		final StringBuilder sb = new StringBuilder();

		long remove = indent;
		while (remove > 0 && sb.length() < remove) {
			advance();
			remove--;
		}

		while (!isAtDocumentEnd() && peekYamlToken().getType() == YamlTokenType.INDENT) {
			advance();
		}

		while (!isAtDocumentEnd() && peekYamlToken().getType() != YamlTokenType.NEWLINE && peekYamlToken().getType() != YamlTokenType.INDENT) {
			sb.append(advance().getValue());
		}

		return sb.toString() + "\n";
	}

	@SuppressWarnings("static-method")
	private String applyChomping(final String text, final char chomping) {
		switch (chomping) {
			case '-':
				return text.replaceAll("\\n+$", "");
			case '+':
				return text;
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

	private void consumeOptionalNewlines() {
		while (match(YamlTokenType.NEWLINE)) {
			// Do nothing
		}
	}

	private void consumeOptionalNewlinesAndComments() {
		boolean progressed;
		do {
			progressed = false;
			while (match(YamlTokenType.NEWLINE)) {
				progressed = true;
			}
			while (check(YamlTokenType.COMMENT) || check(YamlTokenType.COMMENT_INLINE)) {
				if (match(YamlTokenType.COMMENT)) {
					pendingLeadingComments.add(previousYamlToken().getValue());
					progressed = true;
					match(YamlTokenType.NEWLINE);
				} else if (match(YamlTokenType.COMMENT_INLINE)) {
					if (latestYamlDataNode != null) {
						latestYamlDataNode.addInlineComment(previousYamlToken().getValue());
						progressed = true;
						match(YamlTokenType.NEWLINE);
						break;
					} else {
						pendingLeadingComments.add(previousYamlToken().getValue());
						progressed = true;
						match(YamlTokenType.NEWLINE);
					}
				}
			}
		} while (progressed);
	}

	private void consumeNewLineAndIndentsAndDedents() {
		boolean progressed;
		do {
			progressed = false;
			while (match(YamlTokenType.NEWLINE) || match(YamlTokenType.INDENT) || match(YamlTokenType.DEDENT)) {
				progressed = true;
			}
		} while (progressed);
	}

	private void consumeNewLineAndDedents() {
		boolean progressed;
		do {
			progressed = false;
			while (match(YamlTokenType.NEWLINE) || match(YamlTokenType.DEDENT)) {
				progressed = true;
			}
		} while (progressed);
	}

	private void attachPendingComments(final YamlNode node) {
		if (!pendingLeadingComments.isEmpty()) {
			for (final String c : pendingLeadingComments) {
				node.addLeadingComment(c);
			}
			pendingLeadingComments.clear();
		}
	}

	private boolean match(final YamlTokenType type) {
		if (check(type)) {
			advance();
			return true;
		}
		return false;
	}

	private YamlToken consume(final YamlTokenType type, final String message) {
		if (check(type)) {
			return advance();
		} else {
			throw error(peekYamlToken(), message + " (found: " + peekYamlToken().getType() + ")");
		}
	}

	private boolean check(final YamlTokenType type) {
		if (isAtDataEnd()) {
			return false;
		} else {
			return peekYamlToken().getType() == type;
		}
	}

	private YamlToken advance() {
		if (!isAtDataEnd()) {
			yamlTokenIndex++;
		}
		return previousYamlToken();
	}

	private boolean isAtDataEnd() {
		return peekYamlToken().getType() == YamlTokenType.EOF;
	}

	private boolean isAtDocumentEnd() {
		return peekYamlToken().getType() == YamlTokenType.DOCUMENT_END || peekYamlToken().getType() == YamlTokenType.EOF;
	}

	private YamlToken peekYamlToken() {
		return yamlTokens.get(yamlTokenIndex);
	}

	private YamlToken previousYamlToken() {
		return yamlTokens.get(yamlTokenIndex - 1);
	}

	@SuppressWarnings("static-method")
	private RuntimeException error(final YamlToken token, final String message) {
		return new RuntimeException("Parser error in line " + token.getLine() + ", column " + token.getColumn() + ": " + message);
	}

	public static YamlDocument readYamlDocument(final String yamlDocumentString) throws Exception {
		try (final YamlReader yamlReader = new YamlReader(new ByteArrayInputStream(yamlDocumentString.getBytes(StandardCharsets.UTF_8)))) {
			return yamlReader.readYamlDocument();
		}
	}
}
