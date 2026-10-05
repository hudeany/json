package de.soderer.json.schema;

import de.soderer.json.utilities.Utilities;

/**
 * Reference within the same JSON schema, like "#/definitions/address".
 */
public class JsonSchemaPathInternalReference implements JsonSchemaPathElement {
	/**
	 * The reference without leading "#/", empty for the schema root.
	 */
	private final String referenceString;

	/**
	 * Creates an internal reference.
	 *
	 * @param referenceString
	 *            the reference, e.g. "#/definitions/address" or "#"
	 */
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
