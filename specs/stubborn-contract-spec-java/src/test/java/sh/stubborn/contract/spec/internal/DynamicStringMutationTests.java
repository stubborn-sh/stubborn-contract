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
import org.codehaus.groovy.runtime.GStringImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class DynamicStringMutationTests {

	private DynamicStringImpl build() {
		return new DynamicStringImpl(new Object[] { "X", "Y" }, new String[] { "a", "b", "c" });
	}

	@Test
	void toStringInterleavesStringsAndValues() {
		assertThat(build().toString()).isEqualTo("aXbYc");
	}

	@Test
	void toStringWhenMoreValuesThanStrings() {
		DynamicStringImpl dynamic = new DynamicStringImpl(new Object[] { "A", "B", "C" }, new String[] { "x", "y" });
		assertThat(dynamic.toString()).isEqualTo("xAyBC");
	}

	@Test
	void toStringWhenMoreStringsThanValues() {
		DynamicStringImpl dynamic = new DynamicStringImpl(new Object[] { "A" }, new String[] { "x", "y", "z" });
		assertThat(dynamic.toString()).isEqualTo("xAyz");
	}

	@Test
	void getValuesReturnsCopy() {
		DynamicStringImpl dynamic = build();
		Object[] values = dynamic.getValues();
		assertThat(values).containsExactly("X", "Y");
		values[0] = "mutated";
		assertThat(dynamic.getValues()).containsExactly("X", "Y");
	}

	@Test
	void getStringsReturnsCopy() {
		DynamicStringImpl dynamic = build();
		String[] strings = dynamic.getStrings();
		assertThat(strings).containsExactly("a", "b", "c");
		strings[0] = "mutated";
		assertThat(dynamic.getStrings()).containsExactly("a", "b", "c");
	}

	@Test
	void lengthMatchesToString() {
		assertThat(build().length()).isEqualTo("aXbYc".length());
	}

	@Test
	void charAtMatchesToString() {
		assertThat(build().charAt(1)).isEqualTo('X');
	}

	@Test
	void subSequenceMatchesToString() {
		assertThat(build().subSequence(0, 3)).isEqualTo("aXb");
	}

	@Test
	void equalToItself() {
		DynamicStringImpl dynamic = build();
		assertThat(dynamic.equals(dynamic)).isTrue();
	}

	@Test
	void notEqualToNonCharSequence() {
		assertThat(build().equals(42)).isFalse();
	}

	@Test
	void equalToCharSequenceWithSameContent() {
		assertThat(build().equals("aXbYc")).isTrue();
	}

	@Test
	void notEqualToCharSequenceWithDifferentContent() {
		assertThat(build().equals("different")).isFalse();
	}

	@Test
	void hashCodeMatchesToStringHashCode() {
		assertThat(build().hashCode()).isEqualTo("aXbYc".hashCode());
	}

	@Test
	void isEmptyValueFalseWhenHasValues() {
		DynamicStringImpl dynamic = new DynamicStringImpl(new Object[] { "X" }, new String[] { "" });
		assertThat(dynamic.isEmptyValue()).isFalse();
	}

	@Test
	void isEmptyValueFalseWhenHasNonBlankStrings() {
		DynamicStringImpl dynamic = new DynamicStringImpl(new Object[] {}, new String[] { "notBlank" });
		assertThat(dynamic.isEmptyValue()).isFalse();
	}

	@Test
	void isEmptyValueTrueWhenNoValuesAndBlankStrings() {
		DynamicStringImpl dynamic = new DynamicStringImpl(new Object[] {}, new String[] { "", "" });
		assertThat(dynamic.isEmptyValue()).isTrue();
	}

	@Test
	void normalizeLeavesNonGStringUnchanged() {
		Object value = "plain";
		assertThat(DynamicStrings.normalize(value)).isSameAs(value);
	}

	@Test
	void normalizeConvertsGStringToDynamicString() {
		GString gstring = new GStringImpl(new Object[] { "X" }, new String[] { "a", "b" });
		Object normalized = DynamicStrings.normalize(gstring);
		assertThat(normalized).isInstanceOf(DynamicStringImpl.class);
		assertThat(normalized.toString()).isEqualTo("aXb");
	}

	@Test
	void dslPropertyNormalizesGString() {
		GString gstring = new GStringImpl(new Object[] { "X" }, new String[] { "a", "b" });
		DslProperty<Object> property = new DslProperty<>(gstring);
		assertThat(property.getClientValue()).isInstanceOf(DynamicStringImpl.class);
	}

}
