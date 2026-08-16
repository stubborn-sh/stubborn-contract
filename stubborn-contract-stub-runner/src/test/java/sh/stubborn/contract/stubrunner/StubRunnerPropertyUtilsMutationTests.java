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

package sh.stubborn.contract.stubrunner;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class StubRunnerPropertyUtilsMutationTests {

	@AfterEach
	void tearDown() {
		StubRunnerPropertyUtils.FETCHER = new PropertyFetcher();
	}

	@Test
	void shouldNotAppendPrefixWhenPropertyAlreadyPrefixed() {
		List<String> queriedSystemProps = new java.util.ArrayList<>();
		StubRunnerPropertyUtils.FETCHER = new PropertyFetcher() {
			@Override
			public String systemProp(String prop) {
				queriedSystemProps.add(prop);
				return "value";
			}

			@Override
			public String envVar(String prop) {
				return null;
			}
		};

		String result = StubRunnerPropertyUtils.getProperty(new HashMap<>(), "stubborn.contract.stubrunner.foo");

		assertThat(result).isEqualTo("value");
		// already-prefixed property is queried verbatim, never double-prefixed
		assertThat(queriedSystemProps).containsExactly("stubborn.contract.stubrunner.foo");
	}

	@Test
	void shouldAppendPrefixWhenPropertyNotPrefixed() {
		List<String> queriedSystemProps = new java.util.ArrayList<>();
		StubRunnerPropertyUtils.FETCHER = new PropertyFetcher() {
			@Override
			public String systemProp(String prop) {
				queriedSystemProps.add(prop);
				return "value";
			}

			@Override
			public String envVar(String prop) {
				return null;
			}
		};

		String result = StubRunnerPropertyUtils.getProperty(new HashMap<>(), "foo.bar");

		assertThat(result).isEqualTo("value");
		assertThat(queriedSystemProps).containsExactly("stubborn.contract.stubrunner.properties.foo.bar");
	}

}
