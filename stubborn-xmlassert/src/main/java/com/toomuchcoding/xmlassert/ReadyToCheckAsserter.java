package com.toomuchcoding.xmlassert;

import java.util.LinkedList;

import org.jspecify.annotations.Nullable;

class ReadyToCheckAsserter extends XmlAsserter {

	public ReadyToCheckAsserter(XmlCachedObjects cachedObjects, LinkedList<String> xPathBuffer,
			@Nullable Object fieldName, XmlAsserterConfiguration xmlAsserterConfiguration) {
		super(cachedObjects, xPathBuffer, new LinkedList<String>(), fieldName, xmlAsserterConfiguration);
	}

	@Override
	protected boolean isReadyToCheck() {
		return true;
	}

}