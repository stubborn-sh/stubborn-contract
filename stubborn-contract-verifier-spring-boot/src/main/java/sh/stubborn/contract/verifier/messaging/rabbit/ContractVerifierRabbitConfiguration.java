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

import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.integration.ContractVerifierIntegrationConfiguration;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;
import sh.stubborn.contract.verifier.messaging.noop.NoOpContractVerifierAutoConfiguration;
import sh.stubborn.messaging.rabbit.RabbitMessage;
import sh.stubborn.messaging.rabbit.StubbornRabbitMessageVerifierReceiver;
import sh.stubborn.messaging.rabbit.StubbornRabbitMessageVerifierSender;

import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring-on-top wiring for the Spring-free RabbitMQ sender and receiver. It exposes them
 * as the messaging {@code MessageVerifierSender}/{@code MessageVerifierReceiver} beans
 * and a {@link ContractVerifierMessaging} bean that a
 * {@code @AutoConfigureMessageVerifier} test picks up. Sender and receiver are registered
 * as independent beans, each guarded by its own {@code @ConditionalOnMissingBean}, so
 * either can be overridden without disturbing the other — matching the JMS, Camel and
 * Spring Integration backends.
 *
 * <p>
 * The broker connection is taken from the application's Spring AMQP
 * {@link ConnectionFactory} (which Spring Boot autoconfigures from
 * {@code spring.rabbitmq.*} or a {@code @ServiceConnection} Testcontainers RabbitMQ), so
 * the same test setup used with Spring AMQP keeps working — only the underlying verifiers
 * are now the Spring-free, plain {@code amqp-client} implementations. They reuse the
 * fully configured underlying {@code com.rabbitmq.client.ConnectionFactory} (host, port,
 * credentials, virtual host) so no connection detail is lost.
 *
 * @author Marcin Grzejszczak
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ RabbitTemplate.class, StubbornRabbitMessageVerifierSender.class })
@ConditionalOnProperty(name = "stubborn.contract.stubrunner.rabbit.enabled", havingValue = "true",
		matchIfMissing = true)
@AutoConfigureBefore({ ContractVerifierIntegrationConfiguration.class, NoOpContractVerifierAutoConfiguration.class })
public class ContractVerifierRabbitConfiguration {

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(MessageVerifierSender.class)
	StubbornRabbitMessageVerifierSender stubbornRabbitMessageVerifierSender(ConnectionFactory connectionFactory) {
		return new StubbornRabbitMessageVerifierSender(rabbitConnectionFactory(connectionFactory));
	}

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(MessageVerifierReceiver.class)
	StubbornRabbitMessageVerifierReceiver stubbornRabbitMessageVerifierReceiver(ConnectionFactory connectionFactory) {
		return new StubbornRabbitMessageVerifierReceiver(rabbitConnectionFactory(connectionFactory));
	}

	@Bean
	@ConditionalOnMissingBean(ContractVerifierMessaging.class)
	ContractVerifierMessaging<RabbitMessage> contractVerifierRabbitMessaging(
			MessageVerifierSender<RabbitMessage> sender, MessageVerifierReceiver<RabbitMessage> receiver) {
		return new ContractVerifierMessaging<>(sender, receiver);
	}

	private static com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory(ConnectionFactory connectionFactory) {
		if (connectionFactory instanceof AbstractConnectionFactory abstractConnectionFactory) {
			return abstractConnectionFactory.getRabbitConnectionFactory();
		}
		throw new IllegalStateException("Cannot derive an amqp-client ConnectionFactory from a "
				+ connectionFactory.getClass().getName()
				+ "; expected an AbstractConnectionFactory (the standard Spring Boot Rabbit connection factory)");
	}

}
