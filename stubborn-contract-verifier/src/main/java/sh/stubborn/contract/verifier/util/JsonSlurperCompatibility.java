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

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Parses JSON text into plain Java objects, reproducing the behaviour of the Groovy
 * {@code JsonSlurper} that this helper replaces so that legacy Spring Cloud Contract
 * Groovy DSL contracts keep producing byte-identical stubs.
 *
 * <p>
 * Concretely this means: object literals become insertion-ordered
 * {@link java.util.LinkedHashMap}s, arrays become {@link java.util.ArrayList}s and
 * floating-point literals are read as {@link java.math.BigDecimal} (via
 * {@link DeserializationFeature#USE_BIG_DECIMAL_FOR_FLOATS}) so that their textual scale
 * (e.g. {@code 1.20}) survives a parse/serialize round-trip — exactly as
 * {@code JsonSlurper} did. On malformed input the underlying Jackson parser throws an
 * unchecked {@link tools.jackson.core.JacksonException}, which callers catch where they
 * previously caught {@code groovy.json.JsonException}.
 *
 * @author Marcin Grzejszczak
 */
final class JsonSlurperCompatibility {

	private static final JsonMapper MAPPER = JsonMapper.builder()
		.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
		.build();

	private JsonSlurperCompatibility() {
	}

	static Object parse(String json) {
		return MAPPER.readValue(json, Object.class);
	}

}
