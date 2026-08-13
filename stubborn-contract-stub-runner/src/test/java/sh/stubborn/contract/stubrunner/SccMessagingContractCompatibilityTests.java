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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Messaging compatibility: an output-message contract authored in Spring Cloud Contract's
 * YAML format ({@code label} + {@code input.triggeredBy} + {@code outputMessage}) is
 * loaded and triggered by the Spring-free Stubborn stub runner, and the contract's
 * message is published to the destination the contract names — proving SCC messaging
 * stubs are consumed unchanged.
 */
class SccMessagingContractCompatibilityTests {

	private static final int MIN_PORT = 18999;

	private static final int MAX_PORT = 19999;

	@Test
	void triggersAnSccYamlMessagingContractThroughTheStubRunner() {
		CapturingSender sender = new CapturingSender();
		StubRunnerExecutor executor = new StubRunnerExecutor(new AvailablePortScanner(MIN_PORT, MAX_PORT), sender,
				List.of());
		StubConfiguration stub = new StubConfiguration("com.example", "scc-messaging", "stubs", "");
		StubRunnerOptions options = new StubRunnerOptionsBuilder().build();
		try {
			executor.runStubs(options,
					new StubRepository(new File("src/test/resources/scc-messaging"), List.of(), options, null), stub);

			boolean triggered = executor.trigger("greeting_event");

			assertThat(triggered).isTrue();
			assertThat(sender.destination).isEqualTo("greetings");
			assertThat(String.valueOf(sender.payload)).contains("Hello from the SCC producer");
		}
		finally {
			executor.shutdown();
		}
	}

	private static final class CapturingSender
			implements MessageVerifierSender<Object>, MessageVerifierReceiver<Object> {

		private @Nullable String destination;

		private @Nullable Object payload;

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
