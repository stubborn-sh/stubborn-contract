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

import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

/**
 * Out-of-the-box JSON conversion for a consumer's {@code @JmsListener} in a contract
 * test. Restores the zero-config developer experience users had with Spring Cloud
 * Contract: a consumer running {@code @AutoConfigureStubRunner} messaging against a real
 * JMS broker (ActiveMQ Artemis) no longer has to hand-configure a
 * {@code MessageConverter} to bind a typed listener parameter.
 *
 * <p>
 * The stub-runner JMS backend ({@code StubbornJmsMessageVerifierSender}) publishes a
 * contract's {@code outputMessage} to the broker as a {@code TextMessage} carrying the
 * raw JSON body with a {@code contentType=application/json} property and <em>no</em>
 * type-id header. Spring JMS's default {@code SimpleMessageConverter} surfaces that
 * {@code TextMessage} as a {@code String} payload — and, unlike the Kafka and RabbitMQ
 * listener adapters, the JMS listener adapter's default handler-method binding cannot
 * turn that JSON into a typed record. This config installs a Jackson 3
 * {@link JacksonJsonMessageConverter} (from Spring's messaging layer) as the
 * {@code @JmsListener} argument converter through a {@link JmsListenerConfigurer}, so the
 * JSON payload is bound to the type declared by the listener method parameter — the same
 * zero-config outcome as Kafka and RabbitMQ.
 *
 * <p>
 * The converter runs with {@code strictContentTypeMatch=false} so it binds the payload by
 * the listener's target type regardless of the message's {@code contentType}, which a
 * transport-neutral contract message may not surface as a JSON {@code MimeType} header.
 * Spring's own JMS {@code JacksonJsonMessageConverter} is deliberately <em>not</em> used:
 * it resolves the target type from a type-id message property that a contract message
 * does not carry.
 *
 * <h3>Guards</h3>
 * <ul>
 * <li>{@link ConditionalOnClass @ConditionalOnClass(JmsListenerConfigurer.class)} — only
 * active when Spring JMS is on the classpath, so it is inert without JMS messaging.</li>
 * <li>{@link ConditionalOnMissingBean @ConditionalOnMissingBean(JmsListenerConfigurer.class)}
 * — it <em>never</em> overrides a listener configuration the user has already
 * defined.</li>
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
@ConditionalOnClass(JmsListenerConfigurer.class)
@ConditionalOnProperty(name = "stubborn.contract.messaging.consumer-converters.enabled", havingValue = "true",
		matchIfMissing = true)
public class ContractVerifierJmsConsumerConverterConfiguration {

	@Bean
	@ConditionalOnMissingBean(JmsListenerConfigurer.class)
	JmsListenerConfigurer stubbornContractJmsListenerConfigurer() {
		MessageHandlerMethodFactory methodFactory = jsonMessageHandlerMethodFactory();
		return (registrar) -> registrar.setMessageHandlerMethodFactory(methodFactory);
	}

	/**
	 * A {@link MessageHandlerMethodFactory} whose payload converter is a lenient Jackson
	 * 3 JSON converter, so a {@code @JmsListener} method parameter is bound from the JSON
	 * body regardless of the message's declared {@code contentType}.
	 * @return the handler-method factory backing every {@code @JmsListener} in the test
	 */
	private static MessageHandlerMethodFactory jsonMessageHandlerMethodFactory() {
		JacksonJsonMessageConverter jsonConverter = new JacksonJsonMessageConverter(new JsonMapper());
		jsonConverter.setStrictContentTypeMatch(false);
		DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
		factory.setMessageConverter(jsonConverter);
		factory.afterPropertiesSet();
		return factory;
	}

}
