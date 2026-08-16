/*
 * Copyright 2018-present the original author or authors.
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

package sh.stubborn.contract.stubrunner.junit;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

/**
 * Message verifier that throws on every operation, used as a default when none is
 * provided.
 *
 * @author Olga Maciaszek-Sharma
 * @since 1.0.0
 */
class ExceptionThrowingMessageVerifier implements MessageVerifierSender<Object>, MessageVerifierReceiver<Object> {

	private static final String EXCEPTION_MESSAGE = "Please provide a custom MessageVerifier to use this feature";

	@Override
	public void send(Object message, String destination, @Nullable YamlContract contract) {
		throw new UnsupportedOperationException(EXCEPTION_MESSAGE);
	}

	@Override
	public @Nullable Object receive(String destination, long timeout, TimeUnit timeUnit,
			@Nullable YamlContract contract) {
		throw new UnsupportedOperationException(EXCEPTION_MESSAGE);
	}

	@Override
	public @Nullable Object receive(String destination, @Nullable YamlContract contract) {
		throw new UnsupportedOperationException(EXCEPTION_MESSAGE);
	}

	@Override
	public <T> void send(T payload, @Nullable Map<String, Object> headers, String destination,
			@Nullable YamlContract contract) {
		throw new UnsupportedOperationException(EXCEPTION_MESSAGE);
	}

}
