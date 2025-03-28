package de.soderer.yaml;

import java.util.ArrayList;
import java.util.List;

import de.soderer.yaml.directive.YamlDirective;

public class YamlDocument extends YamlObject<YamlDocument> {
	private final List<YamlDirective> directives = new ArrayList<>();

	public List<YamlDirective> getDirectives() {
		return directives;
	}

	public YamlDocument add(YamlDirective directive) throws Exception {
		if (directive != null) {
			directives.add(directive);
		}
		return this;
	}
}
