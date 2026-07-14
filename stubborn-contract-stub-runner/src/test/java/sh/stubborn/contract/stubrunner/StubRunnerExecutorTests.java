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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

import org.springframework.cloud.test.TestSocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class StubRunnerExecutorTests {

	private static final int MIN_PORT = 18999;

	private static final int MAX_PORT = 19999;

	private AvailablePortScanner portScanner;

	private StubRepository repository;

	private StubConfiguration stub = new StubConfiguration("group:artifact", "stubs");

	private StubRunnerOptions stubRunnerOptions = new StubRunnerOptionsBuilder().build();

	@BeforeEach
	void setUp() {
		this.portScanner = new AvailablePortScanner(MIN_PORT, MAX_PORT);
		this.repository = new StubRepository(new File("src/test/resources/repository"), List.of(),
				this.stubRunnerOptions, null);
	}

	@Test
	void shouldProvideUrlForGivenRelativePathOfStub() throws Exception {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		try {
			executor.runStubs(this.stubRunnerOptions, this.repository, this.stub);
			URL url = executor.findStubUrl("group", "artifact");
			assertThat(url.getPort()).isBetween(MIN_PORT, MAX_PORT);
			assertThat(executor.findAllRunningStubs().isPresent("artifact")).isTrue();
			assertThat(executor.findAllRunningStubs().isPresent("group", "artifact")).isTrue();
			assertThat(executor.findAllRunningStubs().isPresent("group:artifact")).isTrue();
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldProvideNoUrlForUnknownDependencyPath() {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		try {
			executor.runStubs(this.stubRunnerOptions, this.repository, this.stub);
			assertThatExceptionOfType(StubNotFoundException.class)
				.isThrownBy(() -> executor.findStubUrl("unknowngroup", "unknownartifact"));
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldStartAStubOnAGivenPort() throws Exception {
		int port = TestSocketUtils.findAvailableTcpPort();
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		StubRunnerOptions options = new StubRunnerOptionsBuilder()
			.withStubs("group:artifact:" + port + ",someotherartifact:" + TestSocketUtils.findAvailableTcpPort())
			.build();
		try {
			executor.runStubs(options, this.repository, this.stub);
			assertThat(executor.findStubUrl("group", "artifact")).isEqualTo(new URL("http://localhost:" + port));
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldEnsureThatTriggeredContractsHaveProperlyParsedMessageBodyWhenAMessageIsSent() {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner, new AssertingStubMessages(), List.of());
		try {
			executor.runStubs(this.stubRunnerOptions, this.repository, this.stub);
			executor.trigger("send_order");
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldEnsureThatTriggeredContractsHaveProperlyParsedMessageBodyFromFileAsBytesWhenAMessageIsSent() {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner, new AssertingStubMessages(), List.of());
		try {
			executor.runStubs(this.stubRunnerOptions, this.repository, this.stub);
			executor.trigger("send_order_bin");
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldEnsureThatTriggeredContractsHaveProperlyParsedMessageBodyFromFileAsJsonWhenAMessageIsSent() {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner, new AssertingStubMessages(), List.of());
		try {
			executor.runStubs(this.stubRunnerOptions, this.repository, this.stub);
			executor.trigger("send_order_json");
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldEnsureThatTriggeredContractsHaveProperlyParsedMessageBodyFromFileAsXmlWhenAMessageIsSent() {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner, new AssertingStubMessages(), List.of());
		try {
			executor.runStubs(this.stubRunnerOptions, this.repository, this.stub);
			executor.trigger("send_order_xml");
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldEnsureThatTriggeredContractsHaveProperlyParsedMessageBodyFromFileAsTextWhenAMessageIsSent() {
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner, new AssertingStubMessages(), List.of());
		try {
			executor.runStubs(this.stubRunnerOptions, this.repository, this.stub);
			executor.trigger("send_order_csv");
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldMatchStubWithEmptyClassifier() throws Exception {
		StubConfiguration stubConf = new StubConfiguration("groupX", "artifactX", "versionX", "");
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		try {
			executor.runStubs(this.stubRunnerOptions, this.repository, stubConf);
			URL url = executor.findStubUrl("groupX:artifactX:versionX:");
			assertThat(url.getPort()).isBetween(MIN_PORT, MAX_PORT);
			assertThat(executor.findAllRunningStubs().isPresent("artifactX")).isTrue();
			assertThat(executor.findAllRunningStubs().isPresent("groupX", "artifactX")).isTrue();
			assertThat(executor.findAllRunningStubs().isPresent("groupX:artifactX")).isTrue();
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldReturnFalseIfNoMessagesAreFound() {
		StubConfiguration stubConf = new StubConfiguration("asd", "asd", "asd", "");
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		try {
			executor.runStubs(this.stubRunnerOptions,
					new StubRepository(new File("src/test/resources/repository/httpcontract"), List.of(),
							new StubRunnerOptionsBuilder().build(), null),
					stubConf);
			assertThat(executor.trigger()).isFalse();
			assertThat(executor.trigger("missing", "label")).isFalse();
			assertThat(executor.trigger("label")).isFalse();
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldNotStartHttpServerIfNoContractsOrMappingsAreFound() {
		StubConfiguration stubConf = new StubConfiguration("asd", "asd", "asd", "");
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner);
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withFailOnNoStubs(false).build();
		try {
			RunningStubs stubs = executor.runStubs(options,
					new StubRepository(new File("src/test/resources/emptyrepo"), List.of(), options, null), stubConf);
			assertThat(stubs.getPort("asd")).isEqualTo(-1);
		}
		finally {
			executor.shutdown();
		}
	}

	@Test
	void shouldGenerateRegexValuesWhenMessageIsToBeSetAndItContainsRegex() {
		MockMessageVerifier messageVerifier = new MockMessageVerifier();
		StubRunnerExecutor executor = new StubRunnerExecutor(this.portScanner, messageVerifier, List.of());
		try {
			StubConfiguration stubConf = new StubConfiguration("asd", "asd", "asd", "");
			executor.runStubs(this.stubRunnerOptions, new StubRepository(new File("src/test/resources/messages"),
					List.of(), new StubRunnerOptionsBuilder().build(), null), stubConf);
			boolean triggered = executor.trigger("trigger");
			assertThat(triggered).isTrue();
			assertThat(messageVerifier.called).isTrue();
		}
		finally {
			executor.shutdown();
		}
	}

	static class MockMessageVerifier implements MessageVerifierSender<Object>, MessageVerifierReceiver<Object> {

		boolean called;

		@Override
		public void send(Object message, String destination, YamlContract contract) {
		}

		@Override
		public Object receive(String destination, long timeout, TimeUnit timeUnit, YamlContract contract) {
			return null;
		}

		@Override
		public Object receive(String destination, YamlContract contract) {
			return null;
		}

		@Override
		public <T> void send(T payload, Map<String, Object> headers, String destination, YamlContract contract) {
			this.called = true;
			assertThat(payload.toString()).doesNotContain("cursor");
			assertThat(headers.values()).allMatch(v -> !v.toString().contains("cursor"));
		}

	}

	private static class AssertingStubMessages
			implements MessageVerifierSender<Object>, MessageVerifierReceiver<Object> {

		@Override
		public void send(Object message, String destination, YamlContract contract) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> void send(T payload, Map<String, Object> headers, String destination, YamlContract contract) {
			if (payload instanceof String) {
				assertThat((String) payload).doesNotContain("serverValue");
			}
			assertThat(headers.entrySet()).allMatch(e -> !e.getValue().toString().contains("serverValue"));
		}

		@Override
		public Object receive(String destination, long timeout, TimeUnit timeUnit, YamlContract contract) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object receive(String destination, YamlContract contract) {
			throw new UnsupportedOperationException();
		}

	}

}
