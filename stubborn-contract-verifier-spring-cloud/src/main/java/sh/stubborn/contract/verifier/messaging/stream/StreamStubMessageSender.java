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

package sh.stubborn.contract.verifier.messaging.stream;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

import org.springframework.context.ApplicationContext;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * Sends messages by resolving the target {@link MessageChannel} from the application
 * context.
 *
 * @author Marcin Grzejszczak
 */
class StreamStubMessageSender implements MessageVerifierSender<Message<?>> {

	private static final Log log = LogFactory.getLog(StreamStubMessageSender.class);

	private final ApplicationContext context;

	private final ContractVerifierStreamMessageBuilder builder = new ContractVerifierStreamMessageBuilder();

	private @Nullable BeanFactoryChannelResolver resolver;

	StreamStubMessageSender(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public <T> void send(T payload, @Nullable Map<String, Object> headers, String destination,
			@Nullable YamlContract contract) {
		send(this.builder.create(payload, (headers != null) ? headers : Map.of()), destination, contract);
	}

	@Override
	public void send(Message<?> message, String destination, @Nullable YamlContract contract) {
		try {
			MessageChannel messageChannel = resolver().resolveDestination(destination);
			messageChannel.send(message);
		}
		catch (Exception ex) {
			log.error("Exception occurred while trying to send a message [" + message + "] "
					+ "to a channel with name [" + destination + "]", ex);
			throw ex;
		}
	}

	private BeanFactoryChannelResolver resolver() {
		BeanFactoryChannelResolver channelResolver = this.resolver;
		if (channelResolver == null) {
			channelResolver = this.context.getBean(BeanFactoryChannelResolver.class);
			this.resolver = channelResolver;
		}
		return channelResolver;
	}

}
