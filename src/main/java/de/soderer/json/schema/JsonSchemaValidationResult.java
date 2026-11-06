package de.soderer.json.schema;

import java.util.Collections;
import java.util.List;

/**
 * Result of {@link JsonSchema#validateAll(de.soderer.json.JsonNode)}: all errors found, up to the
 * configured maximum (see {@link JsonSchemaConfiguration#setMaximumValidationErrors(int)}).
 * <p>
 * Errors of "anyOf" and "oneOf" are reported as a single error, whose causes are the errors of the
 * options (see {@link JsonSchemaDataValidationError#getSubSchemaErrors()}). Errors that only follow
 * from a wrong data type of a node are not reported.
 * </p>
 */
public class JsonSchemaValidationResult {
	/**
	 * The errors found.
	 */
	private final List<JsonSchemaDataValidationError> errors;
	/**
	 * Whether more errors were found than reported.
	 */
	private final boolean truncated;

	/**
	 * Creates a result.
	 *
	 * @param errors
	 *            the errors found
	 * @param truncated
	 *            whether more errors were found than reported
	 */
	JsonSchemaValidationResult(final List<JsonSchemaDataValidationError> errors, final boolean truncated) {
		this.errors = Collections.unmodifiableList(errors);
		this.truncated = truncated;
	}

	/**
	 * Checks whether the data is valid.
	 *
	 * @return true if no errors were found
	 */
	public boolean isValid() {
		return errors.isEmpty();
	}

	/**
	 * Returns the errors found, in the order of validation.
	 *
	 * @return the errors, empty if the data is valid
	 */
	public List<JsonSchemaDataValidationError> getErrors() {
		return errors;
	}

	/**
	 * Checks whether more errors were found than reported. The validation was stopped then after
	 * the maximum number of errors.
	 *
	 * @return true if the list of errors is incomplete
	 */
	public boolean isTruncated() {
		return truncated;
	}

	/**
	 * Returns the errors as text, one per line.
	 */
	@Override
	public String toString() {
		if (errors.isEmpty()) {
			return "Valid";
		}
		final StringBuilder text = new StringBuilder();
		for (final JsonSchemaDataValidationError error : errors) {
			text.append(error.getMessage());
			if (error.getJsonSchemaPath() != null) {
				text.append(" (schema path: ").append(error.getJsonSchemaPath()).append(")");
			}
			text.append("\n");
		}
		if (truncated) {
			text.append("... more errors not reported\n");
		}
		return text.toString();
	}
}
