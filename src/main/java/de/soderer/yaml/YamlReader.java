package de.soderer.yaml;

import java.io.ByteArrayInputStream;
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
import de.soderer.yaml.directive.YamlDirective;

public class YamlReader extends BasicReader {
	private final boolean verboseLog = false;

	private boolean lenient = false;
	private boolean documentContentStarted = false;
	private YamlDocument currentDocument = null;
	private YamlDocumentList documents = null;
	private YamlToken currentToken = null;
	private int currentTokenIndentationLevel = -1;
	private YamlSimpleValue currentYamlSimpleObject = null;
	private YamlNode currentYamlMappingEntryKey = null;
	private String pendingComment = null;
	private String pendingAnchorId = null;

	private final Stack<YamlToken> openYamlItems = new Stack<>();
	private final Stack<String> currentYamlPath = new Stack<>();

	public enum YamlToken {
		YamlMapping_Start,
		YamlMapping_Property,
		YamlMapping_End,
		YamlSequence_Start,
		YamlSequence_Item,
		YamlSequence_End,
		YamlSimpleValue,
		YamlDirective,
		YamlDocument_Start,
		YamlDocument_End,
		YamlComment,
		YamlAnchor,
		YamlReference
	}

	public YamlReader(final InputStream inputStream) throws Exception {
		super(inputStream, null);
		setNormalizeLinebreaks(true);
	}

	public YamlReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		super(inputStream, encodingCharset);
		setNormalizeLinebreaks(true);
	}

	/**
	 * Allow tabs for indentation
	 * @param lenient
	 */

	public boolean isLenient() {
		return lenient;
	}

	/**
	 * Allow tabs for indentation
	 * @param lenient
	 */
	public void setLenient(final boolean lenient) {
		this.lenient = lenient;
	}

	/**
	 * Read all available Yaml data from the input stream at once.
	 * This can only be done once and as the first action on a YamlReader.
	 *
	 * @return YamlMapping or YamlSequence
	 * @throws Exception
	 */
	public YamlValue read() throws Exception {
		if (getReadCharacters() != 0) {
			throw new Exception("YamlReader position was already initialized for other read operation");
		}

		String documentStartComment = null;

		YamlNode returnObject = null;

		readNextToken();
		while (currentToken != null) {
			while (currentToken == YamlToken.YamlComment || currentToken == YamlToken.YamlDirective) {
				readNextToken();
			}
			if (currentToken == YamlToken.YamlDocument_Start) {
				final String yamlDocumentStartLine = readUpToNext(true, null, '\r', '\n');
				if (yamlDocumentStartLine.contains("#")) {
					documentStartComment = yamlDocumentStartLine.substring(yamlDocumentStartLine.indexOf("#") + 1).trim();
				}
				readNextToken();
			}

			while (currentToken == YamlToken.YamlComment) {
				readNextToken();
			}

			if (currentDocument == null && returnObject != null) {
				throw new Exception("Multiple root yaml node for a single yaml document");
			} else if (currentDocument != null && currentDocument.getValue() != null) {
				throw new Exception("Multiple root yaml node for a single yaml document");
			}

			if (currentToken == YamlToken.YamlMapping_Start) {
				returnObject = readYamlMappingWithBrackets();
				readNextToken();
			} else if (currentToken == YamlToken.YamlMapping_Property) {
				returnObject = readYamlMapping(currentTokenIndentationLevel);
			} else if (currentToken == YamlToken.YamlSequence_Start) {
				returnObject = readYamlSequenceWithBrackets();
				readNextToken();
			} else if (currentToken == YamlToken.YamlSequence_Item) {
				returnObject = readYamlSequence();
			} else if (currentToken == YamlToken.YamlSimpleValue) {
				returnObject = currentYamlSimpleObject;
			} else if (currentToken == null) {
				throw new Exception("Invalid yaml data: No YAML data found");
			}

			if (currentDocument != null) {
				currentDocument.setValue(returnObject);
				if (documentStartComment != null) {
					currentDocument.setInlineComment(documentStartComment);
					documentStartComment = null;
				}
				returnObject = null;
			}

			if (currentToken == YamlToken.YamlDocument_Start) {
				if (documents == null) {
					documents = new YamlDocumentList();
					documents.add(currentDocument);
				}
				currentDocument = new YamlDocument();
				documents.add(currentDocument);
				documentContentStarted = true;
			} else if (currentToken == YamlToken.YamlDocument_End) {
				currentDocument = new YamlDocument();
				documentContentStarted = false;
				readNextToken();
			}
		}

		if (documents != null) {
			for (final YamlDocument document : documents) {
				YamlUtilities.checkReferencedAnchors((YamlNode) document.getValue());
			}
			return documents;
		} else if (currentDocument != null) {
			YamlUtilities.checkReferencedAnchors((YamlNode) currentDocument.getValue());
			return currentDocument;
		} else {
			return returnObject;
		}
	}

	public void readNextToken() throws Exception {
		readNextTokenInternal(true);
	}

	protected void readNextTokenInternal(final boolean updateYamlPath) throws Exception {
		Character currentChar = readNextNonWhitespace();
		if (currentChar == null) {
			currentToken = null;
		} else {
			Character nextChar;
			YamlToken yamlToken;
			switch (currentChar) {
				case '\r':
					// Skip empty line
					readNextTokenInternal(updateYamlPath);
					yamlToken = currentToken;
					break;
				case '\n':
					// Skip empty line
					readNextTokenInternal(updateYamlPath);
					yamlToken = currentToken;
					break;
				case '#': // Comment
					final String commentLine = readUpToNext(false, null, '\r', '\n').substring(1).trim();
					if (pendingComment == null) {
						pendingComment = commentLine;
					} else {
						pendingComment = pendingComment + "\n" + commentLine;
					}
					readNextTokenInternal(updateYamlPath);
					yamlToken = currentToken;
					break;
				case '%': // Directive
					if (documentContentStarted) {
						throw new Exception("Invalid yaml data '" + currentChar + "' in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + (getReadCharacters() - 1));
					} else {
						if (currentDocument == null) {
							currentDocument = new YamlDocument();
						}
						yamlToken = YamlToken.YamlDirective;
						try {
							final String yamlDirective = readUpToNext(true, null, '\r', '\n');
							YamlDirective<?> directive;
							if (yamlDirective.toUpperCase().startsWith("%YAML ")) {
								directive = new VersionDirective(yamlDirective.substring(6));
							} else if (yamlDirective.toUpperCase().startsWith("%TAG ")) {
								directive = new TagDirective(yamlDirective.substring(5));
							} else {
								throw new Exception("Unknown yaml directive: " + yamlDirective);
							}
							if (pendingComment != null) {
								directive.setComment(pendingComment);
								pendingComment = null;
							}
							currentDocument.add(directive);
						} catch (final Exception e) {
							throw new Exception("Invalid yaml data '" + currentChar + "' in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + getReadCharacters() + ": " + e.getMessage());
						}
					}
					break;
				case '-':
					nextChar = readNextCharacter();
					if (nextChar == ' ' || nextChar == '\n' || nextChar == '\r' || nextChar == '\t') {
						yamlToken = YamlToken.YamlSequence_Item;
					} else if (nextChar == '-') {
						nextChar = readNextCharacter();
						if (nextChar == '-') {
							// End of directives
							if (currentDocument == null) {
								currentDocument = new YamlDocument();
							}
							documentContentStarted = true;
							yamlToken = YamlToken.YamlDocument_Start;
						} else {
							throw new Exception("Invalid yaml data '" + currentChar + nextChar + "' in line " + (getReadLines() + 1) + " at overall index " + (getReadCharacters() - 1));
						}
					} else {
						if (nextChar != null && Character.isDigit(nextChar)) {
							final String negativeNumberString = "-" + readUpToNext(false, null, ' ', '\t', ':', '#', '\r', '\n');
							nextChar = readNextNonWhitespaceWithinLine();
							if (nextChar != null && nextChar == ':') {
								currentYamlMappingEntryKey = parseSimpleYamlValue(negativeNumberString);
								currentYamlSimpleObject = null;
								yamlToken = YamlToken.YamlMapping_Property;
							} else {
								reuseCurrentChar();
								currentYamlSimpleObject = parseSimpleYamlValue(negativeNumberString);
								if (pendingComment != null) {
									currentYamlSimpleObject.setComment(pendingComment);
									pendingComment = null;
								}
								currentYamlMappingEntryKey = null;
								yamlToken = YamlToken.YamlSimpleValue;
							}
						} else if (nextChar != null && nextChar == '.') {
							final String infinityNumberString = "-" + readUpToNext(false, null, ' ', '\t', ':', '#', '\r', '\n');
							nextChar = readNextNonWhitespaceWithinLine();
							if (nextChar != null && nextChar == ':') {
								currentYamlMappingEntryKey = parseSimpleYamlValue(infinityNumberString);
								currentYamlSimpleObject = null;
								yamlToken = YamlToken.YamlMapping_Property;
							} else {
								reuseCurrentChar();
								currentYamlSimpleObject = parseSimpleYamlValue(infinityNumberString);
								if (pendingComment != null) {
									currentYamlSimpleObject.setComment(pendingComment);
									pendingComment = null;
								}
								currentYamlMappingEntryKey = null;
								yamlToken = YamlToken.YamlSimpleValue;
							}
						} else {
							throw new Exception("Invalid yaml data '" + currentChar + nextChar + "' in line " + (getReadLines() + 1) + " at overall index " + (getReadCharacters() - 1));
						}
					}
					break;
				case '.':
					nextChar = readNextCharacter();
					if (nextChar == '.') {
						nextChar = readNextCharacter();
						if (nextChar == '.') {
							// End of document
							if (documents == null && currentDocument != null) {
								documents = new YamlDocumentList();
								documents.add(currentDocument);
							}
							yamlToken = YamlToken.YamlDocument_End;
						} else {
							throw new Exception("Invalid yaml data '" + currentChar + nextChar + "' in line " + (getReadLines() + 1) + " at overall index " + (getReadCharacters() - 1));
						}
					} else if (nextChar == 'i' || nextChar == 'I') {
						final String infinityNumberString = "-" + readUpToNext(false, null, ' ', '\t', ':', '#', '\r', '\n');
						nextChar = readNextNonWhitespaceWithinLine();
						if (nextChar != null && nextChar == ':') {
							currentYamlMappingEntryKey = parseSimpleYamlValue(infinityNumberString);
							currentYamlSimpleObject = null;
							yamlToken = YamlToken.YamlMapping_Property;
						} else {
							reuseCurrentChar();
							currentYamlSimpleObject = parseSimpleYamlValue(infinityNumberString);
							if (pendingComment != null) {
								currentYamlSimpleObject.setComment(pendingComment);
								pendingComment = null;
							}
							currentYamlMappingEntryKey = null;
							yamlToken = YamlToken.YamlSimpleValue;
						}
					} else if (nextChar == 'n' | nextChar == 'N') {
						final String notNumberString = "-" + readUpToNext(false, null, ' ', '\t', ':', '#', '\r', '\n');
						nextChar = readNextNonWhitespaceWithinLine();
						if (nextChar != null && nextChar == ':') {
							currentYamlMappingEntryKey = parseSimpleYamlValue(notNumberString);
							currentYamlSimpleObject = null;
							yamlToken = YamlToken.YamlMapping_Property;
						} else {
							reuseCurrentChar();
							currentYamlSimpleObject = parseSimpleYamlValue(notNumberString);
							if (pendingComment != null) {
								currentYamlSimpleObject.setComment(pendingComment);
								pendingComment = null;
							}
							currentYamlMappingEntryKey = null;
							yamlToken = YamlToken.YamlSimpleValue;
						}
					} else {
						throw new Exception("Invalid yaml data '" + currentChar + nextChar + "' in line " + (getReadLines() + 1) + " at overall index " + (getReadCharacters() - 1));
					}
					break;
				case '{': // Start YamlMapping
					pushToTokenStack(YamlToken.YamlMapping_Start, null);
					yamlToken = YamlToken.YamlMapping_Start;
					break;
				case '}': // End YamlMapping
					checkTokenStackWithoutPop(YamlToken.YamlMapping_Start);
					yamlToken = YamlToken.YamlMapping_End;
					break;
				case '[': // Start YamlSequence
					pushToTokenStack(YamlToken.YamlSequence_Start, null);
					yamlToken = YamlToken.YamlSequence_Start;
					break;
				case ']': // End YamlSequence
					checkTokenStackWithoutPop(YamlToken.YamlSequence_Start);
					yamlToken = YamlToken.YamlSequence_End;
					break;
				case ',': // Separator of YamlMapping properties or YamlSequence items
					if (!updateYamlPath) {
						// Multiple comma
						throw new Exception("Invalid yaml data '" + currentChar + "' in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + (getReadCharacters() - 1));
					} else {
						currentChar = readNextNonWhitespace();
						if (currentChar == null) {
							throw new Exception("Invalid yaml data '" + currentChar + "' in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + (getReadCharacters() - 1));
						} else {
							reuseCurrentChar();
							readNextTokenInternal(false);
							yamlToken = currentToken;
						}
						break;
					}
				case '>':
				case '|':
					// Read scalar block
					final String blockStart = readUpToNext(true, null, '\r', '\n').trim();
					if (!">".equals(blockStart) && !"|".equals(blockStart) && !">-".equals(blockStart) && !"|-".equals(blockStart) && !">+".equals(blockStart) && !"|+".equals(blockStart)) {
						throw new Exception("Invalid text block start: " + blockStart);
					}
					currentYamlSimpleObject = readScalarBlock(blockStart);
					currentYamlMappingEntryKey = null;
					yamlToken = YamlToken.YamlSimpleValue;
					break;
				case '&': // Anchor for references
					final String newAnchorId = readUpToNext(false, null, ' ', '\t', '\r', '\n', '#').substring(1);
					if (pendingAnchorId != null) {
						throw new Exception("Invalid multiple anchor ids for the same yaml node: '" + pendingAnchorId + "', '" + newAnchorId + "'");
					} else {
						pendingAnchorId = newAnchorId;
					}
					yamlToken = YamlToken.YamlAnchor;
					break;
				case '*': // Reference to anchor
					final String referencedAnchorId = readUpToNext(false, null, '\r', '\n', '#').substring(1);
					yamlToken = YamlToken.YamlReference;
					pendingAnchorId = referencedAnchorId;
					break;
				case '?': // YamlNode as Mapping key
					throw new Exception("YamlNode as Mapping key not implemented yet");
					//					break;
				default: // YamlMapping simple property key or simple YamlSequence item
					currentTokenIndentationLevel = (int) getReadCharactersInCurrentLine() - 1;

					String text;
					try {
						if (currentChar == '"') {
							text = readQuotedText('\\');
						} else if (currentChar == '\'') {
							text = readQuotedText('\'');
						} else {
							if (openYamlItems.size() > 0 && openYamlItems.peek() == YamlToken.YamlSequence_Start) {
								text = readUpToNextString(false, '\\', ",", "\"", "]", ": ", ":\t", ":\r", ":\n", "}", "#", "\r", "\n").trim();
							} else {
								text = readUpToNextString(false, '\\', ",", "\"", ": ", ":\t", ":\r", ":\n", "}", "#", "\r", "\n").trim();
							}
							if (text.replace("''", "_").contains("'")) {
								throw new Exception("Unexpected single quote");
							} else {
								text = text.replace("''", "'");
							}
						}
					} catch (final Exception e) {
						throw new Exception("Invalid yaml data in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + getReadCharacters() + ": " + e.getMessage(), e);
					}

					nextChar = readNextNonWhitespaceWithinLine();
					if (nextChar != null && nextChar == ':') {
						currentYamlMappingEntryKey = parseSimpleYamlValue(text);
						currentYamlSimpleObject = null;

						yamlToken = YamlToken.YamlMapping_Property;
					} else {
						reuseCurrentChar();
						currentYamlSimpleObject = parseSimpleYamlValue(text);
						if (pendingComment != null) {
							currentYamlSimpleObject.setComment(pendingComment);
							pendingComment = null;
						}
						currentYamlMappingEntryKey = null;

						yamlToken = YamlToken.YamlSimpleValue;
					}

					final long readLinesBeforeComment = getReadLines();
					nextChar = readNextNonWhitespaceWithinLine();
					if (nextChar != null && nextChar == '#') {
						final String nextCommentLine = readUpToNext(false, null, '\r', '\n').substring(1).trim();
						if (readLinesBeforeComment == getReadLines()) {
							if (currentYamlSimpleObject != null) {
								currentYamlSimpleObject.setInlineComment(nextCommentLine);
							}
						} else {
							if (pendingComment == null) {
								pendingComment = nextCommentLine;
							} else {
								pendingComment = pendingComment + "\n" + nextCommentLine;
							}
						}
					} else {
						reuseCurrentChar();
					}

					if (pendingAnchorId != null) {
						if (currentYamlSimpleObject != null) {
							currentYamlSimpleObject.setAnchor(pendingAnchorId);
							pendingAnchorId = null;
						}
					}

					break;
			}

			if (updateYamlPath) {
				updateYamlPath(yamlToken);
			}

			currentToken = yamlToken;
			if (verboseLog) {
				System.out.println("next Token: " + NumberUtilities.formatNumber(currentTokenIndentationLevel, 3, '.', null) + " " + currentToken + (currentToken == YamlToken.YamlMapping_Property ? "'" + currentYamlMappingEntryKey + "'" : ""));
			}
		}
	}

	private YamlSimpleValue readScalarBlock(final String blockType) throws Exception {
		final YamlSimpleValue returnValue = new YamlSimpleValue();
		if (blockType.startsWith(">")) {
			returnValue.setStyle(YamlStyle.Block_Folded);
		} else {
			returnValue.setStyle(YamlStyle.Block_Literal);
		}

		final List<String> scalarBlockLines = new ArrayList<>();
		Integer blockIndentationLevel = null;

		StringBuilder currentLine = new StringBuilder();
		boolean isScalarEnd = false;
		while (!isScalarEnd) {
			final Character nextScalarChar = readNextCharacter();
			if (nextScalarChar == null) {
				break;
			} else {
				switch (nextScalarChar) {
					case '\n':
						if (Utilities.isBlank(currentLine.toString())) {
							if (blockIndentationLevel == null) {
								blockIndentationLevel = 0;
							}
							blockIndentationLevel = Math.max(blockIndentationLevel, currentLine.length());
							scalarBlockLines.add("");
						}
						currentLine = new StringBuilder();
						break;
					case ' ':
					case '\t':
						currentLine.append(nextScalarChar);
						break;
					default:
						if (blockIndentationLevel == null) {
							blockIndentationLevel = currentLine.length();
						}
						if (currentLine.length() >= blockIndentationLevel) {
							final String currentLineString = (currentLine.toString() + readUpToNext(false, null, '\r', '\n')).substring(blockIndentationLevel);
							scalarBlockLines.add(currentLineString);
							currentLine = new StringBuilder();

							// Skip linebreak
							readNextCharacter();
						} else {
							reuseCurrentChar();
							isScalarEnd = true;
						}
				}
			}
		}

		String blockString = "";
		boolean isFirstLine = true; // Watch out for leading blank lines
		for (final String scalarBlockLine : scalarBlockLines) {
			if (!isFirstLine) {
				if (blockType.startsWith(">")) {
					blockString += " ";
				} else {
					blockString += "\n";
				}
			}
			blockString += scalarBlockLine;
			isFirstLine = false;
		}
		returnValue.setValue(blockString);
		return returnValue;
	}

	private static YamlSimpleValue parseSimpleYamlValue(final String valueString) {
		Object value;

		if (valueString == null) {
			throw new RuntimeException("Invalid empty yaml data");
		} else if (valueString.startsWith("\"") && valueString.endsWith("\"")) {
			final String returnValue = valueString.replace("\\\"", "\"");
			value = returnValue.substring(1, returnValue.length() -1);
		} else if (valueString.startsWith("'") && valueString.endsWith("'")) {
			final String returnValue = valueString.replace("''", "'");
			value = returnValue.substring(1, returnValue.length() -1);
		} else if ("null".equalsIgnoreCase(valueString)) {
			value = null;
		} else if ("true".equalsIgnoreCase(valueString)) {
			value = true;
		} else if ("false".equalsIgnoreCase(valueString)) {
			value = false;
		} else if ("yes".equalsIgnoreCase(valueString)) {
			value = true;
		} else if ("no".equalsIgnoreCase(valueString)) {
			value = false;
		} else if ("y".equalsIgnoreCase(valueString)) {
			value = true;
		} else if ("n".equalsIgnoreCase(valueString)) {
			value = false;
		} else if ("on".equalsIgnoreCase(valueString)) {
			value = true;
		} else if ("off".equalsIgnoreCase(valueString)) {
			value = false;
		} else if (NumberUtilities.isNumber(valueString)) {
			value = NumberUtilities.parseNumber(valueString);
		} else if (valueString.contains("-") && valueString.contains(":")) {
			try {
				value = DateUtilities.parseIso8601DateTimeString(valueString);
			} catch (@SuppressWarnings("unused") final Exception e) {
				value = valueString;
			}
		} else if (valueString.contains("-")) {
			try {
				value = DateUtilities.parseIso8601DateTimeString(valueString).toLocalDate();
			} catch (@SuppressWarnings("unused") final Exception e) {
				value = valueString;
			}
		} else if (valueString.contains(":")) {
			try {
				value = DateUtilities.parseLocalTime("HH:mm", valueString);
			} catch (@SuppressWarnings("unused") final Exception e) {
				value = valueString;
			}
		} else if (valueString.equalsIgnoreCase(".inf")) {
			value = Float.POSITIVE_INFINITY;
		} else if (valueString.equalsIgnoreCase("-.inf")) {
			value = Float.NEGATIVE_INFINITY;
		} else if (valueString.equalsIgnoreCase(".nan")) {
			value = Float.NaN;
		} else {
			value = valueString;
		}

		final YamlSimpleValue returnValue = new YamlSimpleValue();
		returnValue.setValue(value);
		return returnValue;
	}

	private YamlSequence readYamlSequence() throws Exception {
		if (currentToken != YamlToken.YamlSequence_Item) {
			throw new Exception("Invalid read position for YamlSequence in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + (getReadCharacters() - 1));
		} else {
			final YamlSequence newYamlSequence = new YamlSequence();
			newYamlSequence.setStyle(YamlStyle.Standard);
			if (pendingComment != null) {
				newYamlSequence.setComment(pendingComment);
				pendingComment = null;
			}
			if (pendingAnchorId != null) {
				newYamlSequence.setAnchor(pendingAnchorId);
				pendingAnchorId = null;
			}

			final int yamlSequenceIndentationLevel = currentTokenIndentationLevel;
			long yamlSequenceStartLine = getReadLines();
			if (getCurrentChar() == '\n') {
				yamlSequenceStartLine--;
			}

			while (true) {
				pushToTokenStack(YamlToken.YamlSequence_Item, null);

				readNextToken();

				if (currentToken == YamlToken.YamlMapping_Start) {
					newYamlSequence.add(readYamlMappingWithBrackets());
					readNextToken();
				} else if (currentToken == YamlToken.YamlMapping_Property) {
					newYamlSequence.add(readYamlMapping(currentTokenIndentationLevel));
				} else if (currentToken == YamlToken.YamlSequence_Start) {
					newYamlSequence.add(readYamlSequenceWithBrackets());
					readNextToken();
				} else if (currentToken == YamlToken.YamlSequence_Item) {
					if (yamlSequenceStartLine == getReadLines()) {
						throw new Exception("Invalid complex property value in same line as sequence start");
					} else {
						newYamlSequence.add(readYamlSequence());
					}
				} else if (currentToken == YamlToken.YamlSimpleValue) {
					newYamlSequence.add(currentYamlSimpleObject);
					readNextToken();
				} else if (currentToken == YamlToken.YamlAnchor) {
					readNextToken();
				} else if (currentToken == YamlToken.YamlReference) {
					newYamlSequence.add(new YamlAnchorReference().setValue(pendingAnchorId));
					readNextToken();
				} else {
					throw new Exception("Invalid internal state: " + currentToken);
				}

				checkTokenStack(YamlToken.YamlSequence_Item);

				if (currentToken != YamlToken.YamlSequence_Item || yamlSequenceIndentationLevel != currentTokenIndentationLevel) {
					break;
				}
			}
			return newYamlSequence;
		}
	}

	private YamlSequence readYamlSequenceWithBrackets() throws Exception {
		if (currentToken != YamlToken.YamlSequence_Start) {
			throw new Exception("Invalid read position for YamlSequence in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + (getReadCharacters() - 1));
		} else {
			final YamlSequence newYamlSequence = new YamlSequence();
			if (pendingComment != null) {
				newYamlSequence.setComment(pendingComment);
				pendingComment = null;
			}
			if (pendingAnchorId != null) {
				newYamlSequence.setAnchor(pendingAnchorId);
				pendingAnchorId = null;
			}

			final long sequenceStartLine = getReadLines();

			while (true) {
				readNextToken();
				if (currentToken == YamlToken.YamlSequence_End) {
					break;
				} else {
					checkTokenStackWithoutPop(YamlToken.YamlSequence_Start);

					if (newYamlSequence.getStyle() == null) {
						if (sequenceStartLine == getReadLines()) {
							newYamlSequence.setStyle(YamlStyle.Flow);
						} else {
							newYamlSequence.setStyle(YamlStyle.Bracket);
						}
					}

					if (currentToken == YamlToken.YamlSimpleValue) {
						newYamlSequence.add(currentYamlSimpleObject);
					} else if (currentToken == YamlToken.YamlMapping_Property) {
						newYamlSequence.add(readYamlMapping(currentTokenIndentationLevel));
					} else {
						throw new Exception("Invalid internal state: " + currentToken);
					}
				}
			}
			checkTokenStack(YamlToken.YamlSequence_Start);
			if (newYamlSequence.getStyle() == YamlStyle.Flow) {
				if (sequenceStartLine != getReadLines()) {
					newYamlSequence.setStyle(YamlStyle.Bracket);
				}
			}
			return newYamlSequence;
		}
	}

	private YamlMapping readYamlMapping(final int yamlMappingIndentationLevel) throws Exception {
		if (currentToken != YamlToken.YamlMapping_Property) {
			throw new Exception("Invalid read position for YamlMapping in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + (getReadCharacters() - 1));
		} else {
			final YamlMapping newYamlMapping = new YamlMapping();
			newYamlMapping.setIndentationLevel(yamlMappingIndentationLevel);
			if (pendingComment != null) {
				newYamlMapping.setComment(pendingComment);
				pendingComment = null;
			}
			if (pendingAnchorId != null) {
				newYamlMapping.setAnchor(pendingAnchorId);
				pendingAnchorId = null;
			}

			final long yamlMappingStartLine = getReadLines();

			YamlNode skipForNextToken = null;
			while (true) {
				YamlNode entryKey;
				YamlNode entryValue = null;

				if (skipForNextToken == null) {
					entryKey = currentYamlMappingEntryKey;
					pushToTokenStack(YamlToken.YamlMapping_Property, currentYamlMappingEntryKey.toString());
					readNextToken();
				} else {
					entryKey = skipForNextToken;
					skipForNextToken = null;
				}

				switch (currentToken) {
					case YamlComment:
						if (yamlMappingStartLine == getReadLines()) {
							newYamlMapping.setInlineComment(pendingComment);
							pendingComment = null;
						} else {
							skipForNextToken = entryKey;
						}
						readNextToken();
						break;
					case YamlDirective:
						throw new Exception("TODO");
						//						break;
					case YamlDocument_Start:
						throw new Exception("TODO");
						//						break;
					case YamlDocument_End:
						throw new Exception("TODO");
						//						break;
					case YamlMapping_Start:
						entryValue = readYamlMappingWithBrackets();
						readNextToken();
						break;
					case YamlMapping_Property:
						if (yamlMappingStartLine == getReadLines()) {
							throw new Exception("Invalid complex property value in same line as property key");
						} else {
							entryValue = readYamlMapping(currentTokenIndentationLevel);
						}
						break;
					case YamlMapping_End:
						throw new Exception("Unexpected sequence end sign in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + (getReadCharacters() - 1));
					case YamlSequence_Start:
						final YamlSequence yamlSequenceWithBrackets = readYamlSequenceWithBrackets();
						if (pendingComment != null) {
							yamlSequenceWithBrackets.setInlineComment(pendingComment);
							pendingComment = null;
						}
						entryValue = yamlSequenceWithBrackets;
						readNextToken();
						break;
					case YamlSequence_Item:
						if (yamlMappingStartLine == getReadLines()) {
							throw new Exception("Invalid complex property value in same line as property key");
						} else {
							final YamlSequence yamlSequence = readYamlSequence();
							if (pendingComment != null) {
								yamlSequence.setInlineComment(pendingComment);
								pendingComment = null;
							}
							entryValue = yamlSequence;
						}
						break;
					case YamlSequence_End:
						throw new Exception("TODO");
						//						break;
					case YamlSimpleValue:
						entryValue = currentYamlSimpleObject;
						if (pendingComment != null) {
							currentYamlSimpleObject.setInlineComment(pendingComment);
							pendingComment = null;
						}
						readNextToken();
						break;
					case YamlAnchor:
						skipForNextToken = entryKey;
						readNextToken();
						break;
					case YamlReference:
						final YamlAnchorReference yamlAnchorReference = new YamlAnchorReference().setValue(pendingAnchorId);
						if (pendingComment != null) {
							yamlAnchorReference.setInlineComment(pendingComment);
							pendingComment = null;
						}
						entryValue = yamlAnchorReference;
						readNextToken();
						break;
					default:
						throw new Exception("Invalid yaml data: No YAML data found at root");
				}

				if (skipForNextToken == null) {
					if (entryValue != null) {
						newYamlMapping.put(entryKey, entryValue);
						entryKey = null;
						entryValue = null;
					} else {
						throw new Exception("Missing value for mapping entry");
					}

					checkTokenStack(YamlToken.YamlMapping_Property);

					while (currentToken == YamlToken.YamlComment) {
						readNextToken();
					}

					if (currentToken != YamlToken.YamlMapping_Property || yamlMappingIndentationLevel != currentTokenIndentationLevel) {
						break;
					}
				}
			}

			return newYamlMapping;
		}
	}

	private YamlMapping readYamlMappingWithBrackets() throws Exception {
		if (currentToken != YamlToken.YamlMapping_Start) {
			throw new Exception("Invalid read position for YamlMapping in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + (getReadCharacters() - 1));
		} else {
			final YamlMapping newYamlMapping = new YamlMapping();
			if (pendingComment != null) {
				newYamlMapping.setComment(pendingComment);
				pendingComment = null;
			}
			if (pendingAnchorId != null) {
				newYamlMapping.setAnchor(pendingAnchorId);
				pendingAnchorId = null;
			}

			while (true) {
				readNextToken();
				if (currentToken == YamlToken.YamlMapping_End) {
					break;
				} else if (currentToken != YamlToken.YamlMapping_Property) {
					throw new Exception("Invalid content for YamlMapping");
				} else {
					checkTokenStackWithoutPop(YamlToken.YamlMapping_Start);

					if (newYamlMapping.getStyle() == null) {
						if (currentTokenIndentationLevel > 0) {
							newYamlMapping.setStyle(YamlStyle.Bracket);
						} else {
							newYamlMapping.setStyle(YamlStyle.Flow);
						}
					}

					final YamlNode entryKey = currentYamlMappingEntryKey;
					final YamlNode entryValue;

					readNextToken();
					if (currentToken == YamlToken.YamlMapping_Start) {
						entryValue = readYamlMappingWithBrackets();
					} else if (currentToken == YamlToken.YamlMapping_Property) {
						entryValue = readYamlMapping(currentTokenIndentationLevel);
					} else if (currentToken == YamlToken.YamlSequence_Start) {
						entryValue = readYamlSequenceWithBrackets();
					} else if (currentToken == YamlToken.YamlSequence_Item) {
						entryValue = readYamlSequence();
					} else if (currentToken == YamlToken.YamlSimpleValue) {
						entryValue = currentYamlSimpleObject;
					} else {
						throw new Exception("Invalid yaml data: No YAML data found at root");
					}

					newYamlMapping.put(entryKey, entryValue);
				}
			}
			checkTokenStack(YamlToken.YamlMapping_Start);
			return newYamlMapping;
		}
	}

	/**
	 * Read YAML data node by node.
	 * Use "readNextToken" first to init read by node steps
	 */
	public YamlNode readNextYamlNode() throws Exception {
		if (getReadCharacters() == 0) {
			throw new Exception("YamlReader position was not initialized for 'readNextYamlNode'. Use 'readNextToken' or 'readUpToYamlPath' to init.");
		}

		readNextToken();
		switch (currentToken) {
			case YamlMapping_Start:
				return readYamlMapping(currentTokenIndentationLevel);
			case YamlSequence_Start:
				return readYamlSequenceWithBrackets();
			case YamlSimpleValue:
				// value was already read
				return currentYamlSimpleObject;
			case YamlMapping_End:
				reuseCurrentChar();
				checkTokenStack(YamlToken.YamlMapping_Start);
				return null;
			case YamlSequence_End:
				reuseCurrentChar();
				checkTokenStack(YamlToken.YamlSequence_Start);
				return null;
			case YamlMapping_Property:
				return currentYamlMappingEntryKey;
			case YamlAnchor:
			case YamlComment:
			case YamlDirective:
			case YamlDocument_End:
			case YamlDocument_Start:
			case YamlReference:
			case YamlSequence_Item:
			default:
				throw new Exception("Invalid data in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + (getReadCharacters() - 1));
		}
	}

	//	private YamlSequenceItem readYamlSequenceItem(final int indentationLevel) throws Exception {
	//		if (openYamlItems.peek() != YamlToken.YamlSequence_Start) {
	//			throw new Exception("Invalid read position for YamlSequence in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + (getReadCharacters() - 1));
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
	//			//				throw new Exception("Unexpected YamlToken " + nextToken + " in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + (getReadCharacters() - 1));
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
	//		//			throw new Exception("Invalid read position for YamlSequence in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + (getReadCharacters() - 1));
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
	//		//				throw new Exception("Unexpected YamlToken " + nextToken + " in line " + (getReadLines() + 1) +" starting at position " + (getReadCharactersInCurrentLine() -1) + " at overall index " + (getReadCharacters() - 1));
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
	public static YamlValue readYamlItemString(final String data) throws Exception {
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
				case YamlDocument_End:
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
				case YamlMapping_Property:
					currentYamlPath.push("." + currentYamlMappingEntryKey);
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
				case YamlComment:
					break;
				case YamlAnchor:
					break;
				case YamlReference:
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

	public YamlToken getCurrentToken() {
		if (openYamlItems.empty()) {
			return null;
		} else {
			return openYamlItems.peek();
		}
	}

	private void checkTokenStack(final YamlToken yamlTokenToCheckFor) throws Exception {
		if (openYamlItems.size() > 0 && openYamlItems.peek() == yamlTokenToCheckFor) {
			openYamlItems.pop();
		} else {
			throw new Exception("Invalid internal state: " + openYamlItems.peek());
		}
	}

	private void checkTokenStackWithoutPop(final YamlToken yamlTokenToCheckFor) throws Exception {
		if (openYamlItems.size() == 0) {
			throw new Exception("Invalid internal state: empty stack");
		} else if (openYamlItems.peek() != yamlTokenToCheckFor) {
			throw new Exception("Invalid internal state: " + openYamlItems.peek());
		}
	}

	private void pushToTokenStack(final YamlToken newYamlToken, @SuppressWarnings("unused") final String propertyName) throws Exception {
		openYamlItems.push(newYamlToken);
	}
}
