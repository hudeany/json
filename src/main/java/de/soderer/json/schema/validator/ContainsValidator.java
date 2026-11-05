package de.soderer.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.exception.DuplicateKeyException;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaEvaluationContext;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.schema.JsonSchemaVersion;

/**
 * Validator for the "contains" keyword (since JSON schema draft 6): its value is a schema, which
 * must validate at least one item of the JSON data array. Data that is not an array is ignored,
 * except in simple mode.<br />
 * <br />
 * Since draft 2019-09 the sibling keywords "minContains" (default 1, 0 makes "contains" always
 * valid) and "maxContains" limit the number of matching items. Since draft 2020-12 the matching
 * items count as evaluated for "unevaluatedItems".
 */
public class ContainsValidator extends ExtendedBaseJsonSchemaValidator {
	/** Validators of the schema at least one array item must match. */
	private final List<BaseJsonSchemaValidator> subValidators;

	/** Minimum number of matching items ("minContains", draft 2019-09 and newer). */
	private long minContains = 1;

	/** Maximum number of matching items ("maxContains", draft 2019-09 and newer), or null. */
	private Long maxContains = null;

	/**
	 * Creates a new "contains" validator.
	 *
	 * @param parentValidatorData
	 *            the parent schema containing this keyword
	 * @param jsonSchemaDependencyResolver
	 *            the resolver for references and settings of the JSON schema
	 * @param jsonSchemaPath
	 *            the path of the keyword within the JSON schema
	 * @param validatorData
	 *            the value of the keyword, a schema object or a boolean
	 * @throws JsonSchemaDefinitionError
	 *             if the value is neither object nor boolean, or the schema is invalid
	 * @throws DuplicateKeyException
	 *             if the schema contains duplicate keys
	 */
	public ContainsValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final JsonNode validatorData) throws JsonSchemaDefinitionError, DuplicateKeyException {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData.isBoolean()) {
			subValidators = new ArrayList<>();
			subValidators.add(new BooleanValidator(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData));
		} else if (validatorData.isJsonObject()) {
			try {
				subValidators = JsonSchema.createValidators((JsonObject) validatorData, jsonSchemaDependencyResolver, jsonSchemaPath);
			} catch (final JsonSchemaDefinitionError e) {
				throw new JsonSchemaDefinitionError("Contains data JSON schema is invalid: " + e.getMessage(), jsonSchemaPath, e);
			}
		} else {
			throw new JsonSchemaDefinitionError("Contains data is neither 'object' nor 'boolean'", jsonSchemaPath);
		}

		if (jsonSchemaDependencyResolver.isVersionAtLeast(JsonSchemaVersion.draft2019_09) && jsonSchemaDependencyResolver.isValidationVocabularyActive()) {
			if (parentValidatorData.containsKey("minContains")) {
				minContains = readNonNegativeInteger(parentValidatorData.get("minContains"), "minContains", jsonSchemaPath);
			}
			if (parentValidatorData.containsKey("maxContains")) {
				maxContains = readNonNegativeInteger(parentValidatorData.get("maxContains"), "maxContains", jsonSchemaPath);
			}
		}
	}

	/**
	 * Reads a non-negative integer keyword value. Numbers with zero fractional part (e.g. 2.0) are
	 * accepted.
	 *
	 * @param valueNode
	 *            the keyword value
	 * @param keyword
	 *            the keyword name, used in error messages
	 * @param jsonSchemaPath
	 *            the path of the keyword, used in error messages
	 * @return the value
	 * @throws JsonSchemaDefinitionError
	 *             if the value is no non-negative integer
	 */
	private static long readNonNegativeInteger(final JsonNode valueNode, final String keyword, final JsonSchemaPath jsonSchemaPath) throws JsonSchemaDefinitionError {
		final Number value;
		if (valueNode != null && valueNode.isInteger()) {
			value = ((JsonValueInteger) valueNode).getValue();
		} else if (valueNode != null && valueNode.isNumber()) {
			value = ((JsonValueNumber) valueNode).getValue();
		} else {
			throw new JsonSchemaDefinitionError("Value of '" + keyword + "' is not an integer", jsonSchemaPath);
		}
		final double doubleValue = value.doubleValue();
		if (doubleValue < 0 || doubleValue != Math.floor(doubleValue)) {
			throw new JsonSchemaDefinitionError("Value of '" + keyword + "' is not a non-negative integer", jsonSchemaPath);
		}
		return value.longValue();
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonArray())) {
			if (jsonSchemaDependencyResolver.isSimpleMode()) {
				throw new JsonSchemaDataValidationError("Expected data type 'array' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
			}
		} else {
			// All items must be checked, if the number of matches is limited or matches are annotations (draft 2020-12)
			final boolean markEvaluatedItems = jsonSchemaDependencyResolver.isVersionAtLeast(JsonSchemaVersion.draft2020_12);
			final boolean countAllMatches = maxContains != null || markEvaluatedItems;
			final JsonSchemaEvaluationContext context = jsonSchemaDependencyResolver.getEvaluationContext();
			long matchCount = 0;
			int itemIndex = 0;
			for (final JsonNode itemObject : ((JsonArray) jsonNode).items()) {
				final JsonPath itemJsonPath = new JsonPath(jsonPath).addArrayIndex(itemIndex);
				JsonNode newJsonNode;
				try {
					newJsonNode = itemObject.withRootNode(false);
				} catch (final Exception e) {
					throw new JsonSchemaDataValidationError("Invalid data type '" + itemObject.getClass().getSimpleName() + "'", itemJsonPath, e);
				}
				if (validateSubSchema(subValidators, newJsonNode, itemJsonPath)) {
					matchCount++;
					if (markEvaluatedItems) {
						context.markItemEvaluated(itemIndex);
					}
					if (!countAllMatches && matchCount >= minContains) {
						return;
					}
				}
				itemIndex++;
			}

			if (matchCount < minContains) {
				if (minContains == 1) {
					throw new JsonSchemaDataValidationError("Array does not contain expected item", jsonPath);
				} else {
					throw new JsonSchemaDataValidationError("Array contains " + matchCount + " expected items, but at least " + minContains + " are required ('minContains')", jsonPath);
				}
			} else if (maxContains != null && matchCount > maxContains) {
				throw new JsonSchemaDataValidationError("Array contains " + matchCount + " expected items, but at most " + maxContains + " are allowed ('maxContains')", jsonPath);
			}
		}
	}
}
