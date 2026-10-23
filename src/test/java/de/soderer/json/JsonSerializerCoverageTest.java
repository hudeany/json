package de.soderer.json;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Covers serialization of all supported value types with and without type info, and the different
 * deserialization variants of {@link JsonSerializer}.
 */
@SuppressWarnings({ "static-method" })
class JsonSerializerCoverageTest {
	enum Color {
		RED, GREEN
	}

	static class Inner {
		String name = "inner";
		int value = 7;

		Inner() {
		}

		Inner(final String name, final int value) {
			this.name = name;
			this.value = value;
		}
	}

	static class Base {
		String baseField = "base";
	}

	/** Type set supported by deserialization without type info. */
	static class AllTypes extends Base {
		boolean boolPrim = true;
		Boolean boolObj = Boolean.FALSE;
		byte bytePrim = 1;
		Byte byteObj = Byte.valueOf((byte) 2);
		short shortPrim = 3;
		Short shortObj = Short.valueOf((short) 4);
		int intPrim = 5;
		Integer intObj = Integer.valueOf(6);
		long longPrim = 7L;
		Long longObj = Long.valueOf(8L);
		float floatPrim = 1.5f;
		Float floatObj = Float.valueOf(2.5f);
		double doublePrim = 3.5;
		Double doubleObj = Double.valueOf(4.5);
		BigDecimal bigDecimal = new BigDecimal("12.75");
		char charPrim = 'x';
		Character charObj = Character.valueOf('y');
		String string = "text";
		Charset charset = StandardCharsets.UTF_8;
		LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
		LocalDate localDate = LocalDate.of(2024, 1, 2);
		ZonedDateTime zonedDateTime = ZonedDateTime.of(2024, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC);
		Color color = Color.GREEN;
		String nullValue = null;
		transient String transientValue = "transient";

		boolean[] boolPrimArray = { true, false };
		Boolean[] boolObjArray = { Boolean.TRUE };
		byte[] bytePrimArray = { 1, 2 };
		Byte[] byteObjArray = { Byte.valueOf((byte) 3) };
		short[] shortPrimArray = { 4 };
		Short[] shortObjArray = { Short.valueOf((short) 5) };
		int[] intPrimArray = { 6, 7 };
		Integer[] intObjArray = { Integer.valueOf(8) };
		long[] longPrimArray = { 9L };
		Long[] longObjArray = { Long.valueOf(10L) };
		float[] floatPrimArray = { 1.25f };
		Float[] floatObjArray = { Float.valueOf(2.25f) };
		double[] doublePrimArray = { 3.25 };
		Double[] doubleObjArray = { Double.valueOf(4.25) };
		char[] charPrimArray = { 'a', 'b' };
		Character[] charObjArray = { Character.valueOf('c') };
		String[] stringArray = { "s1", "s2" };
		LocalDateTime[] localDateTimeArray = { LocalDateTime.of(2023, 5, 6, 7, 8, 9) };
		LocalDate[] localDateArray = { LocalDate.of(2023, 5, 6) };
		ZonedDateTime[] zonedDateTimeArray = { ZonedDateTime.of(2023, 5, 6, 7, 8, 9, 0, ZoneOffset.UTC) };
		Color[] colorArray = { Color.RED, Color.GREEN };
		Inner[] innerArray = { new Inner("a", 1) };

		List<String> list = new ArrayList<>(Arrays.asList("l1", "l2"));
		Set<Integer> set = new HashSet<>(Arrays.asList(Integer.valueOf(1), Integer.valueOf(2)));
		Collection<String> collection = new ArrayList<>(Arrays.asList("c1"));
		Map<String, String> map = new LinkedHashMap<>(Map.of("k", "v"));
		Inner inner = new Inner("nested", 42);
	}

	/** Type set supported by deserialization with type info. */
	static class TypedValues {
		Date date = new Date(1_700_000_000_000L);
		File file = new File("some/file.txt");
		Charset charset = StandardCharsets.ISO_8859_1;
		Color color = Color.RED;
		BigDecimal bigDecimal = new BigDecimal("1.5");
		byte byteValue = 1;
		short shortValue = 2;
		long longValue = 3L;
		float floatValue = 4.5f;
		double doubleValue = 5.5;
		char charValue = 'z';
		LocalDateTime localDateTime = LocalDateTime.of(2024, 3, 4, 5, 6, 7);
		LocalDate localDate = LocalDate.of(2024, 3, 4);
		ZonedDateTime zonedDateTime = ZonedDateTime.of(2024, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC);
		String nullString = null;
		Inner inner = new Inner();

		boolean[] boolPrimArray = { true };
		Boolean[] boolObjArray = { Boolean.FALSE };
		byte[] bytePrimArray = { 1 };
		Byte[] byteObjArray = { Byte.valueOf((byte) 2) };
		short[] shortPrimArray = { 3 };
		Short[] shortObjArray = { Short.valueOf((short) 4) };
		int[] intPrimArray = { 5 };
		Integer[] intObjArray = { Integer.valueOf(6) };
		long[] longPrimArray = { 7 };
		Long[] longObjArray = { Long.valueOf(8) };
		float[] floatPrimArray = { 9.5f };
		Float[] floatObjArray = { Float.valueOf(10.5f) };
		double[] doublePrimArray = { 11.5 };
		Double[] doubleObjArray = { Double.valueOf(12.5) };
		char[] charPrimArray = { 'q' };
		Character[] charObjArray = { Character.valueOf('r') };
		String[] stringArray = { "x", null };
		List<Object> list = new ArrayList<>(Arrays.asList("a", Integer.valueOf(1)));
		Map<Object, Object> map = new HashMap<>(Map.of("key", Long.valueOf(5L)));
	}

	@Test
	void serialize_simpleValuesWithoutTypeInfo() throws Exception {
		assertTrue(JsonSerializer.serialize(null).isNull());
		assertTrue(JsonSerializer.serialize(null, true, false, false, true).isNull());
		assertEquals("s", ((JsonValueString) JsonSerializer.serialize("s")).getValue());
		assertTrue(JsonSerializer.serialize(Integer.valueOf(1)).isInteger());
		assertTrue(JsonSerializer.serialize(Long.valueOf(1)).isInteger());
		assertTrue(JsonSerializer.serialize(Float.valueOf(1)).isNumber());
		assertTrue(JsonSerializer.serialize(Double.valueOf(1)).isNumber());
		assertTrue(JsonSerializer.serialize(BigInteger.TEN).isNumber());
		assertEquals("c", ((JsonValueString) JsonSerializer.serialize(Character.valueOf('c'))).getValue());
		assertTrue(JsonSerializer.serialize(Boolean.TRUE).isBoolean());
		assertEquals("UTF-8", ((JsonValueString) JsonSerializer.serialize(StandardCharsets.UTF_8)).getValue());
		assertTrue(JsonSerializer.serialize(new Date(0)).isString());
		assertEquals("2024-01-02T03:04:05", ((JsonValueString) JsonSerializer.serialize(LocalDateTime.of(2024, 1, 2, 3, 4, 5))).getValue());
		assertEquals("2024-01-02", ((JsonValueString) JsonSerializer.serialize(LocalDate.of(2024, 1, 2))).getValue());
		assertTrue(JsonSerializer.serialize(ZonedDateTime.now()).isString());
		assertEquals(new File("x").toString(), ((JsonValueString) JsonSerializer.serialize(new File("x"))).getValue());
		assertEquals("RED", ((JsonValueString) JsonSerializer.serialize(Color.RED)).getValue());
		assertTrue(JsonSerializer.serialize(new int[] { 1 }).isJsonArray());
		assertTrue(JsonSerializer.serialize(Map.of("a", "b")).isJsonArray());
	}

	@Test
	void serialize_simpleValuesWithTypeInfo_roundtrip() throws Exception {
		final Object[] values = {
				"s", Integer.valueOf(1), Long.valueOf(2), Float.valueOf(1.5f), Double.valueOf(2.5), new BigDecimal("3.5"),
				Character.valueOf('c'), Boolean.TRUE, StandardCharsets.UTF_8, new File("dir/file").getAbsoluteFile(), Color.GREEN,
				LocalDate.of(2024, 5, 6), LocalDateTime.of(2024, 5, 6, 7, 8, 9)
		};
		for (final Object value : values) {
			final JsonObject typed = (JsonObject) JsonSerializer.serialize(value, false, false, false, true);
			assertTrue(typed.containsKey("class"));
			assertEquals(value, JsonSerializer.deserialize(typed), "Roundtrip of " + value.getClass());
		}

		final JsonObject nullTyped = (JsonObject) JsonSerializer.serialize(null, false, false, false, true);
		assertTrue(nullTyped.get("class").isNull());
		assertNull(JsonSerializer.deserialize(nullTyped));

		final Date date = new Date(1_700_000_000_000L);
		assertEquals(date, JsonSerializer.deserialize(JsonSerializer.serialize(date, false, false, false, true)));

		final ZonedDateTime zonedDateTime = ZonedDateTime.of(2024, 5, 6, 7, 8, 9, 0, ZoneOffset.UTC);
		assertEquals(zonedDateTime.toInstant(), ((ZonedDateTime) JsonSerializer.deserialize(JsonSerializer.serialize(zonedDateTime, false, false, false, true))).toInstant());

		final BigInteger bigInteger = BigInteger.valueOf(123);
		final JsonObject bigIntegerTyped = (JsonObject) JsonSerializer.serialize(bigInteger, false, false, false, true);
		assertEquals(BigInteger.class.getName(), bigIntegerTyped.getSimpleValue("class"));
	}

	@Test
	void serializeAndDeserialize_withoutTypeInfo_allFieldTypes() throws Exception {
		final AllTypes original = new AllTypes();
		final JsonObject json = (JsonObject) JsonSerializer.serialize(original);
		assertTrue(json.containsKey("baseField"));
		assertFalse(json.containsKey("transientValue"));
		assertTrue(json.get("nullValue").isNull());

		final AllTypes copy = (AllTypes) JsonSerializer.deserialize(AllTypes.class, json);
		assertEquals("base", copy.baseField);
		assertEquals(original.boolPrim, copy.boolPrim);
		assertEquals(original.boolObj, copy.boolObj);
		assertEquals(original.bytePrim, copy.bytePrim);
		assertEquals(original.byteObj, copy.byteObj);
		assertEquals(original.shortPrim, copy.shortPrim);
		assertEquals(original.shortObj, copy.shortObj);
		assertEquals(original.intPrim, copy.intPrim);
		assertEquals(original.intObj, copy.intObj);
		assertEquals(original.longPrim, copy.longPrim);
		assertEquals(original.longObj, copy.longObj);
		assertEquals(original.floatPrim, copy.floatPrim);
		assertEquals(original.floatObj, copy.floatObj);
		assertEquals(original.doublePrim, copy.doublePrim);
		assertEquals(original.doubleObj, copy.doubleObj);
		assertEquals(original.bigDecimal, copy.bigDecimal);
		assertEquals(original.charPrim, copy.charPrim);
		assertEquals(original.charObj, copy.charObj);
		assertEquals(original.string, copy.string);
		assertEquals(original.charset, copy.charset);
		assertEquals(original.localDateTime, copy.localDateTime);
		assertEquals(original.localDate, copy.localDate);
		assertEquals(original.zonedDateTime.toInstant(), copy.zonedDateTime.toInstant());
		assertEquals(original.color, copy.color);
		assertNull(copy.nullValue);

		assertArrayEquals(original.boolPrimArray, copy.boolPrimArray);
		assertArrayEquals(original.boolObjArray, copy.boolObjArray);
		assertArrayEquals(original.bytePrimArray, copy.bytePrimArray);
		assertArrayEquals(original.byteObjArray, copy.byteObjArray);
		assertArrayEquals(original.shortPrimArray, copy.shortPrimArray);
		assertArrayEquals(original.shortObjArray, copy.shortObjArray);
		assertArrayEquals(original.intPrimArray, copy.intPrimArray);
		assertArrayEquals(original.intObjArray, copy.intObjArray);
		assertArrayEquals(original.longPrimArray, copy.longPrimArray);
		assertArrayEquals(original.longObjArray, copy.longObjArray);
		assertArrayEquals(original.floatPrimArray, copy.floatPrimArray);
		assertArrayEquals(original.floatObjArray, copy.floatObjArray);
		assertArrayEquals(original.doublePrimArray, copy.doublePrimArray);
		assertArrayEquals(original.doubleObjArray, copy.doubleObjArray);
		assertArrayEquals(original.charPrimArray, copy.charPrimArray);
		assertArrayEquals(original.charObjArray, copy.charObjArray);
		assertArrayEquals(original.stringArray, copy.stringArray);
		assertArrayEquals(original.localDateTimeArray, copy.localDateTimeArray);
		assertArrayEquals(original.localDateArray, copy.localDateArray);
		assertEquals(original.zonedDateTimeArray[0].toInstant(), copy.zonedDateTimeArray[0].toInstant());
		assertArrayEquals(original.colorArray, copy.colorArray);
		assertEquals("a", copy.innerArray[0].name);
		assertEquals(1, copy.innerArray[0].value);

		assertEquals(original.list, copy.list);
		assertEquals(original.set, copy.set);
		assertEquals(new ArrayList<>(original.collection), new ArrayList<>(copy.collection));
		assertEquals(1, copy.map.size());
		assertEquals("nested", copy.inner.name);
		assertEquals(42, copy.inner.value);
	}

	@Test
	void serialize_excludeNullAndIncludeTransient() throws Exception {
		final AllTypes original = new AllTypes();
		final JsonObject json = (JsonObject) JsonSerializer.serialize(original, true, false, true, false);
		assertFalse(json.containsKey("nullValue"));
		assertEquals("transient", json.getSimpleValue("transientValue"));

		final JsonArray arrayWithoutNulls = (JsonArray) JsonSerializer.serialize(new String[] { "a", null, "b" }, true, false, false, false);
		assertEquals(2, arrayWithoutNulls.size());
		final JsonArray listWithoutNulls = (JsonArray) JsonSerializer.serialize(Arrays.asList("a", null), true, false, false, false);
		assertEquals(1, listWithoutNulls.size());
		final JsonArray listWithNulls = (JsonArray) JsonSerializer.serialize(Arrays.asList("a", null));
		assertEquals(2, listWithNulls.size());
	}

	@Test
	void serialize_mapWithNullKeyAndValue() throws Exception {
		final Map<String, String> map = new HashMap<>();
		map.put(null, "value");
		map.put("key", null);
		final JsonArray json = (JsonArray) JsonSerializer.serialize(map);
		assertEquals(2, json.size());
		for (final JsonNode entry : json.items()) {
			final JsonObject entryObject = (JsonObject) entry;
			assertTrue(entryObject.get("key").isNull() || entryObject.get("value").isNull());
		}
	}

	@Test
	void serializeAndDeserialize_withTypeInfo_allFieldTypes() throws Exception {
		final TypedValues original = new TypedValues();
		final JsonNode json = JsonSerializer.serialize(original, false, false, false, true);
		final TypedValues copy = (TypedValues) JsonSerializer.deserialize(json);

		assertEquals(original.date, copy.date);
		assertEquals(original.file.getAbsoluteFile(), copy.file);
		assertEquals(original.charset, copy.charset);
		assertEquals(original.color, copy.color);
		assertEquals(original.bigDecimal, copy.bigDecimal);
		assertEquals(original.byteValue, copy.byteValue);
		assertEquals(original.shortValue, copy.shortValue);
		assertEquals(original.longValue, copy.longValue);
		assertEquals(original.floatValue, copy.floatValue);
		assertEquals(original.doubleValue, copy.doubleValue);
		assertEquals(original.charValue, copy.charValue);
		assertEquals(original.localDateTime, copy.localDateTime);
		assertEquals(original.localDate, copy.localDate);
		assertEquals(original.zonedDateTime.toInstant(), copy.zonedDateTime.toInstant());
		assertNull(copy.nullString);
		assertEquals("inner", copy.inner.name);

		assertArrayEquals(original.boolPrimArray, copy.boolPrimArray);
		assertArrayEquals(original.boolObjArray, copy.boolObjArray);
		assertArrayEquals(original.bytePrimArray, copy.bytePrimArray);
		assertArrayEquals(original.byteObjArray, copy.byteObjArray);
		assertArrayEquals(original.shortPrimArray, copy.shortPrimArray);
		assertArrayEquals(original.shortObjArray, copy.shortObjArray);
		assertArrayEquals(original.intPrimArray, copy.intPrimArray);
		assertArrayEquals(original.intObjArray, copy.intObjArray);
		assertArrayEquals(original.longPrimArray, copy.longPrimArray);
		assertArrayEquals(original.longObjArray, copy.longObjArray);
		assertArrayEquals(original.floatPrimArray, copy.floatPrimArray);
		assertArrayEquals(original.floatObjArray, copy.floatObjArray);
		assertArrayEquals(original.doublePrimArray, copy.doublePrimArray);
		assertArrayEquals(original.doubleObjArray, copy.doubleObjArray);
		assertArrayEquals(original.charPrimArray, copy.charPrimArray);
		assertArrayEquals(original.charObjArray, copy.charObjArray);
		assertArrayEquals(original.stringArray, copy.stringArray);
		assertEquals(original.list, copy.list);
		assertEquals(original.map, copy.map);
	}

	@Test
	void serialize_cyclicArrayAndMap_throws() {
		final Object[] array = new Object[1];
		array[0] = array;
		assertThrows(Exception.class, () -> JsonSerializer.serialize(array));

		final Map<String, Object> map = new HashMap<>();
		map.put("self", map);
		assertThrows(Exception.class, () -> JsonSerializer.serialize(map));
	}

	@Test
	void serialize_sameObjectTwiceInSiblings_isNoCycle() throws Exception {
		final Inner shared = new Inner();
		final JsonArray json = (JsonArray) JsonSerializer.serialize(new Object[] { shared, shared, new int[0], new int[0] });
		assertEquals(4, json.size());
	}

	@Test
	void deserializeWithTypeInfo_errorCases() throws Exception {
		assertThrows(Exception.class, () -> JsonSerializer.deserialize((JsonNode) null));
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(new JsonValueString("x")));
		assertThrows(Exception.class, () -> JsonSerializer.deserialize((JsonObject) null));
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(new JsonObject().add("value", "x")));
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(new JsonObject().add("class", Integer.valueOf(1)).add("value", "x")));
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(new JsonObject().add("class", String.class.getName())));
		assertNull(JsonSerializer.deserialize(new JsonObject().add("class", String.class.getName()).addNull("value")));

		// Invalid enum constant name
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(new JsonObject().add("class", Color.class.getName()).add("value", "BLUE")));
		// Unsupported class with simple value
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(new JsonObject().add("class", Thread.class.getName()).add("value", "x")));
		// Object class with simple value returns the value itself
		assertEquals("x", JsonSerializer.deserialize(new JsonObject().add("class", Object.class.getName()).add("value", "x")));
		// Object value whose properties have no type info
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(new JsonObject().add("class", Inner.class.getName()).add("value", new JsonObject().add("name", "x"))));
		// Object value with unknown field
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(new JsonObject().add("class", Inner.class.getName()).add("value", new JsonObject().add("unknown", new JsonObject().add("class", String.class.getName()).add("value", "x")))));
		// Map items must be objects
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(new JsonObject().add("class", HashMap.class.getName()).add("value", new JsonArray().add("x"))));
		// Collection items must be objects
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(new JsonObject().add("class", ArrayList.class.getName()).add("value", new JsonArray().add("x"))));
		// Array value for a class that is no collection
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(new JsonObject().add("class", StringBuilder.class.getName()).add("value", new JsonArray())));

		// Double value stored as Float
		assertEquals(Double.valueOf(1.5), JsonSerializer.deserialize(new JsonObject().add("class", Double.class.getName()).add("value", Float.valueOf(1.5f))));
		// Character value stored as single character string
		assertEquals(Character.valueOf('a'), JsonSerializer.deserialize(new JsonObject().add("class", Character.class.getName()).add("value", "abc")));
		// BigDecimal value stored as integer
		assertEquals(new BigDecimal("12"), JsonSerializer.deserialize(new JsonObject().add("class", BigDecimal.class.getName()).add("value", Integer.valueOf(12))));
	}

	@Test
	void deserializeWithClassAndObjectData() throws Exception {
		assertNull(JsonSerializer.deserialize(String.class, (Object) null));
		final Object plain = "plain";
		assertSame(plain, JsonSerializer.deserialize(Object.class, plain));
		assertSame(plain, JsonSerializer.deserialize(String.class, plain));
		assertEquals("text", JsonSerializer.deserialize(String.class, (Object) new JsonValueString("text")));
		final Inner inner = (Inner) JsonSerializer.deserialize(Inner.class, (Object) new JsonObject().add("name", "n"));
		assertEquals("n", inner.name);
	}

	@Test
	void deserializeWithClassAndJsonNode_simpleValues() throws Exception {
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(String.class, (JsonNode) null));
		assertNull(JsonSerializer.deserialize(String.class, new JsonValueNull()));
		assertEquals("n", ((Inner) JsonSerializer.deserialize(Inner.class, (JsonNode) new JsonObject().add("name", "n"))).name);

		assertEquals(Boolean.TRUE, JsonSerializer.deserialize(Boolean.class, new JsonValueBoolean(Boolean.TRUE)));
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(Boolean.TYPE, new JsonValueString("true")));

		assertEquals("s", JsonSerializer.deserialize(String.class, new JsonValueString("s")));
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(String.class, new JsonValueInteger(1)));

		assertEquals(1, ((Number) JsonSerializer.deserialize(Integer.class, new JsonValueInteger(1))).intValue());
		assertEquals(2, ((Number) JsonSerializer.deserialize(Integer.TYPE, new JsonValueNumber(2.7))).intValue());
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(Integer.class, new JsonValueString("1")));

		assertEquals(3L, ((Number) JsonSerializer.deserialize(Long.class, new JsonValueInteger(3L))).longValue());
		assertEquals(4L, ((Number) JsonSerializer.deserialize(Long.TYPE, new JsonValueNumber(4.2))).longValue());
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(Long.class, new JsonValueBoolean(Boolean.TRUE)));

		assertEquals(5.0, ((Number) JsonSerializer.deserialize(Double.class, new JsonValueInteger(5))).doubleValue());
		assertEquals(5.5, ((Number) JsonSerializer.deserialize(Double.TYPE, new JsonValueNumber(5.5))).doubleValue());
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(Double.class, new JsonValueString("x")));

		assertEquals(6, ((Number) JsonSerializer.deserialize(BigDecimal.class, new JsonValueInteger(6))).intValue());
		assertEquals(new BigDecimal("6.5"), JsonSerializer.deserialize(BigDecimal.class, new JsonValueNumber(new BigDecimal("6.5"))));
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(BigDecimal.class, new JsonValueString("x")));

		assertEquals("o", JsonSerializer.deserialize(Object.class, new JsonValueString("o")));
		assertEquals(7, ((Number) JsonSerializer.deserialize(Object.class, new JsonValueInteger(7))).intValue());
		assertEquals(7.5, ((Number) JsonSerializer.deserialize(Object.class, new JsonValueNumber(7.5))).doubleValue());
		assertEquals(Boolean.FALSE, JsonSerializer.deserialize(Object.class, new JsonValueBoolean(Boolean.FALSE)));
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(Object.class, new JsonArray()));

		assertThrows(Exception.class, () -> JsonSerializer.deserialize(Thread.class, new JsonValueString("x")));
	}

	static class NumberCollections {
		List<Long> longs;
		List<Double> doubles;
		List<BigDecimal> bigDecimals;
		double[] doubleArray;
		float[] floatArray;
		Double[] doubleObjArray;
		Float[] floatObjArray;
		BigDecimal bigDecimal;
		long longValue;
	}

	@Test
	void deserialize_numbersKeepTypeAndRange() throws Exception {
		// Long values beyond the int range were cut by intValue() before
		final Object longValue = JsonSerializer.deserialize(Long.class, new JsonValueInteger(5_000_000_000L));
		assertEquals(Long.valueOf(5_000_000_000L), longValue);
		assertEquals(Double.valueOf(5.0), JsonSerializer.deserialize(Double.class, new JsonValueInteger(5)));
		assertEquals(new BigDecimal("7"), JsonSerializer.deserialize(BigDecimal.class, new JsonValueInteger(7)));

		// Values read from JSON text, so the numbers have the types chosen by the reader (Integer, Float, ...)
		final JsonObject json = (JsonObject) JsonReader.readJsonItemString("{\"longs\": [1, 5000000000], \"doubles\": [1, 0.1], \"bigDecimals\": [2, 2.5],"
				+ " \"doubleArray\": [3.25, 1], \"floatArray\": [1.5, 2], \"doubleObjArray\": [0.1], \"floatObjArray\": [4], \"bigDecimal\": 12, \"longValue\": 6000000000}");
		final NumberCollections values = (NumberCollections) JsonSerializer.deserialize(NumberCollections.class, json);
		assertEquals(List.of(Long.valueOf(1), Long.valueOf(5_000_000_000L)), values.longs);
		assertEquals(List.of(Double.valueOf(1), Double.valueOf(0.1)), values.doubles);
		assertEquals(List.of(new BigDecimal("2"), new BigDecimal("2.5")), values.bigDecimals);
		assertArrayEquals(new double[] { 3.25, 1 }, values.doubleArray);
		assertArrayEquals(new float[] { 1.5f, 2 }, values.floatArray);
		assertArrayEquals(new Double[] { Double.valueOf(0.1) }, values.doubleObjArray);
		assertArrayEquals(new Float[] { Float.valueOf(4) }, values.floatObjArray);
		assertEquals(new BigDecimal("12"), values.bigDecimal);
		assertEquals(6_000_000_000L, values.longValue);
	}

	@Test
	void deserializeWithClassAndJsonObject_errorCases() throws Exception {
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(null, new JsonObject()));
		assertNull(JsonSerializer.deserialize(Inner.class, (JsonObject) null));
		// Unknown field
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(Inner.class, new JsonObject().add("unknown", "x")));
		// Value type does not fit the field
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(Inner.class, new JsonObject().add("value", "notANumber")));
		// Map field with non-array value
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(AllTypes.class, new JsonObject().add("map", "x")));
		// Map field with non-object items
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(AllTypes.class, new JsonObject().add("map", new JsonArray().add("x"))));
		// Invalid enum name in array
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(AllTypes.class, new JsonObject().add("colorArray", new JsonArray().add("BLUE"))));
		// Simple value for object field
		assertThrows(Exception.class, () -> JsonSerializer.deserialize(AllTypes.class, new JsonObject().add("inner", "x")));

		// Double field with Float value and unknown enum name for single enum field (ignored)
		final AllTypes values = (AllTypes) JsonSerializer.deserialize(AllTypes.class, new JsonObject()
				.add("doublePrim", Float.valueOf(0.1f))
				.add("color", "BLUE")
				.addNull("string"));
		assertEquals(0.1, values.doublePrim);
		assertEquals(Color.GREEN, values.color);
		assertNull(values.string);
		assertInstanceOf(AllTypes.class, values);
	}
}
