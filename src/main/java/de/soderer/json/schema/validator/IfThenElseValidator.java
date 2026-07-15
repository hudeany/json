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
	private final List<BaseJsonSchemaValidator> ifSubValidators;
	private List<BaseJsonSchemaValidator> thenSubValidators = null;
	private List<BaseJsonSchemaValidator> elseSubValidators = null;

	public IfThenElseValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode ifJsonObject, final JsonNode thenObject, final JsonNode elseObject) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, ifJsonObject);

		if (!jsonSchemaDependencyResolver.isDraftV7Mode()) {
			throw new JsonSchemaDefinitionError("Support for 'if' comes with draft version v7. Please configure used JSON schema version accordingly.", jsonSchemaPath);
		} else if (ifJsonObject == null) {
			throw new JsonSchemaDefinitionError("'if' value is 'null'", jsonSchemaPath);
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
