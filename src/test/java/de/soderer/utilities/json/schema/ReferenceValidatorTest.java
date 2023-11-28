package de.soderer.utilities.json.schema;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.utilities.Utilities;

@SuppressWarnings("static-method")
public class ReferenceValidatorTest {
	@Test
	public void testReference() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { $ref: \"#/definitions/predefType\" }\n"
							+ "},\n"
							+ "definitions: {\n"
							+ "predefType: { type: \"string\" }\n"
							+ "}\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"{\n"
							+ "item1: \"Text1\"\n"
							+ "}\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
		} catch (final JsonSchemaDataValidationError e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
		}
	}

	@Test
	public void testReferenceIndirect() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		InputStream indirectSchemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { $ref: \"otherSchema.json#/definitions/predefType\" }\n"
							+ "}\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));

			final String indirectSchema =
					"{\n"
							+ "definitions: {\n"
							+ "predefType: { type: \"string\" }\n"
							+ "}\n"
							+ "}\n";
			indirectSchemaInputStream = new ByteArrayInputStream(indirectSchema.getBytes(StandardCharsets.UTF_8));

			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream, new JsonSchemaDependency("otherSchema.json", indirectSchemaInputStream));

			final String data =
					"{\n"
							+ "item1: \"Text1\"\n"
							+ "}\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
		} catch (final JsonSchemaDataValidationError e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
			Utilities.closeQuietly(indirectSchemaInputStream);
		}
	}

	@Test
	public void testReferenceIndirectKomplex1() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		InputStream indirectSchemaInputStream1 = null;
		InputStream indirectSchemaInputStream2 = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { $ref: \"otherSchema.json#/definitions/predefType1\" }\n"
							+ "}\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));

			final String indirectSchema1 =
					"{\n"
							+ "definitions: {\n"
							+ "predefType1: { $ref: \"someOtherSchema.json#/definitions/predefType2\" }\n"
							+ "}\n"
							+ "}\n";
			indirectSchemaInputStream1 = new ByteArrayInputStream(indirectSchema1.getBytes(StandardCharsets.UTF_8));

			final String indirectSchema2 =
					"{\n"
							+ "definitions: {\n"
							+ "predefType2: { type: \"string\" }\n"
							+ "}\n"
							+ "}\n";
			indirectSchemaInputStream2 = new ByteArrayInputStream(indirectSchema2.getBytes(StandardCharsets.UTF_8));

			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream, new JsonSchemaDependency("otherSchema.json", indirectSchemaInputStream1), new JsonSchemaDependency("someOtherSchema.json", indirectSchemaInputStream2));

			final String data =
					"{\n"
							+ "item1: \"Text1\"\n"
							+ "}\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
		} catch (final JsonSchemaDataValidationError e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
			Utilities.closeQuietly(indirectSchemaInputStream1);
			Utilities.closeQuietly(indirectSchemaInputStream2);
		}
	}

	@Test
	public void testReferenceIndirectKomplex2() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		InputStream indirectSchemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { $ref: \"otherSchema.json#/definitions/predefType1\" }\n"
							+ "}\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));

			final String indirectSchema =
					"{\n"
							+ "definitions: {\n"
							+ "predefType1: { $ref: \"#/definitions/predefType2\" },\n"
							+ "predefType2: { type: \"string\" }\n"
							+ "}\n"
							+ "}\n";
			indirectSchemaInputStream = new ByteArrayInputStream(indirectSchema.getBytes(StandardCharsets.UTF_8));

			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream, new JsonSchemaDependency("otherSchema.json", indirectSchemaInputStream));

			final String data =
					"{\n"
							+ "item1: \"Text1\"\n"
							+ "}\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
		} catch (final JsonSchemaDataValidationError e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
			Utilities.closeQuietly(indirectSchemaInputStream);
		}
	}

	@Test
	public void testReferenceIndirectSimple() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		InputStream indirectSchemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { $ref: \"predefType\" }\n"
							+ "}\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));

			final String indirectSchema =
					"{\n"
							+ "definitions: {\n"
							+ "predefType: { type: \"string\" }\n"
							+ "}\n"
							+ "}\n";
			indirectSchemaInputStream = new ByteArrayInputStream(indirectSchema.getBytes(StandardCharsets.UTF_8));

			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream, new JsonSchemaDependency("otherSchema.json", indirectSchemaInputStream));

			final String data =
					"{\n"
							+ "item1: \"Text1\"\n"
							+ "}\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
		} catch (final JsonSchemaDataValidationError e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
			Utilities.closeQuietly(indirectSchemaInputStream);
		}
	}

	@Test
	public void testReferenceError1() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { $ref: \"#/definitions/unknownPredefType\" }\n"
							+ "},\n"
							+ "definitions: {\n"
							+ "predefType: { type: \"string\" }\n"
							+ "}\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"{\n"
							+ "item1: \"Text1\"\n"
							+ "}\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
			Assert.fail("Missing expected exception");
		} catch (final JsonSchemaDefinitionError e) {
			// Expected exception
			assertJsonSchemaDefinitionErrorJsonSchemaPath(e, new JsonSchemaPath("#/definitions/unknownPredefType"));
		} catch (final JsonSchemaDataValidationError e) {
			// Expected exception
			assertJsonSchemaDataValidationErrorJsonPath(e, new JsonPath("$.item1"));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
		}
	}

	@Test
	public void testReferenceError2() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { $ref: \"#/definitions/predefType\" }\n"
							+ "},\n"
							+ "definitions: 3\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"{\n"
							+ "item1: \"Text1\"\n"
							+ "}\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
			Assert.fail("Missing expected exception");
		} catch (final JsonSchemaDefinitionError e) {
			// Expected exception
			assertJsonSchemaDefinitionErrorJsonSchemaPath(e, new JsonSchemaPath("#/definitions/predefType"));
		} catch (final JsonSchemaDataValidationError e) {
			// Expected exception
			assertJsonSchemaDataValidationErrorJsonPath(e, new JsonPath("$.item1"));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
		}
	}

	@Test
	public void testReferenceError3() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { $ref: \"#/definitions/predefType\" }\n"
							+ "},\n"
							+ "definitions: {\n"
							+ "predefType: { type: \"string\" }\n"
							+ "}\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"{\n"
							+ "item1: true\n"
							+ "}\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
			Assert.fail("Missing expected exception");
		} catch (final JsonSchemaDataValidationError e) {
			// Expected exception
			assertJsonSchemaDataValidationErrorJsonPath(e, new JsonPath("$.item1"));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
		}
	}

	@Test
	public void testReferenceIndirectError1() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		final InputStream indirectSchemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { $ref: \"#/definitions/predefType\" }\n"
							+ "}\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"{\n"
							+ "item1: \"Text1\"\n"
							+ "}\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
			Assert.fail("Missing expected exception");
		} catch (final JsonSchemaDefinitionError e) {
			// Expected exception
			assertJsonSchemaDefinitionErrorJsonSchemaPath(e, new JsonSchemaPath("#/definitions/predefType"));
		} catch (final JsonSchemaDataValidationError e) {
			// Expected exception
			assertJsonSchemaDataValidationErrorJsonPath(e, new JsonPath("$.item1"));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
			Utilities.closeQuietly(indirectSchemaInputStream);
		}
	}

	@Test
	public void testReferenceIndirectError2() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		InputStream indirectSchemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { $ref: \"otherSchema.json#/definitions/unknownPredefType\" }\n"
							+ "}\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));

			final String indirectSchema =
					"{\n"
							+ "definitions: {\n"
							+ "predefType: { type: \"string\" }\n"
							+ "}\n"
							+ "}\n";
			indirectSchemaInputStream = new ByteArrayInputStream(indirectSchema.getBytes(StandardCharsets.UTF_8));

			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream, new JsonSchemaDependency("otherSchema.json", indirectSchemaInputStream));

			final String data =
					"{\n"
							+ "item1: \"Text1\"\n"
							+ "}\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
			Assert.fail("Missing expected exception");
		} catch (final JsonSchemaDefinitionError e) {
			// Expected exception
			assertJsonSchemaDefinitionErrorJsonSchemaPath(e, new JsonSchemaPath("otherSchema.json#/definitions/unknownPredefType"));
		} catch (final JsonSchemaDataValidationError e) {
			// Expected exception
			assertJsonSchemaDataValidationErrorJsonPath(e, new JsonPath("$.item1"));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
			Utilities.closeQuietly(indirectSchemaInputStream);
		}
	}

	@Test
	public void testReferenceIndirectError3() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		InputStream indirectSchemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { $ref: \"unknownOtherSchema.json#/definitions/unknownPredefType\" }\n"
							+ "}\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));

			final String indirectSchema =
					"{\n"
							+ "definitions: {\n"
							+ "predefType: { type: \"string\" }\n"
							+ "}\n"
							+ "}\n";
			indirectSchemaInputStream = new ByteArrayInputStream(indirectSchema.getBytes(StandardCharsets.UTF_8));

			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream, new JsonSchemaDependency("otherSchema.json", indirectSchemaInputStream));

			final String data =
					"{\n"
							+ "item1: \"Text1\"\n"
							+ "}\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
			Assert.fail("Missing expected exception");
		} catch (final JsonSchemaDefinitionError e) {
			// Expected exception
			assertJsonSchemaDefinitionErrorJsonSchemaPath(e, new JsonSchemaPath("unknownOtherSchema.json#/definitions/unknownPredefType"));
		} catch (final JsonSchemaDataValidationError e) {
			// Expected exception
			assertJsonSchemaDataValidationErrorJsonPath(e, new JsonPath("$.item1"));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
			Utilities.closeQuietly(indirectSchemaInputStream);
		}
	}

	@Test
	public void testReferenceInfiniteCycle() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		final InputStream indirectSchemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { $ref: \"#/definitions/predefType1\" }\n"
							+ "},\n"
							+ "definitions: {\n"
							+ "predefType1: { $ref: \"#/definitions/predefType2\" },\n"
							+ "predefType2: { $ref: \"#/definitions/predefType1\" }\n"
							+ "}\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"{\n"
							+ "item1: \"Text1\"\n"
							+ "}\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
			Assert.fail("Missing expected exception");
		} catch (final JsonSchemaDefinitionError e) {
			// Expected exception
			assertJsonSchemaDefinitionErrorJsonSchemaPath(e, new JsonSchemaPath("#/definitions/predefType1"));
		} catch (final JsonSchemaDataValidationError e) {
			// Expected exception
			assertJsonSchemaDataValidationErrorJsonPath(e, new JsonPath("$.item1"));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
			Utilities.closeQuietly(indirectSchemaInputStream);
		}
	}

	private void assertJsonSchemaDefinitionErrorJsonSchemaPath(final JsonSchemaDefinitionError e, final JsonSchemaPath jsonSchemaPath) {
		if (e == null || e.getMessage() == null) {
			Assert.fail("JsonSchemaDefinitionError json path expected '" + jsonSchemaPath + "' but exception was null");
		} else {
			if (e.getJsonSchemaPath() == null) {
				e.printStackTrace();
				Assert.fail("JsonSchemaDefinitionError json schema path expected '" + jsonSchemaPath + "' but exception json schema path was null");
			} else if (!e.getJsonSchemaPath().equals(jsonSchemaPath)) {
				e.printStackTrace();
				Assert.fail("JsonSchemaDefinitionError json schema path expected '" + jsonSchemaPath + "' but exception json schema path was '" + e.getJsonSchemaPath().getDotFormattedPath() + "'");
			}
		}
	}

	private void assertJsonSchemaDataValidationErrorJsonPath(final JsonSchemaDataValidationError e, final JsonPath jsonPath) {
		if (e == null || e.getMessage() == null) {
			Assert.fail("JsonSchemaDataValidationError with message expected but message was null");
		} else {
			if (e.getJsonDataPath() == null) {
				e.printStackTrace();
				Assert.fail("JsonSchemaDataValidationError json path expected '" + jsonPath + "' but exception json path was null");
			} else if (!e.getJsonDataPath().equals(jsonPath)) {
				e.printStackTrace();
				Assert.fail("JsonSchemaDataValidationError json path expected '" + jsonPath + "' but exception json path was '" + e.getJsonDataPath().getDotFormattedPath() + "'");
			}
		}
	}
}
