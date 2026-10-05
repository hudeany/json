package de.soderer.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

import de.soderer.json.exception.JsonDataException;
import de.soderer.json.exception.JsonReaderStateException;
import de.soderer.json.exception.UnexpectedEndOfJsonDataException;
import de.soderer.json.exception.UnexpectedJsonTokenException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.path.JsonPathArrayElement;
import de.soderer.json.path.JsonPathException;
import de.soderer.json.path.JsonPathPropertyElement;
import de.soderer.json.utilities.BasicReader;
import de.soderer.json.utilities.NumberUtilities;

/**
 * Reader for standard JSON data (RFC 8259) from an input stream.
 * <p>
 * The data can be read completely by {@link #read()}, or token by token by
 * {@link #readNextToken()} and node by node by {@link #readNextJsonNode()}, e.g. to process
 * large arrays item by item after positioning with {@link #readUpToJsonPath(String)}.
 * </p>
 */
public class JsonReader extends BasicReader {
	/**
	 * Simple value or property key read by the last token, otherwise null.
	 */
	protected JsonNode currentObject = null;

	/**
	 * Stack of the currently open objects, arrays and property keys.
	 */
	protected Stack<JsonToken> openJsonItems = new Stack<>();
	/**
	 * JSON path of the current read position.
	 */
	protected JsonPath currentJsonPath = new JsonPath();

	/**
	 * Tokens of JSON data.
	 */
	public enum JsonToken {
		/**
		 * Opening brace of an object.
		 */
		JsonObject_Open,
		/**
		 * Property key of an object, see {@link JsonReader#getCurrentObject()}.
		 */
		JsonObject_PropertyKey,
		/**
		 * Closing brace of an object.
		 */
		JsonObject_Close,
		/**
		 * Opening bracket of an array.
		 */
		JsonArray_Open,
		/**
		 * Closing bracket of an array.
		 */
		JsonArray_Close,
		/**
		 * Simple value, see {@link JsonReader#getCurrentObject()}.
		 */
		JsonSimpleValue
	}

	/**
	 * Creates a new JSON reader using UTF-8 encoding.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @throws Exception
	 *             if the input stream is null
	 */
	public JsonReader(final InputStream inputStream) throws Exception {
		super(inputStream, null);
	}

	/**
	 * Creates a new JSON reader.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @param encodingCharset
	 *            the encoding of the data, or null for UTF-8
	 * @throws Exception
	 *             if the input stream is null
	 */
	public JsonReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		super(inputStream, encodingCharset);
	}

	/**
	 * Returns the simple value or property key read by the last token.
	 *
	 * @return the value or property key, or null if the last token was no simple value or property
	 *         key
	 */
	public JsonNode getCurrentObject() {
		return currentObject;
	}

	/**
	 * Returns the innermost open item (object, array or property key).
	 *
	 * @return the innermost open item, or null if no item is open
	 */
	public JsonToken getCurrentToken() {
		if (openJsonItems.empty()) {
			return null;
		} else {
			return openJsonItems.peek();
		}
	}

	/**
	 * Reads the next token and updates the current JSON path.
	 *
	 * @return the token read, or null at the end of the data
	 * @throws Exception
	 *             if the JSON data is invalid
	 */
	public JsonToken readNextToken() throws Exception {
		return readNextTokenInternal(true);
	}

	/**
	 * Reads the next token. Subclasses override this for other syntaxes like JSON5.
	 *
	 * @param updateJsonPath
	 *            true to update the current JSON path
	 * @return the token read, or null at the end of the data
	 * @throws Exception
	 *             if the JSON data is invalid
	 */
	protected JsonToken readNextTokenInternal(final boolean updateJsonPath) throws Exception {
		currentObject = null;
		Character currentChar = readNextNonWhitespace();
		if (currentChar == null) {
			if (openJsonItems.size() > 0) {
				throw new UnexpectedEndOfJsonDataException(getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
			} else {
				return null;
			}
		}

		JsonToken jsonToken;
		switch (currentChar) {
			case '{': // Open JsonObject
				if (openJsonItems.size() > 0 && openJsonItems.peek() == JsonToken.JsonObject_PropertyKey) {
					openJsonItems.pop();
				}
				openJsonItems.push(JsonToken.JsonObject_Open);
				jsonToken = JsonToken.JsonObject_Open;
				break;
			case '}': // Close JsonObject
				if (openJsonItems.size() == 0 || openJsonItems.pop() != JsonToken.JsonObject_Open) {
					throw new JsonDataException(currentChar, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
				} else {
					jsonToken = JsonToken.JsonObject_Close;
				}
				break;
			case '[': // Open JsonArray
				if (openJsonItems.size() > 0 && openJsonItems.peek() == JsonToken.JsonObject_PropertyKey) {
					openJsonItems.pop();
				}
				openJsonItems.push(JsonToken.JsonArray_Open);
				jsonToken = JsonToken.JsonArray_Open;
				break;
			case ']': // Close JsonArray
				if (openJsonItems.size() == 0 || openJsonItems.pop() != JsonToken.JsonArray_Open) {
					throw new JsonDataException(currentChar, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
				} else {
					jsonToken = JsonToken.JsonArray_Close;
				}
				break;
			case ',': // Separator of JsonObject properties or JsonArray items
				if (!updateJsonPath) {
					// Multiple comma
					throw new JsonDataException(currentChar, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
				} else {
					currentChar = readNextNonWhitespace();
					if (currentChar == null) {
						throw new UnexpectedEndOfJsonDataException(getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
					} else if (currentChar == '}' || currentChar == ']') {
						throw new JsonDataException(currentChar, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
					} else {
						reuse(currentChar);
						jsonToken = readNextTokenInternal(false);
					}
					break;
				}
			case '\'': // Not allowed single-quoted value
				throw new JsonDataException(currentChar, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
			case '"': // Start JsonObject propertykey or propertyvalue or JsonArray item
				if (openJsonItems.size() == 0) {
					currentObject = new JsonValueString(readQuotedText('\\'));
					jsonToken = JsonToken.JsonSimpleValue;
				} else if (openJsonItems.peek() == JsonToken.JsonArray_Open) {
					currentObject = new JsonValueString(readQuotedText('\\'));
					jsonToken = JsonToken.JsonSimpleValue;
				} else if (openJsonItems.peek() == JsonToken.JsonObject_Open) {
					currentObject = new JsonValueString(readQuotedText('\\'));
					if (readNextNonWhitespace() != ':') {
						throw new JsonDataException(currentChar, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
					}
					openJsonItems.push(JsonToken.JsonObject_PropertyKey);
					jsonToken = JsonToken.JsonObject_PropertyKey;
				} else if (openJsonItems.peek() == JsonToken.JsonObject_PropertyKey) {
					currentObject = new JsonValueString(readQuotedText('\\'));
					openJsonItems.pop();
					currentChar = readNextNonWhitespace();
					if (currentChar == null) {
						throw new UnexpectedEndOfJsonDataException(getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
					} else if (currentChar == '}') {
						reuse(currentChar);
					} else if (currentChar != ',') {
						throw new JsonDataException(currentChar, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
					}
					jsonToken = JsonToken.JsonSimpleValue;
				} else {
					throw new JsonDataException(currentChar, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
				}
				break;
			default: // Start JsonObject PropertyValue or JsonArray item without quotes
				if (openJsonItems.size() == 0) {
					// Standalone simple value
					currentObject = readSimpleUnquotedJsonValue(readUpToNext(false, null).trim());
					jsonToken = JsonToken.JsonSimpleValue;
				} else if (openJsonItems.peek() == JsonToken.JsonArray_Open) {
					currentObject = readSimpleUnquotedJsonValue(readUpToNext(false, null, ',', ']').trim());
					jsonToken = JsonToken.JsonSimpleValue;
				} else if (openJsonItems.peek() == JsonToken.JsonObject_PropertyKey) {
					openJsonItems.pop();
					currentObject = readSimpleUnquotedJsonValue(readUpToNext(false, null, ',', '}').trim());
					currentChar = readNextNonWhitespace();
					if (currentChar == null) {
						throw new UnexpectedEndOfJsonDataException(getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
					} else if (currentChar == '}') {
						reuse(currentChar);
					} else if (currentChar == ',') {
						currentChar = readNextNonWhitespace();
						if (currentChar == null) {
							throw new UnexpectedEndOfJsonDataException(getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
						} else if (currentChar == '}') {
							throw new JsonDataException(currentChar, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
						} else {
							reuse(currentChar);
						}
					} else {
						throw new JsonDataException(currentChar, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
					}
					jsonToken = JsonToken.JsonSimpleValue;
				} else {
					throw new JsonDataException(currentChar, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
				}
				break;
		}

		if (updateJsonPath) {
			updateJsonPath(jsonToken);
		}

		return jsonToken;
	}

	/**
	 * Read JSON data node by node.
	 * Use {@link #readNextToken()} or {@link #readUpToJsonPath(String)} first to init read by node steps.
	 *
	 * @return next JSON node (object, array, simple value or property key),
	 *         or null at the end of the data or of the current object/array
	 * @throws Exception
	 *             if the reader was not initialized or the JSON data is invalid
	 */
	public JsonNode readNextJsonNode() throws Exception {
		if (!readWasInitialized()) {
			throw new JsonReaderStateException("JsonReader position was not initialized for 'readNextJsonNode'. Use 'readNextToken' or 'readUpToJsonPath' to init.");
		}

		final JsonToken token = readNextToken();
		if (token == null) {
			return null;
		} else {
			switch (token) {
				case JsonObject_Open:
					return readJsonObject().withRootNode(false);
				case JsonArray_Open:
					return readJsonArray().withRootNode(false);
				case JsonSimpleValue:
					// value was already read
					return currentObject.withRootNode(false);
				case JsonObject_Close:
					reuseCurrentChar();
					openJsonItems.push(JsonToken.JsonObject_Open);
					return null;
				case JsonArray_Close:
					reuseCurrentChar();
					openJsonItems.push(JsonToken.JsonArray_Open);
					return null;
				case JsonObject_PropertyKey:
					return currentObject.withRootNode(false);
				default:
					throw new UnexpectedJsonTokenException(token, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
			}
		}
	}

	/**
	 * Read all available Json data from the input stream at once.
	 * This can only be done once and as the first action on a JsonReader.
	 *
	 * @return the JSON object, array or simple value, marked as root node
	 * @throws Exception
	 *             if the reader was already used, or the JSON data is empty or invalid
	 */
	public JsonNode read() throws Exception {
		if (readWasInitialized()) {
			throw new JsonReaderStateException("JsonReader position was already initialized for other read operation");
		}

		final JsonToken nextToken = readNextToken();
		if (nextToken == JsonToken.JsonObject_Open) {
			return readJsonObject().withRootNode(true);
		} else if (nextToken == JsonToken.JsonArray_Open) {
			return readJsonArray().withRootNode(true);
		} else if (nextToken == JsonToken.JsonSimpleValue) {
			return currentObject.withRootNode(true);
		} else {
			throw new UnexpectedEndOfJsonDataException(getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
		}
	}

	private JsonObject readJsonObject() throws Exception {
		if (openJsonItems.peek() != JsonToken.JsonObject_Open) {
			throw new JsonReaderStateException("Invalid read position for JsonObject in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
		} else {
			final JsonObject returnObject = new JsonObject();
			JsonToken nextToken = readNextToken();
			while (nextToken != JsonToken.JsonObject_Close) {
				if (nextToken == JsonToken.JsonObject_PropertyKey && currentObject instanceof JsonValueString) {
					final String propertyKey = ((JsonValueString) currentObject).getValue();
					nextToken = readNextToken();
					if (nextToken == JsonToken.JsonArray_Open) {
						returnObject.add(propertyKey, readJsonArray());
					} else if (nextToken == JsonToken.JsonObject_Open) {
						returnObject.add(propertyKey, readJsonObject());
					} else if (nextToken == JsonToken.JsonSimpleValue) {
						returnObject.add(propertyKey, currentObject);
					} else {
						throw new UnexpectedJsonTokenException(nextToken, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
					}
					nextToken = readNextToken();
				} else {
					throw new UnexpectedJsonTokenException(nextToken, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
				}
			}
			return returnObject;
		}
	}

	private JsonArray readJsonArray() throws Exception {
		if (openJsonItems.peek() != JsonToken.JsonArray_Open) {
			throw new JsonReaderStateException("Invalid read position for JsonArray in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
		} else {
			JsonToken nextToken = readNextToken();
			if (nextToken == JsonToken.JsonArray_Close
					|| nextToken == JsonToken.JsonObject_Open
					|| nextToken == JsonToken.JsonArray_Open
					|| nextToken == JsonToken.JsonSimpleValue) {
				final JsonArray returnArray = new JsonArray();
				while (nextToken != JsonToken.JsonArray_Close) {
					if (nextToken == JsonToken.JsonArray_Open) {
						returnArray.add(readJsonArray());
					} else if (nextToken == JsonToken.JsonObject_Open) {
						returnArray.add(readJsonObject());
					} else if (nextToken == JsonToken.JsonSimpleValue) {
						returnArray.add(currentObject);
					}
					nextToken = readNextToken();
				}
				return returnArray;
			} else {
				throw new UnexpectedJsonTokenException(nextToken, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
			}
		}
	}

	private JsonNode readSimpleUnquotedJsonValue(final String valueString) throws Exception {
		if (valueString == null) {
			throw new UnexpectedEndOfJsonDataException(getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
		} else if ("null".equalsIgnoreCase(valueString)) {
			return new JsonValueNull();
		} else if ("true".equalsIgnoreCase(valueString)) {
			return new JsonValueBoolean(true);
		} else if ("false".equalsIgnoreCase(valueString)) {
			return new JsonValueBoolean(false);
		} else if (NumberUtilities.isNumber(valueString)) {
			final Number value = NumberUtilities.parseNumber(valueString);
			if (value instanceof Integer) {
				return new JsonValueInteger((Integer) value);
			} else if (value instanceof Long) {
				return new JsonValueInteger((Long) value);
			} else if (value instanceof BigDecimal && NumberUtilities.isInteger((BigDecimal) value)) {
				return new JsonValueInteger((BigDecimal) value);
			} else {
				return new JsonValueNumber(value);
			}
		} else {
			throw new JsonDataException(valueString, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
		}
	}

	/**
	 * This method should only be used to read small Json items
	 *
	 * @param data
	 *            JSON data as string
	 * @return parsed JSON item
	 * @throws Exception
	 *             if the JSON data is invalid
	 */
	public static JsonNode readJsonItemString(final String data) throws Exception {
		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
			try (JsonReader jsonReader = new JsonReader(inputStream)) {
				return jsonReader.read();
			}
		}
	}

	/**
	 * Updates the current JSON path for a token read.
	 *
	 * @param jsonToken
	 *            the token read
	 * @throws Exception
	 *             if the token is unexpected
	 */
	protected void updateJsonPath(final JsonToken jsonToken) throws Exception {
		if (jsonToken != null) {
			switch (jsonToken) {
				case JsonArray_Open:
					if (currentJsonPath.size() > 0 && currentJsonPath.getLastPathPart() instanceof JsonPathArrayElement) {
						riseArrayIndex();
					}
					currentJsonPath.add(new JsonPathArrayElement());
					break;
				case JsonArray_Close:
					if (currentJsonPath.size() > 0 && currentJsonPath.getLastPathPart() instanceof JsonPathArrayElement) {
						currentJsonPath.removeLastElement();
					}
					if (currentJsonPath.size() > 0 && currentJsonPath.getLastPathPart() instanceof JsonPathPropertyElement) {
						currentJsonPath.removeLastElement();
					}
					break;
				case JsonObject_Open:
					if (currentJsonPath.size() > 0 && currentJsonPath.getLastPathPart() instanceof JsonPathArrayElement) {
						riseArrayIndex();
					}
					break;
				case JsonObject_PropertyKey:
					currentJsonPath.add(new JsonPathPropertyElement(((JsonValueString) getCurrentObject()).getValue()));
					break;
				case JsonSimpleValue:
					if (currentJsonPath.size() > 0) {
						if (currentJsonPath.getLastPathPart() instanceof JsonPathArrayElement) {
							riseArrayIndex();
						} else if (currentJsonPath.getLastPathPart() instanceof JsonPathPropertyElement) {
							currentJsonPath.removeLastElement();
						}
					}
					break;
				case JsonObject_Close:
					if (currentJsonPath.size() > 0 && currentJsonPath.getLastPathPart() instanceof JsonPathPropertyElement) {
						currentJsonPath.removeLastElement();
					}
					break;
				default:
					throw new UnexpectedJsonTokenException(jsonToken, getReadLines(), getReadCharactersInCurrentLine(), getReadCharacters());
			}
		}
	}

	private void riseArrayIndex() {
		final JsonPathArrayElement currentJsonPathArrayElement = (JsonPathArrayElement) currentJsonPath.getLastPathPart();
		currentJsonPath.removeLastElement();
		currentJsonPath.add(new JsonPathArrayElement(currentJsonPathArrayElement.getIndex() + 1));
	}

	/**
	 * Get the JsonPath of the current read position.
	 *
	 * JsonPath syntax:
	 * <pre>
	 * $         : root
	 * .         : child separator
	 * [n]       : array operator
	 * </pre>
	 *
	 * JsonPath example: {@code $.list.customer[0].name}
	 *
	 * @return JsonPath of the current read position
	 */
	public JsonPath getCurrentJsonPath() {
		return currentJsonPath;
	}

	/**
	 * Read up to the given JsonPath, so the next read starts at the item of this path.
	 *
	 * JsonPath syntax:
	 * <pre>
	 * $         : root
	 * . or /    : child separator
	 * [n]       : array operator
	 * </pre>
	 *
	 * JsonPath example: {@code $.list.customer[0].name}
	 *
	 * @param jsonPathString
	 *            JsonPath to read up to
	 * @throws Exception
	 *             if the JsonPath is invalid or not part of the JSON data
	 */
	public void readUpToJsonPath(final String jsonPathString) throws Exception {
		readUpToJsonPath(new JsonPath(jsonPathString));
	}

	/**
	 * Read up to the given JsonPath, so the next read starts at the item of this path.
	 *
	 * @param jsonPath
	 *            JsonPath to read up to
	 * @throws Exception
	 *             if the JsonPath is not part of the JSON data
	 */
	public void readUpToJsonPath(final JsonPath jsonPath) throws Exception {
		while (readNextToken() != null && !getCurrentJsonPath().equals(jsonPath)) {
			// Do nothing
		}

		if (!getCurrentJsonPath().equals(jsonPath)) {
			throw new JsonPathException("Path '" + jsonPath + "' is not part of the JSON data", null);
		}
	}
}
