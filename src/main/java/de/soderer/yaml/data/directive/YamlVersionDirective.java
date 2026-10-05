package de.soderer.yaml.data.directive;

/**
 * The "%YAML" directive with the YAML version of the document, like "%YAML 1.2".
 */
public class YamlVersionDirective extends YamlDirective<YamlVersionDirective> {
	/**
	 * The YAML version, e.g. "1.2".
	 */
	private final String yamlVersion;

	/**
	 * Parses the data of a "%YAML" directive.
	 *
	 * @param yamlVersion
	 *            the directive data after "%YAML", e.g. "1.2 # comment"
	 * @throws Exception
	 *             if the data after the version is no comment
	 */
	public YamlVersionDirective(final String yamlVersion) throws Exception {
		final String[] parts = yamlVersion.trim().split("\\s+", 2);
		this.yamlVersion = parts[0];
		if (parts.length == 2) {
			final String commentRaw = parts[1].trim();
			if (commentRaw.startsWith("#")) {
				setInlineComment(commentRaw.substring(1));
			} else {
				throw new Exception("Invalid yaml version directive data '" + yamlVersion + "'");
			}
		}
	}

	/**
	 * Returns the YAML version.
	 *
	 * @return the version, e.g. "1.2"
	 */
	public String getYamlVersion() {
		return yamlVersion;
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
		returnString += "%YAML " + yamlVersion + (getInlineComment() != null ? " #" + getInlineComment() : "");
		return returnString;
	}
}
