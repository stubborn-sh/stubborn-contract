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
 * config installs a {@link StringJacksonJsonMessageConverter} (Jackson 3) as the
 * {@link RecordMessageConverter} <em>on the {@code @KafkaListener} container factory
 * only</em>; because the type mapper defaults to {@code TypePrecedence.INFERRED}, the
 * JSON body is deserialized into the type declared by the listener method parameter. No
 * {@code __TypeId__} header is required.
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
				factory.setRecordMessageConverter(new StringJacksonJsonMessageConverter(new JsonMapper()));
			}
			return bean;
		}

	}

}
