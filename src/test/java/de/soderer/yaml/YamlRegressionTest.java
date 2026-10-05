package de.soderer.yaml;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonDataType;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.yaml.data.YamlComparator;
import de.soderer.yaml.data.YamlDiffPatcher;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlMultilineScalarChompingType;
import de.soderer.yaml.data.YamlMultilineScalarType;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.data.YamlStringQuoteType;

/**
 * Regression tests for bugs found during the Javadoc and bug review of the YAML package.
 */
@SuppressWarnings("static-method")
public class YamlRegressionTest {
	private static YamlNode read(final String yaml) throws Exception {
		return YamlReader.readDocument(yaml).getRoot();
	}

	private static String write(final YamlNode root, final YamlFormat yamlFormat) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (YamlWriter yamlWriter = new YamlWriter(outputStream, yamlFormat)) {
			yamlWriter.writeDocument(new YamlDocument(root));
		}
		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	private static void assertRoundTrip(final YamlNode root, final YamlFormat yamlFormat) throws Exception {
		final String yaml = write(root, yamlFormat);
		Assertions.assertEquals(root, read(yaml), "Round trip failed for YAML:\n" + yaml);
	}

	@Test
	public void testSequenceItemWithMoreBlanksAfterDash() throws Exception {
		final YamlSequence sequence = (YamlSequence) read("-   a: 1\n    b: 2\n- c\n");
		Assertions.assertEquals(2, sequence.size());
		Assertions.assertEquals(2, ((YamlMapping) sequence.get(0)).size());
	}

	@Test
	public void testIndentationSizeFour() throws Exception {
		final YamlMapping root = new YamlMapping().add("a", new YamlSequence().add(new YamlMapping().add("b", 1).add("c", new YamlSequence().add("x"))));
		assertRoundTrip(root, new YamlFormat().withIndentationSize(4));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new YamlFormat().setIndentationSize(1));
	}

	@Test
	public void testFoldedScalar() throws Exception {
		Assertions.assertEquals("a b\n", ((YamlScalar) ((YamlMapping) read("k: >\n  a\n  b\nz: 1\n")).get("k")).getValue());
		Assertions.assertEquals("a\nb\n", ((YamlScalar) ((YamlMapping) read("k: >\n  a\n\n  b\n")).get("k")).getValue());
		Assertions.assertEquals("a\n  more\nb\n", ((YamlScalar) ((YamlMapping) read("k: >\n  a\n    more\n  b\n")).get("k")).getValue());
		Assertions.assertEquals("a b\n\n", ((YamlScalar) ((YamlMapping) read("k: >+\n  a\n  b\n\n")).get("k")).getValue());
	}

	@Test
	public void testBlockScalarHeader() throws Exception {
		Assertions.assertEquals(" x\ny", ((YamlScalar) ((YamlMapping) read("k: |2-\n   x\n  y\n")).get("k")).getValue());
		Assertions.assertEquals(" x\ny", ((YamlScalar) ((YamlMapping) read("k: |-2\n   x\n  y\n")).get("k")).getValue());
		final YamlScalar withComment = (YamlScalar) ((YamlMapping) read("k: | # comment\n  x\n")).get("k");
		Assertions.assertEquals("x\n", withComment.getValue());
		Assertions.assertEquals(" comment", withComment.getInlineComment());
	}

	@Test
	public void testMultilineWritingKeepsText() throws Exception {
		for (final String text : new String[] { "abc", "a\nb\n", "a\n\n", "\n\na\n", " indented\nnext\n", "a\n\n\nb\n  c\nd" }) {
			for (final YamlMultilineScalarType multilineType : YamlMultilineScalarType.values()) {
				final YamlScalar scalar = new YamlScalar(text, YamlScalarType.MULTILINE).withMultilineType(multilineType).withMultilineChompingType(YamlMultilineScalarChompingType.KEEP);
				final String yaml = write(new YamlMapping().add("k", scalar).add("z", "z"), new YamlFormat());
				Assertions.assertEquals(text, ((YamlScalar) ((YamlMapping) read(yaml)).get("k")).getValue(), yaml);
			}
		}
	}

	@Test
	public void testHashInFlowScalar() throws Exception {
		Assertions.assertEquals("a#b", ((YamlSequence) ((YamlMapping) read("k: [a#b, c]\n")).get("k")).simpleItems().iterator().next());
	}

	@Test
	public void testNumbers() throws Exception {
		Assertions.assertEquals(4294967295L, ((YamlScalar) ((YamlMapping) read("k: 0xFFFFFFFF\n")).get("k")).getValue());
		Assertions.assertEquals(15, ((YamlScalar) ((YamlMapping) read("k: 0o17\n")).get("k")).getValue());
		Assertions.assertEquals(12, ((YamlScalar) ((YamlMapping) read("k: +12\n")).get("k")).getValue());
		Assertions.assertEquals("1,234", ((YamlScalar) ((YamlMapping) read("k: 1,234\n")).get("k")).getValue());
		Assertions.assertEquals(new YamlScalar(1), new YamlScalar(1L));
		Assertions.assertEquals(new YamlScalar(1).hashCode(), new YamlScalar(1L).hashCode());
		Assertions.assertNotNull(new YamlMapping().add(1, "one").get(1L));
		Assertions.assertEquals(".nan", new YamlScalar(Double.NaN).getValueString());
		Assertions.assertEquals("-.inf", new YamlScalar(Double.NEGATIVE_INFINITY).getValueString());
	}

	@Test
	public void testStringQuoting() throws Exception {
		for (final String text : new String[] { "a,b", "0x1F", "1_000", ".inf", ".NaN", "...", "--- x", "bell\u0007", "line1\nline2", "+1", "true", "~" }) {
			for (final YamlFormat yamlFormat : new YamlFormat[] { new YamlFormat(), new YamlFormat().withStringValueQuoteType(YamlStringQuoteType.SINGLE) }) {
				assertRoundTrip(new YamlMapping().add("k", text).add(text, "v"), yamlFormat);
				assertRoundTrip(new YamlMapping().add("k", new YamlSequence(true).add(text).add("z")), yamlFormat);
			}
		}
	}

	@Test
	public void testTagDirectiveWithMultipleBlanks() throws Exception {
		Assertions.assertNotNull(read("%YAML 1.2\n%TAG !e!   tag:example.com,2000:\n---\nk: v\n"));
	}

	@Test
	public void testMappingAddNodeWithNumberKey() throws Exception {
		final YamlMapping mapping = new YamlMapping().add(1, new YamlSequence().add("x")).add(true, new YamlMapping());
		Assertions.assertTrue(mapping.get(1) instanceof YamlSequence);
	}

	@Test
	public void testSortMixedTypes() throws Exception {
		final YamlSequence sequence = new YamlSequence();
		for (int i = 0; i < 300; i++) {
			sequence.add(i % 3 == 0 ? (YamlNode) new YamlScalar(i) : new YamlScalar(i + "x"));
			sequence.add(new YamlMapping().add("k", i % 2 == 0 ? (Object) i : "v" + i));
		}
		sequence.sort(true);
		sequence.sortByAttribute("k", false);
		Assertions.assertEquals(600, sequence.size());
	}

	@Test
	public void testConverter() throws Exception {
		final JsonNode jsonNode = YamlToJsonConverter.convert(read(
				"base: &base\n  a: 1\n  b: 2\nderived:\n  <<: *base\n  b: 3\nresponses:\n  200: ok\nlist:\n  - *base\nbig: 123456789012345678901234\n"));
		final JsonObject jsonObject = (JsonObject) jsonNode;
		final JsonObject derived = (JsonObject) jsonObject.get("derived");
		Assertions.assertEquals(1, derived.getSimpleValue("a"));
		Assertions.assertEquals(3, derived.getSimpleValue("b"));
		Assertions.assertEquals("ok", ((JsonObject) jsonObject.get("responses")).getSimpleValue("200"));
		Assertions.assertEquals(JsonDataType.INTEGER, ((JsonObject) jsonObject.get("base")).get("a").getJsonDataType());
		Assertions.assertEquals(JsonDataType.INTEGER, jsonObject.get("big").getJsonDataType());
		Assertions.assertEquals(2, ((JsonObject) ((JsonArray) jsonObject.get("list")).get(0)).size());
		Assertions.assertThrows(RuntimeException.class, () -> YamlToJsonConverter.convert(read("a: *unknown\n")));
	}

	@Test
	public void testDiffAndPatch() throws Exception {
		final YamlMapping left = (YamlMapping) read("a.b: 1\n\"x: y\": old\n1: one\nlist:\n  - a\ntext: |\n  line1\n  line2\nremoved:\n  deep: 1\n");
		final YamlMapping right = (YamlMapping) read("a.b: 2\n\"x: y\": new -> newer\n1: uno\nlist:\n  - a\n  - b\ntext: |\n  line1\n  changed\nadded:\n  deep: [1, 2]\n");
		final List<YamlComparator.DiffEntry> diffEntries = new YamlComparator().compare(left, right);
		final String diffText = YamlComparator.renderAsText(diffEntries);
		YamlDiffPatcher.applyPatch(left, YamlDiffPatcher.parseDiffText(diffText));
		// The diff text does not keep the scalar style (multiline text becomes a plain string), so compare the values
		Assertions.assertTrue(new YamlComparator().compare(left, right).isEmpty(), diffText);

		final YamlSequence leftSequence = (YamlSequence) read("- a\n- b\n");
		final YamlSequence rightSequence = (YamlSequence) read("- a\n- c\n");
		YamlDiffPatcher.applyPatch(leftSequence, YamlDiffPatcher.parseDiffText(YamlComparator.renderAsText(new YamlComparator().compare(leftSequence, rightSequence))));
		Assertions.assertEquals(rightSequence, leftSequence);

		Assertions.assertTrue(new YamlComparator().compare(read("- &a x\n- *a\n"), read("- &a x\n- *a\n")).isEmpty());
	}

	@Test
	public void testBigIntegerValue() throws Exception {
		assertRoundTrip(new YamlMapping().add("k", new BigInteger("123456789012345678901234")), new YamlFormat());
	}
}
