package de.soderer.yaml.data.directive;

public class YamlTagDirective extends YamlDirective<YamlTagDirective> {
	private final String tag;
	private final String replacement;

	public YamlTagDirective(final String yamlDirective) throws Exception {
		final String[] parts = yamlDirective.trim().split(" ", 3);
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

	public String getTag() {
		return tag;
	}

	public String getReplacement() {
		return replacement;
	}

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
