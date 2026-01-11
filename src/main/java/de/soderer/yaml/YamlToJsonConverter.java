package de.soderer.yaml;

import java.util.Map.Entry;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueNull;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueString;
import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;

public class YamlToJsonConverter {
	public static JsonNode convert(final YamlNode yamlNode) throws DuplicateKeyException {
		if (yamlNode == null) {
			return null;
		} else if (yamlNode instanceof YamlMapping) {
			return convert((YamlMapping) yamlNode);
		} else if (yamlNode instanceof YamlSequence) {
			return convert((YamlSequence) yamlNode);
		} else if (yamlNode instanceof YamlScalar) {
			return convert((YamlScalar) yamlNode);
		} else {
			throw new RuntimeException("Unsupported YamlNode value type found: '" + yamlNode.getClass().getSimpleName() + "'");
		}
	}

	public static JsonObject convert(final YamlMapping yamlMapping) throws DuplicateKeyException {
		if (yamlMapping == null) {
			return null;
		} else {
			final JsonObject returnJsonObject = new JsonObject();
			for (final Entry<YamlNode, YamlNode> entry : yamlMapping.entrySet()) {
				if (entry.getKey() instanceof YamlScalar) {
					final YamlScalar keyScalar = (YamlScalar) entry.getKey();
					if (keyScalar.getType() == YamlScalarType.STRING) {
						if (entry.getValue() instanceof YamlMapping) {
							returnJsonObject.add((String) keyScalar.getValue(), convert((YamlMapping) entry.getValue()));
						} else if (entry.getValue() instanceof YamlSequence) {
							returnJsonObject.add((String) keyScalar.getValue(), convert((YamlSequence) entry.getValue()));
						} else if (entry.getValue() instanceof YamlScalar) {
							returnJsonObject.add((String) keyScalar.getValue(), convert(((YamlScalar) entry.getValue())));
						} else {
							throw new RuntimeException("Unsupported YamlNode value type found in YamlMapping: '" + entry.getValue().getClass().getSimpleName() + "'");
						}
					} else {
						throw new RuntimeException("Cannot create JsonObject, because YamlMapping contains non-String key: '" + entry.getKey().getClass().getSimpleName() + "'");
					}
				} else {
					throw new RuntimeException("Cannot create JsonObject, because YamlMapping contains non-String key: '" + entry.getKey().getClass().getSimpleName() + "'");
				}
			}
			return returnJsonObject;
		}
	}

	public static JsonArray convert(final YamlSequence yamlSequence) throws DuplicateKeyException {
		if (yamlSequence == null) {
			return null;
		} else {
			final JsonArray returnJsonArray = new JsonArray();
			for (final YamlNode item : yamlSequence.items()) {
				if (item instanceof YamlMapping) {
					returnJsonArray.add(convert((YamlMapping) item));
				} else if (item instanceof YamlSequence) {
					returnJsonArray.add(convert((YamlSequence) item));
				} else if (item instanceof YamlScalar) {
					returnJsonArray.add(convert((YamlScalar) item));
				} else {
					throw new RuntimeException("Unsupported YamlNode item type found in YamlSequence: '" + item.getClass().getSimpleName() + "'");
				}
			}
			return returnJsonArray;
		}
	}

	public static JsonNode convert(final YamlDocument yamlDocument) throws DuplicateKeyException {
		if (yamlDocument == null || yamlDocument.getRoot() == null) {
			return null;
		} else {
			return convert(yamlDocument.getRoot());
		}
	}

	public static JsonNode convert(final YamlScalar yamlScalar) {
		if (yamlScalar == null) {
			return null;
		} else {
			switch (yamlScalar.getType()) {
				case NULL_VALUE:
					return new JsonValueNull();
				case STRING:
				case MULTILINE_FOLDED:
				case MULTILINE_LITERAL:
					return new JsonValueString((String) yamlScalar.getValue());
				case NUMBER:
					return new JsonValueNumber((Number) yamlScalar.getValue());
				case BOOLEAN:
					return new JsonValueBoolean((Boolean) yamlScalar.getValue());
				default:
					throw new RuntimeException("Unsupported YamlScalar value type found: '" + yamlScalar.getType().name() + "'");
			}
		}
	}
}
