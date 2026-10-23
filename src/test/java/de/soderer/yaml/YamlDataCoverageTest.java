package de.soderer.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.exception.MissingKeyException;
import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlMappingMergeStrategy;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.data.YamlSequenceMergeStrategy;

/**
 * Covers the typed key/value overloads of {@link YamlMapping} and {@link YamlSequence}.
 */
@SuppressWarnings("static-method")
class YamlDataCoverageTest {
	@Test
	void mapping_addWithStringNumberBooleanAndNullKeys() throws Exception {
		final YamlMapping mapping = new YamlMapping()
				.add("string", "value")
				.add("node", new YamlSequence())
				.add(Integer.valueOf(1), "one")
				.add(Boolean.TRUE, Integer.valueOf(2))
				.add((String) null, "nullKey");

		assertEquals(5, mapping.size());
		assertEquals("value", mapping.getSimpleValue("string"));
		assertInstanceOf(YamlSequence.class, mapping.getSimpleValue("node"));
		assertNull(mapping.getSimpleValue("missing"));
		assertEquals("one", ((YamlScalar) mapping.get(Integer.valueOf(1))).getValue());
		assertEquals(2, ((Number) ((YamlScalar) mapping.get(Boolean.TRUE)).getValue()).intValue());
		assertEquals("nullKey", ((YamlScalar) mapping.get((String) null)).getValue());
		assertEquals("nullKey", ((YamlScalar) mapping.get((Number) null)).getValue());
		assertEquals("nullKey", ((YamlScalar) mapping.get((Boolean) null)).getValue());

		assertTrue(mapping.containsKey("string"));
		assertTrue(mapping.containsKey(Integer.valueOf(1)));
		assertTrue(mapping.containsKey(Boolean.TRUE));
		assertTrue(mapping.containsKey((String) null));
		assertTrue(mapping.containsKey((Number) null));
		assertTrue(mapping.containsKey((Boolean) null));
		assertFalse(mapping.containsKey(Boolean.FALSE));

		assertThrows(DuplicateKeyException.class, () -> mapping.add("string", "again"));
		assertThrows(DuplicateKeyException.class, () -> mapping.add(Integer.valueOf(1), "again"));
		assertThrows(DuplicateKeyException.class, () -> mapping.add(Boolean.TRUE, "again"));
		assertThrows(DuplicateKeyException.class, () -> mapping.add((Number) null, "again"));
		assertThrows(DuplicateKeyException.class, () -> mapping.add((Boolean) null, "again"));
	}

	@Test
	void mapping_replace() throws Exception {
		final YamlMapping mapping = new YamlMapping()
				.add("s", "x")
				.add(Long.valueOf(5), "x")
				.add(Boolean.FALSE, "x")
				.add((String) null, "x");

		mapping.replace("s", "new")
		.replace("s", new YamlSequence())
		.replace(Long.valueOf(5), "five")
		.replace(Boolean.FALSE, new YamlMapping())
		.replace((String) null, "nullKey")
		.replace((Number) null, "nullKey2")
		.replace((Boolean) null, "nullKey3");

		assertInstanceOf(YamlSequence.class, mapping.get("s"));
		assertEquals("five", ((YamlScalar) mapping.get(Long.valueOf(5))).getValue());
		assertInstanceOf(YamlMapping.class, mapping.get(Boolean.FALSE));
		assertEquals("nullKey3", ((YamlScalar) mapping.get((String) null)).getValue());

		assertThrows(MissingKeyException.class, () -> mapping.replace("missing", "x"));
		assertThrows(MissingKeyException.class, () -> mapping.replace(Integer.valueOf(99), "x"));
		assertThrows(MissingKeyException.class, () -> mapping.replace(Boolean.TRUE, "x"));
	}

	@Test
	void mapping_putAndRemove() {
		final YamlMapping mapping = new YamlMapping()
				.put("s", "x")
				.put("node", new YamlMapping())
				.put(Integer.valueOf(1), "one")
				.put(Integer.valueOf(2), new YamlSequence())
				.put(Boolean.TRUE, "yes")
				.put(Boolean.FALSE, new YamlMapping())
				.put((String) null, "a")
				.put((Number) null, "b")
				.put((Boolean) null, "c");
		assertEquals(7, mapping.size());
		assertEquals("c", ((YamlScalar) mapping.get((String) null)).getValue());

		assertEquals("x", ((YamlScalar) mapping.remove("s")).getValue());
		assertNull(mapping.remove("s"));
		assertEquals("one", ((YamlScalar) mapping.remove(Integer.valueOf(1))).getValue());
		assertEquals("yes", ((YamlScalar) mapping.remove(Boolean.TRUE)).getValue());
		assertEquals("c", ((YamlScalar) mapping.remove((String) null)).getValue());
		assertNull(mapping.remove((Number) null));
		assertNull(mapping.remove((Boolean) null));
		assertEquals(3, mapping.size());
		assertFalse(mapping.isEmpty());
		assertTrue(new YamlMapping().isEmpty());
	}

	@Test
	void mapping_simpleViews() throws Exception {
		final YamlSequence nested = new YamlSequence();
		final YamlMapping mapping = new YamlMapping()
				.add("string", "v")
				.add(Integer.valueOf(1), (Object) null)
				.add(Boolean.TRUE, nested)
				.add(new YamlScalar("multi\nline", YamlScalarType.MULTILINE), new YamlScalar("text"))
				.add((String) null, (Object) null);
		mapping.add(new YamlScalar("~", YamlScalarType.NULL_VALUE) {
			// Distinct key object with the same type, value differs by identity only
			@Override
			public boolean equals(final Object otherObject) {
				return this == otherObject;
			}

			@Override
			public int hashCode() {
				return System.identityHashCode(this);
			}
		}, nested);

		final List<Object> values = new ArrayList<>(mapping.simpleValues());
		assertEquals("v", values.get(0));
		assertNull(values.get(1));
		assertSame(nested, values.get(2));

		final List<Map.Entry<String, Object>> entries = new ArrayList<>(mapping.simpleEntrySet());
		assertEquals("string", entries.get(0).getKey());
		assertEquals("1", entries.get(1).getKey());
		assertNull(entries.get(1).getValue());
		assertEquals("true", entries.get(2).getKey());
		assertSame(nested, entries.get(2).getValue());
		assertEquals("multi\nline", entries.get(3).getKey());
		assertNull(entries.get(4).getKey());
		assertSame(nested, entries.get(4).getValue());

		final Iterator<Map.Entry<String, Object>> iterator = mapping.iterator();
		assertTrue(iterator.hasNext());

		final YamlMapping nullKeyWithValue = new YamlMapping().add((String) null, "value");
		assertEquals("value", nullKeyWithValue.simpleEntrySet().iterator().next().getValue());

		final YamlMapping complexKey = new YamlMapping().add(new YamlSequence(), new YamlScalar("x"));
		assertThrows(RuntimeException.class, complexKey::simpleEntrySet);

		assertThrows(UnsupportedOperationException.class, () -> mapping.keySet().clear());
		assertThrows(UnsupportedOperationException.class, () -> mapping.values().clear());
	}

	@Test
	void mapping_equalsFlowStyleMergeToString() throws Exception {
		final YamlMapping a = new YamlMapping().add("k", "v");
		final YamlMapping b = new YamlMapping().add("k", "v");
		assertEquals(a, a);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, null);
		assertNotEquals(a, new YamlSequence());
		assertNotEquals(a, new YamlMapping().add("k", "w"));

		assertSame(a, a.withFlowStyle(true));
		assertTrue(a.isFlowStyle());
		assertTrue(a.toString().contains("k"));

		assertSame(a, a.merge(null, YamlMappingMergeStrategy.OVERWRITE));
		a.merge(new YamlMapping().add("k", "other").add("n", "new"), YamlMappingMergeStrategy.KEEP_EXISTING);
		assertEquals("v", a.getSimpleValue("k"));
		assertEquals("new", a.getSimpleValue("n"));
		a.merge(new YamlMapping().add("k", "other"), YamlMappingMergeStrategy.OVERWRITE);
		assertEquals("other", a.getSimpleValue("k"));
	}

	@Test
	void sequence_typedOverloads() {
		final YamlSequence sequence = new YamlSequence()
				.addNull()
				.add("s")
				.add((String) null)
				.add(Integer.valueOf(1))
				.add((Number) null)
				.add(Boolean.TRUE)
				.add((Boolean) null);
		assertEquals(7, sequence.size());

		sequence.insertNull(0)
		.insert(0, "first")
		.insert(0, (String) null)
		.insert(0, Integer.valueOf(9))
		.insert(0, (Number) null)
		.insert(0, Boolean.FALSE)
		.insert(0, (Boolean) null);
		assertEquals(14, sequence.size());
		assertNull(((YamlScalar) sequence.get(0)).getValue());
		assertEquals(Boolean.FALSE, ((YamlScalar) sequence.get(1)).getValue());

		sequence.setNull(0)
		.set(1, "x")
		.set(2, (String) null)
		.set(3, Long.valueOf(7))
		.set(4, (Number) null)
		.set(5, Boolean.TRUE)
		.set(6, (Boolean) null);
		assertEquals("x", ((YamlScalar) sequence.get(1)).getValue());
		assertEquals(7L, ((Number) ((YamlScalar) sequence.get(3)).getValue()).longValue());

		assertTrue(sequence.remove("x"));
		assertFalse(sequence.remove("missing"));
		assertTrue(sequence.remove(Long.valueOf(7)));
		assertTrue(sequence.remove(Boolean.TRUE));
		assertTrue(sequence.remove((String) null));
		assertTrue(sequence.remove((Number) null));
		assertTrue(sequence.remove((Boolean) null));
		assertFalse(sequence.remove(new YamlMapping()));
		assertFalse(sequence.isEmpty());
		assertTrue(new YamlSequence().isEmpty());
	}

	@Test
	void sequence_equalsFlowStyleMergeSort() {
		final YamlSequence a = new YamlSequence().add("x");
		assertEquals(a, a);
		assertEquals(a, new YamlSequence().add("x"));
		assertNotEquals(a, null);
		assertNotEquals(a, new YamlMapping());
		assertNotEquals(a, new YamlSequence().add("y"));

		a.setFlowStyle(true);
		assertTrue(a.isFlowStyle());
		assertSame(a, a.withFlowStyle(false));
		assertTrue(a.toString().contains("x"));

		assertSame(a, a.merge(null, YamlSequenceMergeStrategy.APPEND_ALL));
		a.merge(new YamlSequence().add("x").add("y"), YamlSequenceMergeStrategy.SKIP_DUPLICATES);
		assertEquals(2, a.size());
		a.merge(new YamlSequence().add("x"), YamlSequenceMergeStrategy.APPEND_ALL);
		assertEquals(3, a.size());

		final YamlSequence mixed = new YamlSequence().add("b").addNull().add(Integer.valueOf(2)).add(Boolean.TRUE).add(new YamlMapping()).addNull();
		mixed.sort(true);
		assertNull(((YamlScalar) mixed.get(0)).getValue());
		mixed.sort(false);
		assertEquals("b", ((YamlScalar) mixed.get(0)).getValue());
	}

	@Test
	void comparator_buildDifferenceOnly() throws Exception {
		final de.soderer.yaml.data.YamlComparator comparator = new de.soderer.yaml.data.YamlComparator();
		assertNull(comparator.buildDifferenceOnly(null, null));
		final YamlScalar right = new YamlScalar("x");
		assertSame(right, comparator.buildDifferenceOnly(null, right));
		assertNull(((YamlScalar) comparator.buildDifferenceOnly(right, null)).getValue());
		assertNull(comparator.buildDifferenceOnly(new YamlScalar(null), new YamlScalar(null)));
		assertNull(comparator.buildDifferenceOnly(new YamlScalar("x"), new YamlScalar("x")));
		assertSame(right, comparator.buildDifferenceOnly(new YamlScalar(null), right));
		assertNull(comparator.buildDifferenceOnly(new YamlAlias("a"), new YamlAlias("a")));
		assertSame(right, comparator.buildDifferenceOnly(new YamlMapping(), right));

		final YamlNode left = YamlReader.readDocument("same: 1\nchanged: 1\nremoved: 1\nlist: [1, 2]\nsameList: [1]\n").getRoot();
		final YamlNode newer = YamlReader.readDocument("same: 1\nchanged: 2\nadded: 3\nlist: [1, 3, 4]\nsameList: [1]\n").getRoot();
		final YamlMapping difference = (YamlMapping) comparator.buildDifferenceOnly(left, newer);
		assertEquals(4, difference.size());
		assertEquals(2, ((Number) difference.getSimpleValue("changed")).intValue());
		assertNull(difference.getSimpleValue("removed"));
		assertEquals(3, ((YamlSequence) difference.get("list")).size());

		assertNull(comparator.buildDifferenceOnly(YamlReader.readDocument("a: [1]\n").getRoot(), YamlReader.readDocument("a: [1]\n").getRoot()));
	}

	@Test
	void comparator_buildIntersectionOnly() throws Exception {
		final de.soderer.yaml.data.YamlComparator comparator = new de.soderer.yaml.data.YamlComparator();
		assertNull(comparator.buildIntersectionOnly(null, new YamlMapping()));
		assertNull(((YamlScalar) comparator.buildIntersectionOnly(new YamlScalar(null), new YamlScalar(null))).getValue());
		assertNull(comparator.buildIntersectionOnly(new YamlScalar("a"), new YamlScalar("b")));
		final YamlAlias alias = new YamlAlias("a");
		assertSame(alias, comparator.buildIntersectionOnly(new YamlAlias("a"), alias));
		assertNull(comparator.buildIntersectionOnly(new YamlMapping(), new YamlSequence()));

		final YamlNode left = YamlReader.readDocument("same: 1\nchanged: 1\nonlyLeft: 1\nlist: [1, 2, 3]\nobj:\n  a: 1\n  b: 2\n").getRoot();
		final YamlNode right = YamlReader.readDocument("same: 1\nchanged: 2\nonlyRight: 1\nlist: [1, 5]\nobj:\n  a: 1\n  b: 3\n").getRoot();
		final YamlMapping intersection = (YamlMapping) comparator.buildIntersectionOnly(left, right);
		assertEquals(3, intersection.size());
		assertEquals(1, ((YamlSequence) intersection.get("list")).size());
		assertEquals(1, ((YamlMapping) intersection.get("obj")).size());

		assertNull(comparator.buildIntersectionOnly(YamlReader.readDocument("a: 1\n").getRoot(), YamlReader.readDocument("a: 2\n").getRoot()));
		assertNull(comparator.buildIntersectionOnly(YamlReader.readDocument("- 1\n").getRoot(), YamlReader.readDocument("- 2\n").getRoot()));
	}

	@Test
	void comparator_compareAndRender() throws Exception {
		final de.soderer.yaml.data.YamlComparator comparator = new de.soderer.yaml.data.YamlComparator();
		final YamlMapping left = new YamlMapping().add("a.b", "1").add("alias", new YamlAlias("x")).add("type", new YamlMapping().add("k", "v")).add(new YamlSequence().add("complex"), new YamlScalar("1"));
		final YamlMapping right = new YamlMapping().add("a.b", "2").add("alias", new YamlAlias("y")).add("type", "scalar").add(new YamlSequence().add("complex"), new YamlScalar("2"));
		final String text = de.soderer.yaml.data.YamlComparator.renderAsText(comparator.compare(left, right));
		assertTrue(text.contains("[\"a.b\"]"), text);
		assertTrue(text.contains("yaml:\"*x\""), text);
		assertTrue(text.contains("~ type: yaml:"), text);
		assertTrue(text.contains("[[- complex]]") || text.contains("[\"[- complex]\"]"), text);
		assertTrue(comparator.compare(new YamlAlias("x"), new YamlAlias("x")).isEmpty());
		assertEquals("No differences found", de.soderer.yaml.data.YamlComparator.renderAsText(comparator.compare(null, null)));
	}
}
