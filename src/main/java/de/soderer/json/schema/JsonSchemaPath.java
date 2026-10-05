package de.soderer.json.schema;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

import de.soderer.json.utilities.BasicReader;
import de.soderer.json.utilities.Utilities;

/**
 * Path to a position within a JSON schema, used for references ("$ref") and in error messages.
 * <p>
 * A path consists of a root ("$"), an internal reference ("#/definitions/x") or an external
 * reference ("otherSchema.json#/definitions/x"), followed by property keys and array indexes. It
 * can be formatted in dot notation ({@link #getDotFormattedPath()}), bracket notation
 * ({@link #getBracketFormattedPath()}) or reference notation ({@link #getReferenceFormattedPath()}).
 * The add methods modify this path and return it for chaining, so copy a shared path with
 * {@link #JsonSchemaPath(JsonSchemaPath)} before extending it.
 * </p>
 */
public class JsonSchemaPath {
	/** The path elements, the root or reference element first. */
	private Stack<JsonSchemaPathElement> jsonSchemaPathElements = new Stack<>();

	/**
	 * Creates a new path pointing to the root of the JSON schema ("$").
	 */
	public JsonSchemaPath() {
		jsonSchemaPathElements.push(new JsonSchemaPathRoot("$"));
	}

	/**
	 * Creates a new path by parsing a path string. Allowed syntax:
	 *
	 * <pre>
	 * dot-notation:
	 * 	$.store.customer[5].item[2]
	 *
	 * bracket-notation:
	 * 	$['store']['customer'][5]['item'][2]
	 *
	 * schema-reference-notation:
	 * 	#/store/customer/item
	 *
	 * external schema-reference-notation:
	 * 	otherSchema.json#/store/customer/item
	 * </pre>
	 *
	 * Path parts may contain JSON pointer escapes ("~1" for "/", "~0" for "~").
	 *
	 * @param jsonSchemaPathString
	 *            the path string, an empty string means the root
	 * @throws JsonSchemaDefinitionError
	 *             if the path string is invalid
	 */
	public JsonSchemaPath(final String jsonSchemaPathString) throws JsonSchemaDefinitionError {
		try (JsonSchemaPathReader jsonSchemaPathReader = new JsonSchemaPathReader(jsonSchemaPathString)) {
			jsonSchemaPathElements = jsonSchemaPathReader.getReadJsonSchemaPathElements();
		} catch (final Exception e) {
			throw new JsonSchemaDefinitionError(e.getMessage(), null, e);
		}
	}

	/**
	 * Creates a copy of a path. Extending the copy does not change the original.
	 *
	 * @param jsonSchemaPath
	 *            the path to copy
	 */
	public JsonSchemaPath(final JsonSchemaPath jsonSchemaPath) {
		jsonSchemaPathElements = new Stack<>();
		jsonSchemaPathElements.addAll(jsonSchemaPath.getPathParts());
	}

	/**
	 * Returns this path in dot notation, e.g. "$.properties.address.items[2]". Dots within property
	 * keys are escaped by a backslash.
	 *
	 * @return the formatted path
	 */
	public String getDotFormattedPath() {
		final StringBuilder returnValue = new StringBuilder();
		for (final JsonSchemaPathElement jsonSchemaPathElement : jsonSchemaPathElements) {
			if (jsonSchemaPathElement instanceof JsonSchemaPathRoot) {
				returnValue.append(jsonSchemaPathElement.toString());
			} else if (jsonSchemaPathElement instanceof JsonSchemaPathPropertyElement) {
				returnValue.append(".").append(jsonSchemaPathElement.toString().replace(".", "\\."));
			} else if (jsonSchemaPathElement instanceof JsonSchemaPathArrayElement) {
				returnValue.append("[").append(jsonSchemaPathElement).append("]");
			} else if (jsonSchemaPathElement instanceof JsonSchemaPathExternalReference) {
				if (returnValue.length() > 0) {
					returnValue.append(".");
				}
				returnValue.append(jsonSchemaPathElement.toString());
			} else if (jsonSchemaPathElement instanceof JsonSchemaPathInternalReference) {
				if (returnValue.length() > 0) {
					returnValue.append(".");
				}
				returnValue.append(jsonSchemaPathElement.toString().replace(".", "\\."));
			}
		}
		return returnValue.toString();
	}

	/**
	 * Returns this path in bracket notation, e.g. "$['properties']['address']['items'][2]". Single
	 * quotes within property keys are escaped by a backslash.
	 *
	 * @return the formatted path
	 */
	public String getBracketFormattedPath() {
		final StringBuilder returnValue = new StringBuilder();
		for (final JsonSchemaPathElement jsonSchemaPathElement : jsonSchemaPathElements) {
			if (jsonSchemaPathElement instanceof JsonSchemaPathRoot) {
				returnValue.append(jsonSchemaPathElement.toString());
			} else if (jsonSchemaPathElement instanceof JsonSchemaPathPropertyElement) {
				returnValue.append("['").append(jsonSchemaPathElement.toString().replace("'", "\\'")).append("']");
			} else if (jsonSchemaPathElement instanceof JsonSchemaPathArrayElement) {
				returnValue.append("[").append(jsonSchemaPathElement).append("]");
			} else if (jsonSchemaPathElement instanceof JsonSchemaPathExternalReference) {
				if (returnValue.length() > 0) {
					returnValue.append(".");
				}
				returnValue.append(jsonSchemaPathElement.toString().replace("'", "\\'"));
			} else if (jsonSchemaPathElement instanceof JsonSchemaPathInternalReference) {
				if (returnValue.length() > 0) {
					returnValue.append(".");
				}
				returnValue.append(jsonSchemaPathElement.toString().replace(".", "\\.").replace("'", "\\'"));
			}
		}
		return returnValue.toString();
	}

	/**
	 * Returns this path in reference notation, e.g. "$/properties/address/items[2]". Slashes within
	 * property keys are escaped by a backslash.
	 *
	 * @return the formatted path
	 */
	public String getReferenceFormattedPath() {
		final StringBuilder returnValue = new StringBuilder();
		for (final JsonSchemaPathElement jsonSchemaPathElement : jsonSchemaPathElements) {
			if (jsonSchemaPathElement instanceof JsonSchemaPathRoot) {
				returnValue.append(jsonSchemaPathElement.toString());
			} else if (jsonSchemaPathElement instanceof JsonSchemaPathPropertyElement) {
				returnValue.append("/").append(jsonSchemaPathElement.toString().replace("/", "\\/"));
			} else if (jsonSchemaPathElement instanceof JsonSchemaPathArrayElement) {
				returnValue.append("[").append(jsonSchemaPathElement).append("]");
			} else if (jsonSchemaPathElement instanceof JsonSchemaPathExternalReference) {
				if (returnValue.length() > 0) {
					returnValue.append(".");
				}
				returnValue.append(jsonSchemaPathElement.toString().replace("/", "\\/"));
			} else if (jsonSchemaPathElement instanceof JsonSchemaPathInternalReference) {
				if (returnValue.length() > 0) {
					returnValue.append(".");
				}
				returnValue.append(jsonSchemaPathElement.toString().replace(".", "\\.").replace("/", "\\/"));
			}
		}
		return returnValue.toString();
	}

	/**
	 * Appends a path element to this path.
	 *
	 * @param jsonSchemaPathElement
	 *            the element to append, must not be null
	 * @return this path for chaining
	 * @throws IllegalArgumentException
	 *             if the element is null
	 */
	public JsonSchemaPath add(final JsonSchemaPathElement jsonSchemaPathElement) {
		if (jsonSchemaPathElement == null) {
			throw new IllegalArgumentException("Invalid null value for JsonSchemaPathElement");
		} else {
			jsonSchemaPathElements.push(jsonSchemaPathElement);
			return this;
		}
	}

	/**
	 * Appends an array index to this path.
	 *
	 * @param arrayIndex
	 *            the array index, 0 based
	 * @return this path for chaining
	 */
	public JsonSchemaPath addArrayIndex(final int arrayIndex) {
		jsonSchemaPathElements.push(new JsonSchemaPathArrayElement(arrayIndex));
		return this;
	}

	/**
	 * Appends a property key to this path.
	 *
	 * @param propertyKey
	 *            the property key
	 * @return this path for chaining
	 */
	public JsonSchemaPath addPropertyKey(final String propertyKey) {
		jsonSchemaPathElements.push(new JsonSchemaPathPropertyElement(propertyKey));
		return this;
	}

	/**
	 * Parser for path strings.
	 */
	private static class JsonSchemaPathReader extends BasicReader {
		/** The path elements read. */
		private final Stack<JsonSchemaPathElement> readJsonSchemaPathElements;

		/**
		 * Parses a path string.
		 *
		 * @param jsonSchemaPathString
		 *            the path string
		 * @throws Exception
		 *             if the path string is invalid
		 */
		public JsonSchemaPathReader(final String jsonSchemaPathString) throws Exception {
			super(new ByteArrayInputStream(jsonSchemaPathString.getBytes(StandardCharsets.UTF_8)));

			readJsonSchemaPathElements = new Stack<>();

			Character nextChar = readNextNonWhitespace();
			if (nextChar == null) {
				// Empty json path
				readJsonSchemaPathElements.push(new JsonSchemaPathRoot("$"));
				return;
			} else if (nextChar == '$') {
				// Root element
				readJsonSchemaPathElements.push(new JsonSchemaPathRoot(nextChar.toString()));
				nextChar = readNextNonWhitespace();
			} else if (nextChar == '#') {
				final String schemaReference = readUpToNext(true, '\\');
				readJsonSchemaPathElements.push(new JsonSchemaPathInternalReference(schemaReference));
				return;
			}

			while (nextChar != null) {
				String nextJsonSchemaPathPart;
				switch (nextChar) {
					case '.':
						nextJsonSchemaPathPart = readUpToNext(false, '\\', '.', '[');
						readJsonSchemaPathElements.push(parseJsonSchemaPathElement(nextJsonSchemaPathPart.substring(1).trim()));
						break;
					case '/':
						nextJsonSchemaPathPart = readUpToNext(false, '\\', '/', '[');
						readJsonSchemaPathElements.push(parseJsonSchemaPathElement(nextJsonSchemaPathPart.substring(1).trim()));
						break;
					case '[':
						nextJsonSchemaPathPart = readUpToNext(true, '\\', ']');
						if (nextJsonSchemaPathPart.startsWith("'") && nextJsonSchemaPathPart.endsWith("'")) {
							readJsonSchemaPathElements.push(parseJsonSchemaPathElement(nextJsonSchemaPathPart.substring(1, nextJsonSchemaPathPart.length() - 1)));
						} else {
							readJsonSchemaPathElements.push(parseJsonSchemaPathElement(nextJsonSchemaPathPart));
						}
						break;
					default:
						if (readJsonSchemaPathElements.size() > 0) {
							throw new JsonSchemaDefinitionError("Invalid Json schema reference: " + jsonSchemaPathString, null);
						} else {
							final String schemaLocation = readUpToNext(true, '\\', '#');
							final String schemaReference = readUpToNext(true, '\\');
							readJsonSchemaPathElements.push(new JsonSchemaPathExternalReference(schemaLocation, schemaReference));
							return;
						}
				}

				nextChar = readNextNonWhitespace();
			}
		}


		/**
		 * Returns the path elements read.
		 *
		 * @return the path elements, the root or reference element first
		 */
		public Stack<JsonSchemaPathElement> getReadJsonSchemaPathElements() {
			return readJsonSchemaPathElements;
		}
	}

	/**
	 * Parses a single path part, resolving JSON pointer escapes ("~1" for "/", "~0" for "~") and an
	 * URL encoded percent sign ("%25").
	 *
	 * @param value
	 *            the path part, e.g. "name", "['name']" or "[2]"
	 * @return the path element
	 */
	private static JsonSchemaPathElement parseJsonSchemaPathElement(final String value) {
		// RFC 6901: "~1" must be resolved before "~0", otherwise "~01" would wrongly become "/"
		final String valueRaw = value.replace("~1", "/").replace("~0", "~").replace("%25", "%");
		if (valueRaw.startsWith("['") && valueRaw.endsWith("']")) {
			return new JsonSchemaPathPropertyElement(valueRaw.substring(2, valueRaw.length() - 2));
		} else if (valueRaw.startsWith("[") && valueRaw.endsWith("]")) {
			return new JsonSchemaPathArrayElement(Integer.parseInt(valueRaw.substring(1, valueRaw.length() - 1)));
		} else if (valueRaw.startsWith(".")) {
			return new JsonSchemaPathPropertyElement(valueRaw.substring(1));
		} else {
			return new JsonSchemaPathPropertyElement(valueRaw);
		}
	}

	/**
	 * Returns the elements of this path. The returned stack is the internal one, so changes affect
	 * this path.
	 *
	 * @return the path elements, the root or reference element first
	 */
	public Stack<JsonSchemaPathElement> getPathParts() {
		return jsonSchemaPathElements;
	}

	@Override
	public String toString() {
		return getDotFormattedPath();
	}

	/**
	 * Two paths are equal, if their dot formatted representations are equal.
	 */
	@Override
	public boolean equals(final Object otherObject) {
		if (otherObject == null) {
			return false;
		} else if (!(otherObject instanceof JsonSchemaPath)) {
			return false;
		} else {
			return getDotFormattedPath().equals(((JsonSchemaPath) otherObject).getDotFormattedPath());
		}
	}

	@Override
	public int hashCode() {
		return getDotFormattedPath().hashCode();
	}

	/**
	 * Checks whether this path points to the root of a JSON schema, i.e. "$", an internal reference
	 * "#" or an external reference without reference part.
	 *
	 * @return true, if this path points to a schema root
	 */
	public boolean isRoot() {
		if (jsonSchemaPathElements != null && jsonSchemaPathElements.size() == 1) {
			if (jsonSchemaPathElements.get(0) instanceof JsonSchemaPathExternalReference) {
				final JsonSchemaPathExternalReference jsonSchemaPathExternalReference = (JsonSchemaPathExternalReference) jsonSchemaPathElements.get(0);
				return Utilities.isBlank(jsonSchemaPathExternalReference.getReferenceString());
			} else {
				return "$".equals(jsonSchemaPathElements.get(0).toString()) || jsonSchemaPathElements.get(0).toString().endsWith("#");
			}
		} else {
			return false;
		}
	}
}
