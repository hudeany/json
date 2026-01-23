# Java JsonReader, JsonWriter, YamlReader, YamlWriter and JsonSchema

Read and write JSON and YAML data from and to files or streams.  
Validation by JsonSchema (see http://json-schema.org for specifications).  
Sequential read of JsonArray and YamlSequence items (like SAX parser for XML data).  

## JsonObject with JsonWriter and JsonReader example
```
JsonWriter writer = null;
ByteArrayOutputStream output = null;
JsonReader reader = null;
try {
	output = new ByteArrayOutputStream();
	writer = new JsonWriter(output, StandardCharsets.UTF_8);
	writer.openJsonObject();
	writer.openJsonObjectProperty("abc");
	writer.addSimpleJsonObjectPropertyValue("1");
	writer.openJsonObjectProperty("def");
	writer.addSimpleJsonObjectPropertyValue(2);
	writer.openJsonObjectProperty("ghi");
	writer.addSimpleJsonObjectPropertyValue(3.00);
	writer.closeJsonObject();
	writer.close();
	output.close();

	final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);

	reader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
	final JsonNode nodevalue = reader.read();
	System.out.println(nodevalue.getJsonDataType() == JsonDataType.OBJECT);
	// true
	final JsonObject jsonObject = (JsonObject) nodevalue;
	for (final Map.Entry<String, Object> jsonObjectProperty : jsonObject) {
		System.out.println(jsonObjectProperty.getKey() + ": " + jsonObjectProperty.getValue().getClass().getSimpleName() + ": " + jsonObjectProperty.getValue());
		// abc: String: 1
		// def: Integer: 2
		// ghi: Float: 3.0
	}
} catch (final Exception e) {
	e.printStackTrace();
} finally {
	Utilities.closeQuietly(output);
	Utilities.closeQuietly(writer);
	Utilities.closeQuietly(reader);
}
```

## JsonArray with JsonWriter and JsonReader example
```
JsonWriter writer = null;
ByteArrayOutputStream output = null;
JsonReader reader = null;
try {
	output = new ByteArrayOutputStream();
	writer = new JsonWriter(output, StandardCharsets.UTF_8);
	writer.openJsonArray();
	writer.addSimpleJsonArrayValue("1");
	writer.addSimpleJsonArrayValue(2);
	writer.addSimpleJsonArrayValue(3.00);
	writer.closeJsonArray();
	writer.close();
	output.close();

	final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);

	reader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
	final JsonNode nodevalue = reader.read();
	System.out.println(nodevalue.getJsonDataType() == JsonDataType.ARRAY);
	// true
	final JsonArray jsonArray = (JsonArray) nodevalue;
	for (final Object jsonArrayItem : jsonArray) {
		System.out.println(jsonArrayItem.getClass().getSimpleName() + ": " + jsonArrayItem);
		// String: 1
		// Integer: 2
		// Float: 3.0
	}
} catch (final Exception e) {
	e.printStackTrace();
} finally {
	Utilities.closeQuietly(output);
	Utilities.closeQuietly(writer);
	Utilities.closeQuietly(reader);
}
```

## YamlMapping with YamlWriter and YamlReader example
```
JsonWriter writer = null;
ByteArrayOutputStream output = null;
JsonReader reader = null;
try {
	output = new ByteArrayOutputStream();
	writer = new JsonWriter(output, StandardCharsets.UTF_8);
	writer.openJsonObject();
	writer.openJsonObjectProperty("abc");
	writer.addSimpleJsonObjectPropertyValue("1");
	writer.openJsonObjectProperty("def");
	writer.addSimpleJsonObjectPropertyValue(2);
	writer.openJsonObjectProperty("ghi");
	writer.addSimpleJsonObjectPropertyValue(3.00);
	writer.closeJsonObject();
	writer.close();
	output.close();

	final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);

	reader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
	final JsonNode nodevalue = reader.read();
	System.out.println(nodevalue.getJsonDataType() == JsonDataType.OBJECT);
	// true
	final JsonObject jsonObject = (JsonObject) nodevalue;
	for (final Map.Entry<String, Object> jsonObjectProperty : jsonObject) {
		System.out.println(jsonObjectProperty.getKey() + ": " + jsonObjectProperty.getValue().getClass().getSimpleName() + ": " + jsonObjectProperty.getValue());
		// abc: String: 1
		// def: Integer: 2
		// ghi: Float: 3.0
	}
} catch (final Exception e) {
	e.printStackTrace();
} finally {
	Utilities.closeQuietly(output);
	Utilities.closeQuietly(writer);
	Utilities.closeQuietly(reader);
}
```

## YamlSequence with YamlWriter and YamlReader example
```
YamlWriter writer = null;
ByteArrayOutputStream output = null;
YamlReader reader = null;
try {
	output = new ByteArrayOutputStream();
	writer = new YamlWriter(output, StandardCharsets.UTF_8);

	final YamlMapping outputSequence = new YamlMapping();
	outputSequence.put("abc", "1");
	outputSequence.put("def", 2);
	outputSequence.put("ghi", 3.00);
	final YamlDocument outputDocument = new YamlDocument().setRoot(outputSequence);
	writer.writeDocument(outputDocument);

	final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);

	reader = new YamlReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
	final YamlDocument document = reader.readDocument();
	System.out.println(document.getRoot() instanceof YamlMapping);
	// true
	final YamlMapping yamlMapping = (YamlMapping) document.getRoot();
	for (final Map.Entry<String, Object> yamlObjectProperty : yamlMapping) {
		System.out.println(yamlObjectProperty.getKey() + ": " + yamlObjectProperty.getValue().getClass().getSimpleName() + ": " + yamlObjectProperty.getValue());
		// String: 1
		// Integer: 2
		// Float: 3.0
	}
} catch (final Exception e) {
	e.printStackTrace();
} finally {
	Utilities.closeQuietly(output);
	Utilities.closeQuietly(writer);
	Utilities.closeQuietly(reader);
}
```

For other simple examples see test class "de.soderer.json.JsonTest" and class "de.soderer.yaml.YamlTest":

https://github.com/hudeany/json/blob/master/src/test/java/de/soderer/json/JsonTest.java
  
## Maven2 repository
This library is also available via Maven2 repository

```
<repositories>
	<repository>
		<id>de.soderer</id>
		<url>https://soderer.de/maven2</url>
	</repository>
</repositories>

<dependency>
	<groupId>de.soderer</groupId>
	<artifactId>json</artifactId>
	<version>RELEASE</version>
</dependency>
```
