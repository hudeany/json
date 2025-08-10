package de.soderer.json;

import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.json.compare.JsonCompare;
import de.soderer.json.compare.JsonCompareSettings;

@SuppressWarnings("static-method")
public class JsonCompareTest {
	@Test
	public void testOk() {
		final LocalDateTime now = LocalDateTime.now();

		final JsonObject jsonObjectLeft = new JsonObject();
		jsonObjectLeft.add("attribute1", "value1");
		jsonObjectLeft.add("attribute2", "value2");
		jsonObjectLeft.add("attribute3", 3);
		jsonObjectLeft.add("attribute4", 4.123);
		jsonObjectLeft.add("attribute5", now);
		jsonObjectLeft.add("attribute6", true);

		final JsonArray jsonArrayLeft = new JsonArray();
		jsonArrayLeft.add("value1");
		jsonArrayLeft.add("value1");
		jsonArrayLeft.add("value2");
		jsonArrayLeft.add(3);
		jsonArrayLeft.add(4.123);
		jsonArrayLeft.add(now);
		jsonArrayLeft.add(true);

		jsonObjectLeft.add("array", jsonArrayLeft);

		final JsonObject jsonObjectRight = new JsonObject();
		jsonObjectRight.add("attribute1", "value1");
		jsonObjectRight.add("attribute2", "value2");
		jsonObjectRight.add("attribute3", 3);
		jsonObjectRight.add("attribute4", 4.123);
		jsonObjectRight.add("attribute5", now);
		jsonObjectRight.add("attribute6", true);

		final JsonArray jsonArrayRight = new JsonArray();
		jsonArrayRight.add("value1");
		jsonArrayRight.add("value1");
		jsonArrayRight.add("value2");
		jsonArrayRight.add(3);
		jsonArrayRight.add(4.123);
		jsonArrayRight.add(now);
		jsonArrayRight.add(true);

		jsonObjectRight.add("array", jsonArrayRight);

		final JsonObject compareResultJson = new JsonCompare(jsonObjectLeft, jsonObjectRight).compare();

		Assert.assertNull(compareResultJson);
	}

	@Test
	public void testPropertyKeyError() {
		final LocalDateTime now = LocalDateTime.now();

		final JsonObject jsonObjectLeft = new JsonObject();
		jsonObjectLeft.add("attribute1", "value1");
		jsonObjectLeft.add("attribute2", "value2");
		jsonObjectLeft.add("attributeLeftOnly", 3);
		jsonObjectLeft.add("attribute4", 4.123);
		jsonObjectLeft.add("attribute5", now);
		jsonObjectLeft.add("attribute6", true);

		final JsonObject jsonObjectRight = new JsonObject();
		jsonObjectRight.add("attribute1", "value1");
		jsonObjectRight.add("attribute2", "value2");
		jsonObjectRight.add("attributeRightOnly", 3);
		jsonObjectRight.add("attribute4", 4.123);
		jsonObjectRight.add("attribute5", now);
		jsonObjectRight.add("attribute6", true);

		final JsonObject compareResultJson = new JsonCompare(jsonObjectLeft, jsonObjectRight).compare();

		Assert.assertEquals(compareResultJson.toString(), 2, ((JsonArray) compareResultJson.get("differences")).size());
	}

	@Test
	public void testPropertyValueError() {
		final LocalDateTime now = LocalDateTime.now();

		final JsonObject jsonObjectLeft = new JsonObject();
		jsonObjectLeft.add("attribute1", "value1");
		jsonObjectLeft.add("attribute2", "value2");
		jsonObjectLeft.add("attribute3", 1);
		jsonObjectLeft.add("attribute4", 4.123);
		jsonObjectLeft.add("attribute5", now);
		jsonObjectLeft.add("attribute6", true);

		final JsonObject jsonObjectRight = new JsonObject();
		jsonObjectRight.add("attribute1", "value1");
		jsonObjectRight.add("attribute2", "value2");
		jsonObjectRight.add("attribute3", 2);
		jsonObjectRight.add("attribute4", 4.123);
		jsonObjectRight.add("attribute5", now);
		jsonObjectRight.add("attribute6", true);

		final JsonObject compareResultJson = new JsonCompare(jsonObjectLeft, jsonObjectRight).compare();

		Assert.assertEquals(compareResultJson.toString(), 1, ((JsonArray) compareResultJson.get("differences")).size());
	}

	@Test
	public void testPropertyValueErrorJsonObject() {
		final LocalDateTime now = LocalDateTime.now();

		final JsonObject jsonObjectLeft = new JsonObject();
		jsonObjectLeft.add("attribute1", "value1");
		jsonObjectLeft.add("attribute2", "value2");
		final JsonObject valueJsonObjectLeft = new JsonObject();
		valueJsonObjectLeft.add("subValueLeft", "subValueLeft");
		jsonObjectLeft.add("attribute3", valueJsonObjectLeft);
		jsonObjectLeft.add("attribute4", 4.123);
		jsonObjectLeft.add("attribute5", now);
		jsonObjectLeft.add("attribute6", true);

		final JsonObject jsonObjectRight = new JsonObject();
		jsonObjectRight.add("attribute1", "value1");
		jsonObjectRight.add("attribute2", "value2");
		final JsonObject valueJsonObjectRight = new JsonObject();
		valueJsonObjectRight.add("subValueRight", "subValueRight");
		jsonObjectRight.add("attribute3", valueJsonObjectRight);
		jsonObjectRight.add("attribute4", 4.123);
		jsonObjectRight.add("attribute5", now);
		jsonObjectRight.add("attribute6", true);

		final JsonObject compareResultJson = new JsonCompare(jsonObjectLeft, jsonObjectRight).compare();

		Assert.assertEquals(compareResultJson.toString(), 1, ((JsonArray) compareResultJson.get("differences")).size());
		Assert.assertEquals(compareResultJson.toString(), 2, ((JsonArray) ((JsonObject) ((JsonArray) compareResultJson.get("differences")).get(0)).get("differences")).size());
	}

	@Test
	public void testArrayError() {
		final LocalDateTime now = LocalDateTime.now();

		final JsonArray jsonArrayLeft = new JsonArray();
		jsonArrayLeft.add("value1");
		jsonArrayLeft.add("valueLeftOnly");
		jsonArrayLeft.add("value2");
		jsonArrayLeft.add(3);
		jsonArrayLeft.add(4.123);
		jsonArrayLeft.add(now);
		jsonArrayLeft.add(true);

		final JsonArray jsonArrayRight = new JsonArray();
		jsonArrayRight.add("value1");
		jsonArrayRight.add("valueRightOnly");
		jsonArrayRight.add("value2");
		jsonArrayRight.add(3);
		jsonArrayRight.add(4.123);
		jsonArrayRight.add(now);
		jsonArrayRight.add(true);

		final JsonObject compareResultJson = new JsonCompare(jsonArrayLeft, jsonArrayRight).compare();

		Assert.assertEquals(compareResultJson.toString(), 1, ((JsonArray) compareResultJson.get("differences")).size());
	}

	@Test
	public void testArrayOrderError() {
		final JsonArray jsonArrayLeft = new JsonArray();
		jsonArrayLeft.add("value1");
		jsonArrayLeft.add("value2");
		jsonArrayLeft.add("value3");

		final JsonArray jsonArrayRight = new JsonArray();
		jsonArrayRight.add("value2");
		jsonArrayRight.add("value1");
		jsonArrayRight.add("value3");

		final JsonObject compareResultJson = new JsonCompare(jsonArrayLeft, jsonArrayRight).compare();

		Assert.assertEquals(compareResultJson.toString(), 2, ((JsonArray) compareResultJson.get("differences")).size());
	}

	@Test
	public void testArrayOrderMixed() {
		final JsonArray jsonArrayLeft = new JsonArray();
		jsonArrayLeft.add("value1");
		jsonArrayLeft.add("value2");
		jsonArrayLeft.add("value3");

		final JsonArray jsonArrayRight = new JsonArray();
		jsonArrayRight.add("value2");
		jsonArrayRight.add("value1");
		jsonArrayRight.add("value3");

		final JsonObject compareResultJson = new JsonCompare(jsonArrayLeft, jsonArrayRight).setCompareSettings(new JsonCompareSettings().setAllowArrayMixedOrder(true)).compare();

		Assert.assertNull(compareResultJson);
	}

	@Test
	public void testArrayOrderMixedError() {
		final JsonArray jsonArrayLeft = new JsonArray();
		jsonArrayLeft.add("value1");
		jsonArrayLeft.add("value2");
		jsonArrayLeft.add("value3");

		final JsonArray jsonArrayRight = new JsonArray();
		jsonArrayRight.add("value2");
		jsonArrayRight.add("value4");
		jsonArrayRight.add("value3");

		final JsonObject compareResultJson = new JsonCompare(jsonArrayLeft, jsonArrayRight).setCompareSettings(new JsonCompareSettings().setAllowArrayMixedOrder(true)).compare();

		Assert.assertEquals(compareResultJson.toString(), 2, ((JsonArray) compareResultJson.get("differences")).size());
	}
}
