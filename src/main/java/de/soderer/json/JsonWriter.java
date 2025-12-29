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

import de.soderer.json.utilities.DateUtilities;
import de.soderer.json.utilities.Utilities;

public class JsonWriter implements Closeable {
	/** Default output encoding. */
	public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	/** Output stream. */
	private OutputStream outputStream;

	/** Output encoding. */
	private final Charset encoding;

	/** Output writer. */
	private BufferedWriter outputWriter = null;

	private long writtenCharacters = 0;

	private final Stack<JsonStackItem> openJsonStackItems = new Stack<>();

	private String linebreak = "\n";
	private String indentation = "\t";
	private String separator = " ";

	private enum JsonStackItem {
		Array_Empty,
		Array,
		Object_Empty,
		Object,
		Object_Value
	}

	public JsonWriter(final OutputStream outputStream) {
		this(outputStream, null);
	}

	public JsonWriter(final OutputStream outputStream, final Charset encoding) {
		this.outputStream = outputStream;
		this.encoding = encoding == null ? DEFAULT_ENCODING : encoding;
	}

	public JsonWriter setIndentation(final String indentation) {
		if (indentation == null) {
			this.indentation = "";
		} else {
			this.indentation = indentation;
		}
		return this;
	}

	public JsonWriter setIndentation(final char indentationCharacter) {
		indentation = Character.toString(indentationCharacter);
		return this;
	}

	public String getLinebreak() {
		return linebreak;
	}

	public JsonWriter setLinebreak(final String linebreak) {
		if (linebreak == null) {
			this.linebreak = "";
		} else {
			this.linebreak = linebreak;
		}
		return this;
	}

	public String getSeparator() {
		return separator;
	}

	public JsonWriter setSeparator(final String separator) {
		if (separator == null) {
			this.separator = "";
		} else {
			this.separator = separator;
		}
		return this;
	}

	public long getWrittenCharacters() {
		return writtenCharacters;
	}

	public JsonWriter setUglify(final boolean value) {
		if (value) {
			linebreak = "";
			indentation = "";
			separator = "";
		} else {
			linebreak = "\n";
			indentation = "\t";
			separator = " ";
		}
		return this;
	}

	public void openJsonObject() throws Exception {
		if (outputWriter == null) {
			write("{", true);
			openJsonStackItems.push(JsonStackItem.Object_Empty);
		} else {
			final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
			if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array && latestOpenJsonItem != JsonStackItem.Object_Value) {
				openJsonStackItems.push(latestOpenJsonItem);
				throw new Exception("Not matching open Json item for opening object: " + latestOpenJsonItem);
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

	public void openJsonObjectProperty(final String propertyName) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Object_Empty && latestOpenJsonItem != JsonStackItem.Object) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for opening object property: " + latestOpenJsonItem);
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

	public void addSimpleJsonObjectPropertyValue(final JsonNode propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
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

	public void addSimpleJsonObjectPropertyValueNull() throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			write(separator + "null", false);
		}
	}

	public void addSimpleJsonObjectPropertyValue(final String propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			if (propertyValue == null) {
				write(separator + "null", false);
			} else {
				write(separator + "\"" + formatStringOutput(propertyValue) + "\"", false);
			}
		}
	}

	public void addSimpleJsonObjectPropertyValue(final Date propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			if (propertyValue == null) {
				write(separator + "null", false);
			} else {
				write(separator + "\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, propertyValue) + "\"", false);
			}
		}
	}

	public void addSimpleJsonObjectPropertyValue(final LocalDate propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			if (propertyValue == null) {
				write(separator + "null", false);
			} else {
				write(separator + "\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, propertyValue) + "\"", false);
			}
		}
	}

	public void addSimpleJsonObjectPropertyValue(final LocalDateTime propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
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

	public void addSimpleJsonObjectPropertyValue(final ZonedDateTime propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
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

	public void addSimpleJsonObjectPropertyValue(final Boolean propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			if (propertyValue == null) {
				write(separator + "null", false);
			} else {
				write(separator + Boolean.toString(propertyValue), false);
			}
		}
	}

	public void addSimpleJsonObjectPropertyValue(final Number propertyValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Object_Value) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			if (propertyValue == null) {
				write(separator + "null", false);
			} else {
				write(separator + propertyValue.toString(), false);
			}
		}
	}

	public void closeJsonObject() throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Object_Empty && latestOpenJsonItem != JsonStackItem.Object) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for closing object: " + latestOpenJsonItem);
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

	public void openJsonArray() throws Exception {
		if (outputWriter == null) {
			write("[", true);
			openJsonStackItems.push(JsonStackItem.Array_Empty);
		} else {
			final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
			if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array && latestOpenJsonItem != JsonStackItem.Object_Value) {
				openJsonStackItems.push(latestOpenJsonItem);
				throw new Exception("Not matching open Json item for opening array: " + latestOpenJsonItem);
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

	public void addSimpleJsonArrayValue(final JsonNode arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding array value: " + latestOpenJsonItem);
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

	public void addSimpleJsonArrayValueNull() throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding array value: " + latestOpenJsonItem);
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

	public void addSimpleJsonArrayValue(final String arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding array value: " + latestOpenJsonItem);
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

	public void addSimpleJsonArrayValue(final Date arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding array value: " + latestOpenJsonItem);
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

	public void addSimpleJsonArrayValue(final LocalDate arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding array value: " + latestOpenJsonItem);
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

	public void addSimpleJsonArrayValue(final LocalDateTime arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding array value: " + latestOpenJsonItem);
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

	public void addSimpleJsonArrayValue(final ZonedDateTime arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding array value: " + latestOpenJsonItem);
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

	public void addSimpleJsonArrayValue(final Boolean arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding array value: " + latestOpenJsonItem);
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

	public void addSimpleJsonArrayValue(final Number arrayValue) throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding array value: " + latestOpenJsonItem);
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

	public void addSimpleValue(final Object value) throws Exception {
		if (writtenCharacters > 0 || openJsonStackItems.size() != 0) {
			throw new Exception("Not matching empty Json output for adding simple value");
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
				write("\"" + value.toString().replace("\"", "\\\"") + "\"", true);
			}
		}
	}

	public void closeJsonArray() throws Exception {
		final JsonStackItem latestOpenJsonItem = openJsonStackItems.pop();
		if (latestOpenJsonItem != JsonStackItem.Array_Empty && latestOpenJsonItem != JsonStackItem.Array) {
			openJsonStackItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for closing array: " + latestOpenJsonItem);
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

	public void add(final JsonObject jsonObject) throws Exception {
		if (jsonObject == null) {
			throw new Exception("Invalid null value added via 'add'. If done by intention use 'addSimpleJsonArrayValue' or 'addSimpleJsonObjectPropertyValue'");
		} else {
			openJsonObject();
			for (final Entry<String, JsonNode> property : jsonObject) {
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

	public void add(final JsonArray jsonArray) throws Exception {
		if (jsonArray == null) {
			throw new Exception("Invalid null value added via 'add'. If done by intention use 'addSimpleJsonArrayValue' or 'addSimpleJsonObjectPropertyValue'");
		} else {
			openJsonArray();
			for (final JsonNode arrayValue : jsonArray) {
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

	public void closeAllOpenJsonItems() throws Exception {
		while (!openJsonStackItems.isEmpty()) {
			final JsonStackItem openJsonItem = openJsonStackItems.pop();
			switch(openJsonItem) {
				case Array:
				case Array_Empty:
					closeJsonArray();
					break;
				case Object:
				case Object_Empty:
					closeJsonObject();
					break;
				case Object_Value:
					break;
				default:
					throw new Exception("Invalid open json item");
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
	 * Close this writer and its underlying stream.
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
	 * This method should only be used to write small Json items
	 *
	 * @param jsonItem
	 * @return
	 * @throws Exception
	 */
	public static String getJsonItemString(final JsonObject jsonObject) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (JsonWriter jsonWriter = new JsonWriter(outputStream, StandardCharsets.UTF_8)) {
			jsonWriter.add(jsonObject);
			jsonWriter.close();
		}

		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * This method should only be used to write small Json items
	 *
	 * @param jsonItem
	 * @return
	 * @throws Exception
	 */
	public static String getJsonItemString(final JsonArray jsonArray) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (JsonWriter jsonWriter = new JsonWriter(outputStream, StandardCharsets.UTF_8)) {
			jsonWriter.add(jsonArray);
			jsonWriter.close();
		}

		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * This method should only be used to write small Json items
	 *
	 * @param jsonItem
	 * @return
	 * @throws Exception
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
	 * This method should only be used to write small Json items
	 *
	 * @param jsonItem
	 * @return
	 * @throws Exception
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

	public static String getJsonItemString(final JsonNode jsonNode) throws Exception {
		return getJsonItemString(jsonNode, "\n", "\t", " ");
	}

	/**
	 * This method should only be used to write small JSON items
	 *
	 * @param jsonItem
	 * @return
	 * @throws Exception
	 */
	public static String getJsonItemString(final JsonNode jsonNode, final String linebreak, final String indentation, final String separator) throws Exception {
		switch (jsonNode.getJsonDataType()) {
			case OBJECT:
				return getJsonItemString((JsonObject) jsonNode, linebreak, indentation, separator);
			case ARRAY:
				return getJsonItemString((JsonArray) jsonNode, linebreak, indentation, separator);
			case STRING:
				return ((JsonValueString) jsonNode).getValue();
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

	public static String formatStringOutput(final String value) {
		return value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("/", "\\/")
				.replace("\b", "\\b")
				.replace("\f", "\\f")
				.replace("\r", "\\r")
				.replace("\n", "\\n")
				.replace("\t", "\\t");
	}
}
