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

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * config registers a {@link StringJacksonJsonMessageConverter} (Jackson 3) as the
 * {@link RecordMessageConverter}; Spring Boot wires that single bean into the
 * {@code @KafkaListener} container factory, and — because the type mapper defaults to
 * {@code TypePrecedence.INFERRED} — the JSON body is deserialized into the type declared
 * by the listener method parameter. No {@code __TypeId__} header is required.
 *
 * <h3>Guards</h3>
 * <ul>
 * <li>{@link ConditionalOnClass @ConditionalOnClass(RecordMessageConverter.class)} — only
 * active when Spring Kafka is on the classpath, so it is inert without Kafka
 * messaging.</li>
 * <li>{@link ConditionalOnMissingBean @ConditionalOnMissingBean(RecordMessageConverter.class)}
 * — it <em>never</em> overrides a converter the user has already defined.</li>
 * <li>{@link ConditionalOnProperty @ConditionalOnProperty} on
 * {@code stubborn.contract.messaging.consumer-converters.enabled} (default {@code true})
 * — set it to {@code false} to opt out of the magic entirely.</li>
 * </ul>
 *
 * <p>
 * The converter never touches the consumer's deserializer configuration, so a user who
 * has configured a different {@code value.deserializer} keeps full control. The
 * configuration is imported only through {@code @AutoConfigureMessageVerifier} (and hence
 * {@code @AutoConfigureStubRunner}), so it is harmless in production.
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
	StringJacksonJsonMessageConverter stubbornContractKafkaJsonMessageConverter() {
		return new StringJacksonJsonMessageConverter(new JsonMapper());
	}

}
