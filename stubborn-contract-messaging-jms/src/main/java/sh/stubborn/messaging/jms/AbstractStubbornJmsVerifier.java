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

import java.io.Closeable;
import java.time.Duration;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared connection lifecycle for the Spring-free JMS sender and receiver. Each concrete
 * verifier owns its own started {@link Connection} so a sender and a receiver are fully
 * independent — either can be replaced without disturbing the other, mirroring how the
 * {@code MessageVerifierSender} and {@code MessageVerifierReceiver} beans are overridden
 * separately.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
abstract class AbstractStubbornJmsVerifier implements Closeable {

	private static final Logger log = LoggerFactory.getLogger(AbstractStubbornJmsVerifier.class);

	final Duration defaultReceiveTimeout;

	final Connection connection;

	AbstractStubbornJmsVerifier(ConnectionFactory connectionFactory, Duration defaultReceiveTimeout) {
		this.defaultReceiveTimeout = defaultReceiveTimeout;
		this.connection = openConnection(connectionFactory);
	}

	private static Connection openConnection(ConnectionFactory connectionFactory) {
		try {
			Connection connection = connectionFactory.createConnection();
			connection.start();
			return connection;
		}
		catch (JMSException ex) {
			throw new IllegalStateException("Failed to open a JMS connection", ex);
		}
	}

	@Override
	public void close() {
		try {
			this.connection.close();
		}
		catch (JMSException ex) {
			log.warn("Failed to close JMS connection", ex);
		}
	}

}
