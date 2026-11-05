package de.soderer.json.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;

/**
 * Tests of the format checks in {@link JsonSchemaFormats} and their use by the "format" keyword.
 */
@SuppressWarnings("static-method")
public class JsonSchemaFormatsTest {
	private static void assertAllValid(final Predicate<String> check, final String... values) {
		for (final String value : values) {
			assertTrue(check.test(value), "Expected valid: '" + value + "'");
		}
	}

	private static void assertAllInvalid(final Predicate<String> check, final String... values) {
		for (final String value : values) {
			assertFalse(check.test(value), "Expected invalid: '" + value + "'");
		}
	}

	@Test
	void date() {
		assertAllValid(JsonSchemaFormats::isValidDate, "1963-06-19", "2020-02-29", "2000-02-29");
		assertAllInvalid(JsonSchemaFormats::isValidDate, "1900-02-29", "2021-02-29", "2020-04-31", "2020-13-01", "2020-00-10", "20-01-01", "2020-1-01", "2020-01-01T00:00:00Z", "２０２０-01-01");
	}

	@Test
	void time() {
		assertAllValid(JsonSchemaFormats::isValidTime, "08:30:06Z", "08:30:06z", "08:30:06.283185Z", "08:30:06+01:00", "23:59:60Z", "15:59:60-08:00");
		assertAllInvalid(JsonSchemaFormats::isValidTime, "08:30:06", "08:30:06+01", "24:00:00Z", "08:60:00Z", "08:30:61Z", "22:59:60Z", "23:59:60+01:00", "08:30:06+24:00", "8:30:06Z", "08:30:06 PST");
	}

	@Test
	void dateTime() {
		assertAllValid(JsonSchemaFormats::isValidDateTime, "1963-06-19T08:30:06.283185Z", "1963-06-19t08:30:06z", "1998-12-31T23:59:60Z", "1990-12-31T15:59:60-08:00");
		assertAllInvalid(JsonSchemaFormats::isValidDateTime, "1990-02-31T15:59:59.123-08:00", "1963-06-19 08:30:06Z", "1963-06-19T08:30:06", "1963-06-19", "06/19/1963 08:30:06 PST");
	}

	@Test
	void duration() {
		assertAllValid(JsonSchemaFormats::isValidDuration, "P4DT12H30M5S", "P4Y", "PT0S", "P0D", "P1M", "PT36H", "P2W");
		assertAllInvalid(JsonSchemaFormats::isValidDuration, "PT1D", "P", "PT", "P1Y2W", "P1", "P1DT", "4DT12H30M5S", "P2D1Y");
	}

	@Test
	void email() {
		assertAllValid(JsonSchemaFormats::isValidEmail, "joe.bloggs@example.com", "te~st@example.com", "\"joe bloggs\"@example.com", "\"joe@bloggs\"@example.com", "joe.bloggs@[127.0.0.1]", "joe.bloggs@[IPv6:::1]", "test@io", "\"\\\"\"@iana.org");
		assertAllInvalid(JsonSchemaFormats::isValidEmail, "2962", ".test@example.com", "test.@example.com", "te..st@example.com", "joe.bloggs@[127.0.0.300]", "@example.com", "joe.bloggs@", "joe bloggs@example.com", "aé@iana.org", "a@é.org",
				"test@iana.org.", "test@-iana.org", "a@[]", "test@[RFC-5322-domain-literal]", "a@b@c.org", "test@iana.org\n");
	}

	@Test
	void idnEmail() {
		assertAllValid(JsonSchemaFormats::isValidIdnEmail, "실례@실례.테스트", "δοκιμή@example.com", "\"δοκιμή\"@example.com", "joe.bloggs@example.com");
		assertAllInvalid(JsonSchemaFormats::isValidIdnEmail, "2962", "user＠example.com", "δοκιμή@-example.com", "δοκιμή@example..com", "δοκιμή@");
	}

	@Test
	void hostname() {
		assertAllValid(JsonSchemaFormats::isValidHostname, "www.example.com", "hostname", "1host", "a--b.com", "xn--9n2bp8q.xn--9t4b11yi5a", "xn--ll-0ea");
		assertAllInvalid(JsonSchemaFormats::isValidHostname, "", ".", "example.", ".example", "-hostname", "hostname-", "host_name", "ex ample.com", "Kelvin.example.com", "xn--X", "XN--aa---o47jg78q", "ab--cd.example",
				"xn--07jt112bpxg.xn--9t4b11yi5a", "a" + "b".repeat(63));
		// Up to draft 6 only RFC 1123 applies, without the reserved labels of RFC 5891
		assertTrue(JsonSchemaFormats.isValidHostname("ab--cd.example", false));
		assertFalse(JsonSchemaFormats.isValidHostname("a".repeat(64), false));
	}

	@Test
	void idnHostname() {
		assertAllValid(JsonSchemaFormats::isValidIdnHostname, "실례.테스트", "xn--ihqwcrb4cv8a8dqg056pqjye", "ßς་〇", "l·l", "α͵β", "א׳ב", "・ぁ", "ب٠ب", "क्‍ष", "بي‌بي", "１２３", "a。b", "café.com", "xn--NXASMQ6B");
		assertAllInvalid(JsonSchemaFormats::isValidIdnHostname, "", "〮실례.테스트", "-hello", "ःhello", "ـߺ", "a·l", "α͵S", "A׳ב", "def・abc", "ب٠۰", "क‍ष", "0a.א", "aא", "a..b", "xn--example-", "xn---9uc",
				"ü".repeat(60), "a".repeat(64));
	}

	@Test
	void punycode() {
		assertEquals("bcher-kva", JsonSchemaFormats.encodePunycode("bücher"));
		assertEquals("bücher", JsonSchemaFormats.decodePunycode("bcher-kva"));
		assertEquals("ihqwcrb4cv8a8dqg056pqjye", JsonSchemaFormats.encodePunycode("他们为什么不说中文"));
		assertEquals("他们为什么不说中文", JsonSchemaFormats.decodePunycode("ihqwcrb4cv8a8dqg056pqjye"));
		assertEquals(null, JsonSchemaFormats.decodePunycode("x"));
	}

	@Test
	void ipAddresses() {
		assertAllValid(JsonSchemaFormats::isValidIpv4, "192.168.0.1", "0.0.0.0", "255.255.255.255");
		assertAllInvalid(JsonSchemaFormats::isValidIpv4, "127.0.0.0.1", "256.256.256.256", "127.0", "0x7f000001", "087.10.0.1", "1.2.3.4\n", "১২৭.0.0.1");
		assertAllValid(JsonSchemaFormats::isValidIpv6, "::1", "::", "1:2:3:4:5:6:7:8", "1::8", "::ffff:192.168.0.1", "1:2:3:4:5:6:1.2.3.4", "FE80::0202:B3FF:FE1E:8329");
		assertAllInvalid(JsonSchemaFormats::isValidIpv6, "", "12345::", "1:1:1:1:1:1:1:1:1", "::laptop", ":2:3:4:5:6:7:8", "1::2::3", "fe80::1%eth0", "::ffff:192.168.0.01", "1:2:3:4:5:6:7:1.2.3.4", "1:2:3:4:5:６:7:8");
	}

	@Test
	void uriAndIri() {
		assertAllValid(JsonSchemaFormats::isValidUri, "http://foo.bar/?baz=qux#quux", "urn:oasis:names:specification:docbook:dtd:xml:4.1.2", "mailto:John.Doe@example.com", "http://[2001:db8::7]/c=GB?objectClass?one", "tel:+1-816-555-1212", "ldap://[2001:db8::7]/c=GB");
		assertAllInvalid(JsonSchemaFormats::isValidUri, "//foo.bar/?baz=qux#quux", "/abc", "\\\\WINDOWS\\fileshare", "abc", "http:// shouldfail.com", ":// should fail", "http://[::1.2.3.04]/", "http://ƒøø.ßår/?∂éœ=πîx#πîüx");
		assertAllValid(JsonSchemaFormats::isValidUriReference, "http://foo.bar/?baz=qux#quux", "//foo.bar/?baz=qux#quux", "/abc", "abc", "#fragment", "");
		assertAllInvalid(JsonSchemaFormats::isValidUriReference, "\\\\WINDOWS\\fileshare", "#frag\\ment", "//[::1.2.3.04]/");
		assertAllValid(JsonSchemaFormats::isValidIri, "http://ƒøø.ßår/?∂éœ=πîx#πîüx", "http://[2001:0db8:85a3:0000:0000:8a2e:0370:7334]");
		assertAllInvalid(JsonSchemaFormats::isValidIri, "/abc", "http://2001:0db8:85a3:0000:0000:8a2e:0370:7334");
		assertAllValid(JsonSchemaFormats::isValidIriReference, "//ƒøø.ßår/?∂éœ=πîx#πîüx", "#ƒrägmênt", "/âππ");
		assertAllInvalid(JsonSchemaFormats::isValidIriReference, "\\\\WINDOWS\\filëßåré", "#ƒräg\\mênt");
	}

	@Test
	void uriTemplate() {
		assertAllValid(JsonSchemaFormats::isValidUriTemplate, "http://example.com/dictionary/{term:1}/{term}", "", "{+var}", "{#var}", "{var*}", "{x,y}", "{a.b}", "{%41}", "{v:1000}", "a%41b", "a'b", "a😀b");
		assertAllInvalid(JsonSchemaFormats::isValidUriTemplate, "http://example.com/dictionary/{term:1}/{term", "{}", "{a,,b}", "{v:0}", "{v:10000}", "{v:01}", "foo}bar", "a%", "a%GG", "{var:1*}", "{a.}", "a b", "{=var}");
	}

	@Test
	void jsonPointers() {
		assertAllValid(JsonSchemaFormats::isValidJsonPointer, "", "/", "/foo/bar~0/baz~1/%a", "/foo//bar", "/foo/-", "/0");
		assertAllInvalid(JsonSchemaFormats::isValidJsonPointer, "/foo/bar~", "#", "#/", "/~0~", "/~2", "a");
		assertAllValid(JsonSchemaFormats::isValidRelativeJsonPointer, "1", "0/foo/bar", "2/0/baz/1/zip", "0#", "120/foo/bar");
		assertAllInvalid(JsonSchemaFormats::isValidRelativeJsonPointer, "/foo/bar", "-1/foo/bar", "+1/foo/bar", "0##", "01/a", "01#", "");
	}

	@Test
	void uuid() {
		assertAllValid(JsonSchemaFormats::isValidUuid, "2EB8AA08-AA98-11EA-B4AA-73B441D16380", "2eb8aa08-aa98-11ea-b4aa-73b441d16380", "00000000-0000-0000-0000-000000000000");
		assertAllInvalid(JsonSchemaFormats::isValidUuid, "2eb8aa08-aa98-11ea-b4aa-73b441d1638", "2eb8aa08aa9811eab4aa73b441d16380", "2eb8-aa08-aa98-11ea-b4aa73b441d16380", "2eb8aa08-aa98-11ea-b4ga-73b441d16380");
	}

	@Test
	void formatKeyword_assertionDependsOnVersionAndConfiguration() throws Exception {
		final String schemaText = "{\"format\": \"ipv4\"}";
		// Draft 7: assertion by default
		assertThrows(JsonSchemaDataValidationError.class, () -> createSchema(schemaText, JsonSchemaVersion.draftV7, null).validate(new ByteArrayInputStream("\"1.2.3\"".getBytes(StandardCharsets.UTF_8))));
		// Draft 2020-12: annotation by default, assertion if configured
		createSchema(schemaText, JsonSchemaVersion.draft2020_12, null).validate(new ByteArrayInputStream("\"1.2.3\"".getBytes(StandardCharsets.UTF_8)));
		assertThrows(JsonSchemaDataValidationError.class, () -> createSchema(schemaText, JsonSchemaVersion.draft2020_12, true).validate(new ByteArrayInputStream("\"1.2.3\"".getBytes(StandardCharsets.UTF_8))));
		// Draft 7 with assertion switched off
		createSchema(schemaText, JsonSchemaVersion.draftV7, false).validate(new ByteArrayInputStream("\"1.2.3\"".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void formatKeyword_unknownFormats() throws Exception {
		// Unknown formats are ignored in the JSON schema drafts, but rejected in simple mode
		createSchema("{\"format\": \"my-custom-format\"}", JsonSchemaVersion.draftV7, null).validate(new ByteArrayInputStream("\"x\"".getBytes(StandardCharsets.UTF_8)));
		createSchema("{\"format\": \"my-custom-format\"}", JsonSchemaVersion.draft2020_12, true).validate(new ByteArrayInputStream("\"x\"".getBytes(StandardCharsets.UTF_8)));
		assertThrows(JsonSchemaDefinitionError.class, () -> createSchema("{\"format\": \"my-custom-format\"}", JsonSchemaVersion.simple, null));
	}

	@Test
	void formatKeyword_simpleModeKeepsLenientDateTime() throws Exception {
		// Simple mode accepts short time zones and missing time zones, the drafts require RFC 3339
		createSchema("{\"format\": \"date-time\"}", JsonSchemaVersion.simple, null).validate(new ByteArrayInputStream("\"2017-02-28T17:30:00+01\"".getBytes(StandardCharsets.UTF_8)));
		createSchema("{\"format\": \"time\"}", JsonSchemaVersion.simple, null).validate(new ByteArrayInputStream("\"17:30:00\"".getBytes(StandardCharsets.UTF_8)));
		assertThrows(JsonSchemaDataValidationError.class, () -> createSchema("{\"format\": \"date-time\"}", JsonSchemaVersion.draftV7, null).validate(new ByteArrayInputStream("\"2017-02-28T17:30:00+01\"".getBytes(StandardCharsets.UTF_8))));
		// Draft 3 "time" has no time offset
		createSchema("{\"format\": \"time\"}", JsonSchemaVersion.draftV3, null).validate(new ByteArrayInputStream("\"17:30:00\"".getBytes(StandardCharsets.UTF_8)));
	}

	private static JsonSchema createSchema(final String schemaText, final JsonSchemaVersion version, final Boolean formatAssertion) throws Exception {
		try (JsonReader reader = new JsonReader(new ByteArrayInputStream(schemaText.getBytes(StandardCharsets.UTF_8)))) {
			return new JsonSchema((JsonObject) reader.read(), new JsonSchemaConfiguration().withJsonSchemaVersion(version).withFormatAssertion(formatAssertion));
		}
	}
}
