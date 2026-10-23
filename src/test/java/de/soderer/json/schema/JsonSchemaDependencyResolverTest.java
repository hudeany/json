package de.soderer.json.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;

/**
 * Covers reference resolution of {@link JsonSchemaDependencyResolver} for local, simple and
 * external references, the download of referenced schemas from a local HTTP server, and
 * {@link JsonSchemaDependency} and {@link JsonSchemaPath}.
 */
@SuppressWarnings("static-method")
class JsonSchemaDependencyResolverTest {
	private HttpServer httpServer;
	private String baseUrl;

	private static InputStream stream(final String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
	}

	private static JsonObject object(final String text) throws Exception {
		return (JsonObject) JsonReader.readJsonItemString(text);
	}

	@BeforeEach
	void startServer() throws Exception {
		httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		httpServer.createContext("/schema.json", exchange -> {
			final byte[] body = "{\"definitions\": {\"name\": {\"type\": \"string\", \"minLength\": 2}}}".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream output = exchange.getResponseBody()) {
				output.write(body);
			}
		});
		httpServer.createContext("/redirect", exchange -> {
			exchange.getResponseHeaders().add("Location", "/schema.json");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		httpServer.createContext("/redirectWithoutLocation", exchange -> {
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		httpServer.createContext("/redirectLoop", exchange -> {
			exchange.getResponseHeaders().add("Location", "/redirectLoop");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		httpServer.createContext("/redirectToFile", exchange -> {
			exchange.getResponseHeaders().add("Location", "file:///etc/passwd");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		httpServer.createContext("/missing", exchange -> {
			exchange.sendResponseHeaders(404, -1);
			exchange.close();
		});
		httpServer.createContext("/tooLarge", exchange -> {
			exchange.sendResponseHeaders(200, 0);
			try (OutputStream output = exchange.getResponseBody()) {
				final byte[] blanks = new byte[1024 * 1024];
				Arrays.fill(blanks, (byte) ' ');
				for (int i = 0; i < 11; i++) {
					output.write(blanks);
				}
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Client closes the connection after the size limit was reached
			}
		});
		httpServer.start();
		baseUrl = "http://127.0.0.1:" + httpServer.getAddress().getPort();
	}

	@AfterEach
	void stopServer() {
		httpServer.stop(0);
	}

	@Test
	void simpleReferences_inMainSchemaAndDependencies() throws Exception {
		final JsonSchemaDependency dependency = new JsonSchemaDependency("other", object("{\"definitions\": {\"fromOther\": {\"type\": \"integer\"}, \"invalidOther\": 1}}"));
		final JsonSchemaDependencyResolver resolver = new JsonSchemaDependencyResolver(object("{\"definitions\": {\"local\": {\"type\": \"string\"}, \"invalid\": 1}}"), dependency);

		assertEquals("string", resolver.getDependencyByReference("local", new JsonSchemaPath()).getSimpleValue("type"));
		assertEquals("integer", resolver.getDependencyByReference("fromOther", new JsonSchemaPath()).getSimpleValue("type"));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getDependencyByReference("invalid", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getDependencyByReference("invalidOther", new JsonSchemaPath()));
		assertThrows(Exception.class, () -> resolver.getDependencyByReference("unknown", new JsonSchemaPath()));
		assertThrows(Exception.class, () -> resolver.getDependencyByReference(null, new JsonSchemaPath()));

		assertEquals("#", resolver.getSchemaContainingReference("local", new JsonSchemaPath()));
		assertEquals("other", resolver.getSchemaContainingReference("fromOther", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getSchemaContainingReference("invalid", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getSchemaContainingReference("invalidOther", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getSchemaContainingReference("unknown", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getSchemaContainingReference(null, new JsonSchemaPath()));
	}

	@Test
	void localReferences() throws Exception {
		final JsonSchemaDependencyResolver resolver = new JsonSchemaDependencyResolver(object("{\"definitions\": {\"a\": {\"type\": \"string\"}, \"scalar\": 1}, \"items\": [{\"type\": \"integer\"}, [{\"type\": \"null\"}], 5]}"));

		assertEquals("string", resolver.getDependencyByReference("#/definitions/a", new JsonSchemaPath()).getSimpleValue("type"));
		assertEquals("integer", resolver.getDependencyByReference("#/items/0", new JsonSchemaPath()).getSimpleValue("type"));
		assertEquals("null", resolver.getDependencyByReference("#/items/1/0", new JsonSchemaPath()).getSimpleValue("type"));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getDependencyByReference("#/definitions/missing", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getDependencyByReference("#/definitions/scalar", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getDependencyByReference("#/items/9", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getDependencyByReference("#/items/x", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getDependencyByReference("#/items/2", new JsonSchemaPath()));

		assertEquals("#", resolver.getSchemaContainingReference("#/definitions/a", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getSchemaContainingReference("#/definitions/missing", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getSchemaContainingReference("#/definitions/scalar", new JsonSchemaPath()));
	}

	@Test
	void externalReferences_toAddedSchemas() throws Exception {
		final JsonSchemaDependencyResolver resolver = new JsonSchemaDependencyResolver(new JsonObject());
		resolver.addJsonSchemaDefinition("ext.json", stream("{\"definitions\": {\"x\": {\"type\": \"boolean\", \"items\": [{\"$ref\": \"#/definitions/y\"}]}, \"y\": {}, \"scalar\": 1}}"));

		final JsonObject resolved = resolver.getDependencyByReference("ext.json#/definitions/x", new JsonSchemaPath());
		assertEquals("boolean", resolved.getSimpleValue("type"));
		// Internal references of the added schema are redirected to its name
		assertEquals("ext.json#/definitions/y", ((JsonObject) ((de.soderer.json.JsonArray) resolved.get("items")).get(0)).getSimpleValue("$ref"));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getDependencyByReference("ext.json#/definitions/missing", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getDependencyByReference("ext.json#/definitions/scalar", new JsonSchemaPath()));
		assertThrows(Exception.class, () -> resolver.getDependencyByReference("unknown.json#/definitions/x", new JsonSchemaPath()));

		assertEquals("ext.json", resolver.getSchemaContainingReference("ext.json#/definitions/x", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getSchemaContainingReference("ext.json#/definitions/missing", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getSchemaContainingReference("ext.json#/definitions/scalar", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.getSchemaContainingReference("unknown.json#/definitions/x", new JsonSchemaPath()));
	}

	@Test
	void addJsonSchemaDefinition_errors() throws Exception {
		final JsonSchemaDependencyResolver resolver = new JsonSchemaDependencyResolver(new JsonObject());
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.addJsonSchemaDefinition(" ", stream("{}")));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.addJsonSchemaDefinition("array", stream("[]")));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.addJsonSchemaDefinition("broken", stream("{ broken")));
		resolver.addJsonSchemaDefinition("once", stream("{}"));
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.addJsonSchemaDefinition("once", stream("{}")));

		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.addJsonSchemaDefinition("", new JsonObject()));
		resolver.addJsonSchemaDefinition("object", new JsonObject());
		assertThrows(JsonSchemaDefinitionError.class, () -> resolver.addJsonSchemaDefinition("object", new JsonObject()));

		assertThrows(JsonSchemaDefinitionError.class, () -> new JsonSchemaDependencyResolver(null));
	}

	@Test
	void httpReferences_withoutDownload() throws Exception {
		final String url = baseUrl + "/schema.json";

		final JsonSchemaDependencyResolver lazyResolver = new JsonSchemaDependencyResolver(new JsonObject());
		assertThrows(Exception.class, () -> lazyResolver.getDependencyByReference(url + "#/definitions/name", new JsonSchemaPath()));
		final JsonSchemaDependencyResolver lazyResolver2 = new JsonSchemaDependencyResolver(new JsonObject());
		assertThrows(JsonSchemaDefinitionError.class, () -> lazyResolver2.getSchemaContainingReference(url + "#/definitions/name", new JsonSchemaPath()));

		final JsonSchemaDependencyResolver strictResolver = new JsonSchemaDependencyResolver(new JsonObject()).withLazyFailOnMissingExternalSchemas(false);
		final Exception exception = assertThrows(Exception.class, () -> strictResolver.getDependencyByReference(url + "#/definitions/name", new JsonSchemaPath()));
		assertTrue(exception.getMessage().contains("needs download"), exception.getMessage());
		assertThrows(JsonSchemaDefinitionError.class, () -> strictResolver.getSchemaContainingReference(url + "#/definitions/name", new JsonSchemaPath()));
	}

	@Test
	void httpReferences_download() throws Exception {
		final JsonSchemaDependencyResolver resolver = new JsonSchemaDependencyResolver(new JsonObject()).withDownloadReferencedSchemas(true);
		assertEquals("string", resolver.getDependencyByReference(baseUrl + "/schema.json#/definitions/name", new JsonSchemaPath()).getSimpleValue("type"));

		final JsonSchemaDependencyResolver redirectResolver = new JsonSchemaDependencyResolver(new JsonObject()).withDownloadReferencedSchemas(true);
		assertEquals(baseUrl + "/redirect", redirectResolver.getSchemaContainingReference(baseUrl + "/redirect#/definitions/name", new JsonSchemaPath()));

		final JsonSchemaDependencyResolver errorResolver = new JsonSchemaDependencyResolver(new JsonObject()).withDownloadReferencedSchemas(true);
		assertThrows(JsonSchemaDefinitionError.class, () -> errorResolver.getDependencyByReference(baseUrl + "/missing#/definitions/name", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> errorResolver.getSchemaContainingReference(baseUrl + "/redirectWithoutLocation#/x", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> errorResolver.getDependencyByReference(baseUrl + "/redirectLoop#/x", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> errorResolver.getDependencyByReference(baseUrl + "/redirectToFile#/x", new JsonSchemaPath()));
		assertThrows(JsonSchemaDefinitionError.class, () -> errorResolver.getDependencyByReference(baseUrl + "/tooLarge#/x", new JsonSchemaPath()));
		// Port 1 is not listening, so the connection fails
		assertThrows(JsonSchemaDefinitionError.class, () -> errorResolver.getSchemaContainingReference("http://127.0.0.1:1/x.json#/x", new JsonSchemaPath()));
	}

	@Test
	void httpReferences_inSchemaValidation() throws Exception {
		final String schemaText = "{\"type\": \"object\", \"properties\": {\"name\": {\"$ref\": \"" + baseUrl + "/schema.json#/definitions/name\"}}}";
		final JsonSchema jsonSchema = new JsonSchema(stream(schemaText), new JsonSchemaConfiguration().withJsonSchemaVersion(JsonSchemaVersion.draftV7).withDownloadReferencedSchemas(true));
		jsonSchema.validate(JsonReader.readJsonItemString("{\"name\": \"ab\"}"));
		assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(JsonReader.readJsonItemString("{\"name\": \"a\"}")));
	}

	@Test
	void settersAndModes() throws Exception {
		final JsonSchemaDependencyResolver resolver = new JsonSchemaDependencyResolver(new JsonObject());
		assertTrue(resolver.isDraftV7Mode());
		assertSame(resolver, resolver.withJsonSchemaVersion(JsonSchemaVersion.draftV6));
		assertTrue(resolver.isDraftV6Mode());
		assertFalse(resolver.isDraftV7Mode());
		resolver.setJsonSchemaVersion(null);
		assertTrue(resolver.isSimpleMode());
		resolver.setLazyFailOnMissingExternalSchemas(true);
		resolver.setDownloadReferencedSchemas(false);

		assertNull(resolver.getCachedReferenceValidators("x"));
		resolver.putCachedReferenceValidators("x", new java.util.ArrayList<>());
		assertTrue(resolver.getCachedReferenceValidators("x").isEmpty());
		assertFalse(resolver.enterReferenceResolution("r", "$"));
		assertTrue(resolver.enterReferenceResolution("r", "$"));
		resolver.exitReferenceResolution("r", "$");
		assertFalse(resolver.enterReferenceResolution("r", "$"));
		resolver.exitReferenceResolution("r", "$");

		final JsonSchemaConfiguration configuration = new JsonSchemaConfiguration(StandardCharsets.ISO_8859_1, JsonSchemaVersion.draftV4, false);
		configuration.setDownloadReferencedSchemas(true);
		assertTrue(configuration.isDownloadReferencedSchemas());
		assertEquals(StandardCharsets.ISO_8859_1, configuration.getEncoding());
	}

	@Test
	void jsonSchemaDependency() throws Exception {
		final JsonSchemaDependency fromStream = new JsonSchemaDependency("dep", stream("{\"allOf\": [{\"$ref\": \"#/definitions/a\"}, [{\"$ref\": \"#/x\"}]], \"definitions\": {\"a\": {}}}"));
		assertEquals("dep", fromStream.getJsonSchemaReferenceName());
		final de.soderer.json.JsonArray allOf = (de.soderer.json.JsonArray) fromStream.getJsonSchemaReferenceObject().get("allOf");
		assertEquals("dep#/definitions/a", ((JsonObject) allOf.get(0)).getSimpleValue("$ref"));
		assertEquals("dep#/x", ((JsonObject) ((de.soderer.json.JsonArray) allOf.get(1)).get(0)).getSimpleValue("$ref"));

		assertThrows(Exception.class, () -> new JsonSchemaDependency("", stream("{}")));
		assertThrows(Exception.class, () -> new JsonSchemaDependency("dep", stream("[]")));
		assertThrows(Exception.class, () -> new JsonSchemaDependency(" ", new JsonObject()));

		final JsonObject schemaObject = object("{\"items\": [{\"$ref\": \"#/a\"}, [{\"$ref\": \"#/b\"}]], \"$ref\": \"other#/c\"}");
		@SuppressWarnings("unused")
		final
		JsonSchemaDependency dependency = new JsonSchemaDependency("obj", schemaObject);
		assertEquals("obj#/a", ((JsonObject) ((de.soderer.json.JsonArray) schemaObject.get("items")).get(0)).getSimpleValue("$ref"));
		assertEquals("other#/c", schemaObject.getSimpleValue("$ref"));
	}

	@Test
	void jsonSchemaPath_formats() throws Exception {
		final JsonSchemaPath path = new JsonSchemaPath().addPropertyKey("pro.p'er/ty").addArrayIndex(2);
		assertEquals("$.pro\\.p'er/ty[2]", path.getDotFormattedPath());
		assertEquals("$['pro.p\\'er/ty'][2]", path.getBracketFormattedPath());
		assertEquals("$/pro.p'er\\/ty[2]", path.getReferenceFormattedPath());
		assertEquals(path.getDotFormattedPath(), path.toString());

		final JsonSchemaPath internal = new JsonSchemaPath("#/definitions/a.b");
		assertTrue(internal.getDotFormattedPath().contains("a\\.b"));
		assertTrue(internal.getBracketFormattedPath().startsWith("#"));
		assertTrue(internal.getReferenceFormattedPath().contains("\\/definitions"));
		assertFalse(internal.isRoot());
		assertTrue(new JsonSchemaPath("#").isRoot());
		assertTrue(new JsonSchemaPath("").isRoot());

		final JsonSchemaPath external = new JsonSchemaPath("other.json#/x");
		assertTrue(external.getDotFormattedPath().startsWith("other.json"));
		assertTrue(external.getBracketFormattedPath().startsWith("other.json"));
		assertTrue(external.getReferenceFormattedPath().startsWith("other.json"));
		assertFalse(external.isRoot());
		external.add(new JsonSchemaPathPropertyElement("y"));
		assertTrue(external.getDotFormattedPath().endsWith(".y"));
		assertThrows(IllegalArgumentException.class, () -> external.add(null));

		final JsonSchemaPath parsed = new JsonSchemaPath("$.a['b'][3]/c~1d~0e");
		assertEquals("$.a.b[3].c/d~e", parsed.getDotFormattedPath());
		assertEquals(parsed, new JsonSchemaPath("$['a']['b'][3]['c/d~e']"));
		assertEquals(parsed.hashCode(), new JsonSchemaPath(parsed).hashCode());
		assertNotEquals(parsed, null);
		assertNotEquals(parsed, "x");
		assertFalse(new JsonSchemaPath().addPropertyKey("a").isRoot());
		assertThrows(JsonSchemaDefinitionError.class, () -> new JsonSchemaPath("$x"));
	}
}
