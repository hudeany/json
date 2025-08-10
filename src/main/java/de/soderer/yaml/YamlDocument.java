package de.soderer.yaml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.soderer.yaml.directive.YamlDirective;

public class YamlDocument extends YamlValue {
	private final List<YamlDirective<?>> directives = new ArrayList<>();

	public List<YamlDirective<?>> getDirectives() {
		return directives;
	}

	public YamlDocument add(YamlDirective<?> directive) throws Exception {
		if (directive != null) {
			directives.add(directive);
		}
		return this;
	}
	
	public Set<String> getAllAvailableAnchorIds() {
		Set<String> anchorIds = new HashSet<>();
		if (getValue() != null) {
			anchorIds.addAll(((YamlNode) getValue()).getAllAvailableAnchorIds());
		}
		return anchorIds;
	}
	
	public Set<String> getAllReferencedAnchorIds() {
		Set<String> referencedAnchorIds = new HashSet<>();
		if (getValue() != null) {
			referencedAnchorIds.addAll(((YamlNode) getValue()).getAllReferencedAnchorIds());
		}
		return referencedAnchorIds;
	}
}
