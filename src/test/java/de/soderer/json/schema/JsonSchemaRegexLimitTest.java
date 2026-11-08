package de.soderer.json.schema;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.JsonValueString;

/**
 * Tests of the protection against regular expressions with catastrophic backtracking (ReDoS) and
 * against StackOverflowErrors of the Java regex engine.
 */
@SuppressWarnings("static-method")
public class JsonSchemaRegexLimitTest {
	/** Pattern with polynomial backtracking (n^12), which Java does not optimize. */
	private static final String CATASTROPHIC_PATTERN = "^(.*a){12}$";

	/** Input for {@link #CATASTROPHIC_PATTERN}, which would take far longer than any test timeout. */
	private static final String CATASTROPHIC_INPUT = "a".repeat(60) + "!";

	private static JsonNode json(final String text) throws Exception {
		try (JsonReader reader = new JsonReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)))) {
			return reader.read();
		}
	}

	private static JsonSchema schema(final JsonObject schemaObject, final JsonSchemaConfiguration configuration) throws Exception {
		return new JsonSchema(schemaObject, configuration.withJsonSchemaVersion(JsonSchemaVersion.draft2020_12));
	}

	private static JsonObject patternSchema(final String keyword, final String pattern) throws Exception {
		final JsonObject schemaObject = new JsonObject();
		if ("pattern".equals(keyword)) {
			schemaObject.add("pattern", pattern);
		} else {
			final JsonObject patternProperties = new JsonObject();
			patternProperties.add(pattern, true);
			schemaObject.add("patternProperties", patternProperties);
			schemaObject.add("additionalProperties", false);
		}
		return schemaObject;
	}

	@Test
	void catastrophicPatternIsAborted() {
		final Pattern pattern = JsonSchemaRegexUtilities.compile(CATASTROPHIC_PATTERN);
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			final RegexEvaluationLimitException exception = assertThrows(RegexEvaluationLimitException.class, () -> JsonSchemaRegexUtilities.find(pattern, CATASTROPHIC_INPUT, JsonSchemaRegexUtilities.DEFAULT_REGEX_STEP_LIMIT));
			assertTrue(exception.getMessage().contains("steps"), exception.getMessage());
		});
	}

	@Test
	void ordinaryPatternsAreNotAffected() throws Exception {
		assertTrue(JsonSchemaRegexUtilities.find(JsonSchemaRegexUtilities.compile("b"), "a".repeat(5_000_000) + "b", JsonSchemaRegexUtilities.DEFAULT_REGEX_STEP_LIMIT));
		assertTrue(JsonSchemaRegexUtilities.matches(JsonSchemaRegexUtilities.compile("^[a-z]+$"), "a".repeat(5_000_000), JsonSchemaRegexUtilities.DEFAULT_REGEX_STEP_LIMIT));
		assertFalse(JsonSchemaRegexUtilities.find(JsonSchemaRegexUtilities.compile(CATASTROPHIC_PATTERN), "a".repeat(14) + "!", JsonSchemaRegexUtilities.DEFAULT_REGEX_STEP_LIMIT));
	}

	@Test
	void stepLimitScalesWithInputAndCanBeSwitchedOff() {
		assertEquals(Long.MAX_VALUE, JsonSchemaRegexUtilities.getStepLimit(0, 10));
		assertEquals(Long.MAX_VALUE, JsonSchemaRegexUtilities.getStepLimit(-1, 10));
		assertEquals(100 + 10 * JsonSchemaRegexUtilities.REGEX_STEPS_PER_INPUT_CHARACTER, JsonSchemaRegexUtilities.getStepLimit(100, 10));
		assertEquals(Long.MAX_VALUE, JsonSchemaRegexUtilities.getStepLimit(Long.MAX_VALUE - 5, 10));
	}

	@Test
	void smallStepLimitAbortsEarly() throws Exception {
		// About 4 million steps: fine without limit, but more than 1 + 101 * REGEX_STEPS_PER_INPUT_CHARACTER
		final Pattern pattern = JsonSchemaRegexUtilities.compile("^(.*a){4}$");
		final String input = "a".repeat(100) + "!";
		assertFalse(JsonSchemaRegexUtilities.find(pattern, input, 0));
		assertThrows(RegexEvaluationLimitException.class, () -> JsonSchemaRegexUtilities.find(pattern, input, 1));
	}

	@Test
	void deepRecursionOfRegexEngineIsReported() {
		final Pattern pattern = JsonSchemaRegexUtilities.compile("^(a|b)*$");
		final RegexEvaluationLimitException exception = assertThrows(RegexEvaluationLimitException.class, () -> JsonSchemaRegexUtilities.find(pattern, "ab".repeat(500_000), 0));
		assertTrue(exception.getMessage().contains("recursion"), exception.getMessage());
	}

	@Test
	void deeplyNestedPatternIsNoValidRegex() {
		final String nestedPattern = "(".repeat(200_000) + "a" + ")".repeat(200_000);
		assertFalse(JsonSchemaRegexUtilities.isValidEcmaScriptRegex(nestedPattern));
		assertThrows(java.util.regex.PatternSyntaxException.class, () -> JsonSchemaRegexUtilities.compile(nestedPattern));
	}

	@Test
	void patternKeywordReportsDataAsInvalid() throws Exception {
		final JsonSchema jsonSchema = schema(patternSchema("pattern", CATASTROPHIC_PATTERN), new JsonSchemaConfiguration());
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			final JsonSchemaDataValidationError error = assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(new JsonValueString(CATASTROPHIC_INPUT)));
			assertTrue(error.getMessage().contains("could not be evaluated"), error.getMessage());
		});
		assertDoesNotThrow(() -> jsonSchema.validate(json("\"aaaaaaaaaaaa\"")));
	}

	@Test
	void patternPropertiesAndAdditionalPropertiesAreLimited() throws Exception {
		final JsonSchema jsonSchema = schema(patternSchema("patternProperties", CATASTROPHIC_PATTERN), new JsonSchemaConfiguration());
		final JsonObject data = new JsonObject();
		data.add(CATASTROPHIC_INPUT, 1);
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(data));
		});
	}

	@Test
	void validateAllCollectsLimitErrors() throws Exception {
		final JsonSchema jsonSchema = schema((JsonObject) json("{\"items\": {\"pattern\": \"" + CATASTROPHIC_PATTERN.replace("\\", "\\\\") + "\"}}"), new JsonSchemaConfiguration());
		final JsonArray data = new JsonArray();
		data.add(CATASTROPHIC_INPUT);
		data.add("aaaaaaaaaaaa");
		data.add(CATASTROPHIC_INPUT);
		assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
			assertEquals(2, jsonSchema.validateAll(data).getErrors().size());
		});
	}

	@Test
	void builtInFormatsDoNotOverflowTheStack() {
		// The JSON pointer and email patterns repeat groups, which recurses per repetition in the Java regex engine
		final String longJsonPointer = "/a".repeat(200_000);
		assertFalse(assertDoesNotThrow(() -> JsonSchemaFormats.isValidJsonPointer(longJsonPointer)));
		assertTrue(JsonSchemaFormats.isValidJsonPointer("/a/b~0/c~1"));
	}
}
