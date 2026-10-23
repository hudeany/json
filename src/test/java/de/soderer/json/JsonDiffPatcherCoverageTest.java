package de.soderer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.soderer.json.JsonDiffPatcher.PatchConflictException;
import de.soderer.json.JsonDiffPatcher.PatchEntry;
import de.soderer.json.JsonDiffPatcher.PatchLineType;

/**
 * Covers parsing of diff texts and the conflict checks when applying patches with
 * {@link JsonDiffPatcher}.
 */
@SuppressWarnings("static-method")
class JsonDiffPatcherCoverageTest {
	private static JsonNode json(final String text) throws Exception {
		return JsonReader.readJsonItemString(text);
	}

	private static void patch(final JsonNode root, final String diffText) throws Exception {
		JsonDiffPatcher.applyPatch(root, JsonDiffPatcher.parseDiffText(diffText));
	}

	@Test
	void parseDiffText() throws Exception {
		assertTrue(JsonDiffPatcher.parseDiffText(null).isEmpty());
		assertTrue(JsonDiffPatcher.parseDiffText("\n  \nNo differences found\n").isEmpty());
		assertThrows(PatchConflictException.class, () -> JsonDiffPatcher.parseDiffText("? unknown line"));

		final List<PatchEntry> entries = JsonDiffPatcher.parseDiffText("~ a: \"new\"\r\n+ b: plain\n- c: null\n~ d: \"1\" -> json:\"[1]\"");
		assertEquals(4, entries.size());
		assertTrue(entries.get(0).isIgnoreOldValue());
		assertNull(entries.get(0).getOldValueText());
		assertEquals("new", entries.get(0).getNewValueText());
		assertEquals(PatchLineType.ADDED, entries.get(1).getType());
		assertEquals("plain", entries.get(1).getNewValueText());
		assertEquals(PatchLineType.REMOVED, entries.get(2).getType());
		assertNull(entries.get(2).getOldValueText());
		assertEquals("d", entries.get(3).getPath());
		assertFalse(entries.get(3).isIgnoreOldValue());
	}

	@Test
	void roundtripWithComparator() throws Exception {
		final JsonNode left = json("{\"servers\": [{\"name\": \"a\", \"port\": 80}, {\"name\": \"b\"}], \"obj\": {\"x\": 1}, \"gone\": true, \"key.with.dots\": 1}");
		final JsonNode right = json("{\"servers\": [{\"name\": \"a\", \"port\": 8080}, {\"name\": \"c\", \"tls\": false}, {\"name\": \"d\"}], \"obj\": [1, 2], \"new\": {\"deep\": 1.5}, \"key.with.dots\": 2}");
		final String diff = JsonComparator.renderAsText(new JsonComparator().compare(left, right));
		patch(left, diff);
		assertEquals(right, left);
	}

	@Test
	void addEntries() throws Exception {
		final JsonObject root = (JsonObject) json("{\"list\": [1], \"scalar\": 1}");
		patch(root, "+ a.b.c: \"1\"\n+ list[1]: \"true\"\n+ big: \"123456789012345678901234567890\"\n+ long: \"12345678901\"\n+ dec: \"0.5\"\n+ nothing: null");
		assertEquals(1, ((Number) ((JsonObject) ((JsonObject) root.get("a")).get("b")).getSimpleValue("c")).intValue());
		assertEquals(Boolean.TRUE, ((JsonArray) root.get("list")).getSimpleItem(1));
		assertTrue(root.get("big").isInteger());
		assertTrue(root.get("long").isInteger());
		assertTrue(root.get("dec").isNumber());
		assertTrue(root.get("nothing").isNull());

		assertThrows(PatchConflictException.class, () -> patch(root, "+ list[0]: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "+ scalar: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "+ scalar.sub: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "+ scalar[0]: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "+ list[5].x: \"x\""));
	}

	@Test
	void removeEntries() throws Exception {
		final JsonObject root = (JsonObject) json("{\"list\": [1, {\"o\": 1}], \"s\": \"v\", \"n\": null, \"obj\": {\"k\": 1}, \"nested\": {\"inner\": [5]}}");
		assertThrows(PatchConflictException.class, () -> patch(root, "- s: \"other\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "- s: null"));
		assertThrows(PatchConflictException.class, () -> patch(root, "- n: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "- missing: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "- list[9]: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "- obj: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "- obj: json:\"{\\\"k\\\":2}\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "- missing.sub: \"x\""));

		patch(root, "- s: \"v\"\n- n: null\n- obj: json:\"{\\\"k\\\":1}\"\n- list[1]: json:\"{\\\"o\\\":1}\"\n- nested.inner[0]: \"5\"");
		assertEquals(json("{\"list\": [1], \"nested\": {\"inner\": []}}"), root);
	}

	@Test
	void changeEntries() throws Exception {
		final JsonObject root = (JsonObject) json("{\"list\": [1, 2], \"s\": \"v\"}");
		assertThrows(PatchConflictException.class, () -> patch(root, "~ s: \"wrong\" -> \"new\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "~ missing: \"x\" -> \"y\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "~ list[5]: \"x\" -> \"y\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "~ list[0]: \"9\" -> \"y\""));

		patch(root, "~ s: \"v\" -> \"new\"\n~ list[0]: \"1\" -> \"10\"\n~ list[1]: \"ignored old value\"\n~ s: \"forced\"");
		assertEquals("forced", root.getSimpleValue("s"));
		assertEquals(10, ((Number) ((JsonArray) root.get("list")).getSimpleItem(0)).intValue());
		assertEquals("ignored old value", ((JsonArray) root.get("list")).getSimpleItem(1));
	}

	@Test
	void manualEntries() throws Exception {
		final JsonObject root = new JsonObject().add("a", "1");
		assertThrows(PatchConflictException.class, () -> JsonDiffPatcher.applyPatch(root, List.of(new PatchEntry("", PatchLineType.CHANGED, null, "x"))));
		final PatchEntry entry = new PatchEntry("a", PatchLineType.CHANGED, "1", "2");
		assertEquals("a", entry.getPath());
		assertEquals("1", entry.getOldValueText());
		JsonDiffPatcher.applyPatch(root, List.of(entry));
		assertEquals(2, ((Number) root.getSimpleValue("a")).intValue());
	}
}
