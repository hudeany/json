package de.soderer.json;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class JsonObjectTest {
	@Test
	void sortKeys_ascending_sortsAlphabetically() throws Exception {
		final JsonObject obj = new JsonObject();
		obj.add("banana", "b");
		obj.add("apple", "a");
		obj.add("cherry", "c");

		obj.sortKeys(true);

		final List<String> keys = new ArrayList<>(obj.keySet());
		assertEquals("apple",  keys.get(0));
		assertEquals("banana", keys.get(1));
		assertEquals("cherry", keys.get(2));
	}

	@Test
	void sortKeys_descending_sortsReverseAlphabetically() throws Exception {
		final JsonObject obj = new JsonObject();
		obj.add("banana", "b");
		obj.add("apple", "a");
		obj.add("cherry", "c");

		obj.sortKeys(false);

		final List<String> keys = new ArrayList<>(obj.keySet());
		assertEquals("cherry", keys.get(0));
		assertEquals("banana", keys.get(1));
		assertEquals("apple",  keys.get(2));
	}

	@Test
	void sortKeys_ascending_preservesValues() throws Exception {
		final JsonObject obj = new JsonObject();
		obj.add("z", "last");
		obj.add("a", "first");

		obj.sortKeys(true);

		assertEquals("first", obj.getSimpleValue("a"));
		assertEquals("last",  obj.getSimpleValue("z"));
	}

	@Test
	void sortKeys_emptyObject_noException() {
		final JsonObject obj = new JsonObject();
		assertDoesNotThrow(() -> obj.sortKeys(true));
		assertEquals(0, obj.size());
	}

	@Test
	void sortKeys_singleKey_unchanged() throws Exception {
		final JsonObject obj = new JsonObject();
		obj.add("only", "value");

		obj.sortKeys(true);

		assertEquals(1, obj.size());
		assertEquals("value", obj.getSimpleValue("only"));
	}

	@Test
	void sortKeys_returnsThisForChaining() throws Exception {
		final JsonObject obj = new JsonObject();
		obj.add("a", "1");

		final JsonObject result = obj.sortKeys(true);

		assertSame(obj, result);
	}

	@Test
	void sortKeys_ascending_caseInsensitiveNaturalOrder() throws Exception {
		// String.compareTo is case-sensitive: uppercase letters come before lowercase in ASCII
		final JsonObject obj = new JsonObject();
		obj.add("b", "B");
		obj.add("A", "a-upper");
		obj.add("c", "C");

		obj.sortKeys(true);

		final List<String> keys = new ArrayList<>(obj.keySet());
		// 'A' (65) < 'b' (98) < 'c' (99)
		assertEquals("A", keys.get(0));
		assertEquals("b", keys.get(1));
		assertEquals("c", keys.get(2));
	}

	@Test
	void sortKeys_twoKeys_descending() throws Exception {
		final JsonObject obj = new JsonObject();
		obj.add("alpha", 1);
		obj.add("zeta", 2);

		obj.sortKeys(false);

		final List<String> keys = new ArrayList<>(obj.keySet());
		assertEquals("zeta",  keys.get(0));
		assertEquals("alpha", keys.get(1));
	}

	@Test
	void merge_withNull_returnsUnchanged() throws Exception {
		final JsonObject obj = new JsonObject();
		obj.add("key", "value");

		obj.merge(null, JsonObjectMergeStrategy.OVERWRITE);

		assertEquals(1, obj.size());
	}

	@Test
	void merge_overwrite_replacesExistingKey() throws Exception {
		final JsonObject base = new JsonObject();
		base.add("key", "original");

		final JsonObject other = new JsonObject();
		other.add("key", "updated");

		base.merge(other, JsonObjectMergeStrategy.OVERWRITE);

		assertEquals("updated", base.getSimpleValue("key"));
	}

	@Test
	void merge_keepExisting_doesNotReplaceExistingKey() throws Exception {
		final JsonObject base = new JsonObject();
		base.add("key", "original");

		final JsonObject other = new JsonObject();
		other.add("key", "updated");

		base.merge(other, JsonObjectMergeStrategy.KEEP_EXISTING);

		assertEquals("original", base.getSimpleValue("key"));
	}

	@Test
	void merge_addsNewKeys_withOverwriteStrategy() throws Exception {
		final JsonObject base = new JsonObject();
		base.add("existing", "e");

		final JsonObject other = new JsonObject();
		other.add("newKey", "n");

		base.merge(other, JsonObjectMergeStrategy.OVERWRITE);

		assertTrue(base.containsKey("newKey"));
		assertEquals("n", base.getSimpleValue("newKey"));
	}

	@Test
	void merge_addsNewKeys_withKeepExistingStrategy() throws Exception {
		final JsonObject base = new JsonObject();
		base.add("existing", "e");

		final JsonObject other = new JsonObject();
		other.add("newKey", "n");

		base.merge(other, JsonObjectMergeStrategy.KEEP_EXISTING);

		assertTrue(base.containsKey("newKey"));
		assertEquals("n", base.getSimpleValue("newKey"));
	}

	@Test
	void merge_overwrite_mixedKeysAddedAndOverwritten() throws Exception {
		final JsonObject base = new JsonObject();
		base.add("a", "original-a");
		base.add("b", "original-b");

		final JsonObject other = new JsonObject();
		other.add("b", "updated-b");
		other.add("c", "new-c");

		base.merge(other, JsonObjectMergeStrategy.OVERWRITE);

		assertEquals("original-a", base.getSimpleValue("a"));
		assertEquals("updated-b",  base.getSimpleValue("b"));
		assertEquals("new-c",      base.getSimpleValue("c"));
		assertEquals(3, base.size());
	}

	@Test
	void merge_keepExisting_mixedKeysAddedAndKept() throws Exception {
		final JsonObject base = new JsonObject();
		base.add("a", "original-a");
		base.add("b", "original-b");

		final JsonObject other = new JsonObject();
		other.add("b", "updated-b");
		other.add("c", "new-c");

		base.merge(other, JsonObjectMergeStrategy.KEEP_EXISTING);

		assertEquals("original-a", base.getSimpleValue("a"));
		assertEquals("original-b", base.getSimpleValue("b"));
		assertEquals("new-c",      base.getSimpleValue("c"));
		assertEquals(3, base.size());
	}

	@Test
	void merge_emptyOther_baseUnchanged() throws Exception {
		final JsonObject base = new JsonObject();
		base.add("key", "value");

		base.merge(new JsonObject(), JsonObjectMergeStrategy.OVERWRITE);

		assertEquals(1, base.size());
		assertEquals("value", base.getSimpleValue("key"));
	}

	@Test
	void merge_intoEmptyBase_copiesAllKeys() throws Exception {
		final JsonObject base = new JsonObject();

		final JsonObject other = new JsonObject();
		other.add("x", "1");
		other.add("y", "2");

		base.merge(other, JsonObjectMergeStrategy.OVERWRITE);

		assertEquals(2, base.size());
		assertEquals("1", base.getSimpleValue("x"));
		assertEquals("2", base.getSimpleValue("y"));
	}

	@Test
	void merge_returnsThisForChaining() throws Exception {
		final JsonObject base = new JsonObject();
		final JsonObject other = new JsonObject();
		other.add("k", "v");

		final JsonObject result = base.merge(other, JsonObjectMergeStrategy.OVERWRITE);

		assertSame(base, result);
	}

	@Test
	void merge_overwrite_nullValueFromOther() throws Exception {
		final JsonObject base = new JsonObject();
		base.add("key", "original");

		final JsonObject other = new JsonObject();
		other.addNull("key");

		base.merge(other, JsonObjectMergeStrategy.OVERWRITE);

		// null value replaces the existing string value
		assertNull(base.getSimpleValue("key"));
	}
}