package de.soderer.json.schema.validator;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.schema.JsonSchemaUriUtilities;

/**
 * Validator for the dynamic reference keywords "$dynamicRef" (JSON schema draft 2020-12) and
 * "$recursiveRef" (JSON schema draft 2019-09).<br />
 * <br />
 * The reference is first resolved like "$ref". For "$dynamicRef" with a plain name fragment, if
 * that initial target defines a "$dynamicAnchor" of the same name, the target is replaced by the
 * outermost schema resource in the dynamic scope (the schema resources entered on the way to this
 * keyword) that defines a "$dynamicAnchor" of that name. For "$recursiveRef" ("#" only), if the
 * initial target has "$recursiveAnchor": true, the target is replaced by the outermost schema
 * resource in the dynamic scope that also has "$recursiveAnchor": true.<br />
 * <br />
 * This allows extending recursive schemas, e.g. meta schemas with additional keywords.
 */
public class DynamicReferenceValidator extends ReferenceValidator {
	/** True for "$recursiveRef" (draft 2019-09), false for "$dynamicRef" (draft 2020-12). */
	private final boolean recursiveReference;

	/** Anchor name for "$dynamicRef" with a plain name fragment, otherwise null. */
	private String dynamicAnchorName = null;

	/** Whether the document of the initial target was already loaded into the resource index. */
	private volatile boolean initialTargetLoaded = false;

	/**
	 * Creates a new dynamic reference validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, the reference string
	 * @param baseUri
	 *            the base URI of the schema object containing the keyword
	 * @param recursiveReference
	 *            true for "$recursiveRef", false for "$dynamicRef"
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a string, or "$recursiveRef" is not "#"
	 */
	public DynamicReferenceValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData, final String baseUri, final boolean recursiveReference) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData, baseUri);

		this.recursiveReference = recursiveReference;
		if (recursiveReference && !"#".equals(((JsonValueString) validatorData).getValue())) {
			throw new JsonSchemaDefinitionError("Value of '$recursiveRef' must be '#'", jsonSchemaPath);
		}
		if (!recursiveReference) {
			final String fragment = JsonSchemaUriUtilities.getFragment(absoluteReferenceUri);
			if (fragment != null && !JsonSchemaUriUtilities.isJsonPointerFragment(fragment)) {
				dynamicAnchorName = fragment;
			}
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		final String initialTargetUri = getAbsoluteReferenceUri();
		String targetUri = initialTargetUri;
		final String initialResourceUri = JsonSchemaUriUtilities.removeFragment(initialTargetUri);
		if (!initialTargetLoaded) {
			try {
				// Make sure the initial target is loaded and indexed before its anchors are checked
				jsonSchemaDependencyResolver.getSchemaNodeByUri(initialTargetUri);
				initialTargetLoaded = true;
			} catch (final JsonSchemaDefinitionError e) {
				throw new JsonSchemaDataValidationError("JsonSchemaDefinitionError while using JSON schema reference: " + e.getMessage(), jsonPath, e);
			}
		}

		if (recursiveReference) {
			if (jsonSchemaDependencyResolver.hasRecursiveAnchor(initialResourceUri)) {
				for (final String scopeResourceUri : jsonSchemaDependencyResolver.getEvaluationContext().getDynamicScope()) {
					if (jsonSchemaDependencyResolver.hasRecursiveAnchor(scopeResourceUri)) {
						targetUri = scopeResourceUri;
						break;
					}
				}
			}
		} else if (dynamicAnchorName != null && jsonSchemaDependencyResolver.hasDynamicAnchor(initialResourceUri, dynamicAnchorName)) {
			for (final String scopeResourceUri : jsonSchemaDependencyResolver.getEvaluationContext().getDynamicScope()) {
				if (jsonSchemaDependencyResolver.hasDynamicAnchor(scopeResourceUri, dynamicAnchorName)) {
					targetUri = scopeResourceUri + "#" + dynamicAnchorName;
					break;
				}
			}
		}

		validateAgainstTarget(targetUri, jsonNode, jsonPath);
	}
}
