package de.soderer.yaml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.soderer.json.JsonNode;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlSequence;

/**
 * Tests tags of root nodes and mapping keys and the "!!binary" datatype.
 */
@SuppressWarnings("static-method")
public class YamlTagAndBinaryTest {
	private static YamlDocument read(final String yaml) throws Exception {
		try (YamlReader reader = new YamlReader(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))) {
			return reader.readDocument();
		}
	}

	private static YamlScalar getKey(final YamlMapping mapping, final String keyText) {
		for (final YamlNode key : mapping.keySet()) {
			if (key instanceof final YamlScalar scalarKey && keyText.equals(scalarKey.getValueString())) {
				return scalarKey;
			}
		}
		throw new IllegalArgumentException("Missing key " + keyText);
	}

	private static String toCompactJson(final YamlDocument document) throws Exception {
		final JsonNode json = YamlToJsonConverter.convert(document);
		return json == null ? "null" : json.toString().replaceAll("\\s+", "");
	}

	@ParameterizedTest
	@ValueSource(strings = { "!custom foo\n", "--- !custom foo\n", "!custom\na: 1\nb: 2\n", "!point\n- 1\n- 2\n", "!custom {a: 1}\n", "!custom [1, 2]\n" })
	void rootTagRoundTrip(final String yaml) throws Exception {
		final YamlDocument document = read(yaml);
		assertTrue(document.getRoot().getTag().startsWith("!"), yaml);
		final String written = document.toString();
		final YamlDocument reread = read(written);
		assertEquals(document.getRoot().getTag(), reread.getRoot().getTag(), written);
		assertEquals(toCompactJson(document), toCompactJson(reread), written);
	}

	@Test
	void tagOnOwnLineBelongsToMapping() throws Exception {
		final YamlMapping root = (YamlMapping) read("!custom\na: 1\n").getRoot();
		assertEquals("!custom", root.getTag());
		assertNull(getKey(root, "a").getTag());
	}

	@Test
	void tagBeforeKeyBelongsToKey() throws Exception {
		final YamlMapping root = (YamlMapping) read("!first a: 1\nb: 2\n!third c: 3\n").getRoot();
		assertNull(root.getTag());
		assertEquals("!first", getKey(root, "a").getTag());
		assertNull(getKey(root, "b").getTag());
		assertEquals("!third", getKey(root, "c").getTag());
		assertEquals("{\"a\":1,\"b\":2,\"c\":3}", toCompactJson(read("!first a: 1\nb: 2\n!third c: 3\n")));
	}

	@Test
	void tagBeforeKeyInNestedMappingsAndSequences() throws Exception {
		final String yaml = "m:\n  !k a: |\n    text\n  !k b: 1\nlist:\n  - !item x: 1\n    y: 2\nz: 3\n";
		final YamlDocument document = read(yaml);
		final YamlMapping m = (YamlMapping) ((YamlMapping) document.getRoot()).get("m");
		assertEquals("!k", getKey(m, "a").getTag());
		assertEquals("!k", getKey(m, "b").getTag());
		final YamlMapping item = (YamlMapping) ((YamlSequence) ((YamlMapping) document.getRoot()).get("list")).get(0);
		assertEquals("!item", getKey(item, "x").getTag());
		assertEquals("{\"m\":{\"a\":\"text\\n\",\"b\":1},\"list\":[{\"x\":1,\"y\":2}],\"z\":3}", toCompactJson(document));
	}

	@Test
	void datatypeTagBeforeKey() throws Exception {
		final YamlMapping root = (YamlMapping) read("!!str 1: x\n2: y\n").getRoot();
		assertEquals("1", getKey(root, "1").getValue());
		assertEquals(Integer.valueOf(2), ((YamlScalar) root.keySet().toArray()[1]).getValue());
	}

	@ParameterizedTest
	@ValueSource(strings = { "!key a: 1\nb: 2\n", "a: 1\n!key b: 2\n", "!key a: >-\n  text\n  more\nb: 2\n", "x: 1\n!key \"q\": >-\n  text\nb: 2\n", "? !key [a]\n: v\n", "{!k a: 1, b: !v 2}\n" })
	void keyTagRoundTrip(final String yaml) throws Exception {
		final YamlDocument document = read(yaml);
		final String written = document.toString();
		final YamlDocument reread = read(written);
		assertEquals(toCompactJsonOrKeys(document), toCompactJsonOrKeys(reread), written);
		assertEquals(collectTags(document.getRoot()), collectTags(reread.getRoot()), written);
	}

	private static String toCompactJsonOrKeys(final YamlDocument document) {
		try {
			return toCompactJson(document);
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Complex keys have no JSON representation
			return document.getRoot().toString();
		}
	}

	private static String collectTags(final YamlNode node) {
		final StringBuilder tags = new StringBuilder();
		if (node.getTag() != null) {
			tags.append(node.getTag()).append(";");
		}
		if (node instanceof final YamlMapping mapping) {
			for (final YamlNode key : mapping.keySet()) {
				tags.append(collectTags(key)).append(collectTags(mapping.get(key)));
			}
		} else if (node instanceof final YamlSequence sequence) {
			for (int i = 0; i < sequence.size(); i++) {
				tags.append(collectTags(sequence.get(i)));
			}
		}
		return tags.toString();
	}

	@Test
	void binaryValue() throws Exception {
		final YamlScalar scalar = (YamlScalar) ((YamlMapping) read("c: !!binary aGVsbG8=\n").getRoot()).get("c");
		assertTrue(scalar.isBinary());
		assertEquals(YamlScalar.BINARY_TAG, scalar.getTag());
		assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), scalar.getBinaryValue());
		// The value stays the base64 text, also in JSON
		assertEquals("aGVsbG8=", scalar.getValue());
		assertEquals("{\"c\":\"aGVsbG8=\"}", toCompactJson(read("c: !!binary aGVsbG8=\n")));
	}

	@Test
	void binaryBlockScalar() throws Exception {
		final YamlScalar scalar = (YamlScalar) ((YamlMapping) read("c: !!binary |\n  aGVs\n  bG8=\n").getRoot()).get("c");
		assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), scalar.getBinaryValue());
	}

	@Test
	void binaryRoundTrip() throws Exception {
		final byte[] data = new byte[] { 0, 1, 2, (byte) 0xFF, 'x' };
		final YamlDocument document = new YamlDocument(new YamlMapping().add("data", YamlScalar.fromBinaryValue(data)));
		final String written = document.toString();
		assertTrue(written.contains("!!binary"), written);
		final YamlScalar reread = (YamlScalar) ((YamlMapping) read(written).getRoot()).get("data");
		assertArrayEquals(data, reread.getBinaryValue());
	}

	@Test
	void invalidBinaryIsDetectedWhenDecoded() throws Exception {
		// Documents with placeholders can be read, the base64 text is checked when it is decoded
		final YamlScalar scalar = (YamlScalar) ((YamlMapping) read("c: !!binary 'not base64!'\n").getRoot()).get("c");
		assertTrue(scalar.isBinary());
		assertThrows(IllegalArgumentException.class, scalar::getBinaryValue);
	}

	@Test
	void binaryOnlyForStrings() {
		assertThrows(Exception.class, () -> read("c: !!binary [1, 2]\n"));
		assertThrows(Exception.class, () -> read("c: !!binary 12\n"));
	}

	@Test
	void noBinaryValueWithoutTag() throws Exception {
		final YamlScalar scalar = (YamlScalar) ((YamlMapping) read("c: aGVsbG8=\n").getRoot()).get("c");
		assertFalse(scalar.isBinary());
		assertThrows(IllegalStateException.class, scalar::getBinaryValue);
	}
}
