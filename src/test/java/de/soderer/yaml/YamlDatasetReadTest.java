package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;

@SuppressWarnings("static-method")
public class YamlDatasetReadTest {
	@Test
	public void testPathNotExists() throws Exception {
		try (InputStream testDataStream = getClass().getClassLoader().getResourceAsStream("yaml/dataset/input.yaml")) {
			try (final YamlReader yamlReader = new YamlReader(testDataStream)) {
				try {
					yamlReader.readUpToPath("$.level1.notExists");
					Assert.fail("Missing expected exception");
				} catch (final Exception e) {
					// Expected exception
					Assert.assertEquals("Path '$.level1.notExists' is not part of the YAML data", e.getMessage());
				}
			}
		}
	}

	@Test
	public void testPathWithoutSequence() throws Exception {
		try (InputStream testDataStream = getClass().getClassLoader().getResourceAsStream("yaml/dataset/input.yaml")) {
			try (final YamlReader yamlReader = new YamlReader(testDataStream)) {
				yamlReader.readUpToPath("$.level1");

				try {
					yamlReader.readNextYamlNode();
					Assert.fail("Missing expected exception");
				} catch (final Exception e) {
					// Expected exception
					Assert.assertEquals("Cannot read items of path", e.getMessage());
				}
			}
		}
	}

	@Test
	public void testReadDataSet() throws Exception {
		try (InputStream testDataStream = getClass().getClassLoader().getResourceAsStream("yaml/dataset/input.yaml")) {
			try (final YamlReader yamlReader = new YamlReader(testDataStream)) {
				yamlReader.readUpToPath("$.level1.items");
				try {
					yamlReader.readDocument();
					Assert.fail("Missing expected exception");
				} catch (final Exception e) {
					// Expected exception
					Assert.assertEquals("Search path was already started before by method 'readUpToPath'", e.getMessage());
				}

				YamlNode nextYamlNode;
				int count = 0;
				while ((nextYamlNode = yamlReader.readNextYamlNode()) != null) {
					final String property1 = (String) ((YamlScalar) ((YamlMapping) nextYamlNode).get("property1")).getValue();
					final String property2 = (String) ((YamlScalar) ((YamlMapping) nextYamlNode).get("property2")).getValue();
					final String property3 = (String) ((YamlScalar) ((YamlMapping) nextYamlNode).get("property3")).getValue();
					Assert.assertTrue(("property " + count + "1").equals(property1));
					Assert.assertTrue(("property " + count + "2").equals(property2));
					Assert.assertTrue(("property " + count + "3").equals(property3));
					count++;
				}
			}
		}
	}

	@Test
	public void testWriteDataSet() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		try (final YamlWriter yamlWriter = new YamlWriter(outputStream)) {
			for (int i = 0; i < 1000; i++) {
				final YamlMapping yamlMapping = new YamlMapping();
				yamlMapping.add("property1", "property " + i + "1");
				yamlMapping.add("property2", "property " + i + "2");
				yamlMapping.add("property3", "property " + i + "3");
				yamlWriter.addSequenceItem(yamlMapping);
			}
		}

		try (InputStream testDataStream = new ByteArrayInputStream(outputStream.toByteArray())) {
			try (final YamlReader yamlReader = new YamlReader(testDataStream)) {
				YamlNode nextYamlNode;
				int count = 0;
				while ((nextYamlNode = yamlReader.readNextYamlNode()) != null) {
					final String property1 = (String) ((YamlScalar) ((YamlMapping) nextYamlNode).get("property1")).getValue();
					final String property2 = (String) ((YamlScalar) ((YamlMapping) nextYamlNode).get("property2")).getValue();
					final String property3 = (String) ((YamlScalar) ((YamlMapping) nextYamlNode).get("property3")).getValue();
					Assert.assertTrue(("property " + count + "1").equals(property1));
					Assert.assertTrue(("property " + count + "2").equals(property2));
					Assert.assertTrue(("property " + count + "3").equals(property3));
					count++;
				}
			}
		}
	}
}
