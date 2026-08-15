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

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("NullAway")
class ResponseMutationTests {

	private final Response response = new Response();

	@Test
	void statusFromInt() {
		this.response.status(200);
		assertThat(this.response.getStatus().getClientValue()).isEqualTo(200);
	}

	@Test
	void statusFromDslProperty() {
		this.response.status(new DslProperty(404));
		assertThat(this.response.getStatus().getClientValue()).isEqualTo(404);
	}

	@Test
	void bodyFromObject() {
		this.response.body((Object) "text");
		assertThat(this.response.getBody().getClientValue()).isEqualTo("text");
	}

	@Test
	void bodyFromListObject() {
		this.response.body((Object) List.of("a", "b"));
		assertThat(this.response.getBody()).isNotNull();
	}

	@Test
	void bodyFromList() {
		this.response.body(List.of("a", "b"));
		assertThat(this.response.getBody()).isNotNull();
	}

	@Test
	void bodyFromMap() {
		this.response.body(java.util.Map.of("a", "1"));
		assertThat(this.response.getBody()).isNotNull();
	}

	@Test
	void fixedDelaySetsDelay() {
		this.response.fixedDelayMilliseconds(500);
		assertThat(this.response.getDelay().getClientValue()).isEqualTo(500);
	}

	@Test
	void asyncFlag() {
		assertThat(this.response.isAsync()).isFalse();
		assertThat(this.response.getAsync()).isFalse();
		this.response.async();
		assertThat(this.response.isAsync()).isTrue();
		assertThat(this.response.getAsync()).isTrue();
	}

	@Test
	void valueClientServerSuccessPath() {
		DslProperty property = this.response.value(this.response.client("c"), this.response.server("s"));
		assertThat(property.getClientValue()).isEqualTo("c");
		assertThat(property.getServerValue()).isEqualTo("s");
	}

	@Test
	void valueServerClientSuccessPath() {
		DslProperty property = this.response.value(this.response.server("s"), this.response.client("c"));
		assertThat(property.getClientValue()).isEqualTo("c");
		assertThat(property.getServerValue()).isEqualTo("s");
	}

	@Test
	void valueFromRegexPropertyKeepsPatternOnServer() {
		DslProperty property = this.response.value(new RegexProperty(Pattern.compile("[0-9]{3}")));
		assertThat(property.getServerValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void valueFromServerDslProperty() {
		// ServerDslProperty(server, client) stores server then client
		DslProperty property = this.response.value(new ServerDslProperty("serverVal", "clientVal"));
		assertThat(property.getClientValue()).isEqualTo("clientVal");
		assertThat(property.getServerValue()).isEqualTo("serverVal");
	}

	@Test
	void dollarServerDslProperty() {
		DslProperty property = this.response.$(new ServerDslProperty("serverVal", "clientVal"));
		assertThat(property.getClientValue()).isEqualTo("clientVal");
		assertThat(property.getServerValue()).isEqualTo("serverVal");
	}

	@Test
	void valueFromPattern() {
		DslProperty property = this.response.value(Pattern.compile("[0-9]{3}"));
		assertThat(property.getServerValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void dollarRegexProperty() {
		DslProperty property = this.response.$(new RegexProperty(Pattern.compile("[0-9]{3}")));
		assertThat(property.getServerValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void dollarPattern() {
		DslProperty property = this.response.$(Pattern.compile("[0-9]{3}"));
		assertThat(property.getServerValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void regexPropertyOverride() {
		RegexProperty property = this.response.regexProperty(Pattern.compile("[0-9]{3}"));
		assertThat(property.getServerValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void fromRequestReturnsFromRequest() {
		assertThat(this.response.fromRequest()).isNotNull();
	}

	@Test
	void valueClientServerThrowsWhenClientSideRegex() {
		assertThatThrownBy(() -> this.response.value(this.response.client(new RegexProperty(Pattern.compile("[0-9]+"))),
				this.response.server("x")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("regular expression for the response on the client side");
	}

	@Test
	void valueServerClientThrowsWhenClientSideRegex() {
		assertThatThrownBy(() -> this.response.value(this.response.server("x"),
				this.response.client(new RegexProperty(Pattern.compile("[0-9]+")))))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void assertThatSidesMatchRejectsOptionalOnStubSide() {
		assertThatThrownBy(() -> this.response.assertThatSidesMatch(new OptionalProperty("x"), "y"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Optional can be used only in the test side");
	}

	@Test
	void httpStatusAccessors() {
		assertThat(this.response.CONTINUE()).isEqualTo(HttpStatus.CONTINUE);
		assertThat(this.response.SWITCHING_PROTOCOLS()).isEqualTo(HttpStatus.SWITCHING_PROTOCOLS);
		assertThat(this.response.PROCESSING()).isEqualTo(HttpStatus.PROCESSING);
		assertThat(this.response.CHECKPOINT()).isEqualTo(HttpStatus.CHECKPOINT);
		assertThat(this.response.OK()).isEqualTo(HttpStatus.OK);
		assertThat(this.response.CREATED()).isEqualTo(HttpStatus.CREATED);
		assertThat(this.response.ACCEPTED()).isEqualTo(HttpStatus.ACCEPTED);
		assertThat(this.response.NON_AUTHORITATIVE_INFORMATION()).isEqualTo(HttpStatus.NON_AUTHORITATIVE_INFORMATION);
		assertThat(this.response.NO_CONTENT()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(this.response.RESET_CONTENT()).isEqualTo(HttpStatus.RESET_CONTENT);
		assertThat(this.response.PARTIAL_CONTENT()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
		assertThat(this.response.MULTI_STATUS()).isEqualTo(HttpStatus.MULTI_STATUS);
		assertThat(this.response.ALREADY_REPORTED()).isEqualTo(HttpStatus.ALREADY_REPORTED);
		assertThat(this.response.IM_USED()).isEqualTo(HttpStatus.IM_USED);
		assertThat(this.response.MULTIPLE_CHOICES()).isEqualTo(HttpStatus.MULTIPLE_CHOICES);
		assertThat(this.response.MOVED_PERMANENTLY()).isEqualTo(HttpStatus.MOVED_PERMANENTLY);
		assertThat(this.response.FOUND()).isEqualTo(HttpStatus.FOUND);
		assertThat(this.response.SEE_OTHER()).isEqualTo(HttpStatus.SEE_OTHER);
		assertThat(this.response.NOT_MODIFIED()).isEqualTo(HttpStatus.NOT_MODIFIED);
		assertThat(this.response.TEMPORARY_REDIRECT()).isEqualTo(HttpStatus.TEMPORARY_REDIRECT);
		assertThat(this.response.PERMANENT_REDIRECT()).isEqualTo(HttpStatus.PERMANENT_REDIRECT);
		assertThat(this.response.BAD_REQUEST()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(this.response.UNAUTHORIZED()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(this.response.PAYMENT_REQUIRED()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
		assertThat(this.response.FORBIDDEN()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(this.response.NOT_FOUND()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(this.response.METHOD_NOT_ALLOWED()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
		assertThat(this.response.NOT_ACCEPTABLE()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
		assertThat(this.response.PROXY_AUTHENTICATION_REQUIRED()).isEqualTo(HttpStatus.PROXY_AUTHENTICATION_REQUIRED);
		assertThat(this.response.REQUEST_TIMEOUT()).isEqualTo(HttpStatus.REQUEST_TIMEOUT);
		assertThat(this.response.CONFLICT()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(this.response.GONE()).isEqualTo(HttpStatus.GONE);
		assertThat(this.response.LENGTH_REQUIRED()).isEqualTo(HttpStatus.LENGTH_REQUIRED);
		assertThat(this.response.PRECONDITION_FAILED()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
		assertThat(this.response.PAYLOAD_TOO_LARGE()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
		assertThat(this.response.URI_TOO_LONG()).isEqualTo(HttpStatus.URI_TOO_LONG);
		assertThat(this.response.UNSUPPORTED_MEDIA_TYPE()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		assertThat(this.response.REQUESTED_RANGE_NOT_SATISFIABLE())
			.isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
		assertThat(this.response.EXPECTATION_FAILED()).isEqualTo(HttpStatus.EXPECTATION_FAILED);
		assertThat(this.response.I_AM_A_TEAPOT()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);
		assertThat(this.response.UNPROCESSABLE_ENTITY()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(this.response.LOCKED()).isEqualTo(HttpStatus.LOCKED);
		assertThat(this.response.FAILED_DEPENDENCY()).isEqualTo(HttpStatus.FAILED_DEPENDENCY);
		assertThat(this.response.UPGRADE_REQUIRED()).isEqualTo(HttpStatus.UPGRADE_REQUIRED);
		assertThat(this.response.PRECONDITION_REQUIRED()).isEqualTo(HttpStatus.PRECONDITION_REQUIRED);
		assertThat(this.response.TOO_MANY_REQUESTS()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(this.response.REQUEST_HEADER_FIELDS_TOO_LARGE())
			.isEqualTo(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
		assertThat(this.response.UNAVAILABLE_FOR_LEGAL_REASONS()).isEqualTo(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS);
		assertThat(this.response.INTERNAL_SERVER_ERROR()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(this.response.NOT_IMPLEMENTED()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
		assertThat(this.response.BAD_GATEWAY()).isEqualTo(HttpStatus.BAD_GATEWAY);
		assertThat(this.response.SERVICE_UNAVAILABLE()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(this.response.GATEWAY_TIMEOUT()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
		assertThat(this.response.HTTP_VERSION_NOT_SUPPORTED()).isEqualTo(HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
		assertThat(this.response.VARIANT_ALSO_NEGOTIATES()).isEqualTo(HttpStatus.VARIANT_ALSO_NEGOTIATES);
		assertThat(this.response.INSUFFICIENT_STORAGE()).isEqualTo(HttpStatus.INSUFFICIENT_STORAGE);
		assertThat(this.response.LOOP_DETECTED()).isEqualTo(HttpStatus.LOOP_DETECTED);
		assertThat(this.response.BANDWIDTH_LIMIT_EXCEEDED()).isEqualTo(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED);
		assertThat(this.response.NOT_EXTENDED()).isEqualTo(HttpStatus.NOT_EXTENDED);
		assertThat(this.response.NETWORK_AUTHENTICATION_REQUIRED())
			.isEqualTo(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED);
	}

	@Test
	void anyDelegationsReturnServerDslProperties() {
		assertThat(this.response.anyAlphaUnicode().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyAlphaNumeric().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyNumber().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyInteger().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyPositiveInt().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyDouble().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyHex().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.aBoolean().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyIpAddress().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyHostname().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyEmail().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyUrl().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyHttpsUrl().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyUuid().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyDate().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyDateTime().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyTime().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyIso8601WithOffset().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyNonBlankString().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyNonEmptyString().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.response.anyOf("a", "b").getClientValue()).isIn("a", "b");
	}

	@Test
	void gettersAndSetters() {
		this.response.setStatus(new DslProperty(200));
		assertThat(this.response.getStatus().getClientValue()).isEqualTo(200);
		this.response.setDelay(new DslProperty(100));
		assertThat(this.response.getDelay().getClientValue()).isEqualTo(100);
		Headers headers = new Headers();
		this.response.setHeaders(headers);
		assertThat(this.response.getHeaders()).isSameAs(headers);
		Cookies cookies = new Cookies();
		this.response.setCookies(cookies);
		assertThat(this.response.getCookies()).isSameAs(cookies);
		Body body = new Body("x");
		this.response.setBody(body);
		assertThat(this.response.getBody()).isSameAs(body);
		this.response.setAsync(true);
		assertThat(this.response.isAsync()).isTrue();
		ResponseBodyMatchers matchers = new ResponseBodyMatchers();
		this.response.setBodyMatchers(matchers);
		assertThat(this.response.getBodyMatchers()).isSameAs(matchers);
	}

	@Test
	void headersConsumerCreatesResponseHeaders() {
		this.response.headers((h) -> h.header("A", "1"));
		assertThat(this.response.getHeaders()).isInstanceOf(Response.ResponseHeaders.class);
	}

	@Test
	void cookiesConsumerCreatesResponseCookies() {
		this.response.cookies((c) -> c.cookie("A", "1"));
		assertThat(this.response.getCookies()).isInstanceOf(Response.ResponseCookies.class);
	}

	@Test
	void bodyMatchersConsumer() {
		this.response.bodyMatchers((m) -> m.jsonPath("$.a", m.byEquality()));
		assertThat(this.response.getBodyMatchers().hasMatchers()).isTrue();
	}

	@Test
	void responseHeadersMatchingBuildsServerSidePattern() {
		Response.ResponseHeaders headers = new Response.ResponseHeaders();
		DslProperty property = headers.matching("val");
		assertThat(property.getClientValue()).isEqualTo("val");
		assertThat(property.getServerValue()).isInstanceOf(NotToEscapePattern.class);
	}

	@Test
	void responseCookiesMatchingBuildsServerSidePattern() {
		Response.ResponseCookies cookies = new Response.ResponseCookies();
		DslProperty property = cookies.matching("val");
		assertThat(property.getClientValue()).isEqualTo("val");
		assertThat(property.getServerValue()).isInstanceOf(RegexProperty.class);
	}

	@Test
	void copyConstructorCopiesFields() {
		this.response.status(200);
		Response copy = new Response(this.response);
		assertThat(copy.getStatus().getClientValue()).isEqualTo(200);
	}

	@Test
	void equalsHashCode() {
		Response a = new Response();
		a.status(200);
		Response b = new Response();
		b.status(200);
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void notEqualWhenStatusDiffers() {
		Response a = new Response();
		a.status(200);
		Response b = new Response();
		b.status(404);
		assertThat(a).isNotEqualTo(b);
		assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
	}

	@Test
	void notEqualWhenAsyncDiffers() {
		Response a = new Response();
		a.status(200);
		Response b = new Response();
		b.status(200);
		b.async();
		assertThat(a).isNotEqualTo(b);
	}

	@Test
	void toStringContainsFields() {
		this.response.status(200);
		assertThat(this.response.toString()).contains("Response").contains("status").contains("async");
	}

	@Test
	void setProperty() {
		var property = this.response.getProperty();
		assertThat(property).isNotNull();
		this.response.setProperty(property);
		assertThat(this.response.getProperty()).isSameAs(property);
	}

}
