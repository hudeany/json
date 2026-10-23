package de.soderer.json.schema;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.soderer.json.JsonReader;

/**
 * Covers the checks of invalid validator definitions and the type checks of the validators in
 * simple mode.
 */
@SuppressWarnings("static-method")
class ValidatorDefinitionCoverageTest {
	private static JsonSchema schema(final String schemaText, final JsonSchemaVersion version) throws Exception {
		return new JsonSchema(new ByteArrayInputStream(schemaText.getBytes(StandardCharsets.UTF_8)), new JsonSchemaConfiguration().withJsonSchemaVersion(version));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"minItems\": null}", "{\"minItems\": \"abc\"}", "{\"minItems\": true}", "{\"minItems\": -1}", "{\"minItems\": 1.5}",
			"{\"maxItems\": null}", "{\"maxItems\": \"abc\"}", "{\"maxItems\": true}", "{\"maxItems\": -1}", "{\"maxItems\": 1.5}",
			"{\"minLength\": null}", "{\"minLength\": \"abc\"}", "{\"minLength\": true}", "{\"minLength\": -1}",
			"{\"maxLength\": null}", "{\"maxLength\": \"abc\"}", "{\"maxLength\": true}", "{\"maxLength\": -1}",
			"{\"minProperties\": null}", "{\"minProperties\": \"abc\"}", "{\"minProperties\": true}", "{\"minProperties\": -1}",
			"{\"maxProperties\": null}", "{\"maxProperties\": \"abc\"}", "{\"maxProperties\": true}", "{\"maxProperties\": -1}",
			"{\"minimum\": null}", "{\"minimum\": \"abc\"}", "{\"minimum\": true}",
			"{\"maximum\": null}", "{\"maximum\": \"abc\"}", "{\"maximum\": true}",
			"{\"exclusiveMinimum\": null}", "{\"exclusiveMinimum\": \"abc\"}", "{\"exclusiveMinimum\": true}",
			"{\"exclusiveMaximum\": null}", "{\"exclusiveMaximum\": \"abc\"}", "{\"exclusiveMaximum\": true}",
			"{\"multipleOf\": null}", "{\"multipleOf\": \"abc\"}", "{\"multipleOf\": true}",
			"{\"pattern\": null}", "{\"pattern\": 1}", "{\"pattern\": \"[unclosed\"}",
			"{\"uniqueItems\": \"x\"}", "{\"uniqueItems\": 1}",
			"{\"required\": \"a\"}", "{\"required\": [1]}",
			"{\"propertyNames\": null}", "{\"propertyNames\": 1}",
			"{\"patternProperties\": null}", "{\"patternProperties\": 1}", "{\"patternProperties\": {\"[\": {}}}",
			"{\"properties\": null}", "{\"properties\": 1}",
			"{\"items\": null}", "{\"items\": 1}", "{\"items\": [1]}", "{\"items\": [{}], \"additionalItems\": 1}", "{\"items\": [{}], \"additionalItems\": null}",
			"{\"dependencies\": null}", "{\"dependencies\": 1}", "{\"dependencies\": {\"a\": 1}}",
			"{\"enum\": null}", "{\"enum\": 1}", "{\"enum\": []}",
			"{\"format\": null}", "{\"format\": 1}",
			"{\"type\": null}", "{\"type\": 1}", "{\"type\": \"unknown\"}", "{\"type\": [1]}", "{\"type\": [\"unknown\"]}"
	})
	void invalidDefinitions_draftV7(final String schemaText) {
		assertThrows(JsonSchemaDefinitionError.class, () -> schema(schemaText, JsonSchemaVersion.draftV7), schemaText);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"minimum\": 1, \"exclusiveMinimum\": null}",
			"{\"minimum\": 1, \"exclusiveMinimum\": \"x\"}",
			"{\"maximum\": 1, \"exclusiveMaximum\": null}",
			"{\"maximum\": 1, \"exclusiveMaximum\": \"x\"}"
	})
	void invalidDefinitions_draftV4(final String schemaText) {
		assertThrows(JsonSchemaDefinitionError.class, () -> schema(schemaText, JsonSchemaVersion.draftV4), schemaText);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"maxItems\": \"2\"}", "{\"maxItems\": 2.0}", "{\"maxItems\": 99999999999}",
			"{\"minLength\": \"1\"}", "{\"maxLength\": 2.0}", "{\"maxLength\": 99999999999}",
			"{\"minProperties\": \"1\"}", "{\"maxProperties\": 2.0}", "{\"maxProperties\": 99999999999}",
			"{\"minItems\": \"1\"}", "{\"minimum\": \"1.5\"}", "{\"maximum\": \"1.5\"}",
			"{\"exclusiveMinimum\": \"1\"}", "{\"exclusiveMaximum\": \"1\"}", "{\"multipleOf\": \"0.5\"}"
	})
	void validDefinitions_withStringAndLargeNumbers(final String schemaText) {
		assertDoesNotThrow(() -> schema(schemaText, JsonSchemaVersion.draftV7), schemaText);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"{\"minItems\": 1}|\"x\"",
			"{\"maxItems\": 1}|\"x\"",
			"{\"minLength\": 1}|[1]",
			"{\"maxLength\": 1}|[1]",
			"{\"minProperties\": 1}|[1]",
			"{\"maxProperties\": 1}|[1]",
			"{\"minimum\": 1}|\"x\"",
			"{\"maximum\": 1}|\"x\"",
			"{\"multipleOf\": 2}|\"x\"",
			"{\"pattern\": \"^a\"}|1",
			"{\"uniqueItems\": true}|\"x\"",
			"{\"required\": [\"a\"]}|\"x\"",
			"{\"properties\": {\"a\": {}}}|\"x\"",
			"{\"patternProperties\": {\"^a\": {}}}|\"x\"",
			"{\"items\": {}}|\"x\"",
			"{\"dependencies\": {\"a\": [\"b\"]}}|\"x\""
	})
	void typeMismatch_failsOnlyInSimpleMode(final String schemaText, final String data) throws Exception {
		final JsonSchema simpleSchema = schema(schemaText, JsonSchemaVersion.simple);
		assertThrows(JsonSchemaDataValidationError.class, () -> simpleSchema.validate(JsonReader.readJsonItemString(data)), schemaText);
		final JsonSchema v7Schema = schema(schemaText, JsonSchemaVersion.draftV7);
		assertDoesNotThrow(() -> v7Schema.validate(JsonReader.readJsonItemString(data)), schemaText);
	}

	@Test
	void stringLimits_areApplied() throws Exception {
		final JsonSchema maxItems = schema("{\"maxItems\": \"2\"}", JsonSchemaVersion.draftV7);
		assertThrows(JsonSchemaDataValidationError.class, () -> maxItems.validate(JsonReader.readJsonItemString("[1, 2, 3]")));
		assertDoesNotThrow(() -> maxItems.validate(JsonReader.readJsonItemString("[1, 2]")));

		final JsonSchema minLength = schema("{\"minLength\": \"2\"}", JsonSchemaVersion.draftV7);
		assertThrows(JsonSchemaDataValidationError.class, () -> minLength.validate(JsonReader.readJsonItemString("\"a\"")));

		final JsonSchema minProperties = schema("{\"minProperties\": \"1\"}", JsonSchemaVersion.draftV7);
		assertThrows(JsonSchemaDataValidationError.class, () -> minProperties.validate(JsonReader.readJsonItemString("{}")));
	}
}
