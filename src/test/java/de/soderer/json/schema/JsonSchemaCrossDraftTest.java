package de.soderer.json.schema;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;

/**
 * Tests of references into schema documents declaring another JSON schema version by "$schema"
 * than the referencing schema ("cross-draft" references).
 */
@SuppressWarnings("static-method")
public class JsonSchemaCrossDraftTest {
	/**
	 * Public methods of the resolver a version view answers itself, because they only depend on the
	 * JSON schema version. All other public methods must be delegated to the main resolver.
	 */
	private static final Set<String> VERSION_DEPENDENT_METHODS = Set.of(
			"getJsonSchemaVersion", "isSimpleMode", "isDraftV3Mode", "isDraftV4Mode", "isDraftV6Mode", "isDraftV7Mode",
			"isDraft2019_09Mode", "isDraft2020_12Mode", "isVersionAtLeast", "isValidationVocabularyActive", "getResolverForSchemaNode");

	private static JsonNode json(final String text) throws Exception {
		try (JsonReader reader = new JsonReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)))) {
			return reader.read();
		}
	}

	private static JsonSchema schema(final String schemaText, final JsonSchemaVersion version, final JsonSchemaDependency... dependencies) throws Exception {
		return new JsonSchema((JsonObject) json(schemaText), new JsonSchemaConfiguration().withJsonSchemaVersion(version), dependencies);
	}

	private static void assertValid(final JsonSchema jsonSchema, final String data) {
		assertDoesNotThrow(() -> jsonSchema.validate(json(data)), data);
	}

	private static void assertInvalid(final JsonSchema jsonSchema, final String data) {
		assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(json(data)), data);
	}

	@Test
	void referenceToNewerDraftUsesNewerDraft() throws Exception {
		// "prefixItems" is unknown in draft 7, but the referenced document is a draft 2020-12 schema
		final JsonSchemaDependency newer = new JsonSchemaDependency("https://example.com/newer.json", (JsonObject) json(
				"{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\", \"prefixItems\": [{\"type\": \"string\"}]}"));
		final JsonSchema jsonSchema = schema("{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"array\", \"allOf\": [{\"$ref\": \"https://example.com/newer.json\"}]}", null, newer);
		assertValid(jsonSchema, "[\"a\", 1]");
		assertInvalid(jsonSchema, "[1]");
	}

	@Test
	void referenceToOlderDraftUsesOlderDraft() throws Exception {
		// In draft 7 "$ref" overrides its sibling keywords, so "minimum" of the referenced document is ignored
		final JsonSchemaDependency older = new JsonSchemaDependency("https://example.com/older.json", (JsonObject) json(
				"{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"$ref\": \"#/definitions/number\", \"minimum\": 10, \"definitions\": {\"number\": {\"type\": \"number\"}}}"));
		final JsonSchema jsonSchema = schema("{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\", \"$ref\": \"https://example.com/older.json\"}", null, older);
		assertValid(jsonSchema, "5");
		assertInvalid(jsonSchema, "\"a\"");
	}

	@Test
	void documentWithoutSchemaKeywordUsesVersionOfMainSchema() throws Exception {
		final JsonSchemaDependency plain = new JsonSchemaDependency("https://example.com/plain.json", (JsonObject) json(
				"{\"prefixItems\": [{\"type\": \"string\"}]}"));
		final JsonSchema jsonSchema = schema("{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"allOf\": [{\"$ref\": \"https://example.com/plain.json\"}]}", null, plain);
		// Evaluated as draft 7: "prefixItems" is an unknown keyword and ignored
		assertValid(jsonSchema, "[1]");
	}

	@Test
	void configuredVersionOverridesSchemaKeywordOfMainDocument() throws Exception {
		// The configured version wins over "$schema" of the main document, also for its internal references
		final JsonSchema jsonSchema = schema("{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\", \"allOf\": [{\"$ref\": \"#/definitions/list\"}], \"definitions\": {\"list\": {\"prefixItems\": [{\"type\": \"string\"}]}}}",
				JsonSchemaVersion.draftV7);
		assertValid(jsonSchema, "[1]");
	}

	@Test
	void resolverViewSharesStateAndHasOwnVersion() throws Exception {
		final JsonObject olderDocument = (JsonObject) json("{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"string\"}");
		final JsonObject mainDocument = (JsonObject) json("{\"$ref\": \"https://example.com/older.json\"}");
		final JsonSchemaDependencyResolver resolver = new JsonSchemaDependencyResolver(mainDocument, new JsonSchemaDependency("https://example.com/older.json", olderDocument))
				.withJsonSchemaVersion(JsonSchemaVersion.draft2020_12);

		assertSame(resolver, resolver.getResolverForSchemaNode(mainDocument));
		final JsonSchemaDependencyResolver view = resolver.getResolverForSchemaNode(olderDocument);
		assertNotSame(resolver, view);
		assertEquals(JsonSchemaVersion.draftV7, view.getJsonSchemaVersion());
		assertSame(view, resolver.getResolverForSchemaNode(olderDocument), "Views are reused");
		assertSame(resolver, view.getResolverForSchemaNode(mainDocument), "A view leads back to the main resolver");
		assertTrue(view.isFormatAssertion(), "Draft 7 validates format by default");

		// Settings and documents are shared with the main resolver
		view.setFormatAssertion(Boolean.FALSE);
		assertEquals(Boolean.FALSE, resolver.isFormatAssertion());
		assertSame(olderDocument, view.getSchemaNodeByUri("https://example.com/older.json"));
		assertThrows(UnsupportedOperationException.class, () -> view.setJsonSchemaVersion(JsonSchemaVersion.draftV4));
	}

	@Test
	void simpleModeNeverSwitchesVersion() throws Exception {
		final JsonObject olderDocument = (JsonObject) json("{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"string\"}");
		final JsonSchemaDependencyResolver resolver = new JsonSchemaDependencyResolver((JsonObject) json("{}"), new JsonSchemaDependency("https://example.com/older.json", olderDocument))
				.withJsonSchemaVersion(JsonSchemaVersion.simple);
		assertSame(resolver, resolver.getResolverForSchemaNode(olderDocument));
	}

	@Test
	void versionViewDelegatesAllPublicMethods() throws Exception {
		final Class<?> versionViewClass = Class.forName(JsonSchemaDependencyResolver.class.getName() + "$VersionView");
		final List<String> missingMethods = new ArrayList<>();
		for (final Method method : JsonSchemaDependencyResolver.class.getDeclaredMethods()) {
			if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()) && !VERSION_DEPENDENT_METHODS.contains(method.getName())) {
				try {
					versionViewClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
				} catch (@SuppressWarnings("unused") final NoSuchMethodException e) {
					missingMethods.add(method.toString());
				}
			}
		}
		assertTrue(missingMethods.isEmpty(), "Methods not delegated by the version view: " + missingMethods);
	}
}
