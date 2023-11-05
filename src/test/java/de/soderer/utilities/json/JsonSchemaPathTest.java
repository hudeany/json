package de.soderer.utilities.json;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.json.schema.JsonSchemaPath;

@SuppressWarnings("static-method")
public class JsonSchemaPathTest {
	@Test
	public void test1() throws Exception {
		final JsonSchemaPath jsonSchemaPath = new JsonSchemaPath("$.abc.def[70]");
		Assert.assertEquals("$.abc.def[70]", jsonSchemaPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def'][70]", jsonSchemaPath.getBracketFormattedPath());
		Assert.assertEquals("$/abc/def[70]", jsonSchemaPath.getReferenceFormattedPath());
	}

	@Test
	public void test2() throws Exception {
		final JsonSchemaPath jsonSchemaPath = new JsonSchemaPath("$.abc.def");
		Assert.assertEquals("$.abc.def", jsonSchemaPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def']", jsonSchemaPath.getBracketFormattedPath());
		Assert.assertEquals("$/abc/def", jsonSchemaPath.getReferenceFormattedPath());
	}

	@Test
	public void test3() throws Exception {
		final JsonSchemaPath jsonSchemaPath = new JsonSchemaPath("$['abc']['def'][70]");
		Assert.assertEquals("$.abc.def[70]", jsonSchemaPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def'][70]", jsonSchemaPath.getBracketFormattedPath());
		Assert.assertEquals("$/abc/def[70]", jsonSchemaPath.getReferenceFormattedPath());
	}

	@Test
	public void test4() throws Exception {
		final JsonSchemaPath jsonSchemaPath = new JsonSchemaPath("$['abc']['def']");
		Assert.assertEquals("$.abc.def", jsonSchemaPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def']", jsonSchemaPath.getBracketFormattedPath());
		Assert.assertEquals("$/abc/def", jsonSchemaPath.getReferenceFormattedPath());
	}

	@Test
	public void test5() throws Exception {
		final JsonSchemaPath jsonSchemaPath = new JsonSchemaPath("#/abc/def");
		Assert.assertEquals("#/abc/def", jsonSchemaPath.getDotFormattedPath());
		Assert.assertEquals("#/abc/def", jsonSchemaPath.getBracketFormattedPath());
		Assert.assertEquals("#\\/abc\\/def", jsonSchemaPath.getReferenceFormattedPath());
	}

	@Test
	public void test6() throws Exception {
		final JsonSchemaPath jsonSchemaPath = new JsonSchemaPath("$").addPropertyKey("abc").addPropertyKey("def").addArrayIndex(70);
		Assert.assertEquals("$.abc.def[70]", jsonSchemaPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def'][70]", jsonSchemaPath.getBracketFormattedPath());
		Assert.assertEquals("$/abc/def[70]", jsonSchemaPath.getReferenceFormattedPath());
	}

	@Test
	public void test7() throws Exception {
		final JsonSchemaPath jsonSchemaPath = new JsonSchemaPath("$").addPropertyKey("abc").addPropertyKey("def");
		Assert.assertEquals("$.abc.def", jsonSchemaPath.getDotFormattedPath());
		Assert.assertEquals("$['abc']['def']", jsonSchemaPath.getBracketFormattedPath());
		Assert.assertEquals("$/abc/def", jsonSchemaPath.getReferenceFormattedPath());
	}
}
