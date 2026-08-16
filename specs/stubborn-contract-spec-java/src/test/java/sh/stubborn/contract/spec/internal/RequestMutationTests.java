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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("NullAway")
class RequestMutationTests {

	private final Request request = new Request();

	@Test
	void methodFromString() {
		this.request.method("GET");
		assertThat(this.request.getMethod().getClientValue()).isEqualTo("GET");
	}

	@Test
	void methodFromDslProperty() {
		this.request.method(new DslProperty("POST"));
		assertThat(this.request.getMethod().getClientValue()).isEqualTo("POST");
	}

	@Test
	void urlFromObject() {
		this.request.url("/foo");
		assertThat(this.request.getUrl().getClientValue()).isEqualTo("/foo");
	}

	@Test
	void urlFromDslProperty() {
		this.request.url(new DslProperty("/bar"));
		assertThat(this.request.getUrl().getClientValue()).isEqualTo("/bar");
	}

	@Test
	void urlWithConsumer() {
		this.request.url("/foo", (url) -> url.queryParameters((qp) -> qp.parameter("a", "1")));
		assertThat(this.request.getUrl().getQueryParameters().getParameters()).hasSize(1);
	}

	@Test
	void urlPathVariants() {
		this.request.urlPath("/p");
		assertThat(this.request.getUrlPath().getClientValue()).isEqualTo("/p");
		this.request.urlPath((Object) "/p2");
		assertThat(this.request.getUrlPath().getClientValue()).isEqualTo("/p2");
		this.request.urlPath(new DslProperty("/p3"));
		assertThat(this.request.getUrlPath().getClientValue()).isEqualTo("/p3");
	}

	@Test
	void urlPathWithConsumer() {
		this.request.urlPath("/p", (path) -> path.queryParameters((qp) -> qp.parameter("a", "1")));
		assertThat(this.request.getUrlPath().getQueryParameters().getParameters()).hasSize(1);
	}

	@Test
	void bodyFromMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("a", "1");
		this.request.body(map);
		assertThat(this.request.getBody()).isNotNull();
	}

	@Test
	void bodyFromList() {
		this.request.body(List.of("a", "b"));
		assertThat(this.request.getBody()).isNotNull();
	}

	@Test
	void bodyFromMatchingStrategy() {
		this.request.body(new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO));
		assertThat(this.request.getBody()).isNotNull();
	}

	@Test
	void bodyFromDslProperty() {
		this.request.body(new DslProperty("c", "s"));
		assertThat(this.request.getBody().getClientValue()).isEqualTo("c");
	}

	@Test
	void bodyFromObject() {
		this.request.body((Object) "text");
		assertThat(this.request.getBody().getClientValue()).isEqualTo("text");
	}

	@Test
	void setBody() {
		Body body = new Body("x");
		this.request.setBody(body);
		assertThat(this.request.getBody()).isSameAs(body);
	}

	@Test
	void multipartFromMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("a", "1");
		this.request.multipart(map);
		assertThat(this.request.getMultipart()).isNotNull();
	}

	@Test
	void multipartFromList() {
		this.request.multipart(List.of("a"));
		assertThat(this.request.getMultipart()).isNotNull();
	}

	@Test
	void multipartFromDslProperty() {
		this.request.multipart(new DslProperty("c", "s"));
		assertThat(this.request.getMultipart().getClientValue()).isEqualTo("c");
	}

	@Test
	void multipartFromObject() {
		this.request.multipart((Object) "x");
		assertThat(this.request.getMultipart().getClientValue()).isEqualTo("x");
	}

	@Test
	void setMultipart() {
		Multipart multipart = new Multipart("x");
		this.request.setMultipart(multipart);
		assertThat(this.request.getMultipart()).isSameAs(multipart);
	}

	@Test
	void matchingStrategyBuilders() {
		assertThat(this.request.equalTo("v").getType()).isEqualTo(MatchingStrategy.Type.EQUAL_TO);
		assertThat(this.request.containing("v").getType()).isEqualTo(MatchingStrategy.Type.CONTAINS);
		assertThat(this.request.matching("v").getType()).isEqualTo(MatchingStrategy.Type.MATCHING);
		assertThat(this.request.notMatching("v").getType()).isEqualTo(MatchingStrategy.Type.NOT_MATCHING);
		assertThat(this.request.equalToXml("v").getType()).isEqualTo(MatchingStrategy.Type.EQUAL_TO_XML);
		assertThat(this.request.equalToJson("v").getType()).isEqualTo(MatchingStrategy.Type.EQUAL_TO_JSON);
		assertThat(this.request.absent().getType()).isEqualTo(MatchingStrategy.Type.ABSENT);
	}

	@Test
	void valueFromRegexPropertyKeepsPatternOnClient() {
		DslProperty property = this.request.value(new RegexProperty(Pattern.compile("[0-9]{3}")));
		assertThat(property.getClientValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void dollarRegexProperty() {
		DslProperty property = this.request.$(new RegexProperty(Pattern.compile("[0-9]{3}")));
		assertThat(property.getClientValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void valueFromPattern() {
		DslProperty property = this.request.value(Pattern.compile("[0-9]{3}"));
		assertThat(property.getClientValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void dollarFromPattern() {
		DslProperty property = this.request.$(Pattern.compile("[0-9]{3}"));
		assertThat(property.getClientValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void dollarFromClientDslProperty() {
		DslProperty property = this.request.$(this.request.client("v"));
		assertThat(property.getClientValue()).isEqualTo("v");
	}

	@Test
	void valueClientServerSuccessPath() {
		DslProperty property = this.request.value(this.request.client("c"), this.request.server("s"));
		assertThat(property.getClientValue()).isEqualTo("c");
		assertThat(property.getServerValue()).isEqualTo("s");
	}

	@Test
	void valueServerClientSuccessPath() {
		DslProperty property = this.request.value(this.request.server("s"), this.request.client("c"));
		assertThat(property.getClientValue()).isEqualTo("c");
		assertThat(property.getServerValue()).isEqualTo("s");
	}

	@Test
	void valueClientWithRegexOnServerSideNonSingle() {
		RegexProperty regex = new RegexProperty(Pattern.compile("[0-9]+"));
		DslProperty property = this.request.value(new ClientDslProperty("x", regex));
		assertThat(property.getClientValue()).isEqualTo("x");
		assertThat(property.getServerValue()).isEqualTo("x");
	}

	@Test
	void regexPropertyOverrideProducesConcreteProducer() {
		RegexProperty property = this.request.regexProperty(Pattern.compile("[0-9]{3}"));
		assertThat(property.getClientValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void valueClientServerThrowsWhenServerSideRegex() {
		assertThatThrownBy(() -> this.request.value(this.request.client("x"),
				this.request.server(new RegexProperty(Pattern.compile("[0-9]+")))))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("regular expression for the request on the server side");
	}

	@Test
	void valueServerClientThrowsWhenServerSideRegex() {
		assertThatThrownBy(() -> this.request.value(this.request.server(new RegexProperty(Pattern.compile("[0-9]+"))),
				this.request.client("x")))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void assertThatSidesMatchRejectsOptionalOnTestSide() {
		assertThatThrownBy(() -> this.request.assertThatSidesMatch("x", new OptionalProperty("y")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Optional can be used only for the stub side");
	}

	@Test
	void httpMethodAccessors() {
		assertThat(this.request.GET()).isEqualTo(HttpMethods.GET);
		assertThat(this.request.HEAD()).isEqualTo(HttpMethods.HEAD);
		assertThat(this.request.POST()).isEqualTo(HttpMethods.POST);
		assertThat(this.request.PUT()).isEqualTo(HttpMethods.PUT);
		assertThat(this.request.PATCH()).isEqualTo(HttpMethods.PATCH);
		assertThat(this.request.DELETE()).isEqualTo(HttpMethods.DELETE);
		assertThat(this.request.OPTIONS()).isEqualTo(HttpMethods.OPTIONS);
		assertThat(this.request.TRACE()).isEqualTo(HttpMethods.TRACE);
	}

	@Test
	void anyDelegationsReturnClientDslProperties() {
		assertThat(this.request.anyAlphaUnicode().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyAlphaNumeric().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyNumber().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyInteger().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyPositiveInt().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyDouble().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyHex().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.aBoolean().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyIpAddress().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyHostname().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyEmail().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyUrl().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyHttpsUrl().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyUuid().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyDate().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyDateTime().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyTime().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyIso8601WithOffset().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyNonBlankString().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyNonEmptyString().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.request.anyOf("a", "b").getServerValue()).isIn("a", "b");
	}

	@Test
	void gettersAndSetters() {
		this.request.setMethod(new DslProperty("GET"));
		assertThat(this.request.getMethod().getClientValue()).isEqualTo("GET");
		Url url = new Url("/u");
		this.request.setUrl(url);
		assertThat(this.request.getUrl()).isSameAs(url);
		UrlPath path = new UrlPath("/p");
		this.request.setUrlPath(path);
		assertThat(this.request.getUrlPath()).isSameAs(path);
		Headers headers = new Headers();
		this.request.setHeaders(headers);
		assertThat(this.request.getHeaders()).isSameAs(headers);
		Cookies cookies = new Cookies();
		this.request.setCookies(cookies);
		assertThat(this.request.getCookies()).isSameAs(cookies);
		BodyMatchers matchers = new BodyMatchers();
		this.request.setBodyMatchers(matchers);
		assertThat(this.request.getBodyMatchers()).isSameAs(matchers);
	}

	@Test
	void headersConsumerCreatesRequestHeaders() {
		this.request.headers((h) -> h.header("A", "1"));
		assertThat(this.request.getHeaders()).isInstanceOf(Request.RequestHeaders.class);
	}

	@Test
	void cookiesConsumerCreatesRequestCookies() {
		this.request.cookies((c) -> c.cookie("A", "1"));
		assertThat(this.request.getCookies()).isInstanceOf(Request.RequestCookies.class);
	}

	@Test
	void bodyMatchersConsumer() {
		this.request.bodyMatchers((m) -> m.jsonPath("$.a", m.byEquality()));
		assertThat(this.request.getBodyMatchers().hasMatchers()).isTrue();
	}

	@Test
	void requestHeadersMatchingBuildsDynamicClientValue() {
		Request.RequestHeaders headers = new Request.RequestHeaders();
		DslProperty property = headers.matching("val");
		assertThat(property.getServerValue()).isEqualTo("val");
		assertThat(property.getClientValue()).isInstanceOf(RegexProperty.class);
	}

	@Test
	void requestCookiesMatchingBuildsDynamicClientValue() {
		Request.RequestCookies cookies = new Request.RequestCookies();
		DslProperty property = cookies.matching("val");
		assertThat(property.getServerValue()).isEqualTo("val");
		assertThat(property.getClientValue()).isInstanceOf(RegexProperty.class);
	}

	@Test
	void copyConstructorCopiesFields() {
		this.request.method("GET");
		this.request.url("/foo");
		Request copy = new Request(this.request);
		assertThat(copy.getMethod().getClientValue()).isEqualTo("GET");
		assertThat(copy.getUrl().getClientValue()).isEqualTo("/foo");
	}

	@Test
	void equalsHashCode() {
		Request a = new Request();
		a.method("GET");
		a.url("/foo");
		Request b = new Request();
		b.method("GET");
		b.url("/foo");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void notEqualWhenMethodDiffers() {
		Request a = new Request();
		a.method("GET");
		Request b = new Request();
		b.method("POST");
		assertThat(a).isNotEqualTo(b);
		assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
	}

	@Test
	void notEqualWhenUrlDiffers() {
		Request a = new Request();
		a.url("/foo");
		Request b = new Request();
		b.url("/bar");
		assertThat(a).isNotEqualTo(b);
	}

	@Test
	void toStringContainsFields() {
		this.request.method("GET");
		assertThat(this.request.toString()).contains("Request").contains("method").contains("GET");
	}

	@Test
	void setProperty() {
		var property = this.request.getProperty();
		assertThat(property).isNotNull();
		this.request.setProperty(property);
		assertThat(this.request.getProperty()).isSameAs(property);
	}

}
