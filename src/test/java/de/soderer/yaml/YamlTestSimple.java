package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.json.utilities.IoUtilities;

public class YamlTestSimple {
	public static boolean PRINT_TESTDATA = false;

	@Test
	public void testSimpleSequence() {
		testYamlFile("simpleSequence.yaml", YamlSequence.class);
	}

	@Test
	public void testSimpleSequenceFlow() {
		testYamlFile("simpleSequenceFlow.yaml", YamlSequence.class);
	}

	@Test
	public void testSimpleSequenceBracket() {
		testYamlFile("simpleSequenceBracket.yaml", YamlSequence.class);
	}

	@Test
	public void testSimpleSequenceEmpty() {
		testYamlFile("simpleSequenceEmpty.yaml", YamlSequence.class);
	}

	@Test
	public void testSimpleMapping() {
		testYamlFile("simpleMapping.yaml", YamlMapping.class);
	}

	@Test
	public void testSimpleMappingBracket() {
		testYamlFile("simpleMappingBracket.yaml", YamlMapping.class);
	}

	@Test
	public void testSimpleMappingFlow() {
		testYamlFile("simpleMappingFlow.yaml", YamlMapping.class);
	}

	@Test
	public void testSimpleMappingEmpty() {
		testYamlFile("simpleMappingEmpty.yaml", YamlMapping.class);
	}

	@Test
	public void testSimpleMappingWithSequence() {
		testYamlFile("simpleMappingWithSequence.yaml", YamlMapping.class);
	}

	@Test
	public void testSimpleMappingWithBracketSequence() {
		testYamlFile("simpleMappingWithBracketSequence.yaml", YamlMapping.class);
	}

	@Test
	public void testSimpleMappingWithBracketMapping() {
		testYamlFile("simpleMappingWithBracketMapping.yaml", YamlMapping.class);
	}

	@Test
	public void testSimpleSequenceWithMapping() {
		testYamlFile("simpleSequenceWithMapping.yaml", YamlSequence.class);
	}

	@Test
	public void testSimpleSequenceWithBracketMapping() {
		testYamlFile("simpleSequenceWithBracketMapping.yaml", YamlSequence.class);
	}

	@Test
	public void testSimpleMappingWithMapping() {
		testYamlFile("simpleMappingWithMapping.yaml", YamlMapping.class);
	}

	@Test
	public void testSimpleSequenceWithSequence() {
		testYamlFile("simpleSequenceWithSequence.yaml", YamlSequence.class);
	}

	@Test
	public void testSimpleSequenceWithBracketSequence() {
		testYamlFile("simpleSequenceWithBracketSequence.yaml", YamlSequence.class);
	}

	@Test
	public void testAnchorWithComment() {
		testYamlFile("anchorWithComment.yaml", YamlSequence.class);
	}

	@Test
	public void testSimpleValues() {
		try {
			byte[] testData;
			try (InputStream testDataStream = getClass().getClassLoader().getResourceAsStream("yaml/" + "simpleValues.yaml")) {
				testData = IoUtilities.toByteArray(testDataStream);
			}

			YamlValue testYamlValue;
			try (YamlReader testsuiteReader = new YamlReader(new ByteArrayInputStream(testData))) {
				testYamlValue = testsuiteReader.read();
			}

			Assert.assertTrue(YamlSequence.class.isInstance(testYamlValue));

			String newString;
			try (ByteArrayOutputStream output = new ByteArrayOutputStream();
					YamlWriter writer = new YamlWriter(output, StandardCharsets.UTF_8);) {
				writer.setAlwaysQuoteStringValues(true);
				writer.add(testYamlValue, false);
				writer.flush();
				newString = new String(output.toByteArray(), StandardCharsets.UTF_8);
			}

			if (PRINT_TESTDATA) {
				System.out.println("simpleValues.yaml" + " (" + YamlSequence.class.getSimpleName() + ")");
				System.out.println(newString);
				System.out.println();
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	private void testYamlFile(final String yamlTestFileName, final Class<?> yamlType) {
		try {
			byte[] testData;
			try (InputStream testDataStream = getClass().getClassLoader().getResourceAsStream("yaml/" + yamlTestFileName)) {
				testData = IoUtilities.toByteArray(testDataStream);
			}

			YamlValue testYamlValue;
			try (YamlReader testsuiteReader = new YamlReader(new ByteArrayInputStream(testData))) {
				testYamlValue = testsuiteReader.read();
			}

			Assert.assertTrue("Expected type " + yamlType + " but was " + testYamlValue.getClass(), yamlType.isInstance(testYamlValue));

			String newString;
			try (ByteArrayOutputStream output = new ByteArrayOutputStream();
					YamlWriter writer = new YamlWriter(output, StandardCharsets.UTF_8);) {
				writer.add(testYamlValue, false);
				writer.flush();
				newString = new String(output.toByteArray(), StandardCharsets.UTF_8);
			}

			final String oldString = new String(testData, StandardCharsets.UTF_8);

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
