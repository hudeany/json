package de.soderer.yaml;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.data.YamlSequenceMergeStrategy;

@SuppressWarnings("static-method")
class YamlSequenceTest {
	@Test
	void sort_ascending_stringItems() {
		final YamlSequence seq = new YamlSequence();
		seq.add("banana").add("apple").add("cherry");

		seq.sort(true);

		final List<Object> items = List.copyOf(seq.simpleItems());
		assertEquals("apple", items.get(0));
		assertEquals("banana", items.get(1));
		assertEquals("cherry", items.get(2));
	}

	@Test
	void sort_descending_stringItems() {
		final YamlSequence seq = new YamlSequence();
		seq.add("banana").add("apple").add("cherry");

		seq.sort(false);

		final List<Object> items = List.copyOf(seq.simpleItems());
		assertEquals("cherry", items.get(0));
		assertEquals("banana", items.get(1));
		assertEquals("apple", items.get(2));
	}

	@Test
	void sort_ascending_numberItems() {
		final YamlSequence seq = new YamlSequence();
		seq.add(30).add(10).add(20);

		seq.sort(true);

		final List<Object> items = List.copyOf(seq.simpleItems());
		assertEquals(10.0, ((Number) items.get(0)).doubleValue());
		assertEquals(20.0, ((Number) items.get(1)).doubleValue());
		assertEquals(30.0, ((Number) items.get(2)).doubleValue());
	}

	@Test
	void sort_descending_numberItems() {
		final YamlSequence seq = new YamlSequence();
		seq.add(30).add(10).add(20);

		seq.sort(false);

		final List<Object> items = List.copyOf(seq.simpleItems());
		assertEquals(30.0, ((Number) items.get(0)).doubleValue());
		assertEquals(20.0, ((Number) items.get(1)).doubleValue());
		assertEquals(10.0, ((Number) items.get(2)).doubleValue());
	}

	@Test
	void sort_ascending_booleanItems() {
		final YamlSequence seq = new YamlSequence();
		seq.add(Boolean.TRUE).add(Boolean.FALSE);

		seq.sort(true);

		final List<Object> items = List.copyOf(seq.simpleItems());
		assertEquals(Boolean.FALSE, items.get(0));
		assertEquals(Boolean.TRUE, items.get(1));
	}

	@Test
	void sort_nullComesFirst_ascending() {
		final YamlSequence seq = new YamlSequence();
		seq.add("b").addNull().add("a");

		seq.sort(true);

		final List<Object> items = new ArrayList<>(seq.simpleItems());
		assertNull(items.get(0));
	}

	@Test
	void sort_nullComesLast_descending() {
		final YamlSequence seq = new YamlSequence();
		seq.add("b").addNull().add("a");

		seq.sort(false);

		final List<Object> items = new ArrayList<>(seq.simpleItems());
		assertNull(items.get(items.size() - 1));
	}

	@Test
	void sort_emptySequence_noException() {
		final YamlSequence seq = new YamlSequence();
		assertDoesNotThrow(() -> seq.sort(true));
		assertEquals(0, seq.size());
	}

	@Test
	void sort_singleItem_unchanged() {
		final YamlSequence seq = new YamlSequence();
		seq.add("only");

		seq.sort(true);

		assertEquals(1, seq.size());
		assertEquals("only", seq.simpleItems().iterator().next());
	}

	@Test
	void sortByAttribute_ascending() throws Exception {
		final YamlSequence seq = new YamlSequence();

		final YamlMapping m1 = new YamlMapping();
		m1.add("name", "Charlie");
		final YamlMapping m2 = new YamlMapping();
		m2.add("name", "Alice");
		final YamlMapping m3 = new YamlMapping();
		m3.add("name", "Bob");

		seq.add(m1).add(m2).add(m3);
		seq.sortByAttribute("name", true);

		assertEquals("Alice", ((YamlScalar) ((YamlMapping) seq.get(0)).get("name")).getValue());
		assertEquals("Bob", ((YamlScalar) ((YamlMapping) seq.get(1)).get("name")).getValue());
		assertEquals("Charlie", ((YamlScalar) ((YamlMapping) seq.get(2)).get("name")).getValue());
	}

	@Test
	void sortByAttribute_descending() throws Exception {
		final YamlSequence seq = new YamlSequence();

		final YamlMapping m1 = new YamlMapping();
		m1.add("score", 80);
		final YamlMapping m2 = new YamlMapping();
		m2.add("score", 95);
		final YamlMapping m3 = new YamlMapping();
		m3.add("score", 60);

		seq.add(m1).add(m2).add(m3);
		seq.sortByAttribute("score", false);

		assertEquals(95.0, ((Number) ((YamlScalar) ((YamlMapping) seq.get(0)).get("score")).getValue()).doubleValue());
		assertEquals(80.0, ((Number) ((YamlScalar) ((YamlMapping) seq.get(1)).get("score")).getValue()).doubleValue());
		assertEquals(60.0, ((Number) ((YamlScalar) ((YamlMapping) seq.get(2)).get("score")).getValue()).doubleValue());
	}

	@Test
	void sortByAttribute_missingAttributeTreatedAsNull() throws Exception {
		final YamlSequence seq = new YamlSequence();

		final YamlMapping withAttr = new YamlMapping();
		withAttr.add("name", "Alice");
		final YamlMapping withoutAttr = new YamlMapping();
		// no "name" key

		seq.add(withoutAttr).add(withAttr);
		seq.sortByAttribute("name", true);

		// null (missing) comes first ascending
		assertFalse(((YamlMapping) seq.get(0)).containsKey("name"));
		assertEquals("Alice", ((YamlScalar) ((YamlMapping) seq.get(1)).get("name")).getValue());
	}

	@Test
	void sortByAttribute_nonMappingItemsStayInPlace() {
		final YamlSequence seq = new YamlSequence();
		seq.add("plain-string");
		// sortByAttribute returns 0 for non-mapping items → order unchanged
		assertDoesNotThrow(() -> seq.sortByAttribute("name", true));
		assertEquals(1, seq.size());
	}

	@Test
	void merge_withNull_returnsUnchanged() {
		final YamlSequence seq = new YamlSequence();
		seq.add("a");

		seq.merge(null, YamlSequenceMergeStrategy.APPEND_ALL);

		assertEquals(1, seq.size());
	}

	@Test
	void merge_appendAll_addsDuplicates() {
		final YamlSequence base = new YamlSequence();
		base.add("a").add("b");

		final YamlSequence other = new YamlSequence();
		other.add("b").add("c");

		base.merge(other, YamlSequenceMergeStrategy.APPEND_ALL);

		// "b" appears twice, "c" is appended
		assertEquals(4, base.size());
		final List<Object> items = List.copyOf(base.simpleItems());
		assertEquals("a", items.get(0));
		assertEquals("b", items.get(1));
		assertEquals("b", items.get(2));
		assertEquals("c", items.get(3));
	}

	@Test
	void merge_skipDuplicates_doesNotAddExistingItems() {
		final YamlSequence base = new YamlSequence();
		base.add("a").add("b");

		final YamlSequence other = new YamlSequence();
		other.add("b").add("c");

		base.merge(other, YamlSequenceMergeStrategy.SKIP_DUPLICATES);

		assertEquals(3, base.size());
		final List<Object> items = List.copyOf(base.simpleItems());
		assertEquals("a", items.get(0));
		assertEquals("b", items.get(1));
		assertEquals("c", items.get(2));
	}

	@Test
	void merge_skipDuplicates_emptyOther_unchanged() {
		final YamlSequence base = new YamlSequence();
		base.add("a");

		final YamlSequence other = new YamlSequence();

		base.merge(other, YamlSequenceMergeStrategy.SKIP_DUPLICATES);

		assertEquals(1, base.size());
	}

	@Test
	void merge_appendAll_emptyBase_copiesAll() {
		final YamlSequence base = new YamlSequence();

		final YamlSequence other = new YamlSequence();
		other.add("x").add("y");

		base.merge(other, YamlSequenceMergeStrategy.APPEND_ALL);

		assertEquals(2, base.size());
	}

	@Test
	void merge_returnsThisForChaining() {
		final YamlSequence base = new YamlSequence();
		final YamlSequence other = new YamlSequence();
		other.add("z");

		final YamlSequence result = base.merge(other, YamlSequenceMergeStrategy.APPEND_ALL);

		assertSame(base, result);
	}
}