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

package sh.stubborn.contract.verifier.messaging.jms;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListenerConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the conditions of the out-of-the-box JMS consumer converter auto-configuration:
 * default-on, opt-out, and user-override back-off. The actual JSON-to-record binding is
 * verified against a real broker in
 * {@link ContractVerifierJmsConsumerConverterIntegrationTests}.
 *
 * @author Marcin Grzejszczak
 */
class ContractVerifierJmsConsumerConverterConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ContractVerifierJmsConsumerConverterConfiguration.class));

	@Test
	void shouldRegisterJmsListenerConfigurerByDefault() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(JmsListenerConfigurer.class));
	}

	@Test
	void shouldNotRegisterConfigurerWhenDisabled() {
		this.contextRunner.withPropertyValues("stubborn.contract.messaging.consumer-converters.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(JmsListenerConfigurer.class));
	}

	@Test
	void shouldRegisterConfigurerWhenExplicitlyEnabled() {
		this.contextRunner.withPropertyValues("stubborn.contract.messaging.consumer-converters.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(JmsListenerConfigurer.class));
	}

	@Test
	void shouldBackOffWhenUserDefinesJmsListenerConfigurer() {
		this.contextRunner.withUserConfiguration(CustomConfigurerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(JmsListenerConfigurer.class);
			assertThat(context.getBean(JmsListenerConfigurer.class))
				.isSameAs(context.getBean("customJmsListenerConfigurer"));
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConfigurerConfiguration {

		@Bean
		JmsListenerConfigurer customJmsListenerConfigurer() {
			return (registrar) -> {
			};
		}

	}

}
