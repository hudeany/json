package de.soderer.yaml.directive;

public class TagDirective extends YamlDirective {
	private final String tag;
	private final String replacement;

	public TagDirective(final String yamlDirective) throws Exception {
		final String[] parts = yamlDirective.split(" ");
		if (parts.length != 2) {
			throw new Exception("Invalid yaml tag directive data '" + yamlDirective + "'");
		}
		tag = parts[0];
		replacement = parts[1];
	}

	public String getTag() {
		return tag;
	}

	public String getReplacement() {
		return replacement;
	}
}
