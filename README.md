# Java JsonReader, JsonWriter and JsonSchema

Read and write JSON data from and to files or streams.  
Validation by JsonSchema (see http://json-schema.org for specifications).  
Sequential read of JsonArray items (like SAX parser for XML data).  

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
	jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
	final JsonNode nodevalue = jsonReader.read();
	System.out.println(nodevalue.getJsonDataType() == JsonDataType.OBJECT);
	// true
	final JsonObject jsonObject = (JsonObject) nodevalue.getValue();
	for (final Map.Entry<String, Object> jsonObjectProperty : jsonObject) {
		System.out.println(jsonObjectProperty.getKey() + ": " + jsonObjectProperty.getValue());
		// abc: 1
		// def: 2
		// ghi: 3.0
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

	jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
	final JsonNode nodevalue = jsonReader.read();
	System.out.println(nodevalue.getJsonDataType() == JsonDataType.ARRAY);
	// true
	final JsonArray jsonArray = (JsonArray) nodevalue.getValue();
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

For other simple examples see test class "de.soderer.utilities.json.JsonTest":
https://github.com/hudeany/json/blob/master/src/test/java/de/soderer/utilities/json/JsonTest.java
  
## Maven2 repository
This library is also available via Maven2 repository
 
	<repositories>
		<repository>
			<id>de.soderer</id>
			<url>http://soderer.de/maven2</url>
		</repository>
	</repositories>

	<dependency>
		<groupId>de.soderer</groupId>
		<artifactId>json</artifactId>
		<version>RELEASE</version>
	</dependency>
