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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class UrlMutationTests {

	@Test
	void urlFromObjectSetsBothSides() {
		Url url = new Url("/foo");
		assertThat(url.getClientValue()).isEqualTo("/foo");
		assertThat(url.getServerValue()).isEqualTo("/foo");
	}

	@Test
	void urlFromDslProperty() {
		Url url = new Url(new DslProperty("/c", "/s"));
		assertThat(url.getClientValue()).isEqualTo("/c");
		assertThat(url.getServerValue()).isEqualTo("/s");
	}

	@Test
	void queryParametersInitiallyNull() {
		assertThat(new Url("/foo").getQueryParameters()).isNull();
	}

	@Test
	void queryParametersConsumerBuildsParameters() {
		Url url = new Url("/foo");
		url.queryParameters((qp) -> qp.parameter("a", "1"));
		assertThat(url.getQueryParameters()).isNotNull();
		assertThat(url.getQueryParameters().getParameters()).extracting(QueryParameter::getName).containsExactly("a");
	}

	@Test
	void setQueryParameters() {
		Url url = new Url("/foo");
		QueryParameters parameters = new QueryParameters();
		parameters.parameter("a", "1");
		url.setQueryParameters(parameters);
		assertThat(url.getQueryParameters()).isSameAs(parameters);
	}

	@Test
	void equalsHashCodeSameQueryParameters() {
		Url a = new Url("/foo");
		Url b = new Url("/foo");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void notEqualWhenUrlValueDiffers() {
		assertThat(new Url("/foo")).isNotEqualTo(new Url("/bar"));
	}

	@Test
	void notEqualWhenQueryParametersDiffer() {
		Url a = new Url("/foo");
		a.queryParameters((qp) -> qp.parameter("a", "1"));
		Url b = new Url("/foo");
		assertThat(a).isNotEqualTo(b);
		assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
	}

	@Test
	void notEqualUrlHashCodeDiffers() {
		assertThat(new Url("/foo").hashCode()).isNotEqualTo(new Url("/bar").hashCode());
	}

	@Test
	void toStringContainsQueryParametersAndUrl() {
		Url url = new Url("/foo");
		assertThat(url.toString()).contains("Url").contains("queryParameters").contains("/foo");
	}

	@Test
	void urlPathFromString() {
		UrlPath path = new UrlPath("/p");
		assertThat(path.getClientValue()).isEqualTo("/p");
		assertThat(path.getServerValue()).isEqualTo("/p");
	}

	@Test
	void urlPathFromObject() {
		UrlPath path = new UrlPath((Object) "/p");
		assertThat(path.getClientValue()).isEqualTo("/p");
	}

	@Test
	void urlPathFromDslProperty() {
		UrlPath path = new UrlPath(new DslProperty("/c", "/s"));
		assertThat(path.getClientValue()).isEqualTo("/c");
		assertThat(path.getServerValue()).isEqualTo("/s");
	}

}
