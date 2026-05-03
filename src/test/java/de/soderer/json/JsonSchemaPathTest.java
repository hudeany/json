package de.soderer.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.soderer.json.schema.JsonSchemaPath;

@SuppressWarnings("static-method")
public class JsonSchemaPathTest {
	@Test
	public void test1() throws Exception {
		final JsonSchemaPath jsonSchemaPath = new JsonSchemaPath("$.abc.def[70]");
		Assertions.assertEquals("$.abc.def[70]", jsonSchemaPath.getDotFormattedPath());
		Assertions.assertEquals("$['abc']['def'][70]", jsonSchemaPath.getBracketFormattedPath());
		Assertions.assertEquals("$/abc/def[70]", jsonSchemaPath.getReferenceFormattedPath());
	}

	@Test
	public void test2() throws Exception {
		final JsonSchemaPath jsonSchemaPath = new JsonSchemaPath("$.abc.def");
		Assertions.assertEquals("$.abc.def", jsonSchemaPath.getDotFormattedPath());
		Assertions.assertEquals("$['abc']['def']", jsonSchemaPath.getBracketFormattedPath());
		Assertions.assertEquals("$/abc/def", jsonSchemaPath.getReferenceFormattedPath());
	}

	@Test
	public void test3() throws Exception {
		final JsonSchemaPath jsonSchemaPath = new JsonSchemaPath("$['abc']['def'][70]");
		Assertions.assertEquals("$.abc.def[70]", jsonSchemaPath.getDotFormattedPath());
		Assertions.assertEquals("$['abc']['def'][70]", jsonSchemaPath.getBracketFormattedPath());
		Assertions.assertEquals("$/abc/def[70]", jsonSchemaPath.getReferenceFormattedPath());
	}

	@Test
	public void test4() throws Exception {
		final JsonSchemaPath jsonSchemaPath = new JsonSchemaPath("$['abc']['def']");
		Assertions.assertEquals("$.abc.def", jsonSchemaPath.getDotFormattedPath());
		Assertions.assertEquals("$['abc']['def']", jsonSchemaPath.getBracketFormattedPath());
		Assertions.assertEquals("$/abc/def", jsonSchemaPath.getReferenceFormattedPath());
	}

	@Test
	public void test5() throws Exception {
		final JsonSchemaPath jsonSchemaPath = new JsonSchemaPath("#/abc/def");
		Assertions.assertEquals("#/abc/def", jsonSchemaPath.getDotFormattedPath());
		Assertions.assertEquals("#/abc/def", jsonSchemaPath.getBracketFormattedPath());
		Assertions.assertEquals("#\\/abc\\/def", jsonSchemaPath.getReferenceFormattedPath());
	}

	@Test
	public void test6() throws Exception {
		final JsonSchemaPath jsonSchemaPath = new JsonSchemaPath("$").addPropertyKey("abc").addPropertyKey("def").addArrayIndex(70);
		Assertions.assertEquals("$.abc.def[70]", jsonSchemaPath.getDotFormattedPath());
		Assertions.assertEquals("$['abc']['def'][70]", jsonSchemaPath.getBracketFormattedPath());
		Assertions.assertEquals("$/abc/def[70]", jsonSchemaPath.getReferenceFormattedPath());
	}

	@Test
	public void test7() throws Exception {
		final JsonSchemaPath jsonSchemaPath = new JsonSchemaPath("$").addPropertyKey("abc").addPropertyKey("def");
		Assertions.assertEquals("$.abc.def", jsonSchemaPath.getDotFormattedPath());
		Assertions.assertEquals("$['abc']['def']", jsonSchemaPath.getBracketFormattedPath());
		Assertions.assertEquals("$/abc/def", jsonSchemaPath.getReferenceFormattedPath());
	}
}
