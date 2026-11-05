package de.soderer.json.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import de.soderer.json.schema.JsonSchemaTestSuite.Draft;
import de.soderer.json.schema.JsonSchemaTestSuite.TestCase;
import de.soderer.json.schema.JsonSchemaTestSuite.TestResult;

/**
 * Runs the official JSON-Schema-Test-Suite as JUnit tests, bundled as test resource
 * "json/JSON-Schema-Test-Suite.zip" (no network access needed).<br />
 * <br />
 * Every test case of the suite becomes a dynamic test, grouped by draft and test file. All required
 * tests must pass. The optional tests (including the optional format tests, which are run with
 * format assertion enabled) must pass too, except the known issues listed in
 * {@link #KNOWN_OPTIONAL_ISSUES}, which are reported as aborted (skipped) instead of failed.
 */
@SuppressWarnings("static-method")
@DisplayName("Official JSON-Schema-Test-Suite")
public class JsonSchemaTestSuiteTest {
	/**
	 * Optional test cases which are known to fail, by their key (see {@link TestCase#getKey()}).
	 * They concern optional behavior of the JSON schema standard which is not implemented.
	 */
	private static final Set<String> KNOWN_OPTIONAL_ISSUE_PREFIXES = Set.of(
			// References into schemas of other drafts are evaluated with the draft of the referencing schema
			"optional/cross-draft.json|");

	/** Known issues, filled in {@link #loadTestSuite()} with keys including the draft directory. */
	private static final List<String> KNOWN_OPTIONAL_ISSUES = new ArrayList<>();

	/** The bundled test suite. */
	private static JsonSchemaTestSuite testSuite;

	/**
	 * Reads the bundled test suite.
	 *
	 * @throws Exception
	 *             if the test resource is missing
	 */
	@BeforeAll
	static void loadTestSuite() throws Exception {
		testSuite = JsonSchemaTestSuite.loadBundled();
		for (final Draft draft : Draft.values()) {
			for (final String knownIssuePrefix : KNOWN_OPTIONAL_ISSUE_PREFIXES) {
				KNOWN_OPTIONAL_ISSUES.add(draft.getDirectoryName() + "/" + knownIssuePrefix);
			}
		}
	}

	@TestFactory
	@DisplayName("draft3")
	Stream<DynamicNode> draft3() throws Exception {
		return createTests(Draft.DRAFT3);
	}

	@TestFactory
	@DisplayName("draft4")
	Stream<DynamicNode> draft4() throws Exception {
		return createTests(Draft.DRAFT4);
	}

	@TestFactory
	@DisplayName("draft6")
	Stream<DynamicNode> draft6() throws Exception {
		return createTests(Draft.DRAFT6);
	}

	@TestFactory
	@DisplayName("draft7")
	Stream<DynamicNode> draft7() throws Exception {
		return createTests(Draft.DRAFT7);
	}

	@TestFactory
	@DisplayName("draft2019-09")
	Stream<DynamicNode> draft2019_09() throws Exception {
		return createTests(Draft.DRAFT2019_09);
	}

	@TestFactory
	@DisplayName("draft2020-12")
	Stream<DynamicNode> draft2020_12() throws Exception {
		return createTests(Draft.DRAFT2020_12);
	}

	/**
	 * Creates the dynamic tests of a draft: one container per test file, one test per test case.
	 *
	 * @param draft
	 *            the draft
	 * @return the containers
	 * @throws Exception
	 *             if a test file cannot be read
	 */
	private static Stream<DynamicNode> createTests(final Draft draft) throws Exception {
		final List<DynamicNode> fileContainers = new ArrayList<>();
		for (final String file : testSuite.getTestFiles(draft, true, true)) {
			final List<DynamicNode> tests = new ArrayList<>();
			for (final TestCase testCase : testSuite.getTestCases(draft, file)) {
				tests.add(DynamicTest.dynamicTest(testCase.getGroupDescription() + " | " + testCase.getTestDescription(), () -> executeTestCase(testCase)));
			}
			fileContainers.add(DynamicContainer.dynamicContainer(file, tests.stream()));
		}
		return fileContainers.stream();
	}

	/**
	 * Executes one test case and checks its result.
	 *
	 * @param testCase
	 *            the test case
	 */
	private static void executeTestCase(final TestCase testCase) {
		final TestResult result = testSuite.execute(testCase);
		if (!result.isPassed()) {
			final boolean knownIssue = testCase.isOptional() && KNOWN_OPTIONAL_ISSUES.stream().anyMatch(testCase.getKey()::startsWith);
			Assumptions.assumeFalse(knownIssue, () -> "Known issue of optional test: " + result.getDetail());
			Assertions.fail(testCase.getKey() + ": " + result.getDetail());
		}
	}
}
