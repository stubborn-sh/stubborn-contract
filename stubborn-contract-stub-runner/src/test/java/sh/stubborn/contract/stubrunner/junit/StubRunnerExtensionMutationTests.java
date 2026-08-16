/*
 * Copyright 2018-present the original author or authors.
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

package sh.stubborn.contract.stubrunner.junit;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.stubrunner.BatchStubRunner;
import sh.stubborn.contract.stubrunner.StubConfiguration;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class StubRunnerExtensionMutationTests {

	private final StubRunnerExtension extension = new StubRunnerExtension();

	private final BatchStubRunner batchStubRunner = mock(BatchStubRunner.class);

	private StubRunnerExtension withMockFinder() {
		this.extension.stubFinder(this.batchStubRunner);
		return this.extension;
	}

	@Test
	void builderMethodsReturnNonNullChain() {
		assertThat(this.extension.minPort(1000)).isNotNull();
		assertThat(this.extension.maxPort(2000)).isNotNull();
		assertThat(this.extension.options(sh.stubborn.contract.stubrunner.StubRunnerOptions.fromSystemProps()))
			.isNotNull();
		assertThat(this.extension.downloadStub("g", "a", "1.0.0", "stubs")).isNotNull();
		assertThat(this.extension.downloadStub("g", "a", "1.0.0")).isNotNull();
		assertThat(this.extension.downloadStub("g", "a")).isNotNull();
		assertThat(this.extension.downloadStub("g:a:1.0.0")).isNotNull();
		assertThat(this.extension.downloadLatestStub("g", "a", "stubs")).isNotNull();
		assertThat(this.extension.downloadStubs("g:a:1.0.0", "g:b:1.0.0")).isNotNull();
		assertThat(this.extension.downloadStubs(List.of("g:a:1.0.0"))).isNotNull();
		assertThat(this.extension.withStubPerConsumer(true)).isNotNull();
		assertThat(this.extension.withConsumerName("consumer")).isNotNull();
		assertThat(this.extension.withDeleteStubsAfterTest(true)).isNotNull();
		assertThat(this.extension.withGenerateStubs(true)).isNotNull();
		assertThat(this.extension.failOnNoStubs(true)).isNotNull();
		assertThat(this.extension.withProperties(Map.of("k", "v"))).isNotNull();
		assertThat(this.extension.withHttpServerStubConfigurer(
				sh.stubborn.contract.stubrunner.HttpServerStubConfigurer.NoOpHttpServerStubConfigurer.class))
			.isNotNull();
	}

	@Test
	void messageVerifierSettersReturnNonNullAndStoreValue() {
		MessageVerifierSender<Object> sender = mock(MessageVerifierSender.class);
		MessageVerifierReceiver<Object> receiver = mock(MessageVerifierReceiver.class);
		assertThat(this.extension.messageVerifierSender(sender)).isNotNull();
		assertThat(this.extension.messageVerifierReceiver(receiver)).isNotNull();
		assertThat(this.extension.verifierSender()).isSameAs(sender);
		assertThat(this.extension.verifierReceiver()).isSameAs(receiver);
	}

	@Test
	void getContractsDelegatesToStubFinder() {
		Map<StubConfiguration, Collection<Contract>> contracts = Map.of(new StubConfiguration("g:a:1.0.0"),
				List.of(new Contract()));
		given(this.batchStubRunner.getContracts()).willReturn(contracts);
		assertThat(withMockFinder().getContracts()).isEqualTo(contracts);
	}

	@Test
	void labelsDelegateToStubFinder() {
		Map<String, Collection<String>> labels = Map.of("g:a:1.0.0", List.of("label"));
		given(this.batchStubRunner.labels()).willReturn(labels);
		assertThat(withMockFinder().labels()).isEqualTo(labels);
	}

	@Test
	void triggerWithNotationAndLabelReturnsTrueWhenSuccessful() {
		given(this.batchStubRunner.trigger("g:a", "label")).willReturn(true);
		assertThat(withMockFinder().trigger("g:a", "label")).isTrue();
	}

	@Test
	void triggerWithNotationAndLabelThrowsWhenFailed() {
		given(this.batchStubRunner.trigger("g:a", "label")).willReturn(false);
		assertThatThrownBy(() -> withMockFinder().trigger("g:a", "label")).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("g:a")
			.hasMessageContaining("label");
	}

	@Test
	void triggerWithLabelReturnsTrueWhenSuccessful() {
		given(this.batchStubRunner.trigger("label")).willReturn(true);
		assertThat(withMockFinder().trigger("label")).isTrue();
	}

	@Test
	void triggerWithLabelThrowsWhenFailed() {
		given(this.batchStubRunner.trigger("label")).willReturn(false);
		assertThatThrownBy(() -> withMockFinder().trigger("label")).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("label");
	}

	@Test
	void triggerAllReturnsTrueWhenSuccessful() {
		given(this.batchStubRunner.trigger()).willReturn(true);
		assertThat(withMockFinder().trigger()).isTrue();
	}

	@Test
	void triggerAllThrowsWhenFailed() {
		given(this.batchStubRunner.trigger()).willReturn(false);
		assertThatThrownBy(() -> withMockFinder().trigger()).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Failed to trigger a message");
	}

}
