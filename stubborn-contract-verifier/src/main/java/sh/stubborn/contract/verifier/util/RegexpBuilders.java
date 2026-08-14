/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sh.stubborn.contract.verifier.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.DynamicString;
import sh.stubborn.contract.spec.internal.DynamicStringImpl;
import sh.stubborn.contract.spec.util.RegexpUtils;

/**
 * Useful utility methods to work with regular expressions.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public final class RegexpBuilders {

	private static final String WS = "/\\s*/";

	private static final Function<DslProperty<?>, @Nullable Object> CLIENT_VALUE_EXTRACTOR = DslProperty::getClientValue;

	private RegexpBuilders() {
	}

	/**
	 * Converts the {@link Object} passed values into their stub side String
	 * representations.
	 * @param o value to convert
	 * @return stub side String representation
	 */
	public static String buildGStringRegexpForStubSide(Object o) {
		if (o instanceof DslProperty) {
			return buildGStringRegexpForStubSide((DslProperty<?>) o);
		}
		else if (o instanceof Pattern) {
			return buildGStringRegexpForStubSide((Pattern) o);
		}
		else if (o instanceof DynamicString) {
			return buildGStringRegexpForStubSide((DynamicString) o);
		}
		return escapeSpecialRegexChars(o.toString());
	}

	/**
	 * Converts the {@link DynamicString} passed values into their stub side String
	 * representations.
	 * @param gString value to convert
	 * @return stub side String representation
	 */
	static String buildGStringRegexpForStubSide(DynamicString gString) {
		return new DynamicStringImpl(
				Stream.of(gString.getValues())
					.map(RegexpBuilders::buildGStringRegexpForStubSide)
					.map((s) -> (Object) s)
					.toArray(),
				Stream.of(gString.getStrings()).map(RegexpBuilders::escapeSpecialRegexChars).toArray(String[]::new))
			.toString();
	}

	/**
	 * Converts the {@link Pattern} passed values into their stub side String
	 * representations.
	 * @param pattern value to convert
	 * @return stub side String representation
	 */
	static String buildGStringRegexpForStubSide(Pattern pattern) {
		return pattern.pattern();
	}

	/**
	 * Converts the {@link sh.stubborn.contract.spec.internal.DslProperty} passed values
	 * into their stub side String representations.
	 * @param dslProperty value to convert
	 * @return stub side String representation
	 */
	static String buildGStringRegexpForStubSide(DslProperty<?> dslProperty) {
		return buildGStringRegexpForStubSide(Objects.requireNonNull(dslProperty.getClientValue()));
	}

	/**
	 * Converts the {@link DynamicString} passed values into their test side String
	 * representations.
	 * @param gString value to convert
	 * @return test side String representation
	 */
	public static String buildGStringRegexpForTestSide(DynamicString gString) {
		return new DynamicStringImpl(
				Stream.of(gString.getValues())
					.map(RegexpBuilders::buildGStringRegexpForTestSide)
					.map((s) -> (Object) s)
					.toArray(),
				Stream.of(gString.getStrings()).map(RegexpBuilders::escapeSpecialRegexChars).toArray(String[]::new))
			.toString();
	}

	/**
	 * Converts the {@link Object} passed values into their test side String
	 * representations.
	 * @param o value to convert
	 * @return test side String representation
	 */
	public static String buildGStringRegexpForTestSide(Object o) {
		return o.toString().replaceAll("\\\\", "\\\\\\\\");
	}

	public static String escapeSpecialRegexChars(String str) {
		return RegexpUtils.escapeSpecialRegexChars(str);
	}

	public static String buildJSONRegexpMatch(DynamicString gString) {
		return buildJSONRegexpMatch(ContentUtils.extractValue(gString, ContentType.JSON, CLIENT_VALUE_EXTRACTOR));
	}

	public static String buildJSONRegexpMatch(Map<String, Object> jsonMap) {
		return WS + "\\{"
				+ jsonMap.entrySet().stream().map(RegexpBuilders::buildJSONRegexpMatch).collect(Collectors.joining(","))
				+ "\\}" + WS;
	}

	public static String buildJSONRegexpMatch(List<?> jsonList) {
		return WS + "\\[" + jsonList.stream().map(RegexpBuilders::buildJSONRegexpMatch).collect(Collectors.joining(","))
				+ "\\]" + WS;
	}

	/**
	 * Converts the map into String representation of regular expressions.
	 * @param entry map entry to convert
	 * @return string representation of regular expressions
	 */
	public static String buildJSONRegexpMatch(Map.Entry<String, Object> entry) {
		return buildJSONRegexpMatchString(StringEscapeUtils.escapeJson(entry.getKey())) + ":"
				+ buildJSONRegexpMatch(entry.getValue());
	}

	/**
	 * Converts the object into String representation of regular expressions.
	 * @param value object to convert
	 * @return string representation of regular expressions
	 */
	public static String buildJSONRegexpMatch(Object value) {
		return buildJSONRegexpMatchStringOptionalQuotes(StringEscapeUtils.escapeJson(value.toString()));
	}

	/**
	 * Converts the pattern into String representation of regular expressions.
	 * @param pattern the pattern to convert
	 * @return string representation of regular expressions
	 */
	public static String buildJSONRegexpMatch(Pattern pattern) {
		return buildJSONRegexpMatchStringOptionalQuotes(pattern.pattern());
	}

	/**
	 * Converts the String into String representation of regular expressions.
	 * @param value the value to convert
	 * @return string representation of regular expressions
	 */
	public static String buildJSONRegexpMatchString(String value) {
		return WS + '"' + value + '"' + WS;
	}

	/**
	 * Converts the String into an optional String representation of regular expressions.
	 * @param value the value to convert
	 * @return optional string representation of regular expressions
	 */
	public static String buildJSONRegexpMatchStringOptionalQuotes(String value) {
		return WS + "\"?" + value + "\"?" + WS;
	}

}
