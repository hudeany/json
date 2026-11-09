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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import de.soderer.yaml.YamlTestSuite.TestCase;
import de.soderer.yaml.YamlTestSuite.TestResult;

/**
 * Runs the official YAML test suite (https://github.com/yaml/yaml-test-suite) as JUnit tests.
 * <p>
 * Every test case must be handled like the test suite expects. Only the test cases listed in
 * "yaml/yaml-test-suite-intended-deviations.txt" are handled differently by design: they must fail,
 * so the deviation is checked. The console runner {@link YamlTestSuite#main(String[])} prints a
 * summary.
 * </p>
 */
@SuppressWarnings("static-method")
@DisplayName("Official YAML test suite")
public class YamlTestSuiteTest {
	/** The bundled test suite. */
	private static YamlTestSuite testSuite;

	/** Classpath resource of the intended deviations from the test suite. */
	private static final String INTENDED_DEVIATIONS_RESOURCE = "yaml/yaml-test-suite-intended-deviations.txt";

	/** Ids of the test cases, which fail by design. */
	private static final Set<String> INTENDED_DEVIATIONS = new HashSet<>();

	/**
	 * Reads the bundled test suite and the intended deviations.
	 *
	 * @throws Exception
	 *             if a test resource is missing
	 */
	@BeforeAll
	static void loadTestSuite() throws Exception {
		testSuite = YamlTestSuite.loadBundled();
		readTestCaseIds(INTENDED_DEVIATIONS_RESOURCE, INTENDED_DEVIATIONS);
	}

	/**
	 * Reads a list of test case ids in the format "&lt;test case id&gt; # &lt;description&gt;".
	 */
	private static void readTestCaseIds(final String resource, final Set<String> ids) throws Exception {
		final InputStream resourceStream = YamlTestSuiteTest.class.getClassLoader().getResourceAsStream(resource);
		if (resourceStream == null) {
			throw new IllegalStateException("Missing test resource: " + resource);
		}
		try (InputStream inputStream = resourceStream;
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				final String id = line.contains("#") ? line.substring(0, line.indexOf('#')).trim() : line.trim();
				if (!id.isEmpty()) {
					ids.add(id);
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
		if (INTENDED_DEVIATIONS.contains(testCase.getId())) {
			Assertions.assertFalse(result.isPassed(), () -> testCase.getId() + " is listed in " + INTENDED_DEVIATIONS_RESOURCE + ", but is handled like the test suite expects now");
			return;
		}
		if (!result.isPassed()) {
			Assertions.fail(testCase.getId() + " (" + testCase.getName() + "): " + result.getDetail());
		}
	}
}
