package de.soderer.json.utilities;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validation helper methods for network related data formats like email addresses, host names, IP
 * addresses and URIs.
 */
public class NetworkUtilities {
	private static final String SPECIAL_CHARS_REGEXP = "\\p{Cntrl}\\(\\)<>@,;:'\\\\\\\"\\.\\[\\]";
	private static final String VALID_CHARS_REGEXP = "[^\\s" + SPECIAL_CHARS_REGEXP + "]";
	private static final String QUOTED_USER_REGEXP = "(\"[^\"]*\")";
	private static final String WORD_REGEXP = "((" + VALID_CHARS_REGEXP + "|')+|" + QUOTED_USER_REGEXP + ")";

	private static final String DOMAIN_PART_REGEX = "\\p{Alnum}(?>[\\p{Alnum}-]*\\p{Alnum})*";
	private static final String TOP_DOMAIN_PART_REGEX = "\\p{Alpha}{2,}";
	private static final String DOMAIN_NAME_REGEX = "^(?:" + DOMAIN_PART_REGEX + "\\.)+" + "(" + TOP_DOMAIN_PART_REGEX + ")$";

	private static final Pattern IPV4_PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

	private static final Pattern IPV6_PATTERN = Pattern.compile(
			"("
					+ "([0-9A-F]{1,4}:){7,7}[0-9A-F]{1,4}|"          //# 1:2:3:4:5:6:7:8
					+ "([0-9A-F]{1,4}:){1,7}:|"                      //# 1::                              1:2:3:4:5:6:7::
					+ "([0-9A-F]{1,4}:){1,6}:[0-9A-F]{1,4}|"         //# 1::8             1:2:3:4:5:6::8  1:2:3:4:5:6::8
					+ "([0-9A-F]{1,4}:){1,5}(:[0-9A-F]{1,4}){1,2}|"  //# 1::7:8           1:2:3:4:5::7:8  1:2:3:4:5::8
					+ "([0-9A-F]{1,4}:){1,4}(:[0-9A-F]{1,4}){1,3}|"  //# 1::6:7:8         1:2:3:4::6:7:8  1:2:3:4::8
					+ "([0-9A-F]{1,4}:){1,3}(:[0-9A-F]{1,4}){1,4}|"  //# 1::5:6:7:8       1:2:3::5:6:7:8  1:2:3::8
					+ "([0-9A-F]{1,4}:){1,2}(:[0-9A-F]{1,4}){1,5}|"  //# 1::4:5:6:7:8     1:2::4:5:6:7:8  1:2::8
					+ "[0-9A-F]{1,4}:((:[0-9A-F]{1,4}){1,6})|"       //# 1::3:4:5:6:7:8   1::3:4:5:6:7:8  1::8
					+ ":((:[0-9A-F]{1,4}){1,7}|:)|"                  //# ::2:3:4:5:6:7:8  ::2:3:4:5:6:7:8 ::8       ::
					+ "FE80:(:[0-9A-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|"  //# fe80::7:8%eth0   fe80::7:8%1     (link-local IPv6 addresses with zone index)
					+ "::(FFFF(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|" //# ::255.255.255.255   ::ffff:255.255.255.255  ::ffff:0:255.255.255.255  (IPv4-mapped IPv6 addresses and IPv4-translated addresses)
					+ "([0-9A-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])" //# 2001:db8:3:4::192.0.2.33  64:ff9b::192.0.2.33 (IPv4-Embedded IPv6 Address)
					+ ")", Pattern.CASE_INSENSITIVE
			);

	/**
	 * Regular expression for parsing email addresses.
	 *
	 * Taken from Apache Commons Validator.
	 * If this is not working, shame on Apache ;)
	 */
	private static final String EMAIL_REGEX = "^\\s*?(.+)@(.+?)\\s*$";

	private static final String USER_REGEX = "^\\s*" + WORD_REGEXP + "(\\." + WORD_REGEXP + ")*$";

	/** Regular expression pattern for parsing email addresses. */
	private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

	private static final Pattern USER_PATTERN = Pattern.compile(USER_REGEX);

	private static final Pattern DOMAIN_NAME_PATTERN = Pattern.compile(DOMAIN_NAME_REGEX);

	/**
	 * Utility class, not to be instantiated.
	 */
	private NetworkUtilities() {
	}

	/**
	 * Checks whether a string is a valid fully qualified domain name with at least two labels, like
	 * "example.com". Internationalized domain names (like "münchen.de") are converted to their
	 * ASCII form (punycode) before the check. The top level domain "local" is not accepted.
	 *
	 * @param domain
	 *            the domain name to check
	 * @return true, if the domain name is valid
	 */
	public static boolean isValidDomain(final String domain) {
		final String asciiDomainName;
		try {
			asciiDomainName = java.net.IDN.toASCII(domain);
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Invalid domain name like abc@.ch
			return false;
		}

		// Do not allow ".local" top level domain
		if (asciiDomainName.toLowerCase().endsWith(".local")) {
			return false;
		}

		// Punycode labels ("xn--...") consist of letters, digits and hyphens, so they are checked by the same pattern
		return DOMAIN_NAME_PATTERN.matcher(asciiDomainName).matches();
	}

	/**
	 * Checks whether a string is a valid email address, consisting of a valid user part and a valid
	 * domain part (see {@link #isValidDomain(String)}).
	 *
	 * @param emailAddress
	 *            the email address to check
	 * @return true, if the email address is valid
	 */
	public static boolean isValidEmail(final String emailAddress) {
		final Matcher m = EMAIL_PATTERN.matcher(emailAddress);

		// Check, if email address matches outline structure
		if (!m.matches()) {
			return false;
		}

		// Check if user-part is valid
		if (!isValidUser(m.group(1))) {
			return false;
		}

		// Check if domain-part is valid
		if (!isValidDomain(m.group(2))) {
			return false;
		}

		return true;
	}

	/**
	 * Checks whether a string is a valid user part of an email address (the part before "@").
	 *
	 * @param user
	 *            the user part to check
	 * @return true, if the user part is valid
	 */
	public static boolean isValidUser(final String user) {
		return USER_PATTERN.matcher(user).matches();
	}

	/**
	 * Checks whether a string is a valid host name. Currently the same as
	 * {@link #isValidDomain(String)}, so single label host names like "localhost" are not accepted.
	 *
	 * @param value
	 *            the host name to check
	 * @return true, if the host name is valid
	 */
	public static boolean isValidHostname(final String value) {
		return isValidDomain(value);
	}

	/**
	 * Checks whether a string is a valid IPv4 address in dotted decimal notation, like
	 * "192.168.0.1".
	 *
	 * @param ipv4
	 *            the address to check
	 * @return true, if the address is valid
	 */
	public static boolean isValidIpV4(final String ipv4) {
		return IPV4_PATTERN.matcher(ipv4).matches();
	}

	/**
	 * Checks whether a string is a valid IPv6 address, including compressed notation ("::1"),
	 * link-local addresses with zone index and IPv4 mapped or embedded addresses.
	 *
	 * @param ipv6
	 *            the address to check
	 * @return true, if the address is valid
	 */
	public static boolean isValidIpV6(final String ipv6) {
		return IPV6_PATTERN.matcher(ipv6).matches();
	}

	/**
	 * Checks whether a string is a valid absolute URI with scheme (RFC 3986), like
	 * "https://example.com/path". Relative references like "path/file" are not accepted.
	 *
	 * @param uri
	 *            the URI to check
	 * @return true, if the URI is valid and absolute
	 */
	public static boolean isValidUri(final String uri) {
		try {
			return new URI(uri).isAbsolute();
		} catch (@SuppressWarnings("unused") final Exception e) {
			return false;
		}
	}
}
