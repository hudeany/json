package de.soderer.json.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.soderer.json.schema.JsonSchemaTestSuite.Draft;
import de.soderer.json.schema.JsonSchemaTestSuite.TestCase;
import de.soderer.json.schema.JsonSchemaTestSuite.TestResult;

/**
 * Console runner for the official JSON-Schema-Test-Suite with a summary per draft and test file.
 * The same tests run automatically as JUnit tests in {@link JsonSchemaTestSuiteTest}.<br />
 * <br />
 * Without parameters the bundled test suite is run for all drafts, including the optional tests.
 * <br />
 * <br />
 * Parameters (all optional):
 * <ul>
 * <li>--suite &lt;directory or zip&gt;: other test suite, e.g. a clone or release download of
 * https://github.com/json-schema-org/JSON-Schema-Test-Suite</li>
 * <li>--draft &lt;name&gt;: only this draft, e.g. "draft2020-12" (may be repeated)</li>
 * <li>--filter &lt;text&gt;: only test files whose path contains the text</li>
 * <li>--required-only: skip the optional tests</li>
 * <li>--verbose: print every failed test case with its reason</li>
 * </ul>
 */
public class JsonSchemaTestSuiteRunner {
	/**
	 * Runs the test suite.
	 *
	 * @param args
	 *            the parameters, see class description
	 * @throws Exception
	 *             if the test suite cannot be read
	 */
	public static void main(final String[] args) throws Exception {
		String suitePath = null;
		String filter = null;
		boolean includeOptional = true;
		boolean verbose = false;
		final List<Draft> drafts = new ArrayList<>();
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "--suite":
					suitePath = args[++i];
					break;
				case "--draft":
					final Draft draft = Draft.getByDirectoryName(args[++i]);
					if (draft == null) {
						throw new IllegalArgumentException("Unknown draft '" + args[i] + "'");
					}
					drafts.add(draft);
					break;
				case "--filter":
					filter = args[++i];
					break;
				case "--required-only":
					includeOptional = false;
					break;
				case "--verbose":
					verbose = true;
					break;
				default:
					throw new IllegalArgumentException("Unknown parameter '" + args[i] + "'. Parameters: [--suite <directory or zip>] [--draft <name>]... [--filter <text>] [--required-only] [--verbose]");
			}
		}
		if (drafts.isEmpty()) {
			drafts.addAll(List.of(Draft.values()));
		}

		final JsonSchemaTestSuite testSuite;
		if (suitePath == null) {
			testSuite = JsonSchemaTestSuite.loadBundled();
		} else if (new File(suitePath).isDirectory()) {
			testSuite = JsonSchemaTestSuite.loadDirectory(new File(suitePath));
		} else {
			try (InputStream inputStream = new FileInputStream(suitePath)) {
				testSuite = JsonSchemaTestSuite.loadZip(inputStream);
			}
		}

		int totalFailedRequired = 0;
		for (final Draft draft : drafts) {
			int requiredPassed = 0;
			int requiredTotal = 0;
			int optionalPassed = 0;
			int optionalTotal = 0;
			System.out.println("== " + draft.getDirectoryName());
			for (final String file : testSuite.getTestFiles(draft, includeOptional, includeOptional)) {
				if (filter != null && !file.contains(filter)) {
					continue;
				}
				int filePassed = 0;
				int fileTotal = 0;
				for (final TestCase testCase : testSuite.getTestCases(draft, file)) {
					final TestResult result = testSuite.execute(testCase);
					fileTotal++;
					if (result.isPassed()) {
						filePassed++;
					} else if (verbose) {
						System.out.println("   FAIL " + testCase.getKey() + " -> " + result.getDetail());
					}
				}
				System.out.println(String.format("   %-45s %4d/%-4d%s", file, filePassed, fileTotal, filePassed < fileTotal ? "  (" + (fileTotal - filePassed) + " failed)" : ""));
				if (file.startsWith("optional/")) {
					optionalPassed += filePassed;
					optionalTotal += fileTotal;
				} else {
					requiredPassed += filePassed;
					requiredTotal += fileTotal;
				}
			}
			totalFailedRequired += requiredTotal - requiredPassed;
			System.out.println("   TOTAL required: " + requiredPassed + "/" + requiredTotal + (includeOptional ? ", optional: " + optionalPassed + "/" + optionalTotal : ""));
			System.out.println();
		}
		System.out.println(totalFailedRequired == 0 ? "All required tests passed" : totalFailedRequired + " required tests failed");
	}
}
