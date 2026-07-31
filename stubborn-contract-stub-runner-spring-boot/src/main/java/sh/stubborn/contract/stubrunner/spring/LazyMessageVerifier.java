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

package sh.stubborn.contract.stubrunner.spring;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.noop.NoOpStubMessages;

import org.springframework.beans.factory.BeanFactory;

/**
 * Lazily resolves the {@link MessageVerifierSender} and {@link MessageVerifierReceiver}
 * beans, falling back to a no-op implementation when none are available.
 *
 * @author Marcin Grzejszczak
 */
@SuppressWarnings("unchecked")
class LazyMessageVerifier implements MessageVerifierSender<Object>, MessageVerifierReceiver<Object> {

	private @Nullable MessageVerifierSender<Object> messageVerifierSender;

	private @Nullable MessageVerifierReceiver<Object> messageVerifierReceiver;

	private final BeanFactory beanFactory;

	LazyMessageVerifier(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	private MessageVerifierSender<Object> messageVerifierSender() {
		MessageVerifierSender<Object> sender = this.messageVerifierSender;
		if (sender == null) {
			sender = this.beanFactory.getBeanProvider(MessageVerifierSender.class)
				.getIfAvailable(NoOpStubMessages::new);
			this.messageVerifierSender = sender;
		}
		return sender;
	}

	private MessageVerifierReceiver<Object> messageVerifierReceiver() {
		MessageVerifierReceiver<Object> receiver = this.messageVerifierReceiver;
		if (receiver == null) {
			receiver = this.beanFactory.getBeanProvider(MessageVerifierReceiver.class)
				.getIfAvailable(NoOpStubMessages::new);
			this.messageVerifierReceiver = receiver;
		}
		return receiver;
	}

	@Override
	public void send(Object message, String destination, @Nullable YamlContract contract) {
		messageVerifierSender().send(message, destination, contract);
	}

	@Override
	public @Nullable Object receive(String destination, long timeout, TimeUnit timeUnit,
			@Nullable YamlContract contract) {
		return messageVerifierReceiver().receive(destination, timeout, timeUnit, contract);
	}

	@Override
	public @Nullable Object receive(String destination, @Nullable YamlContract contract) {
		return messageVerifierReceiver().receive(destination, contract);
	}

	@Override
	public <T> void send(T payload, @Nullable Map<String, Object> headers, String destination,
			@Nullable YamlContract contract) {
		messageVerifierSender().send(payload, headers, destination, contract);
	}

}
