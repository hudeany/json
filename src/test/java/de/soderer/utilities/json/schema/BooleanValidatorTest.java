package de.soderer.utilities.json.schema;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.utilities.Utilities;

@SuppressWarnings("static-method")
public class BooleanValidatorTest {
	@Test
	public void testSimpleBooleanSchemaTrue() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema = "true\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data = "{\"abc\":123}\n";
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
	public void testSimpleBooleanSchemaFalse() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema = "false\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data = "{\"abc\":123}\n";
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
	public void testSimpleTinySchemaTrue() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema = "{}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data = "{\"abc\":123}\n";
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
	public void testSimpleTinySchemaFalse() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema = "{\"not\": {}}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data = "{\"abc\":123}\n";
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
	public void testSimpleTinySchemaFalse2() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema = "{\"not\": true}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data = "{\"abc\":123}\n";
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
