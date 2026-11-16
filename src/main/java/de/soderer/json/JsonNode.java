package de.soderer.json;

import java.util.List;

import de.soderer.json.path.JsonPath;
import de.soderer.json.path.JsonPathArrayElement;
import de.soderer.json.path.JsonPathElement;
import de.soderer.json.path.JsonPathEvaluator;
import de.soderer.json.path.JsonPathException;
import de.soderer.json.path.JsonPathPropertyElement;
import de.soderer.json.path.JsonPathRoot;
import de.soderer.json.path.JsonPathUnionElement;
import de.soderer.json.path.JsonPathUnionElement.IndexSelector;
import de.soderer.json.path.JsonPathUnionElement.NameSelector;
import de.soderer.json.path.JsonPathUnionElement.Selector;

/**
 * Base class of all JSON data nodes: objects, arrays and simple values.
 * <p>
 * A node may be marked as root node of a JSON document, which is required for JSON paths starting
 * with "$".
 * </p>
 */
public class JsonNode {
	/**
	 * The data type of this node.
	 */
	protected final JsonDataType jsonDataType;

	/**
	 * True, if this node is the root of a JSON document.
	 */
	private boolean rootNode;

	/**
	 * Creates a new node.
	 *
	 * @param jsonDataType
	 *            the data type of the node
	 */
	protected JsonNode(final JsonDataType jsonDataType) {
		this.jsonDataType = jsonDataType;
	}

	/**
	 * Returns the data type of this node.
	 *
	 * @return the data type
	 */
	public JsonDataType getJsonDataType() {
		return jsonDataType;
	}

	/**
	 * Returns whether this node is the root of a JSON document.
	 *
	 * @return true, if this is a root node
	 */
	public boolean isRootNode() {
		return rootNode;
	}

	/**
	 * Sets whether this node is the root of a JSON document.
	 *
	 * @param rootNode
	 *            true, if this is a root node
	 */
	public void setRootNode(final boolean rootNode) {
		this.rootNode = rootNode;
	}

	/**
	 * Sets whether this node is the root of a JSON document. Note that this changes this node and
	 * does not create a copy.
	 *
	 * @param newRootNode
	 *            true, if this is a root node
	 * @return this node for chaining
	 */
	public JsonNode withRootNode(final boolean newRootNode) {
		setRootNode(newRootNode);
		return this;
	}

	/**
	 * Checks whether this node is the JSON null value.
	 *
	 * @return true, if this node is the JSON null value
	 */
	public boolean isNull() {
		return jsonDataType == JsonDataType.NULL;
	}

	/**
	 * Checks whether this node is a boolean value.
	 *
	 * @return true, if this node is a boolean value
	 */
	public boolean isBoolean() {
		return jsonDataType == JsonDataType.BOOLEAN;
	}

	/**
	 * Checks whether this node is a number without fraction.
	 *
	 * @return true, if this node is a number without fraction
	 */
	public boolean isInteger() {
		return jsonDataType == JsonDataType.INTEGER;
	}

	/**
	 * Checks whether this node is a number with fraction or exponent (not an integer, see {@link #isInteger()}).
	 *
	 * @return true, if this node is a number with fraction or exponent
	 */
	public boolean isNumber() {
		return jsonDataType == JsonDataType.NUMBER;
	}

	/**
	 * Checks whether this node is a string value.
	 *
	 * @return true, if this node is a string value
	 */
	public boolean isString() {
		return jsonDataType == JsonDataType.STRING;
	}

	/**
	 * Checks whether this node is a JSON object.
	 *
	 * @return true, if this node is a JSON object
	 */
	public boolean isJsonObject() {
		return jsonDataType == JsonDataType.OBJECT;
	}

	/**
	 * Checks whether this node is a JSON array.
	 *
	 * @return true, if this node is a JSON array
	 */
	public boolean isJsonArray() {
		return jsonDataType == JsonDataType.ARRAY;
	}

	/**
	 * Checks whether this node is a simple value: null, boolean, integer, number or string.
	 *
	 * @return true, if this node is neither object nor array
	 */
	public boolean isSimpleValue() {
		return jsonDataType == JsonDataType.NULL
				|| jsonDataType == JsonDataType.BOOLEAN
				|| jsonDataType == JsonDataType.INTEGER
				|| jsonDataType == JsonDataType.NUMBER
				|| jsonDataType == JsonDataType.STRING;
	}

	/**
	 * Returns the node at a JSON path relative to this node. A path starting with "$" requires
	 * this node to be a root node. Negative indexes like "[-1]" are supported. Elements which can
	 * select several nodes (wildcards, recursive descents, filters, slices and unions) are not
	 * supported, see {@link #getDataListByJsonPath(JsonPath)}.
	 *
	 * @param jsonPath
	 *            the path to evaluate
	 * @return the node at the path
	 * @throws JsonPathException
	 *             if the path does not exist or contains unsupported elements
	 */
	public JsonNode getDataByJsonPath(final JsonPath jsonPath) throws JsonPathException {
		JsonNode nextDataObject = this;
		for (final JsonPathElement pathPart : jsonPath.getPathParts()) {
			if (pathPart instanceof JsonPathArrayElement) {
				if (nextDataObject != null && nextDataObject instanceof JsonArray) {
					final JsonArray jsonArray = (JsonArray) nextDataObject;
					final int lookingForIndex = ((JsonPathArrayElement) pathPart).getIndex();
					if (jsonArray.size() > lookingForIndex) {
						nextDataObject = jsonArray.get(lookingForIndex);
					} else {
						throw new JsonPathException("JsonNode does not include path", jsonPath);
					}
				} else {
					throw new JsonPathException("JsonNode does not include path", jsonPath);
				}
			} else if (pathPart instanceof JsonPathPropertyElement) {
				if (nextDataObject != null && nextDataObject instanceof JsonObject) {
					final JsonObject jsonObject = (JsonObject) nextDataObject;
					final String lookingForPropertyKey = ((JsonPathPropertyElement) pathPart).getPropertyKey();
					if (jsonObject.containsKey(lookingForPropertyKey)) {
						nextDataObject = jsonObject.get(lookingForPropertyKey);
					} else {
						throw new JsonPathException("JsonNode does not include path", jsonPath);
					}
				} else {
					throw new JsonPathException("JsonNode does not include path", jsonPath);
				}
			} else if (pathPart instanceof JsonPathRoot) {
				if (!rootNode) {
					throw new JsonPathException("JsonNode is not a root", jsonPath);
				}
			} else if (pathPart instanceof JsonPathUnionElement && ((JsonPathUnionElement) pathPart).isSingular()) {
				// A single selector like "[-1]" selects at most one node
				nextDataObject = getSingularUnionMatch(nextDataObject, (JsonPathUnionElement) pathPart, jsonPath);
			} else {
				throw new JsonPathException("Unexpected JsonPathElement", jsonPath);
			}
		}
		return nextDataObject;
	}

	/**
	 * Resolves a union element with a single property key or index (e.g. a negative index "[-1]").
	 *
	 * @param node
	 *            the node to select from
	 * @param unionElement
	 *            the singular union element
	 * @param jsonPath
	 *            the whole path for error messages
	 * @return the selected node
	 * @throws JsonPathException
	 *             if the node does not contain the selected key or index
	 */
	private static JsonNode getSingularUnionMatch(final JsonNode node, final JsonPathUnionElement unionElement, final JsonPath jsonPath) throws JsonPathException {
		final Selector selector = unionElement.getSelectors().get(0);
		if (selector instanceof NameSelector && node instanceof JsonObject && ((JsonObject) node).containsKey(((NameSelector) selector).propertyKey())) {
			return ((JsonObject) node).get(((NameSelector) selector).propertyKey());
		} else if (selector instanceof IndexSelector && node instanceof JsonArray) {
			final JsonArray jsonArray = (JsonArray) node;
			final int index = ((IndexSelector) selector).index();
			final int normalizedIndex = index >= 0 ? index : jsonArray.size() + index;
			if (normalizedIndex >= 0 && normalizedIndex < jsonArray.size()) {
				return jsonArray.get(normalizedIndex);
			}
		}
		throw new JsonPathException("JsonNode does not include path", jsonPath);
	}

	/**
	 * Like {@link #getDataByJsonPath(JsonPath)}, but also supports paths which can match several
	 * nodes: wildcards ({@code *} / {@code [*]}), recursive descents ({@code ..}), filters
	 * ({@code [?(@.price < 10 && @.category == 'fiction')]}), slices ({@code [0:2]}) and unions
	 * ({@code [0,2]}), so this returns a list instead of a single node. See {@link JsonPath} for the
	 * syntax and {@link de.soderer.json.path.JsonPathFilterExpression} for the filter semantics.
	 *
	 * <p>
	 * A path without any of these elements behaves exactly like
	 * {@link #getDataByJsonPath(JsonPath)}, except that the single match is wrapped in a
	 * one-element list instead of being returned directly. The same missing-property/missing-index
	 * errors are thrown in that case.
	 * </p>
	 *
	 * <p>
	 * Once an element selecting several nodes has been evaluated ("fanned out"), every following
	 * step is applied leniently: a candidate that does not have the required property/index is
	 * silently dropped instead of aborting the whole query, since "give me X.field for every X that
	 * has one" is the expected behaviour once several candidates are in play.
	 * </p>
	 *
	 * <p>
	 * A filter behaves slightly differently depending on this fanned-out state:
	 * </p>
	 * <ul>
	 * <li>Before the first fan-out, {@code container[?(...)]} descends into the container's
	 * children and keeps the matching ones (matching plain JSONPath's combined
	 * "select children, then filter" syntax, e.g. {@code $.items[?(@.price<10)]}).</li>
	 * <li>Once already fanned out, an array candidate is replaced by its matching items (e.g.
	 * {@code $..book[?(@.price<10)]}), while any other candidate is tested directly instead of
	 * descending further (e.g. {@code $.*[?(@.version=='1.0')]} tests every value produced by
	 * {@code *}).</li>
	 * <li>Directly after a recursive descent the filter selects the matching children of every
	 * node, as defined by RFC 9535 (e.g. {@code $..[?(@.price<10)]}).</li>
	 * </ul>
	 *
	 * @param jsonPath
	 *            path to evaluate, may contain wildcards, recursive descents, filters, slices and
	 *            unions
	 * @return all matching nodes, empty if nothing matches after an element selecting several
	 *         nodes
	 * @throws JsonPathException
	 *             if a required property/index is missing before the first fan-out, the path does
	 *             not start at a root node as required, or a wildcard/filter is applied to a simple
	 *             value before the first fan-out
	 */
	public List<JsonNode> getDataListByJsonPath(final JsonPath jsonPath) throws JsonPathException {
		return JsonPathEvaluator.evaluate(this, jsonPath);
	}
}
