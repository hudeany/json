package de.soderer.json.schema;

import de.soderer.json.utilities.Utilities;

/**
 * Reference into another JSON schema, like "other.json#/definitions/address".
 */
public class JsonSchemaPathExternalReference implements JsonSchemaPathElement {
	/**
	 * Location (name or URL) of the other schema.
	 */
	private final String schemaLocation;
	/**
	 * The reference within the other schema without leading "#/", empty for its root.
	 */
	private final String referenceString;

	/**
	 * Creates an external reference.
	 *
	 * @param schemaLocation
	 *            location (name or URL) of the other schema, a trailing "#" is removed
	 * @param referenceString
	 *            the reference within the other schema, e.g. "#/definitions/address" or "#"
	 */
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

	/**
	 * Returns the location of the other schema.
	 *
	 * @return the schema location
	 */
	public String getSchemaLocation() {
		return schemaLocation;
	}

	/**
	 * Returns the reference within the other schema.
	 *
	 * @return the reference without leading "#/", empty for its root
	 */
	public String getReferenceString() {
		return referenceString;
	}
}
