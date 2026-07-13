package com.toomuchcoding.xmlassert;

import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.wst.xml.xpath2.api.Item;
import org.eclipse.wst.xml.xpath2.api.ResultSequence;

class ArrayValueAssertion extends FieldAssertion implements XmlArrayVerifiable {

	final boolean checkingPrimitiveType;

	protected ArrayValueAssertion(XmlCachedObjects cachedObjects, LinkedList<String> xPathBuffer,
			LinkedList<String> specialCaseXPathBuffer, Object arrayName,
			XmlAsserterConfiguration xmlAsserterConfiguration) {
		super(cachedObjects, xPathBuffer, specialCaseXPathBuffer, arrayName, xmlAsserterConfiguration);
		this.checkingPrimitiveType = true;
	}

	protected ArrayValueAssertion(XmlCachedObjects cachedObjects, LinkedList<String> xPathBuffer,
			LinkedList<String> specialCaseXPathBuffer, Object arrayName,
			XmlAsserterConfiguration xmlAsserterConfiguration, boolean checkingPrimitiveType) {
		super(cachedObjects, xPathBuffer, specialCaseXPathBuffer, arrayName, xmlAsserterConfiguration);
		this.checkingPrimitiveType = checkingPrimitiveType;
	}

	protected ArrayValueAssertion(XmlCachedObjects cachedObjects, LinkedList<String> xPathBuffer,
			LinkedList<String> specialCaseXPathBuffer, XmlAsserterConfiguration xmlAsserterConfiguration) {
		super(cachedObjects, xPathBuffer, specialCaseXPathBuffer, null, xmlAsserterConfiguration);
		this.checkingPrimitiveType = true;
	}

	protected ArrayValueAssertion(XmlAsserter asserter, boolean checkingPrimitiveType) {
		super(asserter);
		this.checkingPrimitiveType = checkingPrimitiveType;
	}

	@Override
	public XmlArrayVerifiable contains(String value) {
		return new ArrayValueAssertion(cachedObjects, xPathBuffer, specialCaseXPathBuffer, value,
				xmlAsserterConfiguration, false);
	}

	@Override
	public XmlArrayVerifiable hasSize(int size) {
		String xPath = "count(" + createXPathString() + ")";
		ArrayValueAssertion verifiable = new ArrayValueAssertion(this, this.checkingPrimitiveType);
		verifiable.specialCaseXPathBuffer.clear();
		verifiable.specialCaseXPathBuffer.add(xPath);
		String xPathString = verifiable.createSpecialCaseXPathString();
		ResultSequence sequence = verifiable.resultSequence(xPathString);
		Iterator<Item> iterator = sequence.iterator();
		if (!iterator.hasNext()) {
			throw new IllegalStateException(
					"Parsed XML [" + cachedObjects.xmlAsString + "] doesn't match the XPath <" + xPathString + ">");
		}
		int retrievedSize = Integer.valueOf(iterator.next().getStringValue());
		if (retrievedSize != size) {
			throw new IllegalStateException("Parsed XML [" + cachedObjects.xmlAsString + "] has size [" + retrievedSize
					+ "] and not [" + size + "] for XPath <" + xPathString + "> ");
		}
		return verifiable;
	}

	@Override
	public FieldAssertion node(String value) {
		FieldAssertion assertion = super.node(value);
		return new ArrayValueAssertion(assertion, false);
	}

	@Override
	public FieldAssertion node(String... nodeNames) {
		FieldAssertion assertion = super.node(nodeNames);
		return new ArrayValueAssertion(assertion, false);
	}

	@Override
	protected void removeLastFieldElement(XmlAsserter readyToCheck) {
		readyToCheck.xPathBuffer.removeLast();
	}

	@Override
	public XmlVerifiable isEqualTo(String value) {
		if (!checkingPrimitiveType) {
			return super.isEqualTo(value);
		}
		return equalityOnAPrimitive("[text()=" + escapeText(value) + "]");
	}

	@Override
	public XmlVerifiable isEqualTo(Number value) {
		if (!checkingPrimitiveType) {
			return super.isEqualTo(value);
		}
		return equalityOnAPrimitive("[number()=" + String.valueOf(value) + "]");
	}

	private XmlVerifiable equalityOnAPrimitive(String xPath) {
		ReadyToCheckAsserter readyToCheck = new ReadyToCheckAsserter(cachedObjects, xPathBuffer, fieldName,
				xmlAsserterConfiguration);
		readyToCheck.xPathBuffer.removeLast();
		readyToCheck.xPathBuffer.offer(xPath);
		readyToCheck.checkBufferedXPathString();
		return readyToCheck;
	}

	@Override
	public XmlVerifiable matches(String value) {
		if (!checkingPrimitiveType) {
			return super.matches(value);
		}
		return equalityOnAPrimitive("[matches(text(), " + escapeText(escapeRegex(value)) + ")]");
	}

	@Override
	public XmlVerifiable isEqualTo(Boolean value) {
		if (!checkingPrimitiveType) {
			return super.isEqualTo(value);
		}
		return isEqualTo(String.valueOf(value));
	}

	@Override
	public boolean isAssertingAValueInArray() {
		return true;
	}

}