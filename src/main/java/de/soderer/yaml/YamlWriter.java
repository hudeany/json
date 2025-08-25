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

public class YamlWriter implements Closeable {
	private boolean verboseLog = false;
	public YamlWriter setVerboseLog(final boolean verboseLog) {
		this.verboseLog = verboseLog;
		return this;
	}

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

	public YamlWriter openYamlMapping() throws Exception {
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
		return this;
	}

	public YamlWriter openYamlObjectProperty(final String propertyName) throws Exception {
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
		return this;
	}

	public YamlWriter addSimpleYamlObjectPropertyValue(final Object propertyValue) throws Exception {
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
		return this;
	}

	public YamlWriter closeYamlMapping() throws Exception {
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
		return this;
	}

	public YamlWriter addSimpleYamlArrayValue(final Object arrayValue) throws Exception {
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
		return this;
	}

	public YamlWriter addSimpleValue(final Object value, final boolean initiallyIndent) throws Exception {
		if (writtenCharacters > 0 || openYamlStackItems.size() != 0) {
			throw new Exception("Not matching empty Yaml output for adding simple value");
		} else {
			write(getSimpleValueString(value, null), initiallyIndent);
		}
		return this;
	}

	public YamlWriter closeYamlSequence() throws Exception {
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
		return this;
	}

	private YamlWriter add(final YamlNode yamlNode, final boolean initiallyIndent) throws Exception {
		if (yamlNode instanceof YamlSequence) {
			add((YamlSequence) yamlNode, initiallyIndent);
		} else if (yamlNode instanceof YamlMapping) {
			add((YamlMapping) yamlNode, initiallyIndent);
		} else if (yamlNode instanceof YamlSimpleValue) {
			add((YamlSimpleValue) yamlNode, initiallyIndent);
		} else {
			throw new Exception("Unknown yaml object type to add");
		}
		return this;
	}

	public YamlWriter write(final YamlValue yamlValue) throws Exception {
		add(yamlValue, true);
		write(linebreakType.toString(), false);
		return this;
	}

	private YamlWriter add(final YamlValue yamlValue, final boolean initiallyIndent) throws Exception {
		if (!omitComments && yamlValue.getComment() != null) {
			for (final String commentLine : yamlValue.getComment().replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n")) {
				write("# " + commentLine + linebreakType.toString(), true);
			}
		}
		String inlineCommentPart = "";
		if (!omitComments && yamlValue.getInlineComment() != null) {
			inlineCommentPart = "# " + yamlValue.getInlineComment() + linebreakType.toString();
		}
		write(inlineCommentPart + Utilities.repeat(indentation, currentIndentationLevel), true);

		if (yamlValue instanceof YamlSequence) {
			add((YamlSequence) yamlValue, initiallyIndent);
		} else if (yamlValue instanceof YamlMapping) {
			add((YamlMapping) yamlValue, initiallyIndent);
		} else if (yamlValue instanceof YamlSimpleValue) {
			add((YamlSimpleValue) yamlValue, initiallyIndent);
		} else if (yamlValue instanceof YamlDocument) {
			write((YamlDocument) yamlValue);
		} else if (yamlValue instanceof YamlDocumentList) {
			write((YamlDocumentList) yamlValue);
		} else {
			throw new Exception("Unknown yaml object type to add");
		}
		return this;
	}

	public YamlWriter write(final YamlDocument yamlDocument) throws Exception {
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
		add((YamlNode) yamlDocument.getValue(), true);
		return this;
	}

	public YamlWriter write(final YamlDocumentList yamlDocumentList) throws Exception {
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
			write(yamlDocument);
		}
		return this;
	}

	public YamlWriter write(final YamlMapping yamlMapping) throws Exception {
		add(yamlMapping, true);
		write(linebreakType.toString(), false);
		return this;
	}

	private YamlWriter add(final YamlMapping yamlMapping, final boolean initiallyIndent) throws Exception {
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
			if (writeStyle == YamlStyle.Flow && !yamlMapping.hasChildComments()) {
				write("{", initiallyIndent);
				int itemIndex = 0;
				for (final Entry<YamlNode, YamlNode> entry : yamlMapping.entrySet()) {
					itemIndex++;

					String anchorPart = "";
					if (entry.getValue().getAnchor() != null) {
						anchorPart = " &" + entry.getValue().getAnchor();
					}

					String separatorPart = "";
					if (itemIndex < yamlMapping.size()) {
						separatorPart = ", ";
					}

					if (entry.getValue() instanceof YamlSequence) {
						write(getSimpleValueString(entry.getKey(), null) + ": ", false);
						add(entry.getValue(), false);
					} else if (entry.getValue() instanceof YamlMapping) {
						write(getSimpleValueString(entry.getKey(), null) + ": ", false);
						add(entry.getValue(), false);
					} else if (entry.getValue() instanceof YamlSimpleValue) {
						write(getSimpleValueString(entry.getKey(), null) + ": " + getSimpleValueString(entry.getValue(), entry.getValue().getStyle()), false);
					}
					write(anchorPart + separatorPart, false);
				}
				write("}", false);
			} else if (writeStyle == YamlStyle.Bracket || (writeStyle == YamlStyle.Flow && yamlMapping.hasChildComments())) {
				write("{", initiallyIndent);
				currentIndentationLevel++;
				int itemIndex = 0;
				for (final Entry<YamlNode, YamlNode> entry : yamlMapping.entrySet()) {
					itemIndex++;

					write(linebreakType.toString(), false);

					if (!omitComments && entry.getValue().getComment() != null) {
						for (final String commentLine : entry.getValue().getComment().replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n")) {
							write("# " + commentLine + linebreakType.toString(), true);
						}
					}
					String keyAnchorPart = "";
					if (entry.getKey().getAnchor() != null) {
						keyAnchorPart = " &" + entry.getKey().getAnchor();
					}
					String valueAnchorPart = "";
					if (entry.getValue().getAnchor() != null) {
						valueAnchorPart = " &" + entry.getValue().getAnchor();
					}

					String separatorPart = "";
					if (itemIndex < yamlMapping.size()) {
						separatorPart = ",";
					}

					String keyInlineCommentPart = null;
					if (!omitComments && entry.getKey().getInlineComment() != null) {
						keyInlineCommentPart = " # " + entry.getKey().getInlineComment() + linebreakType.toString();
					}
					String valueInlineCommentPart = "";
					if (!omitComments && entry.getValue().getInlineComment() != null) {
						valueInlineCommentPart = " # " + entry.getValue().getInlineComment() + linebreakType.toString();
					}

					write(getSimpleValueString(entry.getKey(), null) + keyAnchorPart + ": ", true);
					if (keyInlineCommentPart != null) {
						write(keyInlineCommentPart, false);
						if (entry.getValue() instanceof YamlSimpleValue) {
							write(getSimpleValueString(entry.getValue(), entry.getValue().getStyle()), true);
						} else {
							add(entry.getValue(), true);
						}
						write(valueAnchorPart + separatorPart + valueInlineCommentPart, false);
					} else {
						if (entry.getValue() instanceof YamlSimpleValue) {
							write(getSimpleValueString(entry.getValue(), entry.getValue().getStyle()), false);
						} else {
							add(entry.getValue(), false);
						}
						write(valueAnchorPart + separatorPart + valueInlineCommentPart, false);
					}
				}
				currentIndentationLevel--;
				if (yamlMapping.size() > 0) {
					write(linebreakType.toString(), false);
					write("}", true);
				} else {
					write("}", false);
				}
			} else {
				boolean isFirstProperty = true;
				for (final Entry<YamlNode, YamlNode> entry : yamlMapping.entrySet()) {
					if (!isFirstProperty) {
						write(linebreakType.toString(), false);
					}

					if (entry.getValue() == null) {
						throw new Exception("Unexpected empty value in YamlMapping: " + entry.getValue().getClass());
					} else if (entry.getValue() instanceof YamlSequence) {
						String keyAnchorPart = "";
						if (entry.getKey().getAnchor() != null) {
							keyAnchorPart = " &" + entry.getKey().getAnchor();
						}
						String valueAnchorPart = "";
						if (entry.getValue().getAnchor() != null) {
							valueAnchorPart = " &" + entry.getValue().getAnchor();
						}
						String keyInlineCommentPart = null;
						if (!omitComments && entry.getKey().getInlineComment() != null) {
							keyInlineCommentPart = " # " + entry.getKey().getInlineComment() + linebreakType.toString();
						}
						String valueInlineCommentPart = "";
						if (!omitComments && entry.getValue().getInlineComment() != null) {
							valueInlineCommentPart = " # " + entry.getValue().getInlineComment() + linebreakType.toString();
						}

						if (keyInlineCommentPart != null) {
							if ((entry.getValue().getStyle() == YamlStyle.Flow || entry.getValue().getStyle() == YamlStyle.Bracket) && !entry.getValue().hasComments()) {
								write(getSimpleValueString(entry.getKey(), null) + keyAnchorPart + ":", true);
								write(keyInlineCommentPart, false);
								write(valueAnchorPart + valueInlineCommentPart, isFirstProperty ? initiallyIndent : true);
								add((YamlSequence) entry.getValue(), false);
							} else {
								write(getSimpleValueString(entry.getKey(), null) + keyAnchorPart + ":", !skipNextIndentation);
								write(keyInlineCommentPart, false);
								write(valueAnchorPart + valueInlineCommentPart + Utilities.repeat(indentation, currentIndentationLevel), true);
								currentIndentationLevel++;
								if (!omitComments && entry.getValue().getComment() != null) {
									for (final String commentLine : entry.getValue().getComment().replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n")) {
										write("# " + commentLine + linebreakType.toString(), true);
									}
								}
								add(entry.getValue(), true);
								currentIndentationLevel--;
							}
						} else {
							if ((entry.getValue().getStyle() == YamlStyle.Flow || entry.getValue().getStyle() == YamlStyle.Bracket) && !entry.getValue().hasComments()) {
								write(getSimpleValueString(entry.getKey(), null) + keyAnchorPart + ":" + valueAnchorPart + valueInlineCommentPart + " ", isFirstProperty ? initiallyIndent : true);
								add((YamlSequence) entry.getValue(), false);
							} else {
								write(getSimpleValueString(entry.getKey(), null) + keyAnchorPart + ":" + valueAnchorPart + valueInlineCommentPart + Utilities.repeat(indentation, currentIndentationLevel), !skipNextIndentation);
								currentIndentationLevel++;
								if (!omitComments && entry.getValue().getComment() != null) {
									for (final String commentLine : entry.getValue().getComment().replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n")) {
										write("# " + commentLine + linebreakType.toString(), true);
									}
								}
								add(entry.getValue(), true);
								currentIndentationLevel--;
							}
						}
					} else if (entry.getValue() instanceof YamlMapping) {
						String keyAnchorPart = "";
						if (entry.getKey().getAnchor() != null) {
							keyAnchorPart = " &" + entry.getKey().getAnchor();
						}
						String valueAnchorPart = "";
						if (entry.getValue().getAnchor() != null) {
							valueAnchorPart = " &" + entry.getValue().getAnchor();
						}
						String keyInlineCommentPart = null;
						if (!omitComments && entry.getKey().getInlineComment() != null) {
							keyInlineCommentPart = " # " + entry.getKey().getInlineComment() + linebreakType.toString();
						}
						String valueInlineCommentPart = "";
						if (!omitComments && entry.getValue().getInlineComment() != null) {
							valueInlineCommentPart = " # " + entry.getValue().getInlineComment();
						}

						if (keyInlineCommentPart != null) {
							if ((entry.getValue().getStyle() == YamlStyle.Flow || entry.getValue().getStyle() == YamlStyle.Bracket) && !entry.getValue().hasComments()) {
								write(getSimpleValueString(entry.getKey(), null) + keyAnchorPart + ":", isFirstProperty ? initiallyIndent : true);
								write(keyInlineCommentPart, false);
								write(valueAnchorPart + valueInlineCommentPart, true);
								add((YamlMapping) entry.getValue(), false);
							} else {
								write(getSimpleValueString(entry.getKey(), null) + keyAnchorPart + ":", isFirstProperty ? initiallyIndent : true);
								write(keyInlineCommentPart, false);
								write(valueAnchorPart + valueInlineCommentPart + linebreakType.toString(), true);
								currentIndentationLevel++;
								if (!omitComments && entry.getValue().getComment() != null) {
									for (final String commentLine : entry.getValue().getComment().replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n")) {
										write("# " + commentLine + linebreakType.toString(), true);
									}
								}
								add((YamlMapping) entry.getValue(), true);
								currentIndentationLevel--;
							}
						} else {
							if ((entry.getValue().getStyle() == YamlStyle.Flow || entry.getValue().getStyle() == YamlStyle.Bracket) && !entry.getValue().hasComments()) {
								write(getSimpleValueString(entry.getKey(), null) + keyAnchorPart + ":" + valueAnchorPart + valueInlineCommentPart + " ", isFirstProperty ? initiallyIndent : true);
								add((YamlMapping) entry.getValue(), false);
							} else {
								write(getSimpleValueString(entry.getKey(), null) + keyAnchorPart + ":" + valueAnchorPart + valueInlineCommentPart + linebreakType.toString(), isFirstProperty ? initiallyIndent : true);
								currentIndentationLevel++;
								if (!omitComments && entry.getValue().getComment() != null) {
									for (final String commentLine : entry.getValue().getComment().replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n")) {
										write("# " + commentLine + linebreakType.toString(), true);
									}
								}
								add((YamlMapping) entry.getValue(), true);
								currentIndentationLevel--;
							}
						}
					} else if (entry.getValue() instanceof YamlSimpleValue) {
						if (!omitComments && entry.getValue().getComment() != null) {
							for (final String commentLine : entry.getValue().getComment().replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n")) {
								write("# " + commentLine + linebreakType.toString(), true);
							}
						}
						String keyAnchorPart = "";
						if (entry.getKey().getAnchor() != null) {
							keyAnchorPart = " &" + entry.getKey().getAnchor();
						}
						String valueAnchorPart = "";
						if (entry.getValue().getAnchor() != null) {
							valueAnchorPart = " &" + entry.getValue().getAnchor();
						}
						String keyInlineCommentPart = null;
						if (!omitComments && entry.getKey().getInlineComment() != null) {
							keyInlineCommentPart = " # " + entry.getKey().getInlineComment() + linebreakType.toString();
						}
						String valueInlineCommentPart = "";
						if (!omitComments && entry.getValue().getInlineComment() != null) {
							valueInlineCommentPart = " # " + entry.getValue().getInlineComment();
						}
						String valuePart = "";
						final String valueString = getSimpleValueString(entry.getValue(), entry.getValue().getStyle());
						if (valueString != null) {
							final YamlDataType explicitDataType = ((YamlSimpleValue) entry.getValue()).getExplicitDataType();
							if (explicitDataType == YamlDataType.Binary) {
								valuePart = " !!" + explicitDataType.getStorageCode() + " |" + linebreakType.toString() + indentMultilineText(valueString,  Utilities.repeat(indentation, currentIndentationLevel + 1));
							} else if (explicitDataType != null) {
								valuePart = " !!" + explicitDataType.getStorageCode() + " " + valueString;
							} else {
								valuePart = " " + valueString;
							}
						}
						if (keyInlineCommentPart != null) {
							write(getSimpleValueString(entry.getKey(), null) + keyAnchorPart + ":", isFirstProperty ? initiallyIndent : true);
							write(keyInlineCommentPart, false);
							write(valueAnchorPart + valuePart + valueInlineCommentPart, true);
						} else {
							write(getSimpleValueString(entry.getKey(), null) + keyAnchorPart + ":" + valueAnchorPart + valuePart + valueInlineCommentPart, isFirstProperty ? initiallyIndent : true);
						}
					} else if (entry.getValue() instanceof YamlAnchorReference) {
						String keyAnchorPart = "";
						if (entry.getKey().getAnchor() != null) {
							keyAnchorPart = " &" + entry.getKey().getAnchor();
						}
						String valueAnchorPart = "";
						if (entry.getValue().getAnchor() != null) {
							valueAnchorPart = " &" + entry.getValue().getAnchor();
						}
						String keyInlineCommentPart = null;
						if (!omitComments && entry.getKey().getInlineComment() != null) {
							keyInlineCommentPart = " # " + entry.getKey().getInlineComment() + linebreakType.toString();
						}
						String valueInlineCommentPart = "";
						if (!omitComments && entry.getValue().getInlineComment() != null) {
							valueInlineCommentPart = " # " + entry.getValue().getInlineComment();
						}

						final String valuePart = " *" + entry.getValue().getValue();

						if (keyInlineCommentPart != null) {
							write(getSimpleValueString(entry.getKey(), null) + keyAnchorPart + ":", isFirstProperty ? initiallyIndent : true);
							write(keyInlineCommentPart, false);
							write(valueAnchorPart + valuePart + valueInlineCommentPart, true);
						} else {
							write(getSimpleValueString(entry.getKey(), null) + keyAnchorPart + ":" + valueAnchorPart + valuePart + valueInlineCommentPart, isFirstProperty ? initiallyIndent : true);
						}
					} else {
						throw new Exception("Unexpected object type in YamlMapping: " + entry.getValue().getClass());
					}
					isFirstProperty = false;
				}
			}
			if (openYamlStackItems.peek() == YamlStackItem.Mapping) {
				openYamlStackItems.pop();
			} else {
				throw new Exception("Invalid internal state: " + openYamlStackItems.peek());
			}
		}
		return this;
	}

	private static String indentMultilineText(final String valueString, final String indentation) {
		return indentation + valueString.replace("\n", "\n" + indentation);
	}

	public YamlWriter write(final YamlSequence yamlSequence) throws Exception {
		add(yamlSequence, true);
		write(linebreakType.toString(), false);
		return this;
	}

	private YamlWriter add(final YamlSequence yamlSequence, final boolean initiallyIndent) throws Exception {
		if (yamlSequence == null) {
			throw new Exception("Invalid null value added via 'add'. If done by intention use 'addSimpleYamlArrayValue' or 'addSimpleYamlObjectPropertyValue'");
		} else {
			YamlStyle writeStyle;
			if (yamlSequence.size() == 0) {
				writeStyle = YamlStyle.Flow;
			} else if (yamlSequence.getStyle() == null) {
				if (yamlSequence.size() <= 5 && !yamlSequence.hasComplexChild()) {
					writeStyle = YamlStyle.Flow;
				} else {
					writeStyle = YamlStyle.Standard;
				}
			} else {
				writeStyle = yamlSequence.getStyle();
			}

			openYamlStackItems.push(YamlStackItem.Sequence);
			if (writeStyle == YamlStyle.Flow && !yamlSequence.hasChildComments()) {
				write("[", initiallyIndent);
				int itemIndex = 0;
				for (final YamlNode sequenceItem : yamlSequence) {
					itemIndex++;

					String anchorPart = "";
					if (sequenceItem.getAnchor() != null) {
						anchorPart = " &" + sequenceItem.getAnchor();
					}

					String separatorPart = "";
					if (itemIndex < yamlSequence.size()) {
						separatorPart = ", ";
					}

					if (sequenceItem instanceof YamlMapping) {
						add(sequenceItem, false);
					} else if (sequenceItem instanceof YamlSequence) {
						add(sequenceItem, false);
					} else if (sequenceItem instanceof YamlSimpleValue) {
						write(getSimpleValueString(sequenceItem.getValue(), sequenceItem.getStyle()), false);
					}
					write(anchorPart + separatorPart, false);
				}
				write("]", false);
			} else if (writeStyle == YamlStyle.Bracket || (writeStyle == YamlStyle.Flow && yamlSequence.hasChildComments())) {
				write("[", initiallyIndent);
				currentIndentationLevel++;
				int itemIndex = 0;
				for (final YamlNode sequenceItem : yamlSequence) {
					itemIndex++;

					write(linebreakType.toString(), false);

					if (!omitComments && sequenceItem.getComment() != null) {
						for (final String commentLine : sequenceItem.getComment().replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n")) {
							write("# " + commentLine + linebreakType.toString(), true);
						}
					}

					String anchorPart = "";
					if (sequenceItem.getAnchor() != null) {
						anchorPart = " &" + sequenceItem.getAnchor();
					}

					String separatorPart = "";
					if (itemIndex < yamlSequence.size()) {
						separatorPart = ",";
					}

					String inlineCommentPart = "";
					if (!omitComments && sequenceItem.getInlineComment() != null) {
						inlineCommentPart = " # " + sequenceItem.getInlineComment();
					}

					if (sequenceItem instanceof YamlMapping) {
						add(sequenceItem, true);
					} else if (sequenceItem instanceof YamlSequence) {
						add(sequenceItem, true);
					} else if (sequenceItem instanceof YamlSimpleValue) {
						write(getSimpleValueString(sequenceItem.getValue(), sequenceItem.getStyle()), true);
					}
					write(anchorPart + separatorPart + inlineCommentPart, false);
				}
				currentIndentationLevel--;
				if (yamlSequence.size() > 0) {
					write(linebreakType.toString(), false);
					write("]", true);
				} else {
					write("]", false);
				}
			} else {
				boolean isFirstItem = true;
				for (final YamlNode sequenceItem : yamlSequence) {
					if (!isFirstItem) {
						write(linebreakType.toString(), false);
					}

					if (!omitComments && sequenceItem.getComment() != null) {
						for (final String commentLine : sequenceItem.getComment().replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n")) {
							write("# " + commentLine + linebreakType.toString(), true);
						}
					}

					String anchorPart = "";
					if (sequenceItem.getAnchor() != null) {
						anchorPart = " &" + sequenceItem.getAnchor();
					}

					String inlineCommentPart = "";
					if (!omitComments && sequenceItem.getInlineComment() != null) {
						inlineCommentPart = " # " + sequenceItem.getInlineComment();
					}

					if (sequenceItem instanceof YamlMapping) {
						if (inlineCommentPart.length() == 0) {
							if (((YamlMapping) sequenceItem).size() == 0) {
								write("- {}" + anchorPart, true);
							} else {
								write("-" + anchorPart + " ", true);
								currentIndentationLevel++;
								skipNextIndentation = true;
								add((YamlMapping) sequenceItem, false);
								currentIndentationLevel--;
							}
						} else {
							if (((YamlMapping) sequenceItem).size() == 0) {
								write("- {}" + anchorPart + inlineCommentPart, true);
							} else {
								write("-" + anchorPart + inlineCommentPart + linebreakType.toString(), true);
								currentIndentationLevel++;
								skipNextIndentation = false;
								add(sequenceItem, true);
								currentIndentationLevel--;
							}
						}
					} else if (sequenceItem instanceof YamlSequence) {
						if (inlineCommentPart.length() == 0) {
							if (((YamlSequence) sequenceItem).size() == 0) {
								write("- []" + anchorPart, true);
							} else {
								write("-" + anchorPart, true);
								write(linebreakType.toString(), false);
								currentIndentationLevel++;
								add((YamlSequence) sequenceItem, true);
								currentIndentationLevel--;
							}
						} else {
							if (((YamlSequence) sequenceItem).size() == 0) {
								write("- []" + anchorPart + inlineCommentPart, true);
							} else {
								write("-" + anchorPart + inlineCommentPart + linebreakType.toString(), true);
								currentIndentationLevel++;
								add(sequenceItem, true);
								currentIndentationLevel--;
							}
						}
					} else if (sequenceItem instanceof YamlSimpleValue) {
						String valuePart = "";
						final String valueString = getSimpleValueString(sequenceItem.getValue(), sequenceItem.getStyle());
						if (valueString != null) {
							valuePart = " " + valueString;
						}
						write("-" + valuePart + anchorPart + inlineCommentPart, true);
					} else {
						throw new Exception("Unexpected object type in YamlSequence");
					}
					isFirstItem = false;
				}
			}
			if (openYamlStackItems.peek() == YamlStackItem.Sequence) {
				openYamlStackItems.pop();
			} else {
				throw new Exception("Invalid internal state: " + openYamlStackItems.peek());
			}
		}
		return this;
	}

	private String getSimpleValueString(Object simpleValue, final YamlStyle style) {
		if (simpleValue == null) {
			return "null";
		} else {
			if (simpleValue instanceof YamlSimpleValue) {
				simpleValue = ((YamlSimpleValue) simpleValue).getValue();
			}

			if (simpleValue == null) {
				return null;
			} else if (simpleValue instanceof Boolean) {
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
			} else if (simpleValue instanceof byte[]) {
				return Utilities.encodeBase64((byte[]) simpleValue, 80, linebreakType.toString());
			} else {
				return formatStringValueOutput(simpleValue.toString());
			}
		}
	}

	private YamlWriter add(final YamlSimpleValue yamlSimpleValue, final boolean initiallyIndent) throws Exception {
		if (yamlSimpleValue == null) {
			throw new Exception("Invalid null value added via 'add'. If done by intention use 'addSimpleYamlArrayValue' or 'addSimpleYamlObjectPropertyValue'");
		} else {
			if (yamlSimpleValue.getAnchor() != null) {
				write(yamlSimpleValue.getAnchor(), false);
			}
			addSimpleValue(yamlSimpleValue.getValue(), initiallyIndent);
		}
		return this;
	}

	public YamlWriter closeAllOpenYamlItems() throws Exception {
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
		return this;
	}

	/**
	 * Flush buffered data.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public YamlWriter flush() throws IOException {
		if (outputWriter != null) {
			outputWriter.flush();
		}
		return this;
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

	private YamlWriter write(final String text, final boolean indent) throws IOException {
		if (outputWriter == null) {
			if (outputStream == null) {
				throw new IllegalStateException("YamlWriter is already closed");
			}
			outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, encoding));
		}

		final String dataToWrite = (indent ? Utilities.repeat(indentation, currentIndentationLevel) : "") + text;
		writtenCharacters += dataToWrite.length();
		outputWriter.write(dataToWrite);
		if (verboseLog) {
			System.out.print(dataToWrite);
		}
		return this;
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
			yamlWriter.add(yamlObject, false);
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
			yamlWriter.add(yamlArray, false);
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
			yamlWriter.add(yamlObject, false);
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
			yamlWriter.add(yamlArray, false);
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
					|| stringValue.contains("\n")
					|| stringValue.contains("{")
					|| stringValue.contains("}")
					|| stringValue.contains("[")
					|| stringValue.contains("]")) {
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
