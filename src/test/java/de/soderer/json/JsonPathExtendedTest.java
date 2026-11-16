package de.soderer.json;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.soderer.json.path.JsonPath;
import de.soderer.json.path.JsonPathException;
import de.soderer.json.path.JsonPathFilterElement;
import de.soderer.json.path.JsonPathFilterElement.FilterOperator;
import de.soderer.json.schema.JsonSchemaDefinitionError;

/**
 * Tests of the extended JSON path syntax: recursive descent, negative indexes, slices, unions and
 * filter expressions with nested paths, logical operators and RFC 9535 comparison semantics.
 */
@SuppressWarnings("static-method")
public class JsonPathExtendedTest {
	/**
	 * The well-known bookstore example of Stefan Goessner's JSONPath article (also used by RFC 9535).
	 */
	private static final String BOOKSTORE_JSON = "{"
			+ "\"store\": {"
			+ "  \"book\": ["
			+ "    {\"category\": \"reference\", \"author\": \"Nigel Rees\", \"title\": \"Sayings of the Century\", \"price\": 8.95},"
			+ "    {\"category\": \"fiction\", \"author\": \"Evelyn Waugh\", \"title\": \"Sword of Honour\", \"price\": 12.99},"
			+ "    {\"category\": \"fiction\", \"author\": \"Herman Melville\", \"title\": \"Moby Dick\", \"isbn\": \"0-553-21311-3\", \"price\": 8.99},"
			+ "    {\"category\": \"fiction\", \"author\": \"J. R. R. Tolkien\", \"title\": \"The Lord of the Rings\", \"isbn\": \"0-395-19395-8\", \"price\": 22.99}"
			+ "  ],"
			+ "  \"bicycle\": {\"color\": \"red\", \"price\": 399}"
			+ "},"
			+ "\"expensive\": 10"
			+ "}";

	private static JsonNode read(final String json) throws Exception {
		return JsonReader.readJsonItemString(json);
	}

	private static List<JsonNode> query(final String json, final String path) throws Exception {
		return read(json).getDataListByJsonPath(new JsonPath(path));
	}

	private static List<String> queryStrings(final String json, final String path) throws Exception {
		final List<String> values = new ArrayList<>();
		for (final JsonNode node : query(json, path)) {
			values.add(node instanceof JsonValueString ? ((JsonValueString) node).getValue() : node.toString());
		}
		return values;
	}

	@Test
	public void testGoessnerBookstoreQueries() throws Exception {
		Assertions.assertEquals(List.of("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien"), queryStrings(BOOKSTORE_JSON, "$.store.book[*].author"));
		Assertions.assertEquals(List.of("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien"), queryStrings(BOOKSTORE_JSON, "$..author"));
		Assertions.assertEquals(2, query(BOOKSTORE_JSON, "$.store.*").size());
		Assertions.assertEquals(5, query(BOOKSTORE_JSON, "$.store..price").size());
		Assertions.assertEquals(List.of("Moby Dick"), queryStrings(BOOKSTORE_JSON, "$..book[2].title"));
		Assertions.assertEquals(List.of("The Lord of the Rings"), queryStrings(BOOKSTORE_JSON, "$..book[-1].title"));
		Assertions.assertEquals(List.of("Sayings of the Century", "Sword of Honour"), queryStrings(BOOKSTORE_JSON, "$..book[0,1].title"));
		Assertions.assertEquals(List.of("Sayings of the Century", "Sword of Honour"), queryStrings(BOOKSTORE_JSON, "$..book[:2].title"));
		Assertions.assertEquals(List.of("Moby Dick", "The Lord of the Rings"), queryStrings(BOOKSTORE_JSON, "$..book[?(@.isbn)].title"));
		Assertions.assertEquals(List.of("Sayings of the Century", "Moby Dick"), queryStrings(BOOKSTORE_JSON, "$..book[?(@.price<10)].title"));
		Assertions.assertEquals(List.of("Sayings of the Century", "Moby Dick"), queryStrings(BOOKSTORE_JSON, "$..book[?(@.price < $.expensive)].title"));
		// All members and items of the document: 2 + 2 + 4 + 18 + 2
		Assertions.assertEquals(28, query(BOOKSTORE_JSON, "$..*").size());
	}

	@Test
	public void testFilterAfterRecursiveDescentSelectsChildrenWithoutDuplicates() throws Exception {
		// RFC 9535: the filter selects the matching children of every node, so each book is found once
		Assertions.assertEquals(List.of("Sayings of the Century", "Moby Dick"), queryStrings(BOOKSTORE_JSON, "$..[?(@.price < 10)].title"));
	}

	@Test
	public void testLogicalOperatorsAndParentheses() throws Exception {
		Assertions.assertEquals(List.of("Sword of Honour", "Moby Dick"), queryStrings(BOOKSTORE_JSON, "$.store.book[?(@.category == 'fiction' && @.price < 15)].title"));
		Assertions.assertEquals(List.of("Sayings of the Century", "Moby Dick", "The Lord of the Rings"), queryStrings(BOOKSTORE_JSON, "$.store.book[?(@.price < 9 || @.price > 20)].title"));
		Assertions.assertEquals(List.of("Sayings of the Century", "Sword of Honour"), queryStrings(BOOKSTORE_JSON, "$.store.book[?(!@.isbn)].title"));
		Assertions.assertEquals(List.of("Sword of Honour", "The Lord of the Rings"), queryStrings(BOOKSTORE_JSON, "$.store.book[?(!(@.price < 10))].title"));
		Assertions.assertEquals(List.of("Moby Dick"), queryStrings(BOOKSTORE_JSON, "$.store.book[?(@.category == 'fiction' && (@.price < 10 || @.price > 100))].title"));
		// "&&" binds stronger than "||"
		Assertions.assertEquals(List.of("Sayings of the Century", "Moby Dick"), queryStrings(BOOKSTORE_JSON, "$.store.book[?(@.category == 'reference' || @.isbn && @.price < 10)].title"));
	}

	@Test
	public void testFilterSyntaxVariants() throws Exception {
		// RFC 9535 form without parentheses
		Assertions.assertEquals(2, query(BOOKSTORE_JSON, "$.store.book[?@.price<10]").size());
		// Double quoted string literal
		Assertions.assertEquals(1, query(BOOKSTORE_JSON, "$.store.book[?(@.category == \"reference\")]").size());
		// Bracket notation within the filter path
		Assertions.assertEquals(1, query(BOOKSTORE_JSON, "$.store.book[?(@['category'] == 'reference')]").size());
		// String ordering by code points
		Assertions.assertEquals(List.of("Nigel Rees", "J. R. R. Tolkien"), queryStrings(BOOKSTORE_JSON, "$.store.book[?(@.author > 'I')].author"));
		// Literal containing characters with a meaning in paths
		final String json = "{\"items\": [{\"title\": \"a]b)c'd && x\"}, {\"title\": \"other\"}]}";
		Assertions.assertEquals(1, query(json, "$.items[?(@.title == 'a]b)c\\'d && x')]").size());
	}

	@Test
	public void testNestedPathsInFilter() throws Exception {
		final String json = "{\"users\": ["
				+ "{\"name\": \"a\", \"address\": {\"city\": \"Berlin\"}, \"tags\": [\"x\", \"y\"]},"
				+ "{\"name\": \"b\", \"address\": {\"city\": \"Munich\"}, \"tags\": [\"y\"]}"
				+ "]}";
		Assertions.assertEquals(List.of("a"), queryStrings(json, "$.users[?(@.address.city == 'Berlin')].name"));
		Assertions.assertEquals(List.of("a"), queryStrings(json, "$.users[?(@.tags[0] == 'x')].name"));
		Assertions.assertEquals(List.of("a", "b"), queryStrings(json, "$.users[?(@.tags[-1] == 'y')].name"));
		// Nested filter: users with any tag "x"
		Assertions.assertEquals(List.of("a"), queryStrings(json, "$.users[?(@.tags[?(@ == 'x')])].name"));
	}

	@Test
	public void testVersionDataExample() throws Exception {
		final String json = "{\"versionData\": ["
				+ "{\"uid\": \"nexinsure\", \"name\": \"nexInsure\", \"version\": \"25.4.28\", \"expectedVersion\": null, \"dependencies\": []},"
				+ "{\"uid\": \"big\", \"name\": \"msg Business Integration Gateway\", \"version\": \"CSS-264.0.0-22\", \"expectedVersion\": null, \"dependencies\": []}"
				+ "]}";
		Assertions.assertEquals(List.of("25.4.28"), queryStrings(json, "$.versionData[?(@.name=='nexInsure')].version"));
		Assertions.assertEquals(List.of("CSS-264.0.0-22"), queryStrings(json, "$.versionData[?(@.name == 'msg Business Integration Gateway')].version"));
		Assertions.assertEquals(List.of("25.4.28", "CSS-264.0.0-22"), queryStrings(json, "$..version"));
		// An index after the field name is applied to the string value, so nothing matches
		Assertions.assertEquals(0, query(json, "$.versionData[?(@.name=='nexInsure')].version[0]").size());
	}

	@Test
	public void testRfcComparisonSemantics() throws Exception {
		// A missing value is "nothing": not equal to null, but unequal to any value
		Assertions.assertEquals(List.of("Sayings of the Century", "Sword of Honour", "The Lord of the Rings"), queryStrings(BOOKSTORE_JSON, "$..book[?(@.isbn != '0-553-21311-3')].title"));

		final String nullJson = "{\"items\": [{\"a\": null}, {\"b\": 1}]}";
		Assertions.assertEquals(1, query(nullJson, "$.items[?(@.a == null)]").size());
		// Existence test is also true for a JSON null value
		Assertions.assertEquals(1, query(nullJson, "$.items[?(@.a)]").size());

		// Numbers by value
		final String numberJson = "{\"items\": [{\"v\": 1}, {\"v\": 1.0}, {\"v\": 2}, {\"v\": 12345678901234567890}]}";
		Assertions.assertEquals(2, query(numberJson, "$.items[?(@.v == 1)]").size());
		Assertions.assertEquals(2, query(numberJson, "$.items[?(@.v <= 1.0)]").size());
		Assertions.assertEquals(1, query(numberJson, "$.items[?(@.v == 12345678901234567890)]").size());
		Assertions.assertEquals(1, query(numberJson, "$.items[?(@.v > 1e10)]").size());

		// Ordering is only defined for numbers and strings
		Assertions.assertEquals(0, query(numberJson, "$.items[?(@.v < 'x')]").size());

		// Structural equality of objects and arrays (numbers by value)
		final String structureJson = "{\"items\": [{\"p\": {\"x\": 1, \"y\": [1, 2]}}, {\"p\": {\"x\": 2}}], \"ref\": {\"y\": [1.0, 2], \"x\": 1}}";
		Assertions.assertEquals(1, query(structureJson, "$.items[?(@.p == $.ref)]").size());

		// Booleans
		final String booleanJson = "{\"items\": [{\"active\": true}, {\"active\": false}, {}]}";
		Assertions.assertEquals(1, query(booleanJson, "$.items[?(@.active == true)]").size());
		Assertions.assertEquals(2, query(booleanJson, "$.items[?(@.active != true)]").size());
	}

	@Test
	public void testSlicesAndUnions() throws Exception {
		final String json = "{\"a\": [0, 1, 2, 3, 4, 5, 6, 7, 8, 9], \"o\": {\"a\": 1, \"b\": 2, \"c\": 3}}";
		Assertions.assertEquals(List.of("1", "2"), queryStrings(json, "$.a[1:3]"));
		Assertions.assertEquals(List.of("8", "9"), queryStrings(json, "$.a[-2:]"));
		Assertions.assertEquals(List.of("0", "3", "6", "9"), queryStrings(json, "$.a[::3]"));
		Assertions.assertEquals(List.of("9", "8", "7", "6", "5", "4", "3", "2", "1", "0"), queryStrings(json, "$.a[::-1]"));
		Assertions.assertEquals(List.of("5", "3"), queryStrings(json, "$.a[5:1:-2]"));
		Assertions.assertEquals(List.of(), queryStrings(json, "$.a[3:1]"));
		Assertions.assertEquals(List.of(), queryStrings(json, "$.a[::0]"));
		Assertions.assertEquals(List.of("2", "0", "9"), queryStrings(json, "$.a[2,0,-1]"));
		Assertions.assertEquals(List.of("0", "1", "8"), queryStrings(json, "$.a[:2, 8]"));
		Assertions.assertEquals(List.of("2", "1"), queryStrings(json, "$.o['b','a']"));
		Assertions.assertEquals(List.of("3"), queryStrings(json, "$.o[\"c\"]"));
	}

	@Test
	public void testGetDataByJsonPathWithNegativeIndex() throws Exception {
		final JsonNode jsonNode = read(BOOKSTORE_JSON);
		Assertions.assertEquals("The Lord of the Rings", ((JsonValueString) jsonNode.getDataByJsonPath(new JsonPath("$.store.book[-1].title"))).getValue());
		Assertions.assertEquals("Sayings of the Century", ((JsonValueString) jsonNode.getDataByJsonPath(new JsonPath("$.store.book[-4].title"))).getValue());
		Assertions.assertThrows(JsonPathException.class, () -> jsonNode.getDataByJsonPath(new JsonPath("$.store.book[-5]")));
		Assertions.assertThrows(JsonPathException.class, () -> jsonNode.getDataByJsonPath(new JsonPath("$..book")));
		Assertions.assertThrows(JsonPathException.class, () -> jsonNode.getDataByJsonPath(new JsonPath("$.store.book[0:2]")));
	}

	@Test
	public void testStrictUntilFirstFanOut() throws Exception {
		final JsonNode jsonNode = read(BOOKSTORE_JSON);
		Assertions.assertThrows(JsonPathException.class, () -> jsonNode.getDataListByJsonPath(new JsonPath("$.missing[*]")));
		Assertions.assertThrows(JsonPathException.class, () -> jsonNode.getDataListByJsonPath(new JsonPath("$.store.book[-5].title")));
		// After the fan-out missing parts are skipped
		Assertions.assertEquals(2, jsonNode.getDataListByJsonPath(new JsonPath("$.store.book[*].isbn")).size());
		Assertions.assertEquals(0, jsonNode.getDataListByJsonPath(new JsonPath("$..missing")).size());
		Assertions.assertEquals(0, jsonNode.getDataListByJsonPath(new JsonPath("$.store.book[10:20]")).size());
	}

	@Test
	public void testFormattingAndReparsing() throws Exception {
		assertFormats("$..book[?(@.price<10)]", "$..book[?(@.price < 10)]", "$..['book'][?(@.price < 10)]");
		assertFormats("$..*", "$..*", "$..[*]");
		assertFormats("$..[0]", "$..[0]", "$..[0]");
		assertFormats("$.store.book[-1]", "$.store.book[-1]", "$['store']['book'][-1]");
		assertFormats("$.a[1:3]", "$.a[1:3]", "$['a'][1:3]");
		assertFormats("$.a[::-1]", "$.a[::-1]", "$['a'][::-1]");
		assertFormats("$.a[ 2 , 0 ]", "$.a[2,0]", "$['a'][2,0]");
		assertFormats("$[\"a\",'b']", "$['a','b']", "$['a','b']");
		assertFormats("$..book[?(@.author.name=='x'&&(@.price<10||!@.isbn))]", "$..book[?(@.author.name == 'x' && (@.price < 10 || !@.isbn))]", null);
		assertFormats("$.a[?(!(@.price<10))]", "$.a[?(!(@.price < 10))]", null);
		assertFormats("$.a[?@.b<$.c.d]", "$.a[?(@.b < $.c.d)]", null);
		assertFormats("$.a[?(@['x y']=='it\\'s')]", "$.a[?(@['x y'] == 'it\\'s')]", null);
		assertFormats("$.a[?(@.tags[?(@=='x')])]", "$.a[?(@.tags[?(@ == 'x')])]", null);
		assertFormats("$.a[?(@..price > 1)]", "$.a[?(@..price > 1)]", null);
		Assertions.assertEquals("$/../book", new JsonPath("$..book").getReferenceFormattedPath());
	}

	private static void assertFormats(final String input, final String expectedDotFormat, final String expectedBracketFormat) throws Exception {
		final JsonPath jsonPath = new JsonPath(input);
		Assertions.assertEquals(expectedDotFormat, jsonPath.getDotFormattedPath(), "Dot format of " + input);
		if (expectedBracketFormat != null) {
			Assertions.assertEquals(expectedBracketFormat, jsonPath.getBracketFormattedPath(), "Bracket format of " + input);
			Assertions.assertEquals(jsonPath, new JsonPath(jsonPath.getBracketFormattedPath()), "Reparsed bracket format of " + input);
		}
		Assertions.assertEquals(jsonPath, new JsonPath(jsonPath.getDotFormattedPath()), "Reparsed dot format of " + input);
	}

	@Test
	public void testPropertyKeysWithSpecialCharactersRoundTrip() throws Exception {
		for (final String key : new String[] { "a.b", "a/b", "a[0]", "a\\b", "it's", "a b", "ä€", "", "*", "*x", " a ", "..", "a\tb" }) {
			final JsonPath jsonPath = new JsonPath().addPropertyKey(key).addArrayIndex(1);
			Assertions.assertEquals(jsonPath, new JsonPath(jsonPath.getDotFormattedPath()), "Dot format of key " + key);
			Assertions.assertEquals(jsonPath, new JsonPath(jsonPath.getBracketFormattedPath()), "Bracket format of key " + key);
			final JsonPath recursivePath = new JsonPath().addRecursiveDescent().addPropertyKey(key);
			Assertions.assertEquals(recursivePath, new JsonPath(recursivePath.getDotFormattedPath()), "Dot format of recursive key " + key);
			Assertions.assertEquals(recursivePath, new JsonPath(recursivePath.getBracketFormattedPath()), "Bracket format of recursive key " + key);
			final JsonObject data = new JsonObject().add(key, new JsonArray().add("x").add("y"));
			data.setRootNode(true);
			Assertions.assertEquals("y", ((JsonValueString) data.getDataByJsonPath(new JsonPath(jsonPath.getDotFormattedPath()))).getValue(), "Data of key " + key);
		}
		Assertions.assertEquals("$.a\\.b", new JsonPath().addPropertyKey("a.b").getDotFormattedPath());
		// JSON pointer escapes of references stay supported
		Assertions.assertEquals("#.a/b.c~d", new JsonPath("#/a~1b/c~0d").getDotFormattedPath().replace("\\", ""));
	}

	@Test
	public void testSimpleFilterGettersAndFluentApi() throws Exception {
		final JsonPathFilterElement simpleFilter = (JsonPathFilterElement) new JsonPath("$.items[?(@.price<10)]").getLastPathPart();
		Assertions.assertTrue(simpleFilter.isSimpleComparison());
		Assertions.assertEquals("price", simpleFilter.getPropertyName());
		Assertions.assertEquals(FilterOperator.LESS_THAN, simpleFilter.getOperator());
		Assertions.assertEquals(10L, simpleFilter.getLiteralValue());

		final JsonPathFilterElement complexFilter = (JsonPathFilterElement) new JsonPath("$.items[?(@.a.b<10)]").getLastPathPart();
		Assertions.assertFalse(complexFilter.isSimpleComparison());
		Assertions.assertNull(complexFilter.getPropertyName());
		Assertions.assertNull(complexFilter.getOperator());

		final JsonPath fluent = new JsonPath().addPropertyKey("store").addRecursiveDescent().addPropertyKey("price");
		Assertions.assertEquals(new JsonPath("$.store..price"), fluent);
		Assertions.assertEquals(5, read(BOOKSTORE_JSON).getDataListByJsonPath(fluent).size());

		final JsonPath fluentFilter = new JsonPath().addPropertyKey("store").addPropertyKey("book").addFilter("@.isbn && @.price > 20");
		Assertions.assertEquals("$.store.book[?(@.isbn && @.price > 20)]", fluentFilter.getDotFormattedPath());
		Assertions.assertEquals(1, read(BOOKSTORE_JSON).getDataListByJsonPath(fluentFilter).size());

		Assertions.assertEquals("$.a[-1][1:3:2]", new JsonPath().addPropertyKey("a").addArrayIndex(-1).addSlice(1, 3, 2).getDotFormattedPath());

		Assertions.assertFalse(new JsonPath("$.a.b[2]").canMatchSeveralNodes());
		Assertions.assertFalse(new JsonPath("$.a[-1]").canMatchSeveralNodes());
		Assertions.assertTrue(new JsonPath("$..a").canMatchSeveralNodes());
		Assertions.assertTrue(new JsonPath("$.a[0,1]").canMatchSeveralNodes());
		Assertions.assertTrue(new JsonPath("$.a[?(@.b)]").canMatchSeveralNodes());
	}

	@Test
	public void testInvalidPaths() {
		for (final String invalidPath : new String[] {
				"$.a[?(@.x==]",
				"$.a[?(@.x == 'unterminated)]",
				"$.a[1:x]",
				"$.a['unterminated]",
				"$..",
				"$.a[?(@.x == 'a' &&)]",
				"$.a[?(@.x == 1)",
				"$.a[?('literal')]",
				"$.a[?(@.x === 1)]",
				"$.a[]",
				"$.a[99999999999]",
				"$.a b c[",
				"abc" }) {
			Assertions.assertThrows(JsonSchemaDefinitionError.class, () -> new JsonPath(invalidPath), "Invalid path " + invalidPath);
		}
		Assertions.assertThrows(JsonSchemaDefinitionError.class, () -> new JsonPath().addFilter("@.a =="));
	}

	@Test
	public void testFilterOnArrayOfSimpleValues() throws Exception {
		final String json = "{\"numbers\": [1, 5, 10, 15], \"words\": [\"apple\", \"banana\", \"cherry\"]}";
		Assertions.assertEquals(List.of("10", "15"), queryStrings(json, "$.numbers[?(@ >= 10)]"));
		Assertions.assertEquals(List.of("banana", "cherry"), queryStrings(json, "$.words[?(@ > 'b')]"));
	}
}
