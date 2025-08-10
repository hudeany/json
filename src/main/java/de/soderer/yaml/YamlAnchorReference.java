package de.soderer.yaml;

import java.util.HashSet;
import java.util.Set;

import de.soderer.json.utilities.Utilities;

public class YamlAnchorReference extends YamlNode {
	public Set<String> getAllAvailableAnchorIds() {
		return new HashSet<>();
	}

	public YamlAnchorReference setValue(final String value) {
		if (Utilities.isBlank(value) || value.startsWith("&") || value.startsWith("*")) {
			throw new RuntimeException("Invalid yaml anchor id '" + value + "'");
		} else {
			super.setValue(value);
			return this;
		}
	}
	
	public Set<String> getAllReferencedAnchorIds() {
		Set<String> referencedAnchorIds = new HashSet<>();
		if (getValue() != null) {
			referencedAnchorIds.add((String) getValue());
		}
		return referencedAnchorIds;
	}
}
