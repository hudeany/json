package de.soderer.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.JsonToYamlConverter;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueNull;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueString;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaVersion;
import de.soderer.json.utilities.Linebreak;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.data.YamlStringQuoteType;

/**
 * Reads various YAML constructs, writes them with different formats and checks that reading the
 * output again results in the same data. Also covers the YAML/JSON converters and
 * {@link YamlUtilities}.
 */
@SuppressWarnings("static-method")
class YamlRoundtripCoverageTest {
	private static InputStream stream(final String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
	}

	private static String write(final YamlDocument document, final YamlFormat format) throws Exception {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (YamlWriter writer = new YamlWriter(output, StandardCharsets.UTF_8, format)) {
			writer.writeDocument(document);
		}
		return new String(output.toByteArray(), StandardCharsets.UTF_8);
	}

	private static void assertRoundtrip(final String yaml, final YamlFormat format) throws Exception {
		final YamlDocument document = YamlReader.readDocument(yaml);
		final String written = write(document, format);
		final YamlDocument reread = YamlReader.readDocument(written);
		assertEquals(document.getRoot(), reread.getRoot(), "Roundtrip failed for:\n" + yaml + "\nWritten:\n" + written);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{a: 1, \"b\": 'two', c: [1, 2, {d: e}], e: {f: null}, g: ~, h: true, empty: {}, emptySeq: []}\n",
			"[a, \"b c\", 'd''e', 1.5, -3, null, true, [nested, list], {k: v}]\n",
			"base: &base\n  x: 1\n  y: 2\nderived:\n  <<: *base\n  y: 3\nlist:\n  - &item one\n  - *item\n",
			"s: !!str 123\nf: !!float 1\nm: !!map {a: 1}\nq: !!seq [1, 2]\nb: !!binary aGk=\n",
			"literal: |\n  line1\n  line2\nfolded: >-\n  folded\n  text\nkeep: |+\n  kept\n\nstrip: |-\n  stripped\nindent: |2\n   indented\n",
			"%YAML 1.2\n%TAG !e! tag:example.com,2000:\n---\n# leading comment\nkey: value # inline\n...\n",
			"? [a, b]\n: value\n? {x: 1}\n: other\n",
			"a: \"tab\\there \\u00e4 \\x41\"\nb: 'single ''quote'''\n",
			"hex: 0x1F\noct: 0o17\ninf: .inf\nninf: -.inf\nnan: .nan\nexp: 1e3\n",
			"- - a\n  - b\n- k: v\n  l: w\n- null\n",
			"seq:\n- a\n- b\nmap:\n  x: [1, {k: [2, 3]}]\n",
			"{a: {b: {c: [1, [2, [3]]]}}, 'q k': \"v, w\", x: 'a: b'}\n",
			"[{a: 1, b: 2}, [x, z], 'str', \"dq\", ~, {}, []]\n",
			"k: # comment\n  v: 1\n",
			"{ a: 1 , b : 2 }\n",
			"[ 1 , 2 ]\n",
			"key: [a, b] # c\nkey2: {x: 1} # d\n",
			"'quoted key': 1\n\"dq key\": 2\n",
			"date: 2024-01-02\nver: 1.2.3\ncolon: a:b\n",
			"# document comment\n\nfirst: 1\n\n# property comment\nsecond:\n  - a # item comment\n  - b\nthird: {}\nfourth: []\n"
	})
	void roundtrip_defaultAndCustomFormats(final String yaml) throws Exception {
		assertRoundtrip(yaml, new YamlFormat());
		assertRoundtrip(yaml, new YamlFormat()
				.withIndentationSize(4)
				.withOmitComments(true)
				.withOmitEmptyLines(true)
				.withUseNoExtraIndentationForYamlSequencesInYamlMappingProperties(true)
				.withUseNoNewLineForEmptyYamlObjectInYamlMappingProperties(true));
		assertRoundtrip(yaml, new YamlFormat().withIgnoreFlowStyleSettings(true).withLinebreak(Linebreak.Windows));
		assertRoundtrip(yaml, new YamlFormat().withAlwaysQuoteStringKeys(true).withAlwaysQuoteStringValues(true).withStringValueQuoteType(YamlStringQuoteType.SINGLE));
		assertRoundtrip(yaml, new YamlFormat().withAlwaysQuoteAllStrings().withStringValueQuoteType(YamlStringQuoteType.DOUBLE));
	}

	@Test
	void yamlFormat_settings() {
		final YamlFormat format = new YamlFormat();
		assertThrows(IllegalArgumentException.class, () -> format.setLinebreak(null));
		assertEquals(Linebreak.Unix, format.getLinebreak());
		format.setLinebreak(Linebreak.Mac);
		assertEquals("\r", format.getLinebreakString());
		format.setStringValueQuoteType(null);
		format.setAlwaysQuoteStringKeys(true);
		assertTrue(format.isAlwaysQuoteStringKeys());
		format.setAlwaysQuoteStringValues(true);
		assertTrue(format.isAlwaysQuoteStringValues());
		format.setOmitEmptyLines(true);
		assertTrue(format.isOmitEmptyLines());
		format.setUseNoExtraIndentationForYamlSequencesInYamlMappingProperties(true);
		assertTrue(format.isUseNoExtraIndentationForYamlSequencesInYamlMappingProperties());
		format.setUseNoNewLineForEmptyYamlObjectInYamlMappingProperties(true);
		assertTrue(format.isUseNoNewLineForEmptyYamlObjectInYamlMappingProperties());
	}

	@Test
	void reader_unsupportedTagsAndAnchors() throws Exception {
		assertThrows(Exception.class, () -> YamlReader.readDocument("a: !!unknownTag 5\n"));
		assertThrows(Exception.class, () -> YamlReader.readDocument("a: !!seq\n  b: 1\n"));
		assertThrows(Exception.class, () -> YamlReader.readDocument("a: !!str\n  b: 1\n"));
		assertThrows(Exception.class, () -> YamlReader.readDocument("a: !!map x\n"));
		assertThrows(Exception.class, () -> YamlReader.readDocument("a: !!seq x\n"));

		try (YamlReader reader = new YamlReader(stream("base: &anchor\n  x: 1\nref: *anchor\n"))) {
			reader.readDocument();
			assertInstanceOf(YamlMapping.class, reader.getYamlNodeByAnchor("anchor"));
			assertThrows(Exception.class, () -> reader.getYamlNodeByAnchor("missing"));
		}
	}

	@Test
	void writer_streamingSequenceItems() throws Exception {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (YamlWriter writer = new YamlWriter(output, new YamlFormat())) {
			assertEquals(StandardCharsets.UTF_8, writer.getEncoding());
			writer.addSequenceItem("text");
			writer.addSequenceItem(Integer.valueOf(1));
			writer.addSequenceItem(Boolean.TRUE);
			writer.addSequenceItem((Object) null);
			writer.addSequenceItem(new YamlMapping().add("k", "v").add("list", new YamlSequence().add("x")));
			writer.addSequenceItem(new YamlSequence().add("nested"));
			assertThrows(Exception.class, () -> writer.addSequenceItem(new Object()));
		}
		final YamlSequence sequence = (YamlSequence) YamlReader.readDocument(new String(output.toByteArray(), StandardCharsets.UTF_8)).getRoot();
		assertEquals(6, sequence.size());
		assertEquals("text", ((YamlScalar) sequence.get(0)).getValue());
		assertInstanceOf(YamlMapping.class, sequence.get(4));
	}

	@Test
	void writer_documentListAndToString() throws Exception {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (YamlWriter writer = new YamlWriter(output, StandardCharsets.UTF_8)) {
			writer.writeDocumentList(List.of(new YamlDocument(new YamlMapping().add("a", "b")), new YamlDocument(new YamlSequence().add("c")), new YamlDocument(new YamlScalar("plain"))));
		}
		final String text = new String(output.toByteArray(), StandardCharsets.UTF_8);
		assertTrue(text.contains("---"), text);
		assertTrue(text.contains("plain"), text);

		assertTrue(YamlWriter.toString(new YamlSequence().withFlowStyle(true).add("a")).contains("[a]"));
		assertTrue(YamlWriter.toString(new YamlSequence().add("a")).contains("- a"));
		assertTrue(YamlWriter.toString(new YamlMapping().withFlowStyle(true).add("k", "v")).contains("{k: v}"));
		assertThrows(IllegalStateException.class, () -> new YamlWriter(null, (YamlFormat) null));
	}

	@Test
	void writer_scalarStyles() throws Exception {
		final YamlMapping mapping = new YamlMapping()
				.add("multi", new YamlScalar("line1\nline2", YamlScalarType.STRING))
				.add("special", new YamlScalar("a: b # c", YamlScalarType.STRING))
				.add("leadingSpace", new YamlScalar(" x", YamlScalarType.STRING))
				.add("numberLike", new YamlScalar("123", YamlScalarType.STRING))
				.add("boolLike", new YamlScalar("true", YamlScalarType.STRING))
				.add("empty", new YamlScalar("", YamlScalarType.STRING))
				.add("control", new YamlScalar("bell\u0007", YamlScalarType.STRING))
				.add("quote", new YamlScalar("it's \"quoted\"", YamlScalarType.STRING))
				.add("bigInteger", new YamlScalar(new BigInteger("123456789012345678901234567890"), YamlScalarType.NUMBER))
				.add("bigDecimal", new YamlScalar(new BigDecimal("1.25"), YamlScalarType.NUMBER));
		final String written = YamlWriter.toString(new YamlDocument(mapping));
		final YamlMapping reread = (YamlMapping) YamlReader.readDocument(written).getRoot();
		assertEquals("line1\nline2", reread.getSimpleValue("multi"));
		assertEquals("a: b # c", reread.getSimpleValue("special"));
		assertEquals(" x", reread.getSimpleValue("leadingSpace"));
		assertEquals("123", reread.getSimpleValue("numberLike"));
		assertEquals("true", reread.getSimpleValue("boolLike"));
		assertEquals("", reread.getSimpleValue("empty"));
		assertEquals("bell\u0007", reread.getSimpleValue("control"));
		assertEquals("it's \"quoted\"", reread.getSimpleValue("quote"));
		assertEquals(0, new BigInteger("123456789012345678901234567890").compareTo(new BigInteger(reread.getSimpleValue("bigInteger").toString())));
	}

	@Test
	void yamlToJsonConverter() throws Exception {
		final YamlDocument document = YamlReader.readDocument("base: &base\n  x: 1\n  p: 2\nderived:\n  <<: *base\n  p: 3\nmulti:\n  <<:\n    - *base\n    - {z: 4}\nbig: 123456789012345678901234567890\nfloat: 1.5\nflag: true\nnothing: ~\ntext: |\n  block\n1: numberKey\ntrue: boolKey\n");
		final JsonObject json = (JsonObject) YamlToJsonConverter.convert(document);
		final JsonObject derived = (JsonObject) json.get("derived");
		assertEquals(1, ((Number) derived.getSimpleValue("x")).intValue());
		assertEquals(3, ((Number) derived.getSimpleValue("p")).intValue());
		final JsonObject multi = (JsonObject) json.get("multi");
		assertEquals(4, ((Number) multi.getSimpleValue("z")).intValue());
		assertTrue(json.get("big").isInteger());
		assertTrue(json.get("float").isNumber());
		assertTrue(json.get("flag").isBoolean());
		assertTrue(json.get("nothing").isNull());
		assertEquals("block\n", json.getSimpleValue("text"));
		assertEquals("numberKey", json.getSimpleValue("1"));
		assertEquals("boolKey", json.getSimpleValue("true"));

		assertNull(YamlToJsonConverter.convert((YamlDocument) null));
		assertNull(YamlToJsonConverter.convert(new YamlDocument()));
		assertNull(YamlToJsonConverter.convert((YamlNode) null));
		assertNull(YamlToJsonConverter.convert((YamlScalar) null));
		assertTrue(YamlToJsonConverter.convert(new YamlSequence().add("x")).size() == 1);
		assertTrue(YamlToJsonConverter.convert(new YamlMapping().add("k", "v")).containsKey("k"));
		assertTrue(YamlToJsonConverter.convert(new YamlScalar(Long.valueOf(5L))).isInteger());
		assertTrue(YamlToJsonConverter.convert(new YamlScalar(new BigDecimal("12"))).isInteger());
		assertTrue(YamlToJsonConverter.convert(new YamlScalar(new BigDecimal("1.5"))).isNumber());
		assertTrue(YamlToJsonConverter.convert(new YamlScalar("x", YamlScalarType.MULTILINE)).isString());
	}

	@Test
	void jsonToYamlConverter() throws Exception {
		assertNull(JsonToYamlConverter.convert((JsonNode) null));
		assertNull(JsonToYamlConverter.convert((JsonObject) null));
		assertNull(JsonToYamlConverter.convert((JsonArray) null));
		assertNull(((YamlScalar) JsonToYamlConverter.convert(new JsonValueNull())).getValue());
		assertEquals("s", ((YamlScalar) JsonToYamlConverter.convert(new JsonValueString("s"))).getValue());
		assertEquals(1, ((Number) ((YamlScalar) JsonToYamlConverter.convert(new JsonValueInteger(1))).getValue()).intValue());
		assertEquals(1.5, ((Number) ((YamlScalar) JsonToYamlConverter.convert(new JsonValueNumber(1.5))).getValue()).doubleValue());
		assertEquals(Boolean.TRUE, ((YamlScalar) JsonToYamlConverter.convert(new JsonValueBoolean(Boolean.TRUE))).getValue());

		final JsonNode json = JsonReader.readJsonItemString("{\"o\": {\"a\": 1}, \"l\": [1, 1.5, \"s\", null, true, {\"x\": 1}, [2]], \"n\": null, \"s\": \"t\", \"i\": 2, \"d\": 2.5, \"b\": false}");
		final YamlMapping mapping = (YamlMapping) JsonToYamlConverter.convert(json);
		assertEquals(7, mapping.size());
		assertEquals(7, ((YamlSequence) mapping.get("l")).size());
		assertEquals(json, YamlToJsonConverter.convert(mapping));
	}

	@Test
	void yamlUtilities_validateAgainstSchema() throws Exception {
		final String schema = "{\"type\": \"object\", \"properties\": {\"a\": {\"type\": \"integer\"}}}";
		assertEquals(1, ((Number) ((YamlMapping) YamlUtilities.validateJsonSchemaOnYamlDataSimple(stream("a: 1\n"), stream(schema)).getRoot()).getSimpleValue("a")).intValue());
		assertTrue(YamlUtilities.validateJsonSchemaOnYamlDataV4(stream("a: 1\n"), stream(schema)).getRoot() instanceof YamlMapping);
		assertTrue(YamlUtilities.validateJsonSchemaOnYamlDataV6(stream("a: 1\n"), stream(schema)).getRoot() instanceof YamlMapping);
		assertTrue(YamlUtilities.validateJsonSchemaOnYamlDataV7(stream("a: 1\n"), stream(schema)).getRoot() instanceof YamlMapping);
		assertThrows(JsonSchemaDataValidationError.class, () -> YamlUtilities.validateJsonSchemaOnYamlDataV7(stream("a: text\n"), stream(schema)));

		// YAML data checked against the bundled meta schema, i.e. the YAML data is a JSON schema itself
		assertTrue(YamlUtilities.validateJsonSchemaOnYamlData(stream("type: object\nrequired: [a]\n"), JsonSchemaVersion.draftV7).getRoot() instanceof YamlMapping);
		assertThrows(JsonSchemaDataValidationError.class, () -> YamlUtilities.validateJsonSchemaOnYamlData(stream("type: 5\n"), JsonSchemaVersion.draftV7));
		assertThrows(IllegalArgumentException.class, () -> YamlUtilities.validateJsonSchemaOnYamlData(stream("a: 1\n"), JsonSchemaVersion.simple));
		assertThrows(IllegalArgumentException.class, () -> YamlUtilities.validateJsonSchemaOnYamlData(stream("a: 1\n"), null));
	}

	@Test
	void yamlUtilities_escapeAndRemoveQuoteTypes() throws Exception {
		assertEquals("\\\\\\\"\\n\\r\\t\\b\\f\\u00A0\\u0085\\u2028\\u2029\\u0007\\u0080\\uFEFFa", YamlUtilities.escapeScalarString("\\\"\n\r\t\b\f\u00a0\u0085\u2028\u2029\u0007\u0080\uFEFFa"));

		final YamlDocument mappingDocument = YamlReader.readDocument("'k1': 'v1'\n\"k2\":\n  - 'item'\n  - ['nested']\n  - {'fk': \"fv\"}\n? ['complex']\n: {'ck': 'cv'}\n");
		YamlUtilities.removeStringKeyQuoteTypes(mappingDocument);
		YamlUtilities.removeStringValueQuoteTypes(mappingDocument);
		final String written = YamlWriter.toString(mappingDocument);
		assertFalse(written.contains("'k1'"), written);
		assertFalse(written.contains("'v1'"), written);
		assertEquals(mappingDocument.getRoot(), YamlReader.readDocument(written).getRoot());

		final YamlDocument sequenceDocument = YamlReader.readDocument("- 'a'\n- ['b']\n- {'c': 'd'}\n");
		YamlUtilities.removeStringKeyQuoteTypes(sequenceDocument);
		YamlUtilities.removeStringValueQuoteTypes(sequenceDocument);
		assertFalse(YamlWriter.toString(sequenceDocument).contains("'"));

		final YamlDocument scalarDocument = new YamlDocument(new YamlScalar("x", YamlScalarType.STRING).withQuoteType(YamlStringQuoteType.DOUBLE));
		YamlUtilities.removeStringKeyQuoteTypes(scalarDocument);
		YamlUtilities.removeStringValueQuoteTypes(scalarDocument);
		assertNull(((YamlScalar) scalarDocument.getRoot()).getQuoteType());

		YamlUtilities.removeStringKeyQuoteTypes(null);
		YamlUtilities.removeStringValueQuoteTypes(null);
		YamlUtilities.removeStringKeyQuoteTypes(new YamlDocument());
		YamlUtilities.removeStringValueQuoteTypes(new YamlDocument());
		YamlUtilities.closeQuietly(null);
		YamlUtilities.closeQuietly(() -> {
			throw new java.io.IOException("ignored");
		});
	}
}
