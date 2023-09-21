package de.soderer.utilities.json;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class JsonPathTest {
	@Test
	public void test1() throws Exception {
		final JsonPath jsonPath = new JsonPath("$.abc.def[70]");
		Assert.assertEquals("$.abc.def[70]", jsonPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def'][70]", jsonPath.getBracketFormattedPath());
		try {
			jsonPath.getReferenceFormattedPath();
			Assert.fail("Missing expected Exception");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected Exception
		}
	}

	@Test
	public void test2() throws Exception {
		final JsonPath jsonPath = new JsonPath("$.abc.def");
		Assert.assertEquals("$.abc.def", jsonPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def']", jsonPath.getBracketFormattedPath());
		Assert.assertEquals("#/abc/def", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test3() throws Exception {
		final JsonPath jsonPath = new JsonPath("$['abc']['def'][70]");
		Assert.assertEquals("$.abc.def[70]", jsonPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def'][70]", jsonPath.getBracketFormattedPath());
		try {
			jsonPath.getReferenceFormattedPath();
			Assert.fail("Missing expected Exception");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected Exception
		}
	}

	@Test
	public void test4() throws Exception {
		final JsonPath jsonPath = new JsonPath("$['abc']['def']");
		Assert.assertEquals("$.abc.def", jsonPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def']", jsonPath.getBracketFormattedPath());
		Assert.assertEquals("#/abc/def", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test5() throws Exception {
		final JsonPath jsonPath = new JsonPath("#/abc/def");
		Assert.assertEquals("$.abc.def", jsonPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def']", jsonPath.getBracketFormattedPath());
		Assert.assertEquals("#/abc/def", jsonPath.getReferenceFormattedPath());
	}

	@Test
	public void test6() throws Exception {
		final JsonPath jsonPath = new JsonPath().appendPropertyKey("abc").appendPropertyKey("def").appendArrayIndex(70);
		Assert.assertEquals("$.abc.def[70]", jsonPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def'][70]", jsonPath.getBracketFormattedPath());
		try {
			jsonPath.getReferenceFormattedPath();
			Assert.fail("Missing expected Exception");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected Exception
		}
	}

	@Test
	public void test7() throws Exception {
		final JsonPath jsonPath = new JsonPath().appendPropertyKey("abc").appendPropertyKey("def");
		Assert.assertEquals("$.abc.def", jsonPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def']", jsonPath.getBracketFormattedPath());
		Assert.assertEquals("#/abc/def", jsonPath.getReferenceFormattedPath());
	}
}
