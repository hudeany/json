package de.soderer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.exception.JsonInvalidValueTypeException;
import de.soderer.json.exception.MissingKeyException;

/**
 * Covers all typed add/replace/put overloads of {@link JsonObject} including their error branches.
 */
@SuppressWarnings("static-method")
class JsonObjectCoverageTest {
	private static final LocalDate LOCAL_DATE = LocalDate.of(2024, 2, 29);
	private static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.of(2024, 2, 29, 13, 14, 15);
	private static final LocalDateTime LOCAL_DATE_TIME_NANOS = LocalDateTime.of(2024, 2, 29, 13, 14, 15, 500_000_000);
	private static final ZonedDateTime ZONED_DATE_TIME = ZonedDateTime.of(LOCAL_DATE_TIME, ZoneOffset.UTC);
	private static final ZonedDateTime ZONED_DATE_TIME_NANOS = ZonedDateTime.of(LOCAL_DATE_TIME_NANOS, ZoneOffset.UTC);

	private static String stringValue(final JsonObject object, final String key) {
		return ((JsonValueString) object.get(key)).getValue();
	}

	@Test
	void add_allTypedOverloads_storeExpectedNodeTypes() throws Exception {
		final JsonObject object = new JsonObject()
				.addNull("null")
				.add("string", "text")
				.add("integer", Integer.valueOf(1))
				.add("long", Long.valueOf(2L))
				.add("numberInteger", (Number) Integer.valueOf(3))
				.add("numberLong", (Number) Long.valueOf(4L))
				.add("numberDouble", Double.valueOf(5.5))
				.add("boolean", Boolean.TRUE)
				.add("date", new Date(0))
				.add("localDate", LOCAL_DATE)
				.add("localDateTime", LOCAL_DATE_TIME)
				.add("localDateTimeNanos", LOCAL_DATE_TIME_NANOS)
				.add("zonedDateTime", ZONED_DATE_TIME)
				.add("zonedDateTimeNanos", ZONED_DATE_TIME_NANOS)
				.add("node", new JsonArray());

		assertEquals(15, object.size());
		assertTrue(object.get("null").isNull());
		assertEquals("text", stringValue(object, "string"));
		assertInstanceOf(JsonValueInteger.class, object.get("integer"));
		assertInstanceOf(JsonValueInteger.class, object.get("long"));
		assertInstanceOf(JsonValueInteger.class, object.get("numberInteger"));
		assertInstanceOf(JsonValueInteger.class, object.get("numberLong"));
		assertInstanceOf(JsonValueNumber.class, object.get("numberDouble"));
		assertEquals(Boolean.TRUE, object.getSimpleValue("boolean"));
		assertTrue(object.get("date").isString());
		assertEquals("2024-02-29", stringValue(object, "localDate"));
		assertEquals("2024-02-29T13:14:15", stringValue(object, "localDateTime"));
		assertTrue(stringValue(object, "localDateTimeNanos").startsWith("2024-02-29T13:14:15."));
		assertEquals("2024-02-29T13:14:15Z", stringValue(object, "zonedDateTime"));
		assertTrue(stringValue(object, "zonedDateTimeNanos").startsWith("2024-02-29T13:14:15."));
		assertTrue(object.get("node").isJsonArray());
	}

	@Test
	void add_nullValues_storeJsonNull() throws Exception {
		final JsonObject object = new JsonObject()
				.add("string", (String) null)
				.add("integer", (Integer) null)
				.add("long", (Long) null)
				.add("number", (Number) null)
				.add("boolean", (Boolean) null)
				.add("date", (Date) null)
				.add("localDate", (LocalDate) null)
				.add("localDateTime", (LocalDateTime) null)
				.add("zonedDateTime", (ZonedDateTime) null)
				.add("node", (JsonNode) null)
				.add("object", (Object) null);

		assertEquals(11, object.size());
		for (final JsonNode value : object.values()) {
			assertTrue(value.isNull());
		}
	}

	@Test
	void add_objectOverload_dispatchesToTypedOverloads() throws Exception {
		final JsonObject object = new JsonObject()
				.add("string", (Object) "text")
				.add("integer", (Object) Integer.valueOf(1))
				.add("long", (Object) Long.valueOf(2L))
				.add("number", (Object) new BigDecimal("1.25"))
				.add("boolean", (Object) Boolean.FALSE)
				.add("date", (Object) new Date(0))
				.add("localDate", (Object) LOCAL_DATE)
				.add("localDateTime", (Object) LOCAL_DATE_TIME)
				.add("zonedDateTime", (Object) ZONED_DATE_TIME)
				.add("node", (Object) new JsonObject());

		assertEquals("text", object.getSimpleValue("string"));
		assertEquals(1, ((Number) object.getSimpleValue("integer")).intValue());
		assertEquals(2L, ((Number) object.getSimpleValue("long")).longValue());
		assertEquals(new BigDecimal("1.25"), object.getSimpleValue("number"));
		assertEquals(Boolean.FALSE, object.getSimpleValue("boolean"));
		assertEquals("2024-02-29", object.getSimpleValue("localDate"));
		assertEquals("2024-02-29T13:14:15", object.getSimpleValue("localDateTime"));
		assertEquals("2024-02-29T13:14:15Z", object.getSimpleValue("zonedDateTime"));
		assertInstanceOf(JsonObject.class, object.getSimpleValue("node"));
	}

	@Test
	void add_unsupportedType_throws() {
		final JsonObject object = new JsonObject();
		final JsonInvalidValueTypeException exception = assertThrows(JsonInvalidValueTypeException.class, () -> object.add("key", new StringBuilder()));
		assertEquals(StringBuilder.class, exception.getUnsupportedClass());
	}

	@Test
	void add_nullKey_throwsForEveryOverload() {
		final JsonObject object = new JsonObject();
		assertThrows(RuntimeException.class, () -> object.addNull(null));
		assertThrows(RuntimeException.class, () -> object.add(null, "x"));
		assertThrows(RuntimeException.class, () -> object.add(null, Integer.valueOf(1)));
		assertThrows(RuntimeException.class, () -> object.add(null, Long.valueOf(1)));
		assertThrows(RuntimeException.class, () -> object.add(null, Double.valueOf(1)));
		assertThrows(RuntimeException.class, () -> object.add(null, Boolean.TRUE));
		assertThrows(RuntimeException.class, () -> object.add(null, new Date()));
		assertThrows(RuntimeException.class, () -> object.add(null, LOCAL_DATE));
		assertThrows(RuntimeException.class, () -> object.add(null, LOCAL_DATE_TIME));
		assertThrows(RuntimeException.class, () -> object.add(null, ZONED_DATE_TIME));
		assertThrows(RuntimeException.class, () -> object.add(null, new JsonObject()));
		assertThrows(RuntimeException.class, () -> object.add(null, (Object) "x"));
	}

	@Test
	void add_duplicateKey_throwsForEveryOverload() throws Exception {
		final JsonObject object = new JsonObject().add("key", "existing");
		assertThrows(DuplicateKeyException.class, () -> object.addNull("key"));
		assertThrows(DuplicateKeyException.class, () -> object.add("key", "x"));
		assertThrows(DuplicateKeyException.class, () -> object.add("key", Integer.valueOf(1)));
		assertThrows(DuplicateKeyException.class, () -> object.add("key", Long.valueOf(1)));
		assertThrows(DuplicateKeyException.class, () -> object.add("key", Double.valueOf(1)));
		assertThrows(DuplicateKeyException.class, () -> object.add("key", Boolean.TRUE));
		assertThrows(DuplicateKeyException.class, () -> object.add("key", new Date()));
		assertThrows(DuplicateKeyException.class, () -> object.add("key", LOCAL_DATE));
		assertThrows(DuplicateKeyException.class, () -> object.add("key", LOCAL_DATE_TIME));
		assertThrows(DuplicateKeyException.class, () -> object.add("key", ZONED_DATE_TIME));
		assertThrows(DuplicateKeyException.class, () -> object.add("key", new JsonObject()));
		final DuplicateKeyException exception = assertThrows(DuplicateKeyException.class, () -> object.add("key", (Object) "x"));
		assertEquals("key", exception.getKey());
	}

	@Test
	void replace_allTypedOverloads_keepPosition() throws Exception {
		final JsonObject object = new JsonObject();
		final String[] keys = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n" };
		for (final String key : keys) {
			object.add(key, "initial");
		}

		object.replaceNull("a")
		.replace("b", "text")
		.replace("c", Integer.valueOf(1))
		.replace("d", Long.valueOf(2))
		.replace("e", (Number) Integer.valueOf(3))
		.replace("f", (Number) Long.valueOf(4))
		.replace("g", Double.valueOf(5.5))
		.replace("h", Boolean.TRUE)
		.replace("i", new Date(0))
		.replace("j", LOCAL_DATE)
		.replace("k", LOCAL_DATE_TIME_NANOS)
		.replace("l", ZONED_DATE_TIME_NANOS)
		.replace("m", new JsonArray())
		.replace("n", LOCAL_DATE_TIME);

		assertEquals(List.of(keys), new ArrayList<>(object.keySet()));
		assertTrue(object.get("a").isNull());
		assertEquals("text", object.getSimpleValue("b"));
		assertInstanceOf(JsonValueInteger.class, object.get("c"));
		assertInstanceOf(JsonValueInteger.class, object.get("d"));
		assertInstanceOf(JsonValueInteger.class, object.get("e"));
		assertInstanceOf(JsonValueInteger.class, object.get("f"));
		assertInstanceOf(JsonValueNumber.class, object.get("g"));
		assertEquals(Boolean.TRUE, object.getSimpleValue("h"));
		assertEquals("2024-02-29", object.getSimpleValue("j"));
		assertTrue(((String) object.getSimpleValue("k")).startsWith("2024-02-29T13:14:15."));
		assertTrue(((String) object.getSimpleValue("l")).startsWith("2024-02-29T13:14:15."));
		assertTrue(object.get("m").isJsonArray());
		assertEquals("2024-02-29T13:14:15", object.getSimpleValue("n"));

		object.replace("l", ZONED_DATE_TIME);
		assertEquals("2024-02-29T13:14:15Z", object.getSimpleValue("l"));
	}

	@Test
	void replace_nullValues_setJsonNull() throws Exception {
		final JsonObject object = new JsonObject();
		for (int i = 0; i < 11; i++) {
			object.add("k" + i, "x");
		}
		object.replace("k0", (String) null)
		.replace("k1", (Integer) null)
		.replace("k2", (Long) null)
		.replace("k3", (Number) null)
		.replace("k4", (Boolean) null)
		.replace("k5", (Date) null)
		.replace("k6", (LocalDate) null)
		.replace("k7", (LocalDateTime) null)
		.replace("k8", (ZonedDateTime) null)
		.replace("k9", (JsonNode) null)
		.replace("k10", (Object) null);
		for (final JsonNode value : object.values()) {
			assertTrue(value.isNull());
		}
	}

	@Test
	void replace_objectOverload_dispatchesToTypedOverloads() throws Exception {
		final JsonObject object = new JsonObject();
		for (int i = 0; i < 10; i++) {
			object.add("k" + i, "x");
		}
		object.replace("k0", (Object) "s")
		.replace("k1", (Object) Integer.valueOf(1))
		.replace("k2", (Object) Long.valueOf(2))
		.replace("k3", (Object) Float.valueOf(1.5f))
		.replace("k4", (Object) Boolean.TRUE)
		.replace("k5", (Object) new Date(0))
		.replace("k6", (Object) LOCAL_DATE)
		.replace("k7", (Object) LOCAL_DATE_TIME)
		.replace("k8", (Object) ZONED_DATE_TIME)
		.replace("k9", (Object) new JsonObject());

		assertEquals("s", object.getSimpleValue("k0"));
		assertInstanceOf(JsonValueNumber.class, object.get("k3"));
		assertTrue(object.get("k9").isJsonObject());
		assertThrows(JsonInvalidValueTypeException.class, () -> object.replace("k0", new Object()));
	}

	@Test
	void replace_missingOrNullKey_throwsForEveryOverload() {
		final JsonObject object = new JsonObject();
		assertThrows(MissingKeyException.class, () -> object.replaceNull("missing"));
		assertThrows(MissingKeyException.class, () -> object.replace("missing", "x"));
		assertThrows(MissingKeyException.class, () -> object.replace("missing", Integer.valueOf(1)));
		assertThrows(MissingKeyException.class, () -> object.replace("missing", Long.valueOf(1)));
		assertThrows(MissingKeyException.class, () -> object.replace("missing", Double.valueOf(1)));
		assertThrows(MissingKeyException.class, () -> object.replace("missing", Boolean.TRUE));
		assertThrows(MissingKeyException.class, () -> object.replace("missing", new Date()));
		assertThrows(MissingKeyException.class, () -> object.replace("missing", LOCAL_DATE));
		assertThrows(MissingKeyException.class, () -> object.replace("missing", LOCAL_DATE_TIME));
		assertThrows(MissingKeyException.class, () -> object.replace("missing", ZONED_DATE_TIME));
		assertThrows(MissingKeyException.class, () -> object.replace("missing", new JsonObject()));
		final MissingKeyException exception = assertThrows(MissingKeyException.class, () -> object.replace("missing", (Object) "x"));
		assertEquals("missing", exception.getKey());

		assertThrows(RuntimeException.class, () -> object.replaceNull(null));
		assertThrows(RuntimeException.class, () -> object.replace(null, "x"));
		assertThrows(RuntimeException.class, () -> object.replace(null, Integer.valueOf(1)));
		assertThrows(RuntimeException.class, () -> object.replace(null, Long.valueOf(1)));
		assertThrows(RuntimeException.class, () -> object.replace(null, Double.valueOf(1)));
		assertThrows(RuntimeException.class, () -> object.replace(null, Boolean.TRUE));
		assertThrows(RuntimeException.class, () -> object.replace(null, new Date()));
		assertThrows(RuntimeException.class, () -> object.replace(null, LOCAL_DATE));
		assertThrows(RuntimeException.class, () -> object.replace(null, LOCAL_DATE_TIME));
		assertThrows(RuntimeException.class, () -> object.replace(null, ZONED_DATE_TIME));
		assertThrows(RuntimeException.class, () -> object.replace(null, new JsonObject()));
		assertThrows(RuntimeException.class, () -> object.replace(null, (Object) "x"));
	}

	@Test
	void put_allTypedOverloads_addOrReplace() throws Exception {
		final JsonObject object = new JsonObject()
				.putNull("null")
				.put("string", "text")
				.put("integer", Integer.valueOf(1))
				.put("long", Long.valueOf(2))
				.put("numberInteger", (Number) Integer.valueOf(3))
				.put("numberLong", (Number) Long.valueOf(4))
				.put("numberDouble", Double.valueOf(5.5))
				.put("boolean", Boolean.TRUE)
				.put("date", new Date(0))
				.put("localDate", LOCAL_DATE)
				.put("localDateTime", LOCAL_DATE_TIME)
				.put("localDateTimeNanos", LOCAL_DATE_TIME_NANOS)
				.put("zonedDateTime", ZONED_DATE_TIME)
				.put("zonedDateTimeNanos", ZONED_DATE_TIME_NANOS)
				.put("node", new JsonObject());
		assertEquals(15, object.size());
		assertInstanceOf(JsonValueInteger.class, object.get("numberInteger"));
		assertInstanceOf(JsonValueInteger.class, object.get("numberLong"));
		assertInstanceOf(JsonValueNumber.class, object.get("numberDouble"));
		assertEquals("2024-02-29T13:14:15Z", object.getSimpleValue("zonedDateTime"));

		// Overwrite existing keys with null values
		object.put("string", (String) null)
		.put("integer", (Integer) null)
		.put("long", (Long) null)
		.put("numberInteger", (Number) null)
		.put("boolean", (Boolean) null)
		.put("date", (Date) null)
		.put("localDate", (LocalDate) null)
		.put("localDateTime", (LocalDateTime) null)
		.put("zonedDateTime", (ZonedDateTime) null)
		.put("node", (JsonNode) null)
		.put("numberLong", (Object) null);
		assertEquals(15, object.size());
		assertNull(object.getSimpleValue("string"));
		assertNull(object.getSimpleValue("node"));
		assertNull(object.getSimpleValue("numberLong"));
	}

	@Test
	void put_objectOverload_dispatchesToTypedOverloads() throws Exception {
		final JsonObject object = new JsonObject()
				.put("s", (Object) "s")
				.put("i", (Object) Integer.valueOf(1))
				.put("l", (Object) Long.valueOf(2))
				.put("n", (Object) Double.valueOf(2.5))
				.put("b", (Object) Boolean.TRUE)
				.put("d", (Object) new Date(0))
				.put("ld", (Object) LOCAL_DATE)
				.put("ldt", (Object) LOCAL_DATE_TIME)
				.put("zdt", (Object) ZONED_DATE_TIME)
				.put("node", (Object) new JsonArray());
		assertEquals(10, object.size());
		assertEquals(2.5, ((Number) object.getSimpleValue("n")).doubleValue());
		assertThrows(JsonInvalidValueTypeException.class, () -> object.put("x", new Object()));
	}

	@Test
	void put_nullKey_throwsForEveryOverload() {
		final JsonObject object = new JsonObject();
		assertThrows(RuntimeException.class, () -> object.putNull(null));
		assertThrows(RuntimeException.class, () -> object.put(null, "x"));
		assertThrows(RuntimeException.class, () -> object.put(null, Integer.valueOf(1)));
		assertThrows(RuntimeException.class, () -> object.put(null, Long.valueOf(1)));
		assertThrows(RuntimeException.class, () -> object.put(null, Double.valueOf(1)));
		assertThrows(RuntimeException.class, () -> object.put(null, Boolean.TRUE));
		assertThrows(RuntimeException.class, () -> object.put(null, new Date()));
		assertThrows(RuntimeException.class, () -> object.put(null, LOCAL_DATE));
		assertThrows(RuntimeException.class, () -> object.put(null, LOCAL_DATE_TIME));
		assertThrows(RuntimeException.class, () -> object.put(null, ZONED_DATE_TIME));
		assertThrows(RuntimeException.class, () -> object.put(null, new JsonObject()));
		assertThrows(RuntimeException.class, () -> object.put(null, (Object) "x"));
	}

	@Test
	void accessors_returnSimpleValuesAndViews() throws Exception {
		final JsonObject nested = new JsonObject();
		final JsonObject object = new JsonObject()
				.add("s", "text")
				.add("i", Integer.valueOf(1))
				.add("n", Double.valueOf(1.5))
				.add("b", Boolean.TRUE)
				.addNull("null")
				.add("o", nested);

		assertNull(object.getSimpleValue("missing"));
		assertNull(object.getSimpleValue("null"));
		assertEquals(1.5, ((Number) object.getSimpleValue("n")).doubleValue());
		assertSame(nested, object.getSimpleValue("o"));

		final List<Object> simpleValues = new ArrayList<>(object.simpleValues());
		assertEquals("text", simpleValues.get(0));
		assertEquals(1, ((Number) simpleValues.get(1)).intValue());
		assertEquals(1.5, ((Number) simpleValues.get(2)).doubleValue());
		assertEquals(Boolean.TRUE, simpleValues.get(3));
		assertNull(simpleValues.get(4));
		assertSame(nested, simpleValues.get(5));

		final Iterator<Map.Entry<String, Object>> iterator = object.iterator();
		assertEquals("s", iterator.next().getKey());
		int count = 1;
		while (iterator.hasNext()) {
			iterator.next();
			count++;
		}
		assertEquals(6, count);

		assertEquals(6, object.entrySet().size());
		assertThrows(UnsupportedOperationException.class, () -> object.keySet().clear());
		assertThrows(UnsupportedOperationException.class, () -> object.values().clear());
		assertThrows(UnsupportedOperationException.class, () -> object.entrySet().clear());

		assertTrue(object.containsKey("null"));
		assertFalse(object.containsKey("missing"));
		assertTrue(object.remove("s").isString());
		assertNull(object.remove("s"));
		assertFalse(object.isEmpty());
		assertTrue(new JsonObject().isEmpty());
	}

	@Test
	void equalsAndHashCode() throws Exception {
		final JsonObject a = new JsonObject().add("x", "1").add("y", Integer.valueOf(2));
		final JsonObject b = new JsonObject().add("y", Integer.valueOf(2)).add("x", "1");
		final JsonObject c = new JsonObject().add("x", "1").add("y", Integer.valueOf(3));
		final JsonObject d = new JsonObject().add("x", "1");

		assertEquals(a, a);
		assertEquals(a, b);
		assertNotEquals(a, c);
		assertNotEquals(a, d);
		assertNotEquals(a, null);
		assertNotEquals(a, "string");
		assertEquals(a.hashCode(), new JsonObject().add("x", "1").add("y", Integer.valueOf(2)).hashCode());
	}

	@Test
	void merge_strategies() throws Exception {
		final JsonObject base = new JsonObject().add("a", "base").add("b", "base");
		final JsonObject other = new JsonObject().add("b", "other").add("c", "other");

		assertSame(base, base.merge(null, JsonObjectMergeStrategy.OVERWRITE));

		final JsonObject keep = new JsonObject().add("a", "base").add("b", "base").merge(other, JsonObjectMergeStrategy.KEEP_EXISTING);
		assertEquals("base", keep.getSimpleValue("b"));
		assertEquals("other", keep.getSimpleValue("c"));

		base.merge(other, JsonObjectMergeStrategy.OVERWRITE);
		assertEquals("other", base.getSimpleValue("b"));
		assertEquals(3, base.size());
	}

	@Test
	void toString_writesJson() throws Exception {
		final String json = new JsonObject().add("key", "value").toString();
		assertTrue(json.contains("\"key\""));
		assertTrue(json.contains("\"value\""));
	}
}
