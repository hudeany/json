package de.soderer.json;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.soderer.json.utilities.ClassUtilities;
import de.soderer.json.utilities.DateUtilities;
import de.soderer.json.utilities.Utilities;

public class JsonSerializer {
	/**
	 * Serialize an object in json data<br />
	 * - Serializes null values<br />
	 * - Excludes static fields<br />
	 * - Excludes transient fields<br />
	 * - Does not show object type infos<br />
	 *
	 * @param dataObject
	 * @return
	 * @throws Exception
	 */
	public static JsonNode serialize(final Object dataObject) throws Exception {
		return serializeInternal(dataObject, false, false, false, false, new ArrayList<>());
	}

	/**
	 * Serialize an object in json data
	 *
	 * @param dataObject
	 * @param excludeNull Do not serialize null values
	 * @param includeStatic Serialize fields with a "static" modifier
	 * @param includeTransient Serialize fields with a "transient" modifier
	 * @param addObjectTypeInfo Add object type info
	 * @return
	 * @throws Exception
	 */
	public static JsonNode serialize(final Object dataObject, final boolean excludeNull, final boolean includeStatic, final boolean includeTransient, final boolean addObjectTypeInfo) throws Exception {
		return serializeInternal(dataObject, excludeNull, includeStatic, includeTransient, addObjectTypeInfo, new ArrayList<>());
	}

	private static JsonNode serializeInternal(final Object dataObject, final boolean excludeNull, final boolean includeStatic, final boolean includeTransient, final boolean addObjectTypeInfo, final List<Object> alreadyVisitedObjects) throws Exception {
		if (dataObject == null) {
			if (excludeNull) {
				// This may only occur on top level of data
				return new JsonValueNull().setRootNode(true);
			} else if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.addNull("class");
				jsonObjectWithTypeInfo.addNull("value");
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueNull().setRootNode(true);
			}
		} else if (dataObject instanceof String) {
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
				jsonObjectWithTypeInfo.add("value", (String) dataObject);
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueString((String) dataObject).setRootNode(true);
			}
		} else if (dataObject instanceof Integer) {
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
				jsonObjectWithTypeInfo.add("value", (Integer) dataObject);
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueInteger((Integer) dataObject).setRootNode(true);
			}
		} else if (dataObject instanceof Long) {
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
				jsonObjectWithTypeInfo.add("value", (Long) dataObject);
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueInteger((Long) dataObject).setRootNode(true);
			}
		} else if (dataObject instanceof Float) {
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
				jsonObjectWithTypeInfo.add("value", (Float) dataObject);
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueNumber((Float) dataObject).setRootNode(true);
			}
		} else if (dataObject instanceof Double) {
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
				jsonObjectWithTypeInfo.add("value", (Double) dataObject);
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueNumber((Double) dataObject).setRootNode(true);
			}
		} else if (dataObject instanceof Number) {
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
				jsonObjectWithTypeInfo.add("value", (Number) dataObject);
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueNumber((Number) dataObject).setRootNode(true);
			}
		} else if (dataObject instanceof Character) {
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
				jsonObjectWithTypeInfo.add("value", ((Character) dataObject).toString());
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueString(((Character) dataObject).toString()).setRootNode(true);
			}
		} else if (dataObject instanceof Boolean) {
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
				jsonObjectWithTypeInfo.add("value", (Boolean) dataObject);
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueBoolean((Boolean) dataObject).setRootNode(true);
			}
		} else if (dataObject instanceof Charset) {
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", Charset.class.getName());
				jsonObjectWithTypeInfo.add("value", ((Charset) dataObject).toString());
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueString(dataObject.toString()).setRootNode(true);
			}
		} else if (dataObject instanceof Date) {
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
				jsonObjectWithTypeInfo.add("value", DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, (Date) dataObject));
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueString(dataObject.toString()).setRootNode(true);
			}
		} else if (dataObject instanceof LocalDateTime) {
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
				jsonObjectWithTypeInfo.add("value", DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, (LocalDateTime) dataObject));
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueString(dataObject.toString()).setRootNode(true);
			}
		} else if (dataObject instanceof LocalDate) {
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
				jsonObjectWithTypeInfo.add("value",DateUtilities.formatDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, (LocalDate) dataObject));
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueString(dataObject.toString()).setRootNode(true);
			}
		} else if (dataObject instanceof ZonedDateTime) {
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
				jsonObjectWithTypeInfo.add("value", DateUtilities.formatDate(DateUtilities.ISO_8601_DATETIME_FORMAT, (ZonedDateTime) dataObject));
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueString(dataObject.toString()).setRootNode(true);
			}
		} else if (dataObject instanceof File) {
			final File fileObject = (File) dataObject;
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
				jsonObjectWithTypeInfo.add("value", fileObject.getAbsolutePath());
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueString(dataObject.toString()).setRootNode(true);
			}
		} else if (dataObject instanceof Enum) {
			if (addObjectTypeInfo) {
				final JsonObject jsonObjectWithTypeInfo = new JsonObject();
				jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
				jsonObjectWithTypeInfo.add("value", dataObject.toString());
				return jsonObjectWithTypeInfo.setRootNode(true);
			} else {
				return new JsonValueString(dataObject.toString()).setRootNode(true);
			}
		} else if (dataObject.getClass().isArray()) {
			if (Utilities.containsObject(alreadyVisitedObjects, dataObject)) {
				throw new Exception("Cyclic reference detected. Cannot serialize object: " + dataObject.getClass() + " (Hashcode " + System.identityHashCode(dataObject) + ")");
			} else {
				// the same object may be included multiple times in parent objects, but not in child objects
				alreadyVisitedObjects.add(dataObject);

				final JsonArray jsonArray = new JsonArray();
				final int length = Array.getLength(dataObject);
				for (int i = 0; i < length; i ++) {
					final Object item = Array.get(dataObject, i);
					if (item != null || !excludeNull) {
						jsonArray.add(serializeInternal(item, excludeNull, includeStatic, includeTransient, addObjectTypeInfo, alreadyVisitedObjects));
					}
				}

				// the same object may be included multiple times in parent objects
				alreadyVisitedObjects.remove(dataObject);

				if (addObjectTypeInfo) {
					final JsonObject jsonObjectWithTypeInfo = new JsonObject();
					jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
					jsonObjectWithTypeInfo.add("value", jsonArray);
					return jsonObjectWithTypeInfo.setRootNode(true);
				} else {
					return jsonArray.setRootNode(true);
				}
			}
		} else if (dataObject instanceof Iterable<?>) {
			if (Utilities.containsObject(alreadyVisitedObjects, dataObject)) {
				throw new Exception("Cyclic reference detected. Cannot serialize object: " + dataObject.getClass() + " (Hashcode " + System.identityHashCode(dataObject) + ")");
			} else {
				// the same object may be included multiple times in parent objects, but not in child objects
				alreadyVisitedObjects.add(dataObject);

				final JsonArray jsonArray = new JsonArray();
				for (final Object item : (Iterable<?>) dataObject) {
					if (item != null || !excludeNull) {
						jsonArray.add(serializeInternal(item, excludeNull, includeStatic, includeTransient, addObjectTypeInfo, alreadyVisitedObjects));
					}
				}

				// the same object may be included multiple times in parent objects
				alreadyVisitedObjects.remove(dataObject);

				if (addObjectTypeInfo) {
					final JsonObject jsonObjectWithTypeInfo = new JsonObject();
					jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
					jsonObjectWithTypeInfo.add("value", jsonArray);
					return jsonObjectWithTypeInfo.setRootNode(true);
				} else {
					return jsonArray.setRootNode(true);
				}
			}
		} else if (dataObject instanceof Map<?, ?>) {
			if (Utilities.containsObject(alreadyVisitedObjects, dataObject)) {
				throw new Exception("Cyclic reference detected. Cannot serialize object: " + dataObject.getClass() + " (Hashcode " + System.identityHashCode(dataObject) + ")");
			} else {
				// the same object may be included multiple times in parent objects, but not in child objects
				alreadyVisitedObjects.add(dataObject);

				final JsonArray jsonArray = new JsonArray();
				for (final Entry<?, ?> entry : ((Map<?, ?>) dataObject).entrySet()) {
					final JsonObject jsonObject = new JsonObject();

					if (entry.getKey() == null) {
						jsonObject.addNull("key");
					} else {
						jsonObject.add("key", serializeInternal(entry.getKey(), excludeNull, includeStatic, includeTransient, addObjectTypeInfo, alreadyVisitedObjects));
					}

					if (entry.getValue() == null) {
						jsonObject.addNull("value");
					} else {
						jsonObject.add("value", serializeInternal(entry.getValue(), excludeNull, includeStatic, includeTransient, addObjectTypeInfo, alreadyVisitedObjects));
					}

					jsonArray.add(jsonObject);
				}

				// the same object may be included multiple times in parent objects
				alreadyVisitedObjects.remove(dataObject);

				if (addObjectTypeInfo) {
					final JsonObject jsonObjectWithTypeInfo = new JsonObject();
					jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
					jsonObjectWithTypeInfo.add("value", jsonArray);
					return jsonObjectWithTypeInfo.setRootNode(true);
				} else {
					return jsonArray.setRootNode(true);
				}
			}
		} else {
			if (Utilities.containsObject(alreadyVisitedObjects, dataObject)) {
				throw new Exception("Cyclic reference detected. Cannot serialize object: " + dataObject.getClass() + " (Hashcode " + System.identityHashCode(dataObject) + ")");
			} else {
				// the same object may be included multiple times in parent objects, but not in child objects
				alreadyVisitedObjects.add(dataObject);

				final JsonObject dataJsonObject = new JsonObject();
				final Class<?> dataClass = dataObject.getClass();
				for (final Field dataField : ClassUtilities.getAllFields(dataClass)) {
					boolean serializeField = true;
					if ((dataField.getModifiers() & Modifier.TRANSIENT) == Modifier.TRANSIENT && !includeTransient) {
						serializeField = false;
					} else if ((dataField.getModifiers() & Modifier.STATIC) == Modifier.STATIC && !includeStatic) {
						serializeField = false;
					}

					if (serializeField) {
						try {
							dataField.setAccessible(true);
						} catch (final Exception e) {
							throw new Exception("Unserializable object found", e);
						}
						final String fieldName = dataField.getName();
						final Object fieldData = dataField.get(dataObject);
						if (fieldData != null || !excludeNull) {
							if (fieldData == null && addObjectTypeInfo) {
								final JsonObject jsonObjectWithTypeInfo = new JsonObject();
								jsonObjectWithTypeInfo.add("class", dataField.getType().getName());
								jsonObjectWithTypeInfo.addNull("value");
								dataJsonObject.add(fieldName, jsonObjectWithTypeInfo);
							} else {
								dataJsonObject.add(fieldName, serializeInternal(fieldData, excludeNull, includeStatic, includeTransient, addObjectTypeInfo, alreadyVisitedObjects));
							}
						}
					}
				}

				// the same object may be included multiple times in parent objects
				alreadyVisitedObjects.remove(dataObject);

				if (addObjectTypeInfo) {
					final JsonObject jsonObjectWithTypeInfo = new JsonObject();
					jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
					jsonObjectWithTypeInfo.add("value", dataJsonObject);
					return jsonObjectWithTypeInfo.setRootNode(true);
				} else {
					return dataJsonObject.setRootNode(true);
				}
			}
		}
	}

	public static Object deserialize(final JsonNode jsonData) throws Exception {
		if (jsonData == null) {
			throw new Exception("JSON data is null");
		} else if (jsonData.isJsonObject()) {
			return deserialize((JsonObject) jsonData);
		} else {
			throw new Exception("JSON data is not a JsonObject: '" + jsonData.getClass().getSimpleName() + "'");
		}
	}

	public static Object deserialize(final JsonObject jsonObject) throws Exception {
		try {
			if (jsonObject == null) {
				throw new Exception("JSON object is null");
			} else {
				if (!jsonObject.containsKey("class")) {
					throw new Exception("JSON object is missing mandatory type information");
				} else if (jsonObject.get("class") == null || jsonObject.get("class").isNull()) {
					// Null value that has no class info
					return null;
				} else if (!(jsonObject.getSimpleValue("class") instanceof String)) {
					throw new Exception("JSON object has invalid type information");
				} else if (!jsonObject.containsKey("value")) {
					throw new Exception("JSON object is missing mandatory value information");
				}

				final JsonNode value = jsonObject.get("value");
				final Class<?> clazz = Class.forName((String) jsonObject.getSimpleValue("class"));
				if (value == null || value.isNull()) {
					return null;
				} else if (clazz == Boolean.TYPE || clazz == Boolean.class) {
					return jsonObject.getSimpleValue("value");
				} else if (clazz == Byte.TYPE || clazz == Byte.class) {
					return ((Number) jsonObject.getSimpleValue("value")).byteValue();
				} else if (clazz == Short.TYPE || clazz == Short.class) {
					return ((Number) jsonObject.getSimpleValue("value")).shortValue();
				} else if (clazz == Integer.TYPE || clazz == Integer.class) {
					return ((Number) jsonObject.getSimpleValue("value")).intValue();
				} else if (clazz == Long.TYPE || clazz == Long.class) {
					return ((Number) jsonObject.getSimpleValue("value")).longValue();
				} else if (clazz == Float.TYPE || clazz == Float.class) {
					return ((Number) jsonObject.getSimpleValue("value")).floatValue();
				} else if (clazz == Double.TYPE || clazz == Double.class) {
					if (jsonObject.getSimpleValue("value").getClass() == Float.class) {
						return Double.parseDouble(Float.toString((Float) jsonObject.getSimpleValue("value")));
					} else {
						return ((Number) jsonObject.getSimpleValue("value")).doubleValue();
					}
				} else if (clazz == BigDecimal.class) {
					return jsonObject.getSimpleValue("value");
				} else if (clazz == Character.TYPE || clazz == Character.class) {
					if (jsonObject.getSimpleValue("value") instanceof Character) {
						return jsonObject.getSimpleValue("value");
					} else {
						return ((String) jsonObject.getSimpleValue("value")).charAt(0);
					}
				} else if (clazz == String.class) {
					return jsonObject.getSimpleValue("value");
				} else if (clazz == Charset.class) {
					return Charset.forName((String) jsonObject.getSimpleValue("value"));
				} else if (clazz == File.class) {
					return new File((String) jsonObject.getSimpleValue("value"));
				} else if (clazz.isEnum()) {
					final String enumName = (String) jsonObject.getSimpleValue("value");
					for (final Object enumConstant : clazz.getEnumConstants()) {
						if (enumName.equals(enumConstant.toString())) {
							return enumConstant;
						}
					}
					throw new Exception("Invalid enum name '" + enumName + "' for type '" + clazz.getName() + "'");
				} else if (Date.class.isAssignableFrom(clazz)) {
					return DateUtilities.getDateForZonedDateTime(DateUtilities.parseIso8601DateTimeString((String) jsonObject.getSimpleValue("value")));
				} else if (LocalDateTime.class.isAssignableFrom(clazz)) {
					return DateUtilities.parseIso8601DateTimeString((String) jsonObject.getSimpleValue("value")).toLocalDateTime();
				} else if (LocalDate.class.isAssignableFrom(clazz)) {
					return DateUtilities.parseIso8601DateTimeString((String) jsonObject.getSimpleValue("value")).toLocalDate();
				} else if (ZonedDateTime.class.isAssignableFrom(clazz)) {
					return DateUtilities.parseIso8601DateTimeString((String) jsonObject.getSimpleValue("value"));
				} else if (value instanceof JsonObject) {
					Object object;
					final Constructor<?> constructor = ClassUtilities.getConstructor(clazz);
					constructor.setAccessible(true);
					object = constructor.newInstance();

					for (final Entry<String, JsonNode> entry : ((JsonObject) value).entrySet()) {
						if (!(entry.getValue() instanceof JsonObject)) {
							throw new Exception("Invalid value type serialization value");
						}

						Field field;
						try {
							field = ClassUtilities.getField(clazz, entry.getKey());
						} catch (@SuppressWarnings("unused") final Exception e) {
							throw new Exception("Invalid field name serialization value: " + entry.getKey());
						}
						field.setAccessible(true);
						field.set(object, deserialize((JsonObject) entry.getValue()));
					}
					return object;
				} else if (value instanceof JsonArray) {
					final JsonArray jsonArray = (JsonArray) value;
					if (clazz.isArray()) {
						if (clazz.getComponentType() == Boolean.TYPE) {
							final boolean[] arrayValue = new boolean[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (boolean) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Boolean.class) {
							final Boolean[] arrayValue = new Boolean[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (Boolean) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Byte.TYPE) {
							final byte[] arrayValue = new byte[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (byte) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Byte.class) {
							final Byte[] arrayValue = new Byte[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (Byte) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Short.TYPE) {
							final short[] arrayValue = new short[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (short) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Short.class) {
							final Short[] arrayValue = new Short[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (Short) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Integer.TYPE) {
							final int[] arrayValue = new int[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (int) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Integer.class) {
							final Integer[] arrayValue = new Integer[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (Integer) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Long.TYPE) {
							final long[] arrayValue = new long[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (long) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Long.class) {
							final Long[] arrayValue = new Long[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (Long) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Float.TYPE) {
							final float[] arrayValue = new float[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (float) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Float.class) {
							final Float[] arrayValue = new Float[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (Float) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Double.TYPE) {
							final double[] arrayValue = new double[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (double) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Double.class) {
							final Double[] arrayValue = new Double[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (Double) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Character.TYPE) {
							final char[] arrayValue = new char[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (char) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else if (clazz.getComponentType() == Character.class) {
							final Character[] arrayValue = new Character[jsonArray.size()];
							for (int i = 0; i < arrayValue.length; i++) {
								arrayValue[i] = (Character) deserialize((JsonObject) jsonArray.get(i));
							}
							return arrayValue;
						} else {
							final Object[] array = (Object[]) Array.newInstance(clazz.getComponentType(), jsonArray.size());
							for (int i = 0; i < array.length; i++) {
								array[i] = deserialize((JsonObject) jsonArray.get(i));
							}
							return array;
						}
					} else if (Map.class.isAssignableFrom(clazz)) {
						final Constructor<?> constructor = clazz.getConstructor();
						@SuppressWarnings("unchecked")
						final Map<Object, Object> mapObject = (Map<Object, Object>) constructor.newInstance();
						for (final JsonNode keyValueObject : (JsonArray) value) {
							if (!(keyValueObject instanceof JsonObject)) {
								throw new Exception("Invalid value type serialization value");
							}

							final JsonObject keyJsonObject = (JsonObject) ((JsonObject) keyValueObject).get("key");
							final Object keyObject = deserialize(keyJsonObject);

							final JsonObject valueJsonObject = (JsonObject) ((JsonObject) keyValueObject).get("value");
							final Object valueObject = deserialize(valueJsonObject);

							mapObject.put(keyObject, valueObject);
						}
						return mapObject;
					} else {
						final List<Object> listOfItems = new ArrayList<>();
						for (final JsonNode item : (JsonArray) value) {
							if (!(item instanceof JsonObject)) {
								throw new Exception("Invalid value type serialization value");
							}

							listOfItems.add(deserialize((JsonObject) item));
						}

						final Constructor<?> constructor = clazz.getConstructor();
						final Object object = constructor.newInstance();
						if (object instanceof Collection) {
							for (final Object item : listOfItems) {
								@SuppressWarnings("unchecked")
								final
								Collection<Object> collectionObject = (Collection<Object>) object;
								collectionObject.add(item);
							}
							return object;
						} else {
							throw new Exception("");
						}
					}
				} else if (clazz == Object.class) {
					return jsonObject.getSimpleValue("value");
				} else {
					throw new Exception("Invalid value type for deserialization: " + clazz.getName());
				}
			}
		} catch (final Exception e) {
			throw e;
		}
	}

	public static Object deserialize(final Class<?> classType, final Object objectData) throws Exception {
		if (objectData == null) {
			return null;
		} else if (classType == Object.class) {
			return objectData;
		} else if (objectData instanceof JsonObject) {
			return deserialize(classType, (JsonObject) objectData);
		} else if (objectData instanceof JsonNode) {
			return deserialize(classType, (JsonNode) objectData);
		} else {
			return objectData;
		}
	}

	public static Object deserialize(final Class<?> classType, final JsonNode jsonData) throws Exception {
		if (jsonData == null) {
			throw new Exception("JSON data is null");
		} else if (jsonData.isJsonObject()) {
			return deserialize(classType, (JsonObject) jsonData);
		} else if (classType == Object.class) {
			if (jsonData.isNull()) {
				return null;
			} else if (jsonData.isString()) {
				return ((JsonValueString) jsonData).getValue();
			} else if (jsonData.isInteger()) {
				return ((JsonValueInteger) jsonData).getValue();
			} else if (jsonData.isNumber()) {
				return ((JsonValueNumber) jsonData).getValue();
			} else if (jsonData.isBoolean()) {
				return ((JsonValueBoolean) jsonData).getValue();
			} else {
				throw new Exception("JSON data is not a JsonObject: '" + jsonData.getClass().getSimpleName() + "'");
			}
		} else {
			throw new Exception("JSON data is not a JsonObject: '" + jsonData.getClass().getSimpleName() + "'");
		}
	}

	public static Object deserialize(final Class<?> classType, final JsonObject jsonObject) throws Exception {
		try {
			if (classType == null) {
				throw new Exception("Invalid class type serialization value");
			} else if (jsonObject == null) {
				return null;
			} else {
				final Constructor<?> constructor = ClassUtilities.getConstructor(classType);
				constructor.setAccessible(true);
				final Object object = constructor.newInstance();
				for (final Entry<String, JsonNode> entry : jsonObject.entrySet()) {
					Field field;
					try {
						field = ClassUtilities.getField(classType, entry.getKey());
					} catch (@SuppressWarnings("unused") final Exception e) {
						throw new Exception("Invalid serialization field name '" + entry.getKey() + "' for class '" + classType.toString() + "'");
					}
					field.setAccessible(true);

					final JsonNode value = entry.getValue();
					try {
						final Class<?> clazz = field.getType();
						if (value == null || value.isNull()) {
							field.set(object, null);
						} else if (clazz == Boolean.TYPE || clazz == Boolean.class) {
							field.set(object, jsonObject.getSimpleValue(entry.getKey()));
						} else if (clazz == Byte.TYPE || clazz == Byte.class) {
							field.set(object, ((Number) jsonObject.getSimpleValue(entry.getKey())).byteValue());
						} else if (clazz == Short.TYPE || clazz == Short.class) {
							field.set(object, ((Number) jsonObject.getSimpleValue(entry.getKey())).shortValue());
						} else if (clazz == Integer.TYPE || clazz == Integer.class) {
							field.set(object, ((Number) jsonObject.getSimpleValue(entry.getKey())).intValue());
						} else if (clazz == Long.TYPE || clazz == Long.class) {
							field.set(object, ((Number) jsonObject.getSimpleValue(entry.getKey())).longValue());
						} else if (clazz == Float.TYPE || clazz == Float.class) {
							field.set(object, ((Number) jsonObject.getSimpleValue(entry.getKey())).floatValue());
						} else if (clazz == Double.TYPE || clazz == Double.class) {
							if (jsonObject.getSimpleValue(entry.getKey()).getClass() == Float.class) {
								field.set(object, Double.parseDouble(Float.toString((Float) jsonObject.getSimpleValue(entry.getKey()))));
							} else {
								field.set(object, ((Number) jsonObject.getSimpleValue(entry.getKey())).doubleValue());
							}
						} else if (clazz == BigDecimal.class) {
							field.set(object, jsonObject.getSimpleValue(entry.getKey()));
						} else if (clazz == Character.TYPE || clazz == Character.class) {
							field.set(object, ((String) jsonObject.getSimpleValue(entry.getKey())).charAt(0));
						} else if (clazz == String.class) {
							field.set(object, jsonObject.getSimpleValue(entry.getKey()));
						} else if (clazz == Charset.class) {
							field.set(object, Charset.forName((String) jsonObject.getSimpleValue(entry.getKey())));
						} else if (Date.class.isAssignableFrom(clazz)) {
							field.set(object, DateUtilities.getDateForZonedDateTime(DateUtilities.parseIso8601DateTimeString((String) jsonObject.getSimpleValue(entry.getKey()))));
						} else if (LocalDateTime.class.isAssignableFrom(clazz)) {
							field.set(object, DateUtilities.parseIso8601DateTimeString((String) jsonObject.getSimpleValue(entry.getKey())).toLocalDateTime());
						} else if (LocalDate.class.isAssignableFrom(clazz)) {
							field.set(object, DateUtilities.parseIso8601DateTimeString((String) jsonObject.getSimpleValue(entry.getKey())).toLocalDate());
						} else if (ZonedDateTime.class.isAssignableFrom(clazz)) {
							field.set(object, DateUtilities.parseIso8601DateTimeString((String) jsonObject.getSimpleValue(entry.getKey())));
						} else if (clazz.isEnum()) {
							final String enumName = (String) jsonObject.getSimpleValue(entry.getKey());
							for (final Object enumConstant : clazz.getEnumConstants()) {
								if (enumName.equals(enumConstant.toString())) {
									field.set(object, enumConstant);
									break;
								}
							}
						} else if (clazz.isArray()) {
							final JsonArray jsonArray = ((JsonArray) value);
							if (clazz.getComponentType() == Boolean.TYPE) {
								final boolean[] arrayValue = new boolean[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = (boolean) jsonArray.getSimpleValue(i);
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Boolean.class) {
								final Boolean[] arrayValue = new Boolean[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = (Boolean) jsonArray.getSimpleValue(i);
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Byte.TYPE) {
								final byte[] arrayValue = new byte[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = ((Number) jsonArray.getSimpleValue(i)).byteValue();
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Byte.class) {
								final Byte[] arrayValue = new Byte[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = ((Number) jsonArray.getSimpleValue(i)).byteValue();
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Short.TYPE) {
								final short[] arrayValue = new short[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = ((Number) jsonArray.getSimpleValue(i)).shortValue();
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Short.class) {
								final Short[] arrayValue = new Short[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = ((Number) jsonArray.getSimpleValue(i)).shortValue();
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Integer.TYPE) {
								final int[] arrayValue = new int[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = ((Number) jsonArray.getSimpleValue(i)).intValue();
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Integer.class) {
								final Integer[] arrayValue = new Integer[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = ((Number) jsonArray.getSimpleValue(i)).intValue();
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Long.TYPE) {
								final long[] arrayValue = new long[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = ((Number) jsonArray.getSimpleValue(i)).longValue();
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Long.class) {
								final Long[] arrayValue = new Long[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = ((Number) jsonArray.getSimpleValue(i)).longValue();
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Float.TYPE) {
								final float[] arrayValue = new float[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = (float) jsonArray.getSimpleValue(i);
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Float.class) {
								final Float[] arrayValue = new Float[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = (float) jsonArray.getSimpleValue(i);
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Double.TYPE) {
								final double[] arrayValue = new double[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = (double) jsonArray.getSimpleValue(i);
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Double.class) {
								final Double[] arrayValue = new Double[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = (double) jsonArray.getSimpleValue(i);
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Character.TYPE) {
								final char[] arrayValue = new char[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = ((String) jsonArray.getSimpleValue(i)).charAt(0);
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == Character.class) {
								final Character[] arrayValue = new Character[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = ((String) jsonArray.getSimpleValue(i)).charAt(0);
								}
								field.set(object, arrayValue);
							} else if (clazz.getComponentType() == String.class) {
								final String[] arrayValue = new String[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = (String) jsonArray.getSimpleValue(i);
								}
								field.set(object, arrayValue);
							} else if (Date.class.isAssignableFrom(clazz.getComponentType())) {
								final Date[] arrayValue = new Date[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = DateUtilities.getDateForLocalDateTime(DateUtilities.parseLocalDateTime(DateUtilities.ISO_8601_DATETIME_FORMAT, (String) jsonArray.getSimpleValue(i)));
								}
								field.set(object, arrayValue);
							} else if (LocalDateTime.class.isAssignableFrom(clazz.getComponentType())) {
								final LocalDateTime[] arrayValue = new LocalDateTime[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = DateUtilities.parseLocalDateTime(DateUtilities.ISO_8601_DATETIME_FORMAT_NO_TIMEZONE, (String) jsonArray.getSimpleValue(i));
								}
								field.set(object, arrayValue);
							} else if (LocalDate.class.isAssignableFrom(clazz.getComponentType())) {
								final LocalDate[] arrayValue = new LocalDate[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = DateUtilities.parseLocalDate(DateUtilities.ISO_8601_DATE_FORMAT_NO_TIMEZONE, (String) jsonArray.getSimpleValue(i));
								}
								field.set(object, arrayValue);
							} else if (ZonedDateTime.class.isAssignableFrom(clazz.getComponentType())) {
								final ZonedDateTime[] arrayValue = new ZonedDateTime[jsonArray.size()];
								for (int i = 0; i < arrayValue.length; i++) {
									arrayValue[i] = DateUtilities.parseZonedDateTime(DateUtilities.ISO_8601_DATETIME_FORMAT, (String) jsonArray.getSimpleValue(i), ZoneId.systemDefault());
								}
								field.set(object, arrayValue);
							} else if (Enum.class.isAssignableFrom(clazz.getComponentType())) {
								final Object[] arrayValue = (Object[]) Array.newInstance(clazz.getComponentType(), jsonArray.size());
								for (int i = 0; i < arrayValue.length; i++) {
									final String enumName = (String) jsonArray.getSimpleValue(i);
									boolean enumFound = false;
									for (final Object enumConstant : clazz.getComponentType().getEnumConstants()) {
										if (enumName.equals(enumConstant.toString())) {
											arrayValue[i] = enumConstant;
											enumFound = true;
											break;
										}
									}
									if (enumFound == false) {
										throw new Exception("Invalid enum name '" + enumName + "' for type '" + clazz.getComponentType().getName() + "'");
									}
								}
								field.set(object, arrayValue);
							} else {
								final Object[] arrayValue = (Object[]) Array.newInstance(clazz.getComponentType(), jsonArray.size());
								for (int i = 0; i < arrayValue.length; i++) {
									final Object item = deserialize(clazz.getComponentType(), jsonArray.get(i));
									arrayValue[i] = item;
								}
								field.set(object, arrayValue);
							}
						} else if (List.class.isAssignableFrom(clazz)) {
							final List<Object> listOfItems = new ArrayList<>();
							final Class<?> genericType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
							for (final JsonNode arrayItem : (JsonArray) value) {
								final Object item = deserialize(genericType, arrayItem);
								listOfItems.add(item);
							}
							field.set(object, listOfItems);
						} else if (Set.class.isAssignableFrom(clazz)) {
							final Set<Object> setOfItems = new HashSet<>();
							final Class<?> genericType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
							for (final JsonNode arrayItem : (JsonArray) value) {
								final Object item = deserialize(genericType, arrayItem);
								setOfItems.add(item);
							}
							field.set(object, setOfItems);
						} else if (Collection.class.isAssignableFrom(clazz)) {
							final Collection<Object> collectionOfItems = new ArrayList<>();
							final Class<?> genericType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
							for (final JsonNode arrayItem : (JsonArray) value) {
								final Object item = deserialize(genericType, arrayItem);
								collectionOfItems.add(item);
							}
							field.set(object, collectionOfItems);
						} else if (Map.class.isAssignableFrom(clazz)) {
							final Map<Object, Object> mapObject = new HashMap<>();
							if (!(value instanceof JsonArray)) {
								throw new Exception("Invalid value type serialization value");
							}
							for (final JsonNode keyValueObject : (JsonArray) value) {
								if (!(keyValueObject instanceof JsonObject)) {
									throw new Exception("Invalid value type serialization value");
								}

								final Object keyObject = ((JsonObject) keyValueObject).get("key");
								final Object valueObject = ((JsonObject) keyValueObject).get("value");

								mapObject.put(keyObject, valueObject);
							}
							field.set(object, mapObject);
						} else if (value instanceof JsonObject) {
							field.set(object, deserialize(clazz, (JsonObject) value));
						} else {
							field.set(object, deserialize(clazz.getComponentType(), (JsonObject) value));
						}
					} catch (final Exception e) {
						throw new Exception("Invalid value type for field '" + entry.getKey() + "' in class '" + classType.toString() + "': " + e.getMessage(), e);
					}
				}
				return object;
			}
		} catch (final Exception e) {
			throw e;
		}
	}
}
