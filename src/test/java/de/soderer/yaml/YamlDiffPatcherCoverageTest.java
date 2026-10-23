package de.soderer.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.soderer.yaml.data.YamlComparator;
import de.soderer.yaml.data.YamlDiffPatcher;
import de.soderer.yaml.data.YamlDiffPatcher.PatchConflictException;
import de.soderer.yaml.data.YamlDiffPatcher.PatchEntry;
import de.soderer.yaml.data.YamlDiffPatcher.PatchLineType;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlSequence;

/**
 * Covers parsing of diff texts and the conflict checks when applying patches with
 * {@link YamlDiffPatcher}.
 */
@SuppressWarnings("static-method")
class YamlDiffPatcherCoverageTest {
	private static YamlNode yaml(final String text) throws Exception {
		return YamlReader.readDocument(text).getRoot();
	}

	private static void patch(final YamlNode root, final String diffText) throws Exception {
		YamlDiffPatcher.applyPatch(root, YamlDiffPatcher.parseDiffText(diffText));
	}

	@Test
	void parseDiffText() throws Exception {
		assertTrue(YamlDiffPatcher.parseDiffText(null).isEmpty());
		assertTrue(YamlDiffPatcher.parseDiffText("\n \nNo differences found\n").isEmpty());
		assertThrows(PatchConflictException.class, () -> YamlDiffPatcher.parseDiffText("? unknown line"));

		final List<PatchEntry> entries = YamlDiffPatcher.parseDiffText("~ a: \"new\"\n+ b: plain\n- c: null\n~ d: \"1\" -> yaml:\"[1]\"");
		assertEquals(4, entries.size());
		assertTrue(entries.get(0).isIgnoreOldValue());
		assertEquals(PatchLineType.ADDED, entries.get(1).getType());
		assertNull(entries.get(2).getOldValueText());
		assertFalse(entries.get(3).isIgnoreOldValue());
	}

	@Test
	void roundtripWithComparator() throws Exception {
		final YamlNode left = yaml("servers:\n  - name: a\n    port: 80\n  - name: b\nobj:\n  x: 1\ngone: true\n\"key.with.dots\": 1\n");
		final YamlNode right = yaml("servers:\n  - name: a\n    port: 8080\n  - name: c\n    tls: false\n  - name: d\nobj: [1, 2]\nnew:\n  deep: 1.5\n\"key.with.dots\": 2\n");
		final String diff = YamlComparator.renderAsText(new YamlComparator().compare(left, right));
		patch(left, diff);
		assertEquals(right, left);
	}

	@Test
	void addEntries() throws Exception {
		final YamlMapping root = (YamlMapping) yaml("list: [1]\nscalar: 1\n");
		patch(root, "+ a.b.c: \"1\"\n+ list[1]: \"true\"\n+ text: \"no number\"\n+ nothing: null\n+ complex: yaml:\"{k: v}\"");
		assertEquals(1, ((Number) ((YamlMapping) ((YamlMapping) root.get("a")).get("b")).getSimpleValue("c")).intValue());
		assertEquals(Boolean.TRUE, ((YamlScalar) ((YamlSequence) root.get("list")).get(1)).getValue());
		assertEquals("no number", root.getSimpleValue("text"));
		assertNull(root.getSimpleValue("nothing"));
		assertTrue(root.get("complex") instanceof YamlMapping);

		assertThrows(PatchConflictException.class, () -> patch(root, "+ list[0]: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "+ scalar: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "+ scalar.sub: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "+ scalar[0]: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "+ list[5].x: \"x\""));
	}

	@Test
	void removeEntries() throws Exception {
		final YamlMapping root = (YamlMapping) yaml("list:\n  - 1\n  - o: 1\ns: v\nempty: null\nobj:\n  k: 1\nnested:\n  inner: [5]\n");
		assertThrows(PatchConflictException.class, () -> patch(root, "- s: \"other\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "- s: null"));
		assertThrows(PatchConflictException.class, () -> patch(root, "- empty: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "- missing: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "- list[9]: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "- obj: \"x\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "- obj: yaml:\"{k: 2}\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "- missing.sub: \"x\""));

		patch(root, "- s: \"v\"\n- empty: null\n- obj: yaml:\"{k: 1}\"\n- list[1]: yaml:\"{o: 1}\"\n- nested.inner[0]: \"5\"");
		assertEquals(2, root.size());
		assertEquals(1, ((YamlSequence) root.get("list")).size());
	}

	@Test
	void changeEntries() throws Exception {
		final YamlMapping root = (YamlMapping) yaml("list: [1, 2]\ns: v\n");
		assertThrows(PatchConflictException.class, () -> patch(root, "~ s: \"wrong\" -> \"new\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "~ missing: \"x\" -> \"z\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "~ list[5]: \"x\" -> \"z\""));
		assertThrows(PatchConflictException.class, () -> patch(root, "~ list[0]: \"9\" -> \"z\""));

		patch(root, "~ s: \"v\" -> \"new\"\n~ list[0]: \"1\" -> \"10\"\n~ list[1]: \"ignored old value\"\n~ s: \"forced\"");
		assertEquals("forced", root.getSimpleValue("s"));
		assertEquals(10, ((Number) ((YamlScalar) ((YamlSequence) root.get("list")).get(0)).getValue()).intValue());
	}

	@Test
	void manualEntries() throws Exception {
		final YamlMapping root = new YamlMapping().add("a", "1");
		assertThrows(PatchConflictException.class, () -> YamlDiffPatcher.applyPatch(root, List.of(new PatchEntry("", PatchLineType.CHANGED, null, "x"))));
		final PatchEntry entry = new PatchEntry("a", PatchLineType.CHANGED, "1", "2");
		assertEquals("a", entry.getPath());
		assertEquals("2", entry.getNewValueText());
		YamlDiffPatcher.applyPatch(root, List.of(entry));
		assertEquals(2, ((Number) root.getSimpleValue("a")).intValue());
	}
}
