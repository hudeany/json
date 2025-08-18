package de.soderer.yaml;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public abstract class YamlValue {
	protected Object value;
	protected String comment;
	protected String inlineComment;

	public Object getValue() {
		return value;
	}

	public YamlValue setValue(final Object value) {
		this.value = value;
		return this;
	}

	public String getComment() {
		return comment;
	}

	public YamlValue setComment(final String comment) {
		this.comment = comment;
		return this;
	}

	public YamlValue addCommentLine(final String commentLine) {
		if (comment == null) {
			comment = commentLine;
		} else {
			comment = comment + "\n" + commentLine;
		}
		return this;
	}

	public String getInlineComment() {
		return inlineComment;
	}

	public YamlValue setInlineComment(final String inlineComment) {
		this.inlineComment = inlineComment;
		return this;
	}

	@Override
	public String toString() {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
				YamlWriter writer = new YamlWriter(output, StandardCharsets.UTF_8);) {
			writer.add(this, false);
			writer.flush();
			return new String(output.toByteArray(), StandardCharsets.UTF_8);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}
