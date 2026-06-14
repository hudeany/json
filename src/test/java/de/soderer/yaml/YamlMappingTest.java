package de.soderer.yaml;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlMappingMergeStrategy;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;

@SuppressWarnings("static-method")
class YamlMappingTest {
	@Test
	void sortKeys_ascending_stringKeys() throws Exception {
		final YamlMapping mapping = new YamlMapping();
		mapping.add("banana", "b");
		mapping.add("apple", "a");
		mapping.add("cherry", "c");

		mapping.sortKeys(true);

		final List<YamlNode> keys = List.copyOf(mapping.keySet());
		assertEquals("apple", ((YamlScalar) keys.get(0)).getValueString());
		assertEquals("banana", ((YamlScalar) keys.get(1)).getValueString());
		assertEquals("cherry", ((YamlScalar) keys.get(2)).getValueString());
	}

	@Test
	void sortKeys_descending_stringKeys() throws Exception {
		final YamlMapping mapping = new YamlMapping();
		mapping.add("banana", "b");
		mapping.add("apple", "a");
		mapping.add("cherry", "c");

		mapping.sortKeys(false);

		final List<YamlNode> keys = List.copyOf(mapping.keySet());
		assertEquals("cherry", ((YamlScalar) keys.get(0)).getValueString());
		assertEquals("banana", ((YamlScalar) keys.get(1)).getValueString());
		assertEquals("apple", ((YamlScalar) keys.get(2)).getValueString());
	}

	@Test
	void sortKeys_ascending_numberKeys() throws Exception {
		final YamlMapping mapping = new YamlMapping();
		mapping.add(30, "thirty");
		mapping.add(10, "ten");
		mapping.add(20, "twenty");

		mapping.sortKeys(true);

		final List<YamlNode> keys = List.copyOf(mapping.keySet());
		assertEquals(10.0, ((Number) ((YamlScalar) keys.get(0)).getValue()).doubleValue());
		assertEquals(20.0, ((Number) ((YamlScalar) keys.get(1)).getValue()).doubleValue());
		assertEquals(30.0, ((Number) ((YamlScalar) keys.get(2)).getValue()).doubleValue());
	}

	@Test
	void sortKeys_descending_numberKeys() throws Exception {
		final YamlMapping mapping = new YamlMapping();
		mapping.add(30, "thirty");
		mapping.add(10, "ten");
		mapping.add(20, "twenty");

		mapping.sortKeys(false);

		final List<YamlNode> keys = List.copyOf(mapping.keySet());
		assertEquals(30.0, ((Number) ((YamlScalar) keys.get(0)).getValue()).doubleValue());
		assertEquals(20.0, ((Number) ((YamlScalar) keys.get(1)).getValue()).doubleValue());
		assertEquals(10.0, ((Number) ((YamlScalar) keys.get(2)).getValue()).doubleValue());
	}

	@Test
	void sortKeys_ascending_booleanKeys() throws Exception {
		final YamlMapping mapping = new YamlMapping();
		mapping.add(true, "yes");
		mapping.add(false, "no");

		mapping.sortKeys(true);

		final List<YamlNode> keys = List.copyOf(mapping.keySet());
		// Boolean.compare(false, true) < 0 → false comes first ascending
		assertEquals(Boolean.FALSE, ((YamlScalar) keys.get(0)).getValue());
		assertEquals(Boolean.TRUE, ((YamlScalar) keys.get(1)).getValue());
	}

	@Test
	void sortKeys_nullKeyComesFirst_ascending() throws Exception {
		final YamlMapping mapping = new YamlMapping();
		mapping.add("b", "B");
		mapping.add((String) null, "null-value");
		mapping.add("a", "A");

		mapping.sortKeys(true);

		final List<YamlNode> keys = List.copyOf(mapping.keySet());
		// null key should sort first when ascending
		assertNull(((YamlScalar) keys.get(0)).getValue());
	}

	@Test
	void sortKeys_nullKeyComesLast_descending() throws Exception {
		final YamlMapping mapping = new YamlMapping();
		mapping.add("b", "B");
		mapping.add((String) null, "null-value");
		mapping.add("a", "A");

		mapping.sortKeys(false);

		final List<YamlNode> keys = List.copyOf(mapping.keySet());
		// null key should sort last when descending
		assertNull(((YamlScalar) keys.get(keys.size() - 1)).getValue());
	}

	@Test
	void sortKeys_preservesValues() throws Exception {
		final YamlMapping mapping = new YamlMapping();
		mapping.add("z", "last");
		mapping.add("a", "first");

		mapping.sortKeys(true);

		assertEquals("first", ((YamlScalar) mapping.get("a")).getValue());
		assertEquals("last", ((YamlScalar) mapping.get("z")).getValue());
	}

	@Test
	void sortKeys_emptyMapping_noException() {
		final YamlMapping mapping = new YamlMapping();
		assertDoesNotThrow(() -> mapping.sortKeys(true));
		assertEquals(0, mapping.size());
	}

	@Test
	void sortKeys_singleEntry_unchanged() throws Exception {
		final YamlMapping mapping = new YamlMapping();
		mapping.add("only", "value");

		mapping.sortKeys(true);

		assertEquals(1, mapping.size());
		assertEquals("value", ((YamlScalar) mapping.get("only")).getValue());
	}

	@Test
	void merge_withNull_returnsUnchanged() throws Exception {
		final YamlMapping mapping = new YamlMapping();
		mapping.add("key", "value");

		mapping.merge(null, YamlMappingMergeStrategy.OVERWRITE);

		assertEquals(1, mapping.size());
	}

	@Test
	void merge_overwrite_replacesExistingKey() throws Exception {
		final YamlMapping base = new YamlMapping();
		base.add("key", "original");

		final YamlMapping other = new YamlMapping();
		other.add("key", "updated");

		base.merge(other, YamlMappingMergeStrategy.OVERWRITE);

		assertEquals("updated", ((YamlScalar) base.get("key")).getValue());
	}

	@Test
	void merge_keepExisting_doesNotReplaceExistingKey() throws Exception {
		final YamlMapping base = new YamlMapping();
		base.add("key", "original");

		final YamlMapping other = new YamlMapping();
		other.add("key", "updated");

		base.merge(other, YamlMappingMergeStrategy.KEEP_EXISTING);

		assertEquals("original", ((YamlScalar) base.get("key")).getValue());
	}

	@Test
	void merge_addsNewKeys_bothStrategies() throws Exception {
		for (final YamlMappingMergeStrategy strategy : YamlMappingMergeStrategy.values()) {
			final YamlMapping base = new YamlMapping();
			base.add("existing", "e");

			final YamlMapping other = new YamlMapping();
			other.add("newKey", "n");

			base.merge(other, strategy);

			assertTrue(base.containsKey("newKey"), "Strategy " + strategy + " should add new keys");
			assertEquals("n", ((YamlScalar) base.get("newKey")).getValue());
		}
	}

	@Test
	void merge_overwrite_addsAndOverwritesMixed() throws Exception {
		final YamlMapping base = new YamlMapping();
		base.add("a", "original-a");
		base.add("b", "original-b");

		final YamlMapping other = new YamlMapping();
		other.add("b", "updated-b");
		other.add("c", "new-c");

		base.merge(other, YamlMappingMergeStrategy.OVERWRITE);

		assertEquals("original-a", ((YamlScalar) base.get("a")).getValue());
		assertEquals("updated-b", ((YamlScalar) base.get("b")).getValue());
		assertEquals("new-c", ((YamlScalar) base.get("c")).getValue());
		assertEquals(3, base.size());
	}

	@Test
	void merge_keepExisting_addsAndKeepsMixed() throws Exception {
		final YamlMapping base = new YamlMapping();
		base.add("a", "original-a");
		base.add("b", "original-b");

		final YamlMapping other = new YamlMapping();
		other.add("b", "updated-b");
		other.add("c", "new-c");

		base.merge(other, YamlMappingMergeStrategy.KEEP_EXISTING);

		assertEquals("original-a", ((YamlScalar) base.get("a")).getValue());
		assertEquals("original-b", ((YamlScalar) base.get("b")).getValue());
		assertEquals("new-c", ((YamlScalar) base.get("c")).getValue());
		assertEquals(3, base.size());
	}

	@Test
	void merge_returnsThisForChaining() throws Exception {
		final YamlMapping base = new YamlMapping();
		final YamlMapping other = new YamlMapping();
		other.add("x", "1");

		final YamlMapping result = base.merge(other, YamlMappingMergeStrategy.OVERWRITE);

		assertSame(base, result);
	}
}