package de.soderer.yaml;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import de.soderer.json.utilities.NumberUtilities;
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
import de.soderer.yaml.data.directive.YamlDirective;

/**
 * Writer for YAML data to an output stream.
 * <p>
 * The output is formatted by a {@link YamlFormat}. Comments, anchors, quote styles, block scalar
 * styles and empty lines kept in the nodes are written again. Strings are quoted where needed,
 * e.g. if they would be read as number or boolean otherwise. Multiple documents are separated by
 * document markers.
 * </p>
 */
public class YamlWriter implements Closeable {
	/** Default output encoding. */
	public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	/** Output stream. */
	private OutputStream outputStream;

	/** Output encoding. */
	private final Charset encoding;

	private final YamlFormat yamlFormat;

	private BufferedWriter outputWriter = null;
	private boolean firstDocument = true;
	private boolean documentEndWasWritten = false;

	/**
	 * Creates a new YAML writer using UTF-8 encoding and default format.
	 *
	 * @param outputStream
	 *            the stream to write to
	 * @throws IllegalStateException
	 *             if the output stream is null
	 */
	public YamlWriter(final OutputStream outputStream) {
		this(outputStream, DEFAULT_ENCODING, new YamlFormat());
	}

	/**
	 * Creates a new YAML writer using default format.
	 *
	 * @param outputStream
	 *            the stream to write to
	 * @param encoding
	 *            the encoding of the output, or null for UTF-8
	 * @throws IllegalStateException
	 *             if the output stream is null
	 */
	public YamlWriter(final OutputStream outputStream, final Charset encoding) {
		this(outputStream, encoding, new YamlFormat());
	}

	/**
	 * Creates a new YAML writer using UTF-8 encoding.
	 *
	 * @param outputStream
	 *            the stream to write to
	 * @param yamlFormat
	 *            the output format
	 * @throws IllegalStateException
	 *             if the output stream is null
	 */
	public YamlWriter(final OutputStream outputStream, final YamlFormat yamlFormat) {
		this(outputStream, DEFAULT_ENCODING, yamlFormat);
	}

	/**
	 * Creates a new YAML writer.
	 *
	 * @param outputStream
	 *            the stream to write to
	 * @param encoding
	 *            the encoding of the output, or null for UTF-8
	 * @param yamlFormat
	 *            the output format, null for default format
	 * @throws IllegalStateException
	 *             if the output stream is null
	 */
	public YamlWriter(final OutputStream outputStream, final Charset encoding, final YamlFormat yamlFormat) {
		this.outputStream = outputStream;
		this.encoding = encoding == null ? DEFAULT_ENCODING : encoding;
		this.yamlFormat = yamlFormat == null ? new YamlFormat() : yamlFormat;

		if (outputStream == null) {
			throw new IllegalStateException("YamlWriter outputStream must not be null");
		}
		outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, this.encoding));
	}

	/**
	 * Returns the encoding of the output.
	 *
	 * @return the encoding
	 */
	public Charset getEncoding() {
		return encoding;
	}

	/**
	 * Writes a document with its directives and comments. From the second document on, documents
	 * are separated by "---" and "..." markers.
	 *
	 * @param document
	 *            the document
	 * @return this writer for chaining
	 * @throws Exception
	 *             if the document contains unsupported nodes or writing fails
	 */
	public YamlWriter writeDocument(final YamlDocument document) throws Exception {
		if (!firstDocument && !documentEndWasWritten) {
			write("..." + yamlFormat.getLinebreakString());
		}

		if (document.getDirectives() != null) {
			for (final YamlDirective<?> directive : document.getDirectives()) {
				write(directive.toString() + yamlFormat.getLinebreakString());
			}
			write("---" + yamlFormat.getLinebreakString());
		} else if (!firstDocument) {
			write("---" + yamlFormat.getLinebreakString());
		}

		if (document.getLeadingComments() != null && !yamlFormat.isOmitComments()) {
			for (final String leadingCommentLine : document.getLeadingComments()) {
				write("#");
				write(leadingCommentLine);
				write(yamlFormat.getLinebreakString());
			}
		}

		if (document.getRoot() != null) {
			writeLeadingEmptyLines(document.getRoot());
			if (document.getRoot().getLeadingComments() != null && !yamlFormat.isOmitComments()) {
				for (final String leadingCommentLine : document.getRoot().getLeadingComments()) {
					write("#");
					write(leadingCommentLine);
					write(yamlFormat.getLinebreakString());
				}
			}
			writePostCommentEmptyLines(document.getRoot());
			writeNode(document.getRoot(), 0, false, false);
		}

		if (document.getDirectives() != null || !firstDocument) {
			write("..." + yamlFormat.getLinebreakString());
			documentEndWasWritten = true;
		} else {
			documentEndWasWritten = false;
		}

		outputWriter.flush();
		firstDocument = false;
		return this;
	}

	/**
	 * Writes multiple documents, see {@link #writeDocument(YamlDocument)}.
	 *
	 * @param documentList
	 *            the documents
	 * @return this writer for chaining
	 * @throws Exception
	 *             if a document contains unsupported nodes or writing fails
	 */
	public YamlWriter writeDocumentList(final List<YamlDocument> documentList) throws Exception {
		for (int i = 0; i < documentList.size(); i++) {
			final YamlDocument document = documentList.get(i);
			writeDocument(document);
			firstDocument = false;
		}
		return this;
	}

	private YamlWriter writeNode(final YamlNode node, final int indentLevel, final boolean inFlow, final boolean isKeyContext) throws Exception {
		if (node instanceof final YamlScalar scalar) {
			writeScalar(scalar, indentLevel, inFlow, isKeyContext);
		} else if (node instanceof final YamlAlias alias) {
			writeLeadingEmptyLines(alias);
			if (alias.getLeadingComments() != null && !yamlFormat.isOmitComments()) {
				for (final String commentLine : alias.getLeadingComments()) {
					writeIndent(indentLevel);
					write("#" + commentLine + yamlFormat.getLinebreakString());
				}
			}

			writePostCommentEmptyLines(alias);
			writeAlias(alias, indentLevel);
			if (!inFlow) {
				write(yamlFormat.getLinebreakString());
			}
		} else if (node instanceof final YamlSequence sequence) {
			if (sequence.size() == 0) {
				writeFlowSequence(sequence, indentLevel);
				if (!inFlow) {
					if (sequence.getInlineComment() != null && !yamlFormat.isOmitComments()) {
						write(" #" + sequence.getInlineComment());
					}
					write(yamlFormat.getLinebreakString());
				}
			} else if ((sequence.isFlowStyle() && !yamlFormat.isIgnoreFlowStyleSettings()) || inFlow) {
				writeFlowSequence(sequence, indentLevel);
				if (!inFlow) {
					if (sequence.getInlineComment() != null && !yamlFormat.isOmitComments()) {
						write(" #" + sequence.getInlineComment());
					}
					write(yamlFormat.getLinebreakString());
				}
			} else {
				writeLeadingEmptyLines(sequence);
				if (sequence.getLeadingComments() != null && !yamlFormat.isOmitComments()) {
					boolean firstCommentLine = true;
					for (final String commentLine : sequence.getLeadingComments()) {
						if (!firstCommentLine) {
							writeIndent(indentLevel);
						}
						write("#" + commentLine + yamlFormat.getLinebreakString());
						writeIndent(indentLevel);
						firstCommentLine = false;
					}
				}

				final int sequencePostCommentEmptyLines = yamlFormat.isOmitEmptyLines() ? 0 : sequence.getPostCommentEmptyLinesCount();
				writePostCommentEmptyLines(sequence);
				if (sequencePostCommentEmptyLines > 0) {
					writeIndent(indentLevel);
				}
				writeBlockSequence(sequence, indentLevel);
			}
		} else if (node instanceof final YamlMapping mapping) {
			if (mapping.size() == 0) {
				writeFlowMapping(mapping, indentLevel);
				if (!inFlow) {
					if (mapping.getInlineComment() != null && !yamlFormat.isOmitComments()) {
						write(" #" + mapping.getInlineComment());
					}
					write(yamlFormat.getLinebreakString());
				}
			} else if ((mapping.isFlowStyle() && !yamlFormat.isIgnoreFlowStyleSettings()) || inFlow) {
				writeFlowMapping(mapping, indentLevel);
				if (!inFlow) {
					if (mapping.getInlineComment() != null && !yamlFormat.isOmitComments()) {
						write(" #" + mapping.getInlineComment());
					}
					write(yamlFormat.getLinebreakString());
				}
			} else {
				writeBlockMapping(mapping, indentLevel);
			}
		} else {
			throw new IllegalStateException("Unknow type of node: " + node.getClass());
		}
		return this;
	}

	private YamlWriter writeIndent(final int indentLevel) throws IOException {
		if (indentLevel > 0) {
			write(" ".repeat(indentLevel * yamlFormat.getIndentationSize()));
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
				if (inFlow) {
					write((value == null ? "null" : value.toString()));
				} else {
					write((value == null ? "null" : value.toString()) + (Utilities.isNotBlank(inlineComment) && !yamlFormat.isOmitComments() ? " #" + inlineComment : "") + yamlFormat.getLinebreakString());
				}
				break;
			case STRING:
				if (inFlow ) {
					writeIndent(indentLevel);
					write(escapePlainString(value, scalar.getQuoteType(), false, true));
				} else if (isKeyContext) {
					writeIndent(indentLevel);
					write(escapePlainString(value, scalar.getQuoteType(), true, false));
				} else {
					write(escapePlainString(value, scalar.getQuoteType(), false, false) + (Utilities.isNotBlank(inlineComment) && !yamlFormat.isOmitComments() ? " #" + inlineComment : "") + yamlFormat.getLinebreakString());
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
		if (alias.getInlineComment() != null && !yamlFormat.isOmitComments()) {
			write(" #" + alias.getInlineComment() + yamlFormat.getLinebreakString());
		} else {
			write(yamlFormat.getLinebreakString());
		}
		return this;
	}

	private YamlWriter writeMultilineStringScalar(final YamlScalar scalar, final int indentLevel) throws IOException {
		if (indentLevel > 0) {
			write(" ");
		}

		final String valueString = scalar.getValueString();
		if (!canWriteAsBlockScalar(scalar)) {
			// Content that a block scalar cannot represent is written as double quoted string
			write("\"" + YamlUtilities.escapeScalarString(valueString) + "\"");
			if (Utilities.isNotBlank(scalar.getInlineComment()) && !yamlFormat.isOmitComments()) {
				write(" #" + scalar.getInlineComment());
			}
			write(yamlFormat.getLinebreakString());
			return this;
		}

		// The chomping indicator is derived from the trailing linebreaks of the text, so the text is kept unchanged
		int trailingLinebreaks = 0;
		while (trailingLinebreaks < valueString.length() && valueString.charAt(valueString.length() - 1 - trailingLinebreaks) == '\n') {
			trailingLinebreaks++;
		}
		final String content = valueString.substring(0, valueString.length() - trailingLinebreaks);

		write(scalar.getMultilineType() == YamlMultilineScalarType.FOLDED ? ">" : "|");
		if (trailingLinebreaks == 0) {
			write("-");
		} else if (trailingLinebreaks > 1 || scalar.getMultilineChompingType() == YamlMultilineScalarChompingType.KEEP) {
			write("+");
		}
		if (scalar.getIndentationIndicator() > 0) {
			write(Integer.toString(scalar.getIndentationIndicator()));
		}
		if (Utilities.isNotBlank(scalar.getInlineComment()) && !yamlFormat.isOmitComments()) {
			write(" #" + scalar.getInlineComment());
		}
		write(yamlFormat.getLinebreakString());

		final List<String> lines = scalar.getMultilineType() == YamlMultilineScalarType.FOLDED ? getFoldedLines(content) : new ArrayList<>(Arrays.asList(content.split("\n", -1)));
		if (content.isEmpty()) {
			lines.clear();
		}
		// Additional trailing linebreaks are kept as empty lines
		for (int i = 1; i < trailingLinebreaks; i++) {
			lines.add("");
		}

		for (final String line : lines) {
			if (scalar.getIndentationIndicator() > 0) {
				writeIndent(indentLevel - 1);
				write(Utilities.repeat(" ", scalar.getIndentationIndicator()));
			} else {
				writeIndent(indentLevel);
			}
			write(line + yamlFormat.getLinebreakString());
		}
		return this;
	}

	/**
	 * Checks whether a multiline scalar can be written as block scalar without changing its text.
	 * Without explicit indentation indicator the first content line must not start with whitespace,
	 * because it defines the indentation. Control characters need escaping.
	 */
	private static boolean canWriteAsBlockScalar(final YamlScalar scalar) {
		final String valueString = scalar.getValueString();
		if (needsEscaping(valueString.replace("\n", ""))) {
			return false;
		} else if (scalar.getIndentationIndicator() > 0) {
			return true;
		} else {
			for (final String line : valueString.split("\n")) {
				if (!line.isEmpty()) {
					return !line.startsWith(" ") && !line.startsWith("\t");
				}
			}
			return true;
		}
	}

	/**
	 * Returns the lines to write for a folded block scalar, so that folding while reading results
	 * in the given text again: a linebreak between two lines that are not more indented is folded
	 * to a blank while reading, so each such linebreak must be written as an additional empty line.
	 */
	private static List<String> getFoldedLines(final String content) {
		final String[] textLines = content.split("\n", -1);
		final List<String> lines = new ArrayList<>();
		int index = 0;
		while (index < textLines.length && textLines[index].isEmpty()) {
			// Leading linebreaks are kept as they are
			lines.add("");
			index++;
		}
		if (index < textLines.length) {
			lines.add(textLines[index]);
			String previousLine = textLines[index];
			index++;
			while (index < textLines.length) {
				int emptyLines = 0;
				while (textLines[index].isEmpty()) {
					emptyLines++;
					index++;
				}
				final String nextLine = textLines[index];
				final boolean folded = !isMoreIndented(previousLine) && !isMoreIndented(nextLine);
				final int emptyLinesToWrite = folded ? emptyLines + 1 : emptyLines;
				for (int i = 0; i < emptyLinesToWrite; i++) {
					lines.add("");
				}
				lines.add(nextLine);
				previousLine = nextLine;
				index++;
			}
		}
		return lines;
	}

	private static boolean isMoreIndented(final String line) {
		return line.startsWith(" ") || line.startsWith("\t");
	}

	private static boolean needsEscaping(final String text) {
		for (int i = 0; i < text.length(); i++) {
			final char c = text.charAt(i);
			if ((c < ' ' && c != '\t') || (c >= 0x7F && c <= 0x9F) || c == '\u2028' || c == '\u2029' || c == '\uFEFF' || c == '\uFFFE' || c == '\uFFFF') {
				return true;
			}
		}
		return false;
	}

	private String escapePlainString(final String text, final YamlStringQuoteType quoteType, final boolean isKey, final boolean isFlow) {
		final boolean alwaysQuote = isKey ? yamlFormat.isAlwaysQuoteStringKeys() : yamlFormat.isAlwaysQuoteStringValues();

		boolean needsQuotes = alwaysQuote || text.isEmpty() || (quoteType != null && quoteType != YamlStringQuoteType.NONE);

		if (!needsQuotes) {
			if (Character.isWhitespace(text.charAt(0)) || Character.isWhitespace(text.charAt(text.length() - 1))) {
				needsQuotes = true;
			}
		}

		final boolean needsEscaping = needsEscaping(text);
		if (needsEscaping) {
			needsQuotes = true;
		}

		if (!needsQuotes) {
			if (text.startsWith("---") || text.startsWith("...") || text.startsWith(",")) {
				// Document markers and indicators that cannot start a plain scalar
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			for (int i = 0; i < text.length(); i++) {
				final char c = text.charAt(i);
				if ((Character.isWhitespace(c) && c != ' ') || "#*!'\"%@`".indexOf(c) > -1 || (isFlow && c == ',')) {
					needsQuotes = true;
					break;
				}
			}
		}

		if (!needsQuotes) {
			if (text.contains(": ") || text.contains(":\t") || text.contains(":\n") || text.endsWith(":")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if (text.contains(" &") || text.contains("\t&") || text.startsWith("&")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if (isFlow) {
				if (text.contains("[") || text.contains("]") || text.contains("{") || text.contains("}")) {
					needsQuotes = true;
				}
			} else {
				if (text.startsWith("[") || text.startsWith("]") || text.startsWith("{") || text.startsWith("}")) {
					needsQuotes = true;
				}
			}
		}

		if (!needsQuotes) {
			if (text.startsWith("|") || text.startsWith(">") || text.startsWith("-") || text.startsWith("?")) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text)
					|| "no".equalsIgnoreCase(text) || "on".equalsIgnoreCase(text) || "off".equalsIgnoreCase(text)
					|| "null".equalsIgnoreCase(text) || "~".equalsIgnoreCase(text) || "y".equalsIgnoreCase(text)
					|| "n".equalsIgnoreCase(text)) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			// Texts that would be read as number, e.g. "1_000", "0x1F" or ".inf"
			if (NumberUtilities.isNumber(text) || ("+-.0123456789".indexOf(text.charAt(0)) >= 0 && YamlScalar.parseYamlNumber(text) != null)) {
				needsQuotes = true;
			}
		}

		if (!needsQuotes) {
			return text;
		} else {
			// Single quoted strings cannot contain escapes, so texts with control characters always use double quotes
			final boolean useDoubleQuotes = needsEscaping || (isKey
					? (quoteType == null || quoteType == YamlStringQuoteType.DOUBLE)
					: (quoteType == YamlStringQuoteType.DOUBLE || (quoteType == null && yamlFormat.getStringValueQuoteType() == YamlStringQuoteType.DOUBLE)));

			if (useDoubleQuotes) {
				return "\"" + YamlUtilities.escapeScalarString(text) + "\"";
			} else {
				return "'" + text.replace("'", "''") + "'";
			}
		}
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
				write(scalar.getValueString() + (Utilities.isNotBlank(inlineComment) && !yamlFormat.isOmitComments() ? " #" + inlineComment : "") + yamlFormat.getLinebreakString());
				break;
			case STRING:
				write(escapePlainString(scalar.getValueString(), scalar.getQuoteType(), false, false) + (Utilities.isNotBlank(inlineComment) && !yamlFormat.isOmitComments() ? " #" + inlineComment : "") + yamlFormat.getLinebreakString());
				break;
			case MULTILINE:
			default:
				write(escapePlainString(scalar.getValueString(), scalar.getQuoteType(), false, false) + yamlFormat.getLinebreakString());
		}
		return this;
	}

	private YamlWriter writeBlockMapping(final YamlMapping yamlMapping, final int indentLevel) throws Exception {
		boolean isFirstData = true;
		for (final Entry<YamlNode, YamlNode> entry : yamlMapping.entrySet()) {
			final YamlNode key = entry.getKey();
			final YamlNode value = entry.getValue();

			writeLeadingEmptyLines(key);
			if (key.getLeadingComments() != null && !yamlFormat.isOmitComments()) {
				if (isFirstData) {
					if (key.getLeadingComments() != null && !yamlFormat.isOmitComments()) {
						boolean firstCommentLine = true;
						for (final String commentLine : key.getLeadingComments()) {
							if (!firstCommentLine) {
								writeIndent(indentLevel);
							}
							write("#" + commentLine + yamlFormat.getLinebreakString());
							firstCommentLine = false;
						}
					}
					isFirstData = false;
				} else {
					if (key.getLeadingComments() != null && !yamlFormat.isOmitComments()) {
						for (final String commentLine : key.getLeadingComments()) {
							writeIndent(indentLevel);
							write("#" + commentLine + yamlFormat.getLinebreakString());
						}
					}
				}
			}

			writePostCommentEmptyLines(key);

			if (key instanceof final YamlScalar scalarKey) {
				if (scalarKey.getType() == YamlScalarType.STRING || scalarKey.getType() == YamlScalarType.MULTILINE) {
					// Multiline keys are written as quoted string with escaped linebreaks
					if (!isFirstData) {
						writeIndent(indentLevel);
						write(escapePlainString(scalarKey.getValueString(), scalarKey.getQuoteType(), true, false));
						isFirstData = false;
					} else {
						write(escapePlainString(scalarKey.getValueString(), scalarKey.getQuoteType(), true, false));
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
			if (key.getInlineComment() != null && !yamlFormat.isOmitComments()) {
				write(" #" + key.getInlineComment());
				startValueInNewLine = true;
			}

			if (value instanceof final YamlScalar scalar) {
				if (scalar.getType() == YamlScalarType.MULTILINE) {
					writeNode(scalar, indentLevel + 1, false, false);
				} else if (!startValueInNewLine
						&& scalar.getAnchorName() == null
						&& (scalar.getLeadingComments() == null || scalar.getLeadingComments().isEmpty() || yamlFormat.isOmitComments())) {
					final String inlineComment = scalar.getInlineComment();
					switch (scalar.getType()) {
						case BOOLEAN:
							write(" ");
							write(scalar.getValueString() + (Utilities.isNotBlank(inlineComment) && !yamlFormat.isOmitComments() ? " #" + inlineComment : "") + yamlFormat.getLinebreakString());
							break;
						case NUMBER:
							write(" ");
							write(scalar.getValueString() + (Utilities.isNotBlank(inlineComment) && !yamlFormat.isOmitComments() ? " #" + inlineComment : "") + yamlFormat.getLinebreakString());
							break;
						case NULL_VALUE:
							if (!"".equals(scalar.getValueString()) ) {
								write(" ");
							}
							write(scalar.getValueString() + (Utilities.isNotBlank(inlineComment) && !yamlFormat.isOmitComments() ? " #" + inlineComment : "") + yamlFormat.getLinebreakString());
							break;
						case STRING:
							write(" ");
							write(escapePlainString(scalar.getValueString(), scalar.getQuoteType(), false, false) + (Utilities.isNotBlank(inlineComment) && !yamlFormat.isOmitComments() ? " #" + inlineComment : "") + yamlFormat.getLinebreakString());
							break;
						case MULTILINE:
							write(" ");
							writeMultilineStringScalar(scalar, indentLevel);
							break;
						default:
							write(" ");
							write(escapePlainString(scalar.getValueString(), scalar.getQuoteType(), false, false) + yamlFormat.getLinebreakString());
					}
				} else {
					write(yamlFormat.getLinebreakString());
					writeLeadingEmptyLines(scalar);
					if (scalar.getLeadingComments() != null && !yamlFormat.isOmitComments()) {
						for (final String commentLine : scalar.getLeadingComments()) {
							writeIndent(indentLevel + 1);
							write("#" + commentLine + yamlFormat.getLinebreakString());
						}
					}
					writePostCommentEmptyLines(scalar);
					writeIndent(indentLevel + 1);
					writeNode(scalar, indentLevel + 1, false, false);
				}
			} else if (value instanceof final YamlAlias alias) {
				write(" *" + alias.getTargetAnchorName());
				if (alias.getInlineComment() != null && !yamlFormat.isOmitComments()) {
					write(" #" + alias.getInlineComment() + yamlFormat.getLinebreakString());
				} else {
					write(yamlFormat.getLinebreakString());
				}
			} else if (value instanceof final YamlMapping mapping) {
				if (mapping.size() == 0 && yamlFormat.isUseNoNewLineForEmptyYamlObjectInYamlMappingProperties()) {
					write(" ");
				} else if (!mapping.isFlowStyle() || yamlFormat.isIgnoreFlowStyleSettings()) {
					write(yamlFormat.getLinebreakString());
					writeIndent(indentLevel + 1);
				} else {
					write(" ");
				}
				writeNode(mapping, indentLevel + 1, false, false);
			} else if (value instanceof final YamlSequence sequence) {
				if (sequence.size() == 0 && yamlFormat.isUseNoNewLineForEmptyYamlObjectInYamlMappingProperties()) {
					write(" ");
					writeNode(sequence, indentLevel + 1, false, false);
				} else {
					final int sequenceIndentLevel = yamlFormat.isUseNoExtraIndentationForYamlSequencesInYamlMappingProperties() ? indentLevel : indentLevel + 1;
					if (!startValueInNewLine
							&& sequence.getAnchorName() == null
							&& (sequence.getLeadingComments() == null || sequence.getLeadingComments().isEmpty())) {
						if (!sequence.isFlowStyle() || yamlFormat.isIgnoreFlowStyleSettings()) {
							if (key instanceof YamlScalar) {
								write(yamlFormat.getLinebreakString());
								writeIndent(sequenceIndentLevel);
							} else {
								write(" ");
							}
						} else {
							write(" ");
						}
						writeNode(sequence, sequenceIndentLevel, false, false);
					} else {
						if (!sequence.isFlowStyle() || yamlFormat.isIgnoreFlowStyleSettings()) {
							write(yamlFormat.getLinebreakString());
							writeLeadingEmptyLines(sequence);
							if (sequence.getLeadingComments() != null && !yamlFormat.isOmitComments()) {
								for (final String commentLine : sequence.getLeadingComments()) {
									writeIndent(sequenceIndentLevel);
									write("#" + commentLine + yamlFormat.getLinebreakString());
								}
							}
							writePostCommentEmptyLines(sequence);
							writeIndent(sequenceIndentLevel);
						} else {
							write(" ");
						}
						writeNode(sequence, sequenceIndentLevel, false, false);
					}
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
					if (pendingValueInlineComment != null && !yamlFormat.isOmitComments()) {
						write(", #" + pendingValueInlineComment + yamlFormat.getLinebreakString());
					} else {
						write("," + yamlFormat.getLinebreakString());
					}
				}
			}

			final YamlNode key = entry.getKey();

			if (key.getLeadingComments() != null && !yamlFormat.isOmitComments()) {
				for (final String commentLine : key.getLeadingComments()) {
					writeIndent(indentLevel + 1);
					write("#" + commentLine + yamlFormat.getLinebreakString());
				}
			}

			if (!first && !isSingleLineFlow) {
				writeIndent(indentLevel + 1);
			}

			if (key instanceof final YamlScalar scalarKey
					&& scalarKey.getType() == YamlScalarType.STRING
					&& scalarKey.getAnchorName() == null
					&& (key.getLeadingComments() == null || key.getLeadingComments().isEmpty())) {
				writeScalarInlineInFlow(scalarKey, true);
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
			if (key.getInlineComment() != null && !yamlFormat.isOmitComments()) {
				write(" #" + key.getInlineComment() + yamlFormat.getLinebreakString());
			}

			final YamlNode value = entry.getValue();

			if (value.getLeadingComments() == null || value.getLeadingComments().isEmpty()) {
				write(" ");
			} else {
				write(yamlFormat.getLinebreakString());
				writeIndent(indentLevel + 2);
			}

			if (value.getLeadingComments() != null && !yamlFormat.isOmitComments()) {
				for (final String commentLine : value.getLeadingComments()) {
					write(yamlFormat.getLinebreakString());
					writeIndent(indentLevel + 2);
					write("#" + commentLine + yamlFormat.getLinebreakString());
				}
			}

			if (value instanceof final YamlScalar scalarVal
					&& scalarVal.getType() != YamlScalarType.MULTILINE
					&& scalarVal.getAnchorName() == null
					&& (scalarVal.getLeadingComments() == null || scalarVal.getLeadingComments().isEmpty())) {
				writeScalarInlineInFlow(scalarVal, false);
			} else if (value instanceof final YamlAlias alias) {
				write(" *" + alias.getTargetAnchorName());
				if (alias.getInlineComment() != null && !yamlFormat.isOmitComments()) {
					write(" #" + alias.getInlineComment() + yamlFormat.getLinebreakString());
				} else {
					write(yamlFormat.getLinebreakString());
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
			write(yamlFormat.getLinebreakString());
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
					if (pendingItemInlineComment != null && !yamlFormat.isOmitComments()) {
						write(", #" + pendingItemInlineComment + yamlFormat.getLinebreakString());
					} else {
						write("," + yamlFormat.getLinebreakString());
					}
				}
			}

			if (item.getLeadingComments() != null && !yamlFormat.isOmitComments()) {
				for (final String commentLine : item.getLeadingComments()) {
					writeIndent(indentLevel + 1);
					write("#" + commentLine + yamlFormat.getLinebreakString());
				}
			}

			if (!first && !isSingleLineFlow) {
				writeIndent(indentLevel + 1);
			}

			if (item instanceof final YamlScalar scalar
					&& scalar.getType() != YamlScalarType.MULTILINE
					&& scalar.getAnchorName() == null
					&& (scalar.getLeadingComments() == null || scalar.getLeadingComments().isEmpty())) {
				writeScalarInlineInFlow(scalar, false);
			} else if (item instanceof final YamlAlias alias) {
				write(" *" + alias.getTargetAnchorName());
				if (alias.getInlineComment() != null && !yamlFormat.isOmitComments()) {
					write(" #" + alias.getInlineComment() + yamlFormat.getLinebreakString());
				} else {
					write(yamlFormat.getLinebreakString());
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
			write(yamlFormat.getLinebreakString());
			writeIndent(indentLevel + 1);
		}

		write("]");
		return this;
	}

	private YamlWriter writeScalarInlineInFlow(final YamlScalar scalar, final boolean isKey) throws IOException {
		final String inlineComment = scalar.getInlineComment();
		switch (scalar.getType()) {
			case BOOLEAN:
			case NUMBER:
			case NULL_VALUE:
				write(scalar.getValueString() + (Utilities.isNotBlank(inlineComment) && !yamlFormat.isOmitComments() ? " #" + inlineComment : ""));
				break;
			case STRING:
				write(escapePlainString(scalar.getValueString(), scalar.getQuoteType(), isKey, true) + (Utilities.isNotBlank(inlineComment) && !yamlFormat.isOmitComments() ? " #" + inlineComment : ""));
				break;
			case MULTILINE:
			default:
				write(escapePlainString(scalar.getValueString(), scalar.getQuoteType(), isKey, true));
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
		YamlUtilities.closeQuietly(outputWriter);
		outputWriter = null;
		YamlUtilities.closeQuietly(outputStream);
		outputStream = null;
	}

	/**
	 * Returns a document as YAML text in default format.
	 *
	 * @param yamlDocument
	 *            the data to write
	 * @return the YAML text
	 * @throws Exception
	 *             if the data contains unsupported nodes
	 */
	public static String toString(final YamlDocument yamlDocument) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (final YamlWriter writer = new YamlWriter(outputStream)) {
			writer.writeDocument(yamlDocument);
		}
		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * Returns a mapping as YAML text in default format.
	 *
	 * @param yamlMapping
	 *            the data to write
	 * @return the YAML text
	 * @throws Exception
	 *             if the data contains unsupported nodes
	 */
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

	/**
	 * Returns a sequence as YAML text in default format.
	 *
	 * @param yamlSequence
	 *            the data to write
	 * @return the YAML text
	 * @throws Exception
	 *             if the data contains unsupported nodes
	 */
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

	/**
	 * Writes the number of leading empty lines stored on the given node (if any) as plain
	 * linebreaks, directly before its leading comments or content are written. Writes nothing
	 * if the format is configured to omit empty lines.
	 */
	private YamlWriter writeLeadingEmptyLines(final YamlNode node) throws IOException {
		if (!yamlFormat.isOmitEmptyLines()) {
			for (int i = 0; i < node.getLeadingEmptyLinesCount(); i++) {
				write(yamlFormat.getLinebreakString());
			}
		}
		return this;
	}

	/**
	 * Writes the number of empty lines stored on the given node that were found after its
	 * leading comments (if any) but still before the node itself, directly after its leading
	 * comments have been written and before its own content follows. Writes nothing if the
	 * format is configured to omit empty lines.
	 */
	private YamlWriter writePostCommentEmptyLines(final YamlNode node) throws IOException {
		if (!yamlFormat.isOmitEmptyLines()) {
			for (int i = 0; i < node.getPostCommentEmptyLinesCount(); i++) {
				write(yamlFormat.getLinebreakString());
			}
		}
		return this;
	}

	/**
	 * Writes the blanks after the "-" of a sequence item, so that the item content starts at the
	 * next indentation level.
	 */
	private void writeSequenceItemPadding() throws IOException {
		write(" ".repeat(yamlFormat.getIndentationSize() - 1));
	}

	/**
	 * Writes a block sequence item ("- ...") with its comments.
	 *
	 * @param item
	 *            the item
	 * @param indentLevel
	 *            the indentation level of the sequence
	 * @param isFirstData
	 *            true, if the current line is already indented (first item after a key or another "-")
	 * @return this writer for chaining
	 * @throws Exception
	 *             if the item contains unsupported nodes or writing fails
	 */
	public YamlWriter addSequenceItem(final YamlNode item, final int indentLevel, boolean isFirstData) throws Exception {
		writeLeadingEmptyLines(item);
		if (item.getLeadingComments() != null && !item.getLeadingComments().isEmpty() && !yamlFormat.isOmitComments()) {
			for (final String commentLine : item.getLeadingComments()) {
				if (!isFirstData) {
					writeIndent(indentLevel);
				}
				write("#" + commentLine + yamlFormat.getLinebreakString());
				isFirstData = false;
			}
		}

		writePostCommentEmptyLines(item);

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
				write(yamlFormat.getLinebreakString());
				writeIndent(indentLevel + 1);
				writeScalarInlineInSequence(scalar);
			} else {
				writeSequenceItemPadding();
				writeScalarInlineInSequence(scalar);
			}
		} else if (item instanceof final YamlAlias alias) {
			writeSequenceItemPadding();
			write("*" + alias.getTargetAnchorName());
			if (alias.getInlineComment() != null && !yamlFormat.isOmitComments()) {
				write(" #" + alias.getInlineComment() + yamlFormat.getLinebreakString());
			} else {
				write(yamlFormat.getLinebreakString());
			}
		} else if (item instanceof final YamlMapping mapping) {
			if (!startItemInNewLine) {
				writeSequenceItemPadding();
				writeNode(mapping, indentLevel + 1, false, false);
			} else {
				write(yamlFormat.getLinebreakString());
				writeIndent(indentLevel + 1);
				writeNode(mapping, indentLevel + 1, false, false);
			}
		} else if (item instanceof final YamlSequence sequence) {
			if (!startItemInNewLine
					&& !mustStartInNewLine(sequence)) {
				writeSequenceItemPadding();
				writeNode(sequence, indentLevel + 1, false, false);
			} else {
				write(yamlFormat.getLinebreakString());
				if (sequence.getLeadingComments() != null && !yamlFormat.isOmitComments()) {
					for (final String commentLine1 : sequence.getLeadingComments()) {
						writeIndent(indentLevel + 1);
						write("#" + commentLine1 + yamlFormat.getLinebreakString());
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

	/**
	 * Writes a block sequence item at top level, e.g. to write a large sequence item by item.
	 *
	 * @param item
	 *            the item
	 * @return this writer for chaining
	 * @throws Exception
	 *             if the item contains unsupported nodes or writing fails
	 */
	public YamlWriter addSequenceItem(final YamlNode item) throws Exception {
		addSequenceItem(item, 0, false);
		return this;
	}

	/**
	 * Writes a simple value as block sequence item at top level.
	 *
	 * @param item
	 *            a String, Number or Boolean, or null for the null value
	 * @return this writer for chaining
	 * @throws Exception
	 *             if the value type is not supported or writing fails
	 */
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
