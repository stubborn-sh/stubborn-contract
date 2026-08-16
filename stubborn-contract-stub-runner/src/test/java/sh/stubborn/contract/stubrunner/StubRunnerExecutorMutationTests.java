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

package sh.stubborn.contract.stubrunner;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SuppressWarnings("NullAway")
class StubRunnerExecutorMutationTests {

	private static final int MIN_PORT = 18000;

	private static final int MAX_PORT = 18500;

	private final AvailablePortScanner portScanner = new AvailablePortScanner(MIN_PORT, MAX_PORT);

	private final StubRunnerOptions options = new StubRunnerOptionsBuilder().build();

	private StubRepository repository(String path) {
		return new StubRepository(new File(path), List.of(), this.options, null);
	}

	@Test
	void shouldMatchGroupArtifactAndVersionForThreePartNotation() throws Exception {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		try {
			executor.runStubs(this.options, repository("src/test/resources/repository"),
					new StubConfiguration("group:artifact", "stubs"));
			URL url = executor.findStubUrl("group:artifact:+");
			assertThat(url.getPort()).isBetween(MIN_PORT, MAX_PORT);
			assertThatExceptionOfType(StubNotFoundException.class)
				.isThrownBy(() -> executor.findStubUrl("group:artifact:9.9.9"));
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldRejectNotationWithMoreThanFourParts() throws Exception {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		try {
			executor.runStubs(this.options, repository("src/test/resources/repository"),
					new StubConfiguration("group:artifact", "stubs"));
			assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> executor.findStubUrl("group:artifact:+:stubs:extra"));
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldMatchByArtifactIdOnlyAndRejectWrongGroup() throws Exception {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		try {
			executor.runStubs(this.options, repository("src/test/resources/repository"),
					new StubConfiguration("group:artifact", "stubs"));
			// single-token notation resolves by artifactId only
			assertThat(executor.findStubUrl("artifact").getPort()).isBetween(MIN_PORT, MAX_PORT);
			assertThat(executor.findStubUrl(null, "artifact").getPort()).isBetween(MIN_PORT, MAX_PORT);
			// group + artifact where the group does not match must fail
			assertThatExceptionOfType(StubNotFoundException.class)
				.isThrownBy(() -> executor.findStubUrl("wronggroup", "artifact"));
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldMatchClassifierForFourPartNotation() throws Exception {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		try {
			executor.runStubs(this.options, repository("src/test/resources/repository"),
					new StubConfiguration("group:artifact", "stubs"));
			URL url = executor.findStubUrl("group:artifact:+:stubs");
			assertThat(url.getPort()).isBetween(MIN_PORT, MAX_PORT);
			assertThatExceptionOfType(StubNotFoundException.class)
				.isThrownBy(() -> executor.findStubUrl("group:artifact:+:wrongclassifier"));
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldStartRealHttpServerWhenContractsHaveRequests() throws Exception {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		try {
			RunningStubs stubs = executor.runStubs(this.options, repository("src/test/resources/repository"),
					new StubConfiguration("group:artifact", "stubs"));
			assertThat(stubs.getPort("group", "artifact")).isBetween(MIN_PORT, MAX_PORT);
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldStartRealHttpServerForRequestContractsWithoutPrebuiltMappings() throws Exception {
		// httpcontract has an HTTP contract (with a request) but no pre-built json
		// mappings,
		// so only the hasRequest() check can decide to start a real server
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		try {
			RunningStubs stubs = executor.runStubs(this.options,
					repository("src/test/resources/repository/httpcontract"),
					new StubConfiguration("some", "artifact", "1.0.0", ""));
			assertThat(stubs.getPort("some", "artifact")).isBetween(MIN_PORT, MAX_PORT);
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldNotStartHttpServerForMessagingOnlyContracts() throws Exception {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		try {
			RunningStubs stubs = executor.runStubs(this.options, repository("src/test/resources/messages"),
					new StubConfiguration("asd", "asd", "asd", ""));
			assertThat(stubs.getPort("asd")).isEqualTo(-1);
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldExposeLabelsForMessagingContracts() {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		try {
			executor.runStubs(this.options, repository("src/test/resources/messages"),
					new StubConfiguration("asd", "asd", "asd", ""));
			assertThat(executor.labels()).containsKey("asd:asd:asd:");
			assertThat(executor.labels().get("asd:asd:asd:")).contains("trigger");
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldSendAvroContractBodyAsRawMapNotJsonString() {
		CapturingVerifier verifier = new CapturingVerifier();
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner, verifier, List.of());
		try {
			executor.runStubs(this.options, repository("src/test/resources/avro-repo"),
					new StubConfiguration("avro", "avro", "1.0.0", ""));
			assertThat(executor.trigger("book_returned")).isTrue();

			assertThat(verifier.destination).isEqualTo("book.returned");
			// avro contract: raw body is passed straight through as a Map, not serialized
			assertThat(verifier.payload).isInstanceOf(Map.class);
			assertThat(verifier.payload.toString()).contains("978-1234567890");
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldSendNonAvroContractBodyAsJsonString() {
		CapturingVerifier verifier = new CapturingVerifier();
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner, verifier, List.of());
		try {
			executor.runStubs(this.options, repository("src/test/resources/messages"),
					new StubConfiguration("asd", "asd", "asd", ""));
			assertThat(executor.trigger("trigger")).isTrue();

			// non-avro contract: body is serialized to a JSON String
			assertThat(verifier.payload).isInstanceOf(String.class);
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldReturnCachedRunningStubsOnSecondRun() throws Exception {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		try {
			StubConfiguration stub = new StubConfiguration("group:artifact", "stubs");
			RunningStubs first = executor.runStubs(this.options, repository("src/test/resources/repository"), stub);
			RunningStubs second = executor.runStubs(this.options, repository("src/test/resources/repository"), stub);
			assertThat(second.toIvyToPortMapping()).isEqualTo(first.toIvyToPortMapping());
		}
		finally {
			executor.shutdown();
		}
	}

	private static final class CapturingVerifier
			implements MessageVerifierSender<Object>, MessageVerifierReceiver<Object> {

		private @Nullable Object payload;

		private @Nullable String destination;

		@Override
		public void send(Object message, String destination, @Nullable YamlContract contract) {
			this.payload = message;
			this.destination = destination;
		}

		@Override
		public <T> void send(T payload, @Nullable Map<String, Object> headers, String destination,
				@Nullable YamlContract contract) {
			this.payload = payload;
			this.destination = destination;
		}

		@Override
		public @Nullable Object receive(String destination, long timeout, TimeUnit timeUnit,
				@Nullable YamlContract contract) {
			return null;
		}

		@Override
		public @Nullable Object receive(String destination, @Nullable YamlContract contract) {
			return null;
		}

	}

}
