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
 * Matchers for the given path.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public interface BodyMatcher {

	/**
	 * Kind of matching to be applied.
	 * @return what kind of matching are we dealing with.
	 */
	@Nullable MatchingType matchingType();

	/**
	 * Path to the element to match.
	 * @return path to the path. Example for JSON it will be JSON Path.
	 */
	String path();

	/**
	 * Value the path should be checked against.
	 * @return optional value that the given path should be checked against. If there is
	 * no value then presence will be checked only together with type check. Example if we
	 * expect a JSON Path path {@code $.a} to be matched. by type, the defined response
	 * body contained an integer but the actual one contained a string then the assertion
	 * should fail.
	 */
	@Nullable Object value();

	/**
	 * Minimum number of occurrences when matching by type.
	 * @return min no of occurrence when matching by type. In all other cases it will be
	 * ignored.
	 */
	@Nullable Integer minTypeOccurrence();

	/**
	 * Maximum number of occurrences when matching by type.
	 * @return max no of occurrence when matching by type. In all other cases it will be
	 * ignored.
	 */
	@Nullable Integer maxTypeOccurrence();

}
