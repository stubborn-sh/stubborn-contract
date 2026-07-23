package com.toomuchcoding.jsonassert;

import com.jayway.jsonpath.DocumentContext;

import java.util.LinkedList;

import org.jspecify.annotations.Nullable;

class ReadyToCheckAsserter extends JsonAsserter {

	public ReadyToCheckAsserter(DocumentContext parsedJson, LinkedList<String> jsonPathBuffer,
			@Nullable Object fieldName, JsonAsserterConfiguration jsonAsserterConfiguration) {
		super(parsedJson, jsonPathBuffer, fieldName, jsonAsserterConfiguration);
	}

}