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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class CookieMutationTests {

	@Test
	void cookieBuildWithMatchingStrategy() {
		MatchingStrategy strategy = new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO);
		Cookie cookie = Cookie.build("session", strategy);
		assertThat(cookie.getKey()).isEqualTo("session");
		assertThat(cookie.getClientValue()).isEqualTo(strategy);
	}

	@Test
	void cookieBuildWithValue() {
		Cookie cookie = Cookie.build("session", "abc");
		assertThat(cookie.getKey()).isEqualTo("session");
		assertThat(cookie.getClientValue()).isEqualTo("abc");
		assertThat(cookie.getServerValue()).isEqualTo("abc");
	}

	@Test
	void cookieConstructorWithDslProperty() {
		Cookie cookie = new Cookie("k", new DslProperty("c", "s"));
		assertThat(cookie.getClientValue()).isEqualTo("c");
		assertThat(cookie.getServerValue()).isEqualTo("s");
	}

	@Test
	void cookieSetKey() {
		Cookie cookie = Cookie.build("k", "v");
		cookie.setKey("k2");
		assertThat(cookie.getKey()).isEqualTo("k2");
	}

	@Test
	void cookieEqualsHashCode() {
		Cookie a = Cookie.build("k", "v");
		Cookie b = Cookie.build("k", "v");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void cookieNotEqualWhenKeyDiffers() {
		assertThat(Cookie.build("k1", "v")).isNotEqualTo(Cookie.build("k2", "v"));
		assertThat(Cookie.build("k1", "v").hashCode()).isNotEqualTo(Cookie.build("k2", "v").hashCode());
	}

	@Test
	void cookieNotEqualWhenValueDiffers() {
		assertThat(Cookie.build("k", "v1")).isNotEqualTo(Cookie.build("k", "v2"));
	}

	@Test
	void cookieToStringContainsKeyAndValue() {
		Cookie cookie = new Cookie("theKey", new DslProperty("cval", "sval"));
		assertThat(cookie.toString()).contains("Cookie").contains("theKey").contains("cval");
	}

	@Test
	void cookiesKeyValueAdds() {
		Cookies cookies = new Cookies();
		cookies.cookie("a", "1");
		assertThat(cookies.getEntries()).extracting(Cookie::getKey).containsExactly("a");
	}

	@Test
	void cookiesFromMapAddsFirst() {
		Cookies cookies = new Cookies();
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("a", "1");
		cookies.cookie(map);
		assertThat(cookies.getEntries()).extracting(Cookie::getKey).containsExactly("a");
	}

	@Test
	void cookiesFromEmptyMapAddsNothing() {
		Cookies cookies = new Cookies();
		cookies.cookie(new LinkedHashMap<>());
		assertThat(cookies.getEntries()).isEmpty();
	}

	@Test
	void executeForEachCookieVisitsAll() {
		Cookies cookies = new Cookies();
		cookies.cookie("a", "1");
		cookies.cookie("b", "2");
		List<String> keys = new ArrayList<>();
		cookies.executeForEachCookie((cookie) -> keys.add(cookie.getKey()));
		assertThat(keys).containsExactlyInAnyOrder("a", "b");
	}

	@Test
	void cookiesMatchingReturnsDslProperty() {
		DslProperty property = new Cookies().matching("v");
		assertThat(property.getClientValue()).isEqualTo("v");
	}

	@Test
	void cookiesAsStubSideMap() {
		Cookies cookies = new Cookies();
		cookies.cookie("a", "1");
		assertThat(cookies.asStubSideMap()).containsExactly(Map.entry("a", "1"));
	}

	@Test
	void cookiesAsTestSideMap() {
		Cookies cookies = new Cookies();
		cookies.cookie("a", "1");
		assertThat(cookies.asTestSideMap()).containsExactly(Map.entry("a", "1"));
	}

	@Test
	void cookiesEqualsHashCode() {
		Cookies a = new Cookies();
		a.cookie("a", "1");
		Cookies b = new Cookies();
		b.cookie("a", "1");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void cookiesNotEqualWhenEntriesDiffer() {
		Cookies a = new Cookies();
		a.cookie("a", "1");
		Cookies b = new Cookies();
		b.cookie("a", "2");
		assertThat(a).isNotEqualTo(b);
		assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
	}

	@Test
	void cookiesToStringContainsEntries() {
		Cookies cookies = new Cookies();
		cookies.cookie("a", "1");
		assertThat(cookies.toString()).contains("Cookies").contains("a");
	}

	@Test
	void cookiesSetEntries() {
		Cookies cookies = new Cookies();
		java.util.Set<Cookie> set = new java.util.HashSet<>();
		set.add(Cookie.build("z", "9"));
		cookies.setEntries(set);
		assertThat(cookies.getEntries()).extracting(Cookie::getKey).containsExactly("z");
	}

}
