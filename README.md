# JSON & YAML for Java

[![Maven Central](https://img.shields.io/maven-central/v/de.soderer/json)](https://central.sonatype.com/artifact/de.soderer/json)

A lightweight Java library to read, write, validate and compare **JSON**, **JSON5** and **YAML** data, without any external dependencies.

## Features

- **JSON and JSON5**: read and write documents, or stream them token by token
- **YAML**: read and write documents while keeping comments, anchors, quote styles and empty lines
- **Sequential reading**: process huge arrays and sequences item by item, like a SAX parser for XML
- **JSON Schema validation**: drafts v4, v6 and v7 (see [json-schema.org](https://json-schema.org)), also applicable to YAML data
- **JSON path**: navigate data with paths like `$.address.city`
- **Conversion** between JSON and YAML, with resolution of YAML aliases and merge keys (`<<`)
- **Diff and patch**: compare two data trees, render the differences as text and apply them as a patch

## Contents

- [Installation](#installation)
- [JSON](#json)
  - [Read and navigate](#read-and-navigate)
  - [Build and write](#build-and-write)
  - [Streaming write](#streaming-write)
  - [Sequential read of large arrays](#sequential-read-of-large-arrays)
  - [JSON5](#json5)
  - [JSON Schema validation](#json-schema-validation)
- [YAML](#yaml)
  - [Read YAML](#read-yaml)
  - [Write YAML](#write-yaml)
  - [Sequential read of large sequences](#sequential-read-of-large-sequences)
- [Convert between YAML and JSON](#convert-between-yaml-and-json)
- [Diff and patch](#diff-and-patch)
- [More examples](#more-examples)

## Installation

The library is available on Maven Central. Replace `VERSION` with the version shown in the badge above.

**Maven**

```xml
<dependency>
	<groupId>de.soderer</groupId>
	<artifactId>json</artifactId>
	<version>VERSION</version>
</dependency>
```

**Gradle**

```groovy
implementation "de.soderer:json:VERSION"
```

**Without a build tool**, download the jar from the [GitHub releases](https://github.com/hudeany/json/releases).

## JSON

The JSON classes are in the package `de.soderer.json`, JSON path in `de.soderer.json.path` and JSON Schema in `de.soderer.json.schema`.

### Read and navigate

```java
final String json = "{\"name\": \"Alice\", \"age\": 42, \"tags\": [\"admin\", \"dev\"], \"address\": {\"city\": \"Munich\"}}";
final JsonObject person = (JsonObject) JsonReader.readJsonItemString(json);

System.out.println(person.getSimpleValue("name"));    // Alice
System.out.println(person.getSimpleValue("age"));     // 42
System.out.println(((JsonArray) person.get("tags")).size()); // 2

// Navigate by JSON path ("$" refers to the root node)
final JsonNode city = person.withRootNode(true).getDataByJsonPath(new JsonPath("$.address.city"));
System.out.println(((JsonValueString) city).getValue()); // Munich
```

To read from a file or stream, use `new JsonReader(inputStream).read()`.

### Build and write

```java
final JsonObject person = new JsonObject()
	.add("name", "Alice")
	.add("age", 42)
	.add("tags", new JsonArray().add("admin").add("dev"));

// Formatted output with tab indentation
System.out.println(person);

// Compact output into any OutputStream
final ByteArrayOutputStream output = new ByteArrayOutputStream();
try (JsonWriter writer = new JsonWriter(output).withUglify(true)) {
	writer.add(person);
}
System.out.println(output.toString(StandardCharsets.UTF_8));
// {"name":"Alice","age":42,"tags":["admin","dev"]}
```

### Streaming write

Large data can be written step by step without building the whole tree in memory. Methods called in a wrong order throw a `JsonWriterStateException`.

```java
try (JsonWriter writer = new JsonWriter(outputStream)) {
	writer.openJsonArray();
	for (int i = 1; i <= 2; i++) {
		writer.openJsonObject();
		writer.openJsonObjectProperty("id");
		writer.addSimpleJsonObjectPropertyValue(i);
		writer.openJsonObjectProperty("name");
		writer.addSimpleJsonObjectPropertyValue("Item " + i);
		writer.closeJsonObject();
	}
	writer.closeJsonArray();
}
```

Output:

```json
[
	{
		"id": 1,
		"name": "Item 1"
	},
	{
		"id": 2,
		"name": "Item 2"
	}
]
```

### Sequential read of large arrays

Only one item at a time is held in memory, so arrays of any size can be processed.

```java
final String data = "{\"customers\": [{\"name\": \"Alice\"}, {\"name\": \"Bob\"}]}";
try (JsonReader reader = new JsonReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)))) {
	reader.readUpToJsonPath("$.customers");
	reader.readNextToken(); // consume the opening "["

	JsonNode customer;
	while ((customer = reader.readNextJsonNode()) != null) {
		System.out.println(((JsonObject) customer).getSimpleValue("name"));
	}
}
// Alice
// Bob
```

### JSON5

`Json5Reader` additionally accepts the [JSON5](https://json5.org) syntax: comments, unquoted keys, single quotes, hexadecimal numbers, trailing commas and more.

```java
final String json5 = """
		{
		  // comments are allowed
		  unquoted: 'single quotes',
		  hex: 0x1F,
		  trailingComma: [1, 2,],
		}""";
final JsonObject object = (JsonObject) Json5Reader.readJsonItemString(json5);
System.out.println(object.getSimpleValue("unquoted") + ", " + object.getSimpleValue("hex"));
// single quotes, 31
```

### JSON Schema validation

```java
final String schema = """
		{
		  "type": "object",
		  "required": ["name"],
		  "properties": {
		    "name": {"type": "string"},
		    "age": {"type": "integer", "minimum": 0}
		  }
		}""";
final JsonSchema jsonSchema = new JsonSchema(new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8)));

// Valid data: returns the data read
jsonSchema.validate(new ByteArrayInputStream("{\"name\": \"Alice\", \"age\": 42}".getBytes(StandardCharsets.UTF_8)));

// Invalid data: the exception names the rule and the position
try {
	jsonSchema.validate(new ByteArrayInputStream("{\"name\": \"Bob\", \"age\": -1}".getBytes(StandardCharsets.UTF_8)));
} catch (final JsonSchemaDataValidationError e) {
	System.out.println(e.getMessage());
	// Invalid JSON data: Minimum number is '0' but value was '-1' at JSON path: $.age
}
```

The schema version is taken from `$schema`. Without `$schema`, a strict simple mode is used, in which unknown keywords are errors. To choose a version explicitly, pass a configuration:

```java
new JsonSchema(schemaInputStream, new JsonSchemaConfiguration().withJsonSchemaVersion(JsonSchemaVersion.draftV7));
```

## YAML

The YAML classes are in the packages `de.soderer.yaml` and `de.soderer.yaml.data`.

### Read YAML

Comments are kept when reading, so a document can be changed and written back without losing them.

```java
final String yaml = """
		# Server configuration
		server:
		  host: example.com
		  port: 8080
		  tls: true
		users:
		  - alice
		  - bob
		""";
final YamlDocument document = YamlReader.readDocument(yaml);
final YamlMapping root = (YamlMapping) document.getRoot();

final YamlMapping server = (YamlMapping) root.get("server");
System.out.println(server.getSimpleValue("host") + ":" + server.getSimpleValue("port")); // example.com:8080
System.out.println(((YamlSequence) root.get("users")).simpleItems());                  // [alice, bob]

// Writing the document again keeps the comment
System.out.print(document);
```

To read from a file or stream, use `new YamlReader(inputStream).readDocument()`. Documents separated by `---` are returned one by one by repeated calls.

### Write YAML

```java
final YamlMapping config = new YamlMapping()
	.add("name", "demo")
	.add("version", 1.5)
	.add("enabled", true)
	.add("ports", new YamlSequence().add(80).add(443))
	.add("limits", new YamlMapping(true).add("cpu", 2).add("memory", "512M")); // flow style

try (YamlWriter writer = new YamlWriter(outputStream)) {
	writer.writeDocument(new YamlDocument(config));
}
```

Output:

```yaml
name: demo
version: 1.5
enabled: true
ports:
  - 80
  - 443
limits: {cpu: 2, memory: 512M}
```

Strings are quoted automatically where needed, e.g. `"true"`, `"0x1F"` or `"a: b"`. The output can be customized by a `YamlFormat`, for example:

```java
new YamlFormat()
	.withIndentationSize(4)
	.withStringValueQuoteType(YamlStringQuoteType.SINGLE)
	.withOmitComments(true);
```

### Sequential read of large sequences

```java
final String yaml = """
		level1:
		  items:
		    - name: first
		    - name: second
		""";
try (YamlReader reader = new YamlReader(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))) {
	reader.readUpToPath("$.level1.items");

	YamlNode item;
	while ((item = reader.readNextYamlNode()) != null) {
		System.out.println(((YamlMapping) item).getSimpleValue("name"));
	}
}
// first
// second
```

## Convert between YAML and JSON

`YamlToJsonConverter` resolves aliases and merge keys (`<<`), so YAML configurations with shared defaults convert as expected:

```java
final String yaml = """
		defaults: &defaults
		  timeout: 30
		  retries: 3
		production:
		  <<: *defaults
		  retries: 5
		""";
final JsonObject json = (JsonObject) YamlToJsonConverter.convert(YamlReader.readDocument(yaml));
System.out.println(json.get("production"));
// {
// 	"timeout": 30,
// 	"retries": 5
// }
```

The other direction works with `JsonToYamlConverter.convert(jsonNode)`.

YAML data can also be validated against a JSON Schema, e.g. with `YamlUtilities.validateJsonSchemaOnYamlDataV7(yamlInputStream, schemaInputStream)`.

## Diff and patch

`JsonComparator` and `YamlComparator` list the differences between two data trees. The text form can be stored or reviewed and applied as a patch later. Before changing a value, the patch checks the old value and throws a `PatchConflictException` if it differs.

```java
final JsonNode before = JsonReader.readJsonItemString("{\"name\": \"Alice\", \"roles\": [\"dev\"], \"age\": 41}");
final JsonNode after = JsonReader.readJsonItemString("{\"name\": \"Alice\", \"roles\": [\"dev\", \"admin\"], \"age\": 42}");

final String diffText = JsonComparator.renderAsText(new JsonComparator().compare(before, after));
System.out.print(diffText);
// + roles[1]: "admin"
// ~ age: "41" -> "42"

JsonDiffPatcher.applyPatch(before, JsonDiffPatcher.parseDiffText(diffText));
System.out.println(before.equals(after)); // true
```

`YamlComparator` and `YamlDiffPatcher` work the same way for YAML.

## More examples

The unit tests show many more use cases:

- [JsonTest.java](https://github.com/hudeany/json/blob/master/src/test/java/de/soderer/json/JsonTest.java)
- [YamlTest.java](https://github.com/hudeany/json/blob/master/src/test/java/de/soderer/yaml/YamlTest.java)
