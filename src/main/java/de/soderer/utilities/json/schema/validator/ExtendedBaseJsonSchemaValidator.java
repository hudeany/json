package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;

public abstract class ExtendedBaseJsonSchemaValidator extends BaseJsonSchemaValidator {
	protected JsonObject parentValidatorData;

	protected ExtendedBaseJsonSchemaValidator(final JsonObject parentValidatorData, final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData, final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData, jsonNode, jsonPath);

		if (parentValidatorData == null) {
			throw new JsonSchemaDefinitionError("ParentValidatorData is 'null'", jsonSchemaPath);
		} else {
			this.parentValidatorData = parentValidatorData;
		}
	}
}
