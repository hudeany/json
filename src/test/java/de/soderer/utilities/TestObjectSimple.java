package de.soderer.utilities;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class TestObjectSimple {
	public static String STATICFIELD = "textField1 value";
	
	private transient String transientField = "textField1 value";

	String packageTextField = "packageTextField value";
	private String privateTextField = "privateTextField value";
	protected String protectedTextField = "protectedTextField value";
	public String publicTextField = "publicTextField value";
	
	private int intField = 1;
	private char charField = 'a';
	private byte byteField = 0x4B;
	private short shortField = 5;
	private float floatField = (float) Math.PI;
	private double doubleField = Math.PI;
	private boolean boolField = true;
	private BigDecimal bigDecimalField = new BigDecimal("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
	private Object nullField = null;
	private Object[] arrayField;
	private List<Object> listField;
	private Set<Object> setField;
	
	public TestObjectSimple() {
		arrayField = new Object[] { 1, 2, 3, "Text Value", true, null };
		
		listField = new ArrayList<Object>();
		listField.add(1);
		listField.add(2);
		listField.add(3);
		listField.add("Text Value");
		listField.add(true);
		listField.add(null);
		
		setField = new HashSet<Object>();
		setField.add(1);
		setField.add(2);
		setField.add(3);
		setField.add("Text Value");
		setField.add(true);
		setField.add(null);
	}
}
