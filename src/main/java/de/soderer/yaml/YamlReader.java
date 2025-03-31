package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.soderer.json.utilities.BasicReader;
import de.soderer.json.utilities.DateUtilities;
import de.soderer.json.utilities.NumberUtilities;
import de.soderer.json.utilities.Utilities;
import de.soderer.yaml.directive.TagDirective;
import de.soderer.yaml.directive.VersionDirective;

public class YamlReader extends BasicReader {
	protected boolean documentContentStarted = false;
	protected YamlDocument latestDocument = null;
	protected YamlDocumentList documents = null;
	protected YamlSimpleValue currentYamlSimpleObject = null;
	protected YamlMappingEntry currentYamlMappingEntry = null;
	protected int currentIndentationLevel = 0;
	protected String pendingComment = null;
	protected Integer indentationLevelOverride = null;

	protected Stack<YamlToken> openYamlItems = new Stack<>();
	protected Stack<String> currentYamlPath = new Stack<>();

	public enum YamlToken {
		YamlMapping_Start,
		YamlMapping_PropertyKey,
		YamlMapping_End,
		YamlSequence_Start,
		YamlSequence_Item,
		YamlSequence_End,
		YamlSimpleValue,
		YamlDirective,
		YamlDocument_Start,
		YamlDocument_End,
		YamlComment
	}

	public YamlReader(final InputStream inputStream) throws Exception {
		super(inputStream, null);
	}

	public YamlReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		super(inputStream, encodingCharset);
	}

	public YamlToken getCurrentToken() {
		if (openYamlItems.empty()) {
			return null;
		} else {
			return openYamlItems.peek();
		}
	}

	/**
	 * Read all available Yaml data from the input stream at once.
	 * This can only be done once and as the first action on a YamlReader.
	 *
	 * @return YamlMapping or YamlSequence
	 * @throws Exception
	 */
	public YamlObject<?> read() throws Exception {
		if (getReadCharacters() != 0) {
			throw new Exception("YamlReader position was already initialized for other read operation");
		}

		YamlObject<?> returnObject = null;

		YamlToken nextToken = readNextToken();
		while (nextToken != null) {
			while (nextToken == YamlToken.YamlDirective) {
				nextToken = readNextToken();
			}
			if (nextToken == YamlToken.YamlDocument_Start) {
				nextToken = readNextToken();
			}

			if (nextToken == YamlToken.YamlMapping_Start) {
				returnObject = readYamlMappingWithBrackets();
			} else if (nextToken == YamlToken.YamlMapping_PropertyKey) {
				returnObject = readYamlMapping();
			} else if (nextToken == YamlToken.YamlSequence_Start) {
				returnObject = readYamlSequenceWithBrackets();
			} else if (nextToken == YamlToken.YamlSequence_Item) {
				returnObject = readYamlSequence();
			} else if (nextToken == YamlToken.YamlSimpleValue) {
				returnObject = currentYamlSimpleObject;
			} else if (nextToken == null) {
				throw new Exception("Invalid yaml data: No YAML data found");
			}

			if (latestDocument != null) {
				latestDocument.setValue(returnObject);
			}

			// Look for more YamlDocuments in a sequential Yaml file
			nextToken = readNextToken();
		}

		if (documents != null) {
			return documents;
		} else if (latestDocument != null) {
			return latestDocument;
		} else {
			return returnObject;
		}
	}

	public YamlToken readNextToken() throws Exception {
		final YamlToken yamlToken = readNextTokenInternal(true);

		return yamlToken;
	}

	protected YamlToken readNextTokenInternal(final boolean updateYamlPath) throws Exception {
		currentIndentationLevel = readNextIndentationLevel();

		Character currentChar = readNextNonWhitespace();
		char nextChar;
		if (currentChar == null) {
			if (openYamlItems.size() > 0) {
				throw new Exception("Premature end of data");
			} else {
				return null;
			}
		}

		YamlToken yamlToken;
		switch (currentChar) {
			case '\r':
				// Skip empty line
				yamlToken = readNextTokenInternal(updateYamlPath);
				break;
			case '\n':
				// Skip empty line
				yamlToken = readNextTokenInternal(updateYamlPath);
				break;
			case '#': // Comment
				final String commentLine = readUpToNext(false, null, '\r', '\n').trim();
				currentIndentationLevel = 0;
				if (pendingComment == null) {
					pendingComment = commentLine;
				} else {
					pendingComment = pendingComment + "\n" + commentLine;
				}
				yamlToken = YamlToken.YamlComment;
				break;
			case '%': // Directive
				if (documentContentStarted) {
					throw new Exception("Invalid yaml data '" + currentChar + "' in line " + getReadLines() + " at overall index " + getReadCharacters());
				} else {
					if (latestDocument == null) {
						latestDocument = new YamlDocument();
					}
					yamlToken = YamlToken.YamlDirective;
					try {
						final String yamlDirective = readUpToNext(true, null, '\r', '\n');
						if (yamlDirective.toLowerCase().startsWith("%YAML")) {
							latestDocument.add(new VersionDirective(yamlDirective));
						} else {
							latestDocument.add(new TagDirective(yamlDirective));
						}
					} catch (final Exception e) {
						throw new Exception("Invalid yaml data '" + currentChar + "' in line " + getReadLines() + " at overall index " + getReadCharacters() + ": " + e.getMessage());
					}
				}
				break;
			case '-':
				nextChar = readNextCharacter();
				if (nextChar == ' ') {
					yamlToken = YamlToken.YamlSequence_Item;
				} else if (nextChar == '-') {
					nextChar = readNextCharacter();
					if (nextChar == '-') {
						// End of directives
						if (latestDocument == null) {
							latestDocument = new YamlDocument();
						}
						documentContentStarted = true;
						yamlToken = YamlToken.YamlDocument_Start;
					} else {
						throw new Exception("Invalid yaml data '" + currentChar + nextChar + "' in line " + getReadLines() + " at overall index " + getReadCharacters());
					}
				} else {
					throw new Exception("Invalid yaml data '" + currentChar + nextChar + "' in line " + getReadLines() + " at overall index " + getReadCharacters());
				}
				break;
			case '.':
				nextChar = readNextCharacter();
				if (nextChar == '.') {
					nextChar = readNextCharacter();
					if (nextChar == '.') {
						// End of document
						if (latestDocument != null) {
							documents = new YamlDocumentList();
							documents.add(latestDocument);
						}
						latestDocument = new YamlDocument();
						documentContentStarted = false;
						yamlToken = YamlToken.YamlDocument_End;
					} else {
						throw new Exception("Invalid yaml data '" + currentChar + nextChar + "' in line " + getReadLines() + " at overall index " + getReadCharacters());
					}
				} else {
					throw new Exception("Invalid yaml data '" + currentChar + nextChar + "' in line " + getReadLines() + " at overall index " + getReadCharacters());
				}
				break;
			case '{': // Start YamlMapping
				if (openYamlItems.size() > 0 && openYamlItems.peek() == YamlToken.YamlMapping_PropertyKey) {
					openYamlItems.pop();
				}
				openYamlItems.push(YamlToken.YamlMapping_Start);
				yamlToken = YamlToken.YamlMapping_Start;
				break;
			case '}': // End YamlMapping
				if (openYamlItems.pop() != YamlToken.YamlMapping_Start) {
					throw new Exception("Invalid yaml data '" + currentChar + "' in line " + getReadLines() + " at overall index " + getReadCharacters());
				} else {
					yamlToken = YamlToken.YamlMapping_End;
				}
				break;
			case '[': // Start YamlSequence
				if (openYamlItems.size() > 0 && openYamlItems.peek() == YamlToken.YamlMapping_PropertyKey) {
					openYamlItems.pop();
				}
				openYamlItems.push(YamlToken.YamlSequence_Start);
				yamlToken = YamlToken.YamlSequence_Start;
				break;
			case ']': // End YamlSequence
				if (openYamlItems.pop() != YamlToken.YamlSequence_Start) {
					throw new Exception("Invalid yaml data '" + currentChar + "' in line " + getReadLines() + " at overall index " + getReadCharacters());
				} else {
					yamlToken = YamlToken.YamlSequence_End;
				}
				break;
			case ',': // Separator of YamlMapping properties or YamlSequence items
				if (!updateYamlPath) {
					// Multiple comma
					throw new Exception("Invalid yaml data '" + currentChar + "' in line " + getReadLines() + " at overall index " + getReadCharacters());
				} else {
					currentChar = readNextNonWhitespace();
					if (currentChar == null || currentChar == '}' || currentChar == ']') {
						throw new Exception("Invalid yaml data '" + currentChar + "' in line " + getReadLines() + " at overall index " + getReadCharacters());
					} else {
						reuseCurrentChar();
						yamlToken = readNextTokenInternal(false);
					}
					break;
				}
			case '>':
			case '|':
				// Read scalar block
				final String blockStart = readUpToNext(false, null, '\r', '\n');
				if (!">".equals(blockStart) && !"|".equals(blockStart) && !">-".equals(blockStart) && !"|-".equals(blockStart) && !">+".equals(blockStart) && !"|+".equals(blockStart)) {
					throw new Exception("Invalid text block start: " + blockStart);
				}
				final int blockIndentationLevel = readNextIndentationLevel();
				currentYamlSimpleObject = readScalarBlock(blockIndentationLevel, currentChar == '>');
				currentYamlMappingEntry = null;
				yamlToken = YamlToken.YamlSimpleValue;
				break;
			default: // Simple YamlMapping property value or YamlSequence item
				String text;
				try {
					if (currentChar == '"') {
						text = readQuotedText('\\');
					} else if (currentChar == '\'') {
						text = readQuotedText('\'');
					} else {
						text = readUpToNextString(false, '\\', ",", "\"", "]", ": ", ":\t", ":\r", ":\n", "}", "#", "\r", "\n").trim();
						if (text.replace("''", "_").contains("'")) {
							throw new Exception("Unexpected single quote");
						} else {
							text = text.replace("''", "'");
						}
					}
				} catch (final Exception e) {
					throw new Exception("Invalid yaml data in line " + getReadLines() + " at overall index " + getReadCharacters() + ": " + e.getMessage(), e);
				}

				Character nextCharacter = readNextNonWhitespace();
				if (nextCharacter != null && nextCharacter == ':') {
					nextCharacter = readNextNonWhitespace();
					reuseCurrentChar();
					currentYamlMappingEntry = new YamlMappingEntry().setKey(parseSimpleYamlValue(text)).setComment(pendingComment);
					currentYamlSimpleObject = null;
					yamlToken = YamlToken.YamlMapping_PropertyKey;
					openYamlItems.push(YamlToken.YamlMapping_PropertyKey);
				} else {
					reuseCurrentChar();
					currentYamlSimpleObject = new YamlSimpleValue().setValue(parseSimpleYamlValue(text)).setComment(pendingComment);
					currentYamlMappingEntry = null;
					yamlToken = YamlToken.YamlSimpleValue;
				}

				pendingComment = null;
				break;
		}

		if (updateYamlPath) {
			updateYamlPath(yamlToken);
		}

		return yamlToken;
	}

	private YamlSimpleValue readScalarBlock(final int blockIndentationLevel, final boolean chompLines) throws Exception {
		final YamlSimpleValue returnValue = new YamlSimpleValue();
		final List<String> scalarBlockLines = new ArrayList<>();
		String nextLine = readUpToNext(true, null, '\r', '\n');
		scalarBlockLines.add(nextLine);
		int nextIndentationLevel = readNextIndentationLevel();
		while (nextIndentationLevel == blockIndentationLevel) {
			nextLine = readUpToNext(true, null, '\r', '\n');
			scalarBlockLines.add(nextLine);
			nextIndentationLevel = readNextIndentationLevel();
		}
		if (nextIndentationLevel > 0) {
			indentationLevelOverride = nextIndentationLevel;
		}
		String blockString = "";
		for (final String scalarBlockLine : scalarBlockLines) {
			if (blockString.length() > 0) {
				if (chompLines) {
					blockString += " ";
				} else {
					blockString += "\n";
				}
			}
			blockString += scalarBlockLine.trim();
		}
		returnValue.setValue(blockString);
		return returnValue;
	}

	private static Object parseSimpleYamlValue(final String valueString) {
		if (valueString == null) {
			throw new RuntimeException("Invalid empty yaml data");
		} else if (valueString.startsWith("\"") && valueString.endsWith("\"")) {
			final String returnValue = valueString.replace("\\\"", "\"");
			return returnValue.substring(1, returnValue.length() -1);
		} else if (valueString.startsWith("'") && valueString.endsWith("'")) {
			final String returnValue = valueString.replace("''", "'");
			return returnValue.substring(1, returnValue.length() -1);
		} else if ("null".equalsIgnoreCase(valueString)) {
			return null;
		} else if ("true".equalsIgnoreCase(valueString)) {
			return true;
		} else if ("false".equalsIgnoreCase(valueString)) {
			return false;
		} else if ("yes".equalsIgnoreCase(valueString)) {
			return true;
		} else if ("no".equalsIgnoreCase(valueString)) {
			return false;
		} else if ("y".equalsIgnoreCase(valueString)) {
			return true;
		} else if ("n".equalsIgnoreCase(valueString)) {
			return false;
		} else if ("on".equalsIgnoreCase(valueString)) {
			return true;
		} else if ("off".equalsIgnoreCase(valueString)) {
			return false;
		} else if (NumberUtilities.isNumber(valueString)) {
			return NumberUtilities.parseNumber(valueString);
		} else if (valueString.contains("-") && valueString.contains(":")) {
			try {
				return DateUtilities.parseIso8601DateTimeString(valueString);
			} catch (@SuppressWarnings("unused") final Exception e) {
				return valueString;
			}
		} else if (valueString.contains("-")) {
			try {
				return DateUtilities.parseIso8601DateTimeString(valueString).toLocalDate();
			} catch (@SuppressWarnings("unused") final Exception e) {
				return valueString;
			}
		} else if (valueString.contains(":")) {
			try {
				return DateUtilities.parseLocalTime("HH:mm", valueString);
			} catch (@SuppressWarnings("unused") final Exception e) {
				return valueString;
			}
		} else {
			return valueString;
		}
	}

	private int readNextIndentationLevel() throws IOException {
		if (indentationLevelOverride != null) {
			final int returnBuffer = indentationLevelOverride;
			indentationLevelOverride = null;
			return returnBuffer;
		} else {
			int indentationLevel = 0;
			Character nextCharacter = readNextCharacter();
			while (nextCharacter != null && (nextCharacter == ' ' || nextCharacter == '\t' || nextCharacter == '\r' || nextCharacter == '\n')) {
				if (nextCharacter == '\r' || nextCharacter == '\n') {
					indentationLevel = 0;
				} else if (nextCharacter == ' ' || nextCharacter == '\t') {
					indentationLevel++;
				}
				nextCharacter = readNextCharacter();
			}
			if (nextCharacter != null) {
				reuseCurrentChar();
			}
			return indentationLevel;
		}
	}

	private YamlSequence readYamlSequence() throws Exception {
		final YamlSequence newYamlSequence = new YamlSequence().setStyle(YamlStyle.Standard);
		final int yamlSequenceIndentationLevel = currentIndentationLevel;
		while (true) {
			openYamlItems.push(YamlToken.YamlSequence_Item);

			final YamlObject<?> nextYamlObject = readYamlObject();
			if (openYamlItems.peek() == YamlToken.YamlSequence_Item) {
				newYamlSequence.add(nextYamlObject);
			} else if (openYamlItems.peek() == YamlToken.YamlMapping_PropertyKey) {
				currentYamlMappingEntry.setValue(readYamlObject());
				newYamlSequence.add(currentYamlMappingEntry);
			} else {
				throw new Exception("Invalid internal state: " + openYamlItems.peek());
			}
			openYamlItems.pop();

			final YamlToken nextToken = readNextToken();
			if (nextToken != YamlToken.YamlSequence_Item || yamlSequenceIndentationLevel != currentIndentationLevel) {
				break;
			}
		}
		return newYamlSequence;
	}

	private YamlSequence readYamlSequenceWithBrackets() throws Exception {
		if (openYamlItems.peek() != YamlToken.YamlSequence_Start) {
			throw new Exception("Invalid internal state: " + openYamlItems.peek());
		} else {
			final YamlSequence newYamlSequence = new YamlSequence();
			while (true) {
				final YamlToken nextToken = readNextToken();
				if (nextToken == YamlToken.YamlSequence_End) {
					break;
				}

				if (newYamlSequence.getStyle() == null) {
					if (currentIndentationLevel > 0) {
						newYamlSequence.setStyle(YamlStyle.Bracket);
					} else {
						newYamlSequence.setStyle(YamlStyle.Flow);
					}
				}

				openYamlItems.push(YamlToken.YamlSequence_Item);

				newYamlSequence.add(readYamlObject());

				if (openYamlItems.peek() == YamlToken.YamlSequence_Item) {
					openYamlItems.pop();
				} else {
					throw new Exception("Invalid internal state: " + openYamlItems.peek());
				}
			}
			return newYamlSequence;
		}
	}

	private YamlMapping readYamlMapping() throws Exception {
		if (openYamlItems.peek() != YamlToken.YamlMapping_PropertyKey) {
			throw new Exception("Invalid read position for YamlMapping in line " + getReadLines() + " at overall index " + getReadCharacters());
		} else {
			final YamlMapping newYamlMapping = new YamlMapping().setComment(pendingComment);
			pendingComment = null;

			final int yamlMappingIndentationLevel = currentIndentationLevel;

			YamlToken nextToken = YamlToken.YamlMapping_PropertyKey;
			while (true) {
				openYamlItems.pop();
				final YamlMappingEntry entry = currentYamlMappingEntry;

				nextToken = readNextToken();
				if (nextToken == YamlToken.YamlMapping_Start) {
					entry.setValue(readYamlMappingWithBrackets());
				} else if (nextToken == YamlToken.YamlMapping_PropertyKey) {
					entry.setValue(readYamlMapping());
				} else if (nextToken == YamlToken.YamlSequence_Start) {
					entry.setValue(readYamlSequenceWithBrackets());
				} else if (nextToken == YamlToken.YamlSequence_Item) {
					entry.setValue(readYamlSequence());
				} else if (nextToken == YamlToken.YamlSimpleValue) {
					entry.setValue(currentYamlSimpleObject);
				} else {
					throw new Exception("Invalid yaml data: No YAML data found at root");
				}

				newYamlMapping.add(entry);

				nextToken = readNextToken();
				if (nextToken != YamlToken.YamlMapping_PropertyKey || yamlMappingIndentationLevel != currentIndentationLevel) {
					break;
				}
			}
			return newYamlMapping;
		}
	}

	private YamlMapping readYamlMappingWithBrackets() throws Exception {
		if (openYamlItems.peek() != YamlToken.YamlMapping_Start) {
			throw new Exception("Invalid read position for YamlMapping in line " + getReadLines() + " at overall index " + getReadCharacters());
		} else {
			final YamlMapping newYamlMapping = new YamlMapping();
			while (true) {
				YamlToken nextToken = readNextToken();
				if (nextToken == YamlToken.YamlMapping_End) {
					break;
				} else if (nextToken != YamlToken.YamlMapping_PropertyKey) {
					throw new Exception("Invalid content for YamlMapping");
				}

				if (newYamlMapping.getStyle() == null) {
					if (currentIndentationLevel > 0) {
						newYamlMapping.setStyle(YamlStyle.Bracket);
					} else {
						newYamlMapping.setStyle(YamlStyle.Flow);
					}
				}

				final YamlMappingEntry entry = currentYamlMappingEntry;

				nextToken = readNextToken();
				if (nextToken == YamlToken.YamlMapping_Start) {
					entry.setValue(readYamlMappingWithBrackets());
				} else if (nextToken == YamlToken.YamlMapping_PropertyKey) {
					entry.setValue(readYamlMapping());
				} else if (nextToken == YamlToken.YamlSequence_Start) {
					entry.setValue(readYamlSequenceWithBrackets());
				} else if (nextToken == YamlToken.YamlSequence_Item) {
					entry.setValue(readYamlSequence());
				} else if (nextToken == YamlToken.YamlSimpleValue) {
					entry.setValue(currentYamlSimpleObject);
				} else {
					throw new Exception("Invalid yaml data: No YAML data found at root");
				}

				newYamlMapping.add(entry);



				if (openYamlItems.peek() == YamlToken.YamlMapping_PropertyKey) {
					openYamlItems.pop();
				} else {
					throw new Exception("Invalid internal state: " + openYamlItems.peek());
				}
			}
			return newYamlMapping;
		}
	}

	private YamlObject<?> readYamlObject() throws Exception {
		final YamlToken nextToken = readNextTokenInternal(true);
		switch(nextToken) {
			case YamlComment:
				break;
			case YamlDirective:
				break;
			case YamlDocument_End:
				break;
			case YamlDocument_Start:
				break;
			case YamlMapping_End:
				break;
			case YamlMapping_PropertyKey:
				return currentYamlMappingEntry;
			case YamlMapping_Start:
				break;
			case YamlSequence_End:
				break;
			case YamlSequence_Item:
				break;
			case YamlSequence_Start:
				break;
			case YamlSimpleValue:
				return currentYamlSimpleObject;
			default:
				break;
		}
		// TODO
		return null;
	}

	/**
	 * Read YAML data node by node.
	 * Use "readNextToken" first to init read by node steps
	 */
	public YamlObject<?> readNextYamlNode() throws Exception {
		if (getReadCharacters() == 0) {
			throw new Exception("YamlReader position was not initialized for 'readNextYamlNode'. Use 'readNextToken' or 'readUpToYamlPath' to init.");
		}

		switch (readNextToken()) {
			case YamlMapping_Start:
				return readYamlMapping();
			case YamlSequence_Start:
				return readYamlSequenceWithBrackets();
			case YamlSimpleValue:
				// value was already read
				return currentYamlSimpleObject;
			case YamlMapping_End:
				reuseCurrentChar();
				openYamlItems.push(YamlToken.YamlMapping_Start);
				return null;
			case YamlSequence_End:
				reuseCurrentChar();
				openYamlItems.push(YamlToken.YamlSequence_Start);
				return null;
			case YamlMapping_PropertyKey:
				return currentYamlMappingEntry;
			default:
				throw new Exception("Invalid data in line " + getReadLines() + " at overall index " + getReadCharacters());
		}
	}

	//	private YamlSequenceItem readYamlSequenceItem(final int indentationLevel) throws Exception {
	//		if (openYamlItems.peek() != YamlToken.YamlSequence_Start) {
	//			throw new Exception("Invalid read position for YamlSequence in line " + getReadLines() + " at overall index " + getReadCharacters());
	//		} else {
	//			readNextTokenInternal(true);
	//			return null;
	//
	//			//			final Object nextObject = readYamlSequenceItem(indentationLevel);
	//			//
	//			//			YamlToken nextToken = readNextToken();
	//			//			if (nextToken == YamlToken.YamlSequence_End
	//			//					|| nextToken == YamlToken.YamlMapping_Start
	//			//					|| nextToken == YamlToken.YamlSequence_Start
	//			//					|| nextToken == YamlToken.YamlSimpleValue) {
	//			//				final YamlSequence returnSequence = new YamlSequence();
	//			//				while (nextToken != YamlToken.YamlSequence_End) {
	//			//					if (nextToken == YamlToken.YamlSequence_Start) {
	//			//						returnSequence.add(readYamlSequence());
	//			//					} else if (nextToken == YamlToken.YamlMapping_Start) {
	//			//						returnSequence.add(readYamlMapping());
	//			//					} else if (nextToken == YamlToken.YamlSimpleValue) {
	//			//						returnSequence.add(currentObject);
	//			//					}
	//			//					nextToken = readNextToken();
	//			//				}
	//			//				return returnSequence;
	//			//			} else {
	//			//				throw new Exception("Unexpected YamlToken " + nextToken + " in line " + getReadLines() + " at overall index " + getReadCharacters());
	//			//			}
	//		}
	//	}

	//	private YamlSequence readYamlSequence(int indentationLevel) throws Exception {
	//		final YamlSequence newYamlSequence = new YamlSequence();
	//		while (true) {
	//			YamlObject yamlObject = readYamlObject();
	//			newYamlSequence.add(new YamlSequenceItem().setValue(yamlObject).setComment(pendingComment));
	//			pendingComment = null;
	//			YamlToken nextToken = readNextToken();
	//			if (nextToken != YamlToken.YamlSequence_Item) {
	//				break;
	//			}
	//		}
	//		return newYamlSequence;
	//
	//
	//		//			final YamlSequence newYamlSequence = new YamlSequence();
	//		//			final int sequenceIndentationLevel = currentIndentationLevel;
	//		//			final YamlSequenceItem nextSequenceItem = readYamlSequenceItem(sequenceIndentationLevel);
	//		//			while (nextSequenceItem != null) {
	//		//				newYamlSequence.add(nextSequenceItem);
	//		//				currentIndentationLevel = readNextIndentationLevel();
	//		//				if (currentIndentationLevel < sequenceIndentationLevel) {
	//		//					break;
	//		//				}
	//		//				updateYamlPath(YamlToken.YamlSequence_End);
	//		//			}
	//		//			currentObject = newYamlSequence;
	//
	//
	//		//		if (openYamlItems.peek() != YamlToken.YamlSequence_Start) {
	//		//			throw new Exception("Invalid read position for YamlSequence in line " + getReadLines() + " at overall index " + getReadCharacters());
	//		//		} else {
	//		//			YamlToken nextToken = readNextToken();
	//		//			if (nextToken == YamlToken.YamlSequence_End
	//		//					|| nextToken == YamlToken.YamlMapping_Start
	//		//					|| nextToken == YamlToken.YamlSequence_Start
	//		//					|| nextToken == YamlToken.YamlSimpleValue) {
	//		//				final YamlSequence returnSequence = new YamlSequence();
	//		//				while (nextToken != YamlToken.YamlSequence_End) {
	//		//					if (nextToken == YamlToken.YamlSequence_Start) {
	//		//						returnSequence.add(readYamlSequence());
	//		//					} else if (nextToken == YamlToken.YamlMapping_Start) {
	//		//						returnSequence.add(readYamlMapping());
	//		//					} else if (nextToken == YamlToken.YamlSimpleValue) {
	//		//						returnSequence.add(currentObject);
	//		//					}
	//		//					nextToken = readNextToken();
	//		//				}
	//		//				return returnSequence;
	//		//			} else {
	//		//				throw new Exception("Unexpected YamlToken " + nextToken + " in line " + getReadLines() + " at overall index " + getReadCharacters());
	//		//			}
	//		//		}
	//		return null;
	//	}

	/**
	 * This method should only be used to read small Yaml items
	 *
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public static YamlObject<?> readYamlItemString(final String data) throws Exception {
		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
			try (YamlReader yamlReader = new YamlReader(inputStream)) {
				return yamlReader.read();
			}
		}
	}

	protected void updateYamlPath(final YamlToken yamlToken) throws Exception {
		if (yamlToken != null) {
			switch (yamlToken) {
				case YamlDirective:
					currentYamlPath.clear();
					break;
				case YamlDocument_Start:
					currentYamlPath.clear();
					break;
				case YamlSequence_Start:
					if (currentYamlPath.size() > 0 && currentYamlPath.peek().startsWith("[")) {
						riseSequenceIndex();
					}
					currentYamlPath.push("[]");
					break;
				case YamlSequence_Item:
					// do nothing
					break;
				case YamlSequence_End:
					if (currentYamlPath.size() > 0 && currentYamlPath.peek().startsWith("[")) {
						currentYamlPath.pop();
					}
					if (currentYamlPath.size() > 0 && currentYamlPath.peek().startsWith(".")) {
						currentYamlPath.pop();
					}
					break;
				case YamlMapping_Start:
					if (currentYamlPath.size() > 0 && currentYamlPath.peek().startsWith("[")) {
						riseSequenceIndex();
					}
					break;
				case YamlMapping_PropertyKey:
					currentYamlPath.push("." + currentYamlMappingEntry.getKey());
					break;
				case YamlSimpleValue:
					if (currentYamlPath.size() > 0) {
						if (currentYamlPath.peek().startsWith("[")) {
							riseSequenceIndex();
						} else if (currentYamlPath.peek().startsWith(".")) {
							currentYamlPath.pop();
						}
					}
					break;
				case YamlMapping_End:
					if (currentYamlPath.size() > 0 && currentYamlPath.peek().startsWith(".")) {
						currentYamlPath.pop();
					}
					break;
				default:
					throw new Exception("Invalid yamlToken: " + yamlToken);
			}
		}
	}

	private void riseSequenceIndex() {
		String currentSequenceIndexString = currentYamlPath.pop();
		currentSequenceIndexString = currentSequenceIndexString.substring(1, currentSequenceIndexString.length() - 1);
		int newSequenceIndex = 0;
		if (currentSequenceIndexString.length() > 0) {
			newSequenceIndex = Integer.parseInt(currentSequenceIndexString) + 1;
		}
		currentYamlPath.push("[" + newSequenceIndex + "]");
	}

	/**
	 *
	 * YamlPath syntax:<br />
	 *	$ : root<br />
	 *	. : child separator<br />
	 *	[n] : sequence operator<br />
	 *<br />
	 * YamlPath example:<br />
	 * 	"$.list.customer[0].name"<br />
	 */
	public String getCurrentYamlPath() {
		return "$" + Utilities.join(currentYamlPath, "");
	}
}
