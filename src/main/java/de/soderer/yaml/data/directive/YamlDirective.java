package de.soderer.yaml.data.directive;

import java.util.ArrayList;
import java.util.List;

public class YamlDirective<T extends YamlDirective<T>> {
	private final List<String> leadingComments = new ArrayList<>();
	private String inlineComment;

	public List<String> getLeadingComments() {
		return leadingComments;
	}

	public YamlDirective<T> addLeadingComment(final String comment) {
		if (comment != null && !comment.isEmpty()) {
			leadingComments.add(comment);
		}
		return this;
	}

	public String getInlineComment() {
		return inlineComment;
	}

	public void setInlineComment(final String inlineComment) {
		this.inlineComment = inlineComment;
	}

	public YamlDirective<T> withInlineComment(final String newInlineComment) {
		setInlineComment(newInlineComment);
		return this;
	}
}
