package de.soderer.yaml.exception;

/**
 * Thrown when resolving aliases and merge keys would create more nodes than the configured maximum.
 * <p>
 * The limit protects against "billion laughs" data, where a few lines of nested aliases expand to
 * billions of nodes and exhaust the memory with an {@link OutOfMemoryError}.
 * </p>
 */
public class YamlAliasExpansionLimitException extends RuntimeException {
	private static final long serialVersionUID = -2280372614938452071L;

	/**
	 * The configured maximum number of nodes created by alias expansion.
	 */
	private final int maximumAliasExpandedNodes;

	/**
	 * Creates a new exception.
	 *
	 * @param maximumAliasExpandedNodes
	 *            the configured maximum number of nodes created by alias expansion
	 */
	public YamlAliasExpansionLimitException(final int maximumAliasExpandedNodes) {
		super("Maximum number of " + maximumAliasExpandedNodes + " nodes created by YAML alias and merge key expansion exceeded");

		this.maximumAliasExpandedNodes = maximumAliasExpandedNodes;
	}

	/**
	 * Returns the configured maximum number of nodes created by alias expansion.
	 *
	 * @return the maximum number of nodes
	 */
	public int getMaximumAliasExpandedNodes() {
		return maximumAliasExpandedNodes;
	}
}
