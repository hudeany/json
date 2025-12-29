package de.soderer.utilities;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class TestObjectSimple {
	public static String STATICFIELD = "textField1 value";

	private transient String transientField = "textField1 value";

	String packageTextField = "packageTextField value";
	private final String privateTextField = "privateTextField value";
	protected String protectedTextField = "protectedTextField value";
	public String publicTextField = "publicTextField value";

	private final int intField = 1;
	private final char charField = 'a';
	private final byte byteField = 0x4B;
	private final short shortField = 5;
	private final float floatField = (float) Math.PI;
	private final double doubleField = Math.PI;
	private final boolean boolField = true;
	private final BigDecimal bigDecimalField = new BigDecimal("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
	private final Object nullField = null;
	private final Object[] arrayField;
	private final List<Object> listField;
	private final Set<Object> setField;

	public TestObjectSimple() {
		arrayField = new Object[] { 1, 2, 3, "Text Value", true, null };

		listField = new ArrayList<>();
		listField.add(1);
		listField.add(2);
		listField.add(3);
		listField.add("Text Value");
		listField.add(true);
		listField.add(null);

		setField = new HashSet<>();
		setField.add(1);
		setField.add(2);
		setField.add(3);
		setField.add("Text Value");
		setField.add(true);
		setField.add(null);
	}
}
