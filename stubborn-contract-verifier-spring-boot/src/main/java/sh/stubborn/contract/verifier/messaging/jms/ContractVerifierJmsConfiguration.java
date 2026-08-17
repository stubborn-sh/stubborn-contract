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

import jakarta.jms.ConnectionFactory;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.integration.ContractVerifierIntegrationConfiguration;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;
import sh.stubborn.contract.verifier.messaging.noop.NoOpContractVerifierAutoConfiguration;
import sh.stubborn.messaging.jms.JmsMessage;
import sh.stubborn.messaging.jms.StubbornJmsMessageVerifierReceiver;
import sh.stubborn.messaging.jms.StubbornJmsMessageVerifierSender;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.core.JmsTemplate;

/**
 * Spring-on-top wiring for the Spring-free JMS sender and receiver. It exposes them as
 * the messaging {@code MessageVerifierSender}/{@code MessageVerifierReceiver} beans and a
 * {@link ContractVerifierMessaging} bean that a {@code @AutoConfigureMessageVerifier}
 * test picks up. Sender and receiver are registered as independent beans, each guarded by
 * its own {@code @ConditionalOnMissingBean}, so either can be overridden without
 * disturbing the other — matching the Kafka, Rabbit, Camel and Spring Integration
 * backends.
 *
 * <p>
 * The broker is taken from the application's {@link ConnectionFactory} (which Spring Boot
 * autoconfigures from {@code spring.artemis.*}/{@code spring.activemq.*} or an embedded
 * broker), so the same test setup used with Spring JMS keeps working — only the
 * underlying verifiers are now the Spring-free, plain {@code jakarta.jms}
 * implementations.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ JmsTemplate.class, StubbornJmsMessageVerifierSender.class })
@ConditionalOnProperty(name = "stubborn.contract.stubrunner.jms.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore({ ContractVerifierIntegrationConfiguration.class, NoOpContractVerifierAutoConfiguration.class })
public class ContractVerifierJmsConfiguration {

	@Bean(destroyMethod = "close")
	@Lazy
	@ConditionalOnMissingBean(MessageVerifierSender.class)
	StubbornJmsMessageVerifierSender stubbornJmsMessageVerifierSender(ConnectionFactory connectionFactory) {
		return new StubbornJmsMessageVerifierSender(connectionFactory);
	}

	@Bean(destroyMethod = "close")
	@Lazy
	@ConditionalOnMissingBean(MessageVerifierReceiver.class)
	StubbornJmsMessageVerifierReceiver stubbornJmsMessageVerifierReceiver(ConnectionFactory connectionFactory) {
		return new StubbornJmsMessageVerifierReceiver(connectionFactory);
	}

	@Bean
	@Lazy
	@ConditionalOnMissingBean(ContractVerifierMessaging.class)
	ContractVerifierMessaging<JmsMessage> contractVerifierJmsMessaging(MessageVerifierSender<JmsMessage> sender,
			MessageVerifierReceiver<JmsMessage> receiver) {
		return new ContractVerifierMessaging<>(sender, receiver);
	}

}
