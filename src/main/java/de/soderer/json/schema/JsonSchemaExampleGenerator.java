package de.soderer.json.schema;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonObjectMergeStrategy;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueNull;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueString;

/**
 * Generates a placeholder example data instance ({@link JsonNode}) for a given JSON Schema node.
 * <p>
 * Resolution rules, in order:
 * <ul>
 * <li>An explicit {@code "example"} on the (sub-)schema always wins and is returned as-is.</li>
 * <li>{@code "$ref"} is resolved via the {@link JsonSchemaDependencyResolver} passed to the constructor.</li>
 * <li>{@code "allOf"} sub-schemas are merged into a single object (first-seen property wins on conflicts).</li>
 * <li>{@code "oneOf"} / {@code "anyOf"}: there is no way to know which alternative is "correct" without more
 * context, so the first alternative is used.</li>
 * <li>{@code "enum"}: the first listed value is used.</li>
 * <li>Otherwise a placeholder value is generated from {@code "type"} (recursing into {@code "properties"} /
 * {@code "items"} / {@code "additionalProperties"} as needed).</li>
 * </ul>
 * Self-referencing schemas (directly or through a chain of "$ref"s) are cut off: once a reference key is
 * encountered a second time on the current path, an empty object/array is produced there instead of recursing
 * forever.
 */
public class JsonSchemaExampleGenerator {
	private final JsonSchemaDependencyResolver dependencyResolver;

	public JsonSchemaExampleGenerator(final JsonSchemaDependencyResolver dependencyResolver) {
		if (dependencyResolver == null) {
			throw new IllegalArgumentException("Invalid null value for JsonSchemaDependencyResolver");
		}
		this.dependencyResolver = dependencyResolver;
	}

	/**
	 * Generates an example data instance for the given schema node.
	 *
	 * @param schema the JSON Schema node to generate an example for (e.g. a "components/schemas/*" entry, or any
	 * nested sub-schema of it)
	 * @return a {@link JsonNode} (typically a {@link JsonObject} or {@link JsonArray}) representing a placeholder
	 * example, or {@code null} if {@code schema} itself was {@code null}
	 */
	public JsonNode generateExample(final JsonObject schema) throws Exception {
		return generateExample(schema, new HashSet<>());
	}

	private JsonNode generateExample(final JsonObject schema, final Set<String> activeReferences) throws Exception {
		if (schema == null) {
			return null;
		}

		final Object referenceValue = schema.getSimpleValue("$ref");
		if (referenceValue instanceof String) {
			final String referenceKey = (String) referenceValue;
			if (!activeReferences.add(referenceKey)) {
				// Cycle detected (e.g. a schema referencing itself, directly or transitively):
				// stop descending further and return an empty placeholder instead of recursing forever.
				return new JsonObject();
			}
			try {
				final JsonObject resolvedSchema = dependencyResolver.getDependencyByReference(referenceKey, new JsonSchemaPath());
				return generateExample(resolvedSchema, activeReferences);
			} finally {
				activeReferences.remove(referenceKey);
			}
		}

		final JsonNode explicitExample = schema.get("example");
		if (explicitExample != null) {
			return explicitExample;
		}

		if (schema.get("allOf") instanceof JsonArray) {
			final JsonObject merged = new JsonObject();
			for (final JsonNode subSchemaNode : ((JsonArray) schema.get("allOf")).items()) {
				if (subSchemaNode instanceof JsonObject) {
					final JsonNode subExample = generateExample((JsonObject) subSchemaNode, activeReferences);
					if (subExample instanceof JsonObject) {
						merged.merge((JsonObject) subExample, JsonObjectMergeStrategy.KEEP_EXISTING);
					}
				}
			}
			return merged;
		}

		for (final String alternativesKey : new String[] { "oneOf", "anyOf" }) {
			if (schema.get(alternativesKey) instanceof JsonArray) {
				final JsonArray alternatives = (JsonArray) schema.get(alternativesKey);
				if (!alternatives.isEmpty() && alternatives.get(0) instanceof JsonObject) {
					return generateExample((JsonObject) alternatives.get(0), activeReferences);
				}
			}
		}

		if (schema.get("enum") instanceof JsonArray) {
			final JsonArray enumValues = (JsonArray) schema.get("enum");
			if (!enumValues.isEmpty()) {
				return enumValues.get(0);
			}
		}

		final Object typeValue = schema.getSimpleValue("type");
		final String type = typeValue instanceof String ? (String) typeValue : null;

		if ("object".equals(type) || (type == null && schema.get("properties") instanceof JsonObject)) {
			return generateObjectExample(schema, activeReferences);
		} else if ("array".equals(type)) {
			return generateArrayExample(schema, activeReferences);
		} else if ("string".equals(type)) {
			return new JsonValueString(generateStringPlaceholder(schema));
		} else if ("integer".equals(type)) {
			return new JsonValueInteger(0);
		} else if ("number".equals(type)) {
			return new JsonValueNumber(0);
		} else if ("boolean".equals(type)) {
			return new JsonValueBoolean(true);
		} else if ("null".equals(type)) {
			return new JsonValueNull();
		} else {
			// Unknown/missing "type" (e.g. a bare "{}" schema): fall back to an empty object
			return new JsonObject();
		}
	}

	private JsonObject generateObjectExample(final JsonObject schema, final Set<String> activeReferences) throws Exception {
		final JsonObject result = new JsonObject();
		if (schema.get("properties") instanceof JsonObject) {
			for (final Entry<String, JsonNode> propertyEntry : ((JsonObject) schema.get("properties")).entrySet()) {
				if (propertyEntry.getValue() instanceof JsonObject) {
					result.add(propertyEntry.getKey(), generateExample((JsonObject) propertyEntry.getValue(), activeReferences));
				}
			}
		} else if (schema.get("additionalProperties") instanceof JsonObject) {
			result.add("key", generateExample((JsonObject) schema.get("additionalProperties"), activeReferences));
		}
		return result;
	}

	private JsonArray generateArrayExample(final JsonObject schema, final Set<String> activeReferences) throws Exception {
		final JsonArray result = new JsonArray();
		if (schema.get("items") instanceof JsonObject) {
			final JsonNode itemExample = generateExample((JsonObject) schema.get("items"), activeReferences);
			if (itemExample != null) {
				result.add(itemExample);
			}
		}
		return result;
	}

	private static String generateStringPlaceholder(final JsonObject schema) {
		final Object formatValue = schema.getSimpleValue("format");
		final String format = formatValue instanceof String ? (String) formatValue : null;
		if (format == null) {
			return "string";
		}
		switch (format) {
			case "date-time":
				return "2025-01-01T00:00:00Z";
			case "date":
				return "2025-01-01";
			case "time":
				return "00:00:00";
			case "email":
				return "user@example.com";
			case "uuid":
				return "00000000-0000-0000-0000-000000000000";
			case "uri":
			case "url":
				return "https://example.com";
			case "ipv4":
				return "127.0.0.1";
			case "ipv6":
				return "::1";
			case "binary":
			case "byte":
				return "";
			default:
				return "string";
		}
	}
}
