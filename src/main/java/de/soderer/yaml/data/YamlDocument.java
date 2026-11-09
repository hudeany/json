package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.soderer.yaml.YamlWriter;
import de.soderer.yaml.data.directive.YamlDirective;

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
	 * Returns the node with an anchor of the given name. If the anchor is defined more than once,
	 * the last definition in document order is returned, which is the one an alias at the end of
	 * the document refers to. To find the node a specific alias refers to, use
	 * {@link #resolveAlias(YamlAlias)}.
	 *
	 * @param anchorName
	 *            the anchor name without "&amp;"
	 * @return the node, or null if there is no anchor with this name
	 */
	public YamlNode getNodeByAnchor(final String anchorName) {
		if (anchorName == null) {
			return null;
		}
		final YamlNode[] result = new YamlNode[1];
		visitInDocumentOrder(root, node -> {
			if (anchorName.equals(node.getAnchorName()) && !(node instanceof YamlAlias)) {
				result[0] = node;
			}
			return true;
		});
		return result[0];
	}

	/**
	 * Returns all nodes with an anchor, which are no aliases, in document order. An anchor defined
	 * more than once is contained once per definition.
	 *
	 * @return the anchored nodes, empty list if there are none
	 */
	public List<YamlNode> getAnchoredNodes() {
		final List<YamlNode> result = new ArrayList<>();
		visitInDocumentOrder(root, node -> {
			if (node.getAnchorName() != null && !(node instanceof YamlAlias)) {
				result.add(node);
			}
			return true;
		});
		return result;
	}

	/**
	 * Returns the node an alias of this document refers to: the latest definition of its anchor
	 * before the alias in document order.
	 *
	 * @param alias
	 *            an alias node contained in this document (identified by identity)
	 * @return the referenced node, or null if the alias is not part of this document or its anchor
	 *         is not defined before it
	 */
	public YamlNode resolveAlias(final YamlAlias alias) {
		Objects.requireNonNull(alias, "Alias must not be null");
		final YamlNode[] latestDefinition = new YamlNode[1];
		final boolean[] aliasFound = new boolean[1];
		visitInDocumentOrder(root, node -> {
			if (node == alias) {
				aliasFound[0] = true;
				return false;
			} else if (alias.getTargetAnchorName().equals(node.getAnchorName()) && !(node instanceof YamlAlias)) {
				latestDefinition[0] = node;
			}
			return true;
		});
		return aliasFound[0] ? latestDefinition[0] : null;
	}

	/**
	 * Callback of {@link YamlDocument#visitInDocumentOrder(YamlNode, NodeVisitor)}.
	 */
	@FunctionalInterface
	private interface NodeVisitor {
		/**
		 * Visits a node.
		 *
		 * @param node
		 *            the node
		 * @return true to continue, false to stop the traversal
		 */
		boolean visit(YamlNode node);
	}

	/**
	 * Visits the node and all nodes contained in it in document order: a collection before its
	 * content, mapping keys before their values.
	 *
	 * @return false if the visitor stopped the traversal
	 */
	private static boolean visitInDocumentOrder(final YamlNode node, final NodeVisitor visitor) {
		if (node == null) {
			return true;
		} else if (!visitor.visit(node)) {
			return false;
		} else if (node instanceof final YamlMapping mapping) {
			for (final YamlNode key : mapping.keySet()) {
				if (!visitInDocumentOrder(key, visitor) || !visitInDocumentOrder(mapping.get(key), visitor)) {
					return false;
				}
			}
		} else if (node instanceof final YamlSequence sequence) {
			for (final YamlNode item : sequence.items()) {
				if (!visitInDocumentOrder(item, visitor)) {
					return false;
				}
			}
		}
		return true;
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
