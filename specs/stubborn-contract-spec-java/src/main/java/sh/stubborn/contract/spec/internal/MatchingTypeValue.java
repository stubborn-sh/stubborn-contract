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

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Matching type with corresponding values.
 *
 * @author Marcin Grzejszczak
 */
public class MatchingTypeValue {

	private @Nullable MatchingType type;

	/**
	 * Value to check.
	 */
	private @Nullable Object value;

	/**
	 * Min occurrence when matching by type.
	 */
	private @Nullable Integer minTypeOccurrence;

	/**
	 * Max occurrence when matching by type.
	 */
	private @Nullable Integer maxTypeOccurrence;

	MatchingTypeValue() {
	}

	MatchingTypeValue(@Nullable MatchingType type) {
		this.type = type;
	}

	MatchingTypeValue(@Nullable MatchingType type, @Nullable Object value) {
		this.type = type;
		this.value = value;
	}

	MatchingTypeValue(@Nullable MatchingType type, @Nullable Object value, @Nullable Integer minTypeOccurrence) {
		this.type = type;
		this.value = value;
		this.minTypeOccurrence = minTypeOccurrence;
	}

	MatchingTypeValue(@Nullable MatchingType type, @Nullable Object value, @Nullable Integer minTypeOccurrence,
			@Nullable Integer maxTypeOccurrence) {
		this.type = type;
		this.value = value;
		this.minTypeOccurrence = minTypeOccurrence;
		this.maxTypeOccurrence = maxTypeOccurrence;
	}

	public @Nullable MatchingType getType() {
		return this.type;
	}

	public void setType(@Nullable MatchingType type) {
		this.type = type;
	}

	public @Nullable Object getValue() {
		return this.value;
	}

	public void setValue(@Nullable Object value) {
		this.value = value;
	}

	public @Nullable Integer getMinTypeOccurrence() {
		return this.minTypeOccurrence;
	}

	public void setMinTypeOccurrence(@Nullable Integer minTypeOccurrence) {
		this.minTypeOccurrence = minTypeOccurrence;
	}

	public @Nullable Integer getMaxTypeOccurrence() {
		return this.maxTypeOccurrence;
	}

	public void setMaxTypeOccurrence(@Nullable Integer maxTypeOccurrence) {
		this.maxTypeOccurrence = maxTypeOccurrence;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MatchingTypeValue value1 = (MatchingTypeValue) o;
		return this.type == value1.type && Objects.equals(this.value, value1.value)
				&& Objects.equals(this.minTypeOccurrence, value1.minTypeOccurrence)
				&& Objects.equals(this.maxTypeOccurrence, value1.maxTypeOccurrence);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.type, this.value, this.minTypeOccurrence, this.maxTypeOccurrence);
	}

	@Override
	public String toString() {
		return "MatchingTypeValue{" + "type=" + this.type + ", value=" + this.value + ", minTypeOccurrence="
				+ this.minTypeOccurrence + ", maxTypeOccurrence=" + this.maxTypeOccurrence + '}';
	}

}
