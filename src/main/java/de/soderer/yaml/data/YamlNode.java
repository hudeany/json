package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.List;

public abstract class YamlNode {
	private List<String> leadingComments = null;
	private String inlineComment = null;
	private String anchorName;

	public List<String> getLeadingComments() {
		return leadingComments;
	}

	public YamlNode addLeadingComment(final String comment) {
		if (comment != null) {
			if (leadingComments == null) {
				leadingComments = new ArrayList<>();
			}
			leadingComments.add(comment);
		}
		return this;
	}

	public String getInlineComment() {
		return inlineComment;
	}

	public YamlNode setInlineComment(final String inlineComment) {
		this.inlineComment = inlineComment;
		return this;
	}

	public String getAnchorName() {
		return anchorName;
	}

	public YamlNode setAnchorName(final String anchorName) {
		this.anchorName = anchorName;
		return this;
	}

	@Override
	public abstract boolean equals(Object otherObject);

	@Override
	public abstract int hashCode();
}
