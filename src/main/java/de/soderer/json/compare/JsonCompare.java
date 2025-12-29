package de.soderer.json.compare;

import java.util.ArrayList;
import java.util.List;

import de.soderer.json.JsonArray;
import de.soderer.json.JsonNode;
import de.soderer.json.JsonObject;

public class JsonCompare {
	private JsonCompareSettings compareSettings = new JsonCompareSettings();

	private final JsonNode objectLeft;
	private final JsonNode objectRight;

	public JsonCompare(final JsonNode objectLeft, final JsonNode objectRight) {
		this.objectLeft = objectLeft;
		this.objectRight = objectRight;
	}

	public JsonCompareSettings getCompareSettings() {
		return compareSettings;
	}

	public JsonCompare setCompareSettings(final JsonCompareSettings compareSettings) {
		this.compareSettings = compareSettings;
		return this;
	}

	public JsonObject compare() throws Exception {
		final JsonArray differences = compare(objectLeft, objectRight);
		if (differences != null) {
			return new JsonObject().add("differences", differences);
		} else {
			return null;
		}
	}

	private JsonArray compare(final JsonObject jsonObjectLeft, final JsonObject jsonObjectRight) throws Exception {
		final JsonArray differences = new JsonArray();
		for (final String key : jsonObjectLeft.keySet()) {
			if (!jsonObjectRight.containsKey(key)) {
				differences.add("jsonObjectRight misses property key '" + key + "'");
			} else {
				final JsonArray propertyDifferences = compare(jsonObjectLeft.get(key), jsonObjectRight.get(key));
				if (propertyDifferences != null) {
					differences.add(new JsonObject().add("propertyKey", key).add("differences", propertyDifferences));
				}
			}
		}
		for (final String key : jsonObjectRight.keySet()) {
			if (!jsonObjectLeft.containsKey(key)) {
				differences.add("jsonObjectLeft misses property key '" + key + "'");
			}
		}

		if (differences.size() > 0) {
			return differences;
		} else {
			return null;
		}
	}

	private JsonArray compare(final JsonArray jsonArrayLeft, final JsonArray jsonArrayRight) throws Exception {
		final JsonArray differences = new JsonArray();

		if (jsonArrayLeft.size() > jsonArrayRight.size()) {
			differences.add("jsonArrayRight misses " + (jsonArrayLeft.size() - jsonArrayRight.size()) + " items");
		} else if (jsonArrayLeft.size() < jsonArrayRight.size()) {
			differences.add("jsonArrayLeft misses " + (jsonArrayRight.size() - jsonArrayLeft.size()) + " items");
		}

		if (differences.size() == 0 || compareSettings.isCompareArraysOfDifferentLength()) {
			if (compareSettings == null || !compareSettings.isAllowArrayMixedOrder()) {
				for (int i = 0; i < jsonArrayLeft.size(); i++) {
					final JsonArray itemDifferences = compare(jsonArrayLeft.get(i), jsonArrayRight.size() > i ? jsonArrayRight.get(i) : null);
					if (itemDifferences != null) {
						differences.add(new JsonObject().add("leftItemIndex", i).add("differences", itemDifferences));
					}
				}
				for (int i = jsonArrayLeft.size(); i < jsonArrayRight.size(); i++) {
					final JsonArray itemDifferences = compare(jsonArrayLeft.size() > i ? jsonArrayLeft.get(i) : null, jsonArrayRight.get(i));
					if (itemDifferences != null) {
						differences.add(new JsonObject().add("rightItemIndex", i).add("differences", itemDifferences));
					}
				}
			} else {
				// ignore array item order
				final List<Integer> alreadyUsedRightIndexes = new ArrayList<>();
				for (int i = 0; i < jsonArrayLeft.size(); i++) {
					boolean foundMatchingItem = false;
					for (int j = 0; j < jsonArrayRight.size(); j++) {
						if (!alreadyUsedRightIndexes.contains(j)) {
							final JsonArray itemDifferences = compare(jsonArrayLeft.get(i), jsonArrayRight.get(j));
							if (itemDifferences == null) {
								alreadyUsedRightIndexes.add(j);
								foundMatchingItem = true;
								break;
							}
						}
					}
					if (!foundMatchingItem) {
						differences.add(new JsonObject().add("leftItemIndex", i).add("differences", "Cannot find matching array item in jsonArrayRight"));
					}
				}
				for (int i = 0; i < jsonArrayRight.size(); i++) {
					if (!alreadyUsedRightIndexes.contains(i)) {
						differences.add(new JsonObject().add("rightItemIndex", i).add("differences", "Cannot find matching array item in jsonArrayLeft"));
						alreadyUsedRightIndexes.add(i);
					}
				}
			}
		}

		if (differences.size() > 0) {
			return differences;
		} else {
			return null;
		}
	}

	private JsonArray compare(final JsonNode valueObjectLeft, final JsonNode valueObjectRight) throws Exception {
		if (valueObjectLeft == null && valueObjectRight == null) {
			return null;
		} else if (valueObjectLeft == null) {
			return new JsonArray().add("objectLeft is null, objectRight is of type '" + valueObjectRight.getClass().getSimpleName() + "'");
		} else if (valueObjectRight == null) {
			return new JsonArray().add("objectLeft is of type '" + valueObjectLeft.getClass().getSimpleName() + "', objectRight is null");
		} else if (!valueObjectLeft.getClass().equals(valueObjectRight.getClass())) {
			return new JsonArray().add("objectLeft is of type '" + valueObjectLeft.getClass().getSimpleName() + "', objectRight is of type '" + valueObjectRight.getClass().getSimpleName() + "'");
		} else if (valueObjectLeft instanceof JsonArray) {
			return compare((JsonArray) valueObjectLeft, (JsonArray) valueObjectRight);
		} else if (valueObjectLeft instanceof JsonObject) {
			return compare((JsonObject) valueObjectLeft, (JsonObject) valueObjectRight);
		} else if (!valueObjectLeft.equals(valueObjectRight)) {
			return new JsonArray().add("objectLeft is '" + valueObjectLeft.toString() + "', objectRight is '" + valueObjectRight.toString() + "'");
		} else {
			return null;
		}
	}
}
