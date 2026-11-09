package de.soderer.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.soderer.yaml.data.YamlAlias;
import de.soderer.yaml.data.YamlNode;
import de.soderer.yaml.data.YamlScalar;

/**
 * Tests {@link YamlReader#readUpToPath(String)}: the path must be found after any kind of
 * preceding value, especially after values, which are empty or no scalar.
 */
@SuppressWarnings("static-method")
@DisplayName("Sequential read by path")
public class YamlReadUpToPathTest {
	/**
	 * Reads the items of the sequence at the path as text.
	 */
	private static List<String> readItems(final String yaml, final String path) throws Exception {
		final List<String> items = new ArrayList<>();
		try (YamlReader reader = new YamlReader(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))) {
			reader.readUpToPath(path);
			YamlNode item;
			while ((item = reader.readNextYamlNode()) != null) {
				if (item instanceof final YamlScalar scalar) {
					items.add(scalar.getValueString());
				} else if (item instanceof final YamlAlias alias) {
					items.add("*" + alias.getTargetAnchorName());
				} else {
					items.add(item.toString());
				}
			}
		}
		return items;
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"a:\n", // empty value
			"a: # comment\n", // empty value with comment
			"a: &anchor\n", // empty value with anchor
			"a: !!str\n", // empty value with tag
			"a: []\n", // empty flow sequence
			"a: {}\n", // empty flow mapping
			"a: [1, {x: 2}]\n", // flow collections
			"a: {x: [1, 2]}\n",
			"a: &al 0\nref: *al\n", // alias value
			"? a\n", // explicit key without value
			"a: |\n  text\n", // block scalar
			"a:\n  x:\n  y:\n", // nested empty values
			"a:\n  - \n  - x\n", // empty sequence item
	})
	@DisplayName("Path is found after preceding values of any kind")
	void pathAfterPrecedingValue(final String precedingYaml) throws Exception {
		assertEquals(List.of("1", "2"), readItems(precedingYaml + "b:\n  - 1\n  - 2\n", "$.b"));
		assertEquals(List.of("1", "2"), readItems(precedingYaml + "b:\n- 1\n- 2\n", "$.b"));
	}

	@Test
	@DisplayName("Nested path after an empty value of a nested mapping")
	void nestedPathAfterEmptyValue() throws Exception {
		assertEquals(List.of("1", "2"), readItems("x:\n  a:\n  b:\n    - 1\n    - 2\n", "$.x.b"));
		assertEquals(List.of("1"), readItems("x:\n  a:\ny:\n  - 1\n", "$.y"));
	}

	@Test
	@DisplayName("Paths with array indexes")
	void arrayIndexes() throws Exception {
		final String yaml = "l:\n  -\n  - - a\n    - b\n  - n: 1\n    items:\n      - c\n  - x: [1, {a: 2}]\n    items:\n      - d\n      - e\n";
		assertEquals(List.of("c"), readItems(yaml, "$.l[2].items"));
		assertEquals(List.of("d", "e"), readItems(yaml, "$.l[3].items"));
	}

	@Test
	@DisplayName("Items following the sequence are not read")
	void stopsAtEndOfSequence() throws Exception {
		assertEquals(List.of("1", "2"), readItems("a:\n  b:\nlist:\n  - 1\n  - 2\nnext: 3\n", "$.list"));
	}

	@Test
	@DisplayName("Aliases in the items refer to anchors read before")
	void aliasItems() throws Exception {
		assertEquals(List.of("*d", "y"), readItems("d: &d\n  x: 1\nitems:\n  - *d\n  - y\n", "$.items"));
	}

	@Test
	@DisplayName("Missing path is reported")
	void missingPath() {
		assertThrows(Exception.class, () -> readItems("a:\nb: 1\n", "$.c"));
	}
}
