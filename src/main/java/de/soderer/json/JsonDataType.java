package de.soderer.json;

/**
 * Data types of JSON values. INTEGER is a JSON number without fraction.
 */
public enum JsonDataType {
	/**
	 * String value.
	 */
	STRING,
	/**
	 * Number value without fraction.
	 */
	INTEGER,
	/**
	 * Number value with fraction or exponent.
	 */
	NUMBER,
	/**
	 * Object with properties.
	 */
	OBJECT,
	/**
	 * Array of values.
	 */
	ARRAY,
	/**
	 * Boolean value.
	 */
	BOOLEAN,
	/**
	 * The null value.
	 */
	NULL;

	/**
	 * Returns the name of this data type as used in JSON schema, e.g. "string".
	 *
	 * @return the lower case name
	 */
	public String getName() {
		return name().toLowerCase();
	}

	/**
	 * Returns the data type with the given name, ignoring case.
	 *
	 * @param value
	 *            the name, e.g. "string"
	 * @return the data type
	 * @throws Exception
	 *             if the name is unknown
	 */
	public static JsonDataType getFromString(final String value) throws Exception {
		for (final JsonDataType jsonDataType : JsonDataType.values()) {
			if (jsonDataType.name().equalsIgnoreCase(value)) {
				return jsonDataType;
			}
		}

		throw new Exception("Invalid JSON data type: " + value);
	}
}
