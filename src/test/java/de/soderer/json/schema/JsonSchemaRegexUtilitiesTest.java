package de.soderer.json.schema;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.PatternSyntaxException;

import org.junit.jupiter.api.Test;

/**
 * Tests of the ECMA-262 to Java regex translation in {@link JsonSchemaRegexUtilities}.
 */
public class JsonSchemaRegexUtilitiesTest {
	private static boolean matches(final String ecmaScriptRegex, final String value) {
		return JsonSchemaRegexUtilities.compile(ecmaScriptRegex).matcher(value).find();
	}

	@Test
	void whiteSpaceHasEcmaScriptSemantics() {
		for (final String whiteSpace : new String[] { " ", "\t", " ", " ", " ", "﻿" }) {
			assertTrue(matches("^\\s$", whiteSpace), "\\s should match U+" + Integer.toHexString(whiteSpace.charAt(0)));
			assertFalse(matches("^\\S$", whiteSpace));
			assertTrue(matches("^[\\s]$", whiteSpace));
			assertFalse(matches("^[a\\S]$", whiteSpace));
		}
		assertTrue(matches("^[a\\S]$", "x"));
		assertFalse(matches("^\\s$", "x"));
	}

	@Test
	void characterClasses() {
		// "[]" matches nothing, "[^]" matches everything
		assertFalse(matches("a[]", "a"));
		assertTrue(matches("^[^]$", "\n"));
		// "[" and "&" are literal within a class
		assertTrue(matches("^[[a]+$", "[a["));
		assertTrue(matches("^[a&&b]$", "&"));
		// Digits and word characters are ASCII only
		assertFalse(matches("^\\d$", "৪"));
		assertFalse(matches("^\\w$", "é"));
	}

	@Test
	void escapes() {
		assertTrue(matches("^\\v$", "\u000B"));
		assertTrue(matches("^\\0$", "\u0000"));
		assertTrue(matches("^\\cJ$", "\n"));
		assertTrue(matches("^\\cj$", "\n"));
		assertTrue(matches("^\\u{1F600}$", "😀"));
		assertTrue(matches("^\\u0041$", "A"));
		assertTrue(matches("^\\/$", "/"));
	}

	@Test
	void unicodePropertyEscapes() {
		assertTrue(matches("^\\p{Letter}+$", "Ünïcödé"));
		assertFalse(matches("^\\p{Letter}+$", "123"));
		assertTrue(matches("^\\p{General_Category=Uppercase_Letter}$", "Ä"));
		assertTrue(matches("^\\P{Lu}$", "ä"));
		assertTrue(matches("^\\p{Script=Greek}+$", "αβγ"));
		assertTrue(matches("^\\p{Script_Extensions=Greek}+$", "αβγ"));
	}

	@Test
	void ecmaScriptValidity() {
		assertTrue(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("^[a-z]+(?:-[a-z]+)*$"));
		assertTrue(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("(?<year>[0-9]{4})-\\k<year>"));
		assertTrue(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("(?<=a)b(?!c)"));
		assertTrue(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("(?i:abc)"));
		assertTrue(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("[^]"));
		assertTrue(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("a{2}"));

		// Valid in Java, but not in ECMA-262
		assertFalse(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("\\a"));
		assertFalse(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("\\Qa.b\\E"));
		assertFalse(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("(?i)abc"));
		assertFalse(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("(?>abc)"));
		assertFalse(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("a*+"));
		assertFalse(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("a{2}+"));
		assertFalse(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("\\z"));
		// Invalid in both
		assertFalse(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("^(abc]"));
		assertFalse(JsonSchemaRegexUtilities.isValidEcmaScriptRegex("[a"));
	}

	@Test
	void compileStaysLenientForJavaOnlyConstructs() {
		// "pattern" keeps accepting Java-only constructs of existing schemas
		assertTrue(matches("(?i)abc", "ABC"));
		assertThrows(PatternSyntaxException.class, () -> JsonSchemaRegexUtilities.compile("^(abc]"));
	}
}
