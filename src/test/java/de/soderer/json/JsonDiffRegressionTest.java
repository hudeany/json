package de.soderer.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the diff text format of {@link JsonComparator} and {@link JsonDiffPatcher}.
 */
@SuppressWarnings("static-method")
public class JsonDiffRegressionTest {
	@Test
	public void testSpecialKeysAndComplexValues() throws Exception {
		final JsonNode left = JsonReader.readJsonItemString("{\"a.b\": 1, \"x: y\": \"old\", \"text\": \"line1\\nline2\", \"removed\": {\"deep\": 1}}");
		final JsonNode right = JsonReader.readJsonItemString("{\"a.b\": 2, \"x: y\": \"new -> newer\", \"text\": \"line1\\nchanged\", \"added\": {\"deep\": [1, 2]}}");
		final String diffText = JsonComparator.renderAsText(new JsonComparator().compare(left, right));
		JsonDiffPatcher.applyPatch(left, JsonDiffPatcher.parseDiffText(diffText));
		Assertions.assertEquals(right, left, diffText);
	}

	@Test
	public void testRootArray() throws Exception {
		final JsonNode left = JsonReader.readJsonItemString("[\"a\", \"b\"]");
		final JsonNode right = JsonReader.readJsonItemString("[\"a\", \"c\", \"d\"]");
		JsonDiffPatcher.applyPatch(left, JsonDiffPatcher.parseDiffText(JsonComparator.renderAsText(new JsonComparator().compare(left, right))));
		Assertions.assertEquals(right, left);
	}

	@Test
	public void testSortMixedTypes() throws Exception {
		final JsonArray jsonArray = new JsonArray();
		for (int i = 0; i < 300; i++) {
			if (i % 3 == 0) {
				jsonArray.add(i);
			} else if (i % 3 == 1) {
				jsonArray.add(i + "x");
			} else {
				jsonArray.add(i % 2 == 0);
			}
		}
		jsonArray.sort(true);
		Assertions.assertEquals(300, jsonArray.size());
	}
}
