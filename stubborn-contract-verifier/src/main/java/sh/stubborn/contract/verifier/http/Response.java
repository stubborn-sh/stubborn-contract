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

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Abstraction over a HTTP response.
 *
 * Warning! This API is experimental and can change in time.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public class Response {

	private final int statusCode;

	private final Body body;

	private final Map<String, Object> headers;

	private final Map<String, Object> cookies;

	Response(int statusCode, Body body, Map<String, Object> headers, Map<String, Object> cookies) {
		this.statusCode = statusCode;
		this.body = body;
		this.headers = headers;
		this.cookies = cookies;
	}

	/**
	 * The numerical status code of the response.
	 * @return numerical representation of a status code
	 */
	public int statusCode() {
		return this.statusCode;
	}

	/**
	 * Returns the header value for the given key.
	 * @param key header key
	 * @return header value or null if not present
	 */
	public @Nullable String header(String key) {
		return this.headers.entrySet()
			.stream()
			.filter((e) -> e.getKey().equalsIgnoreCase(key))
			.findFirst()
			.map((e) -> e.getValue().toString())
			.orElse(null);
	}

	/**
	 * Returns the cookie value for the given key.
	 * @param key cookie key
	 * @return header value or null if not present
	 */
	public @Nullable String cookie(String key) {
		return this.cookies.entrySet()
			.stream()
			.filter((e) -> e.getKey().equalsIgnoreCase(key))
			.findFirst()
			.map((e) -> e.getValue().toString())
			.orElse(null);
	}

	/**
	 * The response body.
	 * @return response body
	 */
	public Body getBody() {
		return this.body;
	}

	/**
	 * Creates a new response builder.
	 * @return builder
	 */
	public static Response.Builder builder() {
		return new Response.Builder();
	}

	/**
	 * The response headers.
	 * @return headers
	 */
	public Map<String, Object> headers() {
		return this.headers;
	}

	/**
	 * The response cookies.
	 * @return cookies
	 */
	public Map<String, Object> cookies() {
		return this.cookies;
	}

	/**
	 * Creates a builder pre-filled from an existing response.
	 * @param response template of a response
	 * @return builder filled with response data
	 */
	public static Builder from(Response response) {
		return new Builder().headers(response.headers)
			.statusCode(response.statusCode)
			.cookies(response.cookies)
			.body(response.body);
	}

	/**
	 * Response builder.
	 */
	public static class Builder {

		int statusCode;

		// Set only when body(...) is invoked; may remain null, matching prior behavior.
		@SuppressWarnings("NullAway.Init")
		Body body;

		Map<String, Object> headers = new HashMap<>();

		Map<String, Object> cookies = new HashMap<>();

		/**
		 * Sets the status code.
		 * @param status as int
		 * @return builder
		 */
		public Response.Builder statusCode(int status) {
			this.statusCode = status;
			return this;
		}

		/**
		 * Sets the response body.
		 * @param body - response body
		 * @return builder
		 */
		public Response.Builder body(Object body) {
			this.body = new Body(body);
			return this;
		}

		/**
		 * Sets the response headers.
		 * @param headers - response headers
		 * @return builder
		 */
		public Response.Builder headers(Map<String, Object> headers) {
			this.headers = headers;
			return this;
		}

		/**
		 * Adds a single header.
		 * @param key header key
		 * @param value header value
		 * @return builder
		 */
		public Response.Builder header(String key, Object value) {
			this.headers.put(key, value);
			return this;
		}

		/**
		 * Adds a single cookie.
		 * @param key cookie key
		 * @param value cookie value
		 * @return builder
		 */
		public Response.Builder cookie(String key, Object value) {
			this.cookies.put(key, value);
			return this;
		}

		/**
		 * Sets the response cookies.
		 * @param cookies - response cookies
		 * @return builder
		 */
		public Response.Builder cookies(Map<String, Object> cookies) {
			this.cookies = cookies;
			return this;
		}

		/**
		 * Builds the response.
		 * @return response
		 */
		public Response build() {
			return new Response(this.statusCode, this.body, this.headers, this.cookies);
		}

	}

}
