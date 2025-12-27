package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.json.utilities.IoUtilities;
import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlKeyValue;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlSequence;

public class YamlRoundTripTestWithYamlReader {
	@Test
	public void testStandard() throws Exception {
		roundTrip("yaml/standard/sample.yaml", "yaml/standard/result.yaml", true);
	}

	@Test
	public void testBig() throws Exception {
		roundTrip("yaml/big/sample.yaml", "yaml/big/result.yaml", false);
	}

	//	@Test
	//	public void testTest() throws Exception {
	//		roundTrip("yaml/test/sample.yaml", "yaml/test/result.yaml", false);
	//	}

	@Test
	public void testMultipleDocuments() throws Exception {
		roundTripMultipleDocuments("yaml/multiple/sample.yaml", "yaml/multiple/result.yaml");
	}

	private void roundTrip(final String inputDataFileNamem, final String outputDataFileName, final boolean alwaysQuote) throws Exception {
		String resultYamlFileString;
		try (InputStream resultDataStream = getClass().getClassLoader().getResourceAsStream(outputDataFileName)) {
			resultYamlFileString = IoUtilities.toString(resultDataStream, StandardCharsets.UTF_8);
		}

		final YamlDocument testDocument1;
		try (InputStream testDataStream = getClass().getClassLoader().getResourceAsStream(inputDataFileNamem)) {
			try (final YamlReader yamlReader = new YamlReader(testDataStream)) {
				testDocument1 = yamlReader.readYamlDocument();
			}
		}

		Assert.assertNotNull("Root node should not be null", testDocument1.getRoot());

		final ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
		try (final YamlWriter writer = new YamlWriter(testOutputStream)) {
			writer.setAlwaysQuoteStringValues(alwaysQuote);
			writer.writeDocument(testDocument1);
		}

		final String serializedYaml = new String(testOutputStream.toByteArray(), StandardCharsets.UTF_8);
		Assert.assertFalse("Serialized YAML should not be empty", serializedYaml.isEmpty());
		Assert.assertEquals(resultYamlFileString, serializedYaml);

		final YamlDocument testDocument2;
		try (final YamlReader yamlReader = new YamlReader(new ByteArrayInputStream(serializedYaml.getBytes(StandardCharsets.UTF_8)))) {
			testDocument2 = yamlReader.readYamlDocument();
		}

		Assert.assertNotNull("Round-trip root should not be null", testDocument2.getRoot());

		Assert.assertTrue( "AST should be equal after round trip", astEquals(testDocument1.getRoot(), testDocument2.getRoot()));
	}

	private void roundTripMultipleDocuments(final String inputDataFileNamem, final String outputDataFileName) throws Exception {
		String resultYamlFileString;
		try (InputStream resultDataStream = getClass().getClassLoader().getResourceAsStream(outputDataFileName)) {
			resultYamlFileString = IoUtilities.toString(resultDataStream, StandardCharsets.UTF_8);
		}

		final List<YamlDocument> yamlDocumentList1;
		try (InputStream testDataStream = getClass().getClassLoader().getResourceAsStream(inputDataFileNamem)) {
			try (final YamlReader yamlReader = new YamlReader(testDataStream)) {
				try {
					yamlReader.readYamlDocument();
					Assert.fail("Missing expected expection for multiple documents");
				} catch (@SuppressWarnings("unused") final Exception e) {
					// Expected expection
				}
				yamlDocumentList1 = yamlReader.readYamlDocumentList();
			}
		}

		Assert.assertEquals(2, yamlDocumentList1.size());
		Assert.assertNotNull("Root node of document 1 should not be null", yamlDocumentList1.get(0).getRoot());
		Assert.assertNotNull("Root node of document 2 should not be null", yamlDocumentList1.get(1).getRoot());

		final ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
		try (final YamlWriter writer = new YamlWriter(testOutputStream)) {
			writer.writeDocument(yamlDocumentList1.get(0));
			writer.writeDocument(yamlDocumentList1.get(1));
		}

		final String serializedYaml = new String(testOutputStream.toByteArray(), StandardCharsets.UTF_8);
		Assert.assertFalse("Serialized YAML should not be empty", serializedYaml.isEmpty());
		Assert.assertEquals(resultYamlFileString, serializedYaml);

		final List<YamlDocument> yamlDocumentList2;
		try (final YamlReader yamlReader = new YamlReader(new ByteArrayInputStream(serializedYaml.getBytes(StandardCharsets.UTF_8)))) {
			yamlDocumentList2 = yamlReader.readYamlDocumentList();
		}

		Assert.assertNotNull("Root node of document 1 should not be null", yamlDocumentList2.get(0).getRoot());
		Assert.assertNotNull("Root node of document 2 should not be null", yamlDocumentList2.get(1).getRoot());

		Assert.assertTrue( "AST should be equal after round trip", astEquals(yamlDocumentList1.get(0).getRoot(), yamlDocumentList2.get(0).getRoot()));
		Assert.assertTrue( "AST should be equal after round trip", astEquals(yamlDocumentList1.get(1).getRoot(), yamlDocumentList2.get(1).getRoot()));
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

		if (!a.getLeadingComments().equals(b.getLeadingComments())) {
			return false;
		}

		if (!safeEquals(a.getAnchorName(), b.getAnchorName())) {
			return false;
		}

		if (a instanceof final YamlScalar sa && b instanceof final YamlScalar sb) {
			return sa.getType() == sb.getType() && safeEquals(sa.getValue(), sb.getValue());
		}

		if (a instanceof final YamlAlias aa && b instanceof final YamlAlias ab) {
			return safeEquals(aa.getTargetAnchorName(), ab.getTargetAnchorName());
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
			if (mapA.getEntries().size() != mapB.getEntries().size()) {
				return false;
			}

			for (int i = 0; i < mapA.getEntries().size(); i++) {
				final YamlKeyValue kvA = mapA.getEntries().get(i);
				final YamlKeyValue kvB = mapB.getEntries().get(i);

				if (!astEquals(kvA.getKey(), kvB.getKey())) {
					return false;
				}
				if (!astEquals(kvA.getValue(), kvB.getValue())) {
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
