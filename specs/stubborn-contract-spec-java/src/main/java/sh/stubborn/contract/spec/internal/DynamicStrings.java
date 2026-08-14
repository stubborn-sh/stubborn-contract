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

package sh.stubborn.contract.spec.internal;

import groovy.lang.GString;
import org.jspecify.annotations.Nullable;

/**
 * Converts a Groovy {@code GString} — produced by an interpolated body/header/url in the
 * Groovy DSL (e.g. {@code body("id=${value(...)}")}) — into the pure-Java
 * {@link DynamicString} the framework-agnostic core understands.
 *
 * <p>
 * The conversion is applied at the {@link DslProperty} construction boundary, so the
 * contract model never stores a {@code GString}: whichever way a contract is authored
 * (Groovy DSL, Java DSL, YAML), the core only ever sees {@link DynamicString}s. It is a
 * structure-preserving shallow wrap — the interpolated values (typically
 * {@link DslProperty} / {@link RegexProperty}) are kept as-is so per-side (client /
 * server) resolution behaves exactly as it did against the original {@code GString}. This
 * is the one place in the project that references {@code GString} outside the Groovy
 * DSL/parsing layer, and it lives in the spec module where Groovy is an allowed
 * dependency.
 *
 * @author Marcin Grzejszczak
 */
final class DynamicStrings {

	private DynamicStrings() {
	}

	/**
	 * Returns a {@link DynamicString} in place of a {@code GString} (recursing into the
	 * {@code GString}'s own interpolated values in case one is itself a {@code GString});
	 * any other value is returned unchanged.
	 * @param value the value to normalise
	 * @return the value with {@code GString}s replaced by {@link DynamicString}s
	 */
	static @Nullable Object normalize(@Nullable Object value) {
		if (value instanceof GString) {
			GString gstring = (GString) value;
			Object[] originalValues = gstring.getValues();
			@Nullable Object[] normalizedValues = new @Nullable Object[originalValues.length];
			for (int i = 0; i < originalValues.length; i++) {
				normalizedValues[i] = normalize(originalValues[i]);
			}
			return new DynamicStringImpl(normalizedValues, gstring.getStrings());
		}
		return value;
	}

}
