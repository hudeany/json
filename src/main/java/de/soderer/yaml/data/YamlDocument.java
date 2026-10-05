package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.List;

import de.soderer.yaml.YamlWriter;
import de.soderer.yaml.data.directive.YamlDirective;

// TODO: Search method for YAML anchor to use when finding YAML references
/**
 * YAML document: optional directives (%YAML, %TAG), leading comments and the root node.
 */
public class YamlDocument {
	/**
	 * Directives of the document, null if there are none.
	 */
	private List<YamlDirective<?>> directives = null;
	/**
	 * Comment lines at the document start, null if there are none.
	 */
	private List<String> leadingComments = null;
	/**
	 * Root node of the document.
	 */
	private YamlNode root;

	/**
	 * Creates an empty document.
	 */
	public YamlDocument() {
	}

	/**
	 * Creates a document with a root node.
	 *
	 * @param root
	 *            the root node
	 */
	public YamlDocument(final YamlNode root) {
		this.root = root;
	}

	/**
	 * Adds a directive.
	 *
	 * @param directive
	 *            the directive
	 * @return this document for chaining
	 */
	public YamlDocument addDirective(final YamlDirective<?> directive) {
		if (directives == null) {
			directives = new ArrayList<>();
		}
		directives.add(directive);
		return this;
	}

	/**
	 * Returns the directives.
	 *
	 * @return the directives, or null if there are none
	 */
	public List<YamlDirective<?>> getDirectives() {
		return directives;
	}

	/**
	 * Adds a comment line at the document start.
	 *
	 * @param comment
	 *            the comment text without "#", null and empty texts are ignored
	 * @return this document for chaining
	 */
	public YamlDocument addLeadingComment(final String comment) {
		if (comment != null && !comment.isEmpty()) {
			if (leadingComments == null) {
				leadingComments = new ArrayList<>();
			}
			leadingComments.add(comment);
		}
		return this;
	}

	/**
	 * Returns the comment lines at the document start.
	 *
	 * @return the comment lines without "#", or null if there are none
	 */
	public List<String> getLeadingComments() {
		return leadingComments;
	}

	/**
	 * Returns the root node.
	 *
	 * @return the root node, null for an empty document
	 */
	public YamlNode getRoot() {
		return root;
	}

	/**
	 * Sets the root node.
	 *
	 * @param root
	 *            the root node
	 */
	public void setRoot(final YamlNode root) {
		this.root = root;
	}

	/**
	 * Sets the root node.
	 *
	 * @param newRoot
	 *            the root node
	 * @return this document for chaining
	 */
	public YamlDocument withRoot(final YamlNode newRoot) {
		setRoot(newRoot);
		return this;
	}

	/**
	 * Returns this document as YAML text in default format.
	 */
	@Override
	public String toString() {
		try {
			return YamlWriter.toString(this);
		} catch (final Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
