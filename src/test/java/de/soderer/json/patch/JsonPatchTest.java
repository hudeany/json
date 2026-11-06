package de.soderer.json.patch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueString;
import de.soderer.json.exception.DuplicateKeyException;

/**
 * Tests {@link JsonPatch} with the official JSON Patch test suite
 * (https://github.com/json-patch/json-patch-tests) and {@link JsonMergePatch} with the examples of
 * RFC 7396.
 */
@SuppressWarnings("static-method")
public class JsonPatchTest {
	private static JsonNode json(final String data) throws Exception {
		return JsonReader.readJsonItemString(data);
	}

	/**
	 * Operation objects of the official test suite with a duplicate key "op", which must be rejected.
	 * {@link JsonReader} already rejects duplicate keys while reading, so these operations are
	 * replaced by an invalid operation for reading the test files, and the rejection by the reader is
	 * checked separately in {@link #duplicateKeyInOperationIsRejected()}.
	 */
	private static final Pattern DUPLICATE_OP_PATTERN = Pattern.compile("\\{[^{}]*\"op\"[^{}]*\"op\"[^{}]*\\}");

	/** The original operations with duplicate keys found in the test files. */
	private static final List<String> DUPLICATE_KEY_OPERATIONS = new ArrayList<>();

	private static JsonArray readTestFile(final String fileName) throws Exception {
		final String testData;
		try (InputStream inputStream = JsonPatchTest.class.getClassLoader().getResourceAsStream("json/patch/" + fileName)) {
			testData = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
		final Matcher matcher = DUPLICATE_OP_PATTERN.matcher(testData);
		final StringBuilder replacedTestData = new StringBuilder();
		while (matcher.find()) {
			DUPLICATE_KEY_OPERATIONS.add(matcher.group());
			matcher.appendReplacement(replacedTestData, Matcher.quoteReplacement("{ \"op\": \"duplicate key 'op' is rejected by JsonReader\", \"path\": \"\" }"));
		}
		matcher.appendTail(replacedTestData);
		return (JsonArray) JsonReader.readJsonItemString(replacedTestData.toString());
	}

	@Test
	void duplicateKeyInOperationIsRejected() throws Exception {
		readTestFile("tests.json");
		readTestFile("spec_tests.json");
		assertFalse(DUPLICATE_KEY_OPERATIONS.isEmpty());
		for (final String operation : DUPLICATE_KEY_OPERATIONS) {
			assertThrows(DuplicateKeyException.class, () -> JsonReader.readJsonItemString("[" + operation + "]"), operation);
		}
	}

	@TestFactory
	Stream<DynamicTest> officialTestSuite() throws Exception {
		final List<DynamicTest> tests = new ArrayList<>();
		for (final String fileName : new String[] { "tests.json", "spec_tests.json" }) {
			final JsonArray testCases = readTestFile(fileName);
			for (int i = 0; i < testCases.size(); i++) {
				final JsonObject testCase = (JsonObject) testCases.get(i);
				if (testCase.get("disabled") instanceof final JsonValueBoolean disabled && disabled.getValue()) {
					continue;
				}
				final String name = fileName + " #" + i + (testCase.get("comment") instanceof final JsonValueString comment ? ": " + comment.getValue() : "");
				tests.add(DynamicTest.dynamicTest(name, () -> executeTestCase(testCase)));
			}
		}
		return tests.stream();
	}

	private static void executeTestCase(final JsonObject testCase) throws Exception {
		final JsonNode document = testCase.get("doc");
		final JsonNode documentBefore = JsonPatch.deepCopy(document);
		if (testCase.containsKey("error")) {
			assertThrows(JsonPatchException.class, () -> JsonPatch.apply(document, toPatchArray(testCase.get("patch"))));
		} else {
			final JsonNode result = JsonPatch.apply(document, toPatchArray(testCase.get("patch")));
			if (testCase.containsKey("expected")) {
				assertTrue(JsonPatch.jsonEquals(testCase.get("expected"), result), () -> "Expected " + testCase.get("expected") + " but was " + result);
			}
		}
		// The original document is never changed
		assertTrue(JsonPatch.jsonEquals(documentBefore, document));
	}

	private static JsonArray toPatchArray(final JsonNode patch) throws JsonPatchException {
		if (patch instanceof final JsonArray patchArray) {
			return patchArray;
		}
		throw new JsonPatchException("Patch is no array", -1);
	}

	@Test
	void failedPatchIsAtomic() throws Exception {
		final JsonNode document = json("{\"a\": 1}");
		assertThrows(JsonPatchException.class, () -> JsonPatch.apply(document, (JsonArray) json("[{\"op\": \"add\", \"path\": \"/b\", \"value\": 2}, {\"op\": \"test\", \"path\": \"/a\", \"value\": 99}]")));
		assertEquals("{\"a\":1}", document.toString().replaceAll("\\s+", ""));
	}

	@Test
	void errorContainsOperationIndex() throws Exception {
		final JsonPatchException exception = assertThrows(JsonPatchException.class,
				() -> JsonPatch.apply(json("{\"a\": 1}"), (JsonArray) json("[{\"op\": \"test\", \"path\": \"/a\", \"value\": 1}, {\"op\": \"remove\", \"path\": \"/x\"}]")));
		assertEquals(1, exception.getOperationIndex());
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"{\"a\": 1, \"b\": [1, 2, 3], \"c\": {\"d\": \"x\"}}  | {\"a\": 2, \"b\": [1, 3], \"c\": {\"e\": null}, \"f\": true}",
			"[1, 2, 3]                                       | [1, 2, 3, 4, 5]",
			"[1, 2, 3, 4]                                    | [9]",
			"{\"a/b\": 1, \"m~n\": 2}                        | {\"a/b\": 3}",
			"{\"a\": 1}                                      | [1]",
			"{\"a\": 1}                                      | {\"a\": 1.0}",
			"null                                            | {\"a\": null}"
	})
	void createAndApplyRoundTrip(final String source, final String target) throws Exception {
		final JsonNode sourceNode = json(source);
		final JsonNode targetNode = json(target);
		final JsonArray patch = JsonPatch.create(sourceNode, targetNode);
		final JsonNode result = JsonPatch.apply(sourceNode, patch);
		assertTrue(JsonPatch.jsonEquals(targetNode, result), () -> "Patch " + patch + " produced " + result);
	}

	@Test
	void createProducesMinimalChangesForObjects() throws Exception {
		final JsonArray patch = JsonPatch.create(json("{\"a\": 1, \"b\": {\"c\": 1, \"d\": 2}}"), json("{\"a\": 1, \"b\": {\"c\": 1, \"d\": 3}}"));
		assertEquals("[{\"op\":\"replace\",\"path\":\"/b/d\",\"value\":3}]", patch.toString().replaceAll("\\s+", ""));
		assertEquals(0, JsonPatch.create(json("{\"a\": [1, {\"b\": 2}]}"), json("{\"a\": [1, {\"b\": 2}]}")).size());
	}

	@Test
	void jsonPointerEscaping() throws Exception {
		assertEquals(List.of("a/b", "m~n", "~1"), JsonPatch.parsePointer("/a~1b/m~0n/~01", 0));
		assertEquals("/a~1b/m~0n/~01", JsonPatch.toPointer(List.of("a/b", "m~n", "~1")));
		assertThrows(JsonPatchException.class, () -> JsonPatch.parsePointer("a", 0));
		assertThrows(JsonPatchException.class, () -> JsonPatch.parsePointer("/a~2", 0));
		assertThrows(JsonPatchException.class, () -> JsonPatch.parsePointer("/a~", 0));
	}

	/**
	 * Examples of RFC 7396, appendix A: original document, merge patch, expected result.
	 */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"{\"a\":\"b\"}                 | {\"a\":\"c\"}                 | {\"a\":\"c\"}",
			"{\"a\":\"b\"}                 | {\"b\":\"c\"}                 | {\"a\":\"b\",\"b\":\"c\"}",
			"{\"a\":\"b\"}                 | {\"a\":null}                  | {}",
			"{\"a\":\"b\",\"b\":\"c\"}     | {\"a\":null}                  | {\"b\":\"c\"}",
			"{\"a\":[\"b\"]}               | {\"a\":\"c\"}                 | {\"a\":\"c\"}",
			"{\"a\":\"c\"}                 | {\"a\":[\"b\"]}               | {\"a\":[\"b\"]}",
			"{\"a\":{\"b\":\"c\"}}         | {\"a\":{\"b\":\"d\",\"c\":null}} | {\"a\":{\"b\":\"d\"}}",
			"{\"a\":[{\"b\":\"c\"}]}       | {\"a\":[1]}                   | {\"a\":[1]}",
			"[\"a\",\"b\"]                 | [\"c\",\"d\"]                 | [\"c\",\"d\"]",
			"{\"a\":\"b\"}                 | [\"c\"]                       | [\"c\"]",
			"{\"a\":\"foo\"}               | null                          | null",
			"{\"a\":\"foo\"}               | \"bar\"                       | \"bar\"",
			"{\"e\":null}                  | {\"a\":1}                     | {\"e\":null,\"a\":1}",
			"[1,2]                         | {\"a\":\"b\",\"c\":null}      | {\"a\":\"b\"}",
			"{}                            | {\"a\":{\"bb\":{\"ccc\":null}}} | {\"a\":{\"bb\":{}}}"
	})
	void mergePatchRfcExamples(final String original, final String mergePatch, final String expected) throws Exception {
		final JsonNode originalNode = json(original);
		final JsonNode result = JsonMergePatch.apply(originalNode, json(mergePatch));
		assertTrue(JsonPatch.jsonEquals(json(expected), result), () -> "Expected " + expected + " but was " + result);
		// The original document is never changed
		assertTrue(JsonPatch.jsonEquals(json(original), originalNode));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"{\"a\": 1, \"b\": {\"c\": 2, \"d\": 3}, \"e\": [1]} | {\"a\": 1, \"b\": {\"c\": 5}, \"e\": [1, 2], \"f\": {\"g\": true}}",
			"{\"a\": 1}                                        | [1, 2]",
			"[1]                                               | {\"a\": 1}",
			"{\"a\": {\"b\": 1}}                               | {\"a\": 2}"
	})
	void mergePatchCreateRoundTrip(final String source, final String target) throws Exception {
		final JsonNode mergePatch = JsonMergePatch.create(json(source), json(target));
		assertTrue(JsonPatch.jsonEquals(json(target), JsonMergePatch.apply(json(source), mergePatch)), () -> "Merge patch " + mergePatch);
	}

	@Test
	void mergePatchCannotExpressNullValues() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> JsonMergePatch.create(json("{\"a\": 1}"), json("{\"a\": null}")));
		assertThrows(IllegalArgumentException.class, () -> JsonMergePatch.create(json("{}"), json("{\"a\": {\"b\": null}}")));
	}
}
