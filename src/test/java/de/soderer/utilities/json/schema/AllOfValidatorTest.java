package de.soderer.utilities.json.schema;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.utilities.Utilities;

@SuppressWarnings("static-method")
public class AllOfValidatorTest {
	@Test
	public void testAllOf() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "allOf: [\n"
							+ "{ enum: [ 1, 7 ] },\n"
							+ "{ enum: [ 7 ] },\n"
							+ "{ enum: [ 2, 7 ] }\n"
							+ "]\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"7\n";
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
	public void testAllOf2() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "allOf: [\n"
							+ "{ enum: [ 1, 7 ] },\n"
							+ "{ type: \"integer\" },\n"
							+ "{ type: \"integer\" },\n"
							+ "{ enum: [ 2, 7 ] }\n"
							+ "]\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"7\n";
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
	public void testAllOf3() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "allOf: [\n"
							+ "{ enum: [ 1, 7 ] },\n"
							+ "{ type: \"integer\" },\n"
							+ "true,\n"
							+ "{ enum: [ 2, 7 ] }\n"
							+ "]\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"7\n";
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
	public void testAllOfError() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "allOf: [\n"
							+ "{ enum: [ 1, 7 ] },\n"
							+ "{ enum: [ 2, 7 ] },\n"
							+ "{ enum: [ 3 ] }\n"
							+ "]\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"7\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
			Assert.fail("Missing expected exception");
		} catch (final JsonSchemaDataValidationError e) {
			// Expected exception
			assertJsonSchemaDataValidationErrorJsonPath(e, new JsonPath("$"));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
		}
	}

	@Test
	public void testAllOfError2() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "allOf: [\n"
							+ "{ enum: [ 1, 7 ] },\n"
							+ "{ type: \"integer\" },\n"
							+ "false,\n"
							+ "{ enum: [ 2, 7 ] }\n"
							+ "]\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"7\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
			Assert.fail("Missing expected exception");
		} catch (final JsonSchemaDataValidationError e) {
			// Expected exception
			assertJsonSchemaDataValidationErrorJsonPath(e, new JsonPath("$"));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
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