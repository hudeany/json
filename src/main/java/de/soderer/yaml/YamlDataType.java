package de.soderer.yaml;

public enum YamlDataType {
	Bool("bool"),
	Integer("int"),
	Float("float"),
	String("str"),
	Binary("binary");

	private final String typeString;

	YamlDataType(final String typeString) {
		this.typeString = typeString;
	}

	public static YamlDataType getYamlDataTypeFromString(final String typeString) throws Exception {
		for (final YamlDataType dataType : YamlDataType.values()) {
			if (dataType.typeString.equalsIgnoreCase(typeString)) {
				return dataType;
			}
		}
		throw new Exception("Unknown Yaml data type: " + typeString);
	}

	public String getStorageCode() {
		return typeString;
	}
}
