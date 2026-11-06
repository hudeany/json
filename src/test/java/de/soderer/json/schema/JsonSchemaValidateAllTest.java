package de.soderer.json.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.JsonValueInteger;

/**
 * Tests {@link JsonSchema#validateAll(JsonNode)}, which collects all validation errors.
 */
@SuppressWarnings("static-method")
public class JsonSchemaValidateAllTest {
	private static final String SCHEMA = "{"
			+ "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
			+ "\"type\": \"object\","
			+ "\"required\": [\"name\", \"port\"],"
			+ "\"properties\": {"
			+ "  \"name\": {\"type\": \"string\", \"minLength\": 3},"
			+ "  \"port\": {\"type\": \"integer\", \"minimum\": 1, \"maximum\": 65535},"
			+ "  \"tags\": {\"type\": \"array\", \"items\": {\"type\": \"string\", \"maxLength\": 5}},"
			+ "  \"server\": {\"type\": \"object\", \"properties\": {\"host\": {\"type\": \"string\"}}, \"required\": [\"host\"]},"
			+ "  \"mode\": {\"anyOf\": [{\"const\": \"a\"}, {\"type\": \"integer\"}]},"
			+ "  \"ref\": {\"$ref\": \"#/$defs/positive\"}"
			+ "},"
			+ "\"additionalProperties\": false,"
			+ "\"$defs\": {\"positive\": {\"type\": \"integer\", \"minimum\": 0}}"
			+ "}";

	private static JsonSchema createSchema(final String schema) throws Exception {
		return new JsonSchema((JsonObject) JsonReader.readJsonItemString(schema));
	}

	private static List<String> getDataPaths(final JsonSchemaValidationResult result) {
		final List<String> dataPaths = new ArrayList<>();
		for (final JsonSchemaDataValidationError error : result.getErrors()) {
			dataPaths.add(error.getJsonDataPath().toString());
		}
		return dataPaths;
	}

	@Test
	void validData() throws Exception {
		final JsonSchemaValidationResult result = createSchema(SCHEMA).validateAll(JsonReader.readJsonItemString("{\"name\": \"server\", \"port\": 8443, \"tags\": [\"a\"], \"mode\": 3, \"ref\": 1}"));
		assertTrue(result.isValid());
		assertTrue(result.getErrors().isEmpty());
		assertFalse(result.isTruncated());
		assertEquals("Valid", result.toString());
	}

	@Test
	void allErrorsAreCollected() throws Exception {
		final JsonSchemaValidationResult result = createSchema(SCHEMA).validateAll(JsonReader.readJsonItemString(
				"{\"name\": \"x\", \"port\": 70000, \"tags\": [\"ok\", \"toolong\", 3], \"server\": \"text\", \"mode\": \"b\", \"ref\": -1, \"extra\": true}"));
		assertFalse(result.isValid());
		assertFalse(result.isTruncated());
		assertEquals(List.of("$.name", "$.port", "$.tags[1]", "$.tags[2]", "$.server", "$.mode", "$.ref", "$"), getDataPaths(result));
	}

	@Test
	void schemaPathsOfErrors() throws Exception {
		final JsonSchemaValidationResult result = createSchema(SCHEMA).validateAll(JsonReader.readJsonItemString("{\"name\": \"x\", \"port\": 1, \"ref\": -1}"));
		assertEquals(2, result.getErrors().size());
		assertEquals("$.properties.name.minLength", result.getErrors().get(0).getJsonSchemaPath().toString());
		// Keywords of a referenced schema continue after the "$ref"
		assertEquals("$.properties.ref.$ref.minimum", result.getErrors().get(1).getJsonSchemaPath().toString());
	}

	@Test
	void wrongTypeHasNoFollowUpErrors() throws Exception {
		// "server" is no object, so its missing "host" is no separate error
		final JsonSchemaValidationResult result = createSchema(SCHEMA).validateAll(JsonReader.readJsonItemString("{\"name\": \"server\", \"port\": 1, \"server\": \"text\"}"));
		assertEquals(1, result.getErrors().size());
		assertEquals("$.server", result.getErrors().get(0).getJsonDataPath().toString());
		assertEquals("$.properties.server.type", result.getErrors().get(0).getJsonSchemaPath().toString());
	}

	@Test
	void anyOfIsOneErrorWithCauses() throws Exception {
		final JsonSchemaValidationResult result = createSchema(SCHEMA).validateAll(JsonReader.readJsonItemString("{\"name\": \"server\", \"port\": 1, \"mode\": \"b\"}"));
		assertEquals(1, result.getErrors().size());
		final JsonSchemaDataValidationError anyOfError = result.getErrors().get(0);
		assertEquals("$.properties.mode.anyOf", anyOfError.getJsonSchemaPath().toString());
		final List<JsonSchemaDataValidationError> causes = anyOfError.getSubSchemaErrors();
		assertEquals(2, causes.size());
		assertEquals("$.properties.mode.anyOf[0].const", causes.get(0).getJsonSchemaPath().toString());
		assertEquals("$.properties.mode.anyOf[1].type", causes.get(1).getJsonSchemaPath().toString());
	}

	@Test
	void missingRequiredPropertiesAreOneErrorEach() throws Exception {
		final JsonSchemaValidationResult result = createSchema("{\"properties\": {\"a\": {\"type\": \"string\"}}, \"required\": [\"b\"], \"minProperties\": 3}")
				.validateAll(JsonReader.readJsonItemString("{\"a\": 1}"));
		assertEquals(3, result.getErrors().size());
	}

	@Test
	void allOfErrorsAreErrorsOfTheData() throws Exception {
		final JsonSchemaValidationResult result = createSchema("{\"allOf\": [{\"properties\": {\"a\": {\"type\": \"string\"}}}, {\"properties\": {\"b\": {\"type\": \"string\"}}}]}")
				.validateAll(JsonReader.readJsonItemString("{\"a\": 1, \"b\": 2}"));
		assertEquals(List.of("$.a", "$.b"), getDataPaths(result));
	}

	@Test
	void notAndIfDoNotReportTheirExpectedFailures() throws Exception {
		final JsonSchemaValidationResult result = createSchema("{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"not\": {\"type\": \"string\"}, \"if\": {\"minimum\": 10}, \"then\": {\"multipleOf\": 2}, \"else\": {\"maximum\": 5}}")
				.validateAll(JsonReader.readJsonItemString("7"));
		// Only the "else" branch fails: "not" succeeds and the failing "if" is no error
		assertEquals(1, result.getErrors().size());
		assertEquals("$.else.maximum", result.getErrors().get(0).getJsonSchemaPath().toString());
	}

	@Test
	void unevaluatedPropertiesWithoutFollowUpErrors() throws Exception {
		final JsonSchemaValidationResult result = createSchema("{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\", \"properties\": {\"a\": {\"type\": \"string\"}}, \"unevaluatedProperties\": false}")
				.validateAll(JsonReader.readJsonItemString("{\"a\": 1, \"b\": 2}"));
		// The invalid "a" was evaluated by "properties", so only "b" is unevaluated
		assertEquals(List.of("$.a", "$.b"), getDataPaths(result));
	}

	@Test
	void resultIsLimited() throws Exception {
		final JsonArray data = new JsonArray();
		for (int i = 0; i < 100_000; i++) {
			data.add(new JsonValueInteger(i));
		}
		final JsonSchema schema = createSchema("{\"type\": \"array\", \"items\": {\"type\": \"string\"}}");

		final JsonSchemaValidationResult defaultResult = schema.validateAll(data);
		assertEquals(JsonSchemaConfiguration.DEFAULT_MAXIMUM_VALIDATION_ERRORS, defaultResult.getErrors().size());
		assertTrue(defaultResult.isTruncated());
		assertTrue(defaultResult.toString().endsWith("... more errors not reported\n"));

		schema.setMaximumValidationErrors(3);
		final JsonSchemaValidationResult limitedResult = schema.validateAll(data);
		assertEquals(3, limitedResult.getErrors().size());
		assertTrue(limitedResult.isTruncated());
	}

	@Test
	void exactlyMaximumErrorsIsNotTruncated() throws Exception {
		final JsonSchema schema = createSchema("{\"type\": \"array\", \"items\": {\"type\": \"string\"}}");
		schema.setMaximumValidationErrors(3);
		final JsonSchemaValidationResult result = schema.validateAll(JsonReader.readJsonItemString("[1, 2, 3]"));
		assertEquals(3, result.getErrors().size());
		assertFalse(result.isTruncated());
	}

	@Test
	void limitFromConfiguration() throws Exception {
		final JsonSchemaConfiguration configuration = new JsonSchemaConfiguration().withMaximumValidationErrors(2);
		final JsonSchema schema = new JsonSchema((JsonObject) JsonReader.readJsonItemString("{\"items\": {\"type\": \"string\"}}"), configuration);
		assertEquals(2, schema.getMaximumValidationErrors());
		assertEquals(2, schema.validateAll(JsonReader.readJsonItemString("[1, 2, 3]")).getErrors().size());
		assertThrows(IllegalArgumentException.class, () -> configuration.setMaximumValidationErrors(0));
		assertThrows(IllegalArgumentException.class, () -> schema.setMaximumValidationErrors(0));
	}

	@Test
	void validateStillThrowsFirstErrorWithSchemaPath() throws Exception {
		final JsonSchema schema = createSchema(SCHEMA);
		final JsonSchemaDataValidationError error = assertThrows(JsonSchemaDataValidationError.class, () -> schema.validate(JsonReader.readJsonItemString("{\"name\": \"x\", \"port\": 70000}")));
		assertEquals("$.name", error.getJsonDataPath().toString());
		assertEquals("$.properties.name.minLength", error.getJsonSchemaPath().toString());
	}

	@Test
	void validateAllFromStream() throws Exception {
		final JsonSchema schema = createSchema(SCHEMA);
		final JsonSchemaValidationResult result = schema.validateAll(new ByteArrayInputStream("{name: 'x', port: 1}".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
		assertEquals(1, result.getErrors().size());
		assertThrows(JsonSchemaDataValidationError.class, () -> schema.validateAll(new ByteArrayInputStream("{".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
	}

	@Test
	void collectingIsCleanedUpAfterValidation() throws Exception {
		final JsonSchema schema = createSchema(SCHEMA);
		schema.validateAll(JsonReader.readJsonItemString("{\"name\": \"x\"}"));
		// A following validation on the same thread must not collect
		assertThrows(JsonSchemaDataValidationError.class, () -> schema.validate(JsonReader.readJsonItemString("{\"name\": \"x\"}")));
		assertEquals(2, schema.validateAll(JsonReader.readJsonItemString("{\"name\": \"x\"}")).getErrors().size());
	}
}
