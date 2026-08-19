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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sh.stubborn.messaging.jms.StubbornJmsMessageVerifierSender;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof that the out-of-the-box JMS consumer converter lets a typed
 * {@code @JmsListener} bind a stub-published JSON message with <strong>zero</strong>
 * consumer configuration — against a real (embedded, in-VM) ActiveMQ Artemis broker, no
 * Docker.
 *
 * <p>
 * The stub-runner JMS backend ({@link StubbornJmsMessageVerifierSender}) sends the
 * contract body as a plain JSON {@link jakarta.jms.TextMessage} with no type-id header;
 * the converter registered by {@link ContractVerifierJmsConsumerConverterConfiguration}
 * reads it into a generic tree and Spring's messaging layer binds it to the
 * {@code NotificationEvent} record declared by the listener method.
 *
 * @author Marcin Grzejszczak
 */
class ContractVerifierJmsConsumerConverterIntegrationTests {

	private static final String QUEUE = "notifications";

	private static EmbeddedActiveMQ broker;

	private static ConnectionFactory connectionFactory;

	@BeforeAll
	static void startBroker() throws Exception {
		broker = new EmbeddedActiveMQ().setConfiguration(new ConfigurationImpl().setPersistenceEnabled(false)
			.setSecurityEnabled(false)
			.addAcceptorConfiguration("in-vm", "vm://0"));
		broker.start();
		connectionFactory = new ActiveMQConnectionFactory("vm://0");
	}

	@AfterAll
	static void stopBroker() throws Exception {
		if (broker != null) {
			broker.stop();
		}
	}

	@Test
	void shouldBindStubJsonMessageToTypedListenerWithoutConsumerConfiguration() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.registerBean(ConnectionFactory.class, () -> connectionFactory);
			context.register(ContractVerifierJmsConsumerConverterConfiguration.class, ListenerConfiguration.class,
					NotificationListener.class);
			context.refresh();

			NotificationListener listener = context.getBean(NotificationListener.class);

			// The stub-runner JMS backend publishes the contract body exactly like this:
			// a JSON TextMessage with a contentType=application/json header, no type id.
			StubbornJmsMessageVerifierSender sender = new StubbornJmsMessageVerifierSender(connectionFactory);
			sender.send("{ \"type\" : \"ORDER_CONFIRMED\", \"recipient\" : \"user@example.com\" }",
					Map.of("contentType", "application/json"), QUEUE, null);

			Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
				assertThat(listener.getReceived()).isNotEmpty();
				NotificationEvent event = listener.getReceived().get(0);
				assertThat(event.type()).isEqualTo("ORDER_CONFIRMED");
				assertThat(event.recipient()).isEqualTo("user@example.com");
			});
		}
	}

	@Configuration
	@EnableJms
	static class ListenerConfiguration {

		@Bean
		DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
			DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
			factory.setConnectionFactory(connectionFactory);
			return factory;
		}

	}

	@Component
	static class NotificationListener {

		private final List<NotificationEvent> received = Collections.synchronizedList(new ArrayList<>());

		@JmsListener(destination = QUEUE)
		void onNotification(NotificationEvent event) {
			this.received.add(event);
		}

		List<NotificationEvent> getReceived() {
			return List.copyOf(this.received);
		}

	}

	record NotificationEvent(String type, String recipient) {
	}

}
