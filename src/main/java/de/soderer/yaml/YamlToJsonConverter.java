package de.soderer.yaml;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueNull;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueString;
import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.data.YamlStringQuoteType;

/**
 * Converts YAML data to JSON data.
 * <p>
 * Aliases are resolved by the anchors defined within the converted data, and merge keys ("&lt;&lt;")
 * of YAML 1.1 are applied: the properties of the referenced mapping (or mappings) are added unless
 * the mapping defines them itself. Integer numbers become JSON integer values, other numbers JSON
 * number values. Scalar mapping keys are converted to their text, e.g. the key 200 to "200".
 * Comments, anchors and styles are not part of JSON and get lost.
 * </p>
 */
public class YamlToJsonConverter {
	/** Merge key of YAML 1.1. */
	private static final String MERGE_KEY = "<<";

	/** Anchor definitions within the converted data. */
	private final Map<String, YamlNode> anchors = new HashMap<>();

	/** Aliases currently being resolved, to detect cyclic references. */
	private final Set<YamlNode> aliasTargetsInProgress = Collections.newSetFromMap(new IdentityHashMap<>());

	/**
	 * Creates a converter for data with the given anchor definitions.
	 *
	 * @param rootNode
	 *            the root node of the data to collect the anchors from
	 */
	private YamlToJsonConverter(final YamlNode rootNode) {
		collectAnchors(rootNode);
	}

	/**
	 * Converts the root node of a YAML document.
	 *
	 * @param yamlDocument
	 *            the YAML document, may be null
	 * @return the JSON data, or null if the document or its root is null
	 * @throws DuplicateKeyException
	 *             if two mapping keys have the same text, e.g. the number 1 and the string "1"
	 * @throws RuntimeException
	 *             if the data contains null keys, complex keys, unknown or cyclic aliases
	 */
	public static JsonNode convert(final YamlDocument yamlDocument) throws DuplicateKeyException {
		if (yamlDocument == null || yamlDocument.getRoot() == null) {
			return null;
		} else {
			return convert(yamlDocument.getRoot());
		}
	}

	/**
	 * Converts a YAML node. Aliases may only refer to anchors within this node.
	 *
	 * @param yamlNode
	 *            the YAML node, may be null
	 * @return the JSON data, or null if the node is null
	 * @throws DuplicateKeyException
	 *             if two mapping keys have the same text, e.g. the number 1 and the string "1"
	 * @throws RuntimeException
	 *             if the data contains null keys, complex keys, unknown or cyclic aliases
	 */
	public static JsonNode convert(final YamlNode yamlNode) throws DuplicateKeyException {
		if (yamlNode == null) {
			return null;
		} else {
			return new YamlToJsonConverter(yamlNode).convertNode(yamlNode);
		}
	}

	/**
	 * Converts a YAML mapping to a JSON object.
	 *
	 * @param yamlMapping
	 *            the YAML mapping, may be null
	 * @return the JSON object, or null if the mapping is null
	 * @throws DuplicateKeyException
	 *             if two mapping keys have the same text
	 * @throws RuntimeException
	 *             if the data contains null keys, complex keys, unknown or cyclic aliases
	 */
	public static JsonObject convert(final YamlMapping yamlMapping) throws DuplicateKeyException {
		return (JsonObject) convert((YamlNode) yamlMapping);
	}

	/**
	 * Converts a YAML sequence to a JSON array.
	 *
	 * @param yamlSequence
	 *            the YAML sequence, may be null
	 * @return the JSON array, or null if the sequence is null
	 * @throws DuplicateKeyException
	 *             if two keys of a contained mapping have the same text
	 * @throws RuntimeException
	 *             if the data contains null keys, complex keys, unknown or cyclic aliases
	 */
	public static JsonArray convert(final YamlSequence yamlSequence) throws DuplicateKeyException {
		return (JsonArray) convert((YamlNode) yamlSequence);
	}

	/**
	 * Converts a YAML scalar to a JSON simple value.
	 *
	 * @param yamlScalar
	 *            the YAML scalar, may be null
	 * @return the JSON value, or null if the scalar is null
	 */
	public static JsonNode convert(final YamlScalar yamlScalar) {
		if (yamlScalar == null) {
			return null;
		} else {
			switch (yamlScalar.getType()) {
				case NULL_VALUE:
					return new JsonValueNull();
				case STRING:
				case MULTILINE:
					return new JsonValueString((String) yamlScalar.getValue());
				case NUMBER:
					return convertNumber(yamlScalar);
				case BOOLEAN:
					return new JsonValueBoolean((Boolean) yamlScalar.getValue());
				default:
					throw new RuntimeException("Unsupported YamlScalar value type found: '" + yamlScalar.getType().name() + "'");
			}
		}
	}

	/**
	 * Converts a YAML number to a JSON integer value or number value.
	 *
	 * @param yamlScalar
	 *            the YAML scalar of type NUMBER
	 * @return the JSON value
	 */
	private static JsonNode convertNumber(final YamlScalar yamlScalar) {
		final Number number = (Number) yamlScalar.getValue();
		if (number instanceof Integer) {
			return new JsonValueInteger((Integer) number);
		} else if (number instanceof Long) {
			return new JsonValueInteger((Long) number);
		} else if (number instanceof BigInteger) {
			return new JsonValueInteger((BigInteger) number);
		} else if (number instanceof BigDecimal && isIntegerNotation(yamlScalar.getValueString())) {
			return new JsonValueInteger((BigDecimal) number);
		} else {
			return new JsonValueNumber(number);
		}
	}

	/**
	 * Checks whether a number was written without fraction and exponent.
	 *
	 * @param numberString
	 *            the number as written
	 * @return true, if the number has integer notation
	 */
	private static boolean isIntegerNotation(final String numberString) {
		return numberString.indexOf('.') < 0 && numberString.indexOf('e') < 0 && numberString.indexOf('E') < 0;
	}

	/**
	 * Collects the anchor definitions of a YAML tree, keys included.
	 *
	 * @param node
	 *            the node to start at
	 */
	private void collectAnchors(final YamlNode node) {
		if (node == null) {
			return;
		}
		if (node.getAnchorName() != null) {
			anchors.put(node.getAnchorName(), node);
		}
		if (node instanceof final YamlMapping mapping) {
			for (final Entry<YamlNode, YamlNode> entry : mapping.entrySet()) {
				collectAnchors(entry.getKey());
				collectAnchors(entry.getValue());
			}
		} else if (node instanceof final YamlSequence sequence) {
			for (final YamlNode item : sequence.items()) {
				collectAnchors(item);
			}
		}
	}

	/**
	 * Returns the node an alias refers to, or the node itself if it is no alias.
	 *
	 * @param node
	 *            the node
	 * @return the resolved node
	 * @throws RuntimeException
	 *             if the alias refers to an unknown anchor
	 */
	private YamlNode resolve(final YamlNode node) {
		if (node instanceof final YamlAlias alias) {
			final YamlNode target = anchors.get(alias.getTargetAnchorName());
			if (target == null) {
				throw new RuntimeException("Unknown YAML alias: '*" + alias.getTargetAnchorName() + "'");
			}
			return target;
		} else {
			return node;
		}
	}

	/**
	 * Converts any YAML node, resolving aliases.
	 *
	 * @param node
	 *            the node
	 * @return the JSON data
	 * @throws DuplicateKeyException
	 *             if two mapping keys have the same text
	 */
	private JsonNode convertNode(final YamlNode node) throws DuplicateKeyException {
		final YamlNode resolvedNode = resolve(node);
		if (!aliasTargetsInProgress.add(resolvedNode)) {
			throw new RuntimeException("Cyclic YAML alias reference found" + (node instanceof final YamlAlias alias ? ": '*" + alias.getTargetAnchorName() + "'" : ""));
		}
		try {
			if (resolvedNode instanceof final YamlMapping mapping) {
				return convertMapping(mapping);
			} else if (resolvedNode instanceof final YamlSequence sequence) {
				final JsonArray jsonArray = new JsonArray();
				for (final YamlNode item : sequence.items()) {
					jsonArray.add(convertNode(item));
				}
				return jsonArray;
			} else if (resolvedNode instanceof final YamlScalar scalar) {
				return convert(scalar);
			} else {
				throw new RuntimeException("Unsupported YamlNode type found: '" + resolvedNode.getClass().getSimpleName() + "'");
			}
		} finally {
			aliasTargetsInProgress.remove(resolvedNode);
		}
	}

	/**
	 * Converts a YAML mapping, applying merge keys.
	 *
	 * @param mapping
	 *            the mapping
	 * @return the JSON object
	 * @throws DuplicateKeyException
	 *             if two keys have the same text
	 */
	private JsonObject convertMapping(final YamlMapping mapping) throws DuplicateKeyException {
		final Set<String> explicitKeys = new HashSet<>();
		for (final YamlNode key : mapping.keySet()) {
			if (!isMergeKey(key)) {
				explicitKeys.add(convertKey(key));
			}
		}

		final JsonObject jsonObject = new JsonObject();
		for (final Entry<YamlNode, YamlNode> entry : mapping.entrySet()) {
			if (isMergeKey(entry.getKey())) {
				for (final YamlMapping mergedMapping : getMergedMappings(entry.getValue())) {
					final JsonObject mergedObject = (JsonObject) convertNode(mergedMapping);
					for (final Entry<String, JsonNode> mergedEntry : mergedObject.entrySet()) {
						// Explicit keys and keys of earlier merged mappings take precedence
						if (!explicitKeys.contains(mergedEntry.getKey()) && !jsonObject.containsKey(mergedEntry.getKey())) {
							jsonObject.add(mergedEntry.getKey(), mergedEntry.getValue());
						}
					}
				}
			} else {
				jsonObject.add(convertKey(entry.getKey()), convertNode(entry.getValue()));
			}
		}
		return jsonObject;
	}

	/**
	 * Returns the mappings referenced by the value of a merge key.
	 *
	 * @param mergeValue
	 *            a mapping, an alias of a mapping, or a sequence of those
	 * @return the mappings to merge in the order of precedence
	 */
	private List<YamlMapping> getMergedMappings(final YamlNode mergeValue) {
		final List<YamlMapping> mergedMappings = new ArrayList<>();
		final YamlNode resolvedValue = resolve(mergeValue);
		if (resolvedValue instanceof final YamlMapping mergedMapping) {
			mergedMappings.add(mergedMapping);
		} else if (resolvedValue instanceof final YamlSequence sequence) {
			for (final YamlNode item : sequence.items()) {
				if (resolve(item) instanceof final YamlMapping mergedMapping) {
					mergedMappings.add(mergedMapping);
				} else {
					throw new RuntimeException("Invalid YAML merge key value: sequence items must be mappings");
				}
			}
		} else {
			throw new RuntimeException("Invalid YAML merge key value: must be a mapping or a sequence of mappings");
		}
		return mergedMappings;
	}

	/**
	 * Checks for the unquoted merge key "&lt;&lt;".
	 *
	 * @param key
	 *            the mapping key
	 * @return true, if the key is a merge key
	 */
	private static boolean isMergeKey(final YamlNode key) {
		return key instanceof final YamlScalar scalarKey
				&& scalarKey.getType() == YamlScalarType.STRING
				&& MERGE_KEY.equals(scalarKey.getValue())
				&& (scalarKey.getQuoteType() == null || scalarKey.getQuoteType() == YamlStringQuoteType.NONE);
	}

	/**
	 * Converts a mapping key to the text of a JSON property key.
	 *
	 * @param key
	 *            the mapping key, an alias of a scalar is resolved
	 * @return the key text
	 * @throws RuntimeException
	 *             for null keys and complex keys
	 */
	private String convertKey(final YamlNode key) {
		if (resolve(key) instanceof final YamlScalar keyScalar) {
			if (keyScalar.getType() == YamlScalarType.NULL_VALUE) {
				throw new RuntimeException("Cannot create JsonObject, because YamlMapping contains a null key");
			} else {
				return keyScalar.getValue().toString();
			}
		} else {
			throw new RuntimeException("Cannot create JsonObject, because YamlMapping contains complex key: '" + key.getClass().getSimpleName() + "'");
		}
	}
}
