/*
 * Copyright 2012-present the original author or authors.
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

package sh.stubborn.contract.verifier.http;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.spec.internal.HttpMethods;

/**
 * Abstraction over a HTTP request.
 *
 * Warning! This API is experimental and can change in time.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public class Request {

	private final ContractVerifierHttpMetaData.Protocol protocol;

	private final ContractVerifierHttpMetaData.Scheme scheme;

	private final HttpMethods.HttpMethod method;

	private final String path;

	private final List<AbstractMap.SimpleEntry<String, String>> queryParameters;

	private final Body body;

	private final Map<String, Object> headers;

	private final Map<String, Object> cookies;

	Request(ContractVerifierHttpMetaData.Protocol protocol, ContractVerifierHttpMetaData.Scheme scheme,
			HttpMethods.HttpMethod method, String path, List<AbstractMap.SimpleEntry<String, String>> queryParameters,
			Body body, Map<String, Object> headers, Map<String, Object> cookies) {
		this.protocol = protocol;
		this.scheme = scheme;
		this.method = method;
		this.path = path;
		this.queryParameters = (queryParameters != null) ? queryParameters : new LinkedList<>();
		this.body = body;
		this.headers = (headers != null) ? headers : new HashMap<>();
		this.cookies = (cookies != null) ? cookies : new HashMap<>();
	}

	/**
	 * Returns the content type from the request headers.
	 * @return content type from headers
	 */
	public @Nullable String contentType() {
		Object value = this.headers.entrySet()
			.stream()
			.filter((e) -> e.getKey().toLowerCase(Locale.ROOT).equals("content-type"))
			.findFirst()
			.orElse(new AbstractMap.SimpleEntry<>("", null))
			.getValue();
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	/**
	 * Returns the request protocol.
	 * @return {@link ContractVerifierHttpMetaData.Protocol}
	 */
	public ContractVerifierHttpMetaData.Protocol protocol() {
		return this.protocol;
	}

	/**
	 * Returns the request scheme.
	 * @return {@link ContractVerifierHttpMetaData.Scheme}
	 */
	public ContractVerifierHttpMetaData.Scheme scheme() {
		return this.scheme;
	}

	/**
	 * Returns the HTTP method.
	 * @return the HTTP method
	 */
	public HttpMethods.HttpMethod method() {
		return this.method;
	}

	/**
	 * Returns the HTTP path.
	 * @return the HTTP path
	 */
	public String path() {
		return this.path;
	}

	/**
	 * Returns the request body.
	 * @return request body
	 */
	public Body body() {
		return this.body;
	}

	/**
	 * Returns the request headers.
	 * @return request headers
	 */
	public Map<String, Object> headers() {
		return this.headers;
	}

	/**
	 * Returns the request cookies.
	 * @return request cookies
	 */
	public Map<String, Object> cookies() {
		return this.cookies;
	}

	/**
	 * Returns the query parameters.
	 * @return query parameters
	 */
	public List<AbstractMap.SimpleEntry<String, String>> queryParams() {
		return this.queryParameters;
	}

	/**
	 * Factory method to pick the HTTP method.
	 * @return method builder
	 */
	public static Request.MethodBuilder given() {
		return new Request.MethodBuilder();
	}

	/**
	 * Creates a builder from an existing request.
	 * @param request from which a builder will be built
	 * @return a builder with request data filled in
	 */
	public static Request.Builder from(Request request) {
		return new MethodBuilder().method(request.method.getMethodName(), request.path)
			.scheme(request.scheme)
			.protocol(request.protocol)
			.queryParams(request.queryParameters)
			.headers(request.headers)
			.cookies(request.cookies)
			.body(request.body);
	}

	/**
	 * Builder over HTTP methods.
	 *
	 * @since 1.0.0
	 */
	public static class MethodBuilder {

		/**
		 * Factory method for a generic HTTP method.
		 * @param method to call
		 * @param path to call
		 * @return builder
		 */
		public Request.Builder method(String method, String path) {
			return new Request.Builder(HttpMethods.HttpMethod.valueOf(method.toUpperCase(Locale.ROOT)), path);
		}

		/**
		 * Factory method for DELETE HTTP method.
		 * @param path to call
		 * @return builder
		 */
		public Request.Builder delete(String path) {
			return new Request.Builder(HttpMethods.HttpMethod.DELETE, path);
		}

		/**
		 * Factory method for GET HTTP method.
		 * @param path to call
		 * @return builder
		 */
		public Request.Builder get(String path) {
			return new Request.Builder(HttpMethods.HttpMethod.GET, path);
		}

		/**
		 * Factory method for HEAD HTTP method.
		 * @param path to call
		 * @return builder
		 */
		public Request.Builder head(String path) {
			return new Request.Builder(HttpMethods.HttpMethod.HEAD, path);
		}

		/**
		 * Factory method for OPTIONS HTTP method.
		 * @param path to call
		 * @return builder
		 */
		public Request.Builder options(String path) {
			return new Request.Builder(HttpMethods.HttpMethod.OPTIONS, path);
		}

		/**
		 * Factory method for PATCH HTTP method.
		 * @param path to call
		 * @return builder
		 */
		public Request.Builder patch(String path) {
			return new Request.Builder(HttpMethods.HttpMethod.PATCH, path);
		}

		/**
		 * Factory method for POST HTTP method.
		 * @param path to call
		 * @return builder
		 */
		public Request.Builder post(String path) {
			return new Request.Builder(HttpMethods.HttpMethod.POST, path);
		}

		/**
		 * Factory method for PUT HTTP method.
		 * @param path to call
		 * @return builder
		 */
		public Request.Builder put(String path) {
			return new Request.Builder(HttpMethods.HttpMethod.PUT, path);
		}

		/**
		 * Factory method for TRACE HTTP method.
		 * @param path to call
		 * @return builder
		 */
		public Request.Builder trace(String path) {
			return new Request.Builder(HttpMethods.HttpMethod.TRACE, path);
		}

	}

	/**
	 * Builder for a {@link Request}.
	 *
	 * @since 1.0.0
	 */
	public static class Builder {

		HttpMethods.HttpMethod method;

		String path;

		List<AbstractMap.SimpleEntry<String, String>> queryParameters = new LinkedList<>();

		ContractVerifierHttpMetaData.Protocol protocol = ContractVerifierHttpMetaData.Protocol.HTTP_1_1;

		ContractVerifierHttpMetaData.Scheme scheme = ContractVerifierHttpMetaData.Scheme.HTTP;

		// Set only when body(...) is invoked; may remain null, matching prior behavior.
		@SuppressWarnings("NullAway.Init")
		Body body;

		Map<String, Object> headers = new HashMap<>();

		Map<String, Object> cookies = new HashMap<>();

		Builder(HttpMethods.HttpMethod method, String path) {
			this.method = method;
			this.path = path;
		}

		/**
		 * Sets the HTTP method.
		 * @param method the HTTP method
		 * @return builder
		 */
		public Request.Builder method(HttpMethods.HttpMethod method) {
			this.method = method;
			return this;
		}

		/**
		 * Sets the HTTP path.
		 * @param path the HTTP path
		 * @return builder
		 */
		public Request.Builder path(String path) {
			this.path = path;
			return this;
		}

		/**
		 * Sets the scheme from its text representation.
		 * @param scheme text representation of a scheme
		 * @return builder
		 */
		public Request.Builder scheme(String scheme) {
			this.scheme = Objects.requireNonNull(ContractVerifierHttpMetaData.Scheme.fromString(scheme));
			return this;
		}

		/**
		 * Sets the protocol from its text representation.
		 * @param protocol text representation of a protocol
		 * @return builder
		 */
		public Request.Builder protocol(String protocol) {
			this.protocol = Objects.requireNonNull(ContractVerifierHttpMetaData.Protocol.fromString(protocol));
			return this;
		}

		/**
		 * Sets the scheme.
		 * @param scheme representation of a scheme
		 * @return builder
		 */
		public Request.Builder scheme(ContractVerifierHttpMetaData.Scheme scheme) {
			this.scheme = scheme;
			return this;
		}

		/**
		 * Sets the protocol.
		 * @param protocol representation of a protocol
		 * @return builder
		 */
		public Request.Builder protocol(ContractVerifierHttpMetaData.Protocol protocol) {
			this.protocol = protocol;
			return this;
		}

		/**
		 * Sets the HTTP body.
		 * @param body the HTTP body
		 * @return builder
		 */
		public Request.Builder body(Object body) {
			this.body = new Body(body);
			return this;
		}

		/**
		 * Adds a single query parameter.
		 * @param name - query parameter name
		 * @param value - query parameter value
		 * @return builder
		 */
		public Request.Builder queryParam(String name, String value) {
			this.queryParameters.add(new AbstractMap.SimpleEntry<>(name, value));
			return this;
		}

		/**
		 * Sets the list of query parameters.
		 * @param queryParameters - list of query parameters
		 * @return builder
		 */
		public Request.Builder queryParams(List<AbstractMap.SimpleEntry<String, String>> queryParameters) {
			this.queryParameters = queryParameters;
			return this;
		}

		/**
		 * Sets the HTTP headers.
		 * @param headers the HTTP headers
		 * @return builder
		 */
		public Request.Builder headers(Map<String, Object> headers) {
			this.headers = headers;
			return this;
		}

		/**
		 * Adds a single HTTP header.
		 * @param key the HTTP header key
		 * @param value the HTTP header value
		 * @return builder
		 */
		public Request.Builder header(String key, Object value) {
			this.headers.put(key, value);
			return this;
		}

		/**
		 * Adds a single cookie.
		 * @param key cookie key
		 * @param value cookie value
		 * @return builder
		 */
		public Request.Builder cookie(String key, Object value) {
			this.cookies.put(key, value);
			return this;
		}

		/**
		 * Sets the HTTP cookies.
		 * @param cookies the HTTP cookies
		 * @return builder
		 */
		public Request.Builder cookies(Map<String, Object> cookies) {
			this.cookies = cookies;
			return this;
		}

		/**
		 * Builds the request.
		 * @return built {@link Request}
		 */
		public Request build() {
			return new Request(this.protocol, this.scheme, this.method, this.path, this.queryParameters, this.body,
					this.headers, this.cookies);
		}

	}

}
