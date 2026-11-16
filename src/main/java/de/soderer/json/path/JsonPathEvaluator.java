package de.soderer.json.path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.path.JsonPathUnionElement.IndexSelector;
import de.soderer.json.path.JsonPathUnionElement.NameSelector;
import de.soderer.json.path.JsonPathUnionElement.Selector;
import de.soderer.json.path.JsonPathUnionElement.SliceSelector;

/**
 * Evaluates JSON paths on JSON data, see {@link de.soderer.json.JsonNode#getDataListByJsonPath}.
 */
public final class JsonPathEvaluator {
	/**
	 * Static utility class.
	 */
	private JsonPathEvaluator() {
	}

	/**
	 * Evaluates a JSON path, starting at the given node.
	 * <p>
	 * Before the first element which can select several nodes (wildcard, filter, recursive
	 * descent, slice or union), a missing property or index is an error. After it, every element is
	 * applied leniently: a candidate without the required property or index is dropped.
	 * </p>
	 *
	 * @param startNode
	 *            node to start at; for paths starting with "$" it must be a root node, and it is
	 *            the node referenced by "$" within filters
	 * @param jsonPath
	 *            the path to evaluate
	 * @return all matching nodes, in document order of the selections
	 * @throws JsonPathException
	 *             if a required property/index is missing before the first element selecting
	 *             several nodes, the path does not start at a root node as required, or a
	 *             wildcard/filter is applied to a simple value at that position
	 */
	public static List<JsonNode> evaluate(final JsonNode startNode, final JsonPath jsonPath) throws JsonPathException {
		return evaluate(new ArrayList<>(jsonPath.getPathParts()), startNode, startNode, false, jsonPath);
	}

	/**
	 * Evaluates path elements leniently (never fails), as needed for paths within filter
	 * expressions.
	 *
	 * @param elements
	 *            the path elements (a root element selects the root node)
	 * @param startNode
	 *            the node to start at
	 * @param rootNode
	 *            the root node, referenced by "$"
	 * @return all matching nodes
	 */
	static List<JsonNode> evaluateLenient(final List<JsonPathElement> elements, final JsonNode startNode, final JsonNode rootNode) {
		try {
			return evaluate(elements, startNode, rootNode, true, null);
		} catch (@SuppressWarnings("unused") final JsonPathException e) {
			// Not possible in lenient mode
			return Collections.emptyList();
		}
	}

	private static List<JsonNode> evaluate(final List<JsonPathElement> elements, final JsonNode startNode, final JsonNode rootNode, final boolean lenientFromStart, final JsonPath jsonPathForErrors) throws JsonPathException {
		List<JsonNode> currentNodes = new ArrayList<>();
		if (startNode != null) {
			currentNodes.add(startNode);
		}
		boolean lenient = lenientFromStart;

		for (int elementIndex = 0; elementIndex < elements.size(); elementIndex++) {
			final JsonPathElement pathPart = elements.get(elementIndex);
			final List<JsonNode> nextNodes = new ArrayList<>();

			if (pathPart instanceof JsonPathRecursiveDescentElement) {
				// ".." applies the following element to every node and all its descendants
				final JsonPathElement followingPart = elementIndex + 1 < elements.size() ? elements.get(elementIndex + 1) : null;
				for (final JsonNode currentNode : currentNodes) {
					for (final JsonNode descendant : getDescendantsOrSelf(currentNode)) {
						if (followingPart == null) {
							nextNodes.add(descendant);
						} else {
							applyChildSelection(followingPart, descendant, rootNode, nextNodes);
						}
					}
				}
				elementIndex++;
				lenient = true;
				currentNodes = nextNodes;
				continue;
			}

			for (final JsonNode currentNode : currentNodes) {
				if (pathPart instanceof JsonPathRoot) {
					if (lenientFromStart) {
						// Within a filter "$" is the root of the evaluated data
						nextNodes.add(rootNode);
					} else if (currentNode != null && currentNode.isRootNode()) {
						nextNodes.add(currentNode);
					} else if (!lenient) {
						throw new JsonPathException("JsonNode is not a root", jsonPathForErrors);
					}
				} else if (pathPart instanceof JsonPathPropertyElement) {
					final String lookingForPropertyKey = ((JsonPathPropertyElement) pathPart).getPropertyKey();
					if (currentNode instanceof JsonObject && ((JsonObject) currentNode).containsKey(lookingForPropertyKey)) {
						nextNodes.add(((JsonObject) currentNode).get(lookingForPropertyKey));
					} else if (!lenient) {
						throw new JsonPathException("JsonNode does not include path", jsonPathForErrors);
					}
				} else if (pathPart instanceof JsonPathArrayElement) {
					final int lookingForIndex = ((JsonPathArrayElement) pathPart).getIndex();
					if (currentNode instanceof JsonArray && lookingForIndex >= 0 && ((JsonArray) currentNode).size() > lookingForIndex) {
						nextNodes.add(((JsonArray) currentNode).get(lookingForIndex));
					} else if (!lenient) {
						throw new JsonPathException("JsonNode does not include path", jsonPathForErrors);
					}
				} else if (pathPart instanceof JsonPathWildcardElement) {
					if (currentNode instanceof JsonObject || currentNode instanceof JsonArray) {
						nextNodes.addAll(getChildren(currentNode));
					} else if (!lenient) {
						throw new JsonPathException("JsonNode does not support a wildcard at this position", jsonPathForErrors);
					}
				} else if (pathPart instanceof JsonPathFilterElement) {
					final JsonPathFilterElement filterElement = (JsonPathFilterElement) pathPart;
					final Collection<JsonNode> candidates;
					if (!lenient) {
						// Not yet fanned out: the filter selects the matching children of the single
						// container reached so far (like "array[?(...)]" in plain JSONPath)
						if (currentNode instanceof JsonObject || currentNode instanceof JsonArray) {
							candidates = getChildren(currentNode);
						} else {
							throw new JsonPathException("JsonNode does not support a filter at this position", jsonPathForErrors);
						}
					} else if (currentNode instanceof JsonArray) {
						// Already fanned out: an array is replaced by its items, e.g. "$..book[?(@.price<10)]"
						candidates = ((JsonArray) currentNode).items();
					} else {
						// Already fanned out: other candidates are tested themselves, e.g.
						// "$.*[?(@.version=='1.0')]" tests every value produced by the "*"
						candidates = Collections.singletonList(currentNode);
					}
					for (final JsonNode candidate : candidates) {
						if (filterElement.matches(candidate, rootNode)) {
							nextNodes.add(candidate);
						}
					}
				} else if (pathPart instanceof JsonPathUnionElement) {
					final JsonPathUnionElement unionElement = (JsonPathUnionElement) pathPart;
					final int sizeBefore = nextNodes.size();
					applyUnion(unionElement, currentNode, nextNodes);
					if (!lenient && unionElement.isSingular() && nextNodes.size() == sizeBefore) {
						throw new JsonPathException("JsonNode does not include path", jsonPathForErrors);
					}
				} else {
					throw new JsonPathException("Unexpected JsonPathElement", jsonPathForErrors);
				}
			}

			if (pathPart instanceof JsonPathWildcardElement || pathPart instanceof JsonPathFilterElement
					|| (pathPart instanceof JsonPathUnionElement && !((JsonPathUnionElement) pathPart).isSingular())) {
				lenient = true;
			}

			currentNodes = nextNodes;
		}

		return currentNodes;
	}

	/**
	 * Applies a path element as child selection of RFC 9535 (as needed after a recursive descent):
	 * a filter selects the matching children of an object or array.
	 */
	private static void applyChildSelection(final JsonPathElement pathPart, final JsonNode node, final JsonNode rootNode, final List<JsonNode> results) {
		if (pathPart instanceof JsonPathPropertyElement) {
			final String propertyKey = ((JsonPathPropertyElement) pathPart).getPropertyKey();
			if (node instanceof JsonObject && ((JsonObject) node).containsKey(propertyKey)) {
				results.add(((JsonObject) node).get(propertyKey));
			}
		} else if (pathPart instanceof JsonPathArrayElement) {
			final int index = ((JsonPathArrayElement) pathPart).getIndex();
			if (node instanceof JsonArray && index >= 0 && index < ((JsonArray) node).size()) {
				results.add(((JsonArray) node).get(index));
			}
		} else if (pathPart instanceof JsonPathWildcardElement) {
			results.addAll(getChildren(node));
		} else if (pathPart instanceof JsonPathFilterElement) {
			for (final JsonNode child : getChildren(node)) {
				if (((JsonPathFilterElement) pathPart).matches(child, rootNode)) {
					results.add(child);
				}
			}
		} else if (pathPart instanceof JsonPathUnionElement) {
			applyUnion((JsonPathUnionElement) pathPart, node, results);
		}
	}

	/**
	 * Applies all selectors of a union to a node.
	 */
	private static void applyUnion(final JsonPathUnionElement unionElement, final JsonNode node, final List<JsonNode> results) {
		for (final Selector selector : unionElement.getSelectors()) {
			if (selector instanceof NameSelector) {
				final String propertyKey = ((NameSelector) selector).propertyKey();
				if (node instanceof JsonObject && ((JsonObject) node).containsKey(propertyKey)) {
					results.add(((JsonObject) node).get(propertyKey));
				}
			} else if (node instanceof JsonArray) {
				final JsonArray array = (JsonArray) node;
				if (selector instanceof IndexSelector) {
					final int index = ((IndexSelector) selector).index();
					final int normalizedIndex = index >= 0 ? index : array.size() + index;
					if (normalizedIndex >= 0 && normalizedIndex < array.size()) {
						results.add(array.get(normalizedIndex));
					}
				} else if (selector instanceof SliceSelector) {
					for (final int index : ((SliceSelector) selector).getIndexes(array.size())) {
						results.add(array.get(index));
					}
				}
			}
		}
	}

	/**
	 * Returns the property values of an object or the items of an array.
	 *
	 * @param node
	 *            the node
	 * @return the children, empty for simple values
	 */
	private static Collection<JsonNode> getChildren(final JsonNode node) {
		if (node instanceof JsonObject) {
			return ((JsonObject) node).values();
		} else if (node instanceof JsonArray) {
			return ((JsonArray) node).items();
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Returns a node and all its descendants in document order (the node first, then depth first).
	 *
	 * @param node
	 *            the node
	 * @return the node and all its descendants
	 */
	private static List<JsonNode> getDescendantsOrSelf(final JsonNode node) {
		final List<JsonNode> result = new ArrayList<>();
		collectDescendantsOrSelf(node, result);
		return result;
	}

	private static void collectDescendantsOrSelf(final JsonNode node, final List<JsonNode> result) {
		if (node == null) {
			return;
		}
		result.add(node);
		for (final JsonNode child : getChildren(node)) {
			collectDescendantsOrSelf(child, result);
		}
	}
}
