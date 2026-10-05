package de.soderer.yaml.data.directive;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class of YAML directives ("%YAML", "%TAG") with their comments.
 *
 * @param <T>
 *            the concrete directive type
 */
public class YamlDirective<T extends YamlDirective<T>> {
	/**
	 * Creates a directive without comments.
	 */
	public YamlDirective() {
		// Comments are set by the setters
	}

	/**
	 * Comment lines before the directive, without "#".
	 */
	private final List<String> leadingComments = new ArrayList<>();
	/**
	 * Comment after the directive on the same line, without "#".
	 */
	private String inlineComment;

	/**
	 * Returns the comment lines before the directive.
	 *
	 * @return the comment lines without "#", never null
	 */
	public List<String> getLeadingComments() {
		return leadingComments;
	}

	/**
	 * Adds a comment line before the directive.
	 *
	 * @param comment
	 *            the comment text without "#", null and empty texts are ignored
	 * @return this directive for chaining
	 */
	public YamlDirective<T> addLeadingComment(final String comment) {
		if (comment != null && !comment.isEmpty()) {
			leadingComments.add(comment);
		}
		return this;
	}

	/**
	 * Returns the comment after the directive.
	 *
	 * @return the comment text without "#", or null
	 */
	public String getInlineComment() {
		return inlineComment;
	}

	/**
	 * Sets the comment after the directive.
	 *
	 * @param inlineComment
	 *            the comment text without "#", or null
	 */
	public void setInlineComment(final String inlineComment) {
		this.inlineComment = inlineComment;
	}

	/**
	 * Sets the comment after the directive.
	 *
	 * @param newInlineComment
	 *            the comment text without "#", or null
	 * @return this directive for chaining
	 */
	public YamlDirective<T> withInlineComment(final String newInlineComment) {
		setInlineComment(newInlineComment);
		return this;
	}
}
