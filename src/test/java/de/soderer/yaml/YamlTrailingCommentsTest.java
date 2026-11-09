package de.soderer.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.soderer.yaml.data.YamlDocument;

/**
 * Tests comments outside of documents: {@link YamlReader#getTrailingComments()} and
 * {@link YamlWriter#writeTrailingComments(List)}.
 */
@SuppressWarnings("static-method")
@DisplayName("Comments after the last document")
public class YamlTrailingCommentsTest {
	private static final class ReadResult {
		private final List<YamlDocument> documents = new ArrayList<>();
		private List<String> trailingComments;
	}

	private static ReadResult readAll(final String yaml) throws Exception {
		final ReadResult result = new ReadResult();
		try (YamlReader reader = new YamlReader(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))) {
			YamlDocument document;
			while ((document = reader.readDocument()) != null) {
				result.documents.add(document);
			}
			result.trailingComments = reader.getTrailingComments();
		}
		return result;
	}

	private static String writeAll(final ReadResult data, final YamlFormat yamlFormat) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (YamlWriter writer = new YamlWriter(outputStream, yamlFormat)) {
			writer.writeDocumentList(data.documents);
			writer.writeTrailingComments(data.trailingComments);
		}
		return outputStream.toString(StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("Data consisting of comments only has no document, but trailing comments")
	void commentsOnly() throws Exception {
		final ReadResult result = readAll("# first\n\n  # second\n");
		assertTrue(result.documents.isEmpty());
		assertEquals(List.of(" first", " second"), result.trailingComments);
		assertEquals("# first\n# second\n", writeAll(result, new YamlFormat()));
	}

	@Test
	@DisplayName("Empty data has neither documents nor trailing comments")
	void emptyData() throws Exception {
		final ReadResult result = readAll("");
		assertTrue(result.documents.isEmpty());
		assertTrue(result.trailingComments.isEmpty());
	}

	@Test
	@DisplayName("Comments after the last document do not belong to it")
	void commentsAfterLastDocument() throws Exception {
		final ReadResult result = readAll("# leading\na: 1\n# end 1\n\n# end 2\n");
		assertEquals(1, result.documents.size());
		assertEquals(List.of(" end 1", " end 2"), result.trailingComments);

		final String written = writeAll(result, new YamlFormat());
		assertEquals("# leading\na: 1\n# end 1\n# end 2\n", written);
		final ReadResult reread = readAll(written);
		assertEquals(result.trailingComments, reread.trailingComments);
	}

	@Test
	@DisplayName("Comments after a document end marker")
	void commentsAfterDocumentEndMarker() throws Exception {
		final ReadResult result = readAll("a: 1\n... # marker comment\n# end\n");
		assertEquals(1, result.documents.size());
		assertEquals(List.of(" marker comment", " end"), result.trailingComments);
	}

	@Test
	@DisplayName("Comments between documents belong to the following document")
	void commentsBetweenDocuments() throws Exception {
		final ReadResult result = readAll("a: 1\n# between\n---\nb: 2\n# end\n");
		assertEquals(2, result.documents.size());
		assertEquals(List.of(" between"), result.documents.get(1).getLeadingComments());
		assertEquals(List.of(" end"), result.trailingComments);
	}

	@Test
	@DisplayName("Trailing comments are available after a single readDocument() call at the end of the data")
	void trailingCommentsAfterSingleRead() throws Exception {
		try (YamlReader reader = new YamlReader(new ByteArrayInputStream("a: 1\n# end\n".getBytes(StandardCharsets.UTF_8)))) {
			assertNotNull(reader.readDocument());
			assertEquals(List.of(" end"), reader.getTrailingComments());
			assertNull(reader.readDocument());
			assertEquals(List.of(" end"), reader.getTrailingComments());
		}
	}

	@Test
	@DisplayName("Comments before a following document are no trailing comments")
	void noTrailingCommentsBeforeFurtherDocument() throws Exception {
		try (YamlReader reader = new YamlReader(new ByteArrayInputStream("a: 1\n# next\n---\nb: 2\n".getBytes(StandardCharsets.UTF_8)))) {
			assertNotNull(reader.readDocument());
			assertTrue(reader.getTrailingComments().isEmpty());
			final YamlDocument secondDocument = reader.readDocument();
			assertEquals(List.of(" next"), secondDocument.getLeadingComments());
			assertNull(reader.readDocument());
			assertTrue(reader.getTrailingComments().isEmpty());
		}
	}

	@Test
	@DisplayName("Trailing comments after several documents are written after the last document end marker")
	void trailingCommentsAfterSeveralDocuments() throws Exception {
		final ReadResult result = readAll("a: 1\n---\nb: 2\n# end\n");
		final String written = writeAll(result, new YamlFormat());
		assertEquals("a: 1\n...\n---\nb: 2\n...\n# end\n", written);
		assertEquals(List.of(" end"), readAll(written).trailingComments);
	}

	@Test
	@DisplayName("Trailing comments are not written, if comments are omitted")
	void trailingCommentsOmitted() throws Exception {
		final ReadResult result = readAll("a: 1\n# end\n");
		assertEquals("a: 1\n", writeAll(result, new YamlFormat().withOmitComments(true)));
	}
}
