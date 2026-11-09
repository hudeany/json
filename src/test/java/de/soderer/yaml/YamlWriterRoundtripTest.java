package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import de.soderer.json.JsonNode;
import de.soderer.json.patch.JsonPatch;
import de.soderer.json.utilities.LineBreak;
import de.soderer.yaml.YamlTestSuite.TestCase;
import de.soderer.yaml.data.YamlDocument;

/**
 * Roundtrip test of the {@link YamlWriter} with the valid test cases of the official YAML test
 * suite (https://github.com/yaml/yaml-test-suite):
 * <ol>
 * <li>read the YAML data of the test case and convert each document to JSON</li>
 * <li>write the documents (and comments after the last document) with {@link YamlWriter}</li>
 * <li>read the written YAML data again and compare the JSON of each document with step 1</li>
 * </ol>
 * The roundtrip is executed with several {@link YamlFormat} settings. Additionally the default
 * format must be stable: writing the data read back again must result in the same text.
 * <p>
 * Test cases, which YamlReader rejects by design (see "yaml/yaml-test-suite-intended-deviations.txt")
 * or which cannot be converted to JSON (like complex mapping keys), are skipped.
 * </p>
 */
@SuppressWarnings("static-method")
@DisplayName("YAML writer roundtrip with the official YAML test suite")
public class YamlWriterRoundtripTest {
	/** The bundled test suite. */
	private static YamlTestSuite testSuite;

	/**
	 * Formats to write the roundtrip with, by display name. A new instance is created for each
	 * test, so the tests do not share mutable format objects.
	 */
	private static final Map<String, Supplier<YamlFormat>> FORMATS = new LinkedHashMap<>();
	static {
		FORMATS.put("default format", YamlFormat::new);
		FORMATS.put("ignore flow style", () -> new YamlFormat().withIgnoreFlowStyleSettings(true));
		FORMATS.put("indentation 4, always quoted strings", () -> new YamlFormat().withIndentationSize(4).withAlwaysQuoteAllStrings());
		FORMATS.put("no extra indentation of sequences in mappings", () -> new YamlFormat().withUseNoExtraIndentationForYamlSequencesInYamlMappingProperties(true));
		FORMATS.put("omit comments and empty lines", () -> new YamlFormat().withOmitComments(true).withOmitEmptyLines(true));
		FORMATS.put("Windows linebreaks", () -> new YamlFormat().withLinebreak(LineBreak.Windows));
	}

	/**
	 * Reads the bundled test suite.
	 *
	 * @throws Exception
	 *             if the test suite resource is missing
	 */
	@BeforeAll
	static void loadTestSuite() throws Exception {
		testSuite = YamlTestSuite.loadBundled();
	}

	@TestFactory
	Stream<DynamicNode> roundtripWithJsonComparison() {
		final List<DynamicNode> containers = new ArrayList<>();
		for (final Map.Entry<String, Supplier<YamlFormat>> format : FORMATS.entrySet()) {
			final List<DynamicNode> tests = new ArrayList<>();
			for (final TestCase testCase : testSuite.getTestCases()) {
				if (!testCase.isError()) {
					tests.add(DynamicTest.dynamicTest(testCase.getId() + " " + testCase.getName(), () -> checkRoundtrip(testCase, format.getValue().get())));
				}
			}
			containers.add(DynamicContainer.dynamicContainer(format.getKey(), tests));
		}
		return containers.stream();
	}

	@TestFactory
	Stream<DynamicTest> writingIsStable() {
		final List<DynamicTest> tests = new ArrayList<>();
		for (final TestCase testCase : testSuite.getTestCases()) {
			if (!testCase.isError()) {
				tests.add(DynamicTest.dynamicTest(testCase.getId() + " " + testCase.getName(), () -> checkStableWriting(testCase)));
			}
		}
		return tests.stream();
	}

	private static void checkRoundtrip(final TestCase testCase, final YamlFormat yamlFormat) throws Exception {
		final ReadResult original = readSupported(testCase);
		final List<JsonNode> expectedJson = toJson(original.documents);
		Assumptions.assumeTrue(expectedJson != null, () -> testCase.getId() + " cannot be converted to JSON");

		final String writtenYaml = write(original, yamlFormat);
		final ReadResult reread;
		try {
			reread = read(writtenYaml);
		} catch (final Exception e) {
			throw new AssertionError("Written YAML cannot be read again: " + e.getMessage() + "\n" + writtenYaml, e);
		}

		Assertions.assertEquals(original.documents.size(), reread.documents.size(), () -> "Number of documents differs:\n" + writtenYaml);
		final List<JsonNode> actualJson = toJson(reread.documents);
		Assertions.assertNotNull(actualJson, () -> "Written YAML cannot be converted to JSON:\n" + writtenYaml);
		if (expectedJson!= null) {
			for (int i = 0; i < expectedJson.size(); i++) {
				final int documentIndex = i;
				Assertions.assertTrue(JsonPatch.jsonEquals(expectedJson.get(i), actualJson.get(i)),
						() -> "Document " + documentIndex + ": expected " + compact(expectedJson.get(documentIndex)) + " but was " + compact(actualJson.get(documentIndex)) + "\nWritten YAML:\n" + writtenYaml);
			}
		}
		if (!yamlFormat.isOmitComments()) {
			Assertions.assertEquals(original.trailingComments, reread.trailingComments, () -> "Trailing comments differ:\n" + writtenYaml);
		}
	}

	private static void checkStableWriting(final TestCase testCase) throws Exception {
		final ReadResult original = readSupported(testCase);
		final String firstWrite = write(original, new YamlFormat());
		final String secondWrite = write(read(firstWrite), new YamlFormat());
		Assertions.assertEquals(firstWrite, secondWrite, "Writing the data read back again results in another text");
	}

	/**
	 * Reads the YAML data of a test case, which YamlReader accepts.
	 */
	private static ReadResult readSupported(final TestCase testCase) {
		try {
			return read(testCase.getYaml());
		} catch (final Exception e) {
			// Intended deviations from the test suite, covered by YamlTestSuiteTest
			Assumptions.assumeTrue(false, () -> testCase.getId() + " is not read by YamlReader: " + e.getMessage());
			return null;
		}
	}

	/** Documents and trailing comments of YAML data. */
	private static final class ReadResult {
		private final List<YamlDocument> documents = new ArrayList<>();
		private List<String> trailingComments;
	}

	private static ReadResult read(final String yaml) throws Exception {
		final ReadResult result = new ReadResult();
		try (YamlReader reader = new YamlReader(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))) {
			YamlDocument document;
			while ((document = reader.readDocument()) != null) {
				result.documents.add(document);
			}
			result.trailingComments = reader.getTrailingComments();
		}
		return result;
	}

	private static String write(final ReadResult data, final YamlFormat yamlFormat) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (YamlWriter writer = new YamlWriter(outputStream, yamlFormat)) {
			writer.writeDocumentList(data.documents);
			writer.writeTrailingComments(data.trailingComments);
		}
		return outputStream.toString(StandardCharsets.UTF_8);
	}

	/**
	 * Converts documents to JSON.
	 *
	 * @return the JSON data of each document, or null if a document cannot be converted
	 */
	private static List<JsonNode> toJson(final List<YamlDocument> documents) {
		final List<JsonNode> result = new ArrayList<>();
		try {
			for (final YamlDocument document : documents) {
				result.add(YamlToJsonConverter.convert(document));
			}
		} catch (@SuppressWarnings("unused") final Exception e) {
			return null;
		}
		return result;
	}

	private static String compact(final JsonNode jsonNode) {
		return jsonNode == null ? "null" : jsonNode.toString().replaceAll("\\s*\\n\\s*", " ");
	}
}
