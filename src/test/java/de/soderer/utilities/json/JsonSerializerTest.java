package de.soderer.utilities.json;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonSerializer;
import de.soderer.utilities.json.JsonWriter;

public class JsonSerializerTest {
	@Test
	public void testSimple() throws Exception {
		JsonNode dataNode = JsonSerializer.serialize(new TestObjectSimple());
		JsonWriter.getJsonItemString(dataNode);
	}
	
	@Test
	public void testSimpleWithTypeData() throws Exception {
		JsonNode dataNodeOriginal = JsonSerializer.serialize(new TestObjectSimple(), false, false, false, true);
		String jsonStringOriginal = JsonWriter.getJsonItemString(dataNodeOriginal);
		TestObjectSimple deserializedObject = (TestObjectSimple) JsonSerializer.deserialize(dataNodeOriginal);
		JsonNode dataNodeDeserialized = JsonSerializer.serialize(deserializedObject, false, false, false, true);
		String jsonStringDeserialized = JsonWriter.getJsonItemString(dataNodeDeserialized);
		Assert.assertEquals(jsonStringOriginal, jsonStringDeserialized);
	}
	
	@Test
	public void testAllFields() throws Exception {
		JsonNode dataNode = JsonSerializer.serialize(new TestObjectComplex(), true, true, true, true);
		JsonWriter.getJsonItemString(dataNode);
	}
	
	@Test
	public void testComplexWithTypeData() throws Exception {
		JsonNode dataNodeOriginal = JsonSerializer.serialize(new TestObjectComplex(), false, false, false, true);
		@SuppressWarnings("unused")
		String jsonStringOriginal = JsonWriter.getJsonItemString(dataNodeOriginal);
		TestObjectComplex deserializedObject = (TestObjectComplex) JsonSerializer.deserialize(dataNodeOriginal);
		JsonNode dataNodeDeserialized = JsonSerializer.serialize(deserializedObject, false, false, false, true);
		@SuppressWarnings("unused")
		String jsonStringDeserialized = JsonWriter.getJsonItemString(dataNodeDeserialized);
		// Hashset item order may have changed
		//Assert.assertEquals(jsonStringOriginal, jsonStringDeserialized);
	}
	
	@Test
	public void testCylic1() throws Exception {
		try {
			List<Object> list = new ArrayList<Object>();
			list.add(1);
			list.add(2);
			list.add(3);
			list.add(list);
			list.add(4);
			JsonSerializer.serialize(list);
			Assert.fail("Missing expected Exception");
		} catch (Exception e) {
			// Expected Exception
			Assert.assertTrue(e.getMessage().contains("Cyclic reference detected"));
		}
	}
	
	@Test
	public void testCylic2() throws Exception {
		try {
			TestObjectComplex someObject = new TestObjectComplex();
			
			List<Object> list1 = new ArrayList<Object>();
			List<Object> list2 = new ArrayList<Object>();
			
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
		} catch (Exception e) {
			// Expected Exception
			Assert.assertTrue(e.getMessage().contains("Cyclic reference detected"));
		}
	}
	
	@Test
	public void testNonCylic() throws Exception {
		TestObjectComplex someObject = new TestObjectComplex();
		
		List<Object> list = new ArrayList<Object>();
		list.add(1);
		list.add(2);
		list.add(someObject);
		list.add(someObject);
		list.add(4);
		JsonSerializer.serialize(list);
	}
	
	@Test
	public void testNonCylic2() throws Exception {
		TestObjectComplex someObject = new TestObjectComplex();
		
		List<Object> list1 = new ArrayList<Object>();
		List<Object> list2 = new ArrayList<Object>();
		
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
