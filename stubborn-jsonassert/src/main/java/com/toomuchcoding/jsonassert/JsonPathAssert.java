package com.toomuchcoding.jsonassert;

import org.assertj.core.api.AbstractAssert;

import com.jayway.jsonpath.DocumentContext;

/**
 * A AssertJ version of JSON Assert.
 *
 * The methods used by JSON Assert are available as assertions of either
 * {@link DocumentContext} or {@link JsonVerifiable}.
 *
 * Remember that the order of execution matters since it's building the JSON Path in the
 * provided sequence.
 *
 * @author Marcin Grzejszczak
 * @since 0.2.0
 */
public class JsonPathAssert extends AbstractAssert<JsonPathAssert, JsonVerifiable> {

	public JsonPathAssert(DocumentContext actual) {
		super(JsonAssertion.assertThatJson(actual), JsonPathAssert.class);
	}

	public JsonPathAssert(JsonVerifiable actual) {
		super(actual, JsonPathAssert.class);
	}

	/**
	 * @see JsonVerifiable#contains(Object)
	 */
	public JsonPathAssert contains(Object value) {
		isNotNull();
		return new JsonPathAssert(actual.contains(value));
	}

	/**
	 * @see JsonVerifiable#field(Object)
	 */
	public JsonPathAssert field(Object value) {
		isNotNull();
		return new JsonPathAssert(actual.field(value));
	}

	/**
	 * @see JsonVerifiable#field(String...)
	 */
	public JsonPathAssert field(String... value) {
		isNotNull();
		return new JsonPathAssert(actual.field(value));
	}

	/**
	 * @see JsonVerifiable#array()} (Object)
	 */
	public JsonPathAssert array(Object value) {
		isNotNull();
		return new JsonPathAssert(actual.array(value));
	}

	/**
	 * @see JsonVerifiable#arrayField()
	 */
	public JsonPathAssert arrayField() {
		isNotNull();
		return new JsonPathAssert(actual.arrayField());
	}

	/**
	 * @see JsonVerifiable#array()
	 */
	public JsonPathAssert array() {
		isNotNull();
		return new JsonPathAssert(actual.array());
	}

	/**
	 * @see JsonVerifiable#isEqualTo(String)
	 */
	public JsonPathAssert isEqualTo(String value) {
		isNotNull();
		try {
			return new JsonPathAssert(actual.isEqualTo(value));
		}
		catch (IllegalStateException e) {
			failWithMessage("Expected JSON to match JSON Path <%s> but it didn't", actual.jsonPath());
			return this;
		}
	}

	/**
	 * @see JsonVerifiable#isEqualTo(Number)
	 */
	public JsonPathAssert isEqualTo(Number value) {
		isNotNull();
		try {
			return new JsonPathAssert(actual.isEqualTo(value));
		}
		catch (IllegalStateException e) {
			failWithMessage("Expected JSON to match JSON Path <%s> but it didn't", actual.jsonPath());
			return this;
		}
	}

	/**
	 * @see JsonVerifiable#isInstanceOf(Class)
	 */
	public JsonPathAssert isInstanceOf(Class clazz) {
		isNotNull();
		try {
			return new JsonPathAssert(actual.isInstanceOf(clazz));
		}
		catch (IllegalStateException e) {
			failWithMessage(e.getMessage());
			return this;
		}
	}

	/**
	 * @see JsonVerifiable#matches(String)
	 */
	public JsonPathAssert matches(String value) {
		isNotNull();
		try {
			return new JsonPathAssert(actual.matches(value));
		}
		catch (IllegalStateException e) {
			failWithMessage("Expected JSON to match JSON Path <%s> but it didn't", actual.jsonPath());
			return this;
		}
	}

	/**
	 * @see JsonVerifiable#isEqualTo(Boolean)
	 */
	public JsonPathAssert isEqualTo(Boolean value) {
		isNotNull();
		try {
			return new JsonPathAssert(actual.isEqualTo(value));
		}
		catch (IllegalStateException e) {
			failWithMessage("Expected JSON to match JSON Path <%s> but it didn't", actual.jsonPath());
			return this;
		}
	}

	/**
	 * @see JsonVerifiable#value()
	 */
	public JsonPathAssert value() {
		isNotNull();
		try {
			return new JsonPathAssert(actual.value());
		}
		catch (IllegalStateException e) {
			failWithMessage("Expected JSON to match JSON Path <%s> but it didn't", actual.jsonPath());
			return this;
		}
	}

	/**
	 * @see JsonVerifiable#isNull()
	 */
	@Override
	public void isNull() {
		isNotNull();
		try {
			actual.isNull();
		}
		catch (IllegalStateException e) {
			failWithMessage("Expected JSON to match JSON Path <%s> but it didn't", actual.jsonPath());
		}
	}

	/**
	 * @see JsonVerifiable#matchesJsonPath(String)
	 */
	public JsonPathAssert matchesJsonPath(String jsonPath) {
		isNotNull();
		try {
			actual.matchesJsonPath(jsonPath);
		}
		catch (IllegalStateException e) {
			failWithMessage("Expected JSON to match JSON Path <%s> but it didn't", jsonPath);
		}
		return this;
	}

	/**
	 * @see JsonVerifiable#isEmpty()
	 */
	public JsonPathAssert isEmpty() {
		isNotNull();
		try {
			actual.isEmpty();
		}
		catch (IllegalStateException e) {
			failWithMessage("Expected JSON to with JSON Path <%s> to be empty", actual.jsonPath());
		}
		return this;
	}

}