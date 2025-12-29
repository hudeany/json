package de.soderer.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

import de.soderer.json.path.JsonPath;
import de.soderer.json.path.JsonPathArrayElement;
import de.soderer.json.path.JsonPathPropertyElement;
import de.soderer.json.utilities.BasicReader;
import de.soderer.json.utilities.NumberUtilities;

public class JsonReader extends BasicReader {
	protected JsonNode currentObject = null;

	protected Stack<JsonToken> openJsonItems = new Stack<>();
	protected JsonPath currentJsonPath = new JsonPath();

	public enum JsonToken {
		JsonObject_Open,
		JsonObject_PropertyKey,
		JsonObject_Close,
		JsonArray_Open,
		JsonArray_Close,
		JsonSimpleValue
	}

	public JsonReader(final InputStream inputStream) throws Exception {
		super(inputStream, null);
	}

	public JsonReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		super(inputStream, encodingCharset);
	}

	public JsonNode getCurrentObject() {
		return currentObject;
	}

	public JsonToken getCurrentToken() {
		if (openJsonItems.empty()) {
			return null;
		} else {
			return openJsonItems.peek();
		}
	}

	public JsonToken readNextToken() throws Exception {
		final JsonToken jsonToken = readNextTokenInternal(true);

		return jsonToken;
	}

	protected JsonToken readNextTokenInternal(final boolean updateJsonPath) throws Exception {
		currentObject = null;
		Character currentChar = readNextNonWhitespace();
		if (currentChar == null) {
			if (openJsonItems.size() > 0) {
				throw new Exception("Premature end of data");
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
				if (openJsonItems.pop() != JsonToken.JsonObject_Open) {
					throw new Exception("Invalid json data '" + currentChar + "' in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
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
				if (openJsonItems.pop() != JsonToken.JsonArray_Open) {
					throw new Exception("Invalid json data '" + currentChar + "' in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
				} else {
					jsonToken = JsonToken.JsonArray_Close;
				}
				break;
			case ',': // Separator of JsonObject properties or JsonArray items
				if (!updateJsonPath) {
					// Multiple comma
					throw new Exception("Invalid json data '" + currentChar + "' in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
				} else {
					currentChar = readNextNonWhitespace();
					if (currentChar == null || currentChar == '}' || currentChar == ']') {
						throw new Exception("Invalid json data '" + currentChar + "' in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
					} else {
						reuse(currentChar);
						jsonToken = readNextTokenInternal(false);
					}
					break;
				}
			case '\'': // Not allowed single-quoted value
				throw new Exception("Invalid json data '" + currentChar + "' in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
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
						throw new Exception("Invalid json data '" + currentChar + "' in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
					}
					openJsonItems.push(JsonToken.JsonObject_PropertyKey);
					jsonToken = JsonToken.JsonObject_PropertyKey;
				} else if (openJsonItems.peek() == JsonToken.JsonObject_PropertyKey) {
					currentObject = new JsonValueString(readQuotedText('\\'));
					openJsonItems.pop();
					currentChar = readNextNonWhitespace();
					if (currentChar == null) {
						throw new Exception("Invalid json data '" + currentChar + "' in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
					} else if (currentChar == '}') {
						reuse(currentChar);
					} else if (currentChar != ',') {
						throw new Exception("Invalid json data '" + currentChar + "' in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
					}
					jsonToken = JsonToken.JsonSimpleValue;
				} else {
					throw new Exception("Invalid json data '" + currentChar + "' in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
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
						throw new Exception("Invalid json data end in line " + (getReadLines() + 1) + " at overall index " + getReadCharacters());
					} else if (currentChar == '}') {
						reuse(currentChar);
					} else {
						currentChar = readNextNonWhitespace();
						if (currentChar == null || currentChar == '}') {
							throw new Exception("Invalid json data '" + currentChar + "' in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
						} else {
							reuse(currentChar);
						}
					}
					jsonToken = JsonToken.JsonSimpleValue;
				} else {
					throw new Exception("Invalid json data '" + currentChar + "' in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
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
	 * Use "readNextToken" first to init read by node steps
	 */
	public JsonNode readNextJsonNode() throws Exception {
		if (getReadCharacters() == 0) {
			throw new Exception("JsonReader position was not initialized for 'readNextJsonNode'. Use 'readNextToken' or 'readUpToJsonPath' to init.");
		}

		switch (readNextToken()) {
			case JsonObject_Open:
				return readJsonObject().setRootNode(false);
			case JsonArray_Open:
				return readJsonArray().setRootNode(false);
			case JsonSimpleValue:
				// value was already read
				return currentObject.setRootNode(false);
			case JsonObject_Close:
				reuseCurrentChar();
				openJsonItems.push(JsonToken.JsonObject_Open);
				return null;
			case JsonArray_Close:
				reuseCurrentChar();
				openJsonItems.push(JsonToken.JsonArray_Open);
				return null;
			case JsonObject_PropertyKey:
				return currentObject.setRootNode(false);
			default:
				throw new Exception("Invalid data in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
		}
	}

	/**
	 * Read all available Json data from the input stream at once.
	 * This can only be done once and as the first action on a JsonReader.
	 *
	 * @return JsonObject or JsonArray
	 * @throws Exception
	 */
	public JsonNode read() throws Exception {
		if (getReadCharacters() != 0) {
			throw new Exception("JsonReader position was already initialized for other read operation");
		}

		final JsonToken nextToken = readNextToken();
		if (nextToken == JsonToken.JsonObject_Open) {
			return readJsonObject().setRootNode(true);
		} else if (nextToken == JsonToken.JsonArray_Open) {
			return readJsonArray().setRootNode(true);
		} else if (nextToken == JsonToken.JsonSimpleValue) {
			return currentObject.setRootNode(true);
		} else {
			throw new Exception("Invalid json data: No JSON data found at root");
		}
	}

	private JsonObject readJsonObject() throws Exception {
		if (openJsonItems.peek() != JsonToken.JsonObject_Open) {
			throw new Exception("Invalid read position for JsonArray in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
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
						throw new Exception("Unexpected JsonToken " + nextToken + " in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
					}
					nextToken = readNextToken();
				} else {
					throw new Exception("Unexpected JsonToken " + nextToken + " in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
				}
			}
			return returnObject;
		}
	}

	private JsonArray readJsonArray() throws Exception {
		if (openJsonItems.peek() != JsonToken.JsonArray_Open) {
			throw new Exception("Invalid read position for JsonArray in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
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
				throw new Exception("Unexpected JsonToken " + nextToken + " in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
			}
		}
	}

	private JsonNode readSimpleUnquotedJsonValue(final String valueString) throws Exception {
		if (valueString == null) {
			throw new Exception("Invalid empty json data");
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
			throw new Exception("Invalid json data in line " + (getReadLines() + 1) +" at overall index " + getReadCharacters());
		}
	}

	/**
	 * This method should only be used to read small Json items
	 *
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public static JsonNode readJsonItemString(final String data) throws Exception {
		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
			try (JsonReader jsonReader = new JsonReader(inputStream)) {
				return jsonReader.read();
			}
		}
	}

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
					throw new Exception("Invalid jsonToken");
			}
		}
	}

	private void riseArrayIndex() {
		final JsonPathArrayElement currentJsonPathArrayElement = (JsonPathArrayElement) currentJsonPath.getLastPathPart();
		currentJsonPath.removeLastElement();
		currentJsonPath.add(new JsonPathArrayElement(currentJsonPathArrayElement.getIndex() + 1));
	}

	/**
	 *
	 * JsonPath syntax:<br />
	 *	$ : root<br />
	 *	. : child separator<br />
	 *	[n] : array operator<br />
	 *<br />
	 * JsonPath example:<br />
	 * 	"$.list.customer[0].name"<br />
	 */
	public JsonPath getCurrentJsonPath() {
		return currentJsonPath;
	}

	/**
	 * JsonPath syntax:<br />
	 *	$ : root<br />
	 *	. or / : child separator<br />
	 *	[n] : array operator<br />
	 *<br />
	 * JsonPath example:<br />
	 * 	"$.list.customer[0].name"<br />
	 *
	 * @param jsonReader
	 * @param jsonPath
	 * @throws Exception
	 */
	public void readUpToJsonPath(final String jsonPathString) throws Exception {
		final JsonPath readJsonPath = new JsonPath(jsonPathString);

		while (readNextToken() != null && !getCurrentJsonPath().equals(readJsonPath)) {
			// Do nothing
		}

		if (!getCurrentJsonPath().equals(readJsonPath)) {
			throw new Exception("Path '" + jsonPathString + "' is not part of the JSON data");
		}
	}

	public void readUpToJsonPath(final JsonPath jsonPath) throws Exception {
		while (readNextToken() != null && !getCurrentJsonPath().equals(jsonPath)) {
			// Do nothing
		}

		if (!getCurrentJsonPath().equals(jsonPath)) {
			throw new Exception("Path '" + jsonPath + "' is not part of the JSON data");
		}
	}
}
