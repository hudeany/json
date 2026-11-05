package de.soderer.json.schema;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueString;

/**
 * Access to the official JSON-Schema-Test-Suite (https://github.com/json-schema-org/JSON-Schema-Test-Suite).<br />
 * <br />
 * The suite is read either from the bundled test resource "json/JSON-Schema-Test-Suite.zip" or from
 * a directory of a cloned suite repository. Remote schemas of the suite ("remotes/...", served at
 * "http://localhost:1234/...") are provided as {@link JsonSchemaDependency}, so no network access is
 * needed.
 */
public class JsonSchemaTestSuite {
	/** Classpath resource of the bundled test suite. */
	public static final String BUNDLED_TEST_SUITE_RESOURCE = "json/JSON-Schema-Test-Suite.zip";

	/** URL prefix of the remote schemas of the test suite. */
	private static final String REMOTES_BASE_URL = "http://localhost:1234/";

	/**
	 * Supported drafts: test suite directory name and JSON schema version.
	 */
	public enum Draft {
		DRAFT3("draft3", JsonSchemaVersion.draftV3),
		DRAFT4("draft4", JsonSchemaVersion.draftV4),
		DRAFT6("draft6", JsonSchemaVersion.draftV6),
		DRAFT7("draft7", JsonSchemaVersion.draftV7),
		DRAFT2019_09("draft2019-09", JsonSchemaVersion.draft2019_09),
		DRAFT2020_12("draft2020-12", JsonSchemaVersion.draft2020_12);

		/** Directory name within "tests/". */
		private final String directoryName;

		/** JSON schema version used for the tests. */
		private final JsonSchemaVersion version;

		Draft(final String directoryName, final JsonSchemaVersion version) {
			this.directoryName = directoryName;
			this.version = version;
		}

		/**
		 * Returns the directory name within "tests/".
		 *
		 * @return the directory name, e.g. "draft2020-12"
		 */
		public String getDirectoryName() {
			return directoryName;
		}

		/**
		 * Returns the JSON schema version used for the tests.
		 *
		 * @return the version
		 */
		public JsonSchemaVersion getVersion() {
			return version;
		}

		/**
		 * Returns the draft for a directory name.
		 *
		 * @param directoryName
		 *            the directory name, e.g. "draft7"
		 * @return the draft, or null if unknown
		 */
		public static Draft getByDirectoryName(final String directoryName) {
			for (final Draft draft : values()) {
				if (draft.directoryName.equals(directoryName)) {
					return draft;
				}
			}
			return null;
		}
	}

	/**
	 * One test case of the suite: JSON data that must be valid or invalid against a schema.
	 */
	public static final class TestCase {
		/** Draft of the test case. */
		private final Draft draft;

		/** Path of the test file relative to the draft directory, e.g. "optional/format/email.json". */
		private final String file;

		/** Index of the test group within the file. */
		private final int groupIndex;

		/** Index of the test within the group. */
		private final int testIndex;

		/** Description of the test group (schema). */
		private final String groupDescription;

		/** Description of the test. */
		private final String testDescription;

		/** Expected validation result. */
		private final boolean expectedValid;

		TestCase(final Draft draft, final String file, final int groupIndex, final int testIndex, final String groupDescription, final String testDescription, final boolean expectedValid) {
			this.draft = draft;
			this.file = file;
			this.groupIndex = groupIndex;
			this.testIndex = testIndex;
			this.groupDescription = groupDescription;
			this.testDescription = testDescription;
			this.expectedValid = expectedValid;
		}

		/**
		 * Returns the draft of the test case.
		 *
		 * @return the draft
		 */
		public Draft getDraft() {
			return draft;
		}

		/**
		 * Returns the path of the test file relative to the draft directory.
		 *
		 * @return the path, e.g. "optional/format/email.json"
		 */
		public String getFile() {
			return file;
		}

		/**
		 * Returns the description of the test group (schema).
		 *
		 * @return the description
		 */
		public String getGroupDescription() {
			return groupDescription;
		}

		/**
		 * Returns the description of the test.
		 *
		 * @return the description
		 */
		public String getTestDescription() {
			return testDescription;
		}

		/**
		 * Returns the expected validation result.
		 *
		 * @return true, if the data must be valid
		 */
		public boolean isExpectedValid() {
			return expectedValid;
		}

		/**
		 * Checks whether the test case belongs to the optional tests of the suite.
		 *
		 * @return true for tests in "optional/"
		 */
		public boolean isOptional() {
			return file.startsWith("optional/");
		}

		/**
		 * Checks whether the test case belongs to the optional format tests of the suite.
		 *
		 * @return true for tests in "optional/format/"
		 */
		public boolean isFormatTest() {
			return file.startsWith("optional/format/");
		}

		/**
		 * Returns a unique key of the test case, used for lists of known issues.
		 *
		 * @return "draft/file|group description|test description"
		 */
		public String getKey() {
			return draft.getDirectoryName() + "/" + file + "|" + groupDescription + "|" + testDescription;
		}

		@Override
		public String toString() {
			return getKey();
		}
	}

	/**
	 * Result of a test case.
	 */
	public static final class TestResult {
		/** Whether the actual result matches the expected result. */
		private final boolean passed;

		/** Explanation of a failure, or null. */
		private final String detail;

		TestResult(final boolean passed, final String detail) {
			this.passed = passed;
			this.detail = detail;
		}

		/**
		 * Returns whether the actual result matches the expected result.
		 *
		 * @return true, if passed
		 */
		public boolean isPassed() {
			return passed;
		}

		/**
		 * Returns an explanation of a failure.
		 *
		 * @return the explanation, or null if passed
		 */
		public String getDetail() {
			return detail;
		}
	}

	/** Content of all test suite files by their path, e.g. "tests/draft7/ref.json". */
	private final Map<String, byte[]> files;

	/** Parsed remote schemas by URL, created on first use. */
	private Map<String, JsonObject> remoteSchemas = null;

	/**
	 * Creates a test suite from file contents.
	 *
	 * @param files
	 *            the content of all files by path
	 */
	private JsonSchemaTestSuite(final Map<String, byte[]> files) {
		this.files = files;
	}

	/**
	 * Reads the bundled test suite from the classpath.
	 *
	 * @return the test suite
	 * @throws IOException
	 *             if the resource is missing or cannot be read
	 */
	public static JsonSchemaTestSuite loadBundled() throws IOException {
		try (InputStream inputStream = JsonSchemaTestSuite.class.getClassLoader().getResourceAsStream(BUNDLED_TEST_SUITE_RESOURCE)) {
			if (inputStream == null) {
				throw new IOException("Missing test resource '" + BUNDLED_TEST_SUITE_RESOURCE + "'");
			}
			return loadZip(inputStream);
		}
	}

	/**
	 * Reads a test suite from a zip file, e.g. a release download of the suite. A common top level
	 * directory ("JSON-Schema-Test-Suite-x.y.z/") is removed.
	 *
	 * @param zipInputStream
	 *            the zip data
	 * @return the test suite
	 * @throws IOException
	 *             if the data cannot be read
	 */
	public static JsonSchemaTestSuite loadZip(final InputStream zipInputStream) throws IOException {
		final Map<String, byte[]> files = new TreeMap<>();
		try (ZipInputStream zip = new ZipInputStream(zipInputStream)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					final ByteArrayOutputStream content = new ByteArrayOutputStream();
					zip.transferTo(content);
					files.put(entry.getName().replace('\\', '/'), content.toByteArray());
				}
			}
		}
		return new JsonSchemaTestSuite(removeTopLevelDirectory(files));
	}

	/**
	 * Reads a test suite from a directory, e.g. a clone of the suite repository.
	 *
	 * @param suiteDirectory
	 *            the directory containing "tests/" and "remotes/"
	 * @return the test suite
	 * @throws IOException
	 *             if the files cannot be read
	 */
	public static JsonSchemaTestSuite loadDirectory(final File suiteDirectory) throws IOException {
		final Map<String, byte[]> files = new TreeMap<>();
		final Path basePath = suiteDirectory.toPath();
		for (final String subDirectory : new String[] { "tests", "remotes" }) {
			final Path directory = basePath.resolve(subDirectory);
			if (Files.isDirectory(directory)) {
				try (Stream<Path> paths = Files.walk(directory)) {
					for (final Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator) {
						files.put(basePath.relativize(path).toString().replace('\\', '/'), Files.readAllBytes(path));
					}
				}
			}
		}
		if (files.isEmpty()) {
			throw new IOException("Directory '" + suiteDirectory + "' does not contain a JSON schema test suite");
		}
		return new JsonSchemaTestSuite(files);
	}

	/**
	 * Removes a common top level directory, if "tests/" is not at the top level.
	 *
	 * @param files
	 *            the files by path
	 * @return the files by path relative to the suite root
	 */
	private static Map<String, byte[]> removeTopLevelDirectory(final Map<String, byte[]> files) {
		for (final String path : files.keySet()) {
			if (path.startsWith("tests/")) {
				return files;
			}
		}
		final Map<String, byte[]> result = new TreeMap<>();
		for (final Map.Entry<String, byte[]> entry : files.entrySet()) {
			final int slashIndex = entry.getKey().indexOf('/');
			if (slashIndex > 0) {
				result.put(entry.getKey().substring(slashIndex + 1), entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Returns the test files of a draft.
	 *
	 * @param draft
	 *            the draft
	 * @param includeOptional
	 *            true to include "optional/" tests
	 * @param includeFormats
	 *            true to include "optional/format/" tests
	 * @return the paths relative to the draft directory, sorted
	 */
	public List<String> getTestFiles(final Draft draft, final boolean includeOptional, final boolean includeFormats) {
		final String prefix = "tests/" + draft.getDirectoryName() + "/";
		final List<String> result = new ArrayList<>();
		for (final String path : files.keySet()) {
			if (path.startsWith(prefix) && path.endsWith(".json")) {
				final String relativePath = path.substring(prefix.length());
				if (relativePath.startsWith("optional/format/")) {
					if (includeFormats) {
						result.add(relativePath);
					}
				} else if (relativePath.startsWith("optional/")) {
					if (includeOptional) {
						result.add(relativePath);
					}
				} else if (!relativePath.contains("/")) {
					result.add(relativePath);
				}
			}
		}
		return result;
	}

	/**
	 * Returns the test cases of a test file.
	 *
	 * @param draft
	 *            the draft
	 * @param file
	 *            the path relative to the draft directory
	 * @return the test cases
	 * @throws Exception
	 *             if the file cannot be parsed
	 */
	public List<TestCase> getTestCases(final Draft draft, final String file) throws Exception {
		final List<TestCase> result = new ArrayList<>();
		final JsonArray groups = (JsonArray) parse(files.get("tests/" + draft.getDirectoryName() + "/" + file));
		for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
			final JsonObject group = (JsonObject) groups.get(groupIndex);
			final String groupDescription = ((JsonValueString) group.get("description")).getValue();
			final JsonArray tests = (JsonArray) group.get("tests");
			for (int testIndex = 0; testIndex < tests.size(); testIndex++) {
				final JsonObject test = (JsonObject) tests.get(testIndex);
				result.add(new TestCase(draft, file, groupIndex, testIndex, groupDescription, ((JsonValueString) test.get("description")).getValue(), ((JsonValueBoolean) test.get("valid")).getValue()));
			}
		}
		return result;
	}

	/**
	 * Executes a test case. The schema and data are parsed freshly for each test case, so test cases
	 * cannot influence each other. Format assertion is enabled for the optional format tests.
	 *
	 * @param testCase
	 *            the test case
	 * @return the result
	 */
	public TestResult execute(final TestCase testCase) {
		try {
			final JsonObject group = (JsonObject) ((JsonArray) parse(files.get("tests/" + testCase.draft.getDirectoryName() + "/" + testCase.file))).get(testCase.groupIndex);
			final JsonNode schemaNode = group.get("schema");
			final JsonNode dataNode = ((JsonObject) ((JsonArray) group.get("tests")).get(testCase.testIndex)).get("data");

			final JsonSchemaConfiguration configuration = new JsonSchemaConfiguration().withJsonSchemaVersion(testCase.draft.getVersion());
			if (testCase.isFormatTest()) {
				configuration.setFormatAssertion(true);
			}
			final JsonSchemaDependency[] remotes = createRemoteDependencies();
			final JsonSchema jsonSchema;
			if (schemaNode.isBoolean()) {
				jsonSchema = new JsonSchema(((JsonValueBoolean) schemaNode).getValue(), configuration, remotes);
			} else {
				jsonSchema = new JsonSchema((JsonObject) schemaNode, configuration, remotes);
			}

			boolean actualValid;
			String validationMessage = null;
			try {
				jsonSchema.validate(dataNode);
				actualValid = true;
			} catch (final JsonSchemaDataValidationError e) {
				actualValid = false;
				validationMessage = e.getMessage();
			}
			if (actualValid == testCase.expectedValid) {
				return new TestResult(true, null);
			} else if (testCase.expectedValid) {
				return new TestResult(false, "Expected valid, but was invalid: " + validationMessage);
			} else {
				return new TestResult(false, "Expected invalid, but was valid");
			}
		} catch (final Throwable e) {
			// Any error while reading the schema counts as failure, even for data expected to be invalid
			return new TestResult(false, "Unexpected " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	/**
	 * Creates the remote schemas of the suite as dependencies, registered by their URL
	 * "http://localhost:1234/...".
	 *
	 * @return the dependencies
	 * @throws Exception
	 *             if a remote schema cannot be parsed
	 */
	private synchronized JsonSchemaDependency[] createRemoteDependencies() throws Exception {
		if (remoteSchemas == null) {
			// Remote schemas are only read by the validators, so they are parsed once and shared
			final Map<String, JsonObject> parsedRemoteSchemas = new TreeMap<>();
			for (final Map.Entry<String, byte[]> entry : files.entrySet()) {
				if (entry.getKey().startsWith("remotes/") && entry.getKey().endsWith(".json")) {
					final JsonNode remoteNode = parse(entry.getValue());
					if (remoteNode.isJsonObject()) {
						parsedRemoteSchemas.put(REMOTES_BASE_URL + entry.getKey().substring("remotes/".length()), (JsonObject) remoteNode);
					}
				}
			}
			remoteSchemas = parsedRemoteSchemas;
		}
		final List<JsonSchemaDependency> dependencies = new ArrayList<>();
		for (final Map.Entry<String, JsonObject> remoteSchema : remoteSchemas.entrySet()) {
			dependencies.add(new JsonSchemaDependency(remoteSchema.getKey(), remoteSchema.getValue()));
		}
		return dependencies.toArray(new JsonSchemaDependency[0]);
	}

	/**
	 * Parses JSON data.
	 *
	 * @param content
	 *            the UTF-8 encoded JSON data
	 * @return the parsed node
	 * @throws Exception
	 *             if the data is no valid JSON
	 */
	private static JsonNode parse(final byte[] content) throws Exception {
		try (JsonReader reader = new JsonReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
			return reader.read();
		}
	}
}
