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

package sh.stubborn.messaging.rabbit;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared connection lifecycle for the Spring-free RabbitMQ sender and receiver. Each
 * concrete verifier owns its own {@link Connection} so a sender and a receiver are fully
 * independent — either can be replaced without disturbing the other, mirroring how the
 * {@code MessageVerifierSender} and {@code MessageVerifierReceiver} beans are overridden
 * separately.
 *
 * @author Marcin Grzejszczak
 */
abstract class AbstractStubbornRabbitVerifier implements Closeable {

	private static final Logger log = LoggerFactory.getLogger(AbstractStubbornRabbitVerifier.class);

	static final String DEFAULT_EXCHANGE = "";

	final Duration defaultReceiveTimeout;

	final Connection connection;

	AbstractStubbornRabbitVerifier(ConnectionFactory connectionFactory, Duration defaultReceiveTimeout) {
		this.defaultReceiveTimeout = defaultReceiveTimeout;
		this.connection = openConnection(connectionFactory);
	}

	static ConnectionFactory factoryFor(String amqpUri) {
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setUri(amqpUri);
			return factory;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Invalid RabbitMQ AMQP URI '" + amqpUri + "'", ex);
		}
	}

	private static Connection openConnection(ConnectionFactory factory) {
		try {
			return factory.newConnection();
		}
		catch (IOException | TimeoutException ex) {
			throw new IllegalStateException("Failed to connect to RabbitMQ broker", ex);
		}
	}

	@Override
	public void close() {
		try {
			this.connection.close();
		}
		catch (IOException ex) {
			log.warn("Failed to close RabbitMQ connection", ex);
		}
	}

}
