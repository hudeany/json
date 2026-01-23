package de.soderer.yaml;

import java.io.Closeable;
import java.io.IOException;

import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlSequence;

public class YamlUtilities {
	public static String escapeScalarString(final String value) {
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

	public static void removeStringValueQuoteTypes(final YamlDocument document) {
		if (document != null && document.getRoot() != null) {
			if (document.getRoot() instanceof final YamlScalar scalar) {
				scalar.setQuoteType(null);
			} else if (document.getRoot() instanceof final YamlMapping mapping) {
				removeStringValueQuoteTypes(mapping);
			} else if (document.getRoot() instanceof final YamlSequence sequence) {
				removeStringValueQuoteTypes(sequence);
			}
		}
	}

	private static void removeStringValueQuoteTypes(final YamlSequence sequence) {
		for (final YamlNode item : sequence.items()) {
			if (item instanceof final YamlScalar scalar) {
				scalar.setQuoteType(null);
			} else if (item instanceof final YamlMapping subMapping) {
				removeStringValueQuoteTypes(subMapping);
			} else if (item instanceof final YamlSequence subSequence) {
				removeStringValueQuoteTypes(subSequence);
			}
		}
	}

	private static void removeStringValueQuoteTypes(final YamlMapping mapping) {
		for (final YamlNode value : mapping.values()) {
			if (value instanceof final YamlScalar scalar) {
				scalar.setQuoteType(null);
			} else if (value instanceof final YamlMapping subMapping) {
				removeStringValueQuoteTypes(subMapping);
			} else if (value instanceof final YamlSequence subSequence) {
				removeStringValueQuoteTypes(subSequence);
			}
		}
	}

	/**
	 * Close a Closable item and ignore any Exception thrown by its close method.
	 *
	 * @param closeableItem
	 *            the closeable item
	 */
	public static void closeQuietly(final Closeable closeableItem) {
		if (closeableItem != null) {
			try {
				closeableItem.close();
			} catch (@SuppressWarnings("unused") final IOException e) {
				// Do nothing
			}
		}
	}
}
