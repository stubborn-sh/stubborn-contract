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

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.utils.Bytes;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.AbstractKafkaListenerContainerFactory;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;

/**
 * Out-of-the-box JSON conversion for a consumer's {@code @KafkaListener} in a contract
 * test. Restores the zero-config developer experience users had with Spring Cloud
 * Contract: a consumer running {@code @AutoConfigureStubRunner} messaging against a real
 * Kafka broker no longer has to hand-configure a JSON deserializer or converter to bind a
 * typed listener parameter.
 *
 * <p>
 * The stub-runner Kafka backend ({@code StubbornKafkaMessageVerifierSender}) publishes a
 * contract's {@code outputMessage} to the broker as raw JSON bytes with a
 * {@code contentType=application/json} header and <em>no</em> {@code __TypeId__} header.
 * Spring Boot defaults the consumer's {@code value.deserializer} to a
 * {@code StringDeserializer}, so the record value arrives as a {@code String}. This
 * config installs a Jackson 3 {@link RecordMessageConverter} <em>on the
 * {@code @KafkaListener} container factory only</em>; because the type mapper defaults to
 * {@code TypePrecedence.INFERRED}, the JSON body is deserialized into the type declared
 * by the listener method parameter. No {@code __TypeId__} header is required.
 *
 * <p>
 * The converter is a {@link StringJacksonJsonMessageConverter} subclass that <em>passes
 * through</em> a {@code String} or {@code byte[]} listener parameter unchanged (see
 * {@code PassThroughStringJacksonJsonMessageConverter}). A stock
 * {@code StringJacksonJsonMessageConverter} would try to deserialize the JSON body
 * <em>into</em> {@code String} and fail with a {@code ConversionException}, breaking the
 * common consumer that receives the raw JSON as a {@code String} and parses it itself.
 * Only a typed listener parameter (a record, a {@code Map}, …) is JSON-bound.
 *
 * <h3>Why a listener-scoped post-processor instead of a bare bean</h3>
 * <p>
 * Exposing the converter as a bare {@link RecordMessageConverter} {@code @Bean} would let
 * Spring Boot wire it into <em>both</em> the {@code @KafkaListener} container factory
 * <em>and</em> the producer's {@code KafkaTemplate} (see
 * {@code KafkaAutoConfiguration#kafkaTemplate} and
 * {@code KafkaAnnotationDrivenConfiguration}). On the producer side that double-encodes a
 * contract's {@code outputMessage}: a producer already serializing with a
 * {@code JsonSerializer} would have its JSON body re-wrapped as a JSON string and tagged
 * with {@code __TypeId__=java.lang.String}, so an {@code assertThatJson(...).field(...)}
 * assertion generated from a Groovy {@code outputMessage} body fails with a jsonpath "can
 * not be applied to primitives" error. Installing the converter through a
 * {@link BeanPostProcessor} that targets only the
 * {@link AbstractKafkaListenerContainerFactory} keeps the producer {@code KafkaTemplate}
 * on its default converter. This mirrors how the JMS and RabbitMQ out-of-the-box
 * converters are scoped to the listener side.
 *
 * <h3>Guards</h3>
 * <ul>
 * <li>{@link ConditionalOnClass @ConditionalOnClass(RecordMessageConverter.class)} — only
 * active when Spring Kafka is on the classpath, so it is inert without Kafka
 * messaging.</li>
 * <li>{@link ConditionalOnMissingBean @ConditionalOnMissingBean(RecordMessageConverter.class)}
 * — when the user has already defined their own {@link RecordMessageConverter} bean,
 * Spring Boot wires it into the listener factory and this post-processor backs off, so it
 * <em>never</em> overrides a user-supplied converter.</li>
 * <li>{@link ConditionalOnProperty @ConditionalOnProperty} on
 * {@code stubborn.contract.messaging.consumer-converters.enabled} (default {@code true})
 * — set it to {@code false} to opt out of the magic entirely.</li>
 * <li>{@link ConditionalOnProperty @ConditionalOnProperty} on
 * {@code spring.kafka.consumer.value-deserializer} — the converter is installed only when
 * the value deserializer is unset (Boot's default {@code StringDeserializer}) or
 * explicitly {@code StringDeserializer}. A consumer that configures its own deserializer
 * (for example a {@code JsonDeserializer} that already yields the typed value) keeps full
 * control and this converter <em>backs off</em>, so it never double-converts an
 * already-deserialized value.</li>
 * </ul>
 *
 * <p>
 * The configuration is imported only through {@code @AutoConfigureMessageVerifier} (and
 * hence {@code @AutoConfigureStubRunner}), so it is harmless in production.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RecordMessageConverter.class)
@ConditionalOnProperty(name = "stubborn.contract.messaging.consumer-converters.enabled", havingValue = "true",
		matchIfMissing = true)
public class ContractVerifierKafkaConsumerConverterConfiguration {

	@Bean
	@ConditionalOnMissingBean(RecordMessageConverter.class)
	@ConditionalOnProperty(name = "spring.kafka.consumer.value-deserializer",
			havingValue = "org.apache.kafka.common.serialization.StringDeserializer", matchIfMissing = true)
	static StubbornContractKafkaListenerConverterPostProcessor stubbornContractKafkaListenerConverterPostProcessor() {
		return new StubbornContractKafkaListenerConverterPostProcessor();
	}

	/**
	 * Installs a Jackson JSON {@link RecordMessageConverter} on every
	 * {@link AbstractKafkaListenerContainerFactory} as it is created, leaving the
	 * producer {@code KafkaTemplate} untouched. Scoping the converter to the listener
	 * factory (rather than exposing it as a bare bean) is what keeps the producer side
	 * free of the double-encoding described in the enclosing class' Javadoc.
	 *
	 * @since 1.0.0
	 */
	static class StubbornContractKafkaListenerConverterPostProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (bean instanceof AbstractKafkaListenerContainerFactory<?, ?, ?> factory) {
				factory.setRecordMessageConverter(new PassThroughStringJacksonJsonMessageConverter(new JsonMapper()));
			}
			return bean;
		}

	}

	/**
	 * A {@link StringJacksonJsonMessageConverter} that passes a {@code String} or
	 * {@code byte[]} listener parameter through unchanged, deferring to the JSON
	 * converter only for a typed listener parameter (a record, a {@code Map}, …).
	 *
	 * <p>
	 * The stock converter always tries to deserialize the JSON body <em>into</em> the
	 * listener parameter type. For the very common consumer that declares a
	 * {@code String} parameter and parses the JSON itself, that means calling
	 * {@code jsonMapper.readValue("{...}", String.class)}, which throws a
	 * {@code ConversionException} ("Failed to convert from JSON") and fails the listener
	 * invocation. By returning the raw record value for {@code String}/{@code byte[]}
	 * parameters we keep the zero-config typed-binding for record parameters while
	 * leaving a raw-payload consumer working exactly as it does with Spring Boot's
	 * default converter.
	 *
	 * @since 1.0.0
	 */
	static final class PassThroughStringJacksonJsonMessageConverter extends StringJacksonJsonMessageConverter {

		PassThroughStringJacksonJsonMessageConverter(JsonMapper jsonMapper) {
			super(jsonMapper);
		}

		@Override
		protected Object extractAndConvertValue(ConsumerRecord<?, ?> record, Type type) {
			Object value = record.value();
			if (value != null && type == String.class) {
				return asString(value);
			}
			if (value != null && type == byte[].class) {
				return asBytes(value);
			}
			return super.extractAndConvertValue(record, type);
		}

		private static Object asString(Object value) {
			if (value instanceof byte[] bytes) {
				return new String(bytes, StandardCharsets.UTF_8);
			}
			if (value instanceof Bytes bytes) {
				return new String(bytes.get(), StandardCharsets.UTF_8);
			}
			return value;
		}

		private static Object asBytes(Object value) {
			if (value instanceof String string) {
				return string.getBytes(StandardCharsets.UTF_8);
			}
			if (value instanceof Bytes bytes) {
				return bytes.get();
			}
			return value;
		}

	}

}
