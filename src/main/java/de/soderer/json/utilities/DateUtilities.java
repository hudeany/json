package de.soderer.json.utilities;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Date;

/**
 * Date and time helper methods for formatting and parsing JSON data values, mainly in ISO 8601
 * format.
 */
public class DateUtilities {
	/** Date format for ISO 8601 without time zone, e.g. "2024-12-31". */
	public static final String ISO_8601_DATE_FORMAT_NO_TIMEZONE = "yyyy-MM-dd";

	/** Date format for ISO 8601 with time zone, e.g. "2024-12-31+01:00". */
	public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-ddX";

	/** DateTime format for ISO 8601 without time zone, e.g. "2024-12-31T23:59:59". */
	public static final String ISO_8601_DATETIME_FORMAT_NO_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss";

	/** DateTime format for ISO 8601 with optional fraction of second, without time zone. */
	public static final String ISO_8601_DATETIME_WITH_NANOS_FORMAT_NO_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss[.n]";

	/** DateTime format for ISO 8601 with time zone, e.g. "2024-12-31T23:59:59+01". */
	public static final String ISO_8601_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";

	/** DateTime format for ISO 8601 with optional fraction of second and time zone. */
	public static final String ISO_8601_DATETIME_WITH_NANOS_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.n]X";

	/** Time format for ISO 8601 without time zone, e.g. "23:59:59". */
	public static final String ISO_8601_TIME_FORMAT_NO_TIMEZONE = "HH:mm:ss";

	/** Time format for ISO 8601 with nanoseconds, without time zone. */
	public static final String ISO_8601_TIME_WITH_NANOS_FORMAT_NO_TIMEZONE = "HH:mm:ss.SSSSSSSSS";

	/** Time format for ISO 8601 with time zone, e.g. "23:59:59+01". */
	public static final String ISO_8601_TIME_FORMAT = "HH:mm:ssX";

	/** Time format for ISO 8601 with nanoseconds and time zone. */
	public static final String ISO_8601_TIME_WITH_NANOS_FORMAT = "HH:mm:ss.SSSSSSSSSX";

	/**
	 * Utility class, not to be instantiated.
	 */
	private DateUtilities() {
	}

	/**
	 * Converts a local date time in the system default time zone to a Date.
	 *
	 * @param localDateTime
	 *            the local date time
	 * @return the date
	 */
	public static Date getDateForLocalDateTime(final LocalDateTime localDateTime) {
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * Converts a zoned date time to a Date.
	 *
	 * @param zonedDateTime
	 *            the zoned date time
	 * @return the date of the same instant
	 */
	public static Date getDateForZonedDateTime(final ZonedDateTime zonedDateTime) {
		return Date.from(zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * Parses a date or date time string in ISO 8601 format. Values without time zone are
	 * interpreted in the system default time zone.
	 *
	 * @param dateValue
	 *            date or date time string in ISO 8601 format
	 * @return the parsed date time, or null for an empty value
	 * @throws DateTimeParseException
	 *             if the value is not in ISO 8601 format
	 */
	public static ZonedDateTime parseIso8601DateTimeString(final String dateValue) {
		return parseIso8601DateTimeString(dateValue, ZoneId.systemDefault());
	}

	/**
	 * Parses a date or date time string in ISO 8601 format. Supported are dates ("2024-12-31"),
	 * date times ("2024-12-31T23:59:59") with optional fraction of second, each with optional time
	 * zone offset ("Z", "+01:00", "+01"). A date without time means midnight.
	 *
	 * @param dateValueString
	 *            date or date time string in ISO 8601 format
	 * @param defaultZoneId
	 *            time zone for values without time zone information
	 * @return the parsed date time, or null for an empty value
	 * @throws DateTimeParseException
	 *             if the value is not in ISO 8601 format
	 */
	public static ZonedDateTime parseIso8601DateTimeString(String dateValueString, final ZoneId defaultZoneId) {
		if (Utilities.isBlank(dateValueString)) {
			return null;
		}

		dateValueString = dateValueString.toUpperCase();

		if (dateValueString.endsWith("Z")) {
			// Standardize UTC time
			dateValueString = dateValueString.replace("Z", "+00:00");
		}

		boolean hasTimezone = false;
		if (dateValueString.length() > 6 && dateValueString.charAt(dateValueString.length() - 3) == ':' && (dateValueString.charAt(dateValueString.length() - 6) == '+' || dateValueString.charAt(dateValueString.length() - 6) == '-')) {
			hasTimezone = true;
		} else if (dateValueString.length() > 6 && (dateValueString.charAt(dateValueString.length() - 3) == '+')) {
			hasTimezone = true;
		}

		if (dateValueString.contains("T")) {
			if (dateValueString.contains(".")) {
				if (hasTimezone) {
					// Date with time and partial seconds
					final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss[.n]XXXXX").withResolverStyle(ResolverStyle.STRICT);
					return ZonedDateTime.parse(dateValueString, dateTimeFormatter);
				} else {
					// Date with time and milliseconds
					final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss[.n]").withResolverStyle(ResolverStyle.STRICT);
					return LocalDateTime.parse(dateValueString, dateTimeFormatter).atZone(defaultZoneId);
				}
			} else {
				// Date with time
				if (hasTimezone) {
					final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withResolverStyle(ResolverStyle.STRICT);
					return ZonedDateTime.parse(dateValueString, dateTimeFormatter);
				} else {
					final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withResolverStyle(ResolverStyle.STRICT);
					return LocalDateTime.parse(dateValueString, dateTimeFormatter).atZone(defaultZoneId);
				}
			}
		} else {
			// Date only
			if (hasTimezone) {
				if (dateValueString.contains("+")) {
					dateValueString = TextUtilities.replaceLast(dateValueString, "+", "T00:00:00+");
				} else {
					dateValueString = TextUtilities.replaceLast(dateValueString, "-", "T00:00:00-");
				}
				final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withResolverStyle(ResolverStyle.STRICT);
				return ZonedDateTime.parse(dateValueString, dateTimeFormatter);
			} else {
				dateValueString = dateValueString + "T00:00:00";
				final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withResolverStyle(ResolverStyle.STRICT);
				return LocalDateTime.parse(dateValueString, dateTimeFormatter).atZone(defaultZoneId);
			}
		}
	}

	/**
	 * Formats a zoned date time.
	 *
	 * @param format
	 *            the pattern for {@link DateTimeFormatter#ofPattern(String)}
	 * @param date
	 *            the date time, may be null
	 * @return the formatted value, or null if the date time is null
	 */
	public static String formatDate(final String format, final ZonedDateTime date) {
		if (date == null) {
			return null;
		} else {
			return DateTimeFormatter.ofPattern(format).format(date);
		}
	}

	/**
	 * Formats a Date in the system default time zone.
	 *
	 * @param format
	 *            the pattern for {@link DateTimeFormatter#ofPattern(String)}
	 * @param date
	 *            the date, may be null; java.sql.Date and java.sql.Timestamp are supported
	 * @return the formatted value, or null if the date is null
	 */
	public static String formatDate(final String format, final Date date) {
		if (date == null) {
			return null;
		} else {
			// Instant.ofEpochMilli instead of date.toInstant(), which is not supported by java.sql.Date
			return DateTimeFormatter.ofPattern(format).format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault()));
		}
	}

	/**
	 * Formats a local date time.
	 *
	 * @param format
	 *            the pattern for {@link DateTimeFormatter#ofPattern(String)}, must not contain
	 *            time zone fields
	 * @param date
	 *            the date time, may be null
	 * @return the formatted value, or null if the date time is null
	 */
	public static String formatDate(final String format, final LocalDateTime date) {
		if (date == null) {
			return null;
		} else {
			return DateTimeFormatter.ofPattern(format).format(date);
		}
	}

	/**
	 * Formats a local date.
	 *
	 * @param format
	 *            the pattern for {@link DateTimeFormatter#ofPattern(String)}, must only contain
	 *            date fields
	 * @param date
	 *            the date, may be null
	 * @return the formatted value, or null if the date is null
	 */
	public static String formatDate(final String format, final LocalDate date) {
		if (date == null) {
			return null;
		} else {
			return DateTimeFormatter.ofPattern(format).format(date);
		}
	}

	/**
	 * Parses a local date leniently (smart resolver, e.g. day 31 in a 30 day month is adjusted).
	 *
	 * @param dateFormatPattern
	 *            the pattern for {@link DateTimeFormatter#ofPattern(String)}
	 * @param dateString
	 *            the value to parse
	 * @return the parsed date
	 * @throws DateTimeParseException
	 *             if the value does not match the pattern
	 */
	public static LocalDate parseLocalDate(final String dateFormatPattern, final String dateString) {
		return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(dateFormatPattern));
	}

	/**
	 * Parses a local date strictly, so invalid dates like "2023-02-29" are rejected.
	 *
	 * @param dateFormatPattern
	 *            the pattern for {@link DateTimeFormatter#ofPattern(String)}, "yyyy" is
	 *            interpreted as proleptic year
	 * @param dateString
	 *            the value to parse
	 * @return the parsed date
	 * @throws DateTimeParseException
	 *             if the value does not match the pattern or is no valid date
	 */
	public static LocalDate parseStrictLocalDate(final String dateFormatPattern, final String dateString) {
		// Strict resolving needs "uuuu" (proleptic year), "yyyy" (year of era) would require an era field
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormatPattern.replace("yyyy", "uuuu")).withResolverStyle(ResolverStyle.STRICT);
		return LocalDate.parse(dateString, dateTimeFormatter);
	}

	/**
	 * Parses a local date time leniently (smart resolver).
	 *
	 * @param dateTimeFormatPattern
	 *            the pattern for {@link DateTimeFormatter#ofPattern(String)}
	 * @param dateTimeString
	 *            the value to parse
	 * @return the parsed date time
	 * @throws DateTimeParseException
	 *             if the value does not match the pattern
	 */
	public static LocalDateTime parseLocalDateTime(final String dateTimeFormatPattern, final String dateTimeString) {
		return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern(dateTimeFormatPattern));
	}

	/**
	 * Parses a local time strictly, so invalid times like "24:30:00" are rejected.
	 *
	 * @param timeFormatPattern
	 *            the pattern for {@link DateTimeFormatter#ofPattern(String)}
	 * @param timeString
	 *            the value to parse
	 * @return the parsed time
	 * @throws DateTimeParseException
	 *             if the value does not match the pattern or is no valid time
	 */
	public static LocalTime parseLocalTime(final String timeFormatPattern, final String timeString) {
		final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timeFormatPattern).withResolverStyle(ResolverStyle.STRICT);
		return LocalTime.parse(timeString, timeFormatter);
	}

	/**
	 * Parses a zoned date time. If the value contains no time zone, the given time zone is used.
	 *
	 * @param format
	 *            the pattern for {@link DateTimeFormatter#ofPattern(String)}
	 * @param dateTimeString
	 *            the value to parse
	 * @param zoneId
	 *            the time zone to use if the value contains none
	 * @return the parsed date time
	 * @throws DateTimeParseException
	 *             if the value does not match the pattern
	 */
	public static ZonedDateTime parseZonedDateTime(final String format, final String dateTimeString, final ZoneId zoneId) {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format).withZone(zoneId);
		return ZonedDateTime.parse(dateTimeString, dateTimeFormatter);
	}
}
