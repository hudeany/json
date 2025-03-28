package de.soderer.json.schema;

import de.soderer.json.path.JsonPath;

public class JsonSchemaDataValidationError extends Exception {
	private static final long serialVersionUID = -4849599671599546633L;

	private final JsonPath jsonDataPath;

	public JsonSchemaDataValidationError(final String message, final JsonPath jsonDataPath) {
		super(message);

		this.jsonDataPath = jsonDataPath;
	}

	public JsonSchemaDataValidationError(final String message, final JsonPath jsonDataPath, final Exception e) {
		super(message, e);

		this.jsonDataPath = jsonDataPath;
	}

	public JsonPath getJsonDataPath() {
		return jsonDataPath;
	}

	@Override
	public String getMessage() {
		return "Invalid JSON data: " + super.getMessage() + (jsonDataPath == null ? "" : " at JSON path: " + jsonDataPath);
	}
}
