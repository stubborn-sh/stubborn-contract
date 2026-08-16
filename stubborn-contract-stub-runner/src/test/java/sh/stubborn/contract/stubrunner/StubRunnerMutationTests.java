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
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

import org.springframework.cloud.test.TestSocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

class StubRunnerMutationTests {

	private StubRunnerOptions options() {
		int port = TestSocketUtils.findAvailableTcpPort();
		return new StubRunnerOptionsBuilder().withMinMaxPort(port, port).build();
	}

	@Test
	void shouldExposeContractsLabelsAndTriggerMessages() throws Exception {
		StubConfiguration stub = new StubConfiguration("asd", "asd", "asd", "");
		StubRunner runner = new StubRunner(options(), "src/test/resources/messages", stub, new NoOpSender());
		try {
			runner.runStubs();

			Map<StubConfiguration, Collection<Contract>> contracts = runner.getContracts();
			assertThat(contracts).containsOnlyKeys(stub);
			assertThat(contracts.get(stub)).isNotEmpty();

			assertThat(runner.labels()).containsKey("asd:asd:asd:");
			assertThat(runner.labels().get("asd:asd:asd:")).contains("trigger");

			assertThat(runner.trigger("trigger")).isTrue();
			assertThat(runner.trigger("asd:asd", "trigger")).isTrue();
			assertThat(runner.trigger()).isTrue();
		}
		finally {
			runner.close();
		}
	}

	@Test
	void shouldWriteAndClearMappingsOutputFolder(@TempDir Path outputFolder) throws Exception {
		StubConfiguration stub = new StubConfiguration("group:artifact", "stubs");
		int port = TestSocketUtils.findAvailableTcpPort();
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withMinMaxPort(port, port)
			.withMappingsOutputFolder(outputFolder.toString())
			.build();
		StubRunner runner = new StubRunner(options, "src/test/resources/repository", stub);
		try {
			runner.runStubs();
			File[] afterFirst = outputFolder.toFile().listFiles();
			assertThat(afterFirst).isNotNull();
			assertThat(afterFirst).anyMatch((file) -> file.getName().startsWith("artifact"));
		}
		finally {
			runner.close();
		}

		// second run against a fresh runner clears the previous mapping file then
		// rewrites
		int port2 = TestSocketUtils.findAvailableTcpPort();
		StubRunnerOptions options2 = new StubRunnerOptionsBuilder().withMinMaxPort(port2, port2)
			.withMappingsOutputFolder(outputFolder.toString())
			.build();
		StubRunner runner2 = new StubRunner(options2, "src/test/resources/repository", stub);
		try {
			runner2.runStubs();
			File[] afterSecond = outputFolder.toFile().listFiles();
			assertThat(afterSecond).isNotNull();
			assertThat(afterSecond).anyMatch((file) -> file.getName().startsWith("artifact"));
		}
		finally {
			runner2.close();
		}
	}

	@Test
	void shouldReturnFalseFromTriggersWhenNoMessagingContractsMatch() throws Exception {
		// httpcontract only has an HTTP contract (no output messages) so every trigger is
		// a
		// no-op returning false
		StubConfiguration stub = new StubConfiguration("some", "artifact", "1.0.0", "");
		StubRunner runner = new StubRunner(options(), "src/test/resources/repository/httpcontract", stub);
		try {
			runner.runStubs();
			assertThat(runner.trigger()).isFalse();
			assertThat(runner.trigger("no-such-label")).isFalse();
			assertThat(runner.trigger("some:artifact", "no-such-label")).isFalse();
		}
		finally {
			runner.close();
		}
	}

	@Test
	void shouldSkipWritingMappingsWhenNoHttpMappingsRegistered(@TempDir Path outputFolder) throws Exception {
		// messaging-only stubs produce no registered HTTP mappings, so nothing is written
		// to the mappings output folder (the blank-mappings branch)
		StubConfiguration stub = new StubConfiguration("asd", "asd", "asd", "");
		int port = TestSocketUtils.findAvailableTcpPort();
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withMinMaxPort(port, port)
			.withMappingsOutputFolder(outputFolder.toString())
			.build();
		StubRunner runner = new StubRunner(options, "src/test/resources/messages", stub, new NoOpSender());
		try {
			runner.runStubs();
			assertThat(outputFolder.toFile().listFiles()).isEmpty();
		}
		finally {
			runner.close();
		}
	}

	@Test
	void shouldClearOnlyMappingsMatchingArtifactPrefix(@TempDir Path outputFolder) throws Exception {
		// pre-seed the folder: one file that matches the artifact prefix (must be
		// deleted)
		// and one that does not (must be preserved) — exercises the filename filter
		File stale = outputFolder.resolve("artifact_stale").toFile();
		File keep = outputFolder.resolve("keepme.txt").toFile();
		java.nio.file.Files.writeString(stale.toPath(), "stale");
		java.nio.file.Files.writeString(keep.toPath(), "keep");

		StubConfiguration stub = new StubConfiguration("group:artifact", "stubs");
		int port = TestSocketUtils.findAvailableTcpPort();
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withMinMaxPort(port, port)
			.withMappingsOutputFolder(outputFolder.toString())
			.build();
		StubRunner runner = new StubRunner(options, "src/test/resources/repository", stub);
		try {
			runner.runStubs();
			assertThat(keep).exists();
			assertThat(stale).doesNotExist();
		}
		finally {
			runner.close();
		}
	}

	private static final class NoOpSender implements MessageVerifierSender<Object>, MessageVerifierReceiver<Object> {

		@Override
		public void send(Object message, String destination, @Nullable YamlContract contract) {
		}

		@Override
		public <T> void send(T payload, @Nullable Map<String, Object> headers, String destination,
				@Nullable YamlContract contract) {
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
