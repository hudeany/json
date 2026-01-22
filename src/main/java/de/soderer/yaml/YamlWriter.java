package de.soderer.yaml;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import de.soderer.json.utilities.Linebreak;
import de.soderer.json.utilities.NumberUtilities;
import de.soderer.json.utilities.Utilities;
import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarQuoteType;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.data.directive.YamlDirective;

/**
 * TODOs:
 * - Write with ignore settings for flow style
 * - Write with ignore settings for comments
 * - Write with multiline String scalars as quoted text
 * - Write with resolving aliases (Check cyclic dependencies in aliases)
 */
public class YamlWriter implements Closeable {
	/** Default output encoding. */
	public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;
	private static Linebreak DEFAULT_LINEBREAK = Linebreak.Unix;

	/** Output stream. */
	private OutputStream outputStream;

	/** Output encoding. */
	private final Charset encoding;

	/** Output linebreak. */
	private Linebreak linebreak;
	private String linebreakString;

	private int indentSize = 2;
	private boolean alwaysQuoteStringKeys = false;
	private boolean alwaysQuoteStringValues = false;

	private BufferedWriter outputWriter = null;
	private boolean firstDocument = true;
	private boolean documentEndWasWritten = false;

	public YamlWriter(final OutputStream outputStream) {
		this(outputStream, null, null);
	}

	public YamlWriter(final OutputStream outputStream, final Charset encoding) {
		this(outputStream, encoding, null);
	}

	public YamlWriter(final OutputStream outputStream, final Linebreak linebreak) {
		this(outputStream, null, linebreak);
	}

	public YamlWriter(final OutputStream outputStream, final Charset encoding, final Linebreak linebreak) {
		this.outputStream = outputStream;
		this.encoding = encoding == null ? DEFAULT_ENCODING : encoding;
		this.linebreak = linebreak == null ? DEFAULT_LINEBREAK : linebreak;
		linebreakString = this.linebreak.toString();

		if (outputStream == null) {
			throw new IllegalStateException("YamlWriter is already closed");
		}
		outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, this.encoding));
	}

	public YamlWriter setIndentSize(final int indentSize) {
		this.indentSize = indentSize;
		return this;
	}

	public YamlWriter setLinebreakType(final Linebreak linebreak) {
		this.linebreak = linebreak == null ? DEFAULT_LINEBREAK : linebreak;
		linebreakString = this.linebreak.toString();
		return this;
	}

	public YamlWriter setAlwaysQuoteAllStrings() {
		alwaysQuoteStringKeys = true;
		alwaysQuoteStringValues = true;
		return this;
	}

	public boolean isAlwaysQuoteStringKeys() {
		return alwaysQuoteStringKeys;
	}

	public YamlWriter setAlwaysQuoteStringKeys(final boolean alwaysQuoteStringKeys) {
		this.alwaysQuoteStringKeys = alwaysQuoteStringKeys;
		return this;
	}

	public boolean isAlwaysQuoteStringValues() {
		return alwaysQuoteStringValues;
	}

	public YamlWriter setAlwaysQuoteStringValues(final boolean alwaysQuoteStringValues) {
		this.alwaysQuoteStringValues = alwaysQuoteStringValues;
		return this;
	}

	public Charset getEncoding() {
		return encoding;
	}

	public int getIndentSize() {
		return indentSize;
	}

	public YamlWriter writeDocument(final YamlDocument document) throws Exception {
		if (!firstDocument && !documentEndWasWritten) {
			write("..." + linebreakString);
		}

		if (document.getDirectives() != null) {
			for (final YamlDirective<?> directive : document.getDirectives()) {
				write(directive.toString() + linebreakString);
			}
			write("---" + linebreakString);
		} else if (!firstDocument) {
			write("---" + linebreakString);
		}

		if (document.getLeadingComments() != null) {
			for (final String leadingCommentLine : document.getLeadingComments()) {
				write("#");
				write(leadingCommentLine);
				write(linebreakString);
			}
		}

		if (document.getRoot() != null) {
			if (document.getRoot().getLeadingComments() != null) {
				for (final String leadingCommentLine : document.getRoot().getLeadingComments()) {
					write("#");
					write(leadingCommentLine);
					write(linebreakString);
				}
			}
			writeNode(document.getRoot(), 0, false, false);
		}

		if (document.getDirectives() != null || !firstDocument) {
			write("..." + linebreakString);
			documentEndWasWritten = true;
		} else {
			documentEndWasWritten = false;
		}

		outputWriter.flush();
		firstDocument = false;
		return this;
	}

	public YamlWriter writeDocumentList(final List<YamlDocument> documentList) throws Exception {
		for (int i = 0; i < documentList.size(); i++) {
			final YamlDocument document = documentList.get(i);
			if (!firstDocument) {
				write("..." + linebreakString);
			}
			writeDocument(document);
			firstDocument = false;
		}
		return this;
	}

	private YamlWriter writeNode(final YamlNode node, final int indentLevel, final boolean inFlow, final boolean isKeyContext) throws Exception {
		if (node instanceof final YamlScalar scalar) {
			writeScalar(scalar, indentLevel, inFlow, isKeyContext);
		} else if (node instanceof final YamlAlias alias) {
			if (alias.getLeadingComments() != null) {
				for (final String commentLine : alias.getLeadingComments()) {
					writeIndent(indentLevel);
					write("#" + commentLine + linebreakString);
				}
			}

			writeAlias(alias, indentLevel);
			if (!inFlow) {
				write(linebreakString);
			}
		} else if (node instanceof final YamlSequence sequence) {
			if (sequence.isFlowStyle() || inFlow) {
				writeFlowSequence(sequence, indentLevel);
				if (!inFlow) {
					if (sequence.getInlineComment() != null) {
						write(" #" + sequence.getInlineComment());
					}
					write(linebreakString);
				}
			} else {
				if (sequence.getLeadingComments() != null) {
					boolean firstCommentLine = true;
					for (final String commentLine : sequence.getLeadingComments()) {
						if (!firstCommentLine) {
							writeIndent(indentLevel);
						}
						write("#" + commentLine + linebreakString);
						writeIndent(indentLevel);
						firstCommentLine = false;
					}
				}

				writeBlockSequence(sequence, indentLevel);
			}
		} else if (node instanceof final YamlMapping mapping) {
			if (mapping.isFlowStyle() || inFlow) {
				writeFlowMapping(mapping, indentLevel);
				if (!inFlow) {
					if (mapping.getInlineComment() != null) {
						write(" #" + mapping.getInlineComment());
					}
					write(linebreakString);
				}
			} else {
				writeBlockMapping(mapping, indentLevel);
			}
		} else {
			throw new IllegalStateException("Unbekannter Node-Typ: " + node.getClass());
		}
		return this;
	}

	private YamlWriter writeIndent(final int indentLevel) throws IOException {
		if (indentLevel > 0) {
			write(" ".repeat(indentLevel * indentSize));
		}
		return this;
	}

	private YamlWriter writeScalar(final YamlScalar scalar, final int indentLevel, final boolean inFlow, final boolean isKeyContext) throws IOException {
		final YamlScalarType type = scalar.getType();
		final String value = scalar.getValueString();
		final String inlineComment = scalar.getInlineComment();

		switch (type) {
			case MULTILINE:
				writeMultilineStringScalar(scalar, indentLevel);
				break;
			case BOOLEAN:
			case NUMBER:
			case NULL_VALUE:
				writeIndent(indentLevel);
				write((value == null ? "null" : value.toString()) + (Utilities.isNotBlank(inlineComment) ? " #" + inlineComment : "") + linebreakString);
				break;
			case STRING:
				if (inFlow ) {
					writeIndent(indentLevel);
					write(escapePlainStringValueInFlow(value, scalar.getQuoteType()));
				} else if (isKeyContext) {
					writeIndent(indentLevel);
					write(escapePlainStringValue(value, scalar.getQuoteType()));
				} else {
					write(escapePlainStringValue(value, scalar.getQuoteType()) + (Utilities.isNotBlank(inlineComment) ? " #" + inlineComment : "") + linebreakString);
				}
				break;
			default:
				// Do nothing
		}
		return this;
	}

	private YamlWriter writeAlias(final YamlAlias alias, final int indentLevel) throws IOException {
		writeIndent(indentLevel);
		write(" *" + alias.getTargetAnchorName());
		if (alias.getInlineComment() == null) {
			write(linebreakString);
		} else {
			write(" #" + alias.getInlineComment() + linebreakString);
		}
		return this;
	}

	private YamlWriter writeMultilineStringScalar(final YamlScalar scalar, final int indentLevel) throws IOException {
		if (indentLevel > 0) {
			write(" ");
		}

		switch (scalar.getMultilineType()) {
			case FOLDED:
				write(">");
				break;
			case LITERAL:
				write("|");
				break;
			default:
				break;
		}
		switch (scalar.getMultilineChompingType()) {
			case KEEP:
				write("+");
				break;
			case STRIP:
				write("-");
				break;
			case CLIP:
			default:
				break;
		}
		if (scalar.getIndentationIndicator() > 0) {
			write(Integer.toString(scalar.getIndentationIndicator()));
		}
		write(linebreakString);

		String text;
		switch (scalar.getMultilineChompingType()) {
			case KEEP:
				text = scalar.getValueString();
				break;
			case STRIP:
				text = scalar.getValueString().replaceAll("\\n+$", "");
				break;
			case CLIP:
			default:
				text = scalar.getValueString().replaceAll("\\n+$", "") + "\n";
				break;
		}

		for (final String line : text.lines().collect(Collectors.toList())) {
			if (scalar.getIndentationIndicator() > 0) {
				writeIndent(indentLevel - 1);
				write(Utilities.repeat(" ", scalar.getIndentationIndicator()));
			} else {
				writeIndent(indentLevel);
			}
			write(line + linebreakString);
		}
		return this;
	}

	private String escapePlainStringKey(final String key, final YamlScalarQuoteType quoteType) {
		boolean needsQuotes = alwaysQuoteStringValues || key.isEmpty() || (quoteType != null && quoteType != YamlScalarQuoteType.NONE);

		if (!needsQuotes) {
			if (Character.isWhitespace(key.charAt(0))
					|| Character.isWhitespace(key.charAt(key.length() - 1))) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			for (int i = 0; i < key.length(); i++) {
				final char nextChar = key.charAt(i);

				if ((Character.isWhitespace(nextChar) && nextChar != ' ')
						|| "#*!'\"%@`".indexOf(nextChar) > -1) {
					needsQuotes = true;
					break;
				}
			}
		}

		if (!needsQuotes) {
			if (key.contains(": ") || key.contains(":\t") || key.contains(":\n")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if (key.contains(" &") || key.contains("\t&") || key.startsWith("&")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if (key.startsWith("[") || key.startsWith("]") || key.startsWith("{") || key.startsWith("}")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if (key.startsWith("|") || key.startsWith(">")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if ("true".equalsIgnoreCase(key)
					|| "y".equalsIgnoreCase(key)
					|| "yes".equalsIgnoreCase(key)
					|| "on".equalsIgnoreCase(key)
					|| "false".equalsIgnoreCase(key)
					|| "n".equalsIgnoreCase(key)
					|| "no".equalsIgnoreCase(key)
					|| "off".equalsIgnoreCase(key)
					|| "null".equalsIgnoreCase(key)
					|| "~".equalsIgnoreCase(key)) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if (NumberUtilities.isNumber(key)) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			return key;
		} else {
			if (quoteType == null || quoteType == YamlScalarQuoteType.DOUBLE) {
				return "\""
						+ escapeScalarString(key)
						+ "\"";
			} else {
				return "'"
						+ key.replace("'", "''")
						+ "'";
			}
		}
	}

	private String escapePlainStringValue(final String value, final YamlScalarQuoteType quoteType) {
		boolean needsQuotes = alwaysQuoteStringValues || value.isEmpty() || (quoteType != null && quoteType != YamlScalarQuoteType.NONE);

		if (!needsQuotes) {
			if (Character.isWhitespace(value.charAt(0))
					|| Character.isWhitespace(value.charAt(value.length() - 1))) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			for (int i = 0; i < value.length(); i++) {
				final char nextChar = value.charAt(i);

				if ((Character.isWhitespace(nextChar) && nextChar != ' ')
						|| "#*!'\"%@`".indexOf(nextChar) > -1) {
					needsQuotes = true;
					break;
				}
			}
		}

		if (!needsQuotes) {
			if (value.contains(": ") || value.contains(":\t") || value.contains(":\n")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if (value.contains(" &") || value.contains("\t&") || value.startsWith("&")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if (value.startsWith("[") || value.startsWith("]") || value.startsWith("{") || value.startsWith("}")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if (value.startsWith("|") || value.startsWith(">")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if ("true".equalsIgnoreCase(value)
					|| "yes".equalsIgnoreCase(value)
					|| "on".equalsIgnoreCase(value)
					|| "false".equalsIgnoreCase(value)
					|| "no".equalsIgnoreCase(value)
					|| "off".equalsIgnoreCase(value)
					|| "null".equalsIgnoreCase(value)
					|| "~".equalsIgnoreCase(value)) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if (NumberUtilities.isNumber(value)) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			return value;
		} else {
			if (quoteType == null || quoteType == YamlScalarQuoteType.DOUBLE) {
				return "\""
						+ escapeScalarString(value)
						+ "\"";
			} else {
				return "'"
						+ value.replace("'", "''")
						+ "'";
			}
		}
	}

	private String escapePlainStringValueInFlow(final String value, final YamlScalarQuoteType quoteType) {
		boolean needsQuotes = alwaysQuoteStringValues || value.isEmpty() || (quoteType != null && quoteType != YamlScalarQuoteType.NONE);

		if (!needsQuotes) {
			if (Character.isWhitespace(value.charAt(0))
					|| Character.isWhitespace(value.charAt(value.length() - 1))) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			for (int i = 0; i < value.length(); i++) {
				final char nextChar = value.charAt(i);

				if ((Character.isWhitespace(nextChar) && nextChar != ' ')
						|| "#*!'\"%@`".indexOf(nextChar) > -1) {
					needsQuotes = true;
					break;
				}
			}
		}

		if (!needsQuotes) {
			if (value.contains(": ") || value.contains(":\t") || value.contains(":\n")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if (value.contains(" &") || value.contains("\t&") || value.startsWith("&")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if (value.contains("[") || value.contains("]") || value.contains("{") || value.contains("}")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if (value.startsWith("|") || value.startsWith(">")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if ("true".equalsIgnoreCase(value)
					|| "yes".equalsIgnoreCase(value)
					|| "on".equalsIgnoreCase(value)
					|| "false".equalsIgnoreCase(value)
					|| "no".equalsIgnoreCase(value)
					|| "off".equalsIgnoreCase(value)
					|| "null".equalsIgnoreCase(value)
					|| "~".equalsIgnoreCase(value)) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if (NumberUtilities.isNumber(value)) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			return value;
		} else {
			if (quoteType == null || quoteType == YamlScalarQuoteType.DOUBLE) {
				return "\""
						+ escapeScalarString(value)
						+ "\"";
			} else {
				return "'"
						+ value.replace("'", "''")
						+ "'";
			}
		}
	}

	private static String escapeScalarString(final String value) {
		final StringBuilder escapedTextBuilder = new StringBuilder();

		for (final char nextChar : value.toCharArray()) {
			switch (nextChar) {
				case '\\':
					escapedTextBuilder.append("\\\\");
					break;
				case '\"':
					escapedTextBuilder.append("\\\"");
					break;
				case '\n':
					escapedTextBuilder.append("\\n");
					break;
				case '\r':
					escapedTextBuilder.append("\\r");
					break;
				case '\t':
					escapedTextBuilder.append("\\t");
					break;
				case '\b':
					escapedTextBuilder.append("\\b");
					break;
				case '\f':
					escapedTextBuilder.append("\\f");
					break;
				case '\u2B7F':
					escapedTextBuilder.append("\\u2B7F"); // VTAB
					break;
				case '\u00A0':
					escapedTextBuilder.append("\\u00A0"); // NBSP
					break;
				case '\u0085':
					escapedTextBuilder.append("\\u0085"); // NEL
					break;
				case '\u2028':
					escapedTextBuilder.append("\\u2028"); // LS
					break;
				case '\u2029':
					escapedTextBuilder.append("\\u2029"); // PS
					break;
				default:
					if (nextChar < 32) {
						escapedTextBuilder.append(String.format("\\u%04X", (int) nextChar));
					} else {
						escapedTextBuilder.append(nextChar);
					}
			}
		}

		return escapedTextBuilder.toString();
	}

	private YamlWriter writeBlockSequence(final YamlSequence yamlSequence, final int indentLevel) throws Exception {
		boolean isFirstData = true;
		for (final YamlNode item : yamlSequence.items()) {
			addSequenceItem(item, indentLevel, isFirstData);

			isFirstData = false;
		}
		return this;
	}

	private YamlWriter writeScalarInlineInSequence(final YamlScalar scalar) throws IOException {
		final String inlineComment = scalar.getInlineComment();
		switch (scalar.getType()) {
			case BOOLEAN:
			case NUMBER:
			case NULL_VALUE:
				write(scalar.getValueString() + (Utilities.isNotBlank(inlineComment) ? " #" + inlineComment : "") + linebreakString);
				break;
			case STRING:
				write(escapePlainStringValue(scalar.getValueString(), scalar.getQuoteType()) + (Utilities.isNotBlank(inlineComment) ? " #" + inlineComment : "") + linebreakString);
				break;
			case MULTILINE:
			default:
				write(escapePlainStringValue(scalar.getValueString(), scalar.getQuoteType()) + linebreakString);
		}
		return this;
	}

	private YamlWriter writeBlockMapping(final YamlMapping yamlMapping, final int indentLevel) throws Exception {
		boolean isFirstData = true;
		for (final Entry<YamlNode, YamlNode> entry : yamlMapping.entrySet()) {
			final YamlNode key = entry.getKey();
			final YamlNode value = entry.getValue();

			if (key.getLeadingComments() != null) {
				if (isFirstData) {
					if (key.getLeadingComments() != null) {
						boolean firstCommentLine = true;
						for (final String commentLine : key.getLeadingComments()) {
							if (!firstCommentLine) {
								writeIndent(indentLevel);
							}
							write("#" + commentLine + linebreakString);
							firstCommentLine = false;
						}
					}
					isFirstData = false;
				} else {
					if (key.getLeadingComments() != null) {
						for (final String commentLine : key.getLeadingComments()) {
							writeIndent(indentLevel);
							write("#" + commentLine + linebreakString);
						}
					}
				}
			}

			if (key instanceof final YamlScalar scalarKey) {
				if (scalarKey.getType() == YamlScalarType.STRING) {
					if (!isFirstData) {
						writeIndent(indentLevel);
						write(escapePlainStringKey(scalarKey.getValueString(), scalarKey.getQuoteType()));
						isFirstData = false;
					} else {
						write(escapePlainStringKey(scalarKey.getValueString(), scalarKey.getQuoteType()));
					}
				} else {
					if (!isFirstData) {
						writeIndent(indentLevel);
						write(scalarKey.getValueString());
						isFirstData = false;
					} else {
						write(scalarKey.getValueString());
					}
				}
			} else {
				if (!isFirstData) {
					writeIndent(indentLevel);
					isFirstData = false;
				}
				write("? ");
				writeNode(key, indentLevel + 1, false, false);
				writeIndent(indentLevel);
			}

			if (key.getAnchorName() != null) {
				write(" &" + key.getAnchorName());
				if (key.getInlineComment() == null) {
					write(" ");
				}
			}

			write(":");

			boolean startValueInNewLine = false;
			if (value.getAnchorName() != null) {
				write(" &" + value.getAnchorName());
			}
			if (key.getInlineComment() != null) {
				write(" #" + key.getInlineComment());
				startValueInNewLine = true;
			}

			if (value instanceof final YamlScalar scalar) {
				if (scalar.getType() == YamlScalarType.MULTILINE) {
					writeNode(scalar, indentLevel + 1, false, false);
				} else if (!startValueInNewLine
						&& scalar.getAnchorName() == null
						&& (scalar.getLeadingComments() == null || scalar.getLeadingComments().isEmpty())) {
					final String inlineComment = scalar.getInlineComment();
					switch (scalar.getType()) {
						case BOOLEAN:
							write(" ");
							write(scalar.getValueString() + (Utilities.isNotBlank(inlineComment) ? " #" + inlineComment : "") + linebreakString);
							break;
						case NUMBER:
							write(" ");
							write(scalar.getValueString() + (Utilities.isNotBlank(inlineComment) ? " #" + inlineComment : "") + linebreakString);
							break;
						case NULL_VALUE:
							if (!"".equals(scalar.getValueString()) ) {
								write(" ");
							}
							write(scalar.getValueString() + (Utilities.isNotBlank(inlineComment) ? " #" + inlineComment : "") + linebreakString);
							break;
						case STRING:
							write(" ");
							write(escapePlainStringValue(scalar.getValueString(), scalar.getQuoteType()) + (Utilities.isNotBlank(inlineComment) ? " #" + inlineComment : "") + linebreakString);
							break;
						case MULTILINE:
							write(" ");
							writeMultilineStringScalar(scalar, indentLevel);
							break;
						default:
							write(" ");
							write(escapePlainStringValue(scalar.getValueString(), scalar.getQuoteType()) + linebreakString);
					}
				} else {
					write(linebreakString);
					if (scalar.getLeadingComments() != null) {
						for (final String commentLine : scalar.getLeadingComments()) {
							writeIndent(indentLevel + 1);
							write("#" + commentLine + linebreakString);
						}
					}
					writeIndent(indentLevel + 1);
					writeNode(scalar, indentLevel + 1, false, false);
				}
			} else if (value instanceof final YamlAlias alias) {
				write(" *" + alias.getTargetAnchorName());
				if (alias.getInlineComment() == null) {
					write(linebreakString);
				} else {
					write(" #" + alias.getInlineComment() + linebreakString);
				}
			} else if (value instanceof final YamlMapping mapping) {
				if (!mapping.isFlowStyle()) {
					write(linebreakString);
					writeIndent(indentLevel + 1);
				} else {
					write(" ");
				}
				writeNode(mapping, indentLevel + 1, false, false);
			} else if (value instanceof final YamlSequence sequence) {
				if (!startValueInNewLine
						&& sequence.getAnchorName() == null
						&& (sequence.getLeadingComments() == null || sequence.getLeadingComments().isEmpty())) {
					if (!sequence.isFlowStyle()) {
						write(linebreakString);
						writeIndent(indentLevel + 1);
					} else {
						write(" ");
					}
					writeNode(sequence, indentLevel + 1, false, false);
				} else {
					if (!sequence.isFlowStyle()) {
						write(linebreakString);
						if (sequence.getLeadingComments() != null) {
							for (final String commentLine : sequence.getLeadingComments()) {
								writeIndent(indentLevel + 1);
								write("#" + commentLine + linebreakString);
							}
						}
						writeIndent(indentLevel + 1);
					} else {
						write(" ");
					}
					writeNode(sequence, indentLevel + 1, false, false);
				}
			} else {
				throw new Exception("Unknown YAML node type: '" + value.getClass().getSimpleName() + "'");
			}

			isFirstData = false;
		}
		return this;
	}

	private YamlWriter writeFlowMapping(final YamlMapping yamlMapping, final int indentLevel) throws Exception {
		write("{");

		boolean isSingleLineFlow = true;
		for (final Entry<YamlNode, YamlNode> entry : yamlMapping.entrySet()) {
			if (entry.getKey() instanceof YamlMapping || entry.getKey() instanceof YamlSequence
					|| entry.getValue() instanceof YamlMapping || entry.getValue() instanceof YamlSequence
					|| (entry.getKey().getLeadingComments() != null && !entry.getKey().getLeadingComments().isEmpty())
					|| (entry.getValue().getLeadingComments() != null && !entry.getValue().getLeadingComments().isEmpty())
					|| entry.getKey().getInlineComment() != null
					|| entry.getValue().getInlineComment() != null) {
				isSingleLineFlow = false;
				break;
			}
		}

		String pendingValueInlineComment = null;

		boolean first = true;
		for (final Entry<YamlNode, YamlNode> entry : yamlMapping.entrySet()) {
			if (!first) {
				if (isSingleLineFlow) {
					write(", ");
				} else {
					if (pendingValueInlineComment != null) {
						write(", #" + pendingValueInlineComment + linebreakString);
					} else {
						write("," + linebreakString);
					}
				}
			}

			final YamlNode key = entry.getKey();

			if (key.getLeadingComments() != null) {
				for (final String commentLine : key.getLeadingComments()) {
					writeIndent(indentLevel + 1);
					write("#" + commentLine + linebreakString);
				}
			}

			if (!first && !isSingleLineFlow) {
				writeIndent(indentLevel + 1);
			}

			if (key instanceof final YamlScalar scalarKey
					&& scalarKey.getType() == YamlScalarType.STRING
					&& scalarKey.getAnchorName() == null
					&& (key.getLeadingComments() == null || key.getLeadingComments().isEmpty())) {
				writeScalarInlineInFlow(scalarKey);
			} else {
				writeNode(key, 0, true, true);
			}

			write(":");

			if (key.getAnchorName() != null) {
				write(" &" + key.getAnchorName());
				if (key.getInlineComment() == null) {
					write(" ");
				}
			}
			if (key.getInlineComment() != null) {
				write(" #" + key.getInlineComment() + linebreakString);
			}

			final YamlNode value = entry.getValue();

			if (value.getLeadingComments() == null || value.getLeadingComments().isEmpty()) {
				write(" ");
			} else {
				write(linebreakString);
				writeIndent(indentLevel + 2);
			}

			if (value.getLeadingComments() != null) {
				for (final String commentLine : value.getLeadingComments()) {
					write(linebreakString);
					writeIndent(indentLevel + 2);
					write("#" + commentLine + linebreakString);
				}
			}

			if (value instanceof final YamlScalar scalarVal
					&& scalarVal.getType() != YamlScalarType.MULTILINE
					&& scalarVal.getAnchorName() == null
					&& (scalarVal.getLeadingComments() == null || scalarVal.getLeadingComments().isEmpty())) {
				writeScalarInlineInFlow(scalarVal);
			} else if (value instanceof final YamlAlias alias) {
				write(" *" + alias.getTargetAnchorName());
				if (alias.getInlineComment() == null) {
					write(linebreakString);
				} else {
					write(" #" + alias.getInlineComment() + linebreakString);
				}
			} else {
				writeNode(value, 0, true, false);
			}

			if (value.getInlineComment() != null) {
				pendingValueInlineComment = value.getInlineComment();
			}

			first = false;
		}

		if (!isSingleLineFlow) {
			write(linebreakString);
			writeIndent(indentLevel);
		}

		write("}");
		return this;
	}

	private YamlWriter writeFlowSequence(final YamlSequence yamlSequence, final int indentLevel) throws Exception {
		write("[");

		boolean isSingleLineFlow = true;
		for (final YamlNode item : yamlSequence.items()) {
			if (item instanceof YamlMapping || item instanceof YamlSequence
					|| (item.getLeadingComments() != null && !item.getLeadingComments().isEmpty())
					|| item.getInlineComment() != null) {
				isSingleLineFlow = false;
				break;
			}
		}

		String pendingItemInlineComment = null;

		boolean first = true;
		for (final YamlNode item : yamlSequence.items()) {
			if (!first) {
				if (isSingleLineFlow) {
					write(", ");
				} else {
					if (pendingItemInlineComment != null) {
						write(", #" + pendingItemInlineComment + linebreakString);
					} else {
						write("," + linebreakString);
					}
				}
			}

			if (item.getLeadingComments() != null) {
				for (final String commentLine : item.getLeadingComments()) {
					writeIndent(indentLevel + 1);
					write("#" + commentLine + linebreakString);
				}
			}

			if (!first && !isSingleLineFlow) {
				writeIndent(indentLevel + 1);
			}

			if (item instanceof final YamlScalar scalar
					&& scalar.getType() != YamlScalarType.MULTILINE
					&& scalar.getAnchorName() == null
					&& (scalar.getLeadingComments() == null || scalar.getLeadingComments().isEmpty())) {
				writeScalarInlineInFlow(scalar);
			} else if (item instanceof final YamlAlias alias) {
				write(" *" + alias.getTargetAnchorName());
				if (alias.getInlineComment() == null) {
					write(linebreakString);
				} else {
					write(" #" + alias.getInlineComment() + linebreakString);
				}
			} else {
				writeNode(item, indentLevel + 1, true, false);
			}

			if (item.getInlineComment() != null) {
				pendingItemInlineComment = item.getInlineComment();
			}

			first = false;
		}

		if (!isSingleLineFlow) {
			write(linebreakString);
			writeIndent(indentLevel + 1);
		}

		write("]");
		return this;
	}

	private YamlWriter writeScalarInlineInFlow(final YamlScalar scalar) throws IOException {
		final String inlineComment = scalar.getInlineComment();
		switch (scalar.getType()) {
			case BOOLEAN:
			case NUMBER:
			case NULL_VALUE:
				write(scalar.getValueString() + (Utilities.isNotBlank(inlineComment) ? " #" + inlineComment : ""));
				break;
			case STRING:
				write(escapePlainStringValueInFlow(scalar.getValueString(), scalar.getQuoteType()) + (Utilities.isNotBlank(inlineComment) ? " #" + inlineComment : ""));
				break;
			case MULTILINE:
			default:
				write(escapePlainStringValueInFlow(scalar.getValueString(), scalar.getQuoteType()));
		}
		return this;
	}

	private static boolean mustStartInNewLine(final YamlSequence sequence) {
		if (sequence.getAnchorName() != null
				|| (sequence.getLeadingComments() != null && !sequence.getLeadingComments().isEmpty())) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Close this writer and its underlying stream.
	 */
	@Override
	public void close() throws IOException {
		closeQuietly(outputWriter);
		outputWriter = null;
		closeQuietly(outputStream);
		outputStream = null;
	}

	/**
	 * Close a Closable item and ignore any Exception thrown by its close method.
	 *
	 * @param closeableItem
	 *            the closeable item
	 */
	private static void closeQuietly(final Closeable closeableItem) {
		if (closeableItem != null) {
			try {
				closeableItem.close();
			} catch (@SuppressWarnings("unused") final IOException e) {
				// Do nothing
			}
		}
	}

	public static String toString(final YamlDocument yamlDocument) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (final YamlWriter writer = new YamlWriter(outputStream)) {
			writer.writeDocument(yamlDocument);
		}
		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	public static String toString(final YamlMapping yamlMapping) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (final YamlWriter writer = new YamlWriter(outputStream)) {
			if (yamlMapping.isFlowStyle()) {
				writer.writeFlowMapping(yamlMapping, 0);
			} else {
				writer.writeBlockMapping(yamlMapping, 0);
			}
		}
		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	public static String toString(final YamlSequence yamlSequence) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (final YamlWriter writer = new YamlWriter(outputStream)) {
			if (yamlSequence.isFlowStyle()) {
				writer.writeFlowSequence(yamlSequence, 0);
			} else {
				writer.writeBlockSequence(yamlSequence, 0);
			}
		}
		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	private YamlWriter write(final String text) throws IOException {
		outputWriter.write(text);
		return this;
	}

	public YamlWriter addSequenceItem(final YamlNode item, final int indentLevel, boolean isFirstData) throws Exception {
		if (item.getLeadingComments() != null && !item.getLeadingComments().isEmpty()) {
			for (final String commentLine : item.getLeadingComments()) {
				if (!isFirstData) {
					writeIndent(indentLevel);
				}
				write("#" + commentLine + linebreakString);
				isFirstData = false;
			}
		}

		if (!isFirstData) {
			writeIndent(indentLevel);
		}
		write("-");

		boolean startItemInNewLine = false;
		if (item.getAnchorName() != null) {
			write(" &" + item.getAnchorName());
			startItemInNewLine = true;
		}

		if (item instanceof final YamlScalar scalar) {
			if (scalar.getType() == YamlScalarType.MULTILINE) {
				writeMultilineStringScalar(scalar, indentLevel + 1);
			} else if (startItemInNewLine) {
				write(linebreakString);
				writeIndent(indentLevel + 1);
				writeScalarInlineInSequence(scalar);
			} else {
				write(" ");
				writeScalarInlineInSequence(scalar);
			}
		} else if (item instanceof final YamlAlias alias) {
			write(" *" + alias.getTargetAnchorName());
			if (alias.getInlineComment() == null) {
				write(linebreakString);
			} else {
				write(" #" + alias.getInlineComment() + linebreakString);
			}
		} else if (item instanceof final YamlMapping mapping) {
			if (!startItemInNewLine) {
				write(" ");
				writeNode(mapping, indentLevel + 1, false, false);
			} else {
				write(linebreakString);
				writeIndent(indentLevel + 1);
				writeNode(mapping, indentLevel + 1, false, false);
			}
		} else if (item instanceof final YamlSequence sequence) {
			if (!startItemInNewLine
					&& !mustStartInNewLine(sequence)) {
				write(" ");
				writeNode(sequence, indentLevel + 1, false, false);
			} else {
				write(linebreakString);
				if (sequence.getLeadingComments() != null) {
					for (final String commentLine1 : sequence.getLeadingComments()) {
						writeIndent(indentLevel + 1);
						write("#" + commentLine1 + linebreakString);
					}
				}
				writeIndent(indentLevel + 1);
				writeNode(sequence, indentLevel + 1, false, false);
			}
		} else {
			throw new Exception("Unknown YAML node type: '" + item.getClass().getSimpleName() + "'");
		}
		return this;
	}

	public YamlWriter addSequenceItem(final YamlNode item) throws Exception {
		addSequenceItem(item, 0, false);
		return this;
	}

	public YamlWriter addSequenceItem(final Object item) throws Exception {
		if (item == null) {
			addSequenceItem(new YamlScalar(null));
		} else if (item instanceof String) {
			addSequenceItem(new YamlScalar((String) item, YamlScalarType.STRING));
		} else if (item instanceof Number) {
			addSequenceItem(new YamlScalar((Number) item, YamlScalarType.NUMBER));
		} else if (item instanceof Boolean) {
			addSequenceItem(new YamlScalar((Boolean) item, YamlScalarType.BOOLEAN));
		} else {
			throw new Exception("Item Object to add by YamlWriter must be String or Boolean or Number, but was: " + item.getClass().getSimpleName());
		}
		return this;
	}
}
