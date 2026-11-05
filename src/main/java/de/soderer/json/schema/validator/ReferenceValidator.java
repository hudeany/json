package de.soderer.json.schema.validator;

import java.util.List;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonValueString;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.schema.JsonSchemaUriUtilities;

/**
 * Validator for the "$ref" keyword: the JSON data node must match the referenced schema, which may
 * be part of the same schema (e.g. "#/$defs/address" or "#anchor"), an additional schema given as
 * {@link de.soderer.json.schema.JsonSchemaDependency}, a bundled meta schema, or a schema
 * downloaded by URL, all resolved by the {@link JsonSchemaDependencyResolver}.<br />
 * <br />
 * The reference value is resolved against the base URI of the schema object containing it. If it
 * cannot be resolved by URI, the former lookup rules of this library are used as fallback (e.g.
 * plain definition names).<br />
 * <br />
 * The referenced schema is resolved lazily on first use and its validators are cached on the
 * resolver. A reference resolved again for the same JSON data path is reported as cyclic.
 */
public class ReferenceValidator extends BaseJsonSchemaValidator {
	/** The reference resolved to an absolute URI, or null to use the legacy lookup only. */
	protected final String absoluteReferenceUri;

	/**
	 * Creates a new "$ref" validator, which resolves the reference against the base URI of the
	 * main schema document.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, the reference string
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a string or there is no dependency resolver
	 */
	public ReferenceValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError {
		this(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData, null);
	}

	/**
	 * Creates a new "$ref" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, the reference string
	 * @param baseUri
	 *            the base URI of the schema object containing the keyword, null for the base URI of
	 *            the main schema document
	 * @throws JsonSchemaDefinitionError
	 *             if the value is not a string, there is no dependency resolver, or the reference
	 *             points into a known schema document but cannot be resolved
	 */
	public ReferenceValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData, final String baseUri) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null || validatorData.isNull()) {
			throw new JsonSchemaDefinitionError("Reference key is 'null'", jsonSchemaPath);
		} else if (!(validatorData.isString())) {
			throw new JsonSchemaDefinitionError("Reference key is not a 'string'", jsonSchemaPath);
		} else if (jsonSchemaDependencyResolver == null) {
			throw new JsonSchemaDefinitionError("JSON schema reference definitions is empty. Cannot dereference key '" + validatorData + "'", jsonSchemaPath);
		}

		final String referenceValue = ((JsonValueString) validatorData).getValue();
		final String effectiveBaseUri = baseUri != null ? baseUri : jsonSchemaDependencyResolver.getBaseUri(null);
		absoluteReferenceUri = JsonSchemaUriUtilities.resolve(effectiveBaseUri, referenceValue);

		// Detect invalid references early, when the schema is read: references into already known documents and
		// relative references without any base URI given by "$id" (only external documents are resolved lazily)
		final boolean knownDocument = jsonSchemaDependencyResolver.isKnownSchemaDocument(absoluteReferenceUri);
		final boolean relativeWithoutBaseUri = !knownDocument && absoluteReferenceUri.startsWith(JsonSchemaUriUtilities.DEFAULT_ROOT_BASE_URI_PREFIX);
		if ((knownDocument && jsonSchemaDependencyResolver.getSchemaNodeByUri(absoluteReferenceUri) == null) || relativeWithoutBaseUri) {
			try {
				jsonSchemaDependencyResolver.getDependencyByLegacyReference(referenceValue, jsonSchemaPath);
			} catch (final JsonSchemaDefinitionError e) {
				throw e;
			} catch (final Exception e) {
				throw new JsonSchemaDefinitionError("Invalid schema reference: " + referenceValue, jsonSchemaPath, e);
			}
		}
	}

	/**
	 * Returns the reference resolved to an absolute URI.
	 *
	 * @return the absolute URI
	 */
	public final String getAbsoluteReferenceUri() {
		return absoluteReferenceUri;
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		validateAgainstTarget(absoluteReferenceUri, jsonNode, jsonPath);
	}

	/**
	 * Validates a JSON data node against the schema at the target URI.
	 *
	 * @param targetUri
	 *            the absolute URI of the referenced schema
	 * @param jsonNode
	 *            the JSON data node
	 * @param jsonPath
	 *            the path of the JSON data node, used in error messages
	 * @throws JsonSchemaDataValidationError
	 *             if the data node is not valid, the reference cannot be resolved or is cyclic
	 */
	protected void validateAgainstTarget(final String targetUri, final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		final String jsonPathString = jsonPath == null ? "" : jsonPath.toString();

		// Detect a genuine cycle: the same "$ref" being resolved again for the exact same JSON data path means the
		// schema keeps referring back to itself without ever descending into a different part of the data.
		if (jsonSchemaDependencyResolver.enterReferenceResolution(targetUri, jsonPathString)) {
			throw new JsonSchemaDataValidationError("Cyclic reference detected for '" + validatorData + "'", jsonPath);
		}
		try {
			final List<BaseJsonSchemaValidator> subValidators = getOrBuildSubValidators(targetUri, jsonPath);
			for (final BaseJsonSchemaValidator subValidator : subValidators) {
				subValidator.validate(jsonNode, jsonPath);
			}
		} finally {
			jsonSchemaDependencyResolver.exitReferenceResolution(targetUri, jsonPathString);
		}
	}

	/**
	 * Builds the sub-validators for a reference target once and caches them on the shared
	 * {@link JsonSchemaDependencyResolver}, instead of rebuilding the whole sub-validator tree on every single data
	 * node that this "$ref" is applied to.
	 *
	 * @param targetUri
	 *            the absolute URI of the referenced schema
	 * @param jsonPath
	 *            the path of the JSON data node, used in error messages
	 * @return the validators of the referenced schema
	 * @throws JsonSchemaDataValidationError
	 *             if the reference cannot be resolved or the referenced schema is invalid
	 */
	private List<BaseJsonSchemaValidator> getOrBuildSubValidators(final String targetUri, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		List<BaseJsonSchemaValidator> subValidators = jsonSchemaDependencyResolver.getCachedReferenceValidators(targetUri);
		if (subValidators != null) {
			return subValidators;
		}

		try {
			// Known documents and bundled meta schemas first, without any download
			JsonNode dereferencedValue = jsonSchemaDependencyResolver.getSchemaNodeByUri(targetUri, false);
			final String referenceValue = ((JsonValueString) validatorData).getValue();
			if (dereferencedValue == null && !referenceValue.startsWith("#") && targetUri.equals(absoluteReferenceUri)) {
				// Fallback to the former lookup rules (e.g. plain definition names) before any download is tried
				try {
					dereferencedValue = jsonSchemaDependencyResolver.getDependencyByLegacyReference(referenceValue, jsonSchemaPath);
				} catch (@SuppressWarnings("unused") final Exception e) {
					dereferencedValue = null;
				}
			}
			if (dereferencedValue == null) {
				// Download, if allowed
				dereferencedValue = jsonSchemaDependencyResolver.getSchemaNodeByUri(targetUri, true);
			}
			if (dereferencedValue == null && !referenceValue.startsWith("#") && targetUri.equals(absoluteReferenceUri)) {
				// Report the error of the former lookup rules, which explains the missing schema
				dereferencedValue = jsonSchemaDependencyResolver.getDependencyByLegacyReference(referenceValue, jsonSchemaPath);
			}
			if (dereferencedValue == null) {
				throw new JsonSchemaDefinitionError("Invalid JSON schema reference data type for key '" + validatorData + "'. Expected 'object' but was 'null'", jsonSchemaPath);
			} else {
				subValidators = JsonSchema.createValidatorsForSchemaNode(dereferencedValue, jsonSchemaDependencyResolver, new JsonSchemaPath(jsonSchemaPath));
			}
		} catch (final JsonSchemaDefinitionError e) {
			throw new JsonSchemaDataValidationError("JsonSchemaDefinitionError while using JSON schema reference: " + e.getMessage(), jsonPath, e);
		} catch (final Exception e) {
			throw new JsonSchemaDataValidationError("Error '" + e.getClass().getSimpleName() + "' while using JSON schema reference: " + e.getMessage(), jsonPath, e);
		}

		jsonSchemaDependencyResolver.putCachedReferenceValidators(targetUri, subValidators);
		return subValidators;
	}
}
