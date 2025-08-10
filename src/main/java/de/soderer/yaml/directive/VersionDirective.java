package de.soderer.yaml.directive;

public class VersionDirective extends YamlDirective<VersionDirective> {
	private final String yamlVersion;

	public VersionDirective(final String yamlVersion) throws Exception {
		final String[] parts = yamlVersion.trim().split(" ", 2);
		this.yamlVersion = parts[0];
		if (parts.length == 2) {
			final String commentRaw = parts[1].trim();
			if (commentRaw.startsWith("#")) {
				setInlineComment(commentRaw.substring(1).trim());
			} else {
				throw new Exception("Invalid yaml tag directive data '" + yamlVersion + "'");
			}
		}
	}

	public String getYamlVersion() {
		return yamlVersion;
	}

	@Override
	public String toString() {
		return "%YAML " + yamlVersion;
	}
}
