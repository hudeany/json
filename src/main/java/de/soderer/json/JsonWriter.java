package de.soderer.json;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Stack;

import de.soderer.json.exception.JsonWriterStateException;
import de.soderer.json.utilities.DateUtilities;
import de.soderer.json.utilities.Utilities;

/**
 * Writer for JSON data to an output stream.
 * <p>
 * Complete JSON items can be written by {@link #add(JsonNode)}. Large data can be written step by
 * step: open objects and arrays, open object properties and add simple values, then close them
 * again in reverse order. Methods called in a wrong order throw a
 * {@link JsonWriterStateException}.
 * </p>
 * <p>
 * The output is formatted with line breaks, tab indentation and a blank after property colons by
 * default; {@link #setUglify(boolean)} switches to compact output.
 * </p>
 */
public class JsonWriter implements Closeable {
	/** Default output encoding. */
	public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	/** Output stream. */
	private OutputStream outputStream;

	/** Output encoding. */
	private final Charset encoding;

	/** Output writer. */
	private BufferedWriter outputWriter = null;

	/**
	 * Number of characters written so far.
	 */
	private long writtenCharacters = 0;

	/**
	 * Stack of the currently open items.
	 */
	private final Stack<JsonStackItem> openJsonStackItems = new Stack<>();

	/**
	 * Line break between items.
	 */
	private String linebreak = "\n";
	/**
	 * Indentation per nesting level.
	 */
	private String indentation = "\t";
	/**
	 * Separator after the colon of a property.
	 */
	private String separator = " ";

	/**
	 * Open items while writing.
	 */
	private enum JsonStackItem {
		Array_Empty,
		Array,
		Object_Empty,
		Object,
		Object_Value
	}

	/**
	 * Creates a new JSON writer using UTF-8 encoding.
	 *
	 * @param outputStream
	 *            the stream to write to
	 */
	public JsonWriter(final OutputStream outputStream) {
		this(outputStream, null);
	}

	/**
	 * Creates a new JSON writer.
	 *
	 * @param outputStream
	 *            the stream to write to
	 * @param encoding
	 *            the encoding of the output, or null for UTF-8
	 */
	public JsonWriter(final OutputStream outputStream, final Charset encoding) {
		this.outputStream = outputStream;
		this.encoding = encoding == null ? DEFAULT_ENCODING : encoding;
	}

	/**
	 * Sets the indentation per nesting level.
	 *
	 * @param indentation
	 *            the indentation, e.g. a tab or some blanks, null for none
	 */
	public void setIndentation(final String indentation) {
		if (indentation == null) {
			this.indentation = "";
		} else {
			this.indentation = indentation;
		}
	}

	/**
	 * Sets the indentation per nesting level.
	 *
	 * @param newIndentation
	 *            the indentation, null for none
	 * @return this writer for chaining
	 */
	public JsonWriter withIndentation(final String newIndentation) {
		setIndentation(newIndentation);
		return this;
	}

	/**
	 * Sets a single character as indentation per nesting level.
	 *
	 * @param indentationCharacter
	 *            the indentation character
	 */
	public void setIndentation(final char indentationCharacter) {
		indentation = Character.toString(indentationCharacter);
	}

	/**
	 * Sets a single character as indentation per nesting level.
	 *
	 * @param newIndentationCharacter
	 *            the indentation character
	 * @return this writer for chaining
	 */
	public JsonWriter withIndentation(final char newIndentationCharacter) {
		setIndentation(newIndentationCharacter);
		return this;
	}

	/**
	 * Returns the line break between items.
	 *
	 * @return the line break
	 */
	public String getLinebreak() {
		return linebreak;
	}

	/**
	 * Sets the line break between items.
	 *
	 * @param linebreak
	 *            the line break, null for none
	 */
	public void setLinebreak(final String linebreak) {
		if (linebreak == null) {
			this.linebreak = "";
		} else {
			this.linebreak = linebreak;
		}
	}

	/**
	 * Sets the line break between items.
	 *
	 * @param newLinebreak
	 *            the line break, null for none
	 * @return this writer for chaining
	 */
	public JsonWriter withLinebreak(final String newLinebreak) {
		setLinebreak(newLinebreak);
		return this;
	}

	/**
	 * Returns the separator after the colon of a property.
	 *
	 * @return the separator
	 */
	public String getSeparator() {
		return separator;
	}

	/**
	 * Sets the separator after the colon of a property.
	 *
	 * @param separator
	 *            the separator, null for none
	 */
	public void setSeparator(final String separator) {
		if (separator == null) {
			this.separator = "";
		} else {
			this.separator = separator;
		}
	}

	/**
	 * Sets the separator after the colon of a property.
	 *
	 * @param newSeparator
	 *            the separator, null for none
	 * @return this writer for chaining
	 */
	public JsonWriter withSeparator(final String newSeparator) {
		setSeparator(newSeparator);
		return this;
	}

	/**
	 * Returns the number of characters written so far.
	 *
	 * @return the number of characters written
	 */
	public long getWrittenCharacters() {
		return writtenCharacters;
	}

	/**
	 * Switches between compact output without line breaks, indentation and separators, and the
	 * default formatted output.
	 *
	 * @param value
	 *            true for compact output, false for formatted output
	 */
	public void setUglify(final boolean value) {
		if (value) {
			linebreak = "";
			indentation = "";
			separator = "";
		} else {
			linebreak = "\n";
			indentation = "\t";
			separator = " ";
		}
	}

	/**
	 * Switches between compact and formatted output, see {@link #setUglify(boolean)}.
	 *
	 * @param newValue
	 *            true for compact output, false for formatted output
	 * @return this writer for chaining
	 */
	public JsonWriter withUglify(final boolean newValue) {
		setUglify(newValue);
		return this;
	}

	/**
	 * Opens an object, as top level item, array item or property value.
	 *
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void openJsonObject() throws Exception {
		if (outputWriter == null) {
			write("{", true);
			openJsonStackItems.push(JsonStackItem.Object_Empty);
		} else {
			final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
			if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array && latestOpenJsonItem != JsonStackItem.Object_Value) {
				openJsonStackItems.push(latestOpenJsonItem);
				throw new JsonWriterStateException("Not matching open Json item for opening object: " + latestOpenJsonItem);
			} else {
				if (latestOpenJsonItem == JsonStackItem.Array) {
					write("," + linebreak, false);
				} else if (latestOpenJsonItem == JsonStackItem.Array_Empty) {
					write(linebreak, false);
				} else if (latestOpenJsonItem == JsonStackItem.Object_Value) {
					openJsonStackItems.push(JsonStackItem.Object_Value);
					write(linebreak, false);
				}

				if (latestOpenJsonItem != JsonStackItem.Object_Value) {
					openJsonStackItems.push(JsonStackItem.Array);
				}

				write("{", true);
				openJsonStackItems.push(JsonStackItem.Object_Empty);
			}
		}
	}

	/**
	 * Opens a property in the current object. Its value must be written next.
	 *
	 * @param propertyName
	 *            the property name
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void openJsonObjectProperty(final String propertyName) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Object_Empty && latestOpenJsonItem != JsonStackItem.Object) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for opening object property: " + latestOpenJsonItem);
		} else {
			if (latestOpenJsonItem == JsonStackItem.Object) {
				write("," + linebreak, false);
			} else {
				write(linebreak, false);
			}
			openJsonStackItems.push(JsonStackItem.Object);
			write("\"" + formatStringOutput(propertyName) + "\":", true);
			openJsonStackItems.push(JsonStackItem.Object_Value);
		}
	}

	/**
	 * Writes a simple JSON value (null, boolean, number or string) as value of the open property.
	 *
	 * @param propertyValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonObjectPropertyValue(final JsonNode propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			if (propertyValue == null || propertyValue instanceof JsonValueNull) {
				write(separator + "null", false);
			} else if (propertyValue instanceof JsonValueBoolean) {
				write(separator + Boolean.toString(((JsonValueBoolean) propertyValue).getValue()), false);
			} else if (propertyValue instanceof JsonValueInteger) {
				write(separator + ((JsonValueInteger) propertyValue).getValue().toString(), false);
			} else if (propertyValue instanceof JsonValueNumber) {
				write(separator + ((JsonValueNumber) propertyValue).getValue().toString(), false);
			} else if (propertyValue instanceof JsonValueString) {
				write(separator + "\"" + formatStringOutput(((JsonValueString) propertyValue).getValue()) + "\"", false);
			}
		}
	}

	/**
	 * Writes null as value of the open property.
	 *
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonObjectPropertyValueNull() throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			write(separator + "null", false);
		}
	}

	/**
	 * Writes a string as value of the open property.
	 *
	 * @param propertyValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonObjectPropertyValue(final String propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			if (propertyValue == null) {
				write(separator + "null", false);
			} else {
				write(separator + "\"" + formatStringOutput(propertyValue) + "\"", false);
			}
		}
	}

	/**
	 * Writes a date time as ISO 8601 string with time zone as value of the open property.
	 *
	 * @param propertyValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonObjectPropertyValue(final Date propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			if (propertyValue == null) {
				write(separator + "null", false);
			} else {
				write(separator + "\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, propertyValue) + "\"", false);
			}
		}
	}

	/**
	 * Writes a date as ISO 8601 string as value of the open property.
	 *
	 * @param propertyValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonObjectPropertyValue(final LocalDate propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			if (propertyValue == null) {
				write(separator + "null", false);
			} else {
				write(separator + "\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, propertyValue) + "\"", false);
			}
		}
	}

	/**
	 * Writes a date time as ISO 8601 string without time zone as value of the open property.
	 *
	 * @param propertyValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonObjectPropertyValue(final LocalDateTime propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			if (propertyValue == null) {
				write(separator + "null", false);
			} else {
				if (propertyValue.getNano() > 0) {
					write(separator + "\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT_NO_TIMEZONE, propertyValue) + "\"", false);
				} else {
					write(separator + "\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, propertyValue) + "\"", false);
				}
			}
		}
	}

	/**
	 * Writes a date time as ISO 8601 string with time zone as value of the open property.
	 *
	 * @param propertyValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonObjectPropertyValue(final ZonedDateTime propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			if (propertyValue == null) {
				write(separator + "null", false);
			} else {
				if (propertyValue.getNano() > 0) {
					write(separator + "\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT, propertyValue) + "\"", false);
				} else {
					write(separator + "\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, propertyValue) + "\"", false);
				}
			}
		}
	}

	/**
	 * Writes a boolean as value of the open property.
	 *
	 * @param propertyValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonObjectPropertyValue(final Boolean propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			if (propertyValue == null) {
				write(separator + "null", false);
			} else {
				write(separator + Boolean.toString(propertyValue), false);
			}
		}
	}

	/**
	 * Writes a number as value of the open property.
	 *
	 * @param propertyValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonObjectPropertyValue(final Number propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			if (propertyValue == null) {
				write(separator + "null", false);
			} else {
				write(separator + propertyValue.toString(), false);
			}
		}
	}

	/**
	 * Closes the current object.
	 *
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void closeJsonObject() throws Exception {
		if (openJsonStackItems.isEmpty()) {
			throw new JsonWriterStateException("Cannot close JsonObject. No open Object left");
		}
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Object_Empty && latestOpenJsonItem != JsonStackItem.Object) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for closing object: " + latestOpenJsonItem);
		} else if (latestOpenJsonItem == JsonStackItem.Object_Empty) {
			write("}", false);
		} else {
			write(linebreak, false);
			write("}", true);
		}

		if (openJsonStackItems.size() > 0 && openJsonStackItems.peek() == JsonStackItem.Object_Value) {
			openJsonStackItems.pop();
		}
	}

	/**
	 * Opens an array, as top level item, array item or property value.
	 *
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void openJsonArray() throws Exception {
		if (outputWriter == null) {
			write("[", true);
			openJsonStackItems.push(JsonStackItem.Array_Empty);
		} else {
			final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
			if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array && latestOpenJsonItem != JsonStackItem.Object_Value) {
				openJsonStackItems.push(latestOpenJsonItem);
				throw new JsonWriterStateException("Not matching open Json item for opening array: " + latestOpenJsonItem);
			} else {
				if (latestOpenJsonItem == JsonStackItem.Array) {
					write("," + linebreak, false);
				} else if (latestOpenJsonItem == JsonStackItem.Array_Empty) {
					write(linebreak, false);
				} else if (latestOpenJsonItem == JsonStackItem.Object_Value) {
					openJsonStackItems.push(JsonStackItem.Object_Value);
					write(linebreak, false);
				}

				if (latestOpenJsonItem != JsonStackItem.Object_Value) {
					openJsonStackItems.push(JsonStackItem.Array);
				}

				write("[", true);
				openJsonStackItems.push(JsonStackItem.Array_Empty);
			}
		}
	}

	/**
	 * Appends a simple JSON value (null, boolean, number or string) to the current array.
	 *
	 * @param arrayValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonArrayValue(final JsonNode arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding array value: " + latestOpenJsonItem);
		} else {
			if (latestOpenJsonItem == JsonStackItem.Array) {
				write("," + linebreak, false);
			} else {
				write(linebreak, false);
			}

			openJsonStackItems.push(JsonStackItem.Array);

			if (arrayValue == null || arrayValue instanceof JsonValueNull) {
				write("null", true);
			} else if (arrayValue instanceof JsonValueBoolean) {
				write(Boolean.toString(((JsonValueBoolean) arrayValue).getValue()), true);
			} else if (arrayValue instanceof JsonValueInteger) {
				write(((JsonValueInteger) arrayValue).getValue().toString(), true);
			} else if (arrayValue instanceof JsonValueNumber) {
				write(((JsonValueNumber) arrayValue).getValue().toString(), true);
			} else if (arrayValue instanceof JsonValueString) {
				write("\"" + formatStringOutput(((JsonValueString) arrayValue).getValue()) + "\"", true);
			}
		}
	}

	/**
	 * Appends null to the current array.
	 *
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonArrayValueNull() throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding array value: " + latestOpenJsonItem);
		} else {
			if (latestOpenJsonItem == JsonStackItem.Array) {
				write("," + linebreak, false);
			} else {
				write(linebreak, false);
			}

			openJsonStackItems.push(JsonStackItem.Array);

			write("null", true);
		}
	}

	/**
	 * Appends a string to the current array.
	 *
	 * @param arrayValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonArrayValue(final String arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding array value: " + latestOpenJsonItem);
		} else {
			if (latestOpenJsonItem == JsonStackItem.Array) {
				write("," + linebreak, false);
			} else {
				write(linebreak, false);
			}

			openJsonStackItems.push(JsonStackItem.Array);

			if (arrayValue == null) {
				write("null", true);
			} else {
				write("\"" + formatStringOutput(arrayValue) + "\"", true);
			}
		}
	}

	/**
	 * Appends a date time as ISO 8601 string with time zone to the current array.
	 *
	 * @param arrayValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonArrayValue(final Date arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding array value: " + latestOpenJsonItem);
		} else {
			if (latestOpenJsonItem == JsonStackItem.Array) {
				write("," + linebreak, false);
			} else {
				write(linebreak, false);
			}

			openJsonStackItems.push(JsonStackItem.Array);

			if (arrayValue == null) {
				write("null", true);
			} else {
				write("\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, arrayValue) + "\"", true);
			}
		}
	}

	/**
	 * Appends a date as ISO 8601 string to the current array.
	 *
	 * @param arrayValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonArrayValue(final LocalDate arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding array value: " + latestOpenJsonItem);
		} else {
			if (latestOpenJsonItem == JsonStackItem.Array) {
				write("," + linebreak, false);
			} else {
				write(linebreak, false);
			}

			openJsonStackItems.push(JsonStackItem.Array);

			if (arrayValue == null) {
				write("null", true);
			} else {
				write("\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, arrayValue) + "\"", true);
			}
		}
	}

	/**
	 * Appends a date time as ISO 8601 string without time zone to the current array.
	 *
	 * @param arrayValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonArrayValue(final LocalDateTime arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding array value: " + latestOpenJsonItem);
		} else {
			if (latestOpenJsonItem == JsonStackItem.Array) {
				write("," + linebreak, false);
			} else {
				write(linebreak, false);
			}

			openJsonStackItems.push(JsonStackItem.Array);

			if (arrayValue == null) {
				write("null", true);
			} else {
				if (arrayValue.getNano() > 0) {
					write("\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT_NO_TIMEZONE, arrayValue) + "\"", true);
				} else {
					write("\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, arrayValue) + "\"", true);
				}
			}
		}
	}

	/**
	 * Appends a date time as ISO 8601 string with time zone to the current array.
	 *
	 * @param arrayValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonArrayValue(final ZonedDateTime arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding array value: " + latestOpenJsonItem);
		} else {
			if (latestOpenJsonItem == JsonStackItem.Array) {
				write("," + linebreak, false);
			} else {
				write(linebreak, false);
			}

			openJsonStackItems.push(JsonStackItem.Array);

			if (arrayValue == null) {
				write("null", true);
			} else {
				if (arrayValue.getNano() > 0) {
					write("\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT, arrayValue) + "\"", true);
				} else {
					write("\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, arrayValue) + "\"", true);
				}
			}
		}
	}

	/**
	 * Appends a boolean to the current array.
	 *
	 * @param arrayValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonArrayValue(final Boolean arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding array value: " + latestOpenJsonItem);
		} else {
			if (latestOpenJsonItem == JsonStackItem.Array) {
				write("," + linebreak, false);
			} else {
				write(linebreak, false);
			}

			openJsonStackItems.push(JsonStackItem.Array);

			if (arrayValue == null) {
				write("null", true);
			} else {
				write(Boolean.toString(arrayValue), true);
			}
		}
	}

	/**
	 * Appends a number to the current array.
	 *
	 * @param arrayValue
	 *            the value, null writes null
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void addSimpleJsonArrayValue(final Number arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for adding array value: " + latestOpenJsonItem);
		} else {
			if (latestOpenJsonItem == JsonStackItem.Array) {
				write("," + linebreak, false);
			} else {
				write(linebreak, false);
			}

			openJsonStackItems.push(JsonStackItem.Array);

			if (arrayValue == null) {
				write("null", true);
			} else {
				write(arrayValue.toString(), true);
			}
		}
	}

	/**
	 * Writes a simple value as the only top level item.
	 *
	 * @param value
	 *            the value: null, Boolean, Number, a date or time type, or any other object written as
	 *            string
	 * @throws Exception
	 *             if something was written already or writing fails
	 */
	public void addSimpleValue(final Object value) throws Exception {
		if (writtenCharacters > 0 || openJsonStackItems.size() != 0) {
			throw new JsonWriterStateException("Not matching empty Json output for adding simple value");
		} else {
			if (value == null) {
				write("null", true);
			} else if (value instanceof Boolean) {
				write(Boolean.toString((Boolean) value), true);
			} else if (value instanceof Date) {
				write("\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, (Date) value) + "\"", true);
			} else if (value instanceof LocalDateTime) {
				if (((LocalDateTime) value).getNano() > 0) {
					write("\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT_NO_TIMEZONE, (LocalDateTime) value) + "\"", true);
				} else {
					write("\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, (LocalDateTime) value) + "\"", true);
				}
			} else if (value instanceof LocalDate) {
				write("\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, (LocalDate) value) + "\"", true);
			} else if (value instanceof ZonedDateTime) {
				if (((ZonedDateTime) value).getNano() > 0) {
					write("\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT, (ZonedDateTime) value) + "\"", true);
				} else {
					write("\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, (ZonedDateTime) value) + "\"", true);
				}
			} else if (value instanceof Number) {
				write(value.toString(), true);
			} else {
				write("\"" + formatStringOutput(value.toString()) + "\"", true);
			}
		}
	}

	/**
	 * Closes the current array.
	 *
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails
	 */
	public void closeJsonArray() throws Exception {
		final JsonStackItem latestOpenJsonItem = popLatestOpenJsonItem();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new JsonWriterStateException("Not matching open Json item for closing array: " + latestOpenJsonItem);
		} else if (latestOpenJsonItem == JsonStackItem.Array_Empty) {
			write("]", false);
		} else {
			write(linebreak, false);
			write("]", true);
		}

		if (openJsonStackItems.size() > 0 && openJsonStackItems.peek() == JsonStackItem.Object_Value) {
			openJsonStackItems.pop();
		}
	}

	/**
	 * Writes a complete object, as top level item, array item or property value.
	 *
	 * @param jsonObject
	 *            the object
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails, or the object is null
	 */
	public void add(final JsonObject jsonObject) throws Exception {
		if (jsonObject == null) {
			throw new JsonWriterStateException("Invalid null value added via 'add'. If done by intention use 'addSimpleJsonArrayValue' or 'addSimpleJsonObjectPropertyValue'");
		} else {
			openJsonObject();
			for (final Entry<String, JsonNode> property : jsonObject.entrySet()) {
				openJsonObjectProperty(property.getKey());
				final JsonNode propertyValue = property.getValue();
				if (propertyValue instanceof JsonObject) {
					add((JsonObject) propertyValue);
				} else if (propertyValue instanceof JsonArray) {
					add((JsonArray) propertyValue);
				} else {
					addSimpleJsonObjectPropertyValue(propertyValue);
				}
			}
			closeJsonObject();
		}
	}

	/**
	 * Writes a complete array, as top level item, array item or property value.
	 *
	 * @param jsonArray
	 *            the array
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails, or the array is null
	 */
	public void add(final JsonArray jsonArray) throws Exception {
		if (jsonArray == null) {
			throw new JsonWriterStateException("Invalid null value added via 'add'. If done by intention use 'addSimpleJsonArrayValue' or 'addSimpleJsonObjectPropertyValue'");
		} else {
			openJsonArray();
			for (final JsonNode arrayValue : jsonArray.items()) {
				if (arrayValue instanceof JsonObject) {
					add((JsonObject) arrayValue);
				} else if (arrayValue instanceof JsonArray) {
					add((JsonArray) arrayValue);
				} else {
					addSimpleJsonArrayValue(arrayValue);
				}
			}
			closeJsonArray();
		}
	}

	/**
	 * Writes a complete JSON item. A simple value is only allowed as top level item.
	 *
	 * @param jsonNode
	 *            the item
	 * @throws Exception
	 *             if the call does not fit the current write position or writing fails, or the item is null
	 */
	public void add(final JsonNode jsonNode) throws Exception {
		if (jsonNode == null) {
			throw new JsonWriterStateException("Invalid null value added via 'add'. If done by intention use 'addSimpleJsonArrayValue' or 'addSimpleJsonObjectPropertyValue'");
		} else if (jsonNode instanceof JsonObject) {
			add((JsonObject) jsonNode);
		} else if (jsonNode instanceof JsonArray) {
			add((JsonArray) jsonNode);
		} else if (jsonNode instanceof JsonValueNull) {
			write("null", true);
		} else if (jsonNode instanceof JsonValueBoolean) {
			write(Boolean.toString(((JsonValueBoolean) jsonNode).getValue()), true);
		} else if (jsonNode instanceof JsonValueInteger) {
			write(((JsonValueInteger) jsonNode).getValue().toString(), true);
		} else if (jsonNode instanceof JsonValueNumber) {
			write(((JsonValueNumber) jsonNode).getValue().toString(), true);
		} else if (jsonNode instanceof JsonValueString) {
			write("\"" + formatStringOutput(((JsonValueString) jsonNode).getValue()) + "\"", true);
		} else {
			throw new RuntimeException("Unsupported JsonNode type for 'add': '" + jsonNode.getClass().getSimpleName() + "'");
		}
	}

	/**
	 * Closes all open objects and arrays, e.g. to finish incomplete output.
	 *
	 * @throws Exception
	 *             if writing fails
	 */
	public void closeAllOpenJsonItems() throws Exception {
		while (!openJsonStackItems.isEmpty()) {
			final JsonStackItem openJsonItem = openJsonStackItems.pop();
			switch(openJsonItem) {
				case Array:
					write(linebreak, false);
					write("]", true);
					break;
				case Array_Empty:
					write("]", false);
					break;
				case Object:
				case Object_Empty:
					closeJsonObject();
					break;
				case Object_Value:
					break;
				default:
					throw new JsonWriterStateException("Invalid open json item");
			}
		}
	}

	/**
	 * Flush buffered data.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void flush() throws IOException {
		if (outputWriter != null) {
			outputWriter.flush();
		}
	}

	/**
	 * Closes this writer and its underlying stream.
	 *
	 * @throws IOException
	 *             if objects or arrays are still open; the stream is closed nevertheless
	 */
	@Override
	public void close() throws IOException {
		closeQuietly(outputWriter);
		outputWriter = null;
		closeQuietly(outputStream);
		outputStream = null;

		if (!openJsonStackItems.isEmpty()) {
			String jsonItemsStackString = "";
			while (!openJsonStackItems.isEmpty()) {
				jsonItemsStackString += "/" + openJsonStackItems.pop().toString();
			}
			throw new IOException("There are still Json items open: " + jsonItemsStackString);
		}
	}

	/**
	 * Removes and returns the innermost open item.
	 *
	 * @return the innermost open item
	 * @throws JsonWriterStateException
	 *             if no item is open, i.e. the method called does not fit the current write position
	 */
	private JsonStackItem popLatestOpenJsonItem() throws JsonWriterStateException {
		if (openJsonStackItems.isEmpty()) {
			throw new JsonWriterStateException("No open Json item for this operation");
		}
		return openJsonStackItems.pop();
	}

	/**
	 * Writes text, optionally indented according to the current nesting depth.
	 *
	 * @param text
	 *            the text to write
	 * @param indent
	 *            true to indent the text
	 * @throws IOException
	 *             if writing fails
	 * @throws IllegalStateException
	 *             if this writer is already closed
	 */
	private void write(final String text, final boolean indent) throws IOException {
		if (outputWriter == null) {
			if (outputStream == null) {
				throw new IllegalStateException("JsonWriter is already closed");
			}
			outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, encoding));
		}

		final String dataToWrite = (indent ? Utilities.repeat(indentation, openJsonStackItems.size()) : "") + text;
		writtenCharacters += dataToWrite.length();
		outputWriter.write(dataToWrite);
	}

	/**
	 * Close a Closable item and ignore any Exception thrown by its close method.
	 *
	 * @param closeableItem
	 *            the closeable item
	 */
	private static void closeQuietly(final Closeable closeableItem) {
		if (closeableItem != null) {
			try {
				closeableItem.close();
			} catch (@SuppressWarnings("unused") final IOException e) {
				// Do nothing
			}
		}
	}

	/**
	 * This method should only be used to write small JSON items
	 *
	 * @param jsonObject
	 *            JSON object to write
	 * @return JSON item as string
	 * @throws Exception
	 *             if the item cannot be written
	 */
	public static String getJsonItemString(final JsonObject jsonObject) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (JsonWriter jsonWriter = new JsonWriter(outputStream, StandardCharsets.UTF_8)) {
			jsonWriter.add(jsonObject);
		}

		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * This method should only be used to write small JSON items
	 *
	 * @param jsonArray
	 *            JSON array to write
	 * @return JSON item as string
	 * @throws Exception
	 *             if the item cannot be written
	 */
	public static String getJsonItemString(final JsonArray jsonArray) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (JsonWriter jsonWriter = new JsonWriter(outputStream, StandardCharsets.UTF_8)) {
			jsonWriter.add(jsonArray);
		}

		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * This method should only be used to write small JSON items
	 *
	 * @param jsonObject
	 *            JSON object to write
	 * @param linebreak
	 *            linebreak to use
	 * @param indentation
	 *            indentation to use per level
	 * @param separator
	 *            separator after the colon of a property
	 * @return JSON item as string
	 * @throws Exception
	 *             if the item cannot be written
	 */
	public static String getJsonItemString(final JsonObject jsonObject, final String linebreak, final String indentation, final String separator) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (JsonWriter jsonWriter = new JsonWriter(outputStream, StandardCharsets.UTF_8)) {
			jsonWriter.setLinebreak(linebreak);
			jsonWriter.setIndentation(indentation);
			jsonWriter.setSeparator(separator);
			jsonWriter.add(jsonObject);
		}

		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * This method should only be used to write small JSON items
	 *
	 * @param jsonArray
	 *            JSON array to write
	 * @param linebreak
	 *            linebreak to use
	 * @param indentation
	 *            indentation to use per level
	 * @param separator
	 *            separator after the colon of a property
	 * @return JSON item as string
	 * @throws Exception
	 *             if the item cannot be written
	 */
	public static String getJsonItemString(final JsonArray jsonArray, final String linebreak, final String indentation, final String separator) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (JsonWriter jsonWriter = new JsonWriter(outputStream, StandardCharsets.UTF_8)) {
			jsonWriter.setLinebreak(linebreak);
			jsonWriter.setIndentation(indentation);
			jsonWriter.setSeparator(separator);
			jsonWriter.add(jsonArray);
		}

		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * Returns a JSON item as formatted text. This method should only be used for small JSON items.
	 *
	 * @param jsonNode
	 *            JSON item to write
	 * @return JSON item as string, a string value is quoted and escaped
	 * @throws Exception
	 *             if the item cannot be written
	 */
	public static String getJsonItemString(final JsonNode jsonNode) throws Exception {
		return getJsonItemString(jsonNode, "\n", "\t", " ");
	}

	/**
	 * This method should only be used to write small JSON items
	 *
	 * @param jsonNode
	 *            JSON item to write
	 * @param linebreak
	 *            linebreak to use
	 * @param indentation
	 *            indentation to use per level
	 * @param separator
	 *            separator after the colon of a property
	 * @return JSON item as string, a string value is quoted and escaped
	 * @throws Exception
	 *             if the item cannot be written
	 */
	public static String getJsonItemString(final JsonNode jsonNode, final String linebreak, final String indentation, final String separator) throws Exception {
		switch (jsonNode.getJsonDataType()) {
			case OBJECT:
				return getJsonItemString((JsonObject) jsonNode, linebreak, indentation, separator);
			case ARRAY:
				return getJsonItemString((JsonArray) jsonNode, linebreak, indentation, separator);
			case STRING:
				// Quoted and escaped like in JsonWriter output, so the result is valid JSON
				return "\"" + formatStringOutput(((JsonValueString) jsonNode).getValue()) + "\"";
			case INTEGER:
				return ((JsonValueInteger) jsonNode).getValue().toString();
			case NUMBER:
				return ((JsonValueNumber) jsonNode).getValue().toString();
			case BOOLEAN:
				return ((JsonValueBoolean) jsonNode).getValue().toString();
			case NULL:
				return "null";
			default:
				throw new RuntimeException("Unknown JsonDataType: '" + jsonNode.getJsonDataType().getName() + "'");
		}
	}

	/**
	 * Escape a string value for JSON output (without the surrounding quotes).
	 * Backslash, quote and all control characters U+0000 to U+001F are escaped as required by the JSON specification.
	 *
	 * @param value
	 *            string value to escape
	 * @return escaped string value
	 */
	public static String formatStringOutput(final String value) {
		final StringBuilder result = new StringBuilder(value.length() + 16);
		for (int i = 0; i < value.length(); i++) {
			final char character = value.charAt(i);
			switch (character) {
				case '\\':
					result.append("\\\\");
					break;
				case '"':
					result.append("\\\"");
					break;
				case '\b':
					result.append("\\b");
					break;
				case '\f':
					result.append("\\f");
					break;
				case '\r':
					result.append("\\r");
					break;
				case '\n':
					result.append("\\n");
					break;
				case '\t':
					result.append("\\t");
					break;
				default:
					if (character < 0x20) {
						// Other control characters have no short escape sequence in JSON
						result.append(String.format("\\u%04x", (int) character));
					} else {
						result.append(character);
					}
			}
		}
		return result.toString();
	}
}
