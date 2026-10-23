package de.soderer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.soderer.json.JsonComparator.DiffEntry;
import de.soderer.json.JsonComparator.DiffType;

@SuppressWarnings("static-method")
class JsonComparatorTest {
	private static JsonNode json(final String text) throws Exception {
		return JsonReader.readJsonItemString(text);
	}

	@Test
	void compare_equalTrees_noDifferences() throws Exception {
		final List<DiffEntry> diff = new JsonComparator().compare(json("{\"a\": 1, \"b\": [1, null, true], \"c\": null}"), json("{\"a\": 1, \"b\": [1, null, true], \"c\": null}"));
		assertTrue(diff.isEmpty());
		assertEquals("No differences found", JsonComparator.renderAsText(diff));
		assertTrue(new JsonComparator().compare(null, null).isEmpty());
	}

	@Test
	void compare_addedRemovedChanged() throws Exception {
		final JsonNode left = json("{\"same\": 1, \"changed\": \"old\", \"removed\": 2, \"list\": [1, 2, 3], \"typeChange\": {\"x\": 1}, \"nullToValue\": null}");
		final JsonNode right = json("{\"same\": 1, \"changed\": \"new\", \"added\": true, \"list\": [1, 5], \"typeChange\": \"scalar\", \"nullToValue\": 3}");

		final List<DiffEntry> diff = new JsonComparator().compare(left, right);
		final String text = JsonComparator.renderAsText(diff);

		assertTrue(text.contains("~ changed: \"old\" -> \"new\""), text);
		assertTrue(text.contains("- removed: \"2\""), text);
		assertTrue(text.contains("+ added: \"true\""), text);
		assertTrue(text.contains("~ list[1]: \"2\" -> \"5\""), text);
		assertTrue(text.contains("- list[2]: \"3\""), text);
		assertTrue(text.contains("~ typeChange: json:"), text);
		assertTrue(text.contains("~ nullToValue: null -> \"3\""), text);

		for (final DiffEntry entry : diff) {
			if ("added".equals(entry.getPath())) {
				assertEquals(DiffType.ADDED, entry.getType());
				assertNull(entry.getOldValue());
				assertEquals(Boolean.TRUE, entry.getNewValue());
			}
		}
	}

	@Test
	void compare_rootLevelNullAndScalars() throws Exception {
		final List<DiffEntry> added = new JsonComparator().compare(null, new JsonValueString("x"));
		assertEquals(DiffType.ADDED, added.get(0).getType());
		assertEquals("root", added.get(0).getPath());

		final List<DiffEntry> removed = new JsonComparator().compare(new JsonObject(), null);
		assertEquals(DiffType.REMOVED, removed.get(0).getType());
		assertTrue(JsonComparator.renderAsText(removed).contains("json:"));

		final List<DiffEntry> changedToNull = new JsonComparator().compare(new JsonValueInteger(1), new JsonValueNull());
		assertEquals(DiffType.CHANGED, changedToNull.get(0).getType());

		// Integer and Long values are compared numerically
		assertTrue(new JsonComparator().compare(new JsonValueInteger(1), new JsonValueInteger(1L)).isEmpty());
		assertTrue(new JsonComparator().compare(new JsonValueNull(), new JsonValueNull()).isEmpty());
	}

	@Test
	void compare_keysWithPathSyntaxCharactersAreQuoted() throws Exception {
		final JsonObject left = new JsonObject().add("a.b", "1").add("", "1").add(" lead", "1").add("tab\tkey", "1").add("nested", new JsonObject().add("x[0]", "1"));
		final JsonObject right = new JsonObject().add("a.b", "2").add("", "2").add(" lead", "2").add("tab\tkey", "2").add("nested", new JsonObject().add("x[0]", "2"));
		final String text = JsonComparator.renderAsText(new JsonComparator().compare(left, right));
		assertTrue(text.contains("[\"a.b\"]"), text);
		assertTrue(text.contains("[\"\"]"), text);
		assertTrue(text.contains("[\" lead\"]"), text);
		assertTrue(text.contains("[\"tab\\tkey\"]"), text);
		assertTrue(text.contains("nested[\"x[0]\"]"), text);
	}

	@Test
	void pathHelpers_quoteAndUnquote() {
		assertEquals("key", JsonComparator.appendKeyToPath("root", "key"));
		assertEquals("parent.key", JsonComparator.appendKeyToPath("parent", "key"));
		assertEquals("parent[\"a:b\"]", JsonComparator.appendKeyToPath("parent", "a:b"));
		assertEquals("[3]", JsonComparator.appendIndexToPath("root", 3));
		assertEquals("list[3]", JsonComparator.appendIndexToPath("list", 3));

		final String original = "back\\slash \"quote\"\nnew\rret\ttab";
		final String quoted = JsonComparator.quote(original);
		assertEquals("\"back\\\\slash \\\"quote\\\"\\nnew\\rret\\ttab\"", quoted);
		assertEquals(original, JsonComparator.unquote(quoted));
		// A trailing single backslash is kept as is
		assertEquals("a\\", JsonComparator.unquote("\"a\\\""));
	}

	@Test
	void buildDifferenceOnly() throws Exception {
		final JsonComparator comparator = new JsonComparator();
		assertNull(comparator.buildDifferenceOnly(null, null));
		final JsonValueString right = new JsonValueString("x");
		assertSame(right, comparator.buildDifferenceOnly(null, right));
		assertInstanceOf(JsonValueNull.class, comparator.buildDifferenceOnly(right, null));
		assertNull(comparator.buildDifferenceOnly(new JsonValueNull(), new JsonValueNull()));
		assertNull(comparator.buildDifferenceOnly(new JsonValueString("x"), new JsonValueString("x")));
		assertSame(right, comparator.buildDifferenceOnly(new JsonValueNull(), right));
		assertSame(right, comparator.buildDifferenceOnly(new JsonObject(), right));

		final JsonNode difference = comparator.buildDifferenceOnly(
				json("{\"same\": 1, \"changed\": 1, \"removed\": 1, \"list\": [1, 2], \"sameList\": [1], \"obj\": {\"a\": 1}}"),
				json("{\"same\": 1, \"changed\": 2, \"added\": 3, \"list\": [1, 3, 4], \"sameList\": [1], \"obj\": {\"a\": 1}}"));
		final JsonObject differenceObject = (JsonObject) difference;
		assertEquals(4, differenceObject.size());
		assertEquals(2, ((Number) differenceObject.getSimpleValue("changed")).intValue());
		assertTrue(differenceObject.get("removed").isNull());
		assertEquals(3, ((JsonArray) differenceObject.get("list")).size());
		assertEquals(3, ((Number) differenceObject.getSimpleValue("added")).intValue());

		assertNull(comparator.buildDifferenceOnly(json("{\"a\": [1]}"), json("{\"a\": [1]}")));
	}

	@Test
	void buildIntersectionOnly() throws Exception {
		final JsonComparator comparator = new JsonComparator();
		assertNull(comparator.buildIntersectionOnly(null, new JsonObject()));
		assertNull(comparator.buildIntersectionOnly(new JsonObject(), null));
		assertInstanceOf(JsonValueNull.class, comparator.buildIntersectionOnly(new JsonValueNull(), new JsonValueNull()));
		assertNull(comparator.buildIntersectionOnly(new JsonValueString("a"), new JsonValueString("b")));
		assertNull(comparator.buildIntersectionOnly(new JsonValueNull(), new JsonValueString("b")));
		assertNull(comparator.buildIntersectionOnly(new JsonObject(), new JsonArray()));

		final JsonObject intersection = (JsonObject) comparator.buildIntersectionOnly(
				json("{\"same\": 1, \"changed\": 1, \"onlyLeft\": 1, \"list\": [1, 2, 3], \"obj\": {\"a\": 1, \"b\": 2}}"),
				json("{\"same\": 1, \"changed\": 2, \"onlyRight\": 1, \"list\": [1, 5], \"obj\": {\"a\": 1, \"b\": 3}}"));
		assertEquals(3, intersection.size());
		assertEquals(1, ((Number) intersection.getSimpleValue("same")).intValue());
		assertEquals(1, ((JsonArray) intersection.get("list")).size());
		assertEquals(1, ((JsonObject) intersection.get("obj")).size());

		assertNull(comparator.buildIntersectionOnly(json("{\"a\": 1}"), json("{\"a\": 2}")));
		assertNull(comparator.buildIntersectionOnly(json("[1]"), json("[2]")));
	}

	@Test
	void diffEntry_getters() {
		final DiffEntry entry = new DiffEntry("p", DiffType.CHANGED, "o", "n");
		assertEquals("p", entry.getPath());
		assertEquals(DiffType.CHANGED, entry.getType());
		assertEquals("o", entry.getOldValue());
		assertEquals("n", entry.getNewValue());
	}
}
