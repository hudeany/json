package de.soderer.json.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
			Assertions.assertEquals(JsonDataType.OBJECT, nodevalue.getJsonDataType());
			final JsonObject jsonObject = (JsonObject) nodevalue;
			Assertions.assertEquals(3, jsonObject.size());
			Assertions.assertEquals("JsonValueString: 1", jsonObject.get("abc").getClass().getSimpleName() + ": " + jsonObject.get("abc"));
			Assertions.assertEquals("JsonValueInteger: 2", jsonObject.get("def").getClass().getSimpleName() + ": " + jsonObject.get("def"));
			Assertions.assertEquals("JsonValueNumber: 3.0", jsonObject.get("ghi").getClass().getSimpleName() + ": " + jsonObject.get("ghi"));
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
			Assertions.assertEquals(JsonDataType.ARRAY, nodevalue.getJsonDataType());
			final JsonArray jsonArray = (JsonArray) nodevalue;
			Assertions.assertEquals(3, jsonArray.size());
			Assertions.assertEquals("JsonValueString: 1", jsonArray.get(0).getClass().getSimpleName() + ": " + jsonArray.get(0));
			Assertions.assertEquals("JsonValueInteger: 2", jsonArray.get(1).getClass().getSimpleName() + ": " + jsonArray.get(1));
			Assertions.assertEquals("JsonValueNumber: 3.0", jsonArray.get(2).getClass().getSimpleName() + ": " + jsonArray.get(2));
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
				Assertions.assertEquals(("value" + count + "1"), (property1));
				Assertions.assertEquals(("value" + count + "2"), (property2));
				Assertions.assertEquals(("value" + count + "3"), (property3));
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assertions.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
}
