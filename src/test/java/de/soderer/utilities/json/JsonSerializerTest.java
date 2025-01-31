package de.soderer.utilities.json;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.TestObjectComplex;
import de.soderer.utilities.TestObjectComplexForSimpleJson;
import de.soderer.utilities.TestObjectSimple;

@SuppressWarnings("static-method")
public class JsonSerializerTest {
	@Test
	public void testSimple() throws Exception {
		final JsonNode dataNode = JsonSerializer.serialize(new TestObjectSimple());
		JsonWriter.getJsonItemString(dataNode);
	}

	@Test
	public void testSimpleWithTypeData() throws Exception {
		final JsonNode dataNodeOriginal = JsonSerializer.serialize(new TestObjectSimple(), false, false, false, true);
		final String jsonStringOriginal = JsonWriter.getJsonItemString(dataNodeOriginal);
		final TestObjectSimple deserializedObject = (TestObjectSimple) JsonSerializer.deserialize(dataNodeOriginal);
		final JsonNode dataNodeSerialized = JsonSerializer.serialize(deserializedObject, false, false, false, true);
		final String jsonStringSerialized = JsonWriter.getJsonItemString(dataNodeSerialized);
		Assert.assertEquals(jsonStringOriginal, jsonStringSerialized);
		final TestObjectSimple deserializedObject2 = (TestObjectSimple) JsonSerializer.deserialize((JsonObject) JsonReader.readJsonItemString(jsonStringOriginal).getValue());
		final JsonNode dataNodeSerialized2 = JsonSerializer.serialize(deserializedObject2, false, false, false, true);
		final String jsonStringSerialized2 = JsonWriter.getJsonItemString(dataNodeSerialized2);
		Assert.assertEquals(jsonStringOriginal, jsonStringSerialized2);
	}

	@Test
	public void testAllFields() throws Exception {
		final JsonNode dataNode = JsonSerializer.serialize(new TestObjectComplex(), true, true, true, true);
		JsonWriter.getJsonItemString(dataNode);
	}

	@Test
	public void testComplexWithTypeData() throws Exception {
		final JsonNode dataNodeOriginal = JsonSerializer.serialize(new TestObjectComplex(), false, false, false, true);
		final String jsonStringOriginal = JsonWriter.getJsonItemString(dataNodeOriginal);
		final TestObjectComplex deserializedObject = (TestObjectComplex) JsonSerializer.deserialize(JsonReader.readJsonItemString(jsonStringOriginal));
		final JsonNode dataNodeSerialized = JsonSerializer.serialize(deserializedObject, false, false, false, true);
		@SuppressWarnings("unused")
		final String jsonStringDeserialized = JsonWriter.getJsonItemString(dataNodeSerialized);
		// Hashset item order may have changed
		//Assert.assertEquals(jsonStringOriginal, jsonStringDeserialized);
	}

	@Test
	public void testComplexWithTypeDataInSimpleJson() throws Exception {
		final JsonNode dataNodeOriginal = JsonSerializer.serialize(new TestObjectComplexForSimpleJson());
		final String jsonStringOriginal = JsonWriter.getJsonItemString(dataNodeOriginal);
		final TestObjectComplexForSimpleJson deserializedObject = (TestObjectComplexForSimpleJson) JsonSerializer.deserialize(TestObjectComplexForSimpleJson.class, JsonReader.readJsonItemString(jsonStringOriginal));
		final JsonNode dataNodeSerialized = JsonSerializer.serialize(deserializedObject);
		final String jsonStringDeserialized = JsonWriter.getJsonItemString(dataNodeSerialized);
		Assert.assertEquals(jsonStringOriginal, jsonStringDeserialized);
	}

	@Test
	public void testCylic1() throws Exception {
		try {
			final List<Object> list = new ArrayList<>();
			list.add(1);
			list.add(2);
			list.add(3);
			list.add(list);
			list.add(4);
			JsonSerializer.serialize(list);
			Assert.fail("Missing expected Exception");
		} catch (final Exception e) {
			// Expected Exception
			Assert.assertTrue(e.getMessage().contains("Cyclic reference detected"));
		}
	}

	@Test
	public void testCylic2() throws Exception {
		try {
			final TestObjectComplex someObject = new TestObjectComplex();

			final List<Object> list1 = new ArrayList<>();
			final List<Object> list2 = new ArrayList<>();

			list1.add(1);
			list1.add(2);
			list1.add(someObject);
			list1.add(list2);
			list1.add(someObject);

			list2.add(1);
			list2.add(2);
			list2.add(someObject);
			list2.add(list1);
			list2.add(someObject);
			JsonSerializer.serialize(list1);
			Assert.fail("Missing expected Exception");
		} catch (final Exception e) {
			// Expected Exception
			Assert.assertTrue(e.getMessage().contains("Cyclic reference detected"));
		}
	}

	@Test
	public void testNonCylic() throws Exception {
		final TestObjectComplex someObject = new TestObjectComplex();

		final List<Object> list = new ArrayList<>();
		list.add(1);
		list.add(2);
		list.add(someObject);
		list.add(someObject);
		list.add(4);
		JsonSerializer.serialize(list);
	}

	@Test
	public void testNonCylic2() throws Exception {
		final TestObjectComplex someObject = new TestObjectComplex();

		final List<Object> list1 = new ArrayList<>();
		final List<Object> list2 = new ArrayList<>();

		list1.add(1);
		list1.add(2);
		list1.add(someObject);
		list1.add(list2);
		list1.add(someObject);

		list2.add(1);
		list2.add(2);
		list2.add(someObject);
		list2.add(someObject);
		list2.add(someObject);
		JsonSerializer.serialize(list1);
	}
}
