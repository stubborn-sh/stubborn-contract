package com.toomuchcoding.jsonassert;

import com.jayway.jsonpath.DocumentContext;

import java.util.LinkedList;

import org.jspecify.annotations.Nullable;

class FieldAssertion extends JsonAsserter {

	protected FieldAssertion(DocumentContext parsedJson, LinkedList<String> jsonPathBuffer, @Nullable Object value,
			JsonAsserterConfiguration jsonAsserterConfiguration) {
		super(parsedJson, jsonPathBuffer, value, jsonAsserterConfiguration);
	}

}