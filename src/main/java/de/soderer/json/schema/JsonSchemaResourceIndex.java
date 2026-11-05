package de.soderer.json.schema;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueString;

/**
 * Index of all schema resources known to a {@link JsonSchemaDependencyResolver}.<br />
 * <br />
 * Each added schema document is walked once. Every schema object gets the base URI in effect for
 * it (changed by "$id", or "id" up to draft 4), and every schema resource, plain name anchor
 * ("$anchor", "$dynamicAnchor", or "$id": "#name" up to draft 7) is registered by its absolute URI.
 * References are then resolved by absolute URI: either directly (resource or anchor) or by a JSON
 * pointer fragment relative to a resource.
 */
public class JsonSchemaResourceIndex {
	/** Schema resources by absolute URI without fragment. */
	private final Map<String, JsonNode> resources = new HashMap<>();

	/** Plain name anchors by absolute URI with fragment ("uri#name"). */
	private final Map<String, JsonNode> anchors = new HashMap<>();

	/** Dynamic anchors ("$dynamicAnchor", draft 2020-12) by absolute URI with fragment. */
	private final Set<String> dynamicAnchors = new HashSet<>();

	/** Resource URIs whose root schema has "$recursiveAnchor": true (draft 2019-09). */
	private final Set<String> recursiveAnchorResources = new HashSet<>();

	/** Base URI (resource URI) in effect for each indexed schema object. */
	private final Map<JsonNode, String> baseUris = new IdentityHashMap<>();

	/** JSON schema version of each indexed document root, detected from "$schema". */
	private final Map<String, JsonSchemaVersion> documentVersions = new HashMap<>();

	/**
	 * Keywords whose values are data, not schemas. Identifiers within them must not be indexed.
	 */
	private static final Set<String> NON_SCHEMA_KEYWORDS = Set.of("enum", "const", "examples", "default", "example");

	/**
	 * Keywords whose values are a schema or an array of schemas (in any supported version).
	 */
	private static final Set<String> SCHEMA_KEYWORDS = Set.of(
			"additionalItems", "additionalProperties", "allOf", "anyOf", "contains", "contentSchema", "else", "extends", "if", "items",
			"not", "oneOf", "prefixItems", "propertyNames", "then", "unevaluatedItems", "unevaluatedProperties", "disallow", "type");

	/**
	 * Keywords whose values are objects mapping names to schemas (in any supported version).
	 */
	private static final Set<String> SCHEMA_MAP_KEYWORDS = Set.of("$defs", "definitions", "dependencies", "dependentSchemas", "patternProperties", "properties");

	/**
	 * Creates an empty index. Schema documents are added by
	 * {@link #addDocument(String, JsonNode, JsonSchemaVersion)}.
	 */
	public JsonSchemaResourceIndex() {
		// Documents are added by addDocument()
	}

	/**
	 * Adds a schema document and indexes all its resources and anchors.
	 *
	 * @param retrievalUri
	 *            the URI the document was retrieved from or is registered by, used as initial base
	 *            URI
	 * @param document
	 *            the schema document (object or boolean)
	 * @param defaultVersion
	 *            the version used, if the document does not declare a known "$schema"
	 */
	public void addDocument(final String retrievalUri, final JsonNode document, final JsonSchemaVersion defaultVersion) {
		final String documentUri = JsonSchemaUriUtilities.removeFragment(JsonSchemaUriUtilities.normalize(retrievalUri));
		JsonSchemaVersion version = defaultVersion;
		if (document instanceof JsonObject && ((JsonObject) document).get("$schema") instanceof JsonValueString) {
			final JsonSchemaVersion declaredVersion = JsonSchemaVersion.getJsonSchemaVersionByVersionUrl(((JsonValueString) ((JsonObject) document).get("$schema")).getValue());
			if (declaredVersion != null) {
				version = declaredVersion;
			}
		}
		resources.put(documentUri, document);
		documentVersions.put(documentUri, version);
		walk(document, documentUri, version);
	}

	/**
	 * Checks whether a document or resource is known by its URI.
	 *
	 * @param uri
	 *            the URI, a fragment is ignored
	 * @return true, if a resource with this URI is indexed
	 */
	public boolean containsResource(final String uri) {
		return resources.containsKey(JsonSchemaUriUtilities.removeFragment(JsonSchemaUriUtilities.normalize(uri)));
	}

	/**
	 * Returns the schema node identified by an absolute URI.
	 *
	 * @param absoluteUri
	 *            the URI, optionally with JSON pointer or plain name fragment
	 * @return the schema node, or null if the resource is not indexed or the fragment does not exist
	 */
	public JsonNode get(final String absoluteUri) {
		final String normalizedUri = JsonSchemaUriUtilities.normalize(absoluteUri);
		final String resourceUri = JsonSchemaUriUtilities.removeFragment(normalizedUri);
		final String fragment = JsonSchemaUriUtilities.getFragment(normalizedUri);
		final JsonNode resource = resources.get(resourceUri);
		if (resource == null) {
			return null;
		} else if (fragment == null || fragment.isEmpty()) {
			return resource;
		} else if (JsonSchemaUriUtilities.isJsonPointerFragment(fragment)) {
			return getByJsonPointer(resource, JsonSchemaUriUtilities.percentDecode(fragment));
		} else {
			final JsonNode anchorNode = anchors.get(resourceUri + "#" + fragment);
			if (anchorNode != null) {
				return anchorNode;
			} else {
				return anchors.get(resourceUri + "#" + JsonSchemaUriUtilities.percentDecode(fragment));
			}
		}
	}

	/**
	 * Returns the node at a JSON pointer position.
	 *
	 * @param startNode
	 *            the node the pointer is relative to
	 * @param jsonPointer
	 *            the decoded JSON pointer, e.g. "/$defs/a~1b"
	 * @return the node, or null if the position does not exist
	 */
	public static JsonNode getByJsonPointer(final JsonNode startNode, final String jsonPointer) {
		if (jsonPointer == null || jsonPointer.isEmpty()) {
			return startNode;
		}
		JsonNode currentNode = startNode;
		// Split with limit -1 to keep empty tokens like in "/a//b"
		final String[] tokens = jsonPointer.substring(1).split("/", -1);
		for (final String rawToken : tokens) {
			final String token = JsonSchemaUriUtilities.decodeJsonPointerToken(rawToken);
			if (currentNode instanceof JsonObject) {
				if (!((JsonObject) currentNode).containsKey(token)) {
					return null;
				}
				currentNode = ((JsonObject) currentNode).get(token);
			} else if (currentNode instanceof JsonArray) {
				try {
					final int index = Integer.parseInt(token);
					if (index < 0 || index >= ((JsonArray) currentNode).size()) {
						return null;
					}
					currentNode = ((JsonArray) currentNode).get(index);
				} catch (@SuppressWarnings("unused") final NumberFormatException e) {
					return null;
				}
			} else {
				return null;
			}
		}
		return currentNode;
	}

	/**
	 * Returns the base URI in effect for an indexed schema object.
	 *
	 * @param schemaNode
	 *            the schema object
	 * @return the base URI, or null if the object is not indexed
	 */
	public String getBaseUri(final JsonNode schemaNode) {
		return baseUris.get(schemaNode);
	}

	/**
	 * Checks whether a schema resource defines a dynamic anchor (draft 2020-12).
	 *
	 * @param resourceUri
	 *            the URI of the schema resource
	 * @param anchorName
	 *            the anchor name
	 * @return true, if the resource contains "$dynamicAnchor" with this name
	 */
	public boolean hasDynamicAnchor(final String resourceUri, final String anchorName) {
		return dynamicAnchors.contains(JsonSchemaUriUtilities.removeFragment(resourceUri) + "#" + anchorName);
	}

	/**
	 * Checks whether the root schema of a resource has "$recursiveAnchor": true (draft 2019-09).
	 *
	 * @param resourceUri
	 *            the URI of the schema resource
	 * @return true, if the recursive anchor is set
	 */
	public boolean hasRecursiveAnchor(final String resourceUri) {
		return recursiveAnchorResources.contains(JsonSchemaUriUtilities.removeFragment(resourceUri));
	}

	/**
	 * Returns the JSON schema version of an indexed document.
	 *
	 * @param documentUri
	 *            the URI the document was added by
	 * @return the version, or null if unknown
	 */
	public JsonSchemaVersion getDocumentVersion(final String documentUri) {
		return documentVersions.get(JsonSchemaUriUtilities.removeFragment(JsonSchemaUriUtilities.normalize(documentUri)));
	}

	/**
	 * Walks a schema node recursively and registers base URIs, resources and anchors.
	 *
	 * @param node
	 *            the current schema node
	 * @param baseUri
	 *            the base URI in effect for the parent of the node
	 * @param version
	 *            the JSON schema version used to interpret identifier keywords
	 */
	private void walk(final JsonNode node, final String baseUri, final JsonSchemaVersion version) {
		if (node instanceof JsonObject) {
			final JsonObject jsonObject = (JsonObject) node;
			final String currentBaseUri = registerIdentifiers(jsonObject, baseUri, version);
			if (!baseUris.containsKey(jsonObject)) {
				baseUris.put(jsonObject, currentBaseUri);
			}
			for (final Entry<String, JsonNode> entry : jsonObject.entrySet()) {
				final JsonNode value = entry.getValue();
				if (NON_SCHEMA_KEYWORDS.contains(entry.getKey()) || !(value instanceof JsonObject || value instanceof JsonArray)) {
					continue;
				} else if (SCHEMA_KEYWORDS.contains(entry.getKey())) {
					walk(value, currentBaseUri, version);
				} else if (SCHEMA_MAP_KEYWORDS.contains(entry.getKey()) && value instanceof JsonObject) {
					for (final JsonNode subSchema : ((JsonObject) value).values()) {
						walk(subSchema, currentBaseUri, version);
					}
					recordBaseUris(value, currentBaseUri);
				} else {
					// Unknown keyword: its value is no schema, identifiers within it must not be registered
					recordBaseUris(value, currentBaseUri);
				}
			}
		} else if (node instanceof JsonArray) {
			for (final JsonNode item : ((JsonArray) node).items()) {
				walk(item, baseUri, version);
			}
		}
	}

	/**
	 * Records the base URI for all objects within a node that is no schema (e.g. the value of an
	 * unknown keyword), without registering identifiers. Such objects may still be referenced by
	 * JSON pointer and then need a base URI for their own references.
	 *
	 * @param node
	 *            the node
	 * @param baseUri
	 *            the base URI in effect
	 */
	private void recordBaseUris(final JsonNode node, final String baseUri) {
		if (node instanceof JsonObject) {
			baseUris.putIfAbsent(node, baseUri);
			for (final JsonNode value : ((JsonObject) node).values()) {
				recordBaseUris(value, baseUri);
			}
		} else if (node instanceof JsonArray) {
			for (final JsonNode item : ((JsonArray) node).items()) {
				recordBaseUris(item, baseUri);
			}
		}
	}

	/**
	 * Registers the identifier keywords of a schema object.
	 *
	 * @param jsonObject
	 *            the schema object
	 * @param baseUri
	 *            the base URI in effect for the parent of the object
	 * @param version
	 *            the JSON schema version
	 * @return the base URI in effect for the object itself
	 */
	private String registerIdentifiers(final JsonObject jsonObject, final String baseUri, final JsonSchemaVersion version) {
		String currentBaseUri = baseUri;
		if (version != null && version.isAtLeast(JsonSchemaVersion.draft2019_09)) {
			final JsonNode idNode = jsonObject.get("$id");
			if (idNode instanceof JsonValueString) {
				final String resolvedId = JsonSchemaUriUtilities.resolve(baseUri, ((JsonValueString) idNode).getValue());
				currentBaseUri = JsonSchemaUriUtilities.removeFragment(resolvedId);
				resources.putIfAbsent(currentBaseUri, jsonObject);
			}
			final JsonNode anchorNode = jsonObject.get("$anchor");
			if (anchorNode instanceof JsonValueString) {
				anchors.putIfAbsent(currentBaseUri + "#" + ((JsonValueString) anchorNode).getValue(), jsonObject);
			}
			final JsonNode dynamicAnchorNode = jsonObject.get("$dynamicAnchor");
			if (dynamicAnchorNode instanceof JsonValueString && version.isAtLeast(JsonSchemaVersion.draft2020_12)) {
				final String anchorUri = currentBaseUri + "#" + ((JsonValueString) dynamicAnchorNode).getValue();
				anchors.putIfAbsent(anchorUri, jsonObject);
				dynamicAnchors.add(anchorUri);
			}
			final JsonNode recursiveAnchorNode = jsonObject.get("$recursiveAnchor");
			if (recursiveAnchorNode instanceof JsonValueBoolean && ((JsonValueBoolean) recursiveAnchorNode).getValue() && version == JsonSchemaVersion.draft2019_09) {
				recursiveAnchorResources.add(currentBaseUri);
			}
		} else {
			// Up to draft 7 "$ref" makes all sibling keywords ignored, including the identifier
			if (!jsonObject.containsKey("$ref")) {
				JsonNode idNode = null;
				if (version == JsonSchemaVersion.draftV3 || version == JsonSchemaVersion.draftV4) {
					idNode = jsonObject.get("id");
				} else if (version == JsonSchemaVersion.draftV6 || version == JsonSchemaVersion.draftV7) {
					idNode = jsonObject.get("$id");
				} else {
					// Simple mode: accept both spellings
					idNode = jsonObject.get("$id") instanceof JsonValueString ? jsonObject.get("$id") : jsonObject.get("id");
				}
				if (idNode instanceof JsonValueString) {
					final String idValue = ((JsonValueString) idNode).getValue();
					final String resolvedId = JsonSchemaUriUtilities.resolve(baseUri, idValue);
					final String fragment = JsonSchemaUriUtilities.getFragment(resolvedId);
					if (!idValue.startsWith("#")) {
						currentBaseUri = JsonSchemaUriUtilities.removeFragment(resolvedId);
						resources.putIfAbsent(currentBaseUri, jsonObject);
					}
					if (fragment != null && !fragment.isEmpty() && !JsonSchemaUriUtilities.isJsonPointerFragment(fragment)) {
						anchors.putIfAbsent(JsonSchemaUriUtilities.removeFragment(resolvedId) + "#" + fragment, jsonObject);
					}
				}
			}
		}
		return currentBaseUri;
	}
}
