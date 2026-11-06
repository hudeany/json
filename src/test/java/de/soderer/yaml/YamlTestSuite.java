package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonReader;
import de.soderer.json.patch.JsonPatch;
import de.soderer.yaml.data.YamlDocument;

/**
 * The official YAML test suite (https://github.com/yaml/yaml-test-suite, data branch) and the
 * execution of its test cases with {@link YamlReader} and {@link YamlToJsonConverter}.
 * <p>
 * Each test case has an input "in.yaml". If an "error" file exists, the input is invalid and must be
 * rejected. Otherwise the input must be read without error, and if an "in.json" exists, the JSON
 * conversion of each document must equal the JSON values of this file.
 * </p>
 */
public class YamlTestSuite {
	/** Classpath resource of the bundled test suite. */
	public static final String BUNDLED_TEST_SUITE_RESOURCE = "yaml/yaml-test-suite.zip";

	/** Maximum time for reading one test case, to detect endless loops. */
	private static final long TIMEOUT_SECONDS = 10;

	/**
	 * A test case of the suite.
	 */
	public static class TestCase {
		private final String id;
		private final String name;
		private final String yaml;
		private final String json;
		private final boolean error;

		TestCase(final String id, final String name, final String yaml, final String json, final boolean error) {
			this.id = id;
			this.name = name;
			this.yaml = yaml;
			this.json = json;
			this.error = error;
		}

		/**
		 * Returns the id of the test case, e.g. "229Q" or "2G84/01" for a case with several inputs.
		 *
		 * @return the id
		 */
		public String getId() {
			return id;
		}

		/**
		 * Returns the description of the test case.
		 *
		 * @return the description
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns the YAML input.
		 *
		 * @return the YAML text
		 */
		public String getYaml() {
			return yaml;
		}

		/**
		 * Returns the expected JSON values of the documents.
		 *
		 * @return the JSON text, null if the data has no JSON representation
		 */
		public String getJson() {
			return json;
		}

		/**
		 * Checks whether the YAML input is invalid and must be rejected.
		 *
		 * @return true for invalid input
		 */
		public boolean isError() {
			return error;
		}
	}

	/**
	 * Result of a test case.
	 */
	public static class TestResult {
		private final boolean passed;
		private final String detail;

		TestResult(final boolean passed, final String detail) {
			this.passed = passed;
			this.detail = detail;
		}

		/**
		 * Checks whether the test case passed.
		 *
		 * @return true if passed
		 */
		public boolean isPassed() {
			return passed;
		}

		/**
		 * Returns the reason of a failure.
		 *
		 * @return the reason, null if passed
		 */
		public String getDetail() {
			return detail;
		}
	}

	/** The test cases by id. */
	private final Map<String, TestCase> testCases = new TreeMap<>();

	/**
	 * Loads the bundled test suite.
	 *
	 * @return the test suite
	 * @throws IOException
	 *             if the resource cannot be read
	 */
	public static YamlTestSuite loadBundled() throws IOException {
		try (InputStream inputStream = YamlTestSuite.class.getClassLoader().getResourceAsStream(BUNDLED_TEST_SUITE_RESOURCE)) {
			if (inputStream == null) {
				throw new IOException("Missing test resource " + BUNDLED_TEST_SUITE_RESOURCE);
			}
			return loadZip(inputStream);
		}
	}

	/**
	 * Loads a test suite from a zip file of the data branch.
	 *
	 * @param zipInputStream
	 *            the zip data
	 * @return the test suite
	 * @throws IOException
	 *             if the data cannot be read
	 */
	public static YamlTestSuite loadZip(final InputStream zipInputStream) throws IOException {
		final Map<String, Map<String, String>> filesByCase = new TreeMap<>();
		try (ZipInputStream zip = new ZipInputStream(zipInputStream, StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					final String entryName = entry.getName();
					final int separatorIndex = entryName.lastIndexOf('/');
					if (separatorIndex > 0) {
						filesByCase.computeIfAbsent(entryName.substring(0, separatorIndex), k -> new TreeMap<>()).put(entryName.substring(separatorIndex + 1), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
					}
				}
			}
		}
		return create(filesByCase);
	}

	/**
	 * Loads a test suite from a checkout of the data branch.
	 *
	 * @param directory
	 *            the directory containing the test case directories
	 * @return the test suite
	 * @throws IOException
	 *             if the data cannot be read
	 */
	public static YamlTestSuite loadDirectory(final File directory) throws IOException {
		final Map<String, Map<String, String>> filesByCase = new TreeMap<>();
		collectFiles(directory, "", filesByCase);
		return create(filesByCase);
	}

	private static void collectFiles(final File directory, final String relativePath, final Map<String, Map<String, String>> filesByCase) throws IOException {
		final File[] files = directory.listFiles();
		if (files == null) {
			return;
		}
		for (final File file : files) {
			if (file.isDirectory() && !file.getName().startsWith(".")) {
				collectFiles(file, relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName(), filesByCase);
			} else if (file.isFile() && !relativePath.isEmpty()) {
				try (InputStream inputStream = new FileInputStream(file)) {
					filesByCase.computeIfAbsent(relativePath, k -> new TreeMap<>()).put(file.getName(), new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
				}
			}
		}
	}

	private static YamlTestSuite create(final Map<String, Map<String, String>> filesByCase) {
		final YamlTestSuite testSuite = new YamlTestSuite();
		for (final Map.Entry<String, Map<String, String>> caseEntry : filesByCase.entrySet()) {
			final Map<String, String> files = caseEntry.getValue();
			if (files.containsKey("in.yaml")) {
				String name = files.get("===");
				if (name == null && caseEntry.getKey().contains("/")) {
					// Cases with several inputs have the name in the parent directory
					name = filesByCase.getOrDefault(caseEntry.getKey().substring(0, caseEntry.getKey().lastIndexOf('/')), Map.of()).get("===");
				}
				testSuite.testCases.put(caseEntry.getKey(), new TestCase(caseEntry.getKey(), name == null ? "" : name.trim(), files.get("in.yaml"), files.get("in.json"), files.containsKey("error")));
			}
		}
		return testSuite;
	}

	/**
	 * Returns all test cases ordered by id.
	 *
	 * @return the test cases
	 */
	public List<TestCase> getTestCases() {
		return new ArrayList<>(testCases.values());
	}

	/**
	 * Executes a test case with a time limit.
	 *
	 * @param testCase
	 *            the test case
	 * @return the result
	 */
	public static TestResult execute(final TestCase testCase) {
		final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
			final Thread thread = new Thread(runnable, "yaml-test-suite");
			thread.setDaemon(true);
			return thread;
		});
		try {
			final Future<TestResult> future = executor.submit(() -> executeWithoutTimeLimit(testCase));
			return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (@SuppressWarnings("unused") final TimeoutException e) {
			return new TestResult(false, "Timeout after " + TIMEOUT_SECONDS + " seconds, probably an endless loop");
		} catch (final Exception e) {
			return new TestResult(false, "Unexpected " + e.getClass().getSimpleName() + ": " + e.getMessage());
		} finally {
			executor.shutdownNow();
		}
	}

	private static TestResult executeWithoutTimeLimit(final TestCase testCase) {
		final List<YamlDocument> documents = new ArrayList<>();
		try (YamlReader reader = new YamlReader(new ByteArrayInputStream(testCase.getYaml().getBytes(StandardCharsets.UTF_8)))) {
			YamlDocument document;
			while ((document = reader.readDocument()) != null) {
				documents.add(document);
			}
		} catch (final Exception e) {
			if (testCase.isError()) {
				return new TestResult(true, null);
			} else {
				return new TestResult(false, "Valid YAML was rejected: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			}
		} catch (final StackOverflowError e) {
			return new TestResult(false, "StackOverflowError");
		}

		if (testCase.isError()) {
			return new TestResult(false, "Invalid YAML was accepted");
		} else if (testCase.getJson() == null) {
			return new TestResult(true, null);
		}

		final List<String> expectedJsonValues = splitJsonValues(testCase.getJson());
		if (expectedJsonValues.size() != documents.size()) {
			return new TestResult(false, "Expected " + expectedJsonValues.size() + " documents, but read " + documents.size());
		}
		for (int i = 0; i < documents.size(); i++) {
			try {
				final JsonNode expected = JsonReader.readJsonItemString(expectedJsonValues.get(i));
				final JsonNode actual = YamlToJsonConverter.convert(documents.get(i));
				if (!JsonPatch.jsonEquals(expected, actual)) {
					return new TestResult(false, "Document " + i + ": expected " + compact(expected) + " but was " + compact(actual));
				}
			} catch (final Exception e) {
				return new TestResult(false, "Document " + i + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
			}
		}
		return new TestResult(true, null);
	}

	private static String compact(final JsonNode jsonNode) {
		final String text = jsonNode == null ? "null" : jsonNode.toString().replaceAll("\\s*\\n\\s*", " ");
		return text.length() > 200 ? text.substring(0, 200) + "..." : text;
	}

	/**
	 * Splits a text of concatenated JSON values, like the "in.json" of a stream with several
	 * documents.
	 *
	 * @param jsonText
	 *            the JSON values separated by whitespace
	 * @return the JSON values as text
	 */
	static List<String> splitJsonValues(final String jsonText) {
		final List<String> values = new ArrayList<>();
		int depth = 0;
		boolean inString = false;
		int valueStart = -1;
		for (int i = 0; i < jsonText.length(); i++) {
			final char character = jsonText.charAt(i);
			if (inString) {
				if (character == '\\') {
					i++;
				} else if (character == '"') {
					inString = false;
					if (depth == 0) {
						values.add(jsonText.substring(valueStart, i + 1));
						valueStart = -1;
					}
				}
			} else if (Character.isWhitespace(character)) {
				if (depth == 0 && valueStart >= 0) {
					values.add(jsonText.substring(valueStart, i));
					valueStart = -1;
				}
			} else {
				if (valueStart < 0) {
					valueStart = i;
				}
				if (character == '"') {
					inString = true;
				} else if (character == '{' || character == '[') {
					depth++;
				} else if (character == '}' || character == ']') {
					depth--;
					if (depth == 0) {
						values.add(jsonText.substring(valueStart, i + 1));
						valueStart = -1;
					}
				}
			}
		}
		if (valueStart >= 0) {
			values.add(jsonText.substring(valueStart));
		}
		return values;
	}

	/**
	 * Console runner: prints the failed test cases and a summary.
	 *
	 * @param args
	 *            optional: directory of a checkout of the data branch, "--verbose" to print the
	 *            reasons
	 * @throws Exception
	 *             if the test suite cannot be read
	 */
	public static void main(final String[] args) throws Exception {
		boolean verbose = false;
		YamlTestSuite testSuite = null;
		for (final String arg : args) {
			if ("--verbose".equals(arg)) {
				verbose = true;
			} else {
				testSuite = loadDirectory(new File(arg));
			}
		}
		if (testSuite == null) {
			testSuite = loadBundled();
		}
		int passedValid = 0;
		int totalValid = 0;
		int passedError = 0;
		int totalError = 0;
		for (final TestCase testCase : testSuite.getTestCases()) {
			final TestResult result = execute(testCase);
			if (testCase.isError()) {
				totalError++;
				passedError += result.isPassed() ? 1 : 0;
			} else {
				totalValid++;
				passedValid += result.isPassed() ? 1 : 0;
			}
			if (!result.isPassed()) {
				System.out.println("FAIL " + testCase.getId() + " (" + testCase.getName() + ")" + (verbose ? ": " + result.getDetail() : ""));
			}
		}
		System.out.println("Valid YAML read correctly: " + passedValid + "/" + totalValid);
		System.out.println("Invalid YAML rejected: " + passedError + "/" + totalError);
	}
}
