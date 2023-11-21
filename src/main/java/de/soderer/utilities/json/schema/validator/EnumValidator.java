package de.soderer.utilities.json.schema.validator;

import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.path.JsonPath;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;
import de.soderer.utilities.json.schema.JsonSchemaPath;
import de.soderer.utilities.json.utilities.Utilities;

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
			if (jsonNode.isNull() && enumObject == null) {
				return;
			} else if (enumObject != null && enumObject.equals(jsonNode.getValue())) {
				return;
			}
		}
		throw new JsonSchemaDataValidationError("Enumeration expected one of '" + Utilities.join((JsonArray) validatorData, "', '") + "' but was " + (jsonNode.isSimpleValue() ? "'" + jsonNode.getValue() + "'" : "'" + jsonNode.getJsonDataType() + "'"), jsonPath);
	}
}
