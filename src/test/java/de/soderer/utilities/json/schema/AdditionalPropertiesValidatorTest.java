package de.soderer.utilities.json.schema;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.utilities.Utilities;

@SuppressWarnings("static-method")
public class AdditionalPropertiesValidatorTest {
	@Test
	public void testBooleanAdditionalPropertiesAllowedDefault() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { enum: [ \"Text1\", \"Text2\", 3 ] },\n"
							+ "item2: { enum: [ \"Text1\", \"Text2\", 3 ] },\n"
							+ "item3: { enum: [ \"Text1\", \"Text2\", 3 ] }\n"
							+ "}"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"{\n"
							+ "item1: \"Text1\",\n"
							+ "item2: \"Text2\",\n"
							+ "item3: 3,\n"
							+ "item4: 4\n"
							+ "}\n";
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
	public void testBooleanAdditionalPropertiesAllowed() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { enum: [ \"Text1\", \"Text2\", 3 ] },\n"
							+ "item2: { enum: [ \"Text1\", \"Text2\", 3 ] },\n"
							+ "item3: { enum: [ \"Text1\", \"Text2\", 3 ] }\n"
							+ "},"
							+ "additionalProperties: true\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"{\n"
							+ "item1: \"Text1\",\n"
							+ "item2: \"Text2\",\n"
							+ "item3: 3,\n"
							+ "item4: 4\n"
							+ "}\n";
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
	public void testBooleanNoAdditionalPropertiesAllowedError() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { enum: [ \"Text1\", \"Text2\", 3 ] },\n"
							+ "item2: { enum: [ \"Text1\", \"Text2\", 3 ] },\n"
							+ "item3: { enum: [ \"Text1\", \"Text2\", 3 ] }\n"
							+ "},"
							+ "additionalProperties: false\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"{\n"
							+ "item1: \"Text1\",\n"
							+ "item2: \"Text2\",\n"
							+ "item3: 3,\n"
							+ "item4: 4\n"
							+ "}\n";
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
	public void testObjectAdditionalPropertiesStringAllowedError() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { enum: [ \"Text1\", \"Text2\", 3 ] },\n"
							+ "item2: { enum: [ \"Text1\", \"Text2\", 3 ] },\n"
							+ "item3: { enum: [ \"Text1\", \"Text2\", 3 ] }\n"
							+ "},"
							+ "additionalProperties: { type: \"string\" }\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"{\n"
							+ "item1: \"Text1\",\n"
							+ "item2: \"Text2\",\n"
							+ "item3: 3,\n"
							+ "item4: 4\n"
							+ "}\n";
			dataInputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			jsonSchema.validate(dataInputStream);
			Assert.fail("Missing expected exception");
		} catch (final JsonSchemaDataValidationError e) {
			// Expected exception
			assertJsonSchemaDataValidationErrorJsonPath(e, new JsonPath("$.item4"));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(dataInputStream);
			Utilities.closeQuietly(schemaInputStream);
		}
	}

	@Test
	public void testObjectAdditionalPropertiesStringAllowed() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "type: \"object\",\n"
							+ "properties: {\n"
							+ "item1: { enum: [ \"Text1\", \"Text2\", 3 ] },\n"
							+ "item2: { enum: [ \"Text1\", \"Text2\", 3 ] },\n"
							+ "item3: { enum: [ \"Text1\", \"Text2\", 3 ] }\n"
							+ "},"
							+ "additionalProperties: { type: \"string\" }\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"{\n"
							+ "item1: \"Text1\",\n"
							+ "item2: \"Text2\",\n"
							+ "item3: 3,\n"
							+ "item4: \"Text4\"\n"
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
