package de.soderer.json.schema.validator;

import java.util.List;

import de.soderer.json.JsonNode;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaEvaluationContext;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Applies all keyword validators of one schema object (JSON schema draft 2019-09 and newer).<br />
 * <br />
 * It opens a frame in the {@link JsonSchemaEvaluationContext} for the evaluated JSON data node,
 * in which the keyword validators collect their annotations (evaluated properties and items) for
 * "unevaluatedProperties" and "unevaluatedItems". The frame also records the schema resource for
 * the dynamic scope of "$dynamicRef" and "$recursiveRef".
 */
public class SchemaObjectValidator extends BaseJsonSchemaValidator {
	/** Validators of the keywords of the schema object, "unevaluated*" validators last. */
	private final List<BaseJsonSchemaValidator> keywordValidators;

	/** URI of the schema resource the schema object belongs to. */
	private final String resourceUri;

	/**
	 * Creates a new schema object validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the schema object within the JSON schema
	 * @param schemaObject
	 *            the schema object
	 * @param keywordValidators
	 *            the validators of its keywords
	 * @param resourceUri
	 *            the URI of the schema resource the schema object belongs to
	 * @throws JsonSchemaDefinitionError
	 *             if the schema object is null
	 */
	public SchemaObjectValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode schemaObject, final List<BaseJsonSchemaValidator> keywordValidators, final String resourceUri) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, schemaObject);

		this.keywordValidators = keywordValidators;
		this.resourceUri = resourceUri;
	}

	/**
	 * Returns the validators of the keywords of the schema object.
	 *
	 * @return the keyword validators
	 */
	public List<BaseJsonSchemaValidator> getKeywordValidators() {
		return keywordValidators;
	}

	/**
	 * Returns the URI of the schema resource the schema object belongs to.
	 *
	 * @return the resource URI
	 */
	public String getResourceUri() {
		return resourceUri;
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		final JsonSchemaEvaluationContext context = jsonSchemaDependencyResolver.getEvaluationContext();
		final JsonSchemaEvaluationContext.Frame frame = context.enterSchemaObject(jsonPath == null ? "" : jsonPath.toString(), resourceUri);
		boolean valid = false;
		try {
			for (final BaseJsonSchemaValidator keywordValidator : keywordValidators) {
				keywordValidator.validate(jsonNode, jsonPath);
			}
			valid = true;
		} finally {
			context.exitSchemaObject(frame, valid);
			if (!context.isActive()) {
				// Validation of the whole document is complete: avoid leaking state on thread-pool threads
				jsonSchemaDependencyResolver.clearEvaluationContext();
			}
		}
	}
}
