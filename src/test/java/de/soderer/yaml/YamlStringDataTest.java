package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlSequence;

@SuppressWarnings("static-method")
public class YamlStringDataTest {
	/**
	 * Original bug report: a literal block scalar ("|-") whose content lines have varying
	 * indentation, where a later line is indented *less* than a previous line but still more
	 * than (or equal to) the block scalar's own base indentation.
	 * This used to throw "Invalid YAML mulitline scalar: Unexpected indentation level".
	 */
	@Test
	public void testMultiLine() throws Exception {
		try {
			final String yamlTestString =
				"extraVolumes: |-\n"
				+ "  - test:\n"
				+ "      abc: def\n"
				+ "    ghi: jkl\n";

			try (YamlReader reader = new YamlReader(new ByteArrayInputStream(yamlTestString.getBytes(StandardCharsets.UTF_8)))) {
				reader.readDocument();
			}
		} catch (final Exception e) {
			Assertions.fail("Unexpected exception: " + e.getMessage());
		}
	}

	/**
	 * Same as {@link #testMultiLine()}, but also checks that the parsed scalar value exactly
	 * matches the expected de-indented content (each line stripped of the base indentation of
	 * 2 spaces established by the first content line, relative indentation preserved).
	 */
	@Test
	public void testMultiLineContent() throws Exception {
		final String yamlTestString =
			"extraVolumes: |-\n"
			+ "  - test:\n"
			+ "      abc: def\n"
			+ "    ghi: jkl\n";

		final YamlScalar valueScalar = readSingleMappingValue(yamlTestString, "extraVolumes");

		final String expected = "- test:\n    abc: def\n  ghi: jkl";
		Assertions.assertEquals(expected, valueScalar.getValueString());
	}

	/**
	 * A literal block scalar followed by a sibling mapping key at the same indentation level
	 * as the original key. Verifies the scalar ends at the correct line and the surrounding
	 * mapping continues to parse correctly afterwards.
	 */
	@Test
	public void testMultiLineFollowedBySiblingKey() throws Exception {
		final String yamlTestString =
			"extraVolumes: |-\n"
			+ "  - test:\n"
			+ "      abc: def\n"
			+ "    ghi: jkl\n"
			+ "otherKey: someValue\n";

		final YamlDocument document = readDocument(yamlTestString);
		final YamlMapping rootMapping = (YamlMapping) document.getRoot();

		Assertions.assertEquals(2, rootMapping.size());

		final YamlScalar extraVolumesValue = getValueByKeyName(rootMapping, "extraVolumes");
		Assertions.assertEquals("- test:\n    abc: def\n  ghi: jkl", extraVolumesValue.getValueString());

		final YamlScalar otherKeyValue = getValueByKeyName(rootMapping, "otherKey");
		Assertions.assertEquals("someValue", otherKeyValue.getValueString());
	}

	/**
	 * A literal block scalar nested two mapping levels deep, followed by a sibling key one
	 * level shallower than the scalar's parent. Verifies the indentation stack is correctly
	 * restored to a middle indentation level (not just back to the root) after the scalar ends.
	 */
	@Test
	public void testMultiLineNestedWithDedentToMiddleLevel() throws Exception {
		final String yamlTestString =
			"outer:\n"
			+ "  inner:\n"
			+ "    extraVolumes: |-\n"
			+ "      x: 1\n"
			+ "        y: 2\n"
			+ "  sibling: 9\n";

		final YamlDocument document = readDocument(yamlTestString);
		final YamlMapping rootMapping = (YamlMapping) document.getRoot();

		final YamlNode outerValue = getRawValueByKeyName(rootMapping, "outer");
		Assertions.assertTrue(outerValue instanceof YamlMapping);
		final YamlMapping outerMapping = (YamlMapping) outerValue;

		Assertions.assertEquals(2, outerMapping.size());

		final YamlNode innerValue = getRawValueByKeyName(outerMapping, "inner");
		Assertions.assertTrue(innerValue instanceof YamlMapping);
		final YamlMapping innerMapping = (YamlMapping) innerValue;

		final YamlScalar extraVolumesValue = getValueByKeyName(innerMapping, "extraVolumes");
		Assertions.assertEquals("x: 1\n  y: 2", extraVolumesValue.getValueString());

		final YamlScalar siblingValue = getValueByKeyName(outerMapping, "sibling");
		Assertions.assertEquals("9", siblingValue.getValueString());
	}

	/**
	 * A literal block scalar using an explicit indentation indicator ("|-2"), where the first
	 * content line is indented *more* than the indicator specifies. The extra leading spaces on
	 * that first line must be preserved as literal content, not stripped away.
	 */
	@Test
	public void testMultiLineWithExplicitIndentationIndicator() throws Exception {
		final String yamlTestString =
			"extraVolumes: |-2\n"
			+ "    extra indented first line\n"
			+ "  normal\n";

		final YamlScalar valueScalar = readSingleMappingValue(yamlTestString, "extraVolumes");

		Assertions.assertEquals("  extra indented first line\nnormal", valueScalar.getValueString());
	}

	/**
	 * A literal block scalar containing an empty line in the middle of its content. The empty
	 * line must be preserved in the parsed value regardless of the indentation, and parsing
	 * must correctly continue with the sibling key afterwards.
	 */
	@Test
	public void testMultiLineWithEmptyLineInContent() throws Exception {
		final String yamlTestString =
			"extraVolumes: |-\n"
			+ "  line1\n"
			+ "\n"
			+ "  line2\n"
			+ "sibling: 1\n";

		final YamlDocument document = readDocument(yamlTestString);
		final YamlMapping rootMapping = (YamlMapping) document.getRoot();

		final YamlScalar extraVolumesValue = getValueByKeyName(rootMapping, "extraVolumes");
		Assertions.assertEquals("line1\n\nline2", extraVolumesValue.getValueString());

		final YamlScalar siblingValue = getValueByKeyName(rootMapping, "sibling");
		Assertions.assertEquals("1", siblingValue.getValueString());
	}

	/**
	 * A simple literal block scalar with constant indentation, to cover the unchanged base case
	 * that already worked correctly before the fix.
	 */
	@Test
	public void testMultiLineSimpleConstantIndentation() throws Exception {
		final String yamlTestString =
			"description: |-\n"
			+ "  line one\n"
			+ "  line two\n";

		final YamlScalar valueScalar = readSingleMappingValue(yamlTestString, "description");

		Assertions.assertEquals("line one\nline two", valueScalar.getValueString());
	}

	/**
	 * A folded block scalar (">") with varying content line indentation, to verify the fix also
	 * applies correctly to the folded multiline type and not just literal ("|").
	 */
	@Test
	public void testMultiLineFoldedWithVaryingIndentation() throws Exception {
		final String yamlTestString =
			"description: >-\n"
			+ "  - test:\n"
			+ "      abc: def\n"
			+ "    ghi: jkl\n";

		final YamlScalar valueScalar = readSingleMappingValue(yamlTestString, "description");

		// Folded lines are joined with spaces, but the relative indentation of each line is
		// preserved as part of its content before folding
		Assertions.assertEquals("- test:     abc: def   ghi: jkl", valueScalar.getValueString());
	}

	/**
	 * A literal block scalar used directly as a sequence item's value ("- |-"), where the
	 * scalar's start column exactly matches the indentation level already introduced by the
	 * sequence item's dash. In this situation no extra, temporary column-indentation level was
	 * added before the multiline scalar, unlike when a multiline scalar is a mapping value at a
	 * column further right than its key. A naive unconditional pop of the indentation stack
	 * before parsing the multiline scalar would incorrectly discard the sequence item's own
	 * indentation level here, breaking the parsing of a subsequent sibling key.
	 */
	@Test
	public void testMultiLineAsSequenceItemValue() throws Exception {
		final String yamlTestString =
			"items:\n"
			+ "  - |-\n"
			+ "    text\n"
			+ "    more\n"
			+ "next: 1\n";

		final YamlDocument document = readDocument(yamlTestString);
		final YamlMapping rootMapping = (YamlMapping) document.getRoot();

		Assertions.assertEquals(2, rootMapping.size());

		final YamlNode itemsValue = getRawValueByKeyName(rootMapping, "items");
		Assertions.assertTrue(itemsValue instanceof YamlSequence);
		final YamlSequence itemsSequence = (YamlSequence) itemsValue;

		Assertions.assertEquals(1, itemsSequence.size());
		YamlNode firstItem = null;
		for (final YamlNode item : itemsSequence.items()) {
			firstItem = item;
			break;
		}
		Assertions.assertTrue(firstItem instanceof YamlScalar);
		Assertions.assertEquals("text\nmore", ((YamlScalar) firstItem).getValueString());

		final YamlScalar nextValue = getValueByKeyName(rootMapping, "next");
		Assertions.assertEquals("1", nextValue.getValueString());
	}

	/**
	 * A literal block scalar with KEEP chomping ("|+") followed by several blank lines and then
	 * a sibling key at a shallower indentation. Per YAML's chomping rules, blank lines between
	 * the last content line and the point where the block scalar ends (here: the less-indented
	 * sibling key) still belong to the scalar's trailing newlines and must be preserved, not
	 * discarded just because the scalar's end was detected via a dedented following line rather
	 * than by reaching the end of the document.
	 */
	@Test
	public void testMultiLineKeepChompingPreservesTrailingBlankLinesBeforeSibling() throws Exception {
		final String yamlTestString =
			"text: |+\n"
			+ "  line1\n"
			+ "\n"
			+ "\n"
			+ "\n"
			+ "sibling: 1\n";

		final YamlDocument document = readDocument(yamlTestString);
		final YamlMapping rootMapping = (YamlMapping) document.getRoot();

		Assertions.assertEquals(2, rootMapping.size());

		final YamlScalar textValue = getValueByKeyName(rootMapping, "text");
		Assertions.assertEquals("line1\n\n\n\n", textValue.getValueString());

		final YamlScalar siblingValue = getValueByKeyName(rootMapping, "sibling");
		Assertions.assertEquals("1", siblingValue.getValueString());
	}

	private static YamlDocument readDocument(final String yamlTestString) throws Exception {
		try (YamlReader reader = new YamlReader(new ByteArrayInputStream(yamlTestString.getBytes(StandardCharsets.UTF_8)))) {
			return reader.readDocument();
		}
	}

	private static YamlScalar readSingleMappingValue(final String yamlTestString, final String keyName) throws Exception {
		final YamlDocument document = readDocument(yamlTestString);
		final YamlMapping rootMapping = (YamlMapping) document.getRoot();
		return getValueByKeyName(rootMapping, keyName);
	}

	private static YamlNode getRawValueByKeyName(final YamlMapping mapping, final String keyName) {
		for (final Entry<YamlNode, YamlNode> entry : mapping.entrySet()) {
			if (entry.getKey() instanceof final YamlScalar keyScalar && keyName.equals(keyScalar.getValueString())) {
				return entry.getValue();
			}
		}
		Assertions.fail("Key '" + keyName + "' not found in mapping");
		return null;
	}

	private static YamlScalar getValueByKeyName(final YamlMapping mapping, final String keyName) {
		final YamlNode value = getRawValueByKeyName(mapping, keyName);
		Assertions.assertTrue(value instanceof YamlScalar, "Value for key '" + keyName + "' is not a scalar");
		return (YamlScalar) value;
	}
}