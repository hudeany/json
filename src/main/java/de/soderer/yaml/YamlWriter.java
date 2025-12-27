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

import de.soderer.json.utilities.Utilities;
import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlKeyValue;
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
	private boolean alwaysQuoteStringValues = false; // TODO

	private BufferedWriter outputWriter = null;
	private long charactersWritten = 0;
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

	public long getCharactersWritten() {
		return charactersWritten;
	}

	public void writeDocument(final YamlDocument document) throws IOException {
		if (!firstDocument) {
			write("...\n");
		}

		if (document.getDirectives().size() > 0) {
			for (final YamlDirective<?> directive : document.getDirectives()) {
				write(directive.toString() + "\n");
			}
			write("---");
		}

		for (final String c : document.getLeadingComments()) {
			write("# ");
			write(c);
			write("\n");
		}

		if (document.getRoot() != null) {
			writeNode(document.getRoot(), 0, false, false);
		}

		outputWriter.flush();
		firstDocument = false;
	}

	public void writeDocumentList(final List<YamlDocument> documentList) throws IOException {
		for (int i = 0; i < documentList.size(); i++) {
			final YamlDocument document = documentList.get(i);
			if (!firstDocument) {
				write("...\n");
			}
			writeDocument(document);
			firstDocument = false;
		}
	}

	private void writeNode(final YamlNode node, final int indentLevel, final boolean inFlow, final boolean isKeyContext) throws IOException {
		if (node.getAnchorName() != null && !(node instanceof YamlAlias)) {
			if (charactersWritten > 0) {
				write(" ");
			}
			write("&" + node.getAnchorName());
		}

		if (node instanceof final YamlScalar scalar) {
			writeLeadingComments(node, indentLevel);

			writeScalar(scalar, indentLevel, inFlow, isKeyContext);
		} else if (node instanceof final YamlAlias alias) {
			writeLeadingComments(node, indentLevel);

			writeAlias(alias, indentLevel);
		} else if (node instanceof final YamlSequence seq) {
			if (seq.isFlowStyle() || inFlow) {
				writeFlowSequence(seq, indentLevel);
			} else {
				if (charactersWritten > 0) {
					write("\n");
				}
				writeLeadingComments(node, indentLevel);

				writeBlockSequence(seq, indentLevel);
			}
		} else if (node instanceof final YamlMapping map) {
			if (map.isFlowStyle() || inFlow) {
				writeFlowMapping(map, indentLevel);
			} else {
				if (charactersWritten > 0) {
					write("\n");
				}
				writeLeadingComments(node, indentLevel);

				writeBlockMapping(map, indentLevel);
			}
		} else {
			throw new IllegalStateException("Unbekannter Node-Typ: " + node.getClass());
		}
	}

	private void writeLeadingComments(final YamlNode node, final int indentLevel) throws IOException {
		for (final String commentLine : node.getLeadingComments()) {
			writeIndent(indentLevel);
			write("# " + commentLine + "\n");
		}
	}

	private void writeIndent(final int indentLevel) throws IOException {
		if (indentLevel > 0) {
			write(" ".repeat(indentLevel * indentSize));
		}
	}

	private void writeScalar(final YamlScalar scalar, final int indentLevel, final boolean inFlow, final boolean isKeyContext) throws IOException {
		final YamlScalarType type = scalar.getType();
		final String value = scalar.getValue();
		final String inlineComment = Utilities.join(scalar.getInlineComments(), " ");

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
				write(value + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : "") + "\n");
				break;
			case STRING:
				if (inFlow || isKeyContext) {
					writeIndent(indentLevel);
					write(escapePlainString(value));
				} else {
					writeIndent(indentLevel);
					write(escapePlainString(value) + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : "") + "\n");
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
		if (charactersWritten > 0) {
			write(" ");
		}
		write("|\n");

		for (final String line : scalar.getValue().split("\n", -1)) {
			writeIndent(indentLevel);
			write(line + "\n");
		}
	}

	private void writeMultilineFolded(final YamlScalar scalar, final int indentLevel) throws IOException {
		if (charactersWritten > 0) {
			write(" ");
		}
		write(">\n");

		for (final String line : scalar.getValue().split("\n", -1)) {
			writeIndent(indentLevel);
			write(line + "\n");
		}
	}

	private String escapePlainString(final String value) {
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

	private void writeBlockSequence(final YamlSequence seq, final int indentLevel) throws IOException {
		for (final YamlNode item : seq.getItems()) {
			writeLeadingComments(item, indentLevel);

			writeIndent(indentLevel);
			write("-");

			if (item instanceof final YamlScalar scalar
					&& scalar.getType() != YamlScalarType.MULTILINE_LITERAL
					&& scalar.getType() != YamlScalarType.MULTILINE_FOLDED
					&& scalar.getAnchorName() == null) {

				write(" ");
				writeScalarInlineInSequence(scalar);
			} else if (item instanceof final YamlAlias alias) {
				write(" *" + alias.getTargetAnchorName() + "\n");
			} else {
				writeNode(item, indentLevel + 1, false, false);
			}
		}
	}

	private void writeScalarInlineInSequence(final YamlScalar scalar) throws IOException {
		final String inlineComment = Utilities.join(scalar.getInlineComments(), " ");
		switch (scalar.getType()) {
			case BOOLEAN:
			case NUMBER:
			case NULL_VALUE:
				write(scalar.getValue() + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : "") + "\n");
				break;
			case STRING:
				write(escapePlainString(scalar.getValue()) + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : "") + "\n");
				break;
			case MULTILINE_FOLDED:
			case MULTILINE_LITERAL:
			default:
				write(escapePlainString(scalar.getValue()) + "\n");
		}
	}

	private void writeBlockMapping(final YamlMapping map, final int indentLevel) throws IOException {
		for (final YamlKeyValue kv : map.getEntries()) {
			final YamlNode key = kv.getKey();
			final YamlNode value = kv.getValue();

			writeLeadingComments(key, indentLevel);

			if (key instanceof final YamlScalar scalarKey
					&& scalarKey.getType() == YamlScalarType.STRING
					&& scalarKey.getAnchorName() == null) {
				writeIndent(indentLevel);
				write(escapePlainString(scalarKey.getValue()));
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

			if (value instanceof final YamlScalar scalarVal
					&& scalarVal.getType() != YamlScalarType.MULTILINE_LITERAL
					&& scalarVal.getType() != YamlScalarType.MULTILINE_FOLDED
					&& scalarVal.getAnchorName() == null
					&& scalarVal.getLeadingComments().isEmpty()) {
				write(" ");
				writeScalarInlineInMappingValue(scalarVal);
			} else if (value instanceof final YamlAlias aliasVal) {
				write(" *" + aliasVal.getTargetAnchorName() + "\n");
			} else {
				writeNode(value, indentLevel + 1, false, false);
			}
		}
	}

	private void writeScalarInlineInMappingValue(final YamlScalar scalar) throws IOException {
		final String inlineComment = Utilities.join(scalar.getInlineComments(), " ");
		switch (scalar.getType()) {
			case BOOLEAN:
			case NUMBER:
			case NULL_VALUE:
				write(scalar.getValue() + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : "") + "\n");
				break;
			case STRING:
				write(escapePlainString(scalar.getValue()) + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : "") + "\n");
				break;
			case MULTILINE_FOLDED:
			case MULTILINE_LITERAL:
			default:
				write(escapePlainString(scalar.getValue()) + "\n");
		}
	}

	private void writeFlowSequence(final YamlSequence seq, final int indentLevel) throws IOException {
		if (!seq.getLeadingComments().isEmpty()) {
			if (charactersWritten > 0) {
				write("\n");
			}
			writeLeadingComments(seq, indentLevel);

			writeIndent(indentLevel);
		} else {
			if (charactersWritten > 0) {
				write(" ");
			}
		}
		write("[");

		boolean first = true;
		for (final YamlNode item : seq.getItems()) {
			if (!first) write(", ");
			first = false;

			if (item instanceof final YamlScalar scalar
					&& scalar.getType() != YamlScalarType.MULTILINE_LITERAL
					&& scalar.getType() != YamlScalarType.MULTILINE_FOLDED
					&& scalar.getAnchorName() == null
					&& scalar.getLeadingComments().isEmpty()) {
				writeScalarInlineInFlow(scalar);
			} else if (item instanceof final YamlAlias alias) {
				write("*" + alias.getTargetAnchorName());
			} else {
				writeNode(item, 0, true, false);
			}
		}

		write("]\n");
	}

	private void writeFlowMapping(final YamlMapping map, final int indentLevel) throws IOException {
		if (!map.getLeadingComments().isEmpty()) {
			if (charactersWritten > 0) {
				write("\n");
			}
			writeLeadingComments(map, indentLevel);

			writeIndent(indentLevel);
		} else {
			if (charactersWritten > 0) {
				write(" ");
			}
		}
		write("{");

		boolean first = true;
		for (final YamlKeyValue kv : map.getEntries()) {
			if (!first) write(", ");
			first = false;

			final YamlNode key = kv.getKey();
			final YamlNode value = kv.getValue();

			if (key instanceof final YamlScalar scalarKey
					&& scalarKey.getType() == YamlScalarType.STRING
					&& scalarKey.getAnchorName() == null
					&& key.getLeadingComments().isEmpty()) {
				writeScalarInlineInFlow(scalarKey);
			} else {
				writeNode(key, 0, true, true);
			}

			write(": ");

			if (value instanceof final YamlScalar scalarVal
					&& scalarVal.getType() != YamlScalarType.MULTILINE_LITERAL
					&& scalarVal.getType() != YamlScalarType.MULTILINE_FOLDED
					&& scalarVal.getAnchorName() == null
					&& scalarVal.getLeadingComments().isEmpty()) {
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
		final String inlineComment = Utilities.join(scalar.getInlineComments(), " ");
		switch (scalar.getType()) {
			case BOOLEAN:
			case NUMBER:
			case NULL_VALUE:
				write(scalar.getValue() + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : ""));
				break;
			case STRING:
				write(escapePlainString(scalar.getValue()) + (Utilities.isNotBlank(inlineComment) ? " # " + inlineComment : ""));
				break;
			case MULTILINE_FOLDED:
			case MULTILINE_LITERAL:
			default:
				write(escapePlainString(scalar.getValue()));
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
		outputWriter.write(text);
		charactersWritten += text.length();
	}
}
