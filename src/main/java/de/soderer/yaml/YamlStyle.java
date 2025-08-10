package de.soderer.yaml;

public enum YamlStyle {
	Standard,
	Bracket,
	Flow,
	
	/** Sign '>' => converts linebreaks to blanks */
	Block_Folded,
	
	/** Sign '|' => keeps linebreaks */
	Block_Literal
}
