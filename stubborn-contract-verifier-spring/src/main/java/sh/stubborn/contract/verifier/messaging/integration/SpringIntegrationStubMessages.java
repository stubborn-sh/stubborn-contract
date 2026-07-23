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

package sh.stubborn.contract.verifier.messaging.integration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;

/**
 * Spring Integration implementation of {@link MessageVerifierSender} and
 * {@link MessageVerifierReceiver}.
 *
 * @author Marcin Grzejszczak
 */
public class SpringIntegrationStubMessages
		implements MessageVerifierSender<Message<?>>, MessageVerifierReceiver<Message<?>> {

	private static final Log log = LogFactory.getLog(SpringIntegrationStubMessages.class);

	private final ApplicationContext context;

	private final ContractVerifierIntegrationMessageBuilder builder = new ContractVerifierIntegrationMessageBuilder();

	public SpringIntegrationStubMessages(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public <T> void send(T payload, Map<String, Object> headers, String destination, @Nullable YamlContract contract) {
		send(this.builder.create(payload, headers), destination, contract);
	}

	@Override
	public void send(Message<?> message, String destination, @Nullable YamlContract contract) {
		try {
			if (trySendViaInputDestination(message, destination)) {
				return;
			}
			MessageChannel messageChannel = this.context.getBean(destination, MessageChannel.class);
			messageChannel.send(message);
		}
		catch (Exception ex) {
			log.error("Exception occurred while trying to send a message [" + message + "] "
					+ "to a channel with name [" + destination + "]", ex);
			throw ex;
		}
	}

	private boolean trySendViaInputDestination(Message<?> message, String destination) {
		if (this.context.containsBean(destination)) {
			return false;
		}
		// Spring Cloud Stream 5.x test binder: channel beans are not registered by name.
		// InputDestination.send(Message, String) routes by destination name — use it
		// reflectively to avoid a compile-time dependency on the test binder.
		try {
			Class<?> inputDestinationType = Class
				.forName("org.springframework.cloud.stream.binder.test.InputDestination");
			if (this.context.getBeanNamesForType(inputDestinationType).length == 0) {
				return false;
			}
			Object inputDestination = this.context.getBean(inputDestinationType);
			java.lang.reflect.Method sendMethod = inputDestinationType.getMethod("send", Message.class, String.class);
			sendMethod.invoke(inputDestination, message, destination);
			return true;
		}
		catch (Exception ignored) {
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public @Nullable Message<?> receive(String destination, long timeout, TimeUnit timeUnit,
			@Nullable YamlContract contract) {
		try {
			if (!this.context.containsBean(destination)) {
				// Spring Cloud Stream 5.x test binder: use
				// OutputDestination.receive(long,
				// String) reflectively to avoid a compile-time dependency on the test
				// binder.
				Message<?> msg = tryReceiveViaOutputDestination(destination, timeUnit.toMillis(timeout));
				if (msg != null) {
					return msg;
				}
			}
			PollableChannel messageChannel = this.context.getBean(destination, PollableChannel.class);
			return messageChannel.receive(timeUnit.toMillis(timeout));
		}
		catch (Exception ex) {
			log.error("Exception occurred while trying to read a message from " + " a channel with name [" + destination
					+ "]", ex);
			throw new IllegalStateException(ex);
		}
	}

	private @Nullable Message<?> tryReceiveViaOutputDestination(String destination, long timeoutMillis) {
		try {
			Class<?> outputDestinationType = Class
				.forName("org.springframework.cloud.stream.binder.test.OutputDestination");
			if (this.context.getBeanNamesForType(outputDestinationType).length == 0) {
				return null;
			}
			Object outputDestination = this.context.getBean(outputDestinationType);
			java.lang.reflect.Method receiveMethod = outputDestinationType.getMethod("receive", long.class,
					String.class);
			return (Message<?>) receiveMethod.invoke(outputDestination, timeoutMillis, destination);
		}
		catch (Exception ignored) {
			return null;
		}
	}

	@Override
	public @Nullable Message<?> receive(String destination, @Nullable YamlContract contract) {
		return receive(destination, 5, TimeUnit.SECONDS, contract);
	}

}
