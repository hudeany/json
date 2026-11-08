package de.soderer.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonWriter;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.data.YamlVersion;
import de.soderer.yaml.exception.YamlParseException;

/**
 * Tests the resolution of plain scalars by YAML version, the handling of tags and the JSON output of
 * non-finite numbers.
 */
@SuppressWarnings("static-method")
public class YamlVersionAndTagTest {
	private static InputStream toStream(final String data) {
		return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
	}

	private static YamlNode readRoot(final String yaml, final YamlVersion defaultYamlVersion) throws Exception {
		try (YamlReader reader = new YamlReader(toStream(yaml))) {
			if (defaultYamlVersion != null) {
				reader.setDefaultYamlVersion(defaultYamlVersion);
			}
			return reader.readDocument().getRoot();
		}
	}

	private static YamlScalar readValue(final String plainScalar, final YamlVersion defaultYamlVersion) throws Exception {
		return (YamlScalar) ((YamlMapping) readRoot("v: " + plainScalar + "\n", defaultYamlVersion)).get("v");
	}

	private static String toJsonString(final JsonNode jsonNode) {
		return jsonNode.toString().replaceAll("\\s+", "");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			// plain scalar | type in YAML 1.2 | type in YAML 1.1
			"true     | BOOLEAN    | BOOLEAN",
			"True     | BOOLEAN    | BOOLEAN",
			"TRUE     | BOOLEAN    | BOOLEAN",
			"tRUE     | STRING     | STRING",
			"yes      | STRING     | BOOLEAN",
			"Yes      | STRING     | BOOLEAN",
			"yEs      | STRING     | STRING",
			"on       | STRING     | BOOLEAN",
			"OFF      | STRING     | BOOLEAN",
			"y        | STRING     | BOOLEAN",
			"N        | STRING     | BOOLEAN",
			"null     | NULL_VALUE | NULL_VALUE",
			"Null     | NULL_VALUE | NULL_VALUE",
			"~        | NULL_VALUE | NULL_VALUE",
			"nULL     | STRING     | STRING",
			"17       | NUMBER     | NUMBER",
			"0o17     | NUMBER     | NUMBER",
			"0x1F     | NUMBER     | NUMBER",
			"1_000    | STRING     | NUMBER",
			"0b101    | STRING     | NUMBER",
			"1.5e3    | NUMBER     | NUMBER",
			".inf     | NUMBER     | NUMBER",
			"-.Inf    | NUMBER     | NUMBER",
			".nan     | NUMBER     | NUMBER",
			".Nan     | STRING     | NUMBER",
			"12:30    | STRING     | STRING",
			"text     | STRING     | STRING"
	})
	void plainScalarTypes(final String plainScalar, final YamlScalarType typeV12, final YamlScalarType typeV11) throws Exception {
		assertEquals(typeV12, readValue(plainScalar, null).getType(), "YAML 1.2: " + plainScalar);
		assertEquals(typeV12, readValue(plainScalar, YamlVersion.V1_2).getType(), "YAML 1.2: " + plainScalar);
		assertEquals(typeV11, readValue(plainScalar, YamlVersion.V1_1).getType(), "YAML 1.1: " + plainScalar);
	}

	@Test
	void leadingZeroNumbers() throws Exception {
		assertEquals(17, ((Number) readValue("017", YamlVersion.V1_2).getValue()).intValue());
		assertEquals(-17, ((Number) readValue("-017", YamlVersion.V1_2).getValue()).intValue());
		assertEquals(15, ((Number) readValue("017", YamlVersion.V1_1).getValue()).intValue());
		assertEquals(15, ((Number) readValue("0o17", YamlVersion.V1_2).getValue()).intValue());
		// The text as written is kept for writing
		assertEquals("017", readValue("017", YamlVersion.V1_2).getValueString());
	}

	@Test
	void defaultIsYaml12() throws Exception {
		try (YamlReader reader = new YamlReader(toStream("a: 1\n"))) {
			assertEquals(YamlVersion.V1_2, reader.getDefaultYamlVersion());
		}
		assertEquals("yes", readValue("yes", null).getValue());
	}

	@Test
	void versionDirectiveTakesPrecedence() throws Exception {
		final YamlMapping v11 = (YamlMapping) readRoot("%YAML 1.1\n---\na: yes\nb: 017\n", YamlVersion.V1_2);
		assertEquals(Boolean.TRUE, v11.getSimpleValue("a"));
		assertEquals(15, ((Number) v11.getSimpleValue("b")).intValue());

		final YamlMapping v12 = (YamlMapping) readRoot("%YAML 1.2\n---\na: yes\nb: 017\n", YamlVersion.V1_1);
		assertEquals("yes", v12.getSimpleValue("a"));
		assertEquals(17, ((Number) v12.getSimpleValue("b")).intValue());
	}

	@Test
	void versionDirectivePerDocument() throws Exception {
		try (YamlReader reader = new YamlReader(toStream("%YAML 1.1\n---\na: yes\n...\n---\nb: yes\n"))) {
			assertEquals(Boolean.TRUE, ((YamlMapping) reader.readDocument().getRoot()).getSimpleValue("a"));
			assertEquals("yes", ((YamlMapping) reader.readDocument().getRoot()).getSimpleValue("b"));
		}
	}

	@Test
	void versionFromDirectiveValue() {
		assertEquals(YamlVersion.V1_1, YamlVersion.fromDirectiveValue("1.0"));
		assertEquals(YamlVersion.V1_1, YamlVersion.fromDirectiveValue("1.1"));
		assertEquals(YamlVersion.V1_2, YamlVersion.fromDirectiveValue("1.2"));
		assertEquals(YamlVersion.V1_2, YamlVersion.fromDirectiveValue("1.3"));
		assertNull(YamlVersion.fromDirectiveValue("x"));
		assertNull(YamlVersion.fromDirectiveValue(null));
	}

	@Test
	void mergeKeyWithKeyY() throws Exception {
		// "y" was read as boolean key "true" by the YAML 1.1 rules
		final YamlNode root = readRoot("base: &b {x: 1}\nd:\n  <<: *b\n  y: 2\n", null);
		final JsonObject d = (JsonObject) ((JsonObject) YamlToJsonConverter.convert(root)).get("d");
		assertEquals("1", d.get("x").toString());
		assertEquals("2", d.get("y").toString());
		assertFalse(d.containsKey("true"));
	}

	@Test
	void writerQuotesAmbiguousStrings() throws Exception {
		// Strings that are booleans or numbers in YAML 1.1 are quoted, so the output is read the same way by any reader
		final YamlNode root = readRoot("a: yes\nb: on\nc: 1_000\n", YamlVersion.V1_2);
		final String yaml = new YamlDocument(root).toString();
		for (final YamlVersion version : YamlVersion.values()) {
			final YamlMapping reread = (YamlMapping) readRoot(yaml, version);
			assertEquals("yes", reread.getSimpleValue("a"), version.name());
			assertEquals("on", reread.getSimpleValue("b"), version.name());
			assertEquals("1_000", reread.getSimpleValue("c"), version.name());
		}
	}

	/** Declaration of the named tag handle "!e!", which must precede its use. */
	private static final String TAG_DIRECTIVE = "%TAG !e! tag:example.com,2026:\n---\n";

	@Test
	void yaml11CollectionTags() throws Exception {
		final YamlMapping set = (YamlMapping) readRoot("--- !!set\n? a\n? b\n", null);
		assertEquals(2, set.size());
		assertThrows(Exception.class, () -> readRoot("--- !!set\na: 1\n", null));
		assertThrows(Exception.class, () -> readRoot("--- !!set\n- a\n", null));

		final YamlSequence omap = (YamlSequence) readRoot("--- !!omap\n- a: 1\n- b: 2\n", null);
		assertEquals(2, omap.size());
		assertTrue(readRoot("--- !!pairs\n- a: 1\n- a: 2\n", null) instanceof YamlSequence);
		assertThrows(Exception.class, () -> readRoot("--- !!omap\n- a\n", null));
		assertThrows(Exception.class, () -> readRoot("--- !!omap\n- a: 1\n  b: 2\n", null));
		assertThrows(Exception.class, () -> readRoot("--- !!pairs\na: 1\n", null));
	}

	@Test
	void undeclaredTagHandleIsRejected() {
		assertThrows(YamlParseException.class, () -> readRoot("a: !e!point [1, 2]\n", null));
		// A %TAG directive is valid for its document only
		assertThrows(YamlParseException.class, () -> {
			try (YamlReader reader = new YamlReader(toStream(TAG_DIRECTIVE + "a: !e!x 1\n---\nb: !e!x 2\n"))) {
				assertNotNull(reader.readDocument());
				reader.readDocument();
			}
		});
	}

	@Test
	void localTags() throws Exception {
		final YamlMapping root = (YamlMapping) readRoot(TAG_DIRECTIVE + "a: !custom foo\nb: !<tag:example.com,2026:p> bar\nc: !e!point [1, 2]\nd: &anchor !custom {k: v}\n", null);
		assertEquals("foo", root.getSimpleValue("a"));
		assertEquals("!custom", root.get("a").getTag());
		assertEquals("bar", root.getSimpleValue("b"));
		assertEquals("!<tag:example.com,2026:p>", root.get("b").getTag());
		assertTrue(root.get("c") instanceof YamlSequence);
		assertEquals("!e!point", root.get("c").getTag());
		assertTrue(root.get("d") instanceof YamlMapping);
		assertEquals("!custom", root.get("d").getTag());
		assertEquals("anchor", root.get("d").getAnchorName());
	}

	@Test
	void localTagsInSequencesAndFlow() throws Exception {
		final YamlMapping root = (YamlMapping) readRoot("list:\n  - !item a\n  - !item {z: 1}\nflow: [!x 1, !!str 2]\n", null);
		final YamlSequence list = (YamlSequence) root.get("list");
		assertEquals("!item", list.get(0).getTag());
		assertEquals("a", ((YamlScalar) list.get(0)).getValue());
		assertEquals("!item", list.get(1).getTag());
		final YamlSequence flow = (YamlSequence) root.get("flow");
		assertEquals("!x", flow.get(0).getTag());
		assertEquals(1, ((Number) ((YamlScalar) flow.get(0)).getValue()).intValue());
		// Datatype tags are applied, not stored
		assertNull(flow.get(1).getTag());
		assertEquals("2", ((YamlScalar) flow.get(1)).getValue());
	}

	@Test
	void nonSpecificTagMakesString() throws Exception {
		final YamlScalar scalar = (YamlScalar) ((YamlMapping) readRoot("a: ! 123\n", null)).get("a");
		assertEquals(YamlScalarType.STRING, scalar.getType());
		assertEquals("123", scalar.getValue());
	}

	@Test
	void tagsRoundTrip() throws Exception {
		final String yaml = TAG_DIRECTIVE + "a: !custom foo\nb: !e!point [1, 2]\nc: &anchor !custom {k: v}\nlist:\n  - !item a\n";
		final YamlDocument document;
		try (YamlReader reader = new YamlReader(toStream(yaml))) {
			document = reader.readDocument();
		}
		// The %TAG directive is written, too
		final String written = document.toString();
		assertTrue(written.contains("%TAG !e! tag:example.com,2026:"), written);
		assertTrue(written.contains("a: !custom foo"), written);
		assertTrue(written.contains("- !item a"), written);
		final YamlMapping reread = (YamlMapping) readRoot(written, null);
		assertEquals("!custom", reread.get("a").getTag());
		assertEquals("!e!point", reread.get("b").getTag());
		assertEquals("!custom", reread.get("c").getTag());
		assertEquals("anchor", reread.get("c").getAnchorName());
		assertEquals("!item", ((YamlSequence) reread.get("list")).get(0).getTag());
	}

	@Test
	void tagsAreIgnoredForJson() throws Exception {
		final JsonNode json = YamlToJsonConverter.convert(readRoot(TAG_DIRECTIVE + "a: !custom foo\nb: !e!point [1, 2]\n", null));
		assertEquals("{\"a\":\"foo\",\"b\":[1,2]}", toJsonString(json));
	}

	@Test
	void invalidTagValues() {
		final YamlScalar scalar = new YamlScalar("x");
		assertThrows(IllegalArgumentException.class, () -> scalar.setTag("custom"));
		assertThrows(IllegalArgumentException.class, () -> scalar.setTag("!a b"));
		scalar.setTag(null);
		assertNull(scalar.getTag());
	}

	@Test
	void unknownDatatypeTagStillFails() {
		assertThrows(Exception.class, () -> readRoot("a: !!unknown 1\n", null));
	}

	@Test
	void nonFiniteNumbersAreValidJson() throws Exception {
		final JsonNode json = YamlToJsonConverter.convert(readRoot("a: .inf\nb: -.inf\nc: .nan\nd: 1.5\n", null));
		assertEquals("{\"a\":null,\"b\":null,\"c\":null,\"d\":1.5}", toJsonString(json));
	}

	@Test
	void nonFiniteNumbersAsJson5() throws Exception {
		final JsonArray array = new JsonArray()
				.add(new JsonValueNumber(Double.POSITIVE_INFINITY))
				.add(new JsonValueNumber(Double.NEGATIVE_INFINITY))
				.add(new JsonValueNumber(Double.NaN));

		final ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
		try (JsonWriter writer = new JsonWriter(standardOutput)) {
			writer.withUglify(true);
			writer.add(array);
		}
		assertEquals("[null,null,null]", standardOutput.toString(StandardCharsets.UTF_8));

		final ByteArrayOutputStream json5Output = new ByteArrayOutputStream();
		try (JsonWriter writer = new JsonWriter(json5Output)) {
			writer.withUglify(true).withNonFiniteNumbersAsJson5(true);
			writer.add(array);
		}
		assertEquals("[Infinity,-Infinity,NaN]", json5Output.toString(StandardCharsets.UTF_8));
	}
}
