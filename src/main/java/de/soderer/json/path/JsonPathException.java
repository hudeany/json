package de.soderer.json.path;

public class JsonPathException extends Exception {
	private static final long serialVersionUID = -3686896720369394469L;

	private final JsonPath jsonPath;

	public JsonPathException(final String message, final JsonPath jsonPath) {
		super(message);
		this.jsonPath = jsonPath;
	}

	public JsonPath getJsonPath() {
		return jsonPath;
	}
}
