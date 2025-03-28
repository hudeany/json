package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

public class YamlUtilities {
	/**
	 * YamlPath syntax:<br />
	 *	$ : root<br />
	 *	. or / : child separator<br />
	 *	[n] : array operator<br />
	 *<br />
	 * YamlPath example:<br />
	 * 	"$.list.customer[0].name"<br />
	 *
	 * @param yamlReader
	 * @param yamlPath
	 * @throws Exception
	 */
	public static void readUpToYamlPath(final YamlReader yamlReader, String yamlPath) throws Exception {
		if (yamlPath.startsWith("/") || yamlPath.startsWith("$")) {
			yamlPath = yamlPath.substring(1);
		}
		if (yamlPath.endsWith("/")) {
			yamlPath = yamlPath.substring(0, yamlPath.length() - 1);
		}
		yamlPath = "$" + yamlPath.replace("/", ".");

		while (yamlReader.readNextToken() != null && !yamlReader.getCurrentYamlPath().equals(yamlPath)) {
			// Do nothing
		}

		if (!yamlReader.getCurrentYamlPath().equals(yamlPath)) {
			throw new Exception("Path '" + yamlPath + "' is not part of the YAML data");
		}
	}

	public static YamlObject<?> validateYaml(final byte[] yamlData, final Charset encoding) throws Exception {
		try (YamlReader yamlReader = new YamlReader(new ByteArrayInputStream(yamlData), encoding)) {
			return yamlReader.read();
		}
	}
}
