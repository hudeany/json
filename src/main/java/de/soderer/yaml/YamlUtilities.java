package de.soderer.yaml;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;

import de.soderer.json.JsonNode;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaConfiguration;
import de.soderer.json.schema.JsonSchemaVersion;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlSequence;

public class YamlUtilities {
	public static YamlDocument validateJsonSchemaOnYamlData(final InputStream yamlDataInputStream, final JsonSchemaVersion jsonSchemaVersion) throws Exception {
		final YamlDocument yamlDocument;
		try (YamlReader yamlReader = new YamlReader(yamlDataInputStream)) {
			yamlDocument = yamlReader.readDocument();
		}

		final JsonNode jsonNode = YamlToJsonConverter.convert(yamlDocument);

		JsonSchema jsonSchema;
		try (InputStream jsonSchemaInputStream = JsonSchema.class.getClassLoader().getResourceAsStream(jsonSchemaVersion.getLocalFile())) {
			jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration());
		}

		jsonSchema.validate(jsonNode);
		return yamlDocument;
	}

	public static YamlDocument validateJsonSchemaOnYamlDataSimple(final InputStream yamlDataInputStream, final InputStream jsonSchemaInputStream) throws Exception {
		final YamlDocument yamlDocument;
		try (YamlReader yamlReader = new YamlReader(yamlDataInputStream)) {
			yamlDocument = yamlReader.readDocument();
		}

		final JsonNode jsonNode = YamlToJsonConverter.convert(yamlDocument);

		final JsonSchema jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration());

		jsonSchema.validate(jsonNode);
		return yamlDocument;
	}

	public static YamlDocument validateJsonSchemaOnYamlDataV4(final InputStream yamlDataInputStream, final InputStream jsonSchemaInputStream) throws Exception {
		final YamlDocument yamlDocument;
		try (YamlReader yamlReader = new YamlReader(yamlDataInputStream)) {
			yamlDocument = yamlReader.readDocument();
		}

		final JsonNode jsonNode = YamlToJsonConverter.convert(yamlDocument);

		final JsonSchema jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration().setJsonSchemaVersion(JsonSchemaVersion.draftV4));

		jsonSchema.validate(jsonNode);
		return yamlDocument;
	}

	public static YamlDocument validateJsonSchemaOnYamlDataV6(final InputStream yamlDataInputStream, final InputStream jsonSchemaInputStream) throws Exception {
		final YamlDocument yamlDocument;
		try (YamlReader yamlReader = new YamlReader(yamlDataInputStream)) {
			yamlDocument = yamlReader.readDocument();
		}

		final JsonNode jsonNode = YamlToJsonConverter.convert(yamlDocument);

		final JsonSchema jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration().setJsonSchemaVersion(JsonSchemaVersion.draftV6));

		jsonSchema.validate(jsonNode);
		return yamlDocument;
	}

	public static YamlDocument validateJsonSchemaOnYamlDataV7(final InputStream yamlDataInputStream, final InputStream jsonSchemaInputStream) throws Exception {
		final YamlDocument yamlDocument;
		try (YamlReader yamlReader = new YamlReader(yamlDataInputStream)) {
			yamlDocument = yamlReader.readDocument();
		}

		final JsonNode jsonNode = YamlToJsonConverter.convert(yamlDocument);

		final JsonSchema jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration().setJsonSchemaVersion(JsonSchemaVersion.draftV7));

		jsonSchema.validate(jsonNode);
		return yamlDocument;
	}

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

	public static void removeStringKeyQuoteTypes(final YamlDocument document) {
		if (document != null && document.getRoot() != null) {
			if (document.getRoot() instanceof final YamlScalar scalar) {
				scalar.setQuoteType(null);
			} else if (document.getRoot() instanceof final YamlMapping mapping) {
				removeStringKeyQuoteTypes(mapping);
			} else if (document.getRoot() instanceof final YamlSequence sequence) {
				removeStringKeyQuoteTypes(sequence);
			}
		}
	}

	private static void removeStringKeyQuoteTypes(final YamlSequence sequence) {
		for (final YamlNode item : sequence.items()) {
			if (item instanceof final YamlScalar scalar) {
				scalar.setQuoteType(null);
			} else if (item instanceof final YamlMapping subMapping) {
				removeStringKeyQuoteTypes(subMapping);
			} else if (item instanceof final YamlSequence subSequence) {
				removeStringKeyQuoteTypes(subSequence);
			}
		}
	}

	private static void removeStringKeyQuoteTypes(final YamlMapping mapping) {
		for (final Entry<YamlNode, YamlNode> entry : mapping.entrySet()) {
			if (entry.getKey() != null) {
				if (entry.getKey() instanceof final YamlScalar scalar) {
					scalar.setQuoteType(null);
				} else if (entry.getKey() instanceof final YamlMapping subMapping) {
					removeStringKeyQuoteTypes(subMapping);
				} else if (entry.getKey() instanceof final YamlSequence subSequence) {
					removeStringKeyQuoteTypes(subSequence);
				}
			}

			if (entry.getValue() != null) {
				if (entry.getValue() instanceof final YamlMapping subMapping) {
					removeStringKeyQuoteTypes(subMapping);
				} else if (entry.getValue() instanceof final YamlSequence subSequence) {
					removeStringKeyQuoteTypes(subSequence);
				}
			}
		}
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
		for (final Entry<YamlNode, YamlNode> entry : mapping.entrySet()) {
			if (entry.getKey() != null) {
				if (entry.getKey() instanceof final YamlMapping subMapping) {
					removeStringValueQuoteTypes(subMapping);
				} else if (entry.getKey() instanceof final YamlSequence subSequence) {
					removeStringValueQuoteTypes(subSequence);
				}
			}

			if (entry.getValue() != null) {
				if (entry.getValue() instanceof final YamlScalar scalar) {
					scalar.setQuoteType(null);
				} else if (entry.getValue() instanceof final YamlMapping subMapping) {
					removeStringValueQuoteTypes(subMapping);
				} else if (entry.getValue() instanceof final YamlSequence subSequence) {
					removeStringValueQuoteTypes(subSequence);
				}
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
