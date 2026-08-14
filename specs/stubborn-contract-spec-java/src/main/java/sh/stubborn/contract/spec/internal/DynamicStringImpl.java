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

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

/**
 * Default {@link DynamicString} implementation. Faithfully reproduces the rendering of
 * the Groovy {@code GStringImpl} it replaces: the literal fragments and the
 * (string-coerced) interpolated values are interleaved as
 * {@code strings[0] value[0] strings[1] … strings[n]}.
 *
 * @author Marcin Grzejszczak
 */
public final class DynamicStringImpl implements DynamicString, Serializable {

	private final @Nullable Object[] values;

	private final String[] strings;

	/**
	 * Creates a dynamic string from its interpolated values and literal fragments.
	 * @param values the interpolated values (typically {@link DslProperty} /
	 * {@link RegexProperty} / plain objects)
	 * @param strings the literal fragments; by convention
	 * {@code strings.length == values.length + 1}
	 */
	public DynamicStringImpl(@Nullable Object[] values, String[] strings) {
		this.values = values.clone();
		this.strings = strings.clone();
	}

	@Override
	public @Nullable Object[] getValues() {
		return this.values.clone();
	}

	@Override
	public String[] getStrings() {
		return this.strings.clone();
	}

	@Override
	public int length() {
		return toString().length();
	}

	@Override
	public char charAt(int index) {
		return toString().charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return toString().subSequence(start, end);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof CharSequence)) {
			return false;
		}
		return toString().equals(obj.toString());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		int max = Math.max(this.strings.length, this.values.length);
		for (int i = 0; i < max; i++) {
			if (i < this.strings.length) {
				builder.append(this.strings[i]);
			}
			if (i < this.values.length) {
				builder.append(String.valueOf(this.values[i]));
			}
		}
		return builder.toString();
	}

}
