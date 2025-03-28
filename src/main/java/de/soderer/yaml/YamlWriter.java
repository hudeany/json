package de.soderer.yaml;

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
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Stack;

import de.soderer.json.utilities.DateUtilities;
import de.soderer.json.utilities.Utilities;

public class YamlWriter implements Closeable {
	/** Default output encoding. */
	public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	/** Output stream. */
	private OutputStream outputStream;

	/** Output encoding. */
	private final Charset encoding;

	private boolean alwaysQuotePropertyNames = false;
	private boolean alwaysQuoteStringValues = false;

	/** Output writer. */
	private BufferedWriter outputWriter = null;

	private long writtenCharacters = 0;

	private final Stack<YamlStackItem> openYamlStackItems = new Stack<>();
	
	private int currentIndentationLevel = 0;

	private String linebreak = "\n";
	private String indentation = "  ";
	private String separator = " ";

	private enum YamlStackItem {
		Sequence_Empty,
		Sequence_Flow,
		Sequence,
		Mapping_Empty,
		Mapping_Flow,
		Mapping,
		Mapping_Value
	}

	public YamlWriter(final OutputStream outputStream) {
		this(outputStream, null);
	}

	public YamlWriter(final OutputStream outputStream, final Charset encoding) {
		this.outputStream = outputStream;
		this.encoding = encoding == null ? DEFAULT_ENCODING : encoding;
	}

	public YamlWriter setIndentation(final String indentation) {
		if (Utilities.isEmpty(indentation)) {
			this.indentation = "  ";
		} else {
			this.indentation = indentation;
		}
		return this;
	}

	public YamlWriter setIndentation(final char indentationCharacter) {
		indentation = Character.toString(indentationCharacter);
		return this;
	}

	public boolean isAlwaysQuotePropertyNames() {
		return alwaysQuotePropertyNames;
	}

	public YamlWriter setAlwaysQuotePropertyNames(final boolean alwaysQuotePropertyNames) {
		this.alwaysQuotePropertyNames = alwaysQuotePropertyNames;
		return this;
	}

	public boolean isAlwaysQuoteStringValues() {
		return alwaysQuoteStringValues;
	}

	public YamlWriter setAlwaysQuoteStringValues(final boolean alwaysQuoteStringValues) {
		this.alwaysQuoteStringValues = alwaysQuoteStringValues;
		return this;
	}

	public String getLinebreak() {
		return linebreak;
	}

	public YamlWriter setLinebreak(final String linebreak) {
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

	public YamlWriter setSeparator(final String separator) {
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

	public YamlWriter setUglify(final boolean value) {
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

	public void openYamlMapping() throws Exception {
		if (outputWriter == null) {
			write("{", true);
			openYamlStackItems.push(YamlStackItem.Mapping_Empty);
		} else {
			final YamlStackItem latestOpenYamlItem = openYamlStackItems.pop();
			if (latestOpenYamlItem != YamlStackItem.Sequence_Empty && latestOpenYamlItem != YamlStackItem.Sequence && latestOpenYamlItem != YamlStackItem.Mapping_Value) {
				openYamlStackItems.push(latestOpenYamlItem);
				throw new Exception("Not matching open Yaml item for opening object: " + latestOpenYamlItem);
			} else {
				if (latestOpenYamlItem == YamlStackItem.Sequence) {
					write("," + linebreak, false);
				} else if (latestOpenYamlItem == YamlStackItem.Sequence_Empty) {
					write(linebreak, false);
				} else if (latestOpenYamlItem == YamlStackItem.Mapping_Value) {
					openYamlStackItems.push(YamlStackItem.Mapping_Value);
					write(linebreak, false);
				}

				if (latestOpenYamlItem != YamlStackItem.Mapping_Value) {
					openYamlStackItems.push(YamlStackItem.Sequence);
				}

				write("{", true);
				openYamlStackItems.push(YamlStackItem.Mapping_Empty);
			}
		}
	}

	public void openYamlObjectProperty(final String propertyName) throws Exception {
		final YamlStackItem latestOpenYamlItem = openYamlStackItems.pop();
		if (latestOpenYamlItem != YamlStackItem.Mapping_Empty && latestOpenYamlItem != YamlStackItem.Mapping) {
			openYamlStackItems.push(latestOpenYamlItem);
			throw new Exception("Not matching open Yaml item for opening object property: " + latestOpenYamlItem);
		} else {
			if (latestOpenYamlItem == YamlStackItem.Mapping) {
				write("," + linebreak, false);
			} else {
				write(linebreak, false);
			}
			openYamlStackItems.push(YamlStackItem.Mapping);
			write(formatPropertyNameOutput(propertyName) + ":", true);
			openYamlStackItems.push(YamlStackItem.Mapping_Value);
		}
	}

	public void addSimpleYamlObjectPropertyValue(final Object propertyValue) throws Exception {
		final YamlStackItem latestOpenYamlItem = openYamlStackItems.pop();
		if (latestOpenYamlItem != YamlStackItem.Mapping_Value) {
			openYamlStackItems.push(latestOpenYamlItem);
			throw new Exception("Not matching open Yaml item for adding object property value: " + latestOpenYamlItem);
		} else {
			if (propertyValue == null) {
				write(separator + "null", false);
			} else if (propertyValue instanceof Boolean) {
				write(separator + Boolean.toString((Boolean) propertyValue), false);
			} else if (propertyValue instanceof Date) {
				write(separator + "\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, (Date) propertyValue) + "\"", false);
			} else if (propertyValue instanceof LocalDateTime) {
				if (((LocalDateTime) propertyValue).getNano() > 0) {
					write(separator + "\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT_NO_TIMEZONE, (LocalDateTime) propertyValue) + "\"", false);
				} else {
					write(separator + "\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, (LocalDateTime) propertyValue) + "\"", false);
				}
			} else if (propertyValue instanceof LocalDate) {
				write(separator + "\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, (LocalDate) propertyValue) + "\"", false);
			} else if (propertyValue instanceof ZonedDateTime) {
				if (((ZonedDateTime) propertyValue).getNano() > 0) {
					write(separator + "\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT, (ZonedDateTime) propertyValue) + "\"", false);
				} else {
					write(separator + "\"" + DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, (ZonedDateTime) propertyValue) + "\"", false);
				}
			} else if (propertyValue instanceof Number) {
				write(separator + propertyValue.toString(), false);
			} else {
				write(separator + "\"" + formatStringValueOutput(propertyValue.toString()) + "\"", false);
			}
		}
	}

	public void closeYamlMapping() throws Exception {
		final YamlStackItem latestOpenYamlItem = openYamlStackItems.pop();
		if (latestOpenYamlItem != YamlStackItem.Mapping_Empty && latestOpenYamlItem != YamlStackItem.Mapping) {
			openYamlStackItems.push(latestOpenYamlItem);
			throw new Exception("Not matching open Yaml item for closing object: " + latestOpenYamlItem);
		} else if (latestOpenYamlItem == YamlStackItem.Mapping_Empty) {
			write("}", false);
		} else {
			write(linebreak, false);
			write("}", true);
		}

		if (openYamlStackItems.size() > 0 && openYamlStackItems.peek() == YamlStackItem.Mapping_Value) {
			openYamlStackItems.pop();
		}
	}

	public void addSimpleYamlArrayValue(final Object arrayValue) throws Exception {
		final YamlStackItem latestOpenYamlItem = openYamlStackItems.pop();
		if (latestOpenYamlItem != YamlStackItem.Sequence_Empty && latestOpenYamlItem != YamlStackItem.Sequence) {
			openYamlStackItems.push(latestOpenYamlItem);
			throw new Exception("Not matching open Yaml item for adding array value: " + latestOpenYamlItem);
		} else {
			if (latestOpenYamlItem == YamlStackItem.Sequence) {
				write("," + linebreak, false);
			} else if (latestOpenYamlItem != YamlStackItem.Sequence_Empty) {
				write(linebreak, false);
			}
			openYamlStackItems.push(YamlStackItem.Sequence);
			
			write(getSimpleValueString(arrayValue), true);
		}
	}

	public void addSimpleValue(final Object value) throws Exception {
		if (writtenCharacters > 0 || openYamlStackItems.size() != 0) {
			throw new Exception("Not matching empty Yaml output for adding simple value");
		} else {
			write(getSimpleValueString(value), true);
		}
	}

	public void closeYamlSequence() throws Exception {
		final YamlStackItem latestOpenYamlItem = openYamlStackItems.pop();
		if (latestOpenYamlItem != YamlStackItem.Sequence_Empty && latestOpenYamlItem != YamlStackItem.Sequence) {
			openYamlStackItems.push(latestOpenYamlItem);
			throw new Exception("Not matching open Yaml item for closing array: " + latestOpenYamlItem);
		} else if (latestOpenYamlItem == YamlStackItem.Sequence_Empty) {
			write("]", false);
		} else {
			write(linebreak, false);
			write("]", true);
		}

		if (openYamlStackItems.size() > 0 && openYamlStackItems.peek() == YamlStackItem.Mapping_Value) {
			openYamlStackItems.pop();
		}
	}
	
	public void add(final YamlObject<?> yamlObject) throws Exception {
		if (yamlObject instanceof YamlSequence) {
			add((YamlSequence) yamlObject);
		} else if (yamlObject instanceof YamlMapping) {
			add((YamlMapping) yamlObject);
		} else if (yamlObject instanceof YamlSimpleValue) {
			add((YamlSimpleValue) yamlObject);
		}
	}

	public void add(final YamlMapping yamlMapping) throws Exception {
		if (yamlMapping == null) {
			throw new Exception("Invalid null value added via 'add'. If done by intention use 'addSimpleYamlArrayValue' or 'addSimpleYamlObjectPropertyValue'");
		} else {
			YamlStyle writeStyle;
			if (yamlMapping.size() == 0) {
				writeStyle = YamlStyle.Flow;
			} else if (yamlMapping.getStyle() == null) {
				writeStyle = YamlStyle.Standard;
			} else {
				writeStyle = yamlMapping.getStyle();
			}
			
			openYamlStackItems.push(YamlStackItem.Mapping);
			if (writeStyle == YamlStyle.Flow) {
				String mappingBracketStyleString = "";
				for (final YamlMappingEntry entry : yamlMapping) {
					if (mappingBracketStyleString.length() > 0) {
						mappingBracketStyleString += ", ";
					}

					if (entry.getValue() instanceof YamlSequence) {
						write(getSimpleValueString(entry.getKey()) + ": ", true);
						add((YamlSequence) entry.getValue());
					} else if (entry.getValue() instanceof YamlMapping) {
						write(getSimpleValueString(entry.getKey()) + ": ", true);
						add((YamlMapping) entry.getValue());
					} else if (entry.getValue() instanceof YamlSimpleValue) {
						mappingBracketStyleString += getSimpleValueString(entry.getKey()) + ": " + getSimpleValueString(entry.getValue());
					}
				}
				write("{" + mappingBracketStyleString + "}" + linebreak, true);
			} else if (writeStyle == YamlStyle.Bracket) {
				String mappingBracketStyleString = "";
				for (final YamlMappingEntry entry : yamlMapping) {
					if (mappingBracketStyleString.length() > 0) {
						mappingBracketStyleString += "," + linebreak;
					}

					if (entry.getValue() instanceof YamlSequence) {
						write(getSimpleValueString(entry.getKey()) + ": ", true);
						add((YamlSequence) entry.getValue());
					} else if (entry.getValue() instanceof YamlMapping) {
						write(getSimpleValueString(entry.getKey()) + ": ", true);
						add((YamlMapping) entry.getValue());
					} else if (entry.getValue() instanceof YamlSimpleValue) {
						mappingBracketStyleString += Utilities.repeat(indentation, currentIndentationLevel + 1) + getSimpleValueString(entry.getKey()) + ": " + getSimpleValueString(entry.getValue());
					}
				}
				write("{" + linebreak + mappingBracketStyleString + linebreak + Utilities.repeat(indentation, currentIndentationLevel) + "}" + linebreak, true);
			} else {
				for (final YamlMappingEntry entry : yamlMapping) {
					if (entry.getValue() instanceof YamlSequence) {
						write(getSimpleValueString(entry.getKey()) + ": ", true);
						add((YamlSequence) entry.getValue());
					} else if (entry.getValue() instanceof YamlMapping) {
						write(getSimpleValueString(entry.getKey()) + ": ", true);
						add((YamlMapping) entry.getValue());
					} else if (entry.getValue() instanceof YamlSimpleValue) {
						write(getSimpleValueString(entry.getKey()) + ": " + getSimpleValueString(entry.getValue()) + linebreak, true);
					} else {
						throw new Exception("Unexpected object type in YamlMapping");
					}
				}
			}
			if (openYamlStackItems.peek() == YamlStackItem.Mapping) {
				openYamlStackItems.pop();
			} else {
				throw new Exception("Invalid internal state: " + openYamlStackItems.peek());
			}
		}
	}

	public void add(final YamlSequence yamlSequence) throws Exception {
		if (yamlSequence == null) {
			throw new Exception("Invalid null value added via 'add'. If done by intention use 'addSimpleYamlArrayValue' or 'addSimpleYamlObjectPropertyValue'");
		} else {
			YamlStyle writeStyle;
			if (yamlSequence.size() == 0) {
				writeStyle = YamlStyle.Flow;
			} else if (yamlSequence.getStyle() == null) {
				if (yamlSequence.size() <= 5) {
					writeStyle = YamlStyle.Flow;
				} else {
					writeStyle = YamlStyle.Standard;
				}
			} else {
				writeStyle = yamlSequence.getStyle();
			}

			openYamlStackItems.push(YamlStackItem.Sequence);
			if (writeStyle == YamlStyle.Flow) {
				String sequenceFlowStyleString = "";
				for (final YamlObject<?> sequenceItem : yamlSequence) {
					if (sequenceFlowStyleString.length() > 0) {
						sequenceFlowStyleString += ", ";
					}
					
					if (sequenceItem instanceof YamlMapping) {
						add((YamlMapping) sequenceItem);
					} else if (sequenceItem instanceof YamlSequence) {
						add((YamlSequence) sequenceItem);
					} else if (sequenceItem instanceof YamlSimpleValue) {
						sequenceFlowStyleString += getSimpleValueString(sequenceItem.getValue());
					}
				}
				write("[" + sequenceFlowStyleString + "]" + linebreak, true);
			} else if (writeStyle == YamlStyle.Bracket) {
				String sequenceBracketStyleString = "";
				for (final YamlObject<?> sequenceItem : yamlSequence) {
					if (sequenceBracketStyleString.length() > 0) {
						sequenceBracketStyleString += "," + linebreak;
					}
					
					if (sequenceItem instanceof YamlMapping) {
						add((YamlMapping) sequenceItem);
					} else if (sequenceItem instanceof YamlSequence) {
						add((YamlSequence) sequenceItem);
					} else if (sequenceItem instanceof YamlSimpleValue) {
						sequenceBracketStyleString += Utilities.repeat(indentation, currentIndentationLevel + 1) + getSimpleValueString(sequenceItem.getValue());
					}
				}
				write("[" + linebreak + sequenceBracketStyleString + linebreak + Utilities.repeat(indentation, currentIndentationLevel) + "]" + linebreak, true);
			} else {
				for (final YamlObject<?> sequenceItem : yamlSequence) {
					if (sequenceItem instanceof YamlMapping) {
						write("- ", true);
						add((YamlMapping) sequenceItem);
					} else if (sequenceItem instanceof YamlSequence) {
						write("- ", true);
						add((YamlSequence) sequenceItem);
					} else if (sequenceItem instanceof YamlSimpleValue) {
						write("- " + getSimpleValueString(sequenceItem.getValue()) + linebreak, true);
					} else {
						throw new Exception("Unexpected object type in YamlSequence");
					}
				}
			}
			if (openYamlStackItems.peek() == YamlStackItem.Sequence) {
				openYamlStackItems.pop();
			} else {
				throw new Exception("Invalid internal state: " + openYamlStackItems.peek());
			}
		}
	}

	private String getSimpleValueString(Object sequenceItemValue) {
		if (sequenceItemValue == null) {
			return "null";
		} else {
			if (sequenceItemValue instanceof YamlSimpleValue) {
				sequenceItemValue = ((YamlSimpleValue) sequenceItemValue).getValue();
			}
			
			if (sequenceItemValue instanceof Boolean) {
				return Boolean.toString((Boolean) sequenceItemValue);
			} else if (sequenceItemValue instanceof Date) {
				return DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, (Date) sequenceItemValue);
			} else if (sequenceItemValue instanceof LocalDateTime) {
				if (((LocalDateTime) sequenceItemValue).getNano() > 0) {
					return DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT_NO_TIMEZONE, (LocalDateTime) sequenceItemValue);
				} else {
					return DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, (LocalDateTime) sequenceItemValue);
				}
			} else if (sequenceItemValue instanceof LocalDate) {
				return DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, (LocalDate) sequenceItemValue);
			} else if (sequenceItemValue instanceof LocalTime) {
				if (((LocalTime) sequenceItemValue).getSecond() == 0) {
					return DateUtilities.formatDate("HH:mm", (LocalTime) sequenceItemValue);
				} else {
					return DateUtilities.formatDate("HH:mm:ss", (LocalTime) sequenceItemValue);
				}
			} else if (sequenceItemValue instanceof ZonedDateTime) {
				if (((ZonedDateTime) sequenceItemValue).getNano() > 0) {
					return DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT, (ZonedDateTime) sequenceItemValue);
				} else {
					return DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, (ZonedDateTime) sequenceItemValue);
				}
			} else if (sequenceItemValue instanceof Number) {
				return sequenceItemValue.toString();
			} else {
				return formatStringValueOutput(sequenceItemValue.toString());
			}
		}
	}

	public void add(final YamlSimpleValue yamlSimpleValue) throws Exception {
		if (yamlSimpleValue == null) {
			throw new Exception("Invalid null value added via 'add'. If done by intention use 'addSimpleYamlArrayValue' or 'addSimpleYamlObjectPropertyValue'");
		} else {
			addSimpleValue(yamlSimpleValue.getValue());
		}
	}

	public void closeAllOpenYamlItems() throws Exception {
		while (!openYamlStackItems.isEmpty()) {
			final YamlStackItem openYamlItem = openYamlStackItems.pop();
			switch(openYamlItem) {
				case Sequence:
				case Sequence_Empty:
					closeYamlSequence();
					break;
				case Mapping:
				case Mapping_Empty:
					closeYamlMapping();
					break;
				case Mapping_Value:
					break;
				default:
					throw new Exception("Invalid open yaml item");
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

		if (!openYamlStackItems.isEmpty()) {
			String yamlItemsStackString = "";
			while (!openYamlStackItems.isEmpty()) {
				yamlItemsStackString += "/" + openYamlStackItems.pop().toString();
			}
			throw new IOException("There are still Yaml items open: " + yamlItemsStackString);
		}
	}
	
	private void write(final String text, final boolean indent) throws IOException {
		if (outputWriter == null) {
			if (outputStream == null) {
				throw new IllegalStateException("YamlWriter is already closed");
			}
			outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, encoding));
		}

		final String dataToWrite = (indent ? Utilities.repeat(indentation, currentIndentationLevel) : "") + text;
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
	 * This method should only be used to write small Yaml items
	 *
	 * @param yamlItem
	 * @return
	 * @throws Exception
	 */
	public static String getYamlItemString(final YamlMapping yamlObject) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (YamlWriter yamlWriter = new YamlWriter(outputStream, StandardCharsets.UTF_8)) {
			yamlWriter.add(yamlObject);
			yamlWriter.close();
		}

		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * This method should only be used to write small Yaml items
	 *
	 * @param yamlItem
	 * @return
	 * @throws Exception
	 */
	public static String getYamlItemString(final YamlSequence yamlArray) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (YamlWriter yamlWriter = new YamlWriter(outputStream, StandardCharsets.UTF_8)) {
			yamlWriter.add(yamlArray);
			yamlWriter.close();
		}

		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * This method should only be used to write small Yaml items
	 *
	 * @param yamlItem
	 * @return
	 * @throws Exception
	 */
	public static String getYamlItemString(final YamlMapping yamlObject, final String linebreak, final String indentation, final String separator) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (YamlWriter yamlWriter = new YamlWriter(outputStream, StandardCharsets.UTF_8)) {
			yamlWriter.setLinebreak(linebreak);
			yamlWriter.setIndentation(indentation);
			yamlWriter.setSeparator(separator);
			yamlWriter.add(yamlObject);
		}

		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * This method should only be used to write small Yaml items
	 *
	 * @param yamlItem
	 * @return
	 * @throws Exception
	 */
	public static String getYamlItemString(final YamlSequence yamlArray, final String linebreak, final String indentation, final String separator) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (YamlWriter yamlWriter = new YamlWriter(outputStream, StandardCharsets.UTF_8)) {
			yamlWriter.setLinebreak(linebreak);
			yamlWriter.setIndentation(indentation);
			yamlWriter.setSeparator(separator);
			yamlWriter.add(yamlArray);
		}

		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	public static String getYamlItemString(final YamlObject<?> yamlObject) throws Exception {
		return getYamlItemString(yamlObject, "\n", "\t", " ");
	}

	/**
	 * This method should only be used to write small Yaml items
	 *
	 * @param yamlItem
	 * @return
	 * @throws Exception
	 */
	public static String getYamlItemString(final YamlObject<?> yamlObject, final String linebreak, final String indentation, final String separator) throws Exception {
		if (yamlObject instanceof YamlMapping) {
			return getYamlItemString((YamlMapping) yamlObject, linebreak, indentation, separator);
		} else if (yamlObject instanceof YamlSequence) {
			return getYamlItemString((YamlSequence) yamlObject, linebreak, indentation, separator);
		} else {
			return getYamlItemString((YamlSimpleValue) yamlObject, linebreak, indentation, separator);
		}
	}

	public String formatPropertyNameOutput(final String propertyName) {
		if (alwaysQuotePropertyNames) {
			return "\"" + propertyName
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("/", "\\/")
				.replace("\b", "\\b")
				.replace("\f", "\\f")
				.replace("\r", "\\r")
				.replace("\n", "\\n")
				.replace("\t", "\\t")
				+ "\"";
		} else {
			return propertyName
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("'", "''")
				.replace("/", "\\/")
				.replace("\b", "\\b")
				.replace("\f", "\\f")
				.replace("\r", "\\r")
				.replace("\n", "\\n")
				.replace("\t", "\\t");
		}
	}

	public String formatStringValueOutput(final String stringValue) {
		boolean quoteString = alwaysQuoteStringValues;
		if (!quoteString) {
			if (stringValue.startsWith(" ")
					|| stringValue.startsWith("\t")
					|| stringValue.endsWith(" ")
					|| stringValue.endsWith("\t")
					|| stringValue.contains(": ")
					|| stringValue.contains(":\t")
					|| stringValue.contains(",")
					|| stringValue.contains("#")
					|| stringValue.contains("\r")
					|| stringValue.contains("\n")) {
				quoteString = true;
			}
		}
		if (quoteString) {
			return "\"" + stringValue
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("/", "\\/")
				.replace("\b", "\\b")
				.replace("\f", "\\f")
				.replace("\r", "\\r")
				.replace("\n", "\\n")
				.replace("\t", "\\t")
				+ "\"";
		} else {
			return stringValue
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("'", "''")
				.replace("/", "\\/")
				.replace("\b", "\\b")
				.replace("\f", "\\f")
				.replace("\r", "\\r")
				.replace("\n", "\\n")
				.replace("\t", "\\t");
		}
	}
}
