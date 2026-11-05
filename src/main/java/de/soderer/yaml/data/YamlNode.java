package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class of all YAML nodes: mappings, sequences, scalars and aliases.
 * <p>
 * Besides the data a node keeps the presentation details read from or written to YAML text:
 * leading comments (the lines before the node), an inline comment (after the node on the same
 * line), an anchor name and the number of empty lines before the node.
 * </p>
 */
public abstract class YamlNode {
	/**
	 * Creates a node without comments and anchor.
	 */
	protected YamlNode() {
		// Presentation details are set by the setters
	}

	/**
	 * Comment lines before the node, without "#".
	 */
	private List<String> leadingComments = null;
	/**
	 * Comment after the node on the same line, without "#".
	 */
	private String inlineComment = null;
	/**
	 * Anchor name ("&amp;name") of this node, referenced by aliases.
	 */
	private String anchorName;
	/**
	 * Tag of this node that is not resolved to a datatype, see {@link #getTag()}.
	 */
	private String tag = null;
	/**
	 * Empty lines before the leading comments.
	 */
	private int leadingEmptyLinesCount = 0;
	/**
	 * Empty lines between the leading comments and the node.
	 */
	private int postCommentEmptyLinesCount = 0;

	/**
	 * Returns the comment lines before the node.
	 *
	 * @return the comment lines without "#", or null if there are none
	 */
	public List<String> getLeadingComments() {
		return leadingComments;
	}

	/**
	 * Adds a comment line before the node.
	 *
	 * @param comment
	 *            the comment text without "#", null is ignored
	 * @return this node for chaining
	 */
	public YamlNode addLeadingComment(final String comment) {
		if (comment != null) {
			if (leadingComments == null) {
				leadingComments = new ArrayList<>();
			}
			leadingComments.add(comment);
		}
		return this;
	}

	/**
	 * Returns the comment after the node on the same line.
	 *
	 * @return the comment text without "#", or null
	 */
	public String getInlineComment() {
		return inlineComment;
	}

	/**
	 * Sets the comment after the node on the same line.
	 *
	 * @param inlineComment
	 *            the comment text without "#", or null
	 */
	public void setInlineComment(final String inlineComment) {
		this.inlineComment = inlineComment;
	}

	/**
	 * Sets the comment after the node on the same line.
	 *
	 * @param newInlineComment
	 *            the comment text without "#", or null
	 * @return this node for chaining
	 */
	public YamlNode withInlineComment(final String newInlineComment) {
		setInlineComment(newInlineComment);
		return this;
	}

	/**
	 * Returns the anchor name of this node.
	 *
	 * @return the anchor name without "&amp;", or null
	 */
	public String getAnchorName() {
		return anchorName;
	}

	/**
	 * Sets the anchor name of this node, so aliases can refer to it.
	 *
	 * @param anchorName
	 *            the anchor name without "&amp;", or null
	 */
	public void setAnchorName(final String anchorName) {
		this.anchorName = anchorName;
	}

	/**
	 * Sets the anchor name of this node.
	 *
	 * @param newAnchorName
	 *            the anchor name without "&amp;", or null
	 * @return this node for chaining
	 */
	public YamlNode withAnchorName(final String newAnchorName) {
		setAnchorName(newAnchorName);
		return this;
	}

	/**
	 * Returns the tag of this node that is not resolved to a datatype, like a local tag "!custom", a
	 * verbatim tag "!&lt;tag:example.com,2026:point&gt;" or a named tag "!e!point". Datatype tags
	 * like "!!str" or "!!int" are applied to the node instead and are not stored.
	 *
	 * @return the tag including its leading "!", or null
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * Sets the tag of this node, see {@link #getTag()}. The value of the node is not changed by the
	 * tag.
	 *
	 * @param tag
	 *            the tag including its leading "!", or null
	 */
	public void setTag(final String tag) {
		if (tag != null && (!tag.startsWith("!") || tag.chars().anyMatch(Character::isWhitespace))) {
			throw new IllegalArgumentException("Invalid YAML tag: '" + tag + "'");
		}
		this.tag = tag;
	}

	/**
	 * Sets the tag of this node, see {@link #getTag()}.
	 *
	 * @param newTag
	 *            the tag including its leading "!", or null
	 * @return this node for chaining
	 */
	public YamlNode withTag(final String newTag) {
		setTag(newTag);
		return this;
	}

	/**
	 * Number of empty lines (lines containing only whitespace characters, if any) that were
	 * found directly before this node's leading comments (or directly before the node itself,
	 * if it has no leading comments) in the parsed YAML document.
	 *
	 * @return the number of empty lines
	 */
	public int getLeadingEmptyLinesCount() {
		return leadingEmptyLinesCount;
	}

	/**
	 * Sets the number of empty lines before the leading comments.
	 *
	 * @param leadingEmptyLinesCount
	 *            the number of empty lines
	 */
	public void setLeadingEmptyLinesCount(final int leadingEmptyLinesCount) {
		this.leadingEmptyLinesCount = leadingEmptyLinesCount;
	}

	/**
	 * Sets the number of empty lines before the leading comments.
	 *
	 * @param newLeadingEmptyLinesCount
	 *            the number of empty lines
	 * @return this node for chaining
	 */
	public YamlNode withLeadingEmptyLinesCount(final int newLeadingEmptyLinesCount) {
		setLeadingEmptyLinesCount(newLeadingEmptyLinesCount);
		return this;
	}

	/**
	 * Number of empty lines found after this node's leading comments but still before the node
	 * itself. Only meaningful when this node also has leading comments.
	 *
	 * @return the number of empty lines
	 */
	public int getPostCommentEmptyLinesCount() {
		return postCommentEmptyLinesCount;
	}

	/**
	 * Sets the number of empty lines between the leading comments and the node.
	 *
	 * @param postCommentEmptyLinesCount
	 *            the number of empty lines
	 */
	public void setPostCommentEmptyLinesCount(final int postCommentEmptyLinesCount) {
		this.postCommentEmptyLinesCount = postCommentEmptyLinesCount;
	}

	/**
	 * Sets the number of empty lines between the leading comments and the node.
	 *
	 * @param newPostCommentEmptyLinesCount
	 *            the number of empty lines
	 * @return this node for chaining
	 */
	public YamlNode withPostCommentEmptyLinesCount(final int newPostCommentEmptyLinesCount) {
		setPostCommentEmptyLinesCount(newPostCommentEmptyLinesCount);
		return this;
	}

	/**
	 * Nodes are equal by their data. Comments, anchors and empty lines are not compared.
	 */
	@Override
	public abstract boolean equals(Object otherObject);

	/**
	 * Hash code consistent with {@link #equals(Object)}.
	 */
	@Override
	public abstract int hashCode();
}