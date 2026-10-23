package de.soderer.json.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;

@SuppressWarnings("static-method")
class JsonSchemaExampleGeneratorTest {
	private static JsonObject object(final String text) throws Exception {
		return (JsonObject) JsonReader.readJsonItemString(text);
	}

	private static JsonNode generate(final String schemaText) throws Exception {
		final JsonObject schema = object(schemaText);
		return new JsonSchemaExampleGenerator(new JsonSchemaDependencyResolver(schema)).generateExample(schema);
	}

	@Test
	void constructor_requiresResolver() {
		assertThrows(IllegalArgumentException.class, () -> new JsonSchemaExampleGenerator(null));
	}

	@Test
	void nullSchema_returnsNull() throws Exception {
		assertNull(new JsonSchemaExampleGenerator(new JsonSchemaDependencyResolver(new JsonObject())).generateExample(null));
	}

	@Test
	void simpleTypes() throws Exception {
		assertEquals(0, (((de.soderer.json.JsonValueInteger) generate("{\"type\": \"integer\"}")).getValue()).intValue());
		assertTrue(generate("{\"type\": \"number\"}").isNumber());
		assertTrue(generate("{\"type\": \"boolean\"}").isBoolean());
		assertTrue(generate("{\"type\": \"null\"}").isNull());
		assertTrue(((JsonObject) generate("{}")).isEmpty());
		assertTrue(((JsonObject) generate("{\"type\": [\"string\", \"null\"]}")).isEmpty());
	}

	@Test
	void stringFormats() throws Exception {
		final String[][] expectations = {
				{ "", "string" },
				{ "date-time", "2025-01-01T00:00:00Z" },
				{ "date", "2025-01-01" },
				{ "time", "00:00:00" },
				{ "email", "user@example.com" },
				{ "uuid", "00000000-0000-0000-0000-000000000000" },
				{ "uri", "https://example.com" },
				{ "url", "https://example.com" },
				{ "ipv4", "127.0.0.1" },
				{ "ipv6", "::1" },
				{ "binary", "" },
				{ "byte", "" },
				{ "hostname", "string" }
		};
		for (final String[] expectation : expectations) {
			final String schema = expectation[0].isEmpty() ? "{\"type\": \"string\"}" : "{\"type\": \"string\", \"format\": \"" + expectation[0] + "\"}";
			assertEquals(expectation[1], ((de.soderer.json.JsonValueString) generate(schema)).getValue(), schema);
		}
	}

	@Test
	void explicitExampleEnumAndAlternatives() throws Exception {
		assertEquals("given", ((de.soderer.json.JsonValueString) generate("{\"type\": \"string\", \"example\": \"given\"}")).getValue());
		assertEquals("first", ((de.soderer.json.JsonValueString) generate("{\"enum\": [\"first\", \"second\"]}")).getValue());
		assertTrue(((JsonObject) generate("{\"enum\": []}")).isEmpty());
		assertTrue(generate("{\"oneOf\": [{\"type\": \"boolean\"}, {\"type\": \"string\"}]}").isBoolean());
		assertTrue(generate("{\"anyOf\": [{\"type\": \"null\"}]}").isNull());
		assertTrue(((JsonObject) generate("{\"anyOf\": []}")).isEmpty());

		final JsonObject merged = (JsonObject) generate("{\"allOf\": [{\"properties\": {\"a\": {\"type\": \"integer\"}}}, {\"properties\": {\"a\": {\"type\": \"string\"}, \"b\": {\"type\": \"boolean\"}}}, {\"type\": \"string\"}, 1]}");
		assertTrue(merged.get("a").isInteger());
		assertTrue(merged.get("b").isBoolean());
	}

	@Test
	void objectsAndArrays() throws Exception {
		final JsonObject example = (JsonObject) generate("{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}, \"tags\": {\"type\": \"array\", \"items\": {\"type\": \"string\", \"format\": \"email\"}}, \"invalid\": 1}}");
		assertEquals("string", example.getSimpleValue("name"));
		assertEquals("user@example.com", ((JsonArray) example.get("tags")).getSimpleItem(0));
		assertEquals(2, example.size());

		final JsonObject additional = (JsonObject) generate("{\"type\": \"object\", \"additionalProperties\": {\"type\": \"integer\"}}");
		assertTrue(additional.get("key").isInteger());
		assertTrue(((JsonObject) generate("{\"type\": \"object\"}")).isEmpty());
		assertTrue(((JsonArray) generate("{\"type\": \"array\"}")).isEmpty());
	}

	@Test
	void references_includingCycles() throws Exception {
		final JsonObject example = (JsonObject) generate("{\"type\": \"object\", \"properties\": {\"node\": {\"$ref\": \"#/definitions/node\"}}, \"definitions\": {\"node\": {\"type\": \"object\", \"properties\": {\"value\": {\"type\": \"integer\"}, \"child\": {\"$ref\": \"#/definitions/node\"}}}}}");
		final JsonObject node = (JsonObject) example.get("node");
		assertTrue(node.get("value").isInteger());
		// The cyclic reference is replaced by an empty object
		assertTrue(((JsonObject) node.get("child")).isEmpty());

		assertTrue(generate("{\"$ref\": \"simple\", \"definitions\": {\"simple\": {\"type\": \"boolean\"}}}").isBoolean());
	}
}
