package de.soderer.yaml;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class YamlObject<T extends YamlObject<T>> {
	private String anchor;
	private Object value;
	private String comment;
	private String inlineComment;
	private YamlStyle style = null;

	public String getAnchor() {
		return anchor;
	}

	@SuppressWarnings("unchecked")
	public T setAnchor(final String anchor) {
		this.anchor = anchor;
		return (T) this;
	}

	public Object getValue() {
		return value;
	}

	@SuppressWarnings("unchecked")
	public T setValue(final Object value) {
		this.value = value;
		return (T) this;
	}

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

	public YamlStyle getStyle() {
		return style;
	}

	@SuppressWarnings("unchecked")
	public T setStyle(YamlStyle style) {
		this.style = style;
		return (T) this;
	}
	
	@Override
	public String toString() {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
				YamlWriter writer = new YamlWriter(output, StandardCharsets.UTF_8);) {
			writer.add(this);
			writer.flush();
			return new String(output.toByteArray(), StandardCharsets.UTF_8);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}
