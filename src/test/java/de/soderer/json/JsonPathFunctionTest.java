package de.soderer.json;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.soderer.json.path.JsonPath;
import de.soderer.json.path.JsonPathFilterElement;
import de.soderer.json.path.JsonPathFilterExpression;
import de.soderer.json.path.JsonPathFilterExpression.Function;
import de.soderer.json.path.JsonPathFilterExpression.FunctionCall;
import de.soderer.json.path.JsonPathFilterExpression.FunctionTest;
import de.soderer.json.path.JsonPathFilterExpression.Literal;
import de.soderer.json.path.JsonPathFilterExpression.PathOperand;
import de.soderer.json.schema.JsonSchemaDefinitionError;

/**
 * Tests of the JSON path filter functions of RFC 9535: length(), count(), value(), match() and
 * search() with I-Regexp (RFC 9485).
 */
@SuppressWarnings("static-method")
public class JsonPathFunctionTest {
	private static final String DATA_JSON = "{"
			+ "\"items\": ["
			+ "  {\"name\": \"nexInsure\", \"version\": \"25.4.28\", \"tags\": [\"core\", \"insurance\"], \"meta\": {\"a\": 1, \"b\": 2}, \"parts\": {\"x\": {\"color\": \"red\"}}},"
			+ "  {\"name\": \"msg Business Integration Gateway\", \"version\": \"CSS-264.0.0-22\", \"tags\": [\"gateway\"], \"meta\": {}, \"parts\": {\"x\": {\"color\": \"blue\"}, \"y\": {\"color\": \"red\"}}},"
			+ "  {\"name\": \"Ölmühle 😀\", \"version\": \"3.1\", \"tags\": [], \"meta\": {\"a\": 1}},"
			+ "  {\"name\": \"line\\nbreak\", \"version\": 7}"
			+ "]"
			+ "}";

	private static List<String> names(final String path) throws Exception {
		final List<String> names = new ArrayList<>();
		for (final JsonNode node : JsonReader.readJsonItemString(DATA_JSON).getDataListByJsonPath(new JsonPath(path))) {
			names.add(((JsonValueString) ((JsonObject) node).get("name")).getValue());
		}
		return names;
	}

	@Test
	public void testLength() throws Exception {
		// Strings: number of code points ("Ölmühle 😀" has 9, the emoji is one code point)
		Assertions.assertEquals(List.of("nexInsure", "Ölmühle \uD83D\uDE00"), names("$.items[?(length(@.name) == 9)]"));
		Assertions.assertEquals(List.of("Ölmühle 😀"), names("$.items[?(length(@.name) == 9 && @.version == '3.1')]"));
		Assertions.assertEquals(List.of("msg Business Integration Gateway"), names("$.items[?(length(@.name) > 20)]"));
		// Arrays and objects
		Assertions.assertEquals(List.of("nexInsure"), names("$.items[?(length(@.tags) == 2)]"));
		Assertions.assertEquals(List.of("Ölmühle 😀"), names("$.items[?(length(@.tags) == 0)]"));
		Assertions.assertEquals(List.of("nexInsure"), names("$.items[?(length(@.meta) >= 2)]"));
		// Numbers and missing values have no length
		Assertions.assertEquals(List.of(), names("$.items[?(length(@.version) == 1)]"));
		Assertions.assertEquals(List.of("line\nbreak"), names("$.items[?(length(@.tags) == length(@.missing))]"));
		// Literal argument
		Assertions.assertEquals(4, JsonReader.readJsonItemString(DATA_JSON).getDataListByJsonPath(new JsonPath("$.items[?(length('abc') == 3)]")).size());
	}

	@Test
	public void testCount() throws Exception {
		Assertions.assertEquals(List.of("nexInsure"), names("$.items[?(count(@.tags[*]) == 2)]"));
		Assertions.assertEquals(List.of("msg Business Integration Gateway"), names("$.items[?(count(@.parts.*) > 1)]"));
		Assertions.assertEquals(List.of("msg Business Integration Gateway"), names("$.items[?(count(@..color) == 2)]"));
		Assertions.assertEquals(List.of("Ölmühle 😀", "line\nbreak"), names("$.items[?(count(@.parts) == 0)]"));
		Assertions.assertEquals(4, JsonReader.readJsonItemString(DATA_JSON).getDataListByJsonPath(new JsonPath("$.items[?(count($.items[*]) == 4)]")).size());
	}

	@Test
	public void testValue() throws Exception {
		// Exactly one node: its value
		Assertions.assertEquals(List.of("nexInsure"), names("$.items[?(value(@..color) == 'red')]"));
		// Several nodes: "nothing"
		Assertions.assertEquals(List.of(), names("$.items[?(value(@..color) == 'blue')]"));
		Assertions.assertEquals(List.of("nexInsure"), names("$.items[?(value(@.tags[0]) == 'core')]"));
		// Nested: value() as argument of length()
		Assertions.assertEquals(List.of("nexInsure"), names("$.items[?(length(value(@..color)) == 3)]"));
	}

	@Test
	public void testMatchAndSearch() throws Exception {
		Assertions.assertEquals(List.of("nexInsure"), names("$.items[?(match(@.version, '2[0-9]\\\\..*'))]"));
		Assertions.assertEquals(List.of("nexInsure", "msg Business Integration Gateway", "line\nbreak"), names("$.items[?(search(@.name, '[Ii]n'))]"));
		Assertions.assertEquals(List.of("nexInsure", "msg Business Integration Gateway"), names("$.items[?(search(@.name, '[Ii]n[a-z]{2}'))]"));
		// match() needs the whole string
		Assertions.assertEquals(List.of(), names("$.items[?(match(@.name, 'nex'))]"));
		Assertions.assertEquals(List.of("nexInsure"), names("$.items[?(match(@.name, 'nex.*'))]"));
		// Negation and combination
		Assertions.assertEquals(List.of("msg Business Integration Gateway", "Ölmühle 😀", "line\nbreak"), names("$.items[?(!match(@.name, 'nex.*'))]"));
		Assertions.assertEquals(List.of("nexInsure"), names("$.items[?(search(@.name, 'nex') && length(@.tags) == 2)]"));
		// Unicode categories and "." without line breaks
		Assertions.assertEquals(List.of("Ölmühle 😀"), names("$.items[?(match(@.name, '\\\\p{Lu}\\\\p{Ll}+ .'))]"));
		Assertions.assertEquals(List.of("nexInsure", "msg Business Integration Gateway", "Ölmühle 😀"), names("$.items[?(match(@.name, '.*'))]"));
		// Non-string values and invalid I-Regexps never match
		Assertions.assertEquals(List.of(), names("$.items[?(match(@.version, '7'))]"));
		Assertions.assertEquals(List.of(), names("$.items[?(match(@.name, 'nex.*?'))]"));
		Assertions.assertEquals(List.of(), names("$.items[?(search(@.name, '\\\\d'))]"));
		// Regular expression from the data
		final String patternJson = "{\"pattern\": \"[0-9]+\\\\.[0-9]+\", \"items\": [{\"v\": \"3.1\"}, {\"v\": \"x\"}]}";
		Assertions.assertEquals(1, JsonReader.readJsonItemString(patternJson).getDataListByJsonPath(new JsonPath("$.items[?(match(@.v, $.pattern))]")).size());
	}

	@Test
	public void testCatastrophicBacktrackingIsLimited() throws Exception {
		final String json = "{\"items\": [{\"s\": \"" + "a".repeat(5000) + "b\"}]}";
		final long start = System.currentTimeMillis();
		Assertions.assertEquals(0, JsonReader.readJsonItemString(json).getDataListByJsonPath(new JsonPath("$.items[?(match(@.s, '(a|aa)+c'))]")).size());
		Assertions.assertTrue(System.currentTimeMillis() - start < 20000, "Evaluation must be limited");
	}

	@Test
	public void testFormatting() throws Exception {
		assertFormat("$.a[?(length(@.b)>3)]", "$.a[?(length(@.b) > 3)]");
		assertFormat("$.a[?count(@.b[*])==2]", "$.a[?(count(@.b.*) == 2)]");
		assertFormat("$.a[?(match(@.v,'2.*')&&!search(@.n,\"x\"))]", "$.a[?(match(@.v, '2.*') && !search(@.n, 'x'))]");
		assertFormat("$.a[?(length(value(@..c))==3)]", "$.a[?(length(value(@..c)) == 3)]");
		assertFormat("$.a[?(@.length == 3)]", "$.a[?(@.length == 3)]");
	}

	private static void assertFormat(final String input, final String expected) throws Exception {
		final JsonPath jsonPath = new JsonPath(input);
		Assertions.assertEquals(expected, jsonPath.getDotFormattedPath(), "Format of " + input);
		Assertions.assertEquals(jsonPath, new JsonPath(jsonPath.getDotFormattedPath()), "Reparsed " + input);
	}

	@Test
	public void testTypeChecks() {
		for (final String invalidPath : new String[] {
				// Unknown function, wrong number of arguments
				"$.a[?(foo(@.b))]",
				"$.a[?(length(@.b, @.c) == 1)]",
				"$.a[?(match(@.b) )]",
				"$.a[?(count() == 1)]",
				// count() and value() need a path
				"$.a[?(count('x') == 1)]",
				"$.a[?(value(1) == 1)]",
				// Value results must be compared, logical results must not be compared
				"$.a[?(length(@.b))]",
				"$.a[?(match(@.b, 'x') == true)]",
				"$.a[?(length(match(@.b, 'x')) == 1)]",
				// Missing closing parenthesis
				"$.a[?(length(@.b == 1)]" }) {
			Assertions.assertThrows(JsonSchemaDefinitionError.class, () -> new JsonPath(invalidPath), "Invalid path " + invalidPath);
		}
	}

	@Test
	public void testProgrammaticConstruction() throws Exception {
		final JsonPathFilterExpression expression = new FunctionTest(new FunctionCall(Function.MATCH, List.of(PathOperand.ofProperty("version"), new Literal("2[0-9]\\..*"))));
		final JsonPath jsonPath = new JsonPath().addPropertyKey("items").add(new JsonPathFilterElement(expression)).addPropertyKey("name");
		Assertions.assertEquals("$.items[?(match(@.version, '2[0-9]\\\\..*'))].name", jsonPath.getDotFormattedPath());
		Assertions.assertEquals(1, JsonReader.readJsonItemString(DATA_JSON).getDataListByJsonPath(jsonPath).size());
		Assertions.assertThrows(IllegalArgumentException.class, () -> new FunctionTest(new FunctionCall(Function.LENGTH, List.of(PathOperand.ofProperty("a")))));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new FunctionCall(Function.COUNT, List.of(new Literal("x"))));
	}
}
