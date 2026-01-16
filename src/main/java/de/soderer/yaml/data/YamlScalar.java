package de.soderer.yaml.data;

import java.util.Objects;

import de.soderer.json.utilities.NumberUtilities;

public class YamlScalar extends YamlNode {
	private final String valueString;
	private final Object value;
	private final YamlScalarType type;

	public YamlScalar(final Object value) throws RuntimeException {
		if (value == null) {
			type = YamlScalarType.NULL_VALUE;
			valueString = "null";
			this.value = null;
		} else if (value instanceof String) {
			type = YamlScalarType.STRING;
			valueString = value.toString();
			this.value = value;
		} else if (value instanceof Number) {
			type = YamlScalarType.NUMBER;
			valueString = value.toString();
			this.value = value;
		} else if (value instanceof Boolean) {
			type = YamlScalarType.BOOLEAN;
			valueString = value.toString();
			this.value = value;
		} else {
			throw new RuntimeException("Unsupported type of value object for Yaml scalar: '" + value.getClass().getSimpleName() + "'");
		}
	}

	public YamlScalar(final String valueString, final YamlScalarType type) throws RuntimeException {
		if (type == null) {
			throw new IllegalArgumentException("ScalarType must not be null");
		}

		if (valueString == null) {
			throw new RuntimeException("Invalid null String value for YAML scalar type: '" + type + "'");
		} else if (type == YamlScalarType.BOOLEAN) {
			this.valueString = valueString;
			if ("true".equalsIgnoreCase(valueString)
					|| "y".equalsIgnoreCase(valueString)
					|| "yes".equalsIgnoreCase(valueString)
					|| "on".equalsIgnoreCase(valueString)) {
				value = true;
				this.type = type;
			} else if ("false".equalsIgnoreCase(valueString)
					|| "n".equalsIgnoreCase(valueString)
					|| "no".equalsIgnoreCase(valueString)
					|| "off".equalsIgnoreCase(valueString)) {
				value = false;
				this.type = type;
			} else {
				throw new RuntimeException("Invalid boolean string repesentation: '" + valueString + "'");
			}
		} else if (type == YamlScalarType.NUMBER) {
			if (valueString.startsWith("0x")) {
				this.valueString = valueString;
				value = Integer.parseInt(valueString.substring(2), 16);
				this.type = type;
			} else if (valueString.startsWith("-0x")) {
				this.valueString = valueString;
				value = -Integer.parseInt(valueString.substring(3), 16);
				this.type = type;
			} else if (valueString.length() > 1 && valueString.startsWith("0") && !valueString.startsWith("0.")) {
				// Octal number
				this.valueString = valueString;
				value = Integer.parseInt(valueString.substring(1), 8);
				this.type = type;
			} else if (valueString.length() > 2 && valueString.startsWith("-0") && !valueString.startsWith("-0.")) {
				// Octal number
				this.valueString = valueString;
				value = -Integer.parseInt(valueString.substring(2), 8);
				this.type = type;
			} else if (valueString.contains(":")) {
				// Sexagesimal number
				this.valueString = valueString;
				value = valueString;
				this.type = YamlScalarType.STRING;
			} else if (".inf".equalsIgnoreCase(valueString)) {
				this.valueString = valueString;
				value = Double.POSITIVE_INFINITY;
				this.type = type;
			} else if ("-.inf".equalsIgnoreCase(valueString)) {
				this.valueString = valueString;
				value = Double.NEGATIVE_INFINITY;
				this.type = type;
			} else if (".nan".equalsIgnoreCase(valueString)) {
				this.valueString = valueString;
				value = Double.NaN;
				this.type = type;
			} else {
				this.valueString = valueString;
				Number numberValue;
				try {
					// Remove optional thousands separator '_'
					numberValue = NumberUtilities.parseNumber(valueString.replace("_", ""));
				} catch (@SuppressWarnings("unused") final NumberFormatException e) {
					numberValue = null;
				}

				if (numberValue != null) {
					value = numberValue;
					this.type = type;
				} else {
					value = valueString;
					this.type = YamlScalarType.STRING;
				}
			}
		} else if (type == YamlScalarType.NULL_VALUE) {
			this.valueString = valueString;
			value = null;
			this.type = type;
		} else {
			this.valueString = valueString;
			value = valueString;
			this.type = type;
		}
	}

	public YamlScalar(final Number value, final YamlScalarType type) throws RuntimeException {
		if (type == null) {
			throw new IllegalArgumentException("ScalarType must not be null");
		}

		if (value == null) {
			throw new RuntimeException("Invalid null value for YAML scalar type for number");
		} else if (type == YamlScalarType.NUMBER) {
			valueString = value.toString();
			this.value = value;
		} else {
			throw new RuntimeException("Invalid YAML scalar type for number: '" + type + "'");
		}

		this.type = type;
	}

	public YamlScalar(final Boolean value, final YamlScalarType type) throws RuntimeException {
		if (type == null) {
			throw new IllegalArgumentException("ScalarType must not be null");
		}

		if (value == null) {
			throw new RuntimeException("Invalid null value for YAML scalar type for boolean");
		} else if (type == YamlScalarType.BOOLEAN) {
			valueString = value.toString();
			this.value = value;
		} else {
			throw new RuntimeException("Invalid YAML scalar type for boolean: '" + type + "'");
		}

		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public String getValueString() {
		return valueString;
	}

	public YamlScalarType getType() {
		return type;
	}

	@Override
	public String toString() {
		if (value == null) {
			return "null";
		} else {
			return value.toString();
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, value);
	}

	@Override
	public boolean equals(final Object otherObject) {
		if (this == otherObject) {
			return true;
		} else if (otherObject == null) {
			return false;
		} else if (getClass() != otherObject.getClass()) {
			return false;
		} else {
			final YamlScalar other = (YamlScalar) otherObject;
			return type == other.type && Objects.equals(value, other.value);
		}
	}
}
