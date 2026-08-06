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
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.spec.util.RegexpUtils;

/**
 * Represents the response side of the HTTP communication.
 *
 * @author Marcin Grzejszczak
 * @author Tim Ysewyn
 * @since 1.0.0
 */
public class Response extends Common implements RegexCreatingProperty<ServerDslProperty> {

	private static final Logger log = LoggerFactory.getLogger(Response.class);

	private @Nullable DslProperty status;

	private @Nullable DslProperty delay;

	private @Nullable Headers headers;

	private @Nullable Cookies cookies;

	private @Nullable Body body;

	private boolean async;

	private @Nullable ResponseBodyMatchers bodyMatchers;

	private ServerPatternValueDslProperty property = new ServerPatternValueDslProperty();

	public Response() {
	}

	public Response(Response response) {
		this.status = response.getStatus();
		this.headers = response.getHeaders();
		this.cookies = response.getCookies();
		this.body = response.getBody();
	}

	/**
	 * Allows to set the HTTP status.
	 * @param status the HTTP status
	 */
	public void status(int status) {
		this.status = toDslProperty(status);
	}

	/**
	 * Allows to set the HTTP status.
	 * @param status the HTTP status
	 */
	public void status(DslProperty status) {
		this.status = toDslProperty(status);
	}

	/**
	 * Allows set an HTTP body.
	 * @param body body to set
	 */
	public void body(Map<String, Object> body) {
		this.body = new Body(convertObjectsToDslProperties(body));
	}

	/**
	 * Allows set an HTTP body.
	 * @param body body to set
	 */
	public void body(List body) {
		this.body = new Body(convertObjectsToDslProperties(body));
	}

	/**
	 * Allows set an HTTP body.
	 * @param bodyAsValue body to set
	 */
	public void body(Object bodyAsValue) {
		if (bodyAsValue instanceof List) {
			body((List) bodyAsValue);
		}
		else {
			this.body = new Body(bodyAsValue);
		}

	}

	/**
	 * Allows to set a fixed delay of the response in milliseconds.
	 * @param timeInMilliseconds delay in millis
	 */
	public void fixedDelayMilliseconds(int timeInMilliseconds) {
		this.delay = toDslProperty(timeInMilliseconds);
	}

	/**
	 * Turns on the asynchronous mode for this contract. Used with MockMvc and the Servlet
	 * 3.0 features.
	 */
	public void async() {
		this.async = true;
	}

	@Override
	public void assertThatSidesMatch(@Nullable Object stubSide, @Nullable Object testSide) {
		if (stubSide instanceof OptionalProperty) {
			throw new IllegalStateException("Optional can be used only in the test side of the response!");
		}
		super.assertThatSidesMatch(stubSide, testSide);
	}

	/**
	 * Allows to set a dynamic value for the given element.
	 * @param server server value
	 * @return dsl property
	 */
	public DslProperty value(ServerDslProperty server) {
		Object dynamicValue = server.getServerValue();
		Object concreteValue = server.getClientValue();
		if (dynamicValue instanceof RegexProperty && server.isSingleValue()) {
			return ((RegexProperty) dynamicValue).concreteClientDynamicProducer();
		}
		return new DslProperty(concreteValue, dynamicValue);
	}

	/**
	 * Allows to set a dynamic value for the given element.
	 * @param server server value
	 * @return dsl property
	 */
	public DslProperty $(ServerDslProperty server) {
		return value(server);
	}

	/**
	 * Allows to set a dynamic value for the given element.
	 * @param server server value
	 * @return dsl property
	 */
	public DslProperty value(Pattern server) {
		return value(new RegexProperty(server));
	}

	/**
	 * Allows to set a dynamic value for the given element.
	 * @param server server value
	 * @return dsl property
	 */
	public DslProperty value(RegexProperty server) {
		return value(new ServerDslProperty(server));
	}

	/**
	 * Allows to set a dynamic value for the given element.
	 * @param server server value
	 * @return dsl property
	 */
	public DslProperty $(RegexProperty server) {
		return value(server);
	}

	/**
	 * Allows to set a dynamic value for the given element.
	 * @param server server value
	 * @return dsl property
	 */
	public DslProperty $(Pattern server) {
		return value(new RegexProperty(server));
	}

	@Override
	public RegexProperty regexProperty(Object object) {
		return new RegexProperty(object).concreteClientDynamicProducer();
	}

	/**
	 * Allows to reference entries from the request.
	 * @return from request object
	 */
	public FromRequest fromRequest() {
		return new FromRequest();
	}

	/**
	 * Allows to set a dynamic value for the given element.
	 * @param client client value
	 * @param server server value
	 * @return dsl property
	 */
	@Override
	public DslProperty value(ClientDslProperty client, ServerDslProperty server) {
		if (client.getClientValue() instanceof RegexProperty) {
			throw new IllegalStateException("You can't have a regular expression for the response on the client side");
		}

		return super.value(client, server);
	}

	/**
	 * Allows to set a dynamic value for the given element.
	 * @param server server value
	 * @param client client value
	 * @return dsl property
	 */
	@Override
	public DslProperty value(ServerDslProperty server, ClientDslProperty client) {
		if (client.getClientValue() instanceof RegexProperty) {
			throw new IllegalStateException("You can't have a regular expression for the response on the client side");
		}

		return super.value(server, client);
	}

	public ServerPatternValueDslProperty getProperty() {
		return this.property;
	}

	public void setProperty(ServerPatternValueDslProperty property) {
		this.property = property;
	}

	public @Nullable DslProperty getStatus() {
		return this.status;
	}

	public void setStatus(@Nullable DslProperty status) {
		this.status = status;
	}

	public @Nullable DslProperty getDelay() {
		return this.delay;
	}

	public void setDelay(@Nullable DslProperty delay) {
		this.delay = delay;
	}

	public @Nullable Headers getHeaders() {
		return this.headers;
	}

	public void setHeaders(@Nullable Headers headers) {
		this.headers = headers;
	}

	public @Nullable Cookies getCookies() {
		return this.cookies;
	}

	public void setCookies(@Nullable Cookies cookies) {
		this.cookies = cookies;
	}

	public @Nullable Body getBody() {
		return this.body;
	}

	public void setBody(@Nullable Body body) {
		this.body = body;
	}

	public boolean getAsync() {
		return this.async;
	}

	public boolean isAsync() {
		return this.async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

	public @Nullable ResponseBodyMatchers getBodyMatchers() {
		return this.bodyMatchers;
	}

	public void setBodyMatchers(@Nullable ResponseBodyMatchers bodyMatchers) {
		this.bodyMatchers = bodyMatchers;
	}

	@Override
	public ServerDslProperty anyAlphaUnicode() {
		return this.property.anyAlphaUnicode();
	}

	@Override
	public ServerDslProperty anyAlphaNumeric() {
		return this.property.anyAlphaNumeric();
	}

	@Override
	public ServerDslProperty anyNumber() {
		return this.property.anyNumber();
	}

	@Override
	public ServerDslProperty anyInteger() {
		return this.property.anyInteger();
	}

	@Override
	public ServerDslProperty anyPositiveInt() {
		return this.property.anyPositiveInt();
	}

	@Override
	public ServerDslProperty anyDouble() {
		return this.property.anyDouble();
	}

	@Override
	public ServerDslProperty anyHex() {
		return this.property.anyHex();
	}

	@Override
	public ServerDslProperty aBoolean() {
		return this.property.aBoolean();
	}

	@Override
	public ServerDslProperty anyIpAddress() {
		return this.property.anyIpAddress();
	}

	@Override
	public ServerDslProperty anyHostname() {
		return this.property.anyHostname();
	}

	@Override
	public ServerDslProperty anyEmail() {
		return this.property.anyEmail();
	}

	@Override
	public ServerDslProperty anyUrl() {
		return this.property.anyUrl();
	}

	@Override
	public ServerDslProperty anyHttpsUrl() {
		return this.property.anyHttpsUrl();
	}

	@Override
	public ServerDslProperty anyUuid() {
		return this.property.anyUuid();
	}

	@Override
	public ServerDslProperty anyDate() {
		return this.property.anyDate();
	}

	@Override
	public ServerDslProperty anyDateTime() {
		return this.property.anyDateTime();
	}

	@Override
	public ServerDslProperty anyTime() {
		return this.property.anyTime();
	}

	@Override
	public ServerDslProperty anyIso8601WithOffset() {
		return this.property.anyIso8601WithOffset();
	}

	@Override
	public ServerDslProperty anyNonBlankString() {
		return this.property.anyNonBlankString();
	}

	@Override
	public ServerDslProperty anyNonEmptyString() {
		return this.property.anyNonEmptyString();
	}

	@Override
	public ServerDslProperty anyOf(String... values) {
		return this.property.anyOf(values);
	}

	public int CONTINUE() {
		return HttpStatus.CONTINUE;
	}

	public int SWITCHING_PROTOCOLS() {
		return HttpStatus.SWITCHING_PROTOCOLS;
	}

	public int PROCESSING() {
		return HttpStatus.PROCESSING;
	}

	public int CHECKPOINT() {
		return HttpStatus.CHECKPOINT;
	}

	public int OK() {
		return HttpStatus.OK;
	}

	public int CREATED() {
		return HttpStatus.CREATED;
	}

	public int ACCEPTED() {
		return HttpStatus.ACCEPTED;
	}

	public int NON_AUTHORITATIVE_INFORMATION() {
		return HttpStatus.NON_AUTHORITATIVE_INFORMATION;
	}

	public int NO_CONTENT() {
		return HttpStatus.NO_CONTENT;
	}

	public int RESET_CONTENT() {
		return HttpStatus.RESET_CONTENT;
	}

	public int PARTIAL_CONTENT() {
		return HttpStatus.PARTIAL_CONTENT;
	}

	public int MULTI_STATUS() {
		return HttpStatus.MULTI_STATUS;
	}

	public int ALREADY_REPORTED() {
		return HttpStatus.ALREADY_REPORTED;
	}

	public int IM_USED() {
		return HttpStatus.IM_USED;
	}

	public int MULTIPLE_CHOICES() {
		return HttpStatus.MULTIPLE_CHOICES;
	}

	public int MOVED_PERMANENTLY() {
		return HttpStatus.MOVED_PERMANENTLY;
	}

	public int FOUND() {
		return HttpStatus.FOUND;
	}

	public int SEE_OTHER() {
		return HttpStatus.SEE_OTHER;
	}

	public int NOT_MODIFIED() {
		return HttpStatus.NOT_MODIFIED;
	}

	public int TEMPORARY_REDIRECT() {
		return HttpStatus.TEMPORARY_REDIRECT;
	}

	public int PERMANENT_REDIRECT() {
		return HttpStatus.PERMANENT_REDIRECT;
	}

	public int BAD_REQUEST() {
		return HttpStatus.BAD_REQUEST;
	}

	public int UNAUTHORIZED() {
		return HttpStatus.UNAUTHORIZED;
	}

	public int PAYMENT_REQUIRED() {
		return HttpStatus.PAYMENT_REQUIRED;
	}

	public int FORBIDDEN() {
		return HttpStatus.FORBIDDEN;
	}

	public int NOT_FOUND() {
		return HttpStatus.NOT_FOUND;
	}

	public int METHOD_NOT_ALLOWED() {
		return HttpStatus.METHOD_NOT_ALLOWED;
	}

	public int NOT_ACCEPTABLE() {
		return HttpStatus.NOT_ACCEPTABLE;
	}

	public int PROXY_AUTHENTICATION_REQUIRED() {
		return HttpStatus.PROXY_AUTHENTICATION_REQUIRED;
	}

	public int REQUEST_TIMEOUT() {
		return HttpStatus.REQUEST_TIMEOUT;
	}

	public int CONFLICT() {
		return HttpStatus.CONFLICT;
	}

	public int GONE() {
		return HttpStatus.GONE;
	}

	public int LENGTH_REQUIRED() {
		return HttpStatus.LENGTH_REQUIRED;
	}

	public int PRECONDITION_FAILED() {
		return HttpStatus.PRECONDITION_FAILED;
	}

	public int PAYLOAD_TOO_LARGE() {
		return HttpStatus.PAYLOAD_TOO_LARGE;
	}

	public int URI_TOO_LONG() {
		return HttpStatus.URI_TOO_LONG;
	}

	public int UNSUPPORTED_MEDIA_TYPE() {
		return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
	}

	public int REQUESTED_RANGE_NOT_SATISFIABLE() {
		return HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
	}

	public int EXPECTATION_FAILED() {
		return HttpStatus.EXPECTATION_FAILED;
	}

	public int I_AM_A_TEAPOT() {
		return HttpStatus.I_AM_A_TEAPOT;
	}

	public int UNPROCESSABLE_ENTITY() {
		return HttpStatus.UNPROCESSABLE_ENTITY;
	}

	public int LOCKED() {
		return HttpStatus.LOCKED;
	}

	public int FAILED_DEPENDENCY() {
		return HttpStatus.FAILED_DEPENDENCY;
	}

	public int UPGRADE_REQUIRED() {
		return HttpStatus.UPGRADE_REQUIRED;
	}

	public int PRECONDITION_REQUIRED() {
		return HttpStatus.PRECONDITION_REQUIRED;
	}

	public int TOO_MANY_REQUESTS() {
		return HttpStatus.TOO_MANY_REQUESTS;
	}

	public int REQUEST_HEADER_FIELDS_TOO_LARGE() {
		return HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE;
	}

	public int UNAVAILABLE_FOR_LEGAL_REASONS() {
		return HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS;
	}

	public int INTERNAL_SERVER_ERROR() {
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

	public int NOT_IMPLEMENTED() {
		return HttpStatus.NOT_IMPLEMENTED;
	}

	public int BAD_GATEWAY() {
		return HttpStatus.BAD_GATEWAY;
	}

	public int SERVICE_UNAVAILABLE() {
		return HttpStatus.SERVICE_UNAVAILABLE;
	}

	public int GATEWAY_TIMEOUT() {
		return HttpStatus.GATEWAY_TIMEOUT;
	}

	public int HTTP_VERSION_NOT_SUPPORTED() {
		return HttpStatus.HTTP_VERSION_NOT_SUPPORTED;
	}

	public int VARIANT_ALSO_NEGOTIATES() {
		return HttpStatus.VARIANT_ALSO_NEGOTIATES;
	}

	public int INSUFFICIENT_STORAGE() {
		return HttpStatus.INSUFFICIENT_STORAGE;
	}

	public int LOOP_DETECTED() {
		return HttpStatus.LOOP_DETECTED;
	}

	public int BANDWIDTH_LIMIT_EXCEEDED() {
		return HttpStatus.BANDWIDTH_LIMIT_EXCEEDED;
	}

	public int NOT_EXTENDED() {
		return HttpStatus.NOT_EXTENDED;
	}

	public int NETWORK_AUTHENTICATION_REQUIRED() {
		return HttpStatus.NETWORK_AUTHENTICATION_REQUIRED;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Response response = (Response) o;
		return this.async == response.async && Objects.equals(this.status, response.status)
				&& Objects.equals(this.delay, response.delay) && Objects.equals(this.headers, response.headers)
				&& Objects.equals(this.cookies, response.cookies) && Objects.equals(this.body, response.body)
				&& Objects.equals(this.bodyMatchers, response.bodyMatchers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.status, this.delay, this.headers, this.cookies, this.body, this.async,
				this.bodyMatchers);
	}

	@Override
	public String toString() {
		return "Response{" + "\nstatus=" + this.status + ", \n\tdelay=" + this.delay + ", \n\theaders=" + this.headers
				+ ", \n\tcookies=" + this.cookies + ", \n\tbody=" + this.body + ", \n\tasync=" + this.async
				+ ", \n\tbodyMatchers=" + this.bodyMatchers + '}';
	}

	/**
	 * Allows to configure HTTP headers.
	 * @param consumer function to manipulate the URL
	 */
	public void headers(Consumer<Headers> consumer) {
		this.headers = new Response.ResponseHeaders();
		consumer.accept(this.headers);
	}

	/**
	 * Allows to configure HTTP cookies.
	 * @param consumer function to manipulate the URL
	 */
	public void cookies(Consumer<Cookies> consumer) {
		this.cookies = new Response.ResponseCookies();
		consumer.accept(this.cookies);
	}

	/**
	 * Allows to set matchers for the body.
	 * @param consumer function to manipulate the URL
	 */
	public void bodyMatchers(Consumer<ResponseBodyMatchers> consumer) {
		this.bodyMatchers = new ResponseBodyMatchers();
		consumer.accept(this.bodyMatchers);
	}

	/**
	 * Allows to configure HTTP headers.
	 * @param consumer function to manipulate the URL
	 */
	public void headers(@DelegatesTo(Headers.class) Closure consumer) {
		this.headers = new Response.ResponseHeaders();
		consumer.setDelegate(this.headers);
		consumer.call();
	}

	/**
	 * Allows to configure HTTP cookies.
	 * @param consumer function to manipulate the URL
	 */
	public void cookies(@DelegatesTo(Cookies.class) Closure consumer) {
		this.cookies = new Response.ResponseCookies();
		consumer.setDelegate(this.cookies);
		consumer.call();
	}

	/**
	 * Allows to set matchers for the body.
	 * @param consumer function to manipulate the URL
	 */
	public void bodyMatchers(@DelegatesTo(ResponseBodyMatchers.class) Closure consumer) {
		this.bodyMatchers = new ResponseBodyMatchers();
		consumer.setDelegate(this.bodyMatchers);
		consumer.call();
	}

	static class ResponseHeaders extends Headers {

		private final Common common = new Common();

		@Override
		public DslProperty matching(final String value) {
			return this.common.$(
					this.common
						.p(notEscaped(Pattern.compile(RegexpUtils.escapeSpecialRegexWithSingleEscape(value) + ".*"))),
					this.common.c(value));
		}

	}

	static class ResponseCookies extends Cookies {

		private final Common common = new Common();

		@Override
		public DslProperty matching(final String value) {
			return this.common.$(
					this.common.p(this.common.regex(RegexpUtils.escapeSpecialRegexWithSingleEscape(value) + ".*")),
					this.common.c(value));
		}

	}

	private class ServerResponse extends Response {

		ServerResponse(Response enclosing, Response request) {
			super(request);
		}

	}

	private class ClientResponse extends Response {

		ClientResponse(Response enclosing, Response request) {
			super(request);
		}

	}

	private static final class ServerPatternValueDslProperty extends PatternValueDslProperty<ServerDslProperty> {

		@Override
		protected ServerDslProperty createProperty(Pattern pattern, @Nullable Object generatedValue) {
			return new ServerDslProperty(pattern, generatedValue);
		}

	}

}
