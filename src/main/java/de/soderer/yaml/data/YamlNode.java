package de.soderer.yaml.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Basisklasse aller YAML-AST-Knoten.
 * Unterstützt:
 *  - Leading Comments
 *  - Inline Comments
 *  - Anchor-Namen (&id)
 */
public abstract class YamlNode {

	// Kommentare, die direkt vor diesem Node stehen
	private final List<String> leadingComments = new ArrayList<>();

	// Kommentare, die in derselben Zeile nach dem Node stehen könnten
	private final List<String> inlineComments = new ArrayList<>();

	// Anchor-Name, falls dieser Node mit &id markiert wurde
	private String anchorName;

	// ---------------------------------------------------------
	// Kommentare
	// ---------------------------------------------------------

	public List<String> getLeadingComments() {
		return leadingComments;
	}

	public void addLeadingComment(final String comment) {
		if (comment != null && !comment.isEmpty()) {
			leadingComments.add(comment);
		}
	}

	public List<String> getInlineComments() {
		return inlineComments;
	}

	public void addInlineComment(final String comment) {
		if (comment != null && !comment.isEmpty()) {
			inlineComments.add(comment);
		}
	}

	// ---------------------------------------------------------
	// Anchor
	// ---------------------------------------------------------

	public String getAnchorName() {
		return anchorName;
	}

	public void setAnchorName(final String anchorName) {
		this.anchorName = anchorName;
	}
}
