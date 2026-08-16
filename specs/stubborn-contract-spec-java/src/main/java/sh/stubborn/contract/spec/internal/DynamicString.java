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

import org.jspecify.annotations.Nullable;

/**
 * A pure-Java representation of an interpolated string — the "value with holes" that the
 * Groovy DSL expresses as a {@code GString} (for example
 * {@code body("id=${value(consumer(...), producer(...))}")}).
 *
 * <p>
 * It mirrors the two arrays a {@code GString} exposes: the literal {@link #getStrings()
 * string fragments} and the interpolated {@link #getValues() values} that sit between
 * them (typically {@link DslProperty}, {@link RegexProperty} or plain objects). Keeping
 * this a first-class Java type lets the framework-agnostic core process Groovy-authored
 * bodies without depending on the Groovy runtime: a {@code GString} produced while
 * parsing a {@code .groovy} contract is normalised into a {@link DynamicString} at the
 * parse boundary, structure-preserving, so downstream client/server-side resolution
 * behaves exactly as it did against the original {@code GString}.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public interface DynamicString extends CharSequence {

	/**
	 * The interpolated values sitting between the literal string fragments. Preserved
	 * as-is (e.g. {@link DslProperty} / {@link RegexProperty}) so each side can still be
	 * resolved lazily.
	 * @return the interpolated values
	 */
	@Nullable Object[] getValues();

	/**
	 * The literal string fragments surrounding the interpolated values. By convention
	 * {@code getStrings().length == getValues().length + 1}.
	 * @return the literal string fragments
	 */
	String[] getStrings();

	/**
	 * Whether this dynamic string carries no content — no interpolated values and only
	 * blank literal fragments.
	 * @return {@code true} if effectively empty
	 */
	default boolean isEmptyValue() {
		if (getValues().length > 0) {
			return false;
		}
		for (String string : getStrings()) {
			if (string != null && !string.isEmpty()) {
				return false;
			}
		}
		return true;
	}

}
