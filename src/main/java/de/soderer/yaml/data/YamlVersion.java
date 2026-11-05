package de.soderer.yaml.data;

/**
 * YAML specification version that decides how plain (unquoted) scalars are resolved to booleans,
 * nulls and numbers.
 * <ul>
 * <li>{@link #V1_2}: core schema of YAML 1.2. Only "true"/"false" (in the cases true, True, TRUE)
 * are booleans, "017" is the decimal 17, octal numbers need the prefix "0o", and "yes", "on", "y",
 * "1_000" or "12:30" are strings.</li>
 * <li>{@link #V1_1}: rules of YAML 1.1. "yes"/"no", "on"/"off" and "y"/"n" are booleans as well,
 * "017" is the octal 15, and "_" may separate digits.</li>
 * </ul>
 */
public enum YamlVersion {
	/** YAML 1.1 */
	V1_1("1.1"),
	/** YAML 1.2 (core schema) */
	V1_2("1.2");

	private final String versionString;

	YamlVersion(final String versionString) {
		this.versionString = versionString;
	}

	/**
	 * Returns the version as written in a "%YAML" directive.
	 *
	 * @return the version string, e.g. "1.2"
	 */
	public String getVersionString() {
		return versionString;
	}

	/**
	 * Returns the version for the value of a "%YAML" directive. Versions before 1.2 follow the rules of
	 * 1.1, later versions the rules of 1.2.
	 *
	 * @param versionString
	 *            the version of the directive, e.g. "1.1"
	 * @return the version, or null if the text is no valid version number
	 */
	public static YamlVersion fromDirectiveValue(final String versionString) {
		if (versionString == null || !versionString.trim().matches("[0-9]+\\.[0-9]+")) {
			return null;
		}
		final String[] parts = versionString.trim().split("\\.");
		final int major = Integer.parseInt(parts[0]);
		final int minor = Integer.parseInt(parts[1]);
		if (major < 1 || (major == 1 && minor < 2)) {
			return V1_1;
		} else {
			return V1_2;
		}
	}
}
