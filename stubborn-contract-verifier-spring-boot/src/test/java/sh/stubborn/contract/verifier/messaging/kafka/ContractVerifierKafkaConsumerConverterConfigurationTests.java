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

package sh.stubborn.contract.verifier.messaging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the out-of-the-box Kafka consumer JSON converter auto-configuration. Verifies the
 * conditions (default-on, opt-out, user-override back-off) and — deterministically, with
 * no broker — the actual conversion behaviour the feature relies on: a JSON record with
 * no {@code __TypeId__} header is bound to the type inferred from the listener method
 * parameter.
 *
 * @author Marcin Grzejszczak
 */
class ContractVerifierKafkaConsumerConverterConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ContractVerifierKafkaConsumerConverterConfiguration.class));

	@Test
	void shouldRegisterJsonRecordMessageConverterByDefault() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(RecordMessageConverter.class);
			assertThat(context).hasSingleBean(StringJacksonJsonMessageConverter.class);
		});
	}

	@Test
	void shouldNotRegisterConverterWhenDisabled() {
		this.contextRunner.withPropertyValues("stubborn.contract.messaging.consumer-converters.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(RecordMessageConverter.class));
	}

	@Test
	void shouldRegisterConverterWhenExplicitlyEnabled() {
		this.contextRunner.withPropertyValues("stubborn.contract.messaging.consumer-converters.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(RecordMessageConverter.class));
	}

	@Test
	void shouldBackOffWhenUserDefinesRecordMessageConverter() {
		this.contextRunner.withUserConfiguration(CustomConverterConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(RecordMessageConverter.class);
			assertThat(context.getBean(RecordMessageConverter.class))
				.isSameAs(context.getBean("customRecordMessageConverter"));
		});
	}

	@Test
	void shouldBackOffWhenConsumerConfiguresItsOwnValueDeserializer() {
		this.contextRunner
			.withPropertyValues("spring.kafka.consumer.value-deserializer="
					+ "org.springframework.kafka.support.serializer.JsonDeserializer")
			.run((context) -> assertThat(context).doesNotHaveBean(RecordMessageConverter.class));
	}

	@Test
	void shouldRegisterConverterWhenValueDeserializerIsExplicitlyStringDeserializer() {
		this.contextRunner
			.withPropertyValues("spring.kafka.consumer.value-deserializer="
					+ "org.apache.kafka.common.serialization.StringDeserializer")
			.run((context) -> assertThat(context).hasSingleBean(RecordMessageConverter.class));
	}

	@Test
	void shouldConvertJsonRecordToInferredTypeWithoutTypeIdHeader() {
		StringJacksonJsonMessageConverter converter = new StringJacksonJsonMessageConverter(new JsonMapper());
		ConsumerRecord<String, String> record = new ConsumerRecord<>("book-returned", 0, 0L, null,
				"{ \"bookName\" : \"foo\" }");

		// The container passes the listener-method parameter type as the inferred payload
		// type; there is no __TypeId__ header on the record.
		Message<?> message = converter.toMessage(record, null, null, BookReturned.class);

		assertThat(message.getPayload()).isInstanceOf(BookReturned.class);
		assertThat(((BookReturned) message.getPayload()).getBookName()).isEqualTo("foo");
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConverterConfiguration {

		@Bean
		RecordMessageConverter customRecordMessageConverter() {
			return new StringJacksonJsonMessageConverter(new JsonMapper());
		}

	}

}
