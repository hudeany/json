package de.soderer.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import de.soderer.json.Json5Reader;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.exception.MaximumNestingDepthExceededException;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.exception.YamlAliasExpansionLimitException;
import de.soderer.yaml.exception.YamlParseException;

/**
 * Tests the limits protecting against malicious data: nesting depth of the JSON and YAML readers and
 * alias expansion of the YAML to JSON conversion.
 */
@SuppressWarnings("static-method")
public class ResourceLimitTest {
	private static InputStream toStream(final String data) {
		return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
	}

	private static String nestedArrays(final int depth) {
		return "[".repeat(depth) + "]".repeat(depth);
	}

	private static String nestedBlockMappings(final int depth) {
		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			builder.append("  ".repeat(i)).append("k:\n");
		}
		return builder.substring(0, builder.length() - 1) + " x\n";
	}

	private static String nestedBlockSequences(final int depth) {
		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			builder.append("  ".repeat(i)).append("-\n");
		}
		return builder.substring(0, builder.length() - 1) + " x\n";
	}

	private static final String BILLION_LAUGHS = ""
			+ "a: &a [x,x,x,x,x,x,x,x,x]\n"
			+ "b: &b [*a,*a,*a,*a,*a,*a,*a,*a,*a]\n"
			+ "c: &c [*b,*b,*b,*b,*b,*b,*b,*b,*b]\n"
			+ "d: &d [*c,*c,*c,*c,*c,*c,*c,*c,*c]\n"
			+ "e: &e [*d,*d,*d,*d,*d,*d,*d,*d,*d]\n"
			+ "f: &f [*e,*e,*e,*e,*e,*e,*e,*e,*e]\n"
			+ "g: &g [*f,*f,*f,*f,*f,*f,*f,*f,*f]\n"
			+ "h: &h [*g,*g,*g,*g,*g,*g,*g,*g,*g]\n";

	@Test
	public void testJsonNestingDepthAtLimit() throws Exception {
		try (JsonReader reader = new JsonReader(toStream(nestedArrays(JsonReader.DEFAULT_MAXIMUM_NESTING_DEPTH)))) {
			assertNotNull(reader.read());
		}
	}

	@Test
	public void testJsonNestingDepthExceeded() throws Exception {
		try (JsonReader reader = new JsonReader(toStream(nestedArrays(100_000)))) {
			final MaximumNestingDepthExceededException exception = assertThrows(MaximumNestingDepthExceededException.class, reader::read);
			assertEquals(JsonReader.DEFAULT_MAXIMUM_NESTING_DEPTH, exception.getMaximumNestingDepth());
		}
		try (JsonReader reader = new JsonReader(toStream("{\"a\":".repeat(501) + "1" + "}".repeat(501)))) {
			assertThrows(MaximumNestingDepthExceededException.class, reader::read);
		}
	}

	@Test
	public void testJsonNestingDepthTokenStream() throws Exception {
		try (JsonReader reader = new JsonReader(toStream(nestedArrays(100_000)))) {
			assertThrows(MaximumNestingDepthExceededException.class, () -> {
				while (reader.readNextToken() != null) {
					// Read all tokens
				}
			});
		}
	}

	@Test
	public void testJson5NestingDepthExceeded() throws Exception {
		try (Json5Reader reader = new Json5Reader(toStream(nestedArrays(100_000)))) {
			assertThrows(MaximumNestingDepthExceededException.class, reader::read);
		}
	}

	@Test
	public void testJsonNestingDepthConfigurable() throws Exception {
		// Above the default limit, but low enough for the stack of any test runner
		try (JsonReader reader = new JsonReader(toStream(nestedArrays(800)))) {
			reader.withMaximumNestingDepth(800);
			assertNotNull(reader.read());
		}
		try (JsonReader reader = new JsonReader(toStream(nestedArrays(801)))) {
			reader.withMaximumNestingDepth(800);
			assertThrows(MaximumNestingDepthExceededException.class, reader::read);
		}
		try (JsonReader reader = new JsonReader(toStream(nestedArrays(3)))) {
			reader.withMaximumNestingDepth(2);
			assertThrows(MaximumNestingDepthExceededException.class, reader::read);
		}
		try (JsonReader reader = new JsonReader(toStream("[]"))) {
			assertThrows(IllegalArgumentException.class, () -> reader.setMaximumNestingDepth(0));
		}
	}

	@Test
	public void testYamlNestingDepthAtLimit() throws Exception {
		final int limit = YamlReader.DEFAULT_MAXIMUM_NESTING_DEPTH;
		try (YamlReader reader = new YamlReader(toStream(nestedBlockMappings(limit)))) {
			assertNotNull(reader.readDocument());
		}
		try (YamlReader reader = new YamlReader(toStream(nestedBlockSequences(limit)))) {
			assertNotNull(reader.readDocument());
		}
		try (YamlReader reader = new YamlReader(toStream("a: " + nestedArrays(limit - 1)))) {
			assertNotNull(reader.readDocument());
		}
	}

	@Test
	public void testYamlNestingDepthExceeded() throws Exception {
		final int limit = YamlReader.DEFAULT_MAXIMUM_NESTING_DEPTH;
		try (YamlReader reader = new YamlReader(toStream(nestedBlockMappings(limit + 1)))) {
			assertThrows(YamlParseException.class, reader::readDocument);
		}
		try (YamlReader reader = new YamlReader(toStream(nestedBlockSequences(limit + 1)))) {
			assertThrows(YamlParseException.class, reader::readDocument);
		}
		try (YamlReader reader = new YamlReader(toStream("a: " + nestedArrays(100_000)))) {
			assertThrows(YamlParseException.class, reader::readDocument);
		}
		try (YamlReader reader = new YamlReader(toStream("? ".repeat(5000) + "x\n"))) {
			assertThrows(YamlParseException.class, reader::readDocument);
		}
	}

	@Test
	public void testYamlScalarIsNoNestingLevel() throws Exception {
		try (YamlReader reader = new YamlReader(toStream("a: 1\n"))) {
			reader.withMaximumNestingDepth(1);
			assertNotNull(reader.readDocument());
		}
		try (YamlReader reader = new YamlReader(toStream("a:\n  b: 1\n"))) {
			reader.withMaximumNestingDepth(1);
			assertThrows(YamlParseException.class, reader::readDocument);
		}
	}

	@Test
	public void testYamlBillionLaughs() throws Exception {
		final YamlDocument document;
		try (YamlReader reader = new YamlReader(toStream(BILLION_LAUGHS))) {
			document = reader.readDocument();
		}
		final YamlAliasExpansionLimitException exception = assertThrows(YamlAliasExpansionLimitException.class, () -> YamlToJsonConverter.convert(document));
		assertEquals(YamlToJsonConverter.DEFAULT_MAXIMUM_ALIAS_EXPANDED_NODES, exception.getMaximumAliasExpandedNodes());
	}

	@Test
	public void testYamlBillionLaughsSchemaValidation() {
		assertThrows(YamlAliasExpansionLimitException.class, () -> YamlUtilities.validateJsonSchemaOnYamlDataSimple(toStream(BILLION_LAUGHS), toStream("{}")));
	}

	@Test
	public void testYamlMergeKeyBomb() throws Exception {
		final StringBuilder data = new StringBuilder("m0: &m0 {k: v}\n");
		for (int i = 1; i <= 40; i++) {
			data.append("m").append(i).append(": &m").append(i).append(" {<<: [*m").append(i - 1).append(", *m").append(i - 1).append("]}\n");
		}
		final YamlDocument document;
		try (YamlReader reader = new YamlReader(toStream(data.toString()))) {
			document = reader.readDocument();
		}
		assertThrows(YamlAliasExpansionLimitException.class, () -> YamlToJsonConverter.convert(document));
	}

	@Test
	public void testYamlAliasExpansionDepth() throws Exception {
		// Each level is 100 deep, but the alias chain expands to 1100 levels
		final StringBuilder data = new StringBuilder("a0: &a0 " + nestedArrays(100) + "\n");
		for (int i = 1; i <= 10; i++) {
			data.append("a").append(i).append(": &a").append(i).append(" ").append("[".repeat(100)).append("*a").append(i - 1).append("]".repeat(100)).append("\n");
		}
		final YamlDocument document;
		try (YamlReader reader = new YamlReader(toStream(data.toString()))) {
			document = reader.readDocument();
		}
		final RuntimeException exception = assertThrows(RuntimeException.class, () -> YamlToJsonConverter.convert(document));
		assertTrue(exception.getMessage().contains("nesting depth"));
	}

	@Test
	public void testYamlAliasExpansionConfigurable() throws Exception {
		final YamlDocument withAlias;
		try (YamlReader reader = new YamlReader(toStream("a: &a 1\nb: *a\n"))) {
			withAlias = reader.readDocument();
		}
		assertThrows(YamlAliasExpansionLimitException.class, () -> YamlToJsonConverter.convert(withAlias, 0));
		assertEquals("1", ((JsonObject) YamlToJsonConverter.convert(withAlias, 1)).get("b").toString());

		final YamlDocument withoutAlias;
		try (YamlReader reader = new YamlReader(toStream("a: 1\nb: [1, 2, 3]\n"))) {
			withoutAlias = reader.readDocument();
		}
		assertNotNull(YamlToJsonConverter.convert(withoutAlias, 0));
	}

	@Test
	public void testYamlNormalAliasesAndMergeKeys() throws Exception {
		final YamlDocument document;
		try (YamlReader reader = new YamlReader(toStream("base: &b {x: 1, z: 3}\nlist: &l [1, 2]\nd:\n  <<: *b\n  w: *l\n  z: 4\n"))) {
			document = reader.readDocument();
		}
		final JsonObject d = (JsonObject) ((JsonObject) YamlToJsonConverter.convert(document)).get("d");
		assertEquals("1", d.get("x").toString());
		assertEquals("4", d.get("z").toString());
		final JsonNode w = d.get("w");
		assertNotNull(w);
	}
}
