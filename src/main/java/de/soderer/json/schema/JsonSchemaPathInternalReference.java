package de.soderer.json.schema;

import de.soderer.json.utilities.Utilities;

public class JsonSchemaPathInternalReference implements JsonSchemaPathElement {
	private final String referenceString;

	public JsonSchemaPathInternalReference(final String referenceString) {
		if (referenceString.startsWith("#.") || referenceString.startsWith("#/")) {
			this.referenceString = referenceString.substring(2);
		} else if (referenceString.startsWith("#")) {
			this.referenceString = referenceString.substring(1);
		} else {
			this.referenceString = referenceString;
		}
	}

	@Override
	public String toString() {
		if (Utilities.isBlank(referenceString)) {
			return "#";
		} else {
			return "#/" + referenceString;
		}
	}
}
