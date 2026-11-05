package de.soderer.json.schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Runtime state of one validation run for JSON schema draft 2019-09 and newer.<br />
 * <br />
 * For each schema object applied to a JSON data node a frame is pushed. A frame collects the
 * annotations needed by "unevaluatedProperties" and "unevaluatedItems" (which properties and items
 * of the data node were evaluated successfully) and knows the schema resource it belongs to, which
 * makes up the dynamic scope for "$dynamicRef" and "$recursiveRef".<br />
 * <br />
 * When a schema object validates successfully, its annotations are merged into the frame of the
 * schema object that applied it, if both refer to the same data node ("in-place" applicators like
 * "allOf" or "$ref"). Annotations of failed schema objects are dropped.<br />
 * <br />
 * Instances are bound to a thread by {@link JsonSchemaDependencyResolver#getEvaluationContext()}.
 */
public class JsonSchemaEvaluationContext {
	/**
	 * Evaluation state of one schema object applied to one JSON data node.
	 */
	public static final class Frame {
		/** Frame of the schema object that applied this one, or null for the root. */
		private final Frame parent;

		/** JSON data path of the evaluated data node. */
		private final String instanceLocation;

		/** URI of the schema resource the schema object belongs to. */
		private final String resourceUri;

		/** Property names evaluated successfully, created on demand. */
		private Set<String> evaluatedProperties = null;

		/** Array indices evaluated successfully, created on demand. */
		private Set<Integer> evaluatedItems = null;

		/** Whether all array items were evaluated successfully. */
		private boolean allItemsEvaluated = false;

		/**
		 * Creates a frame.
		 *
		 * @param parent
		 *            the parent frame or null
		 * @param instanceLocation
		 *            JSON data path of the evaluated data node
		 * @param resourceUri
		 *            URI of the schema resource
		 */
		private Frame(final Frame parent, final String instanceLocation, final String resourceUri) {
			this.parent = parent;
			this.instanceLocation = instanceLocation;
			this.resourceUri = resourceUri;
		}

		/**
		 * Returns the URI of the schema resource of this frame.
		 *
		 * @return the resource URI
		 */
		public String getResourceUri() {
			return resourceUri;
		}

		/**
		 * Merges the annotations of a successfully evaluated child frame into this frame.
		 *
		 * @param child
		 *            the child frame
		 */
		private void merge(final Frame child) {
			if (child.evaluatedProperties != null) {
				if (evaluatedProperties == null) {
					evaluatedProperties = new HashSet<>();
				}
				evaluatedProperties.addAll(child.evaluatedProperties);
			}
			if (child.evaluatedItems != null) {
				if (evaluatedItems == null) {
					evaluatedItems = new HashSet<>();
				}
				evaluatedItems.addAll(child.evaluatedItems);
			}
			allItemsEvaluated = allItemsEvaluated || child.allItemsEvaluated;
		}
	}

	/** Innermost frame, or null if no schema object is being evaluated. */
	private Frame currentFrame = null;

	/**
	 * Creates an evaluation context without any frame, ready for a new validation run.
	 */
	public JsonSchemaEvaluationContext() {
		// Frames are created by enterSchemaObject()
	}

	/**
	 * Starts the evaluation of a schema object for a JSON data node.
	 *
	 * @param instanceLocation
	 *            JSON data path of the data node
	 * @param resourceUri
	 *            URI of the schema resource of the schema object
	 * @return the new frame, to be passed to {@link #exitSchemaObject(Frame, boolean)}
	 */
	public Frame enterSchemaObject(final String instanceLocation, final String resourceUri) {
		currentFrame = new Frame(currentFrame, instanceLocation, resourceUri);
		return currentFrame;
	}

	/**
	 * Ends the evaluation of a schema object. On success its annotations are kept for the parent
	 * schema object, if that one evaluates the same data node.
	 *
	 * @param frame
	 *            the frame returned by {@link #enterSchemaObject(String, String)}
	 * @param valid
	 *            whether the schema object validated the data node successfully
	 */
	public void exitSchemaObject(final Frame frame, final boolean valid) {
		final Frame parent = frame.parent;
		if (valid && parent != null && parent.instanceLocation.equals(frame.instanceLocation)) {
			parent.merge(frame);
		}
		currentFrame = parent;
	}

	/**
	 * Marks a property of the current data object as evaluated.
	 *
	 * @param propertyName
	 *            the property name
	 */
	public void markPropertyEvaluated(final String propertyName) {
		if (currentFrame != null) {
			if (currentFrame.evaluatedProperties == null) {
				currentFrame.evaluatedProperties = new HashSet<>();
			}
			currentFrame.evaluatedProperties.add(propertyName);
		}
	}

	/**
	 * Checks whether a property of the current data object was evaluated by the current schema
	 * object or its successfully evaluated in-place sub schemas.
	 *
	 * @param propertyName
	 *            the property name
	 * @return true, if evaluated
	 */
	public boolean isPropertyEvaluated(final String propertyName) {
		return currentFrame != null && currentFrame.evaluatedProperties != null && currentFrame.evaluatedProperties.contains(propertyName);
	}

	/**
	 * Marks an item of the current data array as evaluated.
	 *
	 * @param index
	 *            the item index
	 */
	public void markItemEvaluated(final int index) {
		if (currentFrame != null) {
			if (currentFrame.evaluatedItems == null) {
				currentFrame.evaluatedItems = new HashSet<>();
			}
			currentFrame.evaluatedItems.add(index);
		}
	}

	/**
	 * Marks all items of the current data array as evaluated.
	 */
	public void markAllItemsEvaluated() {
		if (currentFrame != null) {
			currentFrame.allItemsEvaluated = true;
		}
	}

	/**
	 * Checks whether an item of the current data array was evaluated.
	 *
	 * @param index
	 *            the item index
	 * @return true, if evaluated
	 */
	public boolean isItemEvaluated(final int index) {
		return currentFrame != null && (currentFrame.allItemsEvaluated || (currentFrame.evaluatedItems != null && currentFrame.evaluatedItems.contains(index)));
	}

	/**
	 * Returns the dynamic scope: the URIs of all schema resources entered during the current
	 * evaluation path, outermost first, without consecutive duplicates.
	 *
	 * @return the resource URIs
	 */
	public List<String> getDynamicScope() {
		final List<String> scope = new ArrayList<>();
		for (Frame frame = currentFrame; frame != null; frame = frame.parent) {
			if (frame.resourceUri != null && (scope.isEmpty() || !scope.get(scope.size() - 1).equals(frame.resourceUri))) {
				scope.add(frame.resourceUri);
			}
		}
		final List<String> outermostFirst = new ArrayList<>(scope.size());
		for (int i = scope.size() - 1; i >= 0; i--) {
			outermostFirst.add(scope.get(i));
		}
		return outermostFirst;
	}

	/**
	 * Checks whether a schema object is currently being evaluated.
	 *
	 * @return true, if there is a current frame
	 */
	public boolean isActive() {
		return currentFrame != null;
	}
}
