package de.soderer.json.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests of the URI helpers in {@link JsonSchemaUriUtilities}.
 */
@SuppressWarnings("static-method")
public class JsonSchemaUriUtilitiesTest {
	@Test
	void resolve() {
		assertEquals("http://example.com/b/c.json", JsonSchemaUriUtilities.resolve("http://example.com/b/a.json", "c.json"));
		assertEquals("http://example.com/c.json", JsonSchemaUriUtilities.resolve("http://example.com/b/a.json", "/c.json"));
		assertEquals("http://example.com/a.json#/$defs/x", JsonSchemaUriUtilities.resolve("http://example.com/a.json#old", "#/$defs/x"));
		assertEquals("http://example.com/a.json", JsonSchemaUriUtilities.resolve("http://example.com/a.json#old", ""));
		assertEquals("http://example.com/a.json", JsonSchemaUriUtilities.resolve("http://example.com/a.json", "#"));
		assertEquals("http://example.com/x.json", JsonSchemaUriUtilities.resolve("http://example.com", "x.json"));
		assertEquals("urn:other", JsonSchemaUriUtilities.resolve("http://example.com/a.json", "urn:other"));
		// Fragment only references against URNs
		assertEquals("urn:uuid:deadbeef-1234-ffff-ffff-4321feebdaed#/$defs/x", JsonSchemaUriUtilities.resolve("urn:uuid:deadbeef-1234-ffff-ffff-4321feebdaed", "#/$defs/x"));
		// Characters illegal in java.net.URI
		assertEquals("http://example.com/my%20schema.json", JsonSchemaUriUtilities.resolve("http://example.com/a.json", "my schema.json"));
	}

	@Test
	void fragmentsAndPointers() {
		assertEquals("http://example.com/a.json", JsonSchemaUriUtilities.removeFragment("http://example.com/a.json#/x"));
		assertEquals("/x", JsonSchemaUriUtilities.getFragment("http://example.com/a.json#/x"));
		assertNull(JsonSchemaUriUtilities.getFragment("http://example.com/a.json"));
		assertTrue(JsonSchemaUriUtilities.isJsonPointerFragment("/a/b"));
		assertTrue(JsonSchemaUriUtilities.isJsonPointerFragment(""));
		assertFalse(JsonSchemaUriUtilities.isJsonPointerFragment("anchor"));
		assertEquals("foo\"bar", JsonSchemaUriUtilities.percentDecode("foo%22bar"));
		assertEquals("ä", JsonSchemaUriUtilities.percentDecode("%C3%A4"));
		assertEquals("a/b~c", JsonSchemaUriUtilities.decodeJsonPointerToken("a~1b~0c"));
		assertEquals("a~1b~0c", JsonSchemaUriUtilities.encodeJsonPointerToken("a/b~c"));
		assertTrue(JsonSchemaUriUtilities.hasScheme("urn:x"));
		assertFalse(JsonSchemaUriUtilities.hasScheme("a/b:c"));
	}
}
