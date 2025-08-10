package de.soderer.json;

import java.math.BigDecimal;

import de.soderer.json.path.JsonPath;
import de.soderer.json.path.JsonPathArrayElement;
import de.soderer.json.path.JsonPathElement;
import de.soderer.json.path.JsonPathException;
import de.soderer.json.path.JsonPathPropertyElement;
import de.soderer.json.path.JsonPathRoot;

public class JsonNode {
	private final String propertyName;
	private final Object value;
	private JsonDataType jsonDataType;
	private final boolean rootNode;

	public JsonNode(final boolean rootNode, final Object value) throws Exception {
		this(rootNode, null, value);
	}

	public JsonNode(final boolean rootNode, final String propertyName, final Object value) throws Exception {
		this.propertyName = propertyName;
		this.rootNode = rootNode;
		this.value = value;
		if (value == null) {
			jsonDataType = JsonDataType.NULL;
		} else if (value instanceof Boolean) {
			jsonDataType = JsonDataType.BOOLEAN;
		} else if (value instanceof Integer || value instanceof Long || (value instanceof BigDecimal && ((BigDecimal) value).scale() == 0)) {
			jsonDataType = JsonDataType.INTEGER;
		} else if (value instanceof Number) {
			jsonDataType = JsonDataType.NUMBER;
		} else if (value instanceof String || value instanceof Character) {
			jsonDataType = JsonDataType.STRING;
		} else if (value instanceof JsonObject) {
			jsonDataType = JsonDataType.OBJECT;
		} else if (value instanceof JsonArray) {
			jsonDataType = JsonDataType.ARRAY;
		} else {
			throw new Exception("Unknown JSON data type: " + value.getClass().getSimpleName());
		}
	}

	public boolean isRootNode() {
		return rootNode;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public Object getValue() {
		return value;
	}

	public JsonDataType getJsonDataType() {
		return jsonDataType;
	}

	public boolean isNull() {
		return jsonDataType == JsonDataType.NULL;
	}

	public boolean isBoolean() {
		return jsonDataType == JsonDataType.BOOLEAN;
	}

	public boolean isInteger() {
		return jsonDataType == JsonDataType.INTEGER;
	}

	public boolean isNumber() {
		return jsonDataType == JsonDataType.NUMBER || jsonDataType == JsonDataType.INTEGER;
	}

	public boolean isString() {
		return jsonDataType == JsonDataType.STRING;
	}

	public boolean isJsonObject() {
		return jsonDataType == JsonDataType.OBJECT;
	}

	public boolean isJsonArray() {
		return jsonDataType == JsonDataType.ARRAY;
	}

	public boolean isSimpleValue() {
		return jsonDataType == JsonDataType.NULL
				|| jsonDataType == JsonDataType.STRING
				|| jsonDataType == JsonDataType.BOOLEAN
				|| jsonDataType == JsonDataType.INTEGER
				|| jsonDataType == JsonDataType.NUMBER;
	}

	public boolean isKomplexValue() {
		return jsonDataType == JsonDataType.OBJECT
				|| jsonDataType == JsonDataType.ARRAY;
	}

	public Object getDatabyJsonPath(final JsonPath jsonPath) throws JsonPathException {
		Object nextDataObject = value;
		for (final JsonPathElement pathPart : jsonPath.getPathParts()) {
			if (pathPart instanceof JsonPathArrayElement) {
				if (nextDataObject != null && nextDataObject instanceof JsonArray) {
					final JsonArray jsonArray = (JsonArray) nextDataObject;
					final int lookingForIndex = ((JsonPathArrayElement) pathPart).getIndex();
					if (jsonArray.size() > lookingForIndex) {
						nextDataObject = jsonArray.get(lookingForIndex);
					} else {
						throw new JsonPathException("JsonNode does noth include path", jsonPath);
					}
				} else {
					throw new JsonPathException("JsonNode does not include path", jsonPath);
				}
			} else if (pathPart instanceof JsonPathPropertyElement) {
				if (nextDataObject != null && nextDataObject instanceof JsonObject) {
					final JsonObject jsonObject = (JsonObject) nextDataObject;
					final String lookingForPropertyKey = ((JsonPathPropertyElement) pathPart).getPropertyKey();
					if (jsonObject.containsPropertyKey(lookingForPropertyKey)) {
						nextDataObject = jsonObject.get(lookingForPropertyKey);
					} else {
						throw new JsonPathException("JsonNode does noth include path", jsonPath);
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

	@Override
	public String toString() {
		if (propertyName != null) {
			if (isNull()) {
				return propertyName + ": " + "null";
			} else {
				return propertyName + ": " + getValue().toString();
			}
		} else {
			if (isNull()) {
				return "null";
			} else {
				return getValue().toString();
			}
		}
	}
}
