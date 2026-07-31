package com.toomuchcoding.xmlassert;

import java.util.LinkedList;

import org.jspecify.annotations.Nullable;

class FieldAssertion extends XmlAsserter {

	protected FieldAssertion(XmlCachedObjects cachedObjects, LinkedList<String> xPathBuffer,
			LinkedList<String> specialCaseXPathBuffer, @Nullable Object value,
			XmlAsserterConfiguration xmlAsserterConfiguration) {
		super(cachedObjects, xPathBuffer, specialCaseXPathBuffer, value, xmlAsserterConfiguration);
	}

	protected FieldAssertion(XmlAsserter asserter) {
		super(asserter);
	}

}