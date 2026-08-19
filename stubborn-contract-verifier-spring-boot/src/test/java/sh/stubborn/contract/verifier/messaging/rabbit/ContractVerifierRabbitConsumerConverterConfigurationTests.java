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

package sh.stubborn.contract.verifier.messaging.rabbit;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the out-of-the-box RabbitMQ consumer JSON converter auto-configuration. Verifies
 * the conditions (default-on, opt-out, user-override back-off) and — deterministically,
 * with no broker — the actual conversion behaviour the feature relies on: a JSON message
 * with {@code contentType=application/json} and no {@code __TypeId__} header is bound to
 * the type inferred from the listener method parameter.
 *
 * @author Marcin Grzejszczak
 */
class ContractVerifierRabbitConsumerConverterConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ContractVerifierRabbitConsumerConverterConfiguration.class));

	@Test
	void shouldRegisterJsonMessageConverterByDefault() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(MessageConverter.class);
			assertThat(context).hasSingleBean(JacksonJsonMessageConverter.class);
		});
	}

	@Test
	void shouldNotRegisterConverterWhenDisabled() {
		this.contextRunner.withPropertyValues("stubborn.contract.messaging.consumer-converters.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(MessageConverter.class));
	}

	@Test
	void shouldRegisterConverterWhenExplicitlyEnabled() {
		this.contextRunner.withPropertyValues("stubborn.contract.messaging.consumer-converters.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(MessageConverter.class));
	}

	@Test
	void shouldBackOffWhenUserDefinesMessageConverter() {
		this.contextRunner.withUserConfiguration(CustomConverterConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(MessageConverter.class);
			assertThat(context.getBean(MessageConverter.class)).isSameAs(context.getBean("customMessageConverter"));
		});
	}

	@Test
	void shouldConvertJsonMessageToInferredTypeWithoutTypeIdHeader() {
		JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(new JsonMapper());
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		// The listener adapter supplies the method-parameter type as the inferred type;
		// there is no __TypeId__ header on the message.
		properties.setInferredArgumentType(Person.class);
		Message message = new Message("{ \"id\" : 9, \"name\" : \"me\" }".getBytes(StandardCharsets.UTF_8), properties);

		Object payload = converter.fromMessage(message);

		assertThat(payload).isInstanceOf(Person.class);
		assertThat(((Person) payload).getName()).isEqualTo("me");
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldNotDeserializeToStringWhenATypeIdHeaderIsPresent() {
		this.contextRunner.run((context) -> {
			JacksonJsonMessageConverter converter = context.getBean(JacksonJsonMessageConverter.class);
			MessageProperties properties = new MessageProperties();
			properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
			// Reproduces the real @RabbitListener path: the contract message carries a
			// Spring
			// '__TypeId__=java.lang.String' header and NO inferred-argument type is set
			// on the
			// properties. The default mapper would honour '__TypeId__' and deserialize
			// the
			// JSON body into a String, which Spring's messaging layer then cannot bind to
			// the
			// typed listener parameter. The converter must ignore '__TypeId__' and yield
			// the
			// JSON object as a map instead, so the messaging layer can bind it to the
			// record.
			properties.setHeader("__TypeId__", "java.lang.String");
			Message message = new Message("{ \"id\" : 9, \"name\" : \"me\" }".getBytes(StandardCharsets.UTF_8),
					properties);

			Object payload = converter.fromMessage(message);

			assertThat(payload).isInstanceOf(Map.class);
			assertThat((Map<String, Object>) payload).containsEntry("name", "me").containsEntry("id", 9);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConverterConfiguration {

		@Bean
		MessageConverter customMessageConverter() {
			return new JacksonJsonMessageConverter(new JsonMapper());
		}

	}

}
