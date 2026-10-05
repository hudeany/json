package de.soderer.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.soderer.json.path.JsonPath;
import de.soderer.json.path.JsonPathArrayElement;
import de.soderer.json.path.JsonPathElement;
import de.soderer.json.path.JsonPathException;
import de.soderer.json.path.JsonPathFilterElement;
import de.soderer.json.path.JsonPathFilterElement.FilterOperator;
import de.soderer.json.path.JsonPathPropertyElement;
import de.soderer.json.path.JsonPathRoot;
import de.soderer.json.path.JsonPathWildcardElement;

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
	 * this node to be a root node. Wildcards and filters are not supported, see
	 * {@link #getDataListByJsonPath(JsonPath)}.
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
			} else {
				throw new JsonPathException("Unexpected JsonPathElement", jsonPath);
			}
		}
		return nextDataObject;
	}

	/**
	 * Like {@link #getDataByJsonPath(JsonPath)}, but also supports paths containing a
	 * {@link JsonPathWildcardElement} ({@code *} / {@code [*]}) or a {@link JsonPathFilterElement}
	 * ({@code [?(@.property==value)]}), either of which can turn a single candidate into several,
	 * so this returns a list instead of a single node.
	 *
	 * <p>
	 * A path with neither a wildcard nor a filter behaves exactly like
	 * {@link #getDataByJsonPath(JsonPath)}, except that the single match is wrapped in a
	 * one-element list instead of being returned directly. The same missing-property/missing-index
	 * errors are thrown in that case.
	 * </p>
	 *
	 * <p>
	 * Once a wildcard or filter has been evaluated ("fanned out"), every following
	 * property/array/wildcard step is applied leniently: a candidate that does not have the
	 * required property/index is silently dropped instead of aborting the whole query, since
	 * "give me X.field for every X that has one" is the expected behaviour once several
	 * candidates are in play.
	 * </p>
	 *
	 * <p>
	 * A filter behaves slightly differently depending on this fanned-out state:
	 * </p>
	 * <ul>
	 * <li>Before the first fan-out, {@code container[?(...)]} descends into the container's
	 * children and keeps the matching ones (matching plain JSONPath's combined
	 * "select children, then filter" syntax, e.g. {@code $.items[?(@.price<10)]}).</li>
	 * <li>Once already fanned out, a filter is tested directly against each already-selected
	 * candidate instead of descending further (e.g. {@code $.*[?(@.version=='1.0')]} tests every
	 * value produced by {@code *}).</li>
	 * </ul>
	 *
	 * @param jsonPath
	 *            path to evaluate, may contain wildcards and filters
	 * @return all matching nodes, empty if nothing matches after a wildcard or filter
	 * @throws JsonPathException
	 *             if a required property/index is missing before the first wildcard or filter,
	 *             the path does not start at a root node as required, or a wildcard/filter is
	 *             applied to a simple value
	 */
	public List<JsonNode> getDataListByJsonPath(final JsonPath jsonPath) throws JsonPathException {
		List<JsonNode> currentNodes = new ArrayList<>();
		currentNodes.add(this);
		boolean lenient = false;

		for (final JsonPathElement pathPart : jsonPath.getPathParts()) {
			final List<JsonNode> nextNodes = new ArrayList<>();

			for (final JsonNode currentNode : currentNodes) {
				if (pathPart instanceof JsonPathRoot) {
					if (currentNode != null && currentNode.isRootNode()) {
						nextNodes.add(currentNode);
					} else if (!lenient) {
						throw new JsonPathException("JsonNode is not a root", jsonPath);
					}
				} else if (pathPart instanceof JsonPathWildcardElement) {
					if (currentNode instanceof JsonObject) {
						nextNodes.addAll(((JsonObject) currentNode).values());
					} else if (currentNode instanceof JsonArray) {
						nextNodes.addAll(((JsonArray) currentNode).items());
					} else if (!lenient) {
						throw new JsonPathException("JsonNode does not support a wildcard at this position", jsonPath);
					}
				} else if (pathPart instanceof JsonPathFilterElement) {
					final JsonPathFilterElement filterElement = (JsonPathFilterElement) pathPart;
					if (lenient) {
						// Already fanned out by an earlier wildcard/filter: this filter applies
						// directly to each already-selected candidate (e.g. "$.*[?(@.x==y)]" tests
						// every value produced by the "*"), it does not descend a level further.
						if (matchesFilter(currentNode, filterElement)) {
							nextNodes.add(currentNode);
						}
					} else {
						// Not yet fanned out: currentNode is a single container reached via a
						// property/array step (or the root itself), and the filter selects its
						// matching children - same as plain JSONPath's "array[?(...)]" syntax,
						// which combines "descend into every item" and "keep only matches" in one step.
						final Collection<JsonNode> candidates;
						if (currentNode instanceof JsonObject) {
							candidates = ((JsonObject) currentNode).values();
						} else if (currentNode instanceof JsonArray) {
							candidates = ((JsonArray) currentNode).items();
						} else {
							throw new JsonPathException("JsonNode does not support a filter at this position", jsonPath);
						}
						for (final JsonNode candidate : candidates) {
							if (matchesFilter(candidate, filterElement)) {
								nextNodes.add(candidate);
							}
						}
					}
				} else if (pathPart instanceof JsonPathArrayElement) {
					final int lookingForIndex = ((JsonPathArrayElement) pathPart).getIndex();
					if (currentNode instanceof JsonArray && ((JsonArray) currentNode).size() > lookingForIndex) {
						nextNodes.add(((JsonArray) currentNode).get(lookingForIndex));
					} else if (!lenient) {
						throw new JsonPathException("JsonNode does not include path", jsonPath);
					}
				} else if (pathPart instanceof JsonPathPropertyElement) {
					final String lookingForPropertyKey = ((JsonPathPropertyElement) pathPart).getPropertyKey();
					if (currentNode instanceof JsonObject && ((JsonObject) currentNode).containsKey(lookingForPropertyKey)) {
						nextNodes.add(((JsonObject) currentNode).get(lookingForPropertyKey));
					} else if (!lenient) {
						throw new JsonPathException("JsonNode does not include path", jsonPath);
					}
				} else {
					throw new JsonPathException("Unexpected JsonPathElement", jsonPath);
				}
			}

			if (pathPart instanceof JsonPathWildcardElement || pathPart instanceof JsonPathFilterElement) {
				lenient = true;
			}

			currentNodes = nextNodes;
		}

		return currentNodes;
	}

	/**
	 * A candidate matches a filter if it is a {@link JsonObject}, has the filter's property,
	 * and that property's simple value compares as specified against the filter's literal.
	 * Anything else (not an object, or missing the property) simply does not match. This is
	 * a filter, not an error condition.
	 *
	 * @param candidate
	 *            node to test
	 * @param filterElement
	 *            filter with property name, operator and literal value
	 * @return true if the candidate matches the filter
	 */
	private static boolean matchesFilter(final JsonNode candidate, final JsonPathFilterElement filterElement) {
		if (!(candidate instanceof JsonObject)) {
			return false;
		}
		final JsonObject candidateObject = (JsonObject) candidate;
		if (!candidateObject.containsKey(filterElement.getPropertyName())) {
			return false;
		}
		final Object actualValue = candidateObject.getSimpleValue(filterElement.getPropertyName());
		return compareFilterValues(actualValue, filterElement.getOperator(), filterElement.getLiteralValue());
	}

	/**
	 * Compares a property value with a filter literal. Numbers are compared numerically, other
	 * values only for (in)equality.
	 *
	 * @param actualValue
	 *            the property value
	 * @param operator
	 *            the comparison operator
	 * @param expectedValue
	 *            the filter literal
	 * @return true, if the comparison is fulfilled
	 */
	private static boolean compareFilterValues(final Object actualValue, final FilterOperator operator, final Object expectedValue) {
		switch (operator) {
			case EQUALS:
				return valuesEqual(actualValue, expectedValue);
			case NOT_EQUALS:
				return !valuesEqual(actualValue, expectedValue);
			case LESS_THAN:
			case LESS_EQUALS:
			case GREATER_THAN:
			case GREATER_EQUALS:
				if (!(actualValue instanceof Number) || !(expectedValue instanceof Number)) {
					return false;
				}
				final double actualNumber = ((Number) actualValue).doubleValue();
				final double expectedNumber = ((Number) expectedValue).doubleValue();
				switch (operator) {
					case LESS_THAN:
						return actualNumber < expectedNumber;
					case LESS_EQUALS:
						return actualNumber <= expectedNumber;
					case GREATER_THAN:
						return actualNumber > expectedNumber;
					case GREATER_EQUALS:
						return actualNumber >= expectedNumber;
					case EQUALS:
					case NOT_EQUALS:
					default:
						return false;
				}
			default:
				return false;
		}
	}

	/**
	 * Checks two simple values for equality. Numbers are compared numerically.
	 *
	 * @param actualValue
	 *            the first value
	 * @param expectedValue
	 *            the second value
	 * @return true, if the values are equal
	 */
	private static boolean valuesEqual(final Object actualValue, final Object expectedValue) {
		if (actualValue == null || expectedValue == null) {
			return actualValue == expectedValue;
		} else if (actualValue instanceof Number && expectedValue instanceof Number) {
			return ((Number) actualValue).doubleValue() == ((Number) expectedValue).doubleValue();
		} else {
			return actualValue.equals(expectedValue);
		}
	}
}
