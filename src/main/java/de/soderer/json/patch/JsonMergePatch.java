package de.soderer.json.patch;

import java.util.Map.Entry;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueNull;

/**
 * JSON Merge Patch as defined by RFC 7396: a JSON document that describes the changes by example.
 * Properties of the patch replace the properties of the document, nested objects are merged, and a
 * property with value null removes the property from the document. Arrays are replaced as a whole.
 * <p>
 * Example: the merge patch <code>{"name": "new", "comment": null}</code> sets "name" and removes
 * "comment". Merge patches are simpler than JSON Patch ({@link JsonPatch}), but they cannot set a
 * value to null and cannot change single array items.
 * </p>
 */
public final class JsonMergePatch {
	/**
	 * Utility class.
	 */
	private JsonMergePatch() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Applies a JSON Merge Patch to a JSON document.
	 *
	 * @param document
	 *            the document to patch, which is not changed; null is regarded as JSON null
	 * @param mergePatch
	 *            the merge patch; null is regarded as JSON null, which results in null
	 * @return the patched copy of the document
	 */
	public static JsonNode apply(final JsonNode document, final JsonNode mergePatch) {
		if (mergePatch instanceof final JsonObject patchObject) {
			final JsonObject result;
			if (document instanceof final JsonObject documentObject) {
				result = (JsonObject) JsonPatch.deepCopy(documentObject);
			} else {
				result = new JsonObject();
			}
			for (final Entry<String, JsonNode> patchEntry : patchObject.entrySet()) {
				if (patchEntry.getValue() == null || patchEntry.getValue().isNull()) {
					result.remove(patchEntry.getKey());
				} else {
					result.put(patchEntry.getKey(), apply(result.get(patchEntry.getKey()), patchEntry.getValue()));
				}
			}
			return result;
		} else {
			return JsonPatch.deepCopy(mergePatch);
		}
	}

	/**
	 * Creates a JSON Merge Patch, that changes the source document into the target document.
	 *
	 * @param source
	 *            the original document
	 * @param target
	 *            the changed document
	 * @return the merge patch, an empty object if both documents are equal objects
	 * @throws IllegalArgumentException
	 *             if the target contains null values within objects, which cannot be expressed by a
	 *             merge patch, because null means removal
	 */
	public static JsonNode create(final JsonNode source, final JsonNode target) {
		if (target instanceof final JsonObject targetObject) {
			final JsonObject patch = new JsonObject();
			final JsonObject sourceObject = source instanceof final JsonObject object ? object : null;
			if (sourceObject != null) {
				for (final String key : sourceObject.keySet()) {
					if (!targetObject.containsKey(key)) {
						patch.put(key, new JsonValueNull());
					}
				}
			}
			for (final Entry<String, JsonNode> targetEntry : targetObject.entrySet()) {
				final JsonNode targetValue = targetEntry.getValue();
				if (targetValue == null || targetValue.isNull()) {
					throw new IllegalArgumentException("Property '" + targetEntry.getKey() + "' has the value null, which cannot be expressed by a JSON Merge Patch");
				}
				final JsonNode sourceValue = sourceObject == null ? null : sourceObject.get(targetEntry.getKey());
				if (sourceValue == null) {
					checkNoNullValues(targetValue, targetEntry.getKey());
					patch.put(targetEntry.getKey(), JsonPatch.deepCopy(targetValue));
				} else if (!JsonPatch.jsonEquals(sourceValue, targetValue)) {
					if (targetValue instanceof JsonObject) {
						patch.put(targetEntry.getKey(), create(sourceValue, targetValue));
					} else {
						checkNoNullValues(targetValue, targetEntry.getKey());
						patch.put(targetEntry.getKey(), JsonPatch.deepCopy(targetValue));
					}
				}
			}
			return patch;
		} else {
			return JsonPatch.deepCopy(target);
		}
	}

	/**
	 * Checks that a value contains no null values within objects, which a merge patch would remove.
	 *
	 * @param value
	 *            the value to check
	 * @param key
	 *            the property key of the value for error messages
	 * @throws IllegalArgumentException
	 *             if a null value is found
	 */
	private static void checkNoNullValues(final JsonNode value, final String key) {
		if (value instanceof final JsonObject object) {
			for (final Entry<String, JsonNode> entry : object.entrySet()) {
				if (entry.getValue() == null || entry.getValue().isNull()) {
					throw new IllegalArgumentException("Property '" + key + "." + entry.getKey() + "' has the value null, which cannot be expressed by a JSON Merge Patch");
				}
				checkNoNullValues(entry.getValue(), key + "." + entry.getKey());
			}
		}
	}
}
