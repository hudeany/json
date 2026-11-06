package de.soderer.yaml;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import de.soderer.yaml.YamlTestSuite.TestCase;
import de.soderer.yaml.YamlTestSuite.TestResult;

/**
 * Runs the official YAML test suite (https://github.com/yaml/yaml-test-suite) as JUnit tests.
 * <p>
 * Test cases listed in "yaml/yaml-test-suite-known-issues.txt" are skipped while they fail. If one of
 * them passes, the test fails with a hint to remove it from the list, so improvements of the reader
 * are documented there. The console runner {@link YamlTestSuite#main(String[])} prints a summary.
 * </p>
 */
@SuppressWarnings("static-method")
@DisplayName("Official YAML test suite")
public class YamlTestSuiteTest {
	/** Classpath resource of the known issues. */
	private static final String KNOWN_ISSUES_RESOURCE = "yaml/yaml-test-suite-known-issues.txt";

	/** The bundled test suite. */
	private static YamlTestSuite testSuite;

	/** Ids of the test cases known to fail. */
	private static final Set<String> KNOWN_ISSUES = new HashSet<>();

	/**
	 * Reads the bundled test suite and the known issues.
	 *
	 * @throws Exception
	 *             if a test resource is missing
	 */
	@BeforeAll
	static void loadTestSuite() throws Exception {
		testSuite = YamlTestSuite.loadBundled();
		try (InputStream inputStream = YamlTestSuiteTest.class.getClassLoader().getResourceAsStream(KNOWN_ISSUES_RESOURCE);
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				final String id = line.contains("#") ? line.substring(0, line.indexOf('#')).trim() : line.trim();
				if (!id.isEmpty()) {
					KNOWN_ISSUES.add(id);
				}
			}
		}
	}

	@TestFactory
	Stream<DynamicTest> yamlTestSuite() {
		final List<DynamicTest> tests = new ArrayList<>();
		for (final TestCase testCase : testSuite.getTestCases()) {
			tests.add(DynamicTest.dynamicTest(testCase.getId() + " " + testCase.getName(), () -> executeTestCase(testCase)));
		}
		return tests.stream();
	}

	private static void executeTestCase(final TestCase testCase) {
		final TestResult result = YamlTestSuite.execute(testCase);
		final boolean knownIssue = KNOWN_ISSUES.contains(testCase.getId());
		if (result.isPassed()) {
			Assertions.assertFalse(knownIssue, () -> testCase.getId() + " passes now: please remove it from " + KNOWN_ISSUES_RESOURCE);
		} else {
			Assumptions.assumeFalse(knownIssue, () -> "Known issue: " + result.getDetail());
			Assertions.fail(testCase.getId() + " (" + testCase.getName() + "): " + result.getDetail());
		}
	}
}
