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

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.messaging.kafka.ContractVerifierKafkaConsumerConverterConfiguration.PassThroughStringJacksonJsonMessageConverter;
import sh.stubborn.contract.verifier.messaging.kafka.ContractVerifierKafkaConsumerConverterConfiguration.StubbornContractKafkaListenerConverterPostProcessor;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the out-of-the-box Kafka consumer JSON converter auto-configuration. Verifies the
 * conditions (default-on, opt-out, user-override back-off) and — deterministically, with
 * no broker — that the converter is installed on the {@code @KafkaListener} container
 * factory only, never on the producer {@code KafkaTemplate} (the double-encoding
 * regression, #164), plus the actual conversion behaviour the feature relies on: a JSON
 * record with no {@code __TypeId__} header is bound to the type inferred from the
 * listener method parameter.
 *
 * @author Marcin Grzejszczak
 */
class ContractVerifierKafkaConsumerConverterConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ContractVerifierKafkaConsumerConverterConfiguration.class));

	@Test
	void shouldRegisterListenerConverterPostProcessorByDefault() {
		this.contextRunner.run((context) -> assertThat(context)
			.hasSingleBean(StubbornContractKafkaListenerConverterPostProcessor.class));
	}

	@Test
	void shouldNotRegisterConverterWhenDisabled() {
		this.contextRunner.withPropertyValues("stubborn.contract.messaging.consumer-converters.enabled=false")
			.run((context) -> assertThat(context)
				.doesNotHaveBean(StubbornContractKafkaListenerConverterPostProcessor.class));
	}

	@Test
	void shouldRegisterConverterWhenExplicitlyEnabled() {
		this.contextRunner.withPropertyValues("stubborn.contract.messaging.consumer-converters.enabled=true")
			.run((context) -> assertThat(context)
				.hasSingleBean(StubbornContractKafkaListenerConverterPostProcessor.class));
	}

	@Test
	void shouldBackOffWhenUserDefinesRecordMessageConverter() {
		this.contextRunner.withUserConfiguration(CustomConverterConfiguration.class)
			.run((context) -> assertThat(context)
				.doesNotHaveBean(StubbornContractKafkaListenerConverterPostProcessor.class));
	}

	@Test
	void shouldBackOffWhenConsumerConfiguresItsOwnValueDeserializer() {
		this.contextRunner
			.withPropertyValues("spring.kafka.consumer.value-deserializer="
					+ "org.springframework.kafka.support.serializer.JsonDeserializer")
			.run((context) -> assertThat(context)
				.doesNotHaveBean(StubbornContractKafkaListenerConverterPostProcessor.class));
	}

	@Test
	void shouldRegisterConverterWhenValueDeserializerIsExplicitlyStringDeserializer() {
		this.contextRunner
			.withPropertyValues("spring.kafka.consumer.value-deserializer="
					+ "org.apache.kafka.common.serialization.StringDeserializer")
			.run((context) -> assertThat(context)
				.hasSingleBean(StubbornContractKafkaListenerConverterPostProcessor.class));
	}

	@Test
	void shouldInstallJsonConverterOnListenerContainerFactory() {
		StubbornContractKafkaListenerConverterPostProcessor postProcessor = new StubbornContractKafkaListenerConverterPostProcessor();
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();

		Object result = postProcessor.postProcessAfterInitialization(factory, "kafkaListenerContainerFactory");

		assertThat(result).isSameAs(factory);
		assertThat(ReflectionTestUtils.getField(factory, "recordMessageConverter"))
			.isInstanceOf(PassThroughStringJacksonJsonMessageConverter.class);
	}

	@Test
	void shouldNotInstallConverterOnProducerKafkaTemplate() {
		// #164: the out-of-the-box converter must be scoped to the listener side only.
		// Leaking it onto the producer template re-wraps a JsonSerializer producer's JSON
		// body as a JSON string (tagged __TypeId__=java.lang.String), which makes a
		// field-based assertion generated from a Groovy outputMessage body fail with a
		// jsonpath "can not be applied to primitives" error.
		StubbornContractKafkaListenerConverterPostProcessor postProcessor = new StubbornContractKafkaListenerConverterPostProcessor();
		KafkaTemplate<String, String> template = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(Map.of()));
		RecordMessageConverter original = template.getMessageConverter();

		Object result = postProcessor.postProcessAfterInitialization(template, "kafkaTemplate");

		assertThat(result).isSameAs(template);
		assertThat(template.getMessageConverter()).isSameAs(original);
		assertThat(template.getMessageConverter()).isNotInstanceOf(StringJacksonJsonMessageConverter.class);
	}

	@Test
	void shouldConvertJsonRecordToInferredTypeWithoutTypeIdHeader() {
		PassThroughStringJacksonJsonMessageConverter converter = new PassThroughStringJacksonJsonMessageConverter(
				new JsonMapper());
		ConsumerRecord<String, String> record = new ConsumerRecord<>("book-returned", 0, 0L, null,
				"{ \"bookName\" : \"foo\" }");

		// The container passes the listener-method parameter type as the inferred payload
		// type; there is no __TypeId__ header on the record.
		Message<?> message = converter.toMessage(record, null, null, BookReturned.class);

		assertThat(message.getPayload()).isInstanceOf(BookReturned.class);
		assertThat(((BookReturned) message.getPayload()).getBookName()).isEqualTo("foo");
	}

	@Test
	void shouldPassThroughRawJsonForStringListenerParameter() {
		// The common consumer declares a String @KafkaListener parameter and parses the
		// JSON itself. The stock StringJacksonJsonMessageConverter would call
		// readValue("{...}", String.class) and fail with a ConversionException; the
		// pass-through converter must hand the raw JSON string to the listener unchanged.
		PassThroughStringJacksonJsonMessageConverter converter = new PassThroughStringJacksonJsonMessageConverter(
				new JsonMapper());
		ConsumerRecord<String, String> record = new ConsumerRecord<>("verifications", 0, 0L, null,
				"{\"bookName\":\"foo\"}");

		Message<?> message = converter.toMessage(record, null, null, String.class);

		assertThat(message.getPayload()).isEqualTo("{\"bookName\":\"foo\"}");
	}

	@Test
	void shouldPassThroughRawBytesForByteArrayListenerParameter() {
		PassThroughStringJacksonJsonMessageConverter converter = new PassThroughStringJacksonJsonMessageConverter(
				new JsonMapper());
		ConsumerRecord<String, String> record = new ConsumerRecord<>("verifications", 0, 0L, null,
				"{\"bookName\":\"foo\"}");

		Message<?> message = converter.toMessage(record, null, null, byte[].class);

		assertThat(message.getPayload()).isInstanceOf(byte[].class);
		assertThat(new String((byte[]) message.getPayload(), java.nio.charset.StandardCharsets.UTF_8))
			.isEqualTo("{\"bookName\":\"foo\"}");
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConverterConfiguration {

		@Bean
		RecordMessageConverter customRecordMessageConverter() {
			return new StringJacksonJsonMessageConverter(new JsonMapper());
		}

	}

}
