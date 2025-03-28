package de.soderer.utilities.yaml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.json.utilities.IoUtilities;
import de.soderer.yaml.YamlMapping;
import de.soderer.yaml.YamlObject;
import de.soderer.yaml.YamlReader;
import de.soderer.yaml.YamlWriter;

public class YamlTest {
	public static boolean PRINT_TESTDATA = true;

//	@Test
//	public void test() {
//		testYamlFile("test.yaml", YamlSequence.class);
//	}
	
	@Test
	public void testReceipt() {
		testYamlFile("receipt.yaml", YamlMapping.class);
	}
	
//	@Test
//	public void testHtml() {
//		testYamlFile("html.yaml", YamlSequence.class);
//	}

	private void testYamlFile(String yamlTestFileName, Class<?> yamlType) {
		try {
			byte[] testData;
			try (InputStream testDataStream = getClass().getClassLoader().getResourceAsStream("yaml/" + yamlTestFileName)) {
				testData = IoUtilities.toByteArray(testDataStream);
			}
			
			YamlObject<?> testYamlObject;
			try (YamlReader testsuiteReader = new YamlReader(new ByteArrayInputStream(testData))) {
				testYamlObject = testsuiteReader.read();
			}

			Assert.assertTrue("Expected type " + yamlType + " but was " + testYamlObject.getClass(), yamlType.isInstance(testYamlObject));
			
			String newString;
			try (ByteArrayOutputStream output = new ByteArrayOutputStream();
					YamlWriter writer = new YamlWriter(output, StandardCharsets.UTF_8);) {
				writer.add(testYamlObject);
				writer.flush();
				newString = new String(output.toByteArray(), StandardCharsets.UTF_8);
			}
			
			String oldString = new String(testData, StandardCharsets.UTF_8);
		
			if (PRINT_TESTDATA) {
				System.out.println(yamlTestFileName + " (" + yamlType.getSimpleName() + ")");
				System.out.println(newString);
				System.out.println();
			}
			Assert.assertEquals(oldString.replace("\r\n", "\n").replace("\r", "\n"), newString);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}
