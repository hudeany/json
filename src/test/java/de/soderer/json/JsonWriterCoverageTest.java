package de.soderer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;

import de.soderer.json.exception.JsonWriterStateException;

/**
 * Covers the low level write API of {@link JsonWriter} for all value types and the state checks.
 */
@SuppressWarnings("static-method")
class JsonWriterCoverageTest {
	private static final LocalDate LOCAL_DATE = LocalDate.of(2024, 6, 7);
	private static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.of(2024, 6, 7, 8, 9, 10);
	private static final LocalDateTime LOCAL_DATE_TIME_NANOS = LocalDateTime.of(2024, 6, 7, 8, 9, 10, 500_000_000);
	private static final ZonedDateTime ZONED_DATE_TIME = ZonedDateTime.of(LOCAL_DATE_TIME, ZoneOffset.UTC);
	private static final ZonedDateTime ZONED_DATE_TIME_NANOS = ZonedDateTime.of(LOCAL_DATE_TIME_NANOS, ZoneOffset.UTC);

	@FunctionalInterface
	private interface WriterAction {
		void run(JsonWriter writer) throws Exception;
	}

	private static String writeCompact(final WriterAction action) throws Exception {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (JsonWriter writer = new JsonWriter(output)) {
			writer.withUglify(true);
			action.run(writer);
		}
		return new String(output.toByteArray(), StandardCharsets.UTF_8);
	}

	@Test
	void objectProperties_allValueTypes() throws Exception {
		final String json = writeCompact(writer -> {
			writer.openJsonObject();
			writer.openJsonObjectProperty("node");
			writer.addSimpleJsonObjectPropertyValue(new JsonValueBoolean(Boolean.TRUE));
			writer.openJsonObjectProperty("nodeInt");
			writer.addSimpleJsonObjectPropertyValue(new JsonValueInteger(1));
			writer.openJsonObjectProperty("nodeNum");
			writer.addSimpleJsonObjectPropertyValue(new JsonValueNumber(new BigDecimal("1.5")));
			writer.openJsonObjectProperty("nodeStr");
			writer.addSimpleJsonObjectPropertyValue(new JsonValueString("s"));
			writer.openJsonObjectProperty("nodeNull");
			writer.addSimpleJsonObjectPropertyValue((JsonNode) null);
			writer.openJsonObjectProperty("null");
			writer.addSimpleJsonObjectPropertyValueNull();
			writer.openJsonObjectProperty("str");
			writer.addSimpleJsonObjectPropertyValue("text");
			writer.openJsonObjectProperty("strNull");
			writer.addSimpleJsonObjectPropertyValue((String) null);
			writer.openJsonObjectProperty("date");
			writer.addSimpleJsonObjectPropertyValue(new Date(0));
			writer.openJsonObjectProperty("dateNull");
			writer.addSimpleJsonObjectPropertyValue((Date) null);
			writer.openJsonObjectProperty("localDate");
			writer.addSimpleJsonObjectPropertyValue(LOCAL_DATE);
			writer.openJsonObjectProperty("localDateNull");
			writer.addSimpleJsonObjectPropertyValue((LocalDate) null);
			writer.openJsonObjectProperty("ldt");
			writer.addSimpleJsonObjectPropertyValue(LOCAL_DATE_TIME);
			writer.openJsonObjectProperty("ldtNanos");
			writer.addSimpleJsonObjectPropertyValue(LOCAL_DATE_TIME_NANOS);
			writer.openJsonObjectProperty("ldtNull");
			writer.addSimpleJsonObjectPropertyValue((LocalDateTime) null);
			writer.openJsonObjectProperty("zdt");
			writer.addSimpleJsonObjectPropertyValue(ZONED_DATE_TIME);
			writer.openJsonObjectProperty("zdtNanos");
			writer.addSimpleJsonObjectPropertyValue(ZONED_DATE_TIME_NANOS);
			writer.openJsonObjectProperty("zdtNull");
			writer.addSimpleJsonObjectPropertyValue((ZonedDateTime) null);
			writer.openJsonObjectProperty("bool");
			writer.addSimpleJsonObjectPropertyValue(Boolean.FALSE);
			writer.openJsonObjectProperty("boolNull");
			writer.addSimpleJsonObjectPropertyValue((Boolean) null);
			writer.openJsonObjectProperty("num");
			writer.addSimpleJsonObjectPropertyValue(Integer.valueOf(42));
			writer.openJsonObjectProperty("numNull");
			writer.addSimpleJsonObjectPropertyValue((Number) null);
			writer.closeJsonObject();
		});

		assertTrue(json.startsWith("{\"node\":true,\"nodeInt\":1,\"nodeNum\":1.5,\"nodeStr\":\"s\",\"nodeNull\":null,\"null\":null,\"str\":\"text\",\"strNull\":null,\"date\":\""), json);
		assertTrue(json.contains("\"dateNull\":null"), json);
		assertTrue(json.contains("\"localDate\":\"2024-06-07\",\"localDateNull\":null"), json);
		assertTrue(json.contains("\"ldt\":\"2024-06-07T08:09:10\",\"ldtNanos\":\"2024-06-07T08:09:10.500000000\",\"ldtNull\":null"), json);
		assertTrue(json.contains("\"zdt\":\"2024-06-07T08:09:10Z\",\"zdtNanos\":\"2024-06-07T08:09:10.500000000Z\",\"zdtNull\":null"), json);
		assertTrue(json.endsWith("\"bool\":false,\"boolNull\":null,\"num\":42,\"numNull\":null}"), json);
		// The parsed result must be valid JSON
		assertEquals(22, ((JsonObject) JsonReader.readJsonItemString(json)).size());
	}

	@Test
	void arrayValues_allValueTypes() throws Exception {
		final String json = writeCompact(writer -> {
			writer.openJsonArray();
			writer.addSimpleJsonArrayValue(new JsonValueBoolean(Boolean.TRUE));
			writer.addSimpleJsonArrayValue(new JsonValueInteger(1));
			writer.addSimpleJsonArrayValue(new JsonValueNumber(new BigDecimal("1.5")));
			writer.addSimpleJsonArrayValue(new JsonValueString("s"));
			writer.addSimpleJsonArrayValue((JsonNode) null);
			writer.addSimpleJsonArrayValueNull();
			writer.addSimpleJsonArrayValue("text");
			writer.addSimpleJsonArrayValue((String) null);
			writer.addSimpleJsonArrayValue(new Date(0));
			writer.addSimpleJsonArrayValue((Date) null);
			writer.addSimpleJsonArrayValue(LOCAL_DATE);
			writer.addSimpleJsonArrayValue((LocalDate) null);
			writer.addSimpleJsonArrayValue(LOCAL_DATE_TIME);
			writer.addSimpleJsonArrayValue(LOCAL_DATE_TIME_NANOS);
			writer.addSimpleJsonArrayValue((LocalDateTime) null);
			writer.addSimpleJsonArrayValue(ZONED_DATE_TIME);
			writer.addSimpleJsonArrayValue(ZONED_DATE_TIME_NANOS);
			writer.addSimpleJsonArrayValue((ZonedDateTime) null);
			writer.addSimpleJsonArrayValue(Boolean.FALSE);
			writer.addSimpleJsonArrayValue((Boolean) null);
			writer.addSimpleJsonArrayValue(Long.valueOf(42));
			writer.addSimpleJsonArrayValue((Number) null);
			writer.closeJsonArray();
		});

		final JsonArray array = (JsonArray) JsonReader.readJsonItemString(json);
		assertEquals(22, array.size());
		assertEquals("2024-06-07", array.getSimpleItem(10));
		assertEquals("2024-06-07T08:09:10", array.getSimpleItem(12));
		assertEquals("2024-06-07T08:09:10Z", array.getSimpleItem(15));
		assertEquals(Boolean.FALSE, array.getSimpleItem(18));
		assertEquals(42, ((Number) array.getSimpleItem(20)).intValue());
	}

	@Test
	void firstArrayValueOfEachTypeAfterEmptyArray() throws Exception {
		// Each typed add method has its own handling for the first item of an array
		final WriterAction[] firstValues = {
				writer -> writer.addSimpleJsonArrayValue(new Date(0)),
				writer -> writer.addSimpleJsonArrayValue(LOCAL_DATE),
				writer -> writer.addSimpleJsonArrayValue(LOCAL_DATE_TIME),
				writer -> writer.addSimpleJsonArrayValue(ZONED_DATE_TIME),
				writer -> writer.addSimpleJsonArrayValue(Boolean.TRUE),
				writer -> writer.addSimpleJsonArrayValue(Integer.valueOf(1)),
				writer -> writer.addSimpleJsonArrayValue("x"),
				JsonWriter::addSimpleJsonArrayValueNull
		};
		for (final WriterAction firstValue : firstValues) {
			final String json = writeCompact(writer -> {
				writer.openJsonArray();
				firstValue.run(writer);
				writer.closeJsonArray();
			});
			assertEquals(1, ((JsonArray) JsonReader.readJsonItemString(json)).size());
		}
	}

	@Test
	void nestedItemsInArraysAndObjects() throws Exception {
		final String json = writeCompact(writer -> {
			writer.openJsonArray();
			writer.openJsonObject();
			writer.closeJsonObject();
			writer.openJsonObject();
			writer.openJsonObjectProperty("arr");
			writer.openJsonArray();
			writer.openJsonArray();
			writer.closeJsonArray();
			writer.closeJsonArray();
			writer.openJsonObjectProperty("obj");
			writer.openJsonObject();
			writer.closeJsonObject();
			writer.closeJsonObject();
			writer.openJsonArray();
			writer.closeJsonArray();
			writer.closeJsonArray();
		});
		assertEquals("[{},{\"arr\":[[]],\"obj\":{}},[]]", json);
	}

	@Test
	void stateErrors() throws Exception {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		final JsonWriter writer = new JsonWriter(output);
		// Nothing open yet
		assertThrows(JsonWriterStateException.class, writer::closeJsonObject);
		assertThrows(JsonWriterStateException.class, () -> writer.openJsonObjectProperty("x"));

		writer.openJsonArray();
		assertThrows(JsonWriterStateException.class, () -> writer.openJsonObjectProperty("x"));
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonObjectPropertyValue("x"));
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonObjectPropertyValue((JsonNode) null));
		assertThrows(JsonWriterStateException.class, writer::addSimpleJsonObjectPropertyValueNull);
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonObjectPropertyValue(new Date()));
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonObjectPropertyValue(LOCAL_DATE));
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonObjectPropertyValue(LOCAL_DATE_TIME));
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonObjectPropertyValue(ZONED_DATE_TIME));
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonObjectPropertyValue(Boolean.TRUE));
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonObjectPropertyValue(Integer.valueOf(1)));
		assertThrows(JsonWriterStateException.class, writer::closeJsonObject);
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleValue("x"));

		writer.openJsonObject();
		assertThrows(JsonWriterStateException.class, writer::openJsonObject);
		assertThrows(JsonWriterStateException.class, writer::openJsonArray);
		assertThrows(JsonWriterStateException.class, writer::closeJsonArray);
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonArrayValue((JsonNode) null));
		assertThrows(JsonWriterStateException.class, writer::addSimpleJsonArrayValueNull);
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonArrayValue("x"));
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonArrayValue(new Date()));
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonArrayValue(LOCAL_DATE));
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonArrayValue(LOCAL_DATE_TIME));
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonArrayValue(ZONED_DATE_TIME));
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonArrayValue(Boolean.TRUE));
		assertThrows(JsonWriterStateException.class, () -> writer.addSimpleJsonArrayValue(Integer.valueOf(1)));

		assertThrows(JsonWriterStateException.class, () -> writer.add((JsonObject) null));
		assertThrows(JsonWriterStateException.class, () -> writer.add((JsonArray) null));
		assertThrows(JsonWriterStateException.class, () -> writer.add((JsonNode) null));

		// Closing with open items reports them, but closes the stream anyway
		final IOException exception = assertThrows(IOException.class, writer::close);
		assertTrue(exception.getMessage().contains("Object_Empty"), exception.getMessage());
		assertThrows(IllegalStateException.class, writer::openJsonObject);
	}

	@Test
	void closeAllOpenJsonItems_finishesIncompleteObjects() throws Exception {
		assertEquals("{}", writeCompact(writer -> {
			writer.openJsonObject();
			writer.closeAllOpenJsonItems();
		}));
		assertEquals("[\"a\",[{\"k\":{\"x\":\"y\"},\"list\":[1]}]]", writeCompact(writer -> {
			writer.openJsonArray();
			writer.addSimpleJsonArrayValue("a");
			writer.openJsonArray();
			writer.openJsonObject();
			writer.openJsonObjectProperty("k");
			writer.openJsonObject();
			writer.openJsonObjectProperty("x");
			writer.addSimpleJsonObjectPropertyValue("y");
			writer.closeJsonObject();
			writer.openJsonObjectProperty("list");
			writer.openJsonArray();
			writer.addSimpleJsonArrayValue(Integer.valueOf(1));
			writer.closeAllOpenJsonItems();
		}));
		// A property without value is completed with null, so the output stays valid JSON
		final String danglingProperty = writeCompact(writer -> {
			writer.openJsonObject();
			writer.openJsonObjectProperty("open");
			writer.closeAllOpenJsonItems();
		});
		assertEquals("{\"open\":null}", danglingProperty);
		assertTrue(JsonReader.readJsonItemString(danglingProperty).isJsonObject());
	}

	@Test
	void fractionOfSecond_isWrittenAsDecimalFraction() throws Exception {
		// 5 milliseconds must not be written as ".5000000", which means 0.5 seconds in ISO 8601
		final LocalDateTime fiveMillis = LocalDateTime.of(2024, 6, 7, 8, 9, 10, 5_000_000);
		assertEquals("\"2024-06-07T08:09:10.005000000\"", writeCompact(writer -> writer.addSimpleValue(fiveMillis)));
		assertEquals("\"2024-06-07T08:09:10.005000000Z\"", writeCompact(writer -> writer.addSimpleValue(ZonedDateTime.of(fiveMillis, ZoneOffset.UTC))));
		final String json = new JsonObject().add("t", fiveMillis).toString();
		assertTrue(json.contains("2024-06-07T08:09:10.005000000"), json);
		assertEquals(fiveMillis, de.soderer.json.utilities.DateUtilities.parseIso8601DateTimeString("2024-06-07T08:09:10.005000000", ZoneOffset.UTC).toLocalDateTime());
	}

	@Test
	void closeAllOpenJsonItems_finishesIncompleteArrays() throws Exception {
		final String json = writeCompact(writer -> {
			writer.openJsonArray();
			writer.addSimpleJsonArrayValue("a");
			writer.openJsonArray();
			writer.closeAllOpenJsonItems();
		});
		assertEquals("[\"a\",[]]", json);
	}

	@Test
	void addSimpleValue_allTypes() throws Exception {
		assertEquals("null", writeCompact(writer -> writer.addSimpleValue(null)));
		assertEquals("true", writeCompact(writer -> writer.addSimpleValue(Boolean.TRUE)));
		assertTrue(writeCompact(writer -> writer.addSimpleValue(new Date(0))).startsWith("\"19"));
		assertEquals("\"2024-06-07T08:09:10\"", writeCompact(writer -> writer.addSimpleValue(LOCAL_DATE_TIME)));
		assertEquals("\"2024-06-07T08:09:10.500000000\"", writeCompact(writer -> writer.addSimpleValue(LOCAL_DATE_TIME_NANOS)));
		assertEquals("\"2024-06-07\"", writeCompact(writer -> writer.addSimpleValue(LOCAL_DATE)));
		assertEquals("\"2024-06-07T08:09:10Z\"", writeCompact(writer -> writer.addSimpleValue(ZONED_DATE_TIME)));
		assertEquals("\"2024-06-07T08:09:10.500000000Z\"", writeCompact(writer -> writer.addSimpleValue(ZONED_DATE_TIME_NANOS)));
		assertEquals("12.5", writeCompact(writer -> writer.addSimpleValue(Double.valueOf(12.5))));
		assertEquals("\"a\\\"b\"", writeCompact(writer -> writer.addSimpleValue(new StringBuilder("a\"b"))));
		assertThrows(JsonWriterStateException.class, () -> writeCompact(writer -> {
			writer.addSimpleValue("x");
			writer.addSimpleValue("y");
		}));
	}

	@Test
	void addJsonNode_simpleTopLevelValues() throws Exception {
		assertEquals("null", writeCompact(writer -> writer.add(new JsonValueNull())));
		assertEquals("false", writeCompact(writer -> writer.add(new JsonValueBoolean(Boolean.FALSE))));
		assertEquals("3", writeCompact(writer -> writer.add(new JsonValueInteger(3))));
		assertEquals("3.5", writeCompact(writer -> writer.add(new JsonValueNumber(3.5))));
		assertEquals("\"s\"", writeCompact(writer -> writer.add(new JsonValueString("s"))));
		assertEquals("{}", writeCompact(writer -> writer.add((JsonNode) new JsonObject())));
		assertEquals("[]", writeCompact(writer -> writer.add((JsonNode) new JsonArray())));
	}

	@Test
	void formattingSettings() throws Exception {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (JsonWriter writer = new JsonWriter(output, StandardCharsets.UTF_8)) {
			assertSame(writer, writer.withIndentation("  ").withLinebreak("\r\n").withSeparator("  "));
			assertEquals("\r\n", writer.getLinebreak());
			assertEquals("  ", writer.getSeparator());
			writer.add(new JsonObject().add("a", new JsonArray().add(Integer.valueOf(1))));
			writer.flush();
			assertTrue(writer.getWrittenCharacters() > 0);
		}
		final String formatted = new String(output.toByteArray(), StandardCharsets.UTF_8);
		assertTrue(formatted.startsWith("{\r\n  \"a\":"), formatted);
		assertTrue(formatted.endsWith("\r\n}"), formatted);
		assertEquals(1, ((JsonArray) ((JsonObject) JsonReader.readJsonItemString(formatted)).get("a")).size());

		final ByteArrayOutputStream output2 = new ByteArrayOutputStream();
		try (JsonWriter writer = new JsonWriter(output2)) {
			writer.withIndentation(' ').withUglify(false);
			writer.setIndentation((String) null);
			writer.setLinebreak(null);
			writer.setSeparator(null);
			writer.withIndentation('-').add(new JsonArray().add("x"));
		}
		assertEquals("[-\"x\"]", new String(output2.toByteArray(), StandardCharsets.UTF_8));
	}

	@Test
	void getJsonItemString_variants() throws Exception {
		final JsonArray array = new JsonArray().add("x");
		assertEquals("[\"x\"]", JsonWriter.getJsonItemString(array, "", "", ""));
		assertEquals("[\n\t\"x\"\n]", JsonWriter.getJsonItemString(array));
		assertEquals("{\"k\":1}", JsonWriter.getJsonItemString((JsonNode) new JsonObject().add("k", Integer.valueOf(1)), "", "", ""));
		assertEquals("[\"x\"]", JsonWriter.getJsonItemString((JsonNode) array, "", "", ""));
		assertEquals("\"a\\nb\"", JsonWriter.getJsonItemString(new JsonValueString("a\nb")));
		assertEquals("1", JsonWriter.getJsonItemString(new JsonValueInteger(1)));
		assertEquals("1.5", JsonWriter.getJsonItemString(new JsonValueNumber(1.5)));
		assertEquals("true", JsonWriter.getJsonItemString(new JsonValueBoolean(Boolean.TRUE)));
		assertEquals("null", JsonWriter.getJsonItemString(new JsonValueNull()));
	}
}
