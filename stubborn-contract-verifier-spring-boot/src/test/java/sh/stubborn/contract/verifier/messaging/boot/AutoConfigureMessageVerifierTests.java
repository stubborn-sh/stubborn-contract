/*
 * Copyright 2020-present the original author or authors.
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

package sh.stubborn.contract.verifier.messaging.boot;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tim Ysewyn
 */
class AutoConfigureMessageVerifierTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(Configuration.class);

	@Test
	void shouldConfigureForNoOpWhenMissingImplementation() {
		this.contextRunner
			.withClassLoader(
					new FilteredClassLoader(org.apache.camel.Message.class, org.springframework.messaging.Message.class,
							JmsTemplate.class, KafkaTemplate.class, RabbitTemplate.class, Binder.class))
			.run((context) -> {
				assertThat(context.getBeansOfType(MessageVerifierSender.class)).hasSize(1);
				assertThat(context.getBeansOfType(ContractVerifierObjectMapper.class)).hasSize(1);
			});
	}

	@Test
	void shouldStartWhenTheApplicationDefinesItsOwnSender() {
		this.contextRunner.withUserConfiguration(CustomSenderConfiguration.class).run((context) -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(MessageVerifierSender.class)).isInstanceOf(CustomSender.class);
			assertThat(context.getBeansOfType(ContractVerifierMessaging.class)).hasSize(1);
		});
	}

	@AutoConfigureMessageVerifier
	private static final class Configuration {

	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class CustomSenderConfiguration {

		@Bean
		CustomSender customMessageVerifierSender() {
			return new CustomSender();
		}

	}

	/**
	 * A sender whose payload type is not the one any built-in configuration injects. The
	 * back-off conditions ignore generics, so this suppresses every shipped sender.
	 */
	static class CustomSender implements MessageVerifierSender<String> {

		@Override
		public void send(String message, String destination, @Nullable YamlContract contract) {
		}

		@Override
		public <T> void send(T payload, @Nullable Map<String, Object> headers, String destination,
				@Nullable YamlContract contract) {
		}

	}

}
