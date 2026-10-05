package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;

/**
 * Tripartite directive with mandatory "if", optional "then" and optional "else" part.
 * If the JSON subschema from the "if" part matches, then the JSON schema from the "then" part must match also, if available.
 * If the JSON subschema from the "if" part does not match, then the JSON schema from the "else" part must match also, if available.<br />
 * <br />
 * Note: this validator intentionally builds sub-validators via {@link JsonSchema#createValidators} and invokes them
 * directly with the actual JSON data path (like {@link AllOfValidator}, {@link AnyOfValidator}, {@link OneOfValidator}
 * and {@link NotValidator} do), instead of wrapping "if"/"then"/"else" into separate {@link JsonSchema} instances and
 * calling their single-argument {@code validate(JsonNode)}. That method internally calls
 * {@code JsonNode.setRootNode(true)}, which mutates the given data node in place (it is not a defensive copy) and
 * always validates against a fresh root {@link JsonPath}. Since "if"/"then"/"else" are routinely applied to nested
 * data nodes (not just document roots), using that method here would both discard the real JSON data path in error
 * messages and incorrectly/permanently mark a nested data node as "root".
 */
public class IfThenElseValidator extends BaseJsonSchemaValidator {
	/** Validators of the "if" schema. */
	private final List<BaseJsonSchemaValidator> ifSubValidators;

	/** Validators of the "then" schema, or null if there is no "then" part. */
	private List<BaseJsonSchemaValidator> thenSubValidators = null;

	/** Validators of the "else" schema, or null if there is no "else" part. */
	private List<BaseJsonSchemaValidator> elseSubValidators = null;

	/**
	 * Creates a new "if"/"then"/"else" validator.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema, must be in draft 7
	 *            mode
	 * @param jsonSchemaPath
	 *            the path of the schema object containing the keywords
	 * @param ifJsonObject
	 *            the value of "if", a schema object or a boolean
	 * @param thenObject
	 *            the value of "then", a schema object or a boolean, or null if not defined
	 * @param elseObject
	 *            the value of "else", a schema object or a boolean, or null if not defined
	 * @throws JsonSchemaDefinitionError
	 *             if not in draft 7 mode, a value is neither object nor boolean, or a schema is
	 *             invalid
	 * @throws DuplicateKeyException
	 *             if a schema contains duplicate keys
	 */
	public IfThenElseValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode ifJsonObject, final JsonNode thenObject, final JsonNode elseObject) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, ifJsonObject);

		if (!jsonSchemaDependencyResolver.isDraftV7Mode()) {
			throw new JsonSchemaDefinitionError("Support for 'if' comes with draft version v7. Please configure used JSON schema version accordingly.", jsonSchemaPath);
		} else if (ifJsonObject == null) {
			throw new JsonSchemaDefinitionError("'if' value is 'null'", jsonSchemaPath);
		} else if (!ifJsonObject.isBoolean() && !ifJsonObject.isJsonObject()) {
			throw new JsonSchemaDefinitionError("'if' branch is not 'boolean' or 'object'", jsonSchemaPath);
		} else if (thenObject != null && !thenObject.isBoolean() && !thenObject.isJsonObject()) {
			throw new JsonSchemaDefinitionError("'then' branch is not 'boolean' or 'object'", jsonSchemaPath);
		} else if (elseObject != null && !elseObject.isBoolean() && !elseObject.isJsonObject()) {
			throw new JsonSchemaDefinitionError("'else' branch is not 'boolean' or 'object'", jsonSchemaPath);
		} else {
			ifSubValidators = createBranchValidators(jsonSchemaDependencyResolver, new JsonSchemaPath(jsonSchemaPath).addPropertyKey("if"), ifJsonObject);

			if (thenObject != null) {
				thenSubValidators = createBranchValidators(jsonSchemaDependencyResolver, new JsonSchemaPath(jsonSchemaPath).addPropertyKey("then"), thenObject);
			}

			if (elseObject != null) {
				elseSubValidators = createBranchValidators(jsonSchemaDependencyResolver, new JsonSchemaPath(jsonSchemaPath).addPropertyKey("else"), elseObject);
			}
		}
	}

	/**
	 * Creates the validators of one branch.
	 *
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param branchSchemaPath
	 *            the path of the branch within the JSON schema
	 * @param branchObject
	 *            the value of the branch, a schema object or a boolean
	 * @return the validators of the branch
	 * @throws JsonSchemaDefinitionError
	 *             if the schema is invalid
	 * @throws DuplicateKeyException
	 *             if the schema contains duplicate keys
	 */
	private static List<BaseJsonSchemaValidator> createBranchValidators(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath branchSchemaPath, final JsonNode branchObject) throws JsonSchemaDefinitionError, DuplicateKeyException {
		if (branchObject.isBoolean()) {
			final List<BaseJsonSchemaValidator> subValidators = new ArrayList<>();
			subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, branchSchemaPath, branchObject));
			return subValidators;
		} else {
			return JsonSchema.createValidators((JsonObject) branchObject, jsonSchemaDependencyResolver, branchSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (validateSubSchema(ifSubValidators, jsonNode, jsonPath)) {
			if (thenSubValidators != null) {
				for (final BaseJsonSchemaValidator subValidator : thenSubValidators) {
					subValidator.validate(jsonNode, jsonPath);
				}
			}
		} else {
			if (elseSubValidators != null) {
				for (final BaseJsonSchemaValidator subValidator : elseSubValidators) {
					subValidator.validate(jsonNode, jsonPath);
				}
			}
		}
	}
}
