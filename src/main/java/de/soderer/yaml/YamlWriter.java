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
import java.util.Map.Entry;
import java.util.Stack;

import de.soderer.json.utilities.DateUtilities;
import de.soderer.json.utilities.Linebreak;
import de.soderer.json.utilities.Utilities;
import de.soderer.yaml.directive.YamlDirective;

/**
 * TODO
 *   Print references and anchors
 *   Print comments
 *   Utilities Repository : Linebreak enum
 */
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
	private boolean skipNextIndentation = false;

	private Linebreak linebreakType = Linebreak.Unix;
	private String indentation = "  ";
	private String separator = " ";

	private boolean omitComments = false;

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

	public Linebreak getLinebreakType() {
		return linebreakType;
	}

	public YamlWriter setLinebreakType(final Linebreak linebreakType) {
		if (linebreakType == null) {
			this.linebreakType = Linebreak.Unix;
		} else {
			this.linebreakType = linebreakType;
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

	public boolean isOmitComments() {
		return omitComments;
	}

	public YamlWriter setOmitComments(final boolean omitComments) {
		this.omitComments = omitComments;
		return this;
	}

	public long getWrittenCharacters() {
		return writtenCharacters;
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
					write("," + linebreakType.toString(), false);
				} else if (latestOpenYamlItem == YamlStackItem.Sequence_Empty) {
					write(linebreakType.toString(), false);
				} else if (latestOpenYamlItem == YamlStackItem.Mapping_Value) {
					openYamlStackItems.push(YamlStackItem.Mapping_Value);
					write(linebreakType.toString(), false);
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
				write("," + linebreakType.toString(), false);
			} else {
				write(linebreakType.toString(), false);
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
			write(linebreakType.toString(), false);
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
				write("," + linebreakType.toString(), false);
			} else if (latestOpenYamlItem != YamlStackItem.Sequence_Empty) {
				write(linebreakType.toString(), false);
			}
			openYamlStackItems.push(YamlStackItem.Sequence);

			write(getSimpleValueString(arrayValue, null), true);
		}
	}

	public void addSimpleValue(final Object value) throws Exception {
		if (writtenCharacters > 0 || openYamlStackItems.size() != 0) {
			throw new Exception("Not matching empty Yaml output for adding simple value");
		} else {
			write(getSimpleValueString(value, null), true);
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
			write(linebreakType.toString(), false);
			write("]", true);
		}

		if (openYamlStackItems.size() > 0 && openYamlStackItems.peek() == YamlStackItem.Mapping_Value) {
			openYamlStackItems.pop();
		}
	}

	public void add(final YamlNode yamlNode) throws Exception {
		if (yamlNode instanceof YamlSequence) {
			add((YamlSequence) yamlNode);
		} else if (yamlNode instanceof YamlMapping) {
			add((YamlMapping) yamlNode);
		} else if (yamlNode instanceof YamlSimpleValue) {
			add((YamlSimpleValue) yamlNode);
		} else {
			throw new Exception("Unknown yaml object type to add");
		}
	}

	public void add(final YamlValue yamlValue) throws Exception {
		if (yamlValue instanceof YamlSequence) {
			add((YamlSequence) yamlValue);
		} else if (yamlValue instanceof YamlMapping) {
			add((YamlMapping) yamlValue);
		} else if (yamlValue instanceof YamlSimpleValue) {
			add((YamlSimpleValue) yamlValue);
		} else if (yamlValue instanceof YamlDocument) {
			add((YamlDocument) yamlValue);
		} else if (yamlValue instanceof YamlDocumentList) {
			add((YamlDocumentList) yamlValue);
		} else {
			throw new Exception("Unknown yaml object type to add");
		}
	}

	public void add(final YamlDocument yamlDocument) throws Exception {
		YamlUtilities.checkReferencedAnchors((YamlNode) yamlDocument.getValue());

		if (yamlDocument.getDirectives() != null && yamlDocument.getDirectives().size() > 0) {
			for (final YamlDirective<?> yamlDirective : yamlDocument.getDirectives()) {
				if (yamlDirective.getComment() != null) {
					write(YamlUtilities.createMultiLineComment(yamlDirective.getComment(), linebreakType), false);
				}
				if (yamlDirective.getInlineComment() != null) {
					write(yamlDirective.toString() + " " + YamlUtilities.createSingleLineComment(yamlDirective.getInlineComment()) + linebreakType.toString(), false);
				} else {
					write(yamlDirective.toString() + linebreakType.toString(), false);
				}
			}
			if (yamlDocument.getInlineComment() == null) {
				write("---" + linebreakType.toString(), false);
			} else {
				write("--- " + YamlUtilities.createSingleLineComment(yamlDocument.getInlineComment()) + linebreakType.toString(), false);
			}
		} else {
			write("---" + linebreakType.toString(), false);
		}
		add((YamlNode) yamlDocument.getValue());
	}

	public void add(final YamlDocumentList yamlDocumentList) throws Exception {
		final boolean documentIsOpen = false;
		for (final YamlDocument yamlDocument : yamlDocumentList) {
			if (documentIsOpen) {
				if (yamlDocument.getDirectives() != null && yamlDocument.getDirectives().size() > 0) {
					write("..." + linebreakType.toString(), false);
				} else {
					if (yamlDocument.getInlineComment() == null) {
						write("---" + linebreakType.toString(), false);
					} else {
						write("--- " + YamlUtilities.createSingleLineComment(yamlDocument.getInlineComment()) + linebreakType.toString(), false);
					}
				}
			} else if (yamlDocument.getDirectives() == null || yamlDocument.getDirectives().size() == 0) {
				if (yamlDocument.getInlineComment() == null) {
					write("---" + linebreakType.toString(), false);
				} else {
					write("--- " + YamlUtilities.createSingleLineComment(yamlDocument.getInlineComment()) + linebreakType.toString(), false);
				}
			}
			add(yamlDocument);
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
				for (final Entry<YamlNode, YamlNode> entry : yamlMapping.entrySet()) {
					if (mappingBracketStyleString.length() > 0) {
						mappingBracketStyleString += ", ";
					}

					if (entry.getValue() instanceof YamlSequence) {
						write(getSimpleValueString(entry.getKey(), null) + ": ", true);
						add((YamlSequence) entry.getValue());
					} else if (entry.getValue() instanceof YamlMapping) {
						write(getSimpleValueString(entry.getKey(), null) + ": ", true);
						add((YamlMapping) entry.getValue());
					} else if (entry.getValue() instanceof YamlSimpleValue) {
						mappingBracketStyleString += getSimpleValueString(entry.getKey(), null) + ": " + getSimpleValueString(entry.getValue(), entry.getValue().getStyle());
					}
				}
				write("{" + mappingBracketStyleString + "}" + linebreakType.toString(), false);
			} else if (writeStyle == YamlStyle.Bracket) {
				String mappingBracketStyleString = "";
				for (final Entry<YamlNode, YamlNode> entry : yamlMapping.entrySet()) {
					if (mappingBracketStyleString.length() > 0) {
						mappingBracketStyleString += "," + linebreakType.toString();
					}

					if (entry.getValue() instanceof YamlSequence) {
						write(getSimpleValueString(entry.getKey(), null) + ": ", true);
						add((YamlSequence) entry.getValue());
					} else if (entry.getValue() instanceof YamlMapping) {
						write(getSimpleValueString(entry.getKey(), null) + ": ", true);
						add((YamlMapping) entry.getValue());
					} else if (entry.getValue() instanceof YamlSimpleValue) {
						mappingBracketStyleString += Utilities.repeat(indentation, currentIndentationLevel + 1) + getSimpleValueString(entry.getKey(), null) + ": " + getSimpleValueString(entry.getValue(), entry.getValue().getStyle());
					}
				}
				write("{" + linebreakType.toString() + mappingBracketStyleString + linebreakType.toString() + Utilities.repeat(indentation, currentIndentationLevel) + "}" + linebreakType.toString(), !skipNextIndentation);
				skipNextIndentation = false;
			} else {
				for (final Entry<YamlNode, YamlNode> entry : yamlMapping.entrySet()) {
					if (entry.getValue() == null) {
						throw new Exception("Unexpected empty value in YamlMapping: " + entry.getValue().getClass());
					} else if (entry.getValue() instanceof YamlSequence) {
						write(getSimpleValueString(entry.getKey(), null) + ":" + linebreakType.toString() + Utilities.repeat(indentation, currentIndentationLevel), !skipNextIndentation);
						currentIndentationLevel++;
						add((YamlSequence) entry.getValue());
						currentIndentationLevel--;
					} else if (entry.getValue() instanceof YamlMapping) {
						String anchorPart = "";
						if (entry.getValue().getAnchor() != null) {
							anchorPart = " &" + entry.getValue().getAnchor();
						}
						write(getSimpleValueString(entry.getKey(), null) + ":" + anchorPart + linebreakType.toString() + Utilities.repeat(indentation, currentIndentationLevel), !skipNextIndentation);
						currentIndentationLevel++;
						add((YamlMapping) entry.getValue());
						currentIndentationLevel--;
					} else if (entry.getValue() instanceof YamlSimpleValue) {
						String anchorPart = "";
						if (entry.getValue().getAnchor() != null) {
							anchorPart = "&" + entry.getValue().getAnchor() + " ";
						}
						String inlineCommentPart = "";
						if (entry.getValue().getInlineComment() != null) {
							inlineCommentPart = " # " + entry.getValue().getInlineComment();
						}
						write(getSimpleValueString(entry.getKey(), null) + ": " + anchorPart + getSimpleValueString(entry.getValue(), entry.getValue().getStyle()) + inlineCommentPart+ linebreakType.toString(), !skipNextIndentation);
					} else if (entry.getValue() instanceof YamlAnchorReference) {
						write(getSimpleValueString(entry.getKey(), null) + ": *" + entry.getValue().getValue() + linebreakType.toString(), !skipNextIndentation);
					} else {
						throw new Exception("Unexpected object type in YamlMapping: " + entry.getValue().getClass());
					}
					skipNextIndentation = false;
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
				for (final YamlNode sequenceItem : yamlSequence) {
					if (sequenceFlowStyleString.length() > 0) {
						sequenceFlowStyleString += ", ";
					}

					if (sequenceItem instanceof YamlMapping) {
						add((YamlMapping) sequenceItem);
					} else if (sequenceItem instanceof YamlSequence) {
						add((YamlSequence) sequenceItem);
					} else if (sequenceItem instanceof YamlSimpleValue) {
						sequenceFlowStyleString += getSimpleValueString(sequenceItem.getValue(), sequenceItem.getStyle());
					}
				}
				write("[" + sequenceFlowStyleString + "]" + linebreakType.toString(), true);
			} else if (writeStyle == YamlStyle.Bracket) {
				String sequenceBracketStyleString = "";
				for (final YamlNode sequenceItem : yamlSequence) {
					if (sequenceBracketStyleString.length() > 0) {
						sequenceBracketStyleString += "," + linebreakType.toString();
					}

					if (sequenceItem instanceof YamlMapping) {
						add((YamlMapping) sequenceItem);
					} else if (sequenceItem instanceof YamlSequence) {
						add((YamlSequence) sequenceItem);
					} else if (sequenceItem instanceof YamlSimpleValue) {
						sequenceBracketStyleString += Utilities.repeat(indentation, currentIndentationLevel + 1) + getSimpleValueString(sequenceItem.getValue(), sequenceItem.getStyle());
					}
				}
				write("[" + linebreakType.toString() + sequenceBracketStyleString + linebreakType.toString() + Utilities.repeat(indentation, currentIndentationLevel) + "]" + linebreakType.toString(), true);
			} else {
				for (final YamlNode sequenceItem : yamlSequence) {
					if (sequenceItem instanceof YamlMapping) {
						write("- ", true);
						currentIndentationLevel++;
						skipNextIndentation = true;
						add((YamlMapping) sequenceItem);
						currentIndentationLevel--;
					} else if (sequenceItem instanceof YamlSequence) {
						write("-" + linebreakType.toString(), true);
						currentIndentationLevel++;
						add((YamlSequence) sequenceItem);
						currentIndentationLevel--;
					} else if (sequenceItem instanceof YamlSimpleValue) {
						write("- " + getSimpleValueString(sequenceItem.getValue(), sequenceItem.getStyle()) + linebreakType.toString(), true);
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

	private String getSimpleValueString(Object simpleValue, final YamlStyle style) {
		if (simpleValue == null) {
			return "null";
		} else {
			if (simpleValue instanceof YamlSimpleValue) {
				simpleValue = ((YamlSimpleValue) simpleValue).getValue();
			}

			if (simpleValue instanceof Boolean) {
				return Boolean.toString((Boolean) simpleValue);
			} else if (simpleValue instanceof Date) {
				return DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, (Date) simpleValue);
			} else if (simpleValue instanceof LocalDateTime) {
				if (((LocalDateTime) simpleValue).getNano() > 0) {
					return DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT_NO_TIMEZONE, (LocalDateTime) simpleValue);
				} else {
					return DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, (LocalDateTime) simpleValue);
				}
			} else if (simpleValue instanceof LocalDate) {
				return DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, (LocalDate) simpleValue);
			} else if (simpleValue instanceof LocalTime) {
				if (((LocalTime) simpleValue).getSecond() == 0) {
					return DateUtilities.formatDate("HH:mm", (LocalTime) simpleValue);
				} else {
					return DateUtilities.formatDate("HH:mm:ss", (LocalTime) simpleValue);
				}
			} else if (simpleValue instanceof ZonedDateTime) {
				if (((ZonedDateTime) simpleValue).getNano() > 0) {
					return DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_WITH_NANOS_FORMAT, (ZonedDateTime) simpleValue);
				} else {
					return DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, (ZonedDateTime) simpleValue);
				}
			} else if (simpleValue instanceof Number) {
				return simpleValue.toString();
			} else if (simpleValue instanceof String) {
				if (style == YamlStyle.Block_Folded) {
					return formatStringBlockFolded((String) simpleValue);
				} else  if (style == YamlStyle.Block_Literal) {
					return formatStringBlockLiteral((String) simpleValue);
				} else {
					return formatStringValueOutput((String) simpleValue);
				}
			} else {
				return formatStringValueOutput(simpleValue.toString());
			}
		}
	}

	public void add(final YamlSimpleValue yamlSimpleValue) throws Exception {
		if (yamlSimpleValue == null) {
			throw new Exception("Invalid null value added via 'add'. If done by intention use 'addSimpleYamlArrayValue' or 'addSimpleYamlObjectPropertyValue'");
		} else {
			if (yamlSimpleValue.getAnchor() != null) {
				write(yamlSimpleValue.getAnchor(), false);
			}
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
				case Mapping_Flow:
				case Sequence_Flow:
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
	public static String getYamlItemString(final YamlMapping yamlObject, final Linebreak linebreakType, final String indentation, final String separator) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (YamlWriter yamlWriter = new YamlWriter(outputStream, StandardCharsets.UTF_8)) {
			yamlWriter.setLinebreakType(linebreakType);
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
	public static String getYamlItemString(final YamlSequence yamlArray, final Linebreak linebreakType, final String indentation, final String separator) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (YamlWriter yamlWriter = new YamlWriter(outputStream, StandardCharsets.UTF_8)) {
			yamlWriter.setLinebreakType(linebreakType);
			yamlWriter.setIndentation(indentation);
			yamlWriter.setSeparator(separator);
			yamlWriter.add(yamlArray);
		}

		return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
	}

	public static String getYamlItemString(final YamlNode yamlObject) throws Exception {
		return getYamlItemString(yamlObject, "\n", "\t", " ");
	}

	/**
	 * This method should only be used to write small Yaml items
	 *
	 * @param yamlItem
	 * @return
	 * @throws Exception
	 */
	public static String getYamlItemString(final YamlNode yamlObject, final String linebreak, final String indentation, final String separator) throws Exception {
		if (yamlObject instanceof YamlMapping) {
			return getYamlItemString(yamlObject, linebreak, indentation, separator);
		} else if (yamlObject instanceof YamlSequence) {
			return getYamlItemString(yamlObject, linebreak, indentation, separator);
		} else {
			return getYamlItemString(yamlObject, linebreak, indentation, separator);
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

	public String formatStringBlockFolded(final String stringValue) {
		return (">\n" + Utilities.breakTextToMaximumLinelength(stringValue, 80, linebreakType))
				.replace("\r\n", "\n")
				.replace("\r", "\n")
				.replace("\n", "\n" + Utilities.repeat(indentation, currentIndentationLevel + 1));
	}

	public String formatStringBlockLiteral(final String stringValue) {
		return ("|\n" + stringValue)
				.replace("\r\n", "\n")
				.replace("\r", "\n")
				.replace("\n", "\n" + Utilities.repeat(indentation, currentIndentationLevel + 1));
	}
}
