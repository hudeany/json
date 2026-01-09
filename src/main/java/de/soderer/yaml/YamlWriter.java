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

import de.soderer.json.utilities.Utilities;
import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.data.directive.YamlDirective;

/**
 * TODOs:
 * - Write inline comments for anchors and aliases
 * - Write with ignore settings for flow style
 * - Write with resolving aliases
 */
public class YamlWriter implements Closeable {
	/** Default output encoding. */
	public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	/** Output stream. */
	private OutputStream outputStream;

	/** Output encoding. */
	private final Charset encoding;

	private int indentSize = 2;
	private boolean alwaysQuoteStringKeys = false;
	private boolean alwaysQuoteStringValues = false;

	private BufferedWriter outputWriter = null;
	private boolean firstDocument = true;

	public YamlWriter(final OutputStream outputStream) {
		this(outputStream, null);
	}

	public YamlWriter(final OutputStream outputStream, final Charset encoding) {
		this.outputStream = outputStream;
		this.encoding = encoding == null ? DEFAULT_ENCODING : encoding;

		if (outputStream == null) {
			throw new IllegalStateException("YamlWriter is already closed");
		}
		outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, this.encoding));
	}

	public YamlWriter setIndentSize(final int indentSize) {
		this.indentSize = indentSize;
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

	public void writeDocument(final YamlDocument document) throws Exception {
		if (!firstDocument) {
			write("...\n");
		}

		if (document.getDirectives() != null) {
			for (final YamlDirective<?> directive : document.getDirectives()) {
				write(directive.toString() + "\n");
			}
			write("---");
		}

		if (document.getLeadingComments() != null) {
			for (final String leadingCommentLine : document.getLeadingComments()) {
				write("# ");
				write(leadingCommentLine);
				write("\n");
			}
		}

		if (document.getRoot() != null) {
			writeNode(document.getRoot(), 0, false, false);
		}

		outputWriter.flush();
		firstDocument = false;
	}

	public void writeDocumentList(final List<YamlDocument> documentList) throws Exception {
		for (int i = 0; i < documentList.size(); i++) {
			final YamlDocument document = documentList.get(i);
			if (!firstDocument) {
				write("...\n");
			}
			writeDocument(document);
			firstDocument = false;
		}
	}

	private void writeNode(final YamlNode node, final int indentLevel, final boolean inFlow, final boolean isKeyContext) throws Exception {
		if (node.getAnchorName() != null && !(node instanceof YamlAlias)) {
			if (indentLevel > 0) {
				write(" ");
			}
			write("&" + node.getAnchorName());
		}

		if (node instanceof final YamlScalar scalar) {
			writeScalar(scalar, indentLevel, inFlow, isKeyContext);
		} else if (node instanceof final YamlAlias alias) {
			if (alias.getLeadingComments() != null) {
				for (final String commentLine : alias.getLeadingComments()) {
					writeIndent(indentLevel);
					write("# " + commentLine + "\n");
				}
			}

			writeAlias(alias, indentLevel);
		} else if (node instanceof final YamlSequence seq) {
			if (seq.isFlowStyle() || inFlow) {
				writeFlowSequence(seq, indentLevel);
			} else {
				writeBlockSequence(seq, indentLevel);
			}
		} else if (node instanceof final YamlMapping map) {
			if (map.isFlowStyle() || inFlow) {
				writeFlowMapping(map, indentLevel);
			} else {
				writeBlockMapping(map, indentLevel);
			}
		} else {
			throw new IllegalStateException("Unbekannter Node-Typ: " + node.getClass());
		}
	}

	private void writeIndent(final int indentLevel) throws IOException {
		if (indentLevel > 0) {
			write(" ".repeat(indentLevel * indentSize));
		}
	}

	private void writeScalar(final YamlScalar scalar, final int indentLevel, final boolean inFlow, final boolean isKeyContext) throws IOException {
		final YamlScalarType type = scalar.getType();
		final String value = scalar.getValueString();
		final String inlineComment = scalar.getInlineComment();

		switch (type) {
			case MULTILINE_LITERAL:
				writeMultilineLiteral(scalar, indentLevel);
				break;
			case MULTILINE_FOLDED:
				writeMultilineFolded(scalar, indentLevel);
				break;
			case BOOLEAN:
			case NUMBER:
			case NULL_VALUE:
				writeIndent(indentLevel);
				write((value == null ? "null" : value.toString()) + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : "") + "\n");
				break;
			case STRING:
				if (inFlow || isKeyContext) {
					writeIndent(indentLevel);
					write(escapePlainStringValue(value));
				} else {
					write(escapePlainStringValue(value) + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : "") + "\n");
				}
				break;
			default:
				// Do nothing
		}
	}

	private void writeAlias(final YamlAlias alias, final int indentLevel) throws IOException {
		writeIndent(indentLevel);
		write("*" + alias.getTargetAnchorName() + "\n");
	}

	private void writeMultilineLiteral(final YamlScalar scalar, final int indentLevel) throws IOException {
		if (indentLevel > 0) {
			write(" ");
		}
		write("|\n");

		for (final String line : (scalar.getValueString()).split("\n", -1)) {
			writeIndent(indentLevel);
			write(line + "\n");
		}
	}

	private void writeMultilineFolded(final YamlScalar scalar, final int indentLevel) throws IOException {
		if (indentLevel > 0) {
			write(" ");
		}
		write(">\n");

		for (final String line : (scalar.getValueString()).split("\n", -1)) {
			writeIndent(indentLevel);
			write(line + "\n");
		}
	}

	private String escapePlainStringKey(final String key) {
		if (key.isEmpty()) {
			return "\"\"";
		} else {
			boolean needsQuotes = false;

			if (alwaysQuoteStringKeys) {
				needsQuotes = true;
			} else {
				for (final char c : key.toCharArray()) {
					if ((Character.isWhitespace(c) && c != ' ')
							|| ":{}[],#&*!|>'\"%@`".indexOf(c) > -1) {
						needsQuotes = true;
						break;
					}
				}
			}

			if (!needsQuotes) {
				return key;
			} else {
				return "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
			}
		}
	}

	private String escapePlainStringValue(final String value) {
		if (value.isEmpty()) {
			return "\"\"";
		} else {
			boolean needsQuotes = false;

			if (alwaysQuoteStringValues) {
				needsQuotes = true;
			} else {
				for (final char c : value.toCharArray()) {
					if ((Character.isWhitespace(c) && c != ' ')
							|| ":{}[],#&*!|>'\"%@`".indexOf(c) > -1) {
						needsQuotes = true;
						break;
					}
				}
			}

			if (!needsQuotes) {
				return value;
			} else {
				return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
			}
		}
	}

	private void writeBlockSequence(final YamlSequence yamlSequence, final int indentLevel) throws Exception {
		boolean isFirstData = true;
		for (final YamlNode item : yamlSequence.items()) {
			if (item.getLeadingComments() != null && !item.getLeadingComments().isEmpty()) {
				for (final String commentLine : item.getLeadingComments()) {
					writeIndent(indentLevel);
					write("# " + commentLine + "\n");
				}
			}

			if (!isFirstData) {
				writeIndent(indentLevel);
			}
			write("-");

			boolean startItemInNewLine = false;
			if (item.getInlineComment() != null) {
				write(" # " + item.getInlineComment());
				startItemInNewLine = true;
			}

			if (item instanceof final YamlScalar scalar
					&& scalar.getType() != YamlScalarType.MULTILINE_LITERAL
					&& scalar.getType() != YamlScalarType.MULTILINE_FOLDED
					&& scalar.getAnchorName() == null) {
				write(" ");
				writeScalarInlineInSequence(scalar);
			} else if (item instanceof final YamlAlias alias) {
				write(" ");
				write("*" + alias.getTargetAnchorName() + "\n");
			} else if (item instanceof final YamlMapping mapping) {
				if (!startItemInNewLine
						&& !mustStartInNewLine(mapping)) {
					write(" ");
					writeNode(mapping, indentLevel + 1, false, false);
				} else {
					write("\n");
					writeIndent(indentLevel + 1);
					writeNode(mapping, indentLevel + 1, false, false);
				}
			} else if (item instanceof final YamlSequence sequence) {
				if (!startItemInNewLine
						&& !mustStartInNewLine(sequence)) {
					write(" ");
					writeNode(sequence, indentLevel + 1, false, false);
				} else {
					write("\n");
					if (sequence.getLeadingComments() != null) {
						for (final String commentLine1 : sequence.getLeadingComments()) {
							writeIndent(indentLevel + 1);
							write("# " + commentLine1 + "\n");
						}
					}
					writeIndent(indentLevel + 1);
					writeNode(sequence, indentLevel + 1, false, false);
				}
			} else {
				throw new Exception("Unknown YAML node type: '" + item.getClass().getSimpleName() + "'");
			}

			isFirstData = false;
		}
	}

	private void writeScalarInlineInSequence(final YamlScalar scalar) throws IOException {
		final String inlineComment = scalar.getInlineComment();
		switch (scalar.getType()) {
			case BOOLEAN:
			case NUMBER:
			case NULL_VALUE:
				write(scalar.getValueString() + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : "") + "\n");
				break;
			case STRING:
				write(escapePlainStringValue(scalar.getValueString()) + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : "") + "\n");
				break;
			case MULTILINE_FOLDED:
			case MULTILINE_LITERAL:
			default:
				write(escapePlainStringValue(scalar.getValueString()) + "\n");
		}
	}

	private void writeBlockMapping(final YamlMapping yamlMapping, final int indentLevel) throws Exception {
		boolean isFirstData = true;
		for (final Entry<YamlNode, YamlNode> entry : yamlMapping.entrySet()) {
			final YamlNode key = entry.getKey();
			final YamlNode value = entry.getValue();

			if (key.getLeadingComments() != null) {
				if (isFirstData) {
					if (key.getLeadingComments() != null) {
						for (final String commentLine : key.getLeadingComments()) {
							write("# " + commentLine + "\n");
						}
					}
					isFirstData = false;
				} else {
					if (key.getLeadingComments() != null) {
						for (final String commentLine : key.getLeadingComments()) {
							writeIndent(indentLevel);
							write("# " + commentLine + "\n");
						}
					}
				}
			}

			if (key instanceof final YamlScalar scalarKey
					&& scalarKey.getType() == YamlScalarType.STRING
					&& scalarKey.getAnchorName() == null) {
				if (!isFirstData) {
					writeIndent(indentLevel);
					write(escapePlainStringKey(scalarKey.getValueString()));
					isFirstData = false;
				} else {
					write(escapePlainStringKey(scalarKey.getValueString()));
				}
			} else {
				writeIndent(indentLevel);
				write("?\n");
				writeNode(key, indentLevel + 1, false, false);
				writeIndent(indentLevel);
				write(":\n");
				writeNode(value, indentLevel + 1, false, false);
				continue;
			}

			write(":");

			boolean startValueInNewLine = false;
			if (key.getInlineComment() != null) {
				write(" # " + key.getInlineComment());
				startValueInNewLine = true;
			}

			if (value instanceof final YamlScalar scalar) {
				if (!startValueInNewLine
						&& scalar.getType() != YamlScalarType.MULTILINE_LITERAL
						&& scalar.getType() != YamlScalarType.MULTILINE_FOLDED
						&& scalar.getAnchorName() == null
						&& (scalar.getLeadingComments() == null || scalar.getLeadingComments().isEmpty())) {
					write(" ");
					final String inlineComment = scalar.getInlineComment();
					switch (scalar.getType()) {
						case BOOLEAN:
						case NUMBER:
						case NULL_VALUE:
							write(scalar.getValueString() + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : "") + "\n");
							break;
						case STRING:
							write(escapePlainStringValue(scalar.getValueString()) + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : "") + "\n");
							break;
						case MULTILINE_FOLDED:
						case MULTILINE_LITERAL:
						default:
							write(escapePlainStringValue(scalar.getValueString()) + "\n");
					}
				} else {
					write("\n");
					if (scalar.getLeadingComments() != null) {
						for (final String commentLine : scalar.getLeadingComments()) {
							writeIndent(indentLevel + 1);
							write("# " + commentLine + "\n");
						}
					}
					writeIndent(indentLevel + 1);
					writeNode(scalar, indentLevel + 1, false, false);
				}
			} else if (value instanceof final YamlAlias alias) {
				write(" *" + alias.getTargetAnchorName() + "\n");
			} else if (value instanceof final YamlMapping mapping) {
				write("\n");
				writeIndent(indentLevel + 1);
				writeNode(mapping, indentLevel + 1, false, false);
			} else if (value instanceof final YamlSequence sequence) {
				if (!startValueInNewLine
						&& sequence.getAnchorName() == null
						&& (sequence.getLeadingComments() == null || sequence.getLeadingComments().isEmpty())) {
					write("\n");
					writeIndent(indentLevel + 1);
					writeNode(sequence, indentLevel + 1, false, false);
				} else {
					write("\n");
					if (sequence.getLeadingComments() != null) {
						for (final String commentLine : sequence.getLeadingComments()) {
							writeIndent(indentLevel + 1);
							write("# " + commentLine + "\n");
						}
					}
					writeIndent(indentLevel + 1);
					writeNode(sequence, indentLevel + 1, false, false);
				}
			} else {
				throw new Exception("Unknown YAML node type: '" + value.getClass().getSimpleName() + "'");
			}

			isFirstData = false;
		}
	}

	private void writeFlowSequence(final YamlSequence yamlSequence, final int indentLevel) throws Exception {
		if (yamlSequence.getLeadingComments() != null && !yamlSequence.getLeadingComments().isEmpty()) {
			if (indentLevel > 0) {
				write("\n");
			}
			if (yamlSequence.getLeadingComments() != null) {
				for (final String commentLine : yamlSequence.getLeadingComments()) {
					writeIndent(indentLevel);
					write("# " + commentLine + "\n");
				}
			}

			writeIndent(indentLevel);
		} else {
			if (indentLevel > 0) {
				write(" ");
			}
		}
		write("[");

		boolean first = true;
		for (final YamlNode item : yamlSequence.items()) {
			if (!first) write(", ");
			first = false;

			if (item instanceof final YamlScalar scalar
					&& scalar.getType() != YamlScalarType.MULTILINE_LITERAL
					&& scalar.getType() != YamlScalarType.MULTILINE_FOLDED
					&& scalar.getAnchorName() == null
					&& (scalar.getLeadingComments() == null || scalar.getLeadingComments().isEmpty())) {
				writeScalarInlineInFlow(scalar);
			} else if (item instanceof final YamlAlias alias) {
				write("*" + alias.getTargetAnchorName());
			} else {
				writeNode(item, 0, true, false);
			}
		}

		write("]\n");
	}

	private void writeFlowMapping(final YamlMapping yamlMapping, final int indentLevel) throws Exception {
		if (!yamlMapping.getLeadingComments().isEmpty()) {
			if (indentLevel > 0) {
				write("\n");
			}
			if (yamlMapping.getLeadingComments() != null) {
				for (final String commentLine : yamlMapping.getLeadingComments()) {
					writeIndent(indentLevel);
					write("# " + commentLine + "\n");
				}
			}

			writeIndent(indentLevel);
		} else {
			if (indentLevel > 0) {
				write(" ");
			}
		}
		write("{");

		boolean first = true;
		for (final Entry<YamlNode, YamlNode> entry : yamlMapping.entrySet()) {
			if (!first) write(", ");
			first = false;

			final YamlNode key = entry.getKey();
			final YamlNode value = entry.getValue();

			if (key instanceof final YamlScalar scalarKey
					&& scalarKey.getType() == YamlScalarType.STRING
					&& scalarKey.getAnchorName() == null
					&& (key.getLeadingComments() == null || key.getLeadingComments().isEmpty())) {
				writeScalarInlineInFlow(scalarKey);
			} else {
				writeNode(key, 0, true, true);
			}

			write(": ");

			if (value instanceof final YamlScalar scalarVal
					&& scalarVal.getType() != YamlScalarType.MULTILINE_LITERAL
					&& scalarVal.getType() != YamlScalarType.MULTILINE_FOLDED
					&& scalarVal.getAnchorName() == null
					&& (scalarVal.getLeadingComments() == null || scalarVal.getLeadingComments().isEmpty())) {
				writeScalarInlineInFlow(scalarVal);
			} else if (value instanceof final YamlAlias aliasVal) {
				write("*" + aliasVal.getTargetAnchorName());
			} else {
				writeNode(value, 0, true, false);
			}
		}

		write("}\n");
	}

	private void writeScalarInlineInFlow(final YamlScalar scalar) throws IOException {
		final String inlineComment = scalar.getInlineComment();
		switch (scalar.getType()) {
			case BOOLEAN:
			case NUMBER:
			case NULL_VALUE:
				write(scalar.getValueString() + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : ""));
				break;
			case STRING:
				write(escapePlainStringValue(scalar.getValueString()) + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : ""));
				break;
			case MULTILINE_FOLDED:
			case MULTILINE_LITERAL:
			default:
				write(escapePlainStringValue(scalar.getValueString()));
		}
	}

	private static boolean mustStartInNewLine(final YamlMapping mapping) {
		if (mapping.getAnchorName() != null) {
			return true;
		} else {
			// Only check for first entry
			for (final Entry<YamlNode, YamlNode> entry : mapping.entrySet()) {
				if (entry.getKey().getLeadingComments() != null && !entry.getKey().getLeadingComments().isEmpty()) {
					return true;
				}
			}

			return false;
		}
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

	private void write(final String text) throws IOException {
		System.out.print(text); // TODO remove
		outputWriter.write(text);
	}
}
