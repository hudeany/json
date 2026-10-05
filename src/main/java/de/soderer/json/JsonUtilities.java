package de.soderer.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaVersion;
import de.soderer.json.utilities.Utilities;

/**
 * Helper methods for converting between JSON and XML, for validating JSON data and for
 * validating JSON schema definitions.
 */
public class JsonUtilities {
	/**
	 * Utility class, not to be instantiated.
	 */
	private JsonUtilities() {
	}

	/**
	 * Converts an XML document to a JSON object with the root element name as single property.
	 * See {@link #convertXmlNode(Node)} for the conversion rules.
	 *
	 * @param xmlDocument
	 *            the XML document
	 * @param throwExceptionOnError
	 *            true to throw an exception for invalid data, false to return null
	 * @return the JSON object, or null for invalid data if no exception is thrown
	 * @throws Exception
	 *             if the data is invalid and throwExceptionOnError is set
	 */
	public static JsonObject convertXmlDocument(final Document xmlDocument, final boolean throwExceptionOnError) throws Exception {
		try {
			final JsonObject jsonObject = new JsonObject();
			// The document element, not the first child node, which may be a comment or processing instruction
			final Node rootElement = xmlDocument.getDocumentElement();
			jsonObject.add(rootElement.getNodeName(), convertXmlNode(rootElement));
			return jsonObject;
		} catch (final Exception e) {
			if (throwExceptionOnError) {
				throw new Exception("Invalid data", e);
			} else {
				return null;
			}
		}
	}

	/**
	 * Converts an XML element to a JSON object: attributes become string properties, child elements
	 * with only text become string properties, other child elements become object properties, and
	 * non-blank text content becomes the property "text". Comments are ignored.
	 *
	 * @param xmlNode
	 *            the XML element
	 * @return the JSON object
	 * @throws Exception
	 *             if the element contains a name more than once, e.g. repeated child elements
	 */
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

	/**
	 * Converts JSON data to an XML document. A JSON object with a single property becomes the root
	 * element, otherwise an element "root" is created. Array items become repeated elements with the
	 * name of the array property.
	 *
	 * @param jsonNode
	 *            the JSON data
	 * @param useAttributes
	 *            true to convert simple object properties to XML attributes, false to child elements
	 * @return the XML document
	 * @throws Exception
	 *             if the JSON data is empty or cannot be converted
	 */
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

	/**
	 * Converts the properties of a JSON object to XML nodes.
	 *
	 * @param jsonObject
	 *            the JSON object
	 * @param xmlDocument
	 *            the XML document to create the nodes for
	 * @param useAttributes
	 *            true to convert simple properties to XML attributes, false to child elements
	 * @return the XML nodes, attributes included
	 */
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

	/**
	 * Converts the items of a JSON array to XML elements with the given name. An empty array
	 * becomes a single empty element.
	 *
	 * @param jsonArray
	 *            the JSON array
	 * @param nodeName
	 *            the element name for the items
	 * @param xmlDocument
	 *            the XML document to create the nodes for
	 * @param useAttributes
	 *            true to convert simple properties of item objects to XML attributes
	 * @return the XML elements
	 */
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

	/**
	 * Reads JSON data (JSON5 syntax allowed) and validates it against a JSON schema using
	 * simple JSON schema rules.
	 *
	 * @param jsonDataInputStream
	 *            the JSON data
	 * @param jsonSchemaInputStream
	 *            the JSON schema definition
	 * @return the JSON data read
	 * @throws JsonSchemaDefinitionError
	 *             if the JSON schema is invalid
	 * @throws JsonSchemaDataValidationError
	 *             if the JSON data cannot be read or is not valid against the schema
	 */
	public static JsonNode parseJsonDataAndVerifyJsonSchemaSimple(final InputStream jsonDataInputStream, final InputStream jsonSchemaInputStream) throws JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		final JsonSchema jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration().withJsonSchemaVersion(JsonSchemaVersion.simple));
		return jsonSchema.validate(jsonDataInputStream);
	}

	/**
	 * Reads JSON data (JSON5 syntax allowed) and validates it against a JSON schema using
	 * JSON schema draft v4.
	 *
	 * @param jsonDataInputStream
	 *            the JSON data
	 * @param jsonSchemaInputStream
	 *            the JSON schema definition
	 * @return the JSON data read
	 * @throws JsonSchemaDefinitionError
	 *             if the JSON schema is invalid
	 * @throws JsonSchemaDataValidationError
	 *             if the JSON data cannot be read or is not valid against the schema
	 */
	public static JsonNode parseJsonDataAndVerifyJsonSchemaV4(final InputStream jsonDataInputStream, final InputStream jsonSchemaInputStream) throws JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		final JsonSchema jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration().withJsonSchemaVersion(JsonSchemaVersion.draftV4));
		return jsonSchema.validate(jsonDataInputStream);
	}

	/**
	 * Reads JSON data (JSON5 syntax allowed) and validates it against a JSON schema using
	 * JSON schema draft v6.
	 *
	 * @param jsonDataInputStream
	 *            the JSON data
	 * @param jsonSchemaInputStream
	 *            the JSON schema definition
	 * @return the JSON data read
	 * @throws JsonSchemaDefinitionError
	 *             if the JSON schema is invalid
	 * @throws JsonSchemaDataValidationError
	 *             if the JSON data cannot be read or is not valid against the schema
	 */
	public static JsonNode parseJsonDataAndVerifyJsonSchemaV6(final InputStream jsonDataInputStream, final InputStream jsonSchemaInputStream) throws JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		final JsonSchema jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration().withJsonSchemaVersion(JsonSchemaVersion.draftV6));
		return jsonSchema.validate(jsonDataInputStream);
	}

	/**
	 * Reads JSON data (JSON5 syntax allowed) and validates it against a JSON schema using
	 * JSON schema draft v7.
	 *
	 * @param jsonDataInputStream
	 *            the JSON data
	 * @param jsonSchemaInputStream
	 *            the JSON schema definition
	 * @return the JSON data read
	 * @throws JsonSchemaDefinitionError
	 *             if the JSON schema is invalid
	 * @throws JsonSchemaDataValidationError
	 *             if the JSON data cannot be read or is not valid against the schema
	 */
	public static JsonNode parseJsonDataAndVerifyJsonSchemaV7(final InputStream jsonDataInputStream, final InputStream jsonSchemaInputStream) throws JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		final JsonSchema jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration().withJsonSchemaVersion(JsonSchemaVersion.draftV7));
		return jsonSchema.validate(jsonDataInputStream);
	}

	/**
	 * Check for a valid JSON schema definition (simple JSON schema).
	 *
	 * @param jsonSchemaDataInputStream
	 *            JSON schema definition to check
	 * @param encoding
	 *            encoding of the JSON schema definition
	 * @return parsed JSON schema definition
	 * @throws IOException
	 *             if the data cannot be read
	 * @throws JsonSchemaDefinitionError
	 *             if the meta schema of the JSON schema version cannot be loaded
	 * @throws JsonSchemaDataValidationError
	 *             if the JSON schema definition is invalid for the JSON schema version
	 */
	public static JsonNode validateJsonSchemaSimple(final InputStream jsonSchemaDataInputStream, final Charset encoding) throws IOException, JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		return validateJsonSchema(jsonSchemaDataInputStream, encoding, JsonSchemaVersion.simple);
	}

	/**
	 * Check for a valid JSON schema definition (JSON schema draft v4).
	 *
	 * @param jsonSchemaDataInputStream
	 *            JSON schema definition to check
	 * @param encoding
	 *            encoding of the JSON schema definition
	 * @return parsed JSON schema definition
	 * @throws IOException
	 *             if the data cannot be read
	 * @throws JsonSchemaDefinitionError
	 *             if the meta schema of the JSON schema version cannot be loaded
	 * @throws JsonSchemaDataValidationError
	 *             if the JSON schema definition is invalid for the JSON schema version
	 */
	public static JsonNode validateJsonSchemaV4(final InputStream jsonSchemaDataInputStream, final Charset encoding) throws IOException, JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		return validateJsonSchema(jsonSchemaDataInputStream, encoding, JsonSchemaVersion.draftV4);
	}

	/**
	 * Check for a valid JSON schema definition (JSON schema draft v6).
	 *
	 * @param jsonSchemaDataInputStream
	 *            JSON schema definition to check
	 * @param encoding
	 *            encoding of the JSON schema definition
	 * @return parsed JSON schema definition
	 * @throws IOException
	 *             if the data cannot be read
	 * @throws JsonSchemaDefinitionError
	 *             if the meta schema of the JSON schema version cannot be loaded
	 * @throws JsonSchemaDataValidationError
	 *             if the JSON schema definition is invalid for the JSON schema version
	 */
	public static JsonNode validateJsonSchemaV6(final InputStream jsonSchemaDataInputStream, final Charset encoding) throws IOException, JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		return validateJsonSchema(jsonSchemaDataInputStream, encoding, JsonSchemaVersion.draftV6);
	}

	/**
	 * Check for a valid JSON schema definition (JSON schema draft v7).
	 *
	 * @param jsonSchemaDataInputStream
	 *            JSON schema definition to check
	 * @param encoding
	 *            encoding of the JSON schema definition
	 * @return parsed JSON schema definition
	 * @throws IOException
	 *             if the data cannot be read
	 * @throws JsonSchemaDefinitionError
	 *             if the meta schema of the JSON schema version cannot be loaded
	 * @throws JsonSchemaDataValidationError
	 *             if the JSON schema definition is invalid for the JSON schema version
	 */
	public static JsonNode validateJsonSchemaV7(final InputStream jsonSchemaDataInputStream, final Charset encoding) throws IOException, JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		return validateJsonSchema(jsonSchemaDataInputStream, encoding, JsonSchemaVersion.draftV7);
	}

	/**
	 * Check for a valid JSON schema definition against the meta schema of the given JSON schema version.
	 *
	 * @param jsonSchemaDataInputStream
	 *            JSON schema definition to check
	 * @param encoding
	 *            encoding of the JSON schema definition
	 * @param jsonSchemaVersion
	 *            JSON schema version whose meta schema is used for the check
	 * @return parsed JSON schema definition
	 * @throws IOException
	 *             if the data cannot be read
	 * @throws JsonSchemaDefinitionError
	 *             if the meta schema of the JSON schema version cannot be loaded
	 * @throws JsonSchemaDataValidationError
	 *             if the JSON schema definition is invalid for the JSON schema version
	 */
	public static JsonNode validateJsonSchema(final InputStream jsonSchemaDataInputStream, final Charset encoding, final JsonSchemaVersion jsonSchemaVersion) throws JsonSchemaDefinitionError, JsonSchemaDataValidationError, IOException {
		JsonSchema jsonSchema;
		try (InputStream jsonSchemaInputStream = JsonSchema.class.getClassLoader().getResourceAsStream(jsonSchemaVersion.getLocalFile());) {
			jsonSchema = new JsonSchema(jsonSchemaInputStream, new JsonSchemaConfiguration().withEncoding(encoding));
		}
		return jsonSchema.validate(jsonSchemaDataInputStream, encoding);
	}

	/**
	 * Checks whether data is valid standard JSON.
	 *
	 * @param jsonData
	 *            the data to check
	 * @param encoding
	 *            the encoding of the data, or null for UTF-8
	 * @return the JSON data read
	 * @throws Exception
	 *             if the data is no valid JSON
	 */
	public static JsonNode validateJson(final byte[] jsonData, final Charset encoding) throws Exception {
		try (JsonReader jsonReader = new JsonReader(new ByteArrayInputStream(jsonData), encoding)) {
			return jsonReader.read();
		}
	}

	/**
	 * Checks whether data is valid JSON5.
	 *
	 * @param jsonData
	 *            the data to check
	 * @param encoding
	 *            the encoding of the data, or null for UTF-8
	 * @return the JSON data read
	 * @throws Exception
	 *             if the data is no valid JSON5
	 */
	public static JsonNode validateJson5(final byte[] jsonData, final Charset encoding) throws Exception {
		try (JsonReader jsonReader = new Json5Reader(new ByteArrayInputStream(jsonData), encoding)) {
			return jsonReader.read();
		}
	}
}
