package de.soderer.json;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaConfiguration;
import de.soderer.json.schema.JsonSchemaVersion;
import de.soderer.json.utilities.Utilities;

public class JsonUtilities {
	public static JsonObject convertXmlDocument(final Document xmlDocument, final boolean throwExceptionOnError) throws Exception {
		try {
			final JsonObject jsonObject = new JsonObject();
			jsonObject.add(xmlDocument.getChildNodes().item(0).getNodeName(), convertXmlNode(xmlDocument.getChildNodes().item(0)));
			return jsonObject;
		} catch (final Exception e) {
			if (throwExceptionOnError) {
				throw new Exception("Invalid data", e);
			} else {
				return null;
			}
		}
	}

	public static JsonObject convertXmlNode(final Node xmlNode) throws Exception {
		final JsonObject jsonObject = new JsonObject();
		if (xmlNode.getAttributes() != null && xmlNode.getAttributes().getLength() > 0) {
			for (int attributeIndex = 0; attributeIndex < xmlNode.getAttributes().getLength(); attributeIndex++) {
				final Node attributeNode = xmlNode.getAttributes().item(attributeIndex);
				jsonObject.add(attributeNode.getNodeName(), attributeNode.getNodeValue());
			}
		}
		if (xmlNode.getChildNodes() != null && xmlNode.getChildNodes().getLength() > 0) {
			for (int i = 0; i < xmlNode.getChildNodes().getLength(); i++) {
				final Node childNode = xmlNode.getChildNodes().item(i);
				if (childNode.getNodeType() == Node.TEXT_NODE) {
					if (Utilities.isNotBlank(childNode.getNodeValue())) {
						jsonObject.add("text", childNode.getNodeValue());
					}
				} else if (childNode.getNodeType() == Node.COMMENT_NODE) {
					// do nothing
				} else if (childNode.getChildNodes().getLength() == 1 && childNode.getChildNodes().item(0).getNodeType() == Node.TEXT_NODE) {
					// only one textnode under this node
					jsonObject.add(childNode.getNodeName(), childNode.getChildNodes().item(0).getNodeValue());
				} else {
					final Node xmlSubNode = childNode;
					final JsonObject nodeJsonObject = convertXmlNode(xmlSubNode);
					if (nodeJsonObject != null) {
						jsonObject.add(xmlSubNode.getNodeName(), nodeJsonObject);
					}
				}
			}
		}
		return jsonObject;
	}

	public static Document convertToXmlDocument(final JsonNode jsonNode, final boolean useAttributes) throws Exception {
		try {
			final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			final Document xmlDocument = documentBuilder.newDocument();
			xmlDocument.setXmlStandalone(true);
			List<Node> mainNodes;
			switch (jsonNode.getJsonDataType()) {
				case OBJECT:
					mainNodes = convertToXmlNodes((JsonObject) jsonNode, xmlDocument, useAttributes);
					if (mainNodes == null || mainNodes.size() < 1) {
						throw new Exception("No data found");
					} else if (mainNodes.size() == 1) {
						xmlDocument.appendChild(mainNodes.get(0));
					} else {
						final Node rootNode = xmlDocument.createElement("root");
						for (final Node subNode : mainNodes) {
							if (subNode instanceof Attr) {
								rootNode.getAttributes().setNamedItem(subNode);
							} else {
								rootNode.appendChild(subNode);
							}
						}
						xmlDocument.appendChild(rootNode);
					}
					break;
				case ARRAY:
					mainNodes = convertToXmlNodes((JsonArray) jsonNode, "root", xmlDocument, useAttributes);
					if (mainNodes == null || mainNodes.size() < 1) {
						throw new Exception("No data found");
					} else if (mainNodes.size() == 1) {
						xmlDocument.appendChild(mainNodes.get(0));
					} else {
						final Node rootNode = xmlDocument.createElement("root");
						for (final Node subNode : mainNodes) {
							if (subNode instanceof Attr) {
								rootNode.getAttributes().setNamedItem(subNode);
							} else {
								rootNode.appendChild(subNode);
							}
						}
						xmlDocument.appendChild(rootNode);
					}
					break;
				case STRING:
					final Node rootNodeString = xmlDocument.createElement("root");
					rootNodeString.setTextContent(((JsonValueString) jsonNode).getValue().toString());
					xmlDocument.appendChild(rootNodeString);
					break;
				case INTEGER:
					final Node rootNodeInteger = xmlDocument.createElement("root");
					rootNodeInteger.setTextContent(((JsonValueInteger) jsonNode).getValue().toString());
					xmlDocument.appendChild(rootNodeInteger);
					break;
				case NUMBER:
					final Node rootNodeNumber = xmlDocument.createElement("root");
					rootNodeNumber.setTextContent(((JsonValueNumber) jsonNode).getValue().toString());
					xmlDocument.appendChild(rootNodeNumber);
					break;
				case BOOLEAN:
					final Node rootNodeBoolean = xmlDocument.createElement("root");
					rootNodeBoolean.setTextContent(((JsonValueBoolean) jsonNode).getValue().toString());
					xmlDocument.appendChild(rootNodeBoolean);
					break;
				case NULL:
					final Node rootNodeNull = xmlDocument.createElement("root");
					rootNodeNull.setTextContent("null");
					xmlDocument.appendChild(rootNodeNull);
					break;
				default:
					throw new RuntimeException("Unknown JsonDataType: '" + jsonNode.getJsonDataType().getName() + "'");
			}

			return xmlDocument;
		} catch (final Exception e) {
			throw new Exception("Invalid data", e);
		}
	}

	public static List<Node> convertToXmlNodes(final JsonObject jsonObject, final Document xmlDocument, final boolean useAttributes) {
		final List<Node> list = new ArrayList<>();

		for (final String key : jsonObject.keySet()) {
			final Object subItem = jsonObject.get(key);
			if (subItem instanceof JsonObject) {
				final Node newNode = xmlDocument.createElement(key);
				list.add(newNode);
				for (final Node subNode : convertToXmlNodes((JsonObject) subItem, xmlDocument, useAttributes)) {
					if (subNode instanceof Attr) {
						newNode.getAttributes().setNamedItem(subNode);
					} else {
						newNode.appendChild(subNode);
					}
				}
			} else if (subItem instanceof JsonArray) {
				for (final Node subNode : convertToXmlNodes((JsonArray) subItem, key, xmlDocument, useAttributes)) {
					list.add(subNode);
				}
			} else if (useAttributes) {
				final Attr newAttr = xmlDocument.createAttribute(key);
				newAttr.setNodeValue(subItem.toString());
				list.add(newAttr);
			} else {
				final Node newNode = xmlDocument.createElement(key);
				list.add(newNode);
				newNode.setTextContent(subItem.toString());
			}
		}

		return list;
	}

	public static List<Node> convertToXmlNodes(final JsonArray jsonArray, final String nodeName, final Document xmlDocument, final boolean useAttributes) {
		final List<Node> list = new ArrayList<>();

		if (jsonArray.size() > 0) {
			for (final JsonNode subItem : jsonArray.items()) {
				if (subItem instanceof JsonObject) {
					final Node newNode = xmlDocument.createElement(nodeName);
					list.add(newNode);
					for (final Node subNode : convertToXmlNodes((JsonObject) subItem, xmlDocument, useAttributes)) {
						if (subNode instanceof Attr) {
							newNode.getAttributes().setNamedItem(subNode);
						} else {
							newNode.appendChild(subNode);
						}
					}
				} else if (subItem instanceof JsonArray) {
					final Node newNode = xmlDocument.createElement(nodeName);
					list.add(newNode);
					for (final Node subNode : convertToXmlNodes((JsonArray) subItem, nodeName, xmlDocument, useAttributes)) {
						newNode.appendChild(subNode);
					}
				} else {
					final Node newNode = xmlDocument.createElement(nodeName);
					list.add(newNode);
					newNode.setTextContent(subItem.toString());
				}
			}
		} else {
			final Node newNode = xmlDocument.createElement(nodeName);
			list.add(newNode);
		}

		return list;
	}

	public static JsonNode parseJsonDataAndVerifyJsonSchemaSimple(final byte[] jsonData, final Charset encoding, final String jsonSchemaFileName) throws Exception {
		JsonSchema jsonSchema;
		try (InputStream jsonSchemaInputStream = new FileInputStream(jsonSchemaFileName)) {
			jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration().setEncoding(encoding).setJsonSchemaVersion(JsonSchemaVersion.simple));
		}
		return jsonSchema.validate(new ByteArrayInputStream(jsonData), encoding);
	}

	public static JsonNode parseJsonDataAndVerifyJsonSchemaV4(final byte[] jsonData, final Charset encoding, final String jsonSchemaFileName) throws Exception {
		JsonSchema jsonSchema;
		try (InputStream jsonSchemaInputStream = new FileInputStream(jsonSchemaFileName)) {
			jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration().setEncoding(encoding).setJsonSchemaVersion(JsonSchemaVersion.draftV4));
		}
		return jsonSchema.validate(new ByteArrayInputStream(jsonData), encoding);
	}

	public static JsonNode parseJsonDataAndVerifyJsonSchemaV6(final byte[] jsonData, final Charset encoding, final String jsonSchemaFileName) throws Exception {
		JsonSchema jsonSchema;
		try (InputStream jsonSchemaInputStream = new FileInputStream(jsonSchemaFileName)) {
			jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration().setEncoding(encoding).setJsonSchemaVersion(JsonSchemaVersion.draftV6));
		}
		return jsonSchema.validate(new ByteArrayInputStream(jsonData), encoding);
	}

	public static JsonNode parseJsonDataAndVerifyJsonSchemaV7(final byte[] jsonData, final Charset encoding, final String jsonSchemaFileName) throws Exception {
		JsonSchema jsonSchema;
		try (InputStream jsonSchemaInputStream = new FileInputStream(jsonSchemaFileName)) {
			jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration().setEncoding(encoding).setJsonSchemaVersion(JsonSchemaVersion.draftV7));
		}
		return jsonSchema.validate(new ByteArrayInputStream(jsonData), encoding);
	}

	/**
	 * Check for a valid JSON schema definition.
	 *
	 * @param jsonSchemaData
	 * @param encoding
	 * @return
	 * @throws Exception
	 */
	public static JsonNode validateJsonSchemaSimple(final byte[] jsonSchemaData, final Charset encoding) throws Exception {
		return validateJsonSchema(jsonSchemaData, encoding, JsonSchemaVersion.simple);
	}

	/**
	 * Check for a valid JSON schema definition.
	 *
	 * @param jsonSchemaData
	 * @param encoding
	 * @return
	 * @throws Exception
	 */
	public static JsonNode validateJsonSchemaV4(final byte[] jsonSchemaData, final Charset encoding) throws Exception {
		return validateJsonSchema(jsonSchemaData, encoding, JsonSchemaVersion.draftV4);
	}

	/**
	 * Check for a valid JSON schema definition.
	 *
	 * @param jsonSchemaData
	 * @param encoding
	 * @return
	 * @throws Exception
	 */
	public static JsonNode validateJsonSchemaV6(final byte[] jsonSchemaData, final Charset encoding) throws Exception {
		return validateJsonSchema(jsonSchemaData, encoding, JsonSchemaVersion.draftV6);
	}

	/**
	 * Check for a valid JSON schema definition.
	 *
	 * @param jsonSchemaData
	 * @param encoding
	 * @return
	 * @throws Exception
	 */
	public static JsonNode validateJsonSchemaV7(final byte[] jsonSchemaData, final Charset encoding) throws Exception {
		return validateJsonSchema(jsonSchemaData, encoding, JsonSchemaVersion.draftV7);
	}

	/**
	 * Check for a valid JSON schema definition.
	 *
	 * @param jsonSchemaData
	 * @param encoding
	 * @return
	 * @throws Exception
	 */
	public static JsonNode validateJsonSchema(final byte[] jsonSchemaData, final Charset encoding, final JsonSchemaVersion jsonSchemaVersion) throws Exception {
		JsonSchema jsonSchema;
		try (InputStream jsonSchemaInputStream = JsonSchema.class.getClassLoader().getResourceAsStream(jsonSchemaVersion.getLocalFile());) {
			jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration().setEncoding(encoding));
		}
		return jsonSchema.validate(new ByteArrayInputStream(jsonSchemaData), encoding);
	}

	public static JsonNode validateJson(final byte[] jsonData, final Charset encoding) throws Exception {
		try (JsonReader jsonReader = new JsonReader(new ByteArrayInputStream(jsonData), encoding)) {
			return jsonReader.read();
		}
	}

	public static JsonNode validateJson5(final byte[] jsonData, final Charset encoding) throws Exception {
		try (JsonReader jsonReader = new Json5Reader(new ByteArrayInputStream(jsonData), encoding)) {
			return jsonReader.read();
		}
	}
}
