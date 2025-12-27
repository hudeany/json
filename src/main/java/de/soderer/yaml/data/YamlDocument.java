package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.List;

import de.soderer.yaml.YamlWriter;
import de.soderer.yaml.data.directive.YamlDirective;

public class YamlDocument {
	private final List<YamlDirective<?>> directives = new ArrayList<>();
	private final List<String> leadingComments = new ArrayList<>();
	private YamlNode root;

	public void addDirective(final YamlDirective<?> directive) {
		directives.add(directive);
	}

	public List<YamlDirective<?>> getDirectives() {
		return directives;
	}

	public void addLeadingComment(final String comment) {
		if (comment != null && !comment.isEmpty()) {
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
