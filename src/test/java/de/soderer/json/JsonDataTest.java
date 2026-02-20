package de.soderer.json;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class JsonDataTest {
	@Test
	public void testReplaceInJsonObject() throws Exception {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.add("property1", "value1");
		jsonObject.add("property2", "value2");
		jsonObject.add("property3", "value3");

		Assert.assertEquals("{\n"
				+ "	\"property1\": \"value1\",\n"
				+ "	\"property2\": \"value2\",\n"
				+ "	\"property3\": \"value3\"\n"
				+ "}",
				jsonObject.toString());

		jsonObject.replace("property2", "valueReplacement");

		Assert.assertEquals("{\n"
				+ "	\"property1\": \"value1\",\n"
				+ "	\"property2\": \"valueReplacement\",\n"
				+ "	\"property3\": \"value3\"\n"
				+ "}",
				jsonObject.toString());
	}

	@Test
	public void testRemoveByIndexInJsonArray() throws Exception {
		final JsonArray jsonArray = new JsonArray();
		jsonArray.add("item1");
		jsonArray.add("item2");
		jsonArray.add("item3");

		Assert.assertEquals("[\n"
				+ "	\"item1\",\n"
				+ "	\"item2\",\n"
				+ "	\"item3\"\n"
				+ "]",
				jsonArray.toString());

		jsonArray.removeByIndex(1);

		Assert.assertEquals("[\n"
				+ "	\"item1\",\n"
				+ "	\"item3\"\n"
				+ "]",
				jsonArray.toString());
	}

	@Test
	public void testInsertInJsonArray() throws Exception {
		final JsonArray jsonArray = new JsonArray();
		jsonArray.add("item1");
		jsonArray.add("item2");
		jsonArray.add("item3");

		Assert.assertEquals("[\n"
				+ "	\"item1\",\n"
				+ "	\"item2\",\n"
				+ "	\"item3\"\n"
				+ "]",
				jsonArray.toString());

		jsonArray.insert(2, "item2.5");

		Assert.assertEquals("[\n"
				+ "	\"item1\",\n"
				+ "	\"item2\",\n"
				+ "	\"item2.5\",\n"
				+ "	\"item3\"\n"
				+ "]",
				jsonArray.toString());
	}
}
