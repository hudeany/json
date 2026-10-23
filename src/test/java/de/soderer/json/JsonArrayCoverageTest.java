package de.soderer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Covers the typed add/insert/remove/contains overloads, sorting and merging of {@link JsonArray}.
 */
@SuppressWarnings("static-method")
class JsonArrayCoverageTest {
	private static final LocalDate LOCAL_DATE = LocalDate.of(2024, 1, 2);
	private static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
	private static final LocalDateTime LOCAL_DATE_TIME_NANOS = LocalDateTime.of(2024, 1, 2, 3, 4, 5, 123_000_000);
	private static final ZonedDateTime ZONED_DATE_TIME = ZonedDateTime.of(LOCAL_DATE_TIME, ZoneOffset.UTC);
	private static final ZonedDateTime ZONED_DATE_TIME_NANOS = ZonedDateTime.of(LOCAL_DATE_TIME_NANOS, ZoneOffset.UTC);

	@Test
	void add_allTypedOverloads() {
		final JsonArray array = new JsonArray()
				.addNull()
				.add("text")
				.add(Integer.valueOf(1))
				.add(Long.valueOf(2))
				.add((Number) Integer.valueOf(3))
				.add((Number) Long.valueOf(4))
				.add(Double.valueOf(5.5))
				.add(Boolean.TRUE)
				.add(new Date(0))
				.add(LOCAL_DATE)
				.add(LOCAL_DATE_TIME)
				.add(LOCAL_DATE_TIME_NANOS)
				.add(ZONED_DATE_TIME)
				.add(ZONED_DATE_TIME_NANOS)
				.add(new JsonObject());

		assertEquals(15, array.size());
		assertNull(array.getSimpleItem(0));
		assertEquals("text", array.getSimpleItem(1));
		assertInstanceOf(JsonValueInteger.class, array.get(4));
		assertInstanceOf(JsonValueInteger.class, array.get(5));
		assertInstanceOf(JsonValueNumber.class, array.get(6));
		assertEquals(5.5, ((Number) array.getSimpleItem(6)).doubleValue());
		assertEquals(Boolean.TRUE, array.getSimpleItem(7));
		assertEquals("2024-01-02", array.getSimpleItem(9));
		assertEquals("2024-01-02T03:04:05", array.getSimpleItem(10));
		assertTrue(((String) array.getSimpleItem(11)).startsWith("2024-01-02T03:04:05."));
		assertEquals("2024-01-02T03:04:05Z", array.getSimpleItem(12));
		assertTrue(((String) array.getSimpleItem(13)).startsWith("2024-01-02T03:04:05."));
		assertInstanceOf(JsonObject.class, array.getSimpleItem(14));
	}

	@Test
	void add_nullValues_addJsonNull() {
		final JsonArray array = new JsonArray()
				.add((String) null)
				.add((Integer) null)
				.add((Long) null)
				.add((Number) null)
				.add((Boolean) null)
				.add((Date) null)
				.add((LocalDate) null)
				.add((LocalDateTime) null)
				.add((ZonedDateTime) null)
				.add((JsonNode) null);
		assertEquals(10, array.size());
		for (final JsonNode item : array.items()) {
			assertTrue(item.isNull());
		}
	}

	@Test
	void insert_allTypedOverloads() {
		final JsonArray array = new JsonArray().add("end");
		array.insertNull(0)
		.insert(0, "text")
		.insert(0, Integer.valueOf(1))
		.insert(0, Long.valueOf(2))
		.insert(0, (Number) Integer.valueOf(3))
		.insert(0, (Number) Long.valueOf(4))
		.insert(0, Double.valueOf(5.5))
		.insert(0, Boolean.FALSE)
		.insert(0, new Date(0))
		.insert(0, LOCAL_DATE)
		.insert(0, LOCAL_DATE_TIME)
		.insert(0, LOCAL_DATE_TIME_NANOS)
		.insert(0, ZONED_DATE_TIME)
		.insert(0, ZONED_DATE_TIME_NANOS)
		.insert(0, new JsonArray());

		assertEquals(16, array.size());
		assertTrue(array.get(0).isJsonArray());
		assertEquals("2024-01-02T03:04:05Z", array.getSimpleItem(2));
		assertEquals("2024-01-02T03:04:05", array.getSimpleItem(4));
		assertEquals("2024-01-02", array.getSimpleItem(5));
		assertEquals(Boolean.FALSE, array.getSimpleItem(7));
		assertInstanceOf(JsonValueNumber.class, array.get(8));
		assertInstanceOf(JsonValueInteger.class, array.get(9));
		assertInstanceOf(JsonValueInteger.class, array.get(10));
		assertEquals("text", array.getSimpleItem(13));
		assertNull(array.getSimpleItem(14));
		assertEquals("end", array.getSimpleItem(15));

		array.insert(0, (String) null)
		.insert(0, (Integer) null)
		.insert(0, (Long) null)
		.insert(0, (Number) null)
		.insert(0, (Boolean) null)
		.insert(0, (Date) null)
		.insert(0, (LocalDate) null)
		.insert(0, (LocalDateTime) null)
		.insert(0, (ZonedDateTime) null)
		.insert(0, (JsonNode) null);
		for (int i = 0; i < 10; i++) {
			assertTrue(array.get(i).isNull());
		}
		assertThrows(IndexOutOfBoundsException.class, () -> array.insert(1000, "x"));
	}

	@Test
	void removeAndContains_allTypedOverloads() {
		final JsonArray array = new JsonArray()
				.addNull()
				.add("text")
				.add(Integer.valueOf(1))
				.add(Long.valueOf(2))
				.add(Double.valueOf(5.5))
				.add(Boolean.TRUE)
				.add(new JsonObject());

		assertTrue(array.containsNull());
		assertTrue(array.contains((String) null));
		assertTrue(array.contains((Integer) null));
		assertTrue(array.contains((Long) null));
		assertTrue(array.contains((Number) null));
		assertTrue(array.contains((Boolean) null));
		assertTrue(array.contains((JsonNode) null));
		assertTrue(array.contains("text"));
		assertFalse(array.contains("other"));
		assertTrue(array.contains(Integer.valueOf(1)));
		assertTrue(array.contains(Long.valueOf(2)));
		assertTrue(array.contains((Number) Integer.valueOf(1)));
		assertTrue(array.contains((Number) Long.valueOf(2)));
		assertTrue(array.contains(Double.valueOf(5.5)));
		assertTrue(array.contains(Boolean.TRUE));
		assertFalse(array.contains(Boolean.FALSE));
		assertTrue(array.contains(new JsonObject()));

		assertTrue(array.remove((String) null));
		assertFalse(array.containsNull());
		assertFalse(array.remove((Integer) null));
		assertFalse(array.remove((Long) null));
		assertFalse(array.remove((Number) null));
		assertFalse(array.remove((Boolean) null));
		assertFalse(array.remove((JsonNode) null));
		assertTrue(array.remove("text"));
		assertTrue(array.remove(Integer.valueOf(1)));
		assertTrue(array.remove(Long.valueOf(2)));
		assertTrue(array.remove(Double.valueOf(5.5)));
		assertFalse(array.remove((Number) Integer.valueOf(1)));
		assertFalse(array.remove((Number) Long.valueOf(2)));
		assertTrue(array.remove(Boolean.TRUE));
		assertTrue(array.remove(new JsonObject()));
		assertTrue(array.isEmpty());

		array.add("a").add("b");
		assertEquals("a", ((JsonValueString) array.removeByIndex(0)).getValue());
		assertEquals(1, array.size());
		array.addNull();
		assertTrue(array.removeNull());
	}

	@Test
	void simpleItemsAndIterator() {
		final JsonObject nested = new JsonObject();
		final JsonArray array = new JsonArray().addNull().add("s").add(Integer.valueOf(1)).add(Double.valueOf(1.5)).add(Boolean.TRUE).add(nested);
		final List<Object> simple = new ArrayList<>(array.simpleItems());
		assertNull(simple.get(0));
		assertEquals("s", simple.get(1));
		assertEquals(1, ((Number) simple.get(2)).intValue());
		assertEquals(1.5, ((Number) simple.get(3)).doubleValue());
		assertEquals(Boolean.TRUE, simple.get(4));
		assertSame(nested, simple.get(5));

		final List<Object> iterated = new ArrayList<>();
		for (final Object item : array) {
			iterated.add(item);
		}
		assertEquals(simple, iterated);
		assertSame(nested, array.getSimpleItem(5));
		assertThrows(UnsupportedOperationException.class, () -> array.items().clear());
		assertTrue(array.toString().contains("\"s\""));
	}

	@Test
	void equalsAndHashCode() {
		final JsonArray a = new JsonArray().add("x").add(Integer.valueOf(1));
		final JsonArray b = new JsonArray().add("x").add(Integer.valueOf(1));
		assertEquals(a, a);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, new JsonArray().add(Integer.valueOf(1)).add("x"));
		assertNotEquals(a, new JsonArray().add("x"));
		assertNotEquals(a, null);
		assertNotEquals(a, "x");
	}

	@Test
	void sort_mixedTypes_nullsFirstThenBooleansNumbersStrings() {
		final JsonArray array = new JsonArray()
				.add("b")
				.add(Integer.valueOf(3))
				.addNull()
				.add(Boolean.TRUE)
				.add("a")
				.add(Double.valueOf(1.5))
				.add(Boolean.FALSE)
				.addNull()
				.add(new JsonObject());

		array.sort(true);
		// JSON null values and objects (no simple value) are ranked first
		for (int i = 0; i < 3; i++) {
			assertTrue(array.get(i).isNull() || array.get(i).isJsonObject());
		}
		assertEquals("b", array.getSimpleItem(8));

		final JsonArray simple = new JsonArray().add("b").add(Integer.valueOf(3)).addNull().add(Boolean.TRUE).add("a").add(Double.valueOf(1.5)).add(Boolean.FALSE);
		simple.sort(true);
		assertNull(simple.getSimpleItem(0));
		assertEquals(Boolean.FALSE, simple.getSimpleItem(1));
		assertEquals(Boolean.TRUE, simple.getSimpleItem(2));
		assertEquals(1.5, ((Number) simple.getSimpleItem(3)).doubleValue());
		assertEquals(3, ((Number) simple.getSimpleItem(4)).intValue());
		assertEquals("a", simple.getSimpleItem(5));
		assertEquals("b", simple.getSimpleItem(6));

		simple.sort(false);
		assertEquals("b", simple.getSimpleItem(0));
		assertNull(simple.getSimpleItem(6));
	}

	@Test
	void sortByAttribute_missingAndNonObjectItemsSortLikeNull() throws Exception {
		final JsonArray array = new JsonArray()
				.add(new JsonObject().add("name", "c"))
				.add(new JsonObject().add("name", "a"))
				.add("not an object")
				.add(new JsonObject().add("other", "x"))
				.add(new JsonObject().add("name", "b"));

		array.sortByAttribute("name", true);
		assertEquals("a", ((JsonObject) array.get(2)).getSimpleValue("name"));
		assertEquals("b", ((JsonObject) array.get(3)).getSimpleValue("name"));
		assertEquals("c", ((JsonObject) array.get(4)).getSimpleValue("name"));

		array.sortByAttribute("name", false);
		assertEquals("c", ((JsonObject) array.get(0)).getSimpleValue("name"));

		final JsonArray numbers = new JsonArray()
				.add(new JsonObject().add("v", Integer.valueOf(2)))
				.add(new JsonObject().add("v", Boolean.TRUE))
				.add(new JsonObject().add("v", Double.valueOf(0.5)));
		numbers.sortByAttribute("v", true);
		assertEquals(Boolean.TRUE, ((JsonObject) numbers.get(0)).getSimpleValue("v"));
		assertEquals(2, ((Number) ((JsonObject) numbers.get(2)).getSimpleValue("v")).intValue());
	}

	@Test
	void merge_strategies() {
		final JsonArray other = new JsonArray().add("a").add("c");

		final JsonArray base = new JsonArray().add("a").add("b");
		assertSame(base, base.merge(null, JsonArrayMergeStrategy.APPEND_ALL));
		base.merge(other, JsonArrayMergeStrategy.APPEND_ALL);
		assertEquals(4, base.size());

		final JsonArray skip = new JsonArray().add("a").add("b").merge(other, JsonArrayMergeStrategy.SKIP_DUPLICATES);
		assertEquals(3, skip.size());
		assertEquals("c", skip.getSimpleItem(2));
	}
}
