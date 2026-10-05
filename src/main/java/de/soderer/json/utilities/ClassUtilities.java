package de.soderer.json.utilities;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflection helper methods.
 */
public class ClassUtilities {
	/**
	 * Utility class, not to be instantiated.
	 */
	private ClassUtilities() {
	}

	/**
	 * Returns the constructor without parameters of a class, regardless of its visibility.
	 *
	 * @param clazz
	 *            the class
	 * @return the constructor without parameters
	 * @throws NoSuchMethodException
	 *             if the class has no constructor without parameters
	 * @throws SecurityException
	 *             if access is denied by a security manager
	 */
	public static Constructor<?> getConstructor(final Class<?> clazz) throws NoSuchMethodException, SecurityException {
		return clazz.getDeclaredConstructor();
	}

	/**
	 * Returns all fields declared by a class and its super classes, regardless of their visibility.
	 * Fields of the class itself come first.
	 *
	 * @param clazz
	 *            the class
	 * @return all fields
	 */
	public static List<Field> getAllFields(final Class<?> clazz) {
		final List<Field> fields = new ArrayList<>();
		Class<?> currentClass = clazz;
		while (currentClass != null) {
			for (final Field field : currentClass.getDeclaredFields()) {
				fields.add(field);
			}
			currentClass = currentClass.getSuperclass();
		}
		return fields;
	}

	/**
	 * Returns a field declared by a class or one of its super classes, regardless of its visibility.
	 *
	 * @param clazz
	 *            the class
	 * @param fieldName
	 *            the name of the field
	 * @return the field, declared by the class itself or the nearest super class
	 * @throws NoSuchFieldException
	 *             if neither the class nor a super class declares the field
	 */
	public static Field getField(final Class<?> clazz, final String fieldName) throws NoSuchFieldException {
		Class<?> currentClass = clazz;
		while (true) {
			try {
				return currentClass.getDeclaredField(fieldName);
			} catch (final NoSuchFieldException e) {
				currentClass = currentClass.getSuperclass();
				if (currentClass == null) {
					throw e;
				}
			}
		}
	}
}
