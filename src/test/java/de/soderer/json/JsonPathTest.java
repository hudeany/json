package de.soderer.json;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.json.path.JsonPath;

@SuppressWarnings("static-method")
public class JsonPathTest {
	@Test
	public void test1() throws Exception {
		final JsonPath jsonPath = new JsonPath("$.abc.def[70]");
		Assert.assertEquals("$.abc.def[70]", jsonPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def'][70]", jsonPath.getBracketFormattedPath());
		Assert.assertEquals("$/abc/def[70]", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test2() throws Exception {
		final JsonPath jsonPath = new JsonPath("$.abc.def");
		Assert.assertEquals("$.abc.def", jsonPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def']", jsonPath.getBracketFormattedPath());
		Assert.assertEquals("$/abc/def", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test3() throws Exception {
		final JsonPath jsonPath = new JsonPath("$['abc']['def'][70]");
		Assert.assertEquals("$.abc.def[70]", jsonPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def'][70]", jsonPath.getBracketFormattedPath());
		Assert.assertEquals("$/abc/def[70]", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test4() throws Exception {
		final JsonPath jsonPath = new JsonPath("$['abc']['def']");
		Assert.assertEquals("$.abc.def", jsonPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def']", jsonPath.getBracketFormattedPath());
		Assert.assertEquals("$/abc/def", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test5() throws Exception {
		final JsonPath jsonPath = new JsonPath("#/abc/def");
		Assert.assertEquals("#.abc.def", jsonPath.getDotFormattedPath());
		Assert.assertEquals("#['abc']['def']", jsonPath.getBracketFormattedPath());
		Assert.assertEquals("#/abc/def", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test6() throws Exception {
		final JsonPath jsonPath = new JsonPath("$").addPropertyKey("abc").addPropertyKey("def").addArrayIndex(70);
		Assert.assertEquals("$.abc.def[70]", jsonPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def'][70]", jsonPath.getBracketFormattedPath());
		Assert.assertEquals("$/abc/def[70]", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test7() throws Exception {
		final JsonPath jsonPath = new JsonPath("$").addPropertyKey("abc").addPropertyKey("def");
		Assert.assertEquals("$.abc.def", jsonPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def']", jsonPath.getBracketFormattedPath());
		Assert.assertEquals("$/abc/def", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void testJsonPathOnKomplexExample() {
		try (JsonReader jsonReader = new JsonReader(getClass().getClassLoader().getResourceAsStream("json/KomplexExample.json"))) {
			final JsonNode jsonNode = jsonReader.read();
			Assert.assertEquals(3, ((JsonValueInteger) jsonNode.getDataByJsonPath(new JsonPath("$.item3"))).getValue().intValue());
			Assert.assertEquals(true, ((JsonValueBoolean) jsonNode.getDataByJsonPath(new JsonPath("$.item10.item8[1].item6"))).getValue());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}
