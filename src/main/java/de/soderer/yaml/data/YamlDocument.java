package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.List;

import de.soderer.yaml.YamlWriter;
import de.soderer.yaml.data.directive.YamlDirective;

public class YamlDocument {
	private List<YamlDirective<?>> directives = null;
	private List<String> leadingComments = null;
	private YamlNode root;

	public YamlDocument() {
	}

	public YamlDocument(final YamlNode root) {
		this.root = root;
	}

	public void addDirective(final YamlDirective<?> directive) {
		if (directives == null) {
			directives = new ArrayList<>();
		}
		directives.add(directive);
	}

	public List<YamlDirective<?>> getDirectives() {
		return directives;
	}

	public void addLeadingComment(final String comment) {
		if (comment != null && !comment.isEmpty()) {
			if (leadingComments == null) {
				leadingComments = new ArrayList<>();
			}
			leadingComments.add(comment);
		}
	}

	public List<String> getLeadingComments() {
		return leadingComments;
	}

	public YamlNode getRoot() {
		return root;
	}

	public void setRoot(final YamlNode root) {
		this.root = root;
	}

	@Override
	public String toString() {
		try {
			return YamlWriter.toString(this);
		} catch (final Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
