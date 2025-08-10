package de.soderer.json.schema;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.json.path.JsonPath;
import de.soderer.json.utilities.Utilities;

@SuppressWarnings("static-method")
public class FormatValidatorTest {
	@Test
	public void testFormatEmail() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "format: \"email\"\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"\"tester@example.com\"\n";
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
	public void testFormatEmailError() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "format: \"email\"\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"\"tester@@example.com\"\n";
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
	public void testFormatIpv4() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "format: \"ipv4\"\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"\"192.168.1.70\"\n";
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
	public void testFormatIpv4Error() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "format: \"ipv4\"\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"\"192.168.1.707\"\n";
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
	public void testFormatIpv6() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "format: \"ipv6\"\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"\"2001:0db8:1234::0000\"\n";
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
	public void testFormatIpv6Error() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "format: \"ipv6\"\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"\"2001:0db8:1234:ffff:ffff:ffff:ffff:fffx\"\n";
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
	public void testFormatDate() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "format: \"date\"\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"\"2017-02-28\"\n";
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
	public void testFormatDateError() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "format: \"date\"\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"\"2017-02-32\"\n";
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
	public void testFormatDateTime() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "format: \"date-time\"\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"\"2017-02-28T17:30:00+01\"\n";
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
	public void testFormatDateTimeError() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "format: \"date-time\"\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"\"2017-02-28T17:70:00\"\n";
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
	public void testFormatTime() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "format: \"time\"\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"\"17:30:00+01\"\n";
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
	public void testFormatTimeError() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "format: \"time\"\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"\"24:30:00+01\"\n";
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
	public void testFormatBase64() {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "format: \"base64\"\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"\"" + Utilities.encodeBase64("Text".getBytes(StandardCharsets.UTF_8)) + "\"\n";
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
	public void testFormatBase64Error() throws Exception {
		InputStream dataInputStream = null;
		InputStream schemaInputStream = null;
		try {
			final String schema =
					"{\n"
							+ "format: \"base64\"\n"
							+ "}\n";
			schemaInputStream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
			final JsonSchema jsonSchema = new JsonSchema(schemaInputStream);

			final String data =
					"\"" + Utilities.encodeBase64("Text".getBytes(StandardCharsets.UTF_8)) + "x\"\n";
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
