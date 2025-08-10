package de.soderer.yaml;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class YamlMapping extends YamlNode {
	private int indentationLevel = -1;

	private final Map<YamlNode, YamlNode> mappingEntries = new LinkedHashMap<>();

	protected int getIndentationLevel() {
		return indentationLevel;
	}

	protected void setIndentationLevel(final int indentationLevel) {
		this.indentationLevel = indentationLevel;
	}

	public YamlMapping put(final YamlNode key, final YamlNode newValue) throws Exception {
		if (keySet().contains(key)) {
			throw new Exception("Duplicate mapping key: " + key);
		} else {
			mappingEntries.put(key, newValue);
			return this;
		}
	}

	public YamlNode remove(final YamlNode key) {
		return mappingEntries.remove(key);
	}

	public Object get(final YamlNode key) {
		return mappingEntries.get(key);
	}

	public Object get(final String keyString) {
		final YamlNode keyObject = new YamlSimpleValue();
		keyObject.setValue(keyString);
		return mappingEntries.get(keyObject);
	}

	public boolean contains(final YamlNode key) {
		return mappingEntries.containsKey(key);
	}

	public Set<YamlNode> keySet() {
		return mappingEntries.keySet();
	}

	public Collection<YamlNode> values() {
		return mappingEntries.values();
	}

	public Set<Entry<YamlNode, YamlNode>> entrySet() {
		return mappingEntries.entrySet();
	}

	public int size() {
		return mappingEntries.size();
	}

	@Override
	public int hashCode() {
		return Objects.hash(mappingEntries, anchor, style, value, comment, inlineComment);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final YamlMapping other = (YamlMapping) obj;
		return Objects.equals(mappingEntries, other.mappingEntries)
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
		//		if (mappingEntries != null) {
		//			for (YamlMappingEntry mappingEntry : mappingEntries) {
		//				anchorIds.addAll(((YamlObject<?>) mappingEntry).getAllAvailableAnchorIds());
		//			}
		//		}
		return anchorIds;
	}

	@Override
	public Set<String> getAllReferencedAnchorIds() {
		final Set<String> referencedAnchorIds = new HashSet<>();
		//		if (mappingEntries != null) {
		//			for (YamlMappingEntry mappingEntry : mappingEntries) {
		//				referencedAnchorIds.addAll(((YamlObject<?>) mappingEntry).getAllReferencedAnchorIds());
		//			}
		//		}
		return referencedAnchorIds;
	}
}
