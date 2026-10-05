package de.soderer.yaml.data.directive;

/**
 * The "%TAG" directive defining a tag handle, like "%TAG !e! tag:example.com,2000:".
 */
public class YamlTagDirective extends YamlDirective<YamlTagDirective> {
	/**
	 * The tag handle, e.g. "!e!".
	 */
	private final String tag;
	/**
	 * The tag prefix the handle stands for.
	 */
	private final String replacement;

	/**
	 * Parses the data of a "%TAG" directive.
	 *
	 * @param yamlDirective
	 *            the directive data after "%TAG": handle, prefix and optional comment
	 * @throws Exception
	 *             if handle or prefix are missing or the data after them is no comment
	 */
	public YamlTagDirective(final String yamlDirective) throws Exception {
		final String[] parts = yamlDirective.trim().split("\\s+", 3);
		if (parts.length < 2) {
			throw new Exception("Invalid yaml tag directive data '" + yamlDirective + "'");
		} else {
			tag = parts[0];
			replacement = parts[1];
			if (parts.length == 3) {
				final String commentRaw = parts[2].trim();
				if (commentRaw.startsWith("#")) {
					setInlineComment(commentRaw.substring(1));
				} else {
					throw new Exception("Invalid yaml tag directive data '" + yamlDirective + "'");
				}
			}
		}
	}

	/**
	 * Returns the tag handle.
	 *
	 * @return the handle, e.g. "!e!"
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * Returns the tag prefix the handle stands for.
	 *
	 * @return the prefix
	 */
	public String getReplacement() {
		return replacement;
	}

	/**
	 * Returns the directive as YAML text including its comments.
	 */
	@Override
	public String toString() {
		String returnString = "";
		if (getLeadingComments() != null && !getLeadingComments().isEmpty()) {
			for (final String commentLine : getLeadingComments()) {
				returnString += "#" + commentLine + "\n";
			}
		}
		returnString += "%TAG " + tag + " " + replacement + (getInlineComment() != null ? " #" + getInlineComment() : "");
		return returnString;
	}
}
