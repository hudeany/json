package de.soderer.json.schema.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;
import de.soderer.json.JsonReader;
import de.soderer.json.JsonValueBoolean;
import de.soderer.json.JsonValueInteger;
import de.soderer.json.JsonValueNull;
import de.soderer.json.JsonValueNumber;
import de.soderer.json.JsonValueString;
import de.soderer.json.schema.JsonSchema;
import de.soderer.json.schema.JsonSchemaConfiguration;
import de.soderer.json.schema.JsonSchemaDataValidationError;
import de.soderer.json.schema.JsonSchemaVersion;

/**
 * Tests of the hash based "uniqueItems" check: the hash code must be consistent with
 * {@link BaseJsonSchemaValidator#isJsonDataEqual(JsonNode, JsonNode)}, otherwise duplicates would
 * be missed.
 */
@SuppressWarnings("static-method")
public class UniqueItemsValidatorHashTest {
	private static JsonNode json(final String text) throws Exception {
		try (JsonReader reader = new JsonReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)))) {
			return reader.read();
		}
	}

	private static JsonSchema uniqueItemsSchema() throws Exception {
		return new JsonSchema((JsonObject) json("{\"uniqueItems\": true}"), new JsonSchemaConfiguration().withJsonSchemaVersion(JsonSchemaVersion.draft2020_12));
	}

	private static void assertEqualWithSameHashCode(final JsonNode first, final JsonNode second) {
		assertTrue(BaseJsonSchemaValidator.isJsonDataEqual(first, second), first + " = " + second);
		assertEquals(UniqueItemsValidator.getJsonDataHashCode(first), UniqueItemsValidator.getJsonDataHashCode(second), first + " = " + second);
	}

	@Test
	void numericallyEqualNumbersHaveSameHashCode() {
		final List<JsonNode> ones = List.of(new JsonValueInteger(1), new JsonValueInteger(1L), new JsonValueInteger(BigInteger.ONE), new JsonValueNumber(1.0), new JsonValueNumber(1.0f),
				new JsonValueNumber(new BigDecimal("1.000")), new JsonValueNumber(new BigDecimal("0.1E1")));
		for (final JsonNode first : ones) {
			for (final JsonNode second : ones) {
				assertEqualWithSameHashCode(first, second);
			}
		}
		assertEqualWithSameHashCode(new JsonValueNumber(0.1f), new JsonValueNumber(0.1));
		assertEqualWithSameHashCode(new JsonValueNumber(0.1), new JsonValueNumber(new BigDecimal("0.10")));
		assertEqualWithSameHashCode(new JsonValueNumber(-0.0), new JsonValueInteger(0));
		final BigInteger huge = BigInteger.TEN.pow(400);
		assertEqualWithSameHashCode(new JsonValueInteger(huge), new JsonValueNumber(new BigDecimal(huge).setScale(2)));
	}

	@Test
	void structurallyEqualDataHasSameHashCode() throws Exception {
		assertEqualWithSameHashCode(json("{\"a\": 1, \"b\": [1, {\"c\": null}]}"), json("{\"b\": [1.0, {\"c\": null}], \"a\": 1.00}"));
		assertEqualWithSameHashCode(json("[]"), json("[]"));
		assertEqualWithSameHashCode(json("{}"), json("{}"));
	}

	@Test
	void randomDataHashCodeIsConsistentWithEquality() {
		final Random random = new Random(4711);
		for (int i = 0; i < 20_000; i++) {
			final JsonNode first = randomNode(random, 3);
			final JsonNode second = randomNode(random, 3);
			if (BaseJsonSchemaValidator.isJsonDataEqual(first, second)) {
				assertEquals(UniqueItemsValidator.getJsonDataHashCode(first), UniqueItemsValidator.getJsonDataHashCode(second), first + " = " + second);
			}
		}
	}

	@Test
	void duplicatesAreDetected() throws Exception {
		final JsonSchema jsonSchema = uniqueItemsSchema();
		assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(json("[1, 2, 1.0]")));
		assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(json("[{\"a\": 1, \"b\": 2}, {\"b\": 2, \"a\": 1}]")));
		assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(json("[[1, [2]], [1, [2.0]]]")));
		assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(json("[null, null]")));
		assertDoesNotThrow(() -> jsonSchema.validate(json("[1, true, \"1\", [1], {\"1\": 1}, null, 1.5]")));
		assertDoesNotThrow(() -> jsonSchema.validate(json("[[1, 2], [2, 1]]")));
	}

	@Test
	void largeArraysAreCheckedInLinearTime() throws Exception {
		final JsonSchema jsonSchema = uniqueItemsSchema();
		final JsonArray uniqueData = new JsonArray();
		for (int i = 0; i < 200_000; i++) {
			uniqueData.add(i);
		}
		final JsonArray duplicateAtEnd = new JsonArray();
		for (int i = 0; i < 200_000; i++) {
			duplicateAtEnd.add("item" + i);
		}
		duplicateAtEnd.add("item0");
		// Comparing all pairs would take 2 * 10^10 comparisons
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			jsonSchema.validate(uniqueData);
			assertThrows(JsonSchemaDataValidationError.class, () -> jsonSchema.validate(duplicateAtEnd));
		});
	}

	private static JsonNode randomNode(final Random random, final int depth) {
		final int kind = random.nextInt(depth > 0 ? 8 : 6);
		switch (kind) {
			case 0:
				return new JsonValueInteger(random.nextInt(3));
			case 1:
				return new JsonValueNumber(random.nextInt(3) + (random.nextBoolean() ? 0.0 : 0.5));
			case 2:
				return new JsonValueNumber(new BigDecimal(random.nextInt(3)).setScale(random.nextInt(3)));
			case 3:
				return new JsonValueNumber((float) (random.nextInt(3) + (random.nextBoolean() ? 0.0 : 0.5)));
			case 4:
				return new JsonValueInteger(BigInteger.valueOf(random.nextInt(3)));
			case 5:
				return random.nextBoolean() ? new JsonValueNull() : random.nextBoolean() ? new JsonValueBoolean(random.nextBoolean()) : new JsonValueString(String.valueOf(random.nextInt(2)));
			case 6: {
				final JsonArray array = new JsonArray();
				final int size = random.nextInt(3);
				for (int i = 0; i < size; i++) {
					array.add(randomNode(random, depth - 1));
				}
				return array;
			}
			default: {
				final JsonObject object = new JsonObject();
				final List<String> keys = new ArrayList<>(List.of("a", "b"));
				if (random.nextBoolean()) {
					keys.add(0, keys.remove(1));
				}
				for (final String key : keys) {
					if (random.nextBoolean()) {
						try {
							object.add(key, randomNode(random, depth - 1));
						} catch (final Exception e) {
							throw new IllegalStateException(e);
						}
					}
				}
				return object;
			}
		}
	}
}
