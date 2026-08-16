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

package sh.stubborn.contract.verifier.messaging.camel;

import org.apache.camel.Message;
import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;

/**
 * Camel specific {@link ContractVerifierMessaging} implementation.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
class ContractVerifierCamelHelper extends ContractVerifierMessaging<Message> {

	ContractVerifierCamelHelper(MessageVerifierSender<Message> sender, MessageVerifierReceiver<Message> receiver) {
		super(sender, receiver);
	}

	@Override
	protected @Nullable ContractVerifierMessage convert(@Nullable Message receive) {
		if (receive == null) {
			return null;
		}
		return new ContractVerifierMessage(receive.getBody(), receive.getHeaders());
	}

}
