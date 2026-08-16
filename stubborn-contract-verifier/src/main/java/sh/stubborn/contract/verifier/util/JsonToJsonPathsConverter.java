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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.spec.internal.BodyMatcher;
import sh.stubborn.contract.spec.internal.BodyMatchers;
import sh.stubborn.contract.spec.internal.DynamicString;
import sh.stubborn.jsonassert.JsonAssertion;
import tools.jackson.databind.json.JsonMapper;

/**
 * Converts JSON to a set of JSON paths together with methods needed to be called to build
 * them for test assertions.
 *
 * <p>
 * When {@code spring.cloud.contract.verifier.assert.size} is set to {@code true}, array
 * elements are verified in order using exact indices instead of wildcard matching.
 * </p>
 *
 * @author Marcin Grzejszczak
 * @author Tim Ysewyn
 * @author Olga Maciaszek-Sharma
 * @since 1.0.0
 * @see JsonPathTraverser
 * @see JsonPathMatcherUtils
 */
public class JsonToJsonPathsConverter {

	private static final Log log = LogFactory.getLog(JsonToJsonPathsConverter.class);

	/**
	 * System property to enable size and order assertions on arrays.
	 */
	private static final String SIZE_ASSERTION_SYSTEM_PROP = "stubborn.contract.verifier.assert.size";

	/**
	 * Deprecated system property retained for backward compatibility with Spring Cloud
	 * Contract; superseded by {@link #SIZE_ASSERTION_SYSTEM_PROP}.
	 */
	private static final String LEGACY_SIZE_ASSERTION_SYSTEM_PROP = "spring.cloud.contract.verifier.assert.size";

	private static final AtomicBoolean LEGACY_SIZE_ASSERTION_WARNED = new AtomicBoolean();

	private static final boolean SERVER_SIDE = false;

	private static final boolean CLIENT_SIDE = true;

	private final boolean assertJsonSize;

	public JsonToJsonPathsConverter(boolean assertJsonSize) {
		this.assertJsonSize = assertJsonSize;
	}

	public JsonToJsonPathsConverter() {
		this(false);
	}

	// ========== Public API - Test Side ==========

	/**
	 * Transforms JSON to JSON paths with test (server) side values.
	 * @param json the JSON to transform
	 * @return set of JSON paths with assertions
	 */
	public JsonPaths transformToJsonPathWithTestsSideValues(Object json) {
		return transform(json, SERVER_SIDE, MapConverter.JSON_PARSING_FUNCTION, false);
	}

	/**
	 * Transforms JSON to JSON paths with test (server) side values.
	 * @param json the JSON to transform
	 * @param includeEmptyCheck whether to include empty check
	 * @return set of JSON paths with assertions
	 */
	public JsonPaths transformToJsonPathWithTestsSideValues(Object json, boolean includeEmptyCheck) {
		return transform(json, SERVER_SIDE, MapConverter.JSON_PARSING_FUNCTION, includeEmptyCheck);
	}

	/**
	 * Transforms JSON to JSON paths with test (server) side values.
	 * @param json the JSON to transform
	 * @param parsingFunction function to parse JSON strings
	 * @param includeEmptyCheck whether to include empty check
	 * @return set of JSON paths with assertions
	 */
	public JsonPaths transformToJsonPathWithTestsSideValues(Object json, Function<String, Object> parsingFunction,
			boolean includeEmptyCheck) {
		return transform(json, SERVER_SIDE, parsingFunction, includeEmptyCheck);
	}

	// ========== Public API - Stub Side ==========

	/**
	 * Transforms JSON to JSON paths with stub (client) side values.
	 * @param json the JSON to transform
	 * @return set of JSON paths with assertions
	 */
	public JsonPaths transformToJsonPathWithStubsSideValues(Object json) {
		return transform(json, CLIENT_SIDE, MapConverter.JSON_PARSING_FUNCTION, false);
	}

	/**
	 * Transforms JSON to JSON paths with stub (client) side values.
	 * @param json the JSON to transform
	 * @param includeEmptyCheck whether to include empty check
	 * @return set of JSON paths with assertions
	 */
	public JsonPaths transformToJsonPathWithStubsSideValues(Object json, boolean includeEmptyCheck) {
		return transform(json, CLIENT_SIDE, MapConverter.JSON_PARSING_FUNCTION, includeEmptyCheck);
	}

	/**
	 * Transforms JSON to JSON paths with stub side values without array size check.
	 * @param json the JSON to transform
	 * @return set of JSON paths with assertions
	 */
	public static JsonPaths transformToJsonPathWithStubsSideValuesAndNoArraySizeCheck(Object json) {
		return new JsonToJsonPathsConverter().transform(json, CLIENT_SIDE, MapConverter.JSON_PARSING_FUNCTION, false);
	}

	// ========== Delegating Static Methods ==========

	/**
	 * Removes JSON path matching entries from the parsed JSON.
	 * @param json parsed JSON
	 * @param bodyMatchers matchers to remove
	 * @return json with removed entries
	 * @see JsonPathMatcherUtils#removeMatchingJsonPaths(Object, BodyMatchers)
	 */
	public static Object removeMatchingJsonPaths(Object json, @Nullable BodyMatchers bodyMatchers) {
		return JsonPathMatcherUtils.removeMatchingJsonPaths(json, bodyMatchers);
	}

	/**
	 * Retrieves a value from JSON via json path.
	 * @param json parsed JSON
	 * @param jsonPath path to read
	 * @return matching part of the json
	 * @see JsonPathMatcherUtils#readElement(Object, String)
	 */
	public static Object readElement(Object json, String jsonPath) {
		return JsonPathMatcherUtils.readElement(json, jsonPath);
	}

	/**
	 * Converts a BodyMatcher to a JSON path with regex/equality check.
	 * @param bodyMatcher the body matcher
	 * @return the JSON path with condition
	 * @see JsonPathMatcherUtils#convertJsonPathAndRegexToAJsonPath(BodyMatcher)
	 */
	public static String convertJsonPathAndRegexToAJsonPath(BodyMatcher bodyMatcher) {
		return JsonPathMatcherUtils.convertJsonPathAndRegexToAJsonPath(bodyMatcher);
	}

	/**
	 * Converts a BodyMatcher to a JSON path with regex/equality check.
	 * @param bodyMatcher the body matcher
	 * @param body the body to read from (required for EQUALITY matching)
	 * @return the JSON path with condition
	 * @see JsonPathMatcherUtils#convertJsonPathAndRegexToAJsonPath(BodyMatcher, Object)
	 */
	public static String convertJsonPathAndRegexToAJsonPath(BodyMatcher bodyMatcher, Object body) {
		return JsonPathMatcherUtils.convertJsonPathAndRegexToAJsonPath(bodyMatcher, body);
	}

	/**
	 * Returns generated value if the value is a RegexProperty.
	 * @param value the value to check
	 * @return generated value or original value
	 * @see JsonPathMatcherUtils#generatedValueIfNeeded(Object)
	 */
	public static Object generatedValueIfNeeded(Object value) {
		return JsonPathMatcherUtils.generatedValueIfNeeded(value);
	}

	// ========== Main Transformation Logic ==========

	private JsonPaths transform(Object json, boolean clientSide, Function<String, Object> parsingFunction,
			boolean includeEmptyCheck) {
		if (json == null || (isEmptyJson(json) && !includeEmptyCheck)) {
			return new JsonPaths();
		}

		Object convertedJson = MapConverter.getClientOrServerSideValues(json, clientSide, parsingFunction);
		Object jsonWithPatterns = ContentUtils.convertDslPropsToTemporaryRegexPatterns(convertedJson, parsingFunction);

		MethodBufferingJsonVerifiable rootVerifiable = new DelegatingJsonVerifiable(
				JsonAssertion.assertThat(new JsonMapper().writeValueAsString(jsonWithPatterns))
					.withoutThrowingException());

		JsonPaths pathsAndValues = new JsonPaths();

		if (isRootElement(rootVerifiable) && isEmptyJson(json)) {
			pathsAndValues.add(rootVerifiable.isEmpty());
			return pathsAndValues;
		}

		boolean useOrderedVerification = shouldUseOrderedVerification();
		JsonPathTraverser traverser = new JsonPathTraverser(useOrderedVerification, parsingFunction);
		traverser.traverse(jsonWithPatterns, rootVerifiable, pathsAndValues::add);

		return pathsAndValues;
	}

	// ========== Helper Methods ==========

	private boolean shouldUseOrderedVerification() {
		String systemProp = sizeAssertionSystemProperty();
		return (systemProp != null && Boolean.parseBoolean(systemProp)) || this.assertJsonSize;
	}

	/**
	 * Resolves the array size/order assertion system property, preferring the canonical
	 * {@code stubborn.contract.verifier.assert.size} and falling back to the deprecated
	 * {@code spring.cloud.contract.verifier.assert.size} (logging a one-off warning) so
	 * existing Spring Cloud Contract setups keep working.
	 * @return the resolved property value, or {@code null} if neither is set
	 */
	private static @Nullable String sizeAssertionSystemProperty() {
		String current = System.getProperty(SIZE_ASSERTION_SYSTEM_PROP);
		if (current != null) {
			return current;
		}
		String legacy = System.getProperty(LEGACY_SIZE_ASSERTION_SYSTEM_PROP);
		if (legacy != null && LEGACY_SIZE_ASSERTION_WARNED.compareAndSet(false, true)) {
			log.warn("The '" + LEGACY_SIZE_ASSERTION_SYSTEM_PROP + "' system property is deprecated; use '"
					+ SIZE_ASSERTION_SYSTEM_PROP + "' instead. "
					+ "The legacy property will be removed in the next major release.");
		}
		return legacy;
	}

	private boolean isRootElement(MethodBufferingJsonVerifiable key) {
		return "$".equals(key.jsonPath());
	}

	private boolean isEmptyJson(Object json) {
		if (json == null) {
			return true;
		}
		if (json instanceof String) {
			return ((String) json).isEmpty();
		}
		if (json instanceof DynamicString) {
			return ((DynamicString) json).toString().isEmpty();
		}
		if (json instanceof Map) {
			return ((Map<?, ?>) json).isEmpty();
		}
		if (json instanceof List) {
			return ((List<?>) json).isEmpty();
		}
		return false;
	}

}
