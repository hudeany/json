package de.soderer.json.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * Global Utilities
 *
 * This class does no Logging via Log4J, because it is often used before its initialisation
 */
public class Utilities {
	public static final int EOF = -1;

	public static final String STANDARD_XML = "<?xml version=\"1.0\" encoding=\"<encoding>\" standalone=\"yes\"?>\n<root>\n</root>\n";
	public static final String STANDARD_HTML = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n<html xmlns=\"http://www.w3.org/1999/xhtml\">\n\t<head>\n\t\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=<encoding>\" />\n\t\t<title>HtmlTitle</title>\n\t\t<meta name=\"Title\" content=\"HtmlTitle\" />\n\t</head>\n\t<body>\n\t</body>\n</html>\n";
	public static final String STANDARD_BASHSCRIPTSTART = "#!/bin/bash\n";
	public static final String STANDARD_JSON = "{\n\t\"property1\": null,\n\t\"property2\": " + Math.PI + ",\n\t\"property3\": true,\n\t\"property4\": \"Text\",\n\t\"property5\": [\n\t\tnull,\n\t\t" + Math.PI + ",\n\t\ttrue,\n\t\t\"Text\"\n\t]\n}\n";
	public static final String STANDARD_YAML = "property1: null\nproperty2: " + Math.PI + "\nproperty3: true\nproperty4: Text\nproperty5:\n  - null\n  - " + Math.PI + "\n  - true\n  - Text\n";

	/**
	 * Generate a unique ID
	 *
	 * @return random UUID in upper case without hyphens (32 hex characters)
	 */
	public static String generateUUID() {
		return UUID.randomUUID().toString().toUpperCase().replaceAll("-", "");
	}

	/**
	 * Get a UUID from a string
	 *
	 * @param value
	 *            UUID string without hyphens (32 hex characters)
	 * @return UUID
	 */
	public static UUID getUUIDFromString(final String value) {
		final StringBuilder uuidString = new StringBuilder(value);
		uuidString.insert(20, '-');
		uuidString.insert(16, '-');
		uuidString.insert(12, '-');
		uuidString.insert(8, '-');
		return UUID.fromString(uuidString.toString());
	}

	/**
	 * Get the data of a file included in a jar file
	 *
	 * @param resourceName
	 *            path of the resource within the classpath, without leading slash
	 * @return stream of the resource data, or null if the resource does not exist
	 */
	public static InputStream getResourceAsStream(final String resourceName) {
		return Utilities.class.getResourceAsStream("/" + resourceName);
	}

	/**
	 * Zip a byteArray by GZIP-Algorithm
	 *
	 * @param clearData
	 *            data to compress
	 * @return compressed data, or null on error
	 */
	public static byte[] gzipByteArray(final byte[] clearData) {
		try (ByteArrayOutputStream encoded = new ByteArrayOutputStream();
				GZIPOutputStream gzipCompresser = new GZIPOutputStream(encoded)) {
			gzipCompresser.write(clearData);
			gzipCompresser.close();
			return encoded.toByteArray();
		} catch (@SuppressWarnings("unused") final IOException e) {
			return null;
		}
	}

	/**
	 * Unzip a byteArray by GZIP-Algorithm
	 *
	 * @param zippedData
	 *            GZIP compressed data
	 * @return uncompressed data, or null if the data cannot be read
	 * @throws Exception
	 *             if decompressing fails with an error other than an IOException
	 */
	public static byte[] gunzipByteArray(final byte[] zippedData) throws Exception {
		try {
			final ByteArrayOutputStream decoded = new ByteArrayOutputStream();
			final ByteArrayInputStream encoded = new ByteArrayInputStream(zippedData);
			IoUtilities.copy(new GZIPInputStream(encoded), decoded);
			return decoded.toByteArray();
		} catch (@SuppressWarnings("unused") final IOException e) {
			return null;
		}
	}

	/**
	 * Encode data as Base64 String
	 *
	 * @param clearData
	 *            data to encode
	 * @return Base64 string
	 */
	public static String encodeBase64(final byte[] clearData) {
		return Base64.getEncoder().encodeToString(clearData);
	}

	/**
	 * Encode data as Base64 String split into lines
	 *
	 * @param clearData
	 *            data to encode
	 * @param maxCharactersPerLine
	 *            maximum number of characters per line
	 * @param splitCharacters
	 *            characters appended after every full line, e.g. a linebreak
	 * @return Base64 string split into lines
	 */
	public static String encodeBase64(final byte[] clearData, final int maxCharactersPerLine, final String splitCharacters) {
		final String dataBase64 = Base64.getEncoder().encodeToString(clearData);
		final StringBuilder returnString = new StringBuilder();
		final int fullLines = (int) Math.floor(dataBase64.length() / maxCharactersPerLine);
		for (int i = 0; i < fullLines; i++) {
			returnString.append(dataBase64.substring(i * maxCharactersPerLine, (i * maxCharactersPerLine) + maxCharactersPerLine));
			returnString.append(splitCharacters);
		}
		returnString.append(dataBase64.substring(fullLines * maxCharactersPerLine, (fullLines * maxCharactersPerLine) + (dataBase64.length() % maxCharactersPerLine)));
		return returnString.toString();
	}

	/**
	 * Decode a Base64 String
	 *
	 * @param base64String
	 *            Base64 string, whitespace and linebreaks are ignored
	 * @return decoded data
	 */
	public static byte[] decodeBase64(final String base64String) {
		return Base64.getDecoder().decode(base64String.replace("\r", "").replace("\n", "").replace("\t", "").replace(" ", "").getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Check a simple name string (only letters A-Z and a-z, digits, '_' and '-')
	 *
	 * @param value
	 *            name to check
	 * @return true if the name is valid, false for null
	 */
	public static boolean checkForValidUserName(final String value) {
		return value != null && value.matches("[A-Za-z0-9_-]*");
	}

	/**
	 * Convert an ArrayList of Strings to a StringArray
	 *
	 * @param pArrayListOfStrings
	 *            list to convert
	 * @return array with the strings of the list
	 */
	public static String[] convertArrayListOfStringsToStringArray(final ArrayList<String> pArrayListOfStrings) {
		final String[] arrayofStrings = new String[0];
		return pArrayListOfStrings.toArray(arrayofStrings);
	}

	/**
	 * Get index of an Integer within an Array of Integers
	 *
	 * @param searchInt
	 *            value to search for
	 * @param intArray
	 *            array to search in
	 * @return index of the first occurrence, or -1 if not found
	 */
	public static int getIndex(final int searchInt, final int[] intArray) {
		for (int i = 0; i < intArray.length; i++) {
			if (intArray[i] == searchInt) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Read stream data in byteArray until next linefeed or stream end
	 *
	 * @param inStream
	 *            stream to read from
	 * @return read data, including the linefeed if one was found
	 * @throws IOException
	 *             if reading fails
	 */
	public static byte[] readStreamUntilEndOrLinefeed(final InputStream inStream) throws IOException {
		final ByteArrayOutputStream returnData = new ByteArrayOutputStream();
		int nextByte;
		while (true) {
			nextByte = inStream.read();
			if (nextByte < 0) {
				break;
			} else if (nextByte == '\n') {
				returnData.write(nextByte);
				break;
			} else {
				returnData.write(nextByte);
			}
		}
		return returnData.toByteArray();
	}

	/**
	 * Get email from X509Certificate
	 *
	 * @param cert
	 *            certificate
	 * @return email address of the certificate subject, or null if not available
	 */
	public static String getEmailFromCertificate(final X509Certificate cert) {
		final String[] nameParts = cert.getSubjectX500Principal().toString().split(",");
		for (final String namePart : nameParts) {
			if (namePart.matches("^[ \\t]*EMAILADDRESS=.*")) {
				return namePart.substring(namePart.indexOf("=") + 1).trim();
			}
		}

		return null;
	}

	/**
	 * Get cn from X509Certificate
	 *
	 * @param cert
	 *            certificate
	 * @return common name (CN) of the certificate subject, or null if not available
	 */
	public static String getCnFromCertificate(final X509Certificate cert) {
		final String[] nameParts = cert.getSubjectX500Principal().toString().split(",");
		for (final String namePart : nameParts) {
			if (namePart.matches("^[ \\t]*CN=.*")) {
				return namePart.substring(namePart.indexOf("=") + 1).trim();
			}
		}

		return null;
	}

	/**
	 * Split a list into smaller lists to a maximum chunkSize
	 *
	 * @param originalList
	 *            list to split
	 * @param chunkSize
	 *            maximum size of each chunk
	 * @return list of sublists (views on the original list), or null for an empty list or a chunk size less than 1
	 */
	public static <E> List<List<E>> chopListToChunks(final List<E> originalList, final int chunkSize) {
		if (originalList == null || originalList.size() <= 0 || chunkSize <= 0) {
			return null;
		}

		final List<List<E>> returnList = new ArrayList<>();
		int endIndex = 0;

		while (endIndex < originalList.size()) {
			final int startIndex = endIndex;
			if (chunkSize < originalList.size() - endIndex) {
				endIndex += chunkSize;
			} else {
				endIndex = originalList.size();
			}

			returnList.add(originalList.subList(startIndex, endIndex));
		}

		return returnList;
	}

	/**
	 * List of characters for randomization
	 */
	private static final char[] randomCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜabcdefghijklmnopqrstuvwxyzäöüß".toCharArray();

	/**
	 * List of numbers and strings for randomization
	 */
	private static final char[] randomAlphaNumericCharacters = (new String(randomCharacters) + "0123456789").toCharArray();

	/**
	 * Random generator
	 */
	private static final Random random = new SecureRandom();

	/**
	 * Generate a random number up to maximum value
	 *
	 * @param excludedMaximum
	 *            upper bound (exclusive)
	 * @return random number from 0 to excludedMaximum - 1
	 */
	public static int getRandomNumber(final int excludedMaximum) {
		return random.nextInt(excludedMaximum);
	}

	/**
	 * Generate a random string of given size
	 *
	 * @param length
	 *            length of the string
	 * @return random string of letters (including german umlauts)
	 */
	public static String getRandomString(final int length) {
		final StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(randomCharacters[random.nextInt(randomCharacters.length)]);
		}
		return sb.toString();
	}

	/**
	 * Generate a random string of numbers and characters of given size
	 *
	 * @param length
	 *            length of the string
	 * @return random string of letters (including german umlauts) and digits
	 */
	public static String getRandomAlphanumericString(final int length) {
		final StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(randomAlphaNumericCharacters[random.nextInt(randomAlphaNumericCharacters.length)]);
		}
		return sb.toString();
	}

	/**
	 * Generate a random string of digits of given size
	 *
	 * @param length
	 *            number of digits
	 * @return random string of digits
	 */
	public static String getRandomNumberString(final int length) {
		final StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(random.nextInt(10));
		}
		return sb.toString();
	}

	/**
	 * Generate a random byte
	 *
	 * @return random byte
	 */
	public static byte getRandomByte() {
		final byte[] result = new byte[1];
		random.nextBytes(result);
		return result[0];
	}

	/**
	 * Generate a random byteArray
	 *
	 * @param arrayToFill
	 *            array to fill with random bytes
	 * @return the given array, filled with random bytes
	 */
	public static byte[] getRandomByteArray(final byte[] arrayToFill) {
		random.nextBytes(arrayToFill);
		return arrayToFill;
	}

	/**
	 * Check if an Integer is contained by an interval definition like {@code -1;2-5;8+}
	 *
	 * @param intervals
	 *            interval definitions separated by ';': a single value, a range "a-b", "-b" for up to b or "a+" for a and above
	 * @param item
	 *            value to check
	 * @return true if the value is contained in at least one interval
	 */
	public static boolean checkForIntervalContainment(final String intervals, final int item) {
		if (intervals != null && intervals.length() > 0) {
			final String[] blockStrings = intervals.split(";");
			for (final String blockString : blockStrings) {
				if (blockString.endsWith("+")) {
					if (Integer.parseInt(blockString.substring(0, blockString.length() - 1)) <= item) {
						return true;
					}
				} else if (blockString.matches("\\d+-\\d+")) {
					final int plusIndex = blockString.indexOf("-");
					final int startVersion = Integer.parseInt(blockString.substring(0, plusIndex));
					final int endeVersion = Integer.parseInt(blockString.substring(plusIndex + 1));
					if (startVersion <= item && endeVersion >= item) {
						return true;
					}
				} else if (blockString.matches("-\\d+")) {
					final int endeVersion = Integer.parseInt(blockString.substring(1));
					if (endeVersion >= item) {
						return true;
					}
				} else {
					if (Integer.parseInt(blockString) == item) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Get the minimum of a value list down to a valid minimum
	 *
	 * @param allowedValueMinimum
	 *            values below this minimum are ignored
	 * @param values
	 *            values to check
	 * @return smallest allowed value, or Integer.MAX_VALUE if there is none
	 */
	public static int getMinimumOfAllowedValues(final int allowedValueMinimum, final int... values) {
		int returnValue = Integer.MAX_VALUE;
		if (values != null) {
			for (final int value : values) {
				if (value >= allowedValueMinimum) {
					returnValue = Math.min(returnValue, value);
				}
			}
		}
		return returnValue;
	}

	/**
	 * Convert a string to boolean
	 *
	 * @param value
	 *            string to interpret
	 * @return true for "true", "+", "yes", "y", "ja", "j", "ok", "on" or "an" (case insensitive), otherwise false
	 */
	public static boolean interpretAsBool(String value) {
		if (isNotEmpty(value)) {
			value = value.trim();
			return "true".equalsIgnoreCase(value)
					|| "+".equalsIgnoreCase(value)
					|| "yes".equalsIgnoreCase(value)
					|| "y".equalsIgnoreCase(value)
					|| "ja".equalsIgnoreCase(value)
					|| "j".equalsIgnoreCase(value)
					|| "ok".equalsIgnoreCase(value)
					|| "on".equalsIgnoreCase(value)
					|| "an".equalsIgnoreCase(value);
		} else {
			return false;
		}
	}

	/**
	 * Check if any characters in a list are equal
	 *
	 * @param values
	 *            characters to check
	 * @return true if at least two characters are equal
	 */
	public static boolean anyCharsAreEqual(final char... values) {
		for (int i = 0; i < values.length; i++) {
			for (int j = i + 1; j < values.length; j++) {
				if (values[i] == values[j]) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean contains(final char[] characterArray, final Character searchCharacter) {
		if (characterArray == null || searchCharacter == null) {
			return false;
		}

		for (final char character : characterArray) {
			if (character == searchCharacter) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Math.square
	 *
	 * @param value
	 *            value to square
	 * @return value * value
	 */
	public static int square(final int value) {
		return value * value;
	}

	/**
	 * Math power
	 *
	 * @param base
	 *            base
	 * @param exp
	 *            exponent, must not be negative
	 * @return base to the power of exp
	 * @throws IllegalArgumentException
	 *             if the exponent is negative
	 */
	public static int pow(final int base, final int exp) {
		if (exp < 0) {
			throw new IllegalArgumentException("Invalid negative exponent");
		} else if (exp == 0) {
			return 1;
		} else {
			return square(pow(base, exp / 2)) * (exp % 2 == 1 ? base : 1);
		}
	}

	/**
	 * Get a collection like a set as a ordered list
	 *
	 * @param c
	 *            collection to sort
	 * @return new list with the sorted items
	 */
	public static <T extends Comparable<? super T>> List<T> asSortedList(final Collection<T> c) {
		final List<T> list = new ArrayList<>(c);
		Collections.sort(list);
		return list;
	}

	/**
	 * Sort a collection in natural order, but put the given items first in their given order
	 *
	 * @param collection
	 *            items to sort
	 * @param firstItems
	 *            items to put first, in this order
	 * @return new sorted list
	 */
	@SafeVarargs
	public static <T extends Comparable<? super T>> List<T> sortButPutItemsFirst(final Collection<T> collection, final T... firstItems) {
		final List<T> firstItemsList = new ArrayList<>(Arrays.asList(firstItems));
		final List<T> list = new ArrayList<>(collection);
		Collections.sort(list, new Comparator<T>() {
			@Override
			public int compare(final T o1, final T o2) {
				if (o1.equals(o2)) {
					return 0;
				} else if (firstItemsList.contains(o1)) {
					if (firstItemsList.contains(o2)) {
						return firstItemsList.indexOf(o1) < firstItemsList.indexOf(o2) ? -1 : 1;
					} else {
						return -1;
					}
				} else if (firstItemsList.contains(o2)) {
					return 1;
				} else {
					return o1.compareTo(o2);
				}
			}
		});
		return list;
	}

	/**
	 * Sort a map by a Comparator for the keytype
	 *
	 * @param mapToSort
	 *            map to sort
	 * @param comparator
	 *            comparator for the keys
	 * @return new map with the entries in sorted key order
	 */
	public static <Key, Value> Map<Key, Value> sortMap(final Map<Key, Value> mapToSort, final Comparator<Key> comparator) {
		final List<Key> keys = new ArrayList<>(mapToSort.keySet());
		Collections.sort(keys, comparator);
		final LinkedHashMap<Key, Value> sortedContent = new LinkedHashMap<>();
		for (final Key key : keys) {
			sortedContent.put(key, mapToSort.get(key));
		}
		return sortedContent;
	}

	/**
	 * Sort a map by the String keytype
	 *
	 * @param mapToSort
	 *            map to sort
	 * @return new map with the entries in sorted key order
	 */
	public static <Value> Map<String, Value> sortMap(final Map<String, Value> mapToSort) {
		final List<String> keys = new ArrayList<>(mapToSort.keySet());
		Collections.sort(keys);
		final LinkedHashMap<String, Value> sortedContent = new LinkedHashMap<>();
		for (final String key : keys) {
			sortedContent.put(key, mapToSort.get(key));
		}
		return sortedContent;
	}

	/**
	 * Get files of classpath
	 *
	 * @return classpath entries separated by the platform path separator (":" on Linux, ";" on Windows)
	 */
	public static String getClassPath() {
		return System.getProperty("java.class.path");
	}

	/**
	 * Check array equality. Items are compared by equals(), null items are allowed.
	 *
	 * @param array1
	 *            first array
	 * @param array2
	 *            second array
	 * @return true if both arrays are null or have equal items in the same order
	 */
	public static <T> boolean compare(final T[] array1, final T[] array2) {
		return Arrays.equals(array1, array2);
	}

	/**
	 * Get all system properties
	 *
	 * @return map of all system properties, on Linux extended by "os.distribution.name" and "os.distribution.version"
	 */
	public static Map<String, String> getSystemPropertiesMap() {
		final Map<String, String> propertiesMap = new HashMap<>();
		for (final Object key : System.getProperties().keySet()) {
			propertiesMap.put((String) key, System.getProperties().getProperty((String) key));
		}
		if (SystemUtilities.isLinuxSystem()) {
			final File distributionInfoFile = new File("/etc/os-release");
			if (distributionInfoFile.exists()) {
				try (FileInputStream inputStream = new FileInputStream(distributionInfoFile)) {
					final Properties distributionInfoProperties = new Properties();
					distributionInfoProperties.load(inputStream);
					final String distributionName = distributionInfoProperties.getProperty("NAME");
					if (distributionName != null && !"".equals(distributionName.trim())) {
						propertiesMap.put("os.distribution.name",  Utilities.trimSimultaneously(distributionName, "\""));
					} else {
						propertiesMap.put("os.distribution.name", "Unknown");
					}
					final String distributionVersion = distributionInfoProperties.getProperty("VERSION");
					if (distributionVersion != null && !"".equals(distributionVersion.trim())) {
						propertiesMap.put("os.distribution.version",  Utilities.trimSimultaneously(distributionVersion, "\""));
					} else {
						propertiesMap.put("os.distribution.version", "Unknown");
					}
				} catch (@SuppressWarnings("unused") final Exception e) {
					propertiesMap.put("os.distribution.name", "Unknown");
					propertiesMap.put("os.distribution.version", "Unknown");
				}
			} else {
				propertiesMap.put("os.distribution.name", "Unknown");
				propertiesMap.put("os.distribution.version", "Unknown");
			}
		}
		return propertiesMap;
	}

	/**
	 * Convert Map to String
	 *
	 * @param map
	 *            map to convert
	 * @param entrySeparator
	 *            separator between the entries
	 * @param keySeparator
	 *            separator between key and value
	 * @param sort
	 *            sort the entries by key
	 * @return string with all entries of the map
	 */
	public static String getStringFromMap(final Map<String, ? extends Object> map, final String entrySeparator, final String keySeparator, final boolean sort) {
		final List<String> keyList = new ArrayList<>(map.keySet());
		if (sort) {
			Collections.sort(keyList, new Comparator<String>() {
				@Override
				public int compare(final String o1, final String o2) {
					return Comparator.nullsFirst(String::compareTo).compare(o1, o2);
				}
			});
		}

		final StringBuilder builder = new StringBuilder();
		for (final String key : keyList) {
			if (builder.length() > 0) {
				builder.append(entrySeparator);
			}
			builder.append(key == null ? "" : key);
			builder.append(keySeparator);
			final Object value = map.get(key);
			builder.append(value == null ? "" : value.toString());
		}
		return builder.toString();
	}

	/**
	 * Make a number with unitsign human readable
	 *
	 * @param value
	 *            number to format
	 * @param unitTypeSign
	 *            unit sign appended to the unit prefix, e.g. "B" for bytes
	 * @param siUnits
	 *            true for SI units (factor 1000), false for binary units (factor 1024)
	 * @param amountOfSignifiantDigits
	 *            number of significant digits
	 * @param keepTrailingZeros
	 *            keep trailing zeros of the decimals
	 * @param locale
	 *            locale for the decimal separator
	 * @return formatted number with unit, e.g. "1.5 KiB"
	 */
	public static String getHumanReadableNumber(final Number value, final String unitTypeSign, final boolean siUnits, final int amountOfSignifiantDigits, final boolean keepTrailingZeros, final Locale locale) {
		final int unit = siUnits ? 1000 : 1024;
		double interimValue = value.doubleValue();
		String unitExtension = "";
		if (interimValue < unit) {
			if (isNotBlank(unitTypeSign)) {
				unitExtension = " " + unitTypeSign;
			}

			if (value instanceof Integer || value instanceof Long) {
				return value + unitExtension;
			}
		} else {
			final int exp = (int) (Math.log(interimValue) / Math.log(unit));
			unitExtension = " " + (siUnits ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (siUnits ? "" : "i");
			if (isNotBlank(unitTypeSign)) {
				unitExtension += unitTypeSign;
			}
			interimValue = interimValue / Math.pow(unit, exp);
		}

		final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);
		DecimalFormat numberFormat;
		if (keepTrailingZeros) {
			if (interimValue >= 1000) {
				numberFormat = new DecimalFormat("#0." + repeat("0", amountOfSignifiantDigits - 4), decimalFormatSymbols);
			} else if (interimValue >= 100) {
				numberFormat = new DecimalFormat("#0." + repeat("0", amountOfSignifiantDigits - 3), decimalFormatSymbols);
			} else if (interimValue >= 10) {
				numberFormat = new DecimalFormat("#0." + repeat("0", amountOfSignifiantDigits - 2), decimalFormatSymbols);
			} else if (interimValue >= 1) {
				numberFormat = new DecimalFormat("#0." + repeat("0", amountOfSignifiantDigits - 1), decimalFormatSymbols);
			} else {
				numberFormat = new DecimalFormat("#0." + repeat("0", amountOfSignifiantDigits), decimalFormatSymbols);
			}
		} else {
			if (interimValue >= 1000) {
				numberFormat = new DecimalFormat("#0.0" + repeat("#", amountOfSignifiantDigits - 5), decimalFormatSymbols);
			} else if (interimValue >= 100) {
				numberFormat = new DecimalFormat("#0.0" + repeat("#", amountOfSignifiantDigits - 4), decimalFormatSymbols);
			} else if (interimValue >= 10) {
				numberFormat = new DecimalFormat("#0.0" + repeat("#", amountOfSignifiantDigits - 3), decimalFormatSymbols);
			} else if (interimValue >= 1) {
				numberFormat = new DecimalFormat("#0.0" + repeat("#", amountOfSignifiantDigits - 2), decimalFormatSymbols);
			} else {
				numberFormat = new DecimalFormat("#0.0" + repeat("#", amountOfSignifiantDigits - 1), decimalFormatSymbols);
			}
		}

		return numberFormat.format(interimValue) + unitExtension;
	}

	public static float parseHumanReadableNumber(String value, final String unitTypeSign, final boolean siUnits, final Locale locale) {
		if (isBlank(value)) {
			throw new RuntimeException("Not a numeric value");
		} else if (unitTypeSign != null && endsWithIgnoreCase(value, unitTypeSign)) {
			value = value.substring(0, value.length() - unitTypeSign.length()).trim();
		}

		final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);

		value = value.replace(Character.toString(decimalFormatSymbols.getGroupingSeparator()), "").replace(Character.toString(decimalFormatSymbols.getDecimalSeparator()), ".");

		long dimension = 1;
		if (endsWithIgnoreCase(value, "k")) {
			if (siUnits) {
				dimension = 1000;
			} else {
				dimension = 1024;
			}
			value = value.substring(0, value.length() - 1).trim();
		} else if (endsWithIgnoreCase(value, "M")) {
			if (siUnits) {
				dimension = (long) Math.pow(1000, 2);
			} else {
				dimension = (long) Math.pow(1024, 2);
			}
			value = value.substring(0, value.length() - 1).trim();
		} else if (endsWithIgnoreCase(value, "G")) {
			if (siUnits) {
				dimension = (long) Math.pow(1000, 3);
			} else {
				dimension = (long) Math.pow(1024, 3);
			}
			value = value.substring(0, value.length() - 1).trim();
		} else if (endsWithIgnoreCase(value, "T")) {
			if (siUnits) {
				dimension = (long) Math.pow(1000, 4);
			} else {
				dimension = (long) Math.pow(1024, 4);
			}
			value = value.substring(0, value.length() - 1).trim();
		} else if (endsWithIgnoreCase(value, "P")) {
			if (siUnits) {
				dimension = (long) Math.pow(1000, 5);
			} else {
				dimension = (long) Math.pow(1024, 5);
			}
			value = value.substring(0, value.length() - 1).trim();
		} else if (endsWithIgnoreCase(value, "E")) {
			if (siUnits) {
				dimension = (long) Math.pow(1000, 6);
			} else {
				dimension = (long) Math.pow(1024, 6);
			}
			value = value.substring(0, value.length() - 1).trim();
		}

		final float numericValue = Float.parseFloat(value) * dimension;
		return numericValue;
	}

	public static String getHumanReadableSpeed(final LocalDateTime startTime, final LocalDateTime endTime, final long itemsDone, final String unitTypeSign, final boolean siUnits, final Locale locale) {
		final long milliseconds = Duration.between(startTime, endTime).toMillis();
		if (milliseconds > 0) {
			// Floating point division, so fractions of items per second are kept
			final double itemsPerSecond = itemsDone * 1000.0 / milliseconds;

			final int unit = siUnits ? 1000 : 1024;
			double interimValue = itemsPerSecond;
			String unitExtension = "";
			int maxTrailingDigits = 0;
			if (interimValue > unit) {
				final int exponent = (int) (Math.log(interimValue) / Math.log(unit));
				unitExtension = " " + (siUnits ? "kMGTPE" : "KMGTPE").charAt(exponent - 1) + (siUnits ? "" : "i");
				interimValue = interimValue / Math.pow(unit, exponent);
				maxTrailingDigits = exponent * 3;
			}

			if (isNotBlank(unitTypeSign)) {
				unitExtension = unitExtension + unitTypeSign + "/s";
			} else {
				unitExtension = unitExtension + "/s";
			}

			final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);
			final DecimalFormat numberFormat;
			final int amountOfSignificantDigits = 5;

			int trailingDigits;
			if (interimValue >= 1000) {
				trailingDigits = amountOfSignificantDigits - 4;
			} else if (interimValue >= 100) {
				trailingDigits = amountOfSignificantDigits - 3;
			} else if (interimValue >= 10) {
				trailingDigits = amountOfSignificantDigits - 2;
			} else if (interimValue >= 1) {
				trailingDigits = amountOfSignificantDigits - 1;
			} else {
				trailingDigits = amountOfSignificantDigits;
			}

			// Without unit prefix maxTrailingDigits is 0, so the repeat count must not get negative
			numberFormat = new DecimalFormat("#0.0" + repeat("#", Math.max(0, Math.min(trailingDigits, maxTrailingDigits - 1))), decimalFormatSymbols);

			return numberFormat.format(interimValue) + unitExtension;
		} else {
			return "Done in <1 ms";
		}
	}

	/**
	 * Make an integer with unitsign human readable and keep all digits
	 *
	 * @param value
	 *            number to format
	 * @param unitTypeSign
	 *            unit sign appended after a blank, may be null
	 * @param locale
	 *            locale for the grouping separator
	 * @return formatted number with grouping separators and unit, e.g. "1,234,567 B"
	 */
	public static String getHumanReadableInteger(final Long value, final String unitTypeSign, final Locale locale) {
		final double interimValue = value.doubleValue();
		String unitExtension = "";
		if (isNotBlank(unitTypeSign)) {
			unitExtension = " " + unitTypeSign;
		}

		final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);
		final DecimalFormat numberFormat = new DecimalFormat("###,##0", decimalFormatSymbols);

		return numberFormat.format(interimValue) + unitExtension;
	}

	/**
	 * Generate MD5 from string data
	 *
	 * @param data
	 *            text to hash, encoded as UTF-8
	 * @return MD5 hash
	 * @throws Exception
	 *             if the hash algorithm is not available
	 */
	public static byte[] getMD5Hash(final String data) throws Exception {
		try {
			return MessageDigest.getInstance("MD5").digest(data.getBytes(StandardCharsets.UTF_8));
		} catch (final Exception e) {
			throw new Exception("Error while MD5 hashing", e);
		}
	}

	/**
	 * Generate SHA-1 from string data
	 *
	 * @param data
	 *            text to hash, encoded as UTF-8
	 * @return SHA-1 hash
	 * @throws Exception
	 *             if the hash algorithm is not available
	 */
	public static byte[] getSHA1Hash(final String data) throws Exception {
		try {
			return MessageDigest.getInstance("SHA-1").digest(data.getBytes(StandardCharsets.UTF_8));
		} catch (final Exception e) {
			throw new Exception("Error while SHA-1 hashing", e);
		}
	}

	/**
	 * Generate SHA-256 from string data
	 *
	 * @param data
	 *            text to hash, encoded as UTF-8
	 * @return SHA-256 hash
	 * @throws Exception
	 *             if the hash algorithm is not available
	 */
	public static byte[] getSHA256Hash(final String data) throws Exception {
		try {
			return MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));
		} catch (final Exception e) {
			throw new Exception("Error while SHA-256 hashing", e);
		}
	}

	/**
	 * Generate SHA-384 from string data
	 *
	 * @param data
	 *            text to hash, encoded as UTF-8
	 * @return SHA-384 hash
	 * @throws Exception
	 *             if the hash algorithm is not available
	 */
	public static byte[] getSHA384Hash(final String data) throws Exception {
		try {
			return MessageDigest.getInstance("SHA-384").digest(data.getBytes(StandardCharsets.UTF_8));
		} catch (final Exception e) {
			throw new Exception("Error while SHA-384 hashing", e);
		}
	}

	/**
	 * Generate SHA-512 from string data
	 *
	 * @param data
	 *            text to hash, encoded as UTF-8
	 * @return SHA-512 hash
	 * @throws Exception
	 *             if the hash algorithm is not available
	 */
	public static byte[] getSHA512Hash(final String data) throws Exception {
		try {
			return MessageDigest.getInstance("SHA-512").digest(data.getBytes(StandardCharsets.UTF_8));
		} catch (final Exception e) {
			throw new Exception("Error while SHA-512 hashing", e);
		}
	}

	/**
	 * Get bytearray for list of bytes
	 *
	 * @param data
	 *            list of bytes
	 * @return array with the bytes of the list
	 */
	public static byte[] getByteArray(final List<Byte> data) {
		final byte[] returnArray = new byte[data.size()];
		for (int i = 0; i < data.size(); i++) {
			returnArray[i] = data.get(i);
		}
		return returnArray;
	}

	/**
	 * Get stacktrace as string
	 *
	 * @param stackTrace
	 *            stacktrace elements, may be null
	 * @return stacktrace with one element per line, empty for null
	 */
	public static String stacktraceToString(final StackTraceElement[] stackTrace) {
		final StringBuilder returnBuilder = new StringBuilder();
		if (stackTrace != null) {
			for (final StackTraceElement stackTraceElement : stackTrace) {
				returnBuilder.append(stackTraceElement.toString());
				returnBuilder.append("\n");
			}
		}
		return returnBuilder.toString();
	}

	/**
	 * Download a file from url
	 *
	 * @param url
	 *            URL of the file
	 * @param localeDestionationPath
	 *            local file path to write to
	 * @param proxy
	 *            proxy to use, null for a direct connection
	 * @throws Exception
	 *             if the download or writing the file fails
	 */
	public static void downloadFile(final String url, final String localeDestionationPath, final Proxy proxy) throws Exception {
		final URLConnection urlConnection = URI.create(url).toURL().openConnection(proxy == null ? Proxy.NO_PROXY : proxy);
		try (BufferedInputStream bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
				FileOutputStream fileOutputStream = new FileOutputStream(localeDestionationPath)) {
			IoUtilities.copy(bufferedInputStream, fileOutputStream);
		} catch (final Exception e) {
			throw new Exception("Cannot download file", e);
		}
	}

	/**
	 * Download a file from url
	 *
	 * @param url
	 *            URL of the file
	 * @param localeDestionationPath
	 *            local file path to write to
	 * @throws Exception
	 *             if the download or writing the file fails
	 */
	public static void downloadFile(final String url, final String localeDestionationPath) throws Exception {
		try (BufferedInputStream bufferedInputStream = new BufferedInputStream(URI.create(url).toURL().openStream());
				FileOutputStream fileOutputStream = new FileOutputStream(localeDestionationPath)) {
			IoUtilities.copy(bufferedInputStream, fileOutputStream);
		} catch (final Exception e) {
			throw new Exception("Cannot download file", e);
		}
	}

	/**
	 * Check array for duplicate strings
	 *
	 * @param inputArray
	 *            array to check
	 * @param ignoreNullValues
	 *            do not count multiple null values as duplicates
	 * @return true if the array contains a duplicate
	 */
	public static boolean checkForDuplicates(final String[] inputArray, final boolean ignoreNullValues) {
		final Set<String> tempSet = new HashSet<>();
		for (final String stringItem : inputArray) {
			if (!ignoreNullValues || stringItem != null) {
				if (!tempSet.add(stringItem)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Filter all Objects of given class
	 *
	 * @param collection
	 *            items to filter
	 * @param classToSelect
	 *            class of the items to select
	 * @return list of all items that are instances of the given class
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> selectItems(final Collection<?> collection, final Class<T> classToSelect) {
		final List<T> list = new ArrayList<>();
		for (final Object item : collection) {
			if (classToSelect.isInstance(item)) {
				list.add((T) item);
			}
		}
		return list;
	}

	/**
	 * Filter all Objects of given class
	 *
	 * @param array
	 *            items to filter
	 * @param classToSelect
	 *            class of the items to select
	 * @return list of all items that are instances of the given class
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> selectItems(final Object[] array, final Class<T> classToSelect) {
		final List<T> list = new ArrayList<>();
		for (final Object item : array) {
			if (classToSelect.isInstance(item)) {
				list.add((T) item);
			}
		}
		return list;
	}

	public static <T> T[] revertArray(final T[] array) {
		@SuppressWarnings("unchecked")
		final T[] returnValue = (T[]) new Object[array.length];
		for (int i = 0; i < array.length; i++) {
			returnValue[i] = array[array.length - 1 - i];
		}
		return returnValue;
	}

	public static String getDomainFromUrl(final String url) throws Exception {
		final URI uri = new URI(url);
		final String domain = uri.getHost();
		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}

	public static boolean isEmpty(final String value) {
		return value == null || value.length() == 0;
	}

	public static boolean isNotEmpty(final String value) {
		return !isEmpty(value);
	}

	public static boolean isEmpty(final Collection<?> collection) {
		return collection == null || collection.isEmpty();
	}

	public static boolean isNotEmpty(final Collection<?> collection) {
		return !isEmpty(collection);
	}

	public static boolean isBlank(final String value) {
		return value == null || value.length() == 0 || value.trim().length() == 0;
	}

	public static boolean isNotBlank(final String value) {
		return !isBlank(value);
	}

	public static boolean isEmpty(final char[] value) {
		return value == null || value.length == 0;
	}

	public static void clear(final char[] array) {
		if (array != null) {
			for (int i = 0; i < array.length; i++) {
				array[i] = 0;
			}
		}
	}

	public static void clear(final byte[] array) {
		if (array != null) {
			Arrays.fill(array, (byte) 0);
		}
	}

	public static boolean isNotEmpty(final char[] value) {
		return !isEmpty(value);
	}

	public static boolean isBlank(final char[] value) {
		if (value == null || value.length == 0) {
			return true;
		} else {
			for (final char character : value) {
				if (!Character.isWhitespace(character)) {
					return false;
				}
			}
			return true;
		}
	}

	public static boolean isNotBlank(final char[] value) {
		return !isBlank(value);
	}

	public static void closeQuietly(final Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (@SuppressWarnings("unused") final IOException e) {
				// Do nothing
			}
		}
	}

	/**
	 * XMLStreamReader.close() doesn't close the underlying stream.
	 * So it must be closed separately.
	 *
	 * @param closeable
	 *            XMLStreamReader to close, may be null
	 * @param inputStream
	 *            underlying stream to close, may be null
	 */
	public static void closeQuietly(final XMLStreamReader closeable, final InputStream inputStream) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Do nothing
			}
		}
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Do nothing
			}
		}
	}

	public static void closeQuietly(XMLStreamWriter xmlWriter) {
		if (xmlWriter != null) {
			try {
				xmlWriter.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}
			xmlWriter = null;
		}
	}

	public static String repeat(final char valueChar, final int count) {
		return repeat(Character.toString(valueChar), count, null);
	}

	public static String repeat(final String value, final int count) {
		return repeat(value, count, null);
	}

	public static String repeat(final String value, final int count, final String separatorString) {
		if (value == null) {
			return null;
		} else if (value.length() == 0 || count == 0) {
			return "";
		} else {
			final StringBuilder returnValue = new StringBuilder();
			for (int i = 0; i < count; i++) {
				if (separatorString != null && returnValue.length() > 0) {
					returnValue.append(separatorString);
				}
				returnValue.append(value);
			}
			return returnValue.toString();
		}
	}

	public static String join(final Object[] array, String glue) {
		if (array == null) {
			return null;
		} else if (array.length == 0) {
			return "";
		} else {
			if (glue == null) {
				glue = "";
			}

			final StringBuilder returnValue = new StringBuilder();
			boolean isFirst = true;
			for (Object object : array) {
				if (!isFirst) {
					returnValue.append(glue);
				}
				if (object == null) {
					object = "";
				}
				returnValue.append(object.toString());
				isFirst = false;
			}
			return returnValue.toString();
		}
	}

	public static String join(final char[] array, String glue) {
		if (array == null) {
			return null;
		} else if (array.length == 0) {
			return "";
		} else {
			if (glue == null) {
				glue = "";
			}

			String returnValue = "";
			boolean isFirst = true;
			for (final char nextChar : array) {
				if (!isFirst) {
					returnValue += glue;
				}
				returnValue += nextChar;
				isFirst = false;
			}
			return returnValue;
		}
	}

	public static String join(final Iterable<?> iterableObject, String glue) {
		if (iterableObject == null) {
			return null;
		} else {
			if (glue == null) {
				glue = "";
			}

			final StringBuilder returnValue = new StringBuilder();
			boolean isFirst = true;
			for (Object object : iterableObject) {
				if (!isFirst) {
					returnValue.append(glue);
				}
				if (object == null) {
					object = "";
				}
				returnValue.append(object.toString());
				isFirst = false;
			}
			return returnValue.toString();
		}
	}

	public static <T> T[] remove(final T[] array, final T itemToRemove) {
		int indexToRemove = -1;
		if (array == null) {
			return null;
		} else if (itemToRemove == null) {
			for (int i = 0; i < array.length; i++) {
				if (array[i] == null) {
					indexToRemove = i;
					break;
				}
			}
		} else {
			for (int i = 0; i < array.length; i++) {
				if (itemToRemove.equals(array[i])) {
					indexToRemove = i;
					break;
				}
			}
		}

		if (indexToRemove >= 0) {
			@SuppressWarnings("unchecked")
			final T[] result = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length - 1);
			if (indexToRemove > 0) {
				System.arraycopy(array, 0, result, 0, indexToRemove);
			}
			if (indexToRemove < array.length - 1) {
				System.arraycopy(array, indexToRemove + 1, result, indexToRemove, array.length - indexToRemove - 1);
			}
			return result;
		} else {
			@SuppressWarnings("unchecked")
			final T[] result = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length);
			System.arraycopy(array, 0, result, 0, array.length);
			return result;
		}
	}

	public static <T> T[] removeItemAtIndex(final T[] array, final int itemIndexToRemove) {
		@SuppressWarnings("unchecked")
		final T[] result = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length - 1);
		for (int i = 0; i < array.length; i++) {
			if (i < itemIndexToRemove) {
				result[i] = array[i];
			} else if (i > itemIndexToRemove) {
				result[i - 1] = array[i];
			}
		}
		return result;
	}

	public static byte[] readFileToByteArray(final File file) throws FileNotFoundException, IOException {
		try (FileInputStream in = new FileInputStream(file)) {
			final byte[] returnArray = new byte[(int) file.length()];
			in.read(returnArray);
			return returnArray;
		}
	}

	/**
	 * Prepend blanks at the left of a string to make it fit the given minimum length
	 *
	 * @param value
	 *            string to pad
	 * @param minimumLength
	 *            minimum length of the result
	 * @return padded string, unchanged if it is already long enough
	 */
	public static String leftPad(final String value, final int minimumLength) {
		try {
			return String.format("%1$" + minimumLength + "s", value);
		} catch (@SuppressWarnings("unused") final Exception e) {
			return value;
		}
	}

	public static String leftPad(final String value, final int size, final char padChar) {
		if (value == null) {
			return null;
		} else {
			final int padsize = size - value.length();
			if (padsize <= 0) {
				return value;
			} else {
				return repeat(padChar, padsize).concat(value);
			}
		}
	}

	/**
	 * Append blanks at the right of a string to make it fit the given minimum length
	 *
	 * @param value
	 *            string to pad
	 * @param minimumLength
	 *            minimum length of the result
	 * @return padded string, unchanged if it is already long enough
	 */
	public static String rightPad(final String value, final int minimumLength) {
		try {
			return String.format("%1$-" + minimumLength + "s", value);
		} catch (@SuppressWarnings("unused") final Exception e) {
			return value;
		}
	}

	/**
	 * Only trim the value when the surrounding occurs on both ends
	 *
	 * @param value
	 *            string to trim
	 * @param sourrounding
	 *            text to remove at the start and the end
	 * @return trimmed string, or the unchanged value if it does not start and end with the surrounding
	 */
	public static String trimSimultaneously(final String value, final String sourrounding) {
		if (value == null) {
			return null;
		} else if (isEmpty(sourrounding)) {
			return value;
		} else if (value.startsWith(sourrounding) && value.endsWith(sourrounding)) {
			return value.substring(sourrounding.length(), value.length() - sourrounding.length());
		} else {
			return value;
		}
	}

	public static String trim(final String value) {
		if (value == null) {
			return null;
		} else {
			return value.trim();
		}
	}

	public static String trim(String value, final char trimChar) {
		while (value != null && value.startsWith(Character.toString(trimChar))) {
			value = value.substring(1);
		}

		while (value != null && value.endsWith(Character.toString(trimChar))) {
			value = value.substring(0, value.length() - 1);
		}

		return value;
	}

	public static String toString(final InputStream inputStream, final Charset encoding) throws IOException {
		return new String(IoUtilities.toByteArray(inputStream), encoding);
	}

	public static Object toString(final Reader characterStream) throws IOException {
		final StringBuilder returnValue = new StringBuilder();
		int characterInt;
		while ((characterInt = characterStream.read()) > -1) {
			returnValue.append((char) characterInt);
		}
		return returnValue.toString();
	}

	public static List<String> readLines(final InputStream inStream, final Charset encoding) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, encoding))) {
			final List<String> lines = new ArrayList<>();
			String nextLine;
			while ((nextLine = reader.readLine()) != null) {
				lines.add(nextLine);
			}
			return lines;
		}
	}

	public static String trimLeft(final String value) {
		return value.replaceAll("^\\s+", "");
	}

	public static String trimRight(final String value) {
		return value.replaceAll("\\s+$", "");
	}

	public static void addFileToClasspath(final String filePath) throws IOException {
		try {
			final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			method.invoke(ClassLoader.getSystemClassLoader(), new Object[] { URI.create(new File(filePath).toURI().toString()).toURL() });
		} catch (final Throwable t) {
			throw new IOException("Error, could not add file to system classloader: " + t.getMessage(), t);
		}
	}

	public static String shortenStringToMaxLengthCutRight(final String value, final int maxLength, final String cutSign) {
		if (value != null && value.length() > maxLength) {
			return value.substring(0, maxLength - 4) + cutSign;
		} else {
			return value;
		}
	}

	public static String shortenStringToMaxLengthCutRight(final String value, final int maxLength) {
		return shortenStringToMaxLengthCutRight(value, maxLength, " ...");
	}

	public static String shortenStringToMaxLengthCutMiddle(final String value, final int maxLength) {
		if (value != null && value.length() > maxLength) {
			final int leftLength = (maxLength - 5) / 2;
			return value.substring(0, leftLength) + " ... " + value.substring(value.length() - ((maxLength - leftLength) - 5));
		} else {
			return value;
		}
	}

	public static String shortenStringToMaxLengthCutLeft(final String value, final int maxLength) {
		if (value != null && value.length() > maxLength) {
			return "... " + value.substring((value.length() - maxLength) + 4);
		} else {
			return value;
		}
	}

	public static List<String> splitAndTrimList(final String stringList) {
		if (stringList == null) {
			return null;
		} else {
			final List<String> returnList = new ArrayList<>();
			final String[] parts = stringList.split(",|;|\\|| |\\n|\\r|\\t");
			for (final String part : parts) {
				if (isNotEmpty(part)) {
					returnList.add(part.trim());
				}
			}
			return returnList;
		}
	}

	public static List<String> splitAndTrimList(final String stringList, final Character... separatorChars) {
		if (stringList == null) {
			return null;
		} else {
			final List<String> returnList = new ArrayList<>();
			final String[] parts = stringList.split(join(separatorChars, "|").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r"));
			for (final String part : parts) {
				if (isNotEmpty(part)) {
					returnList.add(part.trim());
				}
			}
			return returnList;
		}
	}

	public static boolean endsWithIgnoreCase(final String data, final String suffix) {
		if (data == suffix) {
			// both null or same object
			return true;
		} else if (data == null) {
			// data is null but suffix is not
			return false;
		} else if (suffix == null) {
			// suffix is null but data is not
			return true;
		} else if (data.toLowerCase().endsWith(suffix.toLowerCase())) {
			// both are set, so ignore the case for standard endsWith-method
			return true;
		} else {
			// anything else
			return false;
		}
	}

	public static int indexOfIgnoreCase(final String data, final String part) {
		if (data == part) {
			// both null or same object
			return 0;
		} else if (data == null || part == null) {
			// suffix is null but data is not or vice versa
			return -1;
		} else {
			// anything else
			return data.toLowerCase().indexOf(part.toLowerCase());
		}
	}

	public static List<String> splitAndTrimListQuoted(final String stringList, final char... separatorChars) {
		final List<String> returnList = new ArrayList<>();
		StringBuilder nextLine = new StringBuilder();
		boolean quotedBySingleQoute = false;
		boolean quotedByDoubleQoute = false;
		for (final char nextChar : stringList.toCharArray()) {
			if ('\'' == nextChar) {
				if (!quotedBySingleQoute && !quotedByDoubleQoute) {
					quotedBySingleQoute = true;
				} else if (quotedBySingleQoute) {
					quotedBySingleQoute = false;
				}
			} else if ('"' == nextChar) {
				if (!quotedBySingleQoute && !quotedByDoubleQoute) {
					quotedByDoubleQoute = true;
				} else if (quotedByDoubleQoute) {
					quotedByDoubleQoute = false;
				}
			}

			boolean splitFound = false;
			for (final char separatorChar : separatorChars) {
				if (separatorChar == nextChar && !quotedBySingleQoute && !quotedByDoubleQoute) {
					final String line = nextLine.toString().trim();
					if (line.length() > 0) {
						returnList.add(line);
						splitFound = true;
					}
					nextLine = new StringBuilder();
					break;
				}
			}

			if (!splitFound) {
				nextLine.append(nextChar);
			}
		}
		final String line = nextLine.toString().trim();
		if (line.length() > 0) {
			returnList.add(line);
		}
		return returnList;
	}

	public static boolean containsIgnoreCase(final Collection<String> list, final String item) {
		if (list == null) {
			return false;
		} else {
			for (final String listItem : list) {
				if (listItem == item || (listItem != null && listItem.equalsIgnoreCase(item))) {
					return true;
				}
			}
			return false;
		}
	}

	public static boolean delete(final File file) {
		if (file.isDirectory()) {
			for (final File subFile : file.listFiles()) {
				if (!delete(subFile)) {
					return false;
				}
			}
		}
		return file.delete();
	}

	public static int limitValue(final int minimum, final int value, final int maximum) {
		if (value < minimum) {
			return minimum;
		} else if (maximum < value) {
			return maximum;
		} else {
			return value;
		}
	}

	public static boolean startsWithCaseinsensitive(final String data, final String prefix) {
		if (data == null || prefix == null) {
			return false;
		} else {
			return data.toLowerCase().startsWith(prefix.toLowerCase());
		}
	}

	public static Map<String, String> createMap(final String... data) throws Exception {
		final Map<String, String> returnMap = new HashMap<>();
		if (data != null && data.length > 0) {
			if (data.length % 2 != 0) {
				throw new Exception("Invalid map data: odd number of parameters, must be even");
			} else {
				for (int i = 0; i < data.length / 2; i++) {
					final String key = data[i * 2];
					final String value = data[i * 2 + 1];
					returnMap.put(key, value);
				}
			}
		}
		return returnMap;
	}

	/**
	 * Check whether an iterable collection contains a special object. Items are compared by identity (==), not by equals().
	 *
	 * @param hayshack
	 *            items to search in
	 * @param needle
	 *            object to search for
	 * @return true if the object is contained
	 */
	public static boolean containsObject(final Iterable<?> hayshack, final Object needle) {
		for (final Object item : hayshack) {
			if (item == needle) {
				return true;
			}
		}
		return false;
	}

	public static String replaceUsersHome(final String filePath) {
		if (filePath == null) {
			return filePath;
		}
		final String homePath = System.getProperty("user.home");
		return filePath
				.replace("~", homePath)
				.replace("$HOME", homePath)
				.replace("${HOME}", homePath);
	}

	public static String replaceUsersHomeByTilde(final String filePath) {
		if (filePath == null) {
			return filePath;
		}
		final String homePath = System.getProperty("user.home");
		return filePath.replace(homePath, "~");
	}

	public static String getUsersDefaultDownloadDirectory() {
		final String userHome = System.getProperty("user.home");

		final File userDirsFile = new File(userHome, ".config/user-dirs.dirs");
		if (userDirsFile.isFile()) {
			try {
				for (final String line : Files.readAllLines(userDirsFile.toPath())) {
					final String trimmedLine = line.trim();
					if (trimmedLine.startsWith("XDG_DOWNLOAD_DIR=")) {
						String value = trimmedLine.substring("XDG_DOWNLOAD_DIR=".length()).trim();
						if (value.startsWith("\"") && value.endsWith("\"")) {
							value = value.substring(1, value.length() - 1);
						}
						value = value.replace("$HOME", userHome);
						if (Utilities.isNotBlank(value)) {
							return value;
						}
					}
				}
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Use fall back
			}
		}

		return new File(userHome, "Downloads").getAbsolutePath();
	}

	public static String substring(final String text, final int startIndex) {
		if (text == null) {
			return null;
		} else {
			if (text.length() < startIndex) {
				return "";
			} else {
				return text.substring(startIndex);
			}
		}
	}

	public static String substring(final String text, final int startIndex, final int endIndex) {
		if (text == null) {
			return null;
		} else {
			if (text.length() < startIndex) {
				return "";
			} else if (text.length() < endIndex) {
				return text.substring(startIndex);
			} else {
				return text.substring(startIndex, endIndex);
			}
		}
	}

	public static boolean contains(final String text, final String searchText) {
		if (text == null) {
			return false;
		} else {
			return text.contains(searchText);
		}
	}

	public static long skipInputStreamData(final InputStream inputStream, final long bytesToSkip) throws Exception {
		long bytesSkipped = 0;
		while (bytesSkipped < bytesToSkip) {
			final long newBytesSkipped = inputStream.skip(bytesToSkip - bytesSkipped);
			if (newBytesSkipped == 0) {
				throw new Exception("Cannot skip data while reading stream");
			}
			bytesSkipped += newBytesSkipped;
		}
		return bytesSkipped;
	}

	public static byte[] convertIntToByteArray(final int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

	public static int convertByteArrayToInt(final byte[] bytes) throws Exception {
		if (bytes.length > 4) {
			throw new Exception("ByteArray is too big for int value");
		} else {
			return ByteBuffer.wrap(bytes).getInt();
		}
	}

	public static String[] split(final String text, final char delimiterChar, final char escapeChar, final int limit) {
		final String escapeCharString = Character.toString(escapeChar);
		final String delimiterCharString = Character.toString(delimiterChar);
		final String splitRegex = "(?<!" + Pattern.quote(escapeCharString) + ")" + Pattern.quote(delimiterCharString);
		final String[] returnParts = text.split(splitRegex, limit);
		for (int i = 0; i < returnParts.length; i++) {
			returnParts[i] = returnParts[i].replace(escapeCharString + delimiterCharString, escapeCharString);
		}
		return returnParts;
	}

	public static String[] split(final String text, final char delimiterChar, final char escapeChar) {
		final String escapeCharString = Character.toString(escapeChar);
		final String delimiterCharString = Character.toString(delimiterChar);
		final String splitRegex = "(?<!" + Pattern.quote(escapeCharString) + ")" + Pattern.quote(delimiterCharString);
		final String[] returnParts = text.split(splitRegex);
		for (int i = 0; i < returnParts.length; i++) {
			returnParts[i] = returnParts[i].replace(escapeCharString + delimiterCharString, escapeCharString);
		}
		return returnParts;
	}

	public static List<String> parseArguments(final String argumentLine) throws Exception {
		final List<String> returnList = new ArrayList<>();
		StringBuilder currentArgument = new StringBuilder();
		Character enclosingChar = null;
		boolean escapedNextChar = false;
		for (final char nextChar : argumentLine.replace("\r\n", "\n").replace("\r", "\n").toCharArray()) {
			if (enclosingChar != null) {
				if (escapedNextChar) {
					if (nextChar != enclosingChar) {
						currentArgument.append("\\");
					}
					currentArgument.append(nextChar);
					escapedNextChar = false;
				} else if (nextChar == '\\') {
					escapedNextChar = true;
				} else if (nextChar == enclosingChar) {
					returnList.add(currentArgument.toString());
					enclosingChar = null;
					currentArgument = new StringBuilder();
				} else {
					currentArgument.append(nextChar);
				}
			} else {
				if (nextChar == '\'' || nextChar == '"') {
					if (currentArgument.length() > 0) {
						returnList.add(currentArgument.toString());
						currentArgument = new StringBuilder();
					}
					enclosingChar = nextChar;
				} else if (nextChar == ' ' || nextChar == '\n' || nextChar == '\t') {
					if (currentArgument.length() > 0) {
						returnList.add(currentArgument.toString());
						currentArgument = new StringBuilder();
					}
				} else {
					currentArgument.append(nextChar);
				}
			}
		}
		if (enclosingChar != null) {
			throw new Exception("Invalid quotation. Missing closing " + enclosingChar);
		} else if (currentArgument.length() > 0) {
			returnList.add(currentArgument.toString());
			currentArgument = new StringBuilder();
		}
		return returnList;
	}

	public static List<String> parseTokens(final String line, final char... delimiters) throws Exception {
		final List<String> returnList = new ArrayList<>();
		StringBuilder currentArgument = new StringBuilder();
		Character enclosingChar = null;
		boolean escapedNextChar = false;
		for (final char nextChar : line.replace("\r\n", "\n").replace("\r", "\n").toCharArray()) {
			if (enclosingChar != null) {
				if (escapedNextChar) {
					if (nextChar != enclosingChar) {
						currentArgument.append("\\");
					}
					currentArgument.append(nextChar);
					escapedNextChar = false;
				} else if (nextChar == '\\') {
					escapedNextChar = true;
				} else if (nextChar == enclosingChar) {
					returnList.add(currentArgument.toString());
					enclosingChar = null;
					currentArgument = new StringBuilder();
				} else {
					currentArgument.append(nextChar);
				}
			} else {
				if (nextChar == '\'' || nextChar == '"') {
					if (currentArgument.length() > 0) {
						returnList.add(currentArgument.toString());
						currentArgument = new StringBuilder();
					}
					enclosingChar = nextChar;
				} else if (equalsAnyChar(nextChar, delimiters)) {
					if (currentArgument.length() > 0) {
						returnList.add(currentArgument.toString());
						currentArgument = new StringBuilder();
					}
				} else {
					currentArgument.append(nextChar);
				}
			}
		}
		if (enclosingChar != null) {
			throw new Exception("Invalid quotation. Missing closing " + enclosingChar);
		} else if (currentArgument.length() > 0) {
			returnList.add(currentArgument.toString());
			currentArgument = new StringBuilder();
		}
		return returnList;
	}

	public static boolean equalsAnyChar(final char checkChar, final char... compareChars) {
		for (final char compareChar : compareChars) {
			if (checkChar == compareChar) {
				return true;
			}
		}
		return false;
	}

	public static List<String> getList(final String... items) {
		final List<String> returnList = new ArrayList<>();
		for (final String item : items) {
			returnList.add(item);
		}
		return returnList;
	}

	public static String getSystemTempDir() {
		return System.getProperty("java.io.tmpdir");
	}

	public static <T> T[] reverseArray(final T[] arrayData) {
		for (int i = 0; i < arrayData.length / 2; i++) {
			final int j = arrayData.length - 1 - i;
			final T tmp = arrayData[i];
			arrayData[i] = arrayData[j];
			arrayData[j] = tmp;
		}
		return arrayData;
	}

	public static int[] reverseArray(final int[] arrayData) {
		for (int i = 0; i < arrayData.length / 2; i++) {
			final int j = arrayData.length - 1 - i;
			final int tmp = arrayData[i];
			arrayData[i] = arrayData[j];
			arrayData[j] = tmp;
		}
		return arrayData;
	}

	public static byte[] reverseArray(final byte[] arrayData) {
		for (int i = 0; i < arrayData.length / 2; i++) {
			final int j = arrayData.length - 1 - i;
			final byte tmp = arrayData[i];
			arrayData[i] = arrayData[j];
			arrayData[j] = tmp;
		}
		return arrayData;
	}

	/**
	 * KeyStores saved with blank password should also contain certificates when opened with a null password.
	 * Special cacerts keystore of JDKs behave this way, because they are JKS keystores, but PKCS12 keystores do not behave this way.
	 * Those are fixed by this keystore copy job to also behave in the intended way to show their certificates when openend with null password.
	 *
	 * @param keyStoreFileWithNullPassword
	 *            PKCS12 keystore file with blank password, replaced by a JKS keystore; files with another password stay unchanged
	 * @throws Exception
	 *             if the keystore cannot be converted or written
	 */
	public static void convertPkcs12TrustStoreToJKS(final File keyStoreFileWithNullPassword) throws Exception {
		final KeyStore readKeyStore = KeyStore.getInstance("PKCS12");
		try (FileInputStream inputStream = new FileInputStream(keyStoreFileWithNullPassword)) {
			readKeyStore.load(inputStream, "".toCharArray());
		} catch (@SuppressWarnings("unused") final Exception e) {
			// KeyStore password is not blank, so do not fix anything
			return;
		}

		final KeyStore writeKeyStore = KeyStore.getInstance("JKS");
		writeKeyStore.load(null, null);

		// KeyStore password is null
		for (final String alias : Collections.list(readKeyStore.aliases())) {
			writeKeyStore.setCertificateEntry(alias, readKeyStore.getCertificate(alias));
		}
		keyStoreFileWithNullPassword.delete();
		try (FileOutputStream outputStream = new FileOutputStream(keyStoreFileWithNullPassword)) {
			writeKeyStore.store(outputStream, "".toCharArray());
		}
	}

	/**
	 * Fix the encoding of a String if it was stored in UTF-8 encoding but decoded with ISO-8859-1 encoding
	 *
	 * <p>Examples of byte data of wrongly encoded Umlauts and other special characters:</p>
	 * <pre>
	 * Ä: [-61, -124]
	 * ä: [-61, -92]
	 * ß: [-61, -97]
	 * è: [-61, -88]
	 * </pre>
	 *
	 * @param comment
	 *            text to fix
	 * @return text decoded as UTF-8 if a wrong encoding was detected, otherwise the unchanged text
	 */
	public static String fixStringEncodingIfNeeded(final String comment) {
		boolean wrongEncodingDetected = false;
		for (final byte nextByte : comment.getBytes(StandardCharsets.ISO_8859_1)) {
			if (nextByte == -61) {
				wrongEncodingDetected = true;
				break;
			}
		}
		if (wrongEncodingDetected) {
			return new String(comment.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
		} else {
			return comment;
		}
	}

	public static String joinNotBlank(final List<String> listOfStrings, final String glue) {
		return join(listOfStrings.stream().filter(x -> isNotBlank(x)).collect(Collectors.toList()), glue);
	}

	public static String upperCase(final String value) {
		if (value == null) {
			return null;
		} else {
			return value.toUpperCase();
		}
	}

	public static <T> boolean arrayContains(final T[] dataArray, final T[] searchArray) {
		if (dataArray == null || searchArray == null || dataArray.length < searchArray.length) {
			return false;
		} else if (searchArray.length == 0) {
			return true;
		} else {
			// Only start positions where the whole searchArray still fits into dataArray
			for (int i = 0; i <= dataArray.length - searchArray.length; i++) {
				int matchingItems = 0;
				while (matchingItems < searchArray.length && Objects.equals(dataArray[i + matchingItems], searchArray[matchingItems])) {
					matchingItems++;
				}
				if (matchingItems == searchArray.length) {
					return true;
				}
			}
			return false;
		}
	}

	public static boolean arrayContains(final byte[] dataArray, final byte[] searchArray) {
		if (dataArray == null || searchArray == null || dataArray.length < searchArray.length) {
			return false;
		} else if (searchArray.length == 0) {
			return true;
		} else {
			// Only start positions where the whole searchArray still fits into dataArray
			for (int i = 0; i <= dataArray.length - searchArray.length; i++) {
				int matchingItems = 0;
				while (matchingItems < searchArray.length && dataArray[i + matchingItems] == searchArray[matchingItems]) {
					matchingItems++;
				}
				if (matchingItems == searchArray.length) {
					return true;
				}
			}
			return false;
		}
	}

	public static String breakTextToMaximumLinelength(final String text, final int maximumLinelength, Linebreak linebreakType) {
		if (linebreakType == null) {
			linebreakType = Linebreak.Unix;
		}

		if (text == null) {
			return text;
		} else {
			String returnValue = "";
			int currentLineLength = 0;
			for (final char nextChar : text.toCharArray()) {
				if (currentLineLength > maximumLinelength) {
					returnValue += linebreakType.toString();
					currentLineLength = 0;
				}
				if (nextChar == '\r' || nextChar == '\n') {
					currentLineLength = 0;
				}
				returnValue += nextChar;
			}

			return returnValue;
		}
	}

	public static String escapeJavaString(final String text) {
		final StringBuilder escapedTextBuilder = new StringBuilder();

		for (final char nextChar : text.toCharArray()) {
			switch (nextChar) {
				case '\\':
					escapedTextBuilder.append("\\\\");
					break;
				case '\"':
					escapedTextBuilder.append("\\\"");
					break;
				case '\n':
					escapedTextBuilder.append("\\n");
					break;
				case '\r':
					escapedTextBuilder.append("\\r");
					break;
				case '\t':
					escapedTextBuilder.append("\\t");
					break;
				case '\b':
					escapedTextBuilder.append("\\b");
					break;
				case '\f':
					escapedTextBuilder.append("\\f");
					break;
				default:
					if (nextChar < 32 || nextChar > 126) {
						escapedTextBuilder.append(String.format("\\u%04X", (int) nextChar));
					} else {
						escapedTextBuilder.append(nextChar);
					}
			}
		}

		return escapedTextBuilder.toString();
	}

	public static String unescapeJavaString(final String javaEscapedText) throws Exception {
		final StringBuilder unescapedTextBuilder = new StringBuilder();
		final int length = javaEscapedText.length();

		for (int i = 0; i < length; i++) {
			final char nextChar = javaEscapedText.charAt(i);

			if (nextChar == '\\' && i + 1 < length) {
				final char oneMoreChar = javaEscapedText.charAt(i + 1);
				switch (oneMoreChar) {
					case 'n':
						unescapedTextBuilder.append('\n');
						i++;
						break;
					case 'r':
						unescapedTextBuilder.append('\r');
						i++;
						break;
					case 't':
						unescapedTextBuilder.append('\t');
						i++;
						break;
					case 'b':
						unescapedTextBuilder.append('\b');
						i++;
						break;
					case 'f':
						unescapedTextBuilder.append('\f');
						i++;
						break;
					case '\\':
						unescapedTextBuilder.append('\\');
						i++;
						break;
					case '\'':
						unescapedTextBuilder.append('\'');
						i++;
						break;
					case '\"':
						unescapedTextBuilder.append('\"');
						i++;
						break;
					case 'x': // hexadecimal escapes: 8-bit size
						if (i + 3 < length) {
							final String hex = javaEscapedText.substring(i + 2, i + 4);
							try {
								final int code = Integer.parseInt(hex, 16);
								unescapedTextBuilder.append((char) code);
								i += 3;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid hex sequence at character index " + i + " ('" + "\\x" + hex + "')", e);
							}
						} else {
							final String invalidHex = javaEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\x" + invalidHex + "')");
						}
						break;
					case 'u': // Java escapes: 16-bit size
						if (i + 5 < length) {
							final String hex = javaEscapedText.substring(i + 2, i + 6);
							try {
								final int code = Integer.parseInt(hex, 16);
								unescapedTextBuilder.append((char) code);
								i += 5;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\u" + hex + "')", e);
							}
						} else {
							final String invalidHex = javaEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\u" + invalidHex + "')");
						}
						break;
					case 'U': // Unicode escapes: 32-bit size
						if (i + 9 < length) {
							final String hex = javaEscapedText.substring(i + 2, i + 10);
							try {
								final int code = Integer.parseInt(hex, 32);
								unescapedTextBuilder.append((char) code);
								i += 9;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\U" + hex + "')", e);
							}
						} else {
							final String invalidHex = javaEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\U" + invalidHex + "')");
						}
						break;
					default:
						throw new Exception("Invalid escape sequence at character index " + i + " ('" + "\\" + oneMoreChar + "')");
				}
			} else {
				unescapedTextBuilder.append(nextChar);
			}
		}

		return unescapedTextBuilder.toString();
	}

	/**
	 * Escape a text for use as a value in a .properties file.
	 * Only escapes that are valid for java.util.Properties are used.
	 * Double quotes have no special meaning in properties files and are not escaped.
	 *
	 * @param text
	 *            text to escape
	 * @return escaped text, non-ASCII and control characters as unicode escapes
	 */
	public static String escapePropertiesString(final String text) {
		final StringBuilder escapedTextBuilder = new StringBuilder();

		for (final char nextChar : text.toCharArray()) {
			switch (nextChar) {
				case '\\':
					escapedTextBuilder.append("\\\\");
					break;
				case '\n':
					escapedTextBuilder.append("\\n");
					break;
				case '\r':
					escapedTextBuilder.append("\\r");
					break;
				case '\t':
					escapedTextBuilder.append("\\t");
					break;
				case '\f':
					escapedTextBuilder.append("\\f");
					break;
				default:
					// "\b" is not a valid properties escape, so backspace and other control chars become unicode escapes
					if (nextChar < 32 || nextChar > 126) {
						escapedTextBuilder.append(String.format("\\u%04X", (int) nextChar));
					} else {
						escapedTextBuilder.append(nextChar);
					}
			}
		}

		return escapedTextBuilder.toString();
	}

	public static String unescapePropertiesString(final String propertiesEscapedText) throws Exception {
		final StringBuilder unescapedTextBuilder = new StringBuilder();
		final int length = propertiesEscapedText.length();

		for (int i = 0; i < length; i++) {
			final char nextChar = propertiesEscapedText.charAt(i);

			if (nextChar == '\\' && i + 1 < length) {
				final char oneMoreChar = propertiesEscapedText.charAt(i + 1);
				switch (oneMoreChar) {
					case 'n':
						unescapedTextBuilder.append('\n');
						i++;
						break;
					case 'r':
						unescapedTextBuilder.append('\r');
						i++;
						break;
					case 't':
						unescapedTextBuilder.append('\t');
						i++;
						break;
					case 'b':
						unescapedTextBuilder.append('\b');
						i++;
						break;
					case 'f':
						unescapedTextBuilder.append('\f');
						i++;
						break;
					case ' ':
						unescapedTextBuilder.append(' ');
						i++;
						break;
					case '\\':
						unescapedTextBuilder.append('\\');
						i++;
						break;
					case '\'':
						unescapedTextBuilder.append('\'');
						i++;
						break;
					case '\"':
						unescapedTextBuilder.append('\"');
						i++;
						break;
					case 'x': // hexadecimal escapes: 8-bit size
						if (i + 3 < length) {
							final String hex = propertiesEscapedText.substring(i + 2, i + 4);
							try {
								final int code = Integer.parseInt(hex, 16);
								unescapedTextBuilder.append((char) code);
								i += 3;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid hex sequence at character index " + i + " ('" + "\\x" + hex + "')", e);
							}
						} else {
							final String invalidHex = propertiesEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\x" + invalidHex + "')");
						}
						break;
					case 'u': // Java escapes: 16-bit size
						if (i + 5 < length) {
							final String hex = propertiesEscapedText.substring(i + 2, i + 6);
							try {
								final int code = Integer.parseInt(hex, 16);
								unescapedTextBuilder.append((char) code);
								i += 5;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\u" + hex + "')", e);
							}
						} else {
							final String invalidHex = propertiesEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\u" + invalidHex + "')");
						}
						break;
					case 'U': // Unicode escapes: 32-bit size
						if (i + 9 < length) {
							final String hex = propertiesEscapedText.substring(i + 2, i + 10);
							try {
								final int codePoint = Integer.parseInt(hex, 16);
								if (!Character.isValidCodePoint(codePoint)) {
									throw new Exception("Invalid unicode code point at character index " + i + " ('" + "\\U" + hex + "')");
								}
								unescapedTextBuilder.appendCodePoint(codePoint);
								i += 9;
							} catch (final NumberFormatException e) {
								throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\U" + hex + "')", e);
							}
						} else {
							final String invalidHex = propertiesEscapedText.substring(i + 2);
							throw new Exception("Invalid unicode sequence at character index " + i + " ('" + "\\U" + invalidHex + "')");
						}
						break;
					default:
						unescapedTextBuilder.append(oneMoreChar);
						i++;
						break;
				}
			} else {
				unescapedTextBuilder.append(nextChar);
			}
		}

		return unescapedTextBuilder.toString();
	}

	public static String getBoxedString(final String text, final char boxCharacter) {
		String returnString = "";
		final String boxCharacterString = Character.toString(boxCharacter);
		returnString += boxCharacterString.repeat(2) + boxCharacterString.repeat(text.length()) + boxCharacterString.repeat(2) + "\n";
		returnString += boxCharacterString + " " + text + " " + boxCharacterString + "\n";
		returnString += boxCharacterString.repeat(2) + boxCharacterString.repeat(text.length()) + boxCharacterString.repeat(2);
		return returnString;
	}

	public static <T> List<T> getListForIterator(final Iterator<T> iterator) {
		final List<T> list = new ArrayList<>();
		iterator.forEachRemaining(list::add);
		return list;
	}

	public static <T> List<T> getListForEnumeration(final Enumeration<T> enumeration) {
		return Collections.list(enumeration);
	}
}
