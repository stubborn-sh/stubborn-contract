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

package sh.stubborn.messaging.kafka;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;

/**
 * Spring-free {@link ContractVerifierMessaging} for Kafka. It bridges the generator's
 * {@link ContractVerifierMessage} to the transport-neutral {@link KafkaMessage} carried
 * by {@link StubbornKafkaMessageVerifier}, overriding {@link #convert(KafkaMessage)} so
 * that a received record's headers are preserved (the base implementation drops them).
 *
 * <p>
 * A generated Kafka messaging test's base class wires one of these:
 * {@code new ContractVerifierKafkaHelper(verifier, verifier)} where {@code verifier} is a
 * {@link StubbornKafkaMessageVerifier} pointed at the (Testcontainers) broker.
 *
 * @author Marcin Grzejszczak
 */
public class ContractVerifierKafkaHelper extends ContractVerifierMessaging<KafkaMessage> {

	public ContractVerifierKafkaHelper(MessageVerifierSender<KafkaMessage> sender,
			MessageVerifierReceiver<KafkaMessage> receiver) {
		super(sender, receiver);
	}

	@Override
	protected @Nullable ContractVerifierMessage convert(@Nullable KafkaMessage message) {
		if (message == null) {
			return null;
		}
		return new ContractVerifierMessage(message.getPayload(), message.getHeaders());
	}

}
