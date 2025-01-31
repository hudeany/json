package de.soderer.utilities;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class TestObjectComplexForSimpleJson {
	public static String STATICFIELD = "textField1 value";

	private transient String transientField = "textField1 value";

	String packageTextField = "packageTextField value";
	private final String privateTextField = "privateTextField value";
	protected String protectedTextField = "protectedTextField value";
	public String publicTextField = "publicTextField value";

	private int id = 0;

	private final int intField = 1;
	private final char charField = 'a';
	private final byte byteField = 0x4B;
	private final short shortField = 5;
	private final float floatField = (float) Math.PI;
	private final double doubleField = Math.PI;
	private final boolean boolField = true;
	private final BigDecimal bigDecimalField = new BigDecimal("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
	private final Object nullField = null;
	private TestObjectComplexForSimpleJson otherObjectField = null;
	private List<TestObjectSimple> typedListField;

	public TestObjectComplexForSimpleJson() {
		otherObjectField = new TestObjectComplexForSimpleJson(4);

		typedListField = new ArrayList<>();
		typedListField.add(new TestObjectSimple());
	}

	private TestObjectComplexForSimpleJson(final int id) {
		this.id = id;
	}
}
