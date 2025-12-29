package de.soderer.json;

import de.soderer.json.path.JsonPath;
import de.soderer.json.path.JsonPathArrayElement;
import de.soderer.json.path.JsonPathElement;
import de.soderer.json.path.JsonPathException;
import de.soderer.json.path.JsonPathPropertyElement;
import de.soderer.json.path.JsonPathRoot;

public class JsonNode {
	protected final JsonDataType jsonDataType;

	private boolean rootNode;

	protected JsonNode(final JsonDataType jsonDataType) {
		this.jsonDataType = jsonDataType;
	}

	public JsonDataType getJsonDataType() {
		return jsonDataType;
	}

	public boolean isRootNode() {
		return rootNode;
	}

	public JsonNode setRootNode(final boolean rootNode) {
		this.rootNode = rootNode;
		return this;
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

	public boolean isFloat() {
		return jsonDataType == JsonDataType.FLOAT;
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
				|| jsonDataType == JsonDataType.BOOLEAN
				|| jsonDataType == JsonDataType.INTEGER
				|| jsonDataType == JsonDataType.FLOAT
				|| jsonDataType == JsonDataType.STRING;
	}

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
						throw new JsonPathException("JsonNode does noth include path", jsonPath);
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
}
