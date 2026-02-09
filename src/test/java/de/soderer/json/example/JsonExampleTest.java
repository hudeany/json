package de.soderer.json.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
	public void testExampleJsonSequentialRead() {
		JsonReader jsonReader = null;
		try {
			final String data = ""
					+ "{"
					+ "	\"level1\":"
					+ "		["
					+ "			{"
					+ "				\"property1\": \"value11\","
					+ "				\"property2\": \"value12\","
					+ "				\"property3\": \"value13\""
					+ "			},"
					+ "			{"
					+ "				\"property1\": \"value21\","
					+ "				\"property2\": \"value22\","
					+ "				\"property3\": \"value23\""
					+ "			}"
					+ "		]"
					+ "}";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			jsonReader.readUpToJsonPath("$.level1");
			jsonReader.readNextToken();

			JsonNode nextJsonNode;
			int count = 0;
			while ((nextJsonNode = jsonReader.readNextJsonNode()) != null) {
				count++;
				final String property1 = (String) ((JsonObject) nextJsonNode).getSimpleValue("property1");
				final String property2 = (String) ((JsonObject) nextJsonNode).getSimpleValue("property2");
				final String property3 = (String) ((JsonObject) nextJsonNode).getSimpleValue("property3");
				Assert.assertEquals(("value" + count + "1"), (property1));
				Assert.assertEquals(("value" + count + "2"), (property2));
				Assert.assertEquals(("value" + count + "3"), (property3));
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
}
