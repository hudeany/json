package de.soderer.utilities.json.schema;

import de.soderer.utilities.json.utilities.Utilities;

public class JsonSchemaPathExternalReference implements JsonSchemaPathElement {
	private final String schemaLocation;
	private final String referenceString;

	public JsonSchemaPathExternalReference(final String schemaLocation, final String referenceString) {
		if (schemaLocation.endsWith("#")) {
			this.schemaLocation = schemaLocation.substring(0, schemaLocation.length() - 1);
		} else {
			this.schemaLocation = schemaLocation;
		}
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
			return schemaLocation + "#";
		} else {
			return schemaLocation + "#/" + referenceString;
		}
	}

	public String getSchemaLocation() {
		return schemaLocation;
	}

	public String getReferenceString() {
		return referenceString;
	}
}
