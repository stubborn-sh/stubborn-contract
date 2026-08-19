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

import tools.jackson.databind.json.JsonMapper;

import org.springframework.amqp.support.converter.JacksonJavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Out-of-the-box JSON conversion for a consumer's {@code @RabbitListener} in a contract
 * test. Restores the zero-config developer experience users had with Spring Cloud
 * Contract: a consumer running {@code @AutoConfigureStubRunner} messaging against a real
 * RabbitMQ broker no longer has to hand-configure a {@code JacksonJsonMessageConverter}
 * to bind a typed listener parameter.
 *
 * <p>
 * The stub-runner Rabbit backend ({@code StubbornRabbitMessageVerifierSender}) publishes
 * a contract's {@code outputMessage} to the broker as raw JSON bytes with a
 * {@code contentType=application/json} property. Without a JSON converter Spring AMQP's
 * default {@code SimpleMessageConverter} yields a {@code String}, which fails to bind to
 * a typed listener record. This config registers a Jackson 3
 * {@link JacksonJsonMessageConverter} as the single {@link MessageConverter} bean that
 * Spring Boot wires into the {@code @RabbitListener} container factory. Its type mapper
 * is set to {@link TypePrecedence#INFERRED} so the target type always comes from the
 * listener method parameter — even when the transport-neutral message carries a Spring
 * {@code __TypeId__} header (Spring AMQP would otherwise honour that header, defaulting
 * to {@code TYPE_ID} precedence, and deserialize the JSON into the wrong type).
 *
 * <h3>Guards</h3>
 * <ul>
 * <li>{@link ConditionalOnClass @ConditionalOnClass(MessageConverter.class)} — only
 * active when Spring AMQP is on the classpath, so it is inert without Rabbit
 * messaging.</li>
 * <li>{@link ConditionalOnMissingBean @ConditionalOnMissingBean(MessageConverter.class)}
 * — it <em>never</em> overrides a converter the user has already defined.</li>
 * <li>{@link ConditionalOnProperty @ConditionalOnProperty} on
 * {@code stubborn.contract.messaging.consumer-converters.enabled} (default {@code true})
 * — set it to {@code false} to opt out of the magic entirely.</li>
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
@ConditionalOnClass(MessageConverter.class)
@ConditionalOnProperty(name = "stubborn.contract.messaging.consumer-converters.enabled", havingValue = "true",
		matchIfMissing = true)
public class ContractVerifierRabbitConsumerConverterConfiguration {

	@Bean
	@ConditionalOnMissingBean(MessageConverter.class)
	JacksonJsonMessageConverter stubbornContractRabbitJsonMessageConverter() {
		JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(new JsonMapper());
		// A transport-neutral contract message may carry a Spring '__TypeId__' header
		// reflecting the wire payload's type (for example 'java.lang.String'). Spring
		// AMQP's
		// type mapper defaults to TYPE_ID precedence, so it would honour that header and
		// deserialize the JSON into a String instead of the listener's type. Force
		// INFERRED
		// precedence so the target type always comes from the '@RabbitListener' method
		// parameter — matching the Kafka converter and the zero-config contract.
		converter.setTypePrecedence(TypePrecedence.INFERRED);
		return converter;
	}

}
