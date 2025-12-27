package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.List;

public abstract class YamlNode {
	private final List<String> leadingComments = new ArrayList<>();

	private final List<String> inlineComments = new ArrayList<>();

	private String anchorName;

	public List<String> getLeadingComments() {
		return leadingComments;
	}

	public void addLeadingComment(final String comment) {
		if (comment != null && !comment.isEmpty()) {
			leadingComments.add(comment);
		}
	}

	public List<String> getInlineComments() {
		return inlineComments;
	}

	public void addInlineComment(final String comment) {
		if (comment != null && !comment.isEmpty()) {
			inlineComments.add(comment);
		}
	}

	public String getAnchorName() {
		return anchorName;
	}

	public void setAnchorName(final String anchorName) {
		this.anchorName = anchorName;
	}

	@Override
	public abstract boolean equals(Object otherObject);

	@Override
	public abstract int hashCode();
}
