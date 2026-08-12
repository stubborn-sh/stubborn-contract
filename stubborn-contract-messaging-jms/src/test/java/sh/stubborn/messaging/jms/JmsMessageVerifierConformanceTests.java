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

package sh.stubborn.messaging.jms;

import java.util.Map;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.tck.AbstractMessageVerifierConformanceTests;

/**
 * Runs the transport-neutral messaging conformance suite against the Spring-free JMS
 * sender and receiver, backed by a real (embedded, in-VM) ActiveMQ Artemis broker — so,
 * unlike Kafka and RabbitMQ, this needs no Docker.
 *
 * @author Marcin Grzejszczak
 */
class JmsMessageVerifierConformanceTests extends AbstractMessageVerifierConformanceTests<JmsMessage> {

	private static EmbeddedActiveMQ broker;

	private static ConnectionFactory connectionFactory;

	@BeforeAll
	static void startBroker() throws Exception {
		Configuration config = new ConfigurationImpl().setPersistenceEnabled(false)
			.setSecurityEnabled(false)
			.addAcceptorConfiguration("in-vm", "vm://0");
		broker = new EmbeddedActiveMQ().setConfiguration(config);
		broker.start();
		connectionFactory = new ActiveMQConnectionFactory("vm://0");
	}

	@AfterAll
	static void stopBroker() throws Exception {
		if (broker != null) {
			broker.stop();
		}
	}

	@Override
	protected MessageVerifierSender<JmsMessage> sender() {
		return new StubbornJmsMessageVerifierSender(connectionFactory);
	}

	@Override
	protected MessageVerifierReceiver<JmsMessage> receiver() {
		return new StubbornJmsMessageVerifierReceiver(connectionFactory);
	}

	@Override
	protected JmsMessage message(@Nullable Object payload, Map<String, Object> headers) {
		return new JmsMessage(payload, headers);
	}

}
