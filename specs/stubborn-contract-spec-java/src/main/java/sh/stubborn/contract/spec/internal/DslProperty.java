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
import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * Represents an element of a DSL that can contain client or sever side values.
 *
 * @param <T> the value type
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public class DslProperty<T extends @Nullable Object> implements Serializable {

	private final @Nullable T clientValue;

	private final @Nullable T serverValue;

	@SuppressWarnings("unchecked")
	public DslProperty(@Nullable T clientValue, @Nullable T serverValue) {
		this.clientValue = (T) DynamicStrings.normalize(clientValue);
		this.serverValue = (T) DynamicStrings.normalize(serverValue);
	}

	@SuppressWarnings("unchecked")
	public DslProperty(@Nullable T singleValue) {
		T normalized = (T) DynamicStrings.normalize(singleValue);
		this.clientValue = normalized;
		this.serverValue = normalized;
	}

	public boolean isSingleValue() {
		return Objects.equals(this.clientValue, this.serverValue)
				|| (this.clientValue != null && this.serverValue == null)
				|| (this.serverValue != null && this.clientValue == null);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DslProperty<?> that = (DslProperty<?>) o;
		Object thisClientValue = stringPatternIfPattern(this.clientValue);
		Object thatClientValue = stringPatternIfPattern(that.clientValue);
		Object thisServerValue = stringPatternIfPattern(this.serverValue);
		Object thatServerValue = stringPatternIfPattern(that.serverValue);
		return Objects.equals(thisClientValue, thatClientValue) && Objects.equals(thisServerValue, thatServerValue);
	}

	private @Nullable Object stringPatternIfPattern(@Nullable Object value) {
		return (value instanceof Pattern) ? ((Pattern) value).pattern() : value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(stringPatternIfPattern(this.clientValue), stringPatternIfPattern(this.serverValue));
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" + "\nclientValue=" + this.clientValue + ", \n\tserverValue="
				+ this.serverValue + '}';
	}

	public final @Nullable T getClientValue() {
		return this.clientValue;
	}

	public final @Nullable T getServerValue() {
		return this.serverValue;
	}

}
