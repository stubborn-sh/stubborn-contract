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

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marcin Grzejszczak
 */
class CookiesTests {

	Cookies cookies;

	@BeforeEach
	void setup() {
		this.cookies = cookie();
	}

	@Test
	void shouldConvertCookiesToAStubSideMap() {
		assertThat(this.cookies.asStubSideMap()).isEqualTo(Map.of("foo", "client", "bar", "client"));
	}

	@Test
	void shouldConvertCookiesToATestSideMap() {
		assertThat(this.cookies.asTestSideMap()).isEqualTo(Map.of("foo", "server", "bar", "server"));
	}

	private Cookies cookie() {
		Cookies c = new Cookies();
		c.cookie("foo", new DslProperty<>("client", "server"));
		c.cookie(Map.of("bar", new DslProperty<>("client", "server")));
		return c;
	}

}
