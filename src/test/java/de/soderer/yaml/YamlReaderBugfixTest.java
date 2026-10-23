package de.soderer.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonObject;
import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;

/**
 * Regression tests for fixed YAML reader and writer errors: empty sequence items, anchors and
 * aliases within flow collections, core schema tags and multi-line plain scalars.
 */
@SuppressWarnings("static-method")
class YamlReaderBugfixTest {
	private static YamlNode read(final String yaml) throws Exception {
		return YamlReader.readDocument(yaml).getRoot();
	}

	private static YamlNode roundtrip(final YamlNode node) throws Exception {
		return YamlReader.readDocument(YamlWriter.toString(new YamlDocument(node))).getRoot();
	}

	@Test
	void emptySequenceItem_isNull() throws Exception {
		final YamlSequence sequence = (YamlSequence) read("- a\n-\n- b\n");
		assertEquals(3, sequence.size());
		assertEquals("a", ((YamlScalar) sequence.get(0)).getValue());
		assertEquals(YamlScalarType.NULL_VALUE, ((YamlScalar) sequence.get(1)).getType());
		assertEquals("b", ((YamlScalar) sequence.get(2)).getValue());

		final YamlSequence withBlanks = (YamlSequence) read("- a\n-   \n- b\n");
		assertEquals(3, withBlanks.size());

		final YamlSequence lastItemEmpty = (YamlSequence) read("- a\n-\n");
		assertEquals(2, lastItemEmpty.size());
		assertNull(((YamlScalar) lastItemEmpty.get(1)).getValue());

		final YamlMapping nested = (YamlMapping) read("k:\n  -\n  - x\n  -\nz: 1\n");
		final YamlSequence nestedSequence = (YamlSequence) nested.get("k");
		assertEquals(3, nestedSequence.size());
		assertNull(((YamlScalar) nestedSequence.get(0)).getValue());
		assertNull(((YamlScalar) nestedSequence.get(2)).getValue());
		assertEquals(1, ((Number) nested.getSimpleValue("z")).intValue());

		// Item content on the next line, indented deeper than the dash
		final YamlSequence contentOnNextLine = (YamlSequence) read("-\n  b\n- c\n");
		assertEquals("b", ((YamlScalar) contentOnNextLine.get(0)).getValue());
		assertEquals(2, contentOnNextLine.size());
		final YamlSequence mappingOnNextLine = (YamlSequence) read("-\n  k: v\n-\n");
		assertInstanceOf(YamlMapping.class, mappingOnNextLine.get(0));
		assertNull(((YamlScalar) mappingOnNextLine.get(1)).getValue());

		assertEquals(sequence, roundtrip(sequence));
	}

	@Test
	void aliasesAndAnchorsInFlowCollections() throws Exception {
		final YamlMapping root = (YamlMapping) read("x: &x 1\nq: [*x, &s2 z, *s2]\nm: {a: *x, &k b: 2, c: &v 3}\nr: *v\n");

		final YamlSequence sequence = (YamlSequence) root.get("q");
		assertInstanceOf(YamlAlias.class, sequence.get(0));
		assertEquals("x", ((YamlAlias) sequence.get(0)).getTargetAnchorName());
		// The anchor belongs to the item, not to the sequence
		assertEquals("s2", sequence.get(1).getAnchorName());
		assertNull(sequence.getAnchorName());
		assertEquals("s2", ((YamlAlias) sequence.get(2)).getTargetAnchorName());

		final YamlMapping mapping = (YamlMapping) root.get("m");
		assertInstanceOf(YamlAlias.class, mapping.get("a"));
		YamlNode anchoredKey = null;
		for (final YamlNode key : mapping.keySet()) {
			if ("b".equals(((YamlScalar) key).getValue())) {
				anchoredKey = key;
			}
		}
		assertNotNull(anchoredKey);
		assertEquals("k", anchoredKey.getAnchorName());
		// An anchor after ':' belongs to the value
		assertEquals("v", mapping.get("c").getAnchorName());

		final JsonObject json = YamlToJsonConverter.convert(root);
		assertEquals(new JsonArray().add(Integer.valueOf(1)).add("z").add("z"), json.get("q"));
		assertEquals(1, ((Number) ((JsonObject) json.get("m")).getSimpleValue("a")).intValue());
		assertEquals(3, ((Number) json.getSimpleValue("r")).intValue());

		// Anchors and aliases survive writing and reading again
		final String written = YamlWriter.toString(new YamlDocument(root));
		assertTrue(written.contains("[*x, &s2 z, *s2]"), written);
		assertTrue(written.contains("{a: *x, &k b: 2, c: &v 3}"), written);
		assertEquals(json, YamlToJsonConverter.convert(YamlReader.readDocument(written)));

		final YamlSequence firstItemAnchor = (YamlSequence) read("[&f first, second]\n");
		assertEquals("f", firstItemAnchor.get(0).getAnchorName());
		assertNull(firstItemAnchor.getAnchorName());
		final YamlMapping firstKeyAnchor = (YamlMapping) read("{&k a: 1}\n");
		assertEquals("k", firstKeyAnchor.keySet().iterator().next().getAnchorName());
		assertNull(firstKeyAnchor.getAnchorName());

		final YamlSequence flowMappingItem = (YamlSequence) read("[a: &w 1, b: *w]\n");
		assertEquals(2, flowMappingItem.size());

		assertThrows(Exception.class, () -> read("[*]\n"));
	}

	@Test
	void coreSchemaTags() throws Exception {
		final YamlMapping root = (YamlMapping) read("i: !!int 5\nbig: !!int 123456789012345678901234567890\nb: !!bool yes\nnul: !!null ~\ne: !!null\ns: !!str 1\nf: !!float 1\n"
				+ "flow: [!!str 1, !!int 2, !!bool false, !!null null]\nmap: {k: !!float 1.5}\nanchor: !!str &an 5\nref: *an\n");
		assertEquals(5, ((Number) root.getSimpleValue("i")).intValue());
		assertEquals(0, new BigInteger("123456789012345678901234567890").compareTo(new BigInteger(root.getSimpleValue("big").toString())));
		assertEquals(Boolean.TRUE, root.getSimpleValue("b"));
		assertEquals(YamlScalarType.NULL_VALUE, ((YamlScalar) root.get("nul")).getType());
		assertEquals(YamlScalarType.NULL_VALUE, ((YamlScalar) root.get("e")).getType());
		assertEquals("1", root.getSimpleValue("s"));
		assertEquals(YamlScalarType.NUMBER, ((YamlScalar) root.get("f")).getType());
		final YamlSequence flow = (YamlSequence) root.get("flow");
		assertEquals("1", ((YamlScalar) flow.get(0)).getValue());
		assertEquals(2, ((Number) ((YamlScalar) flow.get(1)).getValue()).intValue());
		assertEquals(Boolean.FALSE, ((YamlScalar) flow.get(2)).getValue());
		assertEquals(YamlScalarType.NULL_VALUE, ((YamlScalar) flow.get(3)).getType());
		assertEquals("an", root.get("anchor").getAnchorName());
		assertEquals("5", YamlToJsonConverter.convert(root).getSimpleValue("ref"));

		final YamlSequence taggedEmptyItems = (YamlSequence) read("- !!null\n- !!str\n- x\n");
		assertEquals(3, taggedEmptyItems.size());
		assertEquals(YamlScalarType.NULL_VALUE, ((YamlScalar) taggedEmptyItems.get(0)).getType());
		assertEquals("", ((YamlScalar) taggedEmptyItems.get(1)).getValue());

		final YamlMapping typedCollections = (YamlMapping) read("m: !!map {a: 1}\nq: !!seq [1, 2]\nbm: !!map\n  a: 1\nbs: !!seq\n  - 1\nsameLevel: !!seq\n- 2\nitems:\n  - !!seq\n    - 3\n");
		assertInstanceOf(YamlMapping.class, typedCollections.get("m"));
		assertInstanceOf(YamlSequence.class, typedCollections.get("q"));
		assertInstanceOf(YamlMapping.class, typedCollections.get("bm"));
		assertInstanceOf(YamlSequence.class, typedCollections.get("bs"));
		assertInstanceOf(YamlSequence.class, typedCollections.get("sameLevel"));
		assertInstanceOf(YamlSequence.class, ((YamlSequence) typedCollections.get("items")).get(0));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"a: !!int x\n",
			"a: !!int 1.5\n",
			"a: !!int 123456789012345678901234567890.5\n",
			"a: !!float text\n",
			"a: !!bool maybe\n",
			"a: !!null value\n",
			"a: !!map [1]\n",
			"a: !!seq {b: 1}\n",
			"a: !!str [1]\n",
			"a: !!map\n  - 1\n",
			"a: [!!int 1.5]\n",
			"a: {k: !!seq x}\n",
			"a: !!unknown 1\n"
	})
	void coreSchemaTags_invalidValues(final String yaml) {
		assertThrows(Exception.class, () -> read(yaml), yaml);
	}

	@Test
	void multiLinePlainScalars() throws Exception {
		final YamlMapping mapping = (YamlMapping) read("plain: word1\n  word2\n   word3\n\n  word4\nnext: 1\n");
		assertEquals("word1 word2 word3\nword4", mapping.getSimpleValue("plain"));
		assertEquals(1, ((Number) mapping.getSimpleValue("next")).intValue());

		final YamlMapping valueOnNextLine = (YamlMapping) read("a:\n  word1\n  word2\nb: 2\n");
		assertEquals("word1 word2", valueOnNextLine.getSimpleValue("a"));

		final YamlSequence sequence = (YamlSequence) read("- word1\n  word2\n- b\n");
		assertEquals("word1 word2", ((YamlScalar) sequence.get(0)).getValue());
		assertEquals(2, sequence.size());

		final YamlMapping nested = (YamlMapping) read("outer:\n  inner: v1\n    v2\n  other: 3\nlist:\n  - x\n    y\n  - z\n");
		final YamlMapping outer = (YamlMapping) nested.get("outer");
		assertEquals("v1 v2", outer.getSimpleValue("inner"));
		assertEquals(3, ((Number) outer.getSimpleValue("other")).intValue());
		assertEquals("x y", ((YamlScalar) ((YamlSequence) nested.get("list")).get(0)).getValue());

		// A comment line ends the scalar, also when it is indented
		final YamlMapping withComment = (YamlMapping) read("a: 1\n  # comment\nb: 2\n");
		assertEquals(2, ((Number) withComment.getSimpleValue("b")).intValue());

		// A mapping value within a multi-line plain scalar is invalid YAML
		assertThrows(Exception.class, () -> read("a: x\n  b: y\n"));

		assertEquals(mapping, roundtrip(mapping));
	}
}
