package de.soderer.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.json.JsonReader.JsonToken;
import de.soderer.json.utilities.DateUtilities;
import de.soderer.json.utilities.TextUtilities;
import de.soderer.json.utilities.Utilities;

@SuppressWarnings("static-method")
public class JsonTest {
	@Test
	public void testEmptyArray() {
		JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.openJsonArray();
			writer.closeJsonArray();
			writer.close();
			output.close();
			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
			Assert.assertEquals("[]", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			Assert.assertNotNull(jsonReader.read());
		} catch (final Exception e) {
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
			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.add(new JsonArray());
			writer.close();
			output.close();
			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
			Assert.assertEquals("[]", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			Assert.assertNotNull(jsonReader.read());
		} catch (final Exception e) {
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
			final ZonedDateTime testDate = ZonedDateTime.now();
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.openJsonArray();
			writer.addSimpleJsonArrayValueNull();
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
			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
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
							+ "\t\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT, testDate) + "\"\n"
							+ "]",
							result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			Assert.assertNotNull(jsonReader.read());
		} catch (final Exception e) {
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
		final JsonWriter writer = null;
		final ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			final String result =
					"[\r"
							+ "\t1,\r"
							+ "\t2\r"
							+ "]\r";
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			final JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(1, ((JsonArray) jsonNode).getSimpleValue(0));
			Assert.assertEquals(2, ((JsonArray) jsonNode).getSimpleValue(1));
		} catch (final Exception e) {
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
		final JsonWriter writer = null;
		final ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			final String result =
					"[\r\n"
							+ "\t1,\r\n"
							+ "\t2\r\n"
							+ "]\r\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			final JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(1, ((JsonArray) jsonNode).getSimpleValue(0));
			Assert.assertEquals(2, ((JsonArray) jsonNode).getSimpleValue(1));
		} catch (final Exception e) {
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
			final ZonedDateTime testDate = ZonedDateTime.now();
			output = new ByteArrayOutputStream();

			final JsonArray jsonArray = new JsonArray();
			jsonArray.addNull();
			jsonArray.add(true);
			jsonArray.add(1);
			jsonArray.add(2);
			jsonArray.add(1.2f);
			jsonArray.add(0.000002d);
			jsonArray.add(1.3d);
			jsonArray.add(0.000003d);
			jsonArray.add(TextUtilities.GERMAN_TEST_STRING);
			jsonArray.add(testDate);

			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.add(jsonArray);
			writer.close();
			output.close();
			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
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
							+ "\t\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT, testDate) + "\"\n"
							+ "]",
							result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			Assert.assertNotNull(jsonReader.read());
		} catch (final Exception e) {
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
			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.openJsonObject();
			writer.closeJsonObject();
			writer.close();
			output.close();
			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
			Assert.assertEquals("{}", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			Assert.assertNotNull(jsonReader.read());
		} catch (final Exception e) {
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
			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.add(new JsonObject());
			writer.close();
			output.close();
			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
			Assert.assertEquals("{}", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			Assert.assertNotNull(jsonReader.read());
		} catch (final Exception e) {
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
			final ZonedDateTime testDate = ZonedDateTime.now();
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.openJsonObject();
			writer.openJsonObjectProperty("test_null");
			writer.addSimpleJsonObjectPropertyValueNull();
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
			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
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
							+ "\t\"test_date\": \"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT, testDate) + "\"\n"
							+ "}",
							result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			final JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonObject());
		} catch (final Exception e) {
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
			final ZonedDateTime testDate = ZonedDateTime.now();
			output = new ByteArrayOutputStream();

			final JsonObject jsonObject = new JsonObject();
			jsonObject.addNull("test_null");
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

			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.add(jsonObject);
			writer.close();
			output.close();

			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
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
							+ "\t\"test_date\": \"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT, testDate) + "\",\n"
							+ "\t\"minInteger\": " + Integer.MIN_VALUE + ",\n"
							+ "\t\"maxInteger\": " + Integer.MAX_VALUE + "\n"
							+ "}",
							result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			final JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonObject());
		} catch (final Exception e) {
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

			writer = new JsonWriter(output, StandardCharsets.UTF_8);

			writer.openJsonArray();
			for (int i = 0; i < 5; i++) {
				final JsonObject jsonObject = new JsonObject();
				jsonObject.add("number", i);
				jsonObject.add("text", TextUtilities.GERMAN_TEST_STRING);
				writer.add(jsonObject);
			}
			writer.addSimpleJsonArrayValueNull();
			writer.addSimpleJsonArrayValue(true);
			writer.addSimpleJsonArrayValue(false);
			writer.closeJsonArray();

			writer.close();
			output.close();

			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			Assert.assertEquals(JsonReader.JsonToken.JsonArray_Open, jsonReader.readNextToken());

			for (int i = 0; i < 5; i++) {
				final JsonNode nextJsonNode = jsonReader.readNextJsonNode();
				Assert.assertNotNull(nextJsonNode);
				if (nextJsonNode.isJsonObject()) {
					Assert.assertEquals(i, ((JsonObject) nextJsonNode).getSimpleValue("number"));
				} else {
					Assert.fail();
				}
			}
			Object nextJsonNode;
			nextJsonNode = jsonReader.readNextJsonNode();
			Assert.assertNotNull(nextJsonNode);
			Assert.assertEquals(JsonValueNull.class, jsonReader.getCurrentObject().getClass());
			nextJsonNode = jsonReader.readNextJsonNode();
			Assert.assertNotNull(nextJsonNode);
			Assert.assertEquals(true, ((JsonValueBoolean) jsonReader.getCurrentObject()).getValue());
			nextJsonNode = jsonReader.readNextJsonNode();
			Assert.assertNotNull(nextJsonNode);
			Assert.assertEquals(false, ((JsonValueBoolean) jsonReader.getCurrentObject()).getValue());
			nextJsonNode = jsonReader.readNextJsonNode();
			Assert.assertNull(nextJsonNode);

			Assert.assertEquals(JsonReader.JsonToken.JsonArray_Close, jsonReader.readNextToken());
		} catch (final Exception e) {
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

			final JsonArray jsonArray = new JsonArray();
			jsonArray.add(1);
			jsonArray.add("Abc");

			final JsonObject jsonObject = new JsonObject();
			jsonObject.add("Number", 2);
			jsonObject.add("Text", "Abc2");
			jsonObject.add("Array", jsonArray);

			Assert.assertEquals("{\n\t\"Number\": 2,\n\t\"Text\": \"Abc2\",\n\t\"Array\":\n\t\t[\n\t\t\t1,\n\t\t\t\"Abc\"\n\t\t]\n}", jsonObject.toString());

			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.setUglify(true);
			writer.add(jsonObject);
			writer.close();
			output.close();

			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
			Assert.assertEquals("{\"Number\":2,\"Text\":\"Abc2\",\"Array\":[1,\"Abc\"]}", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			final JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonObject());
			Assert.assertEquals(3, ((JsonObject) jsonNode).size());
			Assert.assertEquals("Abc", ((JsonArray) ((JsonObject) jsonNode).get("Array")).getSimpleValue(1));
		} catch (final Exception e) {
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

			final JsonObject jsonObject = new JsonObject();
			jsonObject.add("Number", 2);
			jsonObject.add("Text", "Abc2");

			final JsonArray jsonArray = new JsonArray();
			jsonArray.add(1);
			jsonArray.add(jsonObject);
			jsonArray.add("Abc");

			Assert.assertEquals("[\n\t1,\n\t{\n\t\t\"Number\": 2,\n\t\t\"Text\": \"Abc2\"\n\t},\n\t\"Abc\"\n]", jsonArray.toString());

			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.setUglify(true);
			writer.add(jsonArray);
			writer.close();
			output.close();

			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
			Assert.assertEquals("[1,{\"Number\":2,\"Text\":\"Abc2\"},\"Abc\"]", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			final JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(3, ((JsonArray) jsonNode).size());
			Assert.assertEquals("Abc2", ((JsonObject) ((JsonArray) jsonNode).get(1)).getSimpleValue("Text"));
		} catch (final Exception e) {
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
			jsonReader = new JsonReader(new ByteArrayInputStream("{\"Number\":1,\"Text\":\"Abc2\",\"Array\":[1,\"Abc\"],\"Number2\":2}".getBytes(StandardCharsets.UTF_8)));

			Assert.assertEquals(JsonToken.JsonObject_Open, jsonReader.readNextToken());
			Assert.assertNull(jsonReader.getCurrentObject());

			Assert.assertEquals(JsonToken.JsonObject_PropertyKey, jsonReader.readNextToken());
			Assert.assertEquals("Number", ((JsonValueString) jsonReader.getCurrentObject()).getValue());
			Assert.assertEquals(JsonToken.JsonSimpleValue, jsonReader.readNextToken());
			Assert.assertEquals(1, ((JsonValueInteger) jsonReader.getCurrentObject()).getValue().intValue());

			Assert.assertEquals(JsonToken.JsonObject_PropertyKey, jsonReader.readNextToken());
			Assert.assertEquals("Text", ((JsonValueString) jsonReader.getCurrentObject()).getValue());
			Assert.assertEquals(JsonToken.JsonSimpleValue, jsonReader.readNextToken());
			Assert.assertEquals("Abc2", ((JsonValueString) jsonReader.getCurrentObject()).getValue());

			Assert.assertEquals(JsonToken.JsonObject_PropertyKey, jsonReader.readNextToken());
			Assert.assertEquals("Array", ((JsonValueString) jsonReader.getCurrentObject()).getValue());
			Assert.assertEquals(JsonToken.JsonArray_Open, jsonReader.readNextToken());
			Assert.assertNull(jsonReader.getCurrentObject());

			Assert.assertEquals(JsonToken.JsonSimpleValue, jsonReader.readNextToken());
			Assert.assertEquals(1, ((JsonValueInteger) jsonReader.getCurrentObject()).getValue().intValue());
			Assert.assertEquals(JsonToken.JsonSimpleValue, jsonReader.readNextToken());
			Assert.assertEquals("Abc", ((JsonValueString) jsonReader.getCurrentObject()).getValue());

			Assert.assertEquals(JsonToken.JsonArray_Close, jsonReader.readNextToken());
			Assert.assertNull(jsonReader.getCurrentObject());

			Assert.assertEquals(JsonToken.JsonObject_PropertyKey, jsonReader.readNextToken());
			Assert.assertEquals("Number2", ((JsonValueString) jsonReader.getCurrentObject()).getValue());
			Assert.assertEquals(JsonToken.JsonSimpleValue, jsonReader.readNextToken());
			Assert.assertEquals(2, ((JsonValueInteger) jsonReader.getCurrentObject()).getValue().intValue());

			Assert.assertEquals(JsonToken.JsonObject_Close, jsonReader.readNextToken());
			Assert.assertNull(jsonReader.getCurrentObject());

			Assert.assertNull(jsonReader.readNextToken());
		} catch (final Exception e) {
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
			jsonReader = new JsonReader(new ByteArrayInputStream("{".getBytes(StandardCharsets.UTF_8)));
			jsonReader.read();
			Assert.fail("Missing an expected exception");
		} catch (final Exception e) {
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

			final JsonObject jsonObject1 = new JsonObject();
			jsonObject1.add("Number", 1);
			jsonObject1.add("Text", "Abc1");

			final JsonObject jsonObject2 = new JsonObject();
			jsonObject2.add("Number", 2);
			jsonObject2.add("Text", "Abc2");

			final JsonObject jsonObjectOfObjects = new JsonObject();
			jsonObjectOfObjects.add("Text", "Abc");
			jsonObjectOfObjects.add("Object1", jsonObject1);
			jsonObjectOfObjects.add("Object2", jsonObject2);

			Assert.assertEquals("{\n\t\"Text\": \"Abc\",\n\t\"Object1\":\n\t\t{\n\t\t\t\"Number\": 1,\n\t\t\t\"Text\": \"Abc1\"\n\t\t},\n\t\"Object2\":\n\t\t{\n\t\t\t\"Number\": 2,\n\t\t\t\"Text\": \"Abc2\"\n\t\t}\n}", jsonObjectOfObjects.toString());

			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.setUglify(true);
			writer.add(jsonObjectOfObjects);
			writer.close();
			output.close();

			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
			Assert.assertEquals("{\"Text\":\"Abc\",\"Object1\":{\"Number\":1,\"Text\":\"Abc1\"},\"Object2\":{\"Number\":2,\"Text\":\"Abc2\"}}", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			final JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonObject());
			Assert.assertEquals(3, ((JsonObject) jsonNode).size());
			Assert.assertEquals("Abc1", ((JsonObject) ((JsonObject) jsonNode).get("Object1")).getSimpleValue("Text"));
		} catch (final Exception e) {
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

			final JsonArray jsonArray1 = new JsonArray();
			jsonArray1.add(1);
			jsonArray1.add("Abc1");

			final JsonArray jsonArray2 = new JsonArray();
			jsonArray2.add(2);
			jsonArray2.add("Abc2");

			final JsonArray jsonArrayOfArrays = new JsonArray();
			jsonArrayOfArrays.add("Abc");
			jsonArrayOfArrays.add(jsonArray1);
			jsonArrayOfArrays.add(jsonArray2);

			Assert.assertEquals("[\n\t\"Abc\",\n\t[\n\t\t1,\n\t\t\"Abc1\"\n\t],\n\t[\n\t\t2,\n\t\t\"Abc2\"\n\t]\n]", jsonArrayOfArrays.toString());

			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.setUglify(true);
			writer.add(jsonArrayOfArrays);
			writer.close();
			output.close();

			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
			Assert.assertEquals("[\"Abc\",[1,\"Abc1\"],[2,\"Abc2\"]]", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			final JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(3, ((JsonArray) jsonNode).size());
			Assert.assertEquals("Abc1", ((JsonArray) ((JsonArray) jsonNode).get(1)).getSimpleValue(1));
		} catch (final Exception e) {
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
			final String data =
					"{\n"
							+ "\ttest : 1\n"
							+ "}\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testJson5ObjectSingleQuotedKey() {
		JsonReader jsonReader = null;
		try {
			final String data =
					"{\n"
							+ "\t'test': 1\n"
							+ "}\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testJson5ObjectTrailingComma() {
		JsonReader jsonReader = null;
		try {
			final String data =
					"{\n"
							+ "\t\"test\": 1,\n"
							+ "}\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testJson5ArraySingleQuotedStrings() {
		JsonReader jsonReader = null;
		try {
			final String data =
					"[\n"
							+ "'Abc'"
							+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testJson5ArrayTrailingComma() {
		JsonReader jsonReader = null;
		try {
			final String data =
					"[\n"
							+ "\"Abc\","
							+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testJson5MultilineStringDoubleQuoted() {
		JsonReader jsonReader = null;
		try {
			final String data =
					"[\n"
							+ "\t\"Abc \\n Abc\"\n"
							+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			final JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(1, ((JsonArray) jsonNode).size());
			Assert.assertEquals("Abc \n Abc", ((JsonArray) jsonNode).getSimpleValue(0));
		} catch (final Exception e) {
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
			final String data =
					"[\n"
							+ "\t'Abc \\\n Abc'\n"
							+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testJson5HexadecimalNumberValue() {
		JsonReader jsonReader = null;
		try {
			final String data =
					"[\n"
							+ "0x1a2B3c4D5e6F\n"
							+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testJson5LeadingAnTrailingNumberPoints() {
		Json5Reader jsonReader = null;
		try {
			final String data =
					"[\n"
							+ "123.,\n"
							+ ".123\n"
							+ "]\n";
			jsonReader = new Json5Reader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			final JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(2, ((JsonArray) jsonNode).size());
			Assert.assertEquals(123.0f, ((JsonArray) jsonNode).getSimpleValue(0));
			Assert.assertEquals(0.123f, ((JsonArray) jsonNode).getSimpleValue(1));
		} catch (final Exception e) {
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
			final String data =
					"[\n"
							+ "Infinity,\n"
							+ "-Infinity,\n"
							+ "NaN,\n"
							+ "-NaN\n"
							+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testJson5LeadingPlusNumbers() {
		Json5Reader jsonReader = null;
		try {
			final String data =
					"[\n"
							+ "+123\n"
							+ "]\n";
			jsonReader = new Json5Reader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			final JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(1, ((JsonArray) jsonNode).size());
			Assert.assertEquals(123, ((JsonArray) jsonNode).getSimpleValue(0));
		} catch (final Exception e) {
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
			final String data =
					"[\n"
							+ "// comment in single line\n"
							+ "123\n"
							+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testJson5BlockComment() {
		JsonReader jsonReader = null;
		try {
			final String data =
					"[\n"
							+ "/* comment in \n"
							+ " multiline*/\n"
							+ "123\n"
							+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			jsonReader.read();
			Assert.fail("Missing expected Exception");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected Exception
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testJsonReadPath() {
		JsonReader jsonReader = null;
		try {
			final String data =
					"[\n"
							+ "{\n"
							+ "\"item1\":\n"
							+ "{\n"
							+ "\"item11\": 123,\n"
							+ "\"item12\":\n"
							+ "[\n"
							+ "123,\n"
							+ "123,\n"
							+ "123\n"
							+ "]\n"
							+ "},\n"
							+ "\"item2\": 123,\n"
							+ "\"item3\":\n"
							+ "{\n"
							+ "\"item31\": 123,\n"
							+ "\"item32\": \n"
							+ "[\n"
							+ "123,\n"
							+ "123,\n"
							+ "123\n"
							+ "]\n"
							+ "},\n"
							+ "\"item4\":\n"
							+ "{\n"
							+ "\"item41\": 123,\n"
							+ "\"item42\": 123\n"
							+ "}\n"
							+ "}\n"
							+ "]\n";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			jsonReader.readUpToJsonPath("$[0].item3.item32");
			jsonReader.readNextToken();
			Assert.assertEquals(JsonToken.JsonArray_Open, jsonReader.getCurrentToken());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testJsonNodes() {
		JsonReader jsonReader = null;
		try {
			final String data =
					"["
							+ "	{"
							+ "		\"item1\":"
							+ "			{"
							+ "				\"item11\": 123,"
							+ "				\"item12\":"
							+ "					["
							+ "						123,"
							+ "						123,"
							+ "						123"
							+ "					]"
							+ "			},"
							+ "		\"item2\": 123,"
							+ "		\"item3\":"
							+ "			{"
							+ "				\"item31\": 123,"
							+ "				\"item32\":"
							+ "					["
							+ "						123,"
							+ "						123,"
							+ "						123"
							+ "					]"
							+ "			},"
							+ "		\"item4\":"
							+ "			{"
							+ "				\"item41\": 123,"
							+ "				\"item42\": 123"
							+ "			}"
							+ "	}"
							+ "]";
			jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
			jsonReader.readUpToJsonPath("$[0].item1");
			final JsonToken nextToken = jsonReader.readNextToken();
			Assert.assertTrue(nextToken == JsonToken.JsonObject_Open);

			JsonNode jsonNode = jsonReader.readNextJsonNode();
			Assert.assertNotNull(jsonNode);
			Assert.assertEquals("item11", ((JsonValueString) jsonNode).getValue());
			jsonNode = jsonReader.readNextJsonNode();
			Assert.assertTrue(jsonNode.isInteger());

			jsonNode = jsonReader.readNextJsonNode();
			Assert.assertNotNull(jsonNode);
			Assert.assertEquals("item12", ((JsonValueString) jsonNode).getValue());
			jsonNode = jsonReader.readNextJsonNode();
			Assert.assertTrue(jsonNode.isJsonArray());
			Assert.assertEquals(3, ((JsonArray) jsonNode).size());

			jsonNode = jsonReader.readNextJsonNode();
			Assert.assertNull(jsonNode);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(jsonReader);
		}
	}

	@Test
	public void testKomplexExampleJson() {
		try (JsonReader jsonReader = new JsonReader(getClass().getClassLoader().getResourceAsStream("json/KomplexExample.json"))) {
			final JsonNode jsonNode = jsonReader.read();
			Assert.assertNotNull(jsonNode);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSpecialCharacters() throws Exception {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (JsonWriter writer = new JsonWriter(output)) {
			writer.addSimpleValue(TextUtilities.SPECIAL_TEST_STRING);
		}

		final ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
		JsonNode jsonNode;
		try (JsonReader jsonReader = new JsonReader(input)) {
			jsonNode = jsonReader.read();
		}

		Assert.assertEquals(TextUtilities.SPECIAL_TEST_STRING, ((JsonValueString) jsonNode).getValue());
	}

	@Test
	public void testSpecialCharacters2() {
		try {
			final String testString = "\"\\n\\r\\t\\b\\f\\u00c4\\u00e4\\u00d6\\u00f6\\u00dc\\u00fc\\u00df\"";
			final ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
			JsonNode jsonNode;
			try (JsonReader jsonReader = new JsonReader(input)) {
				jsonNode = jsonReader.read();
			}

			Assert.assertEquals(TextUtilities.SPECIAL_TEST_STRING, ((JsonValueString) jsonNode).getValue());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSpecialCharacters2_Error1() throws Exception {
		try {
			final String testString = "\"\\u0c\"";
			final ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
			try (JsonReader jsonReader = new JsonReader(input)) {
				jsonReader.read();
			}
			Assert.fail("Missing an expected exception");
		} catch (final Exception e) {
			// Expected Exception
			Assert.assertEquals("Invalid unicode sequence at character index 3 in line 1 ('0c\"')", e.getMessage());
		}
	}

	@Test
	public void testSpecialCharacters2_Error2() throws Exception {
		try {
			final String testString = "\"\\u0";
			final ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
			try (JsonReader jsonReader = new JsonReader(input)) {
				jsonReader.read();
			}
			Assert.fail("Missing an expected exception");
		} catch (final Exception e) {
			// Expected Exception
			Assert.assertEquals("Invalid unicode sequence at character index 3 in line 1 ('0')", e.getMessage());
		}
	}

	@Test
	public void testSpecialCharacters2_Error3() throws Exception {
		try {
			final String testString = "\"\\u00FG\"";
			final ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
			try (JsonReader jsonReader = new JsonReader(input)) {
				jsonReader.read();
			}
			Assert.fail("Missing an expected exception");
		} catch (final Exception e) {
			// Expected Exception
			Assert.assertEquals("Invalid unicode sequence at character index 3 in line 1 ('00FG')", e.getMessage());
		}
	}

	@Test
	public void testStreaming() throws Exception {
		try {
			final String data =
					"[\n"
							+ "{\n"
							+ "\"property\": \"value1\"\n"
							+ "},\n"
							+ "{\n"
							+ "\"property\": \"value2\"\n"
							+ "},\n"
							+ "{\n"
							+ "\"property\": \"value3\"\n"
							+ "},\n"
							+ "{\n"
							+ "\"property\": \"value4\"\n"
							+ "},\n"
							+ "{\n"
							+ "\"otherproperty\": \"othervalue\"\n"
							+ "},\n"
							+ "{\n"
							+ "\"property\": \"value5\"\n"
							+ "}\n"
							+ "]\n";
			try (JsonReader jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)))) {
				final JsonArray jsonArray = ((JsonArray) jsonReader.read());
				final List<String> properties = jsonArray.stream().map(x -> (JsonObject) x).map(x -> x.getSimpleValue("property")).map(x -> (String) x).collect(Collectors.toList());
				Assert.assertEquals(6, properties.size());
				Assert.assertEquals("value3", properties.get(2));
				Assert.assertEquals(null, properties.get(4));
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}
