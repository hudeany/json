package de.soderer.yaml.data.directive;

public class YamlDirective<T extends YamlDirective<T>> {
	private String comment;
	private String inlineComment;

	public String getComment() {
		return comment;
	}

	@SuppressWarnings("unchecked")
	public T setComment(final String comment) {
		this.comment = comment;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T addCommentLine(final String commentLine) {
		if (comment == null) {
			comment = commentLine;
		} else {
			comment = comment + "\n" + commentLine;
		}
		return (T) this;
	}

	public String getInlineComment() {
		return inlineComment;
	}

	@SuppressWarnings("unchecked")
	public T setInlineComment(final String inlineComment) {
		this.inlineComment = inlineComment;
		return (T) this;
	}
}
