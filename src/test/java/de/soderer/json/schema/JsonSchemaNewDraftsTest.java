package de.soderer.json.schema;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.JsonUtilities;

/**
 * Tests of the JSON schema drafts 2019-09 and 2020-12, of the URI based reference resolution and
 * of the compatibility with the former reference lookup rules.
 */
public class JsonSchemaNewDraftsTest {
	private static JsonNode json(final String text) throws Exception {
		try (JsonReader reader = new JsonReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)))) {
			return reader.read();
		}
	}

	private static JsonSchema schema(final String schemaText, final JsonSchemaVersion version, final JsonSchemaDependency... dependencies) throws Exception {
		return new JsonSchema((JsonObject) json(schemaText), new JsonSchemaConfiguration().withJsonSchemaVersion(version), dependencies);
	}

	private static void assertValid(final JsonSchema jsonSchema, final String data) {
		assertDoesNotThrow(() -> jsonSchema.validate(json(data)), data);
	}

	private static void assertInvalid(final JsonSchema jsonSchema, final String data) {
		assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(json(data)), data);
	}

	@Test
	void versionDetectionBySchemaKeyword() throws Exception {
		assertEquals(JsonSchemaVersion.draft2020_12, JsonSchemaVersion.getJsonSchemaVersionByVersionUrl("https://json-schema.org/draft/2020-12/schema"));
		assertEquals(JsonSchemaVersion.draft2019_09, JsonSchemaVersion.getJsonSchemaVersionByVersionUrl("http://json-schema.org/draft/2019-09/schema#"));
		assertEquals(JsonSchemaVersion.draftV7, JsonSchemaVersion.getJsonSchemaVersionByVersionUrl("https://json-schema.org/draft-07/schema#"));

		// "prefixItems" is only known since draft 2020-12, so it shows the detected version
		final JsonSchema jsonSchema = schema("{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\", \"prefixItems\": [{\"type\": \"string\"}], \"items\": false}", null);
		assertValid(jsonSchema, "[\"a\"]");
		assertInvalid(jsonSchema, "[\"a\", 1]");
		assertInvalid(jsonSchema, "[1]");
	}

	@Test
	void items_2019_09_vs_2020_12() throws Exception {
		final String arrayItems = "{\"items\": [{\"type\": \"string\"}], \"additionalItems\": {\"type\": \"integer\"}}";
		final JsonSchema draft2019 = schema(arrayItems, JsonSchemaVersion.draft2019_09);
		assertValid(draft2019, "[\"a\", 1, 2]");
		assertInvalid(draft2019, "[\"a\", \"b\"]");

		final JsonSchema draft2020 = schema("{\"prefixItems\": [{\"type\": \"string\"}], \"items\": {\"type\": \"integer\"}}", JsonSchemaVersion.draft2020_12);
		assertValid(draft2020, "[\"a\", 1, 2]");
		assertInvalid(draft2020, "[\"a\", \"b\"]");
	}

	@Test
	void unevaluatedProperties() throws Exception {
		final JsonSchema jsonSchema = schema("{\"allOf\": [{\"properties\": {\"a\": true}}], \"anyOf\": [{\"properties\": {\"b\": true}}, {\"properties\": {\"c\": true}}], \"unevaluatedProperties\": false}", JsonSchemaVersion.draft2020_12);
		assertValid(jsonSchema, "{\"a\": 1, \"b\": 2, \"c\": 3}");
		assertInvalid(jsonSchema, "{\"a\": 1, \"d\": 4}");

		// Annotations of sibling sub schemas ("cousins") are not visible
		final JsonSchema cousins = schema("{\"allOf\": [{\"properties\": {\"a\": true}}, {\"unevaluatedProperties\": false}]}", JsonSchemaVersion.draft2020_12);
		assertInvalid(cousins, "{\"a\": 1}");

		// Properties evaluated by a referenced schema count as evaluated
		final JsonSchema withRef = schema("{\"$ref\": \"#/$defs/base\", \"properties\": {\"b\": true}, \"unevaluatedProperties\": false, \"$defs\": {\"base\": {\"properties\": {\"a\": true}}}}", JsonSchemaVersion.draft2020_12);
		assertValid(withRef, "{\"a\": 1, \"b\": 2}");
		assertInvalid(withRef, "{\"a\": 1, \"x\": 2}");
	}

	@Test
	void unevaluatedItems() throws Exception {
		final JsonSchema jsonSchema = schema("{\"prefixItems\": [true], \"contains\": {\"type\": \"string\"}, \"unevaluatedItems\": false}", JsonSchemaVersion.draft2020_12);
		assertValid(jsonSchema, "[1, \"a\", \"b\"]");
		assertInvalid(jsonSchema, "[1, \"a\", 2]");

		// In draft 2019-09 "contains" does not mark items as evaluated
		final JsonSchema draft2019 = schema("{\"items\": [true], \"contains\": {\"type\": \"string\"}, \"unevaluatedItems\": false}", JsonSchemaVersion.draft2019_09);
		assertInvalid(draft2019, "[1, \"a\"]");
	}

	@Test
	void containsWithMinMax() throws Exception {
		final JsonSchema jsonSchema = schema("{\"contains\": {\"type\": \"integer\"}, \"minContains\": 2, \"maxContains\": 3}", JsonSchemaVersion.draft2019_09);
		assertValid(jsonSchema, "[1, 2, \"a\"]");
		assertInvalid(jsonSchema, "[1, \"a\"]");
		assertInvalid(jsonSchema, "[1, 2, 3, 4]");
		assertValid(schema("{\"contains\": false, \"minContains\": 0}", JsonSchemaVersion.draft2020_12), "[1]");
	}

	@Test
	void dependentRequiredAndSchemas() throws Exception {
		final JsonSchema jsonSchema = schema("{\"dependentRequired\": {\"credit_card\": [\"billing_address\"]}, \"dependentSchemas\": {\"name\": {\"required\": [\"age\"]}}}", JsonSchemaVersion.draft2020_12);
		assertValid(jsonSchema, "{\"credit_card\": 1, \"billing_address\": \"x\"}");
		assertInvalid(jsonSchema, "{\"credit_card\": 1}");
		assertValid(jsonSchema, "{\"name\": \"x\", \"age\": 3}");
		assertInvalid(jsonSchema, "{\"name\": \"x\"}");
	}

	@Test
	void anchorsAndIdsInSubSchemas() throws Exception {
		final JsonSchema jsonSchema = schema("{\"$id\": \"https://example.com/root.json\", \"properties\": {\"a\": {\"$ref\": \"#positive\"}, \"b\": {\"$ref\": \"item.json\"}},"
				+ " \"$defs\": {\"p\": {\"$anchor\": \"positive\", \"minimum\": 0}, \"i\": {\"$id\": \"item.json\", \"type\": \"string\"}}}", JsonSchemaVersion.draft2020_12);
		assertValid(jsonSchema, "{\"a\": 1, \"b\": \"x\"}");
		assertInvalid(jsonSchema, "{\"a\": -1}");
		assertInvalid(jsonSchema, "{\"b\": 1}");
	}

	@Test
	void dynamicReference() throws Exception {
		// The extending schema adds "required" to every node of the recursive tree schema
		final JsonSchemaDependency tree = new JsonSchemaDependency("https://example.com/tree", (JsonObject) json(
				"{\"$id\": \"https://example.com/tree\", \"$dynamicAnchor\": \"node\", \"type\": \"object\", \"properties\": {\"children\": {\"type\": \"array\", \"items\": {\"$dynamicRef\": \"#node\"}}}}"));
		final JsonSchema strictTree = schema("{\"$id\": \"https://example.com/strict-tree\", \"$dynamicAnchor\": \"node\", \"$ref\": \"tree\", \"required\": [\"name\"]}", JsonSchemaVersion.draft2020_12, tree);
		assertValid(strictTree, "{\"name\": \"a\", \"children\": [{\"name\": \"b\"}]}");
		assertInvalid(strictTree, "{\"name\": \"a\", \"children\": [{}]}");
	}

	@Test
	void recursiveReference() throws Exception {
		final JsonSchemaDependency tree = new JsonSchemaDependency("https://example.com/tree", (JsonObject) json(
				"{\"$id\": \"https://example.com/tree\", \"$recursiveAnchor\": true, \"type\": \"object\", \"properties\": {\"children\": {\"type\": \"array\", \"items\": {\"$recursiveRef\": \"#\"}}}}"));
		final JsonSchema strictTree = schema("{\"$id\": \"https://example.com/strict-tree\", \"$recursiveAnchor\": true, \"$ref\": \"tree\", \"required\": [\"name\"]}", JsonSchemaVersion.draft2019_09, tree);
		assertValid(strictTree, "{\"name\": \"a\", \"children\": [{\"name\": \"b\"}]}");
		assertInvalid(strictTree, "{\"name\": \"a\", \"children\": [{}]}");
	}

	@Test
	void refWithSiblings_dependsOnVersion() throws Exception {
		final String schemaText = "{\"$ref\": \"#/definitions/a\", \"maxLength\": 2, \"definitions\": {\"a\": {\"type\": \"string\"}}}";
		// Up to draft 7 "$ref" overrides its siblings, since 2019-09 they apply together
		assertValid(schema(schemaText, JsonSchemaVersion.draftV7), "\"abc\"");
		assertInvalid(schema(schemaText, JsonSchemaVersion.draft2019_09), "\"abc\"");
	}

	@Test
	void emptyEnumAndNumericEquality() throws Exception {
		assertInvalid(schema("{\"enum\": []}", JsonSchemaVersion.draft2020_12), "1");
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"enum\": []}", JsonSchemaVersion.draftV7));
		assertValid(schema("{\"enum\": [[0], {\"a\": 1}]}", JsonSchemaVersion.draftV7), "[0.0]");
		assertValid(schema("{\"enum\": [[0], {\"a\": 1}]}", JsonSchemaVersion.draftV7), "{\"a\": 1.0}");
		assertInvalid(schema("{\"uniqueItems\": true}", JsonSchemaVersion.draftV7), "[[1], [1.0]]");
	}

	@Test
	void contentKeywordsAreAnnotationsSince2019_09() throws Exception {
		final String schemaText = "{\"contentMediaType\": \"application/json\"}";
		assertValid(schema(schemaText, JsonSchemaVersion.draft2020_12), "\"{:}\"");
		assertInvalid(schema(schemaText, JsonSchemaVersion.draftV7), "\"{:}\"");
	}

	@Test
	void legacyReferenceStylesStillWork() throws Exception {
		final String legacySchema = "{\"definitions\": {\"address\": {\"type\": \"object\", \"required\": [\"street\"]}}, \"properties\": {\"home\": {\"$ref\": \"address\"}}}";
		for (final JsonSchemaVersion version : new JsonSchemaVersion[] { JsonSchemaVersion.simple, JsonSchemaVersion.draftV7 }) {
			final JsonSchema jsonSchema = schema(legacySchema, version);
			assertValid(jsonSchema, "{\"home\": {\"street\": \"x\"}}");
			assertInvalid(jsonSchema, "{\"home\": {}}");
		}
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"properties\": {\"a\": {\"$ref\": \"doesNotExist\"}}}", JsonSchemaVersion.draftV7));

		final JsonSchemaDependency other = new JsonSchemaDependency("other.json", (JsonObject) json("{\"definitions\": {\"x\": {\"$ref\": \"#/definitions/y\"}, \"y\": {\"type\": \"integer\"}}}"));
		final JsonSchema withDependency = schema("{\"properties\": {\"a\": {\"$ref\": \"other.json#/definitions/x\"}, \"b\": {\"$ref\": \"y\"}}}", JsonSchemaVersion.draftV7, other);
		assertValid(withDependency, "{\"a\": 5, \"b\": 6}");
		assertInvalid(withDependency, "{\"a\": \"s\"}");
		assertInvalid(withDependency, "{\"b\": \"s\"}");
	}

	@Test
	void metaSchemaValidation() throws Exception {
		assertDoesNotThrow(() -> JsonUtilities.validateJsonSchemaDraft2020_12(stream("{\"type\": \"object\", \"properties\": {\"a\": {\"type\": \"string\", \"minLength\": 1}}}"), StandardCharsets.UTF_8));
		assertThrows(JsonSchemaDataValidationError.class, () -> JsonUtilities.validateJsonSchemaDraft2020_12(stream("{\"properties\": {\"a\": {\"minLength\": -1}}}"), StandardCharsets.UTF_8));
		// Nested schemas are checked via "$dynamicRef" (2020-12) and "$recursiveRef" (2019-09) of the meta schemas
		assertThrows(JsonSchemaDataValidationError.class, () -> JsonUtilities.validateJsonSchemaDraft2020_12(stream("{\"$defs\": {\"x\": {\"items\": {\"type\": 5}}}}"), StandardCharsets.UTF_8));
		assertThrows(JsonSchemaDataValidationError.class, () -> JsonUtilities.validateJsonSchemaDraft2019_09(stream("{\"$defs\": {\"x\": {\"items\": {\"type\": 5}}}}"), StandardCharsets.UTF_8));
		assertDoesNotThrow(() -> JsonUtilities.validateJsonSchemaDraft2019_09(stream("{\"$defs\": {\"x\": {\"items\": {\"type\": \"string\"}}}}"), StandardCharsets.UTF_8));
	}

	@Test
	void parallelValidationWithSharedSchema() throws Exception {
		final JsonSchema metaSchema;
		try (InputStream metaSchemaStream = JsonSchema.class.getClassLoader().getResourceAsStream(JsonSchemaVersion.draft2020_12.getLocalFile())) {
			metaSchema = new JsonSchema(metaSchemaStream);
		}
		final String validSchema = "{\"type\": \"object\", \"properties\": {\"a\": {\"type\": \"string\", \"minLength\": 1}, \"b\": {\"prefixItems\": [{\"type\": \"integer\"}], \"items\": {\"$ref\": \"#/$defs/c\"}}}, \"$defs\": {\"c\": {\"enum\": [1, 2]}}, \"unevaluatedProperties\": false}";
		final ExecutorService executor = Executors.newFixedThreadPool(8);
		try {
			final List<Future<Boolean>> results = new ArrayList<>();
			for (int i = 0; i < 400; i++) {
				final boolean valid = i % 2 == 0;
				final String data = valid ? validSchema : validSchema.replace("\"minLength\": 1", "\"minLength\": -1");
				results.add(executor.submit(() -> {
					try {
						metaSchema.validate(json(data));
						return valid;
					} catch (@SuppressWarnings("unused") final JsonSchemaDataValidationError e) {
						return !valid;
					}
				}));
			}
			for (final Future<Boolean> result : results) {
				assertEquals(Boolean.TRUE, result.get());
			}
		} finally {
			executor.shutdown();
		}
	}

	private static InputStream stream(final String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
	}
}
