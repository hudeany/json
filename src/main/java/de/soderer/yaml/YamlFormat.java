package de.soderer.yaml;

import de.soderer.json.utilities.Linebreak;
import de.soderer.yaml.data.YamlStringQuoteType;

/**
 * Output format settings for {@link YamlWriter}.
 * <p>
 * Defaults: Unix linebreaks, indentation of 2 blanks, double quotes for strings that need quotes,
 * comments and empty lines are written, and the flow style of mappings and sequences is kept.
 * </p>
 */
public class YamlFormat {
	/**
	 * Default linebreak.
	 */
	private static final Linebreak DEFAULT_LINEBREAK = Linebreak.Unix;
	/**
	 * Default quote style for string values that need quotes.
	 */
	private static final YamlStringQuoteType DEFAULT_STRING_VALUE_QUOTETYPE = YamlStringQuoteType.DOUBLE;


	/**
	 * Linebreak to write.
	 */
	private Linebreak linebreak;

	/**
	 * Number of blanks per indentation level.
	 */
	private int indentationSize;

	/**
	 * Quote style for string values that need quotes.
	 */
	private YamlStringQuoteType stringValueQuoteType;

	/**
	 * True to quote all string keys.
	 */
	private boolean alwaysQuoteStringKeys;
	/**
	 * True to quote all string values.
	 */
	private boolean alwaysQuoteStringValues;

	/**
	 * True to omit all comments.
	 */
	private boolean omitComments;
	/**
	 * True to omit the empty lines kept from reading.
	 */
	private boolean omitEmptyLines;

	/**
	 * True to write all mappings and sequences in block style.
	 */
	private boolean ignoreFlowStyleSettings;

	/**
	 * Use no indentation for YamlSequence items in YamlMapping properties values<br />
	 * Normal default output:<br />
	 * <pre>
	 * property:
	 * &nbsp;&nbsp;- item 1
	 * &nbsp;&nbsp;- item 2
	 * </pre>
	 * Example result when activated<br />
	 * <pre>
	 * property:
	 * - item 1
	 * - item 2
	 * </pre>
	 */
	private boolean useNoExtraIndentationForYamlSequencesInYamlMappingProperties;

	/**
	 * Use no new line for empty YamlSequence or YamlMapping in YamlMapping properties values<br />
	 * Normal default output:<br />
	 * <pre>
	 * property1: []
	 * property2: {}
	 * </pre>
	 * Example result when deactivated:<br />
	 * <pre>
	 * property1:
	 * &nbsp;&nbsp;[]
	 * property2:
	 * &nbsp;&nbsp;{}
	 * </pre>
	 */
	private boolean useNoNewLineForEmptyYamlObjectInYamlMappingProperties;

	/**
	 * Creates a format with default settings.
	 */
	public YamlFormat() {
		linebreak = DEFAULT_LINEBREAK;
		stringValueQuoteType = DEFAULT_STRING_VALUE_QUOTETYPE;
		indentationSize = 2;
		alwaysQuoteStringKeys = false;
		alwaysQuoteStringValues = false;
		omitComments = false;
		omitEmptyLines = false;
		ignoreFlowStyleSettings = false;
		useNoExtraIndentationForYamlSequencesInYamlMappingProperties = false;
		useNoNewLineForEmptyYamlObjectInYamlMappingProperties = true;
	}

	/**
	 * Returns the linebreak.
	 *
	 * @return the linebreak
	 */
	public Linebreak getLinebreak() {
		return linebreak;
	}

	/**
	 * Returns the linebreak characters.
	 *
	 * @return the linebreak characters, e.g. "\n"
	 */
	public String getLinebreakString() {
		return linebreak.toString();
	}

	/**
	 * Sets the linebreak.
	 *
	 * @param linebreak
	 *            the linebreak
	 * @throws IllegalArgumentException
	 *             if the linebreak is null
	 */
	public void setLinebreak(final Linebreak linebreak) {
		if (linebreak == null) {
			throw new IllegalArgumentException("Linebreak must not be null");
		}
		this.linebreak = linebreak;
	}

	/**
	 * Sets the linebreak.
	 *
	 * @param newLinebreak
	 *            the linebreak
	 * @return this format for chaining
	 * @throws IllegalArgumentException
	 *             if the linebreak is null
	 */
	public YamlFormat withLinebreak(final Linebreak newLinebreak) {
		setLinebreak(newLinebreak);
		return this;
	}

	/**
	 * Returns the number of blanks per indentation level.
	 *
	 * @return the indentation size
	 */
	public int getIndentationSize() {
		return indentationSize;
	}

	/**
	 * Sets the number of blanks per indentation level. Sequence items are padded after the "-", so
	 * that their content starts at the next level, e.g. "-   key: value" for size 4.
	 *
	 * @param indentationSize
	 *            the indentation size, at least 2
	 * @throws IllegalArgumentException
	 *             if the size is less than 2
	 */
	public void setIndentationSize(final int indentationSize) {
		if (indentationSize < 2) {
			// Content of a sequence item "- " starts at the next indentation level, which needs at least 2 characters
			throw new IllegalArgumentException("Invalid YAML indentation size: " + indentationSize + ", minimum is 2");
		}
		this.indentationSize = indentationSize;
	}

	/**
	 * Sets the number of blanks per indentation level.
	 *
	 * @param newIndentationSize
	 *            the indentation size, at least 2
	 * @return this format for chaining
	 * @throws IllegalArgumentException
	 *             if the size is less than 2
	 */
	public YamlFormat withIndentationSize(final int newIndentationSize) {
		setIndentationSize(newIndentationSize);
		return this;
	}

	/**
	 * Returns the quote style for string values that need quotes and have no own quote style.
	 *
	 * @return the quote style
	 */
	public YamlStringQuoteType getStringValueQuoteType() {
		return stringValueQuoteType;
	}

	/**
	 * Sets the quote style for string values that need quotes and have no own quote style. Texts
	 * with control characters are always double quoted.
	 *
	 * @param stringValueQuoteType
	 *            DOUBLE or SINGLE, null or NONE use single quotes
	 */
	public void setStringValueQuoteType(final YamlStringQuoteType stringValueQuoteType) {
		this.stringValueQuoteType = stringValueQuoteType == null ? YamlStringQuoteType.NONE : stringValueQuoteType;
	}

	/**
	 * Sets the quote style for string values that need quotes.
	 *
	 * @param newStringValueQuoteType
	 *            DOUBLE or SINGLE, null or NONE use single quotes
	 * @return this format for chaining
	 */
	public YamlFormat withStringValueQuoteType(final YamlStringQuoteType newStringValueQuoteType) {
		setStringValueQuoteType(newStringValueQuoteType);
		return this;
	}

	/**
	 * Returns whether all string keys are quoted.
	 *
	 * @return true, if all string keys are quoted
	 */
	public boolean isAlwaysQuoteStringKeys() {
		return alwaysQuoteStringKeys;
	}

	/**
	 * Sets whether all string keys are quoted.
	 *
	 * @param alwaysQuoteStringKeys
	 *            true, if all string keys are quoted
	 */
	public void setAlwaysQuoteStringKeys(final boolean alwaysQuoteStringKeys) {
		this.alwaysQuoteStringKeys = alwaysQuoteStringKeys;
	}

	/**
	 * Sets whether all string keys are quoted.
	 *
	 * @param newAlwaysQuoteStringKeys
	 *            true, if all string keys are quoted
	 * @return this format for chaining
	 */
	public YamlFormat withAlwaysQuoteStringKeys(final boolean newAlwaysQuoteStringKeys) {
		setAlwaysQuoteStringKeys(newAlwaysQuoteStringKeys);
		return this;
	}

	/**
	 * Returns whether all string values are quoted.
	 *
	 * @return true, if all string values are quoted
	 */
	public boolean isAlwaysQuoteStringValues() {
		return alwaysQuoteStringValues;
	}

	/**
	 * Sets whether all string values are quoted.
	 *
	 * @param alwaysQuoteStringValues
	 *            true, if all string values are quoted
	 */
	public void setAlwaysQuoteStringValues(final boolean alwaysQuoteStringValues) {
		this.alwaysQuoteStringValues = alwaysQuoteStringValues;
	}

	/**
	 * Sets whether all string values are quoted.
	 *
	 * @param newAlwaysQuoteStringValues
	 *            true, if all string values are quoted
	 * @return this format for chaining
	 */
	public YamlFormat withAlwaysQuoteStringValues(final boolean newAlwaysQuoteStringValues) {
		setAlwaysQuoteStringValues(newAlwaysQuoteStringValues);
		return this;
	}

	/**
	 * Quotes all string keys and values.
	 */
	public void setAlwaysQuoteAllStrings() {
		alwaysQuoteStringKeys = true;
		alwaysQuoteStringValues = true;
	}

	/**
	 * Quotes all string keys and values.
	 *
	 * @return this format for chaining
	 */
	public YamlFormat withAlwaysQuoteAllStrings() {
		setAlwaysQuoteAllStrings();
		return this;
	}

	/**
	 * Returns whether comments are omitted.
	 *
	 * @return true, if comments are omitted
	 */
	public boolean isOmitComments() {
		return omitComments;
	}

	/**
	 * Sets whether comments are omitted.
	 *
	 * @param omitComments
	 *            true, if comments are omitted
	 */
	public void setOmitComments(final boolean omitComments) {
		this.omitComments = omitComments;
	}

	/**
	 * Sets whether comments are omitted.
	 *
	 * @param newOmitComments
	 *            true, if comments are omitted
	 * @return this format for chaining
	 */
	public YamlFormat withOmitComments(final boolean newOmitComments) {
		setOmitComments(newOmitComments);
		return this;
	}

	/**
	 * Returns whether empty lines kept from reading are omitted.
	 *
	 * @return true, if empty lines kept from reading are omitted
	 */
	public boolean isOmitEmptyLines() {
		return omitEmptyLines;
	}

	/**
	 * Sets whether empty lines kept from reading are omitted.
	 *
	 * @param omitEmptyLines
	 *            true, if empty lines kept from reading are omitted
	 */
	public void setOmitEmptyLines(final boolean omitEmptyLines) {
		this.omitEmptyLines = omitEmptyLines;
	}

	/**
	 * Sets whether empty lines kept from reading are omitted.
	 *
	 * @param newOmitEmptyLines
	 *            true, if empty lines kept from reading are omitted
	 * @return this format for chaining
	 */
	public YamlFormat withOmitEmptyLines(final boolean newOmitEmptyLines) {
		setOmitEmptyLines(newOmitEmptyLines);
		return this;
	}

	/**
	 * Returns whether all mappings and sequences are written in block style.
	 *
	 * @return true, if all mappings and sequences are written in block style
	 */
	public boolean isIgnoreFlowStyleSettings() {
		return ignoreFlowStyleSettings;
	}

	/**
	 * Sets whether all mappings and sequences are written in block style.
	 *
	 * @param ignoreFlowStyleSettings
	 *            true, if all mappings and sequences are written in block style
	 */
	public void setIgnoreFlowStyleSettings(final boolean ignoreFlowStyleSettings) {
		this.ignoreFlowStyleSettings = ignoreFlowStyleSettings;
	}

	/**
	 * Sets whether all mappings and sequences are written in block style.
	 *
	 * @param newIgnoreFlowStyleSettings
	 *            true, if all mappings and sequences are written in block style
	 * @return this format for chaining
	 */
	public YamlFormat withIgnoreFlowStyleSettings(final boolean newIgnoreFlowStyleSettings) {
		setIgnoreFlowStyleSettings(newIgnoreFlowStyleSettings);
		return this;
	}

	/**
	 * Use no indentation for YamlSequence items in YamlMapping properties values<br />
	 * Normal default output:<br />
	 * <pre>
	 * property:
	 * &nbsp;&nbsp;- item 1
	 * &nbsp;&nbsp;- item 2
	 * </pre>
	 * Example result when activated<br />
	 * <pre>
	 * property:
	 * - item 1
	 * - item 2
	 * </pre>
	 *
	 * @return true, if no extra indentation is used for sequences in mapping values
	 */
	public boolean isUseNoExtraIndentationForYamlSequencesInYamlMappingProperties() {
		return useNoExtraIndentationForYamlSequencesInYamlMappingProperties;
	}

	/**
	 * Use no indentation for YamlSequence items in YamlMapping properties values<br />
	 * Normal default output:<br />
	 * <pre>
	 * property:
	 * &nbsp;&nbsp;- item 1
	 * &nbsp;&nbsp;- item 2
	 * </pre>
	 * Example result when activated<br />
	 * <pre>
	 * property:
	 * - item 1
	 * - item 2
	 * </pre>
	 *
	 * @param useNoExtraIndentationForYamlSequencesInYamlMappingProperties
	 *            true, if no extra indentation is used for sequences in mapping values
	 */
	public void setUseNoExtraIndentationForYamlSequencesInYamlMappingProperties(final boolean useNoExtraIndentationForYamlSequencesInYamlMappingProperties) {
		this.useNoExtraIndentationForYamlSequencesInYamlMappingProperties = useNoExtraIndentationForYamlSequencesInYamlMappingProperties;
	}

	/**
	 * Sets whether no extra indentation is used for sequences in mapping values, see {@link #setUseNoExtraIndentationForYamlSequencesInYamlMappingProperties(boolean)}.
	 *
	 * @param newUseNoExtraIndentationForYamlSequencesInYamlMappingProperties
	 *            true, if no extra indentation is used for sequences in mapping values
	 * @return this format for chaining
	 */
	public YamlFormat withUseNoExtraIndentationForYamlSequencesInYamlMappingProperties(final boolean newUseNoExtraIndentationForYamlSequencesInYamlMappingProperties) {
		setUseNoExtraIndentationForYamlSequencesInYamlMappingProperties(newUseNoExtraIndentationForYamlSequencesInYamlMappingProperties);
		return this;
	}

	/**
	 * Use no new line for empty YamlSequence or YamlMapping in YamlMapping properties values<br />
	 * Normal default output:<br />
	 * <pre>
	 * property1: []
	 * property2: {}
	 * </pre>
	 * Example result when deactivated:<br />
	 * <pre>
	 * property1:
	 * &nbsp;&nbsp;[]
	 * property2:
	 * &nbsp;&nbsp;{}
	 * </pre>
	 *
	 * @return true, if empty sequences and mappings in mapping values are written on the key line
	 */
	public boolean isUseNoNewLineForEmptyYamlObjectInYamlMappingProperties() {
		return useNoNewLineForEmptyYamlObjectInYamlMappingProperties;
	}

	/**
	 * Use no new line for empty YamlSequence or YamlMapping in YamlMapping properties values<br />
	 * Normal default output:<br />
	 * <pre>
	 * property1: []
	 * property2: {}
	 * </pre>
	 * Example result when deactivated:<br />
	 * <pre>
	 * property1:
	 * &nbsp;&nbsp;[]
	 * property2:
	 * &nbsp;&nbsp;{}
	 * </pre>
	 *
	 * @param useNoNewLineForEmptyYamlObjectInYamlMappingProperties
	 *            true, if empty sequences and mappings in mapping values are written on the key line
	 */
	public void setUseNoNewLineForEmptyYamlObjectInYamlMappingProperties(final boolean useNoNewLineForEmptyYamlObjectInYamlMappingProperties) {
		this.useNoNewLineForEmptyYamlObjectInYamlMappingProperties = useNoNewLineForEmptyYamlObjectInYamlMappingProperties;
	}

	/**
	 * Sets whether empty sequences and mappings in mapping values are written on the key line, see {@link #setUseNoNewLineForEmptyYamlObjectInYamlMappingProperties(boolean)}.
	 *
	 * @param newUseNoNewLineForEmptyYamlObjectInYamlMappingProperties
	 *            true, if empty sequences and mappings in mapping values are written on the key line
	 * @return this format for chaining
	 */
	public YamlFormat withUseNoNewLineForEmptyYamlObjectInYamlMappingProperties(final boolean newUseNoNewLineForEmptyYamlObjectInYamlMappingProperties) {
		setUseNoNewLineForEmptyYamlObjectInYamlMappingProperties(newUseNoNewLineForEmptyYamlObjectInYamlMappingProperties);
		return this;
	}
}
