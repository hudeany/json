package de.soderer.json.patch;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueNull;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueString;

/**
 * JSON Patch as defined by RFC 6902: a JSON array of operations ("add", "remove", "replace",
 * "move", "copy", "test") that change a JSON document. The locations are JSON Pointers (RFC 6901),
 * like "/customer/addresses/0/street".
 * <p>
 * {@link #apply(JsonNode, JsonArray)} applies a patch atomically: the given document is not
 * changed, the result is a changed copy, and if any operation fails no result is returned at all.
 * {@link #create(JsonNode, JsonNode)} creates a patch that changes one document into another.
 * </p>
 * <p>
 * Example: <code>[{"op": "replace", "path": "/name", "value": "new"}, {"op": "remove", "path": "/tags/0"}]</code>
 * </p>
 */
public final class JsonPatch {
	/**
	 * Utility class.
	 */
	private JsonPatch() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Applies a JSON Patch to a JSON document.
	 *
	 * @param document
	 *            the document to patch, which is not changed
	 * @param patch
	 *            the patch, an array of operation objects
	 * @return the patched copy of the document
	 * @throws JsonPatchException
	 *             if the patch is malformed or an operation cannot be applied; the document is not
	 *             changed then
	 */
	public static JsonNode apply(final JsonNode document, final JsonArray patch) throws JsonPatchException {
		if (patch == null) {
			throw new JsonPatchException("Patch must not be null", -1);
		}
		JsonNode result = deepCopy(document);
		for (int operationIndex = 0; operationIndex < patch.size(); operationIndex++) {
			final JsonNode operationNode = patch.get(operationIndex);
			if (!(operationNode instanceof final JsonObject operation)) {
				throw new JsonPatchException("Operation is not an object", operationIndex);
			}
			result = applyOperation(result, operation, operationIndex);
		}
		return result;
	}

	/**
	 * Applies a single operation.
	 *
	 * @param document
	 *            the document, changed in place where possible
	 * @param operation
	 *            the operation object
	 * @param operationIndex
	 *            the index of the operation for error messages
	 * @return the document, which is a different node if the whole document was replaced
	 * @throws JsonPatchException
	 *             if the operation is malformed or cannot be applied
	 */
	private static JsonNode applyOperation(final JsonNode document, final JsonObject operation, final int operationIndex) throws JsonPatchException {
		final String op = getStringMember(operation, "op", operationIndex);
		final List<String> path = parsePointer(getStringMember(operation, "path", operationIndex), operationIndex);
		switch (op) {
			case "add":
				return add(document, path, deepCopy(getValueMember(operation, operationIndex)), operationIndex);
			case "remove":
				remove(document, path, operationIndex);
				return document;
			case "replace": {
				get(document, path, operationIndex);
				if (path.isEmpty()) {
					return deepCopy(getValueMember(operation, operationIndex));
				}
				final JsonNode parent = get(document, path.subList(0, path.size() - 1), operationIndex);
				if (parent instanceof final JsonObject parentObject) {
					// Keeps the position of the property within the object
					parentObject.put(path.get(path.size() - 1), deepCopy(getValueMember(operation, operationIndex)));
					return document;
				}
				remove(document, path, operationIndex);
				return add(document, path, deepCopy(getValueMember(operation, operationIndex)), operationIndex);
			}
			case "move": {
				final List<String> from = parsePointer(getStringMember(operation, "from", operationIndex), operationIndex);
				if (path.size() > from.size() && path.subList(0, from.size()).equals(from)) {
					throw new JsonPatchException("Cannot move a value into one of its own children", operationIndex);
				} else if (path.equals(from)) {
					get(document, from, operationIndex);
					return document;
				}
				final JsonNode value = get(document, from, operationIndex);
				remove(document, from, operationIndex);
				return add(document, path, value, operationIndex);
			}
			case "copy": {
				final List<String> from = parsePointer(getStringMember(operation, "from", operationIndex), operationIndex);
				return add(document, path, deepCopy(get(document, from, operationIndex)), operationIndex);
			}
			case "test":
				if (!jsonEquals(get(document, path, operationIndex), getValueMember(operation, operationIndex))) {
					throw new JsonPatchException("Test failed for path '" + toPointer(path) + "'", operationIndex);
				}
				return document;
			default:
				throw new JsonPatchException("Unknown operation '" + op + "'", operationIndex);
		}
	}

	/**
	 * Adds a value: sets an object property or inserts an array item.
	 *
	 * @param document
	 *            the document, changed in place
	 * @param path
	 *            the reference tokens of the target location
	 * @param operationIndex
	 *            the index of the operation for error messages
	 * @param value
	 *            the value to add
	 * @return the document, or the value if the whole document is replaced
	 * @throws JsonPatchException
	 *             if the parent of the location does not exist or the array index is invalid
	 */
	private static JsonNode add(final JsonNode document, final List<String> path, final JsonNode value, final int operationIndex) throws JsonPatchException {
		if (path.isEmpty()) {
			return value;
		}
		final JsonNode parent = get(document, path.subList(0, path.size() - 1), operationIndex);
		final String key = path.get(path.size() - 1);
		if (parent instanceof final JsonObject parentObject) {
			parentObject.put(key, value);
		} else if (parent instanceof final JsonArray parentArray) {
			if ("-".equals(key)) {
				parentArray.add(value);
			} else {
				final int index = parseArrayIndex(key, operationIndex);
				if (index > parentArray.size()) {
					throw new JsonPatchException("Array index " + index + " is out of bounds for path '" + toPointer(path) + "'", operationIndex);
				}
				parentArray.insert(index, value);
			}
		} else {
			throw new JsonPatchException("Parent of path '" + toPointer(path) + "' is no object or array", operationIndex);
		}
		return document;
	}

	/**
	 * Removes an existing object property or array item.
	 *
	 * @param document
	 *            the document, changed in place
	 * @param path
	 *            the reference tokens of the target location
	 * @param operationIndex
	 *            the index of the operation for error messages
	 * @throws JsonPatchException
	 *             if the location does not exist or is the whole document
	 */
	private static void remove(final JsonNode document, final List<String> path, final int operationIndex) throws JsonPatchException {
		if (path.isEmpty()) {
			throw new JsonPatchException("Cannot remove the whole document", operationIndex);
		}
		final JsonNode parent = get(document, path.subList(0, path.size() - 1), operationIndex);
		final String key = path.get(path.size() - 1);
		if (parent instanceof final JsonObject parentObject) {
			if (!parentObject.containsKey(key)) {
				throw new JsonPatchException("Path '" + toPointer(path) + "' does not exist", operationIndex);
			}
			parentObject.remove(key);
		} else if (parent instanceof final JsonArray parentArray) {
			final int index = parseArrayIndex(key, operationIndex);
			if (index >= parentArray.size()) {
				throw new JsonPatchException("Path '" + toPointer(path) + "' does not exist", operationIndex);
			}
			parentArray.removeByIndex(index);
		} else {
			throw new JsonPatchException("Path '" + toPointer(path) + "' does not exist", operationIndex);
		}
	}

	/**
	 * Returns the existing value at a path.
	 *
	 * @param document
	 *            the document, not changed
	 * @param path
	 *            the reference tokens of the target location
	 * @param operationIndex
	 *            the index of the operation for error messages
	 * @return the value at the location
	 * @throws JsonPatchException
	 *             if the location does not exist
	 */
	private static JsonNode get(final JsonNode document, final List<String> path, final int operationIndex) throws JsonPatchException {
		JsonNode current = document;
		for (int i = 0; i < path.size(); i++) {
			final String key = path.get(i);
			if (current instanceof final JsonObject currentObject && currentObject.containsKey(key)) {
				current = currentObject.get(key);
			} else if (current instanceof final JsonArray currentArray && !"-".equals(key) && parseArrayIndex(key, operationIndex) < currentArray.size()) {
				current = currentArray.get(parseArrayIndex(key, operationIndex));
			} else {
				throw new JsonPatchException("Path '" + toPointer(path.subList(0, i + 1)) + "' does not exist", operationIndex);
			}
		}
		return current;
	}

	/**
	 * Parses an array index of a JSON Pointer, which has no sign and no leading zeros.
	 *
	 * @param key
	 *            the reference token
	 * @param operationIndex
	 *            the index of the operation for error messages
	 * @return the array index
	 * @throws JsonPatchException
	 *             if the token is no valid array index
	 */
	private static int parseArrayIndex(final String key, final int operationIndex) throws JsonPatchException {
		if (!key.matches("0|[1-9][0-9]*")) {
			throw new JsonPatchException("Invalid array index '" + key + "'", operationIndex);
		}
		try {
			return Integer.parseInt(key);
		} catch (@SuppressWarnings("unused") final NumberFormatException e) {
			throw new JsonPatchException("Invalid array index '" + key + "'", operationIndex);
		}
	}

	/**
	 * Returns a string member of an operation object, like "op" or "path".
	 *
	 * @param operation
	 *            the operation object
	 * @param memberName
	 *            the name of the member
	 * @param operationIndex
	 *            the index of the operation for error messages
	 * @return the value of the member
	 * @throws JsonPatchException
	 *             if the member is missing or no string
	 */
	private static String getStringMember(final JsonObject operation, final String memberName, final int operationIndex) throws JsonPatchException {
		final JsonNode member = operation.get(memberName);
		if (!(member instanceof final JsonValueString memberString)) {
			throw new JsonPatchException("Missing or invalid member '" + memberName + "'", operationIndex);
		}
		return memberString.getValue();
	}

	/**
	 * Returns the member "value" of an operation object, which may be JSON null.
	 *
	 * @param operation
	 *            the operation object
	 * @param operationIndex
	 *            the index of the operation for error messages
	 * @return the value
	 * @throws JsonPatchException
	 *             if the member is missing
	 */
	private static JsonNode getValueMember(final JsonObject operation, final int operationIndex) throws JsonPatchException {
		if (!operation.containsKey("value")) {
			throw new JsonPatchException("Missing member 'value'", operationIndex);
		}
		return operation.get("value");
	}

	/**
	 * Parses a JSON Pointer (RFC 6901) to its unescaped reference tokens.
	 *
	 * @param pointer
	 *            the pointer, "" for the whole document
	 * @param operationIndex
	 *            the index of the operation for error messages
	 * @return the reference tokens, empty for the whole document
	 * @throws JsonPatchException
	 *             if the pointer is invalid
	 */
	static List<String> parsePointer(final String pointer, final int operationIndex) throws JsonPatchException {
		final List<String> tokens = new ArrayList<>();
		if (pointer.isEmpty()) {
			return tokens;
		} else if (!pointer.startsWith("/")) {
			throw new JsonPatchException("Invalid JSON pointer '" + pointer + "', must start with '/'", operationIndex);
		}
		for (final String token : pointer.substring(1).split("/", -1)) {
			if (token.matches(".*~([^01]|$).*")) {
				throw new JsonPatchException("Invalid escape sequence in JSON pointer '" + pointer + "'", operationIndex);
			}
			// "~1" must be resolved before "~0", otherwise "~01" would wrongly become "/"
			tokens.add(token.replace("~1", "/").replace("~0", "~"));
		}
		return tokens;
	}

	/**
	 * Creates a JSON Pointer (RFC 6901) from reference tokens.
	 *
	 * @param tokens
	 *            the unescaped reference tokens
	 * @return the pointer, "" for the whole document
	 */
	static String toPointer(final List<String> tokens) {
		final StringBuilder pointer = new StringBuilder();
		for (final String token : tokens) {
			pointer.append("/").append(token.replace("~", "~0").replace("/", "~1"));
		}
		return pointer.toString();
	}

	/**
	 * Creates a JSON Patch, that changes the source document into the target document. Objects are
	 * compared property by property and arrays item by item, so the patch only contains the changed
	 * parts. The patch contains "add", "remove" and "replace" operations.
	 *
	 * @param source
	 *            the original document
	 * @param target
	 *            the changed document
	 * @return the patch, empty if both documents are equal
	 */
	public static JsonArray create(final JsonNode source, final JsonNode target) {
		final JsonArray patch = new JsonArray();
		createOperations(normalize(source), normalize(target), new ArrayList<>(), patch);
		return patch;
	}

	/**
	 * Adds the operations, that change a source node into a target node, to a patch.
	 *
	 * @param source
	 *            the source node
	 * @param target
	 *            the target node
	 * @param path
	 *            the reference tokens of both nodes
	 * @param patch
	 *            the patch to add the operations to
	 */
	private static void createOperations(final JsonNode source, final JsonNode target, final List<String> path, final JsonArray patch) {
		if (jsonEquals(source, target)) {
			return;
		} else if (source instanceof final JsonObject sourceObject && target instanceof final JsonObject targetObject) {
			for (final String key : sourceObject.keySet()) {
				if (!targetObject.containsKey(key)) {
					patch.add(createOperation("remove", appendToken(path, key), null));
				}
			}
			for (final Entry<String, JsonNode> targetEntry : targetObject.entrySet()) {
				if (sourceObject.containsKey(targetEntry.getKey())) {
					createOperations(sourceObject.get(targetEntry.getKey()), targetEntry.getValue(), appendToken(path, targetEntry.getKey()), patch);
				} else {
					patch.add(createOperation("add", appendToken(path, targetEntry.getKey()), targetEntry.getValue()));
				}
			}
		} else if (source instanceof final JsonArray sourceArray && target instanceof final JsonArray targetArray) {
			final int commonSize = Math.min(sourceArray.size(), targetArray.size());
			for (int i = 0; i < commonSize; i++) {
				createOperations(sourceArray.get(i), targetArray.get(i), appendToken(path, Integer.toString(i)), patch);
			}
			// Remove from the end, so the indexes of the remaining items stay valid
			for (int i = sourceArray.size() - 1; i >= commonSize; i--) {
				patch.add(createOperation("remove", appendToken(path, Integer.toString(i)), null));
			}
			for (int i = commonSize; i < targetArray.size(); i++) {
				patch.add(createOperation("add", appendToken(path, Integer.toString(i)), targetArray.get(i)));
			}
		} else {
			patch.add(createOperation("replace", path, target));
		}
	}

	/**
	 * Creates a new path with an additional reference token.
	 *
	 * @param path
	 *            the reference tokens, which are not changed
	 * @param token
	 *            the token to append
	 * @return the new path
	 */
	private static List<String> appendToken(final List<String> path, final String token) {
		final List<String> newPath = new ArrayList<>(path);
		newPath.add(token);
		return newPath;
	}

	/**
	 * Creates an operation object.
	 *
	 * @param op
	 *            the operation name
	 * @param path
	 *            the reference tokens of the location
	 * @param value
	 *            the value, ignored for "remove"
	 * @return the operation object
	 */
	private static JsonObject createOperation(final String op, final List<String> path, final JsonNode value) {
		final JsonObject operation = new JsonObject();
		operation.put("op", new JsonValueString(op));
		operation.put("path", new JsonValueString(toPointer(path)));
		if (!"remove".equals(op)) {
			operation.put("value", deepCopy(value));
		}
		return operation;
	}

	/**
	 * Converts Java null to a JSON null value.
	 *
	 * @param node
	 *            the node, may be null
	 * @return the node, or a JSON null value
	 */
	private static JsonNode normalize(final JsonNode node) {
		return node == null ? new JsonValueNull() : node;
	}

	/**
	 * Creates a deep copy of a JSON node, so changes of the copy do not affect the original.
	 *
	 * @param node
	 *            the node, null is copied as JSON null value
	 * @return the copy
	 */
	static JsonNode deepCopy(final JsonNode node) {
		if (node == null || node.isNull()) {
			return new JsonValueNull();
		} else if (node instanceof final JsonObject object) {
			final JsonObject copy = new JsonObject();
			for (final Entry<String, JsonNode> entry : object.entrySet()) {
				copy.put(entry.getKey(), deepCopy(entry.getValue()));
			}
			return copy;
		} else if (node instanceof final JsonArray array) {
			final JsonArray copy = new JsonArray();
			for (int i = 0; i < array.size(); i++) {
				copy.add(deepCopy(array.get(i)));
			}
			return copy;
		} else if (node instanceof final JsonValueString string) {
			return new JsonValueString(string.getValue());
		} else if (node instanceof final JsonValueBoolean bool) {
			return new JsonValueBoolean(bool.getValue());
		} else if (node instanceof final JsonValueInteger integer) {
			final Number value = integer.getValue();
			if (value instanceof final Integer intValue) {
				return new JsonValueInteger(intValue);
			} else if (value instanceof final Long longValue) {
				return new JsonValueInteger(longValue);
			} else if (value instanceof final BigInteger bigIntegerValue) {
				return new JsonValueInteger(bigIntegerValue);
			} else {
				return new JsonValueInteger(new BigDecimal(value.toString()));
			}
		} else if (node instanceof final JsonValueNumber number) {
			return new JsonValueNumber(number.getValue());
		} else {
			throw new IllegalArgumentException("Unsupported JSON node type: " + node.getClass().getSimpleName());
		}
	}

	/**
	 * Compares two JSON nodes as defined by RFC 6902 for the "test" operation: numbers are equal if
	 * they are numerically equal (1 equals 1.0), objects regardless of the order of their
	 * properties, arrays item by item in their order.
	 *
	 * @param first
	 *            the first node, null is regarded as JSON null value
	 * @param second
	 *            the second node, null is regarded as JSON null value
	 * @return true if both nodes are equal
	 */
	public static boolean jsonEquals(final JsonNode first, final JsonNode second) {
		if (first == null || first.isNull()) {
			return second == null || second.isNull();
		} else if (second == null || second.isNull()) {
			return false;
		} else if ((first.isInteger() || first.isNumber()) && (second.isInteger() || second.isNumber())) {
			return compareNumbers(getNumber(first), getNumber(second)) == 0;
		} else if (first instanceof final JsonValueBoolean firstBoolean && second instanceof final JsonValueBoolean secondBoolean) {
			return firstBoolean.getValue().equals(secondBoolean.getValue());
		} else if (first instanceof final JsonValueString firstString && second instanceof final JsonValueString secondString) {
			return firstString.getValue().equals(secondString.getValue());
		} else if (first instanceof final JsonArray firstArray && second instanceof final JsonArray secondArray) {
			if (firstArray.size() != secondArray.size()) {
				return false;
			}
			for (int i = 0; i < firstArray.size(); i++) {
				if (!jsonEquals(firstArray.get(i), secondArray.get(i))) {
					return false;
				}
			}
			return true;
		} else if (first instanceof final JsonObject firstObject && second instanceof final JsonObject secondObject) {
			if (firstObject.size() != secondObject.size()) {
				return false;
			}
			for (final Entry<String, JsonNode> entry : firstObject.entrySet()) {
				if (!secondObject.containsKey(entry.getKey()) || !jsonEquals(entry.getValue(), secondObject.get(entry.getKey()))) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns the value of a JSON integer or number node.
	 *
	 * @param node
	 *            the node
	 * @return the number
	 */
	private static Number getNumber(final JsonNode node) {
		if (node instanceof final JsonValueInteger integer) {
			return integer.getValue();
		} else {
			return ((JsonValueNumber) node).getValue();
		}
	}

	/**
	 * Compares two numbers numerically, regardless of their Java type.
	 *
	 * @param first
	 *            the first number
	 * @param second
	 *            the second number
	 * @return a negative value, zero or a positive value, if the first number is smaller, equal or
	 *         greater
	 */
	private static int compareNumbers(final Number first, final Number second) {
		if (!Double.isFinite(first.doubleValue()) && !(first instanceof BigDecimal) && !(first instanceof BigInteger)
				|| !Double.isFinite(second.doubleValue()) && !(second instanceof BigDecimal) && !(second instanceof BigInteger)) {
			return Double.compare(first.doubleValue(), second.doubleValue());
		}
		return new BigDecimal(first.toString()).compareTo(new BigDecimal(second.toString()));
	}
}
