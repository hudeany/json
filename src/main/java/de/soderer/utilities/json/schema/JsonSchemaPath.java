package de.soderer.utilities.json.schema;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

import de.soderer.utilities.json.utilities.BasicReader;
import de.soderer.utilities.json.utilities.Utilities;

public class JsonSchemaPath {
	private Stack<JsonSchemaPathElement> jsonSchemaPathElements = new Stack<>();

	public JsonSchemaPath() {
		jsonSchemaPathElements.push(new JsonSchemaPathRoot("$"));
	}

	/**
	 * Allowed syntax for JSON path:
	 *
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
	 *
	 * @param jsonSchemaPathString
	 * @throws Exception
	 */
	public JsonSchemaPath(final String jsonSchemaPathString) throws JsonSchemaDefinitionError {
		try (JsonSchemaPathReader jsonSchemaPathReader = new JsonSchemaPathReader(jsonSchemaPathString)) {
			jsonSchemaPathElements = jsonSchemaPathReader.getReadJsonSchemaPathElements();
		} catch (final Exception e) {
			throw new JsonSchemaDefinitionError(e.getMessage(), null, e);
		}
	}

	public JsonSchemaPath(final JsonSchemaPath JsonSchemaPath) {
		jsonSchemaPathElements = new Stack<>();
		jsonSchemaPathElements.addAll(JsonSchemaPath.getPathParts());
	}

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

	public String getReferenceFormattedPath() throws Exception {
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

	public JsonSchemaPath add(final JsonSchemaPathElement jsonSchemaPathElement) {
		if (jsonSchemaPathElement == null) {
			throw new RuntimeException("Invalid null value for JsonSchemaPathElement");
		} else {
			jsonSchemaPathElements.push(jsonSchemaPathElement);
			return this;
		}
	}

	public JsonSchemaPath addArrayIndex(final int arrayIndex) {
		jsonSchemaPathElements.push(new JsonSchemaPathArrayElement(arrayIndex));
		return this;
	}

	public JsonSchemaPath addPropertyKey(final String propertyKey) {
		jsonSchemaPathElements.push(new JsonSchemaPathPropertyElement(propertyKey));
		return this;
	}

	private class JsonSchemaPathReader extends BasicReader {
		Stack<JsonSchemaPathElement> readJsonSchemaPathElements;

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
						reuseCurrentChar();
						break;
					case '/':
						nextJsonSchemaPathPart = readUpToNext(false, '\\', '/', '[');
						readJsonSchemaPathElements.push(parseJsonSchemaPathElement(nextJsonSchemaPathPart.substring(1).trim()));
						reuseCurrentChar();
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


		public Stack<JsonSchemaPathElement> getReadJsonSchemaPathElements() {
			return readJsonSchemaPathElements;
		}
	}

	private static JsonSchemaPathElement parseJsonSchemaPathElement(final String value) {
		final String valueRaw = value.replace("~0", "~").replace("~1", "/").replace("%25", "%");
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

	public Stack<JsonSchemaPathElement> getPathParts() {
		return jsonSchemaPathElements;
	}

	@Override
	public String toString() {
		return getDotFormattedPath();
	}

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