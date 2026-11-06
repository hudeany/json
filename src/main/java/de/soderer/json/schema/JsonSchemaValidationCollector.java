package de.soderer.json.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the validation errors of {@link JsonSchema#validateAll(de.soderer.json.JsonNode)}.
 * <p>
 * The collector is bound to the validating thread. While it is active, the validators record each
 * error and continue with the next keyword or item instead of stopping at the first error. Within
 * checks whose failure is expected and not an error of the data ("anyOf", "oneOf", "not", "if",
 * "contains"), collecting is suspended, so these checks stop at their first error as usual.
 * </p>
 * <p>
 * This class is used by the validators and is not meant to be used directly.
 * </p>
 */
public final class JsonSchemaValidationCollector {
	/**
	 * Collector of the current thread, null if no collecting validation is running.
	 */
	private static final ThreadLocal<JsonSchemaValidationCollector> CURRENT = new ThreadLocal<>();

	/**
	 * Maximum number of errors to collect.
	 */
	private final int maximumErrors;
	/**
	 * Errors collected so far.
	 */
	private final List<JsonSchemaDataValidationError> errors = new ArrayList<>();
	/**
	 * Whether more errors were found than collected.
	 */
	private boolean truncated = false;
	/**
	 * Nesting depth of suspended collecting, 0 if collecting is active.
	 */
	private int suspendDepth = 0;

	/**
	 * Creates a collector.
	 *
	 * @param maximumErrors
	 *            the maximum number of errors to collect, at least 1
	 */
	private JsonSchemaValidationCollector(final int maximumErrors) {
		this.maximumErrors = maximumErrors;
	}

	/**
	 * Starts collecting on the current thread. Must be followed by {@link #stop()} in a finally
	 * block.
	 *
	 * @param maximumErrors
	 *            the maximum number of errors to collect, at least 1
	 * @return the new collector
	 * @throws IllegalStateException
	 *             if collecting already runs on this thread
	 */
	static JsonSchemaValidationCollector start(final int maximumErrors) {
		if (maximumErrors < 1) {
			throw new IllegalArgumentException("Maximum number of validation errors must be at least 1");
		} else if (CURRENT.get() != null) {
			throw new IllegalStateException("Collecting JSON schema validation is already running on this thread");
		}
		final JsonSchemaValidationCollector collector = new JsonSchemaValidationCollector(maximumErrors);
		CURRENT.set(collector);
		return collector;
	}

	/**
	 * Stops collecting on the current thread.
	 */
	static void stop() {
		CURRENT.remove();
	}

	/**
	 * Returns the collector of the current thread, if collecting is active and not suspended.
	 *
	 * @return the collector, or null if errors are not collected at the current position
	 */
	public static JsonSchemaValidationCollector getActive() {
		final JsonSchemaValidationCollector collector = CURRENT.get();
		if (collector == null || collector.suspendDepth > 0) {
			return null;
		} else {
			return collector;
		}
	}

	/**
	 * Suspends collecting on the current thread, if collecting runs. Must be followed by
	 * {@link #resume()} in a finally block.
	 */
	public static void suspend() {
		final JsonSchemaValidationCollector collector = CURRENT.get();
		if (collector != null) {
			collector.suspendDepth++;
		}
	}

	/**
	 * Resumes collecting on the current thread after {@link #suspend()}.
	 */
	public static void resume() {
		final JsonSchemaValidationCollector collector = CURRENT.get();
		if (collector != null && collector.suspendDepth > 0) {
			collector.suspendDepth--;
		}
	}

	/**
	 * Records an error. Errors beyond the maximum are not recorded, but mark the result as
	 * truncated.
	 *
	 * @param error
	 *            the error
	 */
	public void addError(final JsonSchemaDataValidationError error) {
		if (errors.size() < maximumErrors) {
			errors.add(error);
		} else {
			truncated = true;
		}
	}

	/**
	 * Checks whether no further errors are collected. The validators skip the remaining validation
	 * then.
	 *
	 * @return true if the maximum number of errors was exceeded
	 */
	public boolean isFull() {
		return truncated;
	}

	/**
	 * Returns the result of the collecting validation.
	 *
	 * @return the result
	 */
	JsonSchemaValidationResult getResult() {
		return new JsonSchemaValidationResult(errors, truncated);
	}
}
