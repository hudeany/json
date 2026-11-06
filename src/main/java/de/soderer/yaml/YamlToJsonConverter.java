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
import de.soderer.json.JsonReader;
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
import de.soderer.yaml.exception.YamlAliasExpansionLimitException;

/**
 * Converts YAML data to JSON data.
 * <p>
 * Aliases are resolved by the anchors defined within the converted data, and merge keys ("&lt;&lt;")
 * of YAML 1.1 are applied: the properties of the referenced mapping (or mappings) are added unless
 * the mapping defines them itself. Integer numbers become JSON integer values, other numbers JSON
 * number values. Scalar mapping keys are converted to their text, e.g. the key 200 to "200".
 * Comments, anchors and styles are not part of JSON and get lost.
 * </p>
 * <p>
 * Each alias and merge key copies the referenced data into the JSON result. To protect against
 * "billion laughs" data, the number of nodes created by these copies is limited, see
 * {@link #DEFAULT_MAXIMUM_ALIAS_EXPANDED_NODES}. Data without aliases is not limited.
 * </p>
 */
public class YamlToJsonConverter {
	/** Merge key of YAML 1.1. */
	private static final String MERGE_KEY = "<<";

	/**
	 * Default maximum number of JSON nodes created by resolving aliases and merge keys.
	 */
	public static final int DEFAULT_MAXIMUM_ALIAS_EXPANDED_NODES = 100_000;

	/** Maximum number of JSON nodes created by resolving aliases and merge keys. */
	private final int maximumAliasExpandedNodes;

	/** Number of nested alias or merge key expansions at the current conversion position. */
	private int aliasExpansionDepth = 0;

	/** Number of JSON nodes created by alias or merge key expansions so far. */
	private int aliasExpandedNodes = 0;

	/**
	 * Maximum nesting depth of the created JSON data. Aliases can create data nested deeper than the
	 * YAML source, so the limit of the reader alone does not prevent a {@link StackOverflowError}.
	 */
	public static final int MAXIMUM_NESTING_DEPTH = JsonReader.DEFAULT_MAXIMUM_NESTING_DEPTH;

	/** Nesting depth of mappings and sequences at the current conversion position. */
	private int nestingDepth = 0;

	/**
	 * Anchor definitions converted so far. They are registered in document order, so an alias refers
	 * to the latest definition of its anchor before it, also if an anchor is defined again.
	 */
	private final Map<String, YamlNode> anchors = new HashMap<>();

	/** Aliases currently being resolved, to detect cyclic references. */
	private final Set<YamlNode> aliasTargetsInProgress = Collections.newSetFromMap(new IdentityHashMap<>());

	/**
	 * Creates a converter for data with the given anchor definitions.
	 *
	 * @param rootNode
	 *            the root node of the data, whose anchors are registered while converting
	 * @param maximumAliasExpandedNodes
	 *            the maximum number of JSON nodes created by resolving aliases and merge keys
	 */
	private YamlToJsonConverter(final YamlNode rootNode, final int maximumAliasExpandedNodes) {
		if (maximumAliasExpandedNodes < 0) {
			throw new IllegalArgumentException("Maximum number of alias expanded nodes must not be negative");
		}
		this.maximumAliasExpandedNodes = maximumAliasExpandedNodes;
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
		return convert(yamlDocument, DEFAULT_MAXIMUM_ALIAS_EXPANDED_NODES);
	}

	/**
	 * Converts the root node of a YAML document with a specific limit for alias expansion.
	 *
	 * @param yamlDocument
	 *            the YAML document, may be null
	 * @param maximumAliasExpandedNodes
	 *            the maximum number of JSON nodes created by resolving aliases and merge keys
	 * @return the JSON data, or null if the document or its root is null
	 * @throws DuplicateKeyException
	 *             if two mapping keys have the same text, e.g. the number 1 and the string "1"
	 * @throws YamlAliasExpansionLimitException
	 *             if alias expansion creates more nodes than allowed
	 * @throws RuntimeException
	 *             if the data contains null keys, complex keys, unknown or cyclic aliases
	 */
	public static JsonNode convert(final YamlDocument yamlDocument, final int maximumAliasExpandedNodes) throws DuplicateKeyException {
		if (yamlDocument == null || yamlDocument.getRoot() == null) {
			return null;
		} else {
			return convert(yamlDocument.getRoot(), maximumAliasExpandedNodes);
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
		return convert(yamlNode, DEFAULT_MAXIMUM_ALIAS_EXPANDED_NODES);
	}

	/**
	 * Converts a YAML node with a specific limit for alias expansion. Aliases may only refer to
	 * anchors within this node.
	 *
	 * @param yamlNode
	 *            the YAML node, may be null
	 * @param maximumAliasExpandedNodes
	 *            the maximum number of JSON nodes created by resolving aliases and merge keys
	 * @return the JSON data, or null if the node is null
	 * @throws DuplicateKeyException
	 *             if two mapping keys have the same text, e.g. the number 1 and the string "1"
	 * @throws YamlAliasExpansionLimitException
	 *             if alias expansion creates more nodes than allowed
	 * @throws RuntimeException
	 *             if the data contains null keys, complex keys, unknown or cyclic aliases
	 */
	public static JsonNode convert(final YamlNode yamlNode, final int maximumAliasExpandedNodes) throws DuplicateKeyException {
		if (yamlNode == null) {
			return null;
		} else {
			return new YamlToJsonConverter(yamlNode, maximumAliasExpandedNodes).convertNode(yamlNode);
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
	 * Registers the anchor of a node, which is no alias.
	 *
	 * @param node
	 *            the node
	 */
	private void registerAnchor(final YamlNode node) {
		if (node != null && !(node instanceof YamlAlias) && node.getAnchorName() != null) {
			anchors.put(node.getAnchorName(), node);
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
		registerAnchor(node);
		final YamlNode resolvedNode = resolve(node);
		if (!aliasTargetsInProgress.add(resolvedNode)) {
			throw new RuntimeException("Cyclic YAML alias reference found" + (node instanceof final YamlAlias alias ? ": '*" + alias.getTargetAnchorName() + "'" : ""));
		}
		final boolean isAlias = node instanceof YamlAlias;
		if (isAlias) {
			aliasExpansionDepth++;
		}
		final boolean isCollection = resolvedNode instanceof YamlMapping || resolvedNode instanceof YamlSequence;
		if (isCollection) {
			nestingDepth++;
		}
		try {
			countAliasExpandedNode();
			if (isCollection && nestingDepth > MAXIMUM_NESTING_DEPTH) {
				throw new RuntimeException("Maximum nesting depth of " + MAXIMUM_NESTING_DEPTH + " exceeded in converted YAML data");
			}
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
			if (isAlias) {
				aliasExpansionDepth--;
			}
			if (isCollection) {
				nestingDepth--;
			}
		}
	}

	/**
	 * Counts a JSON node created within an alias or merge key expansion.
	 *
	 * @throws YamlAliasExpansionLimitException
	 *             if the maximum number of expanded nodes is exceeded
	 */
	private void countAliasExpandedNode() {
		if (aliasExpansionDepth > 0) {
			aliasExpandedNodes++;
			if (aliasExpandedNodes > maximumAliasExpandedNodes) {
				throw new YamlAliasExpansionLimitException(maximumAliasExpandedNodes);
			}
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
					// Merged mappings are copies of their source data like aliases, even when written inline
					final JsonObject mergedObject;
					aliasExpansionDepth++;
					try {
						mergedObject = (JsonObject) convertNode(mergedMapping);
					} finally {
						aliasExpansionDepth--;
					}
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
		registerAnchor(key);
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
