package de.soderer.utilities.json.compare;

import java.util.ArrayList;
import java.util.List;

import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonObject;

public class DirectoryHashJsonCompare {
	public final int MaxDifferenceDepth = Integer.MAX_VALUE;

	private final JsonObject objectLeft;
	private final JsonObject objectRight;

	public DirectoryHashJsonCompare(final JsonObject objectLeft, final JsonObject objectRight) {
		this.objectLeft = objectLeft;
		this.objectRight = objectRight;
	}

	public JsonObject compare() {
		final JsonArray differences = compare("", objectLeft, objectRight, 0);
		if (differences != null) {
			return new JsonObject().add("differences", differences);
		} else {
			return null;
		}
	}

	private JsonArray compare(final String path, final JsonObject jsonObjectLeft, final JsonObject jsonObjectRight, final int differenceDepth) {
		final JsonArray differences = new JsonArray();
		if (jsonObjectLeft == null && jsonObjectRight == null) {
			return null;
		} else if (jsonObjectLeft == null) {
			differences.add(path + ": jsonObjectLeft is null");
		} else if (jsonObjectRight == null) {
			differences.add(path + ": jsonObjectRight is null");
		} else {
			if (jsonObjectLeft.get("isDirectory") == null) {
				differences.add(path + ": jsonObjectLeft is missing mandatory property 'isDirectory'");
			} else if (jsonObjectRight.get("isDirectory") == null) {
				differences.add(path + ": jsonObjectRight is missing mandatory property 'isDirectory'");
			} else if (jsonObjectLeft.get("isDirectory") != jsonObjectRight.get("isDirectory")) {
				differences.add(path + ": jsonObjectLeft type is '" + (((Boolean) jsonObjectLeft.get("isDirectory")) ? "directory" : "file") + "' but jsonObjectRight type is '" + (((Boolean) jsonObjectRight.get("isDirectory")) ? "directory" : "file") + "'");
			} else if (jsonObjectLeft.get("hash") == null && jsonObjectRight.get("hash") == null) {
				if (!jsonObjectLeft.get("name").equals(jsonObjectRight.get("name"))) {
					differences.add(path + ": jsonObjectLeft name is '" + jsonObjectLeft.get("name") + "' but jsonObjectRight name is '" + jsonObjectRight.get("name") + "'");
				}
				if (!jsonObjectLeft.get("size").equals(jsonObjectRight.get("size"))) {
					differences.add(path + ": jsonObjectLeft size is '" + jsonObjectLeft.get("size") + "' but jsonObjectRight size is '" + jsonObjectRight.get("size") + "' with not available hashes");
				}
				if ((Boolean) jsonObjectLeft.get("isDirectory")) {
					final JsonArray childsArrayLeft = (JsonArray) jsonObjectLeft.get("childs");
					final JsonArray childsArrayRight = (JsonArray) jsonObjectRight.get("childs");
					final JsonArray subDirectoryDifferences = compare(path + "/" + jsonObjectLeft.get("name"), childsArrayLeft, childsArrayRight, differenceDepth + 1);
					if (subDirectoryDifferences != null) {
						for (final Object difference : subDirectoryDifferences) {
							differences.add(difference);
						}
					}
				}
			} else if (jsonObjectLeft.get("hash") == null) {
				differences.add(path + ": jsonObjectLeft is missing mandatory property 'hash'");
			} else if (jsonObjectRight.get("hash") == null) {
				differences.add(path + ": jsonObjectRight is missing mandatory property 'hash'");
			} else if (jsonObjectLeft.get("hash").equals(jsonObjectRight.get("hash"))) {
				if (jsonObjectLeft.get("name").equals(jsonObjectRight.get("name"))) {
					return null;
				} else {
					differences.add(path + ": jsonObjectLeft name is '" + jsonObjectLeft.get("name") + "' but jsonObjectRight name is '" + jsonObjectRight.get("name") + "'");
				}
			} else {
				// Hashes do not match
				differences.add(path + ": jsonObjectLeft hash is '" + jsonObjectLeft.get("hash") + "' but jsonObjectRight hash is '" + jsonObjectRight.get("hash") + "'");
				if (!jsonObjectLeft.get("size").equals(jsonObjectRight.get("size"))) {
					differences.add(path + ": jsonObjectLeft size is '" + jsonObjectLeft.get("size") + "' but jsonObjectRight size is '" + jsonObjectRight.get("size") + "'");
				}
				if ((Boolean) jsonObjectLeft.get("isDirectory") && differenceDepth < MaxDifferenceDepth) {
					final JsonArray childsArrayLeft = (JsonArray) jsonObjectLeft.get("childs");
					final JsonArray childsArrayRight = (JsonArray) jsonObjectRight.get("childs");
					final JsonArray subDirectoryDifferences = compare(path + "/" + jsonObjectLeft.get("name"), childsArrayLeft, childsArrayRight, differenceDepth + 1);
					if (subDirectoryDifferences != null) {
						for (final Object difference : subDirectoryDifferences) {
							differences.add(difference);
						}
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

	private JsonArray compare(final String path, final JsonArray childsArrayLeft, final JsonArray childsArrayRight, final int differenceDepth) {
		final JsonArray differences = new JsonArray();

		if (childsArrayLeft == null && childsArrayRight == null) {
			return null;
		} else if (childsArrayLeft == null) {
			differences.add(path + ": childsArrayLeft misses " + childsArrayRight.size() + " items");
			return differences;
		} else if (childsArrayRight == null) {
			differences.add(path + ": childsArrayRight misses " + childsArrayLeft.size() + " items");
			return differences;
		}

		if (childsArrayLeft.size() > childsArrayRight.size()) {
			differences.add(path + ": childsArrayRight misses " + (childsArrayLeft.size() - childsArrayRight.size()) + " items");
		} else if (childsArrayLeft.size() < childsArrayRight.size()) {
			differences.add(path + ": childsArrayLeft misses " + (childsArrayRight.size() - childsArrayLeft.size()) + " items");
		}

		final List<Integer> alreadyUsedRightIndexes = new ArrayList<>();
		for (int i = 0; i < childsArrayLeft.size(); i++) {
			boolean foundMatchingItem = false;
			for (int j = 0; j < childsArrayRight.size(); j++) {
				if (!alreadyUsedRightIndexes.contains(j)) {
					final JsonArray itemDifferences = compare(path, (JsonObject) childsArrayLeft.get(i), (JsonObject) childsArrayRight.get(j), differenceDepth);
					if (itemDifferences == null) {
						alreadyUsedRightIndexes.add(j);
						foundMatchingItem = true;
						break;
					} else if (((JsonObject) childsArrayLeft.get(i)).get("name").equals(((JsonObject) childsArrayRight.get(j)).get("name"))) {
						differences.add(new JsonObject().add("leftItemIndex", i).add("name", ((JsonObject) childsArrayLeft.get(i)).get("name")).add("differences", itemDifferences));
						alreadyUsedRightIndexes.add(j);
						foundMatchingItem = true;
						break;

					}
				}
			}
			if (!foundMatchingItem) {
				differences.add(new JsonObject().add("leftItemIndex", i).add("name", ((JsonObject) childsArrayLeft.get(i)).get("name")).add("differences", "Cannot find matching array item in childsArrayRight"));
			}
		}
		for (int i = 0; i < childsArrayRight.size(); i++) {
			if (!alreadyUsedRightIndexes.contains(i)) {
				differences.add(new JsonObject().add("rightItemIndex", i).add("name", ((JsonObject) childsArrayRight.get(i)).get("name")).add("differences", "Cannot find matching array item in childsArrayLeft"));
				alreadyUsedRightIndexes.add(i);
			}
		}

		if (differences.size() > 0) {
			return differences;
		} else {
			return null;
		}
	}
}
