package com.toomuchcoding.xmlassert;

import java.util.LinkedList;

class FieldAssertion extends XmlAsserter {

	protected FieldAssertion(XmlCachedObjects cachedObjects, LinkedList<String> xPathBuffer,
			LinkedList<String> specialCaseXPathBuffer, Object value,
			XmlAsserterConfiguration xmlAsserterConfiguration) {
		super(cachedObjects, xPathBuffer, specialCaseXPathBuffer, value, xmlAsserterConfiguration);
	}

	protected FieldAssertion(XmlAsserter asserter) {
		super(asserter);
	}

}