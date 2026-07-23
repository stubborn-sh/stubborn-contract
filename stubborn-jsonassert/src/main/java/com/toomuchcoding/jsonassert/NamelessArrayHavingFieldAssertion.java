package com.toomuchcoding.jsonassert;

import com.jayway.jsonpath.DocumentContext;

import java.util.LinkedList;

import org.jspecify.annotations.Nullable;

class NamelessArrayHavingFieldAssertion extends FieldAssertion {

	protected NamelessArrayHavingFieldAssertion(DocumentContext parsedJson, LinkedList<String> jsonPathBuffer,
			@Nullable Object fieldName, JsonAsserterConfiguration jsonAsserterConfiguration) {
		super(parsedJson, jsonPathBuffer, fieldName, jsonAsserterConfiguration);
	}

	@Override
	public boolean isIteratingOverNamelessArray() {
		return true;
	}

}