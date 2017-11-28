package de.soderer.utilities.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.TextUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.json.Json5Reader;
import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.JsonReader;
import de.soderer.utilities.json.JsonUtilities;
import de.soderer.utilities.json.JsonWriter;
import de.soderer.utilities.json.JsonReader.JsonToken;

public class JsonTest {
	@Test
	public void testEmptyArray() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, "UTF-8");
			writer.openJsonArray();
			writer.closeJsonArray();
			writer.close();
			output.close();
			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals("[]", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			Assert.assertNotNull(jsonReader.read());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testEmptyArray2() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, "UTF-8");
			writer.add(new JsonArray());
			writer.close();
			output.close();
			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals("[]", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			Assert.assertNotNull(jsonReader.read());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testArray() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			Date testDate = new Date();
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, "UTF-8");
			writer.openJsonArray();
			writer.addSimpleJsonArrayValue(null);
			writer.addSimpleJsonArrayValue(true);
			writer.addSimpleJsonArrayValue(1);
			writer.addSimpleJsonArrayValue(2);
			writer.addSimpleJsonArrayValue(1.2f);
			writer.addSimpleJsonArrayValue(0.000002d);
			writer.addSimpleJsonArrayValue(1.3d);
			writer.addSimpleJsonArrayValue(0.000003d);
			writer.addSimpleJsonArrayValue(TextUtilities.GERMAN_TEST_STRING);
			writer.addSimpleJsonArrayValue(testDate);
			writer.closeJsonArray();
			writer.close();
			output.close();
			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals(
				"[\n"
					+ "\tnull,\n"
					+ "\ttrue,\n"
					+ "\t1,\n"
					+ "\t2,\n"
					+ "\t1.2,\n"
					+ "\t2.0E-6,\n"
					+ "\t1.3,\n"
					+ "\t3.0E-6,\n"
					+ "\t\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 äöüßÄÖÜµ!?§@€$%&\\/\\\\<>(){}[]'\\\"´`^°¹²³*#.,;:=+-~_|½¼¬\",\n"
					+ "\t\"" + new SimpleDateFormat(DateUtilities.ISO_8601_DATETIME_FORMAT).format(testDate) + "\"\n"
				+ "]",
				result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			Assert.assertNotNull(jsonReader.read());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testLinebreakMac() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			String result = 
				"[\r"
					+ "\t1,\r"
					+ "\t2\r"
				+ "]\r";
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(1, ((JsonArray) jsonNode.getValue()).get(0));
			Assert.assertEquals(2, ((JsonArray) jsonNode.getValue()).get(1));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testLinebreakWindows() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			String result = 
				"[\r\n"
					+ "\t1,\r\n"
					+ "\t2\r\n"
				+ "]\r\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(1, ((JsonArray) jsonNode.getValue()).get(0));
			Assert.assertEquals(2, ((JsonArray) jsonNode.getValue()).get(1));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testArray2() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			Date testDate = new Date();
			output = new ByteArrayOutputStream();
			
			JsonArray jsonArray = new JsonArray();
			jsonArray.add(null);
			jsonArray.add(true);
			jsonArray.add(1);
			jsonArray.add(2);
			jsonArray.add(1.2f);
			jsonArray.add(0.000002d);
			jsonArray.add(1.3d);
			jsonArray.add(0.000003d);
			jsonArray.add(TextUtilities.GERMAN_TEST_STRING);
			jsonArray.add(testDate);

			writer = new JsonWriter(output, "UTF-8");
			writer.add(jsonArray);
			writer.close();
			output.close();
			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals(jsonArray.toString(), result);
			Assert.assertEquals(
				"[\n"
					+ "\tnull,\n"
					+ "\ttrue,\n"
					+ "\t1,\n"
					+ "\t2,\n"
					+ "\t1.2,\n"
					+ "\t2.0E-6,\n"
					+ "\t1.3,\n"
					+ "\t3.0E-6,\n"
					+ "\t\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 äöüßÄÖÜµ!?§@€$%&\\/\\\\<>(){}[]'\\\"´`^°¹²³*#.,;:=+-~_|½¼¬\",\n"
					+ "\t\"" + new SimpleDateFormat(DateUtilities.ISO_8601_DATETIME_FORMAT).format(testDate) + "\"\n"
				+ "]",
				result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			Assert.assertNotNull(jsonReader.read());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testEmptyObject() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, "UTF-8");
			writer.openJsonObject();
			writer.closeJsonObject();
			writer.close();
			output.close();
			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals("{}", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			Assert.assertNotNull(jsonReader.read());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testEmptyObject2() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, "UTF-8");
			writer.add(new JsonObject());
			writer.close();
			output.close();
			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals("{}", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			Assert.assertNotNull(jsonReader.read());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testObject() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			Date testDate = new Date();
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, "UTF-8");
			writer.openJsonObject();
			writer.openJsonObjectProperty("test_null");
			writer.addSimpleJsonObjectPropertyValue(null);
			writer.openJsonObjectProperty("test_true");
			writer.addSimpleJsonObjectPropertyValue(true);
			writer.openJsonObjectProperty("1");
			writer.addSimpleJsonObjectPropertyValue(1);
			writer.openJsonObjectProperty("2");
			writer.addSimpleJsonObjectPropertyValue(2);
			writer.openJsonObjectProperty("test_float");
			writer.addSimpleJsonObjectPropertyValue(1.2f);
			writer.openJsonObjectProperty("test_floatE");
			writer.addSimpleJsonObjectPropertyValue(0.000002d);
			writer.openJsonObjectProperty("test_double");
			writer.addSimpleJsonObjectPropertyValue(1.3d);
			writer.openJsonObjectProperty("test_doubleE");
			writer.addSimpleJsonObjectPropertyValue(0.000003d);
			writer.openJsonObjectProperty("test_äÄ");
			writer.addSimpleJsonObjectPropertyValue(TextUtilities.GERMAN_TEST_STRING);
			writer.openJsonObjectProperty("test_multiline");
			writer.addSimpleJsonObjectPropertyValue("abc\ndef");
			writer.openJsonObjectProperty("test_date");
			writer.addSimpleJsonObjectPropertyValue(testDate);
			writer.closeJsonObject();
			writer.close();
			output.close();
			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals(
				"{\n"
					+ "\t\"test_null\": null,\n"
					+ "\t\"test_true\": true,\n"
					+ "\t\"1\": 1,\n"
					+ "\t\"2\": 2,\n"
					+ "\t\"test_float\": 1.2,\n"
					+ "\t\"test_floatE\": 2.0E-6,\n"
					+ "\t\"test_double\": 1.3,\n"
					+ "\t\"test_doubleE\": 3.0E-6,\n"
					+ "\t\"test_äÄ\": \"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 äöüßÄÖÜµ!?§@€$%&\\/\\\\<>(){}[]'\\\"´`^°¹²³*#.,;:=+-~_|½¼¬\",\n"
					+ "\t\"test_multiline\": \"abc\\ndef\",\n"
					+ "\t\"test_date\": \"" + new SimpleDateFormat(DateUtilities.ISO_8601_DATETIME_FORMAT).format(testDate) + "\"\n"
				+ "}",
				result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonObject());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testObject2() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			Date testDate = new Date();
			output = new ByteArrayOutputStream();
			
			JsonObject jsonObject = new JsonObject();
			jsonObject.add("test_null", null);
			jsonObject.add("test_true", true);
			jsonObject.add("1", 1);
			jsonObject.add("2", 2);
			jsonObject.add("test_float", 1.2f);
			jsonObject.add("test_floatE", 0.000002d);
			jsonObject.add("test_double", 1.3d);
			jsonObject.add("test_doubleE", 0.000003d);
			jsonObject.add("test_äÄ", TextUtilities.GERMAN_TEST_STRING);
			jsonObject.add("test_date", testDate);
			jsonObject.add("minInteger", Integer.MIN_VALUE);
			jsonObject.add("maxInteger", Integer.MAX_VALUE);

			writer = new JsonWriter(output, "UTF-8");
			writer.add(jsonObject);
			writer.close();
			output.close();

			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals(jsonObject.toString(), result);
			Assert.assertEquals(
				"{\n"
					+ "\t\"test_null\": null,\n"
					+ "\t\"test_true\": true,\n"
					+ "\t\"1\": 1,\n"
					+ "\t\"2\": 2,\n"
					+ "\t\"test_float\": 1.2,\n"
					+ "\t\"test_floatE\": 2.0E-6,\n"
					+ "\t\"test_double\": 1.3,\n"
					+ "\t\"test_doubleE\": 3.0E-6,\n"
					+ "\t\"test_äÄ\": \"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 äöüßÄÖÜµ!?§@€$%&\\/\\\\<>(){}[]'\\\"´`^°¹²³*#.,;:=+-~_|½¼¬\",\n"
					+ "\t\"test_date\": \"" + new SimpleDateFormat(DateUtilities.ISO_8601_DATETIME_FORMAT).format(testDate) + "\",\n"
					+ "\t\"minInteger\": " + Integer.MIN_VALUE + ",\n"
					+ "\t\"maxInteger\": " + Integer.MAX_VALUE + "\n"
				+ "}",
				result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonObject());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testBigDataReader() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			output = new ByteArrayOutputStream();
			
			writer = new JsonWriter(output, "UTF-8");
			
			writer.openJsonArray();
			for (int i = 0; i < 5; i++) {
				JsonObject jsonObject = new JsonObject();	
				jsonObject.add("number", i);
				jsonObject.add("text", TextUtilities.GERMAN_TEST_STRING);
				writer.add(jsonObject);
			}
			writer.addSimpleJsonArrayValue(null);
			writer.addSimpleJsonArrayValue(true);
			writer.addSimpleJsonArrayValue(false);
			writer.closeJsonArray();
			
			writer.close();
			output.close();

			String result = new String(output.toByteArray(), "UTF-8");
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			Assert.assertEquals(JsonReader.JsonToken.JsonArray_Open, jsonReader.readNextToken());
			
			for (int i = 0; i < 5; i++) {
				Assert.assertTrue(jsonReader.readNextJsonNode());
				Object nextJsonNode = jsonReader.getCurrentObject();
				if (nextJsonNode instanceof JsonObject) {
					Assert.assertEquals(new Integer(i), ((JsonObject) nextJsonNode).get("number"));
				} else {
					Assert.fail();
				}
			}
			Assert.assertTrue(jsonReader.readNextJsonNode());
			Assert.assertEquals(null, jsonReader.getCurrentObject());
			Assert.assertTrue(jsonReader.readNextJsonNode());
			Assert.assertEquals(true, jsonReader.getCurrentObject());
			Assert.assertTrue(jsonReader.readNextJsonNode());
			Assert.assertEquals(false, jsonReader.getCurrentObject());
			Assert.assertFalse(jsonReader.readNextJsonNode());
			
			Assert.assertEquals(JsonReader.JsonToken.JsonArray_Close, jsonReader.readNextToken());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testUglified() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			output = new ByteArrayOutputStream();
			
			JsonArray jsonArray = new JsonArray();
			jsonArray.add(1);
			jsonArray.add("Abc");
			
			JsonObject jsonObject = new JsonObject();
			jsonObject.add("Number", 2);
			jsonObject.add("Text", "Abc2");
			jsonObject.add("Array", jsonArray);
			
			Assert.assertEquals("{\n\t\"Number\": 2,\n\t\"Text\": \"Abc2\",\n\t\"Array\":\n\t\t[\n\t\t\t1,\n\t\t\t\"Abc\"\n\t\t]\n}", jsonObject.toString());

			writer = new JsonWriter(output, "UTF-8");
			writer.setUglify(true);
			writer.add(jsonObject);
			writer.close();
			output.close();

			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals("{\"Number\":2,\"Text\":\"Abc2\",\"Array\":[1,\"Abc\"]}", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonObject());
			Assert.assertEquals(3, ((JsonObject) jsonNode.getValue()).size());
			Assert.assertEquals("Abc", ((JsonArray) ((JsonObject) jsonNode.getValue()).get("Array")).get(1));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testUglified2() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			output = new ByteArrayOutputStream();
			
			JsonObject jsonObject = new JsonObject();
			jsonObject.add("Number", 2);
			jsonObject.add("Text", "Abc2");
			
			JsonArray jsonArray = new JsonArray();
			jsonArray.add(1);
			jsonArray.add(jsonObject);
			jsonArray.add("Abc");
			
			Assert.assertEquals("[\n\t1,\n\t{\n\t\t\"Number\": 2,\n\t\t\"Text\": \"Abc2\"\n\t},\n\t\"Abc\"\n]", jsonArray.toString());

			writer = new JsonWriter(output, "UTF-8");
			writer.setUglify(true);
			writer.add(jsonArray);
			writer.close();
			output.close();

			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals("[1,{\"Number\":2,\"Text\":\"Abc2\"},\"Abc\"]", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(3, ((JsonArray) jsonNode.getValue()).size());
			Assert.assertEquals("Abc2", ((JsonObject) ((JsonArray) jsonNode.getValue()).get(1)).get("Text"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testReadToken() {
		JsonReader jsonReader = null;
		try {
			jsonReader = new JsonReader(new ByteArrayInputStream("{\"Number\":1,\"Text\":\"Abc2\",\"Array\":[1,\"Abc\"],\"Number2\":2}".getBytes("UTF-8")));
			
			String[] tokenValues = new String[]{
				"JsonObject_Open: null",
					"JsonObject_PropertyKey: Number", "JsonSimpleValue: 1",
					"JsonObject_PropertyKey: Text", "JsonSimpleValue: Abc2",
					"JsonObject_PropertyKey: Array",
						"JsonArray_Open: null",
							"JsonSimpleValue: 1",
							"JsonSimpleValue: Abc",
						"JsonArray_Close: null",
					"JsonObject_PropertyKey: Number2", "JsonSimpleValue: 2",
				"JsonObject_Close: null"
			};
			
			for (String tokenValue : tokenValues) {
				String nextToken = jsonReader.readNextToken() + ": " + jsonReader.getCurrentObject();
				Assert.assertEquals(tokenValue, nextToken);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testInvalidData() {
		JsonReader jsonReader = null;
		try {
			jsonReader = new JsonReader(new ByteArrayInputStream("{".getBytes("UTF-8")));
			jsonReader.read();
			Assert.fail("Missing an expected exception");
		} catch (Exception e) {
			// Expected Exception
			Assert.assertEquals("Premature end of data", e.getMessage());
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testObjectOfObjects() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			output = new ByteArrayOutputStream();
			
			JsonObject jsonObject1 = new JsonObject();
			jsonObject1.add("Number", 1);
			jsonObject1.add("Text", "Abc1");
			
			JsonObject jsonObject2 = new JsonObject();
			jsonObject2.add("Number", 2);
			jsonObject2.add("Text", "Abc2");
			
			JsonObject jsonObjectOfObjects = new JsonObject();
			jsonObjectOfObjects.add("Text", "Abc");
			jsonObjectOfObjects.add("Object1", jsonObject1);
			jsonObjectOfObjects.add("Object2", jsonObject2);
			
			Assert.assertEquals("{\n\t\"Text\": \"Abc\",\n\t\"Object1\":\n\t\t{\n\t\t\t\"Number\": 1,\n\t\t\t\"Text\": \"Abc1\"\n\t\t},\n\t\"Object2\":\n\t\t{\n\t\t\t\"Number\": 2,\n\t\t\t\"Text\": \"Abc2\"\n\t\t}\n}", jsonObjectOfObjects.toString());

			writer = new JsonWriter(output, "UTF-8");
			writer.setUglify(true);
			writer.add(jsonObjectOfObjects);
			writer.close();
			output.close();

			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals("{\"Text\":\"Abc\",\"Object1\":{\"Number\":1,\"Text\":\"Abc1\"},\"Object2\":{\"Number\":2,\"Text\":\"Abc2\"}}", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonObject());
			Assert.assertEquals(3, ((JsonObject) jsonNode.getValue()).size());
			Assert.assertEquals("Abc1", ((JsonObject) ((JsonObject) jsonNode.getValue()).get("Object1")).get("Text"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testArrayOfArrays() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			output = new ByteArrayOutputStream();
			
			JsonArray jsonArray1 = new JsonArray();
			jsonArray1.add(1);
			jsonArray1.add("Abc1");
			
			JsonArray jsonArray2 = new JsonArray();
			jsonArray2.add(2);
			jsonArray2.add("Abc2");
			
			JsonArray jsonArrayOfArrays = new JsonArray();
			jsonArrayOfArrays.add("Abc");
			jsonArrayOfArrays.add(jsonArray1);
			jsonArrayOfArrays.add(jsonArray2);
			
			Assert.assertEquals("[\n\t\"Abc\",\n\t[\n\t\t1,\n\t\t\"Abc1\"\n\t],\n\t[\n\t\t2,\n\t\t\"Abc2\"\n\t]\n]", jsonArrayOfArrays.toString());

			writer = new JsonWriter(output, "UTF-8");
			writer.setUglify(true);
			writer.add(jsonArrayOfArrays);
			writer.close();
			output.close();

			String result = new String(output.toByteArray(), "UTF-8");
			Assert.assertEquals("[\"Abc\",[1,\"Abc1\"],[2,\"Abc2\"]]", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes("UTF-8")));
			JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(3, ((JsonArray) jsonNode.getValue()).size());
			Assert.assertEquals("Abc1", ((JsonArray) ((JsonArray) jsonNode.getValue()).get(1)).get(1));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testJson5ObjectUnquotedKey() {
		JsonReader jsonReader = null;
		try {
			String data =
				"{\n"
					+ "\ttest : 1\n"
				+ "}\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes("UTF-8")));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testJson5ObjectSingleQuotedKey() {
		JsonReader jsonReader = null;
		try {
			String data =
				"{\n"
					+ "\t'test': 1\n"
				+ "}\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes("UTF-8")));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testJson5ObjectTrailingComma() {
		JsonReader jsonReader = null;
		try {
			String data =
				"{\n"
					+ "\t\"test\": 1,\n"
				+ "}\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes("UTF-8")));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testJson5ArraySingleQuotedStrings() {
		JsonReader jsonReader = null;
		try {
			String data =
				"[\n"
					+ "'Abc'"
				+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes("UTF-8")));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testJson5ArrayTrailingComma() {
		JsonReader jsonReader = null;
		try {
			String data =
				"[\n"
					+ "\"Abc\","
				+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes("UTF-8")));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testJson5MultilineStringDoubleQuoted() {
		JsonReader jsonReader = null;
		try {
			String data =
				"[\n"
					+ "\t\"Abc \\n Abc\"\n"
				+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes("UTF-8")));
			JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(1, ((JsonArray) jsonNode.getValue()).size());
			Assert.assertEquals("Abc \n Abc", ((JsonArray) jsonNode.getValue()).get(0));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testJson5MultilineStringSingleQuoted() {
		JsonReader jsonReader = null;
		try {
			String data =
				"[\n"
					+ "\t'Abc \\\n Abc'\n"
				+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes("UTF-8")));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testJson5HexadecimalNumberValue() {
		JsonReader jsonReader = null;
		try {
			String data =
				"[\n"
					+ "0x1a2B3c4D5e6F\n"
				+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes("UTF-8")));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testJson5LeadingAnTrailingNumberPoints() {
		Json5Reader jsonReader = null;
		try {
			String data =
				"[\n"
					+ "123.,\n"
					+ ".123\n"
				+ "]\n";
			jsonReader = new Json5Reader(new ByteArrayInputStream(data.getBytes("UTF-8")));
			JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(2, ((JsonArray) jsonNode.getValue()).size());
			Assert.assertEquals(123.0f, ((JsonArray) jsonNode.getValue()).get(0));
			Assert.assertEquals(0.123f, ((JsonArray) jsonNode.getValue()).get(1));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testJson5ExtremeNumber() {
		JsonReader jsonReader = null;
		try {
			String data =
				"[\n"
					+ "Infinity,\n"
					+ "-Infinity,\n"
					+ "NaN,\n"
					+ "-NaN\n"
				+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes("UTF-8")));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testJson5LeadingPlusNumbers() {
		Json5Reader jsonReader = null;
		try {
			String data =
				"[\n"
					+ "+123\n"
				+ "]\n";
			jsonReader = new Json5Reader(new ByteArrayInputStream(data.getBytes("UTF-8")));
			JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(1, ((JsonArray) jsonNode.getValue()).size());
			Assert.assertEquals(123, ((JsonArray) jsonNode.getValue()).get(0));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testJson5InlineComment() {
		JsonReader jsonReader = null;
		try {
			String data =
				"[\n"
					+ "// comment in single line\n"
					+ "123\n"
				+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes("UTF-8")));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testJson5BlockComment() {
		JsonReader jsonReader = null;
		try {
			String data =
				"[\n"
					+ "/* comment in \n"
					+ " multiline*/\n"
					+ "123\n"
				+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes("UTF-8")));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testJsonReadPath() {
		JsonReader jsonReader = null;
		try {
			String data =
				"[\n"
					+ "{\n"
						+ "item1:\n"
							+ "{\n"
								+ "item11: 123,\n"
								+ "item12:\n"
									+ "[\n"
										+ "123,\n"
										+ "123,\n"
										+ "123,\n"
									+ "]\n"
							+ "},\n"
						+ "item2: 123,\n"
						+ "item3:\n"
							+ "{\n"
								+ "item31: 123,\n"
								+ "item32: \n"
									+ "[\n"
										+ "123,\n"
										+ "123,\n"
										+ "123,\n"
									+ "]\n"
							+ "},\n"
						+ "item4:\n"
						+ "{\n"
							+ "item41: 123,\n"
							+ "item42: 123\n"
						+ "}\n"
					+ "}\n"
				+ "]\n";
			jsonReader = new Json5Reader(new ByteArrayInputStream(data.getBytes("UTF-8")));
			JsonUtilities.readUpToJsonPath(jsonReader, "$[0].item3.item32");
			jsonReader.readNextToken();
			Assert.assertEquals(JsonToken.JsonArray_Open, jsonReader.getCurrentToken());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testKomplexExampleJson() {
		JsonReader jsonReader = null;
		try {
			jsonReader = new JsonReader(getClass().getClassLoader().getResourceAsStream("json/KomplexExample.json"));
			JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}
	
	@Test
	public void testSpecialCharacters() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (JsonWriter writer = new JsonWriter(output)) {
			writer.addSimpleValue(TextUtilities.SPECIAL_TEST_STRING);
		}

		ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
		JsonNode jsonNode;
		try (JsonReader jsonReader = new JsonReader(input)) {
			jsonNode = jsonReader.read();
		}
		
		Assert.assertEquals(TextUtilities.SPECIAL_TEST_STRING, jsonNode.getValue());
	}
	
	@Test
	public void testSpecialCharacters2() {
		try {
			String testString = "\"\\n\\r\\t\\b\\f\\u00c4\\u00e4\\u00d6\\u00f6\\u00dc\\u00fc\\u00df\"";
			ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes("UTF-8"));
			JsonNode jsonNode;
			try (JsonReader jsonReader = new JsonReader(input)) {
				jsonNode = jsonReader.read();
			}
			
			Assert.assertEquals(TextUtilities.SPECIAL_TEST_STRING, jsonNode.getValue());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testSpecialCharacters2_Error1() throws Exception {
		try {
			String testString = "\"\\u0c\"";
			ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes("UTF-8"));
			try (JsonReader jsonReader = new JsonReader(input)) {
				jsonReader.read();
			}
			Assert.fail("Missing an expected exception");
		} catch (Exception e) {
			// Expected Exception
			Assert.assertEquals("Invalid unicode sequence at character: 6", e.getMessage());
		}
	}
	
	@Test
	public void testSpecialCharacters2_Error2() throws Exception {
		try {
			String testString = "\"\\u0";
			ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes("UTF-8"));
			try (JsonReader jsonReader = new JsonReader(input)) {
				jsonReader.read();
			}
			Assert.fail("Missing an expected exception");
		} catch (Exception e) {
			// Expected Exception
			Assert.assertEquals("Invalid unicode sequence at character: 4", e.getMessage());
		}
	}
	
	@Test
	public void testSpecialCharacters2_Error3() throws Exception {
		try {
			String testString = "\"\\u00FG\"";
			ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes("UTF-8"));
			try (JsonReader jsonReader = new JsonReader(input)) {
				jsonReader.read();
			}
			Assert.fail("Missing an expected exception");
		} catch (Exception e) {
			// Expected Exception
			Assert.assertEquals("Invalid unicode sequence at character: 7", e.getMessage());
		}
	}
}
