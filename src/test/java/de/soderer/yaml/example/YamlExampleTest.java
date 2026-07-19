package de.soderer.yaml.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.soderer.json.utilities.Utilities;
import de.soderer.yaml.YamlReader;
import de.soderer.yaml.YamlWriter;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;

public class YamlExampleTest {
	@SuppressWarnings("static-method")
	@Test
	public void testExampleYamlMapping() throws Exception {
		YamlWriter writer = null;
		ByteArrayOutputStream output = null;
		YamlReader reader = null;
		try {
			output = new ByteArrayOutputStream();
			writer = new YamlWriter(output, StandardCharsets.UTF_8);

			final YamlMapping outputMapping = new YamlMapping();
			outputMapping.put("abc", "1");
			outputMapping.put("def", 2);
			outputMapping.put("ghi", 3.00);
			final YamlDocument outputDocument = new YamlDocument().withRoot(outputMapping);
			writer.writeDocument(outputDocument);

			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);

			reader = new YamlReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			final YamlDocument document = reader.readDocument();
			Assertions.assertTrue(document.getRoot() instanceof YamlMapping);
			final YamlMapping yamlMapping = (YamlMapping) document.getRoot();
			Assertions.assertEquals(3, yamlMapping.size());
            assertEquals(YamlScalarType.STRING, ((YamlScalar) yamlMapping.get("abc")).getType());
			Assertions.assertEquals("YamlScalar: 1", yamlMapping.get("abc").getClass().getSimpleName() + ": " + yamlMapping.get("abc"));
            assertEquals(YamlScalarType.NUMBER, ((YamlScalar) yamlMapping.get("def")).getType());
			Assertions.assertEquals("YamlScalar: 2", yamlMapping.get("def").getClass().getSimpleName() + ": " + yamlMapping.get("def"));
            assertEquals(YamlScalarType.NUMBER, ((YamlScalar) yamlMapping.get("ghi")).getType());
			Assertions.assertEquals("YamlScalar: 3.0", yamlMapping.get("ghi").getClass().getSimpleName() + ": " + yamlMapping.get("ghi"));
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(reader);
		}
	}

	@SuppressWarnings("static-method")
	@Test
	public void testExampleYamlSequence() throws Exception {
		YamlWriter writer = null;
		ByteArrayOutputStream output = null;
		YamlReader reader = null;
		try {
			output = new ByteArrayOutputStream();
			writer = new YamlWriter(output, StandardCharsets.UTF_8);

			final YamlSequence outputSequence = new YamlSequence();
			outputSequence.add("1");
			outputSequence.add(2);
			outputSequence.add(3.00);
			final YamlDocument outputDocument = new YamlDocument().withRoot(outputSequence);
			writer.writeDocument(outputDocument);

			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);

			reader = new YamlReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			final YamlDocument document = reader.readDocument();
			Assertions.assertTrue(document.getRoot() instanceof YamlSequence);
			final YamlSequence yamlSequence = (YamlSequence) document.getRoot();
			Assertions.assertEquals(3, yamlSequence.size());
            assertEquals(YamlScalarType.STRING, ((YamlScalar) yamlSequence.get(0)).getType());
			Assertions.assertEquals("YamlScalar: 1", yamlSequence.get(0).getClass().getSimpleName() + ": " + yamlSequence.get(0));
            assertEquals(YamlScalarType.NUMBER, ((YamlScalar) yamlSequence.get(1)).getType());
			Assertions.assertEquals("YamlScalar: 2", yamlSequence.get(1).getClass().getSimpleName() + ": " + yamlSequence.get(1));
            assertEquals(YamlScalarType.NUMBER, ((YamlScalar) yamlSequence.get(2)).getType());
			Assertions.assertEquals("YamlScalar: 3.0", yamlSequence.get(2).getClass().getSimpleName() + ": " + yamlSequence.get(2));
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(reader);
		}
	}

	@SuppressWarnings("static-method")
	@Test
	public void testExampleYamlSequentialRead() throws Exception {
		final String testData = ""
				+ "level1:\n"
				+ "  items:\n"
				+ "    - property1: \"property 01\"\n"
				+ "      property2: \"property 02\"\n"
				+ "      property3: \"property 03\"\n"
				+ "    - property1: \"property 11\"\n"
				+ "      property2: \"property 12\"\n"
				+ "      property3: \"property 13\"\n";

		try (InputStream testDataStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8))) {
			try (final YamlReader yamlReader = new YamlReader(testDataStream)) {
				yamlReader.readUpToPath("$.level1.items");
				YamlNode nextYamlNode;
				int count = 0;
				while ((nextYamlNode = yamlReader.readNextYamlNode()) != null) {
					final String property1 = (String) ((YamlScalar) ((YamlMapping) nextYamlNode).get("property1")).getValue();
					final String property2 = (String) ((YamlScalar) ((YamlMapping) nextYamlNode).get("property2")).getValue();
					final String property3 = (String) ((YamlScalar) ((YamlMapping) nextYamlNode).get("property3")).getValue();
					Assertions.assertTrue(("property " + count + "1").equals(property1));
					Assertions.assertTrue(("property " + count + "2").equals(property2));
					Assertions.assertTrue(("property " + count + "3").equals(property3));
					count++;
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
