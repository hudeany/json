package de.soderer.json;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class JsonArrayTest {
	@Test
	void sort_ascending_stringItems() {
		final JsonArray array = new JsonArray();
		array.add("banana").add("apple").add("cherry");

		array.sort(true);

		final List<Object> items = new ArrayList<>(array.simpleItems());
		assertEquals("apple",  items.get(0));
		assertEquals("banana", items.get(1));
		assertEquals("cherry", items.get(2));
	}

	@Test
	void sort_descending_stringItems() {
		final JsonArray array = new JsonArray();
		array.add("banana").add("apple").add("cherry");

		array.sort(false);

		final List<Object> items = new ArrayList<>(array.simpleItems());
		assertEquals("cherry", items.get(0));
		assertEquals("banana", items.get(1));
		assertEquals("apple",  items.get(2));
	}

	@Test
	void sort_ascending_integerItems() {
		final JsonArray array = new JsonArray();
		array.add(30).add(10).add(20);

		array.sort(true);

		final List<Object> items = new ArrayList<>(array.simpleItems());
		assertEquals(10, items.get(0));
		assertEquals(20, items.get(1));
		assertEquals(30, items.get(2));
	}

	@Test
	void sort_descending_integerItems() {
		final JsonArray array = new JsonArray();
		array.add(30).add(10).add(20);

		array.sort(false);

		final List<Object> items = new ArrayList<>(array.simpleItems());
		assertEquals(30, items.get(0));
		assertEquals(20, items.get(1));
		assertEquals(10, items.get(2));
	}

	@Test
	void sort_ascending_booleanItems() {
		final JsonArray array = new JsonArray();
		array.add(Boolean.TRUE).add(Boolean.FALSE);

		array.sort(true);

		final List<Object> items = new ArrayList<>(array.simpleItems());
		// Boolean.compare(false, true) < 0 → false comes first ascending
		assertEquals(Boolean.FALSE, items.get(0));
		assertEquals(Boolean.TRUE,  items.get(1));
	}

	@Test
	void sort_ascending_nullComesFirst() {
		final JsonArray array = new JsonArray();
		array.add("b").addNull().add("a");

		array.sort(true);

		final List<Object> items = new ArrayList<>(array.simpleItems());
		assertNull(items.get(0));
	}

	@Test
	void sort_descending_nullComesLast() {
		final JsonArray array = new JsonArray();
		array.add("b").addNull().add("a");

		array.sort(false);

		final List<Object> items = new ArrayList<>(array.simpleItems());
		assertNull(items.get(items.size() - 1));
	}

	@Test
	void sort_emptyArray_noException() {
		final JsonArray array = new JsonArray();
		assertDoesNotThrow(() -> array.sort(true));
		assertEquals(0, array.size());
	}

	@Test
	void sort_singleItem_unchanged() {
		final JsonArray array = new JsonArray();
		array.add("only");

		array.sort(true);

		assertEquals(1, array.size());
		assertEquals("only", array.simpleItems().iterator().next());
	}

	@Test
	void sort_returnsThisForChaining() {
		final JsonArray array = new JsonArray();
		array.add("a");

		final JsonArray result = array.sort(true);

		assertSame(array, result);
	}

	@Test
	void sortByAttribute_ascending_stringAttribute() throws Exception {
		final JsonArray array = new JsonArray();

		final JsonObject o1 = new JsonObject();
		o1.add("name", "Charlie");
		final JsonObject o2 = new JsonObject();
		o2.add("name", "Alice");
		final JsonObject o3 = new JsonObject();
		o3.add("name", "Bob");

		array.add(o1).add(o2).add(o3);
		array.sortByAttribute("name", true);

		assertEquals("Alice",   ((JsonObject) array.get(0)).getSimpleValue("name"));
		assertEquals("Bob",     ((JsonObject) array.get(1)).getSimpleValue("name"));
		assertEquals("Charlie", ((JsonObject) array.get(2)).getSimpleValue("name"));
	}

	@Test
	void sortByAttribute_descending_integerAttribute() throws Exception {
		final JsonArray array = new JsonArray();

		final JsonObject o1 = new JsonObject();
		o1.add("score", 80);
		final JsonObject o2 = new JsonObject();
		o2.add("score", 95);
		final JsonObject o3 = new JsonObject();
		o3.add("score", 60);

		array.add(o1).add(o2).add(o3);
		array.sortByAttribute("score", false);

		assertEquals(95, ((JsonObject) array.get(0)).getSimpleValue("score"));
		assertEquals(80, ((JsonObject) array.get(1)).getSimpleValue("score"));
		assertEquals(60, ((JsonObject) array.get(2)).getSimpleValue("score"));
	}

	@Test
	void sortByAttribute_missingAttribute_treatedAsNull_comesFirstAscending() throws Exception {
		final JsonArray array = new JsonArray();

		final JsonObject withAttr = new JsonObject();
		withAttr.add("name", "Alice");
		final JsonObject withoutAttr = new JsonObject();
		// no "name" key → treated as null

		array.add(withoutAttr).add(withAttr);
		array.sortByAttribute("name", true);

		// null (missing) should come first when ascending
		assertFalse(((JsonObject) array.get(0)).containsKey("name"));
		assertEquals("Alice", ((JsonObject) array.get(1)).getSimpleValue("name"));
	}

	@Test
	void sortByAttribute_nullAttributeValue_comesFirstAscending() throws Exception {
		final JsonArray array = new JsonArray();

		final JsonObject withNull = new JsonObject();
		withNull.addNull("name");
		final JsonObject withValue = new JsonObject();
		withValue.add("name", "Bob");

		array.add(withValue).add(withNull);
		array.sortByAttribute("name", true);

		// explicit null value should come first when ascending
		assertNull(((JsonObject) array.get(0)).getSimpleValue("name"));
		assertEquals("Bob", ((JsonObject) array.get(1)).getSimpleValue("name"));
	}

	@Test
	void sortByAttribute_nonObjectItems_stayInPlace() {
		final JsonArray array = new JsonArray();
		array.add("plain-string");

		// non-JsonObject items return 0 in the comparator → no exception
		assertDoesNotThrow(() -> array.sortByAttribute("name", true));
		assertEquals(1, array.size());
	}

	@Test
	void merge_withNull_returnsUnchanged() {
		final JsonArray array = new JsonArray();
		array.add("a");

		array.merge(null, JsonArrayMergeStrategy.APPEND_ALL);

		assertEquals(1, array.size());
	}

	@Test
	void merge_appendAll_addsDuplicates() {
		final JsonArray base = new JsonArray();
		base.add("a").add("b");

		final JsonArray other = new JsonArray();
		other.add("b").add("c");

		base.merge(other, JsonArrayMergeStrategy.APPEND_ALL);

		// "b" appears twice
		assertEquals(4, base.size());
		final List<Object> items = new ArrayList<>(base.simpleItems());
		assertEquals("a", items.get(0));
		assertEquals("b", items.get(1));
		assertEquals("b", items.get(2));
		assertEquals("c", items.get(3));
	}

	@Test
	void merge_skipDuplicates_doesNotAddExistingItems() {
		final JsonArray base = new JsonArray();
		base.add("a").add("b");

		final JsonArray other = new JsonArray();
		other.add("b").add("c");

		base.merge(other, JsonArrayMergeStrategy.SKIP_DUPLICATES);

		assertEquals(3, base.size());
		final List<Object> items = new ArrayList<>(base.simpleItems());
		assertEquals("a", items.get(0));
		assertEquals("b", items.get(1));
		assertEquals("c", items.get(2));
	}

	@Test
	void merge_appendAll_emptyOther_baseUnchanged() {
		final JsonArray base = new JsonArray();
		base.add("a");

		base.merge(new JsonArray(), JsonArrayMergeStrategy.APPEND_ALL);

		assertEquals(1, base.size());
	}

	@Test
	void merge_appendAll_intoEmptyBase_copiesAll() {
		final JsonArray base = new JsonArray();

		final JsonArray other = new JsonArray();
		other.add("x").add("y");

		base.merge(other, JsonArrayMergeStrategy.APPEND_ALL);

		assertEquals(2, base.size());
		final List<Object> items = new ArrayList<>(base.simpleItems());
		assertEquals("x", items.get(0));
		assertEquals("y", items.get(1));
	}

	@Test
	void merge_skipDuplicates_allItemsAlreadyPresent_noneAdded() {
		final JsonArray base = new JsonArray();
		base.add("a").add("b");

		final JsonArray other = new JsonArray();
		other.add("a").add("b");

		base.merge(other, JsonArrayMergeStrategy.SKIP_DUPLICATES);

		assertEquals(2, base.size());
	}

	@Test
	void merge_appendAll_nullItemsAreAppended() {
		final JsonArray base = new JsonArray();
		base.add("a");

		final JsonArray other = new JsonArray();
		other.addNull();

		base.merge(other, JsonArrayMergeStrategy.APPEND_ALL);

		assertEquals(2, base.size());
		final List<Object> items = new ArrayList<>(base.simpleItems());
		assertNull(items.get(1));
	}

	@Test
	void merge_returnsThisForChaining() {
		final JsonArray base = new JsonArray();
		final JsonArray other = new JsonArray();
		other.add("z");

		final JsonArray result = base.merge(other, JsonArrayMergeStrategy.APPEND_ALL);

		assertSame(base, result);
	}
}