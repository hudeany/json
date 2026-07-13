package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.List;

public abstract class YamlNode {
	private List<String> leadingComments = null;
	private String inlineComment = null;
	private String anchorName;
	private int leadingEmptyLinesCount = 0;
	private int postCommentEmptyLinesCount = 0;

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

	/**
	 * Number of empty lines (lines containing only whitespace characters, if any) that were
	 * found directly before this node's leading comments (or directly before the node itself,
	 * if it has no leading comments) in the parsed YAML document.
	 */
	public int getLeadingEmptyLinesCount() {
		return leadingEmptyLinesCount;
	}

	public YamlNode setLeadingEmptyLinesCount(final int leadingEmptyLinesCount) {
		this.leadingEmptyLinesCount = leadingEmptyLinesCount;
		return this;
	}

	/**
	 * Number of empty lines found after this node's leading comments but still before the node
	 * itself. Only meaningful when this node also has leading comments.
	 */
	public int getPostCommentEmptyLinesCount() {
		return postCommentEmptyLinesCount;
	}

	public YamlNode setPostCommentEmptyLinesCount(final int postCommentEmptyLinesCount) {
		this.postCommentEmptyLinesCount = postCommentEmptyLinesCount;
		return this;
	}

	@Override
	public abstract boolean equals(Object otherObject);

	@Override
	public abstract int hashCode();
}