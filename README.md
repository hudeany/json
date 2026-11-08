# JSON & YAML for Java

[![Maven Central](https://img.shields.io/maven-central/v/de.soderer/json)](https://central.sonatype.com/artifact/de.soderer/json)

A lightweight Java library to read, write, validate and compare **JSON**, **JSON5** and **YAML** data, without any external dependencies.

## Features

- **JSON and JSON5**: read and write documents, or stream them token by token
- **YAML**: read and write documents while keeping comments, anchors, quote styles and empty lines
- **YAML 1.2 and 1.1**: the YAML 1.2 core schema by default, the rules of YAML 1.1 (`yes`/`no`, `017` as octal) on demand
- **Sequential reading**: process huge arrays and sequences item by item, like a SAX parser for XML
- **JSON Schema validation**: drafts v3, v4, v6, v7, 2019-09 and 2020-12 (see [json-schema.org](https://json-schema.org)), tested against the official [JSON-Schema-Test-Suite](https://github.com/json-schema-org/JSON-Schema-Test-Suite), also applicable to YAML data; stop at the first error or collect all errors
- **JSON path**: navigate data with paths like `$.address.city`
- **Conversion** between JSON and YAML, with resolution of YAML aliases and merge keys (`<<`)
- **JSON Patch and JSON Merge Patch**: apply and create patches as defined by RFC 6902 and RFC 7396
- **Diff and patch**: compare two data trees, render the differences as text and apply them as a patch
- **Safe for untrusted data**: limits for the nesting depth, for the expansion of YAML aliases and for the evaluation of regular expressions in JSON schemas protect against stack overflows, "billion laughs" data and ReDoS

## Contents

- [Installation](#installation)
- [JSON](#json)
  - [Read and navigate](#read-and-navigate)
  - [Build and write](#build-and-write)
  - [Streaming write](#streaming-write)
  - [Sequential read of large arrays](#sequential-read-of-large-arrays)
  - [JSON5](#json5)
  - [JSON Schema validation](#json-schema-validation)
  - [Collect all validation errors](#collect-all-validation-errors)
  - [JSON Schema drafts 2019-09 and 2020-12](#json-schema-drafts-2019-09-and-2020-12)
- [YAML](#yaml)
  - [Read YAML](#read-yaml)
  - [YAML 1.2 and YAML 1.1](#yaml-12-and-yaml-11)
  - [Write YAML](#write-yaml)
  - [Anchors, tags and binary data](#anchors-tags-and-binary-data)
  - [Sequential read of large sequences](#sequential-read-of-large-sequences)
- [Convert between YAML and JSON](#convert-between-yaml-and-json)
- [JSON Patch and JSON Merge Patch](#json-patch-and-json-merge-patch)
- [Diff and patch](#diff-and-patch)
- [Untrusted data](#untrusted-data)
- [Tests](#tests)
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

The JSON classes are in the package `de.soderer.json`, JSON path in `de.soderer.json.path`, JSON Schema in `de.soderer.json.schema` and JSON Patch in `de.soderer.json.patch`.

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
try (JsonWriter writer = new JsonWriter(output)) {
	writer.setUglify(true);
	writer.add(person);
}
System.out.println(output.toString(StandardCharsets.UTF_8));
// {"name":"Alice","age":42,"tags":["admin","dev"]}
```

Standard JSON has no representation for `NaN` and infinite numbers, so `JsonWriter` writes them as `null` like JavaScript's `JSON.stringify`. With `setNonFiniteNumbersAsJson5(true)` they are written as `NaN`, `Infinity` and `-Infinity` of the JSON5 syntax.

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
	System.out.println(e.getJsonSchemaPath());
	// $.properties.age.minimum
}
```

The schema version is taken from `$schema`. Without `$schema`, a strict simple mode is used, in which unknown keywords are errors. To choose a version explicitly, pass a configuration:

```java
new JsonSchema(schemaInputStream, new JsonSchemaConfiguration().withJsonSchemaVersion(JsonSchemaVersion.draftV7));
```

Supported versions are `draftV3`, `draftV4`, `draftV6`, `draftV7`, `draft2019_09`, `draft2020_12` and `simple`.

A `JsonSchema` can be shared: several threads may validate data with the same instance at the same time.

### Collect all validation errors

`validate()` stops at the first error, which is the fastest way to check data. To report all errors at once, e.g. for a configuration file, use `validateAll()`:

```java
final String schema = """
		{
		  "$schema": "https://json-schema.org/draft/2020-12/schema",
		  "type": "object",
		  "properties": {
		    "name": {"type": "string", "minLength": 3},
		    "port": {"type": "integer", "maximum": 65535},
		    "server": {"type": "object", "required": ["host"]},
		    "mode": {"anyOf": [{"const": "fast"}, {"type": "integer"}]}
		  },
		  "additionalProperties": false
		}""";
final JsonSchema jsonSchema = new JsonSchema(new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8)));

final JsonSchemaValidationResult result = jsonSchema.validateAll(JsonReader.readJsonItemString(
		"{\"name\": \"x\", \"port\": 70000, \"server\": \"text\", \"mode\": \"slow\", \"extra\": true}"));
for (final JsonSchemaDataValidationError error : result.getErrors()) {
	System.out.println(error.getJsonDataPath() + " (" + error.getJsonSchemaPath() + ")");
}
// $.name ($.properties.name.minLength)
// $.port ($.properties.port.maximum)
// $.server ($.properties.server.type)
// $.mode ($.properties.mode.anyOf)
// $ ($.additionalProperties)
```

Errors that only follow from a wrong data type are not reported: `server` is no object, so its missing `host` is no separate error. A failing `anyOf` or `oneOf` is one error, whose causes are the errors of its options (`error.getSubSchemaErrors()`). At most 100 errors are reported by default, so huge invalid data cannot exhaust the memory; `result.isTruncated()` shows whether there were more. The limit is set by `JsonSchemaConfiguration.withMaximumValidationErrors(int)` or `jsonSchema.setMaximumValidationErrors(int)`.

### JSON Schema drafts 2019-09 and 2020-12

All keywords of the newer drafts are supported, e.g. `$defs`, `$anchor`, `prefixItems`, `unevaluatedProperties`, `unevaluatedItems`, `dependentRequired`, `dependentSchemas`, `minContains`/`maxContains`, `$dynamicRef` (2020-12) and `$recursiveRef` (2019-09).

```java
final String schema = """
		{
		  "$schema": "https://json-schema.org/draft/2020-12/schema",
		  "$defs": {
		    "point": {"prefixItems": [{"type": "number"}, {"type": "number"}], "items": false}
		  },
		  "type": "object",
		  "properties": {
		    "name": {"type": "string"},
		    "position": {"$ref": "#/$defs/point"}
		  },
		  "unevaluatedProperties": false
		}""";
final JsonSchema jsonSchema = new JsonSchema(new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8)));

jsonSchema.validate(new ByteArrayInputStream("{\"name\": \"A\", \"position\": [1, 2]}".getBytes(StandardCharsets.UTF_8)));

try {
	jsonSchema.validate(new ByteArrayInputStream("{\"name\": \"A\", \"color\": \"red\"}".getBytes(StandardCharsets.UTF_8)));
} catch (final JsonSchemaDataValidationError e) {
	System.out.println(e.getMessage());
	// Invalid JSON data: Unevaluated property 'color' is not valid at JSON path: $.color
}
```

References (`$ref`) are resolved by URI as defined by the standard: `$id` may change the base URI in any sub schema, and plain name anchors (`$anchor`, `$dynamicAnchor`) and JSON pointers (`#/$defs/point`) are supported. Additional schemas are passed as `JsonSchemaDependency`, registered by their URI. A referenced schema declaring another draft by `$schema` is evaluated by the rules of its own draft, e.g. a draft 7 schema referenced by a draft 2020-12 schema. The meta schemas of all supported drafts are included, so references to them need no download. Downloads of other referenced schemas are disabled by default and can be enabled by `JsonSchemaConfiguration.withDownloadReferencedSchemas(true)`.

Since draft 2019-09, `format` is only an annotation by default, as defined by the standard. To validate formats like `email`, `date-time`, `ipv4`, `uri` or `uuid` anyway:

```java
new JsonSchema(schemaInputStream, new JsonSchemaConfiguration().withFormatAssertion(true));
```

A JSON schema itself can be checked against the meta schema of its draft:

```java
JsonUtilities.validateJsonSchemaDraft2020_12(schemaInputStream, StandardCharsets.UTF_8);
JsonUtilities.validateJsonSchema(schemaInputStream, StandardCharsets.UTF_8, JsonSchemaVersion.draft2019_09);
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

### YAML 1.2 and YAML 1.1

Plain (unquoted) values are resolved by the core schema of YAML 1.2 by default: only `true` and `false` are booleans, `017` is the decimal number 17 and octal numbers need the prefix `0o`. Values like `yes`, `on`, `y` or `1_000` are strings. This avoids surprises like the country code `NO` of Norway being read as `false`.

Documents starting with the directive `%YAML 1.1` are read by the rules of YAML 1.1, in which `yes`/`no`, `on`/`off` and `y`/`n` are booleans and `017` is octal. To read documents without directive by these rules, set the default version:

```java
try (YamlReader reader = new YamlReader(inputStream)) {
	reader.setDefaultYamlVersion(YamlVersion.V1_1);
	final YamlDocument document = reader.readDocument();
}
```

The writer quotes all strings that are booleans or numbers in either version, so its output is read the same way by YAML 1.1 and YAML 1.2 readers.

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

Strings are quoted automatically where needed, e.g. `"true"`, `"yes"`, `"0x1F"` or `"a: b"`. The output can be customized by a `YamlFormat`, for example:

```java
new YamlFormat()
	.withIndentationSize(4)
	.withStringValueQuoteType(YamlStringQuoteType.SINGLE)
	.withOmitComments(true);
```

### Anchors, tags and binary data

Anchors (`&name`) and tags (`!tag`) are kept when reading and written again. Properties on a line of their own belong to the following node, properties before a key on the same line belong to that key:

```java
final YamlMapping root = (YamlMapping) YamlReader.readDocument("""
		defaults: &defaults
		  timeout: 30
		&primary !server main: db1
		backup: *primary
		""").getRoot();
System.out.println(root.get("defaults").getAnchorName()); // defaults
for (final YamlNode key : root.keySet()) {
	System.out.println(key + " " + key.getAnchorName() + " " + key.getTag());
}
// defaults null null
// main primary !server
// backup null null
```

An anchor may be defined again: aliases refer to its latest definition before them. Datatype tags like `!!str` or `!!int` are applied to the value instead of being stored. `!!binary` marks a base64 text as binary data:

```java
final YamlScalar data = YamlScalar.fromBinaryValue(new byte[] { 1, 2, 3 }); // written as: !!binary AQID
final byte[] bytes = data.getBinaryValue();
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

## JSON Patch and JSON Merge Patch

The package `de.soderer.json.patch` implements the standard patch formats, e.g. for HTTP `PATCH` requests. A JSON Patch (RFC 6902) is a list of operations at JSON Pointer locations (RFC 6901):

```java
final JsonNode document = JsonReader.readJsonItemString("{\"name\": \"Alice\", \"tags\": [\"admin\", \"dev\"]}");
final JsonArray patch = (JsonArray) JsonReader.readJsonItemString("""
		[
		  {"op": "test", "path": "/name", "value": "Alice"},
		  {"op": "replace", "path": "/name", "value": "Bob"},
		  {"op": "remove", "path": "/tags/0"},
		  {"op": "add", "path": "/tags/-", "value": "ops"}
		]""");
final JsonNode patched = JsonPatch.apply(document, patch);
// {"name": "Bob", "tags": ["dev", "ops"]}
```

The patch is applied atomically: the original document is not changed, and if an operation fails (e.g. a `test`), a `JsonPatchException` names the failing operation and no result is returned. `JsonPatch.create(source, target)` creates a patch that changes one document into another.

A JSON Merge Patch (RFC 7396) describes the changes by example, `null` removes a property:

```java
final JsonNode merged = JsonMergePatch.apply(document, JsonReader.readJsonItemString("{\"name\": \"Bob\", \"tags\": null}"));
// {"name": "Bob"}
```

`JsonMergePatch.create(source, target)` creates a merge patch. Merge patches cannot set a value to `null` or change single array items; use a JSON Patch for that.

## Diff and patch

`JsonComparator` and `YamlComparator` list the differences between two data trees. Paths use dot notation for keys and brackets for array indexes, e.g. `servers[2].name`. The text form can be stored or reviewed and applied as a patch later. Before changing a value, the patch checks the old value and throws a `PatchConflictException` if it differs.

```java
final JsonNode before = JsonReader.readJsonItemString("{\"name\": \"Alice\", \"age\": 41}");
final JsonNode after = JsonReader.readJsonItemString("{\"name\": \"Alice\", \"age\": 42, \"email\": \"alice@example.com\"}");

final String diffText = JsonComparator.renderAsText(new JsonComparator().compare(before, after));
System.out.print(diffText);
// ~ age: "41" -> "42"
// + email: "alice@example.com"

JsonDiffPatcher.applyPatch(before, JsonDiffPatcher.parseDiffText(diffText));
System.out.println(before.equals(after)); // true
```

`YamlComparator` and `YamlDiffPatcher` work the same way for YAML. For patches exchanged with other systems, use the standard formats [JSON Patch and JSON Merge Patch](#json-patch-and-json-merge-patch).

## Untrusted data

Data from untrusted sources can be read safely, because the following limits prevent a single document from exhausting the stack or the memory:

| Limit | Default | Configuration |
|---|---|---|
| Nesting depth of JSON objects and arrays | 500 | `JsonReader.setMaximumNestingDepth(int)` |
| Nesting depth of YAML mappings and sequences | 500 | `YamlReader.setMaximumNestingDepth(int)` |
| Nodes created by YAML aliases and merge keys ("billion laughs") | 100,000 | `YamlToJsonConverter.convert(document, int)` |
| Errors reported by `JsonSchema.validateAll()` | 100 | `JsonSchemaConfiguration.withMaximumValidationErrors(int)` |
| Steps of the regex engine for `pattern` and `patternProperties` of a JSON schema (ReDoS) | 10,000,000 plus 1,000 per character of the data | `JsonSchemaConfiguration.withRegexStepLimit(long)`, 0 for no limit |

Exceeding a limit throws a `MaximumNestingDepthExceededException`, `YamlParseException` or `YamlAliasExpansionLimitException` instead of a `StackOverflowError` or `OutOfMemoryError`. Data for which a regular expression exceeds its limit is reported as invalid by the JSON schema validation, so a schema from an untrusted source cannot block the validation by a pattern with catastrophic backtracking. Very long data that exceeds the recursion depth of the Java regex engine is also reported as invalid, for `pattern` as well as for the built-in formats like `email` or `json-pointer`. Downloads of schemas referenced by URL are disabled by default.

## Tests

The implementations are tested against the official test suites, which run as unit tests:

- **JSON Schema**: the [JSON-Schema-Test-Suite](https://github.com/json-schema-org/JSON-Schema-Test-Suite) for all supported drafts, both with `validate()` and `validateAll()` ([JsonSchemaTestSuiteTest.java](https://github.com/hudeany/json/blob/master/src/test/java/de/soderer/json/schema/JsonSchemaTestSuiteTest.java)). All required and all optional tests pass.
- **JSON Patch**: the [json-patch-tests](https://github.com/json-patch/json-patch-tests) and the examples of RFC 7396 ([JsonPatchTest.java](https://github.com/hudeany/json/blob/master/src/test/java/de/soderer/json/patch/JsonPatchTest.java)). All tests pass.
- **YAML**: the [yaml-test-suite](https://github.com/yaml/yaml-test-suite) ([YamlTestSuiteTest.java](https://github.com/hudeany/json/blob/master/src/test/java/de/soderer/yaml/YamlTestSuiteTest.java)). Currently 200 of 308 valid inputs are read as expected and 32 of 94 invalid inputs are rejected. The known deviations, mostly rare syntax variants and the rejection of invalid input, are listed in [yaml-test-suite-known-issues.txt](https://github.com/hudeany/json/blob/master/src/test/resources/yaml/yaml-test-suite-known-issues.txt).

## More examples

The unit tests show many more use cases:

- [JsonTest.java](https://github.com/hudeany/json/blob/master/src/test/java/de/soderer/json/JsonTest.java)
- [YamlTest.java](https://github.com/hudeany/json/blob/master/src/test/java/de/soderer/yaml/YamlTest.java)
- [JsonSchemaNewDraftsTest.java](https://github.com/hudeany/json/blob/master/src/test/java/de/soderer/json/schema/JsonSchemaNewDraftsTest.java) for the JSON Schema drafts 2019-09 and 2020-12
- [JsonSchemaValidateAllTest.java](https://github.com/hudeany/json/blob/master/src/test/java/de/soderer/json/schema/JsonSchemaValidateAllTest.java) for collecting all validation errors
- [YamlVersionAndTagTest.java](https://github.com/hudeany/json/blob/master/src/test/java/de/soderer/yaml/YamlVersionAndTagTest.java) and [YamlAnchorTest.java](https://github.com/hudeany/json/blob/master/src/test/java/de/soderer/yaml/YamlAnchorTest.java) for YAML versions, tags and anchors
