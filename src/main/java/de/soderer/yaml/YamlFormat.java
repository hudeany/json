package de.soderer.yaml;

import de.soderer.json.utilities.Linebreak;
import de.soderer.yaml.data.YamlStringQuoteType;

public class YamlFormat {
	private static Linebreak DEFAULT_LINEBREAK = Linebreak.Unix;
	private static YamlStringQuoteType DEFAULT_STRING_VALUE_QUOTETYPE = YamlStringQuoteType.DOUBLE;

	// TODO: Write with multiline String scalars as quoted text

	private Linebreak linebreak;

	private int indentationSize;

	private YamlStringQuoteType stringValueQuoteType;

	private boolean alwaysQuoteStringKeys;
	private boolean alwaysQuoteStringValues;

	private boolean omitComments;

	private boolean ignoreFlowStyleSettings;

	// TODO
	/**
	 * Use no indentation for YamlSequence items in YamlMapping properties values<br />
	 * Example result when activated<br />
	 * <br />
	 * property:<br />
	 * - item 1<br />
	 * - item 2<br />
	 * <br />
	 * Normal default output:<br />
	 * <br />
	 * property:<br />
	 * &nbsp;&nbsp;- item 1<br />
	 * &nbsp;&nbsp;- item 2<br />
	 */
	private boolean useNoExtraIndentationForYamlSequencesInYamlMappingProperties;

	public YamlFormat() {
		linebreak = DEFAULT_LINEBREAK;
		stringValueQuoteType = DEFAULT_STRING_VALUE_QUOTETYPE;
		indentationSize = 2;
		alwaysQuoteStringKeys = false;
		alwaysQuoteStringValues = false;
		omitComments = false;
		ignoreFlowStyleSettings = false;
		useNoExtraIndentationForYamlSequencesInYamlMappingProperties = false;
	}

	public Linebreak getLinebreak() {
		return linebreak;
	}

	public String getLinebreakString() {
		return linebreak.toString();
	}

	public YamlFormat setLinebreak(final Linebreak linebreak) {
		this.linebreak = linebreak;
		return this;
	}

	public int getIndentationSize() {
		return indentationSize;
	}

	public YamlFormat setIndentationSize(final int indentationSize) {
		this.indentationSize = indentationSize;
		return this;
	}

	public YamlStringQuoteType getStringValueQuoteType() {
		return stringValueQuoteType;
	}

	public YamlFormat setStringValueQuoteType(final YamlStringQuoteType stringValueQuoteType) {
		this.stringValueQuoteType = stringValueQuoteType;
		return this;
	}

	public boolean isAlwaysQuoteStringKeys() {
		return alwaysQuoteStringKeys;
	}

	public YamlFormat setAlwaysQuoteStringKeys(final boolean alwaysQuoteStringKeys) {
		this.alwaysQuoteStringKeys = alwaysQuoteStringKeys;
		return this;
	}

	public boolean isAlwaysQuoteStringValues() {
		return alwaysQuoteStringValues;
	}

	public YamlFormat setAlwaysQuoteStringValues(final boolean alwaysQuoteStringValues) {
		this.alwaysQuoteStringValues = alwaysQuoteStringValues;
		return this;
	}

	public YamlFormat setAlwaysQuoteAllStrings() {
		alwaysQuoteStringKeys = true;
		alwaysQuoteStringValues = true;
		return this;
	}

	public boolean isOmitComments() {
		return omitComments;
	}

	public YamlFormat setOmitComments(final boolean omitComments) {
		this.omitComments = omitComments;
		return this;
	}

	public boolean isIgnoreFlowStyleSettings() {
		return ignoreFlowStyleSettings;
	}

	public YamlFormat setIgnoreFlowStyleSettings(final boolean ignoreFlowStyleSettings) {
		this.ignoreFlowStyleSettings = ignoreFlowStyleSettings;
		return this;
	}

	public boolean isUseNoExtraIndentationForYamlSequencesInYamlMappingProperties() {
		return useNoExtraIndentationForYamlSequencesInYamlMappingProperties;
	}

	public YamlFormat setUseNoExtraIndentationForYamlSequencesInYamlMappingProperties(final boolean useNoExtraIndentationForYamlSequencesInYamlMappingProperties) {
		this.useNoExtraIndentationForYamlSequencesInYamlMappingProperties = useNoExtraIndentationForYamlSequencesInYamlMappingProperties;
		return this;
	}
}
