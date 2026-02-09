package de.soderer.json.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonDataType;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.JsonWriter;
import de.soderer.json.utilities.Utilities;
import de.soderer.yaml.YamlReader;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;

@SuppressWarnings("static-method")
public class JsonExampleTest {
	@Test
	public void testExampleJsonObject() throws Exception {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader reader = null;
		try {
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.openJsonObject();
			writer.openJsonObjectProperty("abc");
			writer.addSimpleJsonObjectPropertyValue("1");
			writer.openJsonObjectProperty("def");
			writer.addSimpleJsonObjectPropertyValue(2);
			writer.openJsonObjectProperty("ghi");
			writer.addSimpleJsonObjectPropertyValue(3.00);
			writer.closeJsonObject();
			writer.close();
			output.close();

			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);

			reader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			final JsonNode nodevalue = reader.read();
			System.out.println(nodevalue.getJsonDataType() == JsonDataType.OBJECT);
			// true
			final JsonObject jsonObject = (JsonObject) nodevalue;
			for (final Map.Entry<String, Object> jsonObjectProperty : jsonObject) {
				System.out.println(jsonObjectProperty.getKey() + ": " + jsonObjectProperty.getValue().getClass().getSimpleName() + ": " + jsonObjectProperty.getValue());
				// abc: String: 1
				// def: Integer: 2
				// ghi: Float: 3.0
			}
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(reader);
		}
	}

	@Test
	public void testExampleJsonArray() throws Exception {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader reader = null;
		try {
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.openJsonArray();
			writer.addSimpleJsonArrayValue("1");
			writer.addSimpleJsonArrayValue(2);
			writer.addSimpleJsonArrayValue(3.00);
			writer.closeJsonArray();
			writer.close();
			output.close();

			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);

			reader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			final JsonNode nodevalue = reader.read();
			System.out.println(nodevalue.getJsonDataType() == JsonDataType.ARRAY);
			// true
			final JsonArray jsonArray = (JsonArray) nodevalue;
			for (final Object jsonArrayItem : jsonArray) {
				System.out.println(jsonArrayItem.getClass().getSimpleName() + ": " + jsonArrayItem);
				// String: 1
				// Integer: 2
				// Float: 3.0
			}
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(reader);
		}
	}

	@Test
	public void testExampleJsonSequentialRead() throws Exception {
		final String testData = ""
				+ "level1:\n"
				+ "  items:\n"
				+ "    - property1: \"property 01\"\n"
				+ "      property2: \"property 02\"\n"
				+ "      property3: \"property 03\"\n"
				+ "\n"
				+ "    - property1: \"property 11\"\n"
				+ "      property2: \"property 12\"\n"
				+ "      property3: \"property 13\"\n"
				+ "";

		try (InputStream testDataStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8))) {
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
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
