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

package sh.stubborn.contract.verifier.assertion;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link CollectionAssert} that pin the fluent {@code this}
 * returns and the iterator-based construction.
 */
class CollectionAssertMutationTests {

	private static CollectionAssert<String> assertThatCollection(List<String> list) {
		return new CollectionAssert<>(list);
	}

	@Test
	void iterator_constructor_materializes_elements() {
		CollectionAssert<String> assertion = new CollectionAssert<>(List.of("a", "b").iterator());
		// allElementsMatch calls isNotEmpty(): an empty toIterable would fail here
		CollectionAssert<String> returned = assertion.allElementsMatch("[a-z]");
		assertThat(returned).isSameAs(assertion);
	}

	@Test
	void allElementsMatch_returns_this() {
		CollectionAssert<String> assertion = assertThatCollection(List.of("aa", "bb"));
		assertThat(assertion.allElementsMatch("[a-z]+")).isSameAs(assertion);
	}

	@Test
	void hasSizeGreaterThanOrEqualTo_returns_this() {
		CollectionAssert<String> assertion = assertThatCollection(List.of("a", "b"));
		assertThat(assertion.hasSizeGreaterThanOrEqualTo(2)).isSameAs(assertion);
	}

	@Test
	void hasSizeLessThanOrEqualTo_returns_this() {
		CollectionAssert<String> assertion = assertThatCollection(List.of("a", "b"));
		assertThat(assertion.hasSizeLessThanOrEqualTo(2)).isSameAs(assertion);
	}

	@Test
	void hasSizeBetween_returns_this() {
		CollectionAssert<String> assertion = assertThatCollection(List.of("a", "b"));
		assertThat(assertion.hasSizeBetween(1, 3)).isSameAs(assertion);
	}

	@Test
	void hasFlattenedSizeGreaterThanOrEqualTo_returns_this() {
		CollectionAssert<String> assertion = assertThatCollection(List.of("a", "b", "c"));
		assertThat(assertion.hasFlattenedSizeGreaterThanOrEqualTo(3)).isSameAs(assertion);
	}

	@Test
	void hasFlattenedSizeLessThanOrEqualTo_returns_this() {
		CollectionAssert<String> assertion = assertThatCollection(List.of("a", "b", "c"));
		assertThat(assertion.hasFlattenedSizeLessThanOrEqualTo(3)).isSameAs(assertion);
	}

	@Test
	void hasFlattenedSizeBetween_returns_this() {
		CollectionAssert<String> assertion = assertThatCollection(List.of("a", "b", "c"));
		assertThat(assertion.hasFlattenedSizeBetween(2, 4)).isSameAs(assertion);
	}

	@Test
	void as_returns_this_and_keeps_type() {
		CollectionAssert<String> assertion = assertThatCollection(List.of("aa"));
		CollectionAssert<String> described = assertion.as("my description");
		assertThat(described).isSameAs(assertion);
		// still usable as a CollectionAssert afterwards
		assertThat(described.allElementsMatch("[a-z]+")).isSameAs(assertion);
	}

}
