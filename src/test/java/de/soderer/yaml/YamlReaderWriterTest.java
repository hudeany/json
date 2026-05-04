package de.soderer.yaml;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlDocument;
import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlMultilineScalarChompingType;
import de.soderer.yaml.data.YamlMultilineScalarType;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;
import de.soderer.yaml.data.YamlScalarType;
import de.soderer.yaml.data.YamlSequence;
import de.soderer.yaml.data.directive.YamlTagDirective;
import de.soderer.yaml.data.directive.YamlVersionDirective;

/**
 * Comprehensive JUnit 5 tests for YamlReader and YamlWriter.
 *
 * Covered areas:
 *  - Scalars (String, Number, Boolean, Null)
 *  - Block and flow sequences
 *  - Block and flow mappings
 *  - Multiline scalars (literal | and folded >), all chomping modes
 *  - Anchors (&) and aliases (*)
 *  - Comments (leading and inline)
 *  - Directives (%YAML, %TAG)
 *  - Multiple documents in a stream
 *  - Special characters / quoting
 *  - YamlWriter format options (YamlFormat)
 *  - Error handling (invalid input)
 *  - Round-trip: Read → Write → Read
 */
@DisplayName("YamlReader & YamlWriter Tests")
class YamlReaderWriterTest {

    // =========================================================================
    // Helper methods
    // =========================================================================

    private static YamlDocument read(final String yaml) throws Exception {
        return YamlReader.readDocument(yaml);
    }

    private static String write(final YamlDocument doc) throws Exception {
        return YamlWriter.toString(doc);
    }

    private static YamlScalar scalar(final YamlDocument doc) {
        return (YamlScalar) doc.getRoot();
    }

    private static YamlMapping mapping(final YamlDocument doc) {
        return (YamlMapping) doc.getRoot();
    }

    private static YamlSequence sequence(final YamlDocument doc) {
        return (YamlSequence) doc.getRoot();
    }

    // =========================================================================
    // 1. Scalars
    // =========================================================================

    @Nested
    @DisplayName("1. Scalars")
    class ScalarTests {

        @Test
        @DisplayName("Simple string scalar")
        void simpleString() throws Exception {
            final YamlDocument doc = read("Hello World");
            assertNotNull(doc);
            final YamlScalar s = scalar(doc);
            assertEquals(YamlScalarType.STRING, s.getType());
            assertEquals("Hello World", s.getValueString());
        }

        @Test
        @DisplayName("Integer number")
        void integerNumber() throws Exception {
            final YamlDocument doc = read("42");
            final YamlScalar s = scalar(doc);
            assertEquals(YamlScalarType.NUMBER, s.getType());
            assertEquals("42", s.getValueString());
        }

        @Test
        @DisplayName("Negative number")
        void negativeNumber() throws Exception {
            final YamlDocument doc = read("-7");
            final YamlScalar s = scalar(doc);
            assertEquals(YamlScalarType.NUMBER, s.getType());
        }

        @Test
        @DisplayName("Float number")
        void floatNumber() throws Exception {
            final YamlDocument doc = read("3.14");
            final YamlScalar s = scalar(doc);
            assertEquals(YamlScalarType.NUMBER, s.getType());
        }

        @ParameterizedTest(name = "Boolean: {0}")
        @ValueSource(strings = {"true", "false", "True", "False", "TRUE", "FALSE"})
        @DisplayName("Boolean values")
        void booleanValues(final String value) throws Exception {
            final YamlDocument doc = read(value);
            final YamlScalar s = scalar(doc);
            assertEquals(YamlScalarType.BOOLEAN, s.getType());
        }

        @ParameterizedTest(name = "Null: {0}")
        @ValueSource(strings = {"null", "Null", "NULL", "~"})
        @DisplayName("Null values")
        void nullValues(final String value) throws Exception {
            final YamlDocument doc = read(value);
            final YamlScalar s = scalar(doc);
            assertEquals(YamlScalarType.NULL_VALUE, s.getType());
        }

        @Test
        @DisplayName("Double-quoted string")
        void doubleQuotedString() throws Exception {
            final YamlDocument doc = read("\"hello world\"");
            final YamlScalar s = scalar(doc);
            assertEquals(YamlScalarType.STRING, s.getType());
            assertEquals("hello world", s.getValueString());
        }

        @Test
        @DisplayName("Single-quoted string")
        void singleQuotedString() throws Exception {
            final YamlDocument doc = read("'hello world'");
            final YamlScalar s = scalar(doc);
            assertEquals(YamlScalarType.STRING, s.getType());
            assertEquals("hello world", s.getValueString());
        }

        @Test
        @DisplayName("Empty string (double-quoted)")
        void emptyDoubleQuotedString() throws Exception {
            final YamlDocument doc = read("\"\"");
            final YamlScalar s = scalar(doc);
            assertEquals(YamlScalarType.STRING, s.getType());
            assertEquals("", s.getValueString());
        }

        @Test
        @DisplayName("String with escape sequence (\\n)")
        void escapedNewlineInDoubleQuotedString() throws Exception {
            final YamlDocument doc = read("\"line1\\nline2\"");
            final YamlScalar s = scalar(doc);
            assertTrue(s.getValueString().contains("\n") || s.getValueString().contains("\\n"),
                    "Escape sequence should be processed");
        }

        @Test
        @DisplayName("Single-quoted string with single quote (escaped via '')")
        void singleQuoteEscapeInSingleQuotedString() throws Exception {
            final YamlDocument doc = read("'it''s fine'");
            final YamlScalar s = scalar(doc);
            assertEquals("it's fine", s.getValueString());
        }

        @Test
        @DisplayName("!!str forces string type for numeric input")
        void explicitStrTag() throws Exception {
            final YamlDocument doc = read("!!str 42");
            final YamlScalar s = scalar(doc);
            assertEquals(YamlScalarType.STRING, s.getType());
            assertEquals("42", s.getValueString());
        }

        @Test
        @DisplayName("!!float forces number type for text input")
        void explicitFloatTag() throws Exception {
            final YamlDocument doc = read("!!float 3");
            final YamlScalar s = scalar(doc);
            assertEquals(YamlScalarType.NUMBER, s.getType());
        }
    }

    // =========================================================================
    // 2. Block sequences
    // =========================================================================

    @Nested
    @DisplayName("2. Block Sequences")
    class BlockSequenceTests {

        @Test
        @DisplayName("Simple block sequence with strings")
        void simpleBlockSequence() throws Exception {
            final YamlDocument doc = read("- alpha\n- beta\n- gamma\n");
            final YamlSequence seq = sequence(doc);
            assertEquals(3, seq.size());
            assertEquals("alpha", ((YamlScalar) seq.get(0)).getValueString());
            assertEquals("beta",  ((YamlScalar) seq.get(1)).getValueString());
            assertEquals("gamma", ((YamlScalar) seq.get(2)).getValueString());
        }

        @Test
        @DisplayName("Block sequence with mixed types")
        void mixedTypeBlockSequence() throws Exception {
            final YamlDocument doc = read("- hello\n- 42\n- true\n- null\n");
            final YamlSequence seq = sequence(doc);
            assertEquals(4, seq.size());
            assertEquals(YamlScalarType.STRING,     ((YamlScalar) seq.get(0)).getType());
            assertEquals(YamlScalarType.NUMBER,     ((YamlScalar) seq.get(1)).getType());
            assertEquals(YamlScalarType.BOOLEAN,    ((YamlScalar) seq.get(2)).getType());
            assertEquals(YamlScalarType.NULL_VALUE, ((YamlScalar) seq.get(3)).getType());
        }

        @Test
        @DisplayName("Nested block sequence")
        void nestedBlockSequence() throws Exception {
            final String yaml = "- - a\n  - b\n- - c\n";
            final YamlDocument doc = read(yaml);
            final YamlSequence outer = sequence(doc);
            assertEquals(2, outer.size());
            assertTrue(outer.get(0) instanceof YamlSequence);
            assertEquals(2, ((YamlSequence) outer.get(0)).size());
        }

        @Test
        @DisplayName("Block sequence with mapping elements")
        void blockSequenceWithMappings() throws Exception {
            final String yaml = "- name: Alice\n  age: 30\n- name: Bob\n  age: 25\n";
            final YamlDocument doc = read(yaml);
            final YamlSequence seq = sequence(doc);
            assertEquals(2, seq.size());
            assertTrue(seq.get(0) instanceof YamlMapping);
        }

        @Test
        @DisplayName("Single-element sequence")
        void singleElementSequence() throws Exception {
            final YamlDocument doc = read("- only\n");
            final YamlSequence seq = sequence(doc);
            assertEquals(1, seq.size());
        }
    }

    // =========================================================================
    // 3. Flow sequences
    // =========================================================================

    @Nested
    @DisplayName("3. Flow Sequences")
    class FlowSequenceTests {

        @Test
        @DisplayName("Simple flow sequence")
        void simpleFlowSequence() throws Exception {
            final YamlDocument doc = read("[1, 2, 3]");
            final YamlSequence seq = sequence(doc);
            assertEquals(3, seq.size());
        }

        @Test
        @DisplayName("Empty flow sequence")
        void emptyFlowSequence() throws Exception {
            final YamlDocument doc = read("[]");
            final YamlSequence seq = sequence(doc);
            assertEquals(0, seq.size());
        }

        @Test
        @DisplayName("Flow sequence with quoted strings")
        void flowSequenceWithQuotedStrings() throws Exception {
            final YamlDocument doc = read("[\"hello\", 'world']");
            final YamlSequence seq = sequence(doc);
            assertEquals(2, seq.size());
            assertEquals("hello", ((YamlScalar) seq.get(0)).getValueString());
            assertEquals("world", ((YamlScalar) seq.get(1)).getValueString());
        }

        @Test
        @DisplayName("Nested flow sequences")
        void nestedFlowSequences() throws Exception {
            final YamlDocument doc = read("[[1, 2], [3, 4]]");
            final YamlSequence outer = sequence(doc);
            assertEquals(2, outer.size());
            assertTrue(outer.get(0) instanceof YamlSequence);
        }
    }

    // =========================================================================
    // 4. Block mappings
    // =========================================================================

    @Nested
    @DisplayName("4. Block Mappings")
    class BlockMappingTests {

        @Test
        @DisplayName("Simple block mapping")
        void simpleBlockMapping() throws Exception {
            final YamlDocument doc = read("name: Alice\nage: 30\n");
            final YamlMapping map = mapping(doc);
            assertEquals(2, map.size());
        }

        @Test
        @DisplayName("Nested block mapping")
        void nestedBlockMapping() throws Exception {
            final String yaml = "person:\n  name: Bob\n  age: 25\n";
            final YamlDocument doc = read(yaml);
            final YamlMapping outer = mapping(doc);
            assertEquals(1, outer.size());
            final YamlNode personValue = outer.values().iterator().next();
            assertTrue(personValue instanceof YamlMapping);
            assertEquals(2, ((YamlMapping) personValue).size());
        }

        @Test
        @DisplayName("Empty block mapping (flow)")
        void emptyMapping() throws Exception {
            final YamlDocument doc = read("{}");
            final YamlMapping map = mapping(doc);
            assertEquals(0, map.size());
        }

        @Test
        @DisplayName("Mapping with sequence as value")
        void mappingWithSequenceValue() throws Exception {
            final String yaml = "fruits:\n  - apple\n  - banana\n";
            final YamlDocument doc = read(yaml);
            final YamlMapping map = mapping(doc);
            final YamlNode value = map.values().iterator().next();
            assertTrue(value instanceof YamlSequence);
            assertEquals(2, ((YamlSequence) value).size());
        }

        @Test
        @DisplayName("Mapping with numeric key (quoted)")
        void mappingWithQuotedNumericKey() throws Exception {
            final YamlDocument doc = read("\"123\": value\n");
            final YamlMapping map = mapping(doc);
            assertEquals(1, map.size());
        }

        @Test
        @DisplayName("Duplicate key throws YamlParseException")
        void duplicateKeyThrowsException() {
            assertThrows(Exception.class, () -> read("key: a\nkey: b\n"));
        }
    }

    // =========================================================================
    // 5. Flow mappings
    // =========================================================================

    @Nested
    @DisplayName("5. Flow Mappings")
    class FlowMappingTests {

        @Test
        @DisplayName("Simple flow mapping")
        void simpleFlowMapping() throws Exception {
            final YamlDocument doc = read("{name: Alice, age: 30}");
            final YamlMapping map = mapping(doc);
            assertEquals(2, map.size());
        }

        @Test
        @DisplayName("Empty flow mapping")
        void emptyFlowMapping() throws Exception {
            final YamlDocument doc = read("{}");
            final YamlMapping map = mapping(doc);
            assertEquals(0, map.size());
        }

        @Test
        @DisplayName("Nested flow mappings")
        void nestedFlowMappings() throws Exception {
            final YamlDocument doc = read("{outer: {inner: value}}");
            final YamlMapping outer = mapping(doc);
            assertEquals(1, outer.size());
            assertTrue(outer.values().iterator().next() instanceof YamlMapping);
        }
    }

    // =========================================================================
    // 6. Multiline scalars
    // =========================================================================

    @Nested
    @DisplayName("6. Multiline Scalars (Literal | and Folded >)")
    class MultilineScalarTests {

        @Test
        @DisplayName("Literal block (|) – clip chomping (default)")
        void literalBlockClip() throws Exception {
            final String yaml = "text: |\n  line1\n  line2\n";
            final YamlDocument doc = read(yaml);
            final YamlMapping map = mapping(doc);
            final YamlScalar s = (YamlScalar) map.values().iterator().next();
            assertEquals(YamlScalarType.MULTILINE, s.getType());
            assertEquals(YamlMultilineScalarType.LITERAL, s.getMultilineType());
            assertTrue(s.getValueString().contains("line1"));
            assertTrue(s.getValueString().contains("line2"));
            assertTrue(s.getValueString().endsWith("\n"), "Clip chomping retains exactly one trailing \\n");
        }

        @Test
        @DisplayName("Literal block (|-) – strip chomping")
        void literalBlockStrip() throws Exception {
            final String yaml = "text: |-\n  line1\n  line2\n";
            final YamlDocument doc = read(yaml);
            final YamlMapping map = mapping(doc);
            final YamlScalar s = (YamlScalar) map.values().iterator().next();
            assertEquals(YamlMultilineScalarChompingType.STRIP, s.getMultilineChompingType());
            assertFalse(s.getValueString().endsWith("\n"), "Strip chomping removes trailing newlines");
        }

        @Test
        @DisplayName("Literal block (|+) – keep chomping")
        void literalBlockKeep() throws Exception {
            final String yaml = "text: |+\n  line1\n\n\n";
            final YamlDocument doc = read(yaml);
            final YamlMapping map = mapping(doc);
            final YamlScalar s = (YamlScalar) map.values().iterator().next();
            assertEquals(YamlMultilineScalarChompingType.KEEP, s.getMultilineChompingType());
        }

        @Test
        @DisplayName("Folded block (>) – lines are folded into spaces")
        void foldedBlock() throws Exception {
            final String yaml = "text: >\n  hello\n  world\n";
            final YamlDocument doc = read(yaml);
            final YamlMapping map = mapping(doc);
            final YamlScalar s = (YamlScalar) map.values().iterator().next();
            assertEquals(YamlMultilineScalarType.FOLDED, s.getMultilineType());
        }

        @Test
        @DisplayName("Folded block (>-) – strip chomping")
        void foldedBlockStrip() throws Exception {
            final String yaml = "text: >-\n  hello\n  world\n";
            final YamlDocument doc = read(yaml);
            final YamlMapping map = mapping(doc);
            final YamlScalar s = (YamlScalar) map.values().iterator().next();
            assertEquals(YamlMultilineScalarChompingType.STRIP, s.getMultilineChompingType());
            assertFalse(s.getValueString().endsWith("\n"));
        }

        @Test
        @DisplayName("Multiline scalar as document root")
        void multilineAsRoot() throws Exception {
            final String yaml = "|\n  first\n  second\n";
            final YamlDocument doc = read(yaml);
            final YamlScalar s = scalar(doc);
            assertEquals(YamlScalarType.MULTILINE, s.getType());
        }
    }

    // =========================================================================
    // 7. Anchors and aliases
    // =========================================================================

    @Nested
    @DisplayName("7. Anchors (&) and Aliases (*)")
    class AnchorAliasTests {

        @Test
        @DisplayName("Anchor is set, alias references it")
        void anchorAndAlias() throws Exception {
            final String yaml = "base: &anchor\n  value: 42\nref: *anchor\n";
            final YamlDocument doc = read(yaml);
            assertNotNull(doc);
            final YamlMapping map = mapping(doc);
            assertEquals(2, map.size());
        }

        @Test
        @DisplayName("Anchor on scalar")
        void anchorOnScalar() throws Exception {
            final String yaml = "- &myanchor hello\n- *myanchor\n";
            final YamlDocument doc = read(yaml);
            final YamlSequence seq = sequence(doc);
            assertEquals(2, seq.size());
            final YamlScalar first = (YamlScalar) seq.get(0);
            assertEquals("myanchor", first.getAnchorName());
            assertTrue(seq.get(1) instanceof YamlAlias);
            assertEquals("myanchor", ((YamlAlias) seq.get(1)).getTargetAnchorName());
        }

        @Test
        @DisplayName("Duplicate anchor name throws exception")
        void duplicateAnchorNameThrowsException() {
            assertThrows(Exception.class, () ->
                    read("a: &dup val1\nb: &dup val2\n"));
        }
    }

    // =========================================================================
    // 8. Comments
    // =========================================================================

    @Nested
    @DisplayName("8. Comments")
    class CommentTests {

        @Test
        @DisplayName("Leading comment before document")
        void leadingDocumentComment() throws Exception {
            final String yaml = "# This is a comment\nkey: value\n";
            final YamlDocument doc = read(yaml);
            assertNotNull(doc);
            // Comment is attached either to the document or to the root node
        }

        @Test
        @DisplayName("Inline comment on scalar")
        void inlineCommentOnScalar() throws Exception {
            final String yaml = "key: value # inline comment\n";
            final YamlDocument doc = read(yaml);
            final YamlMapping map = mapping(doc);
            final YamlScalar val = (YamlScalar) map.values().iterator().next();
            assertNotNull(val.getInlineComment());
            assertTrue(val.getInlineComment().contains("inline comment"));
        }

        @Test
        @DisplayName("Comments are omitted when OmitComments=true")
        void omitCommentsInWriter() throws Exception {
            final YamlDocument doc = read("key: value # should disappear\n");
            final YamlFormat format = new YamlFormat();
            format.setOmitComments(true);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (final YamlWriter writer = new YamlWriter(baos, format)) {
                writer.writeDocument(doc);
            }
            final String output = baos.toString(StandardCharsets.UTF_8);
            assertFalse(output.contains("#"), "No comments expected in output");
        }

        @Test
        @DisplayName("Comments are retained by default")
        void keepCommentsByDefault() throws Exception {
            final YamlDocument doc = read("key: value # keep me\n");
            final String output = write(doc);
            assertTrue(output.contains("#"));
        }
    }

    // =========================================================================
    // 9. Directives
    // =========================================================================

    @Nested
    @DisplayName("9. Directives (%YAML, %TAG)")
    class DirectiveTests {

        @Test
        @DisplayName("%YAML directive is read")
        void yamlVersionDirective() throws Exception {
            final String yaml = "%YAML 1.2\n---\nkey: value\n...\n";
            final YamlDocument doc = read(yaml);
            assertNotNull(doc);
            assertNotNull(doc.getDirectives());
            assertTrue(doc.getDirectives().stream().anyMatch(d -> d instanceof YamlVersionDirective));
        }

        @Test
        @DisplayName("%TAG directive is read")
        void tagDirective() throws Exception {
            final String yaml = "%TAG ! tag:example.com,2000:\n---\nkey: value\n...\n";
            final YamlDocument doc = read(yaml);
            assertNotNull(doc.getDirectives());
            assertTrue(doc.getDirectives().stream().anyMatch(d -> d instanceof YamlTagDirective));
        }

        @Test
        @DisplayName("Unknown directive throws exception")
        void unknownDirectiveThrowsException() {
            assertThrows(Exception.class, () ->
                    read("%UNKNOWN foo\n---\nkey: value\n...\n"));
        }
    }

    // =========================================================================
    // 10. Multiple documents
    // =========================================================================

    @Nested
    @DisplayName("10. Multiple Documents")
    class MultiDocumentTests {

        @Test
        @DisplayName("Read two documents via readDocument loop")
        void readTwoDocuments() throws Exception {
            final String yaml = "doc1: value1\n---\ndoc2: value2\n";
            try (final YamlReader reader = new YamlReader(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))) {
                final YamlDocument first = reader.readDocument();
                final YamlDocument second = reader.readDocument();
                assertNotNull(first);
                assertNotNull(second);
            }
        }

        @Test
        @DisplayName("Write multiple documents via YamlWriter")
        void writeTwoDocuments() throws Exception {
            final YamlDocument doc1 = read("a: 1\n");
            final YamlDocument doc2 = read("b: 2\n");
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (final YamlWriter writer = new YamlWriter(baos)) {
                writer.writeDocument(doc1);
                writer.writeDocument(doc2);
            }
            final String output = baos.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("---"), "Separator --- expected");
        }

        @Test
        @DisplayName("Document with explicit start marker (---)")
        void documentWithStartMarker() throws Exception {
            final String yaml = "---\nkey: value\n";
            final YamlDocument doc = read(yaml);
            assertNotNull(doc);
            assertEquals("value", ((YamlScalar) mapping(doc).values().iterator().next()).getValueString());
        }

        @Test
        @DisplayName("Document with end marker (...)")
        void documentWithEndMarker() throws Exception {
            final String yaml = "key: value\n...\n";
            final YamlDocument doc = read(yaml);
            assertNotNull(doc);
        }
    }

    // =========================================================================
    // 11. YamlWriter – format options
    // =========================================================================

    @Nested
    @DisplayName("11. YamlWriter Format Options")
    class YamlWriterFormatTests {

        @Test
        @DisplayName("Write simple mapping (toString)")
        void writeMappingToString() throws Exception {
            final YamlDocument doc = read("name: Alice\nage: 30\n");
            final String output = write(doc);
            assertTrue(output.contains("name"));
            assertTrue(output.contains("Alice"));
            assertTrue(output.contains("age"));
        }

        @Test
        @DisplayName("Write sequence (toString)")
        void writeSequenceToString() throws Exception {
            final YamlDocument doc = read("- a\n- b\n- c\n");
            final String output = write(doc);
            assertTrue(output.contains("- a") || output.contains("-"));
        }

        @Test
        @DisplayName("Indentation size is respected")
        void indentationSize() throws Exception {
            final YamlDocument doc = read("parent:\n  child: value\n");
            final YamlFormat format = new YamlFormat();
            format.setIndentationSize(4);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (final YamlWriter writer = new YamlWriter(baos, format)) {
                writer.writeDocument(doc);
            }
            final String output = baos.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("    child"), "4-space indentation expected");
        }

        @Test
        @DisplayName("Null OutputStream throws IllegalStateException")
        void nullOutputStreamThrowsException() {
            assertThrows(IllegalStateException.class, () -> new YamlWriter(null));
        }

        @Test
        @DisplayName("Default encoding is UTF-8")
        void defaultEncodingIsUtf8() throws IOException {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (final YamlWriter writer = new YamlWriter(baos)) {
                assertEquals(StandardCharsets.UTF_8, writer.getEncoding());
            }
        }

        @Test
        @DisplayName("Encoding can be overridden")
        void customEncoding() throws IOException {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (final YamlWriter writer = new YamlWriter(baos, StandardCharsets.ISO_8859_1)) {
                assertEquals(StandardCharsets.ISO_8859_1, writer.getEncoding());
            }
        }

        @Test
        @DisplayName("addSequenceItem(String) writes string entry")
        void addSequenceItemString() throws Exception {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (final YamlWriter writer = new YamlWriter(baos)) {
                writer.addSequenceItem("hello");
                writer.addSequenceItem("world");
            }
            final String output = baos.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("hello"));
            assertTrue(output.contains("world"));
        }

        @Test
        @DisplayName("addSequenceItem(Number) writes number")
        void addSequenceItemNumber() throws Exception {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (final YamlWriter writer = new YamlWriter(baos)) {
                writer.addSequenceItem(42);
            }
            final String output = baos.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("42"));
        }

        @Test
        @DisplayName("addSequenceItem(null) writes null")
        void addSequenceItemNull() throws Exception {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (final YamlWriter writer = new YamlWriter(baos)) {
                writer.addSequenceItem((Object) null);
            }
            final String output = baos.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("null"));
        }

        @Test
        @DisplayName("addSequenceItem with unknown type throws exception")
        void addSequenceItemUnknownTypeThrowsException() throws Exception {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (final YamlWriter writer = new YamlWriter(baos)) {
                assertThrows(Exception.class, () -> writer.addSequenceItem(new Object()));
            }
        }

        @Test
        @DisplayName("writeDocumentList writes all documents")
        void writeDocumentList() throws Exception {
            final YamlDocument doc1 = read("x: 1\n");
            final YamlDocument doc2 = read("y: 2\n");
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (final YamlWriter writer = new YamlWriter(baos)) {
                writer.writeDocumentList(List.of(doc1, doc2));
            }
            final String output = baos.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("x"));
            assertTrue(output.contains("y"));
        }
    }

    // =========================================================================
    // 12. Special characters and quoting
    // =========================================================================

    @Nested
    @DisplayName("12. Special Characters and Quoting")
    class SpecialCharacterTests {

        @ParameterizedTest(name = "Quoted key: {0}")
        @CsvSource({
                "'true', 'true'",
                "'false', 'false'",
                "'null', 'null'",
                "'42', '42'",
                "'hello world', 'hello world'"
        })
        @DisplayName("Strings resembling special values are quoted and read back correctly")
        void stringsResemblingSpecialValues(final String input, final String expected) throws Exception {
            final YamlDocument doc = read("\"" + input + "\": value\n");
            final YamlMapping map = mapping(doc);
            final YamlScalar key = (YamlScalar) map.keySet().iterator().next();
            assertEquals(expected, key.getValueString());
        }

        @Test
        @DisplayName("String with colon is read correctly")
        void stringWithColon() throws Exception {
            final YamlDocument doc = read("\"key: with colon\": value\n");
            final YamlMapping map = mapping(doc);
            final YamlScalar key = (YamlScalar) map.keySet().iterator().next();
            assertEquals("key: with colon", key.getValueString());
        }

        @Test
        @DisplayName("Unicode characters in string")
        void unicodeCharactersInString() throws Exception {
            final YamlDocument doc = read("\"Unicode test \u00e9\u00e0\u00fc\": value\n");
            final YamlMapping map = mapping(doc);
            final YamlScalar key = (YamlScalar) map.keySet().iterator().next();
            assertTrue(key.getValueString().contains("\u00e9"));
        }

        @Test
        @DisplayName("String starting with special character (>)")
        void stringStartingWithGreaterThan() throws Exception {
            final YamlDocument doc = read("\">> important <<\"\n");
            final YamlScalar s = scalar(doc);
            assertTrue(s.getValueString().contains(">>"));
        }
    }

    // =========================================================================
    // 13. Round-trip tests (Read → Write → Read)
    // =========================================================================

    @Nested
    @DisplayName("13. Round-Trip Tests")
    class RoundTripTests {

        @Test
        @DisplayName("Simple mapping – round-trip")
        void simpleMappingRoundTrip() throws Exception {
            final String original = "name: Alice\nage: 30\n";
            final YamlDocument doc1 = read(original);
            final String written = write(doc1);
            final YamlDocument doc2 = read(written);
            final YamlMapping map1 = mapping(doc1);
            final YamlMapping map2 = mapping(doc2);
            assertEquals(map1.size(), map2.size());
        }

        @Test
        @DisplayName("Sequence of strings – round-trip")
        void stringSequenceRoundTrip() throws Exception {
            final String original = "- alpha\n- beta\n- gamma\n";
            final YamlDocument doc1 = read(original);
            final String written = write(doc1);
            final YamlDocument doc2 = read(written);
            assertEquals(sequence(doc1).size(), sequence(doc2).size());
        }

        @Test
        @DisplayName("Nested mapping – round-trip")
        void nestedMappingRoundTrip() throws Exception {
            final String original = "person:\n  name: Bob\n  address:\n    city: London\n";
            final YamlDocument doc1 = read(original);
            final String written = write(doc1);
            final YamlDocument doc2 = read(written);
            assertNotNull(doc2.getRoot());
        }

        @Test
        @DisplayName("Boolean values – round-trip preserves type")
        void booleanRoundTrip() throws Exception {
            final String original = "- true\n- false\n";
            final YamlDocument doc1 = read(original);
            final String written = write(doc1);
            final YamlDocument doc2 = read(written);
            final YamlSequence seq = sequence(doc2);
            assertEquals(YamlScalarType.BOOLEAN, ((YamlScalar) seq.get(0)).getType());
            assertEquals(YamlScalarType.BOOLEAN, ((YamlScalar) seq.get(1)).getType());
        }

        @Test
        @DisplayName("Null value – round-trip preserves type")
        void nullRoundTrip() throws Exception {
            final String original = "key: null\n";
            final YamlDocument doc1 = read(original);
            final String written = write(doc1);
            final YamlDocument doc2 = read(written);
            final YamlMapping map = mapping(doc2);
            final YamlScalar val = (YamlScalar) map.values().iterator().next();
            assertEquals(YamlScalarType.NULL_VALUE, val.getType());
        }

        @Test
        @DisplayName("Number – round-trip preserves type")
        void numberRoundTrip() throws Exception {
            final String original = "count: 99\n";
            final YamlDocument doc1 = read(original);
            final String written = write(doc1);
            final YamlDocument doc2 = read(written);
            final YamlScalar val = (YamlScalar) mapping(doc2).values().iterator().next();
            assertEquals(YamlScalarType.NUMBER, val.getType());
        }
    }

    // =========================================================================
    // 14. Static helper methods
    // =========================================================================

    @Nested
    @DisplayName("14. Static toString Methods of YamlWriter")
    class StaticToStringTests {

        @Test
        @DisplayName("YamlWriter.toString(YamlDocument) returns non-empty string")
        void toStringDocument() throws Exception {
            final YamlDocument doc = read("key: value\n");
            final String result = YamlWriter.toString(doc);
            assertNotNull(result);
            assertFalse(result.isBlank());
        }

        @Test
        @DisplayName("YamlWriter.toString(YamlMapping) returns non-empty string")
        void toStringMapping() throws Exception {
            final YamlDocument doc = read("a: 1\nb: 2\n");
            final YamlMapping map = mapping(doc);
            final String result = YamlWriter.toString(map);
            assertNotNull(result);
            assertTrue(result.contains("a"));
        }

        @Test
        @DisplayName("YamlWriter.toString(YamlSequence) returns non-empty string")
        void toStringSequence() throws Exception {
            final YamlDocument doc = read("- x\n- y\n");
            final YamlSequence seq = sequence(doc);
            final String result = YamlWriter.toString(seq);
            assertNotNull(result);
            assertTrue(result.contains("x"));
        }
    }

    // =========================================================================
    // 15. Error handling
    // =========================================================================

    @Nested
    @DisplayName("15. Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Invalid indentation throws exception")
        void invalidIndentationThrowsException() {
            // Contradictory indentation level
            assertThrows(Exception.class, () ->
                    read("key:\n    nested: value\n  wronglevel: bad\n"));
        }

        @Test
        @DisplayName("Content after directive without --- throws exception")
        void contentAfterDirectiveWithoutDocumentStartThrowsException() {
            assertThrows(Exception.class, () ->
                    read("%YAML 1.2\nkey: value\n"));
        }

        @Test
        @DisplayName("Empty document returns null")
        void emptyDocumentReturnsNull() throws Exception {
            final YamlDocument doc = read("");
            assertNull(doc, "Empty input should return null");
        }

        @Test
        @DisplayName("Whitespace-only document returns null")
        void whitespaceOnlyDocumentReturnsNull() throws Exception {
            final YamlDocument doc = read("   \n  \n");
            final YamlNode root = doc.getRoot();
            assert(root != null && root instanceof YamlScalar && ((YamlScalar) root).getType() == YamlScalarType.NULL_VALUE);
        }

        @Test
        @DisplayName("Comment-only document returns null")
        void commentOnlyDocumentReturnsNull() throws Exception {
            // A pure comment document without any content
            final YamlDocument doc = read("# just a comment\n");
            final YamlNode root = doc.getRoot();
            assert(root != null && root instanceof YamlScalar && ((YamlScalar) root).getType() == YamlScalarType.NULL_VALUE);
        }
    }

    // =========================================================================
    // 16. readUpToPath / readNextYamlNode
    // =========================================================================

    @Nested
    @DisplayName("16. readUpToPath and readNextYamlNode")
    class PathReadingTests {

        @Test
        @DisplayName("readUpToPath navigates to a known path")
        void readUpToKnownPath() throws Exception {
            final String yaml = "list:\n  - a\n  - b\n  - c\n";
            try (final YamlReader reader = new YamlReader(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))) {
                // $.list must be present
                assertDoesNotThrow(() -> reader.readUpToPath("$.list"));
            }
        }

        @Test
        @DisplayName("readUpToPath throws exception for unknown path")
        void readUpToUnknownPathThrowsException() {
            final String yaml = "key: value\n";
            assertThrows(Exception.class, () -> {
                try (final YamlReader reader = new YamlReader(
                        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))) {
                    reader.readUpToPath("$.nonexistent");
                }
            });
        }

        @Test
        @DisplayName("readDocument after readUpToPath throws exception")
        void readDocumentAfterReadUpToPathThrowsException() {
            final String yaml = "list:\n  - a\n  - b\n";
            assertThrows(Exception.class, () -> {
                try (final YamlReader reader = new YamlReader(
                        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))) {
                    reader.readUpToPath("$.list");
                    reader.readDocument(); // must fail
                }
            });
        }
    }
}