package de.soderer.json;

import java.util.Map.Entry;

import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;

public class JsonToYamlConverter {
	public static YamlNode convert(final JsonNode jsonNode) throws DuplicateKeyException {
		if (jsonNode == null) {
			return null;
		} else if (jsonNode instanceof JsonObject) {
			return convert((JsonObject) jsonNode);
		} else if (jsonNode instanceof JsonArray) {
			return convert((JsonArray) jsonNode);
		} else if (jsonNode instanceof JsonValueNull) {
			return new YamlScalar(null);
		} else if (jsonNode instanceof JsonValueString) {
			return new YamlScalar(((JsonValueString) jsonNode).getValue(), YamlScalarType.STRING);
		} else if (jsonNode instanceof JsonValueInteger) {
			return new YamlScalar(((JsonValueInteger) jsonNode).getValue(), YamlScalarType.NUMBER);
		} else if (jsonNode instanceof JsonValueNumber) {
			return new YamlScalar(((JsonValueNumber) jsonNode).getValue(), YamlScalarType.NUMBER);
		} else if (jsonNode instanceof JsonValueBoolean) {
			return new YamlScalar(((JsonValueBoolean) jsonNode).getValue(), YamlScalarType.BOOLEAN);
		} else {
			throw new RuntimeException("Unsupported JsonNode item type found: '" + jsonNode.getClass().getSimpleName() + "'");
		}
	}

	public static YamlMapping convert(final JsonObject jsonObject) throws DuplicateKeyException {
		if (jsonObject == null) {
			return null;
		} else {
			final YamlMapping returnYamlMapping = new YamlMapping();
			for (final Entry<String, JsonNode> entry : jsonObject.entrySet()) {
				if (entry.getValue() instanceof JsonObject) {
					returnYamlMapping.add(entry.getKey(), convert((JsonObject) entry.getValue()));
				} else if (entry.getValue() instanceof JsonArray) {
					returnYamlMapping.add(entry.getKey(), convert((JsonArray) entry.getValue()));
				} else if (entry.getValue() instanceof JsonValueNull) {
					returnYamlMapping.add(entry.getKey(), null);
				} else if (entry.getValue() instanceof JsonValueString) {
					returnYamlMapping.add(entry.getKey(), ((JsonValueString) entry.getValue()).getValue());
				} else if (entry.getValue() instanceof JsonValueInteger) {
					returnYamlMapping.add(entry.getKey(), ((JsonValueInteger) entry.getValue()).getValue());
				} else if (entry.getValue() instanceof JsonValueNumber) {
					returnYamlMapping.add(entry.getKey(), ((JsonValueNumber) entry.getValue()).getValue());
				} else if (entry.getValue() instanceof JsonValueBoolean) {
					returnYamlMapping.add(entry.getKey(), ((JsonValueBoolean) entry.getValue()).getValue());
				} else {
					throw new RuntimeException("Unsupported JsonNode value type found in JsonObject: '" + entry.getValue().getClass().getSimpleName() + "'");
				}
			}
			return returnYamlMapping;
		}
	}

	public static YamlSequence convert(final JsonArray jsonArray) throws DuplicateKeyException {
		if (jsonArray == null) {
			return null;
		} else {
			final YamlSequence returnYamlSequence = new YamlSequence();
			for (final JsonNode item : jsonArray.items()) {
				if (item instanceof JsonObject) {
					returnYamlSequence.add(convert((JsonObject) item));
				} else if (item instanceof JsonArray) {
					returnYamlSequence.add(convert((JsonArray) item));
				} else if (item instanceof JsonValueNull) {
					returnYamlSequence.addNull();
				} else if (item instanceof JsonValueString) {
					returnYamlSequence.add(((JsonValueString) item).getValue());
				} else if (item instanceof JsonValueInteger) {
					returnYamlSequence.add(((JsonValueInteger) item).getValue());
				} else if (item instanceof JsonValueNumber) {
					returnYamlSequence.add(((JsonValueNumber) item).getValue());
				} else if (item instanceof JsonValueBoolean) {
					returnYamlSequence.add(((JsonValueBoolean) item).getValue());
				} else {
					throw new RuntimeException("Unsupported JsonNode item type found in JsonArray: '" + item.getClass().getSimpleName() + "'");
				}
			}
			return returnYamlSequence;
		}
	}
}
