package de.soderer.yaml.directive;

public class VersionDirective extends YamlDirective {
	private final String yamlVersion;

	public VersionDirective(final String yamlVersion) {
		this.yamlVersion = yamlVersion;
	}

	public String getYamlVersion() {
		return yamlVersion;
	}
}
