package de.soderer.yaml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlSequence;

@SuppressWarnings("static-method")
public class YamlDataTest {
	@Test
	public void testEmptySequenceInYamlMapping() throws Exception {
		final YamlMapping yamlMapping = new YamlMapping();
		yamlMapping.add("property1", new YamlSequence());
		yamlMapping.add("property2", new YamlSequence());
		yamlMapping.add("property3", new YamlSequence());

		Assertions.assertEquals("property1:\n  []\n"
				+ "property2:\n  []\n"
				+ "property3:\n  []\n",
				yamlMapping.toString());
	}

	@Test
	public void testEmptyMappingInYamlMapping() throws Exception {
		final YamlMapping yamlMapping = new YamlMapping();
		yamlMapping.add("property1", new YamlMapping());
		yamlMapping.add("property2", new YamlMapping());
		yamlMapping.add("property3", new YamlMapping());

		Assertions.assertEquals("property1:\n  {}\n"
				+ "property2:\n  {}\n"
				+ "property3:\n  {}\n",
				yamlMapping.toString());
	}

	@Test
	public void testReplaceInYamlMapping() throws Exception {
		final YamlMapping yamlMapping = new YamlMapping();
		yamlMapping.add("property1", "value1");
		yamlMapping.add("property2", "value2");
		yamlMapping.add("property3", "value3");

		Assertions.assertEquals("property1: value1\n"
				+ "property2: value2\n"
				+ "property3: value3\n",
				yamlMapping.toString());

		yamlMapping.replace("property2", "valueReplacement");

		Assertions.assertEquals("property1: value1\n"
				+ "property2: valueReplacement\n"
				+ "property3: value3\n",
				yamlMapping.toString());
	}

	@Test
	public void testRemoveByIndexInYamlSequence() throws Exception {
		final YamlSequence yamlSequence = new YamlSequence();
		yamlSequence.add("item1");
		yamlSequence.add("item2");
		yamlSequence.add("item3");

		Assertions.assertEquals("- item1\n"
				+ "- item2\n"
				+ "- item3\n",
				yamlSequence.toString());

		yamlSequence.removeByIndex(1);

		Assertions.assertEquals("- item1\n"
				+ "- item3\n",
				yamlSequence.toString());
	}

	@Test
	public void testInsertInYamlSequence() throws Exception {
		final YamlSequence yamlSequence = new YamlSequence();
		yamlSequence.add("item1");
		yamlSequence.add("item2");
		yamlSequence.add("item3");

		Assertions.assertEquals("- item1\n"
				+ "- item2\n"
				+ "- item3\n",
				yamlSequence.toString());

		yamlSequence.insert(2, "item2.5");

		Assertions.assertEquals("- item1\n"
				+ "- item2\n"
				+ "- item2.5\n"
				+ "- item3\n",
				yamlSequence.toString());
	}
}
