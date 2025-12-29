package de.soderer.json;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaConfiguration;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaDefinitionError;
import de.soderer.json.schema.JsonSchemaPath;
import de.soderer.json.schema.JsonSchemaVersion;
import de.soderer.json.utilities.Triple;
import de.soderer.json.utilities.Utilities;

@SuppressWarnings("static-method")
public class JsonSchemaSuiteGitHubTest {
	public static final boolean VERBOSE_OUT_STRING = true;
	public static final boolean STOP_ON_FAIL = false;
	public static final boolean DOWNLOAD = false;
	public static final File DOWNLOADED_TESTSUITE_FILE = new File(Utilities.replaceUsersHome("$HOME/Downloads/JSON-Schema-Test-Suite-23.1.0.zip"));
	public static final String TESTSUITE_DOWNLOADURL = "https://github.com/json-schema-org/JSON-Schema-Test-Suite/archive/refs/tags/23.1.0.zip";

	public static final String SINGLE_TEST_TO_EXECUTE = null; //"JSON-Schema-Test-Suite-23.1.0/tests/draft7/propertyNames.json:propertyNames validation with pattern:object without properties is valid";

	public static Map<String, JsonSchemaVersion> TESTVERSIONMAP = Stream.of(new Object[][] {
		{ "JSON-Schema-Test-Suite-23.1.0/tests/draft3", JsonSchemaVersion.draftV3 },
		{ "JSON-Schema-Test-Suite-23.1.0/tests/draft4", JsonSchemaVersion.draftV4 },
		{ "JSON-Schema-Test-Suite-23.1.0/tests/draft6", JsonSchemaVersion.draftV6 },
		{ "JSON-Schema-Test-Suite-23.1.0/tests/draft7", JsonSchemaVersion.draftV7 },

		//		{ "JSON-Schema-Test-Suite-23.1.0/tests/draft2019-09", JsonSchemaVersion.v2019_09 },
		//		{ "JSON-Schema-Test-Suite-23.1.0/tests/draft2020-12", JsonSchemaVersion.v2010_20 }
	}).collect(Collectors.toMap(data -> (String) data[0], data -> (JsonSchemaVersion) data[1]));

	public static int SKIPPED_SCHEMAS = 0;
	public static List<String> KNOWN_SCHEMA_ISSUES_TO_SKIP = Arrays.asList(new String[] {
			"JSON-Schema-Test-Suite-23.1.0/tests/draft3/ref.json:$ref prevents a sibling id from changing the base uri",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft3/refRemote.json:remote ref",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft3/refRemote.json:change resolution scope",

			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/id.json:id inside an enum is not a real identifier",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:patterns always use unicode semantics with patternProperties",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:patternProperties with non-ASCII digits",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/ref.json:$ref prevents a sibling id from changing the base uri",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/ref.json:id must be resolved against nearest parent, not just immediate parent",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/refRemote.json:remote ref",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/refRemote.json:base URI change",

			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/id.json:id inside an enum is not a real identifier",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:patterns always use unicode semantics with patternProperties",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:\\w in patternProperties matches [A-Za-z0-9_], not unicode letters",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:patternProperties with ASCII ranges",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:\\d in patternProperties matches [0-9], not unicode digits",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:patternProperties with non-ASCII digits",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/ref.json:$ref prevents a sibling $id from changing the base uri",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/ref.json:refs with relative uris and defs",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/ref.json:relative refs with absolute uris and defs",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/ref.json:simple URN base URI with $ref via the URN",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/ref.json:ref with absolute-path-reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/refRemote.json:remote ref",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/refRemote.json:base URI change",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/refRemote.json:remote ref with ref to definitions",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/refRemote.json:retrieved nested refs resolve relative to their URI not $id",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/unknownKeyword.json:$id inside an unknown keyword is not a real identifier",

			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/id.json:id inside an enum is not a real identifier",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/cross-draft.json:refs to future drafts are processed as future drafts",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:patterns always use unicode semantics with patternProperties",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:\\w in patternProperties matches [A-Za-z0-9_], not unicode letters",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:patternProperties with ASCII ranges",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:\\d in patternProperties matches [0-9], not unicode digits",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:patternProperties with non-ASCII digits",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:$ref prevents a sibling $id from changing the base uri",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:refs with relative uris and defs",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:relative refs with absolute uris and defs",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:$id must be resolved against nearest parent, not just immediate parent",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:simple URN base URI with $ref via the URN",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:ref to if",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:ref to then",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:ref to else",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:ref with absolute-path-reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/refRemote.json:remote ref",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/refRemote.json:base URI change",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/refRemote.json:remote ref with ref to definitions",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/refRemote.json:retrieved nested refs resolve relative to their URI not $id",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/unknownKeyword.json:$id inside an unknown keyword is not a real identifier",
	});

	public static List<String> KNOWN_TEST_ISSUES_TO_SKIP = Arrays.asList(new String[] {
			"JSON-Schema-Test-Suite-23.1.0/tests/draft3/extends.json:multiple extends:mismatch first extends",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft3/extends.json:multiple extends:mismatch second extends",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft3/optional/ecmascript-regex.json:ECMA 262 regex dialect recognition:[^] is a valid regex",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft3/optional/ecmascript-regex.json:ECMA 262 regex dialect recognition:ECMA 262 has no support for lookbehind",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft3/optional/format/ipv6.json:validation of IPv6 addresses:mixed format with the ipv4 section as decimal octets",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft3/optional/format/uri.json:validation of URIs:an invalid protocol-relative URI Reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft3/optional/format/uri.json:validation of URIs:an invalid URI though valid URI reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft3/ref.json:ref overrides any sibling keywords:remote ref valid, maxItems ignored",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft3/refRemote.json:fragment within remote ref:remote fragment valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft3/refRemote.json:ref within remote ref:ref within ref valid",

			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:ECMA 262 regex escapes control codes with \\c and lower letter:matches",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:ECMA 262 \\s matches whitespace:latin-1 non-breaking-space matches",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:ECMA 262 \\s matches whitespace:zero-width whitespace matches",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:ECMA 262 \\s matches whitespace:paragraph separator matches (line terminator)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:ECMA 262 \\s matches whitespace:EM SPACE matches (Space_Separator)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:ECMA 262 \\S matches everything but whitespace:latin-1 non-breaking-space does not match",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:ECMA 262 \\S matches everything but whitespace:zero-width whitespace does not match",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:ECMA 262 \\S matches everything but whitespace:paragraph separator does not match (line terminator)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:ECMA 262 \\S matches everything but whitespace:EM SPACE does not match (Space_Separator)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:patterns always use unicode semantics with pattern:ascii character in json string",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:patterns always use unicode semantics with pattern:literal unicode character in json string",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:patterns always use unicode semantics with pattern:unicode character in hex format in string",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:patterns always use unicode semantics with pattern:unicode matching is case-sensitive",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:pattern with non-ASCII digits:ascii digits",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:pattern with non-ASCII digits:ascii non-digits",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/ecmascript-regex.json:pattern with non-ASCII digits:non-ascii digits (BENGALI DIGIT FOUR, BENGALI DIGIT TWO)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/format/date-time.json:validation of date-time strings:a valid date-time with a leap second, UTC",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/format/date-time.json:validation of date-time strings:a valid date-time with a leap second, with minus offset",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/format/ipv4.json:validation of IP addresses:invalid leading zeroes, as they are treated as octals",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/format/ipv6.json:validation of IPv6 addresses:mixed format with the ipv4 section as decimal octets",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/format/ipv6.json:validation of IPv6 addresses:zone id is not a part of ipv6 address",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/format/ipv6.json:validation of IPv6 addresses:a long valid ipv6",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/format/uri.json:validation of URIs:an invalid protocol-relative URI Reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/format/uri.json:validation of URIs:an invalid relative URI Reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/optional/format/uri.json:validation of URIs:an invalid URI though valid URI reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/ref.json:ref overrides any sibling keywords:ref valid, maxItems ignored",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/ref.json:Recursive references between schemas:valid tree",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/ref.json:refs with quote:object with numbers is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/ref.json:Location-independent identifier:match",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/ref.json:Location-independent identifier with base URI change in subschema:match",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/refRemote.json:fragment within remote ref:remote fragment valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/refRemote.json:ref within remote ref:ref within ref valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/refRemote.json:base URI change - change folder:number is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/refRemote.json:base URI change - change folder in subschema:number is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/refRemote.json:root ref in remote ref:string is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/refRemote.json:root ref in remote ref:null is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft4/refRemote.json:Location-independent identifier in remote ref:integer is valid",

			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/const.json:float and integers are equal up to 64-bit representation limits:float is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:ECMA 262 regex escapes control codes with \\c and lower letter:matches",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:ECMA 262 \\s matches whitespace:latin-1 non-breaking-space matches",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:ECMA 262 \\s matches whitespace:zero-width whitespace matches",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:ECMA 262 \\s matches whitespace:paragraph separator matches (line terminator)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:ECMA 262 \\s matches whitespace:EM SPACE matches (Space_Separator)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:ECMA 262 \\S matches everything but whitespace:latin-1 non-breaking-space does not match",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:ECMA 262 \\S matches everything but whitespace:zero-width whitespace does not match",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:ECMA 262 \\S matches everything but whitespace:paragraph separator does not match (line terminator)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:ECMA 262 \\S matches everything but whitespace:EM SPACE does not match (Space_Separator)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:patterns always use unicode semantics with pattern:ascii character in json string",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:patterns always use unicode semantics with pattern:literal unicode character in json string",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:patterns always use unicode semantics with pattern:unicode character in hex format in string",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:patterns always use unicode semantics with pattern:unicode matching is case-sensitive",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:pattern with non-ASCII digits:ascii digits",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:pattern with non-ASCII digits:ascii non-digits",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/ecmascript-regex.json:pattern with non-ASCII digits:non-ascii digits (BENGALI DIGIT FOUR, BENGALI DIGIT TWO)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/date-time.json:validation of date-time strings:a valid date-time with a leap second, UTC",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/date-time.json:validation of date-time strings:a valid date-time with a leap second, with minus offset",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/ipv4.json:validation of IP addresses:invalid leading zeroes, as they are treated as octals",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/ipv6.json:validation of IPv6 addresses:mixed format with the ipv4 section as decimal octets",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/ipv6.json:validation of IPv6 addresses:zone id is not a part of ipv6 address",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/ipv6.json:validation of IPv6 addresses:a long valid ipv6",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (~ not escaped)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (URI Fragment Identifier) #1",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (URI Fragment Identifier) #2",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (URI Fragment Identifier) #3",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (some escaped, but not all) #1",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (some escaped, but not all) #2",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (wrong escape character) #1",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (wrong escape character) #2",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (multiple characters not escaped)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (isn't empty nor starts with /) #1",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (isn't empty nor starts with /) #2",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (isn't empty nor starts with /) #3",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/uri-reference.json:validation of URI References:an invalid URI Reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/uri-reference.json:validation of URI References:an invalid URI fragment",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/uri-template.json:format: uri-template:an invalid uri-template",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/uri.json:validation of URIs:an invalid protocol-relative URI Reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/uri.json:validation of URIs:an invalid relative URI Reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/optional/format/uri.json:validation of URIs:an invalid URI though valid URI reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/ref.json:ref overrides any sibling keywords:ref valid, maxItems ignored",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/ref.json:$ref to boolean schema true:any value is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/ref.json:Recursive references between schemas:valid tree",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/ref.json:refs with quote:object with numbers is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/ref.json:Location-independent identifier:match",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/ref.json:Location-independent identifier with base URI change in subschema:match",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/ref.json:URN base URI with URN and JSON pointer ref:a string is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/ref.json:URN base URI with URN and anchor ref:a string is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/refRemote.json:fragment within remote ref:remote fragment valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/refRemote.json:ref within remote ref:ref within ref valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/refRemote.json:base URI change - change folder:number is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/refRemote.json:base URI change - change folder in subschema:number is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/refRemote.json:root ref in remote ref:string is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/refRemote.json:root ref in remote ref:null is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft6/refRemote.json:Location-independent identifier in remote ref:integer is valid",

			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/const.json:float and integers are equal up to 64-bit representation limits:float is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:ECMA 262 regex escapes control codes with \\c and lower letter:matches",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:ECMA 262 \\s matches whitespace:latin-1 non-breaking-space matches",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:ECMA 262 \\s matches whitespace:zero-width whitespace matches",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:ECMA 262 \\s matches whitespace:paragraph separator matches (line terminator)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:ECMA 262 \\s matches whitespace:EM SPACE matches (Space_Separator)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:ECMA 262 \\S matches everything but whitespace:latin-1 non-breaking-space does not match",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:ECMA 262 \\S matches everything but whitespace:zero-width whitespace does not match",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:ECMA 262 \\S matches everything but whitespace:paragraph separator does not match (line terminator)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:ECMA 262 \\S matches everything but whitespace:EM SPACE does not match (Space_Separator)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:patterns always use unicode semantics with pattern:ascii character in json string",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:patterns always use unicode semantics with pattern:literal unicode character in json string",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:patterns always use unicode semantics with pattern:unicode character in hex format in string",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:patterns always use unicode semantics with pattern:unicode matching is case-sensitive",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:pattern with non-ASCII digits:ascii digits",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:pattern with non-ASCII digits:ascii non-digits",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/ecmascript-regex.json:pattern with non-ASCII digits:non-ascii digits (BENGALI DIGIT FOUR, BENGALI DIGIT TWO)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/date-time.json:validation of date-time strings:a valid date-time with a leap second, UTC",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/date-time.json:validation of date-time strings:a valid date-time with a leap second, with minus offset",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:illegal first char U+302E Hangul single dot tone mark",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:contains illegal char U+302E Hangul single dot tone mark",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:Begins with a Spacing Combining Mark",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:Begins with a Nonspacing Mark",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:Begins with an Enclosing Mark",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:Exceptions that are DISALLOWED, left-to-right chars",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:MIDDLE DOT with no preceding 'l'",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:MIDDLE DOT with nothing preceding",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:MIDDLE DOT with no following 'l'",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:MIDDLE DOT with nothing following",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:Greek KERAIA not followed by Greek",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:Greek KERAIA not followed by anything",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:Hebrew GERESH not preceded by anything",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:Hebrew GERSHAYIM not preceded by anything",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:KATAKANA MIDDLE DOT with no Hiragana, Katakana, or Han",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:KATAKANA MIDDLE DOT with no other characters",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:Arabic-Indic digits mixed with Extended Arabic-Indic digits",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:ZERO WIDTH JOINER not preceded by Virama",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/idn-hostname.json:validation of internationalized host names:ZERO WIDTH JOINER not preceded by anything",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/ipv4.json:validation of IP addresses:invalid leading zeroes, as they are treated as octals",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/ipv6.json:validation of IPv6 addresses:mixed format with the ipv4 section as decimal octets",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/ipv6.json:validation of IPv6 addresses:zone id is not a part of ipv6 address",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/ipv6.json:validation of IPv6 addresses:a long valid ipv6",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/iri-reference.json:validation of IRI References:an invalid IRI Reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/iri-reference.json:validation of IRI References:an invalid IRI fragment",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/iri.json:validation of IRIs:an invalid IRI based on IPv6",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/iri.json:validation of IRIs:an invalid relative IRI Reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/iri.json:validation of IRIs:an invalid IRI though valid IRI reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (~ not escaped)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (URI Fragment Identifier) #1",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (URI Fragment Identifier) #2",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (URI Fragment Identifier) #3",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (some escaped, but not all) #1",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (some escaped, but not all) #2",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (wrong escape character) #1",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (wrong escape character) #2",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (multiple characters not escaped)",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (isn't empty nor starts with /) #1",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (isn't empty nor starts with /) #2",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/json-pointer.json:validation of JSON-pointers (JSON String Representation):not a valid JSON-pointer (isn't empty nor starts with /) #3",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/relative-json-pointer.json:validation of Relative JSON Pointers (RJP):an invalid RJP that is a valid JSON Pointer",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/relative-json-pointer.json:validation of Relative JSON Pointers (RJP):negative prefix",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/relative-json-pointer.json:validation of Relative JSON Pointers (RJP):explicit positive prefix",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/relative-json-pointer.json:validation of Relative JSON Pointers (RJP):## is not a valid json-pointer",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/relative-json-pointer.json:validation of Relative JSON Pointers (RJP):zero cannot be followed by other digits, plus json-pointer",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/relative-json-pointer.json:validation of Relative JSON Pointers (RJP):zero cannot be followed by other digits, plus octothorpe",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/relative-json-pointer.json:validation of Relative JSON Pointers (RJP):empty string",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/time.json:validation of time strings:a valid time string with leap second, Zulu",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/time.json:validation of time strings:valid leap second, zero time-offset",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/time.json:validation of time strings:valid leap second, positive time-offset",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/time.json:validation of time strings:valid leap second, large positive time-offset",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/time.json:validation of time strings:valid leap second, negative time-offset",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/time.json:validation of time strings:valid leap second, large negative time-offset",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/time.json:validation of time strings:a valid time string with plus offset",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/time.json:validation of time strings:a valid time string with minus offset",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/time.json:validation of time strings:a valid time string with case-insensitive Z",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/time.json:validation of time strings:no time offset",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/time.json:validation of time strings:no time offset with second fraction",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/uri-reference.json:validation of URI References:an invalid URI Reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/uri-reference.json:validation of URI References:an invalid URI fragment",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/uri-template.json:format: uri-template:an invalid uri-template",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/uri.json:validation of URIs:an invalid protocol-relative URI Reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/uri.json:validation of URIs:an invalid relative URI Reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/optional/format/uri.json:validation of URIs:an invalid URI though valid URI reference",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:ref overrides any sibling keywords:ref valid, maxItems ignored",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:$ref to boolean schema true:any value is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:Recursive references between schemas:valid tree",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:refs with quote:object with numbers is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:Location-independent identifier:match",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:Location-independent identifier with base URI change in subschema:match",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:URN base URI with URN and JSON pointer ref:a string is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/ref.json:URN base URI with URN and anchor ref:a string is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/refRemote.json:fragment within remote ref:remote fragment valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/refRemote.json:ref within remote ref:ref within ref valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/refRemote.json:base URI change - change folder:number is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/refRemote.json:base URI change - change folder in subschema:number is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/refRemote.json:root ref in remote ref:string is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/refRemote.json:root ref in remote ref:null is valid",
			"JSON-Schema-Test-Suite-23.1.0/tests/draft7/refRemote.json:Location-independent identifier in remote ref:integer is valid"
	});

	@Test
	public void testGitHubTestSuite() throws Exception {
		final Map<String, Map<String, Triple<Integer, Integer, Integer>>> testResults = new LinkedHashMap<>();
		final InputStream testSuiteInputSream = getTestSuiteInputSream();
		if (testSuiteInputSream != null) {
			try (final ZipInputStream stream = new ZipInputStream(new BufferedInputStream(testSuiteInputSream))) {
				ZipEntry entry;
				while((entry = stream.getNextEntry()) != null) {
					if (!entry.isDirectory()) {
						String parentPath = new File(entry.getName()).getParent().replace("\\", "/");
						if (parentPath.endsWith("/optional")) {
							parentPath = parentPath.substring(0, parentPath.length() - 9);
						} else if (parentPath.endsWith("/optional/format")) {
							parentPath = parentPath.substring(0, parentPath.length() - 16);
						}

						if (TESTVERSIONMAP.containsKey(parentPath)) {
							Triple<Integer, Integer, Integer> result = new Triple<>(0, 0, 0);

							final ByteArrayOutputStream testFileContent = new ByteArrayOutputStream();
							int len = 0;
							final byte[] buffer = new byte[1024];
							while ((len = stream.read(buffer)) > 0) {
								testFileContent.write(buffer, 0, len);
							}

							try (JsonReader jsonReader = new JsonReader(new ByteArrayInputStream(testFileContent.toByteArray()))) {
								for (final JsonNode jsonSchemaTest : ((JsonArray) jsonReader.read()).items()) {
									final Triple<Integer, Integer, Integer> nextResult = executeJsonSchemaTestCollection((JsonObject) jsonSchemaTest, TESTVERSIONMAP.get(parentPath), entry.getName().replace("\\", "/"));
									result = new Triple<>(result.getFirst() + nextResult.getFirst(), result.getSecond() + nextResult.getSecond(), result.getThird() + nextResult.getThird());
								}
							} catch (final Exception e) {
								if (STOP_ON_FAIL) {
									throw e;
								} else if (VERBOSE_OUT_STRING) {
									e.printStackTrace();
									System.out.println(new String(testFileContent.toByteArray()));
								}
								result = new Triple<>(result.getFirst(), result.getSecond(), result.getThird() + 1);
							}
							final Map<String, Triple<Integer, Integer, Integer>> keyMap = testResults.computeIfAbsent(parentPath, s -> new LinkedHashMap<>());
							keyMap.put(entry.getName(), result);
						}
					}
				}
			}
		}

		Triple<Integer, Integer, Integer> overallResult = new Triple<>(0, 0, 0);
		for (final Entry<String, Map<String, Triple<Integer, Integer, Integer>>> keyEntry : testResults.entrySet()) {
			Triple<Integer, Integer, Integer> directoryResult = new Triple<>(0, 0, 0);
			System.out.println("Directory: " + keyEntry.getKey());
			for (final Entry<String, Triple<Integer, Integer, Integer>> fileEntry : keyEntry.getValue().entrySet()) {
				System.out.println("File: " + fileEntry.getKey()
				+ " PASSED: " + fileEntry.getValue().getFirst()
				+ (fileEntry.getValue().getSecond() > 0 ? " SKIPPED: "  + fileEntry.getValue().getSecond() : "")
				+ (fileEntry.getValue().getThird() > 0 ? " FAILED: "  + fileEntry.getValue().getThird() : ""));
				directoryResult = new Triple<>(directoryResult.getFirst() + fileEntry.getValue().getFirst(), directoryResult.getSecond() + fileEntry.getValue().getSecond(), directoryResult.getThird() + fileEntry.getValue().getThird());
			}
			System.out.println("PASSED: " + directoryResult.getFirst()
			+ (directoryResult.getSecond() > 0 ? " SKIPPED: "  + directoryResult.getSecond() : "")
			+ (directoryResult.getThird() > 0 ? " FAILED: "  + directoryResult.getThird() : ""));
			System.out.println();
			overallResult = new Triple<>(overallResult.getFirst() + directoryResult.getFirst(), overallResult.getSecond() + directoryResult.getSecond(), overallResult.getThird() + directoryResult.getThird());
		}

		final int overallSum = overallResult.getFirst() + overallResult.getSecond() + overallResult.getThird();

		System.out.println("SKIPPED schema test collections: " + SKIPPED_SCHEMAS);
		System.out.println("TESTS: " + overallSum);
		System.out.println("Overall PASSED: " + overallResult.getFirst() + " (" + Math.round(overallResult.getFirst() * 100f / overallSum) + "%)"
				+ (overallResult.getSecond() > 0 ? " SKIPPED: "  + overallResult.getSecond() + " (" + Math.round(overallResult.getSecond() * 100f / overallSum) + "%)" : "")
				+ (overallResult.getThird() > 0 ? " FAILED: "  + overallResult.getThird() + " (" + Math.round(overallResult.getThird() * 100f / overallSum) + "%)" : ""));
		if (overallResult.getThird() > 0) {
			Assert.fail("Test failed. See output for reasons");
		}
	}

	private InputStream getTestSuiteInputSream() throws Exception {
		if (DOWNLOADED_TESTSUITE_FILE.exists()) {
			return new FileInputStream(DOWNLOADED_TESTSUITE_FILE);
		} else if (DOWNLOAD) {
			return URI.create(TESTSUITE_DOWNLOADURL).toURL().openStream();
		} else {
			return null;
		}
	}

	private Triple<Integer, Integer, Integer> executeJsonSchemaTestCollection(final JsonObject jsonSchemaTest, final JsonSchemaVersion jsonSchemaVersion, final String filename) throws Exception {
		final String collectionDescription = ((String) jsonSchemaTest.getSimpleValue("description")).trim();
		final JsonNode schemaJsonObject = jsonSchemaTest.get("schema");
		final JsonArray schemaTestsArray = (JsonArray) jsonSchemaTest.get("tests");

		if (SINGLE_TEST_TO_EXECUTE != null && !SINGLE_TEST_TO_EXECUTE.startsWith(filename + ":" + collectionDescription + ":")) {
			return new Triple<>(0, 0, 0);
		}

		if (KNOWN_SCHEMA_ISSUES_TO_SKIP.contains(filename + ":" + collectionDescription)) {
			SKIPPED_SCHEMAS++;
			return new Triple<>(0, 0, 0);
		} else {
			final JsonSchema jsonSchema;
			try {
				if (schemaJsonObject == null) {
					throw new JsonSchemaDefinitionError("JsonSchema is null", new JsonSchemaPath("$"));
				} else if (schemaJsonObject.isBoolean()) {
					jsonSchema = new JsonSchema(((JsonValueBoolean) schemaJsonObject).getValue(), new JsonSchemaConfiguration().setJsonSchemaVersion(jsonSchemaVersion).setDownloadReferencedSchemas(true));
				} else if (schemaJsonObject.isJsonObject()) {
					jsonSchema = new JsonSchema((JsonObject) schemaJsonObject, new JsonSchemaConfiguration().setJsonSchemaVersion(jsonSchemaVersion).setDownloadReferencedSchemas(true));
				} else {
					throw new JsonSchemaDefinitionError("JsonSchema is not JsonObject or Boolean", new JsonSchemaPath("$"));
				}
			} catch (final JsonSchemaDefinitionError e) {
				if (STOP_ON_FAIL) {
					System.out.println(filename);
					System.out.println(collectionDescription);
					System.out.println(schemaJsonObject);
					e.printStackTrace();
					throw new Exception("Not a valid JSON schema in test '" + collectionDescription + "': " + e.getMessage(), e);
				} else if (VERBOSE_OUT_STRING) {
					e.printStackTrace();
					return new Triple<>(0, 0, 1);
				} else {
					return new Triple<>(0, 0, 1);
				}
			}

			Triple<Integer, Integer, Integer> result = new Triple<>(0, 0, 0);
			for (final JsonNode test : schemaTestsArray.items()) {
				final String description = ((String) ((JsonObject) test).getSimpleValue("description")).trim();

				if (KNOWN_TEST_ISSUES_TO_SKIP.contains(filename + ":" + collectionDescription)
						|| KNOWN_TEST_ISSUES_TO_SKIP.contains(filename + ":" + collectionDescription + ":")
						|| KNOWN_TEST_ISSUES_TO_SKIP.contains(filename + ":" + collectionDescription + ":" + description)) {
					result = new Triple<>(result.getFirst(), result.getSecond() + 1, result.getThird());
				} else {
					try {
						executeJsonSchemaTest(collectionDescription, jsonSchema, filename, schemaJsonObject, (JsonObject) test);
						result = new Triple<>(result.getFirst() + 1, result.getSecond(), result.getThird());
					} catch (final Exception e) {
						if (STOP_ON_FAIL) {
							throw e;
						} else if (VERBOSE_OUT_STRING) {
							e.printStackTrace();
						}
						result = new Triple<>(result.getFirst(), result.getSecond(), result.getThird() + 1);
					}
				}
			}
			return result;
		}
	}

	private void executeJsonSchemaTest(final String collectionDescription, final JsonSchema jsonSchema, final String filename, final Object schemaJsonObject, final JsonObject test) throws Exception {
		final String description = ((String) test.getSimpleValue("description")).trim();

		if (SINGLE_TEST_TO_EXECUTE != null && !(filename + ":" + collectionDescription + ":" + description).startsWith(SINGLE_TEST_TO_EXECUTE)) {
			return;
		}

		final JsonNode data = test.get("data");
		final Boolean valid = (Boolean) test.getSimpleValue("valid");
		try {
			jsonSchema.validate(data);
			if (!valid) {
				System.out.println("FAILED TEST: ");
				System.out.println(filename);
				System.out.println(collectionDescription);
				System.out.println(schemaJsonObject);
				System.out.println(test);
				throw new Exception("Test for invalid JSON data to JSON schema in test collection '" + collectionDescription + "' did not fail '" + description + "'");
			}
		} catch (final JsonSchemaDataValidationError e) {
			if (valid) {
				System.out.println("FAILED TEST (expected error is missing): ");
				System.out.println(filename);
				System.out.println(collectionDescription);
				System.out.println(schemaJsonObject);
				System.out.println(test);
				if (VERBOSE_OUT_STRING) {
					e.printStackTrace();
				}
				throw new Exception("Test for valid JSON data to JSON schema in test collection '" + collectionDescription + "' failed '" + description + "': " + e.getMessage(), e);
			}
		}
	}
}
