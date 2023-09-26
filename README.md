# Java JsonReader, JsonWriter and JsonSchema

Read and write JSON data from and to files or streams.  
Validation by JsonSchema (see http://json-schema.org for specifications).  
Sequential read of JsonArray items (like SAX parser for XML data).  

For simple examples see test class "de.soderer.utilities.json.JsonTest".  

## JsonWriter example
```
JsonWriter writer = null;
		ByteArrayOutputStream output = null;
		JsonReader jsonReader = null;
		try {
			output = new ByteArrayOutputStream();
			writer = new JsonWriter(output, StandardCharsets.UTF_8);
			writer.openJsonArray();
			writer.closeJsonArray();
			writer.close();
			output.close();
			final String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
			Assert.assertEquals("[]", result);
			jsonReader = new JsonReader(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
			Assert.assertNotNull(jsonReader.read());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(output);
			Utilities.closeQuietly(writer);
			Utilities.closeQuietly(jsonReader);
		}
```
  
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
