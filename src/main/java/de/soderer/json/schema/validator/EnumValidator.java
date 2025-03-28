package de.soderer.json.schema.validator;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.path.JsonPath;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaDependencyResolver;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.utilities.NumberUtilities;
import de.soderer.json.utilities.Utilities;

/**
 * A list of values that are allowed
 */
public class EnumValidator extends BaseJsonSchemaValidator {
	public EnumValidator(final JsonSchemaDependencyResolver jsonSchemaDependencyResolver, final JsonSchemaPath jsonSchemaPath, final Object validatorData) throws JsonSchemaDefinitionError {
		super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData);

		if (validatorData == null) {
			throw new JsonSchemaDefinitionError("Enum data is 'null'", jsonSchemaPath);
		} else if (!(validatorData instanceof JsonArray)) {
			throw new JsonSchemaDefinitionError("Enum contains a non-JsonArray", jsonSchemaPath);
		} else if (((JsonArray) validatorData).size() == 0) {
			throw new JsonSchemaDefinitionError("Enum contains an empty JsonArray", jsonSchemaPath);
		}
	}

	@Override
	public void validate(final JsonNode jsonNode, final JsonPath jsonPath) throws JsonSchemaDataValidationError {
		for (final Object enumObject : ((JsonArray) validatorData)) {
			if (enumObject == null && jsonNode.isNull()) {
				return;
			} else if (enumObject != null && jsonNode.getValue() != null) {
				if (enumObject instanceof Boolean) {
					if (jsonNode.getValue() instanceof Boolean && ((Boolean) jsonNode.getValue()) == ((Boolean) enumObject)) {
						return;
					}
				} else if (enumObject instanceof String) {
					if (jsonNode.getValue() instanceof String && ((String) jsonNode.getValue()).equals(enumObject)) {
						return;
					}
				} else if (enumObject instanceof Character) {
					if (jsonNode.getValue() instanceof Character && ((Character) jsonNode.getValue()).equals(enumObject)) {
						return;
					}
				} else if (enumObject instanceof Number && jsonNode.getValue() instanceof Number) {
					if (NumberUtilities.compare((Number) enumObject, (Number) jsonNode.getValue()) == 0) {
						return;
					}
				} else if (enumObject instanceof JsonObject) {
					if (jsonNode.getValue() instanceof JsonObject && ((JsonObject) enumObject).equals(jsonNode.getValue())) {
						return;
					}
				} else if (enumObject instanceof JsonArray) {
					if (jsonNode.getValue() instanceof JsonArray && ((JsonArray) enumObject).equals(jsonNode.getValue())) {
						return;
					}
				}
			}
		}
		throw new JsonSchemaDataValidationError("Enumeration expected one of '" + Utilities.join((JsonArray) validatorData, "', '") + "' but was " + (jsonNode.isSimpleValue() ? "'" + jsonNode.getValue() + "'" : "'" + jsonNode.getJsonDataType() + "'"), jsonPath);
	}
}
