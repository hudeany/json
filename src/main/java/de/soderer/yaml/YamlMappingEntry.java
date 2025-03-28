package de.soderer.yaml;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class YamlMappingEntry extends YamlObject<YamlMappingEntry> {
	private Object key;

	public Object getKey() {
		return key;
	}

	public YamlMappingEntry setKey(final Object key) {
		this.key = key;
		return this;
	}
	
	@Override
	public String toString() {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
				YamlWriter writer = new YamlWriter(output, StandardCharsets.UTF_8);) {
			output.write((key + ": ").getBytes(StandardCharsets.UTF_8));
			writer.add(this);
			writer.flush();
			return new String(output.toByteArray(), StandardCharsets.UTF_8);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}
