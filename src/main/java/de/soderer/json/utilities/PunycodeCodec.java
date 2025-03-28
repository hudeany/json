package de.soderer.json.utilities;

import java.net.IDN;

public class PunycodeCodec {
	private final static int TMIN = 1;
	private final static int TMAX = 26;
	private final static int BASE = 36;
	private final static int INITIAL_N = 128;
	private final static int INITIAL_BIAS = 72;
	private final static int DAMP = 700;
	private final static int SKEW = 38;
	private final static char DELIMITER = '-';

	/**
	 * Encode a string in Unicode
	 */
	public static String encode(final String stringToEncode) throws Exception {
		if (stringToEncode == null) {
			return null;
		}

		int n = INITIAL_N;
		int delta = 0;
		int bias = INITIAL_BIAS;

		final StringBuffer output = new StringBuffer();
		for (final char character : stringToEncode.toCharArray()) {
			if (isSimpleCharacter(character)) {
				output.append(character);
			}
		}

		final int baseOutputLength = output.length();
		if (baseOutputLength > 0 && baseOutputLength < stringToEncode.length()) {
			output.append(DELIMITER);
		}

		int h = baseOutputLength;
		while (h < stringToEncode.length()) {
			int m = Integer.MAX_VALUE;

			for (final int character : stringToEncode.toCharArray()) {
				if (character >= n && character < m) {
					m = character;
				}
			}

			if (m - n > (Integer.MAX_VALUE - delta) / (h + 1)) {
				throw new Exception("Punycode overflow");
			}
			delta = delta + (m - n) * (h + 1);
			n = m;

			for (final char character : stringToEncode.toCharArray()) {
				if (character < n) {
					delta++;
					if (0 == delta) {
						throw new Exception("Punycode overflow");
					}
				}
				if (character == n) {
					int q = delta;

					for (int k = BASE;; k += BASE) {
						int t;
						if (k <= bias) {
							t = TMIN;
						} else if (k >= bias + TMAX) {
							t = TMAX;
						} else {
							t = k - bias;
						}
						if (q < t) {
							break;
						}
						output.append((char) punycodeDigitToCodePoint(t + (q - t) % (BASE - t)));
						q = (q - t) / (BASE - t);
					}

					output.append((char) punycodeDigitToCodePoint(q));
					bias = adapt(delta, h + 1, h == baseOutputLength);
					delta = 0;
					h++;
				}
			}

			delta++;
			n++;
		}

		return output.toString();
	}

	/**
	 * Decode a punycoded string
	 */
	public static String decode(final String punycodeString) throws Exception {
		if (punycodeString == null) {
			return null;
		}

		int n = INITIAL_N;
		int i = 0;
		int bias = INITIAL_BIAS;
		final StringBuffer output = new StringBuffer();

		int d = punycodeString.lastIndexOf(DELIMITER);
		if (d > 0) {
			for (int j = 0; j < d; j++) {
				final char c = punycodeString.charAt(j);
				if (!isSimpleCharacter(c)) {
					throw new Exception("Invalid input for decoding punycode");
				}
				output.append(c);
			}
			d++;
		} else {
			d = 0;
		}

		while (d < punycodeString.length()) {
			final int oldi = i;
			int w = 1;

			for (int k = BASE;; k += BASE) {
				if (d == punycodeString.length()) {
					throw new Exception("Invalid input for decoding punycode");
				}
				final int c = punycodeString.charAt(d++);
				final int digit = codePointToPunycodeDigit(c);
				if (digit > (Integer.MAX_VALUE - i) / w) {
					throw new Exception("Punycode overflow");
				}

				i = i + digit * w;

				int t;
				if (k <= bias) {
					t = TMIN;
				} else if (k >= bias + TMAX) {
					t = TMAX;
				} else {
					t = k - bias;
				}
				if (digit < t) {
					break;
				}
				w = w * (BASE - t);
			}

			bias = adapt(i - oldi, output.length() + 1, oldi == 0);

			if (i / (output.length() + 1) > Integer.MAX_VALUE - n) {
				throw new Exception("Punycode overflow");
			}

			n = n + i / (output.length() + 1);
			i = i % (output.length() + 1);
			output.insert(i, (char) n);
			i++;
		}

		return output.toString();
	}

	private final static int adapt(int delta, final int numpoints, final boolean first) {
		if (first) {
			delta = delta / DAMP;
		} else {
			delta = delta / 2;
		}

		delta = delta + (delta / numpoints);

		int k = 0;
		while (delta > ((BASE - TMIN) * TMAX) / 2) {
			delta = delta / (BASE - TMIN);
			k = k + BASE;
		}

		return k + ((BASE - TMIN + 1) * delta) / (delta + SKEW);
	}

	private final static boolean isSimpleCharacter(final char character) {
		return character < 0x80;
	}

	private final static int punycodeDigitToCodePoint(final int punycodeDigit) throws Exception {
		if (punycodeDigit < 26) {
			return punycodeDigit + 'a';
		} else if (punycodeDigit < 36) {
			return punycodeDigit - 26 + '0';
		} else {
			throw new Exception("Invalid input for decoding punycode");
		}
	}

	private final static int codePointToPunycodeDigit(final int codepoint) throws Exception {
		if (codepoint - '0' < 10) {
			return codepoint - '0' + 26;
		} else if (codepoint - 'a' < 26) {
			return codepoint - 'a';
		} else {
			throw new Exception("Invalid input for decoding punycode");
		}
	}

	public static String encodeDomainName(final String domainName) {
		if (domainName == null) {
			return null;
		} else {
			return IDN.toASCII(domainName);
		}
	}

	public static String decodeDomainName(final String domainNameToDecode) {
		if (domainNameToDecode == null) {
			return null;
		} else {
			return IDN.toUnicode(domainNameToDecode);
		}
	}

	public static String encodeEmailAdress(final String email) throws Exception {
		if (email == null) {
			return null;
		} else if (!NetworkUtilities.isValidEmail(email)) {
			throw new Exception("Not a valid email address: " + email);
		} else {
			final int mailDelimiterIndex = email.trim().indexOf('@');
			if (mailDelimiterIndex < 0) {
				throw new Exception("Invalid email address");
			}

			final String localPart = email.substring(0, mailDelimiterIndex);
			final String domainName = email.substring(mailDelimiterIndex + 1);
			final StringBuilder encodedEmail = new StringBuilder();
			encodedEmail.append(localPart);
			encodedEmail.append('@');
			encodedEmail.append(encodeDomainName(domainName));

			return encodedEmail.toString();
		}
	}

	public static String decodeEmailAdress(final String emailToDecode) throws Exception {
		if (emailToDecode == null) {
			return null;
		} else {
			final int mailDelimiterIndex = emailToDecode.trim().indexOf('@');
			if (mailDelimiterIndex < 0) {
				throw new Exception("Invalid email address");
			}

			final String localPart = emailToDecode.substring(0, mailDelimiterIndex);
			final String domainName = emailToDecode.substring(mailDelimiterIndex + 1);
			final StringBuilder decodedEmail = new StringBuilder();
			decodedEmail.append(localPart);
			decodedEmail.append('@');
			decodedEmail.append(decodeDomainName(domainName));

			return decodedEmail.toString();
		}
	}
}
