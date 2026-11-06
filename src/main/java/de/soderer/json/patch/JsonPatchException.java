package de.soderer.json.patch;

/**
 * Thrown when a JSON Patch (RFC 6902) cannot be applied, e.g. because a path does not exist, a
 * "test" operation fails or an operation is malformed.
 */
public class JsonPatchException extends Exception {
	private static final long serialVersionUID = 6383521793451230564L;

	/**
	 * Index of the failing operation within the patch, -1 if the patch itself is invalid.
	 */
	private final int operationIndex;

	/**
	 * Creates a new exception.
	 *
	 * @param message
	 *            the detail message
	 * @param operationIndex
	 *            index of the failing operation within the patch, -1 if the patch itself is invalid
	 */
	public JsonPatchException(final String message, final int operationIndex) {
		super(operationIndex < 0 ? message : message + " (operation " + operationIndex + ")");

		this.operationIndex = operationIndex;
	}

	/**
	 * Returns the index of the failing operation within the patch.
	 *
	 * @return the zero based index, -1 if the patch itself is invalid
	 */
	public int getOperationIndex() {
		return operationIndex;
	}
}
