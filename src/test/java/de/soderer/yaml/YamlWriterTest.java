package de.soderer.yaml;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@SuppressWarnings("static-method")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class YamlWriterTest {
	@Test
	public void testYamlMapping() {
		try {
			final YamlMapping testYamlValue = new YamlMapping();
			testYamlValue.put(new YamlSimpleValue().setValue("property 1"), new YamlSimpleValue().setValue(null));
			testYamlValue.put(new YamlSimpleValue().setValue("property 2"), new YamlSimpleValue().setValue(true));
			testYamlValue.put(new YamlSimpleValue().setValue("property 3"), new YamlSimpleValue().setValue(1));
			testYamlValue.put(new YamlSimpleValue().setValue("property 4"), new YamlSimpleValue().setValue("String value 4"));

			final YamlSequence otherYamlSequence = new YamlSequence();
			otherYamlSequence.add(new YamlSimpleValue().setValue(null));
			otherYamlSequence.add(new YamlSimpleValue().setValue(true));
			otherYamlSequence.add(new YamlSimpleValue().setValue(23));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 24"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 25"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 26"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 27"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 28"));
			testYamlValue.put("property 5", otherYamlSequence);

			final YamlMapping otherYamlMapping = new YamlMapping();
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 31"), new YamlSimpleValue().setValue(null));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 32"), new YamlSimpleValue().setValue(true));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 33"), new YamlSimpleValue().setValue(33));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 34"), new YamlSimpleValue().setValue("String value 34"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 35"), new YamlSimpleValue().setValue("String value 35"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 36"), new YamlSimpleValue().setValue("String value 36"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 37"), new YamlSimpleValue().setValue("String value 37"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 38"), new YamlSimpleValue().setValue("String value 38"));
			testYamlValue.put("property 6", otherYamlMapping);

			testYamlValue.put(new YamlSimpleValue().setValue("property 7"), new YamlSimpleValue().setValue(null));

			testYamlValue.put("property 8", new YamlSequence());
			testYamlValue.put("property 9", new YamlMapping());

			final String expectedString = ""
					+ "property 1:\n"
					+ "property 2: true\n"
					+ "property 3: 1\n"
					+ "property 4: String value 4\n"
					+ "property 5:\n"
					+ "  - null\n"
					+ "  - true\n"
					+ "  - 23\n"
					+ "  - String value 24\n"
					+ "  - String value 25\n"
					+ "  - String value 26\n"
					+ "  - String value 27\n"
					+ "  - String value 28\n"
					+ "property 6:\n"
					+ "  property 31:\n"
					+ "  property 32: true\n"
					+ "  property 33: 33\n"
					+ "  property 34: String value 34\n"
					+ "  property 35: String value 35\n"
					+ "  property 36: String value 36\n"
					+ "  property 37: String value 37\n"
					+ "  property 38: String value 38\n"
					+ "property 7:\n"
					+ "property 8:\n"
					+ "  []\n"
					+ "property 9:\n"
					+ "  {}\n";

			testYamlObject(expectedString, testYamlValue);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testYamlMappingWithAnchorsAndComments() {
		try {
			final YamlMapping testYamlValue = new YamlMapping().setAnchor("Anchor0").setComment("Comment 0\nMultiline comment 0").setInlineComment("Inline comment 0");
			testYamlValue.put(new YamlSimpleValue().setValue("property 1"), new YamlSimpleValue().setValue(null).setAnchor("Anchor1").setComment("Comment 1\nMultiline comment 1").setInlineComment("Inline comment 1"));
			testYamlValue.put(new YamlSimpleValue().setValue("property 2"), new YamlSimpleValue().setValue(true).setAnchor("Anchor2").setComment("Comment 2\nMultiline comment 2").setInlineComment("Inline comment 2"));
			testYamlValue.put(new YamlSimpleValue().setValue("property 3"), new YamlSimpleValue().setValue(1).setAnchor("Anchor3").setComment("Comment 3\nMultiline comment 3").setInlineComment("Inline comment 3"));
			testYamlValue.put(new YamlSimpleValue().setValue("property 4"), new YamlSimpleValue().setValue("String value 4").setAnchor("Anchor4").setComment("Comment 4\nMultiline comment 4").setInlineComment("Inline comment 4"));

			final YamlSequence otherYamlSequence = new YamlSequence().setAnchor("Anchor5").setComment("Comment 5\nMultiline comment 5").setInlineComment("Inline comment 5");
			otherYamlSequence.add(new YamlSimpleValue().setValue(null).setAnchor("Anchor21").setComment("Comment 21\nMultiline comment 21").setInlineComment("Inline comment 21"));
			otherYamlSequence.add(new YamlSimpleValue().setValue(true).setAnchor("Anchor22").setComment("Comment 22\nMultiline comment 22").setInlineComment("Inline comment 22"));
			otherYamlSequence.add(new YamlSimpleValue().setValue(23).setAnchor("Anchor23").setComment("Comment 23\nMultiline comment 23").setInlineComment("Inline comment 23"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 24").setAnchor("Anchor24").setComment("Comment 24\nMultiline comment 24").setInlineComment("Inline comment 24"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 25").setAnchor("Anchor25").setComment("Comment 25\nMultiline comment 25").setInlineComment("Inline comment 25"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 26").setAnchor("Anchor26").setComment("Comment 26\nMultiline comment 26").setInlineComment("Inline comment 26"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 27").setAnchor("Anchor27").setComment("Comment 27\nMultiline comment 27").setInlineComment("Inline comment 27"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 28").setAnchor("Anchor28").setComment("Comment 28\nMultiline comment 28").setInlineComment("Inline comment 28"));
			testYamlValue.put("property 5", otherYamlSequence);

			final YamlMapping otherYamlMapping = new YamlMapping().setAnchor("Anchor6").setComment("Comment 6\nMultiline comment 6").setInlineComment("Inline comment 6");
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 31"), new YamlSimpleValue().setValue(null).setAnchor("Anchor31").setComment("Comment 31\nMultiline comment 31").setInlineComment("Inline comment 31"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 32"), new YamlSimpleValue().setValue(true).setAnchor("Anchor32").setComment("Comment 32\nMultiline comment 32").setInlineComment("Inline comment 32"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 33"), new YamlSimpleValue().setValue(33).setAnchor("Anchor33").setComment("Comment 33\nMultiline comment 33").setInlineComment("Inline comment 33"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 34"), new YamlSimpleValue().setValue("String value 34").setAnchor("Anchor34").setComment("Comment 34\nMultiline comment 34").setInlineComment("Inline comment 34"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 35"), new YamlSimpleValue().setValue("String value 35").setAnchor("Anchor35").setComment("Comment 35\nMultiline comment 35").setInlineComment("Inline comment 35"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 36"), new YamlSimpleValue().setValue("String value 36").setAnchor("Anchor36").setComment("Comment 36\nMultiline comment 36").setInlineComment("Inline comment 36"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 37"), new YamlSimpleValue().setValue("String value 37").setAnchor("Anchor37").setComment("Comment 37\nMultiline comment 37").setInlineComment("Inline comment 37"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 38"), new YamlSimpleValue().setValue("String value 38").setAnchor("Anchor38").setComment("Comment 38\nMultiline comment 38").setInlineComment("Inline comment 38"));
			testYamlValue.put("property 6", otherYamlMapping);

			testYamlValue.put(new YamlSimpleValue().setValue("property 7"), new YamlSimpleValue().setValue(null).setAnchor("Anchor7").setComment("Comment 7\nMultiline comment 7").setInlineComment("Inline comment 7"));

			testYamlValue.put("property 8", new YamlSequence().setAnchor("Anchor8").setComment("Comment 8\nMultiline comment 8").setInlineComment("Inline comment 8"));
			testYamlValue.put("property 9", new YamlMapping().setAnchor("Anchor9").setComment("Comment 9\nMultiline comment 9").setInlineComment("Inline comment 9"));

			final String expectedString = ""
					+ "# Comment 0\n"
					+ "# Multiline comment 0\n"
					+ "# Inline comment 0\n"
					+ "# Comment 1\n"
					+ "# Multiline comment 1\n"
					+ "property 1: &Anchor1 # Inline comment 1\n"
					+ "# Comment 2\n"
					+ "# Multiline comment 2\n"
					+ "property 2: &Anchor2 true # Inline comment 2\n"
					+ "# Comment 3\n"
					+ "# Multiline comment 3\n"
					+ "property 3: &Anchor3 1 # Inline comment 3\n"
					+ "# Comment 4\n"
					+ "# Multiline comment 4\n"
					+ "property 4: &Anchor4 String value 4 # Inline comment 4\n"
					+ "property 5: &Anchor5 # Inline comment 5\n"
					+ "  # Comment 5\n"
					+ "  # Multiline comment 5\n"
					+ "  # Comment 21\n"
					+ "  # Multiline comment 21\n"
					+ "  - null &Anchor21 # Inline comment 21\n"
					+ "  # Comment 22\n"
					+ "  # Multiline comment 22\n"
					+ "  - true &Anchor22 # Inline comment 22\n"
					+ "  # Comment 23\n"
					+ "  # Multiline comment 23\n"
					+ "  - 23 &Anchor23 # Inline comment 23\n"
					+ "  # Comment 24\n"
					+ "  # Multiline comment 24\n"
					+ "  - String value 24 &Anchor24 # Inline comment 24\n"
					+ "  # Comment 25\n"
					+ "  # Multiline comment 25\n"
					+ "  - String value 25 &Anchor25 # Inline comment 25\n"
					+ "  # Comment 26\n"
					+ "  # Multiline comment 26\n"
					+ "  - String value 26 &Anchor26 # Inline comment 26\n"
					+ "  # Comment 27\n"
					+ "  # Multiline comment 27\n"
					+ "  - String value 27 &Anchor27 # Inline comment 27\n"
					+ "  # Comment 28\n"
					+ "  # Multiline comment 28\n"
					+ "  - String value 28 &Anchor28 # Inline comment 28\n"
					+ "property 6: &Anchor6 # Inline comment 6\n"
					+ "  # Comment 6\n"
					+ "  # Multiline comment 6\n"
					+ "  # Comment 31\n"
					+ "  # Multiline comment 31\n"
					+ "  property 31: &Anchor31 # Inline comment 31\n"
					+ "  # Comment 32\n"
					+ "  # Multiline comment 32\n"
					+ "  property 32: &Anchor32 true # Inline comment 32\n"
					+ "  # Comment 33\n"
					+ "  # Multiline comment 33\n"
					+ "  property 33: &Anchor33 33 # Inline comment 33\n"
					+ "  # Comment 34\n"
					+ "  # Multiline comment 34\n"
					+ "  property 34: &Anchor34 String value 34 # Inline comment 34\n"
					+ "  # Comment 35\n"
					+ "  # Multiline comment 35\n"
					+ "  property 35: &Anchor35 String value 35 # Inline comment 35\n"
					+ "  # Comment 36\n"
					+ "  # Multiline comment 36\n"
					+ "  property 36: &Anchor36 String value 36 # Inline comment 36\n"
					+ "  # Comment 37\n"
					+ "  # Multiline comment 37\n"
					+ "  property 37: &Anchor37 String value 37 # Inline comment 37\n"
					+ "  # Comment 38\n"
					+ "  # Multiline comment 38\n"
					+ "  property 38: &Anchor38 String value 38 # Inline comment 38\n"
					+ "# Comment 7\n"
					+ "# Multiline comment 7\n"
					+ "property 7: &Anchor7 # Inline comment 7\n"
					+ "property 8: &Anchor8 # Inline comment 8\n"
					+ "  # Comment 8\n"
					+ "  # Multiline comment 8\n"
					+ "  []\n"
					+ "property 9: &Anchor9 # Inline comment 9\n"
					+ "  # Comment 9\n"
					+ "  # Multiline comment 9\n"
					+ "  {}\n";

			testYamlObject(expectedString, testYamlValue);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testYamlMappingFlow() {
		try {
			final YamlMapping testYamlValue = new YamlMapping().setStyle(YamlStyle.Flow);
			testYamlValue.put(new YamlSimpleValue().setValue("property 1"), new YamlSimpleValue().setValue(null));
			testYamlValue.put(new YamlSimpleValue().setValue("property 2"), new YamlSimpleValue().setValue(true));
			testYamlValue.put(new YamlSimpleValue().setValue("property 3"), new YamlSimpleValue().setValue(1));
			testYamlValue.put(new YamlSimpleValue().setValue("property 4"), new YamlSimpleValue().setValue("String value 4"));

			final YamlSequence otherYamlSequence = new YamlSequence().setStyle(YamlStyle.Flow);
			otherYamlSequence.add(new YamlSimpleValue().setValue(null));
			otherYamlSequence.add(new YamlSimpleValue().setValue(true));
			otherYamlSequence.add(new YamlSimpleValue().setValue(23));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 24"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 25"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 26"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 27"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 28"));
			testYamlValue.put("property 5", otherYamlSequence);

			final YamlMapping otherYamlMapping = new YamlMapping().setStyle(YamlStyle.Flow);
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 31"), new YamlSimpleValue().setValue(null));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 32"), new YamlSimpleValue().setValue(true));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 33"), new YamlSimpleValue().setValue(33));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 34"), new YamlSimpleValue().setValue("String value 34"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 35"), new YamlSimpleValue().setValue("String value 35"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 36"), new YamlSimpleValue().setValue("String value 36"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 37"), new YamlSimpleValue().setValue("String value 37"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 38"), new YamlSimpleValue().setValue("String value 38"));
			testYamlValue.put("property 6", otherYamlMapping);

			testYamlValue.put(new YamlSimpleValue().setValue("property 7"), new YamlSimpleValue().setValue(null));

			testYamlValue.put("property 8", new YamlSequence().setStyle(YamlStyle.Flow));
			testYamlValue.put("property 9", new YamlMapping().setStyle(YamlStyle.Flow));

			final String expectedString = ""
					+ "{property 1: null, property 2: true, property 3: 1, property 4: String value 4, "
					+ "property 5: [null, true, 23, String value 24, String value 25, String value 26, String value 27, String value 28], "
					+ "property 6: {property 31: null, property 32: true, property 33: 33, property 34: String value 34, property 35: String value 35, property 36: String value 36, property 37: String value 37, property 38: String value 38}, "
					+ "property 7: null, property 8: [], property 9: {}}";

			testYamlObject(expectedString, testYamlValue);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testYamlMappingFlowWithAnchorsAndComments() {
		try {
			final YamlMapping testYamlValue = new YamlMapping().setStyle(YamlStyle.Flow).setAnchor("Anchor0").setComment("Comment 0\nMultiline comment 0").setInlineComment("Inline comment 0");
			testYamlValue.put(new YamlSimpleValue().setValue("property 1"), new YamlSimpleValue().setValue(null).setAnchor("Anchor1").setComment("Comment 1\nMultiline comment 1").setInlineComment("Inline comment 1"));
			testYamlValue.put(new YamlSimpleValue().setValue("property 2"), new YamlSimpleValue().setValue(true).setAnchor("Anchor2").setComment("Comment 2\nMultiline comment 2").setInlineComment("Inline comment 2"));
			testYamlValue.put(new YamlSimpleValue().setValue("property 3"), new YamlSimpleValue().setValue(1).setAnchor("Anchor3").setComment("Comment 3\nMultiline comment 3").setInlineComment("Inline comment 3"));
			testYamlValue.put(new YamlSimpleValue().setValue("property 4"), new YamlSimpleValue().setValue("String value 4").setAnchor("Anchor4").setComment("Comment 4\nMultiline comment 4").setInlineComment("Inline comment 4"));

			final YamlSequence otherYamlSequence = new YamlSequence().setStyle(YamlStyle.Flow).setAnchor("Anchor5").setComment("Comment 5\nMultiline comment 5").setInlineComment("Inline comment 5");
			otherYamlSequence.add(new YamlSimpleValue().setValue(null).setAnchor("Anchor21"));
			otherYamlSequence.add(new YamlSimpleValue().setValue(true).setAnchor("Anchor22"));
			otherYamlSequence.add(new YamlSimpleValue().setValue(23).setAnchor("Anchor23"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 24").setAnchor("Anchor24"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 25").setAnchor("Anchor25"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 26").setAnchor("Anchor26"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 27").setAnchor("Anchor27"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 28").setAnchor("Anchor28"));
			testYamlValue.put("property 5", otherYamlSequence);

			final YamlMapping otherYamlMapping = new YamlMapping().setStyle(YamlStyle.Flow).setAnchor("Anchor6").setComment("Comment 6\nMultiline comment 6");
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 31"), new YamlSimpleValue().setValue(null).setAnchor("Anchor31"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 32"), new YamlSimpleValue().setValue(true).setAnchor("Anchor32"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 33"), new YamlSimpleValue().setValue(33).setAnchor("Anchor33"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 34"), new YamlSimpleValue().setValue("String value 34").setAnchor("Anchor34"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 35"), new YamlSimpleValue().setValue("String value 35").setAnchor("Anchor35"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 36"), new YamlSimpleValue().setValue("String value 36").setAnchor("Anchor36"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 37"), new YamlSimpleValue().setValue("String value 37").setAnchor("Anchor37"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 38"), new YamlSimpleValue().setValue("String value 38").setAnchor("Anchor38"));
			testYamlValue.put("property 6", otherYamlMapping);

			testYamlValue.put(new YamlSimpleValue().setValue("property 7"), new YamlSimpleValue().setValue(null).setAnchor("Anchor7").setComment("Comment 7\nMultiline comment 7").setInlineComment("Inline comment 7"));

			testYamlValue.put("property 8", new YamlSequence().setStyle(YamlStyle.Flow).setAnchor("Anchor8").setComment("Comment 8\nMultiline comment 8").setInlineComment("Inline comment 8"));
			testYamlValue.put("property 9", new YamlMapping().setStyle(YamlStyle.Flow).setAnchor("Anchor9").setComment("Comment 9\nMultiline comment 9").setInlineComment("Inline comment 9"));

			final String expectedString = ""
					+ "# Comment 0\n"
					+ "# Multiline comment 0\n"
					+ "# Inline comment 0\n"
					+ "{\n"
					+ "  # Comment 1\n"
					+ "  # Multiline comment 1\n"
					+ "  property 1: null &Anchor1, # Inline comment 1\n"
					+ "  # Comment 2\n"
					+ "  # Multiline comment 2\n"
					+ "  property 2: true &Anchor2, # Inline comment 2\n"
					+ "  # Comment 3\n"
					+ "  # Multiline comment 3\n"
					+ "  property 3: 1 &Anchor3, # Inline comment 3\n"
					+ "  # Comment 4\n"
					+ "  # Multiline comment 4\n"
					+ "  property 4: String value 4 &Anchor4, # Inline comment 4\n"
					+ "  # Comment 5\n"
					+ "  # Multiline comment 5\n"
					+ "  property 5: [null &Anchor21, true &Anchor22, 23 &Anchor23, String value 24 &Anchor24, String value 25 &Anchor25, String value 26 &Anchor26, String value 27 &Anchor27, String value 28 &Anchor28] &Anchor5, # Inline comment 5\n"
					+ "  # Comment 6\n"
					+ "  # Multiline comment 6\n"
					+ "  property 6: {property 31: null &Anchor31, property 32: true &Anchor32, property 33: 33 &Anchor33, property 34: String value 34 &Anchor34, property 35: String value 35 &Anchor35, property 36: String value 36 &Anchor36, property 37: String value 37 &Anchor37, property 38: String value 38 &Anchor38} &Anchor6,\n"
					+ "  # Comment 7\n"
					+ "  # Multiline comment 7\n"
					+ "  property 7: null &Anchor7, # Inline comment 7\n"
					+ "  # Comment 8\n"
					+ "  # Multiline comment 8\n"
					+ "  property 8: [] &Anchor8, # Inline comment 8\n"
					+ "  # Comment 9\n"
					+ "  # Multiline comment 9\n"
					+ "  property 9: {} &Anchor9 # Inline comment 9\n"
					+ "}";

			testYamlObject(expectedString, testYamlValue);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	@Test
	public void testYamlMappingBracket() {
		try {
			final YamlMapping testYamlValue = new YamlMapping().setStyle(YamlStyle.Bracket).setAnchor("Anchor0").setComment("Comment 0\nMultiline comment 0").setInlineComment("Inline comment 0");
			testYamlValue.put(new YamlSimpleValue().setValue("property 1"), new YamlSimpleValue().setValue(null).setAnchor("Anchor1").setComment("Comment 1\nMultiline comment 1").setInlineComment("Inline comment 1"));
			testYamlValue.put(new YamlSimpleValue().setValue("property 2"), new YamlSimpleValue().setValue(true).setAnchor("Anchor2").setComment("Comment 2\nMultiline comment 2").setInlineComment("Inline comment 2"));
			testYamlValue.put(new YamlSimpleValue().setValue("property 3"), new YamlSimpleValue().setValue(1).setAnchor("Anchor3").setComment("Comment 3\nMultiline comment 3").setInlineComment("Inline comment 3"));
			testYamlValue.put(new YamlSimpleValue().setValue("property 4"), new YamlSimpleValue().setValue("String value 4").setAnchor("Anchor4").setComment("Comment 4\nMultiline comment 4").setInlineComment("Inline comment 4"));

			final YamlSequence otherYamlSequence = new YamlSequence().setStyle(YamlStyle.Bracket).setAnchor("Anchor5").setComment("Comment 5\nMultiline comment 5").setInlineComment("Inline comment 5");
			otherYamlSequence.add(new YamlSimpleValue().setValue(null).setAnchor("Anchor21").setComment("Comment 21\nMultiline comment 21").setInlineComment("Inline comment 21"));
			otherYamlSequence.add(new YamlSimpleValue().setValue(true).setAnchor("Anchor22").setComment("Comment 22\nMultiline comment 22").setInlineComment("Inline comment 22"));
			otherYamlSequence.add(new YamlSimpleValue().setValue(23).setAnchor("Anchor23").setComment("Comment 23\nMultiline comment 23").setInlineComment("Inline comment 23"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 24").setAnchor("Anchor24").setComment("Comment 24\nMultiline comment 24").setInlineComment("Inline comment 24"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 25").setAnchor("Anchor25").setComment("Comment 25\nMultiline comment 25").setInlineComment("Inline comment 25"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 26").setAnchor("Anchor26").setComment("Comment 26\nMultiline comment 26").setInlineComment("Inline comment 26"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 27").setAnchor("Anchor27").setComment("Comment 27\nMultiline comment 27").setInlineComment("Inline comment 27"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 28").setAnchor("Anchor28").setComment("Comment 28\nMultiline comment 28").setInlineComment("Inline comment 28"));
			testYamlValue.put("property 5", otherYamlSequence);

			final YamlMapping otherYamlMapping = new YamlMapping().setStyle(YamlStyle.Bracket).setAnchor("Anchor6").setComment("Comment 6\nMultiline comment 6").setInlineComment("Inline comment 6");
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 31"), new YamlSimpleValue().setValue(null).setAnchor("Anchor31").setComment("Comment 31\nMultiline comment 31").setInlineComment("Inline comment 31"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 32"), new YamlSimpleValue().setValue(true).setAnchor("Anchor32").setComment("Comment 32\nMultiline comment 32").setInlineComment("Inline comment 32"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 33"), new YamlSimpleValue().setValue(33).setAnchor("Anchor33").setComment("Comment 33\nMultiline comment 33").setInlineComment("Inline comment 33"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 34"), new YamlSimpleValue().setValue("String value 34").setAnchor("Anchor34").setComment("Comment 34\nMultiline comment 34").setInlineComment("Inline comment 34"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 35"), new YamlSimpleValue().setValue("String value 35").setAnchor("Anchor35").setComment("Comment 35\nMultiline comment 35").setInlineComment("Inline comment 35"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 36"), new YamlSimpleValue().setValue("String value 36").setAnchor("Anchor36").setComment("Comment 36\nMultiline comment 36").setInlineComment("Inline comment 36"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 37"), new YamlSimpleValue().setValue("String value 37").setAnchor("Anchor37").setComment("Comment 37\nMultiline comment 37").setInlineComment("Inline comment 37"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 38"), new YamlSimpleValue().setValue("String value 38").setAnchor("Anchor38").setComment("Comment 38\nMultiline comment 38").setInlineComment("Inline comment 38"));
			testYamlValue.put("property 6", otherYamlMapping);

			testYamlValue.put(new YamlSimpleValue().setValue("property 7"), new YamlSimpleValue().setValue(null).setAnchor("Anchor7").setComment("Comment 7\nMultiline comment 7").setInlineComment("Inline comment 7"));

			testYamlValue.put("property 8", new YamlSequence().setStyle(YamlStyle.Bracket).setAnchor("Anchor8").setComment("Comment 8\nMultiline comment 8").setInlineComment("Inline comment 8"));
			testYamlValue.put("property 9", new YamlMapping().setStyle(YamlStyle.Bracket).setAnchor("Anchor9").setComment("Comment 9\nMultiline comment 9").setInlineComment("Inline comment 9"));

			final String expectedString = ""
					+ "# Comment 0\n"
					+ "# Multiline comment 0\n"
					+ "# Inline comment 0\n"
					+ "{\n"
					+ "  # Comment 1\n"
					+ "  # Multiline comment 1\n"
					+ "  property 1: null &Anchor1, # Inline comment 1\n"
					+ "  # Comment 2\n"
					+ "  # Multiline comment 2\n"
					+ "  property 2: true &Anchor2, # Inline comment 2\n"
					+ "  # Comment 3\n"
					+ "  # Multiline comment 3\n"
					+ "  property 3: 1 &Anchor3, # Inline comment 3\n"
					+ "  # Comment 4\n"
					+ "  # Multiline comment 4\n"
					+ "  property 4: String value 4 &Anchor4, # Inline comment 4\n"
					+ "  # Comment 5\n"
					+ "  # Multiline comment 5\n"
					+ "  property 5: [\n"
					+ "    # Comment 21\n"
					+ "    # Multiline comment 21\n"
					+ "    null &Anchor21, # Inline comment 21\n"
					+ "    # Comment 22\n"
					+ "    # Multiline comment 22\n"
					+ "    true &Anchor22, # Inline comment 22\n"
					+ "    # Comment 23\n"
					+ "    # Multiline comment 23\n"
					+ "    23 &Anchor23, # Inline comment 23\n"
					+ "    # Comment 24\n"
					+ "    # Multiline comment 24\n"
					+ "    String value 24 &Anchor24, # Inline comment 24\n"
					+ "    # Comment 25\n"
					+ "    # Multiline comment 25\n"
					+ "    String value 25 &Anchor25, # Inline comment 25\n"
					+ "    # Comment 26\n"
					+ "    # Multiline comment 26\n"
					+ "    String value 26 &Anchor26, # Inline comment 26\n"
					+ "    # Comment 27\n"
					+ "    # Multiline comment 27\n"
					+ "    String value 27 &Anchor27, # Inline comment 27\n"
					+ "    # Comment 28\n"
					+ "    # Multiline comment 28\n"
					+ "    String value 28 &Anchor28 # Inline comment 28\n"
					+ "  ] &Anchor5, # Inline comment 5\n"
					+ "  # Comment 6\n"
					+ "  # Multiline comment 6\n"
					+ "  property 6: {\n"
					+ "    # Comment 31\n"
					+ "    # Multiline comment 31\n"
					+ "    property 31: null &Anchor31, # Inline comment 31\n"
					+ "    # Comment 32\n"
					+ "    # Multiline comment 32\n"
					+ "    property 32: true &Anchor32, # Inline comment 32\n"
					+ "    # Comment 33\n"
					+ "    # Multiline comment 33\n"
					+ "    property 33: 33 &Anchor33, # Inline comment 33\n"
					+ "    # Comment 34\n"
					+ "    # Multiline comment 34\n"
					+ "    property 34: String value 34 &Anchor34, # Inline comment 34\n"
					+ "    # Comment 35\n"
					+ "    # Multiline comment 35\n"
					+ "    property 35: String value 35 &Anchor35, # Inline comment 35\n"
					+ "    # Comment 36\n"
					+ "    # Multiline comment 36\n"
					+ "    property 36: String value 36 &Anchor36, # Inline comment 36\n"
					+ "    # Comment 37\n"
					+ "    # Multiline comment 37\n"
					+ "    property 37: String value 37 &Anchor37, # Inline comment 37\n"
					+ "    # Comment 38\n"
					+ "    # Multiline comment 38\n"
					+ "    property 38: String value 38 &Anchor38 # Inline comment 38\n"
					+ "  } &Anchor6, # Inline comment 6\n"
					+ "  # Comment 7\n"
					+ "  # Multiline comment 7\n"
					+ "  property 7: null &Anchor7, # Inline comment 7\n"
					+ "  # Comment 8\n"
					+ "  # Multiline comment 8\n"
					+ "  property 8: [] &Anchor8, # Inline comment 8\n"
					+ "  # Comment 9\n"
					+ "  # Multiline comment 9\n"
					+ "  property 9: {} &Anchor9 # Inline comment 9\n"
					+ "}";

			testYamlObject(expectedString, testYamlValue);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testYamlSequence() {
		try {
			final YamlSequence testYamlValue = new YamlSequence();
			testYamlValue.add(new YamlSimpleValue().setValue(null));
			testYamlValue.add(new YamlSimpleValue().setValue(true));
			testYamlValue.add(new YamlSimpleValue().setValue(1));
			testYamlValue.add(new YamlSimpleValue().setValue("String value 4"));

			final YamlSequence otherYamlSequence = new YamlSequence();
			otherYamlSequence.add(new YamlSimpleValue().setValue(null));
			otherYamlSequence.add(new YamlSimpleValue().setValue(true));
			otherYamlSequence.add(new YamlSimpleValue().setValue(23));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 24"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 25"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 26"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 27"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 28"));
			testYamlValue.add(otherYamlSequence);

			final YamlMapping otherYamlMapping = new YamlMapping();
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 31"), new YamlSimpleValue().setValue(null));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 32"), new YamlSimpleValue().setValue(true));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 33"), new YamlSimpleValue().setValue(33));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 34"), new YamlSimpleValue().setValue("String value 34"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 35"), new YamlSimpleValue().setValue("String value 35"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 36"), new YamlSimpleValue().setValue("String value 36"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 37"), new YamlSimpleValue().setValue("String value 37"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 38"), new YamlSimpleValue().setValue("String value 38"));
			testYamlValue.add(otherYamlMapping);

			testYamlValue.add(new YamlSimpleValue().setValue(null));

			testYamlValue.add(new YamlSequence());
			testYamlValue.add(new YamlMapping());

			final String expectedString = ""
					+ "- null\n"
					+ "- true\n"
					+ "- 1\n"
					+ "- String value 4\n"
					+ "-\n"
					+ "  - null\n"
					+ "  - true\n"
					+ "  - 23\n"
					+ "  - String value 24\n"
					+ "  - String value 25\n"
					+ "  - String value 26\n"
					+ "  - String value 27\n"
					+ "  - String value 28\n"
					+ "- property 31:\n"
					+ "  property 32: true\n"
					+ "  property 33: 33\n"
					+ "  property 34: String value 34\n"
					+ "  property 35: String value 35\n"
					+ "  property 36: String value 36\n"
					+ "  property 37: String value 37\n"
					+ "  property 38: String value 38\n"
					+ "- null\n"
					+ "- []\n"
					+ "- {}\n";

			testYamlObject(expectedString, testYamlValue);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testYamlSequenceWithAnchorsAndComments() {
		try {
			final YamlSequence testYamlValue = new YamlSequence().setAnchor("Anchor0").setComment("Comment 0\nMultiline comment 0").setInlineComment("Inline comment 0");
			testYamlValue.add(new YamlSimpleValue().setValue(null).setAnchor("Anchor1").setComment("Comment 1\nMultiline comment 1").setInlineComment("Inline comment 1"));
			testYamlValue.add(new YamlSimpleValue().setValue(true).setAnchor("Anchor2").setComment("Comment 2\nMultiline comment 2").setInlineComment("Inline comment 2"));
			testYamlValue.add(new YamlSimpleValue().setValue(1).setAnchor("Anchor3").setComment("Comment 3\nMultiline comment 3").setInlineComment("Inline comment 3"));
			testYamlValue.add(new YamlSimpleValue().setValue("String value 4").setAnchor("Anchor4").setComment("Comment 4\nMultiline comment 4").setInlineComment("Inline comment 4"));

			final YamlSequence otherYamlSequence = new YamlSequence().setAnchor("Anchor5").setComment("Comment 5\nMultiline comment 5").setInlineComment("Inline comment 5");
			otherYamlSequence.add(new YamlSimpleValue().setValue(null).setAnchor("Anchor21").setComment("Comment 21\nMultiline comment 21").setInlineComment("Inline comment 21"));
			otherYamlSequence.add(new YamlSimpleValue().setValue(true).setAnchor("Anchor22").setComment("Comment 22\nMultiline comment 22").setInlineComment("Inline comment 22"));
			otherYamlSequence.add(new YamlSimpleValue().setValue(23).setAnchor("Anchor23").setComment("Comment 23\nMultiline comment 23").setInlineComment("Inline comment 23"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 24").setAnchor("Anchor24").setComment("Comment 24\nMultiline comment 24").setInlineComment("Inline comment 24"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 25").setAnchor("Anchor25").setComment("Comment 25\nMultiline comment 25").setInlineComment("Inline comment 25"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 26").setAnchor("Anchor26").setComment("Comment 26\nMultiline comment 26").setInlineComment("Inline comment 26"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 27").setAnchor("Anchor27").setComment("Comment 27\nMultiline comment 27").setInlineComment("Inline comment 27"));
			otherYamlSequence.add(new YamlSimpleValue().setValue("String value 28").setAnchor("Anchor28").setComment("Comment 28\nMultiline comment 28").setInlineComment("Inline comment 28"));
			testYamlValue.add(otherYamlSequence);

			final YamlMapping otherYamlMapping = new YamlMapping().setAnchor("Anchor6").setComment("Comment 6\nMultiline comment 6").setInlineComment("Inline comment 6");
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 31"), new YamlSimpleValue().setValue(null).setAnchor("Anchor31").setComment("Comment 31\nMultiline comment 31").setInlineComment("Inline comment 31"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 32"), new YamlSimpleValue().setValue(true).setAnchor("Anchor32").setComment("Comment 32\nMultiline comment 32").setInlineComment("Inline comment 32"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 33"), new YamlSimpleValue().setValue(33).setAnchor("Anchor33").setComment("Comment 33\nMultiline comment 33").setInlineComment("Inline comment 33"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 34"), new YamlSimpleValue().setValue("String value 34").setAnchor("Anchor34").setComment("Comment 34\nMultiline comment 34").setInlineComment("Inline comment 34"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 35"), new YamlSimpleValue().setValue("String value 35").setAnchor("Anchor35").setComment("Comment 35\nMultiline comment 35").setInlineComment("Inline comment 35"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 36"), new YamlSimpleValue().setValue("String value 36").setAnchor("Anchor36").setComment("Comment 36\nMultiline comment 36").setInlineComment("Inline comment 36"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 37"), new YamlSimpleValue().setValue("String value 37").setAnchor("Anchor37").setComment("Comment 37\nMultiline comment 37").setInlineComment("Inline comment 37"));
			otherYamlMapping.put(new YamlSimpleValue().setValue("property 38"), new YamlSimpleValue().setValue("String value 38").setAnchor("Anchor38").setComment("Comment 38\nMultiline comment 38").setInlineComment("Inline comment 38"));
			testYamlValue.add(otherYamlMapping);

			testYamlValue.add(new YamlSimpleValue().setValue(null).setAnchor("Anchor7").setComment("Comment 7\nMultiline comment 7").setInlineComment("Inline comment 7"));

			testYamlValue.add(new YamlSequence().setAnchor("Anchor8").setComment("Comment 8\nMultiline comment 8").setInlineComment("Inline comment 8"));
			testYamlValue.add(new YamlMapping().setAnchor("Anchor9").setComment("Comment 9\nMultiline comment 9").setInlineComment("Inline comment 9"));

			final String expectedString = ""
					+ "# Comment 0\n"
					+ "# Multiline comment 0\n"
					+ "# Inline comment 0\n"
					+ "# Comment 1\n"
					+ "# Multiline comment 1\n"
					+ "- null &Anchor1 # Inline comment 1\n"
					+ "# Comment 2\n"
					+ "# Multiline comment 2\n"
					+ "- true &Anchor2 # Inline comment 2\n"
					+ "# Comment 3\n"
					+ "# Multiline comment 3\n"
					+ "- 1 &Anchor3 # Inline comment 3\n"
					+ "# Comment 4\n"
					+ "# Multiline comment 4\n"
					+ "- String value 4 &Anchor4 # Inline comment 4\n"
					+ "# Comment 5\n"
					+ "# Multiline comment 5\n"
					+ "- &Anchor5 # Inline comment 5\n"
					+ "  # Comment 21\n"
					+ "  # Multiline comment 21\n"
					+ "  - null &Anchor21 # Inline comment 21\n"
					+ "  # Comment 22\n"
					+ "  # Multiline comment 22\n"
					+ "  - true &Anchor22 # Inline comment 22\n"
					+ "  # Comment 23\n"
					+ "  # Multiline comment 23\n"
					+ "  - 23 &Anchor23 # Inline comment 23\n"
					+ "  # Comment 24\n"
					+ "  # Multiline comment 24\n"
					+ "  - String value 24 &Anchor24 # Inline comment 24\n"
					+ "  # Comment 25\n"
					+ "  # Multiline comment 25\n"
					+ "  - String value 25 &Anchor25 # Inline comment 25\n"
					+ "  # Comment 26\n"
					+ "  # Multiline comment 26\n"
					+ "  - String value 26 &Anchor26 # Inline comment 26\n"
					+ "  # Comment 27\n"
					+ "  # Multiline comment 27\n"
					+ "  - String value 27 &Anchor27 # Inline comment 27\n"
					+ "  # Comment 28\n"
					+ "  # Multiline comment 28\n"
					+ "  - String value 28 &Anchor28 # Inline comment 28\n"
					+ "# Comment 6\n"
					+ "# Multiline comment 6\n"
					+ "- &Anchor6 # Inline comment 6\n"
					+ "  # Comment 31\n"
					+ "  # Multiline comment 31\n"
					+ "  property 31: &Anchor31 # Inline comment 31\n"
					+ "  # Comment 32\n"
					+ "  # Multiline comment 32\n"
					+ "  property 32: &Anchor32 true # Inline comment 32\n"
					+ "  # Comment 33\n"
					+ "  # Multiline comment 33\n"
					+ "  property 33: &Anchor33 33 # Inline comment 33\n"
					+ "  # Comment 34\n"
					+ "  # Multiline comment 34\n"
					+ "  property 34: &Anchor34 String value 34 # Inline comment 34\n"
					+ "  # Comment 35\n"
					+ "  # Multiline comment 35\n"
					+ "  property 35: &Anchor35 String value 35 # Inline comment 35\n"
					+ "  # Comment 36\n"
					+ "  # Multiline comment 36\n"
					+ "  property 36: &Anchor36 String value 36 # Inline comment 36\n"
					+ "  # Comment 37\n"
					+ "  # Multiline comment 37\n"
					+ "  property 37: &Anchor37 String value 37 # Inline comment 37\n"
					+ "  # Comment 38\n"
					+ "  # Multiline comment 38\n"
					+ "  property 38: &Anchor38 String value 38 # Inline comment 38\n"
					+ "# Comment 7\n"
					+ "# Multiline comment 7\n"
					+ "- null &Anchor7 # Inline comment 7\n"
					+ "# Comment 8\n"
					+ "# Multiline comment 8\n"
					+ "- [] &Anchor8 # Inline comment 8\n"
					+ "# Comment 9\n"
					+ "# Multiline comment 9\n"
					+ "- {} &Anchor9 # Inline comment 9\n";

			testYamlObject(expectedString, testYamlValue);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSimple1() {
		try {
			final YamlSequence yamlSequence = new YamlSequence().setStyle(YamlStyle.Standard);
			final YamlMapping yamlMapping = new YamlMapping();
			yamlMapping.put(new YamlSimpleValue().setValue("property 1"), new YamlSimpleValue().setValue(null));
			yamlMapping.put(new YamlSimpleValue().setValue("property 2"), new YamlSimpleValue().setValue("Property value 2"));
			yamlSequence.add(yamlMapping);

			final String expectedString = ""
					+ "- property 1:\n"
					+ "  property 2: Property value 2\n";

			testYamlObject(expectedString, yamlSequence);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSimple2() {
		try {
			final YamlSequence yamlSequence = new YamlSequence().setStyle(YamlStyle.Standard).setInlineComment("Inline comment");
			final YamlMapping yamlMapping = new YamlMapping();
			yamlMapping.put(new YamlSimpleValue().setValue("property 1"), new YamlSimpleValue().setValue(null));
			yamlMapping.put(new YamlSimpleValue().setValue("property 2"), new YamlSimpleValue().setValue("Property value 1"));
			yamlSequence.add(yamlMapping);

			final String expectedString = ""
					+ "# Inline comment\n"
					+ "- property 1:\n"
					+ "  property 2: Property value 1\n";

			testYamlObject(expectedString, yamlSequence);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSimple3() {
		try {
			final YamlSequence yamlSequence = new YamlSequence().setStyle(YamlStyle.Standard);
			final YamlMapping yamlMapping = new YamlMapping().setInlineComment("Inline comment");
			yamlMapping.put(new YamlSimpleValue().setValue("property 1"), new YamlSimpleValue().setValue(null));
			yamlMapping.put(new YamlSimpleValue().setValue("property 2"), new YamlSimpleValue().setValue("Property value 1"));
			yamlSequence.add(yamlMapping);

			final String expectedString = ""
					+ "- # Inline comment\n"
					+ "  property 1:\n"
					+ "  property 2: Property value 1\n";

			testYamlObject(expectedString, yamlSequence);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSimple4() {
		try {
			final YamlMapping yamlSubMapping = new YamlMapping().setInlineComment("anchor \"&id001\"");
			yamlSubMapping.put("property1a", "other property");

			final YamlMapping yamlMapping = new YamlMapping();
			yamlMapping.put("preitem", yamlSubMapping);

			final YamlSequence yamlSequence = new YamlSequence().setStyle(YamlStyle.Standard);
			yamlSequence.add(yamlMapping);

			final String expectedString = ""
					+ "- preitem: # anchor \"&id001\"\n"
					+ "    property1a: other property\n";

			testYamlObject(expectedString, yamlSequence);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSimple5() {
		try {
			final YamlMapping yamlSubMapping = new YamlMapping().setAnchor("id001").setInlineComment("anchor \"&id001\"");
			yamlSubMapping.put("property1a", "other property");

			final YamlMapping yamlMapping = new YamlMapping();
			yamlMapping.put("preitem", yamlSubMapping);

			final YamlSequence yamlSequence = new YamlSequence().setStyle(YamlStyle.Standard);
			yamlSequence.add(yamlMapping);

			final String expectedString = ""
					+ "- preitem: &id001 # anchor \"&id001\"\n"
					+ "    property1a: other property\n";

			testYamlObject(expectedString, yamlSequence);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSimple6() {
		try {
			final YamlMapping yamlSubMapping = new YamlMapping().setAnchor("sub_anchor").setComment("Sub mapping comment").setInlineComment("Sub mapping inline comment");
			yamlSubMapping.put("subMappingItem", "subMappingValue");

			final YamlMapping yamlMapping = new YamlMapping().setAnchor("main_anchor").setComment("Main mapping comment").setInlineComment("Main mapping inline comment");
			yamlMapping.put("mainMappingItem", yamlSubMapping);

			final YamlSequence yamlSequence = new YamlSequence().setStyle(YamlStyle.Standard).setComment("Sequence comment").setInlineComment("Sequence inline comment");
			yamlSequence.add(yamlMapping);

			final String expectedString = ""
					+ "# Sequence comment\n"
					+ "# Sequence inline comment\n"
					+ "# Main mapping comment\n"
					+ "- &main_anchor # Main mapping inline comment\n"
					+ "  mainMappingItem: &sub_anchor # Sub mapping inline comment\n"
					+ "    # Sub mapping comment\n"
					+ "    subMappingItem: subMappingValue\n";

			testYamlObject(expectedString, yamlSequence);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	private static void testYamlObject(final String expectedString, final YamlValue testYamlValue) throws Exception {
		String newString;
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
				YamlWriter writer = new YamlWriter(output, StandardCharsets.UTF_8);) {
			writer.add(testYamlValue, false);
			writer.flush();
			newString = new String(output.toByteArray(), StandardCharsets.UTF_8);
		}
		Assert.assertEquals(expectedString, newString);
	}
}
