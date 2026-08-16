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

package sh.stubborn.contract.verifier.http;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link Body}, {@link Request} and {@link Response}.
 */
class BodyRequestResponseMutationTests {

	// --- Body ---

	@Test
	void body_asByteArray_from_bytes_is_verbatim() {
		byte[] bytes = { 1, 2, 3 };
		assertThat(new Body(bytes).asByteArray()).isSameAs(bytes);
	}

	@Test
	void body_asByteArray_from_string_uses_default_charset() {
		assertThat(new Body("abc").asByteArray()).containsExactly("abc".getBytes());
	}

	@Test
	void body_asString_from_string() {
		assertThat(new Body("hello").asString()).isEqualTo("hello");
	}

	@Test
	void body_asString_from_bytes_uses_charset() {
		byte[] bytes = "world".getBytes(StandardCharsets.UTF_8);
		assertThat(new Body(bytes).asString(StandardCharsets.UTF_8)).isEqualTo("world");
	}

	@Test
	void body_asString_from_other_object() {
		assertThat(new Body(42).asString()).isEqualTo("42");
	}

	// --- Request factory methods ---

	@Test
	void request_factory_methods_set_method() {
		assertThat(Request.given().get("/p").build().method().name()).isEqualTo("GET");
		assertThat(Request.given().head("/p").build().method().name()).isEqualTo("HEAD");
		assertThat(Request.given().options("/p").build().method().name()).isEqualTo("OPTIONS");
		assertThat(Request.given().patch("/p").build().method().name()).isEqualTo("PATCH");
		assertThat(Request.given().post("/p").build().method().name()).isEqualTo("POST");
		assertThat(Request.given().put("/p").build().method().name()).isEqualTo("PUT");
		assertThat(Request.given().trace("/p").build().method().name()).isEqualTo("TRACE");
	}

	@Test
	void request_contentType_from_header_and_null_when_absent() {
		Request withHeader = Request.given().post("/p").header("Content-Type", "application/json").build();
		assertThat(withHeader.contentType()).isEqualTo("application/json");
		Request without = Request.given().get("/p").build();
		assertThat(without.contentType()).isNull();
	}

	@Test
	void request_scheme_protocol_body_path() {
		Request request = Request.given().post("/path").scheme("https").protocol("h2").body("payload").build();
		assertThat(request.scheme()).isEqualTo(ContractVerifierHttpMetaData.Scheme.HTTPS);
		assertThat(request.protocol()).isEqualTo(ContractVerifierHttpMetaData.Protocol.HTTP_2);
		assertThat(request.path()).isEqualTo("/path");
		assertThat(request.body()).isNotNull();
		assertThat(request.body().asString()).isEqualTo("payload");
	}

	// --- Response ---

	@Test
	void response_header_lookup_is_case_insensitive() {
		Map<String, Object> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		Response response = new Response(200, new Body("body"), headers, new HashMap<>());
		assertThat(response.header("content-type")).isEqualTo("application/json");
		assertThat(response.header("missing")).isNull();
	}

	@Test
	void response_cookie_lookup_is_case_insensitive() {
		Map<String, Object> cookies = new HashMap<>();
		cookies.put("Session", "abc123");
		Response response = new Response(200, new Body("body"), new HashMap<>(), cookies);
		assertThat(response.cookie("session")).isEqualTo("abc123");
		assertThat(response.cookie("missing")).isNull();
	}

	@Test
	void response_getBody_and_statusCode() {
		Body body = new Body("body");
		Response response = new Response(201, body, new HashMap<>(), new HashMap<>());
		assertThat(response.getBody()).isSameAs(body);
		assertThat(response.statusCode()).isEqualTo(201);
	}

}
