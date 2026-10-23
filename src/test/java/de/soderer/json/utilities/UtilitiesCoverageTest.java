package de.soderer.json.utilities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Covers the small helper classes of the utilities package.
 */
@SuppressWarnings("static-method")
class UtilitiesCoverageTest {
	@Test
	void numberUtilities_checks() {
		assertTrue(NumberUtilities.isDigit("0123"));
		assertFalse(NumberUtilities.isDigit("12a"));
		assertFalse(NumberUtilities.isDigit((String) null));
		assertTrue(NumberUtilities.isInteger("-12"));
		assertFalse(NumberUtilities.isInteger("12.5"));
		assertTrue(NumberUtilities.isLong("12345678901"));
		assertFalse(NumberUtilities.isLong("x"));
		assertTrue(NumberUtilities.isBigInteger("+123456789012345678901234567890"));
		assertTrue(NumberUtilities.isBigInteger("-1"));
		assertFalse(NumberUtilities.isBigInteger("-"));
		assertFalse(NumberUtilities.isBigInteger(""));
		assertFalse(NumberUtilities.isBigInteger(null));
		assertFalse(NumberUtilities.isBigInteger("12a"));
		assertTrue(NumberUtilities.isDouble("1.5e3"));
		assertFalse(NumberUtilities.isDouble("x"));
		assertFalse(NumberUtilities.isDouble(null));
		assertTrue(NumberUtilities.isNumber("-1.5E+3"));
		assertTrue(NumberUtilities.isNumber(".5"));
		assertFalse(NumberUtilities.isNumber("1,000"));
		assertFalse(NumberUtilities.isNumber(null));
		assertTrue(NumberUtilities.isHexNumber("0xFF"));
		assertFalse(NumberUtilities.isHexNumber("FF"));
		assertFalse(NumberUtilities.isHexNumber(null));
	}

	@Test
	void numberUtilities_compare() {
		assertEquals(0, NumberUtilities.compare(Integer.valueOf(1), Long.valueOf(1)));
		assertEquals(0, NumberUtilities.compare(Double.valueOf(0.1), new BigDecimal("0.1")));
		assertTrue(NumberUtilities.compare(BigInteger.TEN, Integer.valueOf(9)) > 0);
		assertTrue(NumberUtilities.compare(Double.valueOf(Double.POSITIVE_INFINITY), new BigDecimal("1E400")) > 0);
		assertTrue(NumberUtilities.compare(Double.valueOf(Double.NEGATIVE_INFINITY), Integer.valueOf(0)) < 0);
		assertTrue(NumberUtilities.compare(Integer.valueOf(0), Float.valueOf(Float.POSITIVE_INFINITY)) < 0);
		assertTrue(NumberUtilities.compare(Integer.valueOf(0), Double.valueOf(Double.NEGATIVE_INFINITY)) > 0);
		assertEquals(0, NumberUtilities.compare(Double.valueOf(Double.NaN), Double.valueOf(Double.NaN)));
		assertTrue(NumberUtilities.compare(Double.valueOf(Double.NEGATIVE_INFINITY), Double.valueOf(Double.POSITIVE_INFINITY)) < 0);
		assertThrows(NullPointerException.class, () -> NumberUtilities.compare(null, Integer.valueOf(1)));
		assertThrows(NullPointerException.class, () -> NumberUtilities.compare(Integer.valueOf(1), null));
	}

	@Test
	void numberUtilities_parse() {
		assertEquals(Integer.valueOf(1000), NumberUtilities.parseNumber("1,000"));
		assertEquals(Long.valueOf(12345678901L), NumberUtilities.parseNumber("12345678901"));
		assertEquals(new BigDecimal("123456789012345678901234567890"), NumberUtilities.parseNumber("123456789012345678901234567890"));
		assertEquals(Float.valueOf(1.5f), NumberUtilities.parseNumber("1.5"));
		assertEquals(Double.valueOf(0.1234567890123), NumberUtilities.parseNumber("0.1234567890123"));
		assertInstanceOf(BigDecimal.class, NumberUtilities.parseNumber("0.12345678901234567890123"));
		assertInstanceOf(Float.class, NumberUtilities.parseNumber("1e3"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("abc"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber(null));

		assertEquals(Integer.valueOf(255), NumberUtilities.parseHexNumber("0xFF"));
		assertEquals(Long.valueOf(0xFFFFFFFFFFL), NumberUtilities.parseHexNumber("0xFFFFFFFFFF"));
		assertEquals(new BigInteger("FFFFFFFFFFFFFFFFFF", 16), NumberUtilities.parseHexNumber("0xFFFFFFFFFFFFFFFFFF"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseHexNumber("12"));
	}

	@Test
	void numberUtilities_format() {
		assertNull(NumberUtilities.formatNumber(null, ',', '.'));
		assertEquals("1.5", NumberUtilities.formatNumber(Double.valueOf(1.5), '.', null));
		assertEquals("1,5", NumberUtilities.formatNumber(Double.valueOf(1.5), ',', '.'));
		assertEquals("1.000,5", NumberUtilities.formatNumber(new StringNumber("1,000.5"), ',', '.'));
		assertEquals("1000,5", NumberUtilities.formatNumber(new StringNumber("1,000.5"), ',', null));

		assertNull(NumberUtilities.formatNumber(null, 5, ',', null));
		assertEquals("   1,5", NumberUtilities.formatNumber(Double.valueOf(1.5), 4, ',', null));
		assertEquals("  12", NumberUtilities.formatNumber(Integer.valueOf(12), 4, ',', null));
		assertEquals("12345", NumberUtilities.formatNumber(Integer.valueOf(12345), 2, ',', null));
	}

	@Test
	void numberUtilities_isIntegerBigDecimal() {
		assertFalse(NumberUtilities.isInteger((BigDecimal) null));
		assertTrue(NumberUtilities.isInteger(new BigDecimal("12")));
		assertTrue(NumberUtilities.isInteger(new BigDecimal("0.000")));
		assertTrue(NumberUtilities.isInteger(new BigDecimal("12.000")));
		assertFalse(NumberUtilities.isInteger(new BigDecimal("0.5")));
		assertFalse(NumberUtilities.isInteger(new BigDecimal("12.5")));
	}

	/** Number whose string representation contains grouping characters. */
	private static class StringNumber extends Number {
		private static final long serialVersionUID = 1L;
		private final String text;

		StringNumber(final String text) {
			this.text = text;
		}

		@Override
		public int intValue() {
			return 0;
		}

		@Override
		public long longValue() {
			return 0;
		}

		@Override
		public float floatValue() {
			return 0;
		}

		@Override
		public double doubleValue() {
			return 0;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	@Test
	void dateUtilities() {
		final ZonedDateTime utc = ZonedDateTime.of(2024, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC);
		assertEquals(utc.toInstant(), DateUtilities.getDateForZonedDateTime(utc).toInstant());
		final LocalDateTime local = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
		assertEquals(local.atZone(ZoneId.systemDefault()).toInstant(), DateUtilities.getDateForLocalDateTime(local).toInstant());

		assertNull(DateUtilities.parseIso8601DateTimeString(" "));
		assertEquals(utc.toInstant(), DateUtilities.parseIso8601DateTimeString("2024-01-02T03:04:05Z").toInstant());
		assertEquals(utc.toInstant(), DateUtilities.parseIso8601DateTimeString("2024-01-02T05:04:05+02:00").toInstant());
		assertEquals(500_000_000, DateUtilities.parseIso8601DateTimeString("2024-01-02T03:04:05.500000000Z").getNano());
		assertEquals(local.withNano(250_000_000), DateUtilities.parseIso8601DateTimeString("2024-01-02T03:04:05.250000000", ZoneOffset.UTC).toLocalDateTime());
		assertEquals(local, DateUtilities.parseIso8601DateTimeString("2024-01-02T03:04:05", ZoneOffset.UTC).toLocalDateTime());
		assertEquals(LocalDate.of(2024, 1, 2), DateUtilities.parseIso8601DateTimeString("2024-01-02").toLocalDate());
		assertEquals(ZoneOffset.ofHours(2), DateUtilities.parseIso8601DateTimeString("2024-01-02+02:00").getOffset());
		assertEquals(ZoneOffset.ofHours(-5), DateUtilities.parseIso8601DateTimeString("2024-01-02-05:00").getOffset());
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseIso8601DateTimeString("2024-02-30"));

		// The fraction of second is a decimal fraction with 1 to 9 digits
		assertEquals(500_000_000, DateUtilities.parseIso8601DateTimeString("2024-01-02T03:04:05.5Z").getNano());
		assertEquals(5_000_000, DateUtilities.parseIso8601DateTimeString("2024-01-02T03:04:05.005+01:00").getNano());
		assertEquals(123_456_789, DateUtilities.parseIso8601DateTimeString("2024-01-02T03:04:05.123456789", ZoneOffset.UTC).getNano());
		assertEquals(250_000_000, DateUtilities.parseIso8601DateTimeString("2024-01-02T03:04:05.25", ZoneOffset.UTC).getNano());
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseIso8601DateTimeString("2024-01-02T03:04:05.1234567890Z"));
		assertEquals("2024-01-02T03:04:05.005000000", DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT_NO_TIMEZONE, local.withNano(5_000_000)));

		assertNull(DateUtilities.formatDate("yyyy", (ZonedDateTime) null));
		assertNull(DateUtilities.formatDate("yyyy", (Date) null));
		assertNull(DateUtilities.formatDate("yyyy", (LocalDateTime) null));
		assertNull(DateUtilities.formatDate("yyyy", (LocalDate) null));
		assertEquals("2024", DateUtilities.formatDate("yyyy", utc));
		assertEquals("2024-01-02", DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, local.toLocalDate()));
		assertEquals("1970", DateUtilities.formatDate("yyyy", new java.sql.Date(12 * 3600 * 1000L)));

		assertEquals(LocalDate.of(2023, 2, 28), DateUtilities.parseLocalDate("yyyy-MM-dd", "2023-02-28"));
		assertEquals(LocalDate.of(2024, 2, 29), DateUtilities.parseStrictLocalDate("yyyy-MM-dd", "2024-02-29"));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseStrictLocalDate("yyyy-MM-dd", "2023-02-29"));
		assertEquals(local, DateUtilities.parseLocalDateTime("yyyy-MM-dd HH:mm:ss", "2024-01-02 03:04:05"));
		assertEquals(LocalTime.of(23, 59, 1), DateUtilities.parseLocalTime("HH:mm:ss", "23:59:01"));
		assertThrows(DateTimeParseException.class, () -> DateUtilities.parseLocalTime("HH:mm:ss", "24:30:00"));
		assertEquals(utc.toInstant(), DateUtilities.parseZonedDateTime(DateUtilities.ISO_8601_DATETIME_FORMAT, "2024-01-02T03:04:05Z", ZoneId.of("Europe/Berlin")).toInstant());
	}

	@Test
	void utilities() {
		assertEquals("aGk=", Utilities.encodeBase64("hi".getBytes(StandardCharsets.UTF_8)));
		assertTrue(Utilities.isBlank(null));
		assertTrue(Utilities.isBlank(" \t"));
		assertTrue(Utilities.isNotBlank("x"));

		Utilities.closeQuietly(null);
		Utilities.closeQuietly(() -> {
			throw new IOException("ignored");
		});

		assertNull(Utilities.repeat(null, 3));
		assertEquals("", Utilities.repeat("", 3));
		assertEquals("", Utilities.repeat("a", 0));
		assertEquals("aaa", Utilities.repeat("a", 3));
		assertEquals("a, a", Utilities.repeat("a", 2, ", "));

		assertNull(Utilities.join((Object[]) null, ","));
		assertEquals("a,,c", Utilities.join(new Object[] { "a", null, "c" }, ","));
		assertEquals("ab", Utilities.join(new Object[] { "a", "b" }, null));
		assertNull(Utilities.join((char[]) null, ","));
		assertEquals("a-b", Utilities.join(new char[] { 'a', 'b' }, "-"));
		assertEquals("ab", Utilities.join(new char[] { 'a', 'b' }, null));
		assertNull(Utilities.join((Iterable<?>) null, ","));
		assertEquals("a,,c", Utilities.join(Arrays.asList("a", null, "c"), ","));
		assertEquals("ac", Utilities.join(Arrays.asList("a", "c"), null));

		assertNull(Utilities.shortenStringToMaxLengthCutLeft(null, 5));
		assertEquals("short", Utilities.shortenStringToMaxLengthCutLeft("short", 5));
		assertEquals("... 6789", Utilities.shortenStringToMaxLengthCutLeft("0123456789", 8));
		assertEquals("789", Utilities.shortenStringToMaxLengthCutLeft("0123456789", 3));
		assertEquals("", Utilities.shortenStringToMaxLengthCutLeft("0123456789", -1));

		final Object needle = new Object();
		final List<Object> hayshack = Arrays.asList(new Object(), needle);
		assertTrue(Utilities.containsObject(hayshack, needle));
		assertFalse(Utilities.containsObject(hayshack, new Object()));

		final String home = System.getProperty("user.home");
		assertNull(Utilities.replaceUsersHome(null));
		assertEquals(home + "/x", Utilities.replaceUsersHome("~/x"));
		assertEquals("file~1.txt", Utilities.replaceUsersHome("file~1.txt"));
		assertEquals(home + "/a/" + home, Utilities.replaceUsersHome("${HOME}/a/$HOME"));
	}

	@Test
	void textUtilities() {
		assertTrue(TextUtilities.isValidBase64("aGk="));
		assertTrue(TextUtilities.isValidBase64("aGVsbG8h"));
		assertFalse(TextUtilities.isValidBase64("a"));
		assertEquals("a-b+c", TextUtilities.replaceLast("a+b+c", "+", "-").replaceFirst("\\+", "-").replaceFirst("-b-", "-b+"));
		assertEquals("abc", TextUtilities.replaceLast("abc", "x", "y"));
		assertEquals(2, TextUtilities.getUnicodeStringLength("a😀"));
		assertTrue(TextUtilities.GERMAN_TEST_STRING.length() > 0);
		assertTrue(TextUtilities.SPECIAL_TEST_STRING.length() > 0);
	}

	@Test
	void linebreak() {
		assertEquals(Linebreak.Unknown, Linebreak.getLineBreakTypeByName(null));
		assertEquals(Linebreak.Windows, Linebreak.getLineBreakTypeByName("windows"));
		assertEquals(Linebreak.Unix, Linebreak.getLineBreakTypeByName(" UNIX "));
		assertEquals(Linebreak.Mixed, Linebreak.getLineBreakTypeByName("mixed"));
		assertEquals(Linebreak.Unknown, Linebreak.getLineBreakTypeByName("other"));
		assertEquals("\r\n", Linebreak.Windows.toString());
		assertEquals("\r\n", Linebreak.Windows.getRepresentationString());
		assertEquals("\r", Linebreak.Mac.toString());
		assertNull(Linebreak.Mixed.toString());
		assertEquals(Linebreak.Unix, Linebreak.getLineBreakTypeByString("\n"));
		assertEquals(Linebreak.Mac, Linebreak.getLineBreakTypeByString("\r"));
		assertEquals(Linebreak.Windows, Linebreak.getLineBreakTypeByString("\r\n"));
		assertEquals(Linebreak.Unknown, Linebreak.getLineBreakTypeByString("x"));
		assertEquals(Linebreak.Unknown, Linebreak.getLineBreakTypeByString(null));
	}

	@Test
	void triple() {
		final Triple<String, Integer, Boolean> triple = new Triple<>();
		assertEquals("<<null>, <null>, <null>>", triple.toString());
		assertSame(triple, triple.withFirst("a").withSecond(Integer.valueOf(1)).withThird(Boolean.TRUE));
		assertEquals("a", triple.getFirst());
		assertEquals(Integer.valueOf(1), triple.getSecond());
		assertEquals(Boolean.TRUE, triple.getThird());
		assertEquals("<a, 1, true>", triple.toString());
		triple.setFirst("b");
		triple.setSecond(Integer.valueOf(2));
		triple.setThird(Boolean.FALSE);
		assertEquals("<b, 2, false>", new Triple<>(triple.getFirst(), triple.getSecond(), triple.getThird()).toString());
	}

	@Test
	void countingInputStream() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> new CountingInputStream(null));
		try (CountingInputStream stream = new CountingInputStream(new ByteArrayInputStream("0123456789".getBytes(StandardCharsets.UTF_8)))) {
			assertEquals('0', stream.read());
			assertEquals(1, stream.getCount());
			final byte[] buffer = new byte[2];
			assertEquals(2, stream.read(buffer));
			assertEquals(2, stream.read(buffer, 0, 2));
			assertEquals(5, stream.getByteCount());
			stream.mark(100);
			assertEquals(2, stream.skip(2));
			assertEquals(7, stream.getCount());
			stream.reset();
			assertEquals(5, stream.getCount());
			assertEquals(5, stream.resetCount());
			assertEquals(0, stream.getCount());
			assertEquals(2, stream.skip(2));
			stream.reset();
			// The mark stays at the same stream position relative to the new counting start
			assertEquals(0, stream.getByteCount());
			assertEquals(0, stream.skip(0));
			final byte[] rest = new byte[10];
			assertEquals(5, stream.read(rest));
			assertEquals(-1, stream.read());
			assertEquals(-1, stream.read(rest));
			assertEquals(5, stream.resetByteCount());
		}
		try (CountingInputStream stream = new CountingInputStream(new ByteArrayInputStream(new byte[] { 1, 2, 3 }))) {
			stream.read();
			// Reset without mark starts counting from zero
			stream.reset();
			assertEquals(0, stream.getCount());
		}
	}

	@Test
	void ioAndClassUtilities() throws Exception {
		assertEquals("text", IoUtilities.toString(new ByteArrayInputStream("text".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		assertArrayEquals(new byte[] { 1, 2 }, IoUtilities.toByteArray(new ByteArrayInputStream(new byte[] { 1, 2 })));
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		assertEquals(3, IoUtilities.copy(new ByteArrayInputStream(new byte[] { 1, 2, 3 }), output));

		final List<Field> fields = ClassUtilities.getAllFields(StringNumber.class);
		assertTrue(fields.stream().anyMatch(field -> "text".equals(field.getName())));
		assertEquals("text", ClassUtilities.getField(StringNumber.class, "text").getName());
		assertThrows(NoSuchFieldException.class, () -> ClassUtilities.getField(StringNumber.class, "unknown"));
		assertTrue(ClassUtilities.getConstructor(Object.class) != null);
		assertThrows(NoSuchMethodException.class, () -> ClassUtilities.getConstructor(StringNumber.class));
	}

	@Test
	void jwtUtilities() throws Exception {
		assertEquals(ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant(), JwtUtilities.getJwtTokenValidity(jwt("{\"exp\": 1704067200}")).toInstant());
		assertEquals(1704067200L, JwtUtilities.getJwtTokenValidity(jwt("{\"exp\": 1704067200.75}")).toEpochSecond());
		assertEquals(4102444800L, JwtUtilities.getJwtTokenValidity(jwt("{\"exp\": 4102444800}")).toEpochSecond());
		assertEquals(1704067200L, JwtUtilities.getJwtTokenValidity(jwt("{\"exp\": \"1704067200\"}")).toEpochSecond());
		assertNull(JwtUtilities.getJwtTokenValidity(jwt("{\"sub\": \"x\"}")));
		assertNull(JwtUtilities.getJwtTokenValidity(jwt("{\"exp\": true}")));

		assertThrows(Exception.class, () -> JwtUtilities.getJwtTokenValidity("onlyOnePart"));
		assertThrows(Exception.class, () -> JwtUtilities.getJwtTokenValidity("header.!!!.signature"));
		assertThrows(Exception.class, () -> JwtUtilities.getJwtTokenValidity(jwt("[1, 2]")));
		assertThrows(Exception.class, () -> JwtUtilities.getJwtTokenValidity(jwt("{\"exp\": \"no number\"}")));
	}

	private static String jwt(final String payload) {
		final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		return encoder.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8)) + "." + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".sig";
	}

	@Test
	void closeableLambdaCompiles() {
		final Closeable closeable = () -> {
			// Nothing to close
		};
		Utilities.closeQuietly(closeable);
	}
}
