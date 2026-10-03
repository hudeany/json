# Java JsonReader, JsonWriter, YamlReader, YamlWriter and JsonSchema

[![Maven Central](https://img.shields.io/maven-central/v/de.soderer/json)](https://central.sonatype.com/artifact/de.soderer/json)

Read and write JSON and YAML data from and to files or streams.  
Validation by JsonSchema (see http://json-schema.org for specifications).  
Sequential read of JsonArray and YamlSequence items (like SAX parser for XML data).  

## Usage

The library is available on Maven Central. Replace `VERSION` with the version shown in the badge above.

Maven:

```xml
<dependency>
	<groupId>de.soderer</groupId>
	<artifactId>json</artifactId>
	<version>VERSION</version>
</dependency>
```

Gradle:

```groovy
implementation "de.soderer:json:VERSION"
```

Without a build tool, the jar can be downloaded from the [GitHub releases](https://github.com/hudeany/json/releases).

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

## Sequential read of JSON data objects
```
JsonReader jsonReader = null;
try {
	final String data = ""
			+ "{"
			+ "	\"level1\":"
			+ "		["
			+ "			{"
			+ "				\"property1\": \"value11\","
			+ "				\"property2\": \"value12\","
			+ "				\"property3\": \"value13\""
			+ "			},"
			+ "			{"
			+ "				\"property1\": \"value21\","
			+ "				\"property2\": \"value22\","
			+ "				\"property3\": \"value23\""
			+ "			}"
			+ "		]"
			+ "}";
	jsonReader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
	jsonReader.readUpToJsonPath("$.level1");
	jsonReader.readNextToken();

	JsonNode nextJsonNode;
	int count = 0;
	while ((nextJsonNode = jsonReader.readNextJsonNode()) != null) {
		count++;
		final String property1 = (String) ((JsonObject) nextJsonNode).getSimpleValue("property1");
		final String property2 = (String) ((JsonObject) nextJsonNode).getSimpleValue("property2");
		final String property3 = (String) ((JsonObject) nextJsonNode).getSimpleValue("property3");
		Assertions.assertEquals(("value" + count + "1"), (property1));
		Assertions.assertEquals(("value" + count + "2"), (property2));
		Assertions.assertEquals(("value" + count + "3"), (property3));
	}
} catch (final Exception e) {
	e.printStackTrace();
	Assertions.fail(e.getMessage());
} finally {
	Utilities.closeQuietly(jsonReader);
}
```

## YamlMapping with YamlWriter and YamlReader example
```
try {
	final YamlMapping outputMapping = new YamlMapping()
		.put("abc", "1")
		.put("def", 2)
		.put("ghi", 3.00);

	final ByteArrayOutputStream output = new ByteArrayOutputStream();
	try (YamlWriter writer = new YamlWriter(output, StandardCharsets.UTF_8)) {
		writer.writeDocument(new YamlDocument(outputMapping));
	}

	final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);

	try (YamlReader reader = new YamlReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)))) {
		final YamlDocument document = reader.readDocument();
		System.out.println(document.getRoot() instanceof YamlMapping);
		// true
		final YamlMapping yamlMapping = (YamlMapping) document.getRoot();
		for (final Map.Entry<String, Object> yamlMappingEntry : yamlMapping) {
			System.out.println(yamlMappingEntry.getKey() + ": " + yamlMappingEntry.getValue().getClass().getSimpleName() + ": " + yamlMappingEntry.getValue());
			// abc: String: 1
			// def: Integer: 2
			// ghi: Float: 3.0
		}
	}
} catch (final Exception e) {
	e.printStackTrace();
}
```

## YamlSequence with YamlWriter and YamlReader example
```
try {
	final YamlSequence outputSequence = new YamlSequence()
		.add("1")
		.add(2)
		.add(3.00);

	final ByteArrayOutputStream output = new ByteArrayOutputStream();
	try (YamlWriter writer = new YamlWriter(output, StandardCharsets.UTF_8)) {
		writer.writeDocument(new YamlDocument(outputSequence));
	}

	final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);

	try (YamlReader reader = new YamlReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)))) {
		final YamlDocument document = reader.readDocument();
		System.out.println(document.getRoot() instanceof YamlSequence);
		// true
		final YamlSequence yamlSequence = (YamlSequence) document.getRoot();
		for (final Object yamlSequenceItem : yamlSequence) {
			System.out.println(yamlSequenceItem.getClass().getSimpleName() + ": " + yamlSequenceItem);
			// String: 1
			// Integer: 2
			// Float: 3.0
		}
	}
} catch (final Exception e) {
	e.printStackTrace();
}
```

## Sequential read of YAML data objects
```
final String testData = ""
	+ "level1:\n"
	+ "  items:\n"
	+ "    - property1: \"property 01\"\n"
	+ "      property2: \"property 02\"\n"
	+ "      property3: \"property 03\"\n"
	+ "    - property1: \"property 11\"\n"
	+ "      property2: \"property 12\"\n"
	+ "      property3: \"property 13\"\n";

try (InputStream testDataStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8))) {
	try (final YamlReader yamlReader = new YamlReader(testDataStream)) {
		yamlReader.readUpToPath("$.level1.items");
		YamlNode nextYamlNode;
		int count = 0;
		while ((nextYamlNode = yamlReader.readNextYamlNode()) != null) {
			final String property1 = (String) ((YamlScalar) ((YamlMapping) nextYamlNode).get("property1")).getValue();
			final String property2 = (String) ((YamlScalar) ((YamlMapping) nextYamlNode).get("property2")).getValue();
			final String property3 = (String) ((YamlScalar) ((YamlMapping) nextYamlNode).get("property3")).getValue();
			Assertions.assertTrue(("property " + count + "1").equals(property1));
			Assertions.assertTrue(("property " + count + "2").equals(property2));
			Assertions.assertTrue(("property " + count + "3").equals(property3));
			count++;
		}
	}
} catch (final Exception e) {
	e.printStackTrace();
}
```

For other simple examples see test class "de.soderer.json.JsonTest" and class "de.soderer.yaml.YamlTest":

https://github.com/hudeany/json/blob/master/src/test/java/de/soderer/json/JsonTest.java
