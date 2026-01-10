package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonToYamlConverter;
import de.soderer.json.utilities.IoUtilities;
import de.soderer.json.utilities.Linebreak;
import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlSequence;

public class YamlRoundTripTest {
	@Test
	public void testBasicStructureMapping() throws Exception {
		roundTripSingleDocument("yaml/mapping/input.yaml", "yaml/mapping/output.yaml", true);
	}

	@Test
	public void testBasicStructureSequence() throws Exception {
		roundTripSingleDocument("yaml/sequence/input.yaml", "yaml/sequence/output.yaml", true);
	}

	@Test
	public void testComments() throws Exception {
		roundTripSingleDocument("yaml/comments/input.yaml", "yaml/comments/output.yaml", true);
	}

	@Test
	public void testNumbers() throws Exception {
		final String inputDataFileNamem = "yaml/numbers/input.yaml";
		final String outputDataFileName = "yaml/numbers/output.yaml";

		final YamlDocument testDocument;
		try (InputStream testDataStream = getClass().getClassLoader().getResourceAsStream(inputDataFileNamem)) {
			try (final YamlReader yamlReader = new YamlReader(testDataStream)) {
				testDocument = yamlReader.readDocument();
			}
		}

		final YamlSequence yamlNode = (YamlSequence) testDocument.getRoot();
		for (final Object numerItemObject : yamlNode) {
			final YamlMapping numerItem = (YamlMapping) numerItemObject;
			final String description = (String) ((YamlScalar) numerItem.get("description")).getValue();
			if (description.toLowerCase().contains("sexagesimal")
					|| description.toLowerCase().contains("date")
					|| description.toLowerCase().contains("time")) {
				final String numberString = (String) ((YamlScalar) numerItem.get("number")).getValue();
				final String normalizedValue = (String) ((YamlScalar) numerItem.get("normalized")).getValue();
				Assert.assertEquals(description, numberString, normalizedValue);
			} else {
				final Number number = (Number) ((YamlScalar) numerItem.get("number")).getValue();
				final Number normalizedValue = (Number) ((YamlScalar) numerItem.get("normalized")).getValue();
				Assert.assertEquals(description, normalizedValue, number);
			}
		}

		final ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
		try (final YamlWriter writer = new YamlWriter(testOutputStream)) {
			writer.writeDocument(new YamlDocument(yamlNode));
		}

		final String serializedYaml = new String(testOutputStream.toByteArray(), StandardCharsets.UTF_8);

		String resultYamlFileString;
		try (InputStream resultDataStream = getClass().getClassLoader().getResourceAsStream(outputDataFileName)) {
			resultYamlFileString = IoUtilities.toString(resultDataStream, StandardCharsets.UTF_8);
		}

		Assert.assertEquals(resultYamlFileString, serializedYaml);
	}

	@Test
	public void testSimpleValues() throws Exception {
		roundTripSingleDocument("yaml/simpleValues/input.yaml", "yaml/simpleValues/output.yaml", false);
	}

	@Test
	public void testBig() throws Exception {
		roundTripSingleDocument("yaml/big/input.yaml", "yaml/big/output.yaml", false);
	}

	@Test
	public void testFlowStyle() throws Exception {
		roundTripSingleDocument("yaml/flow/input.yaml", "yaml/flow/output.yaml", true);
	}

	@Test
	public void testReference_1_1() throws Exception {
		roundTripSingleDocument("yaml/reference_1_1/input.yaml", "yaml/reference_1_1/output.yaml", true);
	}

	@Test
	public void testStandard() throws Exception {
		roundTripSingleDocument("yaml/standard/input.yaml", "yaml/standard/output.yaml", true);
	}

	@Test
	public void testMultiline() throws Exception {
		roundTripSingleDocument("yaml/multiline/input.yaml", "yaml/multiline/output.yaml", true);
	}

	@Test
	public void testTest() throws Exception {
		roundTripMultipleDocumentsInSingleFile("yaml/test/input.yaml", "yaml/test/output.yaml", false);
	}

	@Test
	public void testConverter() throws Exception {
		final String inputDataFileNamem = "yaml/converter/input.yaml";
		final String outputDataFileName = "yaml/converter/outputWindowsLines.yaml";

		String resultYamlFileString;
		try (InputStream resultDataStream = getClass().getClassLoader().getResourceAsStream(outputDataFileName)) {
			resultYamlFileString = IoUtilities.toString(resultDataStream, StandardCharsets.UTF_8);
		}

		final YamlDocument testDocument1;
		try (InputStream testDataStream = getClass().getClassLoader().getResourceAsStream(inputDataFileNamem)) {
			try (final YamlReader yamlReader = new YamlReader(testDataStream)) {
				testDocument1 = yamlReader.readDocument();
			}
		}

		final JsonNode jsonNode = YamlToJsonConverter.convert(testDocument1.getRoot());
		final YamlNode yamlNode = JsonToYamlConverter.convert(jsonNode);

		final ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
		try (final YamlWriter writer = new YamlWriter(testOutputStream)) {
			writer.setLinebreakType(Linebreak.Windows);
			writer.writeDocument(new YamlDocument(yamlNode));
		}

		final String serializedYaml = new String(testOutputStream.toByteArray(), StandardCharsets.UTF_8);

		if (!resultYamlFileString.contains("\r\n")) {
			resultYamlFileString = resultYamlFileString.replace("\n", "\r\n");
		}
		Assert.assertTrue(resultYamlFileString.contains("\r\n"));

		Assert.assertEquals(resultYamlFileString, serializedYaml);
	}

	private void roundTripSingleDocument(final String inputDataFileNamem, final String outputDataFileName, final boolean alwaysQuote) throws Exception {
		String resultYamlFileString;
		try (InputStream resultDataStream = getClass().getClassLoader().getResourceAsStream(outputDataFileName)) {
			resultYamlFileString = IoUtilities.toString(resultDataStream, StandardCharsets.UTF_8);
		}

		final YamlDocument testDocument1;
		try (InputStream testDataStream = getClass().getClassLoader().getResourceAsStream(inputDataFileNamem)) {
			try (final YamlReader yamlReader = new YamlReader(testDataStream)) {
				testDocument1 = yamlReader.readDocument();
			}
		}

		Assert.assertNotNull("Root node should not be null", testDocument1.getRoot());

		final ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
		try (final YamlWriter writer = new YamlWriter(testOutputStream)) {
			if (alwaysQuote) {
				writer.setAlwaysQuoteAllStrings();
			}
			writer.writeDocument(testDocument1);
		}

		final String serializedYaml = new String(testOutputStream.toByteArray(), StandardCharsets.UTF_8);
		Assert.assertFalse("Serialized YAML should not be empty", serializedYaml.isEmpty());
		Assert.assertEquals(resultYamlFileString, serializedYaml);

		final YamlDocument testDocument2;
		try (final YamlReader yamlReader = new YamlReader(new ByteArrayInputStream(serializedYaml.getBytes(StandardCharsets.UTF_8)))) {
			testDocument2 = yamlReader.readDocument();
		}

		Assert.assertNotNull("Round-trip root should not be null", testDocument2.getRoot());

		Assert.assertTrue( "AST should be equal after round trip", astEquals(testDocument1.getRoot(), testDocument2.getRoot()));
	}

	private void roundTripMultipleDocumentsInSingleFile(final String inputDataFileNamem, final String outputDataFileName, final boolean alwaysQuote) throws Exception {
		String resultYamlFileString;
		try (InputStream resultDataStream = getClass().getClassLoader().getResourceAsStream(outputDataFileName)) {
			resultYamlFileString = IoUtilities.toString(resultDataStream, StandardCharsets.UTF_8);
		}

		final ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
		try (final YamlWriter writer = new YamlWriter(testOutputStream)) {
			if (alwaysQuote) {
				writer.setAlwaysQuoteAllStrings();
			}

			try (InputStream testDataStream = getClass().getClassLoader().getResourceAsStream(inputDataFileNamem)) {
				try (final YamlReader yamlReader = new YamlReader(testDataStream)) {
					YamlDocument testDocument;
					while ((testDocument = yamlReader.readDocument()) != null) {
						writer.writeDocument(testDocument);
					}
				}
			}
		}

		final String serializedYaml = new String(testOutputStream.toByteArray(), StandardCharsets.UTF_8);
		Assert.assertFalse("Serialized YAML should not be empty", serializedYaml.isEmpty());
		Assert.assertEquals(resultYamlFileString, serializedYaml);
	}

	private boolean astEquals(final YamlNode a, final YamlNode b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (!a.getClass().equals(b.getClass())) {
			return false;
		}

		if ((a.getLeadingComments() == null && b.getLeadingComments() != null)
				|| (a.getLeadingComments() != null && !a.getLeadingComments().equals(b.getLeadingComments()))) {
			return false;
		}

		if (!safeEquals(a.getAnchorName(), b.getAnchorName())) {
			return false;
		}

		if (a instanceof final YamlScalar sa && b instanceof final YamlScalar sb) {
			if (sa.getType() == sb.getType() && safeEquals(sa.getValue(), sb.getValue())) {
				return true;
			} else {
				return false;
			}
		}

		if (a instanceof final YamlAlias aa && b instanceof final YamlAlias ab) {
			if (safeEquals(aa.getTargetAnchorName(), ab.getTargetAnchorName())) {
				return true;
			} else {
				return false;
			}
		}

		if (a instanceof final YamlSequence seqA && b instanceof final YamlSequence seqB) {
			if (seqA.isFlowStyle() != seqB.isFlowStyle()) {
				return false;
			}
			if (seqA.size() != seqB.size()) {
				return false;
			}

			for (int i = 0; i < seqA.size(); i++) {
				if (!astEquals(seqA.get(i), seqB.get(i))) {
					return false;
				}
			}
			return true;
		}

		if (a instanceof final YamlMapping mapA && b instanceof final YamlMapping mapB) {
			if (mapA.isFlowStyle() != mapB.isFlowStyle()) {
				return false;
			}
			if (mapA.size() != mapB.size()) {
				return false;
			}

			final Iterator<Entry<YamlNode, YamlNode>> mapA_Iterator = mapA.entrySet().iterator();
			final Iterator<Entry<YamlNode, YamlNode>> mapB_Iterator = mapB.entrySet().iterator();

			for (int i = 0; i < mapA.size(); i++) {
				final Entry<YamlNode, YamlNode> kvAEntry = mapA_Iterator.next();
				final Entry<YamlNode, YamlNode> kvBEntry = mapB_Iterator.next();

				if (!astEquals(kvAEntry.getKey(), kvAEntry.getKey())) {
					return false;
				}
				if (!astEquals(kvBEntry.getValue(), kvBEntry.getValue())) {
					return false;
				}
			}
			return true;
		}

		return false;
	}

	private static boolean safeEquals(final Object a, final Object b) {
		return (a == null && b == null) || (a != null && a.equals(b));
	}
}
