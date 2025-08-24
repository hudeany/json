package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.json.utilities.IoUtilities;

public class YamlTest {
	public static boolean PRINT_TESTDATA = true;

	// TODO
	@Test
	public void test() {
		testYamlFile("test_IN.yaml", "test_IN.yaml", YamlDocumentList.class);
	}

	//	@Test
	//	public void test2() {
	//		testYamlFile("test2_IN.yaml", "test2_OUT.yaml", YamlSequence.class);
	//	}

	//	@Test
	//	public void testReceipt() {
	//		testYamlFile("receipt_IN.yaml", "receipt_OUT.yaml", YamlDocument.class);
	//	}
	//
	//	@Test
	//	public void testHtml() {
	//		testYamlFile("html.yaml", "html.yaml", YamlDocument.class);
	//	}

	private void testYamlFile(final String yamlTestFileNameInput, final String yamlTestFileNameOutput, final Class<?> yamlType) {
		try {
			if (PRINT_TESTDATA) {
				System.out.println(yamlTestFileNameInput + " (" + yamlType.getSimpleName() + ")");
			}

			byte[] testDataIn;
			try (InputStream testDataStream = getClass().getClassLoader().getResourceAsStream("yaml/" + yamlTestFileNameInput)) {
				testDataIn = IoUtilities.toByteArray(testDataStream);
			}

			byte[] testDataOut;
			try (InputStream testDataStream = getClass().getClassLoader().getResourceAsStream("yaml/" + yamlTestFileNameOutput)) {
				testDataOut = IoUtilities.toByteArray(testDataStream);
			}

			YamlValue testYamlValue;
			try (YamlReader testsuiteReader = new YamlReader(new ByteArrayInputStream(testDataIn))) {
				testsuiteReader.setVerboseLog(true);
				testYamlValue = testsuiteReader.read();
			}

			Assert.assertTrue("Expected type " + yamlType + " but was " + testYamlValue.getClass(), yamlType.isInstance(testYamlValue));

			String newString;
			try (ByteArrayOutputStream output = new ByteArrayOutputStream();
					YamlWriter writer = new YamlWriter(output, StandardCharsets.UTF_8)) {
				writer.write(testYamlValue);
				writer.flush();
				newString = new String(output.toByteArray(), StandardCharsets.UTF_8);
			}

			final String oldString = new String(testDataOut, StandardCharsets.UTF_8);

			if (PRINT_TESTDATA) {
				System.out.println(newString);
				System.out.println();
			}
			Assert.assertEquals(oldString.replace("\r\n", "\n").replace("\r", "\n"), newString.replace("\r\n", "\n").replace("\r", "\n"));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}
