package de.soderer.utilities.json.schema;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaConfiguration;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaVersion;
import de.soderer.json.utilities.Utilities;

@SuppressWarnings("static-method")
public class IfThenElseValidatorTest {

	@Test
	public void testIfThenElseTest1() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema = "{\n"
					+ "\"type\": \"object\",\n"
					+ "\"properties\": {\n"
					+ "\"foo\": {\n"
					+ "\"type\": \"string\"\n"
					+ "},\n"
					+ "\"bar\": {\n"
					+ "\"type\": \"string\"\n"
					+ "}\n"
					+ "},\n"
					+ "\"if\": {\n"
					+ "\"properties\": {\n"
					+ "\"foo\": {\n"
					+ "\"enum\": [\n"
					+ "\"needsBar\"\n"
					+ "]\n"
					+ "}\n"
					+ "},\n"
					+ "\"required\": [\n"
					+ "\"foo\"\n"
					+ "]\n"
					+ "},\n"
					+ "\"then\": {\n"
					+ "\"required\": [\n"
					+ "\"bar\"\n"
					+ "]\n"
					+ "},\n"
					+ "\"else\": true\n"
					+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream, new JsonSchemaConfiguration().setJsonSchemaVersion(JsonSchemaVersion.draftV7));

			final String data =
					"{}\n";
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
	public void testIfThenElseTest2() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema = "{\n"
					+ "\"type\": \"object\",\n"
					+ "\"properties\": {\n"
					+ "\"foo\": {\n"
					+ "\"type\": \"string\"\n"
					+ "},\n"
					+ "\"bar\": {\n"
					+ "\"type\": \"string\"\n"
					+ "}\n"
					+ "},\n"
					+ "\"if\": {\n"
					+ "\"properties\": {\n"
					+ "\"foo\": {\n"
					+ "\"enum\": [\n"
					+ "\"needsBar\"\n"
					+ "]\n"
					+ "}\n"
					+ "},\n"
					+ "\"required\": [\n"
					+ "\"foo\"\n"
					+ "]\n"
					+ "},\n"
					+ "\"then\": {\n"
					+ "\"required\": [\n"
					+ "\"bar\"\n"
					+ "]\n"
					+ "},\n"
					+ "\"else\": false\n"
					+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream, new JsonSchemaConfiguration().setJsonSchemaVersion(JsonSchemaVersion.draftV7));

			final String data =
					"{\n"
							+ "\"foo\": \"needsBar\",\n"
							+ "\"bar\": \"barIsNeeded\"\n"
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
	public void testIfThenElseFail() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema = "{\n"
					+ "\"type\": \"object\",\n"
					+ "\"properties\": {\n"
					+ "\"foo\": {\n"
					+ "\"type\": \"string\"\n"
					+ "},\n"
					+ "\"bar\": {\n"
					+ "\"type\": \"string\"\n"
					+ "}\n"
					+ "},\n"
					+ "\"if\": {\n"
					+ "\"properties\": {\n"
					+ "\"foo\": {\n"
					+ "\"enum\": [\n"
					+ "\"needsBar\"\n"
					+ "]\n"
					+ "}\n"
					+ "},\n"
					+ "\"required\": [\n"
					+ "\"foo\"\n"
					+ "]\n"
					+ "},\n"
					+ "\"then\": {\n"
					+ "\"required\": [\n"
					+ "\"bar\"\n"
					+ "]\n"
					+ "},\n"
					+ "\"else\": false\n"
					+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream, new JsonSchemaConfiguration().setJsonSchemaVersion(JsonSchemaVersion.draftV7));

			final String data =
					"{\"foo\": \"needsBar\"}\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
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
