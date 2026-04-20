package de.soderer.json.exception;

public class JsonReaderStateException extends Exception {
	private static final long serialVersionUID = -8358642025701366115L;

	public JsonReaderStateException(final String errormessage) {
		super(errormessage);
	}
}
