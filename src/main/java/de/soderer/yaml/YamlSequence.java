package de.soderer.yaml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class YamlSequence extends YamlNode implements Iterable<YamlNode> {
	private int indentationLevel = -1;

	private final List<YamlNode> items = new ArrayList<>();

	protected int getIndentationLevel() {
		return indentationLevel;
	}

	protected YamlSequence setIndentationLevel(final int indentationLevel) {
		this.indentationLevel = indentationLevel;
		return this;
	}

	public YamlSequence add(final YamlNode newItem) {
		items.add(newItem);
		return this;
	}

	public Object remove(final Object itemToRemove) {
		for (final YamlNode item : items) {
			if (itemToRemove != null && itemToRemove.equals(item.getValue())) {
				items.remove(item);
				return item;
			}
		}
		return null;
	}

	public Object get(final int index) {
		return items.get(index);
	}

	public int size() {
		return items.size();
	}

	@Override
	public Iterator<YamlNode> iterator() {
		return items.iterator();
	}

	public Stream<YamlNode> stream() {
		return items.stream();
	}

	@Override
	public int hashCode() {
		return Objects.hash(items, anchor, style, value, comment, inlineComment);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final YamlSequence other = (YamlSequence) obj;
		return Objects.equals(items, other.items)
				&& Objects.equals(anchor, other.anchor)
				&& Objects.equals(style, other.style)
				&& Objects.equals(value, other.value)
				&& Objects.equals(comment, other.comment)
				&& Objects.equals(inlineComment, other.inlineComment);
	}

	@Override
	public Set<String> getAllAvailableAnchorIds() {
		final Set<String> anchorIds = new HashSet<>();
		if (getAnchor() != null) {
			anchorIds.add(getAnchor());
		}
		if (items != null) {
			for (final YamlNode item : items) {
				anchorIds.addAll(item.getAllAvailableAnchorIds());
			}
		}
		return anchorIds;
	}

	@Override
	public Set<String> getAllReferencedAnchorIds() {
		final Set<String> referencedAnchorIds = new HashSet<>();
		if (items != null) {
			for (final YamlNode item : items) {
				referencedAnchorIds.addAll(item.getAllReferencedAnchorIds());
			}
		}
		return referencedAnchorIds;
	}

	public YamlSequence add(final Number propertyValue) {
		add(new YamlSimpleValue().setValue(propertyValue));
		return this;
	}

	public YamlSequence add(final Boolean propertyValue) {
		add(new YamlSimpleValue().setValue(propertyValue));
		return this;
	}

	public YamlSequence add(final String propertyValue) {
		add(new YamlSimpleValue().setValue(propertyValue));
		return this;
	}

	@Override
	public YamlSequence setAnchor(final String anchor) throws Exception {
		super.setAnchor(anchor);
		return this;
	}

	@Override
	public YamlSequence setStyle(final YamlStyle style) {
		super.setStyle(style);
		return this;
	}

	@Override
	public YamlSequence setComment(final String comment) {
		super.setComment(comment);
		return this;
	}

	@Override
	public YamlSequence addCommentLine(final String commentLine) {
		super.addCommentLine(commentLine);
		return this;
	}

	@Override
	public YamlSequence setInlineComment(final String inlineComment) {
		super.setInlineComment(inlineComment);
		return this;
	}

	public boolean hasComplexChild() {
		for (final YamlNode item : this) {
			if (!(item instanceof YamlSimpleValue)) {
				return true;
			}
		}
		return false;
	}
}
