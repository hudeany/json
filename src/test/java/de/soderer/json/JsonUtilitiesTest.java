package de.soderer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaVersion;

@SuppressWarnings("static-method")
class JsonUtilitiesTest {
	private static InputStream stream(final String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
	}

	private static Document parseXml(final String xml) throws Exception {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream(xml));
	}

	@Test
	void convertXmlDocument_attributesTextAndChildren() throws Exception {
		final Document document = parseXml("<?xml version=\"1.0\"?><!-- leading comment --><root id=\"1\">some text<name>Alice</name><!-- ignored --><address city=\"Munich\"><street>Main</street><zip>80331</zip></address><empty/></root>");
		final JsonObject json = JsonUtilities.convertXmlDocument(document, true);

		final JsonObject root = (JsonObject) json.get("root");
		assertEquals("1", root.getSimpleValue("id"));
		assertEquals("some text", root.getSimpleValue("text"));
		assertEquals("Alice", root.getSimpleValue("name"));
		final JsonObject address = (JsonObject) root.get("address");
		assertEquals("Munich", address.getSimpleValue("city"));
		assertEquals("Main", address.getSimpleValue("street"));
		assertEquals("80331", address.getSimpleValue("zip"));
		assertTrue(((JsonObject) root.get("empty")).isEmpty());
	}

	@Test
	void convertXmlDocument_duplicateChildElements_errorHandling() throws Exception {
		final Document document = parseXml("<root><item>1</item><item>2</item></root>");
		assertNull(JsonUtilities.convertXmlDocument(document, false));
		final Exception exception = assertThrows(Exception.class, () -> JsonUtilities.convertXmlDocument(document, true));
		assertEquals("Invalid data", exception.getMessage());
	}

	@Test
	void convertToXmlDocument_objectWithSingleRootProperty() throws Exception {
		final JsonObject inner = new JsonObject()
				.add("id", Integer.valueOf(7))
				.add("name", "Bob")
				.add("child", new JsonObject().add("flag", Boolean.TRUE))
				.add("list", new JsonArray().add("a").add(new JsonObject().add("x", "1")).add(new JsonArray().add("nested")))
				.add("emptyList", new JsonArray());
		final JsonObject json = new JsonObject().add("person", inner);

		final Document elementsDocument = JsonUtilities.convertToXmlDocument(json, false);
		final Element personElement = elementsDocument.getDocumentElement();
		assertEquals("person", personElement.getNodeName());
		assertEquals("Bob", personElement.getElementsByTagName("name").item(0).getTextContent());
		assertEquals(4, personElement.getElementsByTagName("list").getLength());
		assertEquals(1, personElement.getElementsByTagName("emptyList").getLength());

		final Document attributesDocument = JsonUtilities.convertToXmlDocument(json, true);
		final Element personWithAttributes = attributesDocument.getDocumentElement();
		assertEquals("Bob", personWithAttributes.getAttribute("name"));
		assertEquals("7", personWithAttributes.getAttribute("id"));
		assertEquals("true", ((Element) personWithAttributes.getElementsByTagName("child").item(0)).getAttribute("flag"));
	}

	@Test
	void convertToXmlDocument_objectWithMultipleProperties_createsRootElement() throws Exception {
		final JsonObject json = new JsonObject().add("a", "1").add("b", "2");
		final Document withAttributes = JsonUtilities.convertToXmlDocument(json, true);
		assertEquals("root", withAttributes.getDocumentElement().getNodeName());
		assertEquals("1", withAttributes.getDocumentElement().getAttribute("a"));

		final Document withElements = JsonUtilities.convertToXmlDocument(json, false);
		assertEquals("2", withElements.getDocumentElement().getElementsByTagName("b").item(0).getTextContent());

		assertThrows(Exception.class, () -> JsonUtilities.convertToXmlDocument(new JsonObject(), false));
	}

	@Test
	void convertToXmlDocument_arraysAndSimpleValues() throws Exception {
		final Document singleItem = JsonUtilities.convertToXmlDocument(new JsonArray().add("only"), false);
		assertEquals("root", singleItem.getDocumentElement().getNodeName());
		assertEquals("only", singleItem.getDocumentElement().getTextContent());

		final Document emptyArray = JsonUtilities.convertToXmlDocument(new JsonArray(), false);
		assertEquals("root", emptyArray.getDocumentElement().getNodeName());

		final Document multipleItems = JsonUtilities.convertToXmlDocument(new JsonArray().add("a").add("b"), false);
		assertEquals("root", multipleItems.getDocumentElement().getNodeName());
		assertEquals(2, multipleItems.getDocumentElement().getElementsByTagName("root").getLength());

		assertEquals("text", JsonUtilities.convertToXmlDocument(new JsonValueString("text"), false).getDocumentElement().getTextContent());
		assertEquals("5", JsonUtilities.convertToXmlDocument(new JsonValueInteger(5), false).getDocumentElement().getTextContent());
		assertEquals("1.5", JsonUtilities.convertToXmlDocument(new JsonValueNumber(new BigDecimal("1.5")), false).getDocumentElement().getTextContent());
		assertEquals("true", JsonUtilities.convertToXmlDocument(new JsonValueBoolean(Boolean.TRUE), false).getDocumentElement().getTextContent());
		assertEquals("null", JsonUtilities.convertToXmlDocument(new JsonValueNull(), false).getDocumentElement().getTextContent());
	}

	@Test
	void parseJsonDataAndVerifyJsonSchema_allVersions() throws Exception {
		final String schema = "{ \"type\": \"object\", \"properties\": { \"a\": { \"type\": \"integer\" } } }";
		final String validData = "{ \"a\": 1 }";
		final String invalidData = "{ \"a\": \"x\" }";

		assertTrue(JsonUtilities.parseJsonDataAndVerifyJsonSchemaSimple(stream(validData), stream(schema)).isJsonObject());
		assertTrue(JsonUtilities.parseJsonDataAndVerifyJsonSchemaV4(stream(validData), stream(schema)).isJsonObject());
		assertTrue(JsonUtilities.parseJsonDataAndVerifyJsonSchemaV6(stream(validData), stream(schema)).isJsonObject());
		assertTrue(JsonUtilities.parseJsonDataAndVerifyJsonSchemaV7(stream(validData), stream(schema)).isJsonObject());

		assertThrows(JsonSchemaDataValidationError.class, () -> JsonUtilities.parseJsonDataAndVerifyJsonSchemaSimple(stream(invalidData), stream(schema)));
		assertThrows(JsonSchemaDataValidationError.class, () -> JsonUtilities.parseJsonDataAndVerifyJsonSchemaV4(stream(invalidData), stream(schema)));
		assertThrows(JsonSchemaDataValidationError.class, () -> JsonUtilities.parseJsonDataAndVerifyJsonSchemaV6(stream(invalidData), stream(schema)));
		assertThrows(JsonSchemaDataValidationError.class, () -> JsonUtilities.parseJsonDataAndVerifyJsonSchemaV7(stream(invalidData), stream(schema)));
	}

	@Test
	void validateJsonSchema_againstMetaSchemas() throws Exception {
		final String schema = "{ \"type\": \"object\", \"properties\": { \"a\": { \"type\": \"integer\", \"minimum\": 0 } }, \"required\": [\"a\"] }";
		assertTrue(JsonUtilities.validateJsonSchemaV4(stream(schema), StandardCharsets.UTF_8).isJsonObject());
		assertTrue(JsonUtilities.validateJsonSchemaV6(stream(schema), StandardCharsets.UTF_8).isJsonObject());
		assertTrue(JsonUtilities.validateJsonSchemaV7(stream(schema), StandardCharsets.UTF_8).isJsonObject());
		// Draft v3 defines "required" as boolean property attribute instead of an array
		final String schemaV3 = "{ \"type\": \"object\", \"properties\": { \"a\": { \"type\": \"integer\", \"required\": true } } }";
		assertTrue(JsonUtilities.validateJsonSchema(stream(schemaV3), StandardCharsets.UTF_8, JsonSchemaVersion.draftV3).isJsonObject());
		assertThrows(JsonSchemaDataValidationError.class, () -> JsonUtilities.validateJsonSchema(stream(schema), StandardCharsets.UTF_8, JsonSchemaVersion.draftV3));

		final String invalidSchema = "{ \"type\": 123 }";
		assertThrows(JsonSchemaDataValidationError.class, () -> JsonUtilities.validateJsonSchemaV7(stream(invalidSchema), StandardCharsets.UTF_8));
	}

	@Test
	void validateJsonSchemaSimple_checksDefinitionInSimpleMode() throws Exception {
		// There is no meta schema for simple JSON schemas, the definition is checked by creating a schema in simple mode
		assertTrue(JsonUtilities.validateJsonSchemaSimple(stream("{\"type\": \"object\", \"properties\": {\"a\": {\"type\": \"integer\"}}}"), StandardCharsets.UTF_8).isJsonObject());
		assertTrue(JsonUtilities.validateJsonSchemaSimple(stream("true"), null).isBoolean());
		assertTrue(JsonUtilities.validateJsonSchema(stream("{}"), StandardCharsets.UTF_8, null).isJsonObject());
		// Unknown keywords are not allowed in simple mode
		assertThrows(JsonSchemaDataValidationError.class, () -> JsonUtilities.validateJsonSchemaSimple(stream("{\"unknownKeyword\": 1}"), StandardCharsets.UTF_8));
		assertThrows(JsonSchemaDataValidationError.class, () -> JsonUtilities.validateJsonSchemaSimple(stream("{\"type\": 5}"), StandardCharsets.UTF_8));
		assertThrows(JsonSchemaDataValidationError.class, () -> JsonUtilities.validateJsonSchemaSimple(stream("[1]"), StandardCharsets.UTF_8));
		assertThrows(JsonSchemaDataValidationError.class, () -> JsonUtilities.validateJsonSchemaSimple(stream("{ broken"), StandardCharsets.UTF_8));
	}

	@Test
	void validateJsonAndJson5() throws Exception {
		assertTrue(JsonUtilities.validateJson("{\"a\": [1, 2]}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8).isJsonObject());
		assertThrows(Exception.class, () -> JsonUtilities.validateJson("{a: 1}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

		assertTrue(JsonUtilities.validateJson5("{a: 1, // comment\n b: 'x',}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8).isJsonObject());
		assertThrows(Exception.class, () -> JsonUtilities.validateJson5("{a: ".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
	}
}
