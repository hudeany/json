package de.soderer.json.exception;

public class JsonWriterStateException extends Exception {
	private static final long serialVersionUID = 664685088425241771L;

	public JsonWriterStateException(final String errormessage) {
		super(errormessage);
	}
}
