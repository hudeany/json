package de.soderer.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
 * Tests the position of anchors: an anchor on a line of its own belongs to the following node, an
 * anchor before content on the same line belongs to the first key, if the content is a block
 * mapping.
 */
@SuppressWarnings("static-method")
public class YamlAnchorTest {
	private static YamlDocument read(final String yaml) throws Exception {
		try (YamlReader reader = new YamlReader(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))) {
			return reader.readDocument();
		}
	}

	private static String toCompactJson(final YamlDocument document) throws Exception {
		final JsonNode json = YamlToJsonConverter.convert(document);
		return json == null ? "null" : json.toString().replaceAll("\\s+", "");
	}

	private static YamlScalar getKey(final YamlMapping mapping, final String keyText) {
		for (final YamlNode key : mapping.keySet()) {
			if (key instanceof final YamlScalar scalarKey && keyText.equals(scalarKey.getValueString())) {
				return scalarKey;
			}
		}
		throw new IllegalArgumentException("Missing key " + keyText);
	}

	@ParameterizedTest
	@ValueSource(strings = { "&root\na: 1\n", "--- &root\na: 1\n", "&root !custom\na: 1\n", "&root\n- 1\n- 2\n", "&root [1, 2]\n", "&root scalar\n" })
	void anchorOfRootNode(final String yaml) throws Exception {
		final YamlDocument document = read(yaml);
		assertEquals("root", document.getRoot().getAnchorName(), yaml);
		final YamlDocument reread = read(document.toString());
		assertEquals("root", reread.getRoot().getAnchorName(), document.toString());
	}

	@Test
	void anchorBeforeFirstKeyBelongsToKey() throws Exception {
		final YamlDocument document = read("&a a: b\nc: *a\n");
		final YamlMapping root = (YamlMapping) document.getRoot();
		assertNull(root.getAnchorName());
		assertEquals("a", getKey(root, "a").getAnchorName());
		assertEquals("{\"a\":\"b\",\"c\":\"a\"}", toCompactJson(document));
	}

	@Test
	void anchorBeforeFollowingKeyBelongsToKey() throws Exception {
		// The anchor of the key "b" must not be assigned to the value "1" of the line before
		final YamlDocument document = read("x: 1\n&k b: 2\nc: *k\n");
		final YamlMapping root = (YamlMapping) document.getRoot();
		assertNull(root.get("x").getAnchorName());
		assertEquals("k", getKey(root, "b").getAnchorName());
		assertEquals("{\"x\":1,\"b\":2,\"c\":\"b\"}", toCompactJson(document));
	}

	@Test
	void anchorsOfValueAndKey() throws Exception {
		final YamlDocument document = read("top1: &node1\n  &k1 key1: one\ntop3:\n  &k3 key3: three\nref: *node1\n");
		final YamlMapping root = (YamlMapping) document.getRoot();
		final YamlMapping top1 = (YamlMapping) root.get("top1");
		assertEquals("node1", top1.getAnchorName());
		assertEquals("k1", getKey(top1, "key1").getAnchorName());
		assertEquals("k3", getKey((YamlMapping) root.get("top3"), "key3").getAnchorName());
		assertEquals("{\"top1\":{\"key1\":\"one\"},\"top3\":{\"key3\":\"three\"},\"ref\":{\"key1\":\"one\"}}", toCompactJson(document));
	}

	@Test
	void anchorAndTagBeforeKeyInAnyOrder() throws Exception {
		final YamlMapping root = (YamlMapping) read("&a !t first: 1\n!u &b second: 2\n").getRoot();
		assertEquals("a", getKey(root, "first").getAnchorName());
		assertEquals("!t", getKey(root, "first").getTag());
		assertEquals("b", getKey(root, "second").getAnchorName());
		assertEquals("!u", getKey(root, "second").getTag());
	}

	@Test
	void anchorOfCompactMappingKeyInSequence() throws Exception {
		final YamlDocument document = read("- &a key: value\n  other: 1\n- *a\n");
		final YamlMapping item = (YamlMapping) ((YamlSequence) document.getRoot()).get(0);
		assertNull(item.getAnchorName());
		assertEquals("a", getKey(item, "key").getAnchorName());
		assertEquals("[{\"key\":\"value\",\"other\":1},\"key\"]", toCompactJson(document));
	}

	@Test
	void anchorOfFlowCollectionKey() throws Exception {
		final YamlMapping root = (YamlMapping) read("&mapping\n&key [ &item a, b, c ]: value\n").getRoot();
		assertEquals("mapping", root.getAnchorName());
		final YamlSequence key = (YamlSequence) root.keySet().iterator().next();
		assertEquals("key", key.getAnchorName());
		assertEquals("item", key.get(0).getAnchorName());
	}

	@Test
	void anchorRedefinition() throws Exception {
		// YAML 1.2 example 7.1: an alias refers to the latest definition of its anchor before it
		final YamlDocument document = read("First: &anchor Foo\nSecond: *anchor\nOverride: &anchor Bar\nReuse: *anchor\n");
		assertEquals("{\"First\":\"Foo\",\"Second\":\"Foo\",\"Override\":\"Bar\",\"Reuse\":\"Bar\"}", toCompactJson(document));
	}

	@Test
	void twoAnchorsOfOneNode() {
		assertThrows(Exception.class, () -> read("top: &node\n  &value text\n"));
		assertThrows(Exception.class, () -> read("a: &x &y text\n"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "&a a: b\nc: *a\n", "x: 1\n&k b: 2\nc: *k\n", "top1: &node1\n  &k1 key1: one\nref: *k1\n", "- &a key: value\n- *a\n", "&a !t first: 1\nref: *a\n" })
	void anchorRoundTrip(final String yaml) throws Exception {
		final YamlDocument document = read(yaml);
		final String written = document.toString();
		assertEquals(toCompactJson(document), toCompactJson(read(written)), written);
	}
}
