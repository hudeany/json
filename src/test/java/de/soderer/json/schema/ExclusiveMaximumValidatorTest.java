package de.soderer.json.schema;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.json.path.JsonPath;
import de.soderer.json.utilities.Utilities;

@SuppressWarnings("static-method")
public class ExclusiveMaximumValidatorTest {
	@Test
	public void testSimpleNumberMaximumExclusiveBeforeV6() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "maximum: 1,\n"
							+ "exclusiveMaximum: true\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream, new JsonSchemaConfiguration(StandardCharsets.UTF_8, JsonSchemaVersion.draftV4, false));

			final String data =
					"0";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
		}
	}

	@Test
	public void testSimpleNumberMaximumExclusiveErrorBeforeV6() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "maximum: 0,\n"
							+ "exclusiveMaximum: true\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream, new JsonSchemaConfiguration(StandardCharsets.UTF_8, JsonSchemaVersion.draftV4, false));

			final String data =
					"0";
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
	public void testSimpleNumberMaximumExclusive() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "$schema: \"https://json-schema.org/draft-07/schema#\",\n"
							+ "exclusiveMaximum: 0\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"-1";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
		}
	}

	@Test
	public void testSimpleNumberMaximumExclusiveError() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "$schema: \"https://json-schema.org/draft-07/schema#\",\n"
							+ "exclusiveMaximum: 0\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"0";
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
