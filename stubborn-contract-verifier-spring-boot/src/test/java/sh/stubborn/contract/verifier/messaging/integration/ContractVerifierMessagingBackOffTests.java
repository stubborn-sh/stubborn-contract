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

package sh.stubborn.contract.verifier.messaging.integration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;
import sh.stubborn.contract.verifier.messaging.noop.NoOpContractVerifierAutoConfiguration;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for
 * <a href="https://github.com/stubborn-sh/stubborn-contract/issues/184">gh-184</a>.
 *
 * <p>
 * {@code ContractVerifierIntegrationConfiguration} is guarded by the ubiquitous
 * {@code @ConditionalOnClass(Message.class)} (spring-messaging), and its sender/receiver
 * beans back off via the <b>raw</b>
 * {@code @ConditionalOnMissingBean(MessageVerifierSender/
 * Receiver.class)}. Before the fix, {@code integrationContractVerifierMessaging} injected
 * the <b>narrower</b> {@code MessageVerifierSender<Message<?>>}, so any differently-typed
 * sender present first (e.g. the NoOp {@code MessageVerifierSender<Object>}, or a
 * user-supplied one) suppressed the config's own typed sender while failing to satisfy
 * the generic injection — the context failed to start with
 * {@code NoSuchBeanDefinitionException}. The messaging bean now injects
 * {@code MessageVerifierSender<?>} so it tolerates whatever sender won the back-off.
 */
class ContractVerifierMessagingBackOffTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void integrationAndNoOpTogetherWithMessagePresentStartCleanly() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(ContractVerifierIntegrationConfiguration.class,
					NoOpContractVerifierAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context.getBeansOfType(ContractVerifierMessaging.class)).hasSize(1);
			});
	}

	@Test
	void integrationStartsWhenAnObjectTypedSenderIsAlreadyPresent() {
		this.contextRunner.withBean("preexistingSender", MessageVerifierSender.class, ObjectSender::new)
			.withBean("preexistingReceiver", MessageVerifierReceiver.class, ObjectReceiver::new)
			.withConfiguration(AutoConfigurations.of(ContractVerifierIntegrationConfiguration.class))
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context.getBeansOfType(ContractVerifierMessaging.class)).hasSize(1);
			});
	}

	static final class ObjectSender implements MessageVerifierSender<Object> {

		@Override
		public void send(Object message, String destination, @Nullable YamlContract contract) {
		}

		@Override
		public <T> void send(T payload, @Nullable Map<String, Object> headers, String destination,
				@Nullable YamlContract contract) {
		}

	}

	static final class ObjectReceiver implements MessageVerifierReceiver<Object> {

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
