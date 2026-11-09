package de.soderer.yaml;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.exception.YamlParseException;

/**
 * Tests the check of aliases by {@link YamlReader} and the anchor search of {@link YamlDocument}.
 */
@DisplayName("Alias check and anchor search")
public class YamlAliasCheckTest {
	private static YamlDocument read(final String yaml) throws Exception {
		return YamlReader.readDocument(yaml);
	}

	@Nested
	@DisplayName("Alias check while reading")
	class AliasCheck {
		@ParameterizedTest
		@ValueSource(strings = { "a: *unknown\n", "- *unknown\n", "[*unknown]\n", "{ k: *unknown }\n", "*unknown : value\n", "{ *unknown : value }\n" })
		@DisplayName("Alias to an unknown anchor is rejected")
		void unknownAnchor(final String yaml) {
			final YamlParseException exception = assertThrows(YamlParseException.class, () -> read(yaml));
			assertTrue(exception.getMessage().contains("unknown anchor: '*unknown'"), exception.getMessage());
		}

		@Test
		@DisplayName("Error position is the position of the alias")
		void errorPosition() {
			final YamlParseException exception = assertThrows(YamlParseException.class, () -> read("a: 1\nb:\n  - x\n  - *missing\n"));
			assertEquals(4, exception.getLine());
			assertEquals(5, exception.getColumn());
		}

		@Test
		@DisplayName("Alias before its anchor is rejected")
		void aliasBeforeAnchor() {
			assertThrows(YamlParseException.class, () -> read("a: *x\nb: &x 1\n"));
		}

		@Test
		@DisplayName("Anchors of a previous document are unknown")
		void anchorOfPreviousDocument() throws Exception {
			try (YamlReader reader = new YamlReader(new ByteArrayInputStream("a: &x 1\n---\nb: *x\n".getBytes(StandardCharsets.UTF_8)))) {
				reader.readDocument();
				assertThrows(YamlParseException.class, reader::readDocument);
			}
		}

		@ParameterizedTest
		@ValueSource(strings = { "&a [ *a ]\n", "&a { k: *a }\n", "a: &a\n  b: *a\n", "&a\n- x\n- [ y, *a ]\n", "a: &a\n  b:\n    - *a\n", "&a { *a : 1 }\n" })
		@DisplayName("Alias within the node it refers to is rejected as cyclic")
		void cyclicReference(final String yaml) {
			final YamlParseException exception = assertThrows(YamlParseException.class, () -> read(yaml));
			assertTrue(exception.getMessage().contains("Cyclic alias reference"), exception.getMessage());
		}

		@ParameterizedTest
		@ValueSource(strings = { "a: &a 1\nb: *a\n", "- &a [1, 2]\n- *a\n- *a\n", "base: &base\n  x: 1\nderived:\n  <<: *base\n  y: 2\n", "&a a: &b b\n*b : *a\n",
				"&a [ &a x, *a ]\n", "a: &a { b: &b 1 }\nc: *b\n" })
		@DisplayName("Valid aliases are accepted")
		void validAliases(final String yaml) {
			assertDoesNotThrow(() -> read(yaml));
		}

		@Test
		@DisplayName("Items read by path are checked, they may refer to anchors read before")
		void itemsReadByPath() throws Exception {
			try (YamlReader reader = new YamlReader(new ByteArrayInputStream("defaults: &d {x: 1}\nlist:\n  - *d\n  - *unknown\n".getBytes(StandardCharsets.UTF_8)))) {
				reader.readUpToPath("$.list");
				final YamlNode firstItem = reader.readNextYamlNode();
				assertEquals(new YamlAlias("d"), firstItem);
				final YamlParseException exception = assertThrows(YamlParseException.class, reader::readNextYamlNode);
				assertEquals(4, exception.getLine());
			}
		}
	}

	@Nested
	@DisplayName("Anchor search in YamlDocument")
	class AnchorSearch {
		@Test
		@DisplayName("Node is found by its anchor name")
		void getNodeByAnchor() throws Exception {
			final YamlDocument document = read("a:\n  b: &inner [1, 2]\nc: &scalar text\n");
			final YamlSequence sequence = (YamlSequence) document.getNodeByAnchor("inner");
			assertEquals(2, sequence.size());
			assertEquals("text", ((YamlScalar) document.getNodeByAnchor("scalar")).getValueString());
			assertNull(document.getNodeByAnchor("missing"));
			assertNull(document.getNodeByAnchor(null));
		}

		@Test
		@DisplayName("Anchors of keys and of the root node are found")
		void anchorsOfKeysAndRoot() throws Exception {
			final YamlDocument document = read("&root\n&key k: v\n");
			assertSame(document.getRoot(), document.getNodeByAnchor("root"));
			assertEquals("k", ((YamlScalar) document.getNodeByAnchor("key")).getValueString());
		}

		@Test
		@DisplayName("An anchor defined again: the last definition is found, aliases refer to the definition before them")
		void redefinedAnchor() throws Exception {
			final YamlDocument document = read("a: &x 1\nb: *x\nc: &x 2\nd: *x\n");
			final YamlMapping root = (YamlMapping) document.getRoot();
			assertEquals("2", ((YamlScalar) document.getNodeByAnchor("x")).getValueString());
			assertSame(root.get("a"), document.resolveAlias((YamlAlias) root.get("b")));
			assertSame(root.get("c"), document.resolveAlias((YamlAlias) root.get("d")));

			final List<YamlNode> anchoredNodes = document.getAnchoredNodes();
			assertEquals(2, anchoredNodes.size());
			assertSame(root.get("a"), anchoredNodes.get(0));
			assertSame(root.get("c"), anchoredNodes.get(1));
		}

		@Test
		@DisplayName("An alias, which is not part of the document, is not resolved")
		void foreignAlias() throws Exception {
			final YamlDocument document = read("a: &x 1\n");
			assertNull(document.resolveAlias(new YamlAlias("x")));
		}

		@Test
		@DisplayName("Anchor search in a document without root")
		void emptyDocument() {
			final YamlDocument document = new YamlDocument();
			assertNull(document.getNodeByAnchor("x"));
			assertTrue(document.getAnchoredNodes().isEmpty());
		}
	}
}
