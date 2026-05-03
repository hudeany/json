package de.soderer.json;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.soderer.json.exception.JsonWriterStateException;
import de.soderer.json.path.JsonPath;

/**
 * Comprehensive JUnit 5 test suite for JsonReader and JsonWriter.
 *
 * Test areas:
 *  1.  JsonReader - simple values (String, Integer, Float, Boolean, Null)
 *  2.  JsonReader - JsonObject (flat and nested)
 *  3.  JsonReader - JsonArray (flat and nested)
 *  4.  JsonReader - token-based reading (readNextToken)
 *  5.  JsonReader - error handling (malformed JSON)
 *  6.  JsonReader - JsonPath navigation (readUpToJsonPath)
 *  7.  JsonWriter - openJsonObject / closeJsonObject
 *  8.  JsonWriter - openJsonArray / closeJsonArray
 *  9.  JsonWriter - property values of all types (String, Number, Boolean, Null, Date, ...)
 * 10.  JsonWriter - array values of all types
 * 11.  JsonWriter - formatting options (Uglify, custom Indent/Linebreak/Separator)
 * 12.  JsonWriter - error handling (invalid state)
 * 13.  JsonWriter - static helper methods (getJsonItemString, formatStringOutput)
 * 14.  Round-trip tests (Writer -> Reader)
 * 15.  Edge cases (empty object, empty array, Unicode escaping, special characters)
 */
@DisplayName("JsonReader / JsonWriter Tests")
class JsonReaderWriterTest {

    // =========================================================================
    // Helper methods
    // =========================================================================

    private static JsonReader readerOf(final String json) throws Exception {
        return new JsonReader(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    private static String write(final ThrowingConsumer<JsonWriter> action) throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JsonWriter writer = new JsonWriter(baos, StandardCharsets.UTF_8)) {
            action.accept(writer);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    // =========================================================================
    // 1. JsonReader - simple values
    // =========================================================================

    @Nested
    @DisplayName("JsonReader - Simple Values")
    class ReaderSimpleValues {

        @Test
        @DisplayName("Reads a simple string value")
        void readString() throws Exception {
            final JsonNode node = JsonReader.readJsonItemString("\"Hello World\"");
            assertInstanceOf(JsonValueString.class, node);
            assertEquals("Hello World", ((JsonValueString) node).getValue());
        }

        @Test
        @DisplayName("Reads an integer number")
        void readInteger() throws Exception {
            final JsonNode node = JsonReader.readJsonItemString("42");
            assertInstanceOf(JsonValueInteger.class, node);
            assertEquals(42L, ((JsonValueInteger) node).getValue().longValue());
        }

        @Test
        @DisplayName("Reads a negative number")
        void readNegativeNumber() throws Exception {
            final JsonNode node = JsonReader.readJsonItemString("-7");
            assertInstanceOf(JsonValueInteger.class, node);
            assertEquals(-7L, ((JsonValueInteger) node).getValue().longValue());
        }

        @Test
        @DisplayName("Reads a decimal number")
        void readDecimal() throws Exception {
            final JsonNode node = JsonReader.readJsonItemString("3.14");
            assertInstanceOf(JsonValueNumber.class, node);
            assertEquals(3.14, ((JsonValueNumber) node).getValue().doubleValue(), 0.0001);
        }

        @Test
        @DisplayName("Reads boolean true")
        void readBooleanTrue() throws Exception {
            final JsonNode node = JsonReader.readJsonItemString("true");
            assertInstanceOf(JsonValueBoolean.class, node);
            assertTrue(((JsonValueBoolean) node).getValue());
        }

        @Test
        @DisplayName("Reads boolean false")
        void readBooleanFalse() throws Exception {
            final JsonNode node = JsonReader.readJsonItemString("false");
            assertInstanceOf(JsonValueBoolean.class, node);
            assertFalse(((JsonValueBoolean) node).getValue());
        }

        @Test
        @DisplayName("Reads null value")
        void readNull() throws Exception {
            final JsonNode node = JsonReader.readJsonItemString("null");
            assertInstanceOf(JsonValueNull.class, node);
        }

        @Test
        @DisplayName("Reads string with escape sequences")
        void readStringWithEscapes() throws Exception {
            final JsonNode node = JsonReader.readJsonItemString("\"line1\\nline2\\ttab\"");
            assertInstanceOf(JsonValueString.class, node);
            final String value = ((JsonValueString) node).getValue();
            assertTrue(value.contains("\n"));
            assertTrue(value.contains("\t"));
        }

        @Test
        @DisplayName("Reads empty string")
        void readEmptyString() throws Exception {
            final JsonNode node = JsonReader.readJsonItemString("\"\"");
            assertInstanceOf(JsonValueString.class, node);
            assertEquals("", ((JsonValueString) node).getValue());
        }
    }

    // =========================================================================
    // 2. JsonReader - JsonObject
    // =========================================================================

    @Nested
    @DisplayName("JsonReader - JsonObject")
    class ReaderJsonObject {

        @Test
        @DisplayName("Reads empty object")
        void readEmptyObject() throws Exception {
            final JsonNode node = JsonReader.readJsonItemString("{}");
            assertInstanceOf(JsonObject.class, node);
            assertEquals(0, ((JsonObject) node).size());
        }

        @Test
        @DisplayName("Reads flat object with string property")
        void readFlatObjectString() throws Exception {
            final JsonNode node = JsonReader.readJsonItemString("{\"name\":\"Alice\"}");
            assertInstanceOf(JsonObject.class, node);
            final JsonObject obj = (JsonObject) node;
            assertEquals(1, obj.size());
            assertInstanceOf(JsonValueString.class, obj.get("name"));
            assertEquals("Alice", ((JsonValueString) obj.get("name")).getValue());
        }

        @Test
        @DisplayName("Reads object with multiple properties of mixed types")
        void readObjectMixedTypes() throws Exception {
            final String json = "{\"name\":\"Bob\",\"age\":30,\"active\":true,\"score\":9.5,\"note\":null}";
            final JsonObject obj = (JsonObject) JsonReader.readJsonItemString(json);

            assertEquals("Bob", ((JsonValueString) obj.get("name")).getValue());
            assertEquals(30L, ((JsonValueInteger) obj.get("age")).getValue().longValue());
            assertTrue(((JsonValueBoolean) obj.get("active")).getValue());
            assertEquals(9.5, ((JsonValueNumber) obj.get("score")).getValue().doubleValue(), 0.001);
            assertInstanceOf(JsonValueNull.class, obj.get("note"));
        }

        @Test
        @DisplayName("Reads nested object")
        void readNestedObject() throws Exception {
            final String json = "{\"person\":{\"name\":\"Clara\",\"age\":25}}";
            final JsonObject root = (JsonObject) JsonReader.readJsonItemString(json);
            final JsonObject person = (JsonObject) root.get("person");
            assertNotNull(person);
            assertEquals("Clara", ((JsonValueString) person.get("name")).getValue());
            assertEquals(25L, ((JsonValueInteger) person.get("age")).getValue().longValue());
        }

        @Test
        @DisplayName("Reads object with array property")
        void readObjectWithArray() throws Exception {
            final String json = "{\"tags\":[\"java\",\"json\"]}";
            final JsonObject obj = (JsonObject) JsonReader.readJsonItemString(json);
            final JsonArray tags = (JsonArray) obj.get("tags");
            assertNotNull(tags);
            assertEquals(2, tags.size());
        }
    }

    // =========================================================================
    // 3. JsonReader - JsonArray
    // =========================================================================

    @Nested
    @DisplayName("JsonReader - JsonArray")
    class ReaderJsonArray {

        @Test
        @DisplayName("Reads empty array")
        void readEmptyArray() throws Exception {
            final JsonNode node = JsonReader.readJsonItemString("[]");
            assertInstanceOf(JsonArray.class, node);
            assertEquals(0, ((JsonArray) node).size());
        }

        @Test
        @DisplayName("Reads array of strings")
        void readStringArray() throws Exception {
            final JsonArray arr = (JsonArray) JsonReader.readJsonItemString("[\"a\",\"b\",\"c\"]");
            assertEquals(3, arr.size());
            assertEquals("a", ((JsonValueString) arr.get(0)).getValue());
            assertEquals("b", ((JsonValueString) arr.get(1)).getValue());
            assertEquals("c", ((JsonValueString) arr.get(2)).getValue());
        }

        @Test
        @DisplayName("Reads array with mixed types")
        void readMixedArray() throws Exception {
            final JsonArray arr = (JsonArray) JsonReader.readJsonItemString("[1,\"two\",true,null]");
            assertEquals(4, arr.size());
            assertInstanceOf(JsonValueInteger.class, arr.get(0));
            assertInstanceOf(JsonValueString.class, arr.get(1));
            assertInstanceOf(JsonValueBoolean.class, arr.get(2));
            assertInstanceOf(JsonValueNull.class, arr.get(3));
        }

        @Test
        @DisplayName("Reads nested array")
        void readNestedArray() throws Exception {
            final JsonArray outer = (JsonArray) JsonReader.readJsonItemString("[[1,2],[3,4]]");
            assertEquals(2, outer.size());
            final JsonArray inner = (JsonArray) outer.get(0);
            assertEquals(2, inner.size());
            assertEquals(1L, ((JsonValueInteger) inner.get(0)).getValue().longValue());
        }

        @Test
        @DisplayName("Reads array of objects")
        void readArrayOfObjects() throws Exception {
            final String json = "[{\"id\":1},{\"id\":2}]";
            final JsonArray arr = (JsonArray) JsonReader.readJsonItemString(json);
            assertEquals(2, arr.size());
            assertEquals(1L, ((JsonValueInteger) ((JsonObject) arr.get(0)).get("id")).getValue().longValue());
            assertEquals(2L, ((JsonValueInteger) ((JsonObject) arr.get(1)).get("id")).getValue().longValue());
        }
    }

    // =========================================================================
    // 4. JsonReader - token-based reading
    // =========================================================================

    @Nested
    @DisplayName("JsonReader - Token-based Reading")
    class ReaderTokens {

        @Test
        @DisplayName("First token for object is JsonObject_Open")
        void firstTokenIsObjectOpen() throws Exception {
            try (JsonReader reader = readerOf("{\"x\":1}")) {
                final JsonReader.JsonToken token = reader.readNextToken();
                assertEquals(JsonReader.JsonToken.JsonObject_Open, token);
            }
        }

        @Test
        @DisplayName("First token for array is JsonArray_Open")
        void firstTokenIsArrayOpen() throws Exception {
            try (JsonReader reader = readerOf("[1,2]")) {
                final JsonReader.JsonToken token = reader.readNextToken();
                assertEquals(JsonReader.JsonToken.JsonArray_Open, token);
            }
        }

        @Test
        @DisplayName("Token sequence for simple object is correct")
        void tokenSequenceForObject() throws Exception {
            try (JsonReader reader = readerOf("{\"k\":\"v\"}")) {
                assertEquals(JsonReader.JsonToken.JsonObject_Open, reader.readNextToken());
                assertEquals(JsonReader.JsonToken.JsonObject_PropertyKey, reader.readNextToken());
                assertEquals("k", ((JsonValueString) reader.getCurrentObject()).getValue());
                assertEquals(JsonReader.JsonToken.JsonSimpleValue, reader.readNextToken());
                assertEquals("v", ((JsonValueString) reader.getCurrentObject()).getValue());
                assertEquals(JsonReader.JsonToken.JsonObject_Close, reader.readNextToken());
                assertNull(reader.readNextToken());
            }
        }

        @Test
        @DisplayName("getCurrentToken returns the current stack top")
        void getCurrentTokenReturnsTop() throws Exception {
            try (JsonReader reader = readerOf("[1]")) {
                assertNull(reader.getCurrentToken());
                reader.readNextToken();
                assertEquals(JsonReader.JsonToken.JsonArray_Open, reader.getCurrentToken());
            }
        }
    }

    // =========================================================================
    // 5. JsonReader - error handling
    // =========================================================================

    @Nested
    @DisplayName("JsonReader - Error Handling")
    class ReaderErrors {

        @Test
        @DisplayName("Single-quoted value throws exception")
        void singleQuoteThrows() {
            assertThrows(Exception.class, () -> JsonReader.readJsonItemString("'bad'"));
        }

        @Test
        @DisplayName("Mismatched closing bracket throws exception")
        void wrongClosingBracketThrows() {
            assertThrows(Exception.class, () -> JsonReader.readJsonItemString("{\"a\":1]"));
        }

        @Test
        @DisplayName("Unclosed object throws exception")
        void unclosedObjectThrows() {
            assertThrows(Exception.class, () -> JsonReader.readJsonItemString("{\"a\":1"));
        }

        @Test
        @DisplayName("Double comma throws exception")
        void doubleCommaThrows() {
            assertThrows(Exception.class, () -> JsonReader.readJsonItemString("[1,,2]"));
        }

        @Test
        @DisplayName("Trailing comma throws exception")
        void trailingCommaThrows() {
            assertThrows(Exception.class, () -> JsonReader.readJsonItemString("[1,2,]"));
        }

        @Test
        @DisplayName("Invalid unquoted value throws exception")
        void invalidUnquotedValueThrows() {
            assertThrows(Exception.class, () -> JsonReader.readJsonItemString("notAValue"));
        }
    }

    // =========================================================================
    // 6. JsonReader - JsonPath navigation
    // =========================================================================

    @Nested
    @DisplayName("JsonReader - JsonPath Navigation")
    class ReaderJsonPath {

        @Test
        @DisplayName("readUpToJsonPath navigates to the correct position")
        void readUpToPath() throws Exception {
            final String json = "{\"list\":{\"customer\":[{\"name\":\"Dave\"},{\"name\":\"Eve\"}]}}";
            try (JsonReader reader = readerOf(json)) {
                reader.readUpToJsonPath("$.list.customer[0].name");
                reader.readNextToken();
                final JsonNode node = reader.getCurrentObject();
                assertNotNull(node);
                assertInstanceOf(JsonValueString.class, node);
                assertEquals("Dave", ((JsonValueString) node).getValue());
            }
        }

        @Test
        @DisplayName("readUpToJsonPath with missing path throws exception")
        void readUpToMissingPathThrows() {
            assertThrows(Exception.class, () -> {
                try (JsonReader reader = readerOf("{\"a\":1}")) {
                    reader.readUpToJsonPath("$.b.c");
                }
            });
        }

        @Test
        @DisplayName("getCurrentJsonPath returns the correct path string")
        void getCurrentJsonPathReturnsCorrectPath() throws Exception {
            try (JsonReader reader = readerOf("{\"x\":{\"y\":42}}")) {
                reader.readUpToJsonPath("$.x.y");
                final JsonPath path = reader.getCurrentJsonPath();
                assertNotNull(path);
                assertEquals("$.x.y", path.toString());
            }
        }
    }

    // =========================================================================
    // 7. JsonWriter - JsonObject
    // =========================================================================

    @Nested
    @DisplayName("JsonWriter - Writing JsonObject")
    class WriterJsonObject {

        @Test
        @DisplayName("Writes empty object")
        void writeEmptyObject() throws Exception {
            final String result = write(w -> {
                w.openJsonObject();
                w.closeJsonObject();
            });
            assertEquals("{}", result.trim().replace("\n", "").replace("\t", "").replace(" ", ""));
        }

        @Test
        @DisplayName("Writes object with string property")
        void writeObjectWithStringProperty() throws Exception {
            final String result = write(w -> {
                w.openJsonObject();
                w.openJsonObjectProperty("name");
                w.addSimpleJsonObjectPropertyValue("Alice");
                w.closeJsonObject();
            });
            final JsonObject obj = (JsonObject) JsonReader.readJsonItemString(result);
            assertEquals("Alice", ((JsonValueString) obj.get("name")).getValue());
        }

        @Test
        @DisplayName("Writes object with boolean property")
        void writeObjectWithBooleanProperty() throws Exception {
            final String result = write(w -> {
                w.openJsonObject();
                w.openJsonObjectProperty("active");
                w.addSimpleJsonObjectPropertyValue(Boolean.TRUE);
                w.closeJsonObject();
            });
            final JsonObject obj = (JsonObject) JsonReader.readJsonItemString(result);
            assertTrue(((JsonValueBoolean) obj.get("active")).getValue());
        }

        @Test
        @DisplayName("Writes object with number property")
        void writeObjectWithNumberProperty() throws Exception {
            final String result = write(w -> {
                w.openJsonObject();
                w.openJsonObjectProperty("count");
                w.addSimpleJsonObjectPropertyValue(99);
                w.closeJsonObject();
            });
            final JsonObject obj = (JsonObject) JsonReader.readJsonItemString(result);
            assertEquals(99L, ((JsonValueInteger) obj.get("count")).getValue().longValue());
        }

        @Test
        @DisplayName("Writes object with null property via JsonNode overload")
        void writeObjectWithNullJsonNode() throws Exception {
            final String result = write(w -> {
                w.openJsonObject();
                w.openJsonObjectProperty("nothing");
                w.addSimpleJsonObjectPropertyValue((JsonNode) null);
                w.closeJsonObject();
            });
            final JsonObject obj = (JsonObject) JsonReader.readJsonItemString(result);
            assertInstanceOf(JsonValueNull.class, obj.get("nothing"));
        }

        @Test
        @DisplayName("Writes object with null property via addSimpleJsonObjectPropertyValueNull")
        void writeObjectWithExplicitNull() throws Exception {
            final String result = write(w -> {
                w.openJsonObject();
                w.openJsonObjectProperty("empty");
                w.addSimpleJsonObjectPropertyValueNull();
                w.closeJsonObject();
            });
            final JsonObject obj = (JsonObject) JsonReader.readJsonItemString(result);
            assertInstanceOf(JsonValueNull.class, obj.get("empty"));
        }

        @Test
        @DisplayName("Writes object with LocalDate property")
        void writeObjectWithLocalDate() throws Exception {
            final LocalDate date = LocalDate.of(2024, 6, 15);
            final String result = write(w -> {
                w.openJsonObject();
                w.openJsonObjectProperty("date");
                w.addSimpleJsonObjectPropertyValue(date);
                w.closeJsonObject();
            });
            assertTrue(result.contains("2024-06-15"));
        }

        @Test
        @DisplayName("Writes object with LocalDateTime property")
        void writeObjectWithLocalDateTime() throws Exception {
            final LocalDateTime dt = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
            final String result = write(w -> {
                w.openJsonObject();
                w.openJsonObjectProperty("ts");
                w.addSimpleJsonObjectPropertyValue(dt);
                w.closeJsonObject();
            });
            assertTrue(result.contains("2024-06-15"));
        }

        @Test
        @DisplayName("Writes object with ZonedDateTime property")
        void writeObjectWithZonedDateTime() throws Exception {
            final ZonedDateTime zdt = ZonedDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneId.of("UTC"));
            final String result = write(w -> {
                w.openJsonObject();
                w.openJsonObjectProperty("zts");
                w.addSimpleJsonObjectPropertyValue(zdt);
                w.closeJsonObject();
            });
            assertTrue(result.contains("2024-06-15"));
        }

        @Test
        @DisplayName("Writes nested object via add(JsonObject)")
        void writeNestedObjectViaAdd() throws Exception {
            final JsonObject inner = new JsonObject();
            inner.add("x", new JsonValueInteger(7));
            final JsonObject outer = new JsonObject();
            outer.add("inner", inner);

            final String result = JsonWriter.getJsonItemString(outer);
            final JsonObject parsed = (JsonObject) JsonReader.readJsonItemString(result);
            final JsonObject parsedInner = (JsonObject) parsed.get("inner");
            assertEquals(7L, ((JsonValueInteger) parsedInner.get("x")).getValue().longValue());
        }
    }

    // =========================================================================
    // 8. JsonWriter - JsonArray
    // =========================================================================

    @Nested
    @DisplayName("JsonWriter - Writing JsonArray")
    class WriterJsonArray {

        @Test
        @DisplayName("Writes empty array")
        void writeEmptyArray() throws Exception {
            final String result = write(w -> {
                w.openJsonArray();
                w.closeJsonArray();
            });
            assertTrue(result.contains("[") && result.contains("]"));
            final JsonArray arr = (JsonArray) JsonReader.readJsonItemString(result);
            assertEquals(0, arr.size());
        }

        @Test
        @DisplayName("Writes array with string values")
        void writeArrayWithStrings() throws Exception {
            final String result = write(w -> {
                w.openJsonArray();
                w.addSimpleJsonArrayValue("one");
                w.addSimpleJsonArrayValue("two");
                w.closeJsonArray();
            });
            final JsonArray arr = (JsonArray) JsonReader.readJsonItemString(result);
            assertEquals(2, arr.size());
            assertEquals("one", ((JsonValueString) arr.get(0)).getValue());
            assertEquals("two", ((JsonValueString) arr.get(1)).getValue());
        }

        @Test
        @DisplayName("Writes array with number values")
        void writeArrayWithNumbers() throws Exception {
            final String result = write(w -> {
                w.openJsonArray();
                w.addSimpleJsonArrayValue(1);
                w.addSimpleJsonArrayValue(2.5);
                w.closeJsonArray();
            });
            final JsonArray arr = (JsonArray) JsonReader.readJsonItemString(result);
            assertEquals(2, arr.size());
        }

        @Test
        @DisplayName("Writes array with boolean values")
        void writeArrayWithBooleans() throws Exception {
            final String result = write(w -> {
                w.openJsonArray();
                w.addSimpleJsonArrayValue(Boolean.TRUE);
                w.addSimpleJsonArrayValue(Boolean.FALSE);
                w.closeJsonArray();
            });
            final JsonArray arr = (JsonArray) JsonReader.readJsonItemString(result);
            assertEquals(2, arr.size());
            assertTrue(((JsonValueBoolean) arr.get(0)).getValue());
            assertFalse(((JsonValueBoolean) arr.get(1)).getValue());
        }

        @Test
        @DisplayName("Writes array with explicit null via addSimpleJsonArrayValueNull")
        void writeArrayWithExplicitNull() throws Exception {
            final String result = write(w -> {
                w.openJsonArray();
                w.addSimpleJsonArrayValueNull();
                w.closeJsonArray();
            });
            final JsonArray arr = (JsonArray) JsonReader.readJsonItemString(result);
            assertEquals(1, arr.size());
            assertInstanceOf(JsonValueNull.class, arr.get(0));
        }

        @Test
        @DisplayName("Writes array with null via JsonNode overload")
        void writeArrayWithJsonNodeNull() throws Exception {
            final String result = write(w -> {
                w.openJsonArray();
                w.addSimpleJsonArrayValue((JsonNode) null);
                w.closeJsonArray();
            });
            final JsonArray arr = (JsonArray) JsonReader.readJsonItemString(result);
            assertInstanceOf(JsonValueNull.class, arr.get(0));
        }

        @Test
        @DisplayName("Writes array with LocalDate values")
        void writeArrayWithLocalDate() throws Exception {
            final LocalDate date = LocalDate.of(2025, 1, 1);
            final String result = write(w -> {
                w.openJsonArray();
                w.addSimpleJsonArrayValue(date);
                w.closeJsonArray();
            });
            assertTrue(result.contains("2025-01-01"));
        }

        @Test
        @DisplayName("Writes array via add(JsonArray)")
        void writeArrayViaAdd() throws Exception {
            final JsonArray arr = new JsonArray();
            arr.add(new JsonValueString("alpha"));
            arr.add(new JsonValueInteger(42));

            final String result = JsonWriter.getJsonItemString(arr);
            final JsonArray parsed = (JsonArray) JsonReader.readJsonItemString(result);
            assertEquals(2, parsed.size());
            assertEquals("alpha", ((JsonValueString) parsed.get(0)).getValue());
        }
    }

    // =========================================================================
    // 9. JsonWriter - addSimpleValue (standalone)
    // =========================================================================

    @Nested
    @DisplayName("JsonWriter - addSimpleValue")
    class WriterSimpleValue {

        @Test
        @DisplayName("Writes standalone boolean value")
        void writeSimpleBoolean() throws Exception {
            final String result = write(w -> w.addSimpleValue(Boolean.FALSE));
            assertEquals("false", result.trim());
        }

        @Test
        @DisplayName("Writes standalone null value")
        void writeSimpleNull() throws Exception {
            final String result = write(w -> w.addSimpleValue(null));
            assertEquals("null", result.trim());
        }

        @Test
        @DisplayName("Writes standalone number value")
        void writeSimpleNumber() throws Exception {
            final String result = write(w -> w.addSimpleValue(123));
            assertEquals("123", result.trim());
        }

        @Test
        @DisplayName("Writes standalone string value")
        void writeSimpleString() throws Exception {
            final String result = write(w -> w.addSimpleValue("test"));
            assertTrue(result.contains("test"));
        }
    }

    // =========================================================================
    // 10. JsonWriter - formatting options
    // =========================================================================

    @Nested
    @DisplayName("JsonWriter - Formatting Options")
    class WriterFormatting {

        @Test
        @DisplayName("Uglify removes all whitespace")
        void uglifyRemovesWhitespace() throws Exception {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (JsonWriter w = new JsonWriter(baos)) {
                w.setUglify(true);
                w.openJsonObject();
                w.openJsonObjectProperty("a");
                w.addSimpleJsonObjectPropertyValue("b");
                w.closeJsonObject();
            }
            final String result = baos.toString(StandardCharsets.UTF_8);
            assertFalse(result.contains("\n"), "No line break expected with uglify");
            assertFalse(result.contains("\t"), "No tab expected with uglify");
        }

        @Test
        @DisplayName("Custom separator is applied")
        void customSeparator() throws Exception {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (JsonWriter w = new JsonWriter(baos)) {
                w.setSeparator("  ");
                w.openJsonObject();
                w.openJsonObjectProperty("key");
                w.addSimpleJsonObjectPropertyValue("val");
                w.closeJsonObject();
            }
            final String result = baos.toString(StandardCharsets.UTF_8);
            assertTrue(result.contains("\"val\""));
        }

        @Test
        @DisplayName("Null linebreak is treated as empty string")
        void nullLinebreakBecomesEmpty() throws Exception {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (JsonWriter w = new JsonWriter(baos)) {
                w.setLinebreak(null);
                assertEquals("", w.getLinebreak());
            }
        }

        @Test
        @DisplayName("Null separator is treated as empty string")
        void nullSeparatorBecomesEmpty() throws Exception {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (JsonWriter w = new JsonWriter(baos)) {
                w.setSeparator(null);
                assertEquals("", w.getSeparator());
            }
        }

        @Test
        @DisplayName("getJsonItemString with custom formatting produces compact output")
        void getJsonItemStringCustomFormat() throws Exception {
            final JsonObject obj = new JsonObject();
            obj.add("k", new JsonValueString("v"));
            final String result = JsonWriter.getJsonItemString(obj, "", "", "");
            assertFalse(result.contains("\n"));
            assertTrue(result.contains("\"k\""));
        }

        @Test
        @DisplayName("writtenCharacters counter is incremented correctly")
        void writtenCharactersCount() throws Exception {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (JsonWriter w = new JsonWriter(baos)) {
                w.openJsonObject();
                w.closeJsonObject();
                assertTrue(w.getWrittenCharacters() > 0);
            }
        }
    }

    // =========================================================================
    // 11. JsonWriter - error handling (invalid state)
    // =========================================================================

    @Nested
    @DisplayName("JsonWriter - Error Handling")
    class WriterErrors {

        @Test
        @DisplayName("closeJsonObject without openJsonObject throws JsonWriterStateException")
        void closeObjectWithoutOpenThrows() {
            assertThrows(JsonWriterStateException.class, () -> {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (JsonWriter w = new JsonWriter(baos)) {
                    w.closeJsonObject();
                }
            });
        }

        @Test
        @DisplayName("openJsonObjectProperty without an open object throws exception")
        void openPropertyWithoutObjectThrows() {
            assertThrows(Exception.class, () -> {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (JsonWriter w = new JsonWriter(baos)) {
                    w.openJsonObjectProperty("x");
                }
            });
        }

        @Test
        @DisplayName("addSimpleJsonObjectPropertyValue without an open property throws exception")
        void addPropertyValueWithoutPropertyThrows() {
            assertThrows(Exception.class, () -> {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (JsonWriter w = new JsonWriter(baos)) {
                    w.openJsonObject();
                    w.addSimpleJsonObjectPropertyValue("val");
                }
            });
        }

        @Test
        @DisplayName("add(null JsonObject) throws JsonWriterStateException")
        void addNullJsonObjectThrows() {
            assertThrows(JsonWriterStateException.class, () -> {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (JsonWriter w = new JsonWriter(baos)) {
                    w.add((JsonObject) null);
                }
            });
        }

        @Test
        @DisplayName("add(null JsonArray) throws JsonWriterStateException")
        void addNullJsonArrayThrows() {
            assertThrows(JsonWriterStateException.class, () -> {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (JsonWriter w = new JsonWriter(baos)) {
                    w.add((JsonArray) null);
                }
            });
        }

        @Test
        @DisplayName("close() with unclosed items throws IOException")
        void closeWithOpenItemsThrowsIOException() {
            // JsonWriter.close() itself throws IOException when items are still open,
            // so we call it manually rather than via try-with-resources to capture the exception.
            assertThrows(IOException.class, () -> {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                @SuppressWarnings("resource")
				final JsonWriter w = new JsonWriter(baos);
                w.openJsonObject(); // intentionally not closed
                w.close();          // expected to throw here
            });
        }

        @Test
        @DisplayName("addSimpleValue after content has already been written throws exception")
        void addSimpleValueAfterContentThrows() {
            assertThrows(Exception.class, () -> {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (JsonWriter w = new JsonWriter(baos)) {
                    w.addSimpleValue("first");
                    w.addSimpleValue("second"); // invalid - writer already has content
                }
            });
        }
    }

    // =========================================================================
    // 12. JsonWriter - static helper methods
    // =========================================================================

    @Nested
    @DisplayName("JsonWriter - Static Helper Methods")
    class WriterStaticHelpers {

        @Test
        @DisplayName("getJsonItemString(JsonObject) returns valid JSON")
        void getJsonItemStringObject() throws Exception {
            final JsonObject obj = new JsonObject();
            obj.add("greeting", new JsonValueString("Hello"));
            final String result = JsonWriter.getJsonItemString(obj);
            assertTrue(result.contains("\"greeting\""));
            assertTrue(result.contains("\"Hello\""));
        }

        @Test
        @DisplayName("getJsonItemString(JsonArray) returns valid JSON")
        void getJsonItemStringArray() throws Exception {
            final JsonArray arr = new JsonArray();
            arr.add(new JsonValueBoolean(true));
            final String result = JsonWriter.getJsonItemString(arr);
            assertTrue(result.contains("true"));
        }

        @Test
        @DisplayName("getJsonItemString(JsonNode STRING) returns the plain value")
        void getJsonItemStringNodeString() throws Exception {
            final JsonNode node = new JsonValueString("value");
            final String result = JsonWriter.getJsonItemString(node);
            assertEquals("value", result);
        }

        @Test
        @DisplayName("getJsonItemString(JsonNode INTEGER) returns the number as string")
        void getJsonItemStringNodeInteger() throws Exception {
            final JsonNode node = new JsonValueInteger(55);
            final String result = JsonWriter.getJsonItemString(node);
            assertEquals("55", result);
        }

        @Test
        @DisplayName("getJsonItemString(JsonNode BOOLEAN) returns 'true'")
        void getJsonItemStringNodeBoolean() throws Exception {
            final JsonNode node = new JsonValueBoolean(true);
            final String result = JsonWriter.getJsonItemString(node);
            assertEquals("true", result);
        }

        @Test
        @DisplayName("getJsonItemString(JsonNode NULL) returns 'null'")
        void getJsonItemStringNodeNull() throws Exception {
            final JsonNode node = new JsonValueNull();
            final String result = JsonWriter.getJsonItemString(node);
            assertEquals("null", result);
        }

        @Test
        @DisplayName("formatStringOutput escapes all special characters correctly")
        void formatStringOutputEscapesCorrectly() {
            final String input = "a\\b\"c\nd\te\rf\bg";
            final String output = JsonWriter.formatStringOutput(input);
            assertTrue(output.contains("\\\\"));
            assertTrue(output.contains("\\\""));
            assertTrue(output.contains("\\n"));
            assertTrue(output.contains("\\t"));
            assertTrue(output.contains("\\r"));
            assertTrue(output.contains("\\b"));
        }
    }

    // =========================================================================
    // 13. Round-trip tests (Writer -> Reader)
    // =========================================================================

    @Nested
    @DisplayName("Round-trip Tests")
    class RoundTrip {

        @Test
        @DisplayName("Complex nested object survives a write-read round-trip")
        void complexObjectRoundTrip() throws Exception {
            final JsonObject address = new JsonObject();
            address.add("street", new JsonValueString("123 Main Street"));
            address.add("city", new JsonValueString("Springfield"));

            final JsonArray hobbies = new JsonArray();
            hobbies.add(new JsonValueString("Reading"));
            hobbies.add(new JsonValueString("Programming"));

            final JsonObject person = new JsonObject();
            person.add("name", new JsonValueString("John Doe"));
            person.add("age", new JsonValueInteger(42));
            person.add("active", new JsonValueBoolean(true));
            person.add("score", new JsonValueNumber(9.99));
            person.add("note", new JsonValueNull());
            person.add("address", address);
            person.add("hobbies", hobbies);

            final String json = JsonWriter.getJsonItemString(person);
            final JsonObject parsed = (JsonObject) JsonReader.readJsonItemString(json);

            assertEquals("John Doe", ((JsonValueString) parsed.get("name")).getValue());
            assertEquals(42L, ((JsonValueInteger) parsed.get("age")).getValue().longValue());
            assertTrue(((JsonValueBoolean) parsed.get("active")).getValue());
            assertEquals(9.99, ((JsonValueNumber) parsed.get("score")).getValue().doubleValue(), 0.001);
            assertInstanceOf(JsonValueNull.class, parsed.get("note"));

            final JsonObject parsedAddress = (JsonObject) parsed.get("address");
            assertEquals("Springfield", ((JsonValueString) parsedAddress.get("city")).getValue());

            final JsonArray parsedHobbies = (JsonArray) parsed.get("hobbies");
            assertEquals(2, parsedHobbies.size());
            assertEquals("Reading", ((JsonValueString) parsedHobbies.get(0)).getValue());
        }

        @Test
        @DisplayName("Deeply nested array survives a write-read round-trip")
        void deepNestedArrayRoundTrip() throws Exception {
            final JsonArray inner = new JsonArray();
            inner.add(new JsonValueInteger(1));
            inner.add(new JsonValueInteger(2));

            final JsonArray outer = new JsonArray();
            outer.add(inner);
            outer.add(new JsonValueString("extra"));

            final String json = JsonWriter.getJsonItemString(outer);
            final JsonArray parsed = (JsonArray) JsonReader.readJsonItemString(json);

            assertEquals(2, parsed.size());
            final JsonArray parsedInner = (JsonArray) parsed.get(0);
            assertEquals(2, parsedInner.size());
            assertEquals(1L, ((JsonValueInteger) parsedInner.get(0)).getValue().longValue());
        }

        @Test
        @DisplayName("String with special characters survives a write-read round-trip")
        void specialCharactersRoundTrip() throws Exception {
            final String original = "line1\nline2\ttab\"quote\\backslash";
            final JsonObject obj = new JsonObject();
            obj.add("text", new JsonValueString(original));

            final String json = JsonWriter.getJsonItemString(obj);
            final JsonObject parsed = (JsonObject) JsonReader.readJsonItemString(json);
            assertEquals(original, ((JsonValueString) parsed.get("text")).getValue());
        }
    }

    // =========================================================================
    // 14. Edge cases
    // =========================================================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Reads JSON with surrounding whitespace around tokens")
        void readsJsonWithWhitespace() throws Exception {
            final JsonNode node = JsonReader.readJsonItemString("  {  \"a\"  :  1  }  ");
            assertInstanceOf(JsonObject.class, node);
        }

        @Test
        @DisplayName("Reads very large Long number")
        void readsLongNumber() throws Exception {
            final long big = Long.MAX_VALUE;
            final JsonNode node = JsonReader.readJsonItemString(String.valueOf(big));
            assertInstanceOf(JsonValueInteger.class, node);
            assertEquals(big, ((JsonValueInteger) node).getValue().longValue());
        }

        @Test
        @DisplayName("Reads Unicode escape sequence without throwing an exception")
        void readsUnicodeString() throws Exception {
            final JsonNode node = JsonReader.readJsonItemString("\"\\u00e9l\\u00e8ve\"");
            assertInstanceOf(JsonValueString.class, node);
            assertNotNull(((JsonValueString) node).getValue());
        }

        @Test
        @DisplayName("closeAllOpenJsonItems closes all open items correctly")
        void closeAllOpenItems() throws Exception {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (JsonWriter w = new JsonWriter(baos)) {
                w.openJsonArray();
                w.closeAllOpenJsonItems();
            }
            final String result = baos.toString(StandardCharsets.UTF_8);
            assertTrue(result.contains("[") && result.contains("]"));
        }

        @Test
        @DisplayName("flush() does not throw an exception")
        void flushDoesNotThrow() throws Exception {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (JsonWriter w = new JsonWriter(baos)) {
                w.openJsonArray();
                w.closeJsonArray();
                assertDoesNotThrow(w::flush);
            }
        }

        @Test
        @DisplayName("DEFAULT_ENCODING is UTF-8")
        void defaultEncodingIsUtf8() {
            assertEquals(StandardCharsets.UTF_8, JsonWriter.DEFAULT_ENCODING);
        }
    }
}
