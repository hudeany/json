package de.soderer.json;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.soderer.json.path.JsonPath;
import de.soderer.json.path.JsonPathException;
import de.soderer.json.path.JsonPathFilterElement.FilterOperator;

@SuppressWarnings("static-method")
public class JsonPathTest {
	@Test
	public void test1() throws Exception {
		final JsonPath jsonPath = new JsonPath("$.abc.def[70]");
		Assertions.assertEquals("$.abc.def[70]", jsonPath.getDotFormattedPath());
		Assertions.assertEquals("$['abc']['def'][70]", jsonPath.getBracketFormattedPath());
		Assertions.assertEquals("$/abc/def[70]", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test2() throws Exception {
		final JsonPath jsonPath = new JsonPath("$.abc.def");
		Assertions.assertEquals("$.abc.def", jsonPath.getDotFormattedPath());
		Assertions.assertEquals("$['abc']['def']", jsonPath.getBracketFormattedPath());
		Assertions.assertEquals("$/abc/def", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test3() throws Exception {
		final JsonPath jsonPath = new JsonPath("$['abc']['def'][70]");
		Assertions.assertEquals("$.abc.def[70]", jsonPath.getDotFormattedPath());
		Assertions.assertEquals("$['abc']['def'][70]", jsonPath.getBracketFormattedPath());
		Assertions.assertEquals("$/abc/def[70]", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test4() throws Exception {
		final JsonPath jsonPath = new JsonPath("$['abc']['def']");
		Assertions.assertEquals("$.abc.def", jsonPath.getDotFormattedPath());
		Assertions.assertEquals("$['abc']['def']", jsonPath.getBracketFormattedPath());
		Assertions.assertEquals("$/abc/def", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test5() throws Exception {
		final JsonPath jsonPath = new JsonPath("#/abc/def");
		Assertions.assertEquals("#.abc.def", jsonPath.getDotFormattedPath());
		Assertions.assertEquals("#['abc']['def']", jsonPath.getBracketFormattedPath());
		Assertions.assertEquals("#/abc/def", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test6() throws Exception {
		final JsonPath jsonPath = new JsonPath("$").addPropertyKey("abc").addPropertyKey("def").addArrayIndex(70);
		Assertions.assertEquals("$.abc.def[70]", jsonPath.getDotFormattedPath());
		Assertions.assertEquals("$['abc']['def'][70]", jsonPath.getBracketFormattedPath());
		Assertions.assertEquals("$/abc/def[70]", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test7() throws Exception {
		final JsonPath jsonPath = new JsonPath("$").addPropertyKey("abc").addPropertyKey("def");
		Assertions.assertEquals("$.abc.def", jsonPath.getDotFormattedPath());
		Assertions.assertEquals("$['abc']['def']", jsonPath.getBracketFormattedPath());
		Assertions.assertEquals("$/abc/def", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void testJsonPathOnKomplexExample() {
		try (JsonReader jsonReader = new JsonReader(getClass().getClassLoader().getResourceAsStream("json/KomplexExample.json"))) {
			final JsonNode jsonNode = jsonReader.read();
			Assertions.assertEquals(3, ((JsonValueInteger) jsonNode.getDataByJsonPath(new JsonPath("$.item3"))).getValue().intValue());
			Assertions.assertEquals(true, ((JsonValueBoolean) jsonNode.getDataByJsonPath(new JsonPath("$.item10.item8[1].item6"))).getValue());
		} catch (final Exception e) {
			e.printStackTrace();
			Assertions.fail(e.getMessage());
		}
	}

	/**
	 * JSON used by the wildcard/filter tests below, modelled after the "list all download URLs
	 * for a given version" use case that motivated the feature: a top-level object keyed by
	 * application name, most entries having "version"/"downloadUrl", one entry ("Legacy")
	 * deliberately missing "downloadUrl" to exercise the lenient skip-on-miss behaviour once a
	 * wildcard/filter has been evaluated, and a plain "items" array for the array-wildcard tests.
	 */
	private static final String APPLICATIONS_JSON = "{"
			+ "\"MultiEd\": {\"version\": \"26.1.72\", \"downloadUrl\": \"https://example.com/MultiEd.jar\"},"
			+ "\"NetworkChecker\": {\"version\": \"26.0.29\", \"downloadUrl\": \"https://example.com/NetworkChecker.jar\"},"
			+ "\"Fido2Crypt\": {\"version\": \"26.1.72\", \"downloadUrl\": \"https://example.com/Fido2Crypt.jar\"},"
			+ "\"Legacy\": {\"version\": \"26.1.72\"},"
			+ "\"items\": [{\"price\": 5}, {\"price\": 10}, {\"price\": 15}]"
			+ "}";

	@Test
	public void testWildcardDotAndBracketNotationFormatting() throws Exception {
		final JsonPath dotWildcard = new JsonPath("$.*");
		Assertions.assertEquals("$.*", dotWildcard.getDotFormattedPath());
		Assertions.assertEquals("$[*]", dotWildcard.getBracketFormattedPath());
		Assertions.assertEquals("$/*", dotWildcard.getReferenceFormattedPath());

		// Bracket-notation input should format identically, same as the existing
		// dot-vs-bracket-notation round-trip tests above (test3/test4) do for property/array steps
		final JsonPath bracketWildcard = new JsonPath("$[*]");
		Assertions.assertEquals("$.*", bracketWildcard.getDotFormattedPath());
		Assertions.assertEquals("$[*]", bracketWildcard.getBracketFormattedPath());
		Assertions.assertEquals("$/*", bracketWildcard.getReferenceFormattedPath());
	}

	@Test
	public void testWildcardAfterPropertyFormatting() throws Exception {
		final JsonPath jsonPath = new JsonPath("$.items[*]");
		Assertions.assertEquals("$.items.*", jsonPath.getDotFormattedPath());
		Assertions.assertEquals("$['items'][*]", jsonPath.getBracketFormattedPath());
		Assertions.assertEquals("$/items/*", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void testFilterWithNumberLiteralFormatting() throws Exception {
		final JsonPath jsonPath = new JsonPath("$.items[?(@.price<10)]");
		Assertions.assertEquals("$.items[?(@.price < 10)]", jsonPath.getDotFormattedPath());
		Assertions.assertEquals("$['items'][?(@.price < 10)]", jsonPath.getBracketFormattedPath());
		Assertions.assertEquals("$/items[?(@.price < 10)]", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void testFilterWithStringLiteralFormatting() throws Exception {
		final JsonPath jsonPath = new JsonPath("$.*[?(@.version=='26.1.72')]");
		Assertions.assertEquals("$.*[?(@.version == '26.1.72')]", jsonPath.getDotFormattedPath());
		Assertions.assertEquals("$[*][?(@.version == '26.1.72')]", jsonPath.getBracketFormattedPath());
		Assertions.assertEquals("$/*[?(@.version == '26.1.72')]", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void testFluentAddWildcardAndAddFilterMatchParsedPath() throws Exception {
		final JsonPath parsed = new JsonPath("$.items[?(@.price<10)]");
		final JsonPath fluent = new JsonPath("$").addPropertyKey("items").addFilter("price", FilterOperator.LESS_THAN, 10L);
		Assertions.assertEquals(parsed.getDotFormattedPath(), fluent.getDotFormattedPath());
		Assertions.assertEquals(parsed, fluent);
	}

	@Test
	public void testGetDataListByJsonPathWithWildcardAndStringFilter() throws Exception {
		try (JsonReader jsonReader = new JsonReader(new java.io.ByteArrayInputStream(APPLICATIONS_JSON.getBytes(java.nio.charset.StandardCharsets.UTF_8)))) {
			final JsonNode jsonNode = jsonReader.read();

			// "MultiEd" and "Fido2Crypt" have version 26.1.72, "NetworkChecker" does not, "Legacy"
			// does but has no downloadUrl (see APPLICATIONS_JSON) - the filter alone should still
			// match it, only the trailing ".downloadUrl" step drops it (next test)
			final List<JsonNode> matchingApps = jsonNode.getDataListByJsonPath(new JsonPath("$.*[?(@.version=='26.1.72')]"));
			Assertions.assertEquals(3, matchingApps.size());
			for (final JsonNode matchingApp : matchingApps) {
				Assertions.assertEquals("26.1.72", ((JsonObject) matchingApp).getSimpleValue("version"));
			}
		}
	}

	@Test
	public void testGetDataListByJsonPathSkipsCandidatesMissingATrailingProperty() throws Exception {
		try (JsonReader jsonReader = new JsonReader(new java.io.ByteArrayInputStream(APPLICATIONS_JSON.getBytes(java.nio.charset.StandardCharsets.UTF_8)))) {
			final JsonNode jsonNode = jsonReader.read();

			// Same filter as above, but with a trailing ".downloadUrl": "Legacy" matches the
			// version filter but has no "downloadUrl" - once a wildcard/filter has fanned out,
			// that candidate is silently skipped rather than aborting the whole query
			final List<JsonNode> downloadUrls = jsonNode.getDataListByJsonPath(new JsonPath("$.*[?(@.version=='26.1.72')].downloadUrl"));
			Assertions.assertEquals(2, downloadUrls.size());
			for (final JsonNode downloadUrl : downloadUrls) {
				Assertions.assertTrue(((JsonValueString) downloadUrl).getValue().startsWith("https://example.com/"));
			}
		}
	}

	@Test
	public void testGetDataListByJsonPathWildcardOverArrayWithNumericFilterOperators() throws Exception {
		try (JsonReader jsonReader = new JsonReader(new java.io.ByteArrayInputStream(APPLICATIONS_JSON.getBytes(java.nio.charset.StandardCharsets.UTF_8)))) {
			final JsonNode jsonNode = jsonReader.read();

			Assertions.assertEquals(1, jsonNode.getDataListByJsonPath(new JsonPath("$.items[?(@.price<10)]")).size());
			Assertions.assertEquals(2, jsonNode.getDataListByJsonPath(new JsonPath("$.items[?(@.price<=10)]")).size());
			Assertions.assertEquals(1, jsonNode.getDataListByJsonPath(new JsonPath("$.items[?(@.price>10)]")).size());
			Assertions.assertEquals(2, jsonNode.getDataListByJsonPath(new JsonPath("$.items[?(@.price>=10)]")).size());
			Assertions.assertEquals(1, jsonNode.getDataListByJsonPath(new JsonPath("$.items[?(@.price==10)]")).size());
			Assertions.assertEquals(2, jsonNode.getDataListByJsonPath(new JsonPath("$.items[?(@.price!=10)]")).size());

			// Plain wildcard over the array (no filter) returns all three items unchanged
			Assertions.assertEquals(3, jsonNode.getDataListByJsonPath(new JsonPath("$.items.*")).size());
			Assertions.assertEquals(3, jsonNode.getDataListByJsonPath(new JsonPath("$.items[*]")).size());
		}
	}

	@Test
	public void testGetDataListByJsonPathWithNoWildcardOrFilterBehavesLikeSingleResult() throws Exception {
		try (JsonReader jsonReader = new JsonReader(new java.io.ByteArrayInputStream(APPLICATIONS_JSON.getBytes(java.nio.charset.StandardCharsets.UTF_8)))) {
			final JsonNode jsonNode = jsonReader.read();

			final List<JsonNode> result = jsonNode.getDataListByJsonPath(new JsonPath("$.MultiEd.downloadUrl"));
			Assertions.assertEquals(1, result.size());
			Assertions.assertEquals("https://example.com/MultiEd.jar", ((JsonValueString) result.get(0)).getValue());

			// A genuinely wrong literal path still throws (strict behaviour, since no
			// wildcard/filter was involved) - same as getDataByJsonPath would
			Assertions.assertThrows(JsonPathException.class, () -> jsonNode.getDataListByJsonPath(new JsonPath("$.DoesNotExist")));
		}
	}

	@Test
	public void testGetDataByJsonPathRejectsWildcardAndFilter() throws Exception {
		try (JsonReader jsonReader = new JsonReader(new java.io.ByteArrayInputStream(APPLICATIONS_JSON.getBytes(java.nio.charset.StandardCharsets.UTF_8)))) {
			final JsonNode jsonNode = jsonReader.read();

			// The single-result method cannot represent several matches, so it must reject any
			// path containing a wildcard or filter rather than silently picking one match
			Assertions.assertThrows(JsonPathException.class, () -> jsonNode.getDataByJsonPath(new JsonPath("$.*")));
			Assertions.assertThrows(JsonPathException.class, () -> jsonNode.getDataByJsonPath(new JsonPath("$.items[?(@.price<10)]")));
		}
	}
}
