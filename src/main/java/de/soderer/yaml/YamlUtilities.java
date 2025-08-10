package de.soderer.yaml;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Set;

import de.soderer.json.utilities.Linebreak;

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

		yamlReader.readNextToken();
		while (yamlReader.getCurrentToken() != null && !yamlReader.getCurrentYamlPath().equals(yamlPath)) {
			yamlReader.readNextToken();
		}

		if (!yamlReader.getCurrentYamlPath().equals(yamlPath)) {
			throw new Exception("Path '" + yamlPath + "' is not part of the YAML data");
		}
	}

	public static YamlValue validateYaml(final byte[] yamlData, final Charset encoding) throws Exception {
		try (YamlReader yamlReader = new YamlReader(new ByteArrayInputStream(yamlData), encoding)) {
			return yamlReader.read();
		}
	}
	
	public static void checkReferencedAnchors(YamlNode yamlObject) throws Exception {
		Set<String> availableAnchors = yamlObject.getAllAvailableAnchorIds();
		Set<String> referencedAnchors = yamlObject.getAllReferencedAnchorIds();
		for (String referencedAnchor : referencedAnchors) {
			if (!availableAnchors.contains(referencedAnchor)) {
				throw new Exception("Anchor id '" + referencedAnchor + "' is referenced but not defined");
			}
		}
	}

	public static String createMultiLineComment(String comment, Linebreak linebreakType) {
		String multiLineComment = "";
		for (String commentLine : comment.replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n")) {
			multiLineComment += "# " + commentLine + linebreakType.toString();
		}
		return multiLineComment;
	}

	public static String createSingleLineComment(String comment) {
		return "# " + comment.replaceAll("\r\n", " ").replaceAll("\r", " ").replaceAll("\r", " ");
	}
}
