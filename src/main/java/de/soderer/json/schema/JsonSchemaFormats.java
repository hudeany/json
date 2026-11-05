package de.soderer.json.schema;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks of the string formats defined by the JSON schema standard ("format" keyword), implemented
 * after the referenced RFCs and without external dependencies:
 * <ul>
 * <li>"date", "time", "date-time": RFC 3339 (full-date, full-time, date-time incl. leap seconds)</li>
 * <li>"duration": RFC 3339 appendix A (ISO 8601 duration)</li>
 * <li>"email": RFC 5321 Mailbox, "idn-email": RFC 6531</li>
 * <li>"hostname": RFC 1123 incl. A-label checks, "idn-hostname": RFC 5890-5893 (IDNA2008) with UTS
 * 46 mapping</li>
 * <li>"ipv4": RFC 2673 dotted-quad, "ipv6": RFC 4291</li>
 * <li>"uri", "uri-reference": RFC 3986, "iri", "iri-reference": RFC 3987</li>
 * <li>"uri-template": RFC 6570</li>
 * <li>"json-pointer": RFC 6901, "relative-json-pointer": draft-handrews-relative-json-pointer</li>
 * <li>"uuid": RFC 4122</li>
 * </ul>
 * IDNA2008 code point properties are derived from the Unicode data of the Java runtime (general
 * category, normalization, bidi class) plus the exception tables of RFC 5892. This is a close
 * approximation of the IDNA2008 tables, not a byte exact copy.
 */
public final class JsonSchemaFormats {
	/** Pattern of the "uuid" format (RFC 4122). */
	private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

	/** Pattern of the "duration" format (ISO 8601 duration as in RFC 3339 appendix A). */
	private static final Pattern DURATION_PATTERN = Pattern.compile("P(?:(?:[0-9]+Y(?:[0-9]+M(?:[0-9]+D)?)?|[0-9]+M(?:[0-9]+D)?|[0-9]+D)(?:T(?:[0-9]+H(?:[0-9]+M(?:[0-9]+S)?)?|[0-9]+M(?:[0-9]+S)?|[0-9]+S))?|T(?:[0-9]+H(?:[0-9]+M(?:[0-9]+S)?)?|[0-9]+M(?:[0-9]+S)?|[0-9]+S)|[0-9]+W)");

	/** Pattern of RFC 3339 full-date. */
	private static final Pattern DATE_PATTERN = Pattern.compile("([0-9]{4})-([0-9]{2})-([0-9]{2})");

	/** Pattern of RFC 3339 full-time ("T" and "Z" are case-insensitive). */
	private static final Pattern TIME_PATTERN = Pattern.compile("([0-9]{2}):([0-9]{2}):([0-9]{2})(?:\\.[0-9]+)?(?:([zZ])|([+-])([0-9]{2}):([0-9]{2}))");

	/** Pattern of an IPv4 address in dotted-quad notation without leading zeros. */
	private static final Pattern IPV4_PATTERN = Pattern.compile("(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(?:\\.(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}");

	/** Pattern of a hostname label (RFC 1123). */
	private static final Pattern HOSTNAME_LABEL_PATTERN = Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?");

	/** Pattern of "json-pointer" (RFC 6901). */
	private static final Pattern JSON_POINTER_PATTERN = Pattern.compile("(?:/(?:[^~/]|~[01])*)*");

	/** Pattern of "relative-json-pointer". */
	private static final Pattern RELATIVE_JSON_POINTER_PATTERN = Pattern.compile("(?:0|[1-9][0-9]*)(?:#|(?:/(?:[^~/]|~[01])*)*)");

	/** Pattern of an unquoted email local part (RFC 5321 Dot-string). */
	private static final Pattern EMAIL_DOT_STRING_PATTERN = Pattern.compile("[A-Za-z0-9!#$%&'*+\\-/=?^_`{|}~]+(?:\\.[A-Za-z0-9!#$%&'*+\\-/=?^_`{|}~]+)*");

	/** Pattern of a quoted email local part (RFC 5321 Quoted-string). */
	private static final Pattern EMAIL_QUOTED_STRING_PATTERN = Pattern.compile("\"(?:[\\x20\\x21\\x23-\\x5B\\x5D-\\x7E]|\\\\[\\x20-\\x7E])*\"");

	/** Pattern of an unquoted internationalized email local part (RFC 6531). */
	private static final Pattern IDN_EMAIL_DOT_STRING_PATTERN = Pattern.compile("[A-Za-z0-9!#$%&'*+\\-/=?^_`{|}~\\x{80}-\\x{10FFFF}]+(?:\\.[A-Za-z0-9!#$%&'*+\\-/=?^_`{|}~\\x{80}-\\x{10FFFF}]+)*");

	/** Pattern of a quoted internationalized email local part (RFC 6531). */
	private static final Pattern IDN_EMAIL_QUOTED_STRING_PATTERN = Pattern.compile("\"(?:[\\x20\\x21\\x23-\\x5B\\x5D-\\x7E\\x{80}-\\x{10FFFF}]|\\\\[\\x20-\\x7E])*\"");

	/** RFC 3986 and RFC 3987 grammar parts as regular expressions. */
	private static final String UNRESERVED = "A-Za-z0-9\\-._~";
	private static final String SUB_DELIMS = "!$&'()*+,;=";
	private static final String UCSCHAR = "\\x{A0}-\\x{D7FF}\\x{F900}-\\x{FDCF}\\x{FDF0}-\\x{FFEF}\\x{10000}-\\x{1FFFD}\\x{20000}-\\x{2FFFD}\\x{30000}-\\x{3FFFD}\\x{40000}-\\x{4FFFD}\\x{50000}-\\x{5FFFD}\\x{60000}-\\x{6FFFD}\\x{70000}-\\x{7FFFD}\\x{80000}-\\x{8FFFD}\\x{90000}-\\x{9FFFD}\\x{A0000}-\\x{AFFFD}\\x{B0000}-\\x{BFFFD}\\x{C0000}-\\x{CFFFD}\\x{D0000}-\\x{DFFFD}\\x{E1000}-\\x{EFFFD}";
	private static final String IPRIVATE = "\\x{E000}-\\x{F8FF}\\x{F0000}-\\x{FFFFD}\\x{100000}-\\x{10FFFD}";
	private static final String PCT_ENCODED = "%[0-9A-Fa-f]{2}";

	/** Patterns of URI (RFC 3986) and IRI (RFC 3987) grammars. */
	private static final Pattern URI_PATTERN = Pattern.compile(createUriRegex(false, false));
	private static final Pattern URI_REFERENCE_PATTERN = Pattern.compile(createUriRegex(false, true));
	private static final Pattern IRI_PATTERN = Pattern.compile(createUriRegex(true, false));
	private static final Pattern IRI_REFERENCE_PATTERN = Pattern.compile(createUriRegex(true, true));

	/** Pattern of an "IP-literal" host with group 1 being an IPv6 address candidate. */
	private static final Pattern IP_LITERAL_PATTERN = Pattern.compile("\\[(?:([0-9A-Fa-f:.]+)|v[0-9A-Fa-f]+\\.[" + UNRESERVED + SUB_DELIMS + ":]+)\\]");

	/** Pattern of a variable name of a URI template expression (RFC 6570). */
	private static final Pattern URI_TEMPLATE_VARSPEC_PATTERN = Pattern.compile("(?:[A-Za-z0-9_]|%[0-9A-Fa-f]{2})(?:\\.?(?:[A-Za-z0-9_]|%[0-9A-Fa-f]{2}))*(?::[1-9][0-9]{0,3}|\\*)?");

	/** Characters allowed as label separators by UTS 46 besides ".". */
	private static final String IDN_LABEL_SEPARATORS = "。．｡";

	/** Code points removed by the UTS 46 mapping ("ignored"). */
	private static final Set<Integer> UTS46_IGNORED = Set.of(0x00AD, 0x034F, 0x180B, 0x180C, 0x180D, 0x180F, 0x200B, 0x2060, 0x2064, 0xFEFF);

	/** Code points with combining class Virama (ccc = 9), used by the CONTEXTJ rules. */
	private static final Set<Integer> VIRAMAS = Set.of(
			0x094D, 0x09CD, 0x0A4D, 0x0ACD, 0x0B4D, 0x0BCD, 0x0C4D, 0x0CCD, 0x0D3B, 0x0D3C, 0x0D4D, 0x0DCA, 0x0E3A, 0x0EBA, 0x0F84, 0x1039, 0x103A, 0x1714, 0x1715, 0x1734,
			0x17D2, 0x1A60, 0x1B44, 0x1BAA, 0x1BAB, 0x1BF2, 0x1BF3, 0x2D7F, 0xA806, 0xA82C, 0xA8C4, 0xA953, 0xA9C0, 0xAAF6, 0xABED, 0x10A3F, 0x11046, 0x11070, 0x1107F, 0x110B9,
			0x11133, 0x11134, 0x111C0, 0x11235, 0x112EA, 0x1134D, 0x11442, 0x114C2, 0x115BF, 0x1163F, 0x116B6, 0x1172B, 0x11839, 0x1193D, 0x1193E, 0x119E0, 0x11A34, 0x11A47,
			0x11A99, 0x11C3F, 0x11D44, 0x11D45, 0x11D97, 0x11F41, 0x11F42);

	/** Arabic letters which are right-joining (Joining_Type R), used by the CONTEXTJ rule of ZWNJ. */
	private static final Set<Integer> RIGHT_JOINING = Set.of(
			0x0622, 0x0623, 0x0624, 0x0625, 0x0627, 0x0629, 0x062F, 0x0630, 0x0631, 0x0632, 0x0648, 0x0671, 0x0672, 0x0673, 0x0675, 0x0676, 0x0677, 0x0688, 0x0689, 0x068A,
			0x068B, 0x068C, 0x068D, 0x068E, 0x068F, 0x0690, 0x0691, 0x0692, 0x0693, 0x0694, 0x0695, 0x0696, 0x0697, 0x0698, 0x0699, 0x06C0, 0x06C3, 0x06C4, 0x06C5, 0x06C6,
			0x06C7, 0x06C8, 0x06C9, 0x06CA, 0x06CB, 0x06CD, 0x06CF, 0x06D2, 0x06D3, 0x06D5, 0x06EE, 0x06EF, 0x0710, 0x0715, 0x0716, 0x0717, 0x0718, 0x0719, 0x071E, 0x0728,
			0x072A, 0x072C, 0x072F, 0x074D, 0x0759, 0x075A, 0x075B, 0x076B, 0x076C, 0x0771, 0x0773, 0x0774, 0x0778, 0x0779);

	private JsonSchemaFormats() {
		// Utility class
	}

	// ------------------------------------------------------------------------------------------
	// Date and time (RFC 3339)
	// ------------------------------------------------------------------------------------------

	/**
	 * Checks the "date" format: RFC 3339 full-date, e.g. "2024-02-29".
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidDate(final String value) {
		final java.util.regex.Matcher matcher = DATE_PATTERN.matcher(value);
		if (!matcher.matches()) {
			return false;
		}
		final int year = Integer.parseInt(matcher.group(1));
		final int month = Integer.parseInt(matcher.group(2));
		final int day = Integer.parseInt(matcher.group(3));
		return month >= 1 && month <= 12 && day >= 1 && day <= getDaysOfMonth(year, month);
	}

	/**
	 * Checks the "time" format: RFC 3339 full-time with mandatory time offset, e.g. "08:30:06Z" or
	 * "08:30:06.283+01:00". A leap second is only valid at 23:59:60 UTC.
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidTime(final String value) {
		final java.util.regex.Matcher matcher = TIME_PATTERN.matcher(value);
		if (!matcher.matches()) {
			return false;
		}
		final int hour = Integer.parseInt(matcher.group(1));
		final int minute = Integer.parseInt(matcher.group(2));
		final int second = Integer.parseInt(matcher.group(3));
		int offsetMinutes = 0;
		if (matcher.group(4) == null) {
			final int offsetHour = Integer.parseInt(matcher.group(6));
			final int offsetMinute = Integer.parseInt(matcher.group(7));
			if (offsetHour > 23 || offsetMinute > 59) {
				return false;
			}
			offsetMinutes = ("-".equals(matcher.group(5)) ? -1 : 1) * (offsetHour * 60 + offsetMinute);
		}
		if (hour > 23 || minute > 59 || second > 60) {
			return false;
		} else if (second == 60) {
			// Leap seconds are only inserted at the end of a UTC day
			final int utcMinuteOfDay = Math.floorMod(hour * 60 + minute - offsetMinutes, 24 * 60);
			return utcMinuteOfDay == 23 * 60 + 59;
		} else {
			return true;
		}
	}

	/**
	 * Checks the "date-time" format: RFC 3339 date-time, e.g. "1963-06-19T08:30:06.283185Z".
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidDateTime(final String value) {
		final int separatorIndex = Math.max(value.indexOf('T'), value.indexOf('t'));
		return separatorIndex == 10 && isValidDate(value.substring(0, 10)) && isValidTime(value.substring(11));
	}

	/**
	 * Checks the "duration" format: ISO 8601 duration as in RFC 3339 appendix A, e.g. "P4DT12H30M5S".
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidDuration(final String value) {
		return DURATION_PATTERN.matcher(value).matches();
	}

	/**
	 * Returns the number of days of a month.
	 *
	 * @param year
	 *            the year
	 * @param month
	 *            the month, 1 to 12
	 * @return the number of days
	 */
	private static int getDaysOfMonth(final int year, final int month) {
		switch (month) {
			case 2:
				return (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28;
			case 4:
			case 6:
			case 9:
			case 11:
				return 30;
			default:
				return 31;
		}
	}

	// ------------------------------------------------------------------------------------------
	// Email (RFC 5321, RFC 6531)
	// ------------------------------------------------------------------------------------------

	/**
	 * Checks the "email" format: RFC 5321 Mailbox, e.g. "joe.bloggs@example.com",
	 * "\"joe bloggs\"@example.com" or "joe@[127.0.0.1]".
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidEmail(final String value) {
		return isValidEmail(value, false);
	}

	/**
	 * Checks the "idn-email" format: RFC 6531 Mailbox, which also allows non-ASCII characters in the
	 * local part and internationalized domain names.
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidIdnEmail(final String value) {
		return isValidEmail(value, true);
	}

	/**
	 * Checks an email address.
	 *
	 * @param value
	 *            the value
	 * @param internationalized
	 *            true for RFC 6531, false for RFC 5321
	 * @return true, if valid
	 */
	private static boolean isValidEmail(final String value, final boolean internationalized) {
		final int atIndex = value.lastIndexOf('@');
		if (atIndex <= 0 || atIndex == value.length() - 1) {
			return false;
		}
		final String localPart = value.substring(0, atIndex);
		final String domain = value.substring(atIndex + 1);

		if (localPart.getBytes(StandardCharsets.UTF_8).length > 64) {
			return false;
		}
		final boolean validLocalPart;
		if (internationalized) {
			validLocalPart = IDN_EMAIL_DOT_STRING_PATTERN.matcher(localPart).matches() || IDN_EMAIL_QUOTED_STRING_PATTERN.matcher(localPart).matches();
		} else {
			validLocalPart = EMAIL_DOT_STRING_PATTERN.matcher(localPart).matches() || EMAIL_QUOTED_STRING_PATTERN.matcher(localPart).matches();
		}
		if (!validLocalPart) {
			return false;
		}

		if (domain.startsWith("[")) {
			if (!domain.endsWith("]")) {
				return false;
			}
			final String addressLiteral = domain.substring(1, domain.length() - 1);
			if (addressLiteral.regionMatches(true, 0, "IPv6:", 0, 5)) {
				return isValidIpv6(addressLiteral.substring(5));
			} else {
				return isValidIpv4(addressLiteral);
			}
		} else if (internationalized) {
			return isValidIdnHostname(domain);
		} else {
			return isValidHostname(domain);
		}
	}

	// ------------------------------------------------------------------------------------------
	// IP addresses
	// ------------------------------------------------------------------------------------------

	/**
	 * Checks the "ipv4" format: dotted-quad notation without leading zeros, e.g. "192.168.0.1".
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidIpv4(final String value) {
		return IPV4_PATTERN.matcher(value).matches();
	}

	/**
	 * Checks the "ipv6" format: RFC 4291 text representation, e.g. "::1" or "::ffff:192.168.0.1".
	 * Zone identifiers ("%eth0") are not allowed.
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidIpv6(final String value) {
		if (value.isEmpty()) {
			return false;
		}
		String hexPart = value;
		int groupCount = 0;
		final int lastColonIndex = value.lastIndexOf(':');
		if (lastColonIndex >= 0 && value.indexOf('.', lastColonIndex) >= 0) {
			// Trailing embedded IPv4 address, counts as two groups
			if (!isValidIpv4(value.substring(lastColonIndex + 1))) {
				return false;
			}
			final String prefix = value.substring(0, lastColonIndex);
			// Keep a "::" directly before the IPv4 part
			hexPart = prefix.endsWith(":") ? prefix + ":" : prefix;
			groupCount += 2;
		}

		final int doubleColonIndex = hexPart.indexOf("::");
		if (doubleColonIndex >= 0 && hexPart.indexOf("::", doubleColonIndex + 1) >= 0) {
			return false;
		}
		final List<String> groups = new ArrayList<>();
		if (doubleColonIndex >= 0) {
			addIpv6Groups(hexPart.substring(0, doubleColonIndex), groups);
			addIpv6Groups(hexPart.substring(doubleColonIndex + 2), groups);
		} else {
			addIpv6Groups(hexPart, groups);
		}
		for (final String group : groups) {
			if (group.isEmpty() || group.length() > 4) {
				return false;
			}
			for (int i = 0; i < group.length(); i++) {
				if (!isAsciiHexDigit(group.charAt(i))) {
					return false;
				}
			}
		}
		groupCount += groups.size();
		return doubleColonIndex >= 0 ? groupCount <= 7 : groupCount == 8;
	}

	/**
	 * Checks for an ASCII hexadecimal digit.
	 *
	 * @param character
	 *            the character
	 * @return true, if 0-9, a-f or A-F
	 */
	private static boolean isAsciiHexDigit(final char character) {
		return (character >= '0' && character <= '9') || (character >= 'a' && character <= 'f') || (character >= 'A' && character <= 'F');
	}

	/**
	 * Splits colon separated IPv6 groups.
	 *
	 * @param part
	 *            the groups text, may be empty
	 * @param groups
	 *            the list to add the groups to
	 */
	private static void addIpv6Groups(final String part, final List<String> groups) {
		if (!part.isEmpty()) {
			for (final String group : part.split(":", -1)) {
				groups.add(group);
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	// Host names (RFC 1123, RFC 5890-5893, UTS 46)
	// ------------------------------------------------------------------------------------------

	/**
	 * Checks the "hostname" format: RFC 1123 host name with labels of letters, digits and hyphens.
	 * Labels starting with "xn--" must be valid A-labels (Punycode encoded IDNA2008 labels).
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidHostname(final String value) {
		return isValidHostname(value, true);
	}

	/**
	 * Checks the "hostname" format.
	 *
	 * @param value
	 *            the value
	 * @param checkReservedLabels
	 *            true to check labels with "--" in the 3rd and 4th position after RFC 5891 (only
	 *            valid A-labels "xn--..." are allowed), false for plain RFC 1123 host names (JSON
	 *            schema up to draft 6)
	 * @return true, if valid
	 */
	public static boolean isValidHostname(final String value, final boolean checkReservedLabels) {
		if (value.isEmpty() || value.length() > 253) {
			return false;
		}
		final String[] labels = value.split("\\.", -1);
		final List<String> uLabels = new ArrayList<>();
		for (final String label : labels) {
			if (!HOSTNAME_LABEL_PATTERN.matcher(label).matches()) {
				return false;
			} else if (checkReservedLabels && label.length() >= 4 && label.charAt(2) == '-' && label.charAt(3) == '-') {
				if (!label.regionMatches(true, 0, "xn", 0, 2)) {
					return false;
				}
				final String uLabel = decodeALabel(label);
				if (uLabel == null || !isValidULabel(uLabel)) {
					return false;
				}
				uLabels.add(uLabel);
			} else {
				uLabels.add(label.toLowerCase(Locale.ROOT));
			}
		}
		return isValidBidiDomain(uLabels);
	}

	/**
	 * Checks the "idn-hostname" format: internationalized host name after IDNA2008 (RFC 5890-5893).
	 * The input is mapped by UTS 46 first (e.g. fullwidth characters, case folding, ideographic full
	 * stop as label separator).
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidIdnHostname(final String value) {
		if (value.isEmpty()) {
			return false;
		}
		final String mappedValue = mapUts46(value);
		if (mappedValue == null || mappedValue.isEmpty()) {
			return false;
		}
		final String[] labels = mappedValue.split("\\.", -1);
		final List<String> uLabels = new ArrayList<>();
		int asciiLength = labels.length - 1;
		for (final String label : labels) {
			if (label.isEmpty()) {
				return false;
			}
			final String uLabel;
			if (label.startsWith("xn--")) {
				uLabel = decodeALabel(label);
				if (uLabel == null) {
					return false;
				}
			} else {
				uLabel = label;
			}
			if (!isValidULabel(uLabel)) {
				return false;
			}
			final String aLabel;
			if (isAscii(uLabel)) {
				aLabel = uLabel;
			} else {
				final String punycode = encodePunycode(uLabel);
				if (punycode == null) {
					// Punycode overflow
					return false;
				}
				aLabel = "xn--" + punycode;
			}
			if (aLabel.length() > 63) {
				return false;
			}
			asciiLength += aLabel.length();
			uLabels.add(uLabel);
		}
		return asciiLength <= 253 && isValidBidiDomain(uLabels);
	}

	/**
	 * Maps a host name after UTS 46 (approximated): ignored code points are removed, alternative
	 * label separators become ".", and the result is NFKC normalized and case folded.
	 *
	 * @param value
	 *            the host name
	 * @return the mapped host name, or null if it contains code points that cannot be mapped
	 */
	private static String mapUts46(final String value) {
		final StringBuilder result = new StringBuilder();
		for (int i = 0; i < value.length();) {
			final int codePoint = value.codePointAt(i);
			i += Character.charCount(codePoint);
			if (UTS46_IGNORED.contains(codePoint) || (codePoint >= 0xFE00 && codePoint <= 0xFE0F) || (codePoint >= 0xE0100 && codePoint <= 0xE01EF)) {
				continue;
			} else if (IDN_LABEL_SEPARATORS.indexOf(codePoint) >= 0) {
				result.append('.');
			} else {
				result.appendCodePoint(codePoint);
			}
		}
		final String folded = Normalizer.normalize(Normalizer.normalize(result.toString(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT), Normalizer.Form.NFKC);
		return Normalizer.normalize(folded, Normalizer.Form.NFC);
	}

	/**
	 * Decodes an A-label ("xn--..."). The result must be non-ASCII and encode back to the same
	 * A-label (canonical Punycode).
	 *
	 * @param aLabel
	 *            the A-label
	 * @return the U-label, or null if the A-label is invalid
	 */
	private static String decodeALabel(final String aLabel) {
		final String punycode = aLabel.substring(4).toLowerCase(Locale.ROOT);
		if (punycode.isEmpty()) {
			return null;
		}
		final String uLabel = decodePunycode(punycode);
		if (uLabel == null || isAscii(uLabel) || !punycode.equals(encodePunycode(uLabel))) {
			return null;
		}
		return uLabel;
	}

	/**
	 * Checks a U-label after IDNA2008: protocol rules (RFC 5891 section 5.4), code point properties
	 * (RFC 5892) and contextual rules (RFC 5892 appendix A).
	 *
	 * @param label
	 *            the label in Unicode form
	 * @return true, if valid
	 */
	private static boolean isValidULabel(final String label) {
		if (label.isEmpty() || label.length() > 63 && isAscii(label)) {
			return false;
		} else if (label.startsWith("-") || label.endsWith("-")) {
			return false;
		} else if (label.length() >= 4 && label.charAt(2) == '-' && label.charAt(3) == '-') {
			return false;
		} else if (!Normalizer.isNormalized(label, Normalizer.Form.NFC)) {
			return false;
		}

		final int[] codePoints = label.codePoints().toArray();
		final int firstType = Character.getType(codePoints[0]);
		if (firstType == Character.NON_SPACING_MARK || firstType == Character.COMBINING_SPACING_MARK || firstType == Character.ENCLOSING_MARK) {
			return false;
		}

		boolean hasArabicIndicDigit = false;
		boolean hasExtendedArabicIndicDigit = false;
		for (final int codePoint : codePoints) {
			if (codePoint >= 0x0660 && codePoint <= 0x0669) {
				hasArabicIndicDigit = true;
			} else if (codePoint >= 0x06F0 && codePoint <= 0x06F9) {
				hasExtendedArabicIndicDigit = true;
			}
		}

		for (int i = 0; i < codePoints.length; i++) {
			final int codePoint = codePoints[i];
			switch (getIdnaProperty(codePoint)) {
				case PVALID:
					break;
				case CONTEXTJ:
					if (!isValidContextJ(codePoints, i)) {
						return false;
					}
					break;
				case CONTEXTO:
					if (!isValidContextO(codePoints, i, hasArabicIndicDigit, hasExtendedArabicIndicDigit)) {
						return false;
					}
					break;
				case DISALLOWED:
				default:
					return false;
			}
		}
		return true;
	}

	/** IDNA2008 code point properties (RFC 5892). */
	private enum IdnaProperty {
		PVALID, CONTEXTJ, CONTEXTO, DISALLOWED
	}

	/**
	 * Returns the IDNA2008 property of a code point (RFC 5892 section 3), derived from Unicode
	 * data of the Java runtime.
	 *
	 * @param codePoint
	 *            the code point
	 * @return the property
	 */
	private static IdnaProperty getIdnaProperty(final int codePoint) {
		// Exceptions (RFC 5892 section 2.6)
		switch (codePoint) {
			case 0x00DF:
			case 0x03C2:
			case 0x06FD:
			case 0x06FE:
			case 0x0F0B:
			case 0x3007:
				return IdnaProperty.PVALID;
			case 0x00B7:
			case 0x0375:
			case 0x05F3:
			case 0x05F4:
			case 0x30FB:
				return IdnaProperty.CONTEXTO;
			case 0x0640:
			case 0x07FA:
			case 0x302E:
			case 0x302F:
			case 0x3031:
			case 0x3032:
			case 0x3033:
			case 0x3034:
			case 0x3035:
			case 0x303B:
				return IdnaProperty.DISALLOWED;
			case 0x200C:
			case 0x200D:
				return IdnaProperty.CONTEXTJ;
			default:
				break;
		}
		if ((codePoint >= 0x0660 && codePoint <= 0x0669) || (codePoint >= 0x06F0 && codePoint <= 0x06F9)) {
			return IdnaProperty.CONTEXTO;
		}

		// LDH characters
		if ((codePoint >= 'a' && codePoint <= 'z') || (codePoint >= '0' && codePoint <= '9') || codePoint == '-') {
			return IdnaProperty.PVALID;
		} else if (codePoint < 0x80) {
			return IdnaProperty.DISALLOWED;
		}

		final int type = Character.getType(codePoint);
		if (type == Character.UNASSIGNED) {
			return IdnaProperty.DISALLOWED;
		}

		// Unstable: changed by NFKC case folding
		final String codePointString = new String(Character.toChars(codePoint));
		final String folded = Normalizer.normalize(Normalizer.normalize(codePointString, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT), Normalizer.Form.NFKC);
		if (!folded.equals(codePointString)) {
			return IdnaProperty.DISALLOWED;
		}

		// Ignorable properties and blocks, old Hangul jamo
		if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint) || type == Character.FORMAT
				|| (codePoint >= 0xFDD0 && codePoint <= 0xFDEF) || (codePoint & 0xFFFE) == 0xFFFE
				|| (codePoint >= 0x20D0 && codePoint <= 0x20FF) || (codePoint >= 0x1D100 && codePoint <= 0x1D24F)
				|| (codePoint >= 0x1100 && codePoint <= 0x11FF) || (codePoint >= 0xA960 && codePoint <= 0xA97F) || (codePoint >= 0xD7B0 && codePoint <= 0xD7FF)) {
			return IdnaProperty.DISALLOWED;
		}

		// LetterDigits
		switch (type) {
			case Character.LOWERCASE_LETTER:
			case Character.UPPERCASE_LETTER:
			case Character.OTHER_LETTER:
			case Character.DECIMAL_DIGIT_NUMBER:
			case Character.MODIFIER_LETTER:
			case Character.NON_SPACING_MARK:
			case Character.COMBINING_SPACING_MARK:
				return IdnaProperty.PVALID;
			default:
				return IdnaProperty.DISALLOWED;
		}
	}

	/**
	 * Checks the CONTEXTJ rules of ZERO WIDTH NON-JOINER and ZERO WIDTH JOINER (RFC 5892 appendix
	 * A.1 and A.2).
	 *
	 * @param codePoints
	 *            the code points of the label
	 * @param index
	 *            the index of the joiner
	 * @return true, if valid
	 */
	private static boolean isValidContextJ(final int[] codePoints, final int index) {
		if (index > 0 && VIRAMAS.contains(codePoints[index - 1])) {
			return true;
		} else if (codePoints[index] == 0x200D) {
			return false;
		}
		// ZWNJ: (Joining_Type:{L,D})(Joining_Type:T)*‌(Joining_Type:T)*(Joining_Type:{R,D})
		int before = index - 1;
		while (before >= 0 && isTransparentJoining(codePoints[before])) {
			before--;
		}
		int after = index + 1;
		while (after < codePoints.length && isTransparentJoining(codePoints[after])) {
			after++;
		}
		return before >= 0 && after < codePoints.length && isDualJoining(codePoints[before]) && (isDualJoining(codePoints[after]) || RIGHT_JOINING.contains(codePoints[after]));
	}

	/**
	 * Checks for Joining_Type T (transparent): non-spacing and enclosing marks and format
	 * characters.
	 *
	 * @param codePoint
	 *            the code point
	 * @return true, if transparent
	 */
	private static boolean isTransparentJoining(final int codePoint) {
		final int type = Character.getType(codePoint);
		return type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK || (type == Character.FORMAT && codePoint != 0x200C && codePoint != 0x200D);
	}

	/**
	 * Checks for Joining_Type D (dual joining), approximated by the letters of the joining scripts
	 * (Arabic, Syriac, N'Ko, Mongolian ...) that are not right-joining.
	 *
	 * @param codePoint
	 *            the code point
	 * @return true, if dual joining
	 */
	private static boolean isDualJoining(final int codePoint) {
		if (RIGHT_JOINING.contains(codePoint) || Character.getType(codePoint) != Character.OTHER_LETTER) {
			return false;
		}
		final Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
		return script == Character.UnicodeScript.ARABIC || script == Character.UnicodeScript.SYRIAC || script == Character.UnicodeScript.NKO
				|| script == Character.UnicodeScript.MONGOLIAN || script == Character.UnicodeScript.MANDAIC || script == Character.UnicodeScript.PHAGS_PA;
	}

	/**
	 * Checks the CONTEXTO rules (RFC 5892 appendix A.3 to A.9).
	 *
	 * @param codePoints
	 *            the code points of the label
	 * @param index
	 *            the index of the code point to check
	 * @param hasArabicIndicDigit
	 *            whether the label contains ARABIC-INDIC DIGITs
	 * @param hasExtendedArabicIndicDigit
	 *            whether the label contains EXTENDED ARABIC-INDIC DIGITs
	 * @return true, if valid
	 */
	private static boolean isValidContextO(final int[] codePoints, final int index, final boolean hasArabicIndicDigit, final boolean hasExtendedArabicIndicDigit) {
		final int codePoint = codePoints[index];
		if (codePoint == 0x00B7) {
			// MIDDLE DOT: between two 'l'
			return index > 0 && index < codePoints.length - 1 && codePoints[index - 1] == 'l' && codePoints[index + 1] == 'l';
		} else if (codePoint == 0x0375) {
			// GREEK LOWER NUMERAL SIGN (KERAIA): followed by Greek
			return index < codePoints.length - 1 && Character.UnicodeScript.of(codePoints[index + 1]) == Character.UnicodeScript.GREEK;
		} else if (codePoint == 0x05F3 || codePoint == 0x05F4) {
			// HEBREW PUNCTUATION GERESH and GERSHAYIM: preceded by Hebrew
			return index > 0 && Character.UnicodeScript.of(codePoints[index - 1]) == Character.UnicodeScript.HEBREW;
		} else if (codePoint == 0x30FB) {
			// KATAKANA MIDDLE DOT: label contains Hiragana, Katakana or Han
			for (final int otherCodePoint : codePoints) {
				if (otherCodePoint != 0x30FB) {
					final Character.UnicodeScript script = Character.UnicodeScript.of(otherCodePoint);
					if (script == Character.UnicodeScript.HIRAGANA || script == Character.UnicodeScript.KATAKANA || script == Character.UnicodeScript.HAN) {
						return true;
					}
				}
			}
			return false;
		} else if (codePoint >= 0x0660 && codePoint <= 0x0669) {
			return !hasExtendedArabicIndicDigit;
		} else if (codePoint >= 0x06F0 && codePoint <= 0x06F9) {
			return !hasArabicIndicDigit;
		} else {
			return false;
		}
	}

	/**
	 * Checks the Bidi rule (RFC 5893) for all labels of a domain name. It applies only to domain
	 * names containing at least one right-to-left character (bidi class R, AL or AN).
	 *
	 * @param uLabels
	 *            the labels in Unicode form
	 * @return true, if valid
	 */
	private static boolean isValidBidiDomain(final List<String> uLabels) {
		boolean bidiDomain = false;
		for (final String label : uLabels) {
			for (final int codePoint : label.codePoints().toArray()) {
				final byte direction = Character.getDirectionality(codePoint);
				if (direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT || direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC || direction == Character.DIRECTIONALITY_ARABIC_NUMBER) {
					bidiDomain = true;
				}
			}
		}
		if (bidiDomain) {
			for (final String label : uLabels) {
				if (!isValidBidiLabel(label.codePoints().toArray())) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Checks the six conditions of the Bidi rule (RFC 5893 section 2) for one label.
	 *
	 * @param codePoints
	 *            the code points of the label
	 * @return true, if valid
	 */
	private static boolean isValidBidiLabel(final int[] codePoints) {
		final byte first = Character.getDirectionality(codePoints[0]);
		final boolean rightToLeft;
		if (first == Character.DIRECTIONALITY_RIGHT_TO_LEFT || first == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
			rightToLeft = true;
		} else if (first == Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
			rightToLeft = false;
		} else {
			return false;
		}

		boolean hasEuropeanNumber = false;
		boolean hasArabicNumber = false;
		int lastNonMark = -1;
		for (int i = 0; i < codePoints.length; i++) {
			final byte direction = Character.getDirectionality(codePoints[i]);
			switch (direction) {
				case Character.DIRECTIONALITY_EUROPEAN_NUMBER:
					hasEuropeanNumber = true;
					break;
				case Character.DIRECTIONALITY_ARABIC_NUMBER:
					hasArabicNumber = true;
					break;
				default:
					break;
			}
			if (direction != Character.DIRECTIONALITY_NONSPACING_MARK) {
				lastNonMark = i;
			}

			final boolean allowed;
			if (rightToLeft) {
				allowed = direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT || direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC || direction == Character.DIRECTIONALITY_ARABIC_NUMBER
						|| direction == Character.DIRECTIONALITY_EUROPEAN_NUMBER || direction == Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR || direction == Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR
						|| direction == Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR || direction == Character.DIRECTIONALITY_OTHER_NEUTRALS || direction == Character.DIRECTIONALITY_BOUNDARY_NEUTRAL
						|| direction == Character.DIRECTIONALITY_NONSPACING_MARK;
			} else {
				allowed = direction == Character.DIRECTIONALITY_LEFT_TO_RIGHT || direction == Character.DIRECTIONALITY_EUROPEAN_NUMBER || direction == Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR
						|| direction == Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR || direction == Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR || direction == Character.DIRECTIONALITY_OTHER_NEUTRALS
						|| direction == Character.DIRECTIONALITY_BOUNDARY_NEUTRAL || direction == Character.DIRECTIONALITY_NONSPACING_MARK;
			}
			if (!allowed) {
				return false;
			}
		}

		if (lastNonMark < 0) {
			return false;
		}
		final byte last = Character.getDirectionality(codePoints[lastNonMark]);
		if (rightToLeft) {
			return (last == Character.DIRECTIONALITY_RIGHT_TO_LEFT || last == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC || last == Character.DIRECTIONALITY_EUROPEAN_NUMBER || last == Character.DIRECTIONALITY_ARABIC_NUMBER)
					&& !(hasEuropeanNumber && hasArabicNumber);
		} else {
			return last == Character.DIRECTIONALITY_LEFT_TO_RIGHT || last == Character.DIRECTIONALITY_EUROPEAN_NUMBER;
		}
	}

	/**
	 * Checks whether a text contains only ASCII characters.
	 *
	 * @param value
	 *            the text
	 * @return true, if all characters are below 0x80
	 */
	private static boolean isAscii(final String value) {
		for (int i = 0; i < value.length(); i++) {
			if (value.charAt(i) >= 0x80) {
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------------------------------
	// Punycode (RFC 3492)
	// ------------------------------------------------------------------------------------------

	private static final int PUNYCODE_BASE = 36;
	private static final int PUNYCODE_TMIN = 1;
	private static final int PUNYCODE_TMAX = 26;
	private static final int PUNYCODE_SKEW = 38;
	private static final int PUNYCODE_DAMP = 700;
	private static final int PUNYCODE_INITIAL_BIAS = 72;
	private static final int PUNYCODE_INITIAL_N = 0x80;

	/**
	 * Decodes Punycode (RFC 3492).
	 *
	 * @param input
	 *            the Punycode text without "xn--" prefix, lower case
	 * @return the decoded text, or null if the input is invalid
	 */
	static String decodePunycode(final String input) {
		final int delimiterIndex = input.lastIndexOf('-');
		final List<Integer> output = new ArrayList<>();
		if (delimiterIndex > 0) {
			for (int i = 0; i < delimiterIndex; i++) {
				final char basicChar = input.charAt(i);
				if (basicChar >= 0x80) {
					return null;
				}
				output.add((int) basicChar);
			}
		}
		long n = PUNYCODE_INITIAL_N;
		long i = 0;
		int bias = PUNYCODE_INITIAL_BIAS;
		int position = delimiterIndex > 0 ? delimiterIndex + 1 : 0;
		while (position < input.length()) {
			final long oldI = i;
			long weight = 1;
			for (int k = PUNYCODE_BASE;; k += PUNYCODE_BASE) {
				if (position >= input.length()) {
					return null;
				}
				final int digit = decodePunycodeDigit(input.charAt(position++));
				if (digit < 0) {
					return null;
				}
				i += digit * weight;
				if (i > Integer.MAX_VALUE) {
					return null;
				}
				final int threshold = k <= bias ? PUNYCODE_TMIN : (k >= bias + PUNYCODE_TMAX ? PUNYCODE_TMAX : k - bias);
				if (digit < threshold) {
					break;
				}
				weight *= PUNYCODE_BASE - threshold;
				if (weight > Integer.MAX_VALUE) {
					return null;
				}
			}
			bias = adaptPunycodeBias(i - oldI, output.size() + 1, oldI == 0);
			n += i / (output.size() + 1);
			i %= output.size() + 1;
			if (n > 0x10FFFF || (n >= 0xD800 && n <= 0xDFFF)) {
				return null;
			}
			output.add((int) i, (int) n);
			i++;
		}
		final StringBuilder result = new StringBuilder();
		for (final int codePoint : output) {
			result.appendCodePoint(codePoint);
		}
		return result.toString();
	}

	/**
	 * Encodes a text as Punycode (RFC 3492).
	 *
	 * @param input
	 *            the text
	 * @return the Punycode text without "xn--" prefix, or null on overflow
	 */
	static String encodePunycode(final String input) {
		final int[] codePoints = input.codePoints().toArray();
		final StringBuilder output = new StringBuilder();
		for (final int codePoint : codePoints) {
			if (codePoint < 0x80) {
				output.append((char) codePoint);
			}
		}
		final int basicCount = output.length();
		int handledCount = basicCount;
		if (basicCount > 0) {
			output.append('-');
		}
		long n = PUNYCODE_INITIAL_N;
		long delta = 0;
		int bias = PUNYCODE_INITIAL_BIAS;
		while (handledCount < codePoints.length) {
			long m = Long.MAX_VALUE;
			for (final int codePoint : codePoints) {
				if (codePoint >= n && codePoint < m) {
					m = codePoint;
				}
			}
			delta += (m - n) * (handledCount + 1);
			if (delta > Integer.MAX_VALUE) {
				return null;
			}
			n = m;
			for (final int codePoint : codePoints) {
				if (codePoint < n) {
					delta++;
				}
				if (codePoint == n) {
					long q = delta;
					for (int k = PUNYCODE_BASE;; k += PUNYCODE_BASE) {
						final int threshold = k <= bias ? PUNYCODE_TMIN : (k >= bias + PUNYCODE_TMAX ? PUNYCODE_TMAX : k - bias);
						if (q < threshold) {
							break;
						}
						output.append(encodePunycodeDigit((int) (threshold + (q - threshold) % (PUNYCODE_BASE - threshold))));
						q = (q - threshold) / (PUNYCODE_BASE - threshold);
					}
					output.append(encodePunycodeDigit((int) q));
					bias = adaptPunycodeBias(delta, handledCount + 1, handledCount == basicCount);
					delta = 0;
					handledCount++;
				}
			}
			delta++;
			n++;
		}
		return output.toString();
	}

	/**
	 * Adapts the Punycode bias (RFC 3492 section 6.1).
	 *
	 * @param delta
	 *            the delta
	 * @param numPoints
	 *            number of code points handled so far
	 * @param firstTime
	 *            whether this is the first adaption
	 * @return the new bias
	 */
	private static int adaptPunycodeBias(final long delta, final int numPoints, final boolean firstTime) {
		long adaptedDelta = firstTime ? delta / PUNYCODE_DAMP : delta / 2;
		adaptedDelta += adaptedDelta / numPoints;
		int k = 0;
		while (adaptedDelta > ((PUNYCODE_BASE - PUNYCODE_TMIN) * PUNYCODE_TMAX) / 2) {
			adaptedDelta /= PUNYCODE_BASE - PUNYCODE_TMIN;
			k += PUNYCODE_BASE;
		}
		return (int) (k + (PUNYCODE_BASE - PUNYCODE_TMIN + 1) * adaptedDelta / (adaptedDelta + PUNYCODE_SKEW));
	}

	/**
	 * Decodes a Punycode digit.
	 *
	 * @param character
	 *            the character
	 * @return the digit value, or -1 if invalid
	 */
	private static int decodePunycodeDigit(final char character) {
		if (character >= 'a' && character <= 'z') {
			return character - 'a';
		} else if (character >= '0' && character <= '9') {
			return character - '0' + 26;
		} else {
			return -1;
		}
	}

	/**
	 * Encodes a Punycode digit.
	 *
	 * @param digit
	 *            the digit value, 0 to 35
	 * @return the character
	 */
	private static char encodePunycodeDigit(final int digit) {
		return (char) (digit < 26 ? 'a' + digit : '0' + digit - 26);
	}

	// ------------------------------------------------------------------------------------------
	// URI, IRI (RFC 3986, RFC 3987), URI template (RFC 6570)
	// ------------------------------------------------------------------------------------------

	/**
	 * Checks the "uri" format: absolute URI after RFC 3986.
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidUri(final String value) {
		return matchesUri(URI_PATTERN, value);
	}

	/**
	 * Checks the "uri-reference" format: URI or relative reference after RFC 3986.
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidUriReference(final String value) {
		return matchesUri(URI_REFERENCE_PATTERN, value);
	}

	/**
	 * Checks the "iri" format: absolute IRI after RFC 3987.
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidIri(final String value) {
		return matchesUri(IRI_PATTERN, value);
	}

	/**
	 * Checks the "iri-reference" format: IRI or relative reference after RFC 3987.
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidIriReference(final String value) {
		return matchesUri(IRI_REFERENCE_PATTERN, value);
	}

	/**
	 * Matches a URI pattern and checks an IPv6 literal host, if there is one.
	 *
	 * @param pattern
	 *            the URI pattern, group "host" (or "relativeHost") captures the host
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	private static boolean matchesUri(final Pattern pattern, final String value) {
		final java.util.regex.Matcher matcher = pattern.matcher(value);
		if (!matcher.matches()) {
			return false;
		}
		String host = matcher.group("host");
		if (host == null && pattern.pattern().contains("(?<relativeHost>")) {
			host = matcher.group("relativeHost");
		}
		if (host != null && host.startsWith("[")) {
			final java.util.regex.Matcher literalMatcher = IP_LITERAL_PATTERN.matcher(host);
			if (!literalMatcher.matches()) {
				return false;
			}
			return literalMatcher.group(1) == null || isValidIpv6(literalMatcher.group(1));
		}
		return true;
	}

	/**
	 * Creates the regular expression of the URI or IRI grammar.
	 *
	 * @param iri
	 *            true for IRI (RFC 3987), false for URI (RFC 3986)
	 * @param allowRelativeReference
	 *            true to also allow relative references
	 * @return the regular expression with a named group "host"
	 */
	private static String createUriRegex(final boolean iri, final boolean allowRelativeReference) {
		final String unreserved = UNRESERVED + (iri ? UCSCHAR : "");
		final String pchar = "(?:[" + unreserved + SUB_DELIMS + ":@]|" + PCT_ENCODED + ")";
		final String pcharNoColon = "(?:[" + unreserved + SUB_DELIMS + "@]|" + PCT_ENCODED + ")";
		final String segment = pchar + "*";
		final String segmentNz = pchar + "+";
		final String segmentNzNc = pcharNoColon + "+";
		final String query = "(?:" + pchar + "|[/?" + (iri ? IPRIVATE : "") + "])*";
		final String fragment = "(?:" + pchar + "|[/?])*";
		final String userinfo = "(?:[" + unreserved + SUB_DELIMS + ":]|" + PCT_ENCODED + ")*";
		final String regName = "(?:[" + unreserved + SUB_DELIMS + "]|" + PCT_ENCODED + ")*";
		final String ipLiteral = "\\[[^\\]]*\\]";
		final String authority = "(?:" + userinfo + "@)?(?<host>" + ipLiteral + "|" + regName + ")(?::[0-9]*)?";
		final String pathAbempty = "(?:/" + segment + ")*";
		final String pathAbsolute = "/(?:" + segmentNz + "(?:/" + segment + ")*)?";
		final String pathRootless = segmentNz + "(?:/" + segment + ")*";
		final String pathNoScheme = segmentNzNc + "(?:/" + segment + ")*";
		final String scheme = "[A-Za-z][A-Za-z0-9+\\-.]*";

		final String hierPart = "(?://" + authority + pathAbempty + "|" + pathAbsolute + "|" + pathRootless + "|)";
		final String absolute = scheme + ":" + hierPart + "(?:\\?" + query + ")?(?:#" + fragment + ")?";
		if (!allowRelativeReference) {
			return absolute;
		}
		// A named group must occur only once, so the relative part uses its own host group
		final String relativeAuthority = authority.replace("(?<host>", "(?<relativeHost>");
		final String relativePart = "(?://" + relativeAuthority + pathAbempty + "|" + pathAbsolute + "|" + pathNoScheme + "|)";
		final String relative = relativePart + "(?:\\?" + query + ")?(?:#" + fragment + ")?";
		return "(?:" + absolute + "|" + relative + ")";
	}

	/**
	 * Checks the "uri-template" format: URI template after RFC 6570 (level 4).
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidUriTemplate(final String value) {
		int position = 0;
		while (position < value.length()) {
			final int codePoint = value.codePointAt(position);
			if (codePoint == '{') {
				final int closeIndex = value.indexOf('}', position);
				if (closeIndex < 0) {
					return false;
				}
				String expression = value.substring(position + 1, closeIndex);
				if (expression.isEmpty()) {
					return false;
				}
				if ("+#./;?&=,!@|".indexOf(expression.charAt(0)) >= 0) {
					if ("=,!@|".indexOf(expression.charAt(0)) >= 0) {
						// Operators reserved for future extensions
						return false;
					}
					expression = expression.substring(1);
				}
				for (final String varspec : expression.split(",", -1)) {
					if (!URI_TEMPLATE_VARSPEC_PATTERN.matcher(varspec).matches()) {
						return false;
					}
				}
				position = closeIndex + 1;
			} else if (codePoint == '%') {
				if (position + 2 >= value.length() || !isAsciiHexDigit(value.charAt(position + 1)) || !isAsciiHexDigit(value.charAt(position + 2))) {
					return false;
				}
				position += 3;
			} else if (codePoint <= 0x20 || codePoint == 0x7F || "\"<>\\^`{|}".indexOf(codePoint) >= 0 || (codePoint >= 0x80 && codePoint < 0xA0)) {
				// The apostrophe is allowed in literals (RFC 6570 errata 6937)
				return false;
			} else {
				position += Character.charCount(codePoint);
			}
		}
		return true;
	}

	// ------------------------------------------------------------------------------------------
	// JSON pointer, UUID
	// ------------------------------------------------------------------------------------------

	/**
	 * Checks the "json-pointer" format (RFC 6901), e.g. "/foo/0/a~1b".
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidJsonPointer(final String value) {
		return JSON_POINTER_PATTERN.matcher(value).matches();
	}

	/**
	 * Checks the "relative-json-pointer" format, e.g. "0/foo" or "1#".
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidRelativeJsonPointer(final String value) {
		return RELATIVE_JSON_POINTER_PATTERN.matcher(value).matches();
	}

	/**
	 * Checks the "uuid" format (RFC 4122), e.g. "2eb8aa08-aa98-11ea-b4aa-73b441d16380".
	 *
	 * @param value
	 *            the value
	 * @return true, if valid
	 */
	public static boolean isValidUuid(final String value) {
		return UUID_PATTERN.matcher(value).matches();
	}
}
