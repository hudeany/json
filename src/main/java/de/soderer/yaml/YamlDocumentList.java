package de.soderer.yaml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class YamlDocumentList extends YamlObject<YamlDocumentList> implements Iterable<YamlDocument> {
	private final List<YamlDocument> documents = new ArrayList<>();

	public List<YamlDocument> getDocuments() {
		return documents;
	}

	@Override
	public Object getValue() {
		return documents;
	}

	@Override
	public YamlDocumentList setValue(final Object document) {
		if (document != null && document instanceof YamlDocument) {
			documents.add((YamlDocument) document);
		}
		return this;
	}

	public YamlDocumentList add(YamlDocument document) throws Exception {
		if (document != null) {
			documents.add(document);
		}
		return this;
	}

	@Override
	public Iterator<YamlDocument> iterator() {
		return documents.iterator();
	}

	public Stream<YamlDocument> stream() {
		return documents.stream();
	}
}
