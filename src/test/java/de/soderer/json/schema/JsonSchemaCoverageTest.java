package de.soderer.json.schema;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueString;

/**
 * Covers schema construction, descriptive keywords, keyword placement rules and the validators for
 * "const", "disallow", "extends", "contentEncoding" and "contentMediaType".
 */
@SuppressWarnings("static-method")
class JsonSchemaCoverageTest {
	private static InputStream stream(final String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
	}

	private static JsonSchema schema(final String schemaText, final JsonSchemaVersion version) throws Exception {
		return new JsonSchema(stream(schemaText), new JsonSchemaConfiguration().withJsonSchemaVersion(version));
	}

	private static JsonNode json(final String text) throws Exception {
		return JsonReader.readJsonItemString(text);
	}

	private static void assertValid(final JsonSchema jsonSchema, final String data) throws Exception {
		jsonSchema.validate(json(data));
	}

	private static void assertInvalid(final JsonSchema jsonSchema, final String data) throws Exception {
		final JsonNode jsonData = json(data);
		assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(jsonData), data);
	}

	@Test
	void constructors_booleanSchemas() throws Exception {
		assertDoesNotThrow(() -> new JsonSchema(Boolean.TRUE).validate(json("{\"any\": 1}")));
		assertThrows(JsonSchemaDataValidationError.class, () -> new JsonSchema(Boolean.FALSE).validate(json("1")));
		assertDoesNotThrow(() -> new JsonSchema(Boolean.TRUE, new JsonSchemaConfiguration().withJsonSchemaVersion(JsonSchemaVersion.draftV7)).validate(json("1")));
		final JsonSchemaDependencyResolver resolver = new JsonSchemaDependencyResolver(new JsonObject());
		assertThrows(JsonSchemaDataValidationError.class, () -> new JsonSchema(Boolean.FALSE, resolver).validate(json("1")));
		assertThrows(JsonSchemaDefinitionError.class, () -> new JsonSchema((Boolean) null, resolver));
		assertThrows(JsonSchemaDefinitionError.class, () -> new JsonSchema((Boolean) null));

		// Boolean schemas read from a stream
		assertDoesNotThrow(() -> new JsonSchema(stream("true")).validate(json("[]")));
		assertThrows(JsonSchemaDataValidationError.class, () -> new JsonSchema(stream("false")).validate(json("[]")));
	}

	@Test
	void constructors_invalidInput() throws Exception {
		assertThrows(JsonSchemaDefinitionError.class, () -> new JsonSchema(stream("[1, 2]")));
		assertThrows(JsonSchemaDefinitionError.class, () -> new JsonSchema(stream("{ invalid")));
		assertThrows(JsonSchemaDefinitionError.class, () -> new JsonSchema((JsonObject) null));
		assertThrows(JsonSchemaDefinitionError.class, () -> new JsonSchema((JsonObject) null, new JsonSchemaDependencyResolver(new JsonObject())));

		final JsonSchema fromObject = new JsonSchema(new JsonObject().add("type", "string"));
		assertDoesNotThrow(() -> fromObject.validate(json("\"x\"")));
		final JsonSchema fromObjectWithResolver = new JsonSchema(new JsonObject().add("type", "string"), new JsonSchemaDependencyResolver(new JsonObject()));
		assertThrows(JsonSchemaDataValidationError.class, () -> fromObjectWithResolver.validate(json("1")));
	}

	@Test
	void validate_streams() throws Exception {
		final JsonSchema jsonSchema = schema("{\"type\": \"integer\"}", JsonSchemaVersion.draftV7);
		assertEquals(5, (((de.soderer.json.JsonValueInteger) jsonSchema.validate(stream("5"))).getValue()).intValue());
		assertTrue(jsonSchema.validate(stream("6"), StandardCharsets.UTF_8).isInteger());
		assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(stream("{ broken")));
		assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(stream("{ broken"), StandardCharsets.UTF_8));
		assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(stream("\"x\""), StandardCharsets.UTF_8));
	}

	@Test
	void descriptiveKeywords_gettersAndVersionDetection() throws Exception {
		final JsonSchema v7 = new JsonSchema(stream("{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"$id\": \"http://example.com/s\", \"$comment\": \"c\", \"title\": \"t\", \"description\": \"d\", \"type\": \"string\"}"));
		assertEquals("http://example.com/s", v7.getId());
		assertEquals("http://json-schema.org/draft-07/schema#", v7.getSchemaVersionUrl());
		assertEquals("c", v7.getComment());
		assertEquals("t", v7.getTitle());
		assertEquals("d", v7.getDescription());

		final JsonSchema v4 = new JsonSchema(stream("{\"$schema\": \"http://json-schema.org/draft-04/schema#\", \"id\": \"urn:x\"}"));
		assertEquals("urn:x", v4.getId());
		assertNull(v4.getComment());
	}

	@Test
	void descriptiveKeywords_invalidValues() {
		final String[] invalidSchemas = {
				"{\"id\": null}", "{\"id\": 1}",
				"{\"$id\": null}", "{\"$id\": 1}", "{\"id\": \"a\", \"$id\": \"b\"}",
				"{\"$schema\": null}", "{\"$schema\": 1}",
				"{\"$comment\": null}", "{\"$comment\": 1}",
				"{\"title\": null}", "{\"title\": 1}",
				"{\"description\": null}", "{\"description\": 1}"
		};
		for (final String invalidSchema : invalidSchemas) {
			assertThrows(JsonSchemaDefinitionError.class, () -> schema(invalidSchema, JsonSchemaVersion.draftV7), invalidSchema);
		}
	}

	@Test
	void keywordPlacementRules() {
		// "id" only up to draft v4, "$id" only since draft v6
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"id\": \"x\"}", JsonSchemaVersion.draftV7));
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"$id\": \"x\"}", JsonSchemaVersion.draftV4));
		assertDoesNotThrow(() -> schema("{\"id\": \"x\"}", JsonSchemaVersion.draftV4));

		// Keywords only allowed at the root level
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"properties\": {\"a\": {\"id\": \"x\"}}}", JsonSchemaVersion.draftV4));
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"properties\": {\"a\": {\"$id\": \"x\"}}}", JsonSchemaVersion.draftV7));
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"properties\": {\"a\": {\"$schema\": \"x\"}}}", JsonSchemaVersion.draftV7));
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"properties\": {\"a\": {\"definitions\": {}}}}", JsonSchemaVersion.draftV7));

		// Descriptive keywords in sub schemas must be strings
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"properties\": {\"a\": {\"title\": 1}}}", JsonSchemaVersion.draftV7));
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"properties\": {\"a\": {\"description\": 1}}}", JsonSchemaVersion.draftV7));

		// "if" must be an object or boolean, "$ref" must be a string
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"if\": 1}", JsonSchemaVersion.draftV7));
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"$ref\": 1}", JsonSchemaVersion.draftV7));

		// exclusiveMinimum/exclusiveMaximum need minimum/maximum up to draft v4
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"exclusiveMinimum\": true}", JsonSchemaVersion.draftV4));
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"exclusiveMaximum\": true}", JsonSchemaVersion.draftV4));

		// Unknown keywords are only rejected in simple mode
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"unknownKeyword\": 1}", JsonSchemaVersion.simple));
		assertDoesNotThrow(() -> schema("{\"unknownKeyword\": 1, \"examples\": [1], \"default\": 1, \"additionalItems\": false}", JsonSchemaVersion.draftV7));
	}

	@Test
	void ifThenElse_booleanCondition() throws Exception {
		final JsonSchema jsonSchema = schema("{\"if\": true, \"then\": {\"type\": \"string\"}, \"else\": {\"type\": \"number\"}}", JsonSchemaVersion.draftV7);
		assertValid(jsonSchema, "\"x\"");
		assertInvalid(jsonSchema, "1");
	}

	@Test
	void divisibleBy_isAliasForMultipleOf() throws Exception {
		final JsonSchema jsonSchema = schema("{\"divisibleBy\": 3}", JsonSchemaVersion.draftV3);
		assertValid(jsonSchema, "9");
		assertInvalid(jsonSchema, "10");
	}

	@Test
	void constValidator_allValueTypes() throws Exception {
		assertValid(schema("{\"const\": null}", JsonSchemaVersion.draftV7), "null");
		assertInvalid(schema("{\"const\": null}", JsonSchemaVersion.draftV7), "0");

		final JsonSchema booleanConst = schema("{\"const\": true}", JsonSchemaVersion.draftV7);
		assertValid(booleanConst, "true");
		assertInvalid(booleanConst, "false");
		assertInvalid(booleanConst, "null");

		final JsonSchema stringConst = schema("{\"const\": \"abc\"}", JsonSchemaVersion.draftV7);
		assertValid(stringConst, "\"abc\"");
		assertInvalid(stringConst, "\"abd\"");

		final JsonSchema integerConst = schema("{\"const\": 1}", JsonSchemaVersion.draftV7);
		assertValid(integerConst, "1");
		assertValid(integerConst, "1.0");
		assertInvalid(integerConst, "2");
		assertInvalid(integerConst, "\"1\"");

		final JsonSchema numberConst = schema("{\"const\": 1.5}", JsonSchemaVersion.draftV7);
		assertValid(numberConst, "1.5");
		assertInvalid(numberConst, "1");

		final JsonSchema objectConst = schema("{\"const\": {\"a\": [1]}}", JsonSchemaVersion.draftV7);
		assertValid(objectConst, "{\"a\": [1]}");
		assertInvalid(objectConst, "{\"a\": [2]}");

		final JsonSchema arrayConst = schema("{\"const\": [1, \"x\"]}", JsonSchemaVersion.draftV7);
		assertValid(arrayConst, "[1, \"x\"]");
		assertInvalid(arrayConst, "[\"x\", 1]");
		assertInvalid(arrayConst, "{}");
	}

	@Test
	void disallowValidator_draftV3() throws Exception {
		final JsonSchema disallowString = schema("{\"disallow\": \"string\"}", JsonSchemaVersion.draftV3);
		assertValid(disallowString, "1");
		assertInvalid(disallowString, "\"x\"");

		final JsonSchema disallowNumber = schema("{\"disallow\": \"number\"}", JsonSchemaVersion.draftV3);
		assertInvalid(disallowNumber, "1");
		assertInvalid(disallowNumber, "1.5");
		assertValid(disallowNumber, "true");

		final JsonSchema disallowMixed = schema("{\"disallow\": [\"integer\", {\"type\": \"string\", \"minLength\": 3}]}", JsonSchemaVersion.draftV3);
		assertInvalid(disallowMixed, "1");
		assertInvalid(disallowMixed, "\"long\"");
		assertValid(disallowMixed, "\"ab\"");
		assertValid(disallowMixed, "1.5");
		// Up to draft v4 a number with zero fraction is no integer
		assertValid(disallowMixed, "1.0");
	}

	@Test
	void disallowValidator_any() throws Exception {
		// Draft v3 allows "any" to disallow all values
		final JsonSchema disallowAny = schema("{\"disallow\": \"any\"}", JsonSchemaVersion.draftV3);
		assertInvalid(disallowAny, "1");
		assertInvalid(disallowAny, "null");
		assertInvalid(disallowAny, "{}");

		final JsonSchema disallowAnyInArray = schema("{\"properties\": {\"forbidden\": {\"disallow\": [\"any\"]}}}", JsonSchemaVersion.draftV3);
		assertValid(disallowAnyInArray, "{\"other\": 1}");
		assertInvalid(disallowAnyInArray, "{\"forbidden\": \"x\"}");
	}

	@Test
	void disallowValidator_integerWithZeroFractionSinceDraftV6() throws Exception {
		final JsonSchema disallowInteger = schema("{\"disallow\": \"integer\"}", JsonSchemaVersion.draftV7);
		assertInvalid(disallowInteger, "1.0");
		assertValid(disallowInteger, "1.5");
	}

	@Test
	void disallowValidator_invalidDefinitions() {
		final String[] invalidSchemas = {
				"{\"disallow\": null}",
				"{\"disallow\": 1}",
				"{\"disallow\": \"noType\"}",
				"{\"disallow\": [\"noType\"]}",
				"{\"disallow\": [1]}"
		};
		for (final String invalidSchema : invalidSchemas) {
			assertThrows(JsonSchemaDefinitionError.class, () -> schema(invalidSchema, JsonSchemaVersion.draftV3), invalidSchema);
		}
	}

	@Test
	void extendsValidator_objectAndArray() throws Exception {
		final JsonSchema extendsObject = schema("{\"extends\": {\"minimum\": 3}}", JsonSchemaVersion.draftV3);
		assertValid(extendsObject, "5");
		assertInvalid(extendsObject, "1");

		final JsonSchema extendsArray = schema("{\"extends\": [{\"type\": \"integer\"}, {\"maximum\": 10}]}", JsonSchemaVersion.draftV3);
		assertValid(extendsArray, "5");
		assertInvalid(extendsArray, "11");
		assertInvalid(extendsArray, "\"x\"");

		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"extends\": null}", JsonSchemaVersion.draftV3));
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"extends\": 1}", JsonSchemaVersion.draftV3));
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"extends\": [1]}", JsonSchemaVersion.draftV3));
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"extends\": {\"type\": 1}}", JsonSchemaVersion.draftV3));
	}

	@Test
	void contentEncodingValidator() throws Exception {
		final JsonSchema base64 = schema("{\"contentEncoding\": \"base64\"}", JsonSchemaVersion.draftV7);
		assertValid(base64, "\"" + Base64.getEncoder().encodeToString("hello".getBytes(StandardCharsets.UTF_8)) + "\"");
		assertInvalid(base64, "\"not base64 !!\"");
		assertValid(base64, "1");

		assertValid(schema("{\"contentEncoding\": \"7bit\"}", JsonSchemaVersion.draftV7), "\"anything !!\"");

		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"contentEncoding\": null}", JsonSchemaVersion.draftV7));
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"contentEncoding\": 1}", JsonSchemaVersion.draftV7));
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"contentEncoding\": \" \"}", JsonSchemaVersion.draftV7));
	}

	@Test
	void contentMediaTypeValidator() throws Exception {
		final JsonSchema jsonMediaType = schema("{\"contentMediaType\": \"application/json\"}", JsonSchemaVersion.draftV7);
		assertValid(jsonMediaType, "\"{\\\"a\\\": 1}\"");
		assertInvalid(jsonMediaType, "\"{ broken\"");
		assertValid(jsonMediaType, "1");

		final JsonSchema base64Json = schema("{\"contentMediaType\": \"application/json\", \"contentEncoding\": \"base64\"}", JsonSchemaVersion.draftV7);
		assertValid(base64Json, "\"" + Base64.getEncoder().encodeToString("[1, 2]".getBytes(StandardCharsets.UTF_8)) + "\"");
		assertInvalid(base64Json, "\"" + Base64.getEncoder().encodeToString("[1, ".getBytes(StandardCharsets.UTF_8)) + "\"");
		assertInvalid(base64Json, "\"%%%\"");

		assertValid(schema("{\"contentMediaType\": \"text/plain\"}", JsonSchemaVersion.draftV7), "\"{ no json\"");

		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"contentMediaType\": null}", JsonSchemaVersion.draftV7));
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"contentMediaType\": 1}", JsonSchemaVersion.draftV7));
		assertThrows(JsonSchemaDefinitionError.class, () -> schema("{\"contentMediaType\": \"\"}", JsonSchemaVersion.draftV7));
	}

	@Test
	void validateJsonNode_marksRootNode() throws Exception {
		final JsonSchema jsonSchema = schema("{\"type\": \"array\", \"items\": {\"type\": \"boolean\"}}", JsonSchemaVersion.draftV7);
		final JsonArray data = new JsonArray().add(new JsonValueBoolean(Boolean.TRUE));
		jsonSchema.validate(data);
		assertTrue(data.isRootNode());
		assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(new JsonArray().add(new JsonValueString("x"))));
	}
}
