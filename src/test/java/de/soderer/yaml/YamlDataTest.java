package de.soderer.yaml;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.yaml.data.YamlMapping;
import de.soderer.yaml.data.YamlSequence;

@SuppressWarnings("static-method")
public class YamlDataTest {
	@Test
	public void testReplaceInYamlMapping() throws Exception {
		final YamlMapping yamlMapping = new YamlMapping();
		yamlMapping.add("property1", "value1");
		yamlMapping.add("property2", "value2");
		yamlMapping.add("property3", "value3");

		Assert.assertEquals("property1: value1\n"
				+ "property2: value2\n"
				+ "property3: value3\n",
				yamlMapping.toString());

		yamlMapping.replace("property2", "valueReplacement");

		Assert.assertEquals("property1: value1\n"
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

		Assert.assertEquals("- item1\n"
				+ "- item2\n"
				+ "- item3\n",
				yamlSequence.toString());

		yamlSequence.removeByIndex(1);

		Assert.assertEquals("- item1\n"
				+ "- item3\n",
				yamlSequence.toString());
	}

	@Test
	public void testInsertInYamlSequence() throws Exception {
		final YamlSequence yamlSequence = new YamlSequence();
		yamlSequence.add("item1");
		yamlSequence.add("item2");
		yamlSequence.add("item3");

		Assert.assertEquals("- item1\n"
				+ "- item2\n"
				+ "- item3\n",
				yamlSequence.toString());

		yamlSequence.insert(2, "item2.5");

		Assert.assertEquals("- item1\n"
				+ "- item2\n"
				+ "- item2.5\n"
				+ "- item3\n",
				yamlSequence.toString());
	}
}
