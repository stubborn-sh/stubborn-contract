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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link OkHttpHttpVerifier} exercising a real local HTTP
 * server.
 */
class OkHttpHttpVerifierMutationTests {

	private HttpServer server;

	private int port;

	private final AtomicReference<String> receivedBody = new AtomicReference<>("");

	private final AtomicReference<String> receivedMethod = new AtomicReference<>("");

	private final AtomicReference<String> receivedUri = new AtomicReference<>("");

	@BeforeEach
	void startServer() throws IOException {
		this.server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		this.server.createContext("/", this::handle);
		this.server.start();
		this.port = this.server.getAddress().getPort();
	}

	@AfterEach
	void stopServer() {
		this.server.stop(0);
	}

	private void handle(HttpExchange exchange) throws IOException {
		this.receivedMethod.set(exchange.getRequestMethod());
		this.receivedUri.set(exchange.getRequestURI().toString());
		this.receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
		byte[] response = "pong".getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("X-Custom", "custom-value");
		exchange.getResponseHeaders().add("Set-Cookie", "session=abc");
		exchange.sendResponseHeaders(201, response.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(response);
		}
	}

	private OkHttpHttpVerifier verifier() {
		return new OkHttpHttpVerifier("localhost:" + this.port);
	}

	@Test
	void exchange_post_with_body_query_and_cookie() {
		Request request = Request.given()
			.post("/path")
			.scheme("http")
			.protocol("http/1.1")
			.header("Content-Type", "text/plain")
			.queryParam("q", "1")
			.cookie("c", "v")
			.body("ping")
			.build();
		Response response = verifier().exchange(request);
		assertThat(response.statusCode()).isEqualTo(201);
		assertThat(response.getBody().asString()).isEqualTo("pong");
		assertThat(response.header("X-Custom")).isEqualTo("custom-value");
		assertThat(response.cookie("session")).isEqualTo("abc");
		assertThat(this.receivedMethod.get()).isEqualTo("POST");
		assertThat(this.receivedBody.get()).isEqualTo("ping");
		assertThat(this.receivedUri.get()).isEqualTo("/path?q=1");
	}

	@Test
	void exchange_get_without_body_or_query_and_path_without_slash() {
		Request request = Request.given().get("noslash").scheme("http").protocol("http/1.1").build();
		Response response = verifier().exchange(request);
		assertThat(response.statusCode()).isEqualTo(201);
		assertThat(response.getBody().asString()).isEqualTo("pong");
		assertThat(this.receivedMethod.get()).isEqualTo("GET");
		assertThat(this.receivedUri.get()).isEqualTo("/noslash");
	}

}
